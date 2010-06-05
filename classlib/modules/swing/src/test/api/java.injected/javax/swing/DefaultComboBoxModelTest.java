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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class DefaultComboBoxModelTest extends SwingTestCase {
    private DefaultComboBoxModel model;

    private TestListener listener;

    public DefaultComboBoxModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new DefaultComboBoxModel();
        listener = new TestListener();
        model.addListDataListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
        listener = null;
    }

    public void testDefaultComboBoxModel() throws Exception {
        assertEquals(0, model.getSize());
        assertNull(model.getSelectedItem());
        assertEquals(0, listener.getEvents().size());
        Object[] arrayData = new Object[] { "1", "2", "3" };
        model = new DefaultComboBoxModel(arrayData);
        assertEquals("1", model.getSelectedItem());
        assertEquals(arrayData.length, model.getSize());
        assertEquals("2", model.getElementAt(1));
        arrayData[1] = "21";
        assertEquals("2", model.getElementAt(1));
        Vector<String> vectorData = new Vector<String>();
        vectorData.add("a");
        vectorData.add("b");
        model = new DefaultComboBoxModel(vectorData);
        assertEquals("a", model.getSelectedItem());
        assertEquals(vectorData.size(), model.getSize());
        assertEquals("a", model.getElementAt(0));
        vectorData.setElementAt("a1", 0);
        assertEquals("a1", model.getElementAt(0));
    }

    public void testAddElement() throws Exception {
        assertEquals(0, model.getSize());
        assertNull(model.getSelectedItem());
        model.addElement("1");
        assertEquals(1, model.getSize());
        checkListDataEvent(0, ListDataEvent.INTERVAL_ADDED, 0, 0);
        checkListDataEvent(1, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        assertEquals("1", model.getSelectedItem());
        listener.reset();
        model.addElement("2");
        assertEquals(2, model.getSize());
        checkListDataEvent(0, ListDataEvent.INTERVAL_ADDED, 1, 1);
        assertEquals("1", model.getElementAt(0));
        assertEquals("2", model.getElementAt(1));
    }

    public void testGetElementAt() throws Exception {
        assertNull(model.getElementAt(0));
        model.addElement("a");
        model.addElement("b");
        assertEquals("a", model.getElementAt(0));
        assertEquals("b", model.getElementAt(1));
        assertNull(model.getElementAt(2));
    }

    public void testGetIndexOf() throws Exception {
        assertEquals(-1, model.getIndexOf("a"));
        model.addElement("a");
        model.addElement("b");
        model.addElement("a");
        assertEquals(0, model.getIndexOf("a"));
        assertEquals(1, model.getIndexOf("b"));
        assertEquals(-1, model.getIndexOf("c"));
    }

    public void testGetSize() throws Exception {
        assertEquals(0, model.getSize());
        model.addElement("a");
        assertEquals(1, model.getSize());
        model.addElement("a");
        assertEquals(2, model.getSize());
    }

    public void testInsertElementAt() throws Exception {
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.insertElementAt("a", 1);
            }
        });
        model.insertElementAt("a", 0);
        checkListDataEvent(0, ListDataEvent.INTERVAL_ADDED, 0, 0);
        model.addElement("b");
        model.addElement("c");
        listener.reset();
        model.insertElementAt("after_a", 1);
        assertEquals("after_a", model.getElementAt(1));
        checkListDataEvent(0, ListDataEvent.INTERVAL_ADDED, 1, 1);
    }

    public void testRemoveAllElements() throws Exception {
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        listener.reset();
        model.removeAllElements();
        assertEquals(0, model.getSize());
        checkListDataEvent(0, ListDataEvent.INTERVAL_REMOVED, 0, 2);
        assertNull(model.getSelectedItem());
    }

    public void testRemoveElement() throws Exception {
        model.removeElement("a");
        assertEquals(0, model.getSize());
        assertEquals(0, listener.getEvents().size());
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        listener.reset();
        model.removeElement("b");
        assertEquals(2, model.getSize());
        checkListDataEvent(0, ListDataEvent.INTERVAL_REMOVED, 1, 1);
        assertEquals(1, listener.getEvents().size());
    }

    public void testRemoveElementAt() throws Exception {
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.removeElementAt(0);
            }
        });
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.removeElementAt(3);
            }
        });
        model.removeElementAt(0);
        assertEquals(2, model.getSize());
        assertEquals("b", model.getElementAt(0));
        model.removeElementAt(1);
        assertEquals(1, model.getSize());
        assertEquals("b", model.getElementAt(0));
        model.removeElementAt(0);
        assertEquals(0, model.getSize());
    }

    public void testGetSetSelectedItem() throws Exception {
        assertNull(model.getSelectedItem());
        model.setSelectedItem("3");
        assertEquals("3", model.getSelectedItem());
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        listener.reset();
        model.setSelectedItem(null);
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        listener.reset();
        model.setSelectedItem(null);
        assertEquals(0, listener.getEvents().size());
        model.setSelectedItem("3");
        model.addElement("0");
        model.addElement("1");
        model.addElement("2");
        assertEquals("3", model.getSelectedItem());
        model.removeElement("0");
        assertEquals("3", model.getSelectedItem());
        model.addElement("3");
        assertEquals("3", model.getSelectedItem());
        model.addElement("4");
        listener.reset();
        model.removeElement("3");
        assertEquals("2", model.getSelectedItem());
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        checkListDataEvent(1, ListDataEvent.INTERVAL_REMOVED, 2, 2);
        assertEquals(2, listener.getEvents().size());
        model.removeElementAt(1);
        assertEquals("1", model.getSelectedItem());
        model.addElement("5");
        model.addElement("6");
        assertEquals("1", model.getSelectedItem());
        model.removeElementAt(1);
        assertEquals("1", model.getSelectedItem());
        model.addElement("7");
        model.addElement("1");
        listener.reset();
        model.removeElementAt(0);
        assertEquals("5", model.getSelectedItem());
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        checkListDataEvent(1, ListDataEvent.INTERVAL_REMOVED, 0, 0);
        listener.reset();
        model.removeAllElements();
        assertNull(model.getSelectedItem());
        checkListDataEvent(0, ListDataEvent.INTERVAL_REMOVED, 0, 3);
        assertEquals(1, listener.getEvents().size());
        listener.reset();
        model.setSelectedItem("0");
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        listener.reset();
        model.removeAllElements();
        assertNull(model.getSelectedItem());
        assertEquals(0, listener.getEvents().size());
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        listener.reset();
        assertEquals("a", model.getSelectedItem());
        model.setSelectedItem("b");
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        listener.reset();
        model.setSelectedItem("b");
        assertEquals(0, listener.getEvents().size());
        listener.reset();
        model.setSelectedItem("c");
        checkListDataEvent(0, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        assertEquals(1, listener.getEvents().size());
    }

    private void checkListDataEvent(final int eventIndex, final int eventType,
            final int index0, final int index1) {
        assertTrue(listener.getEvents().size() > eventIndex);
        ListDataEvent event = listener.getEvents().get(eventIndex);
        assertEquals(model, event.getSource());
        assertEquals(eventType, event.getType());
        assertEquals(index0, event.getIndex0());
        assertEquals(index1, event.getIndex1());
    }

    private class TestListener implements ListDataListener {
        private List<ListDataEvent> events = new ArrayList<ListDataEvent>();

        public void contentsChanged(final ListDataEvent e) {
            events.add(e);
        }

        public void intervalAdded(final ListDataEvent e) {
            events.add(e);
        }

        public void intervalRemoved(final ListDataEvent e) {
            events.add(e);
        }

        public List<ListDataEvent> getEvents() {
            return events;
        }

        public void reset() {
            events.clear();
        }
    }
}
