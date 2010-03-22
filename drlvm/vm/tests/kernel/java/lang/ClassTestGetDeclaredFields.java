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

import java.lang.reflect.Field;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getDeclaredFields
 */
@SuppressWarnings(value={"all"}) public class ClassTestGetDeclaredFields extends TestCase {

    private String s;
    
    /**
     * Void.TYPE class does not declare methods members.
     *
     */
    public void test1() {
        Field[] fs = Void.TYPE.getDeclaredFields();
        assertNotNull("null expected", fs);
        assertEquals("array length:", 0, fs.length);
    }

    /**
     * Arrays do not declare field members.
     *
     */
    public void test2() {
        Field[] fs = new int[0].getClass().getDeclaredFields();
        assertNotNull("null expected", fs);
        assertEquals("array length:", 0, fs.length);
    }

    /**
     * Fields of the super class should not be reflected.
     *
     */
    public void test3() {
        Field[] fs = getClass().getDeclaredFields();
        assertNotNull("null expected", fs);
        assertEquals("array length:", 1, fs.length);
        assertEquals("incorrect name", "s", fs[0].getName());
        assertSame("objects differ", getClass(), fs[0].getDeclaringClass());
    }
}