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

import java.lang.reflect.Modifier;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getModifiers
 */
public class ClassTestGetModifiers extends TestCase {

    /**
     * If the class represents a primitive type, then its public and final
     * modifiers should be on as well as its protected, private and interface
     * modifiers should be off.
     */
    public void test1() {
        int mod = Byte.TYPE.getModifiers();
        assertTrue("public:", Modifier.isPublic(mod));
        assertTrue("final:", Modifier.isFinal(mod));
        assertFalse("protected", Modifier.isProtected(mod));
        assertFalse("private", Modifier.isPrivate(mod));
        assertFalse("interface", Modifier.isInterface(mod));
    }

    /**
     * If the class represents an array, then its final modifier should be on
     * and its interface modifier should be off. Whether an array class has
     * public, protected, package private or private modifier determines by the
     * modifier of its component type.
     */
    public void test2() {
        int mod = new int[0].getClass().getModifiers();
        assertTrue("public:", Modifier.isPublic(mod));
        assertTrue("final:", Modifier.isFinal(mod));
        assertFalse("interface", Modifier.isInterface(mod));
    }

    /**
     * If the class represents an array, then its final modifier should be on
     * and its interface modifier should be off. Whether an array class has
     * public, protected, package private or private modifier determines by the
     * modifier of its component type.
     */
    public void test3() {
        int mod = new I[0].getClass().getModifiers();
        assertTrue("private", Modifier.isPrivate(mod));
        assertTrue("final", Modifier.isFinal(mod));
        assertFalse("interface", Modifier.isInterface(mod));
    }

    /**
     * An interface should always has abstract and interface modifiers on.
     */
    public void test4() {
        int mod = I.class.getModifiers();
        assertTrue("private", Modifier.isPrivate(mod));
        assertTrue("interface", Modifier.isInterface(mod));
        assertTrue("abstract", Modifier.isAbstract(mod));
    }

    /**
     * Checks whether a Boolean class is public and final.
     */
    public void test5() {
        int mod = Boolean.class.getModifiers();
        assertEquals("should be public final", 
                Modifier.PUBLIC | Modifier.FINAL, mod);
    }

    /**
     * Checks whether a ClassLoader class is public and abstract.
     */
    public void test6() {
        int mod = ClassLoader.class.getModifiers();
        assertEquals("should be public abstract",
                Modifier.PUBLIC | Modifier.ABSTRACT, mod);
    }

    private interface I {
    }
}