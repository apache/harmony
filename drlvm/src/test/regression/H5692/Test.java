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

package org.apache.harmony.drlvm.tests.regression.h5692;

import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;

import junit.framework.TestCase;

public class Test extends TestCase {

    private final String APP = "Application";

    private final String APP_CLASS = Test.class.getPackage().getName() + "."
            + APP;

    private final String MARK1 = "VERBOSE_CLASS_OFF";

    private final String MARK2 = "COMPLETED";

    private final Character FS = File.separatorChar;

    private final String JAVA_COMMAND;

    private final String JAVA_ARGS;

    public Test() throws Exception {
        super();
        final String APP_RESOURCE = APP_CLASS.replace('.', FS);
        final String fName = new File(ClassLoader.getSystemClassLoader()
                .getResource(APP_RESOURCE + ".class").toURI()).toString();
        final String cp = fName.substring(0, fName.indexOf(APP_RESOURCE));
        JAVA_COMMAND = System.getProperty("java.home") + File.separator + "bin"
                + File.separator + "java ";
        JAVA_ARGS = " -cp " + cp + " " + APP_CLASS + " " + APP_CLASS + "1 "
                + MARK1 + " " + APP_CLASS + "2 " + MARK2;
    }

    private String getProcessOutput(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(JAVA_COMMAND + cmd + JAVA_ARGS);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];

        while (true) {
            int count = p.getInputStream().read(buffer);
            if (count == -1) {
                return baos.toString("ISO-8859-1");
            }
            baos.write(buffer, 0, count);
            Thread.sleep(100);
        }
    }

    /**
     * DRLVM does not report classed loaded via the system class loader. Also it
     * prints slashes instead of dots. After this is fixed, the check may become
     * more sophisticated.
     */
    private void validateVerboseClass(String s) {
        int i = 0;

        i = s.indexOf(MARK1, i);
        assertTrue(i > 0);

        i = s.indexOf(MARK2, i);
        assertTrue(i > 0);
    }

    public void testVerboseAll() throws Exception {
        validateVerboseClass(getProcessOutput("-verbose"));
    }

    public void testVerboseClass() throws Exception {
        validateVerboseClass(getProcessOutput("-verbose:class"));
    }

    public void testXVerboseClass() throws Exception {
        validateVerboseClass(getProcessOutput("-Xverbose:class"));
    }

    private boolean isDebugBuild(String s) {
        int i = 0;

        i = s.indexOf("svn = r", i);
        assertTrue(i > 0);

        if (s.indexOf("debug build", i) > 0) {
            return true;
        }
        assertTrue(s.indexOf("release build", i) > 0);
        return false;
    }

    //public void testTraceAll() throws Exception {
    //    if (!isDebugBuild(getProcessOutput("-version"))) {
    //        return;
    //    }
    //    validateVerboseClass(getProcessOutput("-verbose:class -Xtrace:"));
    //}
}

class Application {
    private static void fail() {
        Thread.dumpStack();
        System.out.println("FAIL");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        ClassLoadingMXBean classMXBean = ManagementFactory
                .getClassLoadingMXBean();
        if (!classMXBean.isVerbose()) {
            fail();
        }
        Class.forName(args[0]);
        classMXBean.setVerbose(false);
        if (classMXBean.isVerbose()) {
            fail();
        }
        System.out.println(args[1]);
        Class.forName(args[2]);
        classMXBean.setVerbose(true);
        if (!classMXBean.isVerbose()) {
            fail();
        }
        System.out.println(args[3]);
        System.exit(77);
    }
}

class Application1 {
}

interface Application2 {
}
