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

package org.apache.harmony.drlvm.tests.regression.h3954;

import junit.framework.TestCase;
import org.vmmagic.unboxed.*;

public class Test extends TestCase {

 
    public static void testClass() {
        Class[] arr1 = new Class[100];
        Class elem = arr1.getClass();
        arr1[1] = elem;
        Class[] arr2 = new Class[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }


    public static void testString() {
        String[] arr1 = new String[100];
        String elem = "Hello!";
        arr1[1] = elem;
        String[] arr2 = new String[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }


    public static void testObject() {
        Object[] arr1 = new Object[100];
        Object elem = new Object();
        arr1[1] = elem;
        Object[] arr2 = new Object[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }

    public static void testByte() {
        byte[] arr1 = new byte[100];
        byte elem = 1;
        arr1[1] = elem;
        byte[] arr2 = new byte[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }


    public static void testShort() {
        short[] arr1 = new short[100];
        short elem = 1;
        arr1[1] = elem;
        short[] arr2 = new short[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }

    public static void testChar() {
        char[] arr1 = new char[100];
        char elem = 1;
        arr1[1] = elem;
        char[] arr2 = new char[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }

    public static void testInt() {
        int[] arr1 = new int[100];
        int elem = 1;
        arr1[1] = elem;
        int[] arr2 = new int[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }

    public static void testLong() {
        long[] arr1 = new long[100];
        long elem = 1;
        arr1[1] = elem;
        long[] arr2 = new long[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }

    public static void testFloat() {
        float[] arr1 = new float[100];
        float elem = 1;
        arr1[1] = elem;
        float[] arr2 = new float[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }

    public static void testDouble() {
        double[] arr1 = new double[100];
        double elem = 1;
        arr1[1] = elem;
        double[] arr2 = new double[100];
        System.arraycopy(arr1, 0, arr2, 0, 100);

        assertEquals(elem, arr2[1]);
    }
}
