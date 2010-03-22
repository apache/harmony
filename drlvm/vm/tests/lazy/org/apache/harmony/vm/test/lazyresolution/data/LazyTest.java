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

import org.apache.harmony.vm.test.lazyresolution.classloader.*;

public class LazyTest {
    

    final LazyClassLoader getLazyClassLoader() {
        return (LazyClassLoader)getClass().getClassLoader();
    }

    final void startTest() {
        getLazyClassLoader().startTest();
    }

    final void endTest() {
        //System.err.println(getClass());
        getLazyClassLoader().endTest();
    }

    final void assertNumLoads(int expected) {
        getLazyClassLoader().assertNumLoads(expected);
    }

    final void assertLoaded(String name) throws Throwable {
        getLazyClassLoader().assertLoaded(name);
    }

    final int getNumLoads() {
        return getLazyClassLoader().numLoads;
    }


    final void hideClass(String name) {
        getLazyClassLoader().hideClass(name);
    }

    final void restoreClass(String name) {
        getLazyClassLoader().restoreClass(name);
    }

    final void setBrokenObjects(boolean flag) {
        getLazyClassLoader().useBrokenPackage = flag;
    }


}