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
package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import javax.swing.JPopupMenuTest.MyAction;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

@SuppressWarnings("serial")
public class JMenuTest extends JMenuItemTest {
    class ConcreteMenuListener implements MenuListener {
        public Object event;

        public Object src;

        private final boolean debugOut;

        ConcreteMenuListener() {
            debugOut = false;
        }

        ConcreteMenuListener(boolean debugOut) {
            this.debugOut = debugOut;
        }

        public void menuCanceled(MenuEvent e) {
            event = "canceled";
            src = e.getSource();
            if (debugOut) {
                System.out.println(event);
            }
        }

        public void menuDeselected(MenuEvent e) {
            event = "deselected";
            src = e.getSource();
            if (debugOut) {
                System.out.println(event);
            }
        }

        public void menuSelected(MenuEvent e) {
            event = "selected";
            src = e.getSource();
            if (debugOut) {
                System.out.println(event);
            }
        }
    };

    protected JMenu menu;

    private int menuOffsetX;

    private int menuOffsetY;

    private int submenuOffsetX;

    private int submenuOffsetY;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menu = new JMenu();
        menuItem = menu;
        button = menuItem;
        menuOffsetX = UIManager.getInt("Menu.menuPopupOffsetX");
        menuOffsetY = UIManager.getInt("Menu.menuPopupOffsetY");
        submenuOffsetX = UIManager.getInt("Menu.submenuPopupOffsetX");
        submenuOffsetY = UIManager.getInt("Menu.submenuPopupOffsetY");
    }

    @Override
    protected void tearDown() throws Exception {
        UIManager.put("Menu.menuPopupOffsetX", new Integer(menuOffsetX));
        UIManager.put("Menu.menuPopupOffsetY", new Integer(menuOffsetY));
        UIManager.put("Menu.submenuPopupOffsetX", new Integer(submenuOffsetX));
        UIManager.put("Menu.submenuPopupOffsetY", new Integer(submenuOffsetY));
        menu = null;
        super.tearDown();
    }

    @Override
    public void testJMenuItem() {
    }

    /*
     * Test method for 'javax.swing.JMenu.JMenu()'
     */
    public void testJMenu() {
        assertTrue("default buttonModel ", button.getModel() instanceof DefaultButtonModel);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", "", button.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertTrue(menuItem.isFocusable());
        assertEquals(menu, menuItem.getComponent());
        assertEquals(0, menu.getItemCount());
        assertEquals(0, menu.getMenuComponentCount());
        assertNotNull(menu.getPopupMenu());
        assertEquals(1, menu.getSubElements().length);
        assertEquals(menu.getPopupMenu(), menu.getSubElements()[0]);
        assertFalse(menu.isTopLevelMenu());
        assertFalse(menu.isSelected());
        assertEquals(200, menu.getDelay());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenu.JMenu(String)'
     */
    public void testJMenuString() {
        String text = "texttext";
        button = menuItem = menu = new JMenu(text);
        assertTrue("default buttonModel ", button.getModel() instanceof DefaultButtonModel);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", text, button.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertEquals(menu, menuItem.getComponent());
        assertEquals(0, menu.getItemCount());
        assertEquals(0, menu.getMenuComponentCount());
        assertNotNull(menu.getPopupMenu());
        assertEquals(1, menu.getSubElements().length);
        assertEquals(menu.getPopupMenu(), menu.getSubElements()[0]);
        assertFalse(menu.isTopLevelMenu());
        assertFalse(menu.isSelected());
        assertTrue(menuItem.isFocusable());
        assertEquals(200, menu.getDelay());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
        button = menuItem = menu = new JMenu((String) null);
        assertEquals("text ", "", button.getText());
    }

    /*
     * Test method for 'javax.swing.JMenu.JMenu(String, boolean)'
     */
    public void testJMenuStringBoolean() {
        String text = "texttext";
        button = menuItem = menu = new JMenu(text, true);
        assertTrue("default buttonModel ", button.getModel() instanceof DefaultButtonModel);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", text, button.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertEquals(menu, menuItem.getComponent());
        assertEquals(0, menu.getItemCount());
        assertEquals(0, menu.getMenuComponentCount());
        assertNotNull(menu.getPopupMenu());
        assertEquals(1, menu.getSubElements().length);
        assertEquals(menu.getPopupMenu(), menu.getSubElements()[0]);
        assertFalse(menu.isTopLevelMenu());
        assertFalse(menu.isSelected());
        assertTrue(menuItem.isFocusable());
        assertEquals(200, menu.getDelay());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
        button = menuItem = menu = new JMenu((String) null, false);
        assertEquals("text ", "", button.getText());
    }

    /*
     * Test method for 'javax.swing.JMenu.JMenu(Action)'
     */
    public void testJMenuAction() {
        final String command = "dnammoc";
        class MyAction extends AbstractAction {
            public MyAction(final String text, final Icon icon) {
                super(text, icon);
                putValue(Action.ACTION_COMMAND_KEY, command);
            }

            public void actionPerformed(final ActionEvent e) {
            }
        }
        ;
        Icon icon = createNewIcon();
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        button = menuItem = menu = new JMenu(action);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals("action", action, menuItem.getAction());
        assertEquals("command ", command, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
        assertNull("accelerator ", menuItem.getAccelerator());
        assertTrue(menuItem.isFocusable());
        assertEquals(200, menu.getDelay());
        action.setEnabled(false);
        button = menuItem = menu = new JMenu(action);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals("action", action, menuItem.getAction());
        assertEquals("command ", command, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertFalse("enabled ", menuItem.isEnabled());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        button = menuItem = menu = new JMenu((Action) null);
        assertNull("icon ", menuItem.getIcon());
        assertNull("text ", menuItem.getText());
        assertNull("action", menuItem.getAction());
        assertNull("command ", menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
        assertEquals(menu, menuItem.getComponent());
        assertEquals(0, menu.getItemCount());
        assertEquals(0, menu.getMenuComponentCount());
        assertNotNull(menu.getPopupMenu());
        assertEquals(1, menu.getSubElements().length);
        assertEquals(menu.getPopupMenu(), menu.getSubElements()[0]);
        assertFalse(menu.isTopLevelMenu());
        assertFalse(menu.isSelected());
        assertTrue("menuItem model is of the proper type",
                menuItem.getModel() instanceof DefaultButtonModel);
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenu.getAccessibleContext()'
     */
    @Override
    public void testGetAccessibleContext() {
        boolean assertedValue = (menuItem.getAccessibleContext() != null && menuItem
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JMenu$AccessibleJMenu"));
        assertTrue("AccessibleContext created properly ", assertedValue);
    }

    /*
     * Test method for 'javax.swing.JMenu.applyComponentOrientation(ComponentOrientation)'
     */
    public void testApplyComponentOrientation() {
        JMenuItem item = new JMenuItem();
        menu.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertTrue(menu.getPopupMenu().getComponentOrientation().isLeftToRight());
        menu.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertFalse(menu.getPopupMenu().getComponentOrientation().isLeftToRight());
        menu.add(item);
        menu.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertFalse(item.getComponentOrientation().isLeftToRight());
    }

    /*
     * Test method for 'javax.swing.JMenu.setComponentOrientation(ComponentOrientation)'
     */
    public void testSetComponentOrientation() {
        JMenuItem item = new JMenuItem();
        menu.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertTrue(menu.getPopupMenu().getComponentOrientation().isLeftToRight());
        menu.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertFalse(menu.getPopupMenu().getComponentOrientation().isLeftToRight());
        menu.add(item);
        menu.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertTrue(item.getComponentOrientation().isLeftToRight());
    }

    /*
     * Test method for 'javax.swing.JMenu.remove(int)'
     */
    public void testRemoveInt() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        menu.add(item1);
        menu.add(item2);
        assertEquals(2, menu.getMenuComponentCount());
        assertSame(item1, menu.getMenuComponent(0));
        assertSame(item2, menu.getMenuComponent(1));
        try {
            menu.remove(5);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            menu.remove(-5);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        menu.remove(0);
        assertEquals(1, menu.getMenuComponentCount());
        assertSame(item2, menu.getMenuComponent(0));
    }

    /*
     * Test method for 'javax.swing.JMenu.remove(Component)'
     */
    public void testRemoveComponent() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        menu.add(item1);
        menu.add(item2);
        assertEquals(2, menu.getMenuComponentCount());
        assertSame(item1, menu.getMenuComponent(0));
        assertSame(item2, menu.getMenuComponent(1));
        menu.remove(new JButton());
        assertEquals(2, menu.getMenuComponentCount());
        menu.remove((Component) item1);
        assertEquals(1, menu.getMenuComponentCount());
        assertSame(item2, menu.getMenuComponent(0));
    }

    /*
     * Test method for 'javax.swing.JMenu.remove(JMenuItem)'
     */
    public void testRemoveJMenuItem() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        menu.add(item1);
        menu.add(item2);
        assertEquals(2, menu.getMenuComponentCount());
        assertSame(item1, menu.getMenuComponent(0));
        assertSame(item2, menu.getMenuComponent(1));
        menu.remove(item1);
        assertEquals(1, menu.getMenuComponentCount());
        assertSame(item2, menu.getMenuComponent(0));
        menu.remove(item1);
        assertEquals(1, menu.getMenuComponentCount());
        assertSame(item2, menu.getMenuComponent(0));
    }

    /*
     * Test method for 'javax.swing.JMenu.removeAll()'
     */
    public void testRemoveAll() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        assertEquals(0, menu.getMenuComponentCount());
        menu.removeAll();
        assertEquals(0, menu.getMenuComponentCount());
        menu.add(item1);
        menu.add(item2);
        assertEquals(2, menu.getMenuComponentCount());
        menu.removeAll();
        assertEquals(0, menu.getMenuComponentCount());
    }

    /*
     * Test method for 'javax.swing.JMenu.getUIClassID()'
     */
    @Override
    public void testGetUIClassID() {
        assertEquals("MenuUI", menu.getUIClassID());
    }

    /*
     * Test method for 'javax.swing.JMenu.doClick(int)'
     */
    @Override
    public void testDoClick() {
    }

    /*
     * Test method for 'javax.swing.JMenu.doClick(int)'
     */
    public void testDoClickInt() {
        JFrame frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        frame.getContentPane().add(menuBar);
        JMenu menu1 = new JMenu();
        JMenu menu2 = new JMenu();
        menuBar.add(menu2);
        menu2.add(menu1);
        menu1.add(menu);
        assertFalse(menu.isSelected());
        menu.doClick(0);
        assertTrue(menu.isSelected());
        assertTrue(menu1.isSelected());
        assertTrue(menu2.isSelected());
        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.JMenu.setSelected(boolean)'
     */
    public void testSetIsSelected() {
        ConcreteMenuListener listener = new ConcreteMenuListener();
        menu.addMenuListener(listener);
        menu.setSelected(true);
        assertTrue(menu.getModel().isSelected());
        assertTrue(menu.isSelected());
        assertEquals("selected", listener.event);
        listener.event = null;
        menu.setSelected(false);
        assertFalse(menu.getModel().isSelected());
        assertFalse(menu.isSelected());
        assertEquals("deselected", listener.event);
        listener.event = null;
    }

    /*
     * Test method for 'javax.swing.JMenu.setAccelerator(KeyStroke)'
     */
    @Override
    public void testSetAccelerator() {
        try {
            menu.setAccelerator(null);
            fail("no exception has been thrown");
        } catch (Error e) {
        }
    }

    /*
     * Test method for 'javax.swing.JMenu.getComponent()'
     */
    @Override
    public void testGetComponent() {
        assertSame(menu, menu.getComponent());
    }

    /*
     * Test method for 'javax.swing.JMenu.getSubElements()'
     */
    @Override
    public void testGetSubElements() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JMenuItem item3 = new JMenuItem();
        JMenuItem item4 = new JMenuItem();
        if (!isHarmony()) {
            assertEquals(0, menu.getSubElements().length);
        } else {
            assertEquals(1, menu.getSubElements().length);
            assertTrue(menu.getSubElements()[0] instanceof JPopupMenu);
        }
        menu.add(item1);
        assertEquals(1, menu.getSubElements().length);
        assertTrue(menu.getSubElements()[0] instanceof JPopupMenu);
        menu.add(item2);
        assertEquals(1, menu.getSubElements().length);
        assertTrue(menu.getSubElements()[0] instanceof JPopupMenu);
        menu.addSeparator();
        assertEquals(1, menu.getSubElements().length);
        assertTrue(menu.getSubElements()[0] instanceof JPopupMenu);
        JButton button = new JButton();
        menu.add(button, 0);
        assertEquals(1, menu.getSubElements().length);
        assertTrue(menu.getSubElements()[0] instanceof JPopupMenu);
        JMenu subMenu = new JMenu();
        subMenu.add(item3);
        subMenu.add(item4);
        menu.add(subMenu);
        assertEquals(1, menu.getSubElements().length);
        assertTrue(menu.getSubElements()[0] instanceof JPopupMenu);
    }

    /*
     * Test method for 'javax.swing.JMenu.menuSelectionChanged(boolean)'
     */
    @Override
    public void testMenuSelectionChanged() {
        ConcreteMenuListener listener = new ConcreteMenuListener();
        menu.addMenuListener(listener);
        menu.menuSelectionChanged(true);
        assertEquals("selected", listener.event);
        assertTrue(menu.getModel().isSelected());
        assertTrue(menu.isSelected());
        listener.event = null;
        menu.menuSelectionChanged(false);
        assertEquals("deselected", listener.event);
        assertFalse(menu.getModel().isSelected());
        assertFalse(menu.isSelected());
        listener.event = null;
    }

    /*
     * Test method for 'javax.swing.JMenu.add(Action)'
     */
    public void testAddAction() {
        Icon icon = createNewIcon();
        String text = "texttext";
        Action action = new JPopupMenuTest.MyAction(text, icon);
        JMenuItem menuItem = menu.add(action);
        assertEquals(1, menu.getItemCount());
        assertSame(menuItem, menu.getItem(0));
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertSame(action, menuItem.getAction());
    }

    /*
     * Test method for 'javax.swing.JMenu.add(String)'
     */
    public void testAddString() {
        String text1 = "text";
        String text2 = "texttext";
        JMenuItem item1 = menu.add(text1);
        assertEquals(text1, item1.getText());
        assertNull(item1.getIcon());
        assertEquals(1, menu.getItemCount());
        assertSame(item1, menu.getItem(0));
        JMenuItem item2 = menu.add(text2);
        assertEquals(text2, item2.getText());
        assertNull(item2.getIcon());
        assertEquals(2, menu.getItemCount());
        assertSame(item1, menu.getItem(0));
        assertSame(item2, menu.getItem(1));
        assertNotSame(item1, item2);
    }

    /*
     * Test method for 'javax.swing.JMenu.add(JMenuItem)'
     */
    public void testAddJMenuItem() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JPopupMenu popup = menu.getPopupMenu();
        JMenuItem item = menu.add(item1);
        assertEquals(0, menu.getComponentCount());
        assertEquals(1, menu.getItemCount());
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item, item1);
        item = menu.add(item2);
        assertEquals(0, menu.getComponentCount());
        assertEquals(2, menu.getItemCount());
        assertEquals(2, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
        assertSame(item, item2);
    }

    /*
     * Test method for 'javax.swing.JMenu.add(Component)'
     */
    public void testAddComponent() {
        Component item1 = new JMenuItem();
        Component item2 = new JMenuItem();
        Component item3 = new JButton();
        JPopupMenu popup = menu.getPopupMenu();
        Component item = menu.add(item1);
        assertEquals(0, menu.getComponentCount());
        assertEquals(1, menu.getItemCount());
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item, item1);
        item = menu.add(item2);
        assertEquals(0, menu.getComponentCount());
        assertEquals(2, menu.getItemCount());
        assertEquals(2, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
        assertSame(item, item2);
        item = menu.add(item3);
        assertEquals(0, menu.getComponentCount());
        assertEquals(3, menu.getItemCount());
        assertEquals(3, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
        assertSame(item3, popup.getComponent(2));
        assertSame(item, item3);
    }

    /*
     * Test method for 'javax.swing.JMenu.add(Component, int)'
     */
    public void testAddComponentInt() {
        Component item1 = new JMenuItem();
        Component item2 = new JMenuItem();
        Component item3 = new JButton();
        JPopupMenu popup = menu.getPopupMenu();
        Component item = menu.add(item1, 0);
        assertEquals(0, menu.getComponentCount());
        assertEquals(1, menu.getItemCount());
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item, item1);
        item = menu.add(item2, 1);
        assertEquals(0, menu.getComponentCount());
        assertEquals(2, menu.getItemCount());
        assertEquals(2, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
        assertSame(item, item2);
        item = menu.add(item3, -1);
        assertEquals(0, menu.getComponentCount());
        assertEquals(3, menu.getItemCount());
        assertEquals(3, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
        assertSame(item3, popup.getComponent(2));
        assertSame(item, item3);
    }

    /*
     * Test method for 'javax.swing.JMenu.addSeparator()'
     */
    public void testAddSeparator() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JPopupMenu popup = menu.getPopupMenu();
        menu.add(item1);
        assertEquals(1, menu.getItemCount());
        assertSame(item1, menu.getItem(0));
        menu.addSeparator();
        assertEquals(2, menu.getItemCount());
        assertTrue(popup.getComponent(1) instanceof JPopupMenu.Separator);
        menu.add(item2);
        assertEquals(3, menu.getItemCount());
        assertSame(item1, menu.getItem(0));
        assertNull(menu.getItem(1));
        assertSame(item2, menu.getItem(2));
        menu.addSeparator();
        assertEquals(4, menu.getItemCount());
        assertNull(menu.getItem(3));
        assertTrue(popup.getComponent(3) instanceof JPopupMenu.Separator);
        assertNotSame(popup.getComponent(1), popup.getComponent(3));
    }

    /*
     * Test method for 'javax.swing.JMenu.addMenuListener(MenuListener)'
     */
    public void testAddGetRemoveMenuListener() {
        MenuListener listener1 = new ConcreteMenuListener();
        MenuListener listener2 = new ConcreteMenuListener();
        MenuListener listener3 = new ConcreteMenuListener();
        EventListener[] listenersArray = null;
        listenersArray = menu.getMenuListeners();
        int initialValue = listenersArray.length;
        menu.addMenuListener(listener1);
        menu.addMenuListener(listener2);
        menu.addMenuListener(listener2);
        listenersArray = menu.getMenuListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        menu.removeMenuListener(listener1);
        menu.addMenuListener(listener3);
        menu.addMenuListener(listener3);
        listenersArray = menu.getMenuListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        menu.removeMenuListener(listener3);
        menu.removeMenuListener(listener3);
        listenersArray = menu.getMenuListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        menu.removeMenuListener(listener2);
        menu.removeMenuListener(listener2);
        listenersArray = menu.getMenuListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.JMenu.createActionChangeListener(JMenuItem)'
     */
    public void testCreateActionChangeListener() {
        String text1 = "text";
        String text2 = "texttext";
        String text3 = "texttexttext";
        JMenuItem item1 = new JMenuItem(text2);
        AbstractAction action1 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
            }
        };
        AbstractAction action2 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
            }
        };
        JMenuItem item2 = new JMenuItem(action2);
        menu.setAction(action1);
        PropertyChangeListener l1 = menu.createActionChangeListener(item1);
        PropertyChangeListener l2 = menu.createActionChangeListener(item2);
        assertNotNull(l1);
        assertNotNull(l2);
        assertNotSame(l1, l2);
        action1.putValue(Action.NAME, text1);
        assertEquals(text1, menu.getText());
        assertEquals(text2, item1.getText());
        action1.addPropertyChangeListener(l1);
        action1.putValue(Action.NAME, text3);
        assertEquals(text3, item1.getText());
        action2.putValue(Action.NAME, text2);
        assertEquals(text2, item2.getText());
    }

    /*
     * Test method for 'javax.swing.JMenu.createActionComponent(Action)'
     */
    public void testCreateActionComponent() {
        final String command = "dnammoc";
        class MyAction extends AbstractAction {
            public MyAction(final String text, final Icon icon) {
                super(text, icon);
                putValue(Action.ACTION_COMMAND_KEY, command);
            }

            public void actionPerformed(final ActionEvent e) {
            }
        }
        ;
        Icon icon = createNewIcon();
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        JMenuItem menuItem1 = menu.createActionComponent(action);
        assertEquals("icon ", icon, menuItem1.getIcon());
        assertEquals("text ", text, menuItem1.getText());
        assertNull("action", menuItem1.getAction());
        assertEquals("command ", text, menuItem1.getActionCommand());
        assertFalse("selected ", menuItem1.isSelected());
        assertTrue("enabled ", menuItem1.isEnabled());
        assertNull("accelerator ", menuItem1.getAccelerator());
        JMenuItem menuItem2 = menu.createActionComponent(action);
        assertNotSame(menuItem1, menuItem2);
        assertEquals("icon ", icon, menuItem2.getIcon());
        assertEquals("text ", text, menuItem2.getText());
        assertNull("action", menuItem1.getAction());
        assertEquals("command ", text, menuItem2.getActionCommand());
        assertFalse("selected ", menuItem2.isSelected());
        assertTrue("enabled ", menuItem2.isEnabled());
        assertNull("accelerator ", menuItem2.getAccelerator());
    }

    /*
     * Test method for 'javax.swing.JMenu.createWinListener(JPopupMenu)'
     */
    public void testCreateWinListener() {
        JPopupMenu pupop = new JPopupMenu();
        JMenu.WinListener listener1 = menu.createWinListener(pupop);
        JMenu.WinListener listener2 = menu.createWinListener(pupop);
        assertNotNull(listener1);
        assertNotNull(listener2);
        assertNotSame(listener1, listener2);
    }

    /*
     * Test method for 'javax.swing.JMenu.fireMenuCanceled()'
     */
    public void testFireMenuCanceled() {
        ConcreteMenuListener listener1 = new ConcreteMenuListener();
        ConcreteMenuListener listener2 = new ConcreteMenuListener();
        menu.addMenuListener(listener1);
        menu.addMenuListener(listener2);
        menu.fireMenuCanceled();
        assertEquals("event fired properly ", "canceled", listener1.event);
        assertEquals("event fired properly ", "canceled", listener2.event);
        assertEquals("source ", menu, listener1.src);
        assertEquals("source ", menu, listener2.src);
    }

    /*
     * Test method for 'javax.swing.JMenu.fireMenuDeselected()'
     */
    public void testFireMenuDeselected() {
        ConcreteMenuListener listener1 = new ConcreteMenuListener();
        ConcreteMenuListener listener2 = new ConcreteMenuListener();
        menu.addMenuListener(listener1);
        menu.addMenuListener(listener2);
        menu.fireMenuDeselected();
        assertEquals("event fired properly ", "deselected", listener1.event);
        assertEquals("event fired properly ", "deselected", listener2.event);
        assertEquals("source ", menu, listener1.src);
        assertEquals("source ", menu, listener2.src);
    }

    /*
     * Test method for 'javax.swing.JMenu.fireMenuSelected()'
     */
    public void testFireMenuSelected() {
        ConcreteMenuListener listener1 = new ConcreteMenuListener();
        ConcreteMenuListener listener2 = new ConcreteMenuListener();
        menu.addMenuListener(listener1);
        menu.addMenuListener(listener2);
        menu.fireMenuSelected();
        assertEquals("event fired properly ", "selected", listener1.event);
        assertEquals("event fired properly ", "selected", listener2.event);
        assertEquals("source ", menu, listener1.src);
        assertEquals("source ", menu, listener2.src);
    }

    /*
     * Test method for 'javax.swing.JMenu.getMenuComponentCount()'
     */
    public void testGetMenuComponentCount() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        assertEquals(0, menu.getMenuComponentCount());
        menu.add(item1);
        assertEquals(1, menu.getMenuComponentCount());
        menu.add(item2);
        assertEquals(2, menu.getMenuComponentCount());
        menu.addSeparator();
        assertEquals(3, menu.getMenuComponentCount());
        menu.add(new JButton(), 0);
        assertEquals(4, menu.getMenuComponentCount());
    }

    /*
     * Test method for 'javax.swing.JMenu.getItemCount()'
     */
    public void testGetItemCount() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        assertEquals(0, menu.getItemCount());
        menu.add(item1);
        assertEquals(1, menu.getItemCount());
        menu.add(item2);
        assertEquals(2, menu.getItemCount());
        menu.addSeparator();
        assertEquals(3, menu.getItemCount());
        menu.add(new JButton(), 0);
        assertEquals(4, menu.getItemCount());
    }

    /*
     * Test method for 'javax.swing.JMenu.getMenuComponent(int)'
     */
    public void testGetMenuComponent() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        assertNull(menu.getMenuComponent(0));
        menu.add(item1);
        assertEquals(item1, menu.getMenuComponent(0));
        menu.add(item2);
        assertEquals(item1, menu.getMenuComponent(0));
        assertEquals(item2, menu.getMenuComponent(1));
        menu.addSeparator();
        assertEquals(item1, menu.getMenuComponent(0));
        assertEquals(item2, menu.getMenuComponent(1));
        assertTrue(menu.getMenuComponent(2) instanceof JSeparator);
        menu.add(new JButton(), 3);
        assertEquals(item1, menu.getMenuComponent(0));
        assertEquals(item2, menu.getMenuComponent(1));
        assertTrue(menu.getMenuComponent(2) instanceof JSeparator);
        assertTrue(menu.getMenuComponent(3) instanceof JButton);
    }

    /*
     * Test method for 'javax.swing.JMenu.getItem(int)'
     */
    public void testGetItem() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        assertNull(menu.getItem(0));
        menu.add(item1);
        assertEquals(item1, menu.getItem(0));
        menu.add(item2);
        assertEquals(item1, menu.getItem(0));
        assertEquals(item2, menu.getItem(1));
        menu.addSeparator();
        assertEquals(item1, menu.getItem(0));
        assertEquals(item2, menu.getItem(1));
        assertNull(menu.getItem(2));
        menu.add(new JButton(), 3);
        assertEquals(item1, menu.getMenuComponent(0));
        assertEquals(item2, menu.getMenuComponent(1));
        assertNull(menu.getItem(2));
        assertNull(menu.getItem(3));
    }

    /*
     * Test method for 'javax.swing.JMenu.getMenuComponents()'
     */
    public void testGetMenuComponents() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JMenuItem item3 = new JMenuItem();
        JMenuItem item4 = new JMenuItem();
        assertEquals(0, menu.getMenuComponents().length);
        menu.add(item1);
        assertEquals(1, menu.getMenuComponents().length);
        assertSame(item1, menu.getMenuComponents()[0]);
        menu.add(item2);
        assertEquals(2, menu.getMenuComponents().length);
        assertSame(item1, menu.getMenuComponents()[0]);
        assertSame(item2, menu.getMenuComponents()[1]);
        menu.addSeparator();
        assertEquals(3, menu.getMenuComponents().length);
        assertSame(item1, menu.getMenuComponents()[0]);
        assertSame(item2, menu.getMenuComponents()[1]);
        assertTrue(menu.getMenuComponents()[2] instanceof JSeparator);
        JButton button = new JButton();
        menu.add(button, 0);
        assertEquals(4, menu.getMenuComponents().length);
        assertTrue(menu.getMenuComponents()[0] instanceof JButton);
        assertSame(item1, menu.getMenuComponents()[1]);
        assertSame(item2, menu.getMenuComponents()[2]);
        assertTrue(menu.getMenuComponents()[3] instanceof JSeparator);
        JMenu subMenu = new JMenu();
        subMenu.add(item3);
        subMenu.add(item4);
        menu.add(subMenu);
        assertEquals(5, menu.getMenuComponents().length);
        assertTrue(menu.getMenuComponents()[4] instanceof JMenu);
    }

    /*
     * Test method for 'javax.swing.JMenu.getPopupMenu()'
     */
    public void testGetPopupMenu() {
        assertNotNull(menu.getPopupMenu());
        assertNull(menu.getPopupMenu().getLabel());
        assertEquals(0, menu.getPopupMenu().getComponentCount());
        JMenuItem item = menu.add("item");
        assertEquals(1, menu.getPopupMenu().getComponentCount());
        assertSame(item, menu.getPopupMenu().getComponent(0));
    }

    /*
     * Test method for 'javax.swing.JMenu.getPopupMenuOrigin()'
     */
    public void testGetPopupMenuOrigin() {
        int offsetX1 = 50;
        int offsetY1 = 60;
        int offsetX2 = 20;
        int offsetY2 = 30;
        JMenuBar menuBar = new JMenuBar();
        JMenu menu2 = new JMenu();
        JWindow frame = new JWindow();
        menuBar.add(menu);
        frame.setLocation(500, 500);
        frame.getContentPane().add(menuBar);
        menu.setPreferredSize(new Dimension(200, 200));
        menu2.setPreferredSize(new Dimension(100, 100));
        frame.pack();
        menu.getPopupMenu().setPreferredSize(new Dimension(50, 50));
        UIManager.put("Menu.menuPopupOffsetX", new Integer(offsetX1));
        UIManager.put("Menu.menuPopupOffsetY", new Integer(offsetY1));
        UIManager.put("Menu.submenuPopupOffsetX", new Integer(offsetX2));
        UIManager.put("Menu.submenuPopupOffsetY", new Integer(offsetY2));
        frame.setVisible(true);
        assertEquals(new Point(offsetX1, menu.getHeight() + offsetY1), menu
                .getPopupMenuOrigin());
        frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(new Point(menu.getWidth() - menu.getPopupMenu().getPreferredSize().width
                - offsetX1, menu.getHeight() + offsetY1), menu.getPopupMenuOrigin());
        menuBar.remove(menu);
        menuBar.add(menu2);
        menu2.add(menu);
        frame.pack();
        frame.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        menu2.setPopupMenuVisible(true);
        assertEquals(new Point(menu.getWidth() + offsetX2, offsetY2), menu.getPopupMenuOrigin());
        frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(new Point(-offsetX2 - menu.getPopupMenu().getPreferredSize().width,
                offsetY2), menu.getPopupMenuOrigin());
        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.JMenu.insert(String, int)'
     */
    public void testInsertStringInt() {
        String text1 = "text";
        String text2 = "texttext";
        menu.insert(text1, 0);
        JMenuItem item1 = menu.getItem(0);
        assertEquals(text1, item1.getText());
        assertNull(item1.getIcon());
        assertEquals(1, menu.getItemCount());
        assertEquals(item1, menu.getItem(0));
        menu.insert(text2, 0);
        JMenuItem item2 = menu.getItem(0);
        assertEquals(text2, item2.getText());
        assertNull(item2.getIcon());
        assertEquals(2, menu.getItemCount());
        assertEquals(item2, menu.getItem(0));
        assertEquals(item1, menu.getItem(1));
        assertNotSame(item1, item2);
        try {
            menu.insert(text1, -6);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        menu.insert(text1, 5);
        assertEquals(3, menu.getItemCount());
        item1 = menu.getItem(2);
        assertEquals(text1, item1.getText());
        assertNull(item1.getIcon());
    }

    /*
     * Test method for 'javax.swing.JMenu.insert(JMenuItem, int)'
     */
    public void testInsertJMenuItemInt() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        assertSame(item1, menu.insert(item1, 0));
        assertEquals(1, menu.getItemCount());
        assertEquals(item1, menu.getItem(0));
        assertSame(item2, menu.insert(item2, 0));
        assertEquals(2, menu.getItemCount());
        assertEquals(item2, menu.getItem(0));
        assertEquals(item1, menu.getItem(1));
        assertNotSame(item1, item2);
        try {
            menu.insert(item1, -6);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        menu.insert(item1, 5);
        assertEquals(2, menu.getItemCount());
    }

    /*
     * Test method for 'javax.swing.JMenu.insert(Action, int)'
     */
    public void testInsertActionInt() {
        Icon icon = createNewIcon();
        String text = "texttext";
        MyAction action1 = new MyAction(text, icon);
        MyAction action2 = new MyAction(text, icon);
        JMenuItem item = menu.insert(action1, 0);
        assertEquals(1, menu.getItemCount());
        JMenuItem menuItem = menu.getItem(0);
        assertSame(item, menuItem);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        item = menu.insert(action2, 1);
        assertEquals(2, menu.getItemCount());
        menuItem = menu.getItem(1);
        assertSame(item, menuItem);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        menu.insert(action1, 16);
        assertEquals(3, menu.getItemCount());
        menuItem = menu.getItem(2);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        try {
            menu.insert(action1, -3);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test method for 'javax.swing.JMenu.insertSeparator(int)'
     */
    public void testInsertSeparator() {
        menu.insertSeparator(0);
        assertEquals(1, menu.getItemCount());
        assertNull(menu.getItem(0));
        assertTrue(menu.getPopupMenu().getComponent(0) instanceof JSeparator);
        menu.insertSeparator(0);
        assertEquals(2, menu.getItemCount());
        assertNull(menu.getItem(0));
        assertNull(menu.getItem(1));
        assertTrue(menu.getPopupMenu().getComponent(0) instanceof JSeparator);
        assertTrue(menu.getPopupMenu().getComponent(1) instanceof JSeparator);
        try {
            menu.insertSeparator(-6);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        menu.insertSeparator(5);
        assertEquals(3, menu.getItemCount());
    }

    /*
     * Test method for 'javax.swing.JMenu.isMenuComponent(Component)'
     */
    public void testIsMenuComponent() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JMenuItem item3 = new JMenuItem();
        JMenuItem item4 = new JMenuItem();
        assertFalse(menu.isMenuComponent(item1));
        assertTrue(menu.isMenuComponent(menu));
        menu.add(item1);
        assertTrue(menu.isMenuComponent(item1));
        assertTrue(menu.isMenuComponent(menu));
        assertFalse(menu.isMenuComponent(item2));
        menu.add(item2);
        assertTrue(menu.isMenuComponent(item2));
        assertTrue(menu.isMenuComponent(item1));
        assertTrue(menu.isMenuComponent(menu));
        menu.addSeparator();
        assertTrue(menu.isMenuComponent(item2));
        assertTrue(menu.isMenuComponent(item1));
        assertTrue(menu.isMenuComponent(menu));
        JButton button = new JButton();
        menu.add(button, 0);
        assertTrue(menu.isMenuComponent(button));
        assertTrue(menu.isMenuComponent(item2));
        assertTrue(menu.isMenuComponent(item1));
        assertTrue(menu.isMenuComponent(menu));
        JMenu subMenu = new JMenu();
        subMenu.add(item3);
        subMenu.add(item4);
        menu.add(subMenu);
        assertTrue(menu.isMenuComponent(subMenu));
        assertTrue(menu.isMenuComponent(item3));
        assertTrue(menu.isMenuComponent(item4));
        assertTrue(menu.isMenuComponent(button));
        assertTrue(menu.isMenuComponent(item2));
        assertTrue(menu.isMenuComponent(item1));
        assertTrue(menu.isMenuComponent(menu));
        assertFalse(menu.isMenuComponent(null));
    }

    /*
     * Test method for 'javax.swing.JMenu.isTearOff()'
     */
    public void testIsTearOff() {
        try {
            menu.isTearOff();
            fail("no exception has been thrown");
        } catch (Error e) {
        }
    }

    /*
     * Test method for 'javax.swing.JMenu.isTopLevelMenu()'
     */
    public void testIsTopLevelMenu() {
        assertFalse(menu.isTopLevelMenu());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu2 = new JMenu();
        menuBar.add(menu);
        menuBar.add(menu2);
        assertTrue(menu.isTopLevelMenu());
        menu2.add(menu);
        assertFalse(menu.isTopLevelMenu());
    }

    /*
     * Test method for 'javax.swing.JMenu.setDelay(int)'
     */
    public void testGetSetDelay() {
        int delay1 = 1000;
        int delay2 = -1000;
        menu.setDelay(0);
        menu.setDelay(delay1);
        assertEquals("delay", delay1, menu.getDelay());
        try {
            menu.setDelay(delay2);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test method for 'javax.swing.JMenu.setMenuLocation(int, int)'
     */
    public void testSetMenuLocation() {
        int x = 100;
        int y = 200;
        menu.getPopupMenu().setVisible(true);
        menu.setMenuLocation(x, y);
        assertEquals(new Point(x, y), menu.getPopupMenu().getLocationOnScreen());
        assertEquals(new Point(0, 0), menu.getPopupMenu().getLocation());
    }

    /*
     * Test method for 'javax.swing.JMenu.setPopupMenuVisible(boolean)'
     */
    public void testSetIsPopupMenuVisible() throws InterruptedException {
        menu.setPopupMenuVisible(true);
        assertFalse(menu.isPopupMenuVisible());
        JWindow frame = new JWindow();
        frame.setLocation(300, 300);
        menu.setPreferredSize(new Dimension(200, 200));
        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        frame.getContentPane().add(bar);
        frame.pack();
        frame.setVisible(true);
        menu.setPopupMenuVisible(true);
        assertTrue(menu.isPopupMenuVisible());
        assertSame(menu, menu.getPopupMenu().getInvoker());
        assertEquals(new Point(), menu.getPopupMenu().getLocation());
        Point menuLocation = menu.getLocationOnScreen();
        menuLocation.translate(0, menu.getHeight());
        assertEquals(menuLocation, menu.getPopupMenu().getLocationOnScreen());
        menu.setPopupMenuVisible(false);
        assertFalse(menu.isPopupMenuVisible());
        frame.dispose();
    }

    @Override
    public void testNumberOfModelListeners() {
        button.setUI(null);
        DefaultButtonModel model = (DefaultButtonModel) button.getModel();
        assertEquals("model's action listeners ", 1, model.getActionListeners().length);
        assertEquals("model's item listeners ", 1, model.getItemListeners().length);
        if (isHarmony()) {
            assertEquals("model's change listeners ", 1, model.getChangeListeners().length);
        } else {
            assertEquals("model's change listeners ", 2, model.getChangeListeners().length);
        }
    }

    @Override
    public void testConfigurePropertiesFromAction() {
        Icon icon1 = createNewIcon();
        Icon icon2 = createNewIcon();
        KeyStroke ks1 = KeyStroke.getKeyStroke('a');
        KeyStroke ks2 = KeyStroke.getKeyStroke('b');
        String text1 = "texttext1";
        String text2 = "texttext2";
        String text3 = "texttext3";
        String text4 = "texttext4";
        AbstractAction action1 = new AbstractAction(text1, icon1) {
            public void actionPerformed(final ActionEvent event) {
            }
        };
        AbstractAction action2 = new AbstractAction(text2, icon2) {
            public void actionPerformed(final ActionEvent event) {
            }
        };
        action1.setEnabled(true);
        action1.putValue(Action.SHORT_DESCRIPTION, text3);
        action1.putValue(Action.MNEMONIC_KEY, new Integer(1));
        button.setAction(action1);
        assertEquals("action ", action1, button.getAction());
        assertTrue("enabled ", button.isEnabled());
        assertTrue("enabled ", action1.isEnabled());
        action1.setEnabled(false);
        button.isEnabled();
        assertFalse("enabled ", button.isEnabled());
        assertFalse("enabled ", action1.isEnabled());
        assertEquals("icon ", icon1, button.getIcon());
        action1.putValue(Action.SMALL_ICON, icon2);
        assertEquals("icon ", icon2, button.getIcon());
        button.setIcon(icon2);
        action1.putValue(Action.SMALL_ICON, null);
        assertNull("icon ", button.getIcon());
        if (isHarmony()) {
            assertEquals("mnemonic ", 1, button.getMnemonic());
            action1.putValue(Action.MNEMONIC_KEY, new Integer(27));
            assertEquals("mnemonic ", 27, button.getMnemonic());
            action1.putValue(Action.ACCELERATOR_KEY, ks1);
            assertNull("accelerator ", menuItem.getAccelerator());
        }
        assertEquals("text ", text1, button.getText());
        action1.putValue(Action.NAME, text2);
        assertEquals("text ", text2, button.getText());
        if (isHarmony()) {
            assertEquals("ToolTipText ", text3, button.getToolTipText());
            action1.putValue(Action.SHORT_DESCRIPTION, text4);
            assertEquals("ToolTipText ", text4, button.getToolTipText());
        }
        action2.putValue(Action.ACCELERATOR_KEY, ks2);
        button.setAction(action2);
        action1.putValue(Action.SHORT_DESCRIPTION, text4);
        assertNull("ToolTipText ", button.getToolTipText());
        if (isHarmony()) {
            action2.putValue(Action.SHORT_DESCRIPTION, text4);
            assertEquals("ToolTipText ", text4, button.getToolTipText());
            assertNull("accelerator ", menuItem.getAccelerator());
        }
    }
}
