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
 * tested method: getMethod
 */
public class ClassTestGetMethod extends TestCase {

    /**
     * "method1" method has public accessibility so it should be accessible
     * through Class.getMethod() method.
     */
    public void test1() {
        try {
            Method m = A.class.getMethod("method1", (Class[]) null);
            assertEquals("incorrect name", "method1", m.getName());
            assertSame("objects differ", A.class, m.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * "method2" method has protected accessibility so it shouldn't be returned
     * by Class.getMethod() method. NoSuchMethodException exception expected.
     */
    public void test2() {
        try {
            A.class.getMethod("method2", (Class[]) null);
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * Attempt to retrieve public method declared in the super class.
     */
    public void test3() {
        try {
            String methodName = "wait";
            Method m = getClass().getMethod(methodName, (Class[]) null);
            assertEquals("incorrect name", methodName, m.getName());
            assertSame("objects differ", Object.class, m.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * if name is null NullPointerException should be thrown.
     */
    public void test4() {
        try {
            String methodName = null;
            getClass().getMethod(methodName, (Class[]) null);
        } catch (NullPointerException e) {
            return;
        } catch (NoSuchMethodException e) {
        }
        fail("NullPointerException exception expected");
    }

    /**
     * NoSuchMethodException should be thrown if name is equal to " <cinit>"
     */
    public void test5() {
        try {
            String methodName = "<cinit>";
            A.class.getMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * NoSuchMethodException should be thrown if name is equal to " <init>"
     */
    public void test6() {
        try {
            String methodName = "<init>";
            A.class.getMethod(methodName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * if a class contains the method with the same name and parameters as its
     * super class then the method of this class should be reflected.
     */
    public void test7() {
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
     * if the super class of this class contains the method with the same
     * descriptor as super interface of this class then the method of the super
     * class should be reflected.
     */
    public void test8() {
        try {
            String methodName = "equals";
            Method m = A.class.getMethod(methodName,
                                         new Class[] { Object.class });
            assertEquals("incorrect name", methodName, m.getName());
            assertSame("objects differ", Object.class, m.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * the getMethod() method  should thow the NoSuchMethodException even if the
     * arguments contain nulls.
     */
    public void testBug537() {
        final String methodName = "testBug537";
        try {
            getClass().getMethod(methodName, new Class [] {null});
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("The NoSuchMethodException exception excpected");
    }
    
    interface I {

        boolean equals(Object obj);
    }

    /**
     * Helper inner class.
     */
    private static class A implements I {

        static int i;

        static {
            i = 0;
        }

        public A() {
            i = 0;
        }

        public void method1() {
        }

        protected void method2() {
        }

        public String toString() {
            return null;
        }
    }
}