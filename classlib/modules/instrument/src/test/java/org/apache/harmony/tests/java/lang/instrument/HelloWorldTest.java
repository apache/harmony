/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.tests.java.lang.instrument;

import static org.apache.harmony.tests.java.lang.instrument.InstrumentTestHelper.LINE_SEPARATOR;
import static org.apache.harmony.tests.java.lang.instrument.InstrumentTestHelper.PREMAIN_CLASS;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.harmony.tests.java.lang.instrument.agents.HelloWorldAgent;

public class HelloWorldTest extends TestCase {
    private InstrumentTestHelper helper = null;

    private static String JAR_PREFIX = "HelloWorldTest";

    @Override
    public void setUp() {
        helper = new InstrumentTestHelper();
    }

    @Override
    public void tearDown() {
        helper.clean();
    }

    public void testHelloWorld() throws Exception {
        helper.addManifestAttributes(PREMAIN_CLASS, HelloWorldAgent.class
                .getName());
        helper.setJarName(JAR_PREFIX + ".testHelloWorld.jar");
        helper.setMainClass(TestMain.class);
        List<Class> classes = new ArrayList<Class>();
        classes.add(HelloWorldAgent.class);
        helper.addClasses(classes);

        helper.run();

        if (helper.getExitCode() != 0) {
            System.out.println("std err:");
            System.out.println(helper.getStdErr());
            System.out.println("std out:");
            System.out.println(helper.getStdOut());
            fail("helper exit code was non-zero");
        }
        assertEquals("Hello World" + LINE_SEPARATOR, helper.getStdOut());
        assertEquals("", helper.getStdErr());
    }

    public void testMultiLineValue() throws Exception {
        StringBuilder name = new StringBuilder(HelloWorldAgent.class.getName());
        // line separator + one whitespace is allowed
        name.insert(5, "\n ");
        helper.addManifestAttributes(PREMAIN_CLASS, name.toString());

        helper.setJarName(JAR_PREFIX + ".testMultiLineValue.jar");
        helper.setMainClass(TestMain.class);
        List<Class> classes = new ArrayList<Class>();
        classes.add(HelloWorldAgent.class);
        helper.addClasses(classes);

        helper.run();

        if (helper.getExitCode() != 0) {
            System.out.println("std err:");
            System.out.println(helper.getStdErr());
            System.out.println("std out:");
            System.out.println(helper.getStdOut());
            fail("helper exit code was non-zero");
        }
        assertEquals("Hello World" + LINE_SEPARATOR, helper.getStdOut());
        assertEquals("", helper.getStdErr());
    }

    public void testInvalidMultiLineValue() throws Exception {
        StringBuilder name = new StringBuilder(HelloWorldAgent.class.getName());
        // line separator + two whitespaces is not allowed
        name.insert(5, "\n  ");
        helper.addManifestAttributes("Premain-Class", name.toString());

        helper.setJarName(JAR_PREFIX + ".testInvalidMultiLineValue.jar");
        helper.setMainClass(TestMain.class);
        List<Class> classes = new ArrayList<Class>();
        classes.add(HelloWorldAgent.class);
        helper.addClasses(classes);

        helper.run();

        if (helper.getExitCode() == 0) {
            System.out.println("std err:");
            System.out.println(helper.getStdErr());
            System.out.println("std out:");
            System.out.println(helper.getStdOut());
            fail("helper exit code was zero");
        }
        if (!(helper.getStdErr().contains(
                  ClassNotFoundException.class.getName()))) {
            System.out.println("std err:");
            System.out.println(helper.getStdErr());
            System.out.println("std out:");
            System.out.println(helper.getStdOut());
            fail("helper should have thrown ClassNotFoundException");
        }
     }
}
