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

package org.apache.harmony.vm.test.lazyresolution.data;

import junit.framework.*;
import org.apache.harmony.vm.test.lazyresolution.classloader.*;
import org.apache.harmony.vm.test.lazyresolution.data.*;

//separate this class from CallsTest :
//  RI verifier loads all interfaces for all invokeinterface bytecodes
//  this makes getNumLoads depend if any method in a class has interface calls.
//  in the methods of this class it's expected that all interfaces are loaded
public class InterfaceCallsTest extends LazyTest {

    public void testInvokeInterface1() throws Throwable {
        startTest();

        LazyInterface2 i2 = new LazyObject2();
        i2.interfaceCall2();

        endTest();
        assertLoaded("LazyObject2");
        Assert.assertEquals(20, LazyObject2.intStaticField);
    }

    public void testInvokeInterface2() throws Throwable {
        startTest();

        LazyInterface3 i3 = new LazyObject3();
        i3.interfaceCall3();

        endTest();

        assertLoaded("LazyObject3");//Assert.assertEquals(1, after-before);
        Assert.assertEquals(110, LazyObject3.intStaticField);
    }

    public void testInvokeInterface3() throws Throwable {
        startTest();

        LazyInterface4 i4 = new LazyObject4();
        i4.interfaceCall2();
        i4.interfaceCall3();
        i4.interfaceCall4();

        endTest();

        assertLoaded("LazyObject4");
        Assert.assertEquals(1120, LazyObject4.intStaticField);
    }


    void _testInvokeInterfaceBroken1() {
        LazyInterface2 i2 = new LazyObject2();
        i2.interfaceCall2a();
    }

    public void testInvokeInterfaceBroken1() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            _testInvokeInterfaceBroken1();            
        } catch (InstantiationError e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    }


    //check for a null object merge
    public void testInvokeInterfaceBroken2() {
        setBrokenObjects(true);
        LazyInterface2 res = null;
        if (LazyObject2.intStaticField==10) {
            res = new LazyObject3();
        }
        boolean passed = false;
        try {
            res.interfaceCall2();
        } catch (IncompatibleClassChangeError e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    }

    //check for 2 unresolved types merge
    //WARN: this has incompatible with RI behaviour if lazy resolution is turned off!
    public void testInvokeInterfaceBroken3() {
        setBrokenObjects(true);
        LazyInterface2 res = null;
        if (LazyObject2.intStaticField==10) {
            res = new LazyObject3();
        } else {
            res = new LazyObject3();
        }
        boolean passed = false;
        try {
            res.interfaceCall2();
        } catch (IncompatibleClassChangeError e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    }


}
