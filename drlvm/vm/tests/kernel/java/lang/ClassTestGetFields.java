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
 * tested method: getFields
 */
@SuppressWarnings(value={"all"}) public class ClassTestGetFields extends TestCase {

    /**
     * Void.TYPE class does not declare fields.
     */
    public void test1() {
        Field[] fs = Void.TYPE.getFields();
        assertNotNull("unexpected null", fs);
        assertEquals("array length", 0, fs.length);
    }

    /**
     * Arrays do not declare fields.
     */
    public void test2() {
        Field[] fs = new int[0].getClass().getFields();
        assertNotNull("unexpected null", fs);
        assertEquals("array length", 0, fs.length);
    }

    /**
     * Every field of interface should be reflected.
     */
    public void test3() {
        Field[] fs = I.class.getFields();
        assertNotNull("unexpected null", fs);
        assertEquals("array length", 1, fs.length);
    }

    /**
     * Every field of interface and its super interfaces should be reflected.
     */
    public void test4() {
        Field[] fs = J.class.getFields();
        assertNotNull("unexpected null", fs);
        assertEquals("array length", 2, fs.length);
    }

    /**
     * Only public fields should be reflected.
     */
    public void test5() {
        Field[] fs = A.class.getFields();
        assertNotNull("unexpected null", fs);
        assertEquals("array length", 3, fs.length);
    }

    /**
     * All fields of this class, its super interfaces and super classes should
     * be included in resulting array. Each field should appear only once.
     */
    public void test6() {
        Field[] fs = B.class.getFields();
        assertNotNull("unexpected null", fs);
        assertEquals("array length", 6, fs.length);
    }

    private static class A implements I {

        public final static int i = 0;

        public final static int j = 1;

        Object o;

        private String s;
    }

    interface I {

        int i = 0;
    }

    interface J extends I {

        int k = 0;
    }

    interface L extends I {        
    }
    
    private class B extends A implements L, J {
        
        public int k;

        public String s;
    }
}