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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import javax.accessibility.AccessibleRole;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

@SuppressWarnings("serial")
public class JMenuItemTest extends AbstractButtonTest {
    protected JMenuItem menuItem = null;

    class ConcreteMenuKeyListener implements MenuKeyListener {
        public Object event;

        public void menuKeyPressed(MenuKeyEvent e) {
            event = "pressed";
        }

        public void menuKeyReleased(MenuKeyEvent e) {
            event = "released";
        }

        public void menuKeyTyped(MenuKeyEvent e) {
            event = "typed";
        }
    };

    class ConcreteMenuDragMouseListener implements MenuDragMouseListener {
        public Object event;

        public void menuDragMouseDragged(MenuDragMouseEvent e) {
            event = "dragged";
        }

        public void menuDragMouseEntered(MenuDragMouseEvent e) {
            event = "entered";
        }

        public void menuDragMouseExited(MenuDragMouseEvent e) {
            event = "exited";
        }

        public void menuDragMouseReleased(MenuDragMouseEvent e) {
            event = "released";
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuItem = new JMenuItem();
        button = menuItem;
    }

    @Override
    protected void tearDown() throws Exception {
        menuItem = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.JMenuItem.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        boolean assertedValue = (menuItem.getAccessibleContext() != null && menuItem
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JMenuItem$AccessibleJMenuItem"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.MENU_ITEM, menuItem
                .getAccessibleContext().getAccessibleRole());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.getUIClassID()'
     */
    public void testGetUIClassID() {
        assertEquals("MenuItemUI", menuItem.getUIClassID());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.JMenuItem()'
     */
    public void testJMenuItem() {
        assertTrue("default buttonModel ", button.getModel() instanceof DefaultButtonModel);
        assertNull("icon ", button.getIcon());
        assertEquals("text ", "", button.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertFalse(menuItem.isFocusable());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.JMenuItem(Icon)'
     */
    public void testJMenuItemIcon() {
        Icon icon = createNewIcon();
        menuItem = new JMenuItem(icon);
        assertTrue("default buttonModel ", button.getModel() instanceof DefaultButtonModel);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", "", menuItem.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertFalse(menuItem.isFocusable());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.JMenuItem(String)'
     */
    public void testJMenuItemString() {
        String text = "texttext";
        menuItem = new JMenuItem(text);
        assertTrue("default buttonModel ", button.getModel() instanceof DefaultButtonModel);
        assertNull("icon ", menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertFalse(menuItem.isFocusable());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.JMenuItem(Action)'
     */
    public void testJMenuItemAction() {
        final String command = "dnammoc";
        final KeyStroke accelerator = KeyStroke.getKeyStroke('a');
        class MyAction extends AbstractAction {
            public MyAction(final String text, final Icon icon) {
                super(text, icon);
                putValue(Action.ACTION_COMMAND_KEY, command);
                putValue(Action.ACCELERATOR_KEY, accelerator);
            }

            public void actionPerformed(final ActionEvent e) {
            }
        }
        ;
        Icon icon = createNewIcon();
        String text = "texttext";
        MyAction action = new MyAction(text, icon);
        menuItem = new JMenuItem(action);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals("action", action, menuItem.getAction());
        assertEquals("command ", command, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
        assertEquals("accelerator ", accelerator, menuItem.getAccelerator());
        assertFalse(menuItem.isFocusable());
        action.setEnabled(false);
        menuItem = new JMenuItem(action);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertEquals("action", action, menuItem.getAction());
        assertEquals("command ", command, menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertFalse("enabled ", menuItem.isEnabled());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        menuItem = new JMenuItem((Action) null);
        assertNull("icon ", menuItem.getIcon());
        assertNull("text ", menuItem.getText());
        assertNull("action", menuItem.getAction());
        assertNull("command ", menuItem.getActionCommand());
        assertFalse("selected ", menuItem.isSelected());
        assertTrue("enabled ", menuItem.isEnabled());
        assertTrue("menuItem model is of the proper type",
                menuItem.getModel() instanceof DefaultButtonModel);
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.JMenuItem(String, Icon)'
     */
    public void testJMenuItemStringIcon() {
        Icon icon = createNewIcon();
        String text = "texttext";
        menuItem = new JMenuItem(text, icon);
        assertTrue("menuItem model is of the proper type",
                menuItem.getModel() instanceof DefaultButtonModel);
        assertEquals("icon ", icon, menuItem.getIcon());
        assertEquals("text ", text, menuItem.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertFalse(menuItem.isFocusable());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.JMenuItem(String, int)'
     */
    public void testJMenuItemStringInt() {
        int mnemonic = 100;
        String text = "texttext";
        menuItem = new JMenuItem(text, mnemonic);
        assertTrue("menuItem model is of the proper type",
                menuItem.getModel() instanceof DefaultButtonModel);
        assertNull("icon ", menuItem.getIcon());
        assertEquals("mnemonic ", mnemonic, menuItem.getMnemonic());
        assertEquals("text ", text, menuItem.getText());
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
        assertFalse(menuItem.isFocusable());
        assertEquals(SwingConstants.LEADING, button.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, button.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, button.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, button.getVerticalTextPosition());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.setArmed(boolean)'
     */
    public void testSetArmed() {
        PropertyChangeController listener = new PropertyChangeController();
        menuItem.setArmed(false);
        menuItem.addPropertyChangeListener(listener);
        menuItem.setArmed(true);
        assertFalse("event's not been fired ", listener.isChanged());
        assertTrue("armed", menuItem.isArmed());
        assertTrue("armed", menuItem.getModel().isArmed());
        listener.reset();
        menuItem.setArmed(false);
        assertFalse("event's not been fired ", listener.isChanged());
        assertFalse("armed", menuItem.isArmed());
        assertFalse("armed", menuItem.getModel().isArmed());
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.JMenuItem.isArmed()'
     */
    public void testIsArmed() {
        assertFalse("armed", menuItem.isArmed());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.setAccelerator(KeyStroke)'
     */
    public void testSetAccelerator() {
        PropertyChangeController listener = new PropertyChangeController();
        KeyStroke accelerator1 = KeyStroke.getKeyStroke('a');
        KeyStroke accelerator2 = KeyStroke.getKeyStroke('b');
        menuItem.setAccelerator(null);
        menuItem.addPropertyChangeListener(listener);
        menuItem.setAccelerator(accelerator1);
        listener.checkPropertyFired(menuItem, "accelerator", null, accelerator1);
        assertEquals("accelerator", accelerator1, menuItem.getAccelerator());
        listener.reset();
        menuItem.setAccelerator(accelerator2);
        listener.checkPropertyFired(menuItem, "accelerator", accelerator1, accelerator2);
        assertEquals("accelerator", accelerator2, menuItem.getAccelerator());
        listener.reset();
        menuItem.setAccelerator(accelerator2);
        assertFalse("event's not been fired ", listener.isChanged());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.getAccelerator()'
     */
    public void testGetAccelerator() {
        assertNull("accelerator", menuItem.getAccelerator());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuDragMouseEntered(MenuDragMouseEvent)'
     */
    public void testFireMenuDragMouseEntered() {
        MenuDragMouseEvent event1 = new MenuDragMouseEvent(menuItem, 0, 0, 0, 0, 0, 0, false,
                new MenuElement[0], new MenuSelectionManager());
        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        ConcreteMenuDragMouseListener listener2 = new ConcreteMenuDragMouseListener();
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.addMenuDragMouseListener(listener2);
        menuItem.fireMenuDragMouseEntered(event1);
        assertEquals("event fired properly ", "entered", listener1.event);
        assertEquals("event fired properly ", "entered", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
        menuItem.fireMenuDragMouseEntered(event1);
        assertEquals("event fired properly ", "entered", listener1.event);
        assertEquals("event fired properly ", "entered", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuDragMouseExited(MenuDragMouseEvent)'
     */
    public void testFireMenuDragMouseExited() {
        MenuDragMouseEvent event1 = new MenuDragMouseEvent(menuItem, 0, 0, 0, 0, 0, 0, false,
                new MenuElement[0], new MenuSelectionManager());
        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        ConcreteMenuDragMouseListener listener2 = new ConcreteMenuDragMouseListener();
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.addMenuDragMouseListener(listener2);
        menuItem.fireMenuDragMouseExited(event1);
        assertEquals("event fired properly ", "exited", listener1.event);
        assertEquals("event fired properly ", "exited", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
        menuItem.fireMenuDragMouseExited(event1);
        assertEquals("event fired properly ", "exited", listener1.event);
        assertEquals("event fired properly ", "exited", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuDragMouseDragged(MenuDragMouseEvent)'
     */
    public void testFireMenuDragMouseDragged() {
        MenuDragMouseEvent event1 = new MenuDragMouseEvent(menuItem, 0, 0, 0, 0, 0, 0, false,
                new MenuElement[0], new MenuSelectionManager());
        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        ConcreteMenuDragMouseListener listener2 = new ConcreteMenuDragMouseListener();
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.addMenuDragMouseListener(listener2);
        menuItem.fireMenuDragMouseDragged(event1);
        assertEquals("event fired properly ", "dragged", listener1.event);
        assertEquals("event fired properly ", "dragged", listener2.event);
        menuItem.fireMenuDragMouseDragged(event1);
        assertEquals("event fired properly ", "dragged", listener1.event);
        assertEquals("event fired properly ", "dragged", listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuDragMouseReleased(MenuDragMouseEvent)'
     */
    public void testFireMenuDragMouseReleased() {
        MenuDragMouseEvent event1 = new MenuDragMouseEvent(menuItem, 0, 0, 0, 0, 0, 0, false,
                new MenuElement[0], new MenuSelectionManager());
        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        ConcreteMenuDragMouseListener listener2 = new ConcreteMenuDragMouseListener();
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.addMenuDragMouseListener(listener2);
        menuItem.fireMenuDragMouseReleased(event1);
        assertEquals("event fired properly ", "released", listener1.event);
        assertEquals("event fired properly ", "released", listener2.event);
        menuItem.fireMenuDragMouseReleased(event1);
        assertEquals("event fired properly ", "released", listener1.event);
        assertEquals("event fired properly ", "released", listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuKeyPressed(MenuKeyEvent)'
     */
    public void testFireMenuKeyPressed() {
        MenuKeyEvent event1 = new MenuKeyEvent(menuItem, 0, 0, 0, 0, 'a', new MenuElement[0],
                new MenuSelectionManager());
        ConcreteMenuKeyListener listener1 = new ConcreteMenuKeyListener();
        ConcreteMenuKeyListener listener2 = new ConcreteMenuKeyListener();
        menuItem.addMenuKeyListener(listener1);
        menuItem.addMenuKeyListener(listener2);
        menuItem.fireMenuKeyPressed(event1);
        assertEquals("event fired properly ", "pressed", listener1.event);
        assertEquals("event fired properly ", "pressed", listener2.event);
        menuItem.fireMenuKeyPressed(event1);
        assertEquals("event fired properly ", "pressed", listener1.event);
        assertEquals("event fired properly ", "pressed", listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuKeyReleased(MenuKeyEvent)'
     */
    public void testFireMenuKeyReleased() {
        MenuKeyEvent event1 = new MenuKeyEvent(menuItem, 0, 0, 0, 0, 'a', new MenuElement[0],
                new MenuSelectionManager());
        ConcreteMenuKeyListener listener1 = new ConcreteMenuKeyListener();
        ConcreteMenuKeyListener listener2 = new ConcreteMenuKeyListener();
        menuItem.addMenuKeyListener(listener1);
        menuItem.addMenuKeyListener(listener2);
        menuItem.fireMenuKeyReleased(event1);
        assertEquals("event fired properly ", "released", listener1.event);
        assertEquals("event fired properly ", "released", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
        menuItem.fireMenuKeyReleased(event1);
        assertEquals("event fired properly ", "released", listener1.event);
        assertEquals("event fired properly ", "released", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.fireMenuKeyTyped(MenuKeyEvent)'
     */
    public void testFireMenuKeyTyped() {
        MenuKeyEvent event1 = new MenuKeyEvent(menuItem, 0, 0, 0, 0, 'a', new MenuElement[0],
                new MenuSelectionManager());
        ConcreteMenuKeyListener listener1 = new ConcreteMenuKeyListener();
        ConcreteMenuKeyListener listener2 = new ConcreteMenuKeyListener();
        menuItem.addMenuKeyListener(listener1);
        menuItem.addMenuKeyListener(listener2);
        menuItem.fireMenuKeyTyped(event1);
        assertEquals("event fired properly ", "typed", listener1.event);
        assertEquals("event fired properly ", "typed", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
        menuItem.fireMenuKeyTyped(event1);
        assertEquals("event fired properly ", "typed", listener1.event);
        assertEquals("event fired properly ", "typed", listener2.event);
        assertSame("one event fired ", listener1.event, listener2.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.addMenuDragMouseListener(MenuDragMouseListener)'
     */
    public void testAddGetRemoveMenuDragMouseListener() {
        MenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        MenuDragMouseListener listener2 = new ConcreteMenuDragMouseListener();
        MenuDragMouseListener listener3 = new ConcreteMenuDragMouseListener();
        EventListener[] listenersArray = null;
        listenersArray = menuItem.getMenuDragMouseListeners();
        int initialValue = listenersArray.length;
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.addMenuDragMouseListener(listener2);
        menuItem.addMenuDragMouseListener(listener2);
        listenersArray = menuItem.getMenuDragMouseListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        menuItem.removeMenuDragMouseListener(listener1);
        menuItem.addMenuDragMouseListener(listener3);
        menuItem.addMenuDragMouseListener(listener3);
        listenersArray = menuItem.getMenuDragMouseListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        menuItem.removeMenuDragMouseListener(listener3);
        menuItem.removeMenuDragMouseListener(listener3);
        listenersArray = menuItem.getMenuDragMouseListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        menuItem.removeMenuDragMouseListener(listener2);
        menuItem.removeMenuDragMouseListener(listener2);
        listenersArray = menuItem.getMenuDragMouseListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.addMenuKeyListener(MenuKeyListener)'
     */
    public void testAddGetRemoveMenuKeyListener() {
        MenuKeyListener listener1 = new ConcreteMenuKeyListener();
        MenuKeyListener listener2 = new ConcreteMenuKeyListener();
        MenuKeyListener listener3 = new ConcreteMenuKeyListener();
        EventListener[] listenersArray = null;
        listenersArray = menuItem.getMenuKeyListeners();
        int initialValue = listenersArray.length;
        menuItem.addMenuKeyListener(listener1);
        menuItem.addMenuKeyListener(listener2);
        menuItem.addMenuKeyListener(listener2);
        listenersArray = menuItem.getMenuKeyListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        menuItem.removeMenuKeyListener(listener1);
        menuItem.addMenuKeyListener(listener3);
        menuItem.addMenuKeyListener(listener3);
        listenersArray = menuItem.getMenuKeyListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        menuItem.removeMenuKeyListener(listener3);
        menuItem.removeMenuKeyListener(listener3);
        listenersArray = menuItem.getMenuKeyListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        menuItem.removeMenuKeyListener(listener2);
        menuItem.removeMenuKeyListener(listener2);
        listenersArray = menuItem.getMenuKeyListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.getComponent()'
     */
    public void testGetComponent() {
        assertSame(menuItem, menuItem.getComponent());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.getSubElements()'
     */
    public void testGetSubElements() {
        assertEquals(0, menuItem.getSubElements().length);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.menuSelectionChanged(boolean)'
     */
    public void testMenuSelectionChanged() {
        assertFalse(menuItem.isArmed());
        menuItem.menuSelectionChanged(true);
        assertTrue(menuItem.isArmed());
        menuItem.menuSelectionChanged(false);
        assertFalse(menuItem.isArmed());
    }

    /*
     * Test method for 'javax.swing.JMenuItem.processMenuKeyEvent(MenuKeyEvent)'
     */
    public void testProcessMenuKeyEvent() {
        final MenuSelectionManager menuSelectionManager = new MenuSelectionManager();
        MenuKeyEvent event1 = new MenuKeyEvent(menuItem, KeyEvent.KEY_PRESSED, 0, 0, 0, 'a',
                new MenuElement[0], menuSelectionManager);
        MenuKeyEvent event2 = new MenuKeyEvent(menuItem, KeyEvent.KEY_RELEASED, 0, 0, 0, 'b',
                new MenuElement[0], menuSelectionManager);
        MenuKeyEvent event3 = new MenuKeyEvent(menuItem, KeyEvent.KEY_TYPED, 0, 0, 0, 'c',
                new MenuElement[0], menuSelectionManager);
        ConcreteMenuKeyListener listener1 = new ConcreteMenuKeyListener();
        menuItem.addMenuKeyListener(listener1);
        menuItem.processMenuKeyEvent(event1);
        assertEquals("event fired properly ", "pressed", listener1.event);
        menuItem.processMenuKeyEvent(event2);
        assertEquals("event fired properly ", "released", listener1.event);
        menuItem.processMenuKeyEvent(event3);
        assertEquals("event fired properly ", "typed", listener1.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.processKeyEvent(KeyEvent, MenuElement[], MenuSelectionManager)'
     */
    public void testProcessKeyEventKeyEventMenuElementArrayMenuSelectionManager() {
        final MenuSelectionManager menuSelectionManager = new MenuSelectionManager();
        KeyEvent event1 = new KeyEvent(menuItem, KeyEvent.KEY_PRESSED, 0, 0, 0, 'a');
        KeyEvent event2 = new KeyEvent(menuItem, KeyEvent.KEY_RELEASED, 0, 0, 0, 'b');
        KeyEvent event3 = new KeyEvent(menuItem, KeyEvent.KEY_TYPED, 0, 0, 0, 'c');
        ConcreteMenuKeyListener listener1 = new ConcreteMenuKeyListener();
        menuItem.addMenuKeyListener(listener1);
        menuItem.processKeyEvent(event1, new MenuElement[0], menuSelectionManager);
        assertEquals("event fired properly ", "pressed", listener1.event);
        menuItem.processKeyEvent(event2, new MenuElement[] {}, menuSelectionManager);
        assertEquals("event fired properly ", "released", listener1.event);
        menuItem.processKeyEvent(event3, new MenuElement[] {}, menuSelectionManager);
        assertEquals("event fired properly ", "typed", listener1.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.processMenuDragMouseEvent(MenuDragMouseEvent)'
     */
    public void testProcessMenuDragMouseEvent() {
        MenuDragMouseEvent event1 = new MenuDragMouseEvent(menuItem, MouseEvent.MOUSE_DRAGGED,
                0, 0, 10, 10, 1, true, new MenuElement[0], new MenuSelectionManager());
        MenuDragMouseEvent event2 = new MenuDragMouseEvent(menuItem, MouseEvent.MOUSE_EXITED,
                0, 0, 10, 10, 1, true, new MenuElement[] { menuItem },
                new MenuSelectionManager());
        MenuDragMouseEvent event3 = new MenuDragMouseEvent(new JButton(),
                MouseEvent.MOUSE_ENTERED, 0, 0, 10, 10, 1, true,
                new MenuElement[] { menuItem }, new MenuSelectionManager());
        MenuDragMouseEvent event4 = new MenuDragMouseEvent(menuItem, MouseEvent.MOUSE_RELEASED,
                0, MouseEvent.BUTTON1, 10, 10, 1, false, new MenuElement[] {},
                new MenuSelectionManager());
        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.processMenuDragMouseEvent(event2);
        assertEquals("exited", listener1.event);
        menuItem.processMenuDragMouseEvent(event1);
        assertEquals("dragged", listener1.event);
        menuItem.processMenuDragMouseEvent(event3);
        assertEquals("entered", listener1.event);
        listener1.event = null;
        menuItem.processMenuDragMouseEvent(event4);
        assertNull(listener1.event);
        menuItem.processMenuDragMouseEvent(event1);
        assertEquals("dragged", listener1.event);
        listener1.event = null;
        menuItem.processMenuDragMouseEvent(event4);
        assertEquals("released", listener1.event);
    }

    /*
     * Test method for 'javax.swing.JMenuItem.processMouseEvent(MouseEvent, MenuElement[], MenuSelectionManager)'
     */
    public void testProcessMouseEventMouseEventMenuElementArrayMenuSelectionManager() {
        MouseEvent event1 = new MouseEvent(menuItem, MouseEvent.MOUSE_DRAGGED, 0, 0, 10, 10, 1,
                true);
        MouseEvent event2 = new MouseEvent(menuItem, MouseEvent.MOUSE_EXITED, 0, 0, 10, 10, 1,
                true);
        MouseEvent event3 = new MouseEvent(new JButton(), MouseEvent.MOUSE_ENTERED, 0, 0, 10,
                10, 1, true);
        MouseEvent event4 = new MouseEvent(menuItem, MouseEvent.MOUSE_RELEASED, 0,
                MouseEvent.BUTTON1, 10, 10, 1, false);
        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        menuItem.addMenuDragMouseListener(listener1);
        menuItem.processMouseEvent(event2, new MenuElement[] {}, new MenuSelectionManager());
        assertEquals("exited", listener1.event);
        menuItem.processMouseEvent(event1, new MenuElement[] {}, new MenuSelectionManager());
        assertEquals("dragged", listener1.event);
        menuItem.processMouseEvent(event3, new MenuElement[] {}, new MenuSelectionManager());
        assertEquals("entered", listener1.event);
        listener1.event = null;
        menuItem.processMouseEvent(event4, new MenuElement[] {}, new MenuSelectionManager());
        assertNull(listener1.event);
        menuItem.processMouseEvent(event1, new MenuElement[] {}, new MenuSelectionManager());
        assertEquals("dragged", listener1.event);
        listener1.event = null;
        menuItem.processMouseEvent(event4, new MenuElement[] {}, new MenuSelectionManager());
        assertEquals("released", listener1.event);
    }

    @Override
    public void testGetUI() {
        assertNotNull("ui is returned ", menuItem.getUI());
    }

    @Override
    public void testIsFocusPainted() {
        assertFalse("default FocusPainted", menuItem.isFocusPainted());
    }

    @Override
    public void testGetHorizontalAlignment() {
    }

    @Override
    public void testConfigurePropertiesFromAction_ShortDescription() {
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
            assertEquals("accelerator ", ks1, menuItem.getAccelerator());
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
            assertEquals("accelerator ", ks2, menuItem.getAccelerator());
        }
    }
}
