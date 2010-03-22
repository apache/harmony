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

import java.io.Serializable;
import java.security.Permission;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: isInstance
 */
public class ClassTestIsInstance extends TestCase {

    /**
     * the isInstance() method returns false if argument is null.
     */
    public void test1() {
        assertFalse(Object.class.isInstance(null));
    }

    /**
     * the isInstance() method returns false if this class represetnts primitive
     * type.
     */
    public void test2() {
        assertFalse("Assert 0:", Integer.TYPE.isInstance(new Integer(1)));
        assertFalse("Assert 1:", int.class.isInstance(new Integer(1)));
    }

    /**
     * checks widening refernce conversion.
     */
    public void test3() {
        assertTrue(Object.class.isInstance(new Character('a')));
    }

    /**
     * The Serializable interface is super interface of the Boolean class. So
     * the Boolean class is instance of the Serializable interface.
     */
    public void test4() {
        assertTrue(Serializable.class.isInstance(new Boolean(true)));
    }

    /**
     * Each array has the Object class as its super class.
     */
    public void test5() {
        assertTrue(Object.class.isInstance(new int[0]));
    }

    /**
     * Array of primitive types is not an instance of array of objects.  
     */
    public void test6() {
        assertFalse(new Object[0].getClass().isInstance(new int[0]));
    }

    /**
     * the isInstance() method returns false if this class represetnts an array
     * of primitive types.
     */
    public void test7() {
        assertFalse("Assert 0:", new double[0].getClass().isInstance(new int[0]));
        assertFalse("Assert 1:", new int[0].getClass().isInstance(new double[0]));
    }

    /**
     * checks whether array components can be converted by widening reference
     * conversion.  
     */
    public void test8() {
        assertTrue("Assert 0:", new Object[0].getClass().isInstance(new String[0]));
        assertFalse("Assert 1:", new String[0].getClass().isInstance(new Object[0]));
    }

    /**
     * The Permission class is super class of the RuntimePermission class. 
     */
    public void test9() {
        Object o = new RuntimePermission("createClassLoader");
        assertTrue(Permission.class.isInstance(o));
    }
}