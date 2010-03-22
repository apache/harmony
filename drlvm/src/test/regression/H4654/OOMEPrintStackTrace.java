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

/**
 * Tests that VM doesn't crash in printStackTrace() for OOME.
 */

public class OOMEPrintStackTrace {

    public static void main(String[] args) {
        int size = 1;

        try {
            for (int i = 0; 0 < 10; i++) {
                System.out.println("Allocating 10^" + i + " longs...");
                long[] b = new long[size];
                size *= 10;
            }
        } catch (OutOfMemoryError e) {
            printStackTrace(e);
        }
    }

    static public void printStackTrace(Throwable e) {
        e.printStackTrace();
    }

}
