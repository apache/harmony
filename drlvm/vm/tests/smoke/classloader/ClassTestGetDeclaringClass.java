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
 * @author Pavel Pervov
 */  
package classloader;
/**
 * @tested class: java.lang.Class
 * @tested method: getDeclaringClass
 */
public class ClassTestGetDeclaringClass {
    
    /**
     * Declaring class of the primitive types should be null. 
     *
     */
    public void test1() {
        Class c = Integer.TYPE.getDeclaringClass();
        assertNull(c, "Integer.TYPE.getDeclaringClass() is not null");
    }
    
    /**
     * Declaring class of the arrays should be null. 
     *
     */
    public void test2() {
        Class c = new int[0].getClass().getDeclaringClass();
        assertNull(c, "int[].getClass().getDeclaringClass() is not null");
    }

    /**
     * The chain of inner classes and interfaces.
     *
     */
    public void test3 () {
        Class c = Inner1.Inner2.Inner3.class.getDeclaringClass();
        assertSame(c, Inner1.Inner2.class,
            "i1.i2.i3.class.getDeclaringClass() != i1.i2.class");
        c = Inner1.Inner2.class.getDeclaringClass();
        assertSame(c, Inner1.class,
            "i1.i2.class.getDeclaringClass() != i1.class");
        c = Inner1.class.getDeclaringClass();
        assertSame(c, getClass(),
            "i1.class.getDeclaringClass() != getClass()");
        c = ClassTestGetDeclaringClass.class.getDeclaringClass();
        assertNull(c, "class.getDeclaringClass() != null");        
    }

    private interface Inner1 {
        public interface Inner2 { 
            class Inner3 {                
            }
        }
    }

    public static void main (String[] args) {
        ClassTestGetDeclaringClass self = new ClassTestGetDeclaringClass();
        System.out.println("test1");
        self.test1();
        System.out.println("test2");
        self.test2();
        System.out.println("test3");
        self.test3();
        if (self.passed) 
            System.out.println("PASSED");
    }

    boolean passed = true;

    public void assertNull (Object x, Object message) {
        if (x != null) {
            passed = false;
            System.out.println("FAILED: " + message + ".");
            System.out.println("Got " + x + " instead of null");
        }
    }

    public void assertSame (Object x, Object y, Object message) {
        if (x != y) {
            passed = false;
            System.out.println("FAILED: " + message + ".");
            System.out.println("Got " + x + " instead of " + y);
        }
    }
}
