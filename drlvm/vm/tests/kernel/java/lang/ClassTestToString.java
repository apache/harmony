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
 * tested method: toString
 */
public class ClassTestToString extends TestCase {

    /**
     * only the class name should be returned in the case of primitive type.  
     */
    public void test1() {
        assertEquals(void.class.toString(), void.class.toString());
    }

    /**
     * The string "interface" should be followed by the class name if this class
     * represents an interface.  
     */
    public void test2() {
        assertEquals("interface " + Cloneable.class.getName(), Cloneable.class
            .toString());
    }

    /**
     * The string "class" should be followed by the class name 
     * if this is a class.
     */
    public void test3() {
        assertEquals("class " + Class.class.getName(), Class.class.toString());
    }

    /**
     * The string "class" should be followed by the class name 
     * if this is a class.
     */
    public void test4() {
        Serializable[] c = new Permission[0]; 
        assertEquals("class " + c.getClass().getName(), c.getClass().toString());
    }
}