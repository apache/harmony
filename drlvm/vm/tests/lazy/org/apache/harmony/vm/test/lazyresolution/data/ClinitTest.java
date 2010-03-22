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

public class ClinitTest extends LazyTest {

    public void testClinit1() {
        int before = getNumLoads();

        int value = LazyObject1.intStaticField;

        int after = getNumLoads();

        Assert.assertEquals(value, 10);
        Assert.assertEquals(after-before, 1);
    }


    public void testClinit2() {
        int before = getNumLoads();

        int value = LazyObject2.intStaticField;

        int after = getNumLoads();

        Assert.assertEquals(10, value);
        Assert.assertEquals(2, after-before); //LazyObject2 + LazyInterface1 inits
    }

 
    public void testClinit3() {
        int before = getNumLoads();

        int value = LazyInterface2.intStaticField;

        int after = getNumLoads();

        Assert.assertEquals(1, after-before); 
        Assert.assertEquals(12, value); 
    }

    public void testClinit4() {
        int before = getNumLoads();

        int value1 = LazyObject1.intStaticField;
        int value2 = new LazyObject1().intField; //this helper has assertion that class is initialized

        int after = getNumLoads();

        Assert.assertEquals(1, after-before); 
        Assert.assertEquals(10, value1); 
        Assert.assertEquals(20, value2); 
    }
 
}
