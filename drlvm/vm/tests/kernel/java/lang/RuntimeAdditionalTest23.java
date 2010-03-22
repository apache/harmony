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

public class RuntimeAdditionalTest23 extends TestCase {
    /**
     * get jvm process' streams, wait for finish of the process then read input
     * stream, then exitValue
     */
    public void test_23() {
        System.out.println("==test_23===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_23): unknown operating system.");
        }
        try {
            String cmnd = RuntimeAdditionalTest0.javaStarter;
            Process pi3 = Runtime.getRuntime().exec(cmnd);
            pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
            while (true) {
                try {
                    Thread.sleep(50);
                    /*System.out.println(*/pi3.exitValue()/*)*/;
                    break;
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }
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
            /*System.out.println(*/pi3.exitValue()/*)*/;
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_23): unexpected exception.");
        }
    }
}