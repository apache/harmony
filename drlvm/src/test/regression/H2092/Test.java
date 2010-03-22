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


package org.apache.harmony.drlvm.tests.regression.h2092;

import junit.framework.TestCase;

public class Test extends TestCase { 

    static int NTHREADS=10; 
    static int NSECONDS=10; 
    static volatile long val=1; 
    static boolean passed = true; 


    public void testMain() { 
        Worker wks[] = new Worker[NTHREADS]; 
        for (int i=0;i<NTHREADS;i++) { 
            wks[i]=new Worker(); 
            wks[i].start(); 
        } 
        try {
            for (int i=0;i<NTHREADS;i++) { 
                wks[i].join(); 
            } 
        } catch (Exception e) {
            passed = false;
        }
        assertTrue(passed);
/*        if (failed) { 
            System.out.println("FAILED"); 
        } else { 
            System.out.println("PASSED"); 
        } 
*/
    } 

    static class Worker extends Thread{ 
        public void run() { 
            long endTime = System.currentTimeMillis() + NSECONDS*1000L; 
            while (System.currentTimeMillis()<endTime && passed) { 
                for (int i=0;i<10000;i++) { 
                    long v = val; 
                    val = -v; 
                    if (v!=1 && v!=-1) { 
                        System.out.println("v="+v); 
                        passed = false; 
                        break; 
                    } 
                } 
            } 
        } 
    } 
}
