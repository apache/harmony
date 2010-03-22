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

public class SignatureTest extends LazyTest {
    
    LazyObject1 foo1() {
        return null;
    }

    LazyObject1 foo2() {
        return new LazyObject1();
    }

    LazyObject2 foo3() {
        return new LazyObject2();
    }

    public void testReturn1() {
        int before = getNumLoads();

        Object o = foo1();
        
        int after = getNumLoads();

        Assert.assertEquals(0, after-before);
        Assert.assertNull(o);
    }

    public void testReturn2() {
        int before = getNumLoads();

        Object o = foo2();
        
        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
        Assert.assertNotNull(o);
    }

    public void testReturn3() {
        int before = getNumLoads();

        LazyInterface2 o = foo3();
        
        int after = getNumLoads();
        Assert.assertEquals(2, after-before);
        Assert.assertNotNull(o);
    }


// most of the params tests are expected to be inlined
// in this case method params are unresolved when method is compiled

    boolean poo1(LazyObject1 o) {
        return o!=null;
    }


    public void testParams1() {
        int before = getNumLoads();

        LazyObject1 o = new LazyObject1();
        boolean res = poo1(o);

        int after = getNumLoads();
        Assert.assertTrue(res);
        Assert.assertEquals(1, after-before);
    }

    boolean poo2(LazyObject1 o1, LazyObject2 o2) {
        return o1!=null && o2!=null;
    }

    public void testParams2() {
        int before = getNumLoads();

        boolean res = poo2(null, null);

        int after = getNumLoads();
        Assert.assertFalse(res);
        Assert.assertEquals(0, after-before);
    }

    long poo3(LazyObject1[] arr) {
        return arr.length;
    }

    public void testParams3() {
        int before = getNumLoads();

        boolean res = poo3(new LazyObject1[10]) == 10L;

        int after = getNumLoads();
        Assert.assertTrue(res);
        Assert.assertEquals(1, after-before);
    }

    long poo4(LazyObject1[][] arr) {
        return arr.length;
    }

    public void testParams4() {
        int before = getNumLoads();

        boolean res = poo4(new LazyObject1[10][]) == 10L;

        int after = getNumLoads();
        Assert.assertTrue(res);
        Assert.assertEquals(1, after-before);
    }

    long poo5(LazyObject1[][] arr) {
        return arr[0].length;
    }

    public void testParams5() {
        int before = getNumLoads();

        boolean res = poo5(new LazyObject1[10][20]) == 20L;

        int after = getNumLoads();
        Assert.assertTrue(res);
        Assert.assertEquals(1, after-before);
    }




    void too1(LazyObject1 o) throws LazyException1 {
        o.hashCode();
    }


    public void testExceptions1() {
        int before = getNumLoads();

        too1(new LazyObject1());

        int after = getNumLoads();
        Assert.assertEquals(1, after-before);
    }


    public void testExceptions2() {
        int before = getNumLoads();
        boolean passed = false;
        try {
            too1(null);
        } catch (RuntimeException e) {
            passed = true;
            e.hashCode(); //prevent optimization
        }

        int after = getNumLoads();
        Assert.assertTrue(passed);
        Assert.assertEquals(0, after-before);
    }

    void too2(LazyObject1 o) throws LazyException1 {
        if (o==null) {
            throw new LazyException1();
        } 
    }

    public void testExceptions3() {
        int before = getNumLoads();
        boolean passed = true;
        try {
            too2(new LazyObject1());
        } catch (LazyException1 e) {
            passed = false;
            e.hashCode();//prevent optimization
        }

        int after = getNumLoads();
        Assert.assertTrue(passed);
        Assert.assertEquals(1, after-before);
    }

    public void testExceptions4() {
        int before = getNumLoads();
        boolean passed = false;
        try {
            too2(null);
        } catch (LazyException1 e) { //exception is loaded by verifier
            passed = true;
        }

        int after = getNumLoads();
        Assert.assertTrue(passed);
        Assert.assertEquals(0, after-before);
    }




    void throwException(int i) {
        if (i==1) {
            throw new LazyException1();
        }
        throw new LazyException2();
    }

    public void testExceptions5() {
        int before = getNumLoads();
        boolean passed = false;
        try {
            throwException(1);
        } catch (LazyException1 e) {
            passed = true;
        } catch (LazyException2 e) {
            System.err.println("Illegal catch handler");
        }
        int after = getNumLoads();
        Assert.assertTrue(passed);
        Assert.assertEquals(0, after-before);
    }


    public void testExceptions6() {
        int before = getNumLoads();
        boolean passed = false;
        try {
            throwException(2);
        } catch (LazyException1 e) {
            System.err.println("Illegal catch handler");
        } catch (LazyException2 e) {
            passed = true;
        }
        int after = getNumLoads();
        Assert.assertTrue(passed);
        Assert.assertEquals(0, after-before);
    }


}
