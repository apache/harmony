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

public class RuntimeAdditionalTest17 extends TestCase {
    /**
     * in loop: creation of echo-process, touch its streams via available and
     * flush, destroy the process and touch streams again
     */
    public void test_17() {
        System.out.println("==test_17===");
        int ia = 0;
        String cmnd = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            cmnd = RuntimeAdditionalTest0.cm + " /C \"echo 777\"";
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //cmnd = "/bin/sh -c \"echo 777\"";
            cmnd = "/bin/echo 777";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }
        while (ia++ < 600/*1000*//*10000*/) {
            try {
                Process pi3 = Runtime.getRuntime().exec(cmnd);
                java.io.OutputStream os = pi3.getOutputStream();
                java.io.InputStream es = pi3.getErrorStream();
                java.io.InputStream is = pi3.getInputStream();
                is.available();
                es.available();
                os.flush();
                pi3.destroy();
                is.available();
                es.available();
                os.flush();
            } catch (Exception eeee) {
				System.out.println("=="+ia+"===");
                eeee.printStackTrace();
                if(ia==1) fail("ERROR (test_17): unexpected exception.");
            }
        }
    }
}