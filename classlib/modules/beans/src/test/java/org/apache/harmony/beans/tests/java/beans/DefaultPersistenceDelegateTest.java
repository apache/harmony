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
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.Introspector;
import java.beans.PersistenceDelegate;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.beans.Statement;
import java.util.Iterator;
import java.util.Vector;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.beans.tests.support.mock.MockFoo;
import org.apache.harmony.beans.tests.support.mock.MockFoo2;
import org.apache.harmony.beans.tests.support.mock.MockFooLabel;
import org.apache.harmony.beans.tests.support.mock.MockFooStop;

import tests.util.CallVerificationStack;

/**
 * Tests the class java.beans.DefaultPersistenceDelegate TODO refactor the class
 * and remove all references to CallVerificationStack
 */
public class DefaultPersistenceDelegateTest extends TestCase {

    public DefaultPersistenceDelegateTest() {
    }

    public DefaultPersistenceDelegateTest(String s) {
        super(s);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Introspector.flushCaches();
        CallVerificationStack.getInstance().clear();
    }

    public static TestSuite suite() {
        // TestSuite suite = new TestSuite();
        TestSuite suite = new TestSuite(DefaultPersistenceDelegateTest.class);

        // suite.addTest(new DefaultPersistenceDelegateTest(
        // "testInitialize_NotRegularGetter"));
        return suite;
    }

    /*
     * Test the default constructor.
     */
    public void testConstructor_Default() {
        new DefaultPersistenceDelegate();
    }

    /*
     * Test the constructor with normal property names.
     */
    public void testConstructor_StringArray_Normal() {
        new DefaultPersistenceDelegate(new String[] { "prop1", "", null });
    }

    /*
     * Test the constructor with an empty string array.
     */
    public void testConstructor_StringArray_Empty() {
        new DefaultPersistenceDelegate(new String[0]);
    }

    /*
     * Test the constructor with null.
     */
    public void testConstructor_StringArray_Null() {
        new DefaultPersistenceDelegate(null);
    }

    /*
     * Test instantiate() under normal conditions: two properties, valid getter
     * method but no setter method and no such a constructor requiring the two
     * properties.
     */
    public void testInstantiate_Normal() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop2" });
        MockBean b = new MockBean();
        Encoder enc = new Encoder();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, enc);
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertSame("bean1", e.getArguments()[0]);
        assertEquals(new Integer(2), e.getArguments()[1]);
    }

    /*
     * Test instantiate() with null instance.
     */
    public void testInstantiate_NullInstance() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop2" });
        Encoder enc = new Encoder();
        try {
            pd.instantiate(null, enc);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test instantiate() with null encoder.
     */
    public void testInstantiate_NullEncoder() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop2" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, null);
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertSame("bean1", e.getArguments()[0]);
        assertEquals(new Integer(2), e.getArguments()[1]);
        assertEquals(2, e.getArguments().length);
    }

    /*
     * Test instantiate() with null property name.
     */
    public void testInstantiate_NullProperty() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", null });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        pd.instantiate(b, new Encoder());
        
        pd = new MockPersistenceDelegate(new String[] {
                "prop1", null, "prop2"});
        MockBean b2 = new MockBean();
        b2.setAll("bean1", 2);
        pd.instantiate(b2, new Encoder());
    }

    /*
     * Test instantiate() with empty property name.
     */
    public void testInstantiate_EmptyProperty() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        pd.instantiate(b, null);
    }

    /*
     * Test instantiate() with no property.
     */
    public void testInstantiate_NoProperty() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, new Encoder());
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(0, e.getArguments().length);
    }

    /*
     * Test instantiate() with one normal property name, but non-existing getter
     * method.
     */
    public void testInstantiate_NonExistingGetter() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "non_existing" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Encoder enc = new Encoder();
        ExceptionListener el = new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                CallVerificationStack.getInstance().push(e);
            }
        };
        enc.setExceptionListener(el);
        Expression e = pd.instantiate(b, enc);
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertSame(null, e.getArguments()[1]);
        assertTrue(CallVerificationStack.getInstance().pop() instanceof Exception);

        enc.setExceptionListener(null);
        assertNotNull(enc.getExceptionListener());
        e = pd.instantiate(b, enc);
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertSame(null, e.getArguments()[1]);
    }

    /*
     * Test instantiate() with one normal property name, but non-existing getter
     * method, and null encoder.
     */
    public void testInstantiate_NonExistingGetterNulEncoder() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "non_existing" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        try {
            pd.instantiate(b, null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test instantiate() with one normal property name, but an invalid getter
     * method (requiring an argument, for instance).
     */
    public void testInstantiate_InvalidGetter() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop4" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, new Encoder());
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertSame(null, e.getArguments()[1]);
    }

    /*
     * Test instantiate() with one normal property name, but a getter method
     * that will throw an exception.
     */
    public void testInstantiate_ExceptionalGetter() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop5" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, new Encoder());
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertSame(null, e.getArguments()[1]);
    }

    /*
     * Test instantiate() with one normal property name, but a getter method
     * that will throw an error.
     */
    public void testInstantiate_ErrorGetter() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop7" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, new Encoder());
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertSame(null, e.getArguments()[1]);
    }

    /*
     * Test instantiate() with one normal property name, but a private getter
     * method.
     */
    public void testInstantiate_PrivateGetter() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[] {
                "prop1", "prop6" });
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, new Encoder());
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertSame(null, e.getArguments()[1]);
    }

    /*
     * Test instantiate() with a property name starting with initial upper case
     * letter, and a valid getter method.
     */
    public void testInstantiate_InitialUpperCasePropName() throws Exception {
        String[] props = new String[] { "Prop1", "prop2" };
        MockPersistenceDelegate pd = new MockPersistenceDelegate(props);
        MockBean b = new MockBean();
        b.setAll("bean1", 2);
        Expression e = pd.instantiate(b, new Encoder());
        assertSame(b, e.getValue());
        assertSame(MockBean.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(2, e.getArguments().length);
        assertSame(b.getProp1(), e.getArguments()[0]);
        assertEquals(new Integer(2), e.getArguments()[1]);
    }

    /*
     * Test instantiate() with a bean with no getter.
     */
    public void testInstantiate_NoGetter() throws Exception {
        MockEncoder enc = new MockEncoder();
        MockPersistenceDelegate pd = new MockPersistenceDelegate(
                new String[] { "i" });
        MockNoGetterBean2 b = new MockNoGetterBean2(3);
        enc.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                CallVerificationStack.getInstance().push(e);
            }
        });
        Expression e = pd.instantiate(b, enc);
        assertSame(b, e.getValue());
        assertSame(MockNoGetterBean2.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(1, e.getArguments().length);
        assertSame(null, e.getArguments()[0]);
        assertFalse(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test instantiate() with a property name that has an irregular getter
     * method, defined by its beaninfo.
     */
    public void testInstantiate_NotRegularGetter() throws Exception {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(
                new String[] { "prop" });
        MockFoo2 b = new MockFoo2(2);
        Expression e = pd.instantiate(b, new Encoder());

        assertSame(b, e.getValue());
        assertSame(MockFoo2.class, e.getTarget());
        assertEquals("new", e.getMethodName());
        assertEquals(1, e.getArguments().length);
        assertNull(e.getArguments()[0]);
    }

    public void testInstantiate_NPE() {

        try {
            testDefaultPersistenceDelegate obj = new testDefaultPersistenceDelegate();
            obj.initialize(Object.class, null, new Object(), new Encoder());
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    class testDefaultPersistenceDelegate extends DefaultPersistenceDelegate {
        testDefaultPersistenceDelegate() {
            super();
        }

        public void initialize(Class<?> type, Object oldInstance,
                Object newInstance, Encoder out) {
            super.initialize(type, oldInstance, newInstance, out);
        }
    }
        
    /*
     * Tests mutatesTo() under normal conditions without any properties.
     */
    public void testMutatesTo_NormalNoProperty() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate();

        assertTrue(pd.mutatesTo("test1", "test1"));
        assertFalse(pd.mutatesTo(new Object(), new Object() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        }));
        assertFalse(pd.mutatesTo(new MockFoo(), new MockFooStop()));
    }

    /*
     * Tests mutatesTo() under normal conditions with properties but no equal
     * method defined.
     */
    public void testMutatesTo_NormalWithPropertyNoEqualMethod() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(
                new String[] { "name" });

        assertFalse(pd.mutatesTo(new Object(), new Object() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        }));
    }

    /*
     * Tests mutatesTo() under normal conditions with null properties and equal
     * method defined.
     */
    public void testMutatesTo_NormalWithNullPropertyPublicEqualMethod() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(
                new String[] { null });

        assertFalse(pd.mutatesTo("test1", "test2"));
    }

    /*
     * Tests mutatesTo() under normal conditions with empty properties and equal
     * method defined.
     */
    public void testMutatesTo_NormalWithEmptyPropertyPublicEqualMethod() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(new String[0]);

        assertTrue(pd.mutatesTo("test1", "test1"));
    }

    /*
     * Tests mutatesTo() under normal conditions with properties and equal
     * method defined.
     */
    public void testMutatesTo_NormalWithPropertyPublicEqualMethod() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(
                new String[] { "name" });

        assertFalse(pd.mutatesTo("test1", "test2"));
        assertTrue(pd.mutatesTo("test1", "test1"));
    }

    /*
     * Tests mutatesTo() under normal conditions with properties and protected
     * equal method defined.
     */
    public void testMutatesTo_NormalWithPropertyProtectedEqualMethod() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate(
                new String[] { "name" });

        assertFalse(pd.mutatesTo(new MockPersistenceDelegate(), "test"));
    }

    /*
     * Tests mutatesTo() with null parameters.
     */
    public void testMutatesTo_Null() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate();

        assertFalse(pd.mutatesTo("test", null));
        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo(null, "test"));
    }
    
    //Regression for HARMONY-3782
    public void test_mutates_with_equals_true() {
        MyObjectEqualsTrue o1 = new MyObjectEqualsTrue();
        MyObjectEqualsTrue o2 = new MyObjectEqualsTrue();
        MyDefaultPersistenceDelegate myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate();
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertFalse(o1.equalsCalled);        
        
        o1 = new MyObjectEqualsTrue();
        o2 = new MyObjectEqualsTrue();
        myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate(new String[0]);
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertFalse(o1.equalsCalled);
        
        o1 = new MyObjectEqualsTrue();
        o2 = new MyObjectEqualsTrue();
        myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate(new String[]{"TEST_ARUGMENT_NAME"});
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertTrue(o1.equalsCalled);              
    }
    
    public void test_mutatesTo_Object() {
        Object o1 = new Object();
        Object o2 = new Object();
        MockPersistenceDelegate mockPersistenceDelegate = new MockPersistenceDelegate();
        assertTrue(mockPersistenceDelegate.mutatesTo(o1, o2));
    }
    
    public void test_initialize() {
        MockBean3 bean1 = new MockBean3();
        bean1.setValue("bean1");
        MockBean3 bean2 = new MockBean3();
        bean2.setValue("bean2");

        // clear flags
        bean1.setValueCalled = false;
        bean2.setValueCalled = false;

        MockPersistenceDelegate mockPersistenceDelegate = new MockPersistenceDelegate();
        mockPersistenceDelegate.initialize(MockBean3.class, bean1, bean2,
                new Encoder());
        assertEquals("bean1", bean1.getValue());
        assertEquals("bean2", bean2.getValue());
        assertFalse(bean1.setValueCalled);
        assertFalse(bean2.setValueCalled);
    }
    
    public void test_mutates_with_equals_false() {
        MyObjectEqualsFalse o1 = new MyObjectEqualsFalse();
        MyObjectEqualsFalse o2 = new MyObjectEqualsFalse();
        MyDefaultPersistenceDelegate myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate();
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertFalse(o1.equalsCalled);
        
        o1 = new MyObjectEqualsFalse();
        o2 = new MyObjectEqualsFalse();
        myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate(new String[0]);
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertFalse(o1.equalsCalled);
        
        o1 = new MyObjectEqualsFalse();
        o2 = new MyObjectEqualsFalse();
        myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate(new String[]{"TEST_ARUGMENT_NAME"});
        assertFalse(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertTrue(o1.equalsCalled);        
    }
    
    public void test_mutates_with_no_equals()
    {
        MyObjectNoExplicitEquals o1 = new MyObjectNoExplicitEquals();
        MyObjectNoExplicitEquals o2 = new MyObjectNoExplicitEquals();
        MyDefaultPersistenceDelegate myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate();
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertFalse(o1.equalsCalled);
        
        o1 = new MyObjectNoExplicitEquals();
        o2 = new MyObjectNoExplicitEquals();
        myDefaultPersistenceDelegate = new MyDefaultPersistenceDelegate(new String[]{"TEST_ARUGMENT_NAME"});
        assertTrue(myDefaultPersistenceDelegate.mutatesTo(o1, o2));
        assertFalse(o1.equalsCalled);
    }
    
    public class MyDefaultPersistenceDelegate extends
            DefaultPersistenceDelegate {
        public MyDefaultPersistenceDelegate() {
            super();
        }

        public MyDefaultPersistenceDelegate(String[] constructorPropertyNames) {
            super(constructorPropertyNames);
        }

        public boolean mutatesTo(Object oldInstance, Object newInstance) {
            return super.mutatesTo(oldInstance, newInstance);
        }
    }

    public class MyObjectEqualsTrue extends Object {
        public boolean equalsCalled = false;

        @Override
        public boolean equals(Object object) {
            equalsCalled = true;
            return true;
        }
    }

    public class MyObjectEqualsFalse extends Object {
        public boolean equalsCalled = false;

        @Override
        public boolean equals(Object object) {
            equalsCalled = true;
            return false;
        }
    }

    public class MyObjectNoExplicitEquals extends MyObjectEqualsFalse {

    }

    /*
     * Test initialize() under normal conditions with a bean that does not have
     * bean info class.
     */
    public void testInitialize_Normal() throws Exception {
        CollectingEncoder enc;
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        MockFoo oldBean;
        MockFoo newBean;

        enc = new CollectingEncoder();
        oldBean = new MockFoo();
        oldBean.setName("myName");
        oldBean.setLabel("myLabel");
        pd.writeObject(oldBean, enc);
        enc.clearCache();
        pd.initialize(MockFoo.class, oldBean, new MockFoo(), enc);

        assertNotNull(findStatement(enc.statements(), oldBean, "setName",
                new Object[] { oldBean.getName() }));
        assertNotNull(findStatement(enc.statements(), oldBean, "setLabel",
                new Object[] { oldBean.getLabel() }));

        enc = new CollectingEncoder();
        oldBean = new MockFoo();
        oldBean.setComplexLabel(new MockFooLabel("myComplexLabel"));
        pd.writeObject(oldBean, enc);
        newBean = new MockFoo();
        newBean.setComplexLabel(new MockFooLabel("complexLabel2"));
        pd.writeObject(newBean, enc);
        enc.clearCache();
        pd.initialize(MockFoo.class, oldBean, newBean, enc);
    }

    /*
     * Test initialize() under normal conditions with a bean that does have bean
     * info class.
     */
    public void testInitialize_NormalBeanInfo() throws Exception {
        CollectingEncoder enc = new CollectingEncoder();
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        MockFoo2 b = new MockFoo2(2);
        MockFoo2 b2 = new MockFoo2(3);
        Iterator<Statement> iter;

        pd.writeObject(b, enc);
        pd.writeObject(b2, enc);
        enc.clearCache();
        pd.initialize(MockFoo2.class, b, b2, enc);

        // XXX RI stores much more statements to the stream
        iter = enc.statements();
        // assertNotNull("required statement not found",
        // findStatement(iter, b, "myget", null));
        assertNotNull("required statement not found", findStatement(iter, null,
                "myset", new Object[] { new Integer(2) }));
    }

    /*
     * Test initialize() when oldInstance == newInstance. XXX The current
     * implementation outputs nothing to the stream. And this seems to be
     * correct from the spec point of view since we need not to do any actions
     * to convert the object to itself. However, RI outputs a lot of stuff to
     * the stream here.
     */
    // public void testInitialize_SameInstance() throws Exception {
    // CollectingEncoder enc = new CollectingEncoder();
    // MockPersistenceDelegate pd = new MockPersistenceDelegate();
    // MockFoo b = new MockFoo();
    // Iterator<Statement> iter;
    //        
    // b.setName("mymyName");
    // // b.setLabel("myLabel");
    //
    // pd.initialize(MockFoo.class, b, b, enc);
    //
    // }
    /*
     * Test initialize() with a bean with a transient property.
     */
    public void testInitialize_TransientProperty() throws Exception {
        CollectingEncoder enc = new CollectingEncoder();
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        MockTransientBean b = new MockTransientBean();

        b.setName("myName");
        pd.writeObject(b, enc);
        enc.clearCache();
        pd.initialize(MockTransientBean.class, b, new MockTransientBean(), enc);
        assertFalse("transient fields should not be affected", enc.statements()
                .hasNext());

        // set transient to false
        Introspector.flushCaches();
        MockTransientBeanBeanInfo.setTransient(false);
        pd.initialize(MockTransientBean.class, b, new MockTransientBean(), enc);
        assertTrue(enc.statements().hasNext());
    }

    /*
     * Test initialize() with a bean with no setter.
     */
    public void testInitialize_NoSetter() throws Exception {
        MockEncoder enc = new MockEncoder();
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        MockNoSetterBean b = new MockNoSetterBean();
        enc.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                CallVerificationStack.getInstance().push(e);
            }
        });
        b.setName("myName");
        pd.initialize(MockNoSetterBean.class, b, new MockNoSetterBean(), enc);
        assertTrue(CallVerificationStack.getInstance().empty());

        pd = new MockPersistenceDelegate(new String[] { "name" });
        enc.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                CallVerificationStack.getInstance().push(e);
            }
        });
        b.setName("myName");
        pd.initialize(MockNoSetterBean.class, b, new MockNoSetterBean(), enc);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test initialize() with a bean with no getter.
     */
    public void testInitialize_NoGetter() throws Exception {
        MockEncoder enc = new MockEncoder();
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        MockNoGetterBean b = new MockNoGetterBean();

        b.setName("myName");
        enc.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                CallVerificationStack.getInstance().push(e);
            }
        });
        enc.writeObject(b);
        CallVerificationStack.getInstance().clear();
        MockNoGetterBean b2 = (MockNoGetterBean) enc.get(b);
        b2.setName("yourName");
        b2.setLabel("hehe");
        pd.initialize(MockNoGetterBean.class, b, b2, enc);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Tests initialize() with null class.
     */
    public void testInitialize_NullClass() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        Encoder enc = new Encoder();
        Object o1 = new Object();
        Object o2 = new Object();
        // enc.setPersistenceDelegate(MockFooStop.class,
        // new MockPersistenceDelegate());
        try {
            enc.setExceptionListener(new ExceptionListener() {
                public void exceptionThrown(Exception e) {
                    CallVerificationStack.getInstance().push(e);
                }
            });
            pd.initialize(null, o1, o2, enc);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Tests initialize() with null old and new instances.
     */
    public void testInitialize_NullInstances() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        Encoder enc = new Encoder();
        MockFoo b = new MockFoo();
        b.setName("myName");
        // enc.setPersistenceDelegate(MockFooStop.class,
        // new MockPersistenceDelegate());
        enc.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                CallVerificationStack.getInstance().push(e);
            }
        });
        try {
            pd.initialize(MockFoo.class, null, b, enc);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
        assertTrue(CallVerificationStack.getInstance().empty());
        pd.initialize(MockFoo.class, b, null, enc);
        assertFalse(CallVerificationStack.getInstance().empty());
    }

    /*
     * Tests initialize() with null encoder.
     */
    public void testInitialize_NullEncoder() {
        MockPersistenceDelegate pd = new MockPersistenceDelegate();
        Object o1 = new Object();
        Object o2 = new Object();
        try {
            pd.initialize(MockFoo.class, o1, o2, null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Tests array persistence delegate
     */
    public void testArrayPD_Normal() {
        Encoder enc = new MockEncoder();
        int[] ia = new int[] { 1 };
        PersistenceDelegate pd = enc.getPersistenceDelegate(ia.getClass());
        pd.writeObject(ia, enc);
    }

    class MockDefaultPersistenceDelegate extends DefaultPersistenceDelegate {
        public MockDefaultPersistenceDelegate(String[] args) {
            super(args);
        }

        public boolean mockMutatesTo(Object obj1, Object obj2) {
            return mutatesTo(obj1, obj2);
        }
    }

    public void test_MutatesTo_scenario1() throws Exception {
        MockDefaultPersistenceDelegate mockDPD = new MockDefaultPersistenceDelegate(
                new String[1]);
        try {
            mockDPD.mockMutatesTo((Object) null, (Object) null);
            fail("should throw NPE");
        } catch (NullPointerException e) {
            // Expected
        }

        try {
            mockDPD.mockMutatesTo((Object) null, (Object) "");
            fail("should throw NPE");
        } catch (NullPointerException e) {
            // Expected
        }

        assertFalse(mockDPD.mockMutatesTo((Object) "", (Object) null));
    }

    public void test_MutatesTo_scenario2() throws Exception {
        MockDefaultPersistenceDelegate mockDPD = new MockDefaultPersistenceDelegate(
                new String[0]);
        assertFalse(mockDPD.mockMutatesTo((Object) null, (Object) null));
        assertFalse(mockDPD.mockMutatesTo((Object) null, (Object) ""));
        assertFalse(mockDPD.mockMutatesTo((Object) "", (Object) null));
        assertTrue(mockDPD.mockMutatesTo((Object) "", (Object) ""));
    }

    /*
     * BeanInfo for the MockBean below.
     */
    public static class MockNoGetterBeanBeanInfo extends SimpleBeanInfo {

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            try {
                PropertyDescriptor pd = new PropertyDescriptor("name", null,
                        MockNoGetterBean.class.getMethod("setName",
                                new Class[] { String.class }));
                PropertyDescriptor pd2 = new PropertyDescriptor("ii", null,
                        MockNoGetterBean.class.getMethod("setII",
                                new Class[] { Integer.class }));
                return new PropertyDescriptor[] { pd, pd2 };
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /*
     * Mock bean with no getter.
     */
    public static class MockNoGetterBean extends MockFoo {
        private static final long serialVersionUID = 5528972237047564849L;

        public void setII(Integer i) {
        }
    }

    /*
     * Mock bean with no getter.
     */
    public static class MockNoGetterBean2 {
        public MockNoGetterBean2(int i) {
        }

        public void setI(int i) {
        }
    }

    /*
     * BeanInfo for the MockBean below.
     */
    public static class MockNoSetterBeanBeanInfo extends SimpleBeanInfo {

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            try {
                PropertyDescriptor pd = new PropertyDescriptor("name",
                        MockNoSetterBean.class.getMethod("getName",
                                (Class[]) null), null);
                return new PropertyDescriptor[] { pd };
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /*
     * Mock bean with no setter.
     */
    public static class MockNoSetterBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /*
     * BeanInfo for the MockBean below.
     */
    public static class MockTransientBeanBeanInfo extends SimpleBeanInfo {

        private static boolean trans = true;

        public static void setTransient(boolean b) {
            trans = b;
        }

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            try {
                PropertyDescriptor pd = new PropertyDescriptor("name",
                        MockTransientBean.class);
                pd.setValue("transient", new Boolean(trans));
                return new PropertyDescriptor[] { pd };
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /*
     * Mock bean with a transient property.
     */
    public static class MockTransientBean extends MockFoo {

        private static final long serialVersionUID = 6924332068595493238L;
    }

    /*
     * Mock bean.
     */
    public static class MockBean {
        private transient String prop1;

        private transient int prop2;

        public String getProp1() {
            return this.prop1;
        }

        public int getProp2() {
            return this.prop2;
        }

        public int getProp3() {
            return this.prop2;
        }

        public void setProp3() {
            // empty
        }

        public int getProp4(int i) {
            return this.prop2;
        }

        public int getProp5() throws Exception {
            throw new Exception();
        }

        public int getProp7() {
            throw new Error();
        }

        public void setAll(String prop1, int prop2) {
            this.prop1 = prop1;
            this.prop2 = prop2;
        }

        public int get() {
            return this.prop2;
        }

    }

    /*
     * Mock DefaultPersistenceDelegate subclass.
     */
    static class MockPersistenceDelegate extends DefaultPersistenceDelegate {

        public MockPersistenceDelegate() {
            super();
        }

        public MockPersistenceDelegate(String[] props) {
            super(props);
        }

        @Override
        public Expression instantiate(Object oldInstance, Encoder out) {
            return super.instantiate(oldInstance, out);
        }

        @Override
        public void initialize(Class<?> type, Object oldInstance,
                Object newInstance, Encoder enc) {
            super.initialize(type, oldInstance, newInstance, enc);
        }

        @Override
        public boolean mutatesTo(Object oldInstance, Object newInstance) {
            return super.mutatesTo(oldInstance, newInstance);
        }

        protected boolean equals(String o) {
            return true;
        }

    }

    /*
     * Mock Encoder.
     */
    static class MockEncoder extends Encoder {

        @Override
        public ExceptionListener getExceptionListener() {
            return super.getExceptionListener();
        }

        @Override
        public PersistenceDelegate getPersistenceDelegate(Class<?> type) {
            return super.getPersistenceDelegate(type);
        }

        @Override
        public void setExceptionListener(ExceptionListener exceptionListener) {
            super.setExceptionListener(exceptionListener);
        }

        @Override
        public void setPersistenceDelegate(Class<?> type,
                PersistenceDelegate persistenceDelegate) {
            super.setPersistenceDelegate(type, persistenceDelegate);
        }

        private void recordCall(Object param) {
            StackTraceElement[] eles = (new Throwable()).getStackTrace();
            int i = 0;
            // skip Throwable.init()
            while (eles[i].getClassName().equals("java.lang.Throwable")) {
                i++;
            }
            // skip calls from MockEncoder
            while (eles[i].getClassName().equals(MockEncoder.class.getName())) {
                i++;
            }
            // skip calls from DefaultPersistenceDelegate & PersistenceDelegate
            while (eles[i].getClassName().equals(
                    DefaultPersistenceDelegate.class.getName())
                    || eles[i].getClassName().equals(
                            PersistenceDelegate.class.getName())) {
                i++;
            }
            if (i > 2
                    && eles[++i].getClassName().equals(
                            DefaultPersistenceDelegateTest.class.getName())) {
                CallVerificationStack.getInstance().push(param);
            }
        }

        @Override
        public Object get(Object oldInstance) {
            recordCall(oldInstance);
            return super.get(oldInstance);
        }

        @Override
        public Object remove(Object oldInstance) {
            recordCall(oldInstance);
            return super.remove(oldInstance);
        }

        @Override
        public void writeExpression(Expression oldExp) {
            recordCall(oldExp);
            super.writeExpression(oldExp);
        }

        @Override
        public void writeStatement(Statement oldStm) {
            recordCall(oldStm);
            super.writeStatement(oldStm);
        }

        @Override
        public void writeObject(Object o) {
            recordCall(o);
            super.writeObject(o);
        }
    }

    /**
     * Searches for the statement with given parameters.
     * 
     * @param iter
     *            iterator to search through, null means ignore this parameter
     * @param target
     * @param methodName
     * @param args
     * @return found statement or null
     */
    static Statement findStatement(Iterator<Statement> iter, Object target,
            String methodName, Object[] args) {

        while (iter.hasNext()) {
            Statement stmt = iter.next();

            if (target != null && stmt.getTarget() != target) {
                continue;
            }

            if (methodName != null && !methodName.equals(stmt.getMethodName())) {
                continue;
            }

            if (args != null) {
                if ((stmt.getArguments() != null && args.length != stmt
                        .getArguments().length)
                        || stmt.getArguments() == null) {
                    continue;
                }

                for (int i = 0; i < args.length; i++) {
                    if ((args[i] == null && stmt.getArguments()[i] != null)
                            || (args[i] != null && stmt.getArguments()[i] == null)
                            || !args[i].equals(stmt.getArguments()[i])) {
                        continue;
                    }
                }
            }

            return stmt;
        }

        return null;
    }

    public static class CollectingEncoder extends Encoder {
        private Vector<Statement> statements = new Vector<Statement>();

        @Override
        public void writeExpression(Expression exp) {
            statements.add(exp);
            super.writeExpression(exp);
        }

        @Override
        public void writeStatement(Statement stm) {
            statements.add(stm);
            super.writeStatement(stm);
        }

        public Iterator<Statement> statements() {
            return statements.iterator();
        }

        public void clearCache() {
            statements = new Vector<Statement>();
        }
    }
    
    public static class MockBean3
    {
        public boolean setValueCalled = false;
        
        public String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
            setValueCalled = true;
        }
    }
}
