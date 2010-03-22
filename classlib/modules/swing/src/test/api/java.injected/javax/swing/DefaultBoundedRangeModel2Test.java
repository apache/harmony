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
 * @author Evgeniya G. Maenkova
 */
package javax.swing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DefaultBoundedRangeModel2Test extends SwingTestCase {
    DefaultBoundedRangeModel brm;

    String fireOrder;

    SimpleChangeListener listener = new SimpleChangeListener("initial");

    final int MIN = 5;

    final int MAX = 30;

    final int VALUE = 10;

    final int EXTENT = 15;

    final boolean IS_ADJUSTING = false;

    @Override
    protected void setUp() throws Exception {
        brm = new DefaultBoundedRangeModel(VALUE, EXTENT, MIN, MAX);
        brm.addChangeListener(listener);
        fireOrder = "";
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    class SimpleChangeListener implements ChangeListener {
        String name;

        ChangeEvent event;

        public SimpleChangeListener(final String s) {
            name = s;
        }

        public void stateChanged(final ChangeEvent e) {
            event = e;
            fireOrder += name;
        }
    }

    public void testAddRemoveGetChangeListener() {
        brm.removeChangeListener(listener);
        SimpleChangeListener listener1 = new SimpleChangeListener("first");
        SimpleChangeListener listener2 = new SimpleChangeListener("second");
        SimpleChangeListener listener3 = new SimpleChangeListener("third");
        ChangeListener changeListsners[] = brm.getChangeListeners();
        ChangeListener listeners[] = brm.getListeners(ChangeListener.class);
        assertNotNull(changeListsners);
        assertNotNull(listeners);
        assertEquals(0, changeListsners.length);
        assertEquals(0, listeners.length);
        brm.addChangeListener(listener1);
        changeListsners = brm.getChangeListeners();
        listeners = brm.getListeners(ChangeListener.class);
        assertEquals(1, changeListsners.length);
        assertEquals(1, listeners.length);
        assertEquals(changeListsners[0], listener1);
        brm.addChangeListener(listener2);
        changeListsners = brm.getChangeListeners();
        listeners = brm.getListeners(ChangeListener.class);
        assertEquals(2, changeListsners.length);
        assertEquals(2, listeners.length);
        assertEquals(changeListsners[0], listener2);
        assertEquals(changeListsners[1], listener1);
        brm.addChangeListener(listener3);
        changeListsners = brm.getChangeListeners();
        listeners = brm.getListeners(ChangeListener.class);
        assertEquals(3, changeListsners.length);
        assertEquals(3, listeners.length);
        assertEquals(changeListsners[0], listener3);
        assertEquals(changeListsners[1], listener2);
        assertEquals(changeListsners[2], listener1);
        listeners = brm.listenerList.getListeners(ChangeListener.class);
        assertEquals(3, listeners.length);
        assertEquals(listeners[0], listener3);
        assertEquals(listeners[1], listener2);
        assertEquals(listeners[2], listener1);
        brm.removeChangeListener(listener3);
        changeListsners = brm.getChangeListeners();
        listeners = brm.getListeners(ChangeListener.class);
        assertEquals(2, changeListsners.length);
        assertEquals(2, listeners.length);
        assertEquals(changeListsners[0], listener2);
        assertEquals(changeListsners[1], listener1);
        brm.removeChangeListener(listener1);
        changeListsners = brm.getChangeListeners();
        listeners = brm.getListeners(ChangeListener.class);
        assertEquals(1, changeListsners.length);
        assertEquals(1, listeners.length);
        assertEquals(changeListsners[0], listener2);
        brm.removeChangeListener(listener2);
        changeListsners = brm.getChangeListeners();
        listeners = brm.getListeners(ChangeListener.class);
        assertEquals(0, changeListsners.length);
        assertEquals(0, listeners.length);
    }

    public void testDefaultBoundedRangeModel() {
        DefaultBoundedRangeModel brm1 = new DefaultBoundedRangeModel();
        checkValues(brm1, 0, 0, 0, 100, false);
    }

    public void testDefaultBoundedRangeModelintintintint() {
        DefaultBoundedRangeModel brm1 = new DefaultBoundedRangeModel(4, 5, -2, 49);
        checkValues(brm1, 4, 5, -2, 49, false);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new DefaultBoundedRangeModel(4, 6, 5, 39);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new DefaultBoundedRangeModel(3, 5, 1, 2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new DefaultBoundedRangeModel(1, -1, 1, 2);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override // Regression test for HARMONY-2621
            public void exceptionalAction() throws Exception {
                new DefaultBoundedRangeModel(Integer.MAX_VALUE,1,2,4);
            }
        });
    }

    public void testFireStateChanged() {
        SimpleChangeListener listener1 = new SimpleChangeListener("1");
        SimpleChangeListener listener2 = new SimpleChangeListener("2");
        SimpleChangeListener listener3 = new SimpleChangeListener("3");
        brm.addChangeListener(listener1);
        brm.addChangeListener(listener2);
        brm.addChangeListener(listener3);
        brm.setValue(7);
        assertEquals("321initial", fireOrder);
        assertEquals(listener.event, brm.changeEvent);
        assertEquals(brm, brm.changeEvent.getSource());
    }

    public void testSetGetExtent() {
        for (int i = -3; i < 100; i++) {
            fireOrder = "";
            int oldValue = brm.getValue();
            int oldMin = brm.getMinimum();
            int oldMax = brm.getMaximum();
            int oldExtent = brm.getExtent();
            brm.setExtent(i);
            int newExtent = Math.min(Math.max(i, 0), oldMax - oldValue);
            assertEquals(oldMin, brm.getMinimum());
            assertEquals(oldValue, brm.getValue());
            assertEquals(newExtent, brm.getExtent());
            assertEquals(oldMax, brm.getMaximum());
            if (oldExtent != newExtent) {
                assertEquals("initial", fireOrder);
            }
        }
    }

    public void testSetGetMaximum() {
        brm.setMaximum(brm.getMaximum());
        assertEquals("", fireOrder);
        for (int i = -3; i < 100; i++) {
            fireOrder = "";
            int oldValue = brm.getValue();
            int oldExtent = brm.getExtent();
            int oldMin = brm.getMinimum();
            brm.setMaximum(i);
            int newValue = Math.min(oldValue, i);
            int newMin = Math.min(i, oldMin);
            int newMax = i;
            int newExtent = Math.min(i - newValue, oldExtent);
            assertEquals(newMin, brm.getMinimum());
            assertEquals(newValue, brm.getValue());
            assertEquals(newExtent, brm.getExtent());
            assertEquals(newMax, brm.getMaximum());
            assertEquals("initial", fireOrder);
        }
    }

    public void testSetGetMinimum() {
        brm.setMinimum(brm.getMinimum());
        assertEquals("", fireOrder);
        for (int i = -3; i < 100; i++) {
            fireOrder = "";
            int oldValue = brm.getValue();
            int oldMax = brm.getMaximum();
            int oldExtent = brm.getExtent();
            brm.setMinimum(i);
            int newValue = Math.max(i, oldValue);
            int newMin = i;
            int newMax = Math.max(i, oldMax);
            int newExtent = (brm.getMaximum() >= brm.getValue() + oldExtent) ? oldExtent : brm
                    .getMaximum()
                    - brm.getValue();
            assertEquals(newMin, brm.getMinimum());
            assertEquals(newValue, brm.getValue());
            assertEquals(newExtent, brm.getExtent());
            assertEquals(newMax, brm.getMaximum());
            assertEquals("initial", fireOrder);
        }
    }

    public void testSetGetValue() {
        for (int i = -3; i < 100; i++) {
            fireOrder = "";
            int oldMin = brm.getMinimum();
            int oldMax = brm.getMaximum();
            int oldExtent = brm.getExtent();
            int oldValue = brm.getValue();
            brm.setValue(i);
            int newValue = Math.min(Math.max(i, oldMin), oldMax - oldExtent);
            assertEquals(oldMin, brm.getMinimum());
            assertEquals(newValue, brm.getValue());
            assertEquals(oldExtent, brm.getExtent());
            assertEquals(oldMax, brm.getMaximum());
            if (oldValue != newValue) {
                assertEquals("initial", fireOrder);
            }
        }
    }

    public void testSetRangeProperties() {
        brm.setRangeProperties(VALUE, EXTENT, MIN, MAX, IS_ADJUSTING);
        assertEquals("", fireOrder);
        brm.setRangeProperties(6, 7, -4, 200, true);
        checkValues(6, 7, -4, 200, true);
        assertEquals("initial", fireOrder);
        fireOrder = "";
        brm.setRangeProperties(5, 7, 6, 200, true);
        checkValues(5, 7, 5, 200, true);
        assertEquals("initial", fireOrder);
        fireOrder = "";
        brm.setRangeProperties(5, 7, 4, 0, true);
        assertEquals(5, brm.getValue());
        assertTrue(brm.getMinimum() <= brm.getValue());
        assertTrue(brm.getExtent() >= 0);
        assertTrue(brm.getValue() + brm.getExtent() <= brm.getMaximum());
        assertEquals("initial", fireOrder);
        fireOrder = "";
    }

    void checkValues(final BoundedRangeModel model, final int value, final int extent,
            final int min, final int max, final boolean isAdjusting) {
        assertEquals(value, model.getValue());
        assertEquals(extent, model.getExtent());
        assertEquals(min, model.getMinimum());
        assertEquals(max, model.getMaximum());
        assertEquals(isAdjusting, model.getValueIsAdjusting());
    }

    void checkValues(final int value, final int extent, final int min, final int max,
            final boolean isAdjusting) {
        checkValues(brm, value, extent, min, max, isAdjusting);
    }

    void resetValues() {
        brm.setRangeProperties(VALUE, EXTENT, MIN, MAX, IS_ADJUSTING);
        checkValues(VALUE, EXTENT, MIN, MAX, IS_ADJUSTING);
    }

    public void testSetGetValueIsAdjusting() {
        assertFalse(brm.getValueIsAdjusting());
        brm.setValueIsAdjusting(false);
        assertEquals("", fireOrder);
        brm.setValueIsAdjusting(true);
        assertTrue(brm.getValueIsAdjusting());
        assertEquals("initial", fireOrder);
        assertEquals(listener.event, brm.changeEvent);
        assertEquals(brm, brm.changeEvent.getSource());
    }

    public void testToString() {
        assertEquals("javax.swing.DefaultBoundedRangeModel[value=10, "
                + "extent=15, min=5, max=30, adj=false]", brm.toString());
    }

    public void testSerializable() throws Exception {
        brm.addChangeListener(listener);
        brm.setValueIsAdjusting(true);
        DefaultBoundedRangeModel brm1 = new DefaultBoundedRangeModel();
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(brm);
        so.flush();
        so.close();
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        brm1 = (DefaultBoundedRangeModel) si.readObject();
        si.close();
        assertEquals(brm.getMinimum(), brm1.getMinimum());
        assertEquals(brm.getValue(), brm1.getValue());
        assertEquals(brm.getExtent(), brm1.getExtent());
        assertEquals(brm.getMaximum(), brm1.getMaximum());
        assertEquals(brm.getValueIsAdjusting(), brm1.getValueIsAdjusting());
        ChangeListener listeners[] = brm1.getChangeListeners();
        assertEquals(0, listeners.length);
    }
}
