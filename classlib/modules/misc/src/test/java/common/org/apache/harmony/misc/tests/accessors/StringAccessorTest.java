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


package org.apache.harmony.misc.tests.accessors;

import junit.framework.TestCase;
import org.apache.harmony.misc.accessors.StringAccessor;
import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.Int8Pointer;

public class StringAccessorTest extends TestCase {
    public void test_getChars() throws Exception {
        StringAccessor acc = AccessorFactory.getStringAccessor();

        String str = new String("Hello world!");

        long ptr1 = acc.getChars(str);
        Int8Pointer iptr1 = NativeBridge.getInstance().createInt8Pointer(ptr1);
        assertEquals(0, iptr1.get(str.length()*2));
        assertEquals(0, iptr1.get(str.length()*2+1));

        Int8Pointer iptr2 = NativeBridge.getInstance().createInt8Pointer(12, false);
        iptr2.fill((byte)0xFF, 12);
        long ptr2 = iptr2.lock();
        long ptr3 = acc.getChars(ptr2, 12, str, 6, 5);
        assertEquals(ptr2, ptr3);
        iptr2.unlock();

        assertEquals(0, acc.compareString(new String("world"), iptr2.lock(), 5));
        iptr2.unlock();

        assertEquals(0, iptr2.get(10));
        assertEquals(0, iptr2.get(11));

        iptr1.free();
        iptr2.free();
    }
}
