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

package org.apache.harmony.drlvm.tests.regression.h2926;

import junit.framework.TestCase;

/**
 * Test correct locations of catch and throw in JVMTI Exception event
 */
public class Test extends TestCase {
    public void test() {
        TestClass tc = new TestClass();
        tc.f1();
        tc.f2();
        tc.f3();
        tc.f4();
        System.out.println("test done");
        assertTrue(Status.status);
    }
}

class Status {
    public static boolean status = false;
}

