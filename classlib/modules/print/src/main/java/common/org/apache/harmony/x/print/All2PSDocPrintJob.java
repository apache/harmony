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
/** 
 * @author Aleksei V. Ivaschenko 
 */ 
package org.apache.harmony.x.print;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.StreamPrintService;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobListener;

/*
 * Image2PSDocPrintJob
  */
public class All2PSDocPrintJob implements DocPrintJob {
    private PrintService begetPrintService;
    private HashPrintJobAttributeSet printJobAttributeSet;
    private ArrayList printJobListeners;
    private ArrayList printJobAttributeListeners; 
    private boolean action = false;
    private PrintStream outstream;
    /*
    private PrinterJob printerJob;
    private MediaSize mediaSize;
    */
    private String jobName;
    private int copies;
        
    private final static int BANK_MAX_BYTES = 32768;
   
    /*
     * static method to read images 
     */
    public static Image readImage(InputStream source) 
                                            throws PrintException {
        ArrayList banks = new ArrayList();
        ArrayList bankLengths = new ArrayList();
        Image image = null;  
        Toolkit toolkit;
                
        int bytesRead = 0;
        int nBanks = 0;
        int totalSize = 0;
        byte[] byteImage;               
        byte[] buffer; 
                        
        try {
            do {             
                buffer = new byte[BANK_MAX_BYTES];        
                bytesRead = source.read(buffer);
                if (bytesRead > 0) {
                    banks.add(buffer);
                    bankLengths.add(new Integer(bytesRead));
                    totalSize += bytesRead;
                }              
            } while (bytesRead >= 0);
            source.close();
            nBanks = banks.size();
            byteImage = new byte[totalSize];
            int k=0;
            for (int i = 0; i < nBanks; i++) {
                buffer = (byte[])banks.get(i);
                int bufferLength = ((Integer)bankLengths.get(i)).intValue();
                for (int j = 0; j < bufferLength; j++) {
                    byteImage[k++] = buffer[j];
                }
            }                
        } catch (IOException ioe) {
            throw new PrintException("Can't read print data.");
        }
        
        toolkit = Toolkit.getDefaultToolkit();

        image = toolkit.createImage(byteImage);
        while (!toolkit.prepareImage(image, -1, -1, null) &&
               (toolkit.checkImage(image, -1, -1, null) &
                       (ImageObserver.ERROR | ImageObserver.ABORT)) == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                // Interrupted by user.
                return (BufferedImage) null;
            }
        }
        if (!toolkit.prepareImage(image, -1, -1, null)) {
            throw new PrintException("Error while loading image (possibly, " +
                    "image format is not supported).");
        }
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = bufferedImage.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        return bufferedImage;
    }
    
    protected All2PSDocPrintJob(StreamPrintService printService) {
        super();
        begetPrintService = printService;
        printJobListeners = new ArrayList();
        printJobAttributeListeners = new ArrayList();
        printJobAttributeSet = new HashPrintJobAttributeSet();
        jobName = "PS printing";
        copies = 1;
        outstream = new PrintStream(printService.getOutputStream());
    }

    /* 
     * Determines the PrintService object 
     * to which this print job object is bound.
     * It's  private field begetPrintService;
     */
    public PrintService getPrintService() {   
        return begetPrintService;
    }

    /* 
     *   Returns the print job attributes. 
     */
    public PrintJobAttributeSet getAttributes() {        
        return printJobAttributeSet;
    }

    /*
     * Registers a listener for event occurring during this print job. 
     */
    public void addPrintJobListener(PrintJobListener listener) {
        if (listener != null){
            if (! printJobListeners.contains(listener)){
                printJobListeners.add(listener);
            }
        }
    }
    
    /*
     * Registers a listener for changes in the specified attributes
     */
    public void addPrintJobAttributeListener(
            PrintJobAttributeListener listener,
            PrintJobAttributeSet attributes) {
        
            if (listener != null){
                printJobAttributeListeners.add(listener);
            }
            printJobAttributeSet.addAll(attributes);
    }      
    
    
    public void removePrintJobAttributeListener(
            PrintJobAttributeListener listener) {
            printJobAttributeListeners.remove(listener);
    }

  
    public void removePrintJobListener(PrintJobListener listener) {
        printJobListeners.remove(listener);
    }
    
  
    public void print(Doc doc, PrintRequestAttributeSet attributes)
            throws PrintException {
        
        Object data;
        DocFlavor docflavor;
        String docflavorClassName;
        Image image = null;
        int x = 0;
        int y = 0;
        int width;
        int height;
        int iWidth;
        int iHeight;        
        int newWidth;
        int newHeight;
        float scaleX;
        float scaleY; 
        
        synchronized (this) {
            if (action) {
                throw new PrintException("printing is in action");
            }
            action = true;
        }

        try { // for finally block. To make action false.

            docflavor = doc.getDocFlavor();
            try {
                data = doc.getPrintData();
            } catch (IOException ioexception) {
                throw new PrintException("no data for print: "
                        + ioexception.toString());
            }            
            if (docflavor == null) {
                throw new PrintException("flavor is null");
            }
            if (!begetPrintService.isDocFlavorSupported(docflavor)) {
                throw new PrintException("invalid flavor :"
                        + docflavor.toString());
            }

            docflavorClassName = docflavor.getRepresentationClassName();

            if (docflavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
                docflavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
                docflavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
                docflavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
                docflavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
                docflavor.equals(DocFlavor.BYTE_ARRAY.PNG)) {                
                try {
                    image = readImage(doc.getStreamForBytes());
                } catch (IOException ioe) {
                    throw new PrintException(ioe);
                }
            } else if (docflavor.equals(DocFlavor.URL.GIF) ||
                       docflavor.equals(DocFlavor.URL.JPEG) ||
                       docflavor.equals(DocFlavor.URL.PNG)) {
                URL url = (URL) data;
                try {
                    image = readImage(url.openStream());
                } catch (IOException ioe) {
                    throw new PrintException(ioe);
                }
            } else if (docflavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)){
                Printable printable = (Printable)data;
                print(printable, null);
            } else if (docflavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE)) {
                Pageable pageable = (Pageable)data;
                print(null, pageable);
            } else {
                throw new PrintException("Wrong DocFlavor class: "
                        + docflavorClassName);
            }

            if (image != null) {
                final PageFormat format = new PageFormat();
                final Paper p = format.getPaper();
                
                MediaSize size = null;
                if (attributes != null) {
                    if (attributes.containsKey(MediaSize.class)) {
                        size = (MediaSize) attributes.get(MediaSize.class);
                    } else if (attributes.containsKey(MediaSizeName.class)) {
                        MediaSizeName name = (MediaSizeName) attributes
                                .get(MediaSizeName.class);
                        size = MediaSize.getMediaSizeForName(name);
                    } else {
                        size = MediaSize
                                .getMediaSizeForName(MediaSizeName.ISO_A4);
                    }
                } else {
                    size = MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
                }
                width = (int) (size.getX(MediaSize.INCH) * 72.0);
                height =(int) (size.getY(MediaSize.INCH) * 72.0);
                if (attributes != null) {
                    if (attributes.containsValue(
                            OrientationRequested.LANDSCAPE)) {
                        int temp = width;
                        width = height;
                        height = temp;
                    }
                }
                iWidth = image.getWidth(null);
                iHeight = image.getHeight(null);
                x = (width - iWidth) / 2;
                y = (height - iHeight) / 2;
                p.setSize(width, height);
                p.setImageableArea(x, y, iWidth, iHeight);
                

                Graphics2D2PS graphics = new Graphics2D2PS(outstream, format);
                graphics.startPage(1);
                if (x < 0 || y < 0) {
                    scaleX = (float) image.getWidth(null) / (float) width;
                    scaleY = (float) image.getHeight(null) / (float) height;
                    newWidth = width;
                    newHeight = height;
                    
                    if (scaleX > scaleY) {
                        newWidth = (int) ((float) iWidth / scaleX);
                        newHeight = (int) ((float) iHeight / scaleX);
                        x = 0;
                        y = (height - newHeight) / 2;
                    } else {
                        newWidth = (int) ((float) iWidth / scaleY);
                        newHeight = (int) ((float) iHeight / scaleY);
                        y = 0;
                        x = (width - newWidth) / 2;
                    }
                    graphics.drawImage(image, x, y, newWidth, newHeight, null);
                } else {
                    graphics.drawImage(image, x, y, null);
                }
                graphics.endOfPage(1);
                graphics.finish();
            }

        } finally {
            synchronized (this) {
                action = false;
            }
        }
    }

    private void print(Printable psPrintable, Pageable psDocument)
            throws PrintException {
        PageFormat format = null;
        Graphics2D2PS converter = null;
        
        if (psPrintable == null && psDocument == null)  {
            return;
        }
        
        if (psDocument == null){
            converter = new Graphics2D2PS(outstream);
            format = null;            
        } else {
            format = psDocument.getPageFormat(0);
            converter = new Graphics2D2PS(outstream, format);               
        }
        
        Graphics2D2PS fake = new Graphics2D2PS(new PrintStream(
                new OutputStream() {
                    public void write(int b) {
                        // Do nothing.
                    }
                }));
        
        int iPage = 0;
        int result = -1;
        int pages = -1;
        if (psDocument != null) {
            pages = psDocument.getNumberOfPages();
        }
        do {
            try {
                Printable page = null;
                PageFormat pageFormat = null;
                result = -1;
                if (psPrintable != null) {
                    page = psPrintable;
                    pageFormat = format;
                    result = psPrintable.print(fake, format, iPage);
                } else {
                    if (pages != Pageable.UNKNOWN_NUMBER_OF_PAGES &&
                        iPage >= pages) {
                        break;
                    }
                    page = psDocument.getPrintable(iPage);
                    pageFormat = psDocument.getPageFormat(iPage);
                    if (page != null) {
                        result = page.print(fake, pageFormat, iPage);
                    } else {
                        throw new PrinterException("No printable for page " +
                                iPage + " in given document.");
                    }
                }
                if (result == Printable.PAGE_EXISTS) {
                    converter.startPage(iPage + 1);
                    result = page.print(converter, pageFormat, iPage);
                    converter.endOfPage(iPage + 1);
                }
            } catch (PrinterException pe) {
                converter.finish();
                throw new PrintException(pe.getMessage());
            }
            iPage++;            
        } while (result == Printable.PAGE_EXISTS);
        converter.finish();
    }
}
