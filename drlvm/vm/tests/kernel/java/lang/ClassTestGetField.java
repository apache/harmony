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
@SuppressWarnings(value={"all"}) public class ClassTestGetField extends TestCase {

    /**
     * the length field should not be reflected.
     */
    public void test1() {
        try {
            final String name = "length";
            Class c = new int[0].getClass();
            c.getField(name);
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
            getClass().getField(name);
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
            Field f = A.class.getField(name);
            assertEquals("incorrect name", name, f.getName());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Private field should not be reflected.
     */
    public void test4() {
        try {
            final String name = "s";
            A.class.getField(name);
        } catch (NoSuchFieldException e) {
            return;
        }
        fail("NoSuchFieldException exception expected");
    }

    /**
     * Class B redefines field s of A class. Field of the B class should be
     * reflected in this case.
     */
    public void test5() {
        try {
            final String name = "s";
            Field f = B.class.getField(name);
            assertEquals("incorrect name", name, f.getName());
            assertSame("objects differ", B.class, f.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Class B does not define field k. But its super interface J defines field
     * with such name. The field of the super interface should be returned.
     */
    public void test6() {
        try {
            final String name = "k";
            Field f = B.class.getField(name);
            assertNotNull("unexpected null", f);
            assertEquals("incorrect name", name, f.getName());
            assertSame("objects differ", J.class, f.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Class B does not define field i. But its super interface I and super
     * class A define field with such name. The field of the super interface
     * should be returned.
     */
    public void test7() {
        try {
            final String name = "i";
            Field f = B.class.getField(name);
            assertNotNull("unexpected null", f);
            assertEquals("incorrect name", name, f.getName());
            assertSame("objects differ", I.class, f.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Class B does not define field j. But its super class A defines field with
     * such name. The field of the super class should be returned.
     */
    public void test8() {
        try {
            final String name = "j";
            Field f = B.class.getField(name);
            assertNotNull("unexpected null", f);
            assertEquals("incorrect name", name, f.getName());
            assertSame("objects differ", A.class, f.getDeclaringClass());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Class B does not define field o. Its super class A defines field with
     * such name. Sice this field has private accessibility an
     * NoSuchFieldException exception should be thrown.
     */
    public void test9() {
        try {
            final String name = "o";
            B.class.getField(name);
        } catch (NoSuchFieldException e) {
            return;
        }
        fail("NoSuchFieldException exception expected");
    }

    private static class A {

        public final static int i = 0;

        public final static int j = 1;

        Object o;

        private String s;
    }

    interface I {

        public final static int i = 0;
    }

    interface J extends I {

        int k = 0;
    }

    private class B extends A implements J {

        public String s;
    }
}