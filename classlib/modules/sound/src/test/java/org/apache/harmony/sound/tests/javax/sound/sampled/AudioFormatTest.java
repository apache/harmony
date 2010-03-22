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

import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import junit.framework.TestCase;

public class AudioFormatTest extends TestCase {

    public void testAudioFormat() {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2,
                3, 4, 5f, true);

        assertEquals(AudioFormat.Encoding.ALAW, format.getEncoding());
        assertEquals(1f, format.getSampleRate());
        assertEquals(2, format.getSampleSizeInBits());
        assertEquals(3, format.getChannels());
        assertEquals(4, format.getFrameSize());
        assertEquals(5f, format.getFrameRate());
        assertTrue(format.isBigEndian());
        assertTrue(format.properties().isEmpty());

        HashMap<String, Object> prop = new HashMap<String, Object>();
        prop.put("bitrate", Integer.valueOf(100));
        prop.put("vbr", Boolean.TRUE);
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 5f, 4, 3, 2,
                1f, false, prop);
        assertEquals(5f, format.getSampleRate());
        assertEquals(4, format.getSampleSizeInBits());
        assertEquals(3, format.getChannels());
        assertEquals(2, format.getFrameSize());
        assertEquals(1f, format.getFrameRate());
        assertFalse(format.isBigEndian());
        assertEquals(2, format.properties().size());
        assertEquals(Integer.valueOf(100), format.properties().get("bitrate"));
        assertEquals(Boolean.TRUE, format.properties().get("vbr"));
        try {
            format.properties().put("aa", 1);
            fail("No expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }

        format = new AudioFormat(1f, 10, 2, true, false);
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        assertEquals(1f, format.getSampleRate());
        assertEquals(10, format.getSampleSizeInBits());
        assertEquals(2, format.getChannels());
        assertEquals(4, format.getFrameSize());
        assertEquals(1f, format.getFrameRate());
        assertFalse(format.isBigEndian());
        assertTrue(format.properties().isEmpty());

    }

    public void testMatches() {
        AudioFormat format;
        AudioFormat format1;

        format = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3, 4, 5f,
                true);
        assertTrue(format.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3, 4, 5f,
                true);
        assertTrue(format.matches(format1));

        format1 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 1f, 2, 3, 4,
                5f, true);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 2f, 2, 3, 4, 5f,
                true);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 3, 3, 4, 5f,
                true);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 4, 4, 5f,
                true);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3, 5, 5f,
                true);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3, 4, 6f,
                true);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3, 4, 5f,
                false);
        assertTrue(format.matches(format1));
        assertTrue(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW,
                AudioSystem.NOT_SPECIFIED, 2, 3, 4, 5f, true);
        assertTrue(format.matches(format1));
        assertFalse(format1.matches(format));

        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3, 4,
                AudioSystem.NOT_SPECIFIED, true);
        assertTrue(format.matches(format1));
        assertFalse(format1.matches(format));

        format = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 10, 3, 4, 5f,
                true);
        format1 = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 10, 3, 4, 5f,
                false);
        assertFalse(format.matches(format1));
        assertFalse(format1.matches(format));
    }

}