/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.beans.tests.java.beans;

import java.beans.DefaultPersistenceDelegate;
import java.beans.Statement;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.harmony.beans.tests.support.SampleException;
import org.apache.harmony.beans.tests.support.TInspectorCluster;
import org.apache.harmony.beans.tests.support.TInspectorCluster.Ancestor;
import org.apache.harmony.beans.tests.support.TInspectorCluster.BooleanInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.CharacterInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.DoubleInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.FloatInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.IntegerInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.LongInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.ObjectInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.ObjectListInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.Offspring;
import org.apache.harmony.beans.tests.support.TInspectorCluster.ShortInspector;
import org.apache.harmony.beans.tests.support.TInspectorCluster.StringInspector;

/**
 * Test the class java.beans.Statement.
 */
public class StatementTest extends TestCase {

    private static int testId = -1;

    /**
     * The test checks the method execute() for setter
     */
    public void testSetter() throws Exception {
        Bean bean = new Bean();
        Statement s = new Statement(bean, "setText", new Object[] { "hello" });
        s.execute();
        assertEquals("hello", bean.getText());
    }

    /**
     * The test checks the method execute() for indexed setter
     */
    public void testIndexedSetter() throws Exception {
        Bean bean = new Bean("hello");
        Statement s = new Statement(bean, "setChar", new Object[] {
                new Integer(1), new Character('a') });
        s.execute();
        assertEquals("hallo", bean.getText());
    }

    /**
     * The test checks the method execute() for array setter
     */
    public void testArraySetter() throws Exception {
        int[] a = { 1, 2, 3 };
        Statement s = new Statement(a, "set", new Object[] { new Integer(1),
                new Integer(7) });
        s.execute();
        assertEquals(7, a[1]);
    }

    /**
     * The test checks the method execute() for static method
     */
    public void testStatic() throws Exception {
        int currentId = getTestId();
        Statement s = new Statement(StatementTest.class, "nextTestId",
                new Object[] {});
        s.execute();
        assertEquals(++currentId, getTestId());
    }

    /**
     * The test checks the method execute() if exception is thrown on method
     * call
     */
    public void testExceptionThrownOnMethodCall() {
        Bean bean = new Bean("hello");
        Statement s = new Statement(bean, "setChar", new Object[] {
                new Integer(5), new Character('a') });

        try {
            s.execute();
            fail("Exception must be thrown while Bean.setChar(5, 'a') "
                    + "invocation.");
        } catch (Exception e) {
            // correct situation
        }
    }

    /**
     * The test checks the method execute() if exception is thrown on static
     * method call
     */
    public void testExceptionThrownOnStaticMethodCall() throws Exception {
        Statement s = new Statement(StatementTest.class, "methodWithException",
                new Object[] {});

        try {
            s.execute();
            fail("Exception must be thrown with methodWithException call");
        } catch (SampleException se) {
            // SampleException is thrown as expected
        }
    }

    /**
     * The test checks the method execute() with array as parameter
     */
    public void testMethodWithArrayParam() throws Exception {
        Statement s = new Statement(StatementTest.class, "methodWithIntArray",
                new Object[] { new int[] { 3 } });
        s.execute();
    }

    /**
     * 
     */
    public static int getTestId() {
        return testId;
    }

    /**
     * 
     */
    public static void nextTestId() {
        ++testId;
    }

    /**
     * 
     */
    public static void methodWithException() throws Exception {
        throw new SampleException("sample");
    }

    /**
     * 
     */
    public static void methodWithIntArray(int[] array) {
    }

    /**
     * 
     */
    public static Test suite() {
        return new TestSuite(StatementTest.class);
    }

    /**
     * 
     */
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public class Bean {

        private String text;

        public Bean() {
            text = null;
        }

        public Bean(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public char getChar(int idx) throws IllegalAccessException {
            if (text == null) {
                throw new IllegalAccessException("Text property is null.");
            }
            return text.charAt(idx);
        }

        public void setChar(int idx, char value) throws IllegalAccessException {
            if (text == null) {
                throw new IllegalAccessException("Text property is null.");
            }
            // IndexOutOfBounds exception is thrown if indexed bounds
            // are violated
            StringBuffer sb = new StringBuffer(text.length());

            if (idx < 0 || idx >= text.length()) {
                throw new IndexOutOfBoundsException();
            }

            if (idx > 0) {
                sb.append(text.substring(0, idx));
            }
            sb.append(value);

            if (idx < (text.length() - 1)) {
                sb.append(text.substring(idx + 1));
            }

            text = sb.toString();
        }
    }

    /*
     * Test the constructor under normal conditions.
     */
    public void testConstructor_Normal() {
        Object arg1 = new Object();
        Object arg2 = "string";
        Object[] oa = new Object[] { arg1, arg2 };
        Statement t = new Statement(arg1, "method", oa);
        assertSame(arg1, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg1, t.getArguments()[0]);
        assertSame(arg2, t.getArguments()[1]);
        assertEquals("Object.method(Object, \"string\");", t.toString());
    }

    /*
     * Test the constructor with null target.
     */
    public void testConstructor_NullTarget() {
        Object arg = new Object();
        Object[] oa = new Object[] { arg };
        Statement t = new Statement(null, "method", oa);
        assertSame(null, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg, t.getArguments()[0]);
        assertEquals("null.method(Object);", t.toString());
    }

    /*
     * Test the constructor with an array target.
     */
    public void testConstructor_ArrayTarget() {
        Object arg = new Object();
        Object[] oa = new Object[] { arg };
        Statement t = new Statement(oa, "method", oa);
        assertSame(oa, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg, t.getArguments()[0]);
        assertEquals("ObjectArray.method(Object);", t.toString());
    }

    /*
     * Test the constructor with null method name.
     */
    public void testConstructor_NullMethodName() {
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Statement t = new Statement(target, null, oa);
        assertSame(target, t.getTarget());
        assertSame(null, t.getMethodName());
        assertSame(oa, t.getArguments());
        assertEquals("Object.null(Object);", t.toString());
    }

    /*
     * Test the constructor with the method name "new".
     */
    public void testConstructor_NewMethodName() {
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Statement t = new Statement(target, "new", oa);
        assertSame(target, t.getTarget());
        assertSame("new", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertEquals("Object.new(Object);", t.toString());
    }

    /*
     * Test the constructor with empty method name.
     */
    public void testConstructor_EmptyMethodName() {
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Statement t = new Statement(target, "", oa);
        assertSame(target, t.getTarget());
        assertSame("", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertEquals("Object.(Object);", t.toString());
    }

    /*
     * Test the constructor with null arguments.
     */
    public void testConstructor_NullArguments() {
        Object target = new Object();
        Statement t = new Statement(target, "method", null);
        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertEquals(0, t.getArguments().length);
        assertEquals("Object.method();", t.toString());
    }

    /*
     * Test the constructor with a null argument.
     */
    public void testConstructor_NullArgument() {
        Object target = new Object();
        Object[] oa = new Object[] { null };
        Statement t = new Statement(target, "method", oa);
        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertNull(t.getArguments()[0]);
        assertEquals("Object.method(null);", t.toString());
    }

    public void testConstructor_EmptyTarget_EmptyMethod_NullArguments() {
        Statement statement = new Statement(new String(), new String(),
                (Object[]) null);
        assertEquals("\"\".();", statement.toString());
    }

    public void testConstructor_StringArrayTarget_EmptyMethod_NullArguments() {
        Object target = new String[2];
        Statement statement = new Statement(target, new String(),
                (Object[]) null);
        assertEquals("StringArray.();", statement.toString());
    }

    public void testConstructor_StringArrayArrayTarget_EmptyMethod_NullArguments() {
        Object target = new String[2][];
        Statement statement = new Statement(target, new String(),
                (Object[]) null);
        assertEquals("StringArrayArray.();", statement.toString());
    }

    public void testConstructor_StringArrayArrayTarget_EmptyMethod_StringArrayArrayTarget() {
        Object target = new String[2][];
        Statement statement = new Statement(target, new String(),
                new Object[] { target });
        assertEquals("StringArrayArray.(StringArrayArray);", statement
                .toString());
    }

    // public void testGetArguments() {
    // // Covered in the testcases for the constructor
    // }
    //
    // public void testGetMethodName() {
    // // Covered in the testcases for the constructor
    // }
    //
    // public void testGetTarget() {
    // // Covered in the testcases for the constructor
    // }
    //
    // public void testToString() {
    // // Covered in the testcases for the constructor
    // }

    /*
     * Test the method execute() with a normal object, a valid method name and
     * valid arguments.
     */
    public void testExecute_NormalInstanceMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Statement t = new Statement(mo, "method", new Object[0]);
        t.execute();
        MockObject.assertCalled("method1", new Object[0]);
        t = new Statement(mo, "method", null);
        t.execute();
        MockObject.assertCalled("method1", new Object[0]);
    }

    /*
     * Test the method execute() with a normal object, normal method and null
     * arguments.
     */
    public void testExecute_NormalInstanceMethodNull() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { null, null, null };
        Statement t = new Statement(mo, "method", arguments);

        t.execute();
        MockObject.assertCalled("method5", arguments);
    }

    /*
     * Test the method execute() with a normal object, a valid method that
     * throws an exception and valid arguments.
     */
    public void testExecute_ExceptionalMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Statement t = new Statement(mo, "method", new Object[] { null, null });
        try {
            t.execute();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        MockObject.assertCalled("method4", new Object[] { null, null });
    }

    /*
     * Test the method execute() with a normal object and a non-existing method
     * name.
     */
    public void testExecute_NonExistingMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Statement t = new Statement(mo, "method_not_existing", new Object[] {
                null, null });
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a null object.
     */
    public void testExecute_NullTarget() throws Exception {
        Statement t = new Statement(null, "method_not_existing", new Object[] {
                null, null });
        try {
            t.execute();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a null method name.
     */
    // public void testExecute_NullMethodName() throws Exception {
    // MockObject mo = new MockObject(false);
    // Statement t = new Statement(mo, null, new Object[] { null, null });
    // try {
    // t.execute();
    // fail("Should throw NoSuchMethodException!");
    // } catch (NoSuchMethodException ex) {
    // // expected
    // }
    // }
    /*
     * Test the method execute() with a normal object, a valid method and
     * invalid arguments (in terms of type, numbers, etc.).
     */
    public void testExecute_InvalidArguments() throws Exception {
        MockObject mo = new MockObject(false);
        Statement t = new Statement(mo, "method", new Object[] { new Object(),
                new Object(), new Object() });
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal object, an overloaded method and
     * valid arguments.
     */
    public void testExecute_OverloadedMethods() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments;
        Statement t;

        arguments = new Object[] { new Object() };
        t = new Statement(mo, "method", arguments);
        t.execute();
        MockObject.assertCalled("method2", arguments);

        arguments = new Object[] { "test" };
        t = new Statement(mo, "method", arguments);
        t.execute();
        MockObject.assertCalled("method3", arguments);

        arguments = new Object[] { new Integer(117) };
        t = new Statement(mo, "method", arguments);
        t.execute();
        MockObject.assertCalled("method1-3", arguments);
    }

    /*
     * Test the method execute() with a normal object, the method name "new" and
     * valid arguments.
     */
    public void testExecute_NormalConstructor() throws Exception {
        Statement t = new Statement(MockObject.class, "new", new Object[0]);
        t.execute();
        MockObject.assertCalled("new0", new Object[0]);
        t = new Statement(MockObject.class, "new", null);
        t.execute();
        MockObject.assertCalled("new0", new Object[0]);
    }

    /*
     * Test the method execute() with a normal object, normal constructor ("new"
     * method) and null arguments.
     */
    public void testExecute_NormalConstructorNull() throws Exception {
        Object[] arguments = new Object[] { null, null };
        Statement t = new Statement(MockObject.class, "new", arguments);

        try {
            t.execute();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        MockObject.assertCalled("new4", arguments);
    }

    /*
     * Test the method execute() with a normal object, the method name "new"
     * that throws an exception and valid arguments.
     */
    public void testExecute_ExceptionalConstructor() throws Exception {
        Statement t = new Statement(MockObject.class, "new", new Object[] {
                null, null });
        try {
            t.execute();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        MockObject.assertCalled("new4", new Object[] { null, null });
    }

    /*
     * Test the method execute() with a normal object, the method name "new" and
     * invalid arguments (in terms of type, numbers, etc.).
     */
    public void testExecute_NonExistingConstructor() throws Exception {
        Statement t = new Statement(MockObject.class, "new", new Object[] {
                null, null, null, null });

        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal object with overloaded
     * constructors, the method name "new" and valid arguments.
     */
    public void testExecute_OverloadedConstructors() throws Exception {
        Object[] arguments = new Object[] { new Object() };
        Statement t = new Statement(MockObject.class, "new", arguments);
        t.execute();
        MockObject.assertCalled("new2", arguments);

        arguments = new Object[] { "test" };
        t = new Statement(MockObject.class, "new", arguments);
        t.execute();
        //FIXME: the following 2 commented assert cannot pass neither in RI nor in Harmony (HARMONY-4392),
        // waiting for dev-list approval to fix Harmony implementation following spec        
//         MockObject.assertCalled("new3", arguments);

        Object[] arguments2 = new Object[] { new Integer(1) };
        t = new Statement(MockObject.class, "new", arguments2);
        t.execute();
//        MockObject.assertCalled("new1-2", arguments2);
    }
    
    /*
     * Test the method execute() with the Class object, a static method name and
     * valid arguments.
     */
    public void testExecute_NormalStaticMethodViaClass() throws Exception {
        Object[] arguments = new Object[] { new Object() };
        Statement t = new Statement(MockObject.class, "staticMethod", arguments);
        t.execute();
        MockObject.assertCalled("staticMethod", arguments);
    }

    /*
     * Test the method execute() with an object, a static method name and valid
     * arguments.
     */
    public void testExecute_NormalStaticMethodViaObject() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Object() };
        Statement t = new Statement(mo, "staticMethod", arguments);
        t.execute();
        MockObject.assertCalled("staticMethod", arguments);
    }

    /*
     * Test the method execute() with a Class object of a normal class that has
     * a method of the same signature as Class.forName(String), a static method
     * name "forName" and valid argument "string".
     */
    public void testExecute_AmbiguousStaticMethod() throws Exception {
        Object[] arguments = new String[] { "test" };
        Statement t = new Statement(MockObject.class, "forName", arguments);
        t.execute();
        MockObject.assertCalled("forName", arguments);

        t = new Statement(String.class, "forName",
                new Object[] { "java.lang.String" });
        t.execute();
    }

    /*
     * Test the method execute() with the special method Class.forName().
     */
    public void testExecute_ClassForName() throws Exception {
        Object[] arguments = new String[] { Statement.class.getName() };
        Statement t = new Statement(Class.class, "forName", arguments);
        t.execute();

        t = new Statement(String.class, "forName",
                new Object[] { "java.lang.String" });
        t.execute();
    }

    /*
     * Test the method execute() with a normal array object, the method name
     * "get" and valid and invalid arguments.
     */
    public void testExecute_ArrayGet() throws Exception {
        Object[] array = new Object[] { "test" };
        Statement t = new Statement(array, "get",
                new Object[] { new Integer(0) });
        t.execute();

        array = new Object[] { "test" };
        t = new Statement(array, "get", new Object[0]);
        try {
            t.execute();
            fail("Should throw ArrayIndexOutOfBoundsException!");
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal array object, the method name
     * "set" and valid arguments.
     */
    public void testExecute_ArraySet() throws Exception {
        Object[] array = new Object[] { "test" };
        Statement t = new Statement(array, "set", new Object[] {
                new Integer(0), "test2" });
        t.execute();
        assertEquals("test2", array[0]);
    }

    /*
     * Test the method execute() with a normal array object, the method name
     * "set" and null index argument.
     */
    public void testExecute_ArrayNullIndex() throws Exception {
        Object[] array = new Object[] { "test" };
        Statement t = new Statement(array, "set",
                new Object[] { null, "test2" });
        try {
            t.execute();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal array object, the method name
     * "set" and invalid arguments.
     */
    public void testExecute_ArrayInvalidSet() throws Exception {
        Object[] array = new Object[] { "test" };
        Statement t = new Statement(array, "set", new Object[] {
                new Integer(0), "test2" });
        t.execute();
        assertEquals("test2", array[0]);

        try {
            t = new Statement(array, "set", new Object[] { "testtest", "test2",
                    new Object() });
            t.execute();
            fail("Should throw ClassCastException!");
        } catch (ClassCastException ex) {
            // expected
        }

        t = new Statement(array, "set", new Object[] { new Integer(0) });
        try {
            t.execute();
            fail("Should throw ArrayIndexOutOfBoundsException!");
        } catch (ArrayIndexOutOfBoundsException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal array object, the method name
     * "getInt" and invalid arguments.
     */
    public void testExecute_ArrayInvalidSetInt() throws Exception {
        int[] array = new int[] { 1 };
        Statement t = new Statement(array, "getInt",
                new Object[] { new Integer(0) });
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal array object, the method name
     * "gets".
     */
    public void testExecute_ArrayInvalidName() throws Exception {
        Object[] array = new Object[] { "test" };
        Statement t = new Statement(array, "gets", new Object[] {
                new Integer(0), new Object() });
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with a normal object with overloaded methods
     * (primitive type VS wrapper class), a valid method name and valid
     * arguments.
     * 
     * Note: decided by definition position!
     */
    public void testExecute_PrimitiveVSWrapper() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Integer(1) };
        Statement t = new Statement(mo, "methodB", arguments);
        t.execute();
        MockObject.assertCalled("methodB1", arguments);

        arguments = new Object[] { Boolean.FALSE };
        t = new Statement(mo, "methodB", arguments);
        t.execute();
        MockObject.assertCalled("methodB2", arguments);
    }

    /*
     * Test the method execute() with a protected method but within java.beans
     * package.
     */
    public void testExecute_ProtectedMethodWithPackage() throws Exception {
        DefaultPersistenceDelegate dpd = new DefaultPersistenceDelegate();
        Object[] arguments = new Object[] { "test", "test" };
        Statement t = new Statement(dpd, "mutatesTo", arguments);
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

    /*
     * Test the method execute() with a method that is applicable via type
     * conversion.
     */
    public void testExecute_ApplicableViaTypeConversion() throws Exception {
        MockObject mo = new MockObject(false);
        // mo.methodB('c');
        Object[] arguments = new Object[] { new Character((char) 1) };
        Statement t = new Statement(mo, "methodB", arguments);
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

    /*
     * Test the method execute() with two equal specific methods.
     * 
     * Note: decided by definition position!
     */
    // public void testExecute_EqualSpecificMethods() throws Exception {
    // MockObject mo = new MockObject(false);
    // Object[] arguments = new Object[] { new MockObject(false),
    // new MockObject(false) };
    // Statement t = new Statement(mo, "equalSpecificMethod", arguments);
    // t.execute();
    // MockObject.assertCalled("equalSpecificMethod1", arguments);
    // }
    /*
     * Test the method execute() with two equal specific methods but one
     * declaring thrown exception.
     * 
     * Note: decided by definition position!
     */
    // public void testExecute_EqualSpecificMethodsException() throws Exception
    // {
    // MockObject mo = new MockObject(false);
    // Object[] arguments = new Object[] { new MockObject(false),
    // new MockObject(false), new Object() };
    // Statement t = new Statement(mo, "equalSpecificMethod", arguments);
    // t.execute();
    // MockObject.assertCalled("equalSpecificMethod4", arguments);
    // }
    /*
     * Test the method execute() with int method while providing a null
     * parameter.
     */
    public void testExecute_IntMethodNullParameter() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { null };
        Statement t = new Statement(mo, "intMethod", arguments);
        try {
            t.execute();
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with int array method while providing an
     * Integer array parameter.
     */
    public void testExecute_IntArrayMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Integer[] { new Integer(1) } };
        Statement t = new Statement(mo, "intArrayMethod", arguments);
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method execute() with Integer array method while providing an
     * int array parameter.
     */
    public void testExecute_IntegerArrayMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new int[] { 1 } };
        Statement t = new Statement(mo, "integerArrayMethod", arguments);
        try {
            t.execute();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }
    
    
    /*
     * Test for special case of overloaded method execute
     */
    public void testExecute_AmbiguousOverloadedMethods() throws Exception {
        MockObject mo = new MockObject();
        Object[] arguments = new Object[] { new MockObject(), new MockObject() };
        Statement t = new Statement(mo, "overloadedMethod", arguments);
        t.execute();
        MockObject.assertCalled("overloadedmethod", arguments);
        
        arguments = new Object[] { new MockParent(), new MockParent() };
        t = new Statement(mo, "overloadedMethod", arguments);
        t.execute();
        MockObject.assertCalled("overloadedmethod2", arguments);
        
        arguments = new Object[] { new MockObject(), new MockObject() };
        t = new Statement(mo, "overloadedMethodB", arguments);
        try{
            t.execute();
            fail("should throw Exception");
        }catch(Exception e){
        }
        
        arguments = new Object[] { new MockObject(), new MockParent() };
        t = new Statement(mo, "overloadedMethodB", arguments);
        t.execute();
        MockObject.assertCalled("overloadedmethodB", arguments);
    }
    
    /*
     * Test for special case of the same signature but different return type
     */
    public void testExecute_SameSignatureDifferentReturn() throws Exception {
        // Regression for Harmony-5854
        Object[] ancestorArguments = new Object[] { new Ancestor() {
        } };
        Object[] offspringArguments = new Object[] { new Offspring() {
        } };
        String methodName = "visit";
        Statement statement = null;

        statement = new Statement(new StringInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_STRING);

        statement = new Statement(new StringInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_STRING);

        statement = new Statement(new BooleanInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_BOOLEAN);

        statement = new Statement(new BooleanInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_BOOLEAN);

        statement = new Statement(new CharacterInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_CHARACTER);

        statement = new Statement(new CharacterInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_CHARACTER);

        statement = new Statement(new ShortInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_SHORT);

        statement = new Statement(new ShortInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_SHORT);

        statement = new Statement(new IntegerInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_INTEGER);

        statement = new Statement(new IntegerInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_INTEGER);

        statement = new Statement(new LongInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_LONG);

        statement = new Statement(new LongInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_LONG);

        statement = new Statement(new FloatInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_FLOAT);

        statement = new Statement(new FloatInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_FLOAT);

        statement = new Statement(new DoubleInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_DOUBLE);

        statement = new Statement(new DoubleInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_DOUBLE);

        statement = new Statement(new ObjectInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_OBJECT);

        statement = new Statement(new ObjectInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_OBJECT);

        statement = new Statement(new ObjectListInspector(), "visit",
                ancestorArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, ancestorArguments,
                TInspectorCluster.ANCESTOR_OBJECT_LIST);

        statement = new Statement(new ObjectListInspector(), "visit",
                offspringArguments);
        statement.execute();
        TInspectorCluster.assertMethodCalled(methodName, offspringArguments,
                TInspectorCluster.OFFSPRING_OBJECT_LIST);
    }

    public void test_Statement_Execute() throws Exception {
        MockTreeMapInnerClass innerTreeMap = new MockTreeMapInnerClass();
        Statement statement = new Statement(innerTreeMap, "get",
                new Object[] { "key" });
        statement.execute();
        assertEquals("value", innerTreeMap.getReturnValue());
        innerTreeMap.reset();
    }

    class MockTreeMapInnerClass extends TreeMap {

        private Object returnValue = null;

        public Object getReturnValue() {
            return returnValue;
        }

        public void reset() {
            returnValue = null;
        }

        @Override
        public Object get(Object key) {
            return returnValue = "value";
        }
    }

    /*
     * Super class of MockObject.
     */
    public static class MockParent {

        protected static String calledMethod = null;

        protected static Vector<Object> receivedArguments = new Vector<Object>();

        public void method() {
            reset();
            calledMethod = "method1";
        }

        protected void method(Boolean o) {
            reset();
            calledMethod = "method1-1";
            receivedArguments.add(o);
        }

        public void method(Number n) {
            reset();
            calledMethod = "method1-2";
            receivedArguments.add(n);
        }

        public void method(Integer o) {
            reset();
            calledMethod = "method1-3";
            receivedArguments.add(o);
        }

        public void method(Object o) {
            reset();
            calledMethod = "method2";
            receivedArguments.add(o);
        }

        public void method(String o) {
            reset();
            calledMethod = "method3";
            receivedArguments.add(o);
        }

        public void method(Object o, Object o2) {
            reset();
            calledMethod = "method4";
            receivedArguments.add(o);
            receivedArguments.add(o2);
            throw new NullPointerException();
        }

        public void method(Object o, Number n, String s) {
            reset();
            calledMethod = "method5";
            receivedArguments.add(o);
            receivedArguments.add(n);
            receivedArguments.add(s);
        }

        public static void reset() {
            receivedArguments.clear();
            calledMethod = null;
        }
    }

    /*
     * Mock object.
     */
    public static class MockObject extends MockParent {

        public MockObject() {
            reset();
            calledMethod = "new0";
        }

        public MockObject(boolean testingConstructor) {
            reset();
            if (testingConstructor) {
                calledMethod = "new1";
            }
        }
        
        public MockObject(String o) {
            reset();
            calledMethod = "new3";
            receivedArguments.add(o);
        }
        
        public MockObject(Object o) {
            reset();
            calledMethod = "new2";
            receivedArguments.add(o);
        }

        public MockObject(Integer o) {
            reset();
            calledMethod = "new1-2";
            receivedArguments.add(o);
        }
        public MockObject(Object o, Object o2) {
            reset();
            calledMethod = "new4";
            receivedArguments.add(o);
            receivedArguments.add(o2);
            throw new NullPointerException();
        }

        @SuppressWarnings("unchecked")
        public MockObject(Object o, Vector v, Class c) {
            reset();
            calledMethod = "new5";
            receivedArguments.add(o);
            receivedArguments.add(v);
            receivedArguments.add(c);
        }

        public void intMethod(int i) {
            reset();
            calledMethod = "intMethod";
            receivedArguments.add(new Integer(i));
        }

        public void intArrayMethod(int[] ia) {
            reset();
            calledMethod = "intArrayMethod";
            receivedArguments.add(ia);
        }

        public void integerArrayMethod(Integer[] ia) {
            reset();
            calledMethod = "integerArrayMethod";
            receivedArguments.add(ia);
        }

        public void methodB(Integer i) {
            reset();
            calledMethod = "methodB1";
            receivedArguments.add(i);
        }

        public void methodB(boolean b) {
            reset();
            calledMethod = "methodB2";
            receivedArguments.add(new Boolean(b));
        }
        
        public void overloadedMethod(MockObject o1, MockObject o2){
            reset();
            calledMethod = "overloadedmethod";
            receivedArguments.add(o1);
            receivedArguments.add(o2);
        }
        
        public void overloadedMethod(MockParent o1, MockParent o2){
            reset();
            calledMethod = "overloadedmethod2";
            receivedArguments.add(o1);
            receivedArguments.add(o2);
        }
        
        public void overloadedMethod(MockObject o1, MockParent o2){
            reset();
            calledMethod = "overloadedmethod2";
            receivedArguments.add(o1);
            receivedArguments.add(o2);
        }
        
        public void overloadedMethodB(MockObject o1, MockParent o2){
            reset();
            calledMethod = "overloadedmethodB";
            receivedArguments.add(o1);
            receivedArguments.add(o2);
        }
        
        public void overloadedMethodB(MockParent o1, MockObject o2){
            reset();
            calledMethod = "overloadedmethodB2";
            receivedArguments.add(o1);
            receivedArguments.add(o2);
        }
        
        public static void staticMethod(Object o) {
            reset();
            calledMethod = "staticMethod";
            receivedArguments.add(o);
        }

        // public void equalSpecificMethod(MockObject o, MockParent p) {
        // reset();
        // calledMethod = "equalSpecificMethod1";
        // receivedArguments.add(o);
        // receivedArguments.add(p);
        // }
        //
        // public void equalSpecificMethod(MockParent p, MockObject o) {
        // reset();
        // calledMethod = "equalSpecificMethod2";
        // receivedArguments.add(p);
        // receivedArguments.add(o);
        // }
        //
        // public void equalSpecificMethod(MockParent p, MockObject o, Object
        // o2)
        // throws Exception {
        // reset();
        // calledMethod = "equalSpecificMethod4";
        // receivedArguments.add(p);
        // receivedArguments.add(o);
        // receivedArguments.add(o2);
        // }
        //
        // public void equalSpecificMethod(MockObject o, MockParent p, Object
        // o2) {
        // reset();
        // calledMethod = "equalSpecificMethod3";
        // receivedArguments.add(o);
        // receivedArguments.add(p);
        // receivedArguments.add(o2);
        // }

        public static Class<?> forName(String o) {
            reset();
            calledMethod = "forName";
            receivedArguments.add(o);
            return null;
        }

        public static void assertCalled(String methodName, Object[] arguments) {
            assertEquals(methodName, calledMethod);
            assertTrue(Arrays.equals(arguments, receivedArguments.toArray()));
            reset();
        }

        public static void assertNotCalled() {
            assertNull(calledMethod);
            assertTrue(receivedArguments.isEmpty());
        }
    }

    public interface IMockObjectA {
    }

    public class MockObjectA implements IMockObjectA {
    }

    public interface IMockAccess<E> {
        public void setProperty(E e);
    }

    public class MockAccess implements IMockAccess<IMockObjectA> {

        private IMockObjectA property;

        public void setProperty(IMockObjectA prop) {
            property = prop;
        }

        public IMockObjectA getProperty() {
            return property;
        }

    }

    public void testExecute_ObjectInterface() throws Exception {
        MockAccess mockAccess = new MockAccess();
        MockObjectA mockObjectA = new MockObjectA();
        new Statement(mockAccess, "setProperty", new Object[] { mockObjectA })
                .execute();
        assertSame(mockObjectA, mockAccess.getProperty());
    }

}
