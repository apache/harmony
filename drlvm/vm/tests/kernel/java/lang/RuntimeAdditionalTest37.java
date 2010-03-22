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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

public class RuntimeAdditionalTest37 extends TestCase {
    /**
     * check an environment variable appears correctly if exec(..., env, ...) is
     * used
     */
    public void test_36_1() {
        System.out.println("==test_36===");
        String command = null;
        if (RuntimeAdditionalTest0.os.equals("Win")) {
            command = RuntimeAdditionalTest0.cm
                    + " /C \"echo %Z_S_S_2%xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"";
        } else if (RuntimeAdditionalTest0.os.equals("Lin")) {
            //command = "sh -c \"echo $Z_S_S_2\"";
            //command = "echo $Z_S_S_2";
            command = "/usr/bin/env";
        } else {
            fail("WARNING (test_1): unknown operating system.");
        }
        String procValue = null;
        try {
            Process proc = Runtime.getRuntime().exec(command,
                    new String[] { "Z_S_S_2=S_O_M_E_T_H_I_N_G" });
            BufferedReader br = new BufferedReader(new InputStreamReader(proc
                    .getInputStream()));
            boolean flg = true;
            while ((procValue = br.readLine()) != null) {
                //if (procValue.indexOf("Z_S_S_2=S_O_M_E_T_H_I_N_G") != -1) {
                if (procValue.indexOf(RuntimeAdditionalTest0.os.equals("Win")?"S_O_M_E_T_H_I_N_Gx":"S_O_M_E_T_H_I_N_G") != -1) {
                    //System.out.println(procValue);
                    flg = false;
                    return;
                }
                System.out
                        .println("WARNING (test_36): should it be only singl line in env after such exec? ("
                                + procValue + ")");
            }
            if (flg) {
                fail("ERROR (test_36): Z_S_S_2 var should be present and assingned correctly.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("ERROR (test_36): unexpected exception.");
        }
    }
}