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
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class DefaultListSelectionModelTest extends SwingTestCase {
    private DefaultListSelectionModel model;

    private TestListener listener;

    public DefaultListSelectionModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new DefaultListSelectionModel();
        listener = new TestListener();
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
        listener = null;
    }

    public void testAddRemoveListSelectionListener() throws Exception {
        assertEquals(0, model.getListSelectionListeners().length);
        model.addListSelectionListener(new TestListener());
        model.addListSelectionListener(listener);
        model.addListSelectionListener(new TestListener());
        assertEquals(3, model.getListSelectionListeners().length);
        model.removeListSelectionListener(listener);
        assertEquals(2, model.getListSelectionListeners().length);
    }

    public void testAddSelectionInterval() throws Exception {
        model.addListSelectionListener(listener);
        model.addSelectionInterval(-1, 0);
        model.addSelectionInterval(-1, 10);
        model.addSelectionInterval(0, -1);
        model.addSelectionInterval(-1, -1);
        assertTrue(model.isSelectionEmpty());
        assertEquals(0, listener.getEvents().size());
        listener.reset();
        model.setLeadAnchorNotificationEnabled(false);
        assertTrue(model.isSelectionEmpty());
        model.addSelectionInterval(3, 5);
        checkSingleEvent(3, 5, false);
        assertFalse(model.isSelectedIndex(2));
        checkIntervalState(3, 5, true);
        listener.reset();
        model.addSelectionInterval(10, 7);
        checkSingleEvent(7, 10, false);
        checkIntervalState(3, 5, true);
        checkIntervalState(7, 10, true);
        listener.reset();
        model.addSelectionInterval(4, 11);
        checkSingleEvent(6, 11, false);
        checkIntervalState(3, 11, true);
        model.clearSelection();
        model.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        model.addSelectionInterval(4, 11);
        checkIntervalState(4, 11, true);
        model.addSelectionInterval(6, 8);
        checkIntervalState(6, 8, true);
        model.clearSelection();
        model.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        model.addSelectionInterval(11, 4);
        checkIntervalState(4, 4, true);
        assertEquals(model.getMaxSelectionIndex(), model.getMinSelectionIndex());
    }

    public void testClearSelectionIsSelectionEmpty() throws Exception {
        assertTrue(model.isSelectionEmpty());
        model.addSelectionInterval(0, 5);
        model.addSelectionInterval(7, 9);
        assertFalse(model.isSelectionEmpty());
        model.addListSelectionListener(listener);
        model.clearSelection();
        checkSingleEvent(0, 9, false);
        assertTrue(model.isSelectionEmpty());
        checkIntervalState(0, 9, false);
    }

    public void testClone() throws Exception {
        model.addListSelectionListener(listener);
        model.addSelectionInterval(2, 4);
        model.addSelectionInterval(7, 9);
        model.setLeadAnchorNotificationEnabled(false);
        model.setValueIsAdjusting(true);
        model = (DefaultListSelectionModel) model.clone();
        assertEquals(0, model.getListSelectionListeners().length);
        checkIntervalState(0, 1, false);
        checkIntervalState(2, 4, true);
        checkIntervalState(5, 6, false);
        checkIntervalState(7, 9, true);
        assertEquals(7, model.getAnchorSelectionIndex());
        assertEquals(9, model.getLeadSelectionIndex());
        assertFalse(model.isLeadAnchorNotificationEnabled());
        assertTrue(model.getValueIsAdjusting());
    }

    public void testFireValueChanged() throws Exception {
        TestListener listener2 = new TestListener();
        model.addListSelectionListener(listener);
        model.addListSelectionListener(listener2);
        model.fireValueChanged(true);
        assertEquals(0, listener.getEvents().size());
        listener.reset();
        model.setValueIsAdjusting(true);
        model.setAnchorSelectionIndex(5);
        checkSingleEvent(listener, 5, 5, true);
        listener.reset();
        model.fireValueChanged(true);
        checkSingleEvent(listener, 5, 5, true);
        model.setSelectionInterval(3, 6);
        model.setSelectionInterval(9, 11);
        listener.reset();
        model.fireValueChanged(true);
        checkSingleEvent(listener, 3, 11, true);
        listener.reset();
        model.fireValueChanged(false);
        assertEquals(0, listener.getEvents().size());
        model.setValueIsAdjusting(false);
        listener.reset();
        listener2.reset();
        model.fireValueChanged(3, 7);
        checkSingleEvent(listener, 3, 7, false);
        checkSingleEvent(listener2, 3, 7, false);
        listener.reset();
        listener2.reset();
        model.fireValueChanged(0, 5, false);
        checkSingleEvent(listener, 0, 5, false);
        checkSingleEvent(listener2, 0, 5, false);
    }

    public void testGetAnchorAndLeadSelectionIndex() throws Exception {
        assertEquals(-1, model.getAnchorSelectionIndex());
        assertEquals(-1, model.getLeadSelectionIndex());
        model.addSelectionInterval(2, 6);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(6, model.getLeadSelectionIndex());
        model.setSelectionInterval(1, 4);
        assertEquals(1, model.getAnchorSelectionIndex());
        assertEquals(4, model.getLeadSelectionIndex());
        model.removeSelectionInterval(2, 7);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
    }

    public void testGetListeners() throws Exception {
        assertEquals(0, model.getListeners(ListSelectionListener.class).length);
        assertEquals(0, model.getListeners(KeyListener.class).length);
        model.addListSelectionListener(listener);
        assertEquals(1, model.getListeners(ListSelectionListener.class).length);
        assertEquals(0, model.getListeners(KeyListener.class).length);
    }

    public void testGetListSelectionListeners() throws Exception {
        assertEquals(0, model.getListSelectionListeners().length);
        model.addListSelectionListener(listener);
        assertEquals(1, model.getListSelectionListeners().length);
    }

    public void testGetMinAndMaxSelectionIndex() throws Exception {
        assertEquals(-1, model.getMinSelectionIndex());
        assertEquals(-1, model.getMaxSelectionIndex());
        model.addSelectionInterval(2, 6);
        model.addSelectionInterval(12, 9);
        assertEquals(2, model.getMinSelectionIndex());
        assertEquals(12, model.getMaxSelectionIndex());
        model.addSelectionInterval(0, 14);
        assertEquals(0, model.getMinSelectionIndex());
        assertEquals(14, model.getMaxSelectionIndex());
    }

    public void testGetSetSelectionMode() throws Exception {
        assertEquals(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, model.getSelectionMode());
        model.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, model.getSelectionMode());
        model.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        assertEquals(ListSelectionModel.SINGLE_INTERVAL_SELECTION, model.getSelectionMode());
        try {
            model.setSelectionMode(100);
            fail("Incorrect selection model should be detected");
        } catch (IllegalArgumentException iae) {
        }
    }

    public void testGetSetValueIsAdjusting() throws Exception {
        model.addListSelectionListener(listener);
        assertFalse(model.getValueIsAdjusting());
        model.setValueIsAdjusting(true);
        assertEquals(0, listener.getEvents().size());
        assertTrue(model.getValueIsAdjusting());
        model.setSelectionInterval(2, 3);
        checkSingleEvent(2, 3, true);
        listener.reset();
        model.setSelectionInterval(5, 7);
        checkSingleEvent(2, 7, true);
        listener.reset();
        model.setSelectionInterval(5, 8);
        checkSingleEvent(7, 8, true);
        listener.reset();
        model.setValueIsAdjusting(false);
        assertEquals(1, listener.getEvents().size());
        assertFalse(model.getValueIsAdjusting());
        checkSingleEvent(2, 8, false);
    }

    public void testInsertIndexInterval() throws Exception {
        model.addListSelectionListener(listener);
        model.setSelectionInterval(3, 5);
        if (isHarmony()) {
            listener.reset();
            model.insertIndexInterval(-1, 0, true);
            model.insertIndexInterval(-1, 0, false);
            model.insertIndexInterval(-1, -1, true);
            model.insertIndexInterval(-1, -1, false);
            model.insertIndexInterval(0, -1, true);
            model.insertIndexInterval(0, -1, false);
            assertEquals(0, listener.getEvents().size());
        }
        listener.reset();
        model.insertIndexInterval(4, 10, true);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 15, true);
        if (isHarmony()) {
            checkSingleEvent(5, 15, false);
        }
        listener.reset();
        model.insertIndexInterval(0, 3, true);
        checkIntervalState(0, 5, false);
        checkIntervalState(6, 18, true);
        checkSingleEvent(3, 18, false);
        model.clearSelection();
        model.setSelectionInterval(3, 5);
        listener.reset();
        model.insertIndexInterval(6, 3, true);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 5, true);
        checkIntervalState(6, 20, false);
        assertEquals(0, listener.getEvents().size());
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(5, model.getLeadSelectionIndex());
        model.clearSelection();
        model.setSelectionInterval(3, 5);
        listener.reset();
        model.insertIndexInterval(6, 3, false);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 5, true);
        checkIntervalState(6, 20, false);
        assertEquals(0, listener.getEvents().size());
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(5, model.getLeadSelectionIndex());
        model.clearSelection();
        model.setSelectionInterval(3, 5);
        listener.reset();
        model.insertIndexInterval(5, 3, false);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 8, true);
        checkIntervalState(9, 20, false);
        checkSingleEvent(6, 8, false);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(5, model.getLeadSelectionIndex());
        model.clearSelection();
        model.setSelectionInterval(3, 5);
        listener.reset();
        model.insertIndexInterval(5, 3, true);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 8, true);
        checkIntervalState(9, 20, false);
        checkSingleEvent(5, 8, false);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        model.clearSelection();
        model.setSelectionInterval(3, 5);
        listener.reset();
        model.insertIndexInterval(3, 3, true);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 8, true);
        checkIntervalState(9, 20, false);
        checkSingleEvent(3, 8, false);
        assertEquals(6, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        model.clearSelection();
        model.setSelectionInterval(1, 2);
        listener.reset();
        model.insertIndexInterval(0, 3, true);
        checkIntervalState(0, 3, false);
        checkIntervalState(4, 5, true);
        checkIntervalState(6, 20, false);
        assertEquals(4, model.getAnchorSelectionIndex());
        assertEquals(5, model.getLeadSelectionIndex());
        listener.reset();
        model.removeSelectionInterval(-1, 0);
        model.removeSelectionInterval(0, -1);
        model.removeSelectionInterval(-1, -1);
        assertEquals(0, listener.getEvents().size());
    }

    public void testRemoveIndexInterval() throws Exception {
        model.setSelectionInterval(3, 8);
        model.addListSelectionListener(listener);
        model.removeSelectionInterval(-1, 10);
        model.removeSelectionInterval(-1, 0);
        model.removeSelectionInterval(0, -1);
        model.removeSelectionInterval(-1, -1);
        assertEquals(0, listener.getEvents().size());
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        listener.reset();
        model.removeIndexInterval(2, 6);
        checkIntervalState(0, 1, false);
        checkIntervalState(2, 3, true);
        assertEquals(1, model.getAnchorSelectionIndex());
        assertEquals(3, model.getLeadSelectionIndex());
        checkSingleEvent(1, 8, false);
        listener.reset();
        model.removeIndexInterval(0, 2);
        checkIntervalState(0, 0, true);
        checkIntervalState(1, 10, false);
        assertEquals(-1, model.getAnchorSelectionIndex());
        assertEquals(0, model.getLeadSelectionIndex());
        if (isHarmony()) {
            checkSingleEvent(0, 3, false);
        } else {
            checkSingleEvent(-1, 3, false);
        }
        listener.reset();
        model.removeIndexInterval(0, 2);
        checkIntervalState(0, 10, false);
        assertEquals(-1, model.getAnchorSelectionIndex());
        if (isHarmony()) {
            assertEquals(-1, model.getLeadSelectionIndex());
        }
        checkSingleEvent(0, 0, false);
        model.setSelectionInterval(3, 8);
        listener.reset();
        model.removeIndexInterval(8, 8);
        checkIntervalState(3, 7, true);
        checkIntervalState(8, 8, false);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        checkSingleEvent(7, 8, false);
        listener.reset();
        model.removeIndexInterval(3, 3);
        checkIntervalState(3, 6, true);
        checkIntervalState(7, 8, false);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(6, model.getLeadSelectionIndex());
        checkSingleEvent(2, 7, false);
        listener.reset();
        model.removeIndexInterval(3, 6);
        checkIntervalState(0, 10, false);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(2, model.getLeadSelectionIndex());
        checkSingleEvent(2, 6, false);
    }

    public void testIsLeadAnchorNotificationEnabled() throws Exception {
        model.addListSelectionListener(listener);
        assertTrue(model.isLeadAnchorNotificationEnabled());
        model.addSelectionInterval(3, 5);
        checkSingleEvent(3, 5, false);
        listener.reset();
        model.addSelectionInterval(7, 8);
        checkSingleEvent(3, 8, false);
        listener.reset();
        model.setSelectionInterval(2, 6);
        checkSingleEvent(2, 8, false);
        listener.reset();
        model.removeSelectionInterval(4, 11);
        checkSingleEvent(2, 11, false);
        listener.reset();
        model.removeSelectionInterval(4, 11);
        assertEquals(0, listener.getEvents().size());
        listener.reset();
        model.removeSelectionInterval(5, 8);
        checkSingleEvent(4, 11, false);
        model.setLeadAnchorNotificationEnabled(false);
        assertFalse(model.isLeadAnchorNotificationEnabled());
        listener.reset();
        model.addSelectionInterval(10, 12);
        checkSingleEvent(10, 12, false);
        listener.reset();
        model.removeSelectionInterval(0, 2);
        checkSingleEvent(2, 2, false);
        listener.reset();
        model.removeSelectionInterval(1, 2);
        assertEquals(0, listener.getEvents().size());
    }

    public void testIsSelectedIndex() throws Exception {
        model.setSelectionInterval(2, 4);
        assertFalse(model.isSelectedIndex(0));
        assertTrue(model.isSelectedIndex(2));
        assertFalse(model.isSelectedIndex(-1));
    }

    public void testGetSetAnchorSelectionIndex() throws Exception {
        model.addListSelectionListener(listener);
        model.setAnchorSelectionIndex(3);
        assertEquals(3, model.getAnchorSelectionIndex());
        checkSingleEvent(3, 3, false);
        listener.reset();
        model.setAnchorSelectionIndex(5);
        assertEquals(5, model.getAnchorSelectionIndex());
        checkSingleEvent(3, 5, false);
        listener.reset();
        model.setLeadAnchorNotificationEnabled(false);
        model.setAnchorSelectionIndex(7);
        assertEquals(7, model.getAnchorSelectionIndex());
        assertEquals(0, listener.getEvents().size());
        listener.reset();
        model.setAnchorSelectionIndex(-1);
        assertEquals(-1, model.getAnchorSelectionIndex());
        assertEquals(0, listener.getEvents().size());
    }

    public void testGetSetLeadSelectionIndex() throws Exception {
        model.addListSelectionListener(listener);
        model.setSelectionInterval(3, 6);
        listener.reset();
        model.setLeadSelectionIndex(-1);
        assertEquals(6, model.getLeadSelectionIndex());
        assertEquals(0, listener.getEvents().size());
        listener.reset();
        model.setLeadSelectionIndex(4);
        assertEquals(4, model.getLeadSelectionIndex());
        checkIntervalState(3, 4, true);
        checkIntervalState(5, 6, false);
        checkSingleEvent(4, 6, false);
        model.setSelectionInterval(3, 6);
        model.setAnchorSelectionIndex(2);
        listener.reset();
        model.setLeadSelectionIndex(8);
        assertEquals(8, model.getLeadSelectionIndex());
        checkIntervalState(2, 8, false);
        checkSingleEvent(3, 8, false);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        model.clearSelection();
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        assertTrue(model.isSelectionEmpty());
        model.setAnchorSelectionIndex(5);
        assertEquals(5, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        assertTrue(model.isSelectionEmpty());
        model.setLeadSelectionIndex(8);
        assertEquals(5, model.getAnchorSelectionIndex());
        assertEquals(8, model.getLeadSelectionIndex());
        assertTrue(model.isSelectionEmpty());
        model.setLeadSelectionIndex(20);
        assertEquals(5, model.getAnchorSelectionIndex());
        assertEquals(20, model.getLeadSelectionIndex());
        assertTrue(model.isSelectionEmpty());
        model.setAnchorSelectionIndex(1);
        assertEquals(1, model.getAnchorSelectionIndex());
        assertEquals(20, model.getLeadSelectionIndex());
        assertTrue(model.isSelectionEmpty());
        model.setLeadSelectionIndex(19);
        assertEquals(1, model.getAnchorSelectionIndex());
        assertEquals(19, model.getLeadSelectionIndex());
        checkIntervalState(0, 19, false);
        checkIntervalState(20, 20, true);
        checkIntervalState(21, 100, false);
        model.setSelectionInterval(2, 5);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(5, model.getLeadSelectionIndex());
        checkIntervalState(0, 1, false);
        checkIntervalState(2, 5, true);
        checkIntervalState(6, 10, false);
        model.setAnchorSelectionIndex(1);
        assertEquals(1, model.getAnchorSelectionIndex());
        assertEquals(5, model.getLeadSelectionIndex());
        checkIntervalState(0, 1, false);
        checkIntervalState(2, 5, true);
        checkIntervalState(6, 10, false);
        model.setLeadSelectionIndex(7);
        assertEquals(1, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        checkIntervalState(0, 10, false);
        model.setSelectionInterval(2, 7);
        assertEquals(2, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        model.setAnchorSelectionIndex(4);
        model.setLeadSelectionIndex(6);
        checkIntervalState(0, 1, false);
        checkIntervalState(2, 6, true);
        checkIntervalState(7, 10, false);
        model.setSelectionInterval(3, 7);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        model.setLeadSelectionIndex(3);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 3, true);
        checkIntervalState(4, 10, false);
        model.setSelectionInterval(3, 7);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        model.setLeadSelectionIndex(3);
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 3, true);
        checkIntervalState(4, 10, false);
        model.setSelectionInterval(3, 7);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        model.setLeadSelectionIndex(1);
        checkIntervalState(0, 0, false);
        checkIntervalState(1, 3, true);
        checkIntervalState(4, 10, false);
        model.setSelectionInterval(3, 7);
        assertEquals(3, model.getAnchorSelectionIndex());
        assertEquals(7, model.getLeadSelectionIndex());
        model.setAnchorSelectionIndex(5);
        model.setLeadSelectionIndex(1);
        checkIntervalState(0, 0, false);
        checkIntervalState(1, 5, true);
        checkIntervalState(6, 10, false);
    }

    public void testMoveLeadSelectionIndex() throws Exception {
        model.addListSelectionListener(listener);
        model.setSelectionInterval(3, 6);

        listener.reset();
        model.moveLeadSelectionIndex(-1);
        assertEquals(6, model.getLeadSelectionIndex());
        assertEquals(0, listener.getEvents().size());

        listener.reset();
        model.moveLeadSelectionIndex(2);
        assertEquals(2, model.getLeadSelectionIndex());
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 6, true);
        checkIntervalState(7, 10, false);
        checkSingleEvent(2, 6, false);

        listener.reset();
        model.moveLeadSelectionIndex(8);
        assertEquals(8, model.getLeadSelectionIndex());
        checkIntervalState(0, 2, false);
        checkIntervalState(3, 6, true);
        checkIntervalState(7, 10, false);
        checkSingleEvent(2, 8, false);
    }

    public void testToString() throws Exception {
        assertNotNull(model.toString());
    }

    private void checkIntervalState(final int beginIndex, final int endIndex,
            final boolean selected) {
        for (int i = beginIndex; i <= endIndex; i++) {
            assertEquals(selected, model.isSelectedIndex(i));
        }
    }

    private void checkSingleEvent(final int beginIndex, final int endIndex,
            final boolean isAdjusting) {
        checkSingleEvent(listener, beginIndex, endIndex, isAdjusting);
    }

    private void checkSingleEvent(final TestListener listener, final int beginIndex,
            final int endIndex, final boolean isAdjusting) {
        assertEquals(1, listener.getEvents().size());
        ListSelectionEvent event = listener.getEvents().get(0);
        assertEquals(model, event.getSource());
        assertEquals(beginIndex, event.getFirstIndex());
        assertEquals(endIndex, event.getLastIndex());
        assertEquals(isAdjusting, event.getValueIsAdjusting());
    }

    private class TestListener implements ListSelectionListener {
        private List<ListSelectionEvent> events = new ArrayList<ListSelectionEvent>();

        public void valueChanged(final ListSelectionEvent event) {
            events.add(event);
        }

        public List<ListSelectionEvent> getEvents() {
            return events;
        }

        public void reset() {
            events.clear();
        }
    }

    public void testIsValidInterval() throws Exception {
        // regression test for HARMONY-1965
        try {
            model.addSelectionInterval(-2, 0);
            fail("addSelectionInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.addSelectionInterval(0, -2);
            fail("addSelectionInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.setSelectionInterval(-2, 0);
            fail("setSelectionInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.setSelectionInterval(0, -2);
            fail("setSelectionInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.removeSelectionInterval(-2, 0);
            fail("removeSelectionInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.removeSelectionInterval(0, -2);
            fail("removeSelectionInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.insertIndexInterval(-2, 0, true);
            fail("insertIndexInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        if (isHarmony()) {
            try {
                model.insertIndexInterval(0, -2, true);
                fail("insertIndexInterval should throw IndexOutOfBoundsException");
            } catch (IndexOutOfBoundsException e) {
            }
        }
        try {
            model.removeIndexInterval(-2, 0);
            fail("removeIndexInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            model.removeIndexInterval(0, -2);
            fail("removeIndexInterval should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }
}
