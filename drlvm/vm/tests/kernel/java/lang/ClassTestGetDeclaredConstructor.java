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

import java.lang.reflect.Constructor;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getDeclaredConstructor
 */
public class ClassTestGetDeclaredConstructor extends TestCase {

    /**
     * Public constructor which takes an integer parameter must be reflected.
     */
    public void test1() {
        try {
            Constructor c = Integer.class
                .getDeclaredConstructor(new Class[] { Integer.TYPE });
            assertNotNull("unexpected null", c);
            assertSame("objects differ:", 
                       Integer.TYPE, c.getParameterTypes()[0]);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Verify parametrized type
     */
    public void test1_java5() {
        try {
            Constructor<Integer> c = Integer.class
                .getDeclaredConstructor(new Class[] { Integer.TYPE });
            assertNotNull("unexpected null", c);
            assertSame("objects differ:", 
                       Integer.TYPE, c.getParameterTypes()[0]);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Only default constructor which takes no parameters must be returned.
     */
    public void test2() {
        try {
            Constructor c = Inner.class.getDeclaredConstructor(new Class[0]);
            assertNotNull("unexpected null", c);
            assertEquals("array length:", 0, c.getParameterTypes().length);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Public constructors of the super class must not be returned.
     */
    public void test3() {
        try {
            Inner.class.getDeclaredConstructor(new Class[] { String.class });
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");

    }

    /**
     * Private constructor which takes no parameters must be reflected.
     */
    public void test4() {
        try {
            Constructor c = Inner2.class.getDeclaredConstructor((Class[]) null);
            assertNotNull("unexpected null", c);
            assertEquals("array length:", 0, c.getParameterTypes().length);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Private constructor which takes no parameters must be reflected.
     */
    public void test5() {
        try {
            Constructor c = PrivateConstructor.class
                .getDeclaredConstructor((Class[]) null);
            assertNotNull("unexpected null", c);
            assertEquals("array length:", 0, c.getParameterTypes().length);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    private static class Inner extends Throwable {
        private static final long serialVersionUID = 0L; 
    }

    public static class Inner2 {

        private Inner2() {
        }
    }

}