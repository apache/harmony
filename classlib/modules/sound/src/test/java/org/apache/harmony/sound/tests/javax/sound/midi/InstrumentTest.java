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

package org.apache.harmony.sound.tests.javax.sound.midi;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;

import junit.framework.TestCase;

public class InstrumentTest extends TestCase {
    /**
     * Test the method Instrument.getPatch() 
     */
    public void test_getPatch() {
        Instrument1 inst = new Instrument1(null, null, null, null);
        assertNull(inst.getPatch());

        Instrument1 inst2 = new Instrument1(null, new Patch(10, 20), "Test", null);
        assertEquals(10, inst2.getPatch().getBank());
        assertEquals(20, inst2.getPatch().getProgram());

        Instrument1 inst3 = new Instrument1(null, new Patch(-10, 208), "Test", null);
        assertEquals(-10, inst3.getPatch().getBank());
        assertEquals(208, inst3.getPatch().getProgram());
    }

    /**
     * Test the method Instrument.getName()
     */
    public void test_getName() {
        Instrument1 inst = new Instrument1(null, null, null, null);
        assertNull(inst.getName());

        Instrument1 inst2 = new Instrument1(null, new Patch(10, 20), "Test", null);
        assertEquals("Test",inst2.getName());
    }

    /**
     * Test the method Instrument.getDataClass()
     */
    public void test_getDataClass() {
        Instrument1 inst = new Instrument1(null, null, null, null);
        assertNull(inst.getDataClass());
    }

    /**
     * Subsidiary class in order to use constructor
     * of class Instrument, because it declared as protected
     */
    static class Instrument1 extends Instrument {
        Instrument1(Soundbank soundbank, Patch patch, String name, Class<?> dataClass) {
            super(soundbank, patch, name, dataClass);
        }

        @Override
        public Object getData() {
            return null;
        }
    }
}
