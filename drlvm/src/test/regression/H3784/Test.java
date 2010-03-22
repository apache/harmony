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

package org.apache.harmony.drlvm.tests.regression.h3784;

import junit.framework.TestCase;
import org.vmmagic.unboxed.*;

public class Test extends TestCase {

    static {
        System.loadLibrary("check");
    }

    public static final Address staticField = Address.fromLong(getAddress());

    public static final long staticVal = -1;
    public static final Address staticField2 = Address.fromLong(staticVal);
 
    public static void test1() {
        boolean result = check(staticField.toLong());
        assertTrue(result);
    }


    public static void test2() {
        long val = staticField.toLong();
        int ptrSize = getPointerSize();
        if (ptrSize == 4) {
            assertEquals((int)val, (int)staticVal);
        } else {
            assertTrue(ptrSize == 8);
            assertEquals(val, staticVal);
        }
    }
    
    static native long    getAddress();
    static native boolean check(long addr);
    static native int     getPointerSize();
}
