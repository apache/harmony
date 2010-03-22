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

package org.apache.harmony.sound.tests.javax.sound.sampled.spi;

import java.io.File;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.AudioFileWriter;

import junit.framework.TestCase;

public class AudioFileWriterTest extends TestCase {

    static AudioFileFormat.Type[] recorded;

    /**
     * @tests javax.sound.midi.sampled.AudioFileWriter#isFileTypeSupported(AudioFileFormat.Type)
     */
    public void testIsFileTypeSupported1() {
        AudioFileWriter writer = new AudioFileWriter() {

            @Override
            public AudioFileFormat.Type[] getAudioFileTypes() {
                return recorded;
            }

            @Override
            public AudioFileFormat.Type[] getAudioFileTypes(
                    AudioInputStream stream) {
                fail("what are doing here?");
                return null;
            }

            @Override
            public int write(AudioInputStream stream,
                    AudioFileFormat.Type fileType, File out) {
                fail("what are doing here?");
                return 0;
            }

            @Override
            public int write(AudioInputStream stream,
                    AudioFileFormat.Type fileType, OutputStream out) {
                fail("what are doing here?");
                return 0;
            }
        };

        recorded = new AudioFileFormat.Type[] {new AudioFileFormat.Type("1", "2")};

        assertTrue(writer.isFileTypeSupported(recorded[0]));

        AudioFileFormat.Type similar = new AudioFileFormat.Type("1", "2");
        assertTrue(writer.isFileTypeSupported(similar));

        try {
            writer.isFileTypeSupported(null);
            fail("NPE expected");
        } catch (NullPointerException e) {}

        recorded = null;
        try {
            writer.isFileTypeSupported(similar);
            fail("NPE expected");
        } catch (NullPointerException e) {}
    }


    /**
     * @tests javax.sound.midi.sampled.AudioFileWriter#isFileTypeSupported(AudioFileFormat.Type,AudioInputStream)
     */
    public void testIsFileTypeSupported2() {
        AudioFileWriter writer = new AudioFileWriter() {

            @Override
            public AudioFileFormat.Type[] getAudioFileTypes() {
                fail("what are doing here?");
                return null;
            }

            @Override
            public AudioFileFormat.Type[] getAudioFileTypes(
                    AudioInputStream stream) {
                return recorded;
            }

            @Override
            public int write(AudioInputStream stream,
                    AudioFileFormat.Type fileType, File out) {
                fail("what are doing here?");
                return 0;
            }

            @Override
            public int write(AudioInputStream stream,
                    AudioFileFormat.Type fileType, OutputStream out) {
                fail("what are doing here?");
                return 0;
            }
        };

        recorded = new AudioFileFormat.Type[] { new AudioFileFormat.Type("1",
                "2") };

        assertTrue(writer.isFileTypeSupported(recorded[0], null));

        AudioFileFormat.Type similar = new AudioFileFormat.Type("1", "2");
        assertTrue(writer.isFileTypeSupported(similar, null));

        try {
            writer.isFileTypeSupported(null, null);
            fail("NPE expected");
        } catch (NullPointerException e) {}

        recorded = null;
        try {
            writer.isFileTypeSupported(similar, null);
            fail("NPE expected");
        } catch (NullPointerException e) {}
    }
}
