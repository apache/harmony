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
 * tested method: getName
 */
public class ClassTestGetName extends TestCase {

    /**
     * Check the name of the primitive type.
     */
    public void test1() {
        final String name = Character.TYPE.getName();
        assertEquals("char", name);
    }
    
    /**
     * Check the name of the void type.
     */
    public void test2() {
        final String name = Void.TYPE.getName();
        assertEquals("void", name);
    }

    /**
     * Check the name of the primitive type.
     */
    public void test3() {
        final String name = boolean.class.getName();
        assertEquals("boolean", name);
    }

    /**
     * Check the name of the reference type.
     */
    public void test4() {
        final String name = Boolean.class.getName();
        assertEquals("java.lang.Boolean", name);
    }

    /**
     * Check the name of an array of the primitive types.
     */
    public void test5() {
        final String name = new int[0][0].getClass().getName();
        assertEquals("[[I", name);
    }

    /**
     * Check the name of an array of the reference types.
     */
    public void test6() {
        final String name = new Boolean[0][0].getClass().getName();
        assertEquals("[[Ljava.lang.Boolean;", name);
    }
}
