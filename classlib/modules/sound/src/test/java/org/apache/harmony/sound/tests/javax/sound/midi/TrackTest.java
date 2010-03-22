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
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import junit.framework.TestCase;

public class TrackTest extends TestCase {
    /**
     * test method add()
     * @throws Exception
     */
    public void test_add() throws Exception {
        /*
         * create an empty Track
         */
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        assertEquals(1, tr.size());
        assertEquals(0, tr.ticks());
        
        byte[] bt = new byte[] {1, 2, 3};
        MidiMessage1 message = new MidiMessage1(bt);
        MidiEvent event = new MidiEvent(message, 10L);
        
        assertTrue(tr.add(event));
        assertFalse(tr.add(event));
        bt[0] = 10;
        bt[1] = 20;
        assertFalse(tr.add(event));
        /*
         * values change
         */
        assertEquals(10, tr.get(0).getMessage().getMessage()[0]);
        assertEquals(20, tr.get(0).getMessage().getMessage()[1]);
        assertEquals(3, tr.get(0).getMessage().getMessage()[2]);
        
        /*
         * if event is equals null, so it doesn't add to Track, i.e. 
         * method add(MidiEvent) return 'false'
         */
        assertFalse(tr.add(null));
        
        /*
         * In the first place, the MidiMessage with the array MidiMessage.data contains 
         * -1, 47 and 0 is the meta-event End of Track, and so it 
         * accrue to Track always, even it contains meta-event already, i.e. method
         * add(MidiEvent) always return 'true' in this case, but size of
         * Track doesn't increase.
         * 
         * In the second place, other events accrue to Track taking
         * into account value of tick of MidiEvent, i.e. the MidiEvent
         * with the smallest value of tick will be the first in the Track
         * and so on and the MidiEvent with the biggest value of tick
         * will be the last but one; the last is the meta-event End of Track
         * 
         * If values of ticks of two or more events are equals, they add 
         * to Track on the basis of addition, i.e. if any event adds to Track,
         * it will be after events with the same value of tick
         */
        assertTrue(tr.add(new MidiEvent(new MidiMessage1(new byte[] {-1, 47, 0}), -20)));
        assertTrue(tr.add(new MidiEvent(new MidiMessage1(new byte[] {-1, 47, 0}), 0)));
        
        assertTrue(tr.add(new MidiEvent(new MidiMessage1(new byte[] {-23, 92, 89}), 8)));
        assertTrue(tr.add(new MidiEvent(new MidiMessage1(new byte[] {23, 2, -9}), 8)));
        
        assertEquals(-23, tr.get(0).getMessage().getMessage()[0]);
        assertEquals(23, tr.get(1).getMessage().getMessage()[0]);
        assertEquals(10, tr.get(2).getMessage().getMessage()[0]);
        assertEquals(-1, tr.get(3).getMessage().getMessage()[0]);
    }

    /**
     * test method get(int)
     * @throws Exception
     */
    public void test_get() throws Exception {
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        
        /*
         * numeration of events begin with 0, and so the first 
         * event gets with index 0, the second - with index 1 
         * and so on
         */
        try {
            tr.get(-1);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {}
        
        try {
            tr.get(1);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {}
    }

    /**
     * test method remove(MidiEvent)
     * @throws Exception
     */
    public void test_remove() throws Exception {
        /*
         * create an empty Track
         */
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        
        byte[] bt = new byte[] {1, 2, 3};
        MidiMessage1 message = new MidiMessage1(bt);
        MidiEvent event1 = new MidiEvent(message, 10L);
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 0);
        MidiEvent event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1}), 6L);
        tr.add(event1);
        tr.add(event2);
        tr.add(event3);
        assertEquals(4, tr.size());
        
        assertTrue(tr.remove(event3));
        assertFalse(tr.remove(event3));
        assertEquals(3, tr.size());
        
        assertFalse(tr.remove(new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 0)));
        assertTrue(tr.remove(event2));
        
        assertFalse(tr.remove(null));
        
        /*
         * I can remove meta-event End of Track
         */
        MidiEvent event4 = tr.get(1);
        assertTrue(tr.remove(event4));
        
        assertTrue(tr.add(event4));
        assertTrue(tr.remove(event4));
        
        assertTrue(tr.remove(event1));
        
        /*
         * If I remove meta-event End of Track and I have some
         * events in Track still, and I try to add new event, it's 
         * all right, it adds, and so meta-event End of Track adds too;
         * if Track doesn't contain any events, i.e. method size() 
         * return 0, if I try to add meta-event End of Track, it's
         * all right, but if I try to add not meta-event, I have some problem.
         * In the first place, meta-event adds to the Track, but event
         * I want to add doesn't add, i.e. size() return 1. But after it
         * I use method add(MidiEvent), and 
         * method add(<the_same_event_I_want_to_add>) return 'false'!!!
         * And method remove(<the_same_event_I_want_to_add>) return 
         * 'false' too! And only now method add(<the_same_event_I_want_to_add>)
         * return 'true', and this event adds to Track;
         * Other events accrue to Track correctly.
         */
        assertEquals(0, tr.size());
        
        assertTrue(tr.add(event4)); //add meta-event to empty track; it's OK
        
        assertTrue(tr.add(event1)); //add some event
        assertEquals(2, tr.size());
        assertTrue(tr.remove(event4)); //remove meta-event End of Track
        assertTrue(tr.add(event2)); //add some event...
        assertEquals(3, tr.size()); //...and size of Track is 3; meta-event 
                                    //was added
        assertTrue(tr.remove(tr.get(2)));
        assertTrue(tr.remove(event2));
        assertTrue(tr.remove(event1)); //now Track is empty
        try {
            tr.add(event2); //add some event; I catch exception
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {}
        assertEquals(1, tr.size()); //size of Track is 1; it contain meta-event
        assertTrue(tr.add(event1)); //I can add event1,...
        assertFalse(tr.add(event2)); //...but I can't add event2...
        assertFalse(tr.remove(event2)); //...and remove it too!
        assertTrue(tr.add(event2)); //but now it's all right!
        assertEquals(3, tr.size()); //Track contain two my events and meta-event
    }

    /**
     * test method size()
     * @throws Exception
     */
    public void test_size() throws Exception {
        /*
         * create an empty Track
         */
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        assertEquals(1, tr.size());
        MidiEvent event1 = new MidiEvent(new MidiMessage1(new byte[] {1, 2, 3}), 10L);
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 0);
        MidiEvent event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1}), 6L);
        tr.add(event1);
        tr.add(event2); 
        tr.add(event3);
        assertEquals(4, tr.size());
        tr.add(event1); //false; it contains already
        assertEquals(4, tr.size());
        tr.remove(event1);
        tr.remove(event2);
        assertEquals(2, tr.size());
        tr.remove(event3);
        tr.remove(tr.get(0));
        assertEquals(0, tr.size());
        tr.add(new MidiEvent(new MidiMessage1(new byte[] {-1, 47, 0}), 6L));
        assertEquals(1, tr.size());
        tr.remove(tr.get(0));
        assertEquals(0, tr.size());
        try {
            tr.add(event1);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {}
        assertEquals(1, tr.size());
    }

    /**
     * test method ticks()
     * @throws Exception
     */
    public void test_ticks() throws Exception {
        /*
         * create an empty Track
         */
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        assertEquals(0, tr.ticks());
        MidiEvent event1 = new MidiEvent(new MidiMessage1(new byte[] {1, 2, 3}), -10L);
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 2L);
        MidiEvent event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1}), 6L);
        /*
         * values of Track.ticks() only positive;
         * value of Track.ticks() is the biggest value of ticks of events that
         * contain in the Track; 
         * if I remove event with the biggest value of 
         * tick, value that return method Track.ticks() will be the same
         */
        tr.add(event1);
        assertEquals(0, tr.ticks());
        tr.add(event2);
        assertEquals(2, tr.ticks());
        tr.add(event3);
        assertEquals(6, tr.ticks());
        tr.remove(event3); //I remove event with the biggest tick,...
        assertEquals(6, tr.ticks()); //...but value that return method Track.ticks() the same
        tr.remove(event2);
        tr.remove(event1);
        assertEquals(6, tr.ticks()); //and even now...
        tr.add(event2);
        assertEquals(6, tr.ticks()); //and now...
        tr.remove(tr.get(1));
        tr.remove(event2);
        assertEquals(0, tr.size()); //Track is empty
        assertEquals(0, tr.ticks()); //Track is empty, value that return Track.ticks() equals 0
    }
    
    /**
     * subsidiary class to use abstract class MidiMessage
     * 
     */
    static public class MidiMessage1 extends MidiMessage {
        MidiMessage1(byte[] data) {
            super(data);
        }
        
        @Override
        public Object clone() {
            return null;
        }
    }
}
