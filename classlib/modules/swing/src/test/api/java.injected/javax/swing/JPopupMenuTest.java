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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import javax.accessibility.AccessibleRole;
import javax.swing.event.MenuEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.PopupMenuUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

public class JPopupMenuTest extends SwingTestCase {
    public static class MyAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        final String command = "dnammoc";

        final KeyStroke accelerator = KeyStroke.getKeyStroke('a');

        public boolean performed = false;

        public MyAction(final String text, final Icon icon) {
            super(text, icon);
            putValue(Action.ACTION_COMMAND_KEY, command);
            putValue(Action.ACCELERATOR_KEY, accelerator);
        }

        public void actionPerformed(final ActionEvent e) {
            performed = true;
        }
    };

    class ConcretePopupMenuListener implements PopupMenuListener {
        public Object event;

        public Object src;

        public void menuSelected(MenuEvent e) {
            event = "selected";
            src = e.getSource();
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            event = "canceled";
            src = e.getSource();
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            event = "invisible";
            src = e.getSource();
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            event = "visible";
            src = e.getSource();
        }

        public void reset() {
            event = null;
            src = null;
        }
    };

    protected JPopupMenu popup;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        popup = new JPopupMenu();
    }

    @Override
    protected void tearDown() throws Exception {
        popup = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        boolean assertedValue = (popup.getAccessibleContext() != null && popup
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JPopupMenu$AccessibleJPopupMenu"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.POPUP_MENU, popup.getAccessibleContext()
                .getAccessibleRole());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setVisible(boolean)'
     */
    public void testSetIsVisible() {
        PropertyChangeController listener1 = new PropertyChangeController();
        popup.addPropertyChangeListener(listener1);
        ConcretePopupMenuListener listener2 = new ConcretePopupMenuListener();
        popup.addPopupMenuListener(listener2);
        assertNull(popup.getParent());
        assertFalse(popup.isVisible());
        assertNull(SwingUtilities.getWindowAncestor(popup));
        popup.setVisible(true);
        assertNotNull(SwingUtilities.getWindowAncestor(popup));
        Container ancestor = popup.getParent();
        assertEquals("event fired properly ", "visible", listener2.event);
        assertEquals("source ", popup, listener2.src);
        listener2.reset();
        listener1.checkPropertyFired(popup, "visible", Boolean.FALSE, Boolean.TRUE);
        listener1.checkPropertyFired(popup, "ancestor", null, ancestor);
        assertNotNull(ancestor);
        assertTrue(popup.isVisible());
        listener1.reset();
        popup.setVisible(false);
        assertNull(SwingUtilities.getWindowAncestor(popup));
        assertEquals("event fired properly ", "invisible", listener2.event);
        assertEquals("source ", popup, listener2.src);
        listener2.reset();
        listener1.checkPropertyFired(popup, "visible", Boolean.TRUE, Boolean.FALSE);
        listener1.checkPropertyFired(popup, "ancestor", ancestor, null);
        assertNull(popup.getParent());
        assertFalse(popup.isVisible());
        listener1.reset();
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setLocation(int, int)'
     */
    @SuppressWarnings("deprecation")
    public void testSetLocationIntInt() {
        JFrame frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        JMenu menu1 = new JMenu();
        JMenu menu2 = new JMenu();
        frame.getContentPane().add(menuBar);
        menuBar.add(menu1);
        menuBar.add(menu2);
        popup = menu1.getPopupMenu();
        popup.setLocation(100, 200);
        assertEquals(new Point(0, 0), popup.getLocation());
        popup.setVisible(true);
        assertEquals(new Point(100, 200), popup.getLocationOnScreen());
        popup.setLocation(10, 20);
        assertEquals(new Point(0, 0), popup.getLocation());
        assertEquals(new Point(10, 20), popup.getLocationOnScreen());
        popup.setInvoker(menu2);
        popup.setLocation(100, 200);
        assertEquals(new Point(0, 0), popup.getLocation());
        assertEquals(new Point(100, 200), popup.getLocationOnScreen());
        frame.pack();
        frame.show();
        popup.setVisible(true);
        popup.setLocation(10, 20);
        assertEquals(new Point(0, 0), popup.getLocation());
        assertEquals(new Point(10, 20), popup.getLocationOnScreen());
        popup.setInvoker(menu2);
        popup.setLocation(100, 200);
        assertEquals(new Point(0, 0), popup.getLocation());
        assertEquals(new Point(100, 200), popup.getLocationOnScreen());
        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.show(Component, int, int)'
     */
    @SuppressWarnings("deprecation")
    public void testShowComponentIntInt() {
        JFrame frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        JMenu menu1 = new JMenu();
        JMenu menu2 = new JMenu();
        frame.getContentPane().add(menuBar);
        menuBar.add(menu1);
        menuBar.add(menu2);
        popup = menu1.getPopupMenu();
        popup.setInvoker(new JButton());
        assertNotNull(popup.getInvoker());
        popup.show(null, 111, 222);
        assertNull(popup.getInvoker());
        assertEquals(new Point(111, 222), popup.getLocationOnScreen());
        assertTrue(popup.isVisible());
        assertTrue(popup.isShowing());
        frame.pack();
        frame.show();
        popup.show(menu2, 333, 111);
        assertSame(menu2, popup.getInvoker());
        assertFalse(popup.getLocationOnScreen().equals(new Point(333, 111)));
        assertTrue(popup.isVisible());
        assertTrue(popup.isShowing());
        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getComponentIndex(Component)'
     */
    public void testGetComponentIndex() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JMenuItem item3 = new JMenuItem();
        popup.add(item1);
        popup.add(item2);
        assertEquals(0, popup.getComponentIndex(item1));
        assertEquals(1, popup.getComponentIndex(item2));
        assertEquals(-1, popup.getComponentIndex(item3));
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.remove(int)'
     */
    public void testRemoveInt() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        popup.add(item1);
        popup.add(item2);
        assertEquals(2, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
        try {
            popup.remove(5);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            popup.remove(-5);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
        popup.remove(0);
        assertEquals(1, popup.getComponentCount());
        assertSame(item2, popup.getComponent(0));
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getUIClassID()'
     */
    public void testGetUIClassID() {
        assertEquals("PopupMenuUI", popup.getUIClassID());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.JPopupMenu()'
     */
    public void testJPopupMenu() {
        assertNull("text ", popup.getLabel());
        assertTrue(popup.getSelectionModel() instanceof DefaultSingleSelectionModel);
        assertEquals(0, popup.getComponentCount());
        assertTrue(popup.isFocusable());
        assertEquals(popup, popup.getComponent());
        assertNull(popup.getInvoker());
        assertEquals(0, popup.getSubElements().length);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.JPopupMenu(String)'
     */
    public void testJPopupMenuString() {
        String label = "label";
        popup = new JPopupMenu(label);
        assertEquals("text ", label, popup.getLabel());
        assertTrue(popup.getSelectionModel() instanceof DefaultSingleSelectionModel);
        assertEquals(0, popup.getComponentCount());
        assertTrue(popup.isFocusable());
        assertEquals(popup, popup.getComponent());
        assertNull(popup.getInvoker());
        assertEquals(0, popup.getSubElements().length);
        popup = new JPopupMenu(null);
        assertNull("text ", popup.getLabel());
        assertTrue(popup.getSelectionModel() instanceof DefaultSingleSelectionModel);
        assertEquals(0, popup.getComponentCount());
        assertTrue(popup.isFocusable());
        assertEquals(popup, popup.getComponent());
        assertNull(popup.getInvoker());
        assertEquals(0, popup.getSubElements().length);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.JPopupMenu()'
     */
    public void testJPopupMenuSeparator() {
        JSeparator separator = new JPopupMenu.Separator();
        assertEquals("PopupMenuSeparatorUI", separator.getUIClassID());

        // Regression test for HARMONY-2631
        assertFalse(separator.isFocusable());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.add(Action)'
     */
    public void testAddAction() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        JMenuItem menuItem = popup.add(action);
        assertEquals(1, popup.getComponentCount());
        assertTrue(popup.getComponent(0) instanceof JMenuItem);
        menuItem = ((JMenuItem) popup.getComponent(0));
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertSame(action, menuItem.getAction());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.add(String)'
     */
    public void testAddString() {
        String label1 = "label1";
        String label2 = "label1";
        popup.add((String) null);
        assertEquals(1, popup.getComponentCount());
        assertTrue(popup.getComponent(0) instanceof JMenuItem);
        assertEquals("", ((JMenuItem) popup.getComponent(0)).getText());
        popup.add(label1);
        assertEquals(2, popup.getComponentCount());
        assertTrue(popup.getComponent(1) instanceof JMenuItem);
        assertEquals(label1, ((JMenuItem) popup.getComponent(1)).getText());
        popup.add(label2);
        assertEquals(3, popup.getComponentCount());
        assertTrue(popup.getComponent(2) instanceof JMenuItem);
        assertEquals(label2, ((JMenuItem) popup.getComponent(2)).getText());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.add(JMenuItem)'
     */
    public void testAddJMenuItem() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        popup.add(item1);
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        popup.add(item2);
        assertEquals(2, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(1));
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.addPopupMenuListener(PopupMenuListener)'
     */
    public void testAddGetRemovePopupMenuListener() {
        PopupMenuListener listener1 = new ConcretePopupMenuListener();
        PopupMenuListener listener2 = new ConcretePopupMenuListener();
        PopupMenuListener listener3 = new ConcretePopupMenuListener();
        EventListener[] listenersArray = null;
        listenersArray = popup.getPopupMenuListeners();
        int initialValue = listenersArray.length;
        popup.addPopupMenuListener(listener1);
        popup.addPopupMenuListener(listener2);
        popup.addPopupMenuListener(listener2);
        listenersArray = popup.getPopupMenuListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        popup.removePopupMenuListener(listener1);
        popup.addPopupMenuListener(listener3);
        popup.addPopupMenuListener(listener3);
        listenersArray = popup.getPopupMenuListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        popup.removePopupMenuListener(listener3);
        popup.removePopupMenuListener(listener3);
        listenersArray = popup.getPopupMenuListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        popup.removePopupMenuListener(listener2);
        popup.removePopupMenuListener(listener2);
        listenersArray = popup.getPopupMenuListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.addSeparator()'
     */
    public void testAddSeparator() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        popup.add(item1);
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        popup.addSeparator();
        assertEquals(2, popup.getComponentCount());
        assertTrue(popup.getComponent(1) instanceof JPopupMenu.Separator);
        popup.add(item2);
        assertEquals(3, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        assertSame(item2, popup.getComponent(2));
        popup.addSeparator();
        assertEquals(4, popup.getComponentCount());
        assertTrue(popup.getComponent(3) instanceof JPopupMenu.Separator);
        assertNotSame(popup.getComponent(1), popup.getComponent(3));
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.createActionChangeListener(JMenuItem)'
     */
    public void testCreateActionChangeListener() {
        AbstractAction action1 = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
            }
        };
        JMenuItem item1 = new JMenuItem(action1);
        JMenuItem item2 = new JMenuItem();
        PropertyChangeListener listener1 = popup.createActionChangeListener(item2);
        PropertyChangeListener listener2 = popup.createActionChangeListener(item1);
        assertNotNull(listener1);
        assertNotNull(listener2);
        assertNotSame(listener1, listener2);
        String text = "textxetxet";
        action1.putValue(Action.NAME, text);
        assertEquals(text, item1.getText());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.createActionComponent(Action)'
     */
    public void testCreateActionComponent() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        JMenuItem menuItem = popup.createActionComponent(action);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertNull("action", menuItem.getAction());
        assertEquals("command ", text, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.firePopupMenuCanceled()'
     */
    public void testFirePopupMenuCanceled() {
        ConcretePopupMenuListener listener1 = new ConcretePopupMenuListener();
        ConcretePopupMenuListener listener2 = new ConcretePopupMenuListener();
        popup.addPopupMenuListener(listener1);
        popup.addPopupMenuListener(listener2);
        popup.firePopupMenuCanceled();
        assertEquals("event fired properly ", "canceled", listener1.event);
        assertEquals("event fired properly ", "canceled", listener2.event);
        assertEquals("source ", popup, listener1.src);
        assertEquals("source ", popup, listener2.src);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.firePopupMenuWillBecomeInvisible()'
     */
    public void testFirePopupMenuWillBecomeInvisible() {
        ConcretePopupMenuListener listener1 = new ConcretePopupMenuListener();
        ConcretePopupMenuListener listener2 = new ConcretePopupMenuListener();
        popup.addPopupMenuListener(listener1);
        popup.addPopupMenuListener(listener2);
        popup.firePopupMenuWillBecomeInvisible();
        assertEquals("event fired properly ", "invisible", listener1.event);
        assertEquals("event fired properly ", "invisible", listener2.event);
        assertEquals("source ", popup, listener1.src);
        assertEquals("source ", popup, listener2.src);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.firePopupMenuWillBecomeVisible()'
     */
    public void testFirePopupMenuWillBecomeVisible() {
        ConcretePopupMenuListener listener1 = new ConcretePopupMenuListener();
        ConcretePopupMenuListener listener2 = new ConcretePopupMenuListener();
        popup.addPopupMenuListener(listener1);
        popup.addPopupMenuListener(listener2);
        popup.firePopupMenuWillBecomeVisible();
        assertEquals("event fired properly ", "visible", listener1.event);
        assertEquals("event fired properly ", "visible", listener2.event);
        assertEquals("source ", popup, listener1.src);
        assertEquals("source ", popup, listener2.src);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getComponent()'
     */
    public void testGetComponent() {
        assertSame(popup, popup.getComponent());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getComponent(int)'
     */
    @SuppressWarnings("deprecation")
    public void testGetComponentAtIndex() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        popup.add(item1);
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponentAtIndex(0));
        popup.add(item2);
        assertEquals(2, popup.getComponentCount());
        assertSame(item1, popup.getComponentAtIndex(0));
        assertSame(item2, popup.getComponentAtIndex(1));
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getMargin()'
     */
    public void testGetMargin() {
        assertEquals(new Insets(0, 0, 0, 0), popup.getMargin());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getSubElements()'
     */
    public void testGetSubElements() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JMenuItem item3 = new JMenuItem();
        assertEquals(0, popup.getSubElements().length);
        popup.add(item1);
        assertEquals(1, popup.getSubElements().length);
        assertSame(item1, popup.getSubElements()[0]);
        popup.add(item2);
        popup.addSeparator();
        assertEquals(2, popup.getSubElements().length);
        assertSame(item1, popup.getSubElements()[0]);
        assertSame(item2, popup.getSubElements()[1]);
        popup.add(item3);
        assertEquals(3, popup.getSubElements().length);
        assertSame(item1, popup.getSubElements()[0]);
        assertSame(item2, popup.getSubElements()[1]);
        assertSame(item3, popup.getSubElements()[2]);
        popup.addSeparator();
        assertEquals(3, popup.getSubElements().length);
        assertSame(item1, popup.getSubElements()[0]);
        assertSame(item2, popup.getSubElements()[1]);
        assertSame(item3, popup.getSubElements()[2]);
        item1.setEnabled(false);
        assertEquals(3, popup.getSubElements().length);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getUI()'
     */
    public void testSetGetUI() {
        assertNotNull("ui is returned ", popup.getUI());
        PopupMenuUI ui = new BasicPopupMenuUI();
        popup.setUI(ui);
        assertSame("ui is returned ", ui, popup.getUI());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.insert(Component, int)'
     */
    public void testInsertComponentInt() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        Component item3 = new JPanel();
        popup.insert(item1, 0);
        assertEquals(1, popup.getComponentCount());
        assertSame(item1, popup.getComponent(0));
        popup.insert(item2, 0);
        assertEquals(2, popup.getComponentCount());
        assertSame(item2, popup.getComponent(0));
        assertSame(item1, popup.getComponent(1));
        popup.insert(item3, 1);
        assertEquals(3, popup.getComponentCount());
        assertSame(item2, popup.getComponent(0));
        assertSame(item3, popup.getComponent(1));
        assertSame(item1, popup.getComponent(2));
        popup.insert(item3, 16);
        assertEquals(3, popup.getComponentCount());
        try {
            popup.insert(item3, -6);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.insert(Action, int)'
     */
    public void testInsertActionInt() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        String text = "texttext";
        MyAction action1 = new MyAction(text, icon);
        MyAction action2 = new MyAction(text, icon);
        popup.insert(action1, 0);
        assertEquals(1, popup.getComponentCount());
        assertTrue(popup.getComponent(0) instanceof JMenuItem);
        JMenuItem menuItem = ((JMenuItem) popup.getComponent(0));
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        popup.insert(action2, 1);
        assertEquals(2, popup.getComponentCount());
        assertTrue(popup.getComponent(0) instanceof JMenuItem);
        menuItem = ((JMenuItem) popup.getComponent(1));
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        popup.insert(action1, 16);
        assertEquals(3, popup.getComponentCount());
        assertTrue(popup.getComponent(2) instanceof JMenuItem);
        menuItem = ((JMenuItem) popup.getComponent(2));
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        try {
            popup.insert(action1, -3);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.isBorderPainted()'
     */
    public void testSetIsBorderPainted() {
        assertTrue(popup.isBorderPainted());
        popup.setBorderPainted(false);
        assertFalse(popup.isBorderPainted());
        popup.setBorderPainted(true);
        assertTrue(popup.isBorderPainted());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.isPopupTrigger(MouseEvent)'
     */
    public void testIsPopupTrigger() {
        class MyPopupMenuUI extends PopupMenuUI {
            public boolean value = false;

            @Override
            public boolean isPopupTrigger(final MouseEvent event) {
                return value;
            }
        }
        ;
        MyPopupMenuUI fakeUI = new MyPopupMenuUI();
        popup.setUI(fakeUI);
        assertSame(fakeUI, popup.getUI());
        fakeUI.value = true;
        assertEquals(fakeUI.value, popup.isPopupTrigger(null));
        fakeUI.value = false;
        assertEquals(fakeUI.value, popup.isPopupTrigger(null));
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.menuSelectionChanged(boolean)'
     */
    @SuppressWarnings("deprecation")
    public void testMenuSelectionChanged() {
        JFrame frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu();
        frame.getContentPane().add(menuBar);
        menuBar.add(menu);
        popup = menu.getPopupMenu();
        popup.menuSelectionChanged(true);
        assertFalse(popup.isShowing());
        assertFalse(popup.isVisible());
        frame.pack();
        frame.show();
        popup.menuSelectionChanged(true);
        assertTrue(popup.isShowing());
        assertTrue(popup.isVisible());
        popup.menuSelectionChanged(false);
        assertFalse(popup.isShowing());
        assertFalse(popup.isVisible());
        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.pack()'
     */
    public void testPack() {
        JButton button1 = new JButton();
        JButton button2 = new JButton();
        popup.add(button1);
        popup.add(button2);
        popup.pack();
        Dimension size = popup.getMinimumSize();
        assertEquals(size, popup.getMinimumSize());
        assertEquals(size, popup.getPreferredSize());
        assertEquals(size, popup.getMaximumSize());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.processKeyEvent(KeyEvent, MenuElement[], MenuSelectionManager)'
     * as method has an empty body test is empty either
     */
    public void testProcessKeyEventKeyEventMenuElementArrayMenuSelectionManager() {
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(boolean)'
     */
    public void testSetGetDefaultLightWeightPopupEnabled() {
        assertTrue(JPopupMenu.getDefaultLightWeightPopupEnabled());
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        assertFalse(JPopupMenu.getDefaultLightWeightPopupEnabled());
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        assertTrue(JPopupMenu.getDefaultLightWeightPopupEnabled());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getInvoker()'
     */
    public void testGetInvoker() {
        assertNull(popup.getInvoker());
        JMenu menu = new JMenu();
        assertSame(menu, menu.getPopupMenu().getInvoker());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setInvoker(Component)'
     */
    public void testSetInvoker() {
        Component invoker1 = new JButton();
        Component invoker2 = new JLabel();
        popup.setInvoker(invoker1);
        assertEquals("invoker", invoker1, popup.getInvoker());
        popup.setInvoker(invoker2);
        assertEquals("invoker", invoker2, popup.getInvoker());
        popup.setInvoker(invoker2);
        assertEquals("invoker", invoker2, popup.getInvoker());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.getLabel()'
     */
    public void testGetLabel() {
        assertNull(popup.getLabel());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setLabel(String)'
     */
    public void testSetLabel() {
        PropertyChangeController listener = new PropertyChangeController();
        String label1 = "dog is dog";
        String label2 = "0xdeadbeef";
        popup.setLabel(null);
        popup.addPropertyChangeListener(listener);
        popup.setLabel(label1);
        listener.checkPropertyFired(popup, "label", null, label1);
        assertEquals("label", label1, popup.getLabel());
        listener.reset();
        popup.setLabel(label2);
        listener.checkPropertyFired(popup, "label", label1, label2);
        assertEquals("label", label2, popup.getLabel());
        listener.reset();
        popup.setLabel(label2);
        assertFalse("event's not been fired ", listener.isChanged());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setLightWeightPopupEnabled(boolean)'
     */
    public void testSetIsLightWeightPopupEnabled() {
        assertTrue(JPopupMenu.getDefaultLightWeightPopupEnabled());
        JPopupMenu menu = new JPopupMenu();
        assertTrue(menu.isLightWeightPopupEnabled());
        menu.setLightWeightPopupEnabled(false);
        assertFalse(menu.isLightWeightPopupEnabled());
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        menu = new JPopupMenu();
        assertFalse(menu.isLightWeightPopupEnabled());
        menu.setLightWeightPopupEnabled(true);
        assertTrue(menu.isLightWeightPopupEnabled());
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setPopupSize(int, int)'
     */
    public void testSetPopupSizeIntInt() {
        int height = 100;
        int width = 200;
        popup.setPopupSize(height, width);
        assertEquals(new Dimension(height, width), popup.getPreferredSize());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setPopupSize(Dimension)'
     */
    public void testSetPopupSizeDimension() {
        int height = 100;
        int width = 200;
        Dimension oldPrefs = popup.getPreferredSize();
        popup.setPopupSize(new Dimension(height, width));
        assertEquals(new Dimension(height, width), popup.getPreferredSize());
        popup.setPopupSize(null);
        assertEquals(oldPrefs, popup.getPreferredSize());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setSelected(Component)'
     */
    public void testSetSelected() {
        SingleSelectionModel selectionModel1 = new DefaultSingleSelectionModel();
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        popup.setSelectionModel(null);
        popup.add(item1);
        popup.add(item2);
        popup.setSelectionModel(selectionModel1);
        popup.setSelected(item1);
        assertEquals("selection", 0, selectionModel1.getSelectedIndex());
        popup.setSelected(item2);
        assertEquals("selection", 1, selectionModel1.getSelectedIndex());
        popup.setSelected(new JButton());
        assertEquals("selection", -1, selectionModel1.getSelectedIndex());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.setSelectionModel(SingleSelectionModel)'
     */
    public void testSetGetSelectionModel() {
        SingleSelectionModel selectionModel1 = new DefaultSingleSelectionModel();
        SingleSelectionModel selectionModel2 = new DefaultSingleSelectionModel();
        popup.setSelectionModel(null);
        popup.setSelectionModel(selectionModel1);
        assertEquals("selectionModel", selectionModel1, popup.getSelectionModel());
        popup.setSelectionModel(selectionModel2);
        assertEquals("selectionModel", selectionModel2, popup.getSelectionModel());
    }
}
