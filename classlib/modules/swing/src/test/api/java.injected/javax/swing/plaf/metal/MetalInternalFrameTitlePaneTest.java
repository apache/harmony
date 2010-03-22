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

import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import javax.swing.SwingTestCase;

public class MetalInternalFrameTitlePaneTest extends SwingTestCase {
    /*
     * This class is used to access protected members
     */
    private class TestMetalInternalFrameTitlePane extends MetalInternalFrameTitlePane {
        private static final long serialVersionUID = 1L;

        public TestMetalInternalFrameTitlePane(final JInternalFrame frame) {
            super(frame);
        }

        public JMenu getWindowMenu() {
            return windowMenu;
        }

        public JMenuBar getMenuBar() {
            return menuBar;
        }

        public JButton getCloseButton() {
            return closeButton;
        }

        public Icon getCloseIcon() {
            return closeIcon;
        }

        public JButton getIconButton() {
            return iconButton;
        }

        public JButton getMaxButton() {
            return maxButton;
        }
    }

    private MetalInternalFrameTitlePane pane;

    private JInternalFrame frame;

    /*
     * Constructor for MetalInternalFrameTitlePaneTest.
     */
    public MetalInternalFrameTitlePaneTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        pane = new MetalInternalFrameTitlePane(frame);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void addNotify()
     */
    public void testAddNotify() {
        // Note: it's unclear what to test here
    }

    /*
     * Class under test for void addSubComponents()
     */
    public void testAddSubComponents() {
        frame = new JInternalFrame("", true, true, true, true);
        pane = new MetalInternalFrameTitlePane(frame);
        assertEquals(3, pane.getComponentCount());
        frame = new JInternalFrame("", true, true, true, false);
        pane = new MetalInternalFrameTitlePane(frame);
        if (isHarmony()) {
            assertEquals(2, pane.getComponentCount());
        }
    }

    /*
     * Class under test for void installDefaults()
     */
    public void testInstallDefaults() {
        pane.uninstallDefaults();
        pane.paletteTitleHeight = 0;
        pane.paletteCloseIcon = null;
        pane.installDefaults();
        assertTrue("paletteTitleHeight != 0", pane.paletteTitleHeight != 0);
        assertTrue("installed paletteCloseIcon", pane.paletteCloseIcon != null);
    }

    /*
     * Class under test for void uninstallDefaults()
     */
    public void testUninstallDefaults() {
        pane.uninstallDefaults();
        assertTrue("paletteTitleHeight != 0", pane.paletteTitleHeight != 0);
        assertTrue("didn't uninstall paletteCloseIcon", pane.paletteCloseIcon != null);
    }

    /*
     * Class under test for void createButtons()
     */
    public void testCreateButtons() {
        TestMetalInternalFrameTitlePane pane = new TestMetalInternalFrameTitlePane(frame);
        assertEquals("maxButton accessible name", "Maximize", pane.getMaxButton()
                .getAccessibleContext().getAccessibleName());
        if (isHarmony()) {
            assertFalse("maxButton's border is not painted", pane.getMaxButton()
                    .isBorderPainted());
        }
        assertEquals("iconButton accessible name", "Iconify", pane.getIconButton()
                .getAccessibleContext().getAccessibleName());
        if (isHarmony()) {
            assertFalse("iconButton's border is not painted", pane.getIconButton()
                    .isBorderPainted());
        }
        assertEquals("closeButton accessible name", "Close", pane.getCloseButton()
                .getAccessibleContext().getAccessibleName());
        if (isHarmony()) {
            assertFalse("closeButton's border is not painted", pane.getCloseButton()
                    .isBorderPainted());
        }
    }

    /*
     * Class under test for void assembleSystemMenu()
     */
    public void testAssembleSystemMenu() {
        TestMetalInternalFrameTitlePane pane = new TestMetalInternalFrameTitlePane(frame);
        pane.assembleSystemMenu();
        assertNull("windowMenu == null", pane.getWindowMenu());
        assertNull("menuBar == null", pane.getMenuBar());
    }

    /*
     * Class under test for void showSystemMenu()
     */
    public void testShowSystemMenu() {
        // does nothing
    }

    /*
     * Class under test for MetalInternalFrameTitlePane(JInternalFrame)
     */
    public void testMetalInternalFrameTitlePane() {
        pane = new MetalInternalFrameTitlePane(frame);
        assertFalse("isPalette == false", pane.isPalette);
    }

    /*
     * Class under test for void addSystemMenuItems(JMenu)
     */
    public void testAddSystemMenuItems() {
        // the tested function does nothing
        JMenu menu = new JMenu();
        pane.addSystemMenuItems(menu);
        assertEquals(0, menu.getItemCount());
    }

    /*
     * Class under test for PropertyChangeListener createPropertyChangeListener()
     */
    public void testCreatePropertyChangeListener() {
        PropertyChangeListener listener = pane.createPropertyChangeListener();
        assertTrue("!= null", listener != null);
        assertTrue("instanceof TitlePaneLayout",
                listener instanceof BasicInternalFrameTitlePane.PropertyChangeHandler);
    }

    /*
     * Class under test for LayoutManager createLayout()
     */
    public void testCreateLayout() {
        LayoutManager layout = pane.createLayout();
        assertTrue("!= null", layout != null);
        assertTrue("instanceof TitlePaneLayout",
                layout instanceof BasicInternalFrameTitlePane.TitlePaneLayout);
    }

    /*
     * Class under test for void void setPalette(boolean)
     */
    public void testSetPalette() {
        TestMetalInternalFrameTitlePane pane = new TestMetalInternalFrameTitlePane(frame);
        frame.setClosable(true);
        frame.setIconifiable(true);
        frame.setMaximizable(true);
        // test set to true
        pane.setPalette(true);
        assertTrue("isPalette is true", pane.isPalette);
        assertTrue("changed close icon",
                pane.getCloseButton().getIcon() == pane.paletteCloseIcon);
        assertTrue("1 child", pane.getComponentCount() == 1);
        // is layoutContainer called?
        // test set to false
        pane.setPalette(false);
        assertFalse("isPalette is false", pane.isPalette);
        assertTrue("changed close icon", pane.getCloseButton().getIcon() == pane.getCloseIcon());
        assertTrue("3 children", pane.getComponentCount() == 3);
    }

    public void testPaintPalette() {
        // Note: painting code, cannot test
    }

    /*
     * Class under test for void paintComponent(Graphics)
     */
    public void testPaintComponent() {
        // Note: painting code, cannot test
    }

    /*
     * Test MetalInternalFrameTitlePane.MetalTitlePaneLayout class
     */
    public void testMetalTitlePaneLayout() {
        TestMetalInternalFrameTitlePane pane = new TestMetalInternalFrameTitlePane(frame);
        pane.setSize(200, 31);
        LayoutManager layout = pane.getLayout();
        final Rectangle iconButtonBounds = new Rectangle(134, 7, 16, 16);
        final Rectangle maximizeButtonBounds = new Rectangle(156, 7, 16, 16);
        final Rectangle closeButtonBounds = new Rectangle(178, 7, 16, 16);
        // test layoutContainer(): non-iconifiable, non-maximizable, non-closable
        layout.layoutContainer(null);
        //        assertEquals("iconButton", zeroBounds,
        //                     pane.getComponent(0).getBounds());
        //        assertTrue("maximizeButton", pane.getComponent(1).getBounds().
        //                equals(zeroBounds));
        //        assertTrue("closeButton", pane.getComponent(2).getBounds().
        //                equals(zeroBounds));
        // test layoutContainer(): iconifiable, maximizable, closable
        frame.setIconifiable(true);
        frame.setMaximizable(true);
        frame.setClosable(true);
        layout.layoutContainer(pane);
        if (isHarmony()) {
            assertEquals("iconButton", iconButtonBounds, pane.getComponent(0).getBounds());
            assertEquals("maximizeButton", maximizeButtonBounds, pane.getComponent(1)
                    .getBounds());
            assertEquals("closeButton", closeButtonBounds, pane.getComponent(2).getBounds());
        }
        // test layoutContainer(): isPalette == true
        pane.setPalette(true);
        layout.layoutContainer(null);
        // these bounds can be changed in the future
        if (isHarmony()) {
            assertEquals("palette: closeButton", new Rectangle(189, 11, 8, 8), pane
                    .getComponent(0).getBounds());
        }
        // minimumLayoutSize(), preferredLayoutSize() implementations
        assertTrue("", layout.minimumLayoutSize(pane) != null);
        assertTrue("", layout.preferredLayoutSize(pane) != null);
    }
}
