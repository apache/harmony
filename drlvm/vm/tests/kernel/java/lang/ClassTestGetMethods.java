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
 * tested method: getMethods
 */
@SuppressWarnings(value={"all"}) public class ClassTestGetMethods extends TestCase {

    /**
     * Void.TYPE class does not declare methods.
     */
    public void test1() {
        Method[] ms = Void.TYPE.getMethods();
        assertNotNull("unexpected null", ms);
        assertEquals("array length:", 0, ms.length);
    }

    /**
     * An array should inherit all public member methods of the Object class.
     */
    public void test2() {
        Method[] ms = new int[0].getClass().getMethods();
        assertNotNull("unexpected null", ms);
        assertEquals("array length:", 9, ms.length);
    }

    /**
     * Only the public member methods should be returned.
     */
    public void test3() {
        Method[] ms = A.class.getMethods();
        assertNotNull("unexpected null", ms);
        assertEquals("array length:", 10, ms.length);
    }

    /**
     * All member methods of the interface should be returned. Note that it does
     * not include methods of the Object class.
     */
    public void test4() {
        Method[] ms = I.class.getMethods();
        assertNotNull("unexpected null", ms);
        assertEquals("array length:", 1, ms.length);
    }

    /**
     * All member methods of the interface and its super interface should be
     * returned. Note that it does not include methods of the Object class.
     */
    public void test5() {
        Method[] ms = J.class.getMethods();
        assertNotNull("unexpected null", ms);
        assertEquals("array length:", 2, ms.length);
    }

    /**
     * Complex case. All public member methods of this class, its super classes
     * and its super interfaces should be returned. Note that each member method
     * appear only once in a resulting array.
     */
    public void test6() {
        Method[] ms = B.class.getMethods();
        assertNotNull("unexpected null", ms);
        assertEquals("array length:", 11, ms.length);
    }

    private static class A {

        public void m1() {
        }

        private void m2() {
        }
    }

    interface I {

        public void m1();
    }

    interface J extends I {

        void m2();
    }

    private class B extends A implements I, J {

        public void m2() {
        }

        private int m3() {
            return 0;
        }
    }
}