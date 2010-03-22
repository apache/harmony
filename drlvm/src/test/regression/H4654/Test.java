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

package org.apache.harmony.drlvm.tests.regression.h4654;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import junit.framework.TestCase;

/**
 * Tests that VM doesn't crash in printStackTrace() for OOME.
 * Runs VM in child process for the 'TestClass' and checks that it doesn't
 * crash and returns zero exitValue.
 * Child process for the test application is required because crash didn't
 * reproduce in junit test (main class derived from junit.framework.TestCase).
 */
public class Test extends TestCase {

    final static String testClass =
       "org.apache.harmony.drlvm.tests.regression.h4654.OOMEPrintStackTrace";

    public static void main(String[] args) {
        (new Test()).test();
    }

    public void test() {
        ProcessBuilder pb = new ProcessBuilder(
            System.getProperty("java.home") + File.separator + "bin" +
                    File.separator+"java",
            "-XX:vm.assert_dialog=false",
            "-cp",
            System.getProperty("java.class.path"),
            testClass
        );

        // merge stderr adn stdout for child VM process
        pb.redirectErrorStream(true);

        System.out.println("Command line for child VM:");

        for (String arg : pb.command()) {
            System.out.println("  " + arg);
        }

        int exitValue = -1;

        try {
            System.out.println("Launching child VM...");
            Process p = pb.start();
            System.out.println("Child VM started.");

            BufferedReader childOut = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            BufferedReader childErr = new BufferedReader(new InputStreamReader(
                    p.getErrorStream()));

            String outLine;

            while (null != (outLine = childOut.readLine())) {
                System.out.println("child-out>   " + outLine);
            }

            System.out.println("Waiting for child VM process to finish...");

            exitValue = p.waitFor();
            System.out.println("Child VM finished. Exit value = " + exitValue);
        } catch (Throwable exc) {
            exc.printStackTrace();
        }

        assertTrue(exitValue == 0);
    }
}
