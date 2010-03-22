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

import java.util.Arrays;
import java.util.HashSet;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getDeclaredClasses
 */
public class ClassTestGetDeclaredClasses extends TestCase {

    /**
     * The getDeclaredClasses() method must return all inner classes and
     * interfaces including protected, package private and private members.
     *  
     */
    public void test1() {
        Class[] cs = ClassTestGetDeclaredClasses.class.getDeclaredClasses();
        HashSet<Class> set = new HashSet<Class>(Arrays.asList(cs));
        assertTrue("Helper1 is not in the list", set.contains(Helper1.class));
        assertTrue("Helper2 is not in the list", set.contains(Helper2.class));
        assertTrue("Helper3 is not in the list", set.contains(Helper3.class));
        assertTrue("Helper4 is not in the list", set.contains(Helper4.class));
        assertFalse("Helper1.Inner1 is not in the list", 
                    set.contains(Helper1.Inner1.class));
    }

    /**
     * The members declared in the super class must not be reflected by
     * getDeclaredClasses() method.
     *  
     */
    public void test2() {
        Class[] cs = Helper3.class.getDeclaredClasses();
        assertNotNull("List of classes should not be null", cs);
        assertEquals("There should be one class in the list", 1, cs.length);
        assertSame("Incorrect class returned", Helper3.Inner2.class, cs[0]);
    }

    /**
     * An empty array must be returned for the classes that represent
     * primitive types.
     *  
     */
    public void test3() {
        Class[] cs = Void.TYPE.getDeclaredClasses();
        assertNotNull("Array should not be null");
        assertEquals("Array must be empty", 0, cs.length);
    }

    /**
     * An empty array must be returned for classes that represent arrays.
     *  
     */
    public void test4() {
        Class[] cs = new Object[0].getClass().getDeclaredClasses();
        assertNotNull("Array should not be null", cs);
        assertEquals("Array must be empty", 0, cs.length);
    }

    /**
     * Should not include non-member inner classes 
     * (anonymous and local).
     */
    public void testAnonymousLocal() {
        Class[] cs = this.getClass().getDeclaredClasses();
        for (Class c: cs) {
            assertFalse("anonymous " + c, c.isAnonymousClass());
            assertFalse("local " + c.toString(), c.isLocalClass());
        }
    }

    public class Helper1 {
        class Inner1 {
        }
    }

    protected interface Helper2 {
    }

    class Helper3 extends Helper1 {
        class Inner2 {
        }
    }

    private class Helper4 {
    }
    
    static Object o1 = new Object() {};
    static void m1() {
        class CC{}
    }
    Object m2() {
        return new Object() {};
    }
}
