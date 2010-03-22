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

public class FieldsTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }


//FIELDS
    public void testStaticGet1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGet1");
    }

    public void testStaticGet2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGet2");
    }

    public void testStaticGet3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGet3");
    }

    public void testStaticGet4() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGet4");
    }

    public void testStaticGetWithNotFoundError() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGetWithNotFoundError");
    }

    public void testStaticPut1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPut1");
    }

    public void testStaticPut2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPut2");
    }

    public void testStaticPut3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPut3");
    }

    public void testStaticPut4() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPut4");
    }

    public void testNonStaticGet1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticGet1");
    }

    public void testNonStaticGet2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticGet2");
    }

    public void testNonStaticGet3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticGet3");
    }

    public void testNonStaticGet4() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticGet4");
    }

    public void testNonStaticPut1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPut1");
    }

    public void testNonStaticPut2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPut2");
    }

    public void testNonStaticPut3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPut3");
    }

    public void testNonStaticPut4() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPut4");
    }

    public void testStaticGetBroken1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGetBroken1");
    }

    public void testStaticPutBroken1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPutBroken1");
    }


    public void testNonStaticGetBroken1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGetBroken1");
    }

    public void testNonStaticPutBroken1() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPutBroken1");
    }

    public void testStaticGetBroken2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGetBroken2");
    }

    public void testStaticPutBroken2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPutBroken2");
    }


    public void testNonStaticGetBroken2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGetBroken2");
    }

    public void testNonStaticPutBroken2() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPutBroken2");
    }

    public void testStaticGetBroken3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticGetBroken3");
    }

    public void testStaticPutBroken3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testStaticPutBroken3");
    }

    public void testNonStaticGetBroken3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticGetBroken3");
    }


    public void testNonStaticPutBroken3() throws Throwable {
        getClassLoader().runTest(this, "FieldsTest.testNonStaticPutBroken3");
    }


}