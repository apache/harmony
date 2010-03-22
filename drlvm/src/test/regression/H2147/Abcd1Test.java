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

public class Abcd1Test extends TestCase {

    private final int limit = 1000;

    public void test() {
        int arr[] = new int[limit];
        try {
            for(int k=1; k<limit; ) {
                System.out.println("k=" + k + ": arr[" + (k-1) + "] will be called");
                k=arr[k-1];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("TEST PASSED: ArrayIndexOutOfBoundsException was thrown");
            return;
        }
        fail("TEST FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
    }
}
