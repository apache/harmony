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
 * tested method: getConstructor
 */
public class ClassTestGetConstructor extends TestCase {

    /**
     * The java.lang.Class class has no public constructotrs.
     * NoSuchMethodException exception must be thrown.
     *  
     */
    public void test1() {
        try {
            Class.class.getConstructor(new Class [] {null});
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * Public constructor which takes an integer parameter must be reflected.
     *  
     */
    public void test2() {
        try {
            Constructor c = Integer.class
                    .getConstructor(new Class[] {Integer.TYPE});
            assertNotNull("Unexpected null", c);
            assertSame("Objects differ", Integer.TYPE, c.getParameterTypes()[0]);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Public constructor which takes a parameter of the java.lang.String type
     * must be reflected.
     *  
     */
    public void test3() {
        try {
            Constructor c = Integer.class
                    .getConstructor(new Class[] {String.class});
            assertNotNull("Unexpected null", c);
            assertSame("Objects differ", String.class, c.getParameterTypes()[0]);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Verify parametrized type
     */
    public void test3_java5() {
        try {
            Constructor<Integer> c = Integer.class
                    .getConstructor(new Class[] {String.class});
            assertNotNull("Unexpected null", c);
            assertSame("Objects differ", String.class, c.getParameterTypes()[0]);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Default constructor which takes no parameters must be returned.
     *  
     */
    public void test4() {
        try {
            Constructor c = getClass().getConstructor(new Class[0]);
            assertNotNull("Unexpected null", c);
            assertEquals("array length:", 0, c.getParameterTypes().length);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Default constructor which takes no parameters must be returned.
     *  
     */
    public void test5() {
        try {
            Constructor c = Inner.class.getConstructor(new Class[0]);
            assertNotNull("Unexpected null", c);
            assertEquals("array length:", 0, c.getParameterTypes().length);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Public constructors of the super class must not be returned.
     *  
     */
    public void test6() {
        try {
            Inner.class.getConstructor(new Class[] {String.class});
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }

    /**
     * Only public constructors must be returned.
     *
     */
    public void test7() {
        try {
            Inner2.class.getConstructor(new Class[] {null});
        } catch (NoSuchMethodException e) {
            return;
        }
        fail("NoSuchMethodException exception expected");
    }
    
    public static class Inner extends Throwable {
        private static final long serialVersionUID = 0L;
    }

    private static class Inner2 {

        Inner2() {
        }
    }
}