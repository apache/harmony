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
 * @author Evgueni V. Brevnov
 */
package java.lang.reflect;

import java.lang.reflect.Method;

import org.apache.harmony.lang.C2;
import org.apache.harmony.lang.ProtectedMethod;
import org.apache.harmony.lang.ProtectedSuccessor;
import org.apache.harmony.test.TestResources;

import junit.framework.TestCase;

@SuppressWarnings(value={"all"}) 
public class MethodTestInvoke extends TestCase {
    
    static class MyException extends RuntimeException {}
    
    public static void doFail() {
        throw new MyException();
    }

    /**
     * Regression test for HARMONY-5179.
     */
    public void testLoaderAccess() throws Throwable {
        ClassLoader l1 = TestResources.createLoader();
        ClassLoader l2 = TestResources.createLoader();
        Class c1 = l1.loadClass("org.apache.harmony.lang.test.resource.DefaultMethodInvoker");
        Class c2 = l2.loadClass("org.apache.harmony.lang.test.resource.DefaultMethodInvoker");
        Method m1 = c1.getMethod("run", new Class[] {Method.class, Object.class, Object[].class}); 
        Method m2 = c2.getMethod("dummy", new Class[] {});
        try {
            m1.setAccessible(true);
            m1.invoke(null, new Object[] {m2, null, null});
            fail("Access to package methods should be checked with runtime packages");
        } catch (InvocationTargetException expected) {
            Throwable cause = expected.getCause();
            assertTrue("bad cause: " + cause, cause instanceof IllegalAccessException);
        }
    }
    
    /**
     * Tests that correct cause of reflection error is provided.
     */
    public void testInvocationTargetExceptionCause() throws Exception {
        try {
            this.getClass().getMethod("doFail").invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue("Unexpected cause: " + cause, 
                    cause instanceof MyException);
        }
    }

    int state = 0;
    
    public void test1() {
        new Inner1().test1();
    }

    public void test2() {
        new Inner2().test2();
    }
    
    public void test3() {
        int status = 0;
        try {
            Method meth = C2.class.getMethod("publicMethod", (Class[])null);
            Field statField = C2.class.getField("stat");
            C2 clazz = new C2(); 
            assertEquals(0, clazz.stat);
            clazz.publicMethod();
            assertEquals(10, clazz.stat);
            meth.invoke(clazz, (Object[])null);
            status = ((Integer) statField.get(clazz)).intValue();
        } catch (Exception e) {
            fail(e.toString());
        }
        assertEquals(20, status);
    }
    
    public void test4() {
        new Inner3().test4();
    }

    public void test5() {
        new Inner4().test5();
    }
    
    /* PRIVATE SECTION */
    
    private void privateMethod() {
        state += 10;
    }

    // helper class for test1
    class Inner1 extends Object {

        void test1() {
            try {
                assertEquals(0, state);
                privateMethod();
                assertEquals(10, state);
                Method method = MethodTestInvoke.class
                    .getDeclaredMethod("privateMethod", (Class[])null);
                method.invoke(MethodTestInvoke.this, (Object[])null);
                assertEquals(20, state);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }
    
    // helper class for test 2
    class Inner2 extends ProtectedMethod {

        public void test2() {
            Inner2 thisInstance = new Inner2();
            ProtectedMethod superInstance = new ProtectedMethod();
            Method m = null;
            try {
                assertEquals(0, status);
                assertEquals(0, thisInstance.status);
                assertEquals(0, superInstance.status);
                m = ProtectedMethod.class.getDeclaredMethod("protectedMethod", new Class[] {});
                m.invoke(thisInstance, (Object[])null);                
            } catch (Exception e) {
                fail(e.toString());
            }
            try {
                assertEquals(0, status);
                assertEquals(10, thisInstance.status);
                assertEquals(0, superInstance.status);
                m.invoke(superInstance, (Object[])null);
            } catch (IllegalAccessException e) {
                assertEquals(0, status);
                assertEquals(10, thisInstance.status);
                assertEquals(0, superInstance.status);
                return;
            } catch (Exception e) {
            }
            fail("IllegalAccessException expected");
        }
    }
    
    // helper class for test4
    class Inner3 {
        
        public void test4() {
            try {
                Method m = ProtectedMethod.class
                    .getDeclaredMethod("protectedMethod", new Class[] {});
                m.invoke(this, (Object[])null);                
            } catch (IllegalArgumentException e) {
                return;
            } catch (Exception e) {
                fail(e.toString());
            }
            fail("IllegalArgumentException expected");
        }
    }
    
    // helper class for test 5
    class Inner4 extends ProtectedSuccessor {

        public void test5() {
            Method m = null;
            try {
                assertEquals(0, status);
                this.protectedMethod();
                assertEquals(10, status);                
                m = ProtectedMethod.class
                    .getDeclaredMethod("protectedMethod", new Class[] {});
                m.invoke(this, (Object[])null);
            } catch (Exception e) {
                fail(e.toString());
            }
            try {
                assertEquals(20, status);                
                ProtectedSuccessor successor = new ProtectedSuccessor();
                m = ProtectedSuccessor.class
                    .getDeclaredMethod("privateMethod", new Class[] {});
                m.invoke(successor, (Object[])null);
            } catch (IllegalAccessException e) {                
            } catch (Exception e) {
                fail(e.toString());
            }
            try {
                assertEquals(20, status);                
                m = ProtectedSuccessor.class
                    .getDeclaredMethod("privateMethod", new Class[] {});
                m.invoke(this, (Object[])null);
            } catch (IllegalAccessException e) {                
            } catch (Exception e) {
                fail(e.toString());
            }
            assertEquals(20, status);                
        }
    }    
}