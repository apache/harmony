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

package org.apache.harmony.x.print.awt;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.SimpleDoc;
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
import javax.swing.JOptionPane;

import org.apache.harmony.x.print.ServiceUIDialog;
import org.apache.harmony.x.print.attributes.MediaMargins;


public class PSPrinterJob extends PrinterJob {

    private Printable psPrintable;
    private Pageable psDocument;
    private PageFormat psFormat;
    private PrintStream stream;
    
    PrintRequestAttributeSet attrs;             // Job attributes
    private PrintService service = null;        // Job print service
    
    static String os = null;                    // OS type
  
    static {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                os = System.getProperty("os.name");
                if (os.startsWith("Windows")) {
                    System.loadLibrary("print");
                }
                return null;
            }
        });
    }
       
    public PSPrinterJob() {
        super();
        attrs = new HashPrintRequestAttributeSet();
        service = PrintServiceLookup.lookupDefaultPrintService();
        /* Probably need to add default attributes to attrs here */
    }

    public void setPrintable(Printable painter) {
        psPrintable = painter;
    }

    /*
     * @see java.awt.print.PrinterJob#setPrintable(Printable, PageFormat)
     */
    public void setPrintable(Printable painter, PageFormat format) {
        psPrintable = painter;
        psFormat = format;
    }

    /*
     * @see java.awt.print.PrinterJob#setPageable(Pageable)
     */
    public void setPageable(Pageable document) throws NullPointerException {       
        if (document == null){
            throw new NullPointerException("Pageable argument is null");
        }        
        psDocument = document; 
    }

    /*
     * @see java.awt.print.PrinterJob#print()
     */
    public void print() throws PrinterException {
        Doc doc = null;
        if (psPrintable != null) {
            Pageable pageable = new Pageable() {
                public int getNumberOfPages() {
                    return UNKNOWN_NUMBER_OF_PAGES;
                }
                public PageFormat getPageFormat(int pageIndex)
                        throws IndexOutOfBoundsException {
                    return (psFormat != null) ? psFormat : defaultPage();
                }
                public Printable getPrintable(int pageIndex)
                        throws IndexOutOfBoundsException {
                    return psPrintable;
                }
            };
            
            doc = new SimpleDoc(pageable,
                    DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        } else if (psDocument != null) {
            doc = new SimpleDoc(psDocument,
                    DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        } else {
            throw new PrinterException("Neither Printable nor Pageable were " +
                    "specified.");
        }
        
        PrintService pService = service;
        if (pService == null) {
            pService = PrintServiceLookup.lookupDefaultPrintService();
        }
        
        try {
            DocPrintJob job = pService.createPrintJob();
            job.print(doc, attrs);
        } catch (PrintException pe) {
            throw new PrinterException(pe.getMessage());
        }
    }
    
    /*
     * @see java.awt.print.PrinterJob#print(PrintRequestAttributeSet)
     */
    public void print(PrintRequestAttributeSet attributes)
            throws PrinterException {
        attrs = attributes;
        print();
    }
        
    public PrintService getPrintService() {
        return service;
    }

    public void setPrintService(PrintService printservice) 
            throws PrinterException {
        if (printservice.isDocFlavorSupported(
                DocFlavor.SERVICE_FORMATTED.PRINTABLE) &&
            printservice.isDocFlavorSupported(
                DocFlavor.SERVICE_FORMATTED.PAGEABLE)) {
            service = printservice;
        } else {
            throw new PrinterException("PrintService doesn't support " +
                    "SERVICE_FORMATTED doc flavors.");
        }
    }

    public void setJobName(String jobName) {
        attrs.add(new JobName(jobName, Locale.getDefault()));
    }

    public void setCopies(int copies) {
        attrs.add(new Copies(copies));
    }

    public int getCopies() {
        return attrs.containsKey(Copies.class) 
               ? ((Copies) (attrs.get(Copies.class))).getValue()
               : 1; 
    }

    public String getUserName() {
        return attrs.containsKey(RequestingUserName.class) 
               ? ((RequestingUserName)(attrs.get(RequestingUserName.class))).
                        getValue()
               : null;
    }

    public String getJobName() {       
        return attrs.containsKey(JobName.class)  
               ? ((JobName)(attrs.get(JobName.class))).getValue()
               : null;        
    }

    /*
     * This method shows Windows native print dialog on Windows and 
     * javax.print.ServiceUI standard print dialog on Linux. We suppose that on
     * Linux this dialog should contain the list of native print services only,
     * however corresponding native agent function is not realized yet.
     * 
     * Throws HeadlessException if Graphics Environment is headless.
    */
    public boolean printDialog() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        if (os.startsWith("Windows")) {         /* Windows OS */
            
            /* call Windows native dialog */
            String res = getPrinter(service.getName(), getCopies());
            
            if (res != null) {
                try {
                    setPrintService(findPrintService(res));
                    return true;
                } catch(PrinterException e) {
                    JOptionPane.showMessageDialog(KeyboardFocusManager
                         .getCurrentKeyboardFocusManager().getActiveWindow(),
                         "Can not set selected printer!", 
                         "Incorrect service",
                         JOptionPane.ERROR_MESSAGE);
                }
            }
            return false;
        } 
        /* Linux OS */
        /*
         * TODO: Need to create new native agent function which returns
         * a list of native printers. Need to call javax.print.ServiceUI
         * print dialog with this native printers list instead of all 
         * registered print services list as it is realized here! 
         */
        printDialog(attrs);
        return false;        
    }

    /*
     * Calls cross-platforms print dialog with all registered print services
     * (this function just calls ServiceUI.printDialog(...) method with 
     * corresponding parameters).
     * 
     * Parameters:
     *      attrs - attributes for the dialog
     * 
     * Throws:
     *      NullPointerException - if attrs is null
     *      HeadlessException - if Graphics Environment is headless
     */
    public boolean printDialog(PrintRequestAttributeSet dialogAttrs)
            throws HeadlessException {

        if (dialogAttrs == null) {
            throw new NullPointerException();
        } else if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        /* Combine this PrinterJob attrs attribute set and printerAttrs set
           and resolve MediaPrintableArea/MediaMargins conflict if it is 
           needed */
        PrintRequestAttributeSet sum = 
                updateMediaAndMarginsIfNeeded(dialogAttrs);

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration().getBounds();

        /* call cross-platform print dialog */
        PrintService newSrv = ServiceUI.printDialog(
                null, 
                screen.width / 3, 
                screen.height / 3, 
                lookupServicesForDialog(), 
                service,
                DocFlavor.SERVICE_FORMATTED.PRINTABLE, 
                sum);

        if (newSrv!=null) {
            /* Set selected print service and update attrs attribute set 
               if user clicked "Print" button */
            try {
                setPrintService(newSrv);
                this.attrs.clear();
                this.attrs.addAll(sum);
                return true;
            } catch (PrinterException e) {
                System.out.println(e);
                return false;
            }
        }
        return false;   /* "Cancel button was pressed */
    }

    /*
     * This method calls standard page dialog that allows PageFormat 
     * modification.
     * Parameters:
     *      page - PageFormat for modification
     * Returns:
     *      original page if the dialog is cancelled or print service for the
     *      job is not set
     *      new PageFormat object from the dialog if the user click "Print"
     *      button
     * Throws HeadlessException if Graphics Environment is headless. 
    */
    public PageFormat pageDialog(PageFormat page) 
            throws HeadlessException {
        if (getPrintService() == null) {
            return page;
        } 

        /* get attribute set which combines this job attribute set and
           attributes for the given page PageFormat */
        HashPrintRequestAttributeSet myattrs = getAttrsForPageFormat(page);
        return pageDialog(myattrs);
    }

    /*
     * This method calls standard page dialog with the given attribute set
     * Parameters:
     *      attrs - attribute set for the dialog
     * Returns:
     *      original page if the dialog is cancelled or print service for the
     *      job is not set
     *      new PageFormat object from the dialog if the user click "Print"
     *      button
     * Throws:
     *      HeadlessException if Graphics Environment is headless.
     *      NullPointerException if attrs is null
    */
    public PageFormat pageDialog(PrintRequestAttributeSet arg_attrs)
            throws HeadlessException {

        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        } else if (arg_attrs == null) {
            throw new NullPointerException();
        } else if (getPrintService() == null) {
            JOptionPane.showMessageDialog(KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getActiveWindow(),
                    "Print service is not set for the PrinterJob!", "Error!",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Window wnd = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getActiveWindow();
        Window owner = (((wnd instanceof Dialog)||(wnd instanceof Frame)) 
                ? wnd : new Frame());

        /* Combine this PrinterJob this.attrs attribute set and attrs set
            and resolve MediaPrintableArea/MediaMargins conflict if it is
            needed */
        PrintRequestAttributeSet sum = updateMediaAndMarginsIfNeeded(arg_attrs);

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();

        /* create and show the page dialog */
        ServiceUIDialog dialog = new ServiceUIDialog(null, 
                                                     screen.width/3,
                                                     screen.height/3,
                                                     getPrintService(),
                                                     sum,
                                                     owner);
        dialog.show();

        if (owner != wnd) {
            owner.dispose();
        }

        /* update this.attrs attribute set and result page format to return */
        if (dialog.getResult() == ServiceUIDialog.APPROVE_PRINT) {
            PrintRequestAttributeSet newattrs = dialog.getAttributes();

            if (!newattrs.containsKey(Media.class)) {
                this.attrs.remove(Media.class);
            }
            if (!newattrs.containsKey(OrientationRequested.class)) {
                this.attrs.remove(OrientationRequested.class);
            }
            if (!newattrs.containsKey(MediaPrintableArea.class)) {
                this.attrs.remove(MediaPrintableArea.class);
            }
            if (!newattrs.containsKey(MediaMargins.class)) {
                this.attrs.remove(MediaMargins.class);
            }
            this.attrs.addAll(newattrs);
            return getPageFormatForAttrs(newattrs);
        } 
        
        return null;
        
    }

    /* 
     * Returns this printer job's attribute set
     */
    public PrintRequestAttributeSet getAttributes() {
        return attrs;
    }

    /*
     * Native method which calls Windows java native dialog.
     * This method add/update Copies attribute into the attr attribute set
     * and set new selected printer if user clicked OK.
     * It does not change any other PrinterJob attributes now.
     * TODO: wtite new Windows native agent function which save dialog DEVMODE 
     * structure and returns correct attribute set for it. 
     */
    native String getPrinter(String defaultPrinter, int copies);

    public PageFormat defaultPage(PageFormat page) {
        attrs.addAll(getAttrsForPageFormat(page));
        return getPageFormatForAttrs(attrs);
    }

//  ---------------------------------------------------------------

    /*
     * Returns PrintRequestAttributeSet which corresponds to given page
     * PageFormat. We always adds MediaMArgins (not MediaPrintableArea)
     * attribute for the result attribute set.
     */
    protected HashPrintRequestAttributeSet 
            getAttrsForPageFormat(PageFormat page) {

        HashPrintRequestAttributeSet lattrs=new HashPrintRequestAttributeSet();

        /* Add Orientation attribute */
        switch (page.getOrientation()) {
            case PageFormat.LANDSCAPE:
                lattrs.add(OrientationRequested.LANDSCAPE);
                break;
            case PageFormat.PORTRAIT:
                lattrs.add(OrientationRequested.PORTRAIT);
                break;
            case PageFormat.REVERSE_LANDSCAPE:
                lattrs.add(OrientationRequested.REVERSE_LANDSCAPE);
                break;
        }

        /* Add Media attribute */
        MediaSizeName media = MediaSize.findMedia(
                (float) (page.getWidth() / 72.0),
                (float) (page.getHeight() / 72.0), 
                Size2DSyntax.INCH);
        if (media != null) {
            lattrs.add(media);
        }

        /* Add MediaMargins attribute */
        lattrs.add(new MediaMargins((float) (page.getImageableX() / 72.0), 
                (float) (page.getImageableY() / 72.0), 
                (float) ((page.getWidth() - page.getImageableX() -
                        page.getImageableWidth()) / 72.0),
                (float) ((page.getHeight() - page.getImageableHeight() -
                        page.getImageableY()) / 72.0), 
                MediaMargins.INCH));

        return lattrs;
    }

    /*
     * Returns PageFormat object which corresponds to the given newattrs 
     * attribute set.
     */
    protected PageFormat getPageFormatForAttrs(
            PrintRequestAttributeSet newattrs) {
        
        PageFormat pf = new PageFormat();
        
        if (newattrs.containsKey(OrientationRequested.class)) {
            OrientationRequested or = (OrientationRequested)
                    newattrs.get(OrientationRequested.class);
            pf.setOrientation(or.equals(OrientationRequested.LANDSCAPE)  
                    ? PageFormat.LANDSCAPE
                    : (or.equals(OrientationRequested.REVERSE_LANDSCAPE)  
                            ? PageFormat.REVERSE_LANDSCAPE 
                            : PageFormat.PORTRAIT));
        }

        Paper paper = new Paper();
        MediaSize size = MediaSize.getMediaSizeForName(
                newattrs.containsKey(Media.class) 
                        && (newattrs.get(Media.class).getClass().
                                isAssignableFrom(MediaSizeName.class))
                ? (MediaSizeName)newattrs.get(Media.class) 
                : MediaSizeName.ISO_A4);
        paper.setSize(size.getX(Size2DSyntax.INCH) * 72.0, 
                      size.getY(Size2DSyntax.INCH) * 72.0);


        MediaMargins mm;
        if (newattrs.containsKey(MediaMargins.class)) {
            mm = (MediaMargins) newattrs.get(MediaMargins.class);
        } else if(newattrs.containsKey(MediaPrintableArea.class)) {
            mm = new MediaMargins(size, 
                 (MediaPrintableArea) attrs.get(MediaPrintableArea.class));
        } else {
            mm = new MediaMargins(25.4F, 25.4F, 25.4F, 25.4F, MediaMargins.MM);
        }
        paper.setImageableArea(mm.getX1(MediaMargins.INCH) * 72.0, 
                mm.getY1(MediaMargins.INCH) * 72.0, 
                (size.getX(Size2DSyntax.INCH) - mm.getX1(MediaMargins.INCH) -
                mm.getX2(MediaMargins.INCH)) * 72.0,
                (size.getY(Size2DSyntax.INCH) - mm.getY1(MediaMargins.INCH) -
                mm.getY2(MediaMargins.INCH)) * 72.0 );
        pf.setPaper(paper);
        return pf;
    }

    /* 
     * Find PrintService with the given name 
    */
    protected PrintService findPrintService(String name) {
        PrintService srvs [] = 
                PrintServiceLookup.lookupPrintServices(null, null);
        if (srvs != null) {
            for (int i = 0; i < srvs.length; i++) {
                if (srvs[i].getName().equals(name)) {
                    return srvs[i];
                }
            }
        }
        return null;
    }

    /*
     * This method returns all registered PrintServices list if this list
     * contains this printer job's print service.
     * If this list does not contain this job's print service, this function
     * returns new print services list which contains all registered services
     * plus this jib's service. 
     */
    protected PrintService[] lookupServicesForDialog() {
        PrintService [] services = lookupPrintServices();
        if (services != null) {
            for (int i = 0; i < services.length; i++) {
                if (services[i].equals(service)) {
                    return services;
                }
            }
            PrintService [] ret = new PrintService [services.length + 1];
            for (int i = 0; i < services.length; i++) {
                ret[i] = services[i];
            }
            ret[services.length] = service;
            return ret;
        }
        
        return new PrintService [] {service};
        
    }

    /* 
     * This method adds newAttrs attributes to this.attrs attribute set. If
     * newAttrs contains MediaMargins or MediaPrintableAttribute, result 
     * attribute set must not contain MediaPrintableArea or MediaMargins from 
     * this.attrs attribute set because of the possible conflict.
     */
    protected PrintRequestAttributeSet updateMediaAndMarginsIfNeeded(
            PrintRequestAttributeSet newAttrs) {

        /* create copy of this.attrs*/
        PrintRequestAttributeSet sum = 
                new HashPrintRequestAttributeSet(this.attrs);

        /* remove MediaMargins and MediaPrintableArea attributes from the copy
           of job attributes if newAttrs contains  MediaMargins or 
           PrintableArea */
        if (newAttrs.containsKey(MediaPrintableArea.class)
                    || attrs.containsKey(MediaMargins.class)) {
            sum.remove(MediaPrintableArea.class);
            sum.remove(MediaMargins.class);
        }

        sum.addAll(newAttrs);
        return sum;
    }
    
    /*
     * TODO: Need to implement this methods 
     */
    public void cancel() {
        /* */
    }

    public boolean isCancelled() {
        return false;
    }

    public PageFormat validatePage(PageFormat page) {
        return null;
    }

    

}
