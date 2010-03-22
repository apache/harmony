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
 * @author Dmitry A. Durnev
 */
package java.awt;
import java.awt.event.*;

import junit.framework.TestCase;

/**
 * ListTest
 */
public class ListTest extends TestCase {
    private List list;
    private boolean eventProcessed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new List();
        eventProcessed = false;
    }


    private void selectCurrentItem() {
        list.setMultipleMode(true);
        for (int i=0; i< list.getItemCount(); i++) {
            list.select(i);
        }
        list.setMultipleMode(false);
    }
    public final void testAddNotify() {
        Frame f = new Frame("");
        f.add(list);
        assertNull(list.getGraphics());
        list.add("item");
        list.add("item1", 0);
        selectCurrentItem();
        assertEquals(-1, list.getSelectedIndex());
        f.addNotify();
        selectCurrentItem();
        assertEquals(list.getItemCount() - 1, list.getSelectedIndex());
        assertNotNull(list.getGraphics());
        f.dispose();
        assertNull(list.getGraphics());
    }

    public final void testGetAccessibleContext() {
        //TODO Implement getAccessibleContext().
    }

    public final void testParamString() {
        String str = list.paramString();
        assertEquals("name is correct", 0, str.indexOf("list"));
        assertTrue(str.indexOf("selected=" + null) > 0);
    }

    public final void testRemoveNotify() {
        Frame f = new Frame("");
        f.add(list);
        list.add("item");
        list.add("item1", 0);
        f.addNotify();
        assertNotNull(list.getGraphics());
        selectCurrentItem();
        assertEquals(list.getItemCount() - 1, list.getSelectedIndex());
        list.removeNotify();
        selectCurrentItem();
        assertEquals(-1, list.getSelectedIndex());
        assertNull(list.getGraphics());
        f.dispose();
    }

    /*
     * Class under test for java.awt.Dimension getMinimumSize()
     */
    public final void testGetMinimumSize() {
        assertEquals(new Dimension(), list.getMinimumSize());
        Frame f = new Frame("");
        f.add(list);
        f.addNotify();
        Dimension minSize = list.getMinimumSize();
        int fontSize = list.getFont().getSize();
        assertTrue("min height > font size * rows", minSize.height > list.getRows() * fontSize);
        assertTrue("min width > 0", minSize.width > 0);
        f.dispose();
    }

    /*
     * Class under test for java.awt.Dimension minimumSize()
     */
    public final void testMinimumSize() {
        // TODO: deprecated
    }

    /*
     * Class under test for java.awt.Dimension preferredSize()
     */
    public final void testPreferredSize() {
        // TODO: deprecated
    }

    public final void testProcessEvent() {
        eventProcessed = false;
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent a0) {
                eventProcessed = true;
            }
        });
        list.processEvent(new KeyEvent(list, KeyEvent.KEY_PRESSED, 0, 0, 0, 'q'));
        assertTrue(eventProcessed);
    }

    public final void testGetListeners() {
        assertEquals(0, list.getListeners(KeyListener.class).length);
        KeyAdapter listener = new KeyAdapter(){};
        list.addKeyListener(listener);
        Class<KeyListener> clazz =  KeyListener.class;
        assertEquals(1, list.getListeners(clazz).length);
        assertEquals(listener, list.getListeners(clazz)[0]);
        list.removeKeyListener(listener);
        assertEquals(0, list.getListeners(clazz).length);
    }

    /*
     * Class under test for void List(int)
     */
    public final void testListint() {
        int rows = 0;
        list = new List(rows);
        assertEquals(4, list.getRows());
        assertFalse(list.isMultipleMode());
        list = new List(rows = 1);
        assertEquals(rows, list.getRows());
        list = new List(rows = Integer.MAX_VALUE);
        assertEquals(rows, list.getRows());
        list = new List(rows = Integer.MIN_VALUE);
        assertEquals(rows, list.getRows());

    }

    /*
     * Class under test for void List()
     */
    public final void testList() {
        assertEquals(4, list.getRows());
        assertEquals(-1, list.getSelectedIndex());
        assertNull(list.getSelectedItem());
        assertFalse(list.isMultipleMode());
        assertEquals(0, list.getSelectedItems().length);
        assertEquals(0, list.getSelectedIndexes().length);
        assertEquals(0, list.getSelectedObjects().length);
    }

    /*
     * Class under test for void List(int, boolean)
     */
    public final void testListintboolean() {
        int rows = 0;
        list = new List(rows, false);
        assertEquals(4, list.getRows());
        assertFalse(list.isMultipleMode());
        list = new List(rows=13, true);
        assertEquals(rows, list.getRows());
        assertTrue(list.isMultipleMode());
    }

    /*
     * Class under test for void add(java.lang.String)
     */
    public final void testAddString() {
        String item = "item";
        list.add(item);
        assertSame(item, list.getItem(0));
        list.add(item = "item1");
        assertSame(item, list.getItem(1));
        list.add(item = null);
        assertEquals("",list.getItem(2));

    }

    /*
     * Class under test for void add(java.lang.String, int)
     */
    public final void testAddStringint() {
        String item = "item";
        int idx = 0;
        list.add(item, idx);
        assertSame(item, list.getItem(idx));
        list.add(item = "item1", idx);
        assertEquals("item", list.getItem(idx + 1));
        assertSame(item, list.getItem(idx));
        list.add(item = "item2", idx = -20);
        assertSame(item, list.getItem(list.getItemCount() - 1));
        list.add(item = "item3", idx = Integer.MAX_VALUE);
        assertSame(item, list.getItem(list.getItemCount() - 1));
        list.add(item = null, idx = 1);
        assertEquals("", list.getItem(idx));

    }

    /*
     * Class under test for void remove(java.lang.String)
     */
    public final void testRemoveString() {
        String item = "q";
        boolean iae = false;
        try {
            list.remove(item);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        list.add(item);
        list.add(item);
        list.remove(item);
        assertEquals(1, list.getItemCount());
        list.select(0);
        assertSame(item, list.getSelectedItem());
        list.remove(item);
        assertEquals(0, list.getItemCount());
        assertNull(list.getSelectedItem());
        assertEquals(-1, list.getSelectedIndex());
    }

    /*
     * Class under test for void remove(int)
     */
    public final void testRemoveint() {
        int pos = 0;
        boolean exception = false;
        try {
            list.remove(pos);
        } catch (ArrayIndexOutOfBoundsException e) {
            exception = true;
        }
        assertTrue(exception);
        String item = "item";
        list.add(item);
        list.add(item);
        list.remove(1);
        assertEquals(1, list.getItemCount());
        assertSame(item, list.getItem(0));
        list.select(0);
        assertSame(item, list.getSelectedItem());
        list.remove(0);
        assertEquals(0, list.getItemCount());
        assertEquals(-1, list.getSelectedIndex());
        assertNull(list.getSelectedItem());
        list.add(item = "item1");
        list.add("item2");
        list.add("item3");
        list.setMultipleMode(true);
        list.select(0);
        list.select(2);
        assertEquals(2, list.getSelectedIndexes()[1]);
        list.remove(1);
        // test shift of selected indexes:
        assertEquals(1, list.getSelectedIndexes()[1]);

    }

    public final void testClear() {
        // TODO: deprecated
    }

    public final void testRemoveAll() {
        list.add("ppp");
        list.add("qqq");
        list.removeAll();
        assertEquals(0, list.getItemCount());
        assertEquals(0, list.getItems().length);
    }

    /*
     * Class under test for java.awt.Dimension getMinimumSize(int)
     */
    public final void testGetMinimumSizeint() {
        int i = 1;
        assertEquals(new Dimension(), list.getMinimumSize(i));
        Frame f = new Frame("");
        f.add(list);
        f.addNotify();
        Dimension minSize = list.getMinimumSize(i);
        int fontSize = list.getFont().getSize();
        assertTrue("min height > font size * i", minSize.height > i * fontSize);
        assertTrue("min width > 0", minSize.width > 0);
        f.dispose();
    }

    /*
     * Class under test for java.awt.Dimension minimumSize(int)
     */
    public final void testMinimumSizeint() {
        // TODO: deprecated
    }

    /*
     * Class under test for java.awt.Dimension preferredSize(int)
     */
    public final void testPreferredSizeint() {
        // TODO: deprecated
    }

    /*
     * Class under test for void addItem(java.lang.String)
     */
    public final void testAddItemString() {
        // TODO: deprecated
    }

    /*
     * Class under test for void addItem(java.lang.String, int)
     */
    public final void testAddItemStringint() {
        // TODO: deprecated
    }

    public final void testAllowsMultipleSelections() {
        // TODO: deprecated
    }

    public final void testCountItems() {
        // TODO: deprecated
    }

    public final void testDelItem() {
        // TODO: deprecated
    }

    @SuppressWarnings("deprecation")
    public final void testDelItems() {
        String item = "item";
        list.add(item);
        list.add("item1");
        list.add("item2");
        list.delItems(1, 2);
        assertEquals(1, list.getItemCount());
        assertSame(item, list.getItem(0));
    }

    public final void testDeselect() {
        list.add("item");
        list.add("item1");
        list.add("item2");
        int idx = 0;
        list.deselect(idx);
        assertEquals(-1, list.getSelectedIndex());
        list.select(idx);
        assertEquals(idx, list.getSelectedIndex());
        list.deselect(idx);
        assertEquals(-1, list.getSelectedIndex());
        list.select(2);
        list.setMultipleMode(true);
        list.select(idx);
        list.deselect(2);
        assertEquals(idx, list.getSelectedIndex());
        assertEquals(idx, list.getSelectedIndexes()[0]);
    }

    public final void testGetItemCount() {
        assertEquals(0, list.getItemCount());
    }

    public final void testGetItems() {
        assertEquals(0, list.getItems().length);
    }

    public final void testGetRows() {
        assertEquals(4, list.getRows());
    }

    public final void testGetSelectedIndex() {
        assertEquals(-1, list.getSelectedIndex());
        list = new List(5, true);
        list.add("qqq");
        assertEquals(-1, list.getSelectedIndex());
        list.select(0);
        assertEquals(0, list.getSelectedIndex());
        list.add("ppp");
        list.select(1);
        assertEquals(-1, list.getSelectedIndex());
    }

    public final void testGetSelectedIndexes() {
        assertEquals(0, list.getSelectedIndexes().length);
        String item = "item";
        list.add(item);
        int idx = 0;
        list.select(idx);
        assertEquals(idx, list.getSelectedIndexes()[0]);
        list.setMultipleMode(true);
        list.add(item);
        list.select(0);
        assertEquals(idx, list.getSelectedIndexes()[0]);
        list.add(item = "item1");
        list.select(idx = 1);
        assertEquals(idx, list.getSelectedIndexes()[1]);
    }

    public final void testGetSelectedItem() {
        assertNull(list.getSelectedItem());
    }

    public final void testGetSelectedItems() {
        assertEquals(0, list.getSelectedItems().length);
    }

    public final void testGetSelectedObjects() {
        assertEquals(0, list.getSelectedObjects().length);
    }

    public final void testGetVisibleIndex() {
        assertEquals(-1, list.getVisibleIndex());
    }

    public final void testIsIndexSelected() {
        assertFalse(list.isIndexSelected(-1));
        assertFalse(list.isIndexSelected(0));
        list.add("item");
        list.add("item1");
        list.add("item2");
        list.select(0);
        assertTrue(list.isIndexSelected(0));
        assertFalse(list.isIndexSelected(1));
        list.select(1);
        assertTrue(list.isIndexSelected(1));
        assertFalse(list.isIndexSelected(0));
        list.setMultipleMode(true);
        list.select(2);
        assertTrue(list.isIndexSelected(1));
        assertTrue(list.isIndexSelected(2));
        assertFalse(list.isIndexSelected(0));
    }

    public final void testIsMultipleMode() {
        assertFalse(list.isMultipleMode());
    }

    public final void testIsSelected() {
        // TODO: deprecated
    }

    public final void testMakeVisible() {
        list.add("item1");
        list.add("item2");
        int idx = -1;
        list.makeVisible(idx);
        assertEquals(idx, list.getVisibleIndex());
        list.makeVisible(idx = 1);
        assertEquals(idx, list.getVisibleIndex());
    }

    public final void testReplaceItem() {
        int pos = 0;
        boolean exception = false;
        String item = "", item1 = "item1";
        try {
            list.replaceItem(item, pos);
        } catch (ArrayIndexOutOfBoundsException e) {
            exception = true;
        }
        assertTrue(exception);
        list.add(item = "item");
        list.replaceItem(item1, pos);
        assertSame(item1, list.getItem(pos));
    }

    public final void testSelect() {
        list.add("item");
        list.add("item1");
        list.add("item2");
        int idx = 0;
        list.select(idx);
        assertEquals(idx, list.getSelectedIndex());
        list.select(idx = 1);
        assertEquals(idx, list.getSelectedIndex());

        assertEquals(idx, list.getSelectedIndexes()[0]);
        list.setMultipleMode(true);
        assertEquals(idx, list.getSelectedIndex());
        assertEquals(idx, list.getSelectedIndexes()[0]);
        list.select(idx = 2);
        assertEquals(-1, list.getSelectedIndex());
        int [] indexes = list.getSelectedIndexes();
        assertEquals(1, indexes[0]);
        assertEquals(idx, indexes[1]);

    }

//    public final void testCrash() {
//      System.out.println((new int[0])[0]);
//    }

    public final void testSetMultipleMode() {
        list.setMultipleMode(true);
        assertTrue(list.isMultipleMode());
        list.add("item");
        list.add("item1");
        list.select(1);
        list.select(0);
        Frame f = new Frame("");
        f.add(list);
        f.pack();
        list.setMultipleMode(false);
        assertFalse(list.isMultipleMode());
        assertEquals(0, list.getSelectedIndex());
        f.dispose();
    }

    public final void testSetMultipleSelections() {
        // TODO: deprecated
    }

    public final void testAddGetRemoveActionListener() {
        assertEquals(0, list.getActionListeners().length);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {}
        };
        list.addActionListener(listener);
        assertEquals(1, list.getActionListeners().length);
        assertSame(listener, list.getActionListeners()[0]);

        list.removeActionListener(listener);
        assertEquals(0, list.getActionListeners().length);
    }

    public final void testAddGetRemoveItemListener() {
        assertEquals(0, list.getItemListeners().length);

        ItemListener listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {}
        };

        list.addItemListener(listener);
        assertEquals(1, list.getItemListeners().length);
        assertSame(listener, list.getItemListeners()[0]);

        list.removeItemListener(listener);
        assertEquals(0, list.getItemListeners().length);
    }

    public final void testProcessItemEvent() {
        eventProcessed = false;
        list.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                eventProcessed = true;
            }
        });
        list.processEvent(new ItemEvent(list, ItemEvent.ITEM_STATE_CHANGED,
                                          null, ItemEvent.SELECTED));
        assertTrue(eventProcessed);
    }

    public final void testProcessActionEvent() {
        eventProcessed = false;
        list.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                eventProcessed = true;
            }
        });
        list.processEvent(new ActionEvent(list, ActionEvent.ACTION_PERFORMED,
                                          null, 0, 0));
        assertTrue(eventProcessed);
    }

}
