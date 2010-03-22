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

package org.apache.harmony.drlvm.tests.regression.h2147;

import junit.framework.TestCase;

public class LowBoundCheckTest extends TestCase {

    static int num = 0;

    public void test() {
        try {
            int[] arr = new int[5];
            int limit = arr.length-1;
            for (int j=limit; j > 0; j--) {
                System.out.println("Call arr[" + (j-3) + "]");
                num = arr[j-3];
            }
            fail("TEST FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.out.println("TEST PASSED");
        }
    }
}
