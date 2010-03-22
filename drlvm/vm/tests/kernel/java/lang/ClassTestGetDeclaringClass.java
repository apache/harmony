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
 * tested method: getDeclaringClass
 */
public class ClassTestGetDeclaringClass extends TestCase {
    
    /**
     * Declaring class of the primitive types should be null. 
     *
     */
    public void test1() {
        Class c = Integer.TYPE.getDeclaringClass();
        assertNull("null expected", c);
    }
    
    /**
     * Declaring class of the arrays should be null. 
     *
     */
    public void test2() {
        Class c = new int[0].getClass().getDeclaringClass();
        assertNull("null expected", c);
    }

    /**
     * The chain of inner classes and interfaces.
     *
     */
    public void test3 () {
        Class c = Inner1.Inner2.Inner3.class.getDeclaringClass();
        assertSame("objects differ", Inner1.Inner2.class, c);
        c = c.getDeclaringClass();
        assertSame("objects differ", Inner1.class, c);
        c = c.getDeclaringClass();
        assertSame("objects differ", getClass(), c);
        c = c.getDeclaringClass();
        assertNull("null expected", c);        
    }

    private interface Inner1 {
        public interface Inner2 { 
            class Inner3 {                
            }
        }
    }
}
