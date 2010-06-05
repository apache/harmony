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


package org.apache.harmony.awt.tests.nativebridge;

import junit.framework.TestCase;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.ByteBase;

public class PointerPointerTest extends TestCase {
    private PointerPointer p;
    private PointerPointer p1;

    
    // Regression tests for HARMONY-2547    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        p = NativeBridge.getInstance().createPointerPointer(2, false);
        p.setAddress(1, 0xFFFFFFFFL);
        p1 = NativeBridge.getInstance().createPointerPointer(2, false);
        p1.setAddress(0, 0xFFFFFFFFL);
    }

    public void test_get() {
        assertNull("*p != 0", p.get(0));
        assertNull("*(p+1) != 0", p1.get(1));
    }

    public void test_getAddress() {
        assertEquals("*p != 0", 0, p.getAddress(0));
        assertEquals("*(p+1) != 0", 0, p1.getAddress(1));
    }

    public void test_set() {
        byte dst[] = new byte[8];

        p.set(1, NativeBridge.getInstance().createInt32Pointer(0x04030201L));
        p.byteBase.get(dst, 0, ByteBase.POINTER_SIZE*2);	

        for(int i = ByteBase.POINTER_SIZE; i < ByteBase.POINTER_SIZE + 4; i++) {
            assertEquals(i - ByteBase.POINTER_SIZE + 1, dst[i]);
        }
    }

    public void test_setAddress() {
        byte dst[] = new byte[8];

        p.setAddress(1, 0x04030201L);
        p.byteBase.get(dst, 0, ByteBase.POINTER_SIZE*2);

        for(int i = ByteBase.POINTER_SIZE; i < ByteBase.POINTER_SIZE + 4; i++) {
            assertEquals(i - ByteBase.POINTER_SIZE + 1, dst[i]);
        }
    }

}
