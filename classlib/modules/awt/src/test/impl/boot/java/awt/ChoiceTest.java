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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import junit.framework.TestCase;

/**
 * ChoiceTest
 */
public class ChoiceTest extends TestCase {

    private Choice choice;
    private boolean eventProcessed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        choice = new Choice();
        eventProcessed = false;
    }

    public final void testAddNotify() {
        Frame f = new Frame("");
        f.add(choice);
        assertNull(choice.getGraphics());
        assertEquals(new Dimension(), choice.getMinimumSize());
        choice.add("item");
        assertEquals(0, choice.getSelectedIndex());
        choice.add("item1");
        f.addNotify();

        assertNotNull(choice.getGraphics());
        Dimension minSize = choice.getMinimumSize();
        assertTrue(minSize.height > 0);
        assertTrue(minSize.width > 0);
        f.dispose();
        assertNull(choice.getGraphics());
    }

    public final void testGetAccessibleContext() {
        //TODO Implement getAccessibleContext().
    }

    public final void testParamString() {
        String str = choice.paramString();
        assertEquals("name is correct", 0, str.indexOf("choice"));
        assertTrue(str.indexOf("current=" + null) > 0);
    }

    public final void testProcessEvent() {
        eventProcessed = false;
        choice.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                eventProcessed = true;
            }
        });
        choice.processEvent(new KeyEvent(choice, KeyEvent.KEY_RELEASED,
                                         0, 0, 0, 'q'));
        assertTrue(eventProcessed);
    }

    public final void testGetListeners() {
        assertEquals(0, choice.getListeners(KeyListener.class).length);
        KeyAdapter listener = new KeyAdapter(){};
        choice.addKeyListener(listener);
        Class<KeyListener> clazz =  KeyListener.class;
        assertEquals(1, choice.getListeners(clazz).length);
        assertEquals(listener, choice.getListeners(clazz)[0]);
        choice.removeKeyListener(listener);
        assertEquals(0, choice.getListeners(clazz).length);
    }

    public final void testChoice() {
        assertFalse(choice.isLightweight());
        assertEquals(-1, choice.getSelectedIndex());
        assertNull(choice.getSelectedItem());
    }

    /*
     * Class under test for void add(java.lang.String)
     */
    public final void testAddString() {
        String item = "item";
        choice.add(item);
        assertSame(item, choice.getItem(0));
        choice.add(item = "item1");
        assertSame(item, choice.getItem(1));
        boolean npe = false;
        try {
            choice.add(item = null);
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
    }

    /*
     * Class under test for void remove(java.lang.String)
     */
    public final void testRemoveString() {
        String item = "q";
        boolean exception = false;
        try {
            choice.remove(item);
        } catch (IllegalArgumentException e) {
            exception = true;
        }
        assertTrue(exception);
        choice.add("pp");
        choice.add(item);
        choice.add("qq");
        choice.add(item);
        choice.select(1);
        choice.remove(item);
        assertEquals(3, choice.getItemCount());
        assertEquals("pp", choice.getItem(0));
        assertEquals("qq", choice.getItem(1));
        assertSame(item, choice.getItem(2));
        assertEquals(0, choice.getSelectedIndex());
    }

    /*
     * Class under test for void remove(int)
     */
    public final void testRemoveint() {
        int pos = 0;
        boolean exception = false;
        try {
            choice.remove(pos);
        } catch (IndexOutOfBoundsException e) {
            exception = true;
        }
        assertTrue(exception);
        String item = "item";
        choice.add(item);
        choice.add(item);
        choice.remove(1);
        assertEquals(1, choice.getItemCount());
        assertSame(item, choice.getItem(0));
        choice.select(0);
        assertSame(item, choice.getSelectedItem());
        choice.remove(0);
        assertEquals(0, choice.getItemCount());
        assertEquals(-1, choice.getSelectedIndex());
        assertNull(choice.getSelectedItem());
        choice.add(item = "item1");
        choice.add("item2");
        choice.add("item3");
        choice.select(2);
        // test shift of selected index:
        choice.remove(0);
        assertEquals(1, choice.getSelectedIndex());
        choice.remove(1);
        assertEquals(0, choice.getSelectedIndex());
    }

    public final void testRemoveAll() {
        choice.add("ppp");
        choice.add("qqq");
        assertEquals(2, choice.getItemCount());
        choice.removeAll();
        assertEquals(0, choice.getItemCount());
    }

    public final void testInsert() {
        String item = "item";
        boolean iae = false;
        try {
            choice.insert(item, -1);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        int pos = 0;
        choice.insert(item = "item1", pos);
        assertSame(item, choice.getItem(pos));
        assertEquals(0, choice.getSelectedIndex());
        choice.add("item2");
        choice.add("item3");
        choice.select(2);
        assertEquals(2, choice.getSelectedIndex());
        choice.insert(item = "item", pos = 1);
        assertSame(item, choice.getItem(pos));
        assertEquals("item2", choice.getItem(pos + 1));
        assertEquals("item3", choice.getItem(pos + 2));
        assertEquals(0, choice.getSelectedIndex());
        choice.select(2);
        choice.insert(item = "end", choice.getItemCount() + 100);
        assertSame(item, choice.getItem(choice.getItemCount() - 1));
        assertEquals(2, choice.getSelectedIndex());
        
        // Regression test for HARMONY-2468
        try {
            new Choice().insert(null, 0);
            fail("NullPointerException expected"); //$NON-NLS-1$
        } catch (NullPointerException ex) {
            // expected
        }
    }

    public final void testGetSelectedObjects() {
        assertNull(choice.getSelectedObjects());
        choice.add("item");
        String selItem = "item1";
        choice.add(selItem);
        choice.add("item2");
        choice.select(selItem);
        Object[] objs = choice.getSelectedObjects();
        assertNotNull(objs);
        assertEquals(1, objs.length);
        assertSame(selItem, objs[0]);
    }

    public final void testGetItem() {
        boolean exception = false;
        try {
            choice.getItem(0);
        } catch (IndexOutOfBoundsException ex) {
            exception = true;
        }
        assertTrue(exception);
        String item = "item";
        choice.add(item);
        assertSame(item, choice.getItem(0));
        choice.add(item = "item1");
        assertSame(item, choice.getItem(1));
    }

    public final void testAddItem() {
        String item = "item";
        choice.addItem(item);
        assertSame(item, choice.getItem(0));
        choice.addItem(item = "item1");
        assertSame(item, choice.getItem(1));
        boolean npe = false;
        try {
            choice.addItem(item = null);
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
    }

    @SuppressWarnings("deprecation")
    public final void testCountItems() {
        assertEquals(0, choice.countItems());
    }

    public final void testGetItemCount() {
        assertEquals(0, choice.getItemCount());
    }

    public final void testGetSelectedIndex() {
        assertEquals(-1, choice.getSelectedIndex());
        choice.add("qqq");
        assertEquals(0, choice.getSelectedIndex());
        choice.select(0);
        assertEquals(0, choice.getSelectedIndex());
        choice.add("ppp");
        choice.select(1);
        assertEquals(1, choice.getSelectedIndex());
    }

    public final void testGetSelectedItem() {
        assertNull(choice.getSelectedItem());
    }

    /*
     * Class under test for void select(int)
     */
    public final void testSelectint() {
        choice.add("item");
        choice.add("item1");
        choice.add("item2");
        int idx = 0;
        choice.select(idx);
        assertEquals(idx, choice.getSelectedIndex());
        choice.select(idx = 1);
        assertEquals(idx, choice.getSelectedIndex());
        boolean iae = false;
        try {
            choice.select(1000);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        iae = false;
        try {
            choice.select(-1);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
    }

    /*
     * Class under test for void select(java.lang.String)
     */
    public final void testSelectString() {
        String item = "item";
        choice.add(item);
        choice.add("item1");
        choice.add(item);
        choice.select("item");
        assertEquals(item, choice.getSelectedItem());
        assertEquals(0, choice.getSelectedIndex());
        choice.select(item = "item1");
        assertEquals(item, choice.getSelectedItem());
        choice.select("q");
        assertEquals(item, choice.getSelectedItem());
        choice.select(null);
        assertEquals(item, choice.getSelectedItem());

    }

    public final void testAddGetRemoveItemListener() {
        assertEquals(0, choice.getItemListeners().length);

        ItemListener listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {}
        };

        choice.addItemListener(listener);
        assertEquals(1, choice.getItemListeners().length);
        assertSame(listener, choice.getItemListeners()[0]);

        choice.removeItemListener(listener);
        assertEquals(0, choice.getItemListeners().length);
    }

    public final void testProcessItemEvent() {
        eventProcessed = false;
        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                eventProcessed = true;
            }
        });
        choice.processEvent(new ItemEvent(choice, ItemEvent.ITEM_STATE_CHANGED,
                                          null, ItemEvent.DESELECTED));
        assertTrue(eventProcessed);
    }

    public void testDeadLoop4887() {
        final int count[] = new int[1];
        Component c = new Choice() {
            public void paint(Graphics g) {
                count[0]++;
                if (getItemCount() == 0) {
                    add("item");      
                }
                select(0);
            }
        };
        
        Tools.checkDeadLoop(c, count);
    }
}
