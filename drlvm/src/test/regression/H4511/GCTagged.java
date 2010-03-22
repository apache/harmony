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

package org.apache.harmony.drlvm.tests.regression.h4511;

import junit.framework.TestCase;

/**
 * Test case for garbage collectin of tagged objects.
 */
public class GCTagged extends TestCase {

    static final int OBJECT_NUMBER = 1000;
    static final int OBJECT_SIZE = 1024 * 1024;

    public static void main(String args[]) {
        (new GCTagged()).test();
    }

    public void test() {

        System.out.println("Allocating " + OBJECT_NUMBER +
                " byte arrays of size " + OBJECT_SIZE);
        int a = 0;
        int i = 0;
        OutOfMemoryError exception = null;

        try {
            for (; i < OBJECT_NUMBER; i++) {

                Object obj = new byte[OBJECT_SIZE];
                try {
                    // notify agent to set the tag
                    throw new InvokeAgentException(obj);
                } catch (InvokeAgentException exc) {
                    // useless code just to prevent optimizing jit from
                    // eliminating emty catch block
                    a += obj.hashCode();
                }
            }

            System.out.println("done.");
        } catch (OutOfMemoryError exc) {
            exception = exc;

            System.out.println("Caught " + exc);
            System.out.println("    at iteration " + i);
            System.out.println("    approx alocated size (bytes) " +
                    i * OBJECT_SIZE);
        }

        assertTrue(null == exception);
    }
}

class InvokeAgentException extends Exception {

    Object object;

    InvokeAgentException(Object obj) {
        this.object = obj;
    }
}
