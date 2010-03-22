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
 * @author Dennis Ushakov
 */
package javax.swing;

public class SpinnerNumberModelTest extends BasicSwingTestCase {
    private SpinnerNumberModel model;

    private ChangeController chl;

    @Override
    public void setUp() {
        model = new SpinnerNumberModel();
        chl = new ChangeController();
        model.addChangeListener(chl);
    }

    @Override
    public void tearDown() {
        model = null;
        chl = null;
    }

    public void testSpinnerNumberModel() {
        assertNull(model.getMaximum());
        assertNull(model.getMinimum());
        assertEquals(new Integer(0), model.getValue());
        assertEquals(new Integer(1), model.getStepSize());
        final Integer val = new Integer(10);
        model = new SpinnerNumberModel(10, 10, 10, 10);
        assertEquals(val, model.getValue());
        assertEquals(val, model.getStepSize());
        assertEquals(val, model.getValue());
        assertEquals(val, model.getStepSize());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerNumberModel(null, val, val, val);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerNumberModel(val, val, val, null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model = new SpinnerNumberModel(val, val, new Integer(-1), val);
            }
        });
    }

    public void testSetGetMinimum() {
        final Double min = new Double(-2.);
        model.setMinimum(min);
        assertTrue(chl.isChanged());
        assertSame(min, model.getMinimum());
        model.setMinimum(new Integer(10));
        model.setMinimum(min);
        chl.reset();
        model.setMinimum(min);
        assertFalse(chl.isChanged());
    }

    public void testSetGetMaximum() {
        final Double max = new Double(35.);
        model.setMaximum(max);
        assertTrue(chl.isChanged());
        assertSame(max, model.getMaximum());
        model.setMaximum(new Integer(-10));
        model.setMaximum(max);
        chl.reset();
        model.setMaximum(max);
        assertFalse(chl.isChanged());
    }

    public void testSetGetStepSize() {
        final Integer step = new Integer(3);
        model.setStepSize(step);
        assertTrue(chl.isChanged());
        assertSame(step, model.getStepSize());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setStepSize(null);
            }
        });
        model.setStepSize(step);
        chl.reset();
        model.setStepSize(step);
        assertFalse(chl.isChanged());
    }

    public void testSetGetValue() {
        final Integer value = new Integer(10);
        model.setValue(value);
        assertTrue(chl.isChanged());
        assertSame(value, model.getValue());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setValue(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setValue("test");
            }
        });
        model.setValue(value);
        chl.reset();
        model.setValue(value);
        assertFalse(chl.isChanged());
    }

    public void testGetNumber() {
        final Integer value = new Integer(10);
        model.setValue(value);
        assertSame(model.getValue(), model.getNumber());
    }

    public void testGetPreviousValue() {
        model.getPreviousValue();
        Integer value = new Integer(10);
        model.setValue(value);
        Integer min = new Integer(8);
        model.setMinimum(min);
        Integer step = new Integer(2);
        model.setStepSize(step);
        assertEquals(min, model.getPreviousValue());
        step = new Integer(3);
        model.setStepSize(step);
        assertNull(model.getPreviousValue());
    }

    public void testNextValue() {
        model.getNextValue();
        Double value = new Double(10);
        Double max = new Double(12);
        Integer step = new Integer(2);
        model = new SpinnerNumberModel(value, null, max, step);
        assertEquals(max, model.getNextValue());
        step = new Integer(3);
        model.setStepSize(step);
        assertNull(model.getNextValue());
    }
}
