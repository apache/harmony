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

public class RuntimeAdditionalTest34 extends TestCase {
    /**
     * creat two cat-process, one_byte-read from one one_byte-write (using
     * flushing) to another and one_byte-read there again
     */
    public void test_33() { // it, maybe, hangs the test set run
        System.out.println("==test_33===");
        //String cmnd = null;
        String cmnd1 = null;
        String cmnd2 = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            //cmnd1 = RuntimeAdditionalTest0.cm + " /C tree \"C:\\Documents and Settings\"";
            //cmnd2 = RuntimeAdditionalTest0.cm + " /C cat";
            cmnd1 = RuntimeAdditionalTest0.treeStarter+" \"C:\\Documents and Settings\"";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd1 = "sh -c \"tree /lib\"";
            //cmnd1 = "/usr/bin/tree /lib";
            //cmnd1 = "/usr/bin/tree /bin";
            //cmnd2 = "sh -c  cat";
            cmnd1 = RuntimeAdditionalTest0.treeStarter+" /bin";
            cmnd2 = RuntimeAdditionalTest0.catStarter;
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }
        try {
            Process pi3 = Runtime.getRuntime().exec(cmnd1);
            Process pi4 = Runtime.getRuntime().exec(cmnd2);
            pi3.getOutputStream();
            pi3.getErrorStream();
            java.io.InputStream is = pi3.getInputStream();
            java.io.OutputStream os4 = pi4.getOutputStream();
            pi4.getErrorStream();
            java.io.InputStream is4 = pi4.getInputStream();

            int b1;
            //int b2;
            while (true) {
                while ((is.available()) != 0) {
                    b1 = is.read();
                    os4.write(b1);
                    while ((is4.available()) != 0) {
                        /*b2 =*/ is4.read();
                        ///* if (b1!=b2) */System.out.print(Character
                        //        .toString((char) b2));
                    }
                }
                try {
                    pi3.exitValue();
                    while ((is.available()) != 0) {
                        b1 = is.read();
                        os4.write(b1);
                        while ((is4.available()) != 0) {
                            /*b2 =*/ is4.read();
                            //\\//\\System.out.print(Character.toString((char)b2));
                        }
                    }
                    break;
                } catch (IllegalThreadStateException e) {
                    Thread.sleep(20);
                    continue;
                }
            }

        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_33): unexpected exception.");
        }
		RuntimeAdditionalTest0.killCat();
	}
}