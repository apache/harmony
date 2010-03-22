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

package org.apache.harmony.drlvm.tests.regression.h2103;

import junit.framework.TestCase;

/**
 * Loads class and tries to invoke a method which should fail
 * verification.
 *
 * SubClass contains an incorrect invokespecial instruction which invokes
 * a method from a subclass of the current class, while only superclass
 * constructors can be called using this instruction.
 */
public class Test extends TestCase {
    public static void main(String args[]) {
        (new Test()).test();
    }

    public void test() {
        try {
            SupClass.test();
        } catch (VerifyError ve) {
            return;
        } catch (Exception e) {
        }
        fail("A method of SupClass class should throw VerifyError");
    }
}


