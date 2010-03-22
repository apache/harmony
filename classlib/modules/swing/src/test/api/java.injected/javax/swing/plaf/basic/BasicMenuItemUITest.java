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
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;

public class BasicMenuItemUITest extends SwingTestCase {
    protected String prefix;

    protected BasicMenuItemUI menuItemUI;

    private Icon oldArrow;

    private Icon oldCheck;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuItemUI = new BasicMenuItemUI();
        prefix = "MenuItem.";
        oldArrow = UIManager.getIcon(prefix + "arrowIcon");
        oldCheck = UIManager.getIcon(prefix + "checkIcon");
        MenuSelectionManager.defaultManager().clearSelectedPath();
    }

    @Override
    protected void tearDown() throws Exception {
        menuItemUI = null;
        UIManager.put(prefix + "arrowIcon", oldArrow);
        UIManager.put(prefix + "checkIcon", oldCheck);
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.paint(Graphics, JComponent)'
     */
    @SuppressWarnings("deprecation")
    public void testPaint() {
        JFrame frame = new JFrame();
        JMenuItem item = new JMenuItem();
        item.setUI(menuItemUI);
        frame.getContentPane().add(item);
        frame.pack();
        frame.show();
        menuItemUI.paint(item.getGraphics(), item);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.getPreferredSize(JComponent)'
     */
    public void testGetSizes() {
        JMenuItem item = new JMenuItem() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(Font font) {
                return BasicMenuItemUITest.this.getFontMetrics(font);
            }
        };
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        UIManager.put(prefix + "border", border);
        Insets margin = new InsetsUIResource(1, 2, 3, 4);
        UIManager.put("OptionPane.margin", margin);
        menuItemUI = new BasicMenuItemUI();
        item.setUI(menuItemUI);
        assertNull(menuItemUI.getMinimumSize(item));
        assertNull(menuItemUI.getMaximumSize(item));
        assertEquals(menuItemUI.getPreferredMenuItemSize(item, menuItemUI.checkIcon,
                menuItemUI.arrowIcon, menuItemUI.defaultTextIconGap), menuItemUI
                .getPreferredSize(item));
        item.setIcon(new ImageIcon(new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB)));
        assertNull(menuItemUI.getMinimumSize(item));
        assertNull(menuItemUI.getMaximumSize(item));
        assertEquals(menuItemUI.getPreferredMenuItemSize(item, menuItemUI.checkIcon,
                menuItemUI.arrowIcon, menuItemUI.defaultTextIconGap), menuItemUI
                .getPreferredSize(item));
        item.setAccelerator(KeyStroke.getKeyStroke('a'));
        assertEquals(menuItemUI.getPreferredMenuItemSize(item, menuItemUI.checkIcon,
                menuItemUI.arrowIcon, menuItemUI.defaultTextIconGap), menuItemUI
                .getPreferredSize(item));
        try { //Regression test for HARMONY-2695
            menuItemUI.getMinimumSize(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        try { //Regression test for HARMONY-2695
            menuItemUI.getMaximumSize(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.uninstallUI(JComponent)'
     */
    public void testInstallUninstallUI() {
        JMenuItem item = new JMenuItem();
        menuItemUI.installUI(item);
        assertNotNull(item.getBorder());
        assertNotNull(SwingUtilities.getUIActionMap(item));
        assertNotNull(menuItemUI.mouseInputListener);
        assertEquals(0, item.getComponentCount());
        menuItemUI.uninstallUI(item);
        assertNull(item.getBorder());
        assertNull(SwingUtilities.getUIActionMap(item));
        assertNull(menuItemUI.mouseInputListener);
        assertEquals(0, item.getComponentCount());

        try { //Regression test for HARMONY-2704
            menuItemUI.uninstallUI(new JOptionPane());
            fail("ClassCastException should have been thrown");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.createUI(JComponent)'
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicMenuItemUI.createUI(null);
        ComponentUI ui2 = BasicMenuItemUI.createUI(null);
        assertTrue(ui1 instanceof BasicMenuItemUI);
        assertNotSame(ui1, ui2);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.installDefaults()'
     */
    public void testInstallUninstallDefaults() {
        JMenuItem item = new JMenuItem();
        item.setUI(menuItemUI);
        UIManager.getDefaults().put(prefix + "background", new ColorUIResource(Color.red));
        UIManager.getDefaults().put(prefix + "foreground", new ColorUIResource(Color.yellow));
        UIManager.getDefaults().put(prefix + "acceleratorForeground",
                new ColorUIResource(Color.cyan));
        UIManager.getDefaults().put(prefix + "acceleratorSelectionForeground",
                new ColorUIResource(Color.magenta));
        UIManager.getDefaults().put(prefix + "selectionBackground",
                new ColorUIResource(Color.green));
        UIManager.getDefaults().put(prefix + "selectionForeground",
                new ColorUIResource(Color.pink));
        UIManager.getDefaults().put(prefix + "disabledForeground",
                new ColorUIResource(Color.orange));
        Font font = new FontUIResource(item.getFont().deriveFont(100f));
        UIManager.getDefaults().put(prefix + "font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.getDefaults().put(prefix + "border", border);
        Insets margin = new InsetsUIResource(1, 2, 3, 4);
        UIManager.getDefaults().put(prefix + "margin", margin);
        Icon arrow = new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        UIManager.getDefaults().put(prefix + "arrowIcon", arrow);
        Icon check = new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        UIManager.getDefaults().put(prefix + "checkIcon", check);
        UIManager.getDefaults().put(prefix + "borderPainted", Boolean.FALSE);
        menuItemUI.defaultTextIconGap = 1000;
        menuItemUI.oldBorderPainted = true;
        menuItemUI.installDefaults();
        assertEquals(Color.red, item.getBackground());
        assertEquals(Color.yellow, item.getForeground());
        assertEquals(font, item.getFont());
        assertEquals(border, item.getBorder());
        assertEquals(margin, item.getMargin());
        assertEquals(4, menuItemUI.defaultTextIconGap);
        if (isHarmony()) {
            assertFalse(menuItemUI.oldBorderPainted);
        }
        assertEquals(Color.cyan, menuItemUI.acceleratorForeground);
        assertEquals(Color.magenta, menuItemUI.acceleratorSelectionForeground);
        assertEquals(Color.green, menuItemUI.selectionBackground);
        assertEquals(Color.pink, menuItemUI.selectionForeground);
        assertEquals(Color.orange, menuItemUI.disabledForeground);
        assertEquals(arrow, menuItemUI.arrowIcon);
        assertEquals(check, menuItemUI.checkIcon);
        menuItemUI.uninstallDefaults();
        assertNull(item.getBackground());
        assertNull(item.getForeground());
        assertNull(item.getFont());
        assertNull(item.getBorder());
        // Assertion below has been deleted because now we are compartible with
        // RI in this point. For details see HARMONY-4655
        // assertNull(item.getMargin());
        assertEquals(4, menuItemUI.defaultTextIconGap);
        if (isHarmony()) {
            assertFalse(menuItemUI.oldBorderPainted);
        }
        assertEquals(Color.cyan, menuItemUI.acceleratorForeground);
        assertEquals(Color.magenta, menuItemUI.acceleratorSelectionForeground);
        assertEquals(Color.green, menuItemUI.selectionBackground);
        assertEquals(Color.pink, menuItemUI.selectionForeground);
        assertEquals(Color.orange, menuItemUI.disabledForeground);
        assertEquals(arrow, menuItemUI.arrowIcon);
        assertEquals(check, menuItemUI.checkIcon);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.getPropertyPrefix()'
     */
    public void testGetPropertyPrefix() {
        assertEquals("MenuItem", menuItemUI.getPropertyPrefix());
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.installListeners()'
     */
    public void testInstallUninstallListeners() {
        JMenuItem item = new JMenuItem();
        menuItemUI = (BasicMenuItemUI) item.getUI();
        menuItemUI.uninstallListeners();
        assertEquals(0, item.getMenuDragMouseListeners().length);
        assertEquals(0, item.getMenuKeyListeners().length);
        assertEquals(0, item.getPropertyChangeListeners().length);
        assertEquals(0, item.getMouseMotionListeners().length);
        assertEquals(0, item.getMouseListeners().length);
        assertNull(menuItemUI.mouseInputListener);
        assertNull(menuItemUI.menuKeyListener);
        assertNull(menuItemUI.menuDragMouseListener);
        menuItemUI.menuItem = item;
        menuItemUI.installListeners();
        Object listener = menuItemUI.mouseInputListener;
        assertNotNull(menuItemUI.mouseInputListener);
        if (isHarmony()) {
            assertNull(menuItemUI.menuKeyListener);
        } else {
            assertNotNull(menuItemUI.menuKeyListener);
        }
        assertNotNull(menuItemUI.menuDragMouseListener);
        assertSame(menuItemUI.menuDragMouseListener, item.getMenuDragMouseListeners()[0]);
        if (isHarmony()) {
            assertSame(listener, item.getMouseMotionListeners()[0]);
            assertSame(listener, item.getMouseListeners()[0]);
        }
        assertNotNull(item.getFocusListeners()[0]);
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.installKeyboardActions()'
     */
    public void testInstallUninstallKeyboardActions() {
        JMenuItem item = new JMenuItem();
        item.setUI(menuItemUI);
        menuItemUI.uninstallKeyboardActions();
        assertEquals("RegisteredKeyStrokes", 0, item.getRegisteredKeyStrokes().length);
        menuItemUI.installKeyboardActions();
        assertEquals(14, item.getActionMap().allKeys().length);
        if (!isHarmony()) {
            assertEquals(1, item.getActionMap().getParent().keys().length);
        }
        menuItemUI.uninstallKeyboardActions();
        if (isHarmony()) {
            assertEquals(0, item.getActionMap().allKeys().length);
        } else {
            assertNull(item.getActionMap().allKeys());
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.createMouseInputListener(JComponent)'
     */
    public void testCreateMouseInputListener() {
        assertNotNull(menuItemUI.createMouseInputListener(null));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.createMenuDragMouseListener(JComponent)'
     */
    public void testCreateMenuDragMouseListener() {
        assertNotNull(menuItemUI.createMenuDragMouseListener(null));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.createMenuKeyListener(JComponent)'
     */
    public void testCreateMenuKeyListener() {
        if (isHarmony()) {
            assertNull(menuItemUI.createMenuKeyListener(null));
        } else {
            assertNotNull(menuItemUI.createMenuKeyListener(null));
        }
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.getPreferredMenuItemSize(JComponent, Icon, Icon, int)'
     */
    public void testGetPreferredMenuItemSize() {
        Icon icon1 = null;
        Icon icon2 = new ImageIcon(new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB));
        Icon icon3 = new ImageIcon(new BufferedImage(100, 20, BufferedImage.TYPE_INT_RGB));
        Icon icon4 = new ImageIcon(new BufferedImage(1000, 20, BufferedImage.TYPE_INT_RGB));
        JMenuItem item = new JMenuItem() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("deprecation")
            @Override
            public FontMetrics getFontMetrics(Font font) {
                return Toolkit.getDefaultToolkit().getFontMetrics(font);
            }
        };
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(10, 20, 30, 40));
        UIManager.put("MenuItem.border", border);
        Insets margin = new InsetsUIResource(1, 2, 3, 4);
        UIManager.put("OptionPane.margin", margin);
        menuItemUI = new BasicMenuItemUI();
        item.setUI(menuItemUI);
        assertEquals(new Dimension(61, 41), menuItemUI.getPreferredMenuItemSize(item, icon1,
                icon1, 0));
        assertEquals(new Dimension(71, 41), menuItemUI.getPreferredMenuItemSize(item, icon1,
                icon2, 0));
        assertEquals(new Dimension(171, 41), menuItemUI.getPreferredMenuItemSize(item, icon2,
                icon3, 0));
        assertEquals(new Dimension(211, 41), menuItemUI.getPreferredMenuItemSize(item, icon2,
                icon3, 10));
        assertEquals(new Dimension(251, 41), menuItemUI.getPreferredMenuItemSize(item, icon2,
                icon3, 20));
        item.setIcon(icon4);
        assertEquals(new Dimension(1061, 61), menuItemUI.getPreferredMenuItemSize(item, icon1,
                icon1, 0));
        assertEquals(new Dimension(1171, 61), menuItemUI.getPreferredMenuItemSize(item, icon2,
                icon3, 0));
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.getPath()'
     */
    @SuppressWarnings("deprecation")
    public void testGetPath() {
        JFrame frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("menu");
        JMenu menu2 = new JMenu("menu");
        JMenuItem item = new JMenuItem("item");
        JMenuItem item2 = new JMenuItem("item 2");
        item.setUI(menuItemUI);
        menuBar.add(menu);
        menuBar.add(menu2);
        menu.add(item);
        frame.getContentPane().add(menuBar);
        frame.pack();
        frame.show();
        assertEquals(0, menuItemUI.getPath().length);
        MenuSelectionManager manager = MenuSelectionManager.defaultManager();
        manager.setSelectedPath(new MenuElement[] { menuBar, menu, menu.getPopupMenu(), item });
        assertEquals(4, menuItemUI.getPath().length);
        assertSame(menuBar, menuItemUI.getPath()[0]);
        assertSame(menu, menuItemUI.getPath()[1]);
        assertSame(menu.getPopupMenu(), menuItemUI.getPath()[2]);
        assertSame(item, menuItemUI.getPath()[3]);
        manager
                .setSelectedPath(new MenuElement[] { menuBar, item2, menu2, menu.getPopupMenu() });
        assertEquals(5, menuItemUI.getPath().length);
        assertSame(menuBar, menuItemUI.getPath()[0]);
        assertSame(item2, menuItemUI.getPath()[1]);
        assertSame(menu2, menuItemUI.getPath()[2]);
        assertSame(menu.getPopupMenu(), menuItemUI.getPath()[3]);
        assertSame(item, menuItemUI.getPath()[4]);
        manager.setSelectedPath(new MenuElement[] { menuBar });
        assertEquals(1, menuItemUI.getPath().length);
        assertSame(item, menuItemUI.getPath()[0]);
        manager.clearSelectedPath();
        assertEquals(0, menuItemUI.getPath().length);
        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuItemUI.doClick(MenuSelectionManager)'
     */
    public void testDoClick() {
        class MyJMenuItem extends JMenuItem {
            private static final long serialVersionUID = 1L;

            public int time = -111;

            @Override
            public void doClick(final int pressTime) {
                time = pressTime;
            }
        }
        MenuSelectionManager manager = new MenuSelectionManager();
        manager.setSelectedPath(new MenuElement[] { new JMenu(), new JMenu() });
        MyJMenuItem item = new MyJMenuItem();
        item.setUI(menuItemUI);
        menuItemUI.doClick(manager);
        assertEquals(0, manager.getSelectedPath().length);
        assertEquals(0, item.time);
    }
}
