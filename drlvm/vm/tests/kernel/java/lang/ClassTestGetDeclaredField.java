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

import java.lang.reflect.Field;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getDeclaredField
 */
@SuppressWarnings(value={"all"}) public class ClassTestGetDeclaredField extends TestCase {

    /**
     * the length field should not be reflected.
     */
    public void test1() {
        try {
            final String name = "length";
            Class c = new int[0].getClass();
            c.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return;
        }
        fail("NoSuchFieldException exception expected");
    }

    /**
     * if name is null NullPoinerexception exception should be thrown.
     */
    public void test2() {
        try {
            final String name = null;
            getClass().getDeclaredField(name);
        } catch (NullPointerException e) {
            return;
        } catch (NoSuchFieldException e) {
        }
        fail("NullPointerException exception expected");
    }

    /**
     * checks whether public field is reflected
     */
    public void test3() {
        try {
            final String name = "i";
            Field f = A.class.getDeclaredField(name);
            assertEquals("incorrect name", name, f.getName());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * checks whether private field is reflected
     */
    public void test4() {
        try {
            final String name = "s";
            Field f = A.class.getDeclaredField(name);
            assertEquals("incorrect name", name, f.getName());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Class B redefines field s of A class. Field of B class should be
     * reflected in this case.
     */
    public void test5() {
        try {
            final String name = "s";
            Field f = B.class.getDeclaredField(name);
            assertEquals("incorrect name", name, f.getName());
            assertSame("objects differ", B.class, f.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Class B does not define field i. So an NoSuchFieldException exception
     * should be thrown even if its super class defines field with this name.
     */
    public void test6() {
        try {
            final String name = "i";
            B.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return;
        }
        fail("NoSuchFieldException exception expected");
    }

    /**
    * Class B does not define field j. So an NoSuchFieldException exception
    * should be thrown even if its super interface defines field with this name.
    */
    public void test7() {
        try {
            final String name = "j";
            B.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return;
        }
        fail("NoSuchFieldException exception expected");
    }

    private static class A {

        public final static int i = 0;

        private String          s;
    }

    interface I {

        public final static int i = 0;

        public final static int j = 1;
    }

    private class B extends A implements I {

        String s;
    }
}