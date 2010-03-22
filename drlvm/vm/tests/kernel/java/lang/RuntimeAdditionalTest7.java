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

public class RuntimeAdditionalTest7 extends TestCase {
    /**
     * read the result of tree process using the "exitValue -
     * IllegalThreadStateException" loop to try to read all outed data
     */
    public void test_8() {
        System.out.println("==test_8===");
        String cmnd = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd = RuntimeAdditionalTest0.cm + " /C tree \"C:\\Documents and Settings\"";
            cmnd = RuntimeAdditionalTest0.treeStarter+" \"C:\\Documents and Settings\"";
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd = "sh -c \"tree /lib\"";
            //cmnd = "/usr/bin/tree /lib";
            cmnd = RuntimeAdditionalTest0.treeStarter+" /lib";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }

        try {
            Process pi3 = Runtime.getRuntime().exec(cmnd);
            //try {
            //	Thread.sleep(2000);
            //} catch (Exception e) {
            //}
            pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
            int ia;
            while (true) {
                while ((ia = is.available()) != 0) {
                    byte[] bbb = new byte[ia];
                    is.read(bbb);
                    //\\//\\System.out.println(new String(bbb));
                }
                try {
                    pi3.exitValue();
                    while ((ia = is.available()) != 0) {
                        byte[] bbb = new byte[ia];
                        is.read(bbb);
                        //\\//\\System.out.println(new String(bbb));
                    }
                    break;
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }

        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_8): unexpected exception.");
        }
    }
}