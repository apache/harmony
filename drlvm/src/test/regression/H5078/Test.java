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

package org.apache.harmony.drlvm.tests.regression.h5078;

import junit.framework.TestCase;

public class Test extends TestCase {
    static {
        System.loadLibrary("Test");
    }

    public native boolean funcd(double d1, double d2, double d3,
        double d4, double d5, double d6,
        double d7, double d8, double d9);

    public native boolean funcf(float f1, float f2, float f3,
        float f4, float f5, float f6,
        float f7, float f8, float f9);

    public void test() {
        boolean b1 = funcd(0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09);
        boolean b2 = funcf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f);
        assertEquals(true, b1 && b2);
    }
    
    public static void main(String args[]) {
        Test t = new Test();
        t.test();
    }
}

