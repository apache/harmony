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

package org.apache.harmony.drlvm.tests.regression.h788;

import junit.framework.TestCase;

public class Test extends TestCase {

    public void test() throws Exception {
        // check multianewarray
        try { 
            Class cl = Class.forName("org.apache.harmony.drlvm.tests.regression.h788.TestArray");
            cl.newInstance();
            fail();
        } catch (LinkageError e) {
            System.out.println("TestArray:     Passes: " + e);
        }
        // check invokespecial
        try { 
            Class cl = Class.forName("org.apache.harmony.drlvm.tests.regression.h788.TestSpecial");
            cl.newInstance();
            fail();
        } catch (LinkageError e) {
            System.out.println("TestSpecial:   Passes: " + e);
        }
        // check invokevirtual
        try { 
            Class cl = Class.forName("org.apache.harmony.drlvm.tests.regression.h788.TestVirtual");
            cl.newInstance();
            fail();
        } catch (LinkageError e) {
            System.out.println("TestVirtual:   Passes: " + e);
        }        
        // check invokeinterface
        try { 
            Class cl = Class.forName("org.apache.harmony.drlvm.tests.regression.h788.TestInterface");
            cl.newInstance();
            fail();
        } catch (LinkageError e) {
            System.out.println("TestInterface: Passes: " + e);
        }
        // check invokestatic
        try { 
            Class cl = Class.forName("org.apache.harmony.drlvm.tests.regression.h788.TestStatic");
            cl.newInstance();
            fail();
        } catch (LinkageError e) {
            System.out.println("TestStatic:    Passes: " + e);
        }
    }
}
