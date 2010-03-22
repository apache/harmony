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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import junit.framework.TestCase;

public class AudioFileFormatTest extends TestCase {

    public void testAudioFileFormat() {
        AudioFormat af = new AudioFormat(AudioFormat.Encoding.ALAW, 1f, 2, 3,
                4, 5f, true);

        AudioFileFormat aff = new MyAudioFileFormat(AudioFileFormat.Type.AIFC,
                AudioSystem.NOT_SPECIFIED, af, 100);
        assertEquals(AudioFileFormat.Type.AIFC, aff.getType());
        assertEquals(AudioSystem.NOT_SPECIFIED, aff.getByteLength());
        assertEquals(af, aff.getFormat());
        assertEquals(100, aff.getFrameLength());
        assertNull(aff.properties());
        assertNull(aff.getProperty("key"));

        aff = new AudioFileFormat(AudioFileFormat.Type.WAVE, af, 10);
        assertEquals(AudioFileFormat.Type.WAVE, aff.getType());
        assertEquals(AudioSystem.NOT_SPECIFIED, aff.getByteLength());
        assertEquals(af, aff.getFormat());
        assertEquals(10, aff.getFrameLength());
        assertNull(aff.properties());
        assertNull(aff.getProperty("key"));

        HashMap<String, Object> prop = new HashMap<String, Object>();
        prop.put("duration", Long.valueOf(100));
        prop.put("title", "Title String");
        aff = new AudioFileFormat(AudioFileFormat.Type.AU, af,
                AudioSystem.NOT_SPECIFIED, prop);
        assertEquals(AudioFileFormat.Type.AU, aff.getType());
        assertEquals(AudioSystem.NOT_SPECIFIED, aff.getByteLength());
        assertEquals(af, aff.getFormat());
        assertEquals(AudioSystem.NOT_SPECIFIED, aff.getFrameLength());
        assertEquals(2, aff.properties().size());
        assertEquals(Long.valueOf(100), aff.properties().get("duration"));
        assertNull(aff.getProperty("key"));
        assertEquals("Title String", aff.getProperty("title"));
        try {
            aff.properties().put("aa", 1);
            fail("No expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }

    }

    private class MyAudioFileFormat extends AudioFileFormat {
        public MyAudioFileFormat(AudioFileFormat.Type type, int byteLength,
                AudioFormat format, int frameLength) {
            super(type, byteLength, format, frameLength);
        }
    }

}