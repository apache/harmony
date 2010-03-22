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
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.metal;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;

public class MetalInternalFrameUITest extends SwingTestCase {
    private JInternalFrame frame;

    private MetalInternalFrameUI ui;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        ui = new MetalInternalFrameUI(frame);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private boolean belongs(final Object o, final Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == o) {
                return true;
            }
        }
        return false;
    }

    /*
     * Constructor for MetalInternalFrameUITest.
     * @param name
     */
    public MetalInternalFrameUITest(final String name) {
        super(name);
    }

    /*
     * Class under test for JComponent createNorthPane(JInternalFrame)
     */
    public void testCreateNorthPane() {
        JComponent comp = ui.createNorthPane(frame);
        assertTrue("not null", comp != null);
        assertTrue("instanceof MetalInternalFrameTitlePane",
                comp instanceof MetalInternalFrameTitlePane);
        JComponent comp2 = ui.createNorthPane(frame);
        assertTrue("new object", comp != comp2);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui = MetalInternalFrameUI.createUI(frame);
        assertTrue("not null", ui != null);
        assertTrue("instanceof MetalInternalFrameUI", ui instanceof MetalInternalFrameUI);
        ComponentUI ui2 = MetalInternalFrameUI.createUI(frame);
        assertTrue("stateful", ui != ui2);
    }

    /*
     * Class under test for void installUI(JComponent)
     */
    public void testInstallUI() {
        ui.installUI(frame);
        assertFalse("false by default",
                ((MetalInternalFrameTitlePane) ui.getNorthPane()).isPalette);
        frame.putClientProperty(MetalInternalFrameUI.IS_PALETTE, Boolean.TRUE);
        ui = new MetalInternalFrameUI(frame);
        ui.installUI(frame);
        assertTrue("true", ((MetalInternalFrameTitlePane) ui.getNorthPane()).isPalette);
        frame.putClientProperty(MetalInternalFrameUI.IS_PALETTE, Boolean.FALSE);
        ui = new MetalInternalFrameUI(frame);
        ui.installUI(frame);
        assertFalse("false again", ((MetalInternalFrameTitlePane) ui.getNorthPane()).isPalette);
    }

    /*
     * Class under test for void uninstallUI(JComponent)
     */
    public void testUninstallUI() {
        // Note: nothing to test
    }

    /*
     * Class under test for void installListeners()
     */
    public void testInstallListeners() {
        frame.setUI(ui);
        ui.uninstallListeners();
        ui.installListeners();
        frame.putClientProperty(MetalInternalFrameUI.IS_PALETTE, Boolean.TRUE);
        assertTrue("isPalette of titlePane is true", ((MetalInternalFrameTitlePane) ui
                .getNorthPane()).isPalette);
        frame.putClientProperty(MetalInternalFrameUI.IS_PALETTE, Boolean.FALSE);
        assertFalse("isPalette of titlePane is false", ((MetalInternalFrameTitlePane) ui
                .getNorthPane()).isPalette);
    }

    /*
     * Class under test for void uninstallListeners()
     */
    public void testUninstallListeners() {
        frame.setUI(ui);
        ui.uninstallListeners();
        frame.putClientProperty(MetalInternalFrameUI.IS_PALETTE, Boolean.TRUE);
        assertFalse("isPalette of titlePane is false", ((MetalInternalFrameTitlePane) ui
                .getNorthPane()).isPalette);
    }

    /*
     * Class under test for void installKeyboardActions()
     */
    public void testInstallKeyboardActions() {
        // Note: nothing to test
    }

    /*
     * Class under test for void uninstallKeyboardActions()
     */
    public void testUninstallKeyboardActions() {
        // Note: nothing to test
    }

    /*
     * Class under test for void uninstallComponents()
     */
    public void testUninstallComponents() {
        frame.setUI(ui);
        ui.uninstallComponents();
        assertFalse("titlePane uninstalled", belongs(ui.getNorthPane(), frame.getComponents()));
        assertNull("northPane == null", ui.getNorthPane());
    }

    /*
     * Class under test for MetalInternalFrameUI(JInternalFrame)
     */
    public void testMetalInternalFrameUI() {
        // nothing to test
    }

    /*
     * Class under test for void setPalette(boolean)
     */
    public void testSetPalette() {
        frame.setUI(ui);
        ui.setPalette(true);
        assertTrue("isPalette of titlePane is true", ((MetalInternalFrameTitlePane) ui
                .getNorthPane()).isPalette);
        assertTrue("changed frame border", UIManager.getDefaults().getBorder(
                "InternalFrame.paletteBorder") == frame.getBorder());
        // test propertyChangeListener;
        // ui.setPalette(false) has to be called by the listener
        frame.putClientProperty(MetalInternalFrameUI.IS_PALETTE, Boolean.FALSE);
        assertFalse("isPalette of titlePane is false", ((MetalInternalFrameTitlePane) ui
                .getNorthPane()).isPalette);
        assertTrue("changed frame border", UIManager.getDefaults().getBorder(
                "InternalFrame.border") == frame.getBorder());
    }
}
