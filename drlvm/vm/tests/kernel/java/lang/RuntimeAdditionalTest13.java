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
 * @author Serguei S.Zapreyev
 */

package java.lang;

import junit.framework.TestCase;

/*
 * Created on March 29, 2006
 *
 * This RuntimeAdditionalTest class is used to test the Core API Runtime class
 * 
 */

/**
 * ###############################################################################
 * ###############################################################################
 * TODO: 1.
 * ###############################################################################
 * ###############################################################################
 */

public class RuntimeAdditionalTest13 extends TestCase {
    /**
     * creation of process for incorrect command should lead to IOException then
     * get Streams of such process should lead to NPE
     */
    public void test_13() {
        System.out.println("==test_13===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_13): unknown operating system.");
        }
        Process pi3 = null;
        try {
            String cmnd = "XjavaX";
            pi3 = Runtime.getRuntime().exec(cmnd);
        } catch (Exception eeee) {
            try {
                pi3.getOutputStream();
                pi3.getErrorStream();
                pi3.getInputStream();
            } catch (NullPointerException e) {
                return;
            }
        }
        fail("FAILED: test_13");
    }
}