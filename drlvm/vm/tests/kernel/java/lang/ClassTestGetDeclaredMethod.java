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
 * tested method: getDeclaredMethod
 */
@SuppressWarnings(value={"all"}) public class ClassTestGetDeclaredMethod extends TestCase {

    /**
     * Private method method1 should be reflected by getDeclaredMethod() method.
     */
    public void test1() {
        try {
            final String name = "method1";
            Method m = A.class.getDeclaredMethod(name, (Class[]) null);
            assertNotNull("null expected", m);
            assertEquals("incorrect name", name, m.getName());
            assertSame("objects differ", A.class, m.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Attempt to retrieve public method declared in the super class.
     */
    public void test2() {
        try {
            String methodName = "wait";
            getClass().getDeclaredMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * if name is null NullPointerException should be thrown.
     */
    public void test3() {
        try {
            String methodName = null;
            getClass().getDeclaredMethod(methodName, (Class[]) null);
        } catch (NullPointerException e) {
            return;
        } catch (NoSuchMethodException e) {
        }
        fail("NullPointerException exception expected");
    }

    /**
     * NoSuchMethodException should be thrown if name is equal to "&lt;cinit&gt;"
     */
    public void test4() {
        try {
            String methodName = "<cinit>";
            A.class.getDeclaredMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * NoSuchMethodException should be thrown if name is equal to "&lt;init&gt;"
     */
    public void test5() {
        try {
            String methodName = "<init>";
            A.class.getDeclaredMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * if a class contains the method with the same name and parameters as its
     * super class then the method of this class should be reflected.
     */
    public void test6() {
        try {
            String methodName = "toString";
            Method m = A.class.getMethod(methodName, new Class[] {});
            assertEquals("incorrect name", methodName, m.getName());
            assertSame("objects differ", A.class, m.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
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

        public String toString() {
            return null;
        }
    }
}