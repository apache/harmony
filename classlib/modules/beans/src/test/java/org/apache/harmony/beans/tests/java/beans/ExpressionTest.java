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
import java.beans.Expression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.SampleBean;

/**
 * Test the class java.beans.Expression.
 */
public class ExpressionTest extends TestCase {

    /**
     * The test checks the correct constructor is initialized
     */
    public void testConstructor() throws Exception {
        Expression expr = new Expression(SampleBean.class, "new",
                new Object[] { "hello" });
        Object result = expr.getValue();
        if (result != null && result instanceof SampleBean) {
            SampleBean bean = (SampleBean) result;
            assertEquals("hello", bean.getText());
        } else {
            fail("Cannot instantiate an instance of Bean class.");
        }
    }

    /**
     * The test checks the correct static method is initialized
     */
    public void testStatic() throws Exception {
        SampleBean theBean = new SampleBean();
        Expression expr = new Expression(SampleBean.class, "create",
                new Object[] { "hello", theBean });

        Object result = expr.getValue();
        if (result != null && result instanceof SampleBean) {
            SampleBean bean = (SampleBean) result;
            assertEquals("hello", bean.getText());
            assertEquals(theBean, bean.getObject());
        } else {
            fail("Cannot instantiate an instance of Bean class by "
                    + "static method.");
        }
    }

    /**
     * The test checks the correct getter is initialized
     */
    public void testGetter() throws Exception {
        Expression expr = new Expression(new SampleBean("hello"), "getText",
                new Object[] {});

        Object result = expr.getValue();
        if (result != null && result instanceof String) {
            assertEquals("hello", result);
        } else {
            fail("Result of SampleBean.getText() call is not "
                    + "of String type.");
        }
    }

    /**
     * The test checks the correct array getter is initialized
     */
    public void testArrayGetter() throws Exception {
        int[] a = { 1, 2, 3 };
        Expression expr = new Expression(a, "get",
                new Object[] { new Integer(1) });

        Object result = expr.getValue();
        if (result != null && result instanceof Integer) {
            assertEquals(new Integer(2), result);
        } else {
            fail("Result of array getter is not of Integer type.");
        }
    }

    /*
     * Test the constructor under normal conditions.
     */
    public void testConstructor_Normal() {
        Object target = new MockParent();
        Object arg1 = "string1";
        Object arg2 = new Object();
        Object arg3 = "string3";
        Object arg4 = new Integer(117);
        Object[] oa = new Object[] { arg1, arg2, arg3, arg4 };
        Expression t = new Expression(target, "method", oa);

        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg1, t.getArguments()[0]);
        assertSame(arg2, t.getArguments()[1]);
        assertSame(arg3, t.getArguments()[2]);
        assertSame(arg4, t.getArguments()[3]);

        assertEquals("<unbound>=ExpressionTest$MockParent.method("
                + "\"string1\", Object, \"string3\", Integer);", t.toString());
    }

    /*
     * Test the constructor with null target.
     */
    public void testConstructor_NullTarget() {
        Object arg = new Object();
        Object[] oa = new Object[] { arg };
        Expression t = new Expression(null, "method", oa);

        assertSame(null, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg, t.getArguments()[0]);
    }

    /*
     * Test the constructor with an array target.
     */
    public void testConstructor_ArrayTarget() {
        Object target = new MockParent();
        Object arg = new Object();
        Object[] oa = new Object[] { arg };
        Expression t = new Expression(target, "method", oa);

        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg, t.getArguments()[0]);

        assertEquals("<unbound>=ExpressionTest$MockParent.method(Object);", t
                .toString());
    }

    /*
     * Test the constructor with null method name.
     */
    public void testConstructor_NullMethodName() {
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Expression t = new Expression(target, null, oa);

        assertSame(target, t.getTarget());
        assertSame(null, t.getMethodName());
        assertSame(oa, t.getArguments());
    }

    /*
     * Test the constructor with the method name "new".
     */
    public void testConstructor_NewMethodName() {
        Object target = MockObject.class;
        Object[] oa = new Object[] { new Object() };
        Expression t = new Expression(target, "new", oa);

        assertSame(target, t.getTarget());
        assertSame("new", t.getMethodName());
        assertSame(oa, t.getArguments());

        assertEquals("<unbound>=Class.new(Object);", t.toString());
    }

    /*
     * Test the constructor with empty method name.
     */
    public void testConstructor_EmptyMethodName() {
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Expression t = new Expression(target, "", oa);

        assertSame(target, t.getTarget());
        assertSame("", t.getMethodName());
        assertSame(oa, t.getArguments());
    }

    /*
     * Test the constructor with null arguments.
     */
    public void testConstructor_NullArguments() {
        Object target = new MockParent();
        Expression t = new Expression(target, "method", null);

        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertEquals(0, t.getArguments().length);

        assertEquals("<unbound>=ExpressionTest$MockParent.method();", t
                .toString());
    }

    /*
     * Test the constructor with a null argument.
     */
    public void testConstructor_NullArgument() {
        Object target = new MockParent();
        Object[] oa = new Object[] { null };
        Expression t = new Expression(target, "method", oa);

        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertNull(t.getArguments()[0]);

        assertEquals("<unbound>=ExpressionTest$MockParent.method(null);", t
                .toString());
    }

    /*
     * Test the constructor(value, ...) under normal conditions.
     */
    public void testConstructor_Value_Normal() throws Exception {
        Object val = new Object();
        Object target = new MockParent();
        Object arg1 = "mama";
        Object arg2 = new Object();
        Object arg3 = new Object();
        Object arg4 = new Long(7);
        Object[] oa = new Object[] { arg1, arg2, arg3, arg4 };
        Expression t = new Expression(val, target, "method", oa);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg1, t.getArguments()[0]);
        assertSame(arg2, t.getArguments()[1]);
        assertSame(arg3, t.getArguments()[2]);
        assertSame(arg4, t.getArguments()[3]);

        assertEquals("Object=ExpressionTest$MockParent.method("
                + "\"mama\", Object, Object, Long);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with null target.
     */
    public void testConstructor_Value_NullTarget() throws Exception {
        Object val = new Object();
        Object arg = new Object();
        Object[] oa = new Object[] { arg };
        Expression t = new Expression(val, null, "method", oa);

        assertSame(val, t.getValue());
        assertSame(null, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg, t.getArguments()[0]);

        assertEquals("Object=null.method(Object);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with an array target.
     */
    public void testConstructor_Value_ArrayTarget() throws Exception {
        Integer val = new Integer(69);
        Object target = new Integer[] { val };
        Object arg = new Integer(0);
        Object[] oa = new Object[] { arg };
        Expression t = new Expression(val, target, "get", oa);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("get", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertSame(arg, t.getArguments()[0]);

        assertEquals("Integer=IntegerArray.get(Integer);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with null method name.
     */
    public void testConstructor_Value_NullMethodName() throws Exception {
        Object val = new Object();
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Expression t = new Expression(val, target, null, oa);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame(null, t.getMethodName());
        assertSame(oa, t.getArguments());

        assertEquals("Object=Object.null(Object);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with the method name "new".
     */
    public void testConstructor_Value_NewMethodName() throws Exception {
        Object val = new Object();
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Expression t = new Expression(val, target, "new", oa);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("new", t.getMethodName());
        assertSame(oa, t.getArguments());

        assertEquals("Object=Object.new(Object);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with empty method name.
     */
    public void testConstructor_Value_EmptyMethodName() throws Exception {
        Object val = new Object();
        Object target = new Object();
        Object[] oa = new Object[] { new Object() };
        Expression t = new Expression(val, target, "", oa);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("", t.getMethodName());
        assertSame(oa, t.getArguments());

        assertEquals("Object=Object.(Object);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with null arguments.
     */
    public void testConstructor_Value_NullArguments() throws Exception {
        Object val = new Object();
        Object target = new Object();
        Expression t = new Expression(val, target, "method", null);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertEquals(0, t.getArguments().length);

        assertEquals("Object=Object.method();", t.toString());
    }

    /*
     * Test the constructor(value, ...) with a null argument.
     */
    public void testConstructor_Value_NullArgument() throws Exception {
        Object val = new Object();
        Object target = new Object();
        Object[] oa = new Object[] { null };
        Expression t = new Expression(val, target, "method", oa);

        assertSame(val, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertNull(t.getArguments()[0]);

        assertEquals("Object=Object.method(null);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with a null value.
     */
    public void testConstructor_Value_NullValue() throws Exception {
        Object target = new Object();
        Object[] oa = new Object[] { null };
        Expression t = new Expression(null, target, "method", oa);

        assertSame(null, t.getValue());
        assertSame(target, t.getTarget());
        assertSame("method", t.getMethodName());
        assertSame(oa, t.getArguments());
        assertNull(t.getArguments()[0]);

        assertEquals("null=Object.method(null);", t.toString());
    }

    /*
     * Test the constructor(value, ...) with a expression value.
     */
    public void testConstructor_EmptyTarget_EmptyMethod_ExpressionArguments() {
        Object[] objectArray = new Object[] { new Expression((Object) null,
                (String) null, (Object[]) null) };
        Expression expression = new Expression(objectArray, new String(),
                new String(), objectArray);
        assertEquals("ObjectArray=\"\".(Expression);", expression.toString());
    }

    /*
     * Test the setValue() method with a non-null value when the value of the
     * expression is still unbounded.
     */
    public void testSetValue_UnboundNormal() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        t.setValue(mo);
        assertSame(mo, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the setValue() method with a null value when the value of the
     * expression is still unbounded.
     */
    public void testSetValue_UnboundNull() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        t.setValue(null);
        assertSame(null, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the setValue() method when the value of the expression is set by the
     * constructor.
     */
    public void testSetValue_Constructor() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, mo, "method", new Object[0]);
        assertSame(mo, t.getValue());
        MockObject.assertNotCalled();
        t.setValue(null);
        assertSame(null, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the setValue() method when the value of the expression is set by a
     * previous call to setValue().
     */
    public void testSetValue_Set() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        t.setValue(mo);
        assertSame(mo, t.getValue());
        MockObject.assertNotCalled();
        t.setValue(null);
        assertSame(null, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the setValue() method when the value of the expression is set by a
     * previous call to getValue().
     */
    public void testSetValue_Evaluated() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        assertEquals("method1", t.getValue());
        MockObject.assertCalled("method1", new Object[0]);
        t.setValue(mo);
        assertSame(mo, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the getValue() method when the value of the expression is evaluated
     * by a previous call to getValue().
     */
    public void testGetValue_Evaluated() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        assertEquals("method1", t.getValue());
        MockObject.assertCalled("method1", new Object[0]);
        assertEquals("method1", t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the getValue() method when the value of expression is set by the
     * constructor.
     */
    public void testGetValue_Constructor() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, mo, "method", new Object[0]);
        assertSame(mo, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the getValue() method when the value of expression is set by a
     * previous call to setValue().
     */
    public void testGetValue_Set() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        t.setValue(mo);
        assertSame(mo, t.getValue());
        MockObject.assertNotCalled();
    }

    /*
     * Test the method getValue() with a normal object, a valid method name and
     * valid arguments.
     */
    public void testGetValue_UnboundedNormalInstanceMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[0]);
        assertEquals("method1", t.getValue());
        MockObject.assertCalled("method1", new Object[0]);
        t = new Expression(mo, "method", null);
        assertEquals("method1", t.getValue());
        MockObject.assertCalled("method1", new Object[0]);
    }

    /*
     * Test the method getValue() with a normal object, a valid method that
     * throws an exception and valid arguments.
     */
    public void testGetValue_UnboundedExceptionalMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[] { null, null });
        try {
            t.getValue();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        MockObject.assertCalled("method4", new Object[] { null, null });
    }

    /*
     * Test the method getValue() with a normal object and a non-existing method
     * name.
     */
    public void testGetValue_UnboundedNonExistingMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method_not_existing", new Object[] {
                null, null });
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a null object.
     */
    public void testGetValue_UnboundedNullTarget() throws Exception {
        Expression t = new Expression(null, "method_not_existing",
                new Object[] { null, null });
        try {
            t.getValue();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a null method name.
     */
    public void testGetValue_UnboundedNullMethodName() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, null, new Object[] { null, null });
        try {
            t.getValue();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a normal object, a valid method and
     * invalid arguments (in terms of type, numbers, etc.).
     */
    public void testGetValue_UnboundedInvalidArguments() throws Exception {
        MockObject mo = new MockObject(false);
        Expression t = new Expression(mo, "method", new Object[] {
                new Object(), new Object(), new Object() });
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a normal object, an overloaded method and
     * valid arguments.
     */
    public void testGetValue_UnboundedOverloadedMethods() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Object() };
        Expression t = new Expression(mo, "method", arguments);
        assertEquals("method2", t.getValue());
        MockObject.assertCalled("method2", arguments);

        arguments = new Object[] { "test" };
        t = new Expression(mo, "method", arguments);
        assertEquals("method3", t.getValue());
        MockObject.assertCalled("method3", arguments);
    }

    /*
     * Test the method getValue() with a normal object, the method name "new"
     * and valid arguments.
     */
    public void testGetValue_UnboundedNormalConstructor() throws Exception {
        Expression t = new Expression(MockObject.class, "new", new Object[0]);
        t.getValue();
        MockObject.assertCalled("new0", new Object[0]);
        t = new Expression(MockObject.class, "new", null);
        assertTrue(t.getValue() instanceof MockObject);
        MockObject.assertCalled("new0", new Object[0]);
    }

    /*
     * Test the method getValue() with a normal object, the method name "new"
     * that throws an exception and valid arguments.
     */
    public void testGetValue_UnboundedExceptionalConstructor() throws Exception {
        Expression t = new Expression(MockObject.class, "new", new Object[] {
                null, null });
        try {
            t.getValue();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        MockObject.assertCalled("new4", new Object[] { null, null });
    }

    /*
     * Test the method getValue() with a normal object, the method name "new"
     * and invalid arguments (in terms of type, numbers, etc.).
     */
    public void testGetValue_UnboundedNonExistingConstructor() throws Exception {
        Expression t = new Expression(MockObject.class, "new", new Object[] {
                null, null, null });
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a normal object with overloaded
     * constructors, the method name "new" and valid arguments. See Java
     * Language Specification (15.11) for reference.
     * 
     * Note: decided by definition position.
     */
    public void testGetValue_UnboundedOverloadedConstructors() throws Exception {
        Object[] arguments = new Object[] { new Object() };
        Expression t = new Expression(MockObject.class, "new", arguments);
        t.getValue();
        MockObject.assertCalled("new2", arguments);

        //FIXME: the following 2 commented assert cannot pass neither in RI nor in Harmony (HARMONY-4392),
        // waiting for dev-list approval to fix Harmony implementation following spec 
        arguments = new Object[] { "test" };
        t = new Expression(MockObject.class, "new", arguments);
        assertTrue(t.getValue() instanceof MockObject);
//         MockObject.assertCalled("new3", arguments);

        arguments = new Object[] { new Integer(1) };
        t = new Expression(MockObject.class, "new", arguments);
        assertTrue(t.getValue() instanceof MockObject);
//        MockObject.assertCalled("new1-2", arguments);
    }

    /*
     * Test the method getValue() with the Class object, a static method name
     * and valid arguments.
     */
    public void testGetValue_UnboundedNormalStaticMethodViaClass()
            throws Exception {
        Object[] arguments = new Object[] { new Object() };
        Expression t = new Expression(MockObject.class, "staticMethod",
                arguments);
        assertEquals("staticMethod", t.getValue());
        MockObject.assertCalled("staticMethod", arguments);
    }

    /*
     * Test the method getValue() with an object, a static method name and valid
     * arguments.
     */
    public void testGetValue_UnboundedNormalStaticMethodViaObject()
            throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Object() };
        Expression t = new Expression(mo, "staticMethod", arguments);
        assertEquals("staticMethod", t.getValue());
        MockObject.assertCalled("staticMethod", arguments);
    }

    /*
     * Test the method getValue() with a Class object of a normal class that has
     * a method of the same signature as Class.forName(String), a static method
     * name "forName" and valid argument "string".
     */
    public void testGetValue_UnboundedAmbitiousStaticMethod() throws Exception {
        Object[] arguments = new Object[] { "test" };
        Expression t = new Expression(MockObject.class, "forName", arguments);
        assertNull(t.getValue());
        MockObject.assertCalled("forName", arguments);

        t = new Expression(String.class, "forName",
                new Object[] { "java.lang.String" });
        assertSame(String.class, t.getValue());
    }

    /*
     * Test the method getValue() with the special method Class.forName().
     */
    public void testGetValue_UnboundedClassForName() throws Exception {
        Object[] arguments = new String[] { Expression.class.getName() };
        Expression t = new Expression(Class.class, "forName", arguments);
        assertSame(Expression.class, t.getValue());

        // t = new Expression(String.class, "forName", arguments);
        // assertSame(this.getClass(), t.getValue());
    }

    /*
     * Test the method getValue() with a normal array object, the method name
     * "get" and valid arguments.
     */
    public void testGetValue_UnboundedArrayGet() throws Exception {
        Object[] array = new Object[] { "test" };
        Expression t = new Expression(array, "get", new Object[] { new Integer(
                0) });
        assertEquals("test", t.getValue());
    }

    /*
     * Test the method getValue() with a normal array object, the method name
     * "getInt" and invalid arguments.
     */
    public void testGetValue_UnboundedArrayInvalidSetInt() throws Exception {
        int[] array = new int[] { 1 };
        Expression t = new Expression(array, "getInt",
                new Object[] { new Integer(0) });
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a normal array object, the method name
     * "gets".
     */
    public void testGetValue_UnboundedArrayInvalidName() throws Exception {
        Object[] array = new Object[] { "test" };
        Expression t = new Expression(array, "gets", new Object[] {
                new Integer(0), new Object() });
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException ex) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a normal object with overloaded methods
     * (primitive type VS wrapper class), a valid method name and valid
     * arguments.
     * 
     * Note: decided by definition position!
     */
    public void testGetValue_UnboundedPrimitiveVSWrapper() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Integer(1) };
        Expression t = new Expression(mo, "methodB", arguments);
        assertEquals("methodB2", t.getValue());
        MockObject.assertCalled("methodB2", arguments);

        arguments = new Object[] { Boolean.FALSE };
        t = new Expression(mo, "methodB", arguments);
        assertEquals("methodB3", t.getValue());
        MockObject.assertCalled("methodB3", arguments);
    }

    /*
     * Test the method getValue() with a normal object with void method name and
     * valid arguments.
     */
    public void testGetValue_UnboundedVoidMethod() throws Exception {
        MockObject mo = new MockObject(false);
        Object[] arguments = new Object[] { new Integer(1) };
        Expression t = new Expression(mo, "voidMethod", arguments);
        assertNull(t.getValue());
        MockObject.assertCalled("voidMethod2", arguments);
    }

    /*
     * Test the method getValue() with a protected method but within java.beans
     * package.
     */
    public void testGetValue_ProtectedMethodWithPackage() throws Exception {
        DefaultPersistenceDelegate dpd = new DefaultPersistenceDelegate();
        Object[] arguments = new Object[] { "test", "test" };
        Expression t = new Expression(dpd, "mutatesTo", arguments);
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

    /*
     * Test the method getValue() with a method that is applicable via type
     * conversion.
     */
    public void testGetValue_ApplicableViaTypeConversion() throws Exception {
        MockObject mo = new MockObject(false);
        // mo.methodB('c');
        Object[] arguments = new Object[] { new Character((char) 1) };
        Expression t = new Expression(mo, "methodB", arguments);
        try {
            t.getValue();
            fail("Should throw NoSuchMethodException!");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

    public void testGetValue_returnNull() throws Exception {
        MockTarget target = new MockTarget();
        Expression e = new Expression(target, "aMethod", new Object[] {});
        Object got = e.getValue();
        assertTrue(MockTarget.isCalled());
        got = e.getValue();
        assertFalse(MockTarget.isCalled());
    }

    public void testGetValue_newInstanceNormalMethod() throws Exception {
        Expression expression = new Expression(new NormalTarget(),
                "newInstance", new Object[0]);
        assertEquals("Normal-Called", expression.getValue());
    }

    public void testGetValue_newInstanceStaticMethod() throws Exception {
        Expression expression = new Expression(new StaticTarget(),
                "newInstance", new Object[0]);
        assertEquals("Static-Called", expression.getValue());
    }

    public class NormalTarget {

        public String newInstance() {
            return "Normal-Called";
        }
    }

    public static class StaticTarget {

        public static String newInstance() {
            return "Static-Called";
        }
    }

    /*
     * Test the method getValue() with two equal specific methods.
     * 
     * Note: decided by definition position! should be ambiguous.
     */
    // public void testGetValue_EqualSpecificMethods() throws Exception {
    // MockObject mo = new MockObject(false);
    // Object[] arguments = new Object[] { new MockObject(false),
    // new MockObject(false) };
    // Expression t = new Expression(mo, "equalSpecificMethod", arguments);
    // assertEquals("equalSpecificMethod1", t.getValue());
    // MockObject.assertCalled("equalSpecificMethod1", arguments);
    // }
    /*
     * Test the method getValue() with two equal specific methods but one
     * declaring thrown exception.
     * 
     * Note: decided by definition position! should call the one with exception.
     */
    // public void testGetValue_EqualSpecificMethodsException() throws Exception
    // {
    // MockObject mo = new MockObject(false);
    // Object[] arguments = new Object[] { new MockObject(false),
    // new MockObject(false), new Object() };
    // Expression t = new Expression(mo, "equalSpecificMethod", arguments);
    // assertEquals("equalSpecificMethod3", t.getValue());
    // MockObject.assertCalled("equalSpecificMethod3", arguments);
    // }
    /*
     * Super class of MockObject.
     */
    public static class MockParent {

        protected static String calledMethod = null;

        protected static Vector<Object> receivedArguments = new Vector<Object>();

        public Object method() {
            reset();
            calledMethod = "method1";
            return calledMethod;
        }

        protected Object method(Boolean o) {
            reset();
            calledMethod = "method1-1";
            receivedArguments.add(o);
            return calledMethod;
        }

        public Object method(Integer o) {
            reset();
            calledMethod = "method1-2";
            receivedArguments.add(o);
            return calledMethod;
        }

        public Object method(Object o) {
            reset();
            calledMethod = "method2";
            receivedArguments.add(o);
            return calledMethod;
        }

        public Object method(String o) {
            reset();
            calledMethod = "method3";
            receivedArguments.add(o);
            return calledMethod;
        }

        public Object method(Object o, Object o2) {
            reset();
            calledMethod = "method4";
            receivedArguments.add(o);
            receivedArguments.add(o2);
            throw new NullPointerException();
        }

        public Object method(Object o, Object o2, Object o3, Object o4) {
            reset();
            calledMethod = "method5";
            receivedArguments.add(o);
            receivedArguments.add(o2);
            receivedArguments.add(o3);
            receivedArguments.add(o4);
            return calledMethod;
        }

        public static void reset() {
            receivedArguments.clear();
            calledMethod = null;
        }
    }

    public static class MockTarget {
        static int called = 0;

        static int base = 0;

        public Object aMethod() { // should return null on first call
            called++;
            return null;
        }

        public static boolean isCalled() {
            boolean result = !(base == called);
            base = called;
            return result;
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

        // public Object methodB(Integer i) {
        // reset();
        // calledMethod = "methodB1";
        // receivedArguments.add(i);
        // return calledMethod;
        // }

        public Object methodB(int i) {
            reset();
            calledMethod = "methodB2";
            receivedArguments.add(new Integer(i));
            return calledMethod;
        }

        public Object methodB(boolean b) {
            reset();
            calledMethod = "methodB3";
            receivedArguments.add(new Boolean(b));
            return calledMethod;
        }

        // public Object methodB(Boolean b) {
        // reset();
        // calledMethod = "methodB4";
        // receivedArguments.add(b);
        // return calledMethod;
        // }

        public Object voidMethod(Object o) {
            reset();
            calledMethod = "voidMethod";
            receivedArguments.add(o);
            return "voidMethod";
        }

        public void voidMethod(Integer o) {
            reset();
            calledMethod = "voidMethod2";
            receivedArguments.add(o);
        }

        public static Object staticMethod(Object o) {
            reset();
            calledMethod = "staticMethod";
            receivedArguments.add(o);
            return calledMethod;
        }

        // public Object equalSpecificMethod(MockObject o, MockParent p) {
        // reset();
        // calledMethod = "equalSpecificMethod1";
        // receivedArguments.add(o);
        // receivedArguments.add(p);
        // return calledMethod;
        // }

        // public Object equalSpecificMethod(MockParent p, MockObject o) {
        // reset();
        // calledMethod = "equalSpecificMethod2";
        // receivedArguments.add(p);
        // receivedArguments.add(o);
        // return calledMethod;
        // }

        // public Object equalSpecificMethod(MockObject o, MockParent p, Object
        // o2) {
        // reset();
        // calledMethod = "equalSpecificMethod3";
        // receivedArguments.add(o);
        // receivedArguments.add(p);
        // receivedArguments.add(o2);
        // return calledMethod;
        // }

        // public Object equalSpecificMethod(MockParent p, MockObject o, Object
        // o2)
        // throws Exception {
        // reset();
        // calledMethod = "equalSpecificMethod4";
        // receivedArguments.add(p);
        // receivedArguments.add(o);
        // receivedArguments.add(o2);
        // return calledMethod;
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

    public void testSubExpression() throws Exception {
        MyExpression my_e = new MyExpression();
        my_e.setTarget(new Target());
        my_e.setArguments(new Object[] {});
        my_e.setMethodName("aMethod");
        my_e.execute();
        assertEquals("haha", my_e.getValue());
    }

    private static class MyExpression extends java.beans.Expression {

        private Object target = null;

        private Object args[] = new Object[] { new Object() };

        private String name = "";

        public MyExpression() {
            super(null, null, null);
        }

        public void setTarget(Object t) {
            target = t;
        }

        public Object getTarget() {
            return target;
        }

        public void setArguments(Object[] a) {
            args = a;
        }

        public Object[] getArguments() {
            return args;
        }

        public void setMethodName(String n) {
            name = n;
        }

        public String getMethodName() {
            return name;
        }

        public void setValue(Object value) {
            super.setValue(value);
        }

        public Object getValue() {
            return "haha";
        }

    }

    public static class Target {

        public Object aMethod() {
            return "haha";
        }
    }

    public void test_Expression_Constructor_OneArgument_senario1()
            throws Exception {
        Object[] arguments = new Object[] { "test" };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("string", arguments);
    }

    public void test_Expression_Constructor_OneArgument_senario2()
            throws Exception {
        Object[] arguments = new Object[] { new Integer(1) };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("integer", arguments);
    }

    public void test_Expression_Constructor_OneArgument_senario3()
            throws Exception {
        Object[] arguments = new Object[] { new Object() };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object", arguments);
    }

    public void test_Expression_Constructor_OneArgument_senario4()
            throws Exception {
        Object[] arguments = new Object[] { null };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario1()
            throws Exception {
        Object[] arguments = new Object[] { null, null };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object_object", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario2()
            throws Exception {
        Object[] arguments = new Object[] { null, "test" };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object_string", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario3()
            throws Exception {
        Object[] arguments = new Object[] { new Object(), "test" };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object_string", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario4()
            throws Exception {
        Object[] arguments = new Object[] { "test1", "test2" };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("string_string", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario5()
            throws Exception {
        Object[] arguments = new Object[] { "test", new Integer(1) };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("string_integer", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario6()
            throws Exception {
        Object[] arguments = new Object[] { "test", (String) null };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object_object", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario7()
            throws Exception {
        Object[] arguments = new Object[] { new Integer(1), "test" };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object_string", arguments);
    }

    public void test_Expression_Constructor_twoArguments_senario8()
            throws Exception {
        Object[] arguments = new Object[] { new Integer(1), new Integer(2) };
        Expression expression = new Expression(SampleObject.class, "new",
                arguments);
        assertTrue(expression.getValue() instanceof SampleObject);
        SampleObject.assertCalled("object_object", arguments);
    }

    public static class SampleObject {

        public static String calledMethod = null;

        public static ArrayList<Object> receivedArguments = new ArrayList<Object>();

        public SampleObject(String o) {
            reset();
            calledMethod = "string";
            receivedArguments.add(o);
        }

        public SampleObject(Object o) {
            reset();
            calledMethod = "object";
            receivedArguments.add(o);
        }

        public SampleObject(Integer o) {
            reset();
            calledMethod = "integer";
            receivedArguments.add(o);
        }

        public SampleObject(Object arg1, Object arg2) {
            reset();
            calledMethod = "object_object";
            receivedArguments.add(arg1);
            receivedArguments.add(arg2);
        }

        public SampleObject(Object arg1, String arg2) {
            reset();
            calledMethod = "object_string";
            receivedArguments.add(arg1);
            receivedArguments.add(arg2);
        }

        public SampleObject(String arg1, String arg2) {
            reset();
            calledMethod = "string_string";
            receivedArguments.add(arg1);
            receivedArguments.add(arg2);
        }

        public SampleObject(String arg1, Integer arg2) {
            reset();
            calledMethod = "string_integer";
            receivedArguments.add(arg1);
            receivedArguments.add(arg2);
        }

        public static void assertCalled(String methodName, Object[] arguments) {
            assertEquals(methodName, calledMethod);
            assertTrue(Arrays.equals(arguments, receivedArguments.toArray()));
            reset();
        }

        public static void reset() {
            receivedArguments.clear();
            calledMethod = null;
        }
    }
}
