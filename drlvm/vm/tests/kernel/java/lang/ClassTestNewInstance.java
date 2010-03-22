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
 * tested method: newInstance
 */
public class ClassTestNewInstance extends TestCase {

    /**
     * Attempt to create an abstract class. InstantiationException exception
     * must be thrown in this case.
     */
    public void test1() {
        try {
            ClassTestNewInstance.AbstractClassPublicConstructor.class
                .newInstance();
        } catch (InstantiationException e) {
            return;
        } catch (Exception e) {
        }
        fail("InstantiationException exception expected");
    }

    /**
     * Attempt to create a class with a private constructor.
     * IllegalAccessException must be thrown in this case.
     */
    public void test2() {
        try {
            java.lang.PrivateConstructor.class.newInstance();
        } catch (IllegalAccessException e) {
            return;
        } catch (Exception e) {
        }
        fail("IllegalAccessException exception expected");
    }

    /**
     * Attempt to create aclass which throws an exception.
     * InstantiationException must be thrown in this case.
     */
    public void test3() {
        try {
            ClassTestNewInstance.ExceptionThrowner.class.newInstance();
        } catch (InstantiationException e) {
            return;
        } catch (Exception e) {
        }
        fail("InstantiationException exception expected");
    }

    /**
     * This is normal use of newInstance method. No exceptions must be thrown.
     * The reated object must have correct type.
     */
    public void test4() {
        try {
            Object o = ClassTestNewInstance.class.newInstance();
            assertTrue("Wrong type", o instanceof ClassTestNewInstance);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Attempt to instantiate an object of the primitive type.
     */
    public void test5() {
        try {
            Character.TYPE.newInstance();
        } catch (InstantiationException e) {
            return;
        } catch (Exception e) {
        }
        fail("InstantiationException exception expected");
    }

    /**
     * Attempt to instantiate an object of the primitive type.
     */
    public void test6() {
        try {
            new int[0].getClass().newInstance();
        } catch (InstantiationException e) {
            return;
        } catch (Exception e) {
        }
        fail("InstantiationException exception expected");
    }
    
    public void testBug521() {
        try {
            Class cls = Class.forName(Inner.class.getName());
            cls.newInstance();
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Helper inner class.
     */
    private abstract class AbstractClassPublicConstructor {

        public AbstractClassPublicConstructor() {
        }
    }

    /**
     * Helper inner class.
     */
    private class ExceptionThrowner {

        public ExceptionThrowner() throws Exception {
            throw new Exception("This class cannot be instatiated");
        }
    }
    
    static class Inner {}
}