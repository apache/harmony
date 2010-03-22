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

import org.apache.harmony.vm.test.lazyresolution.data.*;
import junit.framework.*;
import org.apache.harmony.vm.test.lazyresolution.classloader.*;

public class AllocationTest extends LazyTest {
    
    //opcode new + invokespecial<init> call
    public void testNewObj1() {
        int before = getNumLoads();

        new LazyObject1();

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
    }

    private Object createObject(boolean object2) {
        if (object2) {
            return new LazyObject2();
        } 
        return new LazyObject3();
//        return object2 ? new LazyObject2() : new LazyObject3(); -> dont use it. RI's verifier will load both classes!
    }


    public void testNewObj2() {
        int before = getNumLoads();
        createObject(true);

        int after = getNumLoads();
        Assert.assertEquals(2, after-before);

    }

    public void testNewObj3() {
        int before = getNumLoads();

        createObject(false);

        int after = getNumLoads();
        Assert.assertEquals(3, after-before);
    }


    public void testNewObj4() {
        hideClass("LazyObject1");
        try {
            new LazyObject1();
            Assert.fail("NoClassDefFoundError expected");
        } catch (NoClassDefFoundError e) {
        }
    } 

    public void testNewObj5() {
        hideClass("LazyInterface2");
        try {
            new LazyObject2();
            Assert.fail("NoClassDefFoundError expected");
        } catch (NoClassDefFoundError e) {
        } 
    } 


    public void testNewObjInLoop() {
        int before = getNumLoads();

        for (int i=0;i<100;i++) {
            new LazyObject1();
        }

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
    }


    //opcode new + invokespecial<init> call
    public void testNewObjBroken1() {
        setBrokenObjects(true);

        try {        
            new LazyObject1();
            Assert.fail("IllegalAccessError expected");
        } catch (IllegalAccessError e) {
        }
    }

    public void testNewObjBroken2() {
        setBrokenObjects(true);

        boolean passed = false;
        try {        
            new LazyObject2();
        } catch (InstantiationError e) {
            passed = true;
        }

        Assert.assertTrue(passed);
    }

    public void testNewObjBroken3() {
        setBrokenObjects(true);

        boolean passed = false;
        try {        
            new LazyObject1(100, 200);
        } catch (NoSuchMethodError e) {
            passed = true;
        }

        Assert.assertTrue(passed);
    }

    public void testNewArray1() {
        int before = getNumLoads();

        LazyObject1[] arr = new LazyObject1[10];

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
        Assert.assertEquals(10, arr.length);
    }

    public void testNewArray2() {
        boolean passed = false;
        try {
            LazyObject1[] arr = new LazyObject1[-1];
        } catch (NegativeArraySizeException e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    }

    public void testNewArray3() {
        hideClass("LazyObject1");
        boolean passed = false;
        try {
            LazyObject1[] arr = new LazyObject1[10];
        } catch (NoClassDefFoundError e) {
            passed=true;
        }
        Assert.assertTrue(passed);
    } 

    public void testNewArray4() {
        hideClass("LazyInterface2");
        boolean passed = false;
        try {
            LazyObject2[] arr = new LazyObject2[10];
        } catch (NoClassDefFoundError e) {
            passed=true;
        }
        Assert.assertTrue(passed);
    } 


    public void testNewArray5() {
        boolean passed = false;
        try {
            LazyObject2[] arr = new LazyObject2[2*1000*1000*1000];
        } catch (OutOfMemoryError e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    } 


    public void testNewArray6() {
        int before = getNumLoads();

        LazyObject1[] arr = new LazyObject1[10];
        LazyObject1 o = new LazyObject1();
        arr[1] = o;

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
        Assert.assertEquals(10, arr.length);
        Assert.assertEquals(o, arr[1]);
    }

     public void testNewMultiArray1() {
        int before = getNumLoads();

        LazyObject1[][] arr = new LazyObject1[10][20];

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
        Assert.assertEquals(10, arr.length);
        Assert.assertEquals(20, arr[0].length);
    }

     public void testNewMultiArray2() {
        boolean passed = false;
        try {
            LazyObject1[][] arr = new LazyObject1[10][-1];
        } catch (NegativeArraySizeException e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    }

     public void testNewMultiArray3() {
        boolean passed = false;
        try {
            LazyObject1[][] arr = new LazyObject1[-1][-1];
        } catch (NegativeArraySizeException e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    }


     public void testNewMultiArray4() {
        int before = getNumLoads();

        LazyObject1[][] arr = new LazyObject1[10][];

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
        Assert.assertEquals(10, arr.length);
        Assert.assertEquals(null, arr[0]);
    }

    public void testNewMultiArray5() {
        hideClass("LazyInterface2");
        boolean passed = false;
        try {
            LazyObject2[][] arr = new LazyObject2[10][10];
        } catch (NoClassDefFoundError e) {
            passed=true;
        }
        Assert.assertTrue(passed);
    } 


    public void testNewMultiArray6() {
        boolean passed = false;
        try {
            LazyObject2[][] arr = new LazyObject2[2*1000][1000*1000];
        } catch (OutOfMemoryError e) {
            passed = true;
        }
        Assert.assertTrue(passed);
    } 

     public void testNewMultiArray7() {
        int before = getNumLoads();

        LazyObject1[][] arr = new LazyObject1[10][];
        LazyObject1[] ar = new LazyObject1[11];
        LazyObject1 o = new LazyObject1();
        ar[0]=o;
        arr[7]=ar;

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
        Assert.assertEquals(10, arr.length);
        Assert.assertEquals(null, arr[0]);
        Assert.assertEquals(ar, arr[7]);
        Assert.assertEquals(o, arr[7][0]);
    }


}