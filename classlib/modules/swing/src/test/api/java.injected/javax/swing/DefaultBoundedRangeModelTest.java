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
 * @author Sergey Burlak
 */
package javax.swing;

import java.util.EventListener;
import java.util.List;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DefaultBoundedRangeModelTest extends SwingTestCase {
    private DefaultBoundedRangeModel model;

    private List<String> testList;

    @Override
    public void setUp() throws Exception {
        testList = new Vector<String>();
        model = new DefaultBoundedRangeModel();
        model.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                testList.add("1");
            }
        });
    }

    @Override
    public void tearDown() throws Exception {
        model = null;
        testList = null;
    }

    public void testSetExtent() throws Exception {
        checkValues(0, 0, 0, 100, 0);
        model.setExtent(10);
        checkValues(0, 10, 0, 100, 1);
        model.setValue(90);
        checkValues(90, 10, 0, 100, 2);
        model.setExtent(20);
        checkValues(90, 10, 0, 100, 2);
        model.setExtent(-10);
        checkValues(90, 0, 0, 100, 3);
    }

    public void testSetValue() throws Exception {
        model.setValue(50);
        checkValues(50, 0, 0, 100, 1);
        model.setValue(99);
        checkValues(99, 0, 0, 100, 2);
        model.setValue(20);
        model.setExtent(10);
        model.setMinimum(5);
        checkValues(20, 10, 5, 100, 5);
        model.setValue(4);
        checkValues(5, 10, 5, 100, 6);
        model.setValue(96);
        checkValues(90, 10, 5, 100, 7);
    }

    public void testSetMaximum() throws Exception {
        model.setExtent(10);
        model.setValue(90);
        checkValues(90, 10, 0, 100, 2);
        model.setMaximum(90);
        checkValues(80, 10, 0, 90, 3);
        model.setMaximum(200);
        checkValues(80, 10, 0, 200, 4);
        model.setMinimum(20);
        checkValues(80, 10, 20, 200, 5);
        model.setValue(0);
        model.setMaximum(23);
        checkValues(20, 3, 20, 23, 7);
        model.setMaximum(-1);
        checkValues(-1, 0, -1, -1, 8);
        model.setRangeProperties(50, 0, 30, 100, false);
        checkValues(50, 0, 30, 100, 9);
        model.setMaximum(20);
        checkValues(20, 0, 20, 20, 10);
    }

    public void testSetMinimum() throws Exception {
        model.setExtent(10);
        model.setValue(30);
        checkValues(30, 10, 0, 100, 2);
        model.setMinimum(20);
        checkValues(30, 10, 20, 100, 3);
        model.setMinimum(40);
        checkValues(40, 10, 40, 100, 4);
        model.setMinimum(20);
        checkValues(40, 10, 20, 100, 5);
        model.setMinimum(61);
        checkValues(61, 10, 61, 100, 6);
        model.setMinimum(99);
        checkValues(99, 1, 99, 100, 7);
    }

    public void testSetValueIsAdjusting() throws Exception {
        assertFalse(model.getValueIsAdjusting());
        model.setValueIsAdjusting(true);
        assertTrue(model.getValueIsAdjusting());
        assertEquals(1, testList.size());
    }

    public void testSetRangeProperties() throws Exception {
        checkValues(0, 0, 0, 100, 0);
        model.setRangeProperties(0, 10, 0, 20, true);
        checkValues(0, 10, 0, 20, 1);
        model.setRangeProperties(300, 10, 0, 20, true);
        checkValues(300, 0, 0, 300, 2);
        model.setRangeProperties(100, 10, -40, 200, true);
        checkValues(100, 10, -40, 200, 3);
        model.setRangeProperties(50, 10, 100, 200, true);
        checkValues(50, 10, 50, 200, 4);
    }

    public void testListeners() throws Exception {
        assertEquals(1, model.getChangeListeners().length);
        ChangeListener l = new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
            }
        };
        model.addChangeListener(l);
        assertEquals(2, model.getChangeListeners().length);
        assertEquals(2, model.getListeners(ChangeListener.class).length);
        model.removeChangeListener(l);
        assertEquals(1, model.getChangeListeners().length);
        assertEquals(1, model.getListeners(ChangeListener.class).length);
        assertEquals(0, model.getListeners(EventListener.class).length);
    }

    private void checkValues(final int value, final int extent, final int min, final int max,
            final int eventNumber) {
        assertEquals(value, model.getValue());
        assertEquals(extent, model.getExtent());
        assertEquals(min, model.getMinimum());
        assertEquals(max, model.getMaximum());
        assertEquals(eventNumber, testList.size());
    }
}
