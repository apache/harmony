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

package org.apache.harmony.vm.test.lazyresolution;

import junit.framework.*;
import org.apache.harmony.vm.test.lazyresolution.classloader.*;

public class InstanceOfTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }


//INSTANCEOF
    public void testInstanceOf1() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf1");
    }

    public void testInstanceOf2() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf2");
    }

    public void testInstanceOf3() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf3");
    }

    public void testInstanceOf4() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf4");
    }

    public void testInstanceOf5() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf5");
    }

    public void testInstanceOf6() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf6");
    }

    public void testInstanceOf7() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf7");
    }

    public void testInstanceOf8() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOf8");
    }

    public void testInstanceOfWithNotFoundError() throws Throwable {
        getClassLoader().runTest(this, "InstanceOfTest.testInstanceOfWithNotFoundError");
    }

}
