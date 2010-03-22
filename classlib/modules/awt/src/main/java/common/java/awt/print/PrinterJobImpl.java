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

package java.awt.print;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;

import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.SimpleDoc;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.RequestingUserName;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.x.print.ServiceUIDialog;

class PrinterJobImpl extends PrinterJob {

    private final PrintRequestAttributeSet attributes;

    private Doc                            doc;
    private PageFormat                     format;
    private DocPrintJob                    printJob;
    private PrintService                   printService;
    private boolean                        isCanceled;

    PrinterJobImpl() {
        attributes = new HashPrintRequestAttributeSet();
    }

    @Override
    public PrintService getPrintService() {
        if (printService == null) {
            printService = PrintServiceLookup.lookupDefaultPrintService();
            printService = isServiceSupported(printService) ? printService
                            : null;
        }
        return printService;
    }

    @Override
    public void setPrintService(final PrintService printservice)
                    throws PrinterException {
        if (!isServiceSupported(printservice)) {
            // DocFlavor.SERVICE_FORMATTED is not supported
            throw new PrinterException(Messages.getString(
                            "awt.5D", "DocFlavor.SERVICE_FORMATTED")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        this.printService = printservice;
    }

    @Override
    public void cancel() {
        if (printJob instanceof CancelablePrintJob) {
            try {
                ((CancelablePrintJob) printJob).cancel();
                isCanceled = true;
            } catch (PrintException e) {
                // ignore
            }
        }
    }

    @Override
    public PageFormat defaultPage(final PageFormat page) {
        if (page != null) {
            format = (PageFormat) page.clone();
            attributes.addAll(formatToAttrs(format));
        }

        return format;
    }

    @Override
    public int getCopies() {
        final Copies c = (Copies) attributes.get(Copies.class);
        return c != null ? c.getValue() : 1;
    }

    @Override
    public String getJobName() {
        final JobName name = (JobName) attributes.get(JobName.class);
        return name != null ? name.getValue() : null;
    }

    @Override
    public String getUserName() {
        final RequestingUserName name = (RequestingUserName) attributes
                        .get(RequestingUserName.class);
        return name != null ? name.getValue() : null;
    }

    @Override
    public boolean isCancelled() {
        return isCanceled;
    }

    @Override
    public PageFormat pageDialog(final PrintRequestAttributeSet attributes)
                    throws HeadlessException {
        checkHeadless();

        final Window wnd = KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getActiveWindow();
        final Window owner = (((wnd instanceof Dialog) || (wnd instanceof Frame))
                        ? wnd : new Frame());
        final Rectangle screen = GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration().getBounds();
        final ServiceUIDialog dialog = new ServiceUIDialog(null,
                        screen.width / 3, screen.height / 3, getPrintService(),
                        attributes, owner);

        dialog.show();

        if (owner != wnd) {
            owner.dispose();
        }

        if (attributes != dialog.getAttributes()) {
            attributes.addAll(dialog.getAttributes());
        }

        return (dialog.getResult() == ServiceUIDialog.APPROVE_PRINT)
                        ? attrsToFormat(attributes) : null;
    }

    @Override
    public PageFormat pageDialog(final PageFormat page)
                    throws HeadlessException {
        final PageFormat format = pageDialog(formatToAttrs(page));
        return format != null ? format : page;
    }

    @Override
    public void print() throws PrinterException {
        print(attributes);
    }

    @Override
    public void print(final PrintRequestAttributeSet attributes)
                    throws PrinterException {
        if (doc == null) {
            // Neither Printable nor Pageable specified
            throw new PrinterException(Messages.getString("awt.29A")); //$NON-NLS-1$
        }

        final PrintRequestAttributeSet attrs = mergeAttrs(attributes);
        final PrintService service = getPrintService();

        if (service == null) {
            // Printer not found
            throw new PrinterException(Messages.getString("awt.29A")); //$NON-NLS-1$
        }

        try {
            printJob = service.createPrintJob();
            printJob.print(doc, attrs);
        } catch (final PrintException ex) {
            final PrinterException pe = new PrinterException();
            pe.initCause(ex);
            throw pe;
        }
    }

    @Override
    public boolean printDialog() throws HeadlessException {
        return printDialog(attributes);
    }

    @Override
    public boolean printDialog(final PrintRequestAttributeSet attributes)
                    throws HeadlessException {
        checkHeadless();

        if (attributes == null) {
            throw new NullPointerException();
        }

        final PrintRequestAttributeSet attrs = mergeAttrs(attributes);
        final Rectangle screen = GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration().getBounds();
        final PrintService newService = ServiceUI.printDialog(null,
                        screen.width / 3, screen.height / 3,
                        lookupPrintServices(), getPrintService(),
                        DocFlavor.SERVICE_FORMATTED.PRINTABLE, attrs);

        if (newService != null) {
            printService = newService;
        }

        return newService != null;
    }

    @Override
    public void setCopies(final int copies) {
        attributes.add(new Copies(copies));
    }

    @Override
    public void setJobName(final String jobName) {
        attributes.add(new JobName(jobName, null));
    }

    @Override
    public void setPageable(final Pageable document)
                    throws NullPointerException {
        doc = new SimpleDoc(document, DocFlavor.SERVICE_FORMATTED.PAGEABLE,
                        null);
    }

    @Override
    public void setPrintable(final Printable painter) {
        setPrintable(painter, null);
    }

    @Override
    public void setPrintable(final Printable painter, final PageFormat format) {
        if (format != null) {
            doc = new SimpleDoc(new PrintableWrapper(painter, format),
                            DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        } else {
            doc = new SimpleDoc(new PrintableWrapper(painter, this.format),
                            DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        }
    }

    @Override
    public PageFormat validatePage(final PageFormat page) {
        return page;
    }

    private static void checkHeadless() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    private static boolean isServiceSupported(final PrintService service) {
        return (service != null) && service.isDocFlavorSupported(
                        DocFlavor.SERVICE_FORMATTED.PAGEABLE);
    }

    private static PrintRequestAttributeSet formatToAttrs(
                    final PageFormat format) {
        final PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();

        if (format != null) {
            attributes.add(new MediaPrintableArea((float) (format
                            .getImageableX() / 72.0), (float) (format
                            .getImageableY() / 72.0), (float) (format
                            .getWidth() / 72.0),
                            (float) (format.getHeight() / 72.0),
                            Size2DSyntax.INCH));

            switch (format.getOrientation()) {
            case PageFormat.PORTRAIT:
                attributes.add(OrientationRequested.PORTRAIT);
                break;
            case PageFormat.LANDSCAPE:
                attributes.add(OrientationRequested.LANDSCAPE);
                break;
            case PageFormat.REVERSE_LANDSCAPE:
                attributes.add(OrientationRequested.REVERSE_LANDSCAPE);
                break;
            }
        }

        return attributes;
    }

    private static PageFormat attrsToFormat(
                    final PrintRequestAttributeSet attributes) {
        if (attributes == null) {
            return new PageFormat();
        }

        final PageFormat format = new PageFormat();
        final Paper paper = new Paper();
        final OrientationRequested orient = (OrientationRequested) attributes
                        .get(OrientationRequested.class);
        final MediaSize size = attributes.containsKey(Media.class) ? MediaSize
                        .getMediaSizeForName((MediaSizeName) attributes
                                        .get(Media.class))
                        : (MediaSize) attributes.get(MediaSize.class);
        final MediaPrintableArea area = (MediaPrintableArea) attributes
                        .get(MediaPrintableArea.class);

        if (orient != null) {
            if (orient.equals(OrientationRequested.LANDSCAPE)) {
                format.setOrientation(PageFormat.LANDSCAPE);
            } else if (orient.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
                format.setOrientation(PageFormat.REVERSE_LANDSCAPE);
            }
        }

        if (size != null) {
            paper.setSize(size.getX(Size2DSyntax.INCH) * 72.0, size
                            .getY(Size2DSyntax.INCH) * 72.0);
        }

        if (area != null) {
            paper.setImageableArea(area.getX(Size2DSyntax.INCH) * 72.0, area
                            .getY(Size2DSyntax.INCH) * 72.0, area
                            .getWidth(Size2DSyntax.INCH) * 72.0, area
                            .getHeight(Size2DSyntax.INCH) * 72.0);
        }

        format.setPaper(paper);

        return format;
    }

    private PrintRequestAttributeSet mergeAttrs(
                    final PrintRequestAttributeSet dest) {
        if (dest == attributes) {
            return attributes;
        }

        final PrintRequestAttributeSet attrs = (dest != null) ? dest
                        : new HashPrintRequestAttributeSet();

        for (Attribute attr : attributes.toArray()) {
            if ((attr != null) && !attrs.containsKey(attr.getCategory())) {
                attrs.add(attr);
            }
        }
        return attrs;
    }

    private static class PrintableWrapper implements Pageable {
        private final Printable  p;
        private final PageFormat format;

        PrintableWrapper(final Printable p, final PageFormat format) {
            this.p = p;
            this.format = (format != null) ? format : new PageFormat();
        }

        public int getNumberOfPages() {
            return Pageable.UNKNOWN_NUMBER_OF_PAGES;
        }

        public PageFormat getPageFormat(int pageIndex)
                        throws IndexOutOfBoundsException {
            return format;
        }

        public Printable getPrintable(int pageIndex)
                        throws IndexOutOfBoundsException {
            return p;
        }
    }
}
