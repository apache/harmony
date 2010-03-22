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

package org.apache.harmony.drlvm.tests.regression.h3121;

import junit.framework.TestCase;


/*  The test checks that the JIT correctly optimizes virtual calls
 *  when the receiver is of interface type. Loops are needed to force
 *  JIT recompilation. The method vc() contains both an interface call
 *  and a virtual call, an attempt to devirtualize a virtual call caused
 *  segfault in the ValueProfileCollector (Harmony-3121 issue).
 */
public class H3121 extends TestCase {

    Intf io = new IntfClass(); 
    Object o = new Object();

    public void test() {
	boolean b = false;
        for (int i = 0; i < 10000000; i++) {
            b = vc();
        }
        System.out.println("Test passed");
    }

    public boolean vc() {
        io.fake();
	return io.equals(o);
    }
}
 
interface Intf {
    public void fake();
}

class IntfClass implements Intf {
    public void fake() {
        for (int i = 0; i < 100; i++) {}
    }
}
