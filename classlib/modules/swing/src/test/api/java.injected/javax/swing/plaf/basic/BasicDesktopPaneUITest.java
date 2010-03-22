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

import java.awt.Dimension;
import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;

public class BasicDesktopPaneUITest extends SwingTestCase {
    private JDesktopPane desktop;

    private BasicDesktopPaneUI ui;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        desktop = new JDesktopPane();
        ui = new BasicDesktopPaneUI();
        desktop.setUI(ui);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for BasicDesktopPaneUITest.
     * @param name
     */
    public BasicDesktopPaneUITest(final String name) {
        super(name);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicDesktopPaneUI.createUI(desktop);
        ComponentUI ui2 = BasicDesktopPaneUI.createUI(desktop);
        assertTrue("not null", ui1 != null);
        assertTrue("stateful", ui1 != ui2);
    }

    public void testBasicDesktopPaneUI() {
    }

    public void testCreatePropertyChangeListener() {
        assertNotNull(ui.createPropertyChangeListener());
    }

    /*
     * Class under test for Dimension getMaximumSize(JComponent)
     */
    public void testGetMaximumSize() {
        assertEquals(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), ui
                .getMaximumSize(desktop));
    }

    /*
     * Class under test for Dimension getMinimumSize(JComponent)
     */
    public void testGetMinimumSize() {
        assertEquals(new Dimension(0, 0), ui.getMinimumSize(desktop));
    }

    /*
     * Class under test for Dimension getPreferredSize(JComponent)
     */
    public void testGetPreferredSize() {
        assertNull(ui.getPreferredSize(desktop));
    }

    public void testInstallDefaults() {
        desktop.setBackground(null);
        ui.installDefaults();
        assertNotNull("background", desktop.getBackground());
    }

    public void testUninstallDefaults() {
        ui.uninstallDefaults();
        if (isHarmony()) {
            assertNull("background", desktop.getBackground());
        }
    }

    public void testInstallDesktopManager() {
        desktop.setDesktopManager(null);
        ui.installDesktopManager();
        assertNotNull(desktop.getDesktopManager());
        assertEquals("installed", ui.desktopManager, desktop.getDesktopManager());
    }

    public void testUninstallDesktopManager() {
        ui.installDesktopManager();
        ui.uninstallDesktopManager();
        assertNotNull(desktop.getDesktopManager());
        DefaultDesktopManager m = new DefaultDesktopManager();
        desktop.setDesktopManager(m);
        ui.uninstallDesktopManager();
        assertSame(m, desktop.getDesktopManager());
        desktop.setDesktopManager(null);
        ui.uninstallDesktopManager();
        assertNotNull(desktop.getDesktopManager());
    }

    public void testInstallKeyboardActions() {
        assertNotNull("action map", SwingUtilities.getUIActionMap(desktop));
        assertNotNull("input map", SwingUtilities.getUIInputMap(desktop,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    }

    public void testUninstallKeyboardActions() {
        ui.uninstallKeyboardActions();
        assertNull("action map", SwingUtilities.getUIActionMap(desktop));
        assertNull("input map", SwingUtilities.getUIInputMap(desktop,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    }

    public void testInstallListeners() {
        // nothing to test
    }

    /*
     * Class under test for void installUI(JComponent)
     */
    public void testInstallUI() {
        assertEquals("desktop", desktop, ui.desktop);
    }

    /*
     * Class under test for void uninstallUI(JComponent)
     */
    public void testUninstallUI() {
        ui.uninstallUI(desktop);
        assertNull("action map", SwingUtilities.getUIActionMap(desktop));
        assertNull(desktop.getDesktopManager());
    }

    /*
     * Class under test for void paint(Graphics, JComponent)
     */
    public void testPaint() {
        // cannot test
    }

    public void testRegisterKeyboardActions() {
        // nothing to test
    }

    public void testUninstallListeners() {
        // nothing to test
    }

    public void testUnregisterKeyboardActions() {
        // nothing to test
    }
}
