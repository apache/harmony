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

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: forName
 */
@SuppressWarnings(value={"all"}) public class ClassTestForName extends TestCase {

    /**
     * This test case checks two cases. First, a class must be sucessfully
     * loaded by bootstrup class loader. Second, this class instance must be the
     * same as previously loaded.
     */
    public void test1() {
        try {
            final String name = "java.lang.Object";
            Class c = Class.forName(name);
            assertSame("Objects differ", Object.class, c);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * This test case checks two cases. First, a class must be sucessfully
     * loaded by user-defined class loader. Second, this class instance must be
     * the same as previously loaded.
     */
    public void test2() {
        try {
            final String name = ClassTestForName.class.getName();
            Class c = Class.forName(name);
            assertSame("Objects differ", ClassTestForName.class, c);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * This test case checks two cases. First, an array class must be
     * sucessfully loaded by bootstrup class loader. Second, this class instance
     * must be the same as previously loaded.
     */
    public void test3() {
        try {
            Object[] array = new Object[0];
            final String name = array.getClass().getName();
            Class c = Class.forName(name);
            assertSame("Objects differ", array.getClass(), c);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * This test case checks whether a class loaded by forName() method is the
     * same as a class loaded by loadClass() method through the same class
     * loader.
     */
    public void test4() {
        try {
            final String name = getClass().getName() + "$InnerHelper";
            ClassLoader cLoader = ClassLoader.getSystemClassLoader();
            Class c1 = Class.forName(name, true, cLoader);
            Class c2 = cLoader.loadClass(name);
            assertSame("Objects differ", c1, c2);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Simple reference type and array type must be loaded by the same class
     * loader.
     */
    public void test5() {
        try {
            final String name = getClass().getName() + "$InnerHelper";
            Class c1 = Class.forName(name);
            Class c2 = Class.forName("[[L" + name + ";");
            assertSame("Objects differ", c1.getClassLoader(), c2.getClassLoader());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * class names must be delimited by dotes not by any over delimiter.
     */
    public void test6() {
        try {
            final String name = "java/lang/Object";
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            return;
        }
        fail("ClassNotFoundException exception expected");
    }

    /**
     * primitive types can not be loaded by forName() method.
     */
    public void test7() {
        try {
            final String name = Integer.TYPE.getName();
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            return;
        }
        fail("ClassNotFoundException exception expected");
    }

    /**
     * primitive types can not be loaded by forName() method.
     */
    public void test8() {
        try {
            Class c = new int[0].getClass();
            assertSame("Objects differ", c, Class.forName(c.getName()));
        } catch (Exception e) {
            fail(e.toString());
        }
    }
    
    
    /**
     * Regression test for HARMONY-887
     */
    public void testHARMONY887() throws ClassNotFoundException {
        try {
            Class.forName(null, true, ClassLoader.getSystemClassLoader());
        } catch (NullPointerException npe) {return;}
        fail("NullPointerException exception expected");
    }
    
    /**
     * Helper class.
     */
    private static class InnerHelper {
    }
}