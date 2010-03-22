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

@SuppressWarnings(value={"all"}) public class MethodTest extends TestCase {

    /**
     *  
     */
    public void test_equals_Obj() {
        class X {
            public int m() {
                return 777;
            }
        }
        class Y {
            public int m() {
                return 777;
            }
        }
        try {
            Method m1 = X.class.getDeclaredMethod("m", (Class[]) null);
            Method m2 = Y.class.getDeclaredMethod("m", (Class[]) null);
            assertEquals("Error1: equal methods should coincide", m1, m1);
            assertTrue("Error2: coincidence of the unequal methods is detected",
                    !m1.equals(m2));
        } catch (NoSuchMethodException _) {
            fail("Error3: unfound method");
        }
    }

    /**
     *  
     */
    public void test_getDeclaringClass_V() {
        class X {
            public int m() {
                return 777;
            }
        }
        new X();
        try {
            Method m = X.class.getDeclaredMethod("m", (Class[]) null);
            assertEquals("Error1", "java.lang.reflect.MethodTest$2X", 
                    m.getDeclaringClass().getName());
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

            public int m() throws Throwable, Y {
                return 777;
            }

            public int m2() {
                return 777;
            }
        }
        new X();
        try {
            Method m = X.class.getDeclaredMethod("m", (Class[]) null);
            assertTrue("Error1", (m.getExceptionTypes()[0].getName().equals(
                    "java.lang.reflect.MethodTest$3X$Y") || m
                    .getExceptionTypes()[0].getName().equals(
                    "java.lang.Throwable"))
                    && (m.getExceptionTypes()[1].getName().equals(
                            "java.lang.reflect.MethodTest$3X$Y") || m
                            .getExceptionTypes()[1].getName().equals(
                            "java.lang.Throwable")));
        } catch (Exception e) {
            fail("Error2" + e.toString());
        }
        try {
            Method m = X.class.getDeclaredMethod("m2", (Class[]) null);
            assertEquals("Error3", 0, m.getExceptionTypes().length);
        } catch (Exception e) {
            fail("Error4" + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getModifiers_V() {
        class X {
            public int m() {
                return 777;
            }

            final int m2() {
                return 777;
            }
        }
        new X();
        try {
            Method m = X.class.getDeclaredMethod("m", (Class[]) null);
            assertTrue("Error1", java.lang.reflect.Modifier.isPublic(m
                    .getModifiers()));
            m = X.class.getDeclaredMethod("m2", (Class[]) null);
            assertTrue("Error2", java.lang.reflect.Modifier.isFinal(m
                    .getModifiers()));
        } catch (Exception e) {
            fail("Error3" + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getName_V() {
        class X {
            public int first() {
                return 777;
            }

            final int second() {
                return 777;
            }
        }
        new X();
        try {
            Method af[] = X.class.getDeclaredMethods();
            int res = 0;
            for (int i = 0; i < af.length; i++) {
                if (af[i].getName().equals("first")
                        || af[i].getName().equals("second"))
                    res++;
            }
            assertTrue("Error1", res == 2);
        } catch (Exception e) {
            fail("Error2" + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getParameterTypes_V() {
        class X {
            public int m(boolean a1, byte a2, char a3, double a4, float a5,
                    int a6, long a7, short a8, X a9, MethodTest a10) {
                return 777;
            }
        }
        new X();
        try {
            Class ac[] = X.class.getDeclaredMethod(
                    "m",
                    new Class[] { boolean.class, byte.class, char.class,
                            double.class, float.class, int.class, long.class,
                            short.class, X.class, MethodTest.class })
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
                if (ac[i].getName().equals("java.lang.reflect.MethodTest$6X"))
                    res += 100000000;
                if (ac[i].getName().equals("java.lang.reflect.MethodTest"))
                    res += 1000000000;
            }
            assertEquals("Error1", 1111111111, res);
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_getReturnType_V() {
        class X {
            public X m(X a9) {
                return a9;
            }
        }
        new X();
        try {
            assertEquals("Error1", 
                    "java.lang.reflect.MethodTest$7X", 
                    X.class.getDeclaredMethod("m", new Class[] { X.class })
                    .getReturnType().getName());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_hashCode_V() {
        class X {
            public X first(X a9) {
                return a9;
            }
        }
        try {
            Method m = X.class.getDeclaredMethod("first",
                    new Class[] { X.class });
            assertEquals("Error1", m.getDeclaringClass().getName().hashCode()
                    ^ m.getName().hashCode(), m.hashCode());
        } catch (NoSuchMethodException _) {
            fail("Error2");
        }
    }

    /**
     *  
     */
    public void test_invoke_Obj_Obj() {

        class X {
            public X first(X a9) {
                return a9;
            }
        }
        X x = new X();
        try {
            Method m = X.class.getDeclaredMethod("first",
                    new Class[] { X.class });
            Object o = m.invoke(x, new Object[] { new X() });
            assertTrue("Error1", o instanceof X);
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /**
     *  
     */
    public void test_invoke_Obj_Obj_2() {
        int sz = 500;
        Object obj = null;
        Class cls = null;
        Method m;
        try {         
            cls = Class.forName("java.lang.reflect.AuxiliaryClass");
            obj = cls.newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed during class creation: Unexpected error: " + e);
        }       
        int pp = 0;
        for (int j = 0; j < sz; j++) {
            try {
                m = cls.getMethod("get", (Class[])null);
                int ans = ((Integer) (m.invoke(obj, (Object[])null))).intValue();
                fail("Expected InvocationTargetException was not thrown: ans = "
                	+ ans + " step: " + j + " method name: "
                    + m.getDeclaringClass().getName() + "." + m.getName());
            } catch (InvocationTargetException e) {
                // expected
            } catch (Throwable e) {
                e.printStackTrace();
                fail("Test failed: Unexpected error: " + e);
            }
        }
    }

    /**
     *  
     */
    public void test_toString_Obj() {
        class X {
            public X first(X a9) {
                return a9;
            }
        }
        new X();
        try {
            Method m = X.class.getDeclaredMethod("first",
                    new Class[] { X.class });
            assertEquals("Error1 ",
                    "public java.lang.reflect.MethodTest$10X " +
                        "java.lang.reflect.MethodTest$10X.first(" +
                        "java.lang.reflect.MethodTest$10X)",
                    m.toString());
        } catch (Exception e) {
            fail("Error2: " + e.toString());
        }
    }

    /*
     * Commented out until HARMONY-5790 is properly resolved
     *
    interface GenericSample1 {
        public <T extends GenericSample1, E extends Throwable> T test(T param) throws E;
    }

    public void test_getGeneric() throws Exception {
        // Regression for HARMONY-5622, HARMONY-5790
        Method method = GenericSample1.class.getMethods()[0];
        for (int i = 0; i < 5; i++) {
            switch (i) {
            case 0: method.getGenericParameterTypes();
                break;
            case 1: method.getGenericReturnType();
                break;
            case 2: method.getGenericExceptionTypes();
                break;
            case 3: method.getTypeParameters();
                break;
            case 4: method.toGenericString();
                break;
            }
        }
    }

    interface GenericSample2 {
        public <T extends GenericSample2, E extends Throwable> T test(T param) throws E;
    }

    boolean success;

    public void test_getGenericThread() throws Exception {
        // Regression for HARMONY-5622, HARMONY-5790
        success = false;
        Thread thread = new Thread() {
            public void run() {
                try { // Using separate thread to avoid stack overflow
                    Method method = GenericSample2.class.getMethods()[0];
                    for (int i = 0; i < 5; i++) {
                        switch (i) {
                        case 0: method.getGenericParameterTypes();
                            break;
                        case 1: method.getGenericReturnType();
                            break;
                        case 2: method.getGenericExceptionTypes();
                            break;
                        case 3: method.getTypeParameters();
                            break;
                        case 4: method.toGenericString();
                            break;
                        }
                    }
                    success = true;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        thread.join();
        assert(success);
    }
    */
}
