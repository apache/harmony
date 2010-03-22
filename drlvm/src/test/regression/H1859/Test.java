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

package org.apache.harmony.drlvm.tests.regression.h1859;

import junit.framework.TestCase;

public class Test extends TestCase {

    public void test() {
        boolean passed = false;
        try {
            foo(1, 2, 3, 4, 5, 6);
        } catch (StackOverflowError e) {
            passed = true;
        }
        assertTrue(passed);
        assertEquals(0, i);
    }

    static int i=0;

    static void foo(int i1, int i2, int i3, int i4, int i5, int i6) {
        try {
            i++;
            foo(i1, i2, i3, i4, i5, i6);
        } finally {
            i--;
        }
    }

}

