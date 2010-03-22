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

package org.apache.harmony.drlvm.tests.regression.h4512;

import junit.framework.TestCase;

/**
 * Test case for GetCurrentThreadCpuTime() TI function.
 * Tests that vm doesn't crash in that function.
 */
public class GetCurrentThreadCpuTime extends TestCase {

    public static void main(String args[]) {
        (new GetCurrentThreadCpuTime()).test();
    }

    public void test() {

        try {
            System.err.println("[Java]: Throwing an exception to invoke agent");
            // pass execution to the agent
            throw new InvokeAgentException();
        } catch (Exception e) {
            System.err.println("[Java]: Exception caught");
        }

        System.err.println("[Java]: test done");
        assertTrue(Status.status);
    }
}

class InvokeAgentException extends Exception {}

class Status {
    /** the field should be modified by jvmti agent to determine test result. */
    public static boolean status = false;
}
