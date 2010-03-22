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

/**
 * @author Evgueni V. Brevnov, Roman S. Bushmanov
 */
package java.lang;

import java.lang.reflect.Method;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getDeclaredMethods
 */
@SuppressWarnings(value={"all"}) public class ClassTestGetDeclaredMethods extends TestCase {

    /**
     * Void.TYPE class does not declare methods.
     */
    public void test1() {
        Method[] ms = Void.TYPE.getDeclaredMethods();
        assertNotNull("null expected", ms);
        assertEquals("array length:", 0, ms.length);
    }

    /**
     * Arrays do not declare methods.
     */
    public void test2() {
        Method[] ms = new int[0].getClass().getDeclaredMethods();
        assertNotNull("null expected", ms);
        assertEquals("array length:", 0, ms.length);
    }

    /**
     * This test case checks several statements. The methods of the super class
     * should not be included in resulting array as well as the &lt;clinit&gt;
     * method. Only private method with "method1" name should be returned.
     */
    public void test3() {
        Method[] ms = A.class.getDeclaredMethods();
        assertNotNull("null expected", ms);
        assertEquals("array length:", 1, ms.length);
        assertEquals("incorrect name", "method1", ms[0].getName());
    }

    /**
     * Helper inner class.
     */
    private static class A {

        static int i;

        static {
            i = 0;
        }

        public A() {
            i = 0;
        }

        private void method1() {
        }
    }
}