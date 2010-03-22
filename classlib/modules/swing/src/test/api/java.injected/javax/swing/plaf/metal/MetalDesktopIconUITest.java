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

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

public class MetalDesktopIconUITest extends SwingTestCase {
    private TestMetalDesktopIconUI ui;

    private JInternalFrame.JDesktopIcon icon;

    private JInternalFrame frame;

    private class TestMetalDesktopIconUI extends MetalDesktopIconUI {
        public JComponent getIconPane() {
            return iconPane;
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        icon = frame.getDesktopIcon();
        ui = new TestMetalDesktopIconUI();
        ui.installUI(icon);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for MetalDesktopIconUITest.
     * @param name
     */
    public MetalDesktopIconUITest(final String name) {
        super(name);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui1 = MetalDesktopIconUI.createUI(frame);
        ComponentUI ui2 = MetalDesktopIconUI.createUI(frame);
        assertTrue("not null", ui1 != null);
        assertTrue("statefull", ui1 != ui2);
    }

    /*
     * Class under test for Dimension getMaximumSize(JComponent)
     */
    public void testGetMaximumSize() {
        assertEquals("== minimumSize (JRockit fails)", ui.getMinimumSize(icon), ui
                .getMaximumSize(icon));
    }

    /*
     * Class under test for Dimension getMinimumSize(JComponent)
     */
    public void testGetMinimumSize() {
        assertEquals("== preferredSize (JRockit fails)", ui.getPreferredSize(icon), ui
                .getMinimumSize(icon));
    }

    /*
     * Class under test for Dimension getPreferredSize(JComponent)
     */
    public void testGetPreferredSize() {
        Dimension size = ui.getPreferredSize(icon);
        int desktopIconWidth = UIManager.getInt("DesktopIcon.width");
        assertEquals("width ok", desktopIconWidth, size.width);
        icon.setSize(ui.getPreferredSize(icon));
        icon.doLayout();
        if (isHarmony()) {
            assertEquals("height ok", ui.getIconPane().getPreferredSize().height, ui
                    .getIconPane().getSize().height);
        }
    }

    public void testInstallComponents() {
        int count = icon.getComponentCount();
        ui.uninstallComponents();
        assertEquals("uninstalled", count - 2, icon.getComponentCount());
        ui.installComponents();
        assertEquals("added 2 component", count, icon.getComponentCount());
        if (isHarmony()) {
            assertTrue("added iconPane", icon.isAncestorOf(ui.getIconPane()));
        }
    }

    public void testUninstallComponents() {
        int count = icon.getComponentCount();
        if (isHarmony()) {
            assertTrue("added iconPane", icon.isAncestorOf(ui.getIconPane()));
        }
        ui.uninstallComponents();
        assertEquals("uninstalled", count - 2, icon.getComponentCount());
        if (isHarmony()) {
            assertFalse("removed iconPane", icon.isAncestorOf(ui.getIconPane()));
        }
    }

    public void testInstallDefaults() {
        icon.setBackground(null);
        icon.setForeground(null);
        icon.setFont(null);
        ui.installDefaults();
        assertTrue("opaque", icon.isOpaque());
        assertNotNull(icon.getBackground());
        assertNotNull(icon.getForeground());
        assertNotNull(icon.getFont());
    }

    public void testInstallListeners() {
        // nothing to test
    }

    public void testUninstallListeners() {
        // nothing to test
    }

    public void testMetalDesktopIconUI() {
        // nothing to test
    }
}
