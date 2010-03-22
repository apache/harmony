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

public class CastsTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }

// CASTS

    public void testCast1() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast1");
    }

    public void testCast2() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast2");
    }

    public void testCast3() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast3");
    }

    public void testCast4() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast4");
    }

    public void testCast5() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast5");
    }

    public void testCast6() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast6");
    }

    public void testCast7() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast7");
    }

    public void testCast8() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCast8");
    }

    public void testCastWithNotFoundError() throws Throwable {
        getClassLoader().runTest(this, "CastsTest.testCastWithNotFoundError");
    }

}