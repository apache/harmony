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

import java.util.Arrays;

import javax.sound.sampled.EnumControl;

import junit.framework.TestCase;

public class EnumControlTest extends TestCase {

    public void testEnumControl() {
        Object[] values = new Object[] { "val1", "val2" };
        EnumControl control = new MyControl(EnumControl.Type.REVERB, values,
                "val1");

        assertEquals("val1", control.getValue());
        assertTrue(Arrays.equals(values, control.getValues()));
        assertEquals("Reverb with current value: val1", control.toString());

        control.setValue("val2");
        assertEquals("val2", control.getValue());
        assertTrue(Arrays.equals(values, control.getValues()));
        assertEquals("Reverb with current value: val2", control.toString());

        try {
            control.setValue("val3");
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {            
        }
    }

    private class MyControl extends EnumControl {
        public MyControl(EnumControl.Type type, Object[] values, Object value) {
            super(type, values, value);
        }
    }

}
