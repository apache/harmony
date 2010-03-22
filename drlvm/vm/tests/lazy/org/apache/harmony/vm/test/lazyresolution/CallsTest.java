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

package org.apache.harmony.vm.test.lazyresolution;

import junit.framework.*;
import org.apache.harmony.vm.test.lazyresolution.classloader.*;

public class CallsTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }

//CALLS

    public void testInvokeStatic1() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeStatic1");
    }

    public void testInvokeStaticWithNotFoundError() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeStaticWithNotFoundError");
    }

    public void testInvokeStaticBroken1() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeStaticBroken1");
    }

    public void testInvokeStaticBroken2() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeStaticBroken2");
    }

    public void testInvokeVirtual1() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeVirtual1");
    }

    public void testInvokeVirtual2() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeVirtual2");
    }

    public void testInvokeVirtual3() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeVirtual3");
    }

    public void testInvokeVirtualBroken1() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeVirtualBroken1");
    }

    public void testInvokeVirtualBroken2() throws Throwable {
        getClassLoader().runTest(this, "CallsTest.testInvokeVirtualBroken2");
    }

}