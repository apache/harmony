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

public class InterfaceCallsTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }


    public void testInvokeInterface1() throws Throwable {
        getClassLoader().runTest(this, "InterfaceCallsTest.testInvokeInterface1");
    }

    public void testInvokeInterface2() throws Throwable {
        getClassLoader().runTest(this, "InterfaceCallsTest.testInvokeInterface2");
    }

    public void testInvokeInterface3() throws Throwable {
        getClassLoader().runTest(this, "InterfaceCallsTest.testInvokeInterface3");
    }

    public void testInvokeInterfaceBroken1() throws Throwable {
        getClassLoader().runTest(this, "InterfaceCallsTest.testInvokeInterfaceBroken1");
    }

    public void testInvokeInterfaceBroken2() throws Throwable {
        getClassLoader().runTest(this, "InterfaceCallsTest.testInvokeInterfaceBroken2");
    }

    public void testInvokeInterfaceBroken3() throws Throwable {
        getClassLoader().runTest(this, "InterfaceCallsTest.testInvokeInterfaceBroken3");
    }

}