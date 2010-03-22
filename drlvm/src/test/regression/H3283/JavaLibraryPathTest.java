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

package org.apache.harmony.drlvm.tests.regression.h3283;

import junit.framework.TestCase;

/**
 * Tests that VM doesn't crash in server mode with java.library.path property
 * redefined via command line option.
 */
public class JavaLibraryPathTest extends TestCase {

    public static void main(String args[]) {
        (new JavaLibraryPathTest()).test();
    }

    public void test() {
        String actual = System.getProperty("java.library.path");
        String expected = System.getProperty("expected.value");

        boolean status = actual.equals(expected);

        if (! status) {
            System.out.println("java.library.path = \"" + actual + "\"");
            System.out.println("expected.value = \"" + expected + "\"");
            System.out.println("Are not equal !!!");
        }

        assertEquals(expected, actual);
    }
}
