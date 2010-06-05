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
 * @author Anton Avtamonov, Sergey Burlak
 */
package javax.swing.plaf.basic;

import java.io.File;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;

public class BasicFileChooserUITest extends SwingTestCase {
    private BasicFileChooserUI ui;

    private JFileChooser fc;

    @Override
    protected void setUp() throws Exception {
        fc = new JFileChooser();
        ui = new BasicFileChooserUI(fc);
    }

    @Override
    protected void tearDown() throws Exception {
        fc = null;
        ui = null;
    }

    public void testGetAcceptAllFileFilter() throws Exception {
        ui.installUI(fc);
        assertNotNull(ui.getAcceptAllFileFilter(fc));
        assertEquals(UIManager.getString("FileChooser.acceptAllFileFilterText"), ui
                .getAcceptAllFileFilter(fc).getDescription());
        assertTrue(ui.getAcceptAllFileFilter(fc).accept(new File("")));
    }

    public void testGetApproveButtonText() throws Exception {
        ui.installUI(fc);
        assertEquals(UIManager.get("FileChooser.openDialogTitleText"), ui.getDialogTitle(fc));
        assertEquals(UIManager.get("FileChooser.openButtonText"), ui.getApproveButtonText(fc));
        assertEquals(UIManager.get("FileChooser.openButtonToolTipText"), ui
                .getApproveButtonToolTipText(fc));
        assertEquals(UIManager.getInt("FileChooser.openButtonMnemonic"), ui
                .getApproveButtonMnemonic(fc));
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        assertEquals(UIManager.get("FileChooser.saveDialogTitleText"), ui.getDialogTitle(fc));
        assertEquals(UIManager.get("FileChooser.saveButtonText"), ui.getApproveButtonText(fc));
        assertEquals(UIManager.get("FileChooser.saveButtonToolTipText"), ui
                .getApproveButtonToolTipText(fc));
        assertEquals(UIManager.getInt("FileChooser.saveButtonMnemonic"), ui
                .getApproveButtonMnemonic(fc));
        fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
        assertNull(ui.getApproveButtonText(fc));
        assertNull(ui.getApproveButtonToolTipText(fc));
        assertNull(ui.getDialogTitle(fc));
        assertEquals(0, ui.getApproveButtonMnemonic(fc));
    }

    public void testGetApproveButtonToolTipText() throws Exception {
        try {     
            javax.swing.plaf.basic.BasicFileChooserUI b = 
                new javax.swing.plaf.basic.BasicFileChooserUI(new JFileChooser("")); 
            b.getApproveButtonToolTipText(null); 
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {    
            // PASSED          
        }
    }

    public void testGetModel() throws Exception {
        ui.installUI(fc);
        assertNotNull(ui.getModel());
    }

    public void testBasicFileView() throws Exception {
        BasicFileChooserUI.BasicFileView fileView = new BasicFileChooserUI(fc).new BasicFileView();
        assertNotNull(fileView);
        assertEquals(0, fileView.iconCache.size());
        File f = new File("f");
        f.createNewFile();
        Icon i = new ImageIcon();
        fileView.cacheIcon(f, i);
        assertEquals(1, fileView.iconCache.size());
        assertTrue(i == fileView.getCachedIcon(f));
        assertTrue(i == fileView.getIcon(f));
        fileView.cacheIcon(null, null);
        assertEquals(1, fileView.iconCache.size());
        assertEquals(f.getName(), fileView.getDescription(f));
        ui.installUI(fc);
        fileView = (BasicFileChooserUI.BasicFileView) ui.getFileView(fc);
        fileView.clearIconCache();
        assertEquals(0, fileView.iconCache.size());
        f.delete();
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertNull(ui.createPropertyChangeListener(fc));
    }

    public void testGetApproveButton() {
        ui.installUI(fc);
        assertNull(ui.getApproveButton(fc));
    }

    public void testGetDialogTitle() {
        assertNull(ui.getDialogTitle(fc));
        ui.installUI(fc);
        fc.setDialogTitle("my");
        assertEquals("my", ui.getDialogTitle(fc));
    }

    public void testGetFileView() {
        ui.installUI(fc);
        assertNotNull(ui.getFileView(fc));
        assertTrue(ui.getFileView(fc) instanceof BasicFileChooserUI.BasicFileView);
    }

    public void testGetSetDirectory() {
        assertNull(ui.getDirectory());
        ui.installUI(fc);
        assertNull(ui.getDirectory());
        final File f = new File("aa/aa/aa");
        ui.setDirectory(f);
        assertEquals(f, ui.getDirectory());
    }

    public void testGetSetDirectoryName() {
        assertNull(ui.getDirectoryName());
        ui.setDirectoryName("a");
        assertNull(ui.getDirectoryName());
    }

    public void testGetSetDirectorySelected() {
        assertFalse(ui.isDirectorySelected());
        ui.setDirectorySelected(true);
        assertTrue(ui.isDirectorySelected());
    }

    public void testGetAccessoryPanel() {
        assertNull(ui.getAccessoryPanel());
        ui.installUI(fc);
        assertNotNull(ui.getAccessoryPanel());
        JPanel ap = new JPanel();
        fc.setAccessory(ap);
        assertNotSame(ap, ui.getAccessoryPanel());
    }

    public void testGetApproveButtonAction() {
        ui.installUI(fc);
        assertNotNull(ui.getApproveSelectionAction());
        assertEquals(ui.getApproveSelectionAction(), ui.getApproveSelectionAction());
    }

    public void testGetPreferredSize() {
        assertNull(ui.getPreferredSize(fc));
        ui.installUI(fc);
        assertNull(ui.getPreferredSize(fc));
    }

    public void testEnsureFileIsVisible() {
        try {   
            BasicFileChooserUI fc = new BasicFileChooserUI(null);  
            fc.ensureFileIsVisible(new JFileChooser(), new File("a")); 
            // PASSED
        } catch (NullPointerException npe) {     
            fail("NPE should not be thrown");            
        }
    }

    public void testInstallDefaults() {
        try {
            new BasicFileChooserUI(null) {
                public void installDefaults(JFileChooser fc) {
                    super.installDefaults(fc);
                }
            }.installDefaults(null);
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {
            // Passed
        }
    }
}
