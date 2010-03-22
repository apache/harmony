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

import java.util.*;

/**
 * Microbenchmark for float & integer computations.
 */
public class test_f2i_speed {
    public static void main(String[] args) {
        //
        // warm-up - force the method to be recompiled
        //
	    System.out.println("Warming up ...");
        for (int i=0; i<20000; i++) {
            test(false);
        }
        //
        // The real measure
        //
	System.out.println("Measuring ...");
        long startTime = System.currentTimeMillis();
        test(true);
        long endTime = System.currentTimeMillis();
        //
        //
        long spentTime = endTime - startTime;
	    System.out.println("... done.");
        System.out.println("The test took: "+spentTime+"ms");
    }

    static void test(boolean do_test) {
        int problem_size = do_test ? 10000000 : 5;
        int array_size = 300000;
        int[] array = new int[array_size];
        Random rndValue = new Random(0);

        for (int i=0; i<problem_size; i++) {
            int index = i % array.length;
            float value = rndValue.nextFloat();
            array[index] = (int)value;
        }
    }
}