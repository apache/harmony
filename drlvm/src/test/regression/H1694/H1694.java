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

package org.apache.harmony.drlvm.tests.regression.h1694;

import junit.framework.TestCase;

public class H1694 extends TestCase {

    public void test() {
        Object arrayOfObjects[] = new Object[10000]; // array of objects

        // padding memory
        System.out.println("Padding memory...");           
        int numObjects=0;
        
        try {
            while (true) {
                arrayOfObjects[numObjects] = new StressTest01Object1(); // padding memory by big objects
                numObjects++;
            }
        }
        catch (OutOfMemoryError oome) {
        }

        System.out.println("Test passed");
    }
}
 
/* big padding object */
class StressTest01Object1 {
    int testArray[][][] = new int[100][100][100];
}
