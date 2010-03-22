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
 * tested method: getDeclaredConstructors
 */
public class ClassTestGetDeclaredConstructors extends TestCase {

    /**
     * The primitive types don't declare constructors.
     */
    public void test1() {
        Constructor[] cs = Integer.TYPE.getDeclaredConstructors();
        assertNotNull("unexpected null", cs);
        assertEquals("array length:", 0, cs.length);
    }

    /**
     * The arrays don't declare constructors.
     */
    public void test2() {
        Constructor[] cs = new int[0].getClass().getDeclaredConstructors();
        assertNotNull("unexpected null", cs);
        assertEquals("array length:", 0, cs.length);
    }

    /**
     * Only default constructor which takes no parameters must be returned.
     * Constructors of the super class must not be returned.
     */
    public void test3() {
        Constructor[] cs = ClassTestGetDeclaredConstructors.class
            .getDeclaredConstructors();
        assertNotNull("unexpected null", cs);
        assertEquals("array length:", 1, cs.length);
        assertEquals("array length:", 0, cs[0].getParameterTypes().length);
    }

    /**
     * Only default constructor which takes no parameters must be returned.
     * Constructors of the super class must not be returned.
     */
    public void test4() {
        Constructor[] cs = Inner.class
            .getDeclaredConstructors();
        assertNotNull("unexpected null", cs);
        assertEquals("Constructors length:", 1, cs.length);
        assertEquals("Parameters length:", 0, cs[0].getParameterTypes().length);
    }

    /**
     * The getDeclaredConstructors() method must return protected, package
     * private and private constructors.
     */
    public void test5() {
        Constructor[] cs = Inner2.class.getDeclaredConstructors();
        assertEquals("Constructors length:", 2, cs.length);
        Class[] args = cs[0].getParameterTypes();
        if (args.length == 0) {
            args = cs[1].getParameterTypes();
            assertEquals("Assert 0: args length:", 1, args.length);
            assertSame("objects differ:", String.class, args[0]);
        } else {
            assertEquals("Assert 1: args length:", 1, args.length);
            assertSame("objects differ:", String.class, args[0]);
            args = cs[1].getParameterTypes();
            assertEquals("Assert 2: args length:", 0, args.length);
        }
    }

    /**
     * The interfaces can not define constructors.
     */
    public void test6() {
        Constructor[] cs = Inner3.class.getDeclaredConstructors();
        assertNotNull("unexpected null", cs);
        assertEquals("array length:", 0, cs.length);
    }

    static class Inner extends Throwable {
        private static final long serialVersionUID = 0L; 
    }

    public static class Inner2 {

        public Inner2(String s) {
        }

        private Inner2() {
        }
    }

    public static interface Inner3 {
    }
}