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
import java.util.*;

public class FieldsTest extends LazyTest {

    public void testStaticGet1() {
        int before = getNumLoads();

        int value = LazyObject1.intStaticField;

        int after = getNumLoads();
        Assert.assertEquals(10, value); 
        Assert.assertEquals(1, after-before); 
    } 

    public void testStaticGet2() {
        int before = getNumLoads();

        LazyObject1 obj = LazyObject5.staticObjectField;

        int after = getNumLoads();
        Assert.assertNotNull(obj); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testStaticGet3() {
        int before = getNumLoads();

        LazyObject1[] arr = LazyObject5.staticArrayField;

        int after = getNumLoads();
        Assert.assertNotNull(arr); 
        Assert.assertEquals(10, arr.length); 
        Assert.assertNull(arr[0]);
        Assert.assertEquals(2, after-before); 
    } 

    public void testStaticGet4() {
        int before = getNumLoads();

        LazyObject1[][] arr = LazyObject5.staticMultiArrayField;

        int after = getNumLoads();
        Assert.assertNotNull(arr); 
        Assert.assertEquals(10, arr.length); 
        Assert.assertNotNull(arr[5]); 
        Assert.assertEquals(20, arr[5].length); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testStaticGetWithNotFoundError() {
        int before = getNumLoads();
        hideClass("LazyObject1");
        boolean wasCNFE = false;
        try {
            int value = LazyObject1.intStaticField;
        } catch (NoClassDefFoundError e) {
            wasCNFE=true;
        } finally {
            restoreClass("LazyObject1");
        }
        Assert.assertTrue(wasCNFE);
    } 

    public void testStaticPut1() {
        int before = getNumLoads();

        int value = 12;
        LazyObject1.intStaticField = value;

        int after = getNumLoads();
        Assert.assertEquals(value, LazyObject1.getIntStaticField()); 
        Assert.assertEquals(1, after-before); 
    } 


    public void testStaticPut2() {
        int before = getNumLoads();

        LazyObject1 value = new LazyObject1();
        LazyObject5.staticObjectField = value;

        int after = getNumLoads();
        Assert.assertEquals(value, LazyObject5.getStaticObjectField()); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testStaticPut3() {
        int before = getNumLoads();

        LazyObject1[] value = new LazyObject1[100];
        LazyObject5.staticArrayField = value;

        int after = getNumLoads();
        Assert.assertEquals(value, LazyObject5.getStaticArrayField()); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testStaticPut4() {
        int before = getNumLoads();

        LazyObject1[][] value = new LazyObject1[10][10];
        LazyObject5.staticMultiArrayField = value;

        int after = getNumLoads();
        Assert.assertEquals(value, LazyObject5.getStaticMultiArrayField()); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testNonStaticGet1() {
        int before = getNumLoads();

        int value = new LazyObject1().intField;

        int after = getNumLoads();
        Assert.assertEquals(20, value); 
        Assert.assertEquals(1, after-before); 
    } 

    public void testNonStaticGet2() {
        int before = getNumLoads();

        Object o = new LazyObject5().objectField;

        int after = getNumLoads();
        Assert.assertNotNull(o); 
        Assert.assertEquals(2, after-before); 
        Assert.assertEquals(LazyObject5.getStaticObjectField(), o); 
    } 

    public void testNonStaticGet3() {
        int before = getNumLoads();

        Object[] o = new LazyObject5().arrayField;

        int after = getNumLoads();
        Assert.assertNotNull(o); 
        Assert.assertEquals(2, after-before); 
        Assert.assertEquals(LazyObject5.getStaticArrayField(), o); 
    } 

    public void testNonStaticGet4() {
        int before = getNumLoads();

        Object[][] o = new LazyObject5().multiArrayField;

        int after = getNumLoads();
        Assert.assertNotNull(o); 
        Assert.assertEquals(2, after-before); 
        Assert.assertEquals(LazyObject5.getStaticMultiArrayField(), o); 
    } 

    public void testNonStaticPut1() {
        int before = getNumLoads();

        int value = 22;
        LazyObject1 lo = new LazyObject1();
        lo.intField = value;            

        int after = getNumLoads();
        Assert.assertEquals(value, lo.getIntField()); 
        Assert.assertEquals(1, after-before); 
    } 

    public void testNonStaticPut2() {
        int before = getNumLoads();

        LazyObject5 o = new LazyObject5();
        LazyObject1 value = new LazyObject1();
        o.objectField = value;            

        int after = getNumLoads();
        Assert.assertEquals(value, o.getObjectField()); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testNonStaticPut3() {
        int before = getNumLoads();

        LazyObject5 o = new LazyObject5();
        LazyObject1[] value = new LazyObject1[100];
        o.arrayField = value;            

        int after = getNumLoads();
        Assert.assertEquals(value, o.getArrayField()); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testNonStaticPut4() {
        int before = getNumLoads();

        LazyObject5 o = new LazyObject5();
        LazyObject1[][] value = new LazyObject1[10][10];
        o.multiArrayField = value;            

        int after = getNumLoads();
        Assert.assertEquals(value, o.getMultiArrayField()); 
        Assert.assertEquals(2, after-before); 
    } 

    public void testStaticGetBroken1() {
        setBrokenObjects(true);
        int value = 0;
        try {
            value = LazyObject1.intStaticField;
        } catch (IllegalAccessError e) {
            value = -1;
        }
        Assert.assertEquals(-1, value); 
    } 

    public void testStaticPutBroken1() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            LazyObject1.intStaticField=10;
        } catch (IllegalAccessError e) {
            passed = true;
        }
        Assert.assertTrue(passed); 
    } 


    public void testNonStaticGetBroken1() {
        setBrokenObjects(true);
        int value = 0;
        try {
            value = new LazyObject1(10).intField;
        } catch (IllegalAccessError e) {
            value = -1;
        }
        Assert.assertEquals(-1, value); 
    } 

    public void testNonStaticPutBroken1() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            new LazyObject1(10).intField = 100;
        } catch (IllegalAccessError e) {
            passed=true;
        }
        Assert.assertTrue(passed); 
    } 


    public void testStaticGetBroken2() {
        setBrokenObjects(true);
        int value = 0;
        try {
            value = LazyObject1.intStaticField2;
        } catch (NoSuchFieldError e) {
            value = -1;
        }
        Assert.assertEquals(-1, value); 
    } 

    public void testStaticPutBroken2() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            LazyObject1.intStaticField2=10;
        } catch (NoSuchFieldError e) {
            passed = true;
        }
        Assert.assertTrue(passed); 
    } 


    public void testNonStaticGetBroken2() {
        setBrokenObjects(true);
        int value = 0;
        try {
            value = new LazyObject1(10).intField2;
        } catch (NoSuchFieldError  e) {
            value = -1;
        }
        Assert.assertEquals(-1, value); 
    } 

    public void testNonStaticPutBroken2() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            new LazyObject1(10).intField2 = 100;
        } catch (NoSuchFieldError  e) {
            passed=true;
        }
        Assert.assertTrue(passed); 
    } 

    private static void _testStaticGetBroken3() {
        for (int i=0;i<100;i++){} //avoid inlining
        Map m = LazyObject1.staticMapField; //mapField has String type if for a broken package
    }

    public void testStaticGetBroken3() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            new HashMap();//preload classes
            new String("");
            _testStaticGetBroken3();            
        } catch (NoSuchFieldError e) {
            passed = true;
        }
        Assert.assertTrue(passed); 
    } 

    private static void _testStaticPutBroken3() {
        for (int i=0;i<100;i++){} //avoid inlining
        LazyObject1.staticMapField = new HashMap(); //mapField has String type if for a broken package
    }

    public void testStaticPutBroken3() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            new HashMap();//preload classes
            new String("");
            _testStaticPutBroken3();            
        } catch (NoSuchFieldError e) {
            passed = true;
        }
        Assert.assertTrue(passed); 
    } 


    private static void _testNonStaticGetBroken3() {
        for (int i=0;i<100;i++){} //avoid inlining
        Map m = new LazyObject1(1).mapField; //mapField has String type if for a broken package
    }                           

    public void testNonStaticGetBroken3() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            new HashMap();//preload classes
            new String("");
            _testNonStaticGetBroken3();            
        } catch (NoSuchFieldError e) {
            passed = true;
        }
        Assert.assertTrue(passed); 
    } 

    private static void _testNonStaticPutBroken3() {
        for (int i=0;i<100;i++){} //avoid inlining
        new LazyObject1(1).mapField = new HashMap(); //mapField has String type if for a broken package
    }

    public void testNonStaticPutBroken3() {
        setBrokenObjects(true);
        boolean passed = false;
        try {
            new HashMap();//preload classes
            new String("");
            _testStaticPutBroken3();            
        } catch (NoSuchFieldError e) {
            passed = true;
        }
        Assert.assertTrue(passed); 
    } 



}