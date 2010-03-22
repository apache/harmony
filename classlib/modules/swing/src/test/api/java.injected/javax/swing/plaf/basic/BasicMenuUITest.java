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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;

public class BasicMenuUITest extends SwingTestCase {
    protected BasicMenuUI menuUI;

    private Icon oldArrow;

    private Icon oldCheck;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuUI = new BasicMenuUI();
        oldArrow = UIManager.getIcon("Menu.arrowIcon");
        oldCheck = UIManager.getIcon("Menu.checkIcon");
    }

    @Override
    protected void tearDown() throws Exception {
        menuUI = null;
        UIManager.put("Menu.arrowIcon", oldArrow);
        UIManager.put("Menu.checkIcon", oldCheck);
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.getMaximumSize(JComponent)'
     */
    public void testGetMaximumSize() {
        JMenuBar bar = new JMenuBar();
        JMenu menu1 = new JMenu();
        menu1.setUI(menuUI);
        assertNull(menuUI.getMaximumSize(menu1));
        bar.add(menu1);
        assertEquals(menuUI.getPreferredSize(menu1).width, menuUI.getMaximumSize(menu1).width);
        assertEquals(Short.MAX_VALUE, menuUI.getMaximumSize(menu1).height);
        menu1.setPreferredSize(new Dimension(1000, 1000));
        assertEquals(1000, menuUI.getMaximumSize(menu1).width);
        assertEquals(Short.MAX_VALUE, menuUI.getMaximumSize(menu1).height);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new BasicMenuUI().getMaximumSize(null);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override // Regression for HARMONY-2663
            public void exceptionalAction() throws Exception {
                new BasicMenuUI().getMaximumSize(new JOptionPane());
            }
        });
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createUI(JComponent)'
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicMenuUI.createUI(null);
        ComponentUI ui2 = BasicMenuUI.createUI(null);
        assertTrue(ui1 instanceof BasicMenuUI);
        assertNotSame(ui1, ui2);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.getPropertyPrefix()'
     */
    public void testGetPropertyPrefix() {
        assertEquals("Menu", menuUI.getPropertyPrefix());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.installDefaults()'
     */
    public void testInstallUninstallDefaults() {
        JMenu menu = new JMenu();
        UIManager.getDefaults().put("Menu.background", new ColorUIResource(Color.red));
        UIManager.getDefaults().put("Menu.foreground", new ColorUIResource(Color.yellow));
        UIManager.getDefaults().put("Menu.acceleratorForeground",
                new ColorUIResource(Color.cyan));
        UIManager.getDefaults().put("Menu.acceleratorSelectionForeground",
                new ColorUIResource(Color.magenta));
        UIManager.getDefaults().put("Menu.selectionBackground",
                new ColorUIResource(Color.green));
        UIManager.getDefaults()
                .put("Menu.selectionForeground", new ColorUIResource(Color.pink));
        UIManager.getDefaults().put("Menu.disabledForeground",
                new ColorUIResource(Color.orange));
        Font font = new FontUIResource(menu.getFont().deriveFont(100f));
        UIManager.getDefaults().put("Menu.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.getDefaults().put("Menu.border", border);
        Insets margin = new InsetsUIResource(1, 2, 3, 4);
        UIManager.getDefaults().put("Menu.margin", margin);
        Icon arrow = new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        UIManager.getDefaults().put("Menu.arrowIcon", arrow);
        Icon check = new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        UIManager.getDefaults().put("Menu.checkIcon", check);
        menu.setUI(menuUI);
        menuUI.installDefaults();
        assertEquals(Color.red, menu.getBackground());
        assertEquals(Color.yellow, menu.getForeground());
        assertEquals(font, menu.getFont());
        assertEquals(border, menu.getBorder());
        assertEquals(margin, menu.getMargin());
        assertEquals(4, menuUI.defaultTextIconGap);
        assertTrue(menuUI.oldBorderPainted);
        assertEquals(Color.cyan, menuUI.acceleratorForeground);
        assertEquals(Color.magenta, menuUI.acceleratorSelectionForeground);
        assertEquals(Color.green, menuUI.selectionBackground);
        assertEquals(Color.pink, menuUI.selectionForeground);
        assertEquals(Color.orange, menuUI.disabledForeground);
        assertEquals(arrow, menuUI.arrowIcon);
        assertEquals(check, menuUI.checkIcon);
        if (isHarmony()) {
            assertFalse(menu.isOpaque());
        }
        menuUI.uninstallDefaults();
        assertNull(menu.getBackground());
        assertNull(menu.getForeground());
        assertNull(menu.getFont());
        assertNull(menu.getBorder());
        // Assertion below has been deleted because now we are compartible with
        // RI in this point. For details see HARMONY-4655
        // assertNull(menu.getMargin());
        assertEquals(4, menuUI.defaultTextIconGap);
        assertTrue(menuUI.oldBorderPainted);
        assertEquals(Color.cyan, menuUI.acceleratorForeground);
        assertEquals(Color.magenta, menuUI.acceleratorSelectionForeground);
        assertEquals(Color.green, menuUI.selectionBackground);
        assertEquals(Color.pink, menuUI.selectionForeground);
        assertEquals(Color.orange, menuUI.disabledForeground);
        assertEquals(arrow, menuUI.arrowIcon);
        assertEquals(check, menuUI.checkIcon);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.installListeners()'
     */
    public void testInstallUninstallListeners() {
        JMenu menu = new JMenu();
        menuUI = (BasicMenuUI) menu.getUI();
        menuUI.uninstallListeners();
        assertEquals(0, menu.getMenuDragMouseListeners().length);
        assertEquals(0, menu.getMenuKeyListeners().length);
        assertEquals(0, menu.getPropertyChangeListeners().length);
        assertEquals(0, menu.getMouseMotionListeners().length);
        assertEquals(0, menu.getMouseListeners().length);
        assertEquals(0, menu.getChangeListeners().length);
        assertNull(menuUI.mouseInputListener);
        assertNull(menuUI.menuKeyListener);
        assertNull(menuUI.menuDragMouseListener);
        menuUI.menuItem = menu;
        menuUI.installListeners();
        assertNull(menuUI.changeListener);
        assertNull(menuUI.menuListener);
        assertNotNull(menuUI.propertyChangeListener);
        assertNotNull(menuUI.mouseInputListener);
        if (!isHarmony()) {
            assertNotNull(menuUI.menuKeyListener);
        }
        assertNotNull(menuUI.menuDragMouseListener);
        assertSame(menuUI.menuDragMouseListener, menu.getMenuDragMouseListeners()[0]);
        if (!isHarmony()) {
            assertSame(menuUI.menuKeyListener, menu.getMenuKeyListeners()[0]);
        }
        assertSame(menuUI.propertyChangeListener, menu.getPropertyChangeListeners()[0]);
        assertSame(menuUI.mouseInputListener, menu.getMouseMotionListeners()[0]);
        assertSame(menuUI.mouseInputListener, menu.getMouseListeners()[0]);
        menuUI.uninstallListeners();
        assertNull(menuUI.propertyChangeListener);
        assertNull(menuUI.mouseInputListener);
        assertNull(menuUI.menuKeyListener);
        assertNull(menuUI.menuDragMouseListener);
        assertEquals(0, menu.getMenuDragMouseListeners().length);
        assertEquals(0, menu.getMenuKeyListeners().length);
        assertEquals(0, menu.getPropertyChangeListeners().length);
        assertEquals(0, menu.getMouseMotionListeners().length);
        assertEquals(0, menu.getMouseListeners().length);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.installKeyboardActions()'
     */
    public void testInstallUninstallKeyboardActions() {
        JMenu menu = new JMenu();
        menu.setUI(menuUI);
        menuUI.uninstallKeyboardActions();
        assertEquals("RegisteredKeyStrokes", 0, menu.getRegisteredKeyStrokes().length);
        menuUI.installKeyboardActions();
        if (isHarmony()) {
            assertEquals(14, menu.getActionMap().allKeys().length);
            assertEquals(1, menu.getActionMap().getParent().keys().length);
        } else {
            assertEquals(15, menu.getActionMap().allKeys().length);
            assertEquals(2, menu.getActionMap().getParent().keys().length);
        }
        menuUI.uninstallKeyboardActions();
        if (isHarmony()) {
            assertEquals(0, menu.getActionMap().allKeys().length);
        } else {
            assertNull(menu.getActionMap().allKeys());
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createMouseInputListener(JComponent)'
     */
    public void testCreateMouseInputListener() {
        assertNotNull(menuUI.createMouseInputListener(null));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createMenuDragMouseListener(JComponent)'
     */
    public void testCreateMenuDragMouseListener() {
        assertNotNull(menuUI.createMenuDragMouseListener(null));
        if (isHarmony()) {
            assertSame(menuUI.createMenuDragMouseListener(null), menuUI
                    .createMenuDragMouseListener(null));
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createMenuKeyListener(JComponent)'
     */
    public void testCreateMenuKeyListener() {
        if (isHarmony()) {
            assertNull(menuUI.createMenuKeyListener(null));
            assertSame(menuUI.createMenuKeyListener(null), menuUI.createMenuKeyListener(null));
        } else {
            assertNotNull(menuUI.createMenuKeyListener(null));
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.BasicMenuUI()'
     */
    public void testBasicMenuUI() {
        assertNull(menuUI.changeListener);
        assertNull(menuUI.menuListener);
        assertNull(menuUI.propertyChangeListener);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createChangeListener(JComponent)'
     */
    public void testCreateChangeListener() {
        assertNull(menuUI.createChangeListener(null));
        assertNull(menuUI.createChangeListener(new JMenu()));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createMenuListener(JComponent)'
     */
    public void testCreateMenuListener() {
        if (!isHarmony()) {
            return;
        }
        // Updated for regression of HARMONY-2663
        assertNull(menuUI.createMenuListener(null));
        assertNull(menuUI.createMenuListener(new JMenu()));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.createPropertyChangeListener(JComponent)'
     */
    public void testCreatePropertyChangeListener() {
        assertNotNull(menuUI.createPropertyChangeListener(null));
    }

    public void testInstallUninstallUI() {
        JMenu item = new JMenu();
        menuUI.installUI(item);
        assertNotNull(item.getBorder());
        assertNotNull(SwingUtilities.getUIActionMap(item));
        assertNotNull(menuUI.mouseInputListener);
        assertEquals(0, item.getComponentCount());
        menuUI.uninstallUI(item);
        assertNull(item.getBorder());
        assertNull(SwingUtilities.getUIActionMap(item));
        assertNull(menuUI.mouseInputListener);
        assertEquals(0, item.getComponentCount());
    }
}
