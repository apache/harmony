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
 * @author Evgueni V. Brevnov
 */
package java.lang.reflect;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Constructor
 * tested method: newInstance
 * 
 */
public class ConstructorTestNewInstace extends TestCase {

    /**
     * Attempt to create a class with a protected constructor 
     * from another package.
     * This situation must produce IllegalAccessException.
     */
    public void test1() {
        try {
            Constructor c = java.lang.ProtectedConstructor.class
                    .getDeclaredConstructor((Class[]) null);
            c.newInstance((Object[]) null);
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e instanceof IllegalAccessException);
        }
    }

    /**
     * Attempt to create a class with a protected constructor 
     * from another package.
     * Now we've changed the constructor accessiblity before creating. 
     * This situation must not produce any exception.
     */
    public void test2() {
        try {
            Constructor c = java.lang.ProtectedConstructor.class
                    .getDeclaredConstructor((Class[]) null);
            c.setAccessible(true);
            c.newInstance((Object[]) null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Attempt to create a class with a protected constructor 
     * in the same package.
     * This situation must not produce any exception.
     */
    public void test3() {
        try {
            Constructor c = java.lang.reflect.ProtectedConstructor.class
                    .getDeclaredConstructor((Class[]) null);
            c.newInstance((Object[]) null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Attempt to create an inner class which is package accessible. 
     * This situation must produce IllegalAccessException.
     */
    public void test4() {
        try {
            Object o = java.lang.PackageAccessible
                    .getProtectedClassInstance();
            Constructor c = o.getClass().getDeclaredConstructor((Class[]) null);
            c.newInstance((Object[]) null);
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e instanceof IllegalAccessException);
        }
    }

    /**
     * Attempt to create an inner class which is package accessible. 
     * Now we've changed the class accessiblity. 
     * This situation must not produce any exception.
     */
    public void test5() {
        try {
            Object o = java.lang.PackageAccessible
                    .getProtectedClassInstance();
            Constructor c = o.getClass().getDeclaredConstructor((Class[]) null);
            c.setAccessible(true);
            c.newInstance((Object[]) null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void test6() {
        try {
            A.class.getDeclaredConstructor((Class[]) null).newInstance(
                    (Object[]) null);
        } catch (InvocationTargetException e) {
            return;
        } catch (Exception e) {
        }
        fail("The InvocationTargetException exception expected");
    }

    static class A {

        public A() {
            throw new NullPointerException();
        }
    }
}