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
import javax.sound.midi.SysexMessage;

import junit.framework.TestCase;

public class SysexMessageTest extends TestCase {
    public void test_constants() {
        assertEquals(247, SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE);
        assertEquals(240, SysexMessage.SYSTEM_EXCLUSIVE);
    }
    
    /**
     * test constructors
     *
     */
    public void test_constructors() {
        SysexMessage1 message = new SysexMessage1();
        assertEquals(2, message.getLength());
        assertEquals(240, message.getStatus());
        assertEquals(2, message.getMessage().length);
        assertEquals(-16, message.getMessage()[0]);
        assertEquals(-9, message.getMessage()[1]);
        assertEquals(1, message.getData().length);
        assertEquals(-9, message.getData()[0]);
        
        byte[] bt = new byte[] {16, 28, -43, 18, 54};
        SysexMessage1 message1 = new SysexMessage1(bt);
        assertEquals(5, message1.getLength());
        assertEquals(16, message1.getStatus());
        assertEquals(5, message1.getMessage().length);
        assertEquals(16, message1.getMessage()[0]);
        assertEquals(28, message1.getMessage()[1]);
        assertEquals(-43, message1.getMessage()[2]);
        assertEquals(18, message1.getMessage()[3]);
        assertEquals(54, message1.getMessage()[4]);
        assertEquals(4, message1.getData().length);
        assertEquals(28, message1.getData()[0]);
        assertEquals(-43, message1.getData()[1]);
        assertEquals(18, message1.getData()[2]);
        assertEquals(54, message1.getData()[3]);
        bt[0] = 18;
        bt[1] = 24;
        bt[2] = 89;
        bt[3] = -6;
        bt[4] = -90;
        /*
         * values change
         */
        assertEquals(5, message1.getLength());
        assertEquals(18, message1.getStatus());
        assertEquals(5, message1.getMessage().length);
        assertEquals(18, message1.getMessage()[0]);
        assertEquals(24, message1.getMessage()[1]);
        assertEquals(89, message1.getMessage()[2]);
        assertEquals(-6, message1.getMessage()[3]);
        assertEquals(-90, message1.getMessage()[4]);
        assertEquals(4, message1.getData().length);
        assertEquals(24, message1.getData()[0]);
        assertEquals(89, message1.getData()[1]);
        assertEquals(-6, message1.getData()[2]);
        assertEquals(-90, message1.getData()[3]);
        byte[] nb = message1.getData();
        nb[0] = 34;
        nb[1] = 8;
        nb[2] = -4;
        nb[3] = 3;
        /*
         * values don't change
         */
        assertEquals(24, message1.getData()[0]);
        assertEquals(89, message1.getData()[1]);
        assertEquals(-6, message1.getData()[2]);
        assertEquals(-90, message1.getData()[3]);
        
        SysexMessage1 message2 = new SysexMessage1(new byte[0]);
        assertEquals(0, message2.getLength());
        assertEquals(0, message2.getStatus());
        assertEquals(0, message2.getMessage().length);
        try {
            message2.getData();
            fail("NegativeArraySizeException expected");
        } catch (NegativeArraySizeException e) {}
        
        SysexMessage1 message3 = new SysexMessage1(null);
        assertEquals(0, message3.getLength());
        assertEquals(0, message3.getStatus());
        try {
            message3.getMessage();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        try {
            message3.getData();
            fail("NegativeArraySizeException expected");
        } catch (NegativeArraySizeException e) {}
    }
    
    /**
     * test method setMessage(byte[], int)
     * 
     */
    public void test_setMessage1() throws Exception {
        SysexMessage message = new SysexMessage();
        
        byte[] bt = new byte[] {-9, 18, -6, -9, 3};
        message.setMessage(bt, 5);
        assertEquals(5, message.getLength());
        assertEquals(247, message.getStatus());
        assertEquals(5, message.getMessage().length);
        assertEquals(-9, message.getMessage()[0]);
        assertEquals(18, message.getMessage()[1]);
        assertEquals(-6, message.getMessage()[2]);
        assertEquals(-9, message.getMessage()[3]);
        assertEquals(3, message.getMessage()[4]);
        assertEquals(4, message.getData().length);
        assertEquals(18, message.getData()[0]);
        assertEquals(-6, message.getData()[1]);
        assertEquals(-9, message.getData()[2]);
        assertEquals(3, message.getData()[3]);
        bt[0] = 10;
        bt[1] = 20;
        bt[2] = 30;
        bt[3] = 40;
        bt[4] = 50;
        /*
         * values don't change
         */
        assertEquals(-9, message.getMessage()[0]);
        assertEquals(18, message.getMessage()[1]);
        assertEquals(-6, message.getMessage()[2]);
        assertEquals(-9, message.getMessage()[3]);
        assertEquals(3, message.getMessage()[4]);
        assertEquals(18, message.getData()[0]);
        assertEquals(-6, message.getData()[1]);
        assertEquals(-9, message.getData()[2]);
        assertEquals(3, message.getData()[3]);
        
        try {
            message.setMessage(new byte[] {34}, 1);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        /*
         * it's all right!
         */
        message.setMessage(new byte[] {-9}, 1);
        message.setMessage(new byte[] {-16}, 1);
        
        message.setMessage(new byte[] {-16, 34, 56}, 1);
        assertEquals(1, message.getLength());
        assertEquals(1, message.getMessage().length);
        assertEquals(0, message.getData().length);
        
        try {
            message.setMessage(new byte[] {-1, 34, 56}, 4);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        try {
            message.setMessage(new byte[] {-9, 34, 56}, 4);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {}
        try {
            message.setMessage(new byte[] {-9, 34, 56}, -1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {}
        
        try {
            message.setMessage(new byte[0], 0);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {}
        
        try {
            message.setMessage(null, 0);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        
        message.setMessage(new byte[] {-9, 3, 4}, 0);
        assertEquals(0, message.getLength());
        assertEquals(0, message.getStatus());
        assertEquals(0, message.getMessage().length);
        try {
            message.getData();
            fail("NegativeArraySizeException expected");
        } catch (NegativeArraySizeException e) {}
    }
    
    /**
     * test method setMessage(int, byte[], int)
     *
     */
    public void test_setMessage2() throws Exception {
        SysexMessage message = new SysexMessage();
        try {
            message.setMessage(34, new byte[] {1, 2}, 2);
            fail("InvalidMidiDataException expected");
        } catch (InvalidMidiDataException e) {}
        
        /*
         * it's all right
         */
        message.setMessage(240, new byte[] {1, 2}, 2);
        message.setMessage(247, new byte[] {34, -42}, 2);
        
        assertEquals(3, message.getLength());
        assertEquals(247, message.getStatus());
        assertEquals(3, message.getMessage().length);
        assertEquals(-9, message.getMessage()[0]);
        assertEquals(34, message.getMessage()[1]);
        assertEquals(-42, message.getMessage()[2]);
        assertEquals(2, message.getData().length);
        assertEquals(34, message.getData()[0]);
        assertEquals(-42, message.getData()[1]);
        
        message.setMessage(240, new byte[] {12, 47, -9}, 1);
        assertEquals(2, message.getLength());
        assertEquals(240, message.getStatus());
        assertEquals(2, message.getMessage().length);
        assertEquals(-16, message.getMessage()[0]);
        assertEquals(12, message.getMessage()[1]);
        assertEquals(1, message.getData().length);
        assertEquals(12, message.getData()[0]);
        
        message.setMessage(240, new byte[] {12, 4, 9}, 0);
        assertEquals(1, message.getLength());
        assertEquals(240, message.getStatus());
        assertEquals(1, message.getMessage().length);
        assertEquals(-16, message.getMessage()[0]);
        assertEquals(0, message.getData().length);
        
        try {
            message.setMessage(247, new byte[] {1, 2, 3}, 4);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {}
        
        try {
            message.setMessage(247, new byte[] {1, 2, 3}, -1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {}
        
        message.setMessage(247, new byte[0], 0);
        assertEquals(1, message.getLength());
        assertEquals(247, message.getStatus());
        assertEquals(1, message.getMessage().length);
        assertEquals(-9, message.getMessage()[0]);
        assertEquals(0, message.getData().length);
        
        try {
            message.setMessage(247, null, 0);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
    
    /**
     * Test method clone() of class SysexMessage.
     */
    public void test_clone1() {
        SysexMessage message = new SysexMessage();
        assertTrue(message.clone() != message);
        assertEquals(message.clone().getClass(), message.getClass());
        SysexMessage tmessage;
        tmessage = (SysexMessage) message.clone();
        assertEquals(message.getLength(), tmessage.getLength());
        assertEquals(message.getMessage().length, tmessage.getMessage().length);
        assertEquals(message.getData().length, tmessage.getData().length);
        if (message.getData().length != 0) {
            for (int i = 0; i < message.getData().length; i++) {
                assertEquals(message.getData()[i], tmessage.getData()[i]);
            }
        }
        if (message.getMessage().length != 0) {
            for (int i = 0; i < message.getMessage().length; i++) {
                assertEquals(message.getMessage()[i], tmessage.getMessage()[i]);
            }
        }
    }

    /**
     * Test method clone() of class SysexMessage.
     */
    public void test_clone2() throws Exception {
        SysexMessage message = new SysexMessage();

        message.setMessage(240, new byte[] { 23, 45, -90 }, 2);

        assertTrue(message.clone() != message);
        assertEquals(message.clone().getClass(), message.getClass());
        SysexMessage tmessage;
        tmessage = (SysexMessage) message.clone();
        assertEquals(message.getLength(), tmessage.getLength());
        assertEquals(message.getMessage().length, tmessage.getMessage().length);
        assertEquals(message.getData().length, tmessage.getData().length);
        if (message.getData().length != 0) {
            for (int i = 0; i < message.getData().length; i++) {
                assertEquals(message.getData()[i], tmessage.getData()[i]);
            }
        }
        if (message.getMessage().length != 0) {
            for (int i = 0; i < message.getMessage().length; i++) {
                assertEquals(message.getMessage()[i], tmessage.getMessage()[i]);
            }
        }
    }
    
    public void test_clone3() throws Exception {
        byte[] bt = new byte[] {1, 2, 3, 4};
        SysexMessage1 message = new SysexMessage1(bt);
        assertTrue(message.clone() != message);
        
        SysexMessage tmessage;
        tmessage = (SysexMessage) message.clone();
        bt[0] = 34;
        bt[1] = 15;
        bt[2] = 89;
        bt[3] = 1;
        assertEquals(message.getLength(), tmessage.getLength());
        assertEquals(message.getMessage().length, tmessage.getMessage().length);
        assertEquals(message.getData().length, tmessage.getData().length);
        /*
         * 'real' SysexMessage change, but 'clone' not
         */
        //'real'
        assertEquals(15, message.getData()[0]);
        assertEquals(89, message.getData()[1]);
        assertEquals(1, message.getData()[2]);
        assertEquals(34, message.getMessage()[0]);
        assertEquals(15, message.getMessage()[1]);
        assertEquals(89, message.getMessage()[2]);
        assertEquals(1, message.getMessage()[3]);
        //'clone'
        assertEquals(2, tmessage.getData()[0]);
        assertEquals(3, tmessage.getData()[1]);
        assertEquals(4, tmessage.getData()[2]);
        assertEquals(1, tmessage.getMessage()[0]);
        assertEquals(2, tmessage.getMessage()[1]);
        assertEquals(3, tmessage.getMessage()[2]);
        assertEquals(4, tmessage.getMessage()[3]);
    }
    
    static class SysexMessage1 extends SysexMessage {
        SysexMessage1() {
            super();
        }
        
        SysexMessage1(byte[] data) {
            super(data);
        }
    }
}
