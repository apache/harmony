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

public class SignatureTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }

//SIGNATURE

    public void testReturn1() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testReturn1");
    }

    public void testReturn2() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testReturn2");
    }
    
    public void testReturn3() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testReturn3");
    }


    public void testParams1() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testParams1");
    }

    public void testParams2() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testParams2");
    }

    public void testParams3() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testParams3");
    }


    public void testParams4() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testParams4");
    }


    public void testParams5() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testParams5");
    }

    public void testExceptions1() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testExceptions1");
    }

    public void testExceptions2() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testExceptions2");
    }

    public void testExceptions3() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testExceptions3");
    }

    public void testExceptions4() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testExceptions4");
    }

    public void testExceptions5() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testExceptions5");
    }

    public void testExceptions6() throws Throwable {
        getClassLoader().runTest(this, "SignatureTest.testExceptions6");
    }

}