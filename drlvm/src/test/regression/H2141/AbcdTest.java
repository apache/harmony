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

package org.apache.harmony.drlvm.tests.regression.h2141;

import junit.framework.TestCase;

public class AbcdTest extends TestCase {

    public void test() {
        int i = 0;
        int arr[] = new int[300000];
        try {
            for(i=0; i<100000; i++) {
                while(i<3) {
                    arr[i-1] = 1;
                    i++;
                }
            }
            fail("ArrayIndexOutOfBoundsException wasn't thrown");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("TEST 1 PASSED");
        }

        i = 0;
        try {
            for(i=0; i<100000; i++) {
                if (i>5) {
                    arr[i-100] = 1;
                    i++;
                }
            }
            fail("ArrayIndexOutOfBoundsException wasn't thrown");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("TEST 2 PASSED");
        }
    }
}
