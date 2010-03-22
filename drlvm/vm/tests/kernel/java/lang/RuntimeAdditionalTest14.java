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

public class RuntimeAdditionalTest14 extends TestCase {
    /**
     * reading the streams after destroy
     */
    public void test_14() {
        System.out.println("==test_14===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_14): unknown operating system.");
        }
        try {
            String cmnd = RuntimeAdditionalTest0.javaStarter;
            Process pi3 = Runtime.getRuntime().exec(cmnd);
            pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
            pi3.destroy();
            Thread.sleep(5000);
            int ia;
            while (true) {
                while ((ia = is.available()) != 0) {
                    byte[] bbb = new byte[ia];
                    is.read(bbb);
                    //System.out.println(new String(bbb));
                }
                try {
                    pi3.exitValue();
                    while ((ia = is.available()) != 0) {
                        byte[] bbb = new byte[ia];
                        is.read(bbb);
                        //System.out.println(new String(bbb));
                    }
                    break;
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_14): unexpected exception.");
        }
    }
}