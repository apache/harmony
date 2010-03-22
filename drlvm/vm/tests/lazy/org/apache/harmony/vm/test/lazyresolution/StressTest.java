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

package org.apache.harmony.vm.test.lazyresolution;

import junit.framework.TestSuite;
import junit.framework.Test;

public class StressTest extends TestSuite {
    
    public static final String ITERATIONS = "lazy.test.iterations";
    public static final String THREADS = "lazy.test.threads";

    /**
     * @param args
     */
    public static void main(String[] args) throws Throwable {
        int count = 1;
        try {
            count = Integer.parseInt(System.getProperty(THREADS));
        } catch (Exception ignore){}
        
        if (--count > 1) {
            for (int i = 0; i < count; ++i) {
                new Thread() {
                    public void run() {
                        StressTest.runSuite();
                    }
                }.start();
            }
        }
        
        // run in main thread as well
        runSuite();
    }
    
    public static void runSuite() {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        int count = 10;
        try {
            count = Integer.parseInt(System.getProperty(ITERATIONS));
        } catch (Exception ignore){}
        
        TestSuite suite = new TestSuite();
        for (int i = 0; i < count; ++i) {
            suite.addTestSuite(AllocationTest.class); 
            suite.addTestSuite(CallsTest.class);
            suite.addTestSuite(CastsTest.class);
            suite.addTestSuite(ClinitTest.class);
            suite.addTestSuite(FieldsTest.class);
            suite.addTestSuite(InstanceOfTest.class);
            suite.addTestSuite(InterfaceCallsTest.class);
            suite.addTestSuite(OptimizerTest.class);
            suite.addTestSuite(SignatureTest.class);
        }
        return suite;
    }
}
