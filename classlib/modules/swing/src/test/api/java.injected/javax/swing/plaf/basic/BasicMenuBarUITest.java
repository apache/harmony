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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ContainerListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.OverlayLayout;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;

public class BasicMenuBarUITest extends SwingTestCase {
    protected BasicMenuBarUI menuBarUI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuBarUI = new BasicMenuBarUI();
    }

    @Override
    protected void tearDown() throws Exception {
        menuBarUI = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.installUI(JComponent)'
     */
    public void testInstallUninstallUI() {
        JMenuBar menu = new JMenuBar();
        menu.setUI(menuBarUI);
        LayoutManager oldManager = menu.getLayout();
        menuBarUI.uninstallUI(menu);
        menuBarUI.installUI(menu);
        assertNotNull(menu.getLayout());
        assertNotSame(oldManager, menu.getLayout());
        menuBarUI.uninstallUI(menu);
        oldManager = new OverlayLayout(menu);
        menu.setLayout(oldManager);
        menuBarUI.installUI(menu);
        assertEquals(oldManager, menu.getLayout());
    }

    public void testInstallUninstallUI2() {
        JMenuBar menu = new JMenuBar();
        menu.setUI(menuBarUI);
        menuBarUI.installUI(menu);
        assertSame(menu, menuBarUI.menuBar);
        menuBarUI.uninstallUI(menu);
        assertNull(menuBarUI.menuBar);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.createUI(JComponent)'
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicMenuBarUI.createUI(null);
        ComponentUI ui2 = BasicMenuBarUI.createUI(null);
        assertTrue(ui1 instanceof BasicMenuBarUI);
        assertNotSame(ui1, ui2);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.createChangeListener()'
     */
    public void testCreateChangeListener() {
        assertNotNull(menuBarUI.createChangeListener());
        assertSame(menuBarUI.createChangeListener(), menuBarUI.createChangeListener());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.createContainerListener()'
     */
    public void testCreateContainerListener() {
        final ContainerListener containerListener1 = menuBarUI.createContainerListener();
        final ContainerListener containerListener2 = menuBarUI.createContainerListener();
        assertNotNull(containerListener1);
        assertSame(containerListener1, containerListener2);
        assertSame(containerListener1, menuBarUI.createChangeListener());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.installDefaults()'
     */
    public void testInstallUninstallDefaults() {
        JMenuBar menuBar = new JMenuBar();
        UIManager.getDefaults().put("MenuBar.background", new ColorUIResource(Color.red));
        UIManager.getDefaults().put("MenuBar.foreground", new ColorUIResource(Color.yellow));
        UIManager.getDefaults().put("MenuBar.highlight", new ColorUIResource(Color.cyan));
        UIManager.getDefaults().put("MenuBar.shadow", new ColorUIResource(Color.magenta));
        Font font = new FontUIResource(menuBar.getFont().deriveFont(100f));
        UIManager.getDefaults().put("MenuBar.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.getDefaults().put("MenuBar.border", border);
        menuBar.setUI(menuBarUI);
        menuBarUI.installDefaults();
        assertEquals(Color.red, menuBar.getBackground());
        assertEquals(Color.yellow, menuBar.getForeground());
        assertEquals(font, menuBar.getFont());
        assertEquals(border, menuBar.getBorder());
        menuBarUI.uninstallDefaults();
        assertEquals(Color.red, menuBar.getBackground());
        assertEquals(Color.yellow, menuBar.getForeground());
        assertEquals(font, menuBar.getFont());
        assertNull(menuBar.getBorder());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.installKeyboardActions()'
     */
    public void testInstallUninstallKeyboardActions() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setUI(menuBarUI);
        menuBarUI.installKeyboardActions();
        assertEquals(1, menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys().length);
        assertEquals(1, menuBar.getActionMap().allKeys().length);
        menuBarUI.uninstallKeyboardActions();
        if (isHarmony()) {
            assertEquals(0,
                    menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys().length);
            assertEquals(0, menuBar.getActionMap().allKeys().length);
        } else {
            assertNull(menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys());
            assertNull(menuBar.getActionMap().allKeys());
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuBarUI.installListeners()'
     */
    public void testInstallUninstallListeners() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu();
        menuBar.add(menu);
        menuBarUI = (BasicMenuBarUI) menuBar.getUI();
        menuBarUI.uninstallListeners();
        assertEquals(0, menuBar.getContainerListeners().length);
        assertNull(menuBarUI.changeListener);
        assertNull(menuBarUI.containerListener);
        menuBarUI.menuBar = menuBar;
        menuBarUI.installListeners();
        assertEquals(1, menuBar.getContainerListeners().length);
        assertNotNull(menuBarUI.changeListener);
        assertNotNull(menuBarUI.containerListener);
    }
}
