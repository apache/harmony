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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;

import junit.framework.TestCase;

public class AudioInputStreamTest extends TestCase {

    public void testAudioInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[1001]);
        AudioFormat format = new AudioFormat(1f, 16, 2, true, false);

        AudioInputStream ais = new AudioInputStream(is, format, 10);

        assertEquals(format, ais.getFormat());
        assertEquals(10, ais.getFrameLength());
        assertEquals(40, ais.available());
        assertEquals(8, ais.read(new byte[10]));
        assertEquals(32, ais.available());
        assertTrue(ais.markSupported());
        ais.mark(1000);
        assertEquals(8, ais.read(new byte[10]));
        assertEquals(24, ais.available());
        assertEquals(0, ais.skip(2));
        assertEquals(8, ais.skip(10));
        ais.reset();
        assertEquals(32, ais.available());
        assertEquals(0, ais.read(new byte[10], -1, 2));
        assertEquals(8, ais.read(new byte[10], 0, 11));
        try {
            ais.read();
            fail("No expected IOException");
        } catch (IOException expected) {
        }
        ais.close(); // no exception expected

        is = new ByteArrayInputStream(new byte[1001]);
        ais = new AudioInputStream(is, format, 500);

        assertEquals(format, ais.getFormat());
        assertEquals(500, ais.getFrameLength());
        assertEquals(1001, ais.available());
        assertEquals(8, ais.read(new byte[10]));
        assertEquals(993, ais.available());
        ais.mark(1000);
        assertEquals(8, ais.read(new byte[10]));
        assertEquals(985, ais.available());
        assertEquals(0, ais.skip(2));
        assertEquals(8, ais.skip(10));
        ais.reset();
        assertEquals(993, ais.available());
        assertEquals(0, ais.read(new byte[10], -1, 2));
        assertEquals(8, ais.read(new byte[10], 0, 11));
        try {
            ais.read();
            fail("No expected IOException");
        } catch (IOException expected) {
        }
        ais.close(); // no exception expected
    }

}
