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
 * tested method: isArray
 */
public class ClassTestIsArray extends TestCase {

    /**
     * The Object class does not represent array.
     */
    public void test1() {
        assertFalse(Object.class.isArray());
    }

    /**
     * The primitive class does not represent array.
     */
    public void test2() {
        assertFalse(Character.TYPE.isArray());
    }

    /**
     * The interface does not represent array.
     */
    public void test3() {
        assertFalse(Cloneable.class.isArray());
    }

    /**
     * Array of interfaces.
     */
    public void test4() {
        assertTrue(new Cloneable[0].getClass().isArray());
    }

    public void test5() {
        assertTrue(new int[0].getClass().isArray());
    }

    /**
     * Array of primitive types.
     */
    public void test6() {
        try {
            assertTrue(Class.forName("[[I").isArray());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Array of recference types.
     */
    public void test7() {
        try {
            assertTrue(Class.forName("[Ljava.lang.Object;").isArray());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

}