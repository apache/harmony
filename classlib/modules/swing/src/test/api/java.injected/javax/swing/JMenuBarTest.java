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

import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.MenuBarUI;
import javax.swing.plaf.basic.BasicMenuBarUI;
import javax.swing.plaf.metal.MetalMenuBarUI;

public class JMenuBarTest extends SwingTestCase {
    protected JMenuBar menuBar;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuBar = new JMenuBar();
    }

    @Override
    protected void tearDown() throws Exception {
        menuBar = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.JMenuBar.JMenuBar()'
     */
    public void testJMenuBar() {
        assertTrue(menuBar.getSelectionModel() instanceof DefaultSingleSelectionModel);
        assertEquals(0, menuBar.getComponentCount());
        assertTrue(menuBar.isFocusable());
        assertEquals(menuBar, menuBar.getComponent());
        assertTrue(menuBar.isBorderPainted());
        assertTrue(menuBar.isOpaque());
        assertEquals(0, menuBar.getSubElements().length);
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getAccessibleContext()'
     */
    public void testGetAccessibleContext() {
        boolean assertedValue = (menuBar.getAccessibleContext() != null && menuBar
                .getAccessibleContext().getClass().getName().equals(
                        "javax.swing.JMenuBar$AccessibleJMenuBar"));
        assertTrue("AccessibleContext created properly ", assertedValue);
        assertEquals("AccessibleRole", AccessibleRole.MENU_BAR, menuBar.getAccessibleContext()
                .getAccessibleRole());
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getComponentIndex(Component)'
     */
    public void testGetComponentIndex() {
        JMenuItem item1 = new JMenuItem();
        JMenuItem item2 = new JMenuItem();
        JMenuItem item3 = new JMenuItem();
        menuBar.add(item1);
        menuBar.add(item2);
        assertEquals(0, menuBar.getComponentIndex(item1));
        assertEquals(1, menuBar.getComponentIndex(item2));
        assertEquals(-1, menuBar.getComponentIndex(item3));
    }

    /*
     * Test method for 'javax.swing.JMenuBar.processKeyBinding(KeyStroke, KeyEvent, int, boolean)'
     */
    public void testProcessKeyBinding() {
        JMenuBar jm = new JMenuBar() {
            public boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                    int condition, boolean pressed) {
                return super.processKeyBinding(ks, e, condition, pressed);
            }
        };
        KeyStroke ks = KeyStroke.getKeyStroke('x');

        try { // Regression test for HARMONY-2622
            jm.processKeyBinding(ks, null, -1, true);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        } catch (NullPointerException e) {
            fail("NullPointerException is thrown instead of IllegalArgumentException");
        }

        // TODO implement other checkings
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getUIClassID()'
     */
    public void testGetUIClassID() {
        assertEquals("MenuBarUI", menuBar.getUIClassID());
    }

    /*
     * Test method for 'javax.swing.JMenuBar.add(JMenu)'
     */
    public void testAddJMenu() {
        JMenu item1 = new JMenu();
        JMenu item2 = new JMenu();
        menuBar.add(item1);
        assertEquals(1, menuBar.getComponentCount());
        assertSame(item1, menuBar.getComponent(0));
        menuBar.add(item2);
        assertEquals(2, menuBar.getComponentCount());
        assertSame(item1, menuBar.getComponent(0));
        assertSame(item2, menuBar.getComponent(1));
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getComponent()'
     */
    public void testGetComponent() {
        assertSame(menuBar, menuBar.getComponent());
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getMenu(int)'
     */
    public void testGetMenu() {
        JMenu menu1 = new JMenu();
        JMenu menu2 = new JMenu();
        menuBar.add(menu1);
        assertEquals(menu1, menuBar.getMenu(0));
        menuBar.add(menu2);
        assertEquals(menu1, menuBar.getMenu(0));
        assertEquals(menu2, menuBar.getMenu(1));
        menuBar.add(new JButton());
        assertEquals(menu1, menuBar.getMenu(0));
        assertEquals(menu2, menuBar.getMenu(1));
        assertNull(menuBar.getMenu(2));

        try {         
            JMenuBar jm = new JMenuBar();
            jm.getMenu(0);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {    
            // PASSED            
        }
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getMenuCount()'
     */
    public void testGetMenuCount() {
        JMenu item1 = new JMenu();
        JMenuItem item2 = new JMenuItem();
        assertEquals(0, menuBar.getMenuCount());
        menuBar.add(item1);
        assertEquals(1, menuBar.getMenuCount());
        menuBar.add(item2);
        assertEquals(2, menuBar.getMenuCount());
        menuBar.add(new JSeparator());
        assertEquals(3, menuBar.getMenuCount());
        menuBar.add(new JButton(), 0);
        assertEquals(4, menuBar.getMenuCount());
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getSubElements()'
     */
    public void testGetSubElements() {
        JMenu item1 = new JMenu();
        JMenuItem item2 = new JMenuItem();
        JMenu item3 = new JMenu();
        JMenu item4 = new JMenu();
        assertEquals(0, menuBar.getSubElements().length);
        menuBar.add(item1);
        assertEquals(1, menuBar.getSubElements().length);
        assertSame(item1, menuBar.getSubElements()[0]);
        menuBar.add(item2);
        menuBar.add(new JButton());
        assertEquals(2, menuBar.getSubElements().length);
        assertSame(item1, menuBar.getSubElements()[0]);
        assertSame(item2, menuBar.getSubElements()[1]);
        menuBar.add(item3);
        assertEquals(3, menuBar.getSubElements().length);
        assertSame(item1, menuBar.getSubElements()[0]);
        assertSame(item2, menuBar.getSubElements()[1]);
        assertSame(item3, menuBar.getSubElements()[2]);
        menuBar.add(new JButton());
        assertEquals(3, menuBar.getSubElements().length);
        assertSame(item1, menuBar.getSubElements()[0]);
        assertSame(item2, menuBar.getSubElements()[1]);
        assertSame(item3, menuBar.getSubElements()[2]);
        item3.add(item4);
        assertEquals(3, menuBar.getSubElements().length);
    }

    /*
     * Test method for 'javax.swing.JMenuBar.isSelected()'
     */
    public void testIsSelected() {
        SingleSelectionModel selectionModel1 = new DefaultSingleSelectionModel();
        JMenu item1 = new JMenu();
        JMenuItem item2 = new JMenuItem();
        if (isHarmony()) {
            menuBar.setSelectionModel(null);
            assertFalse(menuBar.isSelected());
        }
        menuBar.setSelectionModel(selectionModel1);
        menuBar.add(item1);
        menuBar.add(item2);
        assertFalse(menuBar.isSelected());
        menuBar.setSelected(item1);
        assertTrue(menuBar.isSelected());
        menuBar.setSelected(item2);
        assertTrue(menuBar.isSelected());
        menuBar.setSelected(new JButton());
        assertFalse(menuBar.isSelected());
    }

    /*
     * Test method for 'javax.swing.JPopupMenu.isBorderPainted()'
     */
    public void testSetIsBorderPainted() {
        PropertyChangeController listener = new PropertyChangeController();
        menuBar.addPropertyChangeListener(listener);
        assertTrue(menuBar.isBorderPainted());
        menuBar.setBorderPainted(false);
        assertFalse(menuBar.isBorderPainted());
        listener.checkLastPropertyFired(menuBar, "borderPainted", Boolean.TRUE, Boolean.FALSE);
        listener.reset();
        menuBar.setBorderPainted(true);
        listener.checkLastPropertyFired(menuBar, "borderPainted", Boolean.FALSE, Boolean.TRUE);
        assertTrue(menuBar.isBorderPainted());
        listener.reset();
        menuBar.setBorderPainted(true);
        assertFalse(listener.isChanged("borderPainted"));
    }

    /*
     * Test method for 'javax.swing.JMenuBar.setHelpMenu(JMenu)'
     */
    public void testSetGetHelpMenu() {
        try {
            menuBar.setHelpMenu(new JMenu());
            fail("no exception has been thrown");
        } catch (Error e) {
        }
        try {
            menuBar.getHelpMenu();
            fail("no exception has been thrown");
        } catch (Error e) {
        }
    }

    /*
     * Test method for 'javax.swing.JMenuBar.setMargin(Insets)'
     */
    public void testSetGetMargin() {
        Insets defaultMargin = new Insets(2, 14, 2, 14);
        menuBar.setMargin(defaultMargin);
        Insets margin1 = new Insets(1, 1, 1, 1);
        Insets margin2 = new Insets(2, 2, 2, 2);
        PropertyChangeController listener1 = new PropertyChangeController();
        menuBar.addPropertyChangeListener(listener1);
        menuBar.setMargin(margin1);
        assertSame(margin1, menuBar.getMargin());
        listener1.checkLastPropertyFired(menuBar, "margin", defaultMargin, margin1);
        listener1.reset();
        menuBar.setMargin(margin2);
        assertSame(margin2, menuBar.getMargin());
        listener1.checkLastPropertyFired(menuBar, "margin", margin1, margin2);
        listener1.reset();
        menuBar.setMargin(margin2);
        assertFalse(listener1.isChanged("margin"));
        listener1.reset();
    }

    /*
     * Test method for 'javax.swing.JMenuBar.setSelected(Component)'
     */
    public void testSetSelected() {
        SingleSelectionModel selectionModel1 = new DefaultSingleSelectionModel();
        JMenu item1 = new JMenu();
        JMenuItem item2 = new JMenuItem();
        menuBar.setSelectionModel(null);
        menuBar.add(item1);
        menuBar.add(item2);
        menuBar.setSelectionModel(selectionModel1);
        menuBar.setSelected(item1);
        assertEquals("selection", 0, selectionModel1.getSelectedIndex());
        menuBar.setSelected(item2);
        assertEquals("selection", 1, selectionModel1.getSelectedIndex());
        menuBar.setSelected(new JButton());
        assertEquals("selection", -1, selectionModel1.getSelectedIndex());
    }

    /*
     * Test method for 'javax.swing.JMenuBar.setSelectionModel(SingleSelectionModel)'
     */
    public void testSetGetSelectionModel() {
        SingleSelectionModel selectionModel1 = new DefaultSingleSelectionModel();
        SingleSelectionModel selectionModel2 = new DefaultSingleSelectionModel();
        PropertyChangeController listener = new PropertyChangeController();
        menuBar.setSelectionModel(null);
        menuBar.addPropertyChangeListener(listener);
        menuBar.setSelectionModel(selectionModel1);
        assertEquals("selectionModel", selectionModel1, menuBar.getSelectionModel());
        listener.checkLastPropertyFired(menuBar, "selectionModel", null, selectionModel1);
        listener.reset();
        menuBar.setSelectionModel(selectionModel2);
        assertEquals("selectionModel", selectionModel2, menuBar.getSelectionModel());
        listener.checkLastPropertyFired(menuBar, "selectionModel", selectionModel1,
                selectionModel2);
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.JMenuBar.getUI()'
     */
    public void testGetUI() {
        assertNotNull(menuBar.getUI());
    }

    /*
     * Test method for 'javax.swing.JMenuBar.setUI(MenuBarUI)'
     */
    public void testSetUIMenuBarUI() {
        MenuBarUI ui1 = new BasicMenuBarUI();
        MenuBarUI ui2 = new MetalMenuBarUI();
        JMenuBar menuBar = new JMenuBar();
        menuBar.setUI(ui1);
        assertSame(ui1, menuBar.getUI());
        menuBar.setUI(ui2);
        assertSame(ui2, menuBar.getUI());
    }
}
