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
package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.beans.PropertyVetoException;
import javax.swing.JInternalFrame;
import javax.swing.SwingTestCase;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;

public class BasicDesktopIconUITest extends SwingTestCase {
    private static boolean belongs(final Object o, final Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == o) {
                return true;
            }
        }
        return false;
    }

    private static class MyBasicDesktopIconUI extends BasicDesktopIconUI {
        public MouseInputListener mouseInputListener;

        @Override
        protected MouseInputListener createMouseInputListener() {
            mouseInputListener = super.createMouseInputListener();
            return mouseInputListener;
        }
    }

    private MyBasicDesktopIconUI ui;

    private JInternalFrame.JDesktopIcon icon;

    private JInternalFrame frame;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        icon = frame.getDesktopIcon();
        ui = new MyBasicDesktopIconUI();
        icon.setUI(ui);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public BasicDesktopIconUITest(final String name) {
        super(name);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicDesktopIconUI.createUI(frame);
        ComponentUI ui2 = BasicDesktopIconUI.createUI(frame);
        assertTrue("not null", ui1 != null);
        assertTrue("stateful", ui1 != ui2);
    }

    /*
     * Class under test for BasicDesktopIconUI()
     */
    public void testBasicDesktopIconUI() {
        // nothing to test
    }

    /*
     * Class under test for MouseInputListener createMouseInputListener()
     */
    public void testCreateMouseInputListener() {
        MouseInputListener l1 = ui.createMouseInputListener();
        MouseInputListener l2 = ui.createMouseInputListener();
        assertTrue("not null", l1 != null);
        if (isHarmony()) {
            assertSame("the same instance", l1, l2);
        }
    }

    /*
     * Class under test for void deiconize()
     */
    public void testDeiconize() {
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
        }
        assertTrue("is icon", frame.isIcon());
        ui.deiconize();
        assertFalse("deiconized", frame.isIcon());
    }

    /*
     * Class under test for Insets getInsets(JComponent)
     */
    public void testGetInsets() {
        final Insets validInsets = new Insets(5, 5, 5, 5);
        Insets insets = ui.getInsets(icon);
        assertTrue("not null", insets != null);
        assertEquals("ok", validInsets, insets);
        try { //Regression test for HARMONY-2664
            ui.getInsets(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /*
     * Class under test for Dimension getMaximumSize(JComponent)
     */
    public void testGetMaximumSize() {
        if (isHarmony()) {
            assertEquals("== minimumSize", ui.getMinimumSize(icon), ui.getMaximumSize(icon));
        }
    }

    /*
     * Class under test for Dimension getMinimumSize(JComponent)
     */
    public void testGetMinimumSize() {
        if (isHarmony()) {
            assertEquals("== preferredSize", ui.getPreferredSize(icon), ui.getMinimumSize(icon));
        }
    }

    /*
     * Class under test for Dimension getPreferredSize(JComponent)
     */
    public void testGetPreferredSize() {
        icon.setSize(ui.getPreferredSize(icon));
        icon.doLayout();
        assertEquals(ui.iconPane.getPreferredSize(), ui.iconPane.getSize());
    }

    /*
     * Class under test for void installComponents()
     */
    public void testInstallComponents() {
        int count = icon.getComponentCount();
        ui.uninstallComponents();
        assertEquals("uninstalled", count - 1, icon.getComponentCount());
        ui.installComponents();
        assertEquals("added 1 component", count, icon.getComponentCount());
        assertTrue("added iconPane", icon.isAncestorOf(ui.iconPane));
    }

    /*
     * Class under test for void uninstallComponents()
     */
    public void testUninstallComponents() {
        int count = icon.getComponentCount();
        assertTrue("added iconPane", icon.isAncestorOf(ui.iconPane));
        ui.uninstallComponents();
        assertEquals("uninstalled", count - 1, icon.getComponentCount());
        assertFalse("removed iconPane", icon.isAncestorOf(ui.iconPane));
    }

    /*
     * Class under test for void installDefaults()
     */
    public void testInstallDefaults() {
        icon.setBorder(null);
        icon.setLayout(null);
        ui.installDefaults();
        assertTrue("opaque", icon.isOpaque());
        assertTrue("border", icon.getBorder() != null);
        assertNull("layout", icon.getLayout());
    }

    /*
     * Class under test for void uninstallDefaults()
     */
    public void testUninstallDefaults() {
        ui.uninstallDefaults();
        assertNull("border", icon.getBorder());
        assertTrue("layout", icon.getLayout() instanceof BorderLayout);
    }

    /*
     * Class under test for void installListeners()
     */
    public void testInstallListeners() {
        ui.uninstallListeners();
        ui.installListeners();
        MouseInputListener listener = ui.mouseInputListener;
        assertTrue("installed mouseListener", belongs(listener, icon.getMouseListeners()));
        assertTrue("installed mouseMotionListener", belongs(listener, icon
                .getMouseMotionListeners()));
    }

    /*
     * Class under test for void uninstallListeners()
     */
    public void testUninstallListeners() {
        MouseInputListener listener = ui.createMouseInputListener();
        ui.uninstallListeners();
        assertFalse("uninstalled mouseListener", belongs(listener, icon.getMouseListeners()));
        assertFalse("uninstalled mouseMotionListener", belongs(listener, icon
                .getMouseMotionListeners()));
    }

    /*
     * Class under test for void installUI(JComponent)
     */
    public void testInstallUI() {
        assertTrue("desktopIcon", ui.desktopIcon == icon);
        assertTrue("frame", ui.frame == frame);
        assertTrue("iconPane", ui.iconPane != null);
        assertTrue("width != 0", icon.getWidth() != 0);
        assertTrue("height != 0", icon.getHeight() != 0);
    }

    /*
     * Class under test for void uninstallUI(JComponent)
     */
    public void testUninstallUI() {
        // test that no crash occur
        ui.uninstallUI(icon);
    }
}
