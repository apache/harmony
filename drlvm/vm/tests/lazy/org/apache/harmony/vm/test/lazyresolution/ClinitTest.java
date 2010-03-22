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

public class ClinitTest extends TestCase {

    public LazyClassLoader getClassLoader() {
        return new LazyClassLoader(Thread.currentThread().getContextClassLoader());
    }

//SIDEEFFECTS

    public void testClinit1() throws Throwable {
        getClassLoader().runTest(this, "ClinitTest.testClinit1");
    }

    public void testClinit2() throws Throwable {
        getClassLoader().runTest(this, "ClinitTest.testClinit2");
    }

    public void testClinit3() throws Throwable {
        getClassLoader().runTest(this, "ClinitTest.testClinit3");
    }

    public void testClinit4() throws Throwable {
        getClassLoader().runTest(this, "ClinitTest.testClinit4");
    }

}