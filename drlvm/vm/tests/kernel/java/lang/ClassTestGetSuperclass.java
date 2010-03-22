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
 * tested method: getSuperclass
 */
public class ClassTestGetSuperclass extends TestCase {

    /**
     * Super class of the Object class is null.
     */
    public void test1() {
        Class c = Object.class.getSuperclass();
        assertNull("null expected", c);
    }

    /**
     * Super class of the primitive class is null.
     */
    public void test2() {
        Class c = Character.TYPE.getSuperclass();
        assertNull("null expected", c);
    }

    /**
     * Super class of the interface is null.
     */
    public void test3() {
        Class c = Cloneable.class.getSuperclass();
        assertNull("null expected", c);
    }

    /**
     * Super class of the array class is the Object class.
     */
    public void test4() {
        Class c = new Cloneable[0].getClass().getSuperclass();
        assertSame("objects differ", Object.class, c);
    }

    /**
     * Verify parametrized type.
     */
    public void test4_java5() {
        Class<? super java.io.FileReader> c = java.io.InputStreamReader.class.getSuperclass();
        assertSame("objects differ", java.io.Reader.class, c);
    }

    /**
     * Super class of the this class is the TestCase class.
     */
    public void test5() {
        Class c = getClass().getSuperclass();
        assertSame("objects differ", TestCase.class, c);
    }
}