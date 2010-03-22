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
 * @author Evgeny S. Sidorenko
 */

package org.apache.harmony.sound.tests.javax.sound.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import junit.framework.TestCase;

public class ShortMessageTest extends TestCase {

    /**
     * test constants
     *
     */
    public void test_constants() {
        assertEquals(254, ShortMessage.ACTIVE_SENSING);
        assertEquals(208, ShortMessage.CHANNEL_PRESSURE);
        assertEquals(251, ShortMessage.CONTINUE);
        assertEquals(176, ShortMessage.CONTROL_CHANGE);
        assertEquals(247, ShortMessage.END_OF_EXCLUSIVE);
        assertEquals(241, ShortMessage.MIDI_TIME_CODE);
        assertEquals(128, ShortMessage.NOTE_OFF);
        assertEquals(144, ShortMessage.NOTE_ON);
        assertEquals(224, ShortMessage.PITCH_BEND);
        assertEquals(160, ShortMessage.POLY_PRESSURE);
        assertEquals(192, ShortMessage.PROGRAM_CHANGE);
        assertEquals(242, ShortMessage.SONG_POSITION_POINTER);
        assertEquals(243, ShortMessage.SONG_SELECT);
        assertEquals(250, ShortMessage.START);
        assertEquals(252, ShortMessage.STOP);
        assertEquals(255, ShortMessage.SYSTEM_RESET);
        assertEquals(248, ShortMessage.TIMING_CLOCK);
        assertEquals(246, ShortMessage.TUNE_REQUEST);
    }

    /**
     * test constructors
     *
     */
    public void test_constructor_ShortMessage1() {
        ShortMessage1 message = new ShortMessage1();
        assertEquals(0, message.getChannel());
        assertEquals(144, message.getCommand());
        assertEquals(64, message.getData1());
        assertEquals(127, message.getData2());
        assertEquals(3, message.getLength());
        assertEquals(144, message.getStatus());
        assertEquals(3, message.getMessage().length);
        assertEquals(-112, message.getMessage()[0]);
        assertEquals(64, message.getMessage()[1]);
        assertEquals(127, message.getMessage()[2]);

        byte[] bt = new byte[] { -95, -5, 9, 56, -18 };
        ShortMessage1 message1 = new ShortMessage1(bt);
        assertEquals(1, message1.getChannel());
        assertEquals(160, message1.getCommand());
        assertEquals(251, message1.getData1());
        assertEquals(9, message1.getData2());
        assertEquals(5, message1.getLength());
        assertEquals(161, message1.getStatus());
        assertEquals(5, message1.getMessage().length);
        assertEquals(-95, message1.getMessage()[0]);
        assertEquals(-5, message1.getMessage()[1]);
        assertEquals(9, message1.getMessage()[2]);
        assertEquals(56, message1.getMessage()[3]);
        assertEquals(-18, message1.getMessage()[4]);
        bt[0] = 1;
        bt[1] = 2;
        bt[2] = 3;
        bt[3] = 4;
        bt[4] = 5;
        /*
         * values change
         */
        assertEquals(1, message1.getChannel());
        assertEquals(0, message1.getCommand());
        assertEquals(2, message1.getData1());
        assertEquals(3, message1.getData2());
        assertEquals(5, message1.getLength());
        assertEquals(1, message1.getStatus());
        assertEquals(1, message1.getMessage()[0]);
        assertEquals(2, message1.getMessage()[1]);
        assertEquals(3, message1.getMessage()[2]);
        assertEquals(4, message1.getMessage()[3]);
        assertEquals(5, message1.getMessage()[4]);

        ShortMessage1 message2 = new ShortMessage1(new byte[] { 95, 14 });
        assertEquals(15, message2.getChannel());
        assertEquals(80, message2.getCommand());
        assertEquals(14, message2.getData1());
        assertEquals(0, message2.getData2());
        assertEquals(2, message2.getLength());
        assertEquals(95, message2.getStatus());
        assertEquals(2, message2.getMessage().length);

        ShortMessage1 message3 = new ShortMessage1(null);
        assertEquals(0, message3.getChannel());
        assertEquals(0, message3.getCommand());
        assertEquals(0, message3.getData1());
        assertEquals(0, message3.getData2());
        assertEquals(0, message3.getLength());
        assertEquals(0, message3.getStatus());
        try {
            message3.getMessage();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        ShortMessage1 message4 = new ShortMessage1(new byte[] {0});
        assertEquals(0, message4.getChannel());
        assertEquals(0, message4.getCommand());
        assertEquals(0, message4.getData1());
        assertEquals(0, message4.getData2());
        assertEquals(1, message4.getLength());
        assertEquals(0, message4.getStatus());
        assertEquals(1, message4.getMessage().length);
        
        ShortMessage1 message5 = new ShortMessage1(new byte[0]);
        assertEquals(0, message5.getChannel());
        assertEquals(0, message5.getCommand());
        assertEquals(0, message5.getData1());
        assertEquals(0, message5.getData2());
        assertEquals(0, message5.getLength());
        assertEquals(0, message5.getStatus());
        assertEquals(0, message5.getMessage().length);
    }

    /**
     * test method setMessage(int) of class ShortMessage
     */
    public void test_setMessage1() throws Exception {
        ShortMessage1 message = new ShortMessage1();
        /*
         * value of variable status is more or equals 246 and
         * less or equals 255
         */
        try {
            message.setMessage(245);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.setMessage(256);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        
        message.setMessage(250);
        /*
         * channel change from 0 up to 15, and
         * channel + command == status, so
         * the value of command divisible by 16
         */
        assertEquals(10, message.getChannel());
        assertEquals(240, message.getCommand());
        assertEquals(0, message.getData1());
        assertEquals(0, message.getData2());
        assertEquals(1, message.getLength());
        assertEquals(250, message.getStatus());
        assertEquals(1, message.getMessage().length);
    }

    /**
     * test method setMessage(int, int, int) of 
     * class ShortMessage
     */
    public void test_setMessage2() throws Exception {
        ShortMessage1 message = new ShortMessage1();
        /*
         * value of variable status is more or equals 246 and
         * less or equals 255
         */
        try {
            message.setMessage(245, 34, 56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        try {
            message.setMessage(256, 34, 56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        
        message.setMessage(250, 34, 56);
        /*
         * channel change from 0 up to 15, and
         * channel + command == status, so
         * the value of command divisible by 16.
         */
        assertEquals(10, message.getChannel());
        assertEquals(240, message.getCommand());
        assertEquals(0, message.getData1());
        assertEquals(0, message.getData2());
        assertEquals(1, message.getLength());
        assertEquals(250, message.getStatus());
        assertEquals(1, message.getMessage().length);
    }

    /**
     * test method setMessage(int, int, int, int) of 
     * class ShortMessage
     */
    public void test_setMessage3() throws Exception {
        ShortMessage1 message = new ShortMessage1();
        /*
         * value of variable command is more or equals 128 and
         * less or equals 239
         */
        try {
            message.setMessage(127, 10, 34, 56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        try {
            message.setMessage(240, 34, 56, 13);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        /*
         * value of variable channel is more or equals 0 and 
         * less or equals 15
         */
        try {
            message.setMessage(200, -1, 34, 56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        try {
            message.setMessage(200, 16, 34, 56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        /*
         * value of data1 and data2 is more or equals 0 and
         * less or equals 127, but when command more or
         * equals 192 and less or equals 223 the second data,
         * data2, is unused, because getDataLength(int) return 1
         * in this case, and in other cases it return 2
         */
        try {
            message.setMessage(200, 12, -1, 56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.setMessage(225, 8, 34, 456);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        
        /*
         * it's all right
         */
        message.setMessage(200, 8, 34, 456);

        message.setMessage(200, 9, 34, 56);
        /*
         * channel change from 0 up to 15
         * command must to divisible by 16, and so it less or 
         * equals parameter command
         * status is sum of channel and command
         */
        assertEquals(9, message.getChannel());
        assertEquals(192, message.getCommand());
        assertEquals(34, message.getData1());
        assertEquals(0, message.getData2());
        assertEquals(2, message.getLength());
        assertEquals(201, message.getStatus());
        assertEquals(2, message.getMessage().length);

        message.setMessage(148, 9, 34, 56);
        assertEquals(9, message.getChannel());
        assertEquals(144, message.getCommand());
        assertEquals(34, message.getData1());
        assertEquals(56, message.getData2());
        assertEquals(3, message.getLength());
        assertEquals(153, message.getStatus());
        assertEquals(3, message.getMessage().length);
    }

    /**
     * test method getChannel() of class ShortMessage
     *
     */
    public void test_getChannel() throws Exception {
        ShortMessage message = new ShortMessage();
        assertEquals(0, message.getChannel());

        byte[] bt = new byte[] {23, 16, 35};
        ShortMessage1 message1 = new ShortMessage1(bt);
        assertEquals(7, message1.getChannel());
        bt[0] = 15;
        /*
         * value change
         */
        assertEquals(15, message1.getChannel());
        
        ShortMessage1 message2 = new ShortMessage1(null);
        assertEquals(0, message2.getChannel());

        message.setMessage(249);
        assertEquals(9, message.getChannel());

        message.setMessage(250, 14, 62);
        assertEquals(10, message.getChannel());

        message.setMessage(234, 15, 14, 62);
        assertEquals(15, message.getChannel());

    }

    /**
     * test method getCommand() of class ShortMessage
     *
     */
    public void test_getCommand() throws Exception {
        ShortMessage message = new ShortMessage();
        assertEquals(144, message.getCommand());

        byte[] bt = new byte[] {23, 16, 35};
        ShortMessage1 message1 = new ShortMessage1(bt);
        assertEquals(16, message1.getCommand());
        bt[0] = 4;
        /*
         * value change
         */
        assertEquals(0, message1.getCommand());

        ShortMessage1 message2 = new ShortMessage1(null);
        assertEquals(0, message2.getCommand());

        message.setMessage(249);
        assertEquals(240, message.getCommand());

        message.setMessage(250, 14, 62);
        assertEquals(240, message.getCommand());

        message.setMessage(234, 15, 14, 62);
        assertEquals(224, message.getCommand());
    }

    /**
     * test method getLength() of class ShortMessage
     *
     */
    public void test_getLength() throws Exception {
        ShortMessage message = new ShortMessage();
        assertEquals(3, message.getLength());

        ShortMessage1 message1 = new ShortMessage1(new byte[] {23, 16, 35});
        assertEquals(3, message1.getLength());

        ShortMessage1 message2 = new ShortMessage1(null);
        assertEquals(0, message2.getLength());

        message.setMessage(249);
        assertEquals(1, message.getLength());

        message.setMessage(250, 14, 62);
        assertEquals(1, message.getLength());

        message.setMessage(234, 15, 14, 62);
        assertEquals(3, message.getLength());

        message.setMessage(214, 15, 14, 62);
        assertEquals(2, message.getLength());
    }

    /**
     * test method getStatus() of class ShortMessage
     *
     */
    public void test_getStatus() throws Exception {
        ShortMessage message = new ShortMessage();
        assertEquals(144, message.getStatus());

        byte[] bt = new byte[] {23, 16, 35};
        ShortMessage1 message1 = new ShortMessage1(bt);
        assertEquals(23, message1.getStatus());
        bt[0] = 84;
        /*
         * value change
         */
        assertEquals(84, message1.getStatus());

        ShortMessage1 message2 = new ShortMessage1(null);
        assertEquals(0, message2.getStatus());

        message.setMessage(249);
        assertEquals(249, message.getStatus());

        message.setMessage(250, 14, 62);
        assertEquals(250, message.getStatus());

        message.setMessage(234, 15, 14, 62);
        assertEquals(239, message.getStatus());
    }

    /**
     * test methods getData1() and getData2() of 
     * class ShortMessage
     *
     */
    public void test_getData1_2() throws Exception {
        ShortMessage message = new ShortMessage();
        assertEquals(64, message.getData1());
        assertEquals(127, message.getData2());

        byte[] bt = new byte[] { 23, 16, 35 };
        ShortMessage1 message1 = new ShortMessage1(bt);
        assertEquals(16, message1.getData1());
        assertEquals(35, message1.getData2());
        bt[1] = 84;
        bt[2] = -16;
        /*
         * values change
         */
        assertEquals(84, message1.getData1());
        assertEquals(240, message1.getData2());

        ShortMessage1 message11 = new ShortMessage1(new byte[] { 23, 16 });
        assertEquals(16, message11.getData1());
        assertEquals(0, message11.getData2());

        ShortMessage1 message12 = new ShortMessage1(new byte[] { 23 });
        assertEquals(0, message12.getData1());
        assertEquals(0, message12.getData2());

        ShortMessage1 message2 = new ShortMessage1(null);
        assertEquals(0, message2.getData1());
        assertEquals(0, message2.getData2());

        message.setMessage(249);
        assertEquals(0, message.getData1());
        assertEquals(0, message.getData2());

        message.setMessage(250, 14, 62);
        assertEquals(0, message.getData1());
        assertEquals(0, message.getData2());

        message.setMessage(234, 15, 14, 62);
        assertEquals(14, message.getData1());
        assertEquals(62, message.getData2());

        message.setMessage(198, 15, 14, 62);
        assertEquals(14, message.getData1());
        assertEquals(0, message.getData2());
        
        ShortMessage1 message15 = new ShortMessage1(new byte[] {-16, 16, 35});
        assertEquals(16, message15.getData1());
        assertEquals(35, message15.getData2());
    }

    /**
     * test method getDataLength() of class ShortMessage
     *
     */
    public void test_getDataLength() throws Exception {
        ShortMessage1 message = new ShortMessage1();

        /*
         * I have some question about this method. I didn't understand how
         * should to work this method, but some results I get by
         * experimental method.
         * From 0 up to 127, from 256 up to 383 and so on this method throw
         * out exception, i.e. after 256 begin cycle with some small 
         * differences in the first lap, from 0 up to 255. From the second
         * lap and so on this method, getDataLength(int), throw out 
         * exception with value of status from 496 up to 511, from 752 up to 
         * 767 and so on, i.e. on the last 16 number of 256-lap.
         * And now differences in the first lap. This method don't throw out 
         * exception with value of status from 240 up to 255. It has next 
         * behavior:
         * - value of status equals 240 -- throw out exception;
         * - 241 -- return 1; 
         * - 242 -- return 2;
         * - 243 -- return 1;
         * - from 244 up to 245 -- throw out exception;
         * - from 246 up to 255 -- return 0;
         */
        try {
            message.getDataLength1(-1);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(56);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(127);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(240);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(244);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(245);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(256);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        try {
            message.getDataLength1(519);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}

        assertEquals(2, message.getDataLength1(128));
        assertEquals(1, message.getDataLength1(200));
        assertEquals(2, message.getDataLength1(230));
        assertEquals(1, message.getDataLength1(241));
        assertEquals(2, message.getDataLength1(242));
        assertEquals(1, message.getDataLength1(243));
        assertEquals(0, message.getDataLength1(250));
        assertEquals(2, message.getDataLength1(647));
    }

    /**
     * tests method clone() of class ShortMessage
     *
     */
    public void test_clone1() {
        ShortMessage message = new ShortMessage();
        assertTrue(message.clone() != message);
        assertEquals(message.clone().getClass(), message.getClass());
        ShortMessage tmessage;
        tmessage = (ShortMessage) message.clone();
        assertEquals(message.getLength(), tmessage.getLength());
        assertEquals(message.getMessage().length, tmessage.getMessage().length);
        assertEquals(message.getData1(), tmessage.getData1());
        assertEquals(message.getData2(), tmessage.getData2());
        assertEquals(message.getChannel(), tmessage.getChannel());
        assertEquals(message.getCommand(), tmessage.getCommand());
        assertEquals(message.getStatus(), tmessage.getStatus());
        if (message.getMessage().length != 0) {
            for (int i = 0; i < message.getMessage().length; i++) {
                assertEquals(message.getMessage()[i], tmessage.getMessage()[i]);
            }
        }
    }
    
    public void test_clone2() {
        byte[] bt = new byte[] {1, 2, 3, 4, 5};
        ShortMessage1 message = new ShortMessage1(bt);
        assertTrue(message.clone() != message);
        
        ShortMessage tmessage;
        tmessage = (ShortMessage) message.clone();
        assertEquals(message.getLength(), tmessage.getLength());
        assertEquals(message.getMessage().length, tmessage.getMessage().length);
        assertEquals(message.getData1(), tmessage.getData1());
        assertEquals(message.getData2(), tmessage.getData2());
        assertEquals(message.getChannel(), tmessage.getChannel());
        assertEquals(message.getCommand(), tmessage.getCommand());
        assertEquals(message.getStatus(), tmessage.getStatus());
        if (message.getMessage().length != 0) {
            for (int i = 0; i < message.getMessage().length; i++) {
                assertEquals(message.getMessage()[i], tmessage.getMessage()[i]);
            }
        }
        bt[0] = 10;
        bt[1] = 20;
        bt[2] = 30;
        bt[3] = 40;
        bt[4] = 50;
        /*
         * 'real' ShortMessage change, but 'clone' not
         */
        //'real'
        assertEquals(10, message.getChannel());
        assertEquals(0, message.getCommand());
        assertEquals(20, message.getData1());
        assertEquals(30, message.getData2());
        assertEquals(10, message.getStatus());
        assertEquals(10, message.getMessage()[0]);
        assertEquals(20, message.getMessage()[1]);
        assertEquals(30, message.getMessage()[2]);
        assertEquals(40, message.getMessage()[3]);
        assertEquals(50, message.getMessage()[4]);
        //'clone'
        assertEquals(1, tmessage.getChannel());
        assertEquals(0, tmessage.getCommand());
        assertEquals(2, tmessage.getData1());
        assertEquals(3, tmessage.getData2());
        assertEquals(1, tmessage.getStatus());
        assertEquals(1, tmessage.getMessage()[0]);
        assertEquals(2, tmessage.getMessage()[1]);
        assertEquals(3, tmessage.getMessage()[2]);
        assertEquals(4, tmessage.getMessage()[3]);
        assertEquals(5, tmessage.getMessage()[4]);
    }
    
    public void test_clone3() throws Exception {
        ShortMessage message = new ShortMessage();
        message.setMessage(150, 14, 45, 60);
        assertTrue(message.clone() != message);
        assertEquals(message.clone().getClass(), message.getClass());
        ShortMessage tmessage;
        tmessage = (ShortMessage) message.clone();
        assertEquals(message.getLength(), tmessage.getLength());
        assertEquals(message.getMessage().length, tmessage.getMessage().length);
        assertEquals(message.getData1(), tmessage.getData1());
        assertEquals(message.getData2(), tmessage.getData2());
        assertEquals(message.getChannel(), tmessage.getChannel());
        assertEquals(message.getCommand(), tmessage.getCommand());
        assertEquals(message.getStatus(), tmessage.getStatus());
        if (message.getMessage().length != 0) {
            for (int i = 0; i < message.getMessage().length; i++) {
                assertEquals(message.getMessage()[i], tmessage.getMessage()[i]);
            }
        }
    }

    /**
     * Subsidiary class in order to use constructor
     * and method getDataLength( int ) of class Instrument, 
     * because its declared as protected
     */
    static class ShortMessage1 extends ShortMessage {

        ShortMessage1() {
            super();
        }

        ShortMessage1(byte[] data) {
            super(data);
        }

        public int getDataLength1(int status) throws InvalidMidiDataException {
            return super.getDataLength(status);
        }
    }
}
