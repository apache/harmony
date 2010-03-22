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

package org.apache.harmony.drlvm.tests.regression.h1788;

import junit.framework.TestCase;


public class abcdTest extends TestCase {

    public void test2141() {
        System.out.println("Start test2141 ...");
        int i = 0;
        int arr[] = new int[300000];
        try {
            for(i=0; i<100000; i++) {
                while(i<3) {
                    arr[i-1] = 1;
                    i++;
                }
            }
            fail("TEST 1 FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
        } 
        catch (ArrayIndexOutOfBoundsException ae) {
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
            fail("TEST 2 FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
        } 
        catch (ArrayIndexOutOfBoundsException ae) {
            System.out.println("TEST 2 PASSED");
        }
    }

    public void test2144() {
        final int limit = 10000;
        System.out.println("Start test2144 ...");
        int arr[] = new int[limit];
        int j = 1;
        try {
            for(int k = 2; k < limit; k = 1 + k + k * j) {
                if (k < 0) System.out.println("---Overflow---");
                System.out.println("k=" + k + ": arr[" + (k - 2) + "] will be called");
                arr[k] = arr[k - 2];
                j = k * k;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("TEST PASSED ArrayIndexOutOfBoundsException was thrown");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            fail("TEST FAILED: unexpected exception was thrown");
        }
        fail("TEST FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
    }

    public void test2147_1() {
        final int limit = 1000;
        System.out.println("Start test2147_1 ...");
        int arr[] = new int[limit];
        try {
            for(int k = 1; k < limit; ) {
                System.out.println("k=" + k + ": arr[" + (k - 1) + "] will be called");
                k = arr[k - 1];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("TEST PASSED: ArrayIndexOutOfBoundsException was thrown");
            return;
        }
        fail("TEST FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
    }

    static int num = 0;
    public void test2147_2() {
        System.out.println("Start test2147_2: LowBoundCheck Test ...");
        try {
            int[] arr = new int[5];
            int limit = arr.length-1;
            for (int j = limit; j > 0; j--) {
                System.out.println("Call arr[" + (j - 3) + "]");
                num = arr[j - 3];
            }
            fail("TEST FAILED: ArrayIndexOutOfBoundsException wasn't thrown");
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.out.println("TEST PASSED");
        }
    }


}
