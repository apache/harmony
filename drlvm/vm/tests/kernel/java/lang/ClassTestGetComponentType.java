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
 * tested method: getComponentType
 */
public class ClassTestGetComponentType extends TestCase {

    /**
     * if class is not an array than null value must be returned
     *  
     */
    public void test1() {
        assertNull(Class.class.getComponentType());
    }

    /**
     * checks component type for array of the primitive types
     *  
     */
    public void test2() {
        Class c = new int[0].getClass();
        assertSame(Integer.TYPE, c.getComponentType());
    }

    /**
     * checks component type for array of the reference types
     *  
     */
    public void test3() {
        Class c = new Object[0].getClass();
        assertSame(Object.class, c.getComponentType());
    }

    /**
     * the component of multidimensional array is an array which has one
     * dimensional less.
     *  
     */
    public void test4() {
        try {
            Class c = new int[0][0].getClass();
            assertSame(Class.forName("[I"), c.getComponentType());
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}