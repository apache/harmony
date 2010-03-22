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
 * @author Serguei S.Zapreyev
 * 
 * This MethodTest class ("Software") is furnished under license and may only be
 * used or copied in accordance with the terms of that license.
 *  
 */

package java.lang.reflect;

import junit.framework.TestCase;

/*
 * Created on 01.28.2006
 */

@SuppressWarnings(value={"all"}) public class ConstructorTest extends TestCase {

    /**
     *  
     */
    public void test_equals_Obj() {
        class X {
            public X() {
                return;
            }
        }
        class Y {
            public Y() {
                return;
            }
        }
        try {
            Constructor m1 = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.ConstructorTest.class });
            Constructor m2 = Y.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.ConstructorTest.class });
            assertTrue(
                    "Error1: lack of coincidence of the equal constructors is detected",
                    m1.equals(m1));
            assertTrue(
                    "Error2: coincidence of the unequal constructors is detected",
                    !m1.equals(m2));
        } catch (NoSuchMethodException e) {
            fail("Error3: unfound constructor" + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getDeclaringClass_V() {
        class X {
            public X() {
                return;
            }
        }
        new X();
        try {
            Constructor m = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.ConstructorTest.class });
            assertTrue("Error1", m.getDeclaringClass().getName().equals(
                    "java.lang.reflect.ConstructorTest$2X"));
        } catch (NoSuchMethodException _) {
            fail("Error2");
        }
    }

    /**
     *  
     */
    public void test_getExceptionTypes_V() {
        class X {
            class Y extends Throwable {
                private static final long serialVersionUID = 0L;
            };

            public X() throws Throwable, Y {
                return;
            }

            public X(Y a1) {
                return;
            }
        }
        try {
            Constructor m = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.ConstructorTest.class });
            assertTrue("Error1", (m.getExceptionTypes()[0].getName().equals(
                    "java.lang.reflect.ConstructorTest$3X$Y") || m
                    .getExceptionTypes()[0].getName().equals(
                    "java.lang.Throwable"))
                    && (m.getExceptionTypes()[1].getName().equals(
                            "java.lang.reflect.ConstructorTest$3X$Y") || m
                            .getExceptionTypes()[1].getName().equals(
                            "java.lang.Throwable")));
        } catch (Exception e) {
            fail("Error2" + e.toString());
        }
        try {
            Constructor m = X.class.getDeclaredConstructor(new Class[] {
                    java.lang.reflect.ConstructorTest.class, X.Y.class });
            assertTrue("Error3", m.getExceptionTypes().length == 0);
        } catch (Exception e) {
            fail("Error4" + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getModifiers_V() {
        class X {
            public X() {
                return;
            }
        }
        new X();
        try {
            Constructor m = X.class
                    .getDeclaredConstructor(new Class[] { java.lang.reflect.ConstructorTest.class });
            assertTrue("Error1", java.lang.reflect.Modifier.isPublic(m
                    .getModifiers()));
        } catch (Exception e) {
            fail("Error2" + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getName_V() {
        class X {
            public X() {
                return;
            }

            public X(X a1) {
                return;
            }
        }
        new X();
        Constructor af[] = X.class.getDeclaredConstructors();
        assertEquals("num of ctors", 2, af.length);
        for (int i = 0; i < af.length; i++) {
            assertEquals("name", "java.lang.reflect.ConstructorTest$5X", af[i].getName());
        }
    }

    /**
     *  
     */
    public void test_getParameterTypes_V() {
        class X {
            public X(boolean a1, byte a2, char a3, double a4, float a5, int a6,
                    long a7, short a8, X a9, ConstructorTest a10) {
                return;
            }
        }
        try {
            Class ac[] = X.class.getDeclaredConstructor(
                    new Class[] { java.lang.reflect.ConstructorTest.class,
                            boolean.class, byte.class, char.class,
                            double.class, float.class, int.class, long.class,
                            short.class, X.class, ConstructorTest.class })
                    .getParameterTypes();
            int res = 0;
            for (int i = 0; i < ac.length; i++) {
                if (ac[i].getName().equals("boolean"))
                    res += 1;
                if (ac[i].getName().equals("byte"))
                    res += 10;
                if (ac[i].getName().equals("char"))
                    res += 100;
                if (ac[i].getName().equals("double"))
                    res += 1000;
                if (ac[i].getName().equals("float"))
                    res += 10000;
                if (ac[i].getName().equals("int"))
                    res += 100000;
                if (ac[i].getName().equals("long"))
                    res += 1000000;
                if (ac[i].getName().equals("short"))
                    res += 10000000;
                if (ac[i].getName().equals(
                        "java.lang.reflect.ConstructorTest$6X"))
                    res += 100000000;
                if (ac[i].getName().equals("java.lang.reflect.ConstructorTest"))
                    res += 1000000000;
            }
            assertTrue("Error1", res == 2111111111);
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_hashCode_V() {
        class X {
            public X(X a9) {
                return;
            }
        }
        try {
            Constructor m = X.class.getDeclaredConstructor(new Class[] {
                    java.lang.reflect.ConstructorTest.class, X.class });
            assertTrue("Error1", m.hashCode() == m.getDeclaringClass()
                    .getName().hashCode());
        } catch (NoSuchMethodException _) {
            fail("Error2");
        }
    }

    /**
     *  
     */
    public void test_newInstance_Obj() {
        class X1 {
            public X1() {
                return;
            }

            public X1(X1 a9) {
                return;
            }
        }
        X1 x = new X1(new X1());
        try {
            Constructor m = X1.class.getDeclaredConstructor(new Class[] {
                    java.lang.reflect.ConstructorTest.class, X1.class });
            Object o = m.newInstance(new Object[] {
                    new java.lang.reflect.ConstructorTest(), new X1() });
            assertTrue("Error1", o instanceof X1);
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_toString_Obj() {
        class X {
            public X() {
                return;
            }

            public X(X a9) {
                return;
            }
        }
        try {
            Constructor m = X.class.getDeclaredConstructor(new Class[] {
                    java.lang.reflect.ConstructorTest.class, X.class });
            assertTrue(
                    "Error1 " + m.toString(),
                    m
                            .toString()
                            .equals(
                                    "public java.lang.reflect.ConstructorTest$8X(java.lang.reflect.ConstructorTest,java.lang.reflect.ConstructorTest$8X)"));
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }
}