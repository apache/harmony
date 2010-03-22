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

/**
 * Microbenchmark for System.arraycopy.
 * Should be run with -Xms1048M -Xmx1048M options.
 */
class ArraycopyTest {

    public static int length = 100000000;

    public static void copy(int[] src, int srcPos, int[] dst, int dstPos,
            int length) {

        System.arraycopy(src, srcPos, dst, dstPos, length);
    }

    public static void main(String[] args) {
        int arrA[] = new int[length];
        int arrB[] = new int[length];

        for (int i = 0; i < length; i++) {
            arrA[i] = i;
        }
        // this part is for debugging
        /*
        * printArr(arrA,0,10, "arrA initial"); printArr(arrB,0,10, "arrB
        * initial");
        * 
        * System.arraycopy(arrA,1,arrB,3,5); printArr(arrB,0,10, "arrB
        * changed");
        * 
        * System.arraycopy(arrB,3,arrB,8,5); printArr(arrB,0,10, "arrB self
        * forward");
        * 
        * System.arraycopy(arrB,4,arrB,0,3); printArr(arrB,0,10, "arrB self
        * backward");
        */

        if (args.length == 0) {
            args = new String[]{"100"};
        }
        for (String s : args) {
            int lim = Integer.parseInt(s);

            System.out.println("");
            System.out.println("START!");

            long start = System.currentTimeMillis();

            for (int i = 0; i < lim; i++) {
                copy(arrA, i, arrB, i + 2, length - i - 2);
                // System.out.print(". ");
            }

            long end = System.currentTimeMillis();

            System.out.println("FINISHED");

            System.out.println("duration = " + (end - start) + " millis");
        }
    }

    public static void printArr(int[] arr, int pos, int count, String prefix) {

        String out = prefix + " : ";
        for (int i = pos; i < pos + count; i++) {
            out = out + arr[i] + " ";
        }
        System.out.println(out);
    }
}
