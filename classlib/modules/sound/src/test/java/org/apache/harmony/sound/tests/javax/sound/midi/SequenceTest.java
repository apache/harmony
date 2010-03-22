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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import junit.framework.TestCase;

public class SequenceTest extends TestCase {
    /**
     * test constants
     */
    public void test_constants() {
        assertEquals(0.0f, Sequence.PPQ, 0f);
        assertEquals(24.0f, Sequence.SMPTE_24, 0f);
        assertEquals(25.0f, Sequence.SMPTE_25, 0f);
        assertEquals(30.0f, Sequence.SMPTE_30, 0f);
        assertEquals(29.969999313354492f, Sequence.SMPTE_30DROP, 0f);
    }
    /**
     * test constructor Sequence(float, int)
     */
    public void test_constructor1() throws Exception {
        
        Sequence seq0 = new Sequence(Sequence.PPQ, 10, 2);
        seq0.getTracks();
        
        new Sequence(Sequence.PPQ, 10);
        Sequence seq2 = new Sequence(Sequence.SMPTE_24, -10);
        new Sequence(Sequence.SMPTE_25, 9854);
        new Sequence(Sequence.SMPTE_30, -82534);
        new Sequence(Sequence.SMPTE_30DROP, 0);
        
        try {
            new Sequence(32.0f, 16);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        
        assertEquals(24.0f, seq2.getDivisionType(), 0f);
        assertEquals(0, seq2.getMicrosecondLength());
        assertEquals(-10, seq2.getResolution());
        assertEquals(0, seq2.getTickLength());
        assertEquals(0, seq2.getPatchList().length);
        assertEquals(0, seq2.getTracks().length);
    }
    
    /**
     * test constructor Sequence(float, int, int)
     */
    public void test_constructor2() throws Exception {
        new Sequence(Sequence.PPQ, 10, 0);
        Sequence seq2 = new Sequence(Sequence.SMPTE_24, -10, 8762);
        Sequence seq3 = new Sequence(Sequence.SMPTE_25, 9854, -18);
        new Sequence(Sequence.SMPTE_30, -82534, 34);
        Sequence seq5 = new Sequence(Sequence.SMPTE_30DROP, 0);
        
        try {
            new Sequence(-9.1f, 16, 54);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        
        assertEquals(25.0f, seq3.getDivisionType(), 0f);
        assertEquals(0, seq3.getMicrosecondLength());
        assertEquals(9854, seq3.getResolution());
        assertEquals(0, seq3.getTickLength());
        assertEquals(0, seq3.getPatchList().length);
        assertEquals(8762, seq2.getTracks().length); //seq2
        assertEquals(0, seq3.getTracks().length);
        assertEquals(0, seq5.getTracks().length); //seq5
        
        assertEquals(1, seq2.getTracks()[0].size());
        assertEquals(0, seq2.getTracks()[0].ticks());
        
        assertEquals(0, seq2.getTracks()[345].get(0).getTick());
        
        MidiMessage mes = seq2.getTracks()[345].get(0).getMessage();
        assertEquals(3, mes.getLength());
        assertEquals(255, mes.getStatus());
        assertEquals(3, mes.getMessage().length);
        assertEquals(-1, mes.getMessage()[0]);
        assertEquals(47, mes.getMessage()[1]);
        assertEquals(0, mes.getMessage()[2]);
    }
    
    /**
     * test method createTrack()
     */
    public void test_createTrack() throws Exception {
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        assertEquals(9, seq.getTracks().length);

        Track tr = seq.createTrack();
        Track tr1 = seq.createTrack();
        /*
         * returned Track is the same that create with constructor,
         * but size of vector increase
         */
        assertEquals(1, tr.size());
        assertEquals(3, tr.get(0).getMessage().getMessage().length);
        assertEquals(-1, tr.get(0).getMessage().getMessage()[0]);
        assertEquals(47, tr.get(0).getMessage().getMessage()[1]);
        assertEquals(0, tr.get(0).getMessage().getMessage()[2]);
        
        assertEquals(1, tr1.size());
        assertEquals(3, tr1.get(0).getMessage().getMessage().length);
        assertEquals(-1, tr1.get(0).getMessage().getMessage()[0]);
        assertEquals(47, tr1.get(0).getMessage().getMessage()[1]);
        assertEquals(0, tr1.get(0).getMessage().getMessage()[2]);
        
        assertEquals(11, seq.getTracks().length);
        
        /*
         * new Tracks accrue to the end of vector
         */
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 3L);
        tr1.add(event2);
        assertEquals(23, seq.getTracks()[10].get(0).getMessage().getMessage()[0]);
    }
    
    /**
     * test method deleteTrack(Track)
     * @throws Exception
     */
    public void test_deleteTrack() throws Exception {
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        Track tr1 = seq.createTrack();
        assertEquals(11, seq.getTracks().length);
        
        tr.add(new MidiEvent(new MidiMessage1(new byte[] {1, 2, 3}), 10));
        assertTrue(seq.deleteTrack(tr));
        assertFalse(seq.deleteTrack(tr));
        assertEquals(10, seq.getTracks().length);
        
        tr1 = null;
        assertFalse(seq.deleteTrack(tr1));
    }
    
    /**
     * test method getMicrosecondLength()
     *
     */
    public void test_getMicrosecondLength() throws Exception {
        Sequence seq = new Sequence(Sequence.PPQ, 15, 2);
        Track tr = seq.createTrack();
        MidiEvent event1 = new MidiEvent(new MidiMessage1(new byte[] {1, 2, 3}), -10L);
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 3L);
        MidiEvent event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1}), 6L);
        
        /*------- Sequence.PPQ -------*/
        tr.add(event2);     
        assertEquals(100000, seq.getMicrosecondLength());
        Track tr1 = seq.createTrack();
        tr1.add(event1);
        tr1.add(event2);
        tr1.add(event3);
        assertEquals(200000, seq.getMicrosecondLength());
        seq.deleteTrack(tr1);
        assertEquals(100000, seq.getMicrosecondLength());
        seq.deleteTrack(tr);
        assertEquals(0, seq.getMicrosecondLength());
        
        /*------- Sequence.SMPTE_24 -------*/
        event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 0L);
        seq = new Sequence(Sequence.SMPTE_24, 17, 2);
        tr = seq.createTrack();
        tr.add(event2);     
        assertEquals(0, seq.getMicrosecondLength());
        tr1 = seq.createTrack();
        tr1.add(event1);
        tr1.add(event2);
        tr1.add(event3);
        assertEquals(14705, seq.getMicrosecondLength());
        seq.deleteTrack(tr1);
        assertEquals(0, seq.getMicrosecondLength());
        
        /*------- Sequence.SMPTE_25 -------*/
        event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 3L);
        event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1}), 97913L);
        seq = new Sequence(Sequence.SMPTE_25, -5, 2);
        tr = seq.createTrack();
        tr.add(event2);     
        assertEquals(-24000, seq.getMicrosecondLength());
        tr1 = seq.createTrack();
        tr1.add(event1);
        tr1.add(event2);
        tr1.add(event3);
        assertEquals(-783304000, seq.getMicrosecondLength());
        seq.deleteTrack(tr1);
        assertEquals(-24000, seq.getMicrosecondLength());

        /*------- Sequence.SMPTE_30 -------*/
        seq = new Sequence(Sequence.SMPTE_30, 0, 2);
        tr = seq.createTrack();
        tr.add(event2);     
        assertEquals(9223372036854775807L, seq.getMicrosecondLength());
        tr1 = seq.createTrack();
        tr1.add(event1);
        tr1.add(event2);
        tr1.add(event3);
        assertEquals(9223372036854775807L, seq.getMicrosecondLength());
        seq.deleteTrack(tr1);
        assertEquals(9223372036854775807L, seq.getMicrosecondLength());
        
        /*------- Sequence.SMPTE_30DROP -------*/
        seq = new Sequence(Sequence.SMPTE_30DROP, 17, 2);
        tr = seq.createTrack();
        tr.add(event2);     
        assertEquals(5888L, seq.getMicrosecondLength());
        tr1 = seq.createTrack();
        tr1.add(event1);
        tr1.add(event2);
        tr1.add(event3);
        assertEquals(192178456L, seq.getMicrosecondLength());
        seq.deleteTrack(tr1);
        assertEquals(5888L, seq.getMicrosecondLength());
    }
    
    /**
     * test method getPatchList()
     *
     */
    public void test_getPatchList() throws Exception {
        //TODO
        /*
         * I don't understand how this method works
         */
        Sequence seq = new Sequence(Sequence.PPQ, 987, 2);
        Track tr = seq.createTrack();
        MidiEvent event1 = new MidiEvent(new MidiMessage1(new byte[] {1, 2, 3, 34, -98, -27}), -10L);
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4, 78, -12, 5}), 3L);
        MidiEvent event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1, 87, 9, 8, -2}), 6L);
        tr.add(event1);
        tr.add(event3);
        Track tr1 = seq.createTrack();
        tr1.add(event2);
        assertEquals(0, seq.getPatchList().length);
    }
    
    /**
     * test method getTickLength()
     *
     */
    public void test_getTickLength() throws Exception {
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        Track tr = seq.createTrack();
        MidiEvent event1 = new MidiEvent(new MidiMessage1(new byte[] {1, 2, 3}), -10L);
        MidiEvent event2 = new MidiEvent(new MidiMessage1(new byte[] {23, -16, 4}), 2L);
        MidiEvent event3 = new MidiEvent(new MidiMessage1(new byte[] {3, -67, -1}), 6L);
        tr.add(event1);
        tr.add(event2);
        
        assertEquals(2, seq.getTickLength());
        Track tr1 = seq.createTrack();
        tr1.add(event1);
        tr1.add(event2);
        tr1.add(event3);
        assertEquals(6, seq.getTickLength());
        seq.deleteTrack(tr1);
        assertEquals(2, seq.getTickLength());
        seq.deleteTrack(tr);
        assertEquals(0, seq.getTickLength());
    }
    
    /**
     * test method getTracks()
     *
     */
    public void test_getTracks() throws Exception {
        Sequence seq = new Sequence(Sequence.SMPTE_24, 67, 9);
        seq.createTrack();
        Track tr1 = seq.createTrack();
        
        Track[] tracks = seq.getTracks();
        assertEquals(11, tracks.length);
        
        /*
         * actions with array doesn't influence on 
         * initial data...
         */
        assertEquals(tr1, tracks[10]);
        tracks[10] = null;
        assertTrue(seq.deleteTrack(tr1));
        assertTrue(seq.deleteTrack(tracks[9]));
        /*
         * ...and action with initial data doesn't 
         * influence on array
         */
        assertEquals(11, tracks.length);
    }
    
    
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
