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

public class RuntimeAdditionalTest16 extends TestCase {
    /**
     * creation, getting streams, waiting the finish for lots of echo-process
     */
    public void test_16() {
        System.out.println("==test_16===");
        String cmnd = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            cmnd = RuntimeAdditionalTest0.cm + " /C \"echo 777\"";
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            cmnd = "sh -c \"echo 777\"";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }
        int ia = 0;
        //while (ia < (Integer.MAX_VALUE - (Integer.MAX_VALUE / 300) * 2)) {
        //    ia += Integer.MAX_VALUE / 300;
        while (ia < 70/*100*/) {
            ia += 1;
            try {
                Process pi3 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                java.io.InputStream is = pi3.getInputStream();
                while (true) {
                    try {
                        Thread.sleep(50);
                        pi3.exitValue();
                        break;
                    } catch (IllegalThreadStateException e) {
                        continue;
                    }
                }
                is.available();
            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_16): unexpected exception.");
            }
        }
    }
}