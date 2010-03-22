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


//separate this class from InterfaceCallsTest :
//  verifier loads all interfaces for all invokeinterface bytecodes found in a class
//  this makes getNumLoads depend if any method in a class has interface calls.
//  in the methods of this class it's expected that all interfaces are NOT loaded
public class CallsTest extends LazyTest {

     //call a static method
    public void testInvokeStatic1() {
        int before = getNumLoads();

        LazyObject1.getIntStaticField();
        
        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
    }

    public void testInvokeStaticWithNotFoundError() {
        hideClass("LazyObject1");
        boolean wasCNFE = false;
        try {
            LazyObject1.getIntStaticField();
        } catch (NoClassDefFoundError e) {
            wasCNFE=true;
        } finally {
            restoreClass("LazyObject1");
        }
        Assert.assertTrue(wasCNFE);
    } 

    public void testInvokeStaticBroken1() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            LazyObject1.getIntStaticField();
        } catch (IllegalAccessError e) {
            passed = true;            
        }
        Assert.assertTrue(passed);
    }

    public void testInvokeStaticBroken2() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            LazyObject1.getIntStaticField2();
        } catch (NoSuchMethodError e) {
            passed = true;            
        }
        Assert.assertTrue(passed);
    }


     //call a virtual method
    public void testInvokeVirtual1() {
        int before = getNumLoads();

        LazyObject1 o = new LazyObject1();
        o.virtualCall();
        
        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
    }


   //call an interface method
    public void testInvokeVirtual2() {
        int before = getNumLoads();

        LazyObject2 o = new LazyObject2();
        o.interfaceCall2(); //known object type -> invokevirtual
        
        int after = getNumLoads();

        Assert.assertEquals(2, after-before);
    }

    //call an interface method
    public void testInvokeVirtual3() {
        int before = getNumLoads();

        LazyObject3 o = new LazyObject3();
        o.interfaceCall2();//known object type -> invokevirtual
        o.interfaceCall3();//known object type -> invokevirtual

        int after = getNumLoads();

        Assert.assertEquals(3, after-before);
        Assert.assertEquals(120, LazyObject3.intStaticField);
    }


    public void _testInvokeVirtualBroken1() {
        LazyObject1 o = new LazyObject1(100);
        o.getIntField();
    }

    public void testInvokeVirtualBroken1() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            _testInvokeVirtualBroken1(); //if this method is inlined LazyObject1 class is loaded by RI's verifier
        } catch (IllegalAccessError e) {
            passed = true;            
        }
        Assert.assertTrue(passed);
    }

    public void _testInvokeVirtualBroken2() {
        LazyObject1 o = new LazyObject1(100);
        o.getIntField2();
    }

    public void testInvokeVirtualBroken2() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            _testInvokeVirtualBroken2(); //if this method is inlined LazyObject1 class is loaded by RI's verifier
        } catch (NoSuchMethodError e) {
            passed = true;            
        }
        Assert.assertTrue(passed);
    }

}
