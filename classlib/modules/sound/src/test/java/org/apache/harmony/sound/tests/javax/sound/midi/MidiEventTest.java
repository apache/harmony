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

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;

import junit.framework.TestCase;

public class MidiEventTest extends TestCase {
    public void test_getMessage() {
        byte[] bt = new byte[] {1, 2, 3, 4};
        MidiMessage1 message = new MidiMessage1(bt);
        MidiEvent event = new MidiEvent(message, 10L);
        MidiMessage mm = event.getMessage();
        assertEquals(1, mm.getMessage()[0]);
        assertEquals(2, mm.getMessage()[1]);
        assertEquals(3, mm.getMessage()[2]);
        assertEquals(4, mm.getMessage()[3]);
        bt[0] = 10;
        bt[1] = 20;
        bt[2] = 30;
        bt[3] = 40;
        /*
         * values change
         */
        assertEquals(10, mm.getMessage()[0]);
        assertEquals(20, mm.getMessage()[1]);
        assertEquals(30, mm.getMessage()[2]);
        assertEquals(40, mm.getMessage()[3]);
        byte[] nb = mm.getMessage();
        nb[0] = 45;
        nb[1] = -12;
        nb[2] = 90;
        nb[3] = 14;
        /*
         * values don't change
         */
        assertEquals(10, mm.getMessage()[0]);
        assertEquals(20, mm.getMessage()[1]);
        assertEquals(30, mm.getMessage()[2]);
        assertEquals(40, mm.getMessage()[3]);
        
        MidiEvent event1 = new MidiEvent(null, 10L);
        assertNull(event1.getMessage());
    }
    
    static class MidiMessage1 extends MidiMessage {
        public MidiMessage1(byte[] data) {
            super(data);
        }
        
        @Override
        public Object clone() {
            return null;
        }
    }
}
