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

public class AllocationTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }


//ALLOCATION
    public void testNewObj1() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObj1");
    }

    public void testNewObj2() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObj2");
    }

    public void testNewObj3() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObj3");
    }

    public void testNewObj4() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObj4");
    }

    public void testNewObj5() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObj5");
    }

    public void testNewObjInLoop() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObjInLoop");
    }

    public void testNewObjBroken1() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObjBroken1");
    }

    public void testNewObjBroken2() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObjBroken2");
    }

    public void testNewObjBroken3() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewObjBroken3");
    }

    public void testNewArray1() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewArray1");
    }

    public void testNewArray2() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewArray2");
    }

    public void testNewArray3() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewArray3");
    }

    public void testNewArray4() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewArray4");
    }

    public void testNewArray5() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewArray5");
    }

    public void testNewArray6() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewArray6");
    }

    public void testNewMultiArray1() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray1");
    }


    public void testNewMultiArray2() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray2");
    }

    public void testNewMultiArray3() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray3");
    }

    public void testNewMultiArray4() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray4");
    }

    public void testNewMultiArray5() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray5");
    }

    public void testNewMultiArray6() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray6");
    }

    public void testNewMultiArray7() throws Throwable {
        getClassLoader().runTest(this, "AllocationTest.testNewMultiArray7");
    }

}