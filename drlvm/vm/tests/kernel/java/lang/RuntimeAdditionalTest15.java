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
import java.io.IOException;

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

public class RuntimeAdditionalTest15 extends TestCase {
    /**
     * creation and destroying a lot of jvm-processes getting their streams
     */
    public void test_15() {
        System.out.println("==test_15===");
        if (RuntimeAdditionalTest0.os.equals("Unk")) {
            fail("WARNING (test_15): unknown operating system.");
        }
        int ia = 0;
        String cmnd = RuntimeAdditionalTest0.javaStarter;
        while (ia++ < 5/*100*//*300*//*3000*/) {
            try {
                Process pi3 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                java.io.InputStream is = pi3.getInputStream();
                Process pi32 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                pi3.getInputStream();
                Process pi33 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                pi3.getInputStream();
                Process pi34 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                pi3.getInputStream();
                Process pi35 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                pi3.getInputStream();
                Process pi36 = Runtime.getRuntime().exec(cmnd);
                pi3.getOutputStream();
                pi3.getErrorStream();
                pi3.getInputStream();
                pi3.destroy();
                pi32.destroy();
                pi33.destroy();
                pi34.destroy();
                pi35.destroy();
                pi36.destroy();
                Thread.sleep(50);
                pi3.getOutputStream();
                pi3.getErrorStream();
                is = pi3.getInputStream();
				try {
				   is.available();
				} catch (IOException _) {
				}
            } catch (Exception eeee) {
				System.out.println("=="+ia+"===");
                eeee.printStackTrace();
                if (ia == 1) fail("ERROR (test_15): unexpected exception just at the first java invocation.");
            }
        }
    }
}