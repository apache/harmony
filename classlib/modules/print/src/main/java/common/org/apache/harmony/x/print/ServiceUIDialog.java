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

/*
 * ServiceUIDialog class - the class for the PrintService selecting
 * and page setup dialogs.
 * 
 * This class can be used in the following cases:
 * 
 * 1. ServiceUI.printDialog(...) method calls this class to show dialog for the
 *    PrintServices selecting;
 * 
 * 2. Default implementation of java.awt.prin.PrinterJob class may call this
 *    class to show dialogs in printDialog(), printDialog
 *    (PrintRequestAttributeSet), pageDialog(), pageDialog(PageFormat) methods.
 *    (Please, see org.apache.harmony.x.print.awt.PSPrinterJob class and 
 *    org.apache.harmony.x.print.DefaultPrinterJob test class as examples of 
 *    ServiceUIDialog using for the awt.print printing)
 * 
 * 
 * Our printing dialogs look like competitor dialogs, however there are some
 * distinctions and problems there:
 * 
 * 1. Internationalization - will we internationalize the dialogs? It is not
 *    internationalized yet.
 * 
 * 2. Icons for Orientation ("Page Setup" tab) and Sides ("Appearance" tab) - 
 *    how to obtain them?
 * 
 * 3. How to select Media attribute ("Page Setup" tab)? Competitor dialogs 
 *    contain "Source" and "Size" comboboxes. "Source" combobox contains 
 *    MediaTray list and "Size" combobox contains all the others Medias 
 *    including non-standard (i.e. MediaSizeName, MediaName and all the others
 *    but the MediaTray). Do we really need both these comboboxes? Which Media
 *    (from "Size" or "Source" combobox) should we add into the result 
 *    attribute set? Specification says nothing. Now I made "Source" combobox
 *    invisible and add all the Medias supported by the selected PrintService 
 *    into the "Size" combobox.
 * 
 * 4. How to use Margins fields ("Left (mm)", "Right (mm)", "Top (mm)", 
 *    "Bottom (mm)" fields in the "Page Setup" tab)? 
 *    There is only one standard printing attribute related with paper margins 
 *    - MediaPrintableArea. It is a printing attribute used to distingwish 
 *    printable and not printable areas of media. However, this attribute is 
 *    not described as the edges of the paper: MediaPrintableArea is defined 
 *    as a rectangle with its x and y coordinates and its width and height. So,
 *    this attribute depends on selected media size. But printing dialog
 *    contains input fields for margins, not for printable area. In other 
 *    words, we can construct MediaPrintableArea attribute (using dialog 
 *    margins input fields) for the result attribute set only if we can get
 *    selected Media width and height, i.e. if selected media is MediaSizeName
 *    attribute. To avoid this problem, we launched additional (non-standard)
 *    printing attribute - MediaMargins. We suppose that our standard print 
 *    services will support this attribute. 
 *    As a standard java attribute, MediaPrintableArea always should have 
 *    the first priority in case of conflict between MediaPrintableArea and 
 *    MediaMargins attributes. It means, that if we have MediaSizeName, 
 *    MediaPrintableArea and MediaMargins in print request attribute set, we
 *    should use MediaSizeName + MediaPrintableArea to calculate margins. 
 *    However, if we can not get Media size for the required media (for
 *    example, for MediaTray), we may work with Margins attribute, if 
 *    PrintSerevice supports MediaMargins.
 *    Using of MediaMargins attribute is also very convinient if we are setting
 *    page parameters (for example, in standard page dialog) and do not know 
 *    Media which will be used for the printing yet.
 *    Please, see fillMarginsFields() method for more details about the 
 *    MediaPrintableArea and MediaMargins attributes using in dialog result 
 *    attribute set. 
 *    Please, see also comments for the MediaMargins class. 
 * 
 * 5. Specification says nothing about the using of "Properties" button, 
 *    however it reads that if print service provides any vendor extention,
 *    this extention may be accessibly throw additional vendor supplied 
 *    dialog tab panel.
 *    So, I made "Properties" button invisible now and added "Vendor Supplied"
 *    tab instead. I make "Vendor supplied" tab visible if selected print 
 *    service provides some kinds of vendor extentions.
 * 
 * 6. The main problem is that current MRTD version does not support many Swing
 *    components yet, so this dialog can not work now.
 */

package org.apache.harmony.x.print;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilePermission;
import java.net.URI;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.util.Locale;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.TextSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterInfo;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterMakeAndModel;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;

import org.apache.harmony.x.print.attributes.MediaMargins;


public class ServiceUIDialog extends ServiceUIDialogTemplate {

    // State of dialog:
    public static final int APPROVE_PRINT = 1;    // OK button was pressed
    public static final int CANCEL_PRINT = -1;    // Cancel button was pressed
    public static final int SETUP_ERROR = 2;      // Dialog Setup was finished with 
                                                  // error, dialog can not be shown
    public static final int SETUP_OK = 3;         // Dialog setup was OK, 
                                                  // you can show the dialog
    int dialogResult = 0;                         // Current dialog status
    
    // Dialog type:
    public static final int PRINT_DIALOG = 1;     // Dialog for PrintService selecting,
                                                  // all dialog tabs are visible 
    public static final int PAGE_DIALOG = 2;      // Page setup dialog:
                                                  // only PageSetup dialog tab is visible
    private int dialogType = PRINT_DIALOG;  // dialog type
    
    PrintService [] services = null;        // Print services array for the choice
    
    private DocFlavor flavor = null;        // DocFlavor for the dialog
    
    private PrintRequestAttributeSet attrs = null;      
                                            // AttributeSet for the dialog creation
    private PrintRequestAttributeSet newAttrs = null;   
                                            // Result AttributeSet
    
    PrintService myService = null;          // Last selected PrintService
    
    // Button groups:
    ButtonGroup prnRngGrp = null;
    ButtonGroup orientGrp = null;
    ButtonGroup colorGrp = null;
    ButtonGroup sidesGrp = null;
    ButtonGroup qualGrp = null;
    
    // Last selected Orientation
    OrientationRequested lastOrient = null;
    
    // True means that dialog fields were not initialized yet.
    boolean firstUse = true;
    
    // Do we have permitions for the Destination attribute using?
    private Permission destPermission = 
        new FilePermission("<<ALL FILES>>", "read,write");
    
    //---------------------------------------------------------------------
    /*
     * Constructor for the PRINT_DIALOG dialog type. 
     * The dialog is modal. 
     * It can be called from javax.print.ServiceUI.printDialog(...) and from 
     * printDialog(), printDialog(PrintRequestAttributeSet) methods of default 
     * java.awt.print.PrinterJob class implementation.
     * 
     * Parameters:
     *  gc - GraphicsConfiguration to select screen. If gc is null, default screen 
     *       is used.
     *  x - x location of the dialog in screen coordinates
     *  y - y location of the dialog in screen coordinates
     *  services - PrintServices array to be browsable (should be non-null)
     *  defPrintService - initially selected PrintService index in services array
     *                    (should be more then 0 and less then services array 
     *                    length)
     *  flavor - printed DocFlavor (may be null) 
     *  attrs - Initial print request attribute set. It can not be null, but may be
     *          empty. On output this attribute set reflects changes made by user.
     *  owner - dialog owner, should be Frame or Dialog.
     * 
     * Throws:
     *  HeadlessException if current graphics environment is headless
     * 
     * Set dialogResult to SETUP_ERROR if HeadlessException was thrown, owner is 
     * not Frame or Dialog object, services array is null or empty, defServiceIndex
     * is incorrect or attrs is null; set dialogResult to SETUP_OK otherwise. 
    */
    public ServiceUIDialog(GraphicsConfiguration gc, 
                           int x, 
                           int y,
                           PrintService[] dialogServices, 
                           int defServiceIndex, 
                           DocFlavor dialogFlavor,
                           PrintRequestAttributeSet dialogAttrs, 
                           Window owner) 
    {
        if (GraphicsEnvironment.isHeadless()) {
            dialogResult = SETUP_ERROR;
            throw new HeadlessException();
        }
    
        if (owner instanceof Frame) {
            printDialog = new JDialog((Frame)owner, "Print", true, gc);
        } else if(owner instanceof Dialog) {
            printDialog=new JDialog((Dialog)owner, "Print", true, gc);
        } else {
            dialogResult = SETUP_ERROR;
        }
    
        if (printDialog != null) {
            printDialog.setSize(542, 444);
            printDialog.setLocation(x, y);
            printDialog.setContentPane(getPanel());
            printDialog.setResizable(false);
            dialogResult = setup(dialogServices, 
                                 defServiceIndex, 
                                 dialogFlavor, 
                                 dialogAttrs);
        }
    }
    
    /*
     * Constructor for the PAGE_DIALOG dialog type. 
     * The dialog is modal. 
     * It can be called from pageDialog(), pageDialog(PageFormat) methods of 
     * default java.awt.print.PrinterJob class implementation.
     * 
     * Parameters:
     *  gc - GraphicsConfiguration to select screen. If gc is null, default screen 
     *       is used.
     *  x - x location of the dialog in screen coordinates
     *  y - y location of the dialog in screen coordinates
     *  aService - print service for this page dialog 
     *  attrs - initial print request attribute set. It can not be null, but may be
     *          empty. On output this attribute set reflects changes made by user.
     *          Attributes not related with page foemat settings are ignored.
     *  owner - dialog owner, should be Frame or Dialog.
     * 
     * Throws:
     *  HeadlessException if current graphics environment is headless
     * 
     * Set dialogResult to SETUP_ERROR if HeadlessException was thrown, owner is 
     * not Frame or Dialog object, aService or attrs is null.
    */
    public ServiceUIDialog(GraphicsConfiguration gc, 
                           int x, 
                           int y,
                           PrintService aService, 
                           PrintRequestAttributeSet dialogAttrs, 
                           Window owner) {
    
        dialogType = PAGE_DIALOG;
    
        if (GraphicsEnvironment.isHeadless()) {
            dialogResult = SETUP_ERROR;
            throw new HeadlessException();
        }
    
        if (owner instanceof Frame) {
            printDialog = new JDialog((Frame)owner, "Print", true, gc);
        } else if (owner instanceof Dialog) {
            printDialog=new JDialog((Dialog)owner, "Print", true, gc);
        } else {
            dialogResult = SETUP_ERROR;
        }
    
        if (printDialog != null) {
            printDialog.setSize(530, 400);
            printDialog.setLocation(x, y);
            printDialog.setContentPane(getPageDialogPanel());
            printDialog.setResizable(false);
            dialogResult = pageSetup(aService, dialogAttrs);
        }
    }
    
    // ---------------------------------------------------------------------
    /*
     * Shows the dialog if the dialog fields were successfully initialized before
    */
    public void show() {
        if (dialogResult == SETUP_OK) {
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    printDialog.show();
                    return null;
                }
            });       
        }
    }
    
    //---------------------------------------------------------------------
    /*
     * Initialization for PRINT_DIALOG dialog
     * 
      * Parameters:
     *  services - PrintServices array to be browsable
     *  defPrintService - initially selected PrintService index in services array
     *  flavor - printed DocFlavor (may be null) 
     *  attrs - Initial print request attribute set. 
     * 
     * Set dialogResult to SETUP_ERROR services array is null or empty, 
     * defServiceIndex is incorrect or attrs is null; set dialogResult to SETUP_OK 
     * otherwise. 
    */
    private int setup(PrintService[] dialogServices, 
                      int defServiceIndex, 
                      DocFlavor dialogFlavor,
                      PrintRequestAttributeSet dialogAttrs) 
    {
        if ((dialogServices == null) 
                || (dialogServices.length <= 0) 
                || (defServiceIndex < 0) 
                || (defServiceIndex >= dialogServices.length) 
                || (dialogAttrs == null)) {
            return SETUP_ERROR;
        }
    
        services = dialogServices;
        flavor = dialogFlavor;
        attrs = dialogAttrs;
        this.myService = services[defServiceIndex];
        if (servicesBox.getItemCount() <= 0) {
            for (int i = 0; i < services.length; i++) {
                servicesBox.addItem(services[i].getName());
            }
        }
        newAttrs = new HashPrintRequestAttributeSet(attrs);
    
        prepareDialog();        // Prepare dialog
        servicesBox.setSelectedIndex(defServiceIndex);  
                                // Select default PrintService and
                                // initialize dialog fields
        firstUse = false;                               
        return SETUP_OK;
    }
    
    /*
     * Initialization for PAGE_DIALOG dialog
     *  
     * Parameters:
     *  aService - print service for this page dialog 
     *  attrs - initial print request attribute set.
     * 
     * Set dialogResult to SETUP_ERROR if aService or attrs is null.
    */
    private int pageSetup(PrintService aService, 
                          PrintRequestAttributeSet requestAttrs) 
    {
        myService = (aService == null)  
                ? PrintServiceLookup.lookupDefaultPrintService() 
                : aService;
        
        if ((requestAttrs == null) || (aService == null)) {
            return SETUP_ERROR;
        }
        
        attrs = requestAttrs;
        newAttrs = new HashPrintRequestAttributeSet(attrs);
        myService = aService;
        
        prepareDialog();        // prepare dialog
        fillPageSetupFields();  // Initialize dialog fields
        firstUse = false;
        return SETUP_OK;
    }
    
    /*
     * Dialog preparing: create button groups, add listeners to components, etc.
     * This method logically should belong to the ServiceUIDialogTemplate class,
     * however I place it in ServiceUIDialog because ServiceUIDialogTemplated was
     * generated automatically by Eclipse Visual Editor
    */
    private void prepareDialog() {
        JRadioButton [] orientArr = new JRadioButton [] { 
                    portraitBtn, landscapeBtn, rvportraitBtn, rvlandscapeBtn };    
        organizeButtonGroup(orientGrp, orientArr); 
    
        sourceBox.setVisible(false);
        sourceLabel.setVisible(false);
        
        if (dialogType == PRINT_DIALOG) {
            JRadioButton [] rangesArr  = new JRadioButton[] {
                    allRngBtn, pageRngBtn };
            JRadioButton [] colorsArr  = new JRadioButton[] { monoBtn, colorBtn };
            JRadioButton [] sidesArr   = new JRadioButton[] { 
                    oneSideBtn, tumbleBtn, duplexBtn };
            JRadioButton [] qualityArr = new JRadioButton[] { 
                    draftBtn, normalBtn, highBtn };
            
            organizeButtonGroup(prnRngGrp, rangesArr); 
            organizeButtonGroup(colorGrp, colorsArr); 
            organizeButtonGroup(sidesGrp,sidesArr); 
            organizeButtonGroup(qualGrp, qualityArr);
            
            propertiesBtn.setVisible(false);
    
            prtSpinner.setModel(new SpinnerNumberModel(1, 1, 100, 1));
            
            cpSpinner.addChangeListener(new CopiesChangeListener());
            allRngBtn.addChangeListener(new PagesButtonChangeListener());
            pageRngBtn.addChangeListener(new PagesButtonChangeListener());
            servicesBox.addActionListener(new ServicesActionListener());
        }
    
        portraitBtn.addChangeListener(new OrientationChangeListener());
        landscapeBtn.addChangeListener(new OrientationChangeListener());
        rvportraitBtn.addChangeListener(new OrientationChangeListener());
        rvlandscapeBtn.addChangeListener(new OrientationChangeListener()); 
        printBtn.addActionListener(new OKButtonListener());
        cancelBtn.addActionListener(new cancelButtonListener());
        
        printDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                dialogResult = CANCEL_PRINT;
            }
        });
    }
    
    /*
     * Adds all JRadioButtons from "buttons" array to "group" ButtonGroup
     */
    private void organizeButtonGroup(ButtonGroup group, JRadioButton[] buttons) {
        group = new ButtonGroup();
        for (int i = 0; i< buttons.length; i++) {
            group.add(buttons[i]);
        }
    }
    
    //---------------------------------------------------------------------
    
    /*
     * ActionListener for the PrintServices combo box:
     * Update all dialog fields if new print service is selected.
     * As the user browses print services, attributes and values are copied to new
     * display. If user select a print service which does not support particular
     * attribute value, default attribute for this print service is used instead. 
     * Unsupported attributes fields are disabled for the selected print service.
    */
    class ServicesActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (firstUse  
                    || (myService != services[servicesBox.getSelectedIndex()])) {
                myService = services[servicesBox.getSelectedIndex()];
                
                fillGeneralFields();        // General standard tab
                fillPageSetupFields();      // Page Setup standard tab
                fillAppearanceFields();     // Appearance standard tab
                fillVendorSuppliedTab();    // vendor supplied tab (if exists)
            }
        }
    } /* End of ServiceActionListener class */
    
    //---------------------------------------------------------------------
    /*
     *General tab fields filling after PrintService selecting
    */
    void fillGeneralFields() {
        fillStatusField();
        fillTypeField();
        fillInfoField();
        filltoFileBox();
        fillCopiesFields();
        fillPrintRangeFields();
    }
    
    /*
     * "Status" field from "General" tab:
     * If selected print service does not support PrinterIsAcceptingJobs attribute,
     * "Status" field is empty. Otherwise, it is "Accepting jobs" if 
     * PrinterIsAcceptingJob attribute for this print service is ACCEPTING_JOBS; or 
     * "Not accepting jobs" if PrinterIsAcceptingJobs is NOT_ACCEPTING_JOBS.  
    */
    void fillStatusField() {
        String text;
        PrinterIsAcceptingJobs job = (PrinterIsAcceptingJobs)
                myService.getAttribute(PrinterIsAcceptingJobs.class);
        if (job != null) {
            text = job.equals(PrinterIsAcceptingJobs.ACCEPTING_JOBS) 
                   ? "Accepting jobs" 
                   : "Not accepting jobs";
        } else {
            text = "";
        }
    
        statusText.setText(text);
    }
    
    /*
     * "Type" field from "General" tab:
     * This field contains PrinterMakeAndModel attribute of selected print service
     * or is empty if service does not support PrinterMakeAndModel.
    */
    void fillTypeField() {
        PrinterMakeAndModel type = (PrinterMakeAndModel)
                myService.getAttribute(PrinterMakeAndModel.class);
        typeText.setText(type == null ? "" : type.getValue());
    }
    
    /*
     * "Info" field from "General" tab:
     * This field contains PrinterInfo attribute of selected print service
     * or is empty if service does not support PrinterInfo.
    */
    void fillInfoField() {
        PrinterInfo info = (PrinterInfo) myService.getAttribute(PrinterInfo.class);
        infoText.setText(info == null ? "" : info.getValue());
    }
    
    /*
     * "Print to file" combobox from "General" tab:
     * This combobox will be enabled if Destination attribute is supported by 
     * selected print service and user can write to file.
    */
    void filltoFileBox() {
        if (firstUse && attrs.containsKey(Destination.class)) {
            toFileBox.setSelected(true);
        }
        toFileBox.setEnabled(checkFilePermission(destPermission)
                && myService.isAttributeCategorySupported(Destination.class));
    }
    
    /*
     * Checks if the user has given permission
     */
    boolean checkFilePermission(Permission permission) {
        SecurityManager manager = System.getSecurityManager();
        if (manager != null) {
            try {
                manager.checkPermission(permission);
                return true;
            }
            catch(SecurityException e) {
                return false;
            }
        }
        return true;
    }
    
    /*
     * Copies and Collate fields
    */ 
    void fillCopiesFields() {
        fillCopiesSpinner();
        fillCollateBox();
    }
    
    /*
     * "Number of copies" spinner from "General" tab:
     * It is enabled if selected printer supports Copies attribute. Maximum and
     * minimum values of the spinner are minimum and maximum supported Copies 
     * values for selected print service.
    */
    void fillCopiesSpinner() {
        boolean isEnabled = myService.isAttributeCategorySupported(Copies.class);
        
        copiesLabel.setEnabled(isEnabled);
        cpSpinner.setEnabled(isEnabled);
    
        if (firstUse && !isEnabled) {
            int value = (attrs.containsKey(Copies.class) 
                    ? ((Copies)(attrs.get(Copies.class))).getValue() : 1);
            cpSpinner.setModel(new SpinnerNumberModel(value, value, value, 1));
        }
    
        if (isEnabled) {
            int value = (firstUse && attrs.containsKey(Copies.class)  
                        ? ((Copies) (attrs.get(Copies.class))).getValue()  
                        : ((Integer) cpSpinner.getValue()).intValue());
            CopiesSupported supported = (CopiesSupported) myService
                    .getSupportedAttributeValues(Copies.class, flavor, attrs);
            Copies defaul = (Copies) 
                    myService.getDefaultAttributeValue(Copies.class);
            
            if(supported == null) {
                /* 
                 * It is incorrect situation, however it is possible: Copies
                 * category is supported, but there are no supported values. I 
                 * suppose that only default Copies value is supported in this 
                 * case. If default Copies value is null - I suppose that default 
                 * and supported value is 1 Copy only. 
                */
                supported = new CopiesSupported( (defaul != null)
                        ? defaul.getValue()
                        : 1);
            }
            
            int [][] range = supported.getMembers();
            
            if (!supported.contains(value)) {
                value = (((defaul == null) 
                                || (!supported.contains(defaul.getValue()))) 
                        ? range[0][0] 
                        : defaul.getValue());
            }
            
            cpSpinner.setModel(
                    new SpinnerNumberModel(value, range[0][0], range[0][1], 1));
        }
    }
    
    /*
     * Change listener for "Number of copies" spinner.
     * "Collate" combobox is enabled if Copies > 1 only
    */
    class CopiesChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            fillCollateBox();
        }
    }
    
    /* 
     * "Collate" combobox from "General" tab.
     * This box will be enabled if more then one SheetCollate attribute 
     * is supported by selected print service and "Number of copies" value > 1
    */
    void fillCollateBox() {
        boolean isSupported = 
            myService.isAttributeCategorySupported(SheetCollate.class);
        SheetCollate [] supported = (SheetCollate []) (myService
                .getSupportedAttributeValues(SheetCollate.class, flavor, attrs));
        Attribute attr = attrs.get(SheetCollate.class);
        int spinnerValue = ((Integer) cpSpinner.getValue()).intValue();
        
        if ((supported == null) || !isSupported) {
            if (attrs.containsKey(SheetCollate.class)) {
                collateBox.setSelected(attr.equals(SheetCollate.COLLATED));
            }
        } else {
            boolean isValueSupported = myService.isAttributeValueSupported(
                    SheetCollate.COLLATED, flavor, attrs);
            if (attrs.containsKey(SheetCollate.class) && isValueSupported) {
                collateBox.setSelected(attr.equals(SheetCollate.COLLATED)); 
            } else {
                Object defaul = 
                        myService.getDefaultAttributeValue(SheetCollate.class);
                collateBox.setSelected(defaul != null
                        ? defaul.equals(SheetCollate.COLLATED)
                        : true);
            }
        }
    
        collateBox.setEnabled(isSupported 
                           && (spinnerValue > 1)
                           && (!(supported == null || supported.length <= 1)));
    }
    
    /*
     * "Print ranges" fields from "General" tab.
     * "From" and "to" text fields are enabled only if "Pages" radiobutton is 
     * selected. If attr set does not contain PageRanges attribute, default value
     * is always "All". 
    */
    void fillPrintRangeFields() {
        if (firstUse) {
            if (attrs.containsKey(PageRanges.class)) {
                PageRanges aRange = (PageRanges) (attrs.get(PageRanges.class));
                int [][] range = aRange.getMembers();
                fromTxt.setText(range.length > 0 
                        ? Integer.toString(range[0][0]) : "1");
                toTxt.setText(range.length > 0 
                        ? Integer.toString(range[0][1]) : "1");
                pageRngBtn.setSelected(true);
            } else {
                allRngBtn.setSelected(true);
                fromTxt.setEnabled(false);
                toTxt.setEnabled(false);
                fromTxt.setText("1");
                toTxt.setText("1");
                toLabel.setEnabled(false);
            }
        }
    }
    
    /*
     * Change listener for "Print Ranges" fields:
     * Range fields are enabled only if not all the pages should be printed, i.e.
     * if "Pages" button is selected.  
     */
    class PagesButtonChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            fromTxt.setEnabled(pageRngBtn.isSelected());
            toTxt.setEnabled(pageRngBtn.isSelected());
            toLabel.setEnabled(pageRngBtn.isSelected());
        }
    } /* End of PagesButtonChangeListener class */
    
    //---------------------------------------------------------------------
    /*
     * Page Setup fields filling after PrintService selecting
    */
    void fillPageSetupFields() {
        fillMediaFields();
        fillOrientationFields();
        fillMarginsFields();
    }
    
    /*
     * "Size" and "Source" comboboxes from "Page Setup" tab.
     * 
     * I made "Source" combobox invisible because it is unclear how to work with
     * two different Media comboboxes in one dialog (please, see comments in the 
     * beginning of the ServiceUIDialog class for more details).
     * Probably, we will need to change it in the future.
     * 
     * "Size" combobox contains all medias supported by the selected print service.
     * It is disabled if given service does not support Media attribute or 
     * supported media list is empty.
    */
    void fillMediaFields() {
        if (myService.isAttributeCategorySupported(Media.class)) {
            Media [] mediaList = (Media []) myService
                    .getSupportedAttributeValues(Media.class, flavor, attrs);
            Media oldMedia = (sizeBox.getItemCount() <= 0) 
                    ? null 
                    : (Media)sizeBox.getSelectedItem();
    
            sizeBox.removeAllItems();
            if ((mediaList != null) && (mediaList.length > 0)) {
                for(int i = 0; i < mediaList.length; i++) {
                    sizeBox.addItem(mediaList[i]);
                }
                selectMedia(oldMedia);
            }
            sizeBox.setEnabled((mediaList != null) && (mediaList.length > 0));
            sizeLabel.setEnabled((mediaList != null) && (mediaList.length > 0));
        } else {
            sizeBox.setEnabled(false);
            sizeLabel.setEnabled(false);
        }
        sizeBox.updateUI();
    }
    
    /*
     * Selects media in "Sizes" combobox. Selected media is previously 
     * selected Media if it is supported by current print service.
     * Otherwise selected media Media from attrs (if it is supported) or default
     * Media for selected service.
    */
    void selectMedia(Media oldMedia) {
        if (sizeBox.getItemCount() > 0) {    
            
            /* if media was not set - get it from attributes */
            if ((oldMedia == null) && attrs.containsKey(Media.class)) {
                oldMedia = (Media) attrs.get(Media.class);
            }
            sizeBox.setSelectedItem(oldMedia);
        
            if ((sizeBox.getSelectedIndex() < 0)
                    || (!sizeBox.getSelectedItem().equals(oldMedia))) {
                Object media = myService.getDefaultAttributeValue(Media.class);
                if (media != null) {
                    sizeBox.setSelectedItem(media);
                }
            }
            
            /* select first media if there is still no selection */
            if (sizeBox.getSelectedIndex() < 0) {
                sizeBox.setSelectedIndex(0);
            }
        }
    }
    
    /*
     * "Orientation" radiobuttons from "Page Setup" tab.
     * All these buttons are disabled if selected print service does not support 
     * OrientationRequested attribute. Only supported by service orientations 
     * are enabled.
    */
    void fillOrientationFields() {
        
        OrientationRequested orient = 
                (OrientationRequested) attrs.get(OrientationRequested.class);
        boolean isSupported = 
                myService.isAttributeCategorySupported(OrientationRequested.class);
    
        OrientationRequested [] supportedList = (isSupported 
                ? (OrientationRequested []) myService.getSupportedAttributeValues(
                        OrientationRequested.class, flavor, attrs) 
                : null);
        
        enableOrient(supportedList);
          
        /* Select orientation at first time (orientation from attributes set or 
           default orientation for this Print Service) */
        if (firstUse) {
            if (orient != null) { 
                selectOrient(orient);   
            } else {
                OrientationRequested defaul = (OrientationRequested) 
                    myService.getDefaultAttributeValue(OrientationRequested.class);
                selectOrient(isSupported ? defaul : null);
            }
        }
    
        /* Select orientation if previosly selected button is disabled now */
        if (supportedList != null) {
            OrientationRequested oldValue = getOrient();
            if (!orientEnabled(oldValue)) {
                selectOrient(orientEnabled(orient) ? orient : supportedList[0]);
            }
        }
    }
    
    /*
     * Select "Orientation" button corresponding to the given orientation
    */
    private void selectOrient(OrientationRequested par) {
        if (par == null) {
            par = OrientationRequested.PORTRAIT;
        }
        if (par.equals(OrientationRequested.LANDSCAPE)) {
            landscapeBtn.setSelected(true);
        } else if (par.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
            rvlandscapeBtn.setSelected(true);
        } else if (par.equals(OrientationRequested.REVERSE_PORTRAIT)) {
            rvportraitBtn.setSelected(true);
        } else {
            portraitBtn.setSelected(true);
        }
    }
    
    /*
     enable/disable corresponding "Orientation" buttons
    */
    private void enableOrient(OrientationRequested[] list) {
        portraitBtn.setEnabled(false);
        landscapeBtn.setEnabled(false);
        rvportraitBtn.setEnabled(false);
        rvlandscapeBtn.setEnabled(false);
        
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(OrientationRequested.LANDSCAPE)) {
                    landscapeBtn.setEnabled(true);
                } else if (list[i].equals(OrientationRequested.PORTRAIT)) {
                    portraitBtn.setEnabled(true);
                } else if (list[i].equals(OrientationRequested.REVERSE_LANDSCAPE)) {
                    rvlandscapeBtn.setEnabled(true);
                } else if (list[i].equals(OrientationRequested.REVERSE_PORTRAIT)) {
                    rvportraitBtn.setEnabled(true);
                }
            }
        }
    }
    
    /*
     * get selected orientation
    */
    OrientationRequested getOrient() {
        if (portraitBtn.isSelected()) {
            return OrientationRequested.PORTRAIT;
        } else if (landscapeBtn.isSelected()) {
            return OrientationRequested.LANDSCAPE;
        } else if (rvportraitBtn.isSelected()) {
            return OrientationRequested.REVERSE_PORTRAIT;
        } else if (rvlandscapeBtn.isSelected()) {
            return OrientationRequested.REVERSE_LANDSCAPE;
        } else {
            return null;
        }
    }
    
    /*
     * returns true if button for the given orientation is enabled
    */
    private boolean orientEnabled(OrientationRequested par) {
        if (par == null) {
            return false;
        } else if (par.equals(OrientationRequested.LANDSCAPE)) {
            return landscapeBtn.isEnabled();
        } else if (par.equals(OrientationRequested.PORTRAIT)) {
            return portraitBtn.isEnabled();
        } else if (par.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
            return rvlandscapeBtn.isEnabled();
        } else if (par.equals(OrientationRequested.REVERSE_PORTRAIT)) {
            return rvportraitBtn.isEnabled();
        } else {
            return false;
        }
    }
    
    /*
     * return true if at least one orientation button is enabled,
     * i.e. at least one OrientationRequested attribute is supported.
    */
    private boolean isOrientSupported() {
        return landscapeBtn.isEnabled() 
                || portraitBtn.isEnabled() 
                || rvlandscapeBtn.isEnabled() 
                || rvportraitBtn.isEnabled();
    }
    
    /* 
     * Change listener for "Orientation" buttons:
     * "Margins" fields should be updated after the orientation is changed.
    */
    class OrientationChangeListener implements ChangeListener {
    
        public void stateChanged(ChangeEvent e) {
            OrientationRequested now = getOrient();
        
            if ((lastOrient != null) && (now != null) && (!lastOrient.equals(now))) {
                /* if orientation was really changed */
                
                String txt = leftTxt.getText();
        
                if ((lastOrient.equals(OrientationRequested.PORTRAIT)
                                && now.equals(OrientationRequested.LANDSCAPE))
                        || (lastOrient.equals(OrientationRequested.LANDSCAPE)  
                                && now.equals(OrientationRequested.REVERSE_PORTRAIT))
                        || (lastOrient.equals(OrientationRequested.REVERSE_PORTRAIT)
                                && now.equals(OrientationRequested.REVERSE_LANDSCAPE)) 
                        || (lastOrient.equals(OrientationRequested.REVERSE_LANDSCAPE) 
                                && now.equals(OrientationRequested.PORTRAIT))) {
                    leftTxt.setText(bottomTxt.getText());
                    bottomTxt.setText(rightTxt.getText());
                    rightTxt.setText(topTxt.getText());
                    topTxt.setText(txt);
        
                } else if ((lastOrient.equals(OrientationRequested.PORTRAIT) 
                                && now.equals(OrientationRequested.REVERSE_PORTRAIT))
                        || (lastOrient.equals(OrientationRequested.LANDSCAPE) 
                                && now.equals(OrientationRequested.REVERSE_LANDSCAPE))
                        || (lastOrient.equals(OrientationRequested.REVERSE_PORTRAIT) 
                                && now.equals(OrientationRequested.PORTRAIT))
                        || (lastOrient.equals(OrientationRequested.REVERSE_LANDSCAPE)
                                && now.equals(OrientationRequested.LANDSCAPE))) {
                    leftTxt.setText(rightTxt.getText());
                    rightTxt.setText(txt);
                    txt = topTxt.getText();
                    topTxt.setText(bottomTxt.getText());
                    bottomTxt.setText(txt);
                
                } else {
                    leftTxt.setText(topTxt.getText());
                    topTxt.setText(rightTxt.getText());
                    rightTxt.setText(bottomTxt.getText());
                    bottomTxt.setText(txt);
                }
            }
            
            if (now != null) {
                lastOrient = now;
            }
        }
    } /* End of OrientationChangeListener class */
    
    /*
     * "Margins" fields from "Page Setup" tab.
     * 
     * These fields are related with Media, MediaPrintableArea and MediaMargins
     * attributes. 
     * These fields are enabled if selected print service supports MediaMargins
     * attribute, or service supports Media + MediaPrintebleArea attributes and at
     * lease one Media is supported. They are also always enabled if this is a
     * PAGE_DIALOG.
     * 
     * Meaning of this fields should be updated if Orientation is changed.
     * 
     * When we initialize the dialog at first time, "Margins" values are 
     * calculated using the following algorithm: 
     *
     *  * 1. If MediaPrintableArea + Media attributes are supported, attrs set 
     *    contains MediaPrintableArea and Media, Media attribute from attrs is 
     *    supported by selected print service and this is MediaSizeName object 
     *    (i.e. we can get size of this Media) and margins may be correctly 
     *    calculated using these Media and MediaMargins attributes - we get 
     *    margins from these Media and MediaPrintableArea.
     * 2. If margins fields are not defined yet and MediaMargins is supported by 
     *    selected service or this is a page setup dialog, we get MediaMargins from 
     *    attribute set (if it is present) or default MediaMargins for selected 
     *    print service (if service has default MediaMargins)
     * 3. If margins fields are not defined yet, try to obtain MediaMargins from 
     *    selected Media and default MediaPrintebleArea (if it is present) for 
     *    selected print service. If margins can be calculated - fill "Margins"
     *    fields with these meanings.
     * 4. If margins fields are not defined yet, we set them just to some default
     *    meanings (25.4 mm). 
     *  
     * Please, see also comments in the beginning of the ServiceUIDialog class.
    */
    void fillMarginsFields() {
        boolean isMediaSupported = 
                myService.isAttributeCategorySupported(Media.class);
        boolean isPaSupported = myService
                .isAttributeCategorySupported(MediaPrintableArea.class);
        boolean isMarginsSupported = myService
                .isAttributeCategorySupported(MediaMargins.class);
        
        /* We enable margins fields if this is a PAGE_DIALOG or Media and 
           MediaPrintableArea attributes are supported or MediaMargins attribute is
           supported by selected PrintService */
        boolean isMarginsEnabled = ((dialogType == PAGE_DIALOG) 
                || isMarginsSupported  
                || (isMediaSupported 
                        && isPaSupported 
                        && (sizeBox.getSelectedItem() != null)));
        
        enableMargins(isMarginsEnabled);
    
        if (firstUse) {
            /* set margins at first time */
            MediaMargins margins = null;    // Margins for the dialog Margins fields
    
            if (isMarginsEnabled) { // Margins fields are enabled and can be edited
                
                Media selectedMedia = (Media) sizeBox.getSelectedItem();
                boolean isMediaSizeSelected = (selectedMedia == null)
                    ? false :
                    selectedMedia.getClass().isAssignableFrom(MediaSizeName.class);
                MediaSize selectedSize = isMediaSizeSelected
                    ? MediaSize.getMediaSizeForName((MediaSizeName) selectedMedia)
                    : null;
                
                if (isMediaSupported 
                        && isPaSupported 
                        && attrs.containsKey(Media.class)
                        && attrs.containsKey(MediaPrintableArea.class)
                        && attrs.get(Media.class).equals(selectedMedia)
                        && isMediaSizeSelected) {       
                    /* p.1 - see fillMarginsFields() comments above*/   
                    try {
                        MediaPrintableArea attrsPA = (MediaPrintableArea)
                                attrs.get(MediaPrintableArea.class); 
                        margins = new MediaMargins(selectedSize, attrsPA);
                    } catch(IllegalArgumentException e) {
                        /*
                         * If we are unable to get correct margins values from the 
                         * given MediaPrintableArea (attrsPA) and MediaSize 
                         * (selectedSize), we just ignore this case
                         */
                    }
                }
    
                if ((margins == null) 
                        && (isMarginsSupported || (dialogType == PAGE_DIALOG))) {
                    /* p.2 - see fillMarginsFields() comments above*/   
                    margins = (MediaMargins) (attrs.containsKey(MediaMargins.class) 
                         ? attrs.get(MediaMargins.class)
                         : myService.getDefaultAttributeValue(MediaMargins.class));
                }
    
                if ((margins == null)  
                        && isPaSupported  
                        && isMediaSupported
                        && isMediaSizeSelected) {
                    /* p.3 - see fillMarginsFields() comments above*/   
                    try {
                        MediaPrintableArea defaultPA = (MediaPrintableArea) 
                                myService.getDefaultAttributeValue(
                                        MediaPrintableArea.class); 
                        if ((defaultPA != null) && (selectedSize != null)) {
                            margins = new MediaMargins(selectedSize, defaultPA);
                        }
                    } catch (IllegalArgumentException e) {
                        /*
                         * If we are unable to get correct margins value from the
                         * default MediaPrintableArea (defPA) for this service and
                         * MediaSize (selectedSize), we just ignoew this case.
                         */
                    }
                }
    
                if (margins == null) {
                    /* Just 25.4 mm margins! */
                    margins = new MediaMargins(25.4F, 25.4F, 25.4F, 25.4F,
                            MediaMargins.MM);
                }
    
            } else {    
                /* Margins fields are disabled, but we always set them to some 
                   default meanings (25.4 mm) */
                margins = (attrs.containsKey(MediaMargins.class)  
                     ? (MediaMargins) attrs.get(MediaMargins.class) 
                     : new MediaMargins(25.4F, 25.4F, 25.4F, 25.4F, 
                             MediaMargins.MM));
            }
            setMargins(margins);
        }
    }
    
    /* 
     * Enable/disable all Margins fields
     */
    private void enableMargins(boolean flg) {
        leftLabel.setEnabled(flg);
        rightLabel.setEnabled(flg);
        topLabel.setEnabled(flg);
        bottomLabel.setEnabled(flg);
        leftTxt.setEnabled(flg);
        rightTxt.setEnabled(flg);
        topTxt.setEnabled(flg);
        bottomTxt.setEnabled(flg);
    }
    
    /*
     * Set Margins dialog fields in accordance with the given MediaMargins object
    */
    private void setMargins(MediaMargins margins) {
        NumberFormatter fmt = getFloatFormatter();
        try {
            leftTxt.setText(fmt.valueToString(
                    new Float(margins.getX1(MediaMargins.MM))));
            rightTxt.setText(fmt.valueToString(
                    new Float(margins.getX2(MediaMargins.MM))));
            topTxt.setText(fmt.valueToString(
                    new Float(margins.getY1(MediaMargins.MM))));
            bottomTxt.setText(fmt.valueToString(
                    new Float(margins.getY2(MediaMargins.MM))));
        } catch (ParseException e) {
            /* Ignore incorrect float format */
        }
    }
    
    //---------------------------------------------------------------------
    /* 
     * "Apparance" tab fields filling after new PrintService selecting
    */
    void fillAppearanceFields() {
        fillColorFields();
        fillQualityFields();
        fillSidesFields();
        fillJobAttributesFields();
    }
    
    /*
     * "Color" panel radiobuttons from "Appearance" tab.
     * The buttons are enabled only if selected service supports both COLOR and
     * MONOCHROME Chromaticity attributes.  
    */
    void fillColorFields() {
        boolean lastIsMonochrome = getLastColor();   
       
        monoBtn.setEnabled(false);
        colorBtn.setEnabled(false);
    
        if (myService.isAttributeCategorySupported(Chromaticity.class)) {
            Chromaticity [] supported = (Chromaticity []) (myService
                  .getSupportedAttributeValues(Chromaticity.class, flavor, attrs));
            if (supported != null) {
                if (supported.length == 1) {
                    lastIsMonochrome = setMonochrome(
                            (supported[0]).equals(Chromaticity.MONOCHROME));
                } else if (supported.length > 1) {
                    monoBtn.setEnabled(true);
                    colorBtn.setEnabled(true);
                }
            }
        }
    
        if (lastIsMonochrome) {
            monoBtn.setSelected(true);
        } else {
            colorBtn.setSelected(true);
        }
    }
    
    /* 
     * get last selected Chromaticity button 
     */
    private boolean getLastColor() {
        if (firstUse) {
            if (attrs.containsKey(Chromaticity.class)) {
                Attribute value = attrs.get(Chromaticity.class);
                return value.equals(Chromaticity.MONOCHROME);
            } 
            
            Object defaul = myService.getDefaultAttributeValue(Chromaticity.class);
            return (myService.isAttributeCategorySupported(Chromaticity.class)
                        && (defaul != null))
                    ? defaul.equals(Chromaticity.MONOCHROME)
                    : true;
        } 
    
        return monoBtn.isSelected();
    }
    
    private boolean setMonochrome(boolean flg) {
        monoBtn.setEnabled(flg);
        colorBtn.setEnabled(!flg);
        return flg;
    }
    
    /* 
     * "Quality" panel radiobuttons from "Appearance" tab 
     * Only supported by selected print service PrintQualities are enabled.  
    */
    void fillQualityFields() {
        PrintQuality quality = (PrintQuality) attrs.get(PrintQuality.class);
        if (firstUse) {
            selectQualityButton(quality);
        }
    
        PrintQuality [] aList = (
                myService.isAttributeCategorySupported(PrintQuality.class) 
                ? (PrintQuality []) myService
                    .getSupportedAttributeValues(PrintQuality.class, flavor, attrs)
                : null);
        enableQualityButtons(aList);    /* enable qualities which are supported */
    
        /* select quality */
        if ((aList != null) && (!qualityIsEnabled(getSelectedQuality()))) {
            selectQualityButton(qualityIsEnabled(quality) 
                    ? quality 
                    : (PrintQuality) (myService
                            .getDefaultAttributeValue(PrintQuality.class)));
        }
    }
    
    /* 
     * select "Quality" button for the given PrintQuality attribute 
    */
    private void selectQualityButton(PrintQuality par) {
        if (par == null) {
            par = PrintQuality.NORMAL;
        }
        if (par.equals(PrintQuality.DRAFT)) {
            draftBtn.setSelected(true);
        } else if (par.equals(PrintQuality.HIGH)) {
            highBtn.setSelected(true);
        } else {
            normalBtn.setSelected(true);
        }
    }
    
    /* 
     * enable "Quality" buttons for the PrintQuality attributes from the given list 
    */
    private void enableQualityButtons(PrintQuality [] list) {
        normalBtn.setEnabled(false);
        draftBtn.setEnabled(false);
        highBtn.setEnabled(false);
     
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(PrintQuality.DRAFT)) {
                    draftBtn.setEnabled(true);
                } else if (list[i].equals(PrintQuality.NORMAL)) {
                    normalBtn.setEnabled(true);
                } else if (list[i].equals(PrintQuality.HIGH)) {
                    highBtn.setEnabled(true);
                }
            }
        }
    }
    
    /* 
     * return PrintQuality attribute for the selected "Quality" button 
    */
    private PrintQuality getSelectedQuality() {
        if (normalBtn.isSelected()) {
            return PrintQuality.NORMAL;
        } else if (draftBtn.isSelected()) {
            return PrintQuality.DRAFT;
        } else if (highBtn.isSelected()) {
            return PrintQuality.HIGH;
        } else {
            return null;
        }
    }
    
    /* 
     * returns true if "Quality" button for the given PrintQuality attribute 
     * enabled
    */
    private boolean qualityIsEnabled(PrintQuality par) {
        if (par == null) {
            return false;
        } else if (par.equals(PrintQuality.NORMAL)) {
            return normalBtn.isEnabled();
        } else if (par.equals(PrintQuality.DRAFT)) {
            return draftBtn.isEnabled();
        } else if (par.equals(PrintQuality.HIGH)) {
            return highBtn.isEnabled();
        } else {
            return false;
        }
    }
    
    /* 
     * returns true if at least one "Quality" button enabled 
    */
    private boolean isQualitySupported() {
        return (normalBtn.isEnabled() 
             || draftBtn.isEnabled()   
             || highBtn.isEnabled());
    }
    
    /* 
     * "Sides" panel radiobuttons from "Appearance" tab 
     * Only supported by selected print service Sides are enabled.  
    */
    void fillSidesFields() {
        Sides side = (Sides) attrs.get(Sides.class);
        if (firstUse) {
            selectSidesButton(side);
        }
    
        Sides [] aList = (myService.isAttributeCategorySupported(Sides.class) 
                ? (Sides []) (myService.getSupportedAttributeValues(
                        Sides.class, flavor, attrs))
                : null);
        enableSidesButtons(aList);
    
        if ((aList != null) && !sideIsEnabled(getSelectedSide())) {
            selectSidesButton(sideIsEnabled(side) 
                    ? side  
                    : (Sides) (myService.getDefaultAttributeValue(Sides.class)));
        }
    }
    
    /* 
     * Select "Sides" button for the given Sides attribute 
    */
    private void selectSidesButton(Sides par) {
        if (par == null) {
            par = Sides.ONE_SIDED;
        } 
        if (par.equals(Sides.TUMBLE) 
                || par.equals(Sides.TWO_SIDED_SHORT_EDGE)) {
            tumbleBtn.setSelected(true);
        } else if (par.equals(Sides.DUPLEX)  
                || par.equals(Sides.TWO_SIDED_LONG_EDGE)) {
            duplexBtn.setSelected(true);
        } else {
            oneSideBtn.setSelected(true);
        }
    }
    
    /* 
     * enable "Sides" buttons for the Sides attributes from the given list 
    */
    private void enableSidesButtons(Sides [] list) {
        oneSideBtn.setEnabled(false);
        duplexBtn.setEnabled(false);
        tumbleBtn.setEnabled(false);
        
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(Sides.ONE_SIDED)) {
                    oneSideBtn.setEnabled(true);
                } else if (list[i].equals(Sides.DUPLEX)
                        || list[i].equals(Sides.TWO_SIDED_LONG_EDGE)) {
                    duplexBtn.setEnabled(true);
                } else if (list[i].equals(Sides.TUMBLE)
                        || list[i].equals(Sides.TWO_SIDED_SHORT_EDGE)) {
                    tumbleBtn.setEnabled(true);
                }
            }
        }
    }
    
    /* 
     * returns Sides attribute object for the selected "Sides" button 
    */
    private Sides getSelectedSide() {
        if (oneSideBtn.isSelected()) {
            return Sides.ONE_SIDED;
        } else if (duplexBtn.isSelected()) {
            return Sides.DUPLEX;
        } else if (tumbleBtn.isSelected()) {
            return Sides.TUMBLE;
        } else {
            return null;
        }
    }
    
    /* 
     * returns true if "Sides" button for this Sides attribute enabled 
    */
    private boolean sideIsEnabled(Sides par) {
        if (par == null) {
            return false;
        } else if (par.equals(Sides.ONE_SIDED)) {
            return oneSideBtn.isEnabled();
        } else if (par.equals(Sides.DUPLEX) || 
                   par.equals(Sides.TWO_SIDED_LONG_EDGE)) {
            return duplexBtn.isEnabled();
        } else if (par.equals(Sides.TUMBLE) || 
                   par.equals(Sides.TWO_SIDED_SHORT_EDGE)) {
            return tumbleBtn.isEnabled();
        } else {
            return false;
        }
    }
    
    /*
     * returns true if at least one "Sides" button is enabled
     * (that means at least one Sides attribute supported)
    */
    private boolean isSidesSupported() {
        return (oneSideBtn.isEnabled() 
             || duplexBtn.isEnabled() 
             || tumbleBtn.isEnabled());
    }
    
    /*
     * "Job Attribute" panel fields from "Appearance" tab 
    */
    void fillJobAttributesFields() {
        fillBannerPageField();
        fillPriorityField();
        fillJobNameField();
        fillUserNameField();
    }
    
    /* 
     * "Banner Page" checkbox from "Appearance" tab.
     * This checkbox is enabled if selected print service supports more then one 
     * JobSheets attributes. This checkbox is selected if it is corresponds to 
     * JobSheets.STANDARD attribute and unselected otherwise.
    */
    void fillBannerPageField() {
        JobSheets [] supported = 
                (myService.isAttributeCategorySupported(JobSheets.class) 
                ? (JobSheets[]) (myService.getSupportedAttributeValues(
                        JobSheets.class, flavor, attrs))
                : null); 
        Attribute value = attrs.get(JobSheets.class);
    
        if ((supported != null) && (supported.length == 0)) {
            supported = null;
        }
    
        if (supported == null) {
            /* if PrintService does not supported any JobSheets, set current 
               meaning from attribute set (if present) and disable checkbox */
            if (firstUse && attrs.containsKey(JobSheets.class)) {
                bannerBox.setSelected(value.equals(JobSheets.STANDARD));
            }
        } else {
            if (supported.length == 1) {
                bannerBox.setSelected(supported[0] == JobSheets.STANDARD);
            } else if (attrs.containsKey(JobSheets.class)) {
                bannerBox.setSelected(value.equals(JobSheets.STANDARD));
            }  else {
                Object def = myService.getDefaultAttributeValue(JobSheets.class);
                bannerBox.setSelected(def == null
                        ? false
                        : def.equals(JobSheets.STANDARD));
            }
        }
        bannerBox.setEnabled((supported != null) && (supported.length > 1));
    }
    
    /* 
     * "Priority" spinner from "Appearance" tab.
     * It is enabled if selected print service supports JobPriority attribute. 
    */
    void fillPriorityField() {
        boolean enabled = 
                myService.isAttributeCategorySupported(JobPriority.class);
        priorityLabel.setEnabled(enabled);
        prtSpinner.setEnabled(enabled);
    
        if (firstUse) {
            if (attrs.containsKey(JobPriority.class)) {
                JobPriority value = (JobPriority) (attrs.get(JobPriority.class));
                prtSpinner.setValue(new Integer(value.getValue()));
            } else {
                if (enabled) {
                    JobPriority defaul = (JobPriority)  (
                            myService.getDefaultAttributeValue(JobPriority.class));
                    prtSpinner.setValue (defaul == null 
                            ? new Integer(1) 
                            : new Integer(defaul.getValue()));
                } else {
                    prtSpinner.setValue(new Integer(1));
                }
            }
        }
    }
    
    /* 
     * "Job Name" text field from "Appearance" tab
     * It is enabled if selected print service supports JobName attribute. 
    */
    void fillJobNameField() {
        boolean supported = myService.isAttributeCategorySupported(JobName.class);
        jobNameTxt.setEnabled(supported);
        jobNameLabel.setEnabled(supported);
    
        if (firstUse && attrs.containsKey(JobName.class)) {
            jobNameTxt.setText(((TextSyntax) attrs.get(JobName.class)).getValue());
        }
       
        if(supported && (jobNameTxt.getText().length() <= 0)) {
            TextSyntax txt = (TextSyntax) 
                    (myService.getDefaultAttributeValue(JobName.class));
            jobNameTxt.setText(txt == null ? "" : txt.getValue());
        }
    }
    
    /* 
     * "User Name" text field from "Appaerance" tab 
     * It is enabled if selected print service supports RequestingUserName 
     * attribute. 
    */
    void fillUserNameField() {
        boolean flg = 
            myService.isAttributeCategorySupported(RequestingUserName.class);
        userNameTxt.setEnabled(flg);
        userNameLabel.setEnabled(flg);
        
        if (firstUse && attrs.containsKey(RequestingUserName.class)) {
            userNameTxt.setText(((TextSyntax) 
                    attrs.get(RequestingUserName.class)).getValue());
        }
       
        if (flg && (userNameTxt.getText().length() <= 0)) {
            RequestingUserName defaul = (RequestingUserName) (myService
                    .getDefaultAttributeValue(RequestingUserName.class));
            userNameTxt.setText(defaul==null ? "" : (String) (defaul.getValue()));
        }
    }
    
    //---------------------------------------------------------------------
    /* 
     * We add Vendor supplied tab to the dialog panel if selected print service has
     * UIFactory and this factory has MAIN_UIROLE Panel or JComponent. 
    */ 
    void fillVendorSuppliedTab() {
        ServiceUIFactory factory = myService.getServiceUIFactory();
    
        if (tabbedPane.getTabCount() > 3) {
            tabbedPane.remove(3);
        }
        
        if (factory != null) {
            JComponent swingUI = (JComponent) factory.getUI(
                    ServiceUIFactory.MAIN_UIROLE, ServiceUIFactory.JCOMPONENT_UI);
            if (swingUI != null) {
                tabbedPane.addTab("Vendor Supplied", swingUI);
                tabbedPane.setMnemonicAt(3, 'V');
            } else {
                Panel panelUI = (Panel) factory.getUI(ServiceUIFactory.MAIN_UIROLE,
                                                      ServiceUIFactory.PANEL_UI);
                if (panelUI != null) {
                    tabbedPane.addTab("Vendor Supplied", panelUI);
                    tabbedPane.setMnemonicAt(3, 'V');
                }
            }
        }
    }
    
    //---------------------------------------------------------------------
    /*
     * ActionListener for "Print" button:
     * if we can get correct result attribute set (newAttrs),
     * hides the dialog and set dialog result to APPROVE_PRINT
     */
    class OKButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (updateAttributes()) {   // form result attribute set for the dialog
                dialogResult = APPROVE_PRINT;
                printDialog.hide();
            }
        }
    } /* End of OKButtonListener */
    
    /*
     * ActionListener for "Cancel" button:
     * hides the dialog and set dialog result to CANCEL_PRINT.
     * This method does not change dialog result attribute set (newAttrs)
     */
    class cancelButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            dialogResult = CANCEL_PRINT;
            printDialog.hide();
        }
    } /* End of cancelButtonListener */
    
    /*
     * returns current dialogResult
     */
    public int getResult() {
        return dialogResult;
    }
    
    /*
     * returns result attribute set.
     */
    public PrintRequestAttributeSet getAttributes() {
        return newAttrs;
    }
    
    /*
     * returns result PrintService if dialogResult is APPROVE_PRINT
     */
    public PrintService getPrintService() {
        return (dialogResult == APPROVE_PRINT) ? myService : null;
    }
    
    /*
     * returns dialog's PrintServices list
     */
    public PrintService [] getServices() {
        return services;
    }
    
    /*
     * returns last selected PrintService
     */
    public PrintService getSelectedService() {
        return myService;
    }
    
    /*
     * returns dialog's docflavor
     */
    public DocFlavor getFlavor() {
        return flavor;
    }
    
    //---------------------------------------------------------------------
    /* 
     * Getting result attribute set after OK button click 
    */
    protected boolean updateAttributes() {
        newAttrs = new HashPrintRequestAttributeSet(attrs);
        if (dialogType == PRINT_DIALOG) {
            updateCopies();
            updateCollate();
            if (!updatePrintRange()) {
                JOptionPane.showMessageDialog(printDialog,
                                              "Incorrect Print Range!", 
                                              "Incorrect parameter",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            updateColor();
            updateQuality();
            updateSides();
            updateBannerPage();
            updatePriority();
            updateJobName();
            updateUserName();
            updatePrintToFile();
        }
        updateMedia();
        updateOrientation();
        if (!updateMargins()) {
            JOptionPane.showMessageDialog(printDialog, 
                                          "Incorrect margins!",
                                          "Incorrect parameter", 
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    /* 
     * Select output file and add/update Destination attribute to the result 
     * attribute set if "Print to file" box is enabled and selected. 
     * Remove Destination attribute otherwise.
    */
    private void updatePrintToFile() {
        if (toFileBox.isEnabled() && toFileBox.isSelected()) {      
            
            Destination dest = (Destination) 
                        (newAttrs.containsKey(Destination.class)
                    ? newAttrs.get(Destination.class)     
                    : myService.getDefaultAttributeValue(Destination.class));
            File file = null;
            DestinationChooser chooser = new DestinationChooser();
    
            if (dest == null) {
                dest = new Destination((new File("out.prn")).toURI());
                /* Default file name for the output file is "out.prn" */
            }
            
            try {
                file = new File(dest.getURI());
            } catch (Exception e) {
                file = new File("out.prn");
            }  
            
            chooser.setSelectedFile(file);
            chooser.setDialogTitle("Print to file");
            int chooserResult = chooser.showDialog(printDialog, "OK"); 
            if (chooserResult == JFileChooser.APPROVE_OPTION) {
                try {
                    URI selectedFile = chooser.getSelectedFile().toURI();
                    newAttrs.add(new Destination(selectedFile));
                } catch (Exception e) {
                    removeAttribute(Destination.class);
                }
            }
        } else {
            removeAttribute(Destination.class);
        }
    }
    
    /* 
     * Add/update Copies attribute to the result attribute set if "Number of 
     * copies" spinner is enabled. Remove Copies attribute otherwise. 
    */
    private void updateCopies() {
        if (cpSpinner.isEnabled()) {
            int copiesValue = ((SpinnerNumberModel) 
                    (cpSpinner.getModel())).getNumber().intValue();
            newAttrs.add(new Copies(copiesValue));
        } else {
            removeAttribute(Copies.class);
        }
    }
    
    /* 
     * Add/update SheetCollate attribute to the result attribute set if "Collate"
     * checkbox is enabled. Remove SheetCollate attribute otherwise.
    */
    private void updateCollate() {
        if (collateBox.isEnabled()) {
            newAttrs.add(collateBox.isSelected() 
                    ? SheetCollate.COLLATED 
                    : SheetCollate.UNCOLLATED);
        } else {
            removeAttribute(SheetCollate.class);
        }
    }
    
    /* 
     * Add/update PageRanges attribute to the result attribute set if "Pages"
     * radiobutton is selected and enabled and "from" <= "to". Remove PageRanges
     * otherwise. "All" is always default print range, so we do not need to add
     * PageRanges to the result attribute set if "All" button is selected.
     * 
     * Returns false if "from" > "to" or "from" or "to" have incorrect number
     * format.
    */
    protected boolean updatePrintRange() {
        if (pageRngBtn.isEnabled() && pageRngBtn.isSelected()) {
            try {
                int fromValue = Integer.valueOf(fromTxt.getText()).intValue();
                int toValue = Integer.valueOf(toTxt.getText()).intValue();
                if (fromValue > toValue) {
                    throw new NumberFormatException();
                }
                newAttrs.add(new PageRanges(fromValue, toValue));
            } catch (NumberFormatException e) {
                return false;
            } catch (IllegalArgumentException e) {
                return false;            
            }
        } else {
            removeAttribute(PageRanges.class);
        }
        return true;
    }
    
    /*
     * Add Media attribute to result attribute set if "Size" combobox is enabled,
     * remove Media otherwise.
    */
    private void updateMedia() {
        if (sizeBox.isEnabled() && (sizeBox.getItemCount() > 0)) {
            newAttrs.add((Media) (sizeBox.getSelectedItem()));
        } else {
            removeAttribute(Media.class);
        }
    }
    
    /*
     * Add OrientationRequested attribute if selected service supports Orientation
     * attribute, remove OrientationRequested otherwise
    */
    private void updateOrientation() {
        if (isOrientSupported()) {
            newAttrs.add(getOrient());
        } else {
            removeAttribute(OrientationRequested.class);
        }
    }
    
    /*
     * If Margins fields are disabled, remove MediaPrintableArea and MediaMargins
     * from the result attribute set and returns true.
     * Otherwise try to add/update MediaPrintableArea attribute if print service
     * supports MediaPrintableArea, try to add/update MediaMargins attribute if
     * service supports MediaMargins attribute. 
     * 
     * Returns false if margins fields have incorrect number format or margins too
     * big for selected Media. 
    */
    private boolean updateMargins() {
        float x1;
        float y1;
        float x2; 
        float y2;    
        NumberFormatter format = getFloatFormatter();
    
        if (!leftTxt.isEnabled()) {
            removeAttribute(MediaPrintableArea.class);
            removeAttribute(MediaMargins.class);
            return true;
        }
    
        try {
            x1 = ((Float) format.stringToValue(leftTxt.getText())).floatValue();
            x2 = ((Float) format.stringToValue(rightTxt.getText())).floatValue();
            y1 = ((Float) format.stringToValue(topTxt.getText())).floatValue();
            y2 = ((Float) format.stringToValue(bottomTxt.getText())).floatValue();
        } catch(ParseException e) {
            return false;
        }
    
        if (sizeBox.isEnabled() 
             && (sizeBox.getSelectedItem() instanceof MediaSizeName) 
             && myService.isAttributeCategorySupported(MediaPrintableArea.class)) {
            MediaSize mediaSize = MediaSize.getMediaSizeForName(
                    (MediaSizeName) sizeBox.getSelectedItem());
            float paperWidth = mediaSize.getX(Size2DSyntax.MM);
            float paperHeight = mediaSize.getY(Size2DSyntax.MM);
            if ((x1 + x2 >= paperWidth) || (y1 + y2 >= paperHeight)) {
                return false;
            }
            newAttrs.add(new MediaPrintableArea(x1, 
                                                y1, 
                                                paperWidth - x1 - x2,
                                                paperHeight - y1 - y2, 
                                                MediaPrintableArea.MM));
        } else {
            removeAttribute(MediaPrintableArea.class);
        }
    
        if (myService.isAttributeCategorySupported(MediaMargins.class)) {
            newAttrs.add(new MediaMargins(x1, y1, x2, y2, MediaMargins.MM));
        } else {
            removeAttribute(MediaMargins.class);
        }
        return true;
    }
    
    /* 
     * Add/update Chromaticity attribute to the result attribute set if needed.
     * Remove Chromaticity otherwise. 
    */
    private void updateColor() {
        if (monoBtn.isEnabled() && monoBtn.isSelected()) {
            newAttrs.add(Chromaticity.MONOCHROME);
        } else if (colorBtn.isEnabled() && colorBtn.isSelected()) {
            newAttrs.add(Chromaticity.COLOR);
        } else {
            removeAttribute(Chromaticity.class);
        }
    }
    
    /* 
     * Add/update PrintQuality attribute to the result attribute set if print 
     * service supports PrintQuality. Remove PrintQuality otherwise. 
    */
    private void updateQuality() {
        if (isQualitySupported()) {
            newAttrs.add(getSelectedQuality());
        } else {
            removeAttribute(PrintQuality.class);
        }
    }
    
    /* 
     * Add/update Sides attribute to the result attribute set if print service 
     * supports Sides. Remove Sides otherwise. 
    */
    private void updateSides() {
        if (isSidesSupported()) {
            newAttrs.add(getSelectedSide());
        } else {
            removeAttribute(Sides.class);
        }
    }
    
    /* 
     * Add/update JobSheets attribute to the result attribute set if "Banner Page"
     * combobox box is enabled. Remove JobSheets otherwise. 
    */
    private void updateBannerPage() {
        if (bannerBox.isEnabled()) {
            newAttrs.add(bannerBox.isSelected()  
                    ? JobSheets.STANDARD 
                    : JobSheets.NONE);
        } else {
            removeAttribute(JobSheets.class);
        }
    }
    
    /* 
     * Add/update JobPriority attribute to the result attribute set if "Priority"
     * spinner is enabled. Remove JobPriority otherwise. 
    */
    private void updatePriority() {
        if (prtSpinner.isEnabled()) {
            int priority = ((Integer) (prtSpinner.getValue())).intValue();
            newAttrs.add(new JobPriority(priority));
        } else {
            removeAttribute(JobPriority.class);
        }
    }
    
    /* 
     * Add/update JobName attribute to the result attribute set if "Job name" field
     * is enabled and is not empty. Remove JobName otherwise. 
    */
    private void updateJobName() {
        if (jobNameTxt.isEnabled()) {
            String name = jobNameTxt.getText();
            if (name.length() == 0) {
                removeAttribute(JobName.class);
            } else {
                newAttrs.add(new JobName(name, Locale.getDefault()));
            }
        } else {
            removeAttribute(JobName.class);
        }
    }
    
    /* 
     * Add/update JobName attribute to the result attribute set if "User name" 
     * field is enabled and is not empty. Remove UserName otherwise. 
    */
    private void updateUserName() {
        if (userNameTxt.isEnabled()) {
            String name = userNameTxt.getText();
            if (name.length() == 0) {
                removeAttribute(RequestingUserName.class);
            } else {
                newAttrs.add(new RequestingUserName(name, Locale.getDefault()));
            }
        } else {
            removeAttribute(RequestingUserName.class);
        }
    }
    
    private void removeAttribute(Class cls) {
        if (newAttrs.containsKey(cls)) {
            newAttrs.remove(cls);
        }
    }
    
    //---------------------------------------------------------------------
    /* 
     * Panel for the Page Setup dialog 
    */
    private JPanel getPageDialogPanel() {
        JPanel pageDialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints182 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints172 = new GridBagConstraints();
        pageDialogPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        pageDialogPanel.setSize(532, 389);
        gridBagConstraints172.gridx = 0;
        gridBagConstraints172.gridy = 1;
        gridBagConstraints172.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints172.gridwidth = 3;
        gridBagConstraints182.gridx = 0;
        gridBagConstraints182.gridy = 0;
        gridBagConstraints182.weightx = 1.0;
        gridBagConstraints182.weighty = 1.0;
        gridBagConstraints182.fill = java.awt.GridBagConstraints.BOTH;
        pageDialogPanel.add(getButtonsPanel(), gridBagConstraints172);
        pageDialogPanel.add(getPageSetupPanel(), gridBagConstraints182);
        return pageDialogPanel;
    }
    
    //---------------------------------------------------------------------
    /* 
     * JFileChooser for the selecting file for the Destination attribute.
     * Shows confirm message if the selected file is always exists
     */
    private class DestinationChooser extends JFileChooser {
        
        private static final long serialVersionUID = 5429146989329327138L;
        
        public void approveSelection() {
            boolean doesFileExists = false; // Does selected file exist?
            boolean result = true;      // File selection result
            
            try {
                doesFileExists = getSelectedFile().exists();
            } catch (Exception e) {
                /* if exception was thrown, fileExists flag remains false */
            }
            
            if (doesFileExists) {
                FilePermission delPermission = new FilePermission(
                        getSelectedFile().getAbsolutePath(), "delete");
                if (checkFilePermission(delPermission)) {
                    String msg = "File " + getSelectedFile() + " is already exists.\n" +
                                 "Do you want to overwrite it?";
                    int approveDelete = JOptionPane.showConfirmDialog(
                            null, "File exists!", msg, JOptionPane.YES_NO_OPTION);
                    result = (approveDelete == JOptionPane.YES_OPTION);           
                } else {
                    JOptionPane.showMessageDialog(
                            null, "Can not delete file " + getSelectedFile());
                    result = false;
                }
            }
        
            if (result) {
                super.approveSelection();
            }
        }
    } /* End of DestinationChooser class */
} /* End of ServiceUIDialog class */
