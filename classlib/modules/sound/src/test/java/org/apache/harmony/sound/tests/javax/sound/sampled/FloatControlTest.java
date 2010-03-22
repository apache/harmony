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

package org.apache.harmony.sound.tests.javax.sound.sampled;

import javax.sound.sampled.FloatControl;

import junit.framework.TestCase;

public class FloatControlTest extends TestCase {

    public void testFloatControl() {

        FloatControl control = new MyControl(FloatControl.Type.MASTER_GAIN, 1f,
                10f, 3f, 4, 5f, "units", "minLabel", "midLabel", "maxLabel");

        assertEquals(FloatControl.Type.MASTER_GAIN, control.getType());
        assertEquals(1f, control.getMinimum());
        assertEquals(10f, control.getMaximum());
        assertEquals("units", control.getUnits());
        assertEquals("minLabel", control.getMinLabel());
        assertEquals("midLabel", control.getMidLabel());
        assertEquals("maxLabel", control.getMaxLabel());
        assertEquals(3f, control.getPrecision());
        assertEquals(4, control.getUpdatePeriod());

        assertEquals(5f, control.getValue());
        control.setValue(9f);
        assertEquals(9f, control.getValue());
        control.shift(9f, 4f, 5);
        assertEquals(4f, control.getValue());
        assertEquals(
                "Master Gain with current value: 4.0 units (range: 1.0 - 10.0)",
                control.toString());

        try {
            control.setValue(15f);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }

        control = new MyControl(FloatControl.Type.SAMPLE_RATE, 1f, 10f, 3f, 4,
                5f, "units");
        assertEquals(FloatControl.Type.SAMPLE_RATE, control.getType());
        assertEquals(1f, control.getMinimum());
        assertEquals(10f, control.getMaximum());
        assertEquals("units", control.getUnits());
        assertEquals("", control.getMinLabel());
        assertEquals("", control.getMidLabel());
        assertEquals("", control.getMaxLabel());
        assertEquals(3f, control.getPrecision());
        assertEquals(4, control.getUpdatePeriod());
        assertEquals(5f, control.getValue());
        assertEquals(
                "Sample Rate with current value: 5.0 units (range: 1.0 - 10.0)",
                control.toString());

    }

    private class MyControl extends FloatControl {
        public MyControl(FloatControl.Type type, float minimum, float maximum,
                float precision, int updatePeriod, float initialValue,
                String units, String minLabel, String midLabel, String maxLabel) {
            super(type, minimum, maximum, precision, updatePeriod,
                    initialValue, units, minLabel, midLabel, maxLabel);
        }

        public MyControl(FloatControl.Type type, float minimum, float maximum,
                float precision, int updatePeriod, float initialValue,
                String units) {
            super(type, minimum, maximum, precision, updatePeriod,
                    initialValue, units);
        }
    }

}