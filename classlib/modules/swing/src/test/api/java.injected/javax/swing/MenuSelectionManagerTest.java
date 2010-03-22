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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MenuSelectionManagerTest extends SwingTestCase {
    //    class ConcreteMenuDragMouseListener implements MenuDragMouseListener {
    //        public Object event;
    //        public void menuDragMouseDragged(MenuDragMouseEvent e) {
    //            event = "dragged";
    //        }
    //        public void menuDragMouseEntered(MenuDragMouseEvent e) {
    //            event = "entered";
    //        }
    //        public void menuDragMouseExited(MenuDragMouseEvent e) {
    //            event = "exited";
    //        }
    //        public void menuDragMouseReleased(MenuDragMouseEvent e) {
    //            event = "released";
    //        }
    //    };
    //    JMenuItem itemitem = new JMenuItem() {
    //        public void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager) {
    //            System.out.println("itemitem: processKeyEvent");
    //            super.processKeyEvent(event, path, manager);
    //        }
    //
    //        public int getMnemonic() {
    //            System.out.println("itemitem: getMnemonic");
    //            return super.getMnemonic();
    //        }
    //    };
    class ConcreteMenuElement extends JComponent implements MenuElement {
        private static final long serialVersionUID = 1L;

        private Component component;

        private String name;

        public boolean selected = false;

        public ConcreteMenuElement(String name, Component component) {
            this.component = component;
            this.name = name;
        }

        public ConcreteMenuElement(String name) {
            this.component = this;
            this.name = name;
        }

        public void processMouseEvent(MouseEvent event, MenuElement[] path,
                MenuSelectionManager manager) {
            //            System.out.println("processMouseEvent (" + name + ")");
        }

        public void processKeyEvent(KeyEvent event, MenuElement[] path,
                MenuSelectionManager manager) {
            //            System.out.println("processKeyEvent (" + name + ")");
        }

        public void menuSelectionChanged(boolean isIncluded) {
            //            System.out.println("menuSelectionChanged (" + name + ")" + ": " + isIncluded);
            selected = isIncluded;
        }

        public MenuElement[] getSubElements() {
            //            System.out.println("getSubElements (" + name + ")");
            return new MenuElement[] { new ConcreteMenuElement(name + "1", component),
                    new ConcreteMenuElement(name + "2", component) };
        }

        public Component getComponent() {
            //            System.out.println("getComponent (" + name + ")");
            return component;
        }
    }

    class ConcreteChangeListener implements ChangeListener {
        public ChangeEvent eventHappened;

        private final boolean debugOut;

        public ConcreteChangeListener() {
            debugOut = false;
        }

        public ConcreteChangeListener(final boolean debugOut) {
            this.debugOut = debugOut;
        }

        public void stateChanged(final ChangeEvent event) {
            eventHappened = event;
            if (debugOut) {
                System.out.println("stateChanged");
                System.out.println("Class " + event.getClass());
                System.out.println("Source " + event.getSource());
                System.out.println();
            }
        }
    };

    protected MenuSelectionManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new MenuSelectionManager();
    }

    @Override
    protected void tearDown() throws Exception {
        manager = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.MenuSelectionManager()'
     */
    public void testMenuSelectionManager() {
        assertNotNull(manager.listenerList);
        assertEquals(0, manager.listenerList.getListenerCount());
        assertNull(manager.changeEvent);
        assertEquals(0, manager.getSelectedPath().length);
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.addChangeListener(ChangeListener)'
     */
    public void testAddGetRemoveChangeListener() {
        ChangeListener listener1 = new ConcreteChangeListener();
        ChangeListener listener2 = new ConcreteChangeListener();
        ChangeListener listener3 = new ConcreteChangeListener();
        EventListener[] listenersArray = null;
        listenersArray = manager.getChangeListeners();
        int initialValue = listenersArray.length;
        manager.addChangeListener(listener1);
        manager.addChangeListener(listener2);
        manager.addChangeListener(listener2);
        listenersArray = manager.getChangeListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        manager.removeChangeListener(listener1);
        manager.addChangeListener(listener3);
        manager.addChangeListener(listener3);
        listenersArray = manager.getChangeListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        manager.removeChangeListener(listener3);
        manager.removeChangeListener(listener3);
        listenersArray = manager.getChangeListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        manager.removeChangeListener(listener2);
        manager.removeChangeListener(listener2);
        listenersArray = manager.getChangeListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.componentForPoint(Component, Point)'
     */
    public void testComponentForPoint() {
        // TODO implement
        //        final JMenuBar menuBar = new JMenuBar();
        //        final JMenu menu1 = new JMenu();
        //        final JMenu menu2 = new JMenu();
        //        final JMenu menu3 = new JMenu();
        //        JFrame frame = new JFrame();
        //        frame.getContentPane().add(menuBar);
        //        menuBar.add(menu1);
        //        menu1.add(menu2);
        //        menu2.add(menu3);
        //        menu1.setPreferredSize(new Dimension(20, 20));
        //        menu2.setPreferredSize(new Dimension(100, 100));
        //        menu3.setPreferredSize(new Dimension(100, 100));
        //        MenuElement[] path1 = new MenuElement[] {menuBar, menu1, menu2};
        //        MenuElement[] path2 = new MenuElement[] {menu3, menu1, menu2};
        //        MenuElement[] path4 = new MenuElement[] {menu1};
        //        MenuElement[] path5 = new MenuElement[] {menu2};
        //        MenuElement[] path6 = new MenuElement[] {menu3};
        //        frame.pack();
        //        frame.show();
        //        manager.setSelectedPath(path1);
        //        for (int i = 0; i < 100; i++) {
        //            if (manager.componentForPoint(frame, new Point(i, i)) != null) {
        //                System.out.println(manager.componentForPoint(frame, new Point(i, i)));
        //            }
        //        }
        //        menu1.setPopupMenuVisible(true);
        //        manager.setSelectedPath(path1);
        //        for (int i = 0; i < 100; i++) {
        //            if (manager.componentForPoint(frame, new Point(i, i)) != null) {
        //                System.out.println(manager.componentForPoint(frame, new Point(i, i)));
        //            }
        //        }
        //        frame.dispose();
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.defaultManager()'
     */
    public void testDefaultManager() {
        assertNotNull(MenuSelectionManager.defaultManager());
        assertSame(MenuSelectionManager.defaultManager(), MenuSelectionManager.defaultManager());
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.fireStateChanged()'
     */
    public void testFireStateChanged() {
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteChangeListener listener2 = new ConcreteChangeListener();
        manager.addChangeListener(listener1);
        manager.addChangeListener(listener2);
        manager.fireStateChanged();
        assertNotNull("event fired properly ", listener1.eventHappened);
        assertNotNull("event fired properly ", listener2.eventHappened);
        assertSame("one event fired ", listener1.eventHappened, listener2.eventHappened);
        ChangeEvent event1 = listener1.eventHappened;
        listener1.eventHappened = null;
        listener2.eventHappened = null;
        manager.fireStateChanged();
        assertNotNull("event fired properly ", listener1.eventHappened);
        assertNotNull("event fired properly ", listener2.eventHappened);
        assertSame("one event fired ", listener1.eventHappened, listener2.eventHappened);
        assertSame("one event fired ", event1, listener1.eventHappened);
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.isComponentPartOfCurrentMenu(Component)'
     */
    public void testIsComponentPartOfCurrentMenu() {
        final JMenu menu1 = new JMenu();
        final JMenu menu2 = new JMenu();
        final JMenu menu3 = new JMenu();
        final JMenu menu4 = new JMenu();
        final JMenuItem menu5 = new JMenuItem();
        final JMenuBar menu6 = new JMenuBar();
        final JMenuItem menu7 = new JMenuItem();
        menu6.add(menu4);
        menu4.add(menu5);
        menu6.add(menu7);
        menu1.add(menu2);
        menu3.add(menu1);
        MenuElement[] path1 = new MenuElement[] { menu1, menu2, new JMenuItem() };
        MenuElement[] path2 = new MenuElement[] { menu3, menu1, new JCheckBoxMenuItem() };
        MenuElement[] path3 = new MenuElement[] { menu4, menu1, new JCheckBoxMenuItem() };
        MenuElement[] path4 = new MenuElement[] { menu3 };
        MenuElement[] path5 = new MenuElement[] { menu5 };
        MenuElement[] path6 = new MenuElement[] { menu6 };
        assertFalse(manager.isComponentPartOfCurrentMenu(menu1));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu2));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu3));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu4));
        manager.setSelectedPath(path1);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu1));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu2));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu3));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu4));
        manager.setSelectedPath(path2);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu1));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu2));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu3));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu4));
        manager.setSelectedPath(path3);
        assertFalse(manager.isComponentPartOfCurrentMenu(menu1));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu2));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu3));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu4));
        manager.setSelectedPath(path4);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu1));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu2));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu3));
        assertFalse(manager.isComponentPartOfCurrentMenu(menu4));
        manager.setSelectedPath(path5);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu5));
        manager.setSelectedPath(path6);
        assertTrue(manager.isComponentPartOfCurrentMenu(menu6));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu4));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu5));
        assertTrue(manager.isComponentPartOfCurrentMenu(menu7));
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.processKeyEvent(KeyEvent)'
     */
    public void testProcessKeyEvent() {
        // TODO implement
        //        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        //        Component c = new ConcreteMenuElement("container", new JMenu());
        //        ConcreteMenuElement element1 = new ConcreteMenuElement("elem1", c);
        //        ConcreteMenuElement element2 = new ConcreteMenuElement("elem2", c);
        //        MenuElement[] path1 = new MenuElement[] {element1};
        //        MenuElement[] path2 = new MenuElement[] {element1, element2};
        //        manager.addChangeListener(listener1);
        //
        //        itemitem.setMnemonic(KeyEvent.VK_A);
        //        itemitem.addActionListener(new ActionListener() {
        //            public void actionPerformed(ActionEvent e) {
        //                System.out.println("Item");
        //            }
        //        });
        //        KeyEvent event = new KeyEvent(itemitem, KeyEvent.KEY_PRESSED, EventQueue.getMostRecentEventTime(), 0, KeyEvent.VK_A);
        //        manager.processKeyEvent(event);
        //        manager.setSelectedPath(new MenuElement[] {element1, element2});
        //        manager.processKeyEvent(event);
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.processMouseEvent(MouseEvent)'
     */
    public void testProcessMouseEvent() {
        //        JMenuItem menuItem = new JMenuItem();
        //        MouseEvent event1 = new MouseEvent(menuItem, MouseEvent.MOUSE_DRAGGED,
        //                                           0, 0, 10, 10, 1, true);
        //        MouseEvent event2 = new MouseEvent(menuItem, MouseEvent.MOUSE_EXITED,
        //                                           0, 0, 10, 10, 1, true);
        //        MouseEvent event3 = new MouseEvent(new JButton(), MouseEvent.MOUSE_ENTERED,
        //                                           0, 0, 10, 10, 1, true);
        //        MouseEvent event4 = new MouseEvent(menuItem, MouseEvent.MOUSE_RELEASED,
        //                                           0, MouseEvent.BUTTON1, 10, 10, 1, false);
        //        ConcreteMenuDragMouseListener listener1 = new ConcreteMenuDragMouseListener();
        //        menuItem.addMenuDragMouseListener(listener1);
        //
        //        manager.setSelectedPath(new MenuElement[] {menuItem});
        //        manager.processMouseEvent(event1);
        //        assertEquals("exited", listener1.event);
        //
        //        manager.processMouseEvent(event1);
        //        assertEquals("dragged", listener1.event);
        //
        //        manager.processMouseEvent(event3);
        //        assertEquals("entered", listener1.event);
        //        listener1.event = null;
        //
        //        manager.processMouseEvent(event4);
        //        assertNull(listener1.event);
        //
        //        manager.processMouseEvent(event1);
        //        assertEquals("dragged", listener1.event);
        //        listener1.event = null;
        //
        //        manager.processMouseEvent(event4);
        //        assertEquals("released", listener1.event);

        try {   
            MenuSelectionManager m = new MenuSelectionManager(); 
            m.processMouseEvent(null); 
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {              
            // PASSED            
        }
    }

    /*
     * Test method for 'javax.swing.MenuSelectionManager.setSelectedPath(MenuElement[])'
     */
    public void testSetGetClearSelectedPath() {
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        MenuElement[] path1 = new MenuElement[] { new JMenu(), new JMenu(), new JMenuItem() };
        MenuElement[] path2 = new MenuElement[] { new JMenu(), new JMenu(),
                new JCheckBoxMenuItem() };
        manager.addChangeListener(listener1);
        manager.setSelectedPath(path1);
        assertNotSame(path1, manager.getSelectedPath());
        assertEquals(path1.length, manager.getSelectedPath().length);
        assertSame(path1[0], manager.getSelectedPath()[0]);
        assertSame(path1[1], manager.getSelectedPath()[1]);
        assertSame(path1[2], manager.getSelectedPath()[2]);
        assertNotNull(listener1.eventHappened);
        assertSame(manager, listener1.eventHappened.getSource());
        listener1.eventHappened = null;
        assertNotSame(manager.getSelectedPath(), manager.getSelectedPath());
        manager.clearSelectedPath();
        assertEquals(0, manager.getSelectedPath().length);
        assertNotNull(listener1.eventHappened);
        assertSame(manager, listener1.eventHappened.getSource());
        manager.setSelectedPath(path2);
        assertNotSame(path2, manager.getSelectedPath());
        assertEquals(path2.length, manager.getSelectedPath().length);
        assertSame(path2[0], manager.getSelectedPath()[0]);
        assertSame(path2[1], manager.getSelectedPath()[1]);
        assertSame(path2[2], manager.getSelectedPath()[2]);
        assertNotNull(listener1.eventHappened);
        assertSame(manager, listener1.eventHappened.getSource());
        listener1.eventHappened = null;
        manager.setSelectedPath(path2);
        assertEquals(path2.length, manager.getSelectedPath().length);
        if (!isHarmony()) {
            assertNotNull(listener1.eventHappened);
        } else {
            assertNull(listener1.eventHappened);
        }
        listener1.eventHappened = null;
        manager.setSelectedPath(null);
        assertEquals(0, manager.getSelectedPath().length);
        assertNotNull(listener1.eventHappened);
        assertSame(manager, listener1.eventHappened.getSource());
    }

    public void testSetSelectedPath() {
        ConcreteChangeListener listener1 = new ConcreteChangeListener();
        ConcreteMenuElement element1 = new ConcreteMenuElement("elem1", null);
        ConcreteMenuElement element2 = new ConcreteMenuElement("elem2", null);
        MenuElement[] path1 = new MenuElement[] { element1 };
        MenuElement[] path2 = new MenuElement[] { element1, element2 };
        manager.addChangeListener(listener1);
        manager.setSelectedPath(path1);
        assertTrue(element1.selected);
        assertFalse(element2.selected);
        manager.setSelectedPath(path2);
        assertTrue(element1.selected);
        assertTrue(element2.selected);
        manager.setSelectedPath(path1);
        assertTrue(element1.selected);
        assertFalse(element2.selected);
        manager.clearSelectedPath();
        assertFalse(element1.selected);
        assertFalse(element2.selected);
        manager.setSelectedPath(path2);
        assertTrue(element1.selected);
        assertTrue(element2.selected);
        manager.clearSelectedPath();
        assertFalse(element1.selected);
        assertFalse(element2.selected);
    }
}
