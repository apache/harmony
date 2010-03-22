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

public class CastsTest extends LazyTest {


    public void testCast1() {
        int before = getNumLoads();

        LazyInterface2 i2 = new LazyObject2();
        LazyObject2 o2 = (LazyObject2)i2;

        Assert.assertEquals(o2.getClass(), LazyObject2.class);

        int after = getNumLoads();

        Assert.assertEquals(2, after-before);
    }

    private Object _testCast2() {
        Object i2 = new LazyObject2();
        return (LazyObject1)i2;
    }

    public void testCast2() {

        int before = getNumLoads();

        boolean passed = false;
        try {
            _testCast2();
        } catch (ClassCastException e) {
            passed = true;            
        }

        int after = getNumLoads();

        Assert.assertEquals(3, after-before);
        Assert.assertTrue(passed);
    }

    void _testCast3(Object o) {
        LazyObject1[] arr = (LazyObject1[])o;
        if (arr.length!=10) {
            throw new RuntimeException();
        }
    }

    public void testCast3() {
        int before = getNumLoads();

        Object o = new LazyObject1[10];
        _testCast3(o);

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
    }

    public void testCast4() {
        int before = getNumLoads();

        boolean passed = false;
        Object o = new LazyObject1();
        try {
            _testCast3(o);
        } catch (ClassCastException e) {
            passed = true;            
        }

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
        Assert.assertTrue(passed);
    }

    public void testCast5() {
        int before = getNumLoads();

        boolean passed = false;
        Object o = new LazyObject1[10][20];
        try {
            _testCast3(o);
        } catch (ClassCastException e) {
            passed = true;            
        }

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
        Assert.assertTrue(passed);
    }



    void _testCast6(Object o) {
        LazyObject1[][] arr = (LazyObject1[][])o;
        if (arr.length!=10) {
            throw new RuntimeException();
        }
        if (arr[9].length!=20) {
            throw new RuntimeException();
        }
    }


    public void testCast6() {
        int before = getNumLoads();

        Object o = new LazyObject1[10][20];
        _testCast6(o);

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
    }

    public void testCast7() {
        int before = getNumLoads();

        boolean passed = false;
        Object o = new LazyObject1();
        try {
            _testCast6(o);
        } catch (ClassCastException e) {
            passed = true;            
        }

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
        Assert.assertTrue(passed);
    }

    public void testCast8() {
        int before = getNumLoads();

        boolean passed = false;
        Object o = new LazyObject1[10];
        try {
            _testCast6(o);
        } catch (ClassCastException e) {
            passed = true;            
        }

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
        Assert.assertTrue(passed);
    }

   public void testCastWithNotFoundError() {
        boolean passed = false;

        hideClass("LazyObject1");
        try  {
            _testCast2();
        } catch (NoClassDefFoundError e) {
            passed = true;
        }
        restoreClass("LazyObject1");
        Assert.assertTrue(passed);
    }

}
