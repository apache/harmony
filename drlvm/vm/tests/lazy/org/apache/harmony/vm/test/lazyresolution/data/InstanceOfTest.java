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

public class InstanceOfTest extends LazyTest {

    private Object createObject(boolean object2) {
        if (object2) {
            return new LazyObject2();
        } 
        return new LazyObject3();
//        return object2 ? new LazyObject2() : new LazyObject3();// -> dont use it. RI's verifier will load both classes!
    }

    public void testInstanceOf1() {
        int before = getNumLoads();

        Object o = createObject(true);
        boolean passed = o instanceof LazyObject2;


        int after = getNumLoads();

        Assert.assertEquals(2, after-before); 
        Assert.assertTrue(passed);
    }

    public void testInstanceOf2() {
        int before = getNumLoads();

        Object o = createObject(false);
        boolean passed = ! (o instanceof LazyObject2);


        int after = getNumLoads();
        Assert.assertEquals(4, after-before); 
        Assert.assertTrue(passed);
    }

    public void testInstanceOf3() {
        int before = getNumLoads();

        Object o = createObject(true);
        boolean passed = o instanceof LazyInterface2;


        int after = getNumLoads();

        Assert.assertEquals(2, after-before); 
        Assert.assertTrue(passed);
    }

    public void testInstanceOf4() {
        int before = getNumLoads();

        Object o = createObject(true);
        boolean passed = !(o instanceof LazyInterface4);


        int after = getNumLoads();

        Assert.assertEquals(4, after-before); 
        Assert.assertTrue(passed);
    }


    public void testInstanceOf5() {
        int before = getNumLoads();

        Object o = createObject(false);
        boolean passed = (o instanceof LazyInterface2) && (o instanceof LazyInterface3);


        int after = getNumLoads();

        Assert.assertEquals(3, after-before); 
        Assert.assertTrue(passed);
    }


    public Object createArray(int dims) {
        if (dims == 0) {
            return new LazyObject1();
        } else if (dims == 1) {
            return new LazyObject1[1];
        } else  if (dims==2) {
            return new LazyObject1[1][2];
        }
        return new LazyObject1[1][2][3];
    }

    public void testInstanceOf6() {
        int before = getNumLoads();

        Object o = createArray(1);
        boolean passed = o instanceof LazyObject1[];


        int after = getNumLoads();

        Assert.assertEquals(1, after-before); 
        Assert.assertTrue(passed);
    }

    public void testInstanceOf7() {
        int before = getNumLoads();

        Object o = createArray(2);
        boolean passed = o instanceof LazyObject1[][];


        int after = getNumLoads();

        Assert.assertEquals(1, after-before); 
        Assert.assertTrue(passed);
    }

    public void testInstanceOf8() {
        int before = getNumLoads();

        Object o = createArray(3);
        boolean passed = o instanceof LazyObject1[][][];


        int after = getNumLoads();

        Assert.assertEquals(1, after-before); 
        Assert.assertTrue(passed);
    }

    public void testInstanceOfWithNotFoundError() {
        boolean passed = false;

        hideClass("LazyObject2");
        try  {
            createObject(true);
        } catch (NoClassDefFoundError e) {
            passed = true;
        }
        restoreClass("LazyObject2");
        Assert.assertTrue(passed);
    }

}
