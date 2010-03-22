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

import java.io.File;

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

public class RuntimeAdditionalTest36 extends TestCase {
    /**
     * check the process exit code has a waited value
     */
    public void test_35() { // it can hang the test set run
        System.out.println("==test_35===");
        String cmnd = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            cmnd = RuntimeAdditionalTest0.cm + " /C ";
            try {
                //for (int i = 0; i < Short.MAX_VALUE / 30; i++) {
                //for (int i = 0; i < Short.MAX_VALUE / 100; i++) {
                for (int i = 0; i < Short.MAX_VALUE / 150; i++) {
                    Process pi3 = Runtime.getRuntime()
                            .exec(
                                    cmnd + "\"exit "
                                            + Integer.toString(i - 500) + "\"");
                    int j = pi3.waitFor();
                    /*System.out.println("sent " + Integer.toString(i - 500)
                            + " | received " + pi3.waitFor());*/
                    if (j != (i - 500)) {
                        fail("ERROR(test_35): exiValue " + pi3.exitValue()
                                + " should be " + (i - 500));
                    }
                }
            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_35): unexpected exception.");
            }
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            try {
                for (int i = 500; i < Short.MAX_VALUE / 100/*30*/; i++) {
                    //Process pi3 = Runtime.getRuntime().exec("java RuntimeAdditionalSupport1 " +
                    // Integer.toString(i-500));
                    //\\//\\Process pi3 = Runtime.getRuntime().exec("java -cp
                    // \""+System.getProperty("java.class.path")+"\" RuntimeAdditionalSupport1 " +
                    // Integer.toString(i-500));
//                    Process pi3 = Runtime
//                            .getRuntime()
//                            .exec(
//                                    "java "
//                                            + (System.getProperty(
//                                                    "java.class.path").length() > 0 ? "-cp "
//                                                    + System
//                                                            .getProperty("java.class.path")
//                                                    : "") + " RuntimeAdditionalSupport1 "
//                                            + Integer.toString(i - 500));
                    String ttt = System.getProperty("vm.boot.class.path") +(System.getProperty("java.class.path").length() > 0 ? 
                            File.pathSeparator + System.getProperty("java.class.path") : "");
                    Process pi3 = Runtime
                    .getRuntime()
                    .exec(
                            "java -Xbootclasspath/a:" + ttt + " -cp " + ttt + " java.lang.RuntimeAdditionalSupport1 "
                                    + Integer.toString(i - 500));
                    int j = pi3.waitFor();
                    /*System.out.println("sent " + Integer.toString(i - 500)
                            + " | received " + pi3.waitFor() + " (" + j + ")"
                            + ((i - 500) & 0xFF));*/
                    if (j != ((i - 500) & 0xFF)) {
                        fail("ERROR(test_35): exiValue " + pi3.exitValue()
                                + " should be " + ((i - 500) & 0xFF));
                    }
                }
            } catch (Exception eeee) {
                eeee.printStackTrace();
                fail("ERROR (test_35): unexpected exception.");
            }
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }
    }

}