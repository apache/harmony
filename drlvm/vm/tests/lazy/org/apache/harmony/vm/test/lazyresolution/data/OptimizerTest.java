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

//tests for HLO optimizer.
//triggering optimizations to work with unresolved types

public class OptimizerTest extends LazyTest {
    
    private int getConst1() {
        return 1;
    }


    public void testIfSimplifier1() {
        int before = getNumLoads();

        if (getConst1() == 1) {
            new LazyObject1();
        }

        int after = getNumLoads();

        Assert.assertEquals(1, after-before);
    }


    public void testIfSimplifier2() {
        int before = getNumLoads();

        if (getConst1() == 1) {
            new LazyObject2();
        }

        int after = getNumLoads();

        Assert.assertEquals(2, after-before);
    }

    public void testIfSimplifier3() {
        int before = getNumLoads();

        if (getConst1() != 1) {
            new LazyObject1();
        }

        int after = getNumLoads();

        Assert.assertEquals(0, after-before);
    }


    int _testDevirt1(boolean v) {
        for (int i=0;i<100;i++){} //avoid inlining
        Map map = null;
        if (v) { // need this branch for merging results of unresolved field & resolved new op
            map = LazyObject1.staticMapField;
        } else {
            map = new HashMap();;
        }
        return map.size();
    }

    public void testDevirt1() {
        //checks that virtual/interface call on resolved object 
        //obtained from unresolved field can be devirtualized
        //this test relies on assertion in devirtualizer

        new HashMap();//resolve field type
        int res = _testDevirt1(true);
        Assert.assertEquals(1, res);
    }

}
