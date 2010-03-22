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

package org.apache.harmony.drlvm.tests.regression.h0000;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import junit.framework.TestCase;

public class DirectByteBufferTest extends TestCase {

    static { System.loadLibrary("DirectByteBufferTest"); }

    public static void main(String[] args) {
        new DirectByteBufferTest().testValidBuffer();
    }

    private static native String tryDirectBuffer();
    private static native String checkSameDirectStorage(Buffer b1, Buffer b2);
    
    private static void assertView(String message, ByteBuffer b1, Buffer b2, int capacityRatio) {
        assertEquals(message + ":capacity", b1.capacity()/capacityRatio, b2.capacity());
        String err = checkSameDirectStorage(b1, b2);
        assertNull(message + " : " + err, err);
    }
    
    public void testValidBuffer() {
        String err = tryDirectBuffer();
        assertNull(err, err);
    }

    /**
     * A regression test for HARMONY-3591: 
     * JNI operations fail on non-byte views of a direct buffer.
     */
    public void testBufferView() {
        ByteBuffer b = ByteBuffer.allocateDirect(100);
        assertTrue(b.isDirect());
        assertView("duplicate", b, b.duplicate(), 1);
        assertView("char view", b, b.asCharBuffer(), 2);
        assertView("short view", b, b.asShortBuffer(), 2);
        assertView("int view", b, b.asIntBuffer(), 4);
        assertView("float view", b, b.asFloatBuffer(), 4);
        assertView("double view", b, b.asDoubleBuffer(), 8);
        assertView("long view", b, b.asLongBuffer(), 8);
    }
}
