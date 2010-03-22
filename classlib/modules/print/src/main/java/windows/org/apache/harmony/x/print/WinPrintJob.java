/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.harmony.x.print;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.TextSyntax;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.event.PrintJobAttributeEvent;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

import org.apache.harmony.awt.gl.CommonGraphics2D;
import org.apache.harmony.awt.gl.windows.WinGDIPGraphics2D;

class WinPrintJob implements CancelablePrintJob {

    final Object                                               lock;
    final Map<PrintJobListener, Object>                        jobListeners;
    final Map<PrintJobAttributeListener, PrintJobAttributeSet> attrListeners;
    final WinPrintService                                      service;
    Printer                                                    printer;

    WinPrintJob(final WinPrintService service) {
        this.service = service;
        lock = new Object();
        jobListeners = Collections
                        .synchronizedMap(new WeakHashMap<PrintJobListener, Object>());
        attrListeners = Collections
                        .synchronizedMap(new WeakHashMap<PrintJobAttributeListener, PrintJobAttributeSet>());
    }

    public void cancel() throws PrintException {
        synchronized (lock) {
            if (printer == null) {
                throw new PrintException("Job is not started"); //$NON-NLS-1$
            }
            printer.cancelJob();
        }
    }

    public void addPrintJobAttributeListener(
                    final PrintJobAttributeListener listener,
                    final PrintJobAttributeSet attributes) {
        if (listener != null) {
            attrListeners.put(listener, attributes);
        }
    }

    public void addPrintJobListener(final PrintJobListener listener) {
        if (listener != null) {
            jobListeners.put(listener, null);
        }
    }

    public PrintJobAttributeSet getAttributes() {
        final PrintJobAttributeSet attrs = service.getPrinterProps()
                        .getAttributes(new HashPrintJobAttributeSet());

        for (Attribute attr : attrs.toArray()) {
            if (!(attr instanceof PrintJobAttribute)) {
                attrs.remove(attr);
            }
        }

        return AttributeSetUtilities.unmodifiableView(attrs);
    }

    public PrintService getPrintService() {
        return service;
    }

    public void print(final Doc doc, final PrintRequestAttributeSet attributes)
                    throws PrintException {
        synchronized (lock) {
            if (printer != null) {
                throw new PrintException("Printer is busy"); //$NON-NLS-1$
            } else {
                final DocFlavor flavor = doc.getDocFlavor();

                if ((flavor == null) || !service.isDocFlavorSupported(flavor)) {
                    throw new PrintException("Doc flavor is not supported"); //$NON-NLS-1$
                }

                printer = new Printer(doc, attributes);
                printer.print();
            }
        }
    }

    public void removePrintJobAttributeListener(
                    final PrintJobAttributeListener listener) {
        attrListeners.remove(listener);
    }

    public void removePrintJobListener(final PrintJobListener listener) {
        jobListeners.remove(listener);
    }

    void notifyJobListeners(final int reason) {
        final PrintJobEvent event = new PrintJobEvent(this, reason);

        for (PrintJobListener listener : jobListeners.keySet()) {
            switch (reason) {
            case PrintJobEvent.DATA_TRANSFER_COMPLETE:
                listener.printDataTransferCompleted(event);
                break;
            case PrintJobEvent.JOB_CANCELED:
                listener.printJobCanceled(event);
                break;
            case PrintJobEvent.JOB_COMPLETE:
                listener.printJobCompleted(event);
                break;
            case PrintJobEvent.JOB_FAILED:
                listener.printJobFailed(event);
                break;
            case PrintJobEvent.NO_MORE_EVENTS:
                listener.printJobNoMoreEvents(event);
                break;
            case PrintJobEvent.REQUIRES_ATTENTION:
                listener.printJobRequiresAttention(event);
                break;
            }
        }
    }

    void notifyAttrListeners(final PrintJobAttribute... attrs) {
        final PrintJobAttributeSet attrSet = new HashPrintJobAttributeSet(attrs);
        final PrintJobAttributeEvent event = new PrintJobAttributeEvent(this,
                        attrSet);

        for (PrintJobAttribute attr : attrs) {
            final Class<? extends Attribute> cat = attr.getCategory();

            for (Map.Entry<PrintJobAttributeListener, PrintJobAttributeSet> e : attrListeners
                            .entrySet()) {
                if ((e.getValue() == null) || (e.getValue().containsKey(cat))) {
                    e.getKey().attributeUpdate(event);
                }
            }
        }
    }

    void notifyAttrListeners(final AttributeSet... attrSets) {
        final List<PrintJobAttribute> list = new ArrayList<PrintJobAttribute>();
        final PrintJobAttribute[] attrs;

        for (AttributeSet attrSet : attrSets) {
            if (attrSet != null) {
                for (Attribute attr : attrSet.toArray()) {
                    if (attr instanceof PrintJobAttribute) {
                        list.add((PrintJobAttribute) attr);
                    }
                }
            }
        }

        attrs = new PrintJobAttribute[list.size()];
        notifyAttrListeners(list.toArray(attrs));
    }

    private class Printer extends Thread {
        final Doc                      doc;
        final PrintRequestAttributeSet attributes;
        int                            jobId;

        Printer(final Doc doc, final PrintRequestAttributeSet attributes) {
            super(WinPrintService.DEFAULT_JOB_NAME.getValue());
            this.doc = doc;
            this.attributes = attributes;
        }
        
        public void run() {
            try {
                print();
            } catch (final PrintException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void print() throws PrintException {
            final DocFlavor flavor = doc.getDocFlavor();
            final DevmodeStructWrapper dm = service.getPrinterProps();

            dm.setAttributes(attributes);
            dm.setAttributes(doc.getAttributes());
            notifyAttrListeners(dm.getAttributes(new HashAttributeSet()));

            try {
                if (DocFlavor.SERVICE_FORMATTED.PRINTABLE.equals(flavor)) {
                    printPrintable(doc, attributes);
                } else if (DocFlavor.SERVICE_FORMATTED.PAGEABLE.equals(flavor)) {
                    printPageable(doc, attributes);
                } else if (DocFlavor.URL.JPEG.equals(flavor)
                                || DocFlavor.URL.GIF.equals(flavor)
                                || DocFlavor.URL.PNG.equals(flavor)) {
                    printImage(ImageIO.read(castDoc(doc, URL.class)), doc,
                                    attributes);
                } else if (DocFlavor.INPUT_STREAM.JPEG.equals(flavor)
                                || DocFlavor.INPUT_STREAM.GIF.equals(flavor)
                                || DocFlavor.INPUT_STREAM.PNG.equals(flavor)) {
                    printImage(ImageIO.read(doc.getStreamForBytes()), doc,
                                    attributes);
                } else if (DocFlavor.BYTE_ARRAY.JPEG.equals(flavor)
                                || DocFlavor.BYTE_ARRAY.GIF.equals(flavor)
                                || DocFlavor.BYTE_ARRAY.PNG.equals(flavor)) {
                    printImage(ImageIO.read(doc.getStreamForBytes()), doc,
                                    attributes);
                } else {
                    throw new PrintException("Doc flavor is not supported"); //$NON-NLS-1$
                }

                notifyJobListeners(PrintJobEvent.DATA_TRANSFER_COMPLETE);
                notifyJobListeners(PrintJobEvent.JOB_COMPLETE);
            } catch (final PrintException ex) {
                throw ex;
            } catch (final Exception ex) {
                synchronized (this) {
                    if (jobId != -1) {
                        notifyJobListeners(PrintJobEvent.JOB_FAILED);
                        throw new PrintException(ex);
                    }
                }
            } finally {
                synchronized (lock) {
                    printer = null;
                }
            }
        }

        synchronized void cancelJob() throws PrintException {
            if (jobId > 0) {
                WinPrinterFactory.cancelPrinterJob(service.getPrinterHandle(),
                                jobId);
            }

            jobId = -1;
            notifyJobListeners(PrintJobEvent.JOB_CANCELED);
        }

        private synchronized void startJob(final long pdc,
                        final String docName, final String filePath)
                        throws PrintException {
            if (jobId == -1) {
                throw new PrintException("Job has been canceled"); //$NON-NLS-1$
            }

            jobId = WinPrinterFactory.startDoc(pdc, docName, filePath);
        }

        private synchronized void endJob(final long pdc) throws PrintException {
            if (jobId <= 0) {
                throw new PrintException("Job is not started");//$NON-NLS-1$ 
            }
            WinPrinterFactory.endDoc(pdc);
        }

        private void printPrintable(final Doc doc,
                        final PrintRequestAttributeSet attributes)
                        throws PrintException {
            final long pdc = WinPrinterFactory.getPrinterDC(
                            service.printerName, service.getPrinterProps()
                                            .getStructPtr());
            final AttributeSet docAttrs = doc.getAttributes();
            final Printable printable = castDoc(doc, Printable.class);
            final PageFormat format = getPageFormat(docAttrs, attributes);
            final PageRanges ranges = getAttribute(PageRanges.class, docAttrs,
                            attributes);
            int res = Printable.PAGE_EXISTS;

            try {
                startJob(pdc, getDocName(printable, docAttrs, attributes),
                                getDestinationPath(attributes));

                for (int i = 0; res == Printable.PAGE_EXISTS; i++) {
                    if ((ranges != null) && !ranges.contains(i)) {
                        continue;
                    }

                    res = printPrintable(printable, pdc, format, i);
                }

                endJob(pdc);
            } finally {
                WinPrinterFactory.releasePrinterDC(pdc);
            }
        }

        private void printPageable(final Doc doc,
                        final PrintRequestAttributeSet attributes)
                        throws PrintException {
            final Pageable pageable = castDoc(doc, Pageable.class);
            final PageFormat defaultFormat = getPageFormat(doc.getAttributes(),
                            attributes);
            final long pdc = WinPrinterFactory.getPrinterDC(
                            service.printerName, service.getPrinterProps()
                                            .getStructPtr());
            final AttributeSet docAttrs = doc.getAttributes();
            int pages = pageable.getNumberOfPages();
            final PageRanges ranges = getAttribute(PageRanges.class, docAttrs,
                            attributes);

            if (pages == Pageable.UNKNOWN_NUMBER_OF_PAGES) {
                pages = Integer.MAX_VALUE;
            }

            try {
                startJob(pdc, getDocName(pageable, docAttrs, attributes),
                                getDestinationPath(attributes));

                for (int i = 0; i < pages; i++) {
                    if ((ranges != null) && !ranges.contains(i)) {
                        continue;
                    }

                    final Printable printable = pageable.getPrintable(i);
                    final PageFormat format = null;

                    if (printable == null) {
                        throw new PrintException("No such page: " + i); //$NON-NLS-1$
                    }

                    if (printPrintable(printable, pdc, format != null ? format
                                    : defaultFormat, i) == Printable.NO_SUCH_PAGE) {
                        break;
                    }
                }

                endJob(pdc);
            } finally {
                WinPrinterFactory.releasePrinterDC(pdc);
            }
        }

        private void printImage(final Image img, final Doc doc,
                        final PrintRequestAttributeSet attributes)
                        throws PrintException {
            final PageFormat format = getPageFormat(attributes);
            final long pdc = WinPrinterFactory.getPrinterDC(
                            service.printerName, service.getPrinterProps()
                                            .getStructPtr());
            final double xRes = WinPrinterFactory.getPixelsPerInchX(pdc) / 72;
            final double yRes = WinPrinterFactory.getPixelsPerInchY(pdc) / 72;
            final Graphics2D g2d = new WinGDIPGraphics2D(pdc, (char) 2,
                            (int) (format.getWidth() * xRes), (int) (format
                                            .getHeight() * yRes));

            try {
                startJob(pdc, getDocName(img, attributes),
                                getDestinationPath(attributes));
                WinPrinterFactory.startPage(pdc);
                g2d.drawImage(img, (int) (format.getImageableX() * xRes),
                                (int) (format.getImageableY() * yRes),
                                (int) (format.getImageableWidth() * xRes),
                                (int) (format.getImageableHeight() * yRes),
                                Color.WHITE, null);
                WinPrinterFactory.endPage(pdc);
                endJob(pdc);
            } finally {
                WinPrinterFactory.releasePrinterDC(pdc);
            }
        }

        private int printPrintable(final Printable p, final long pdc,
                        final PageFormat format, final int pageIndex)
                        throws PrintException {
            int result = Printable.PAGE_EXISTS;

            try {
                // Before drawing on printer's device context trying to draw on
                // a dummy graphics to ensure that the page exists.
                result = p.print(new DummyGraphics2D((int) format.getWidth(),
                                (int) format.getHeight()), format, pageIndex);
            } catch (final Exception ex) {
                // ignore
            }

            if (result == Printable.PAGE_EXISTS) {
                try {
                    WinPrinterFactory.startPage(pdc);
                    result = p.print(getGraphics(pdc, format), format,
                                    pageIndex);
                    WinPrinterFactory.endPage(pdc);
                } catch (final PrinterException ex) {
                    throw new PrintException(ex);
                }
            }

            return result;
        }

        private <T extends Attribute> T getAttribute(final Class<T> c,
                        final AttributeSet... attrSets) {
            for (AttributeSet attrs : attrSets) {
                if (attrs != null) {
                    for (Attribute attr : attrs.toArray()) {
                        if (c.equals(attr.getCategory())) {
                            return c.cast(attr);
                        }
                    }
                }
            }

            return null;
        }

        private String getDocName(final Object doc,
                        final AttributeSet... attrSets) {
            Attribute name = getAttribute(DocumentName.class, attrSets);

            if (name == null) {
                name = getAttribute(JobName.class, attrSets);
            }

            if ((name == null) && (doc instanceof Component)) {
                Component c = (Component) doc;

                while (c != null) {
                    if (c instanceof Frame) {
                        if (((Frame) c).getTitle().length() > 0) {
                            return ((Frame) c).getTitle();
                        }
                    }
                    c = c.getParent();
                }
            }

            return name != null ? ((TextSyntax) name).getValue()
                            : WinPrintService.DEFAULT_JOB_NAME.getValue();
        }

        private String getDestinationPath(final PrintRequestAttributeSet attrs)
                        throws PrintException {
            if (attrs != null) {
                final Destination dest = (Destination) attrs
                                .get(Destination.class);
                return dest != null ? new File(dest.getURI()).getAbsolutePath()
                                : null;
            }
            return null;
        }

        private <T> T castDoc(final Doc doc, final Class<T> c)
                        throws PrintException {
            try {
                return c.cast(doc.getPrintData());
            } catch (final IOException ex) {
                throw new PrintException(ex);
            }
        }

        private Graphics2D getGraphics(final long pdc, final PageFormat format)
                        throws PrintException {
            final Graphics2D g2d = new WinGDIPGraphics2D(pdc, (char) 3,
                            (int) format.getWidth(), (int) format.getHeight());

            g2d.setColor(Color.BLACK);
            g2d.setBackground(Color.WHITE);
            g2d.setClip((int) format.getImageableX(), (int) format
                            .getImageableY(), (int) format.getImageableWidth(),
                            (int) format.getImageableHeight());

            return g2d;
        }

        private PageFormat getPageFormat(final AttributeSet... attrSets) {
            final Paper paper = new Paper();
            final PageFormat format = new PageFormat();
            final DevmodeStructWrapper dm = service.getPrinterProps();
            final OrientationRequested o = dm.getOrientation();
            final MediaPrintableArea area = getAttribute(
                            MediaPrintableArea.class, attrSets);
            DevmodeStructWrapper.Paper p = dm.getPaper();

            if (p == null) {
                p = (DevmodeStructWrapper.Paper) service
                                .getDefaultAttributeValue(DevmodeStructWrapper.Paper.class);
                dm.setPaper(p);
            }

            paper.setSize(p.getSize().getX(Size2DSyntax.INCH) * 72.0, p
                            .getSize().getY(Size2DSyntax.INCH) * 72.0);
            format.setPaper(paper);

            if (OrientationRequested.LANDSCAPE.equals(o)
                            || OrientationRequested.REVERSE_LANDSCAPE.equals(o)) {
                format.setOrientation(PageFormat.LANDSCAPE);
            } else {
                format.setOrientation(PageFormat.PORTRAIT);
            }

            if (area != null) {
                paper.setImageableArea(area.getX(MediaPrintableArea.INCH) * 72,
                                area.getY(MediaPrintableArea.INCH) * 72,
                                area.getWidth(MediaPrintableArea.INCH) * 72,
                                area.getHeight(MediaPrintableArea.INCH) * 72);
            } else {
                final double x = paper.getWidth() / 10;
                final double y = paper.getHeight() / 10;

                paper.setImageableArea(x, y, (paper.getWidth() - 2 * x), (paper
                                .getHeight() - 2 * y));
            }

            return format;
        }
    }

    private static class DummyGraphics2D extends CommonGraphics2D {
        DummyGraphics2D(final int width, final int height) {
            setClip(new Rectangle(width, height));
        }

        public void drawGlyphVector(GlyphVector g, float x, float y) {
        }

        public void drawString(String s, float x, float y) {
        }

        public GraphicsConfiguration getDeviceConfiguration() {
            return null;
        }

        public void copyArea(int sx, int sy, int width, int height, int dx,
                        int dy) {
        }

        public Graphics create() {
            return this;
        }
    }
}
