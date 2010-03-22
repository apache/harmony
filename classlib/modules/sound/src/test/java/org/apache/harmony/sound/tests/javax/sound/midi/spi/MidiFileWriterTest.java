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

package org.apache.harmony.sound.tests.javax.sound.midi.spi;

import java.io.File;
import java.io.OutputStream;

import javax.sound.midi.Sequence;
import javax.sound.midi.spi.MidiFileWriter;

import junit.framework.TestCase;

public class MidiFileWriterTest extends TestCase {

    static int[] recorded;

    /**
     * @tests javax.sound.midi.spi.MidiFileWriter#isFileTypeSupported(int)
     */
    public void testIsFileTypeSupported1() {
        MidiFileWriter writer = new MidiFileWriter() {

            @Override
            public int[] getMidiFileTypes() {
                return recorded;
            }

            @Override
            public int[] getMidiFileTypes(Sequence sequence) {
                fail("what are doing here?");
                return null;
            }

            @Override
            public int write(Sequence in, int fileType, File out) {
                fail("what are doing here?");
                return 0;
            }

            @Override
            public int write(Sequence in, int fileType, OutputStream out) {
                fail("what are doing here?");
                return 0;
            }
        };

        recorded = new int[1];
        recorded[0] = 3;

        assertTrue(writer.isFileTypeSupported(3));
        assertFalse(writer.isFileTypeSupported(5));

        recorded = null;
        try {
            writer.isFileTypeSupported(10);
            fail("NPE expected");
        } catch (NullPointerException e) {}
    }

    /**
     * @tests javax.sound.midi.spi.MidiFileWriter#isFileTypeSupported(int,Sequence)
     */
    public void testIsFileTypeSupported2() {
        MidiFileWriter writer = new MidiFileWriter() {

            @Override
            public int[] getMidiFileTypes() {
                fail("what are doing here?");
                return null;
            }

            @Override
            public int[] getMidiFileTypes(Sequence sequence) {
                return recorded;
            }

            @Override
            public int write(Sequence in, int fileType, File out) {
                fail("what are doing here?");
                return 0;
            }

            @Override
            public int write(Sequence in, int fileType, OutputStream out) {
                fail("what are doing here?");
                return 0;
            }
        };

        recorded = new int[] {3};

        assertTrue(writer.isFileTypeSupported(3, null));
        assertFalse(writer.isFileTypeSupported(5, null));

        recorded = null;
        try {
            writer.isFileTypeSupported(10, null);
            fail("NPE expected");
        } catch (NullPointerException e) {}
    }
}
