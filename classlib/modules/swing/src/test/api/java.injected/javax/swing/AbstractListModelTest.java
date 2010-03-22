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

import java.awt.event.KeyListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class AbstractListModelTest extends SwingTestCase {
    private AbstractListModel model;

    public AbstractListModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new AbstractListModel() {
            private static final long serialVersionUID = 1L;

            public Object getElementAt(final int index) {
                return null;
            }

            public int getSize() {
                return 0;
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
    }

    public void testAddRemoveGetDataListener() throws Exception {
        assertEquals(0, model.getListDataListeners().length);
        TestListener l1 = new TestListener();
        TestListener l2 = new TestListener();
        TestListener l3 = new TestListener();
        model.addListDataListener(l1);
        model.addListDataListener(l3);
        model.addListDataListener(l2);
        assertEquals(3, model.getListDataListeners().length);
        model.removeListDataListener(l2);
        assertEquals(2, model.getListDataListeners().length);
        model.removeListDataListener(new TestListener());
        assertEquals(2, model.getListDataListeners().length);
    }

    public void testGetListeners() throws Exception {
        assertEquals(0, model.getListeners(ListDataListener.class).length);
        assertEquals(0, model.getListeners(KeyListener.class).length);
        model.addListDataListener(new TestListener());
        assertEquals(1, model.getListeners(ListDataListener.class).length);
        assertEquals(0, model.getListeners(KeyListener.class).length);
    }

    public void testFireContentChanged() throws Exception {
        TestListener l1 = new TestListener();
        TestListener l2 = new TestListener();
        model.addListDataListener(l1);
        model.addListDataListener(l2);
        model.fireContentsChanged(new Object(), 0, 2);
        checkListDataEvent(l1.getEvent(), ListDataEvent.CONTENTS_CHANGED, 0, 2);
        assertEquals(ListDataEvent.CONTENTS_CHANGED, l1.getType());
        checkListDataEvent(l2.getEvent(), ListDataEvent.CONTENTS_CHANGED, 0, 2);
        assertEquals(ListDataEvent.CONTENTS_CHANGED, l2.getType());
    }

    public void testFireIntervalAdded() throws Exception {
        TestListener l1 = new TestListener();
        TestListener l2 = new TestListener();
        model.addListDataListener(l1);
        model.addListDataListener(l2);
        model.fireIntervalAdded(new Object(), 5, 2);
        checkListDataEvent(l1.getEvent(), ListDataEvent.INTERVAL_ADDED, 2, 5);
        assertEquals(ListDataEvent.INTERVAL_ADDED, l1.getType());
        checkListDataEvent(l2.getEvent(), ListDataEvent.INTERVAL_ADDED, 2, 5);
        assertEquals(ListDataEvent.INTERVAL_ADDED, l2.getType());
        model.fireIntervalAdded(new Object(), 2, 5);
        checkListDataEvent(l1.getEvent(), ListDataEvent.INTERVAL_ADDED, 2, 5);
    }

    public void testFireIntervalRemoved() throws Exception {
        TestListener l1 = new TestListener();
        TestListener l2 = new TestListener();
        model.addListDataListener(l1);
        model.addListDataListener(l2);
        model.fireIntervalRemoved(new Object(), 1, 4);
        checkListDataEvent(l1.getEvent(), ListDataEvent.INTERVAL_REMOVED, 1, 4);
        assertEquals(ListDataEvent.INTERVAL_REMOVED, l1.getType());
        checkListDataEvent(l2.getEvent(), ListDataEvent.INTERVAL_REMOVED, 1, 4);
        assertEquals(ListDataEvent.INTERVAL_REMOVED, l2.getType());
    }

    private void checkListDataEvent(final ListDataEvent event, final int expectedType,
            final int expectedIndex0, final int expectedIndex1) {
        assertNotNull(event);
        assertEquals(expectedType, event.getType());
        assertEquals(expectedIndex0, event.getIndex0());
        assertEquals(expectedIndex1, event.getIndex1());
    }

    private class TestListener implements ListDataListener {
        private ListDataEvent event;

        private int eventType = -1;

        public void contentsChanged(final ListDataEvent e) {
            event = e;
            eventType = ListDataEvent.CONTENTS_CHANGED;
        }

        public void intervalAdded(final ListDataEvent e) {
            event = e;
            eventType = ListDataEvent.INTERVAL_ADDED;
        }

        public void intervalRemoved(final ListDataEvent e) {
            event = e;
            eventType = ListDataEvent.INTERVAL_REMOVED;
        }

        public ListDataEvent getEvent() {
            return event;
        }

        public int getType() {
            return eventType;
        }
    }
}
