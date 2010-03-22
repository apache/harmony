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

public class RuntimeAdditionalTest20 extends TestCase {
    /**
     * wait for finish of jvm process then destroy it then exitValue, exitValue
     */
    public void test_20() {
        System.out.println("==test_20===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_20): unknown operating system.");
        }
        try {
            String cmnd = RuntimeAdditionalTest0.javaStarter+" MAIN";
            Process pi3 = Runtime.getRuntime().exec(cmnd);
            while (true) {
                try {
                    Thread.sleep(50);
                    pi3.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                    continue;
                }
            }
            pi3.destroy();
            /*System.out.println(*/pi3.exitValue()/*)*/;
            /*System.out.println(*/pi3.exitValue()/*)*/;
        } catch (Exception eeee) {
            eeee.printStackTrace();
            fail("ERROR (test_20): unexpected exception.");
        }
    }
}