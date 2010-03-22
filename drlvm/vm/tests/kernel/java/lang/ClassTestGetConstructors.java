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

import java.lang.reflect.Constructor;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getConstructors
 */
public class ClassTestGetConstructors extends TestCase {

    /**
     * The java.lang.Class class has no public constructotrs.
     */
    public void test1() {
        Constructor[] cs = Class.class.getConstructors();
        assertNotNull("Unexpected null", cs);
        assertEquals("array length:", 0, cs.length);
    }

    /**
     * The primitive types don't declare public constructors.
     */
    public void test2() {
        Constructor[] cs = Integer.TYPE.getConstructors();
        assertNotNull("Unexpected null", cs);
        assertEquals("array length:", 0, cs.length);
    }

    /**
     * The arrays don't declare public constructors.
     */
    public void test3() {
        Constructor[] cs = new int[0].getClass().getConstructors();
        assertNotNull("Unexpected null", cs);
        assertEquals("array length:", 0, cs.length);
    }

    /**
     * The java.lang.Integer class must have two public constructors. One takes
     * integer parameter another takes parameter of the java.lang.String type.
     */
    public void test4() {
        Constructor[] cs = Integer.class.getConstructors();
        assertEquals("Assert 0: array length:", 2, cs.length);
        Class[] args = cs[0].getParameterTypes();
        assertEquals("Assert 1: array length:", 1, args.length);
        if (Integer.TYPE == args[0]) {
            args = cs[1].getParameterTypes();
            assertEquals("Assert 2: array length:", 1, args.length);
            assertSame("Assert 3: Objects differ:", String.class, args[0]);
        } else {
            assertSame("Assert 4: Objects differ:", String.class, args[0]);
            args = cs[1].getParameterTypes();
            assertEquals("Assert 5: array length:", 1, args.length);
            assertSame("Assert 6: Objects differ:", Integer.TYPE, args[0]);
        }
    }

    /**
     * The getConstructors() method must not return public constructors of the
     * super class. Default constructor which takes no parameters must be
     * returned.
     */
    public void test5() {
        Constructor[] cs = getClass().getConstructors();
        assertNotNull("Unexpected null", cs);
        assertEquals("Assert 0: array length:", 1, cs.length);
        assertEquals("Assert 1: array length:", 0, cs[0].getParameterTypes().length);
    }

    /**
     * The getConstructors() method must not return public constructors of the
     * super class. Default constructor which takes no parameters must be
     * returned.
     */
    public void test6() {
        Constructor[] cs = Inner.class.getConstructors();
        assertNotNull("Unexpected null", cs);
        assertEquals("Assert 0: array length:", 1, cs.length);
        assertEquals("Assert 1: array length:", 0, cs[0].getParameterTypes().length);
    }

    public static class Inner extends Throwable {
        private static final long serialVersionUID = 0L;
    }
}