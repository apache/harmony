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

import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import junit.framework.TestCase;

public class DataLineInfoTest extends TestCase {

    public void testDataLineInfo() {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 8,
                3, 4, 5f, true);
        AudioFormat format1 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                1f, 8, 3, 4, 5f, true);

        DataLine.Info info = new DataLine.Info("aaaa".getClass(), format, 5);
        assertEquals(1, info.getFormats().length);
        assertEquals(format, info.getFormats()[0]);
        assertTrue(info.isFormatSupported(format));
        assertFalse(info.isFormatSupported(format1));
        assertEquals(5, info.getMinBufferSize());
        assertEquals(5, info.getMaxBufferSize());

        info = new DataLine.Info("aaaa".getClass(), format);
        assertEquals(1, info.getFormats().length);
        assertEquals(format, info.getFormats()[0]);
        assertTrue(info.isFormatSupported(format));
        assertFalse(info.isFormatSupported(format1));
        assertEquals(AudioSystem.NOT_SPECIFIED, info.getMinBufferSize());
        assertEquals(AudioSystem.NOT_SPECIFIED, info.getMaxBufferSize());
        assertEquals(
                "class java.lang.String supporting format ALAW 1.0 Hz, 8 bit, 3 channels, 4 bytes/frame, 5.0 frames/second, ",
                info.toString());

        AudioFormat[] formats = new AudioFormat[] { format, format1 };
        info = new DataLine.Info(new Object().getClass(), formats, 1, 10);
        assertEquals(2, info.getFormats().length);
        assertTrue(Arrays.equals(formats, info.getFormats()));
        assertTrue(info.isFormatSupported(format));
        assertTrue(info.isFormatSupported(format1));
        assertEquals(1, info.getMinBufferSize());
        assertEquals(10, info.getMaxBufferSize());
        assertEquals(
                "class java.lang.Object supporting 2 audio formats, and buffers of 1 to 10 bytes",
                info.toString());

    }

    public void testMatches() {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 8,
                3, 4, 5f, true);
        DataLine.Info info1 = new DataLine.Info("aaaa".getClass(), format);
        DataLine.Info info2 = new DataLine.Info(new Object().getClass(), format);

        assertTrue(info1.matches(info1));
        assertFalse(info1.matches(info2));
        assertTrue(info2.matches(info1));

        info2 = new DataLine.Info("aaaa".getClass(), format, 5);
        assertTrue(info1.matches(info2));
        assertTrue(info2.matches(info1));

        info1 = new DataLine.Info("aaaa".getClass(), format, 4);
        assertFalse(info1.matches(info2));
        assertFalse(info2.matches(info1));

        info2 = new DataLine.Info("aaaa".getClass(),
                new AudioFormat[] { format }, 3, 5);
        assertTrue(info1.matches(info2));
        assertFalse(info2.matches(info1));

        AudioFormat format1 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                1f, 8, 3, 4, 5f, true);
        info1 = new DataLine.Info("aaaa".getClass(), new AudioFormat[] {
                format, format1 }, 3, 5);
        assertFalse(info1.matches(info2));
        assertTrue(info2.matches(info1));

    }

}
