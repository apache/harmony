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

package org.apache.harmony.drlvm.tests.regression.h3908;

import junit.framework.TestCase;

import java.lang.reflect.Method;

public class InvokeTest extends TestCase {

    byte b = 0;

    public static void main(String args[]) {
        (new InvokeTest()).test();
    }

    public void test() {
        boolean status = false;

        try {
            Method method = InvokeTest.class.getDeclaredMethod("method", new Class[] {Byte.TYPE});
            method.invoke(new InvokeTest(), new Object[] {new N()});
            System.out.println("TEST FAILED: no exception");
        } catch(IllegalArgumentException e) {
            status = true;
            System.out.println("TEST PASSED: " + e);
        } catch(Exception e) {
            System.out.println("TEST FAILED: unexpected " + e);
        }

        assertTrue(status);
    }

    public void method(byte b) {
        this.b = b;
    }
}

class N extends Number {

    public double doubleValue() {
        return 0;
    }

    public float floatValue() {
        return 0;
    }

    public int intValue() {
        return 0;
    }

    public long longValue() {
        return 0;
    }
}
