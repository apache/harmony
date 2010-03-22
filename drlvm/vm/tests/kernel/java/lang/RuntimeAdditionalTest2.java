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

public class RuntimeAdditionalTest2 extends TestCase {
    /**
     * 1. destroy java process then Process.exitValue 2. waitFor java process
     * then Process.exitValue, Process.exitValue 3. destroy "java -version"
     * process then Process.exitValue, Process.exitValue
     */
    public void test_3() {
        System.out.println("==test_3===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_3): unknown operating system.");
        }
        Process pi3 = null;
        try {

            String cmnd = RuntimeAdditionalTest0.javaStarter;
            pi3 = Runtime.getRuntime().exec(cmnd);
            pi3.destroy();
            /*System.out.println(*/pi3.exitValue()/*)*/;
        } catch (Exception eeee) {
            //eeee.printStackTrace();
            while (true) {
                try {
                    /*System.out.println(=ntln("XXX: " + */pi3.exitValue()/*)*/;
                    break;
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }
        }
        /*System.out.println("YYY: " + */pi3.exitValue()/*)*/;
        try {

            String cmnd = RuntimeAdditionalTest0.javaStarter;
            pi3 = Runtime.getRuntime().exec(cmnd);
            /*System.out.println(*/pi3.waitFor()/*)*/;
            /*System.out.println(*/pi3.exitValue()/*)*/;
            /*System.out.println(*/pi3.exitValue()/*)*/;
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_3): unexpected exception.");
        }
        try {

            String cmnd = RuntimeAdditionalTest0.javaStarter+" -version";
            pi3 = Runtime.getRuntime().exec(cmnd);
            /*System.out.println(*/pi3.waitFor()/*)*/;
            /*System.out.println(*/pi3.exitValue()/*)*/;
            /*System.out.println(*/pi3.exitValue()/*)*/;
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_3): unexpected exception.");
        }
    }
}