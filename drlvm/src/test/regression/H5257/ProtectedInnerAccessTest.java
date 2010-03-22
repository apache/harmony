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

package org.apache.harmony.drlvm.tests.regression.h5257;

import junit.framework.TestCase;
import org.apache.harmony.drlvm.tests.regression.h5257.pkg.Holder;

public class ProtectedInnerAccessTest extends TestCase {

    public void test1() {
        assertNotNull(Accessor.getObj());
    }
    public void test2() {
        assertNotNull(Accessor.getArray());
    }
    public void test3() {
        assertNotNull(Accessor.castArray(Accessor.getArray()));
    }

    static class Accessor extends Holder {
    static Object getObj() {
        return new I();
    }

    static Object[] getArray() {
        return new I[0];
    }

    static Object castArray(Object arr) {
        return (I[])arr;
    }
}
}

