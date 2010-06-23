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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;
import java.util.EventObject;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.harmony.beans.tests.support.SampleEvent;
import org.apache.harmony.beans.tests.support.SampleListener;
import org.apache.harmony.beans.tests.support.mock.MockButton;

/**
 * Unit test for EventHandler.
 */
public class EventHandlerTest extends TestCase {

    private Object object;

    private String methodName;

    private Object[] params;

    private final String text = "something";

    public EventHandlerTest() {
        super();
    }

    public EventHandlerTest(String name) {
        super(name);
    }

    /**
     * The test checks event handler accessors
     */
    public void testAccessors() {
        InvocationObject invocationObject = new InvocationObject();
        EventHandler handler = new EventHandler(invocationObject, "someText",
                "source.text", "actionPerformed");
        assertEquals(invocationObject, handler.getTarget());
        assertEquals("someText", handler.getAction());
        assertEquals("source.text", handler.getEventPropertyName());
        assertEquals("actionPerformed", handler.getListenerMethodName());
    }

    /**
     * The test checks the method invoke() with null listener value
     */
    public void testNullListenerMethodName() throws Exception {
        InvocationObject invocationObject = new InvocationObject();

        EventHandler handler = new EventHandler(invocationObject, "someText",
                "source.text", null);

        Object proxy = EventHandler.create(ActionListener.class,
                invocationObject, "someText", "source.text");

        Method m = ActionListener.class.getMethod("actionPerformed",
                new Class[] { ActionEvent.class });
        handler.invoke(proxy, m, new Object[] { new ActionEvent(this, 0, "") });

        assertEquals(invocationObject.getSomeText(), getText());
    }

    /**
     * The test checks the method invoke()
     */
    public void testInvoke() throws Exception {
        InvocationObject invocationObject = new InvocationObject();

        EventHandler handler = new EventHandler(invocationObject, "someText",
                "source.text", "actionPerformed");

        Object proxy = EventHandler.create(ActionListener.class,
                invocationObject, "someText", "source.text");

        Method m = ActionListener.class.getMethod("actionPerformed",
                new Class[] { ActionEvent.class });
        handler.invoke(proxy, m, new Object[] { new ActionEvent(this, 0, "") });

        assertEquals(invocationObject, handler.getTarget());
        assertEquals(invocationObject.getSomeText(), getText());
        try {
            handler.invoke(proxy, null, new Object[] { new ActionEvent(this, 0,
                    "") });
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /**
     * The test checks the method invoke() with null property name
     */
    public void testInvokeWithNullPropertyName() throws Exception {
        InvocationObject invocationObject = new InvocationObject();

        EventHandler handler = new EventHandler(invocationObject,
                "doSomething", null, null);

        Object proxy = EventHandler.create(SampleListener.class,
                invocationObject, "doSomething");

        Method m = SampleListener.class.getMethod("fireSampleEvent",
                new Class[] { SampleEvent.class });
        handler.invoke(proxy, m, new Object[] { new SampleEvent("") });

        assertEquals(invocationObject, handler.getTarget());
        assertEquals("doSomething", getMethodName());

        // Regression test for HARMONY-4033
        m = FredListener.class.getMethod("fireFredEvent",
                new Class[] { FredEvent.class });
        proxy = EventHandler.create(FredListener.class, invocationObject,
                "doSomething", null, "fireFredEvent");
        m.invoke(proxy, new Object[] { null });
    }

    /**
     * The test checks the object created with the create() method call
     */
    public void testCreateWithMethodCall() {
        Object invocationObject = new InvocationObject();
        ActionListener listener = EventHandler.create(ActionListener.class,
                invocationObject, "doSomething");
        listener.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(getObject(), invocationObject);
        assertEquals("doSomething", getMethodName());

        Object[] params = getParams();
        if (params.length != 0) {
            fail("Number of params should be 0");
        }
    }

    /**
     * The test checks the setter is initialized properly
     */
    public void testCreateWithSetterCall() {
        Object invocationObject = new InvocationObject();
        ActionEvent ae = new ActionEvent(this, 0, "");
        ActionListener listener = EventHandler.create(ActionListener.class,
                invocationObject, "someObject", "source");
        listener.actionPerformed(ae);

        assertEquals(getObject(), invocationObject);
        assertEquals("setSomeObject", getMethodName());

        Object[] params = getParams();
        assertEquals("Number of params should be 1", 1, params.length);
        assertEquals(ae.getSource(), params[0]);
    }

    /**
     * The test checks the object created with the create() method call for
     * dot-separated property
     */
    public void testCreateWithDottedParameterCall() {
        Object invocationObject = new InvocationObject();
        ActionEvent ae = new ActionEvent(this, 0, "");
        ActionListener listener = EventHandler.create(ActionListener.class,
                invocationObject, "someText", "source.text");
        listener.actionPerformed(ae);

        assertEquals(getObject(), invocationObject);
        assertEquals("setSomeText", getMethodName());

        Object[] params = getParams();
        assertEquals("Number of params should be 1", 1, params.length);
        assertEquals(((EventHandlerTest) ae.getSource()).getText(), params[0]);
    }

    /**
     * The test checks the event is fired for object created with the create()
     */
    public void testCreateWithMethodCallWhichIsSetter() {
        InvocationObject invocationObject = new InvocationObject();
        SampleEvent event = new SampleEvent("bean");

        SampleListener listener = EventHandler.create(SampleListener.class,
                invocationObject, "doSomething", "i", null);

        listener.fireSampleEvent(event);

        assertEquals("doSomething", getMethodName());
        assertEquals(event.getI(), invocationObject.getIntValue());
    }

    /**
     * fireSampleEvent scenario
     */
    public void testCreateForStaticMethodAsPropertyGetter() {
        InvocationObject invocationObject = new InvocationObject();
        SampleEvent event = new SampleEvent("bean");

        SampleListener listener = EventHandler.create(SampleListener.class,
                invocationObject, "someValue", "j");

        listener.fireSampleEvent(event);

        assertEquals("setSomeValue", getMethodName());
    }
    
    /**
     * fileSampleEvent scenario by throwing RuntimeException
     */
    public void test_Create_WithThrowRuntimeException() {
        // Regression for Harmony-2434
        InvocationObject invocationObject = new InvocationObject();
        SampleListener listener = EventHandler.create(SampleListener.class,invocationObject, "throwRuntimeException");
        try {
            listener.fireSampleEvent(new SampleEvent("bean"));
            fail("Expected RuntimeException thrown");
        } catch (RuntimeException re) {
            // Expected
        }
    }

    /**
     * 
     */
    public static Test suite() {
        // TestSuite suite = new TestSuite();
        //
        // suite.addTest(new
        // EventHandlerTest("testCreateClassObjectStringStringString_PropertyNull"));
        // return suite;
        return new TestSuite(EventHandlerTest.class);
    }

    /**
     * 
     */
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void logMethodCall(Object object, String methodName, Object[] params) {
        this.object = object;
        this.methodName = methodName;
        this.params = params;
    }

    public String getText() {
        return text;
    }

    private Object getObject() {
        return object;
    }

    private String getMethodName() {
        return methodName;
    }

    private Object[] getParams() {
        return params;
    }

    /*
     * Class under test for Object create(Class, Object, String)
     */
    public void testCreateClassObjectString() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "setCalled");
        button.addPropertyChangeListener(proxy);
        button.setLabel("new label value");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        assertTrue(target.isCalled());
    }

    /*
     * listenerInterface class is null
     */
    public void testCreateClassObjectString_ClassNull() {
        MockTarget target = new MockTarget();
        try {
            EventHandler.create(null, target, "setCalled");
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    /*
     * listenerInterface is not a interface
     */
    public void testCreateClassObjectString_ClassInvalid() {
        MockTarget target = new MockTarget();
        try {
            EventHandler.create(MockButton.class, target, "setCalled");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * the target object is null
     */
    public void testCreateClassObjectString_ObjectNull() {
        try {
            EventHandler
                    .create(PropertyChangeListener.class, null, "setCalled");
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    /*
     * the target's method is null
     */
    public void testCreateClassObjectString_MethodNull() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        try {
            PropertyChangeListener proxy = EventHandler.create(
                    PropertyChangeListener.class, target, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "");
        button.addPropertyChangeListener(proxy);
        try {
            button.setLabel("new label value");
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
    }

    /*
     * the target's method is invalid
     */
    public void testCreateClassObjectString_MethodEmpty() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "");
        button.addPropertyChangeListener(proxy);
        try {
            button.setLabel("new label value");
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
    }

    /*
     * Class under test for Object create(Class, Object, String, String)
     */
    public void testCreateClassObjectStringString() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text", "source.label");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        button.setLabel(newLabel);
        assertEquals(MockButton.defaultName, target.getText());
        button.setLabel("New Value: set text2.");
        assertEquals(newLabel, target.getText());
    }

    /*
     * listenerInterface is null
     */
    public void testCreateClassObjectStringString_ClassNull() {
        MockTarget target = new MockTarget();
        try {
            EventHandler.create(null, target, "text", "source.label");
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /*
     * the target object is null
     */
    public void testCreateClassObjectStringString_TargetNull() {
        try {
            EventHandler.create(PropertyChangeListener.class, null, "text",
                    "source.label");
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /*
     * the action is null
     */
    public void testCreateClassObjectStringString_ActionNull() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        try {
            PropertyChangeListener proxy = EventHandler.create(
                    PropertyChangeListener.class, target, null, "source.label");
            fail("should throw NPE");
        } catch (NullPointerException e) {

        }    
    }

    /*
     * action is invalid
     */
    public void testCreateClassObjectStringString_ActionInvalid() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "action_invalid",
                "source.label");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        try {
            button.setLabel(newLabel);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }
    }

    /*
     * propertyname is null
     */
    public void testCreateClassObjectStringString_PropertyNameNull() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text", null);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        try {
            button.setLabel(newLabel);
            fail("Should throw NullPointerException.");
        } catch (Exception e) {
        }
    }

    /*
     * property name is invalid
     */
    public void testCreateClassObjectStringString_PropertyNameInvalid() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text",
                "source.label_invalid");

        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";

        try {
            button.setLabel(newLabel);
            fail("Should throw NullPointerException.");
        } catch (RuntimeException e) {
        }
    }

    /*
     * Class under test for Object create(Class, Object, String, String, String)
     */
    public void testCreateClassObjectStringStringString() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text", "source.label",
                "propertyChange");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        button.setLabel(newLabel);
        assertEquals(MockButton.defaultName, target.getText());
        button.setLabel("New Value: set text2.");
        assertEquals(newLabel, target.getText());
    }

    /*
     * listenerInterface is null
     */
    public void testCreateClassObjectStringStringString_ClassNull() {
        MockTarget target = new MockTarget();
        try {
            EventHandler.create(null, target, "text", "source.label",
                    "propertyChange");
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {

        }
    }

    /*
     * listenerInterface is invalid
     */
    public void testCreateClassObjectStringStringString_ClassInvalid() {
        MockTarget target = new MockTarget();
        try {
            EventHandler.create(String.class, target, "text", "source.label",
                    "propertyChange");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * the target object is null
     */
    public void testCreateClassObjectStringStringString_TargetNull() {
        try {
            EventHandler.create(PropertyChangeListener.class, null, "text",
                    "source.label", "propertyChange");
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    /*
     * action is null
     */
    public void testCreateClassObjectStringStringString_ActionNull() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        try {
            PropertyChangeListener proxy = EventHandler.create(
                    PropertyChangeListener.class, target, null, "source.label",
                    "propertyChange");
            fail("should throw NPE");
        } catch (NullPointerException e) {

        }        
    }

    /*
     * action is invalid
     */
    public void testCreateClassObjectStringStringString_ActionInvalid() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text_invalid",
                "source.label", "propertyChange");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        try {
            button.setLabel(newLabel);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }
    }

    /*
     * property name is null
     */
    public void testCreateClassObjectStringStringString_PropertyNull() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        String newLabel = "New Value: set text.";
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text", null,
                "propertyChange");

        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        button.addPropertyChangeListener(proxy);

        try {
            button.setLabel(newLabel);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }
    }

    /*
     * property name is invalid
     */
    public void testCreateClassObjectStringStringString_PropertyInvalid() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text",
                "source.label.invalid", "propertyChange");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        try {
            button.setLabel(newLabel);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }
    }

    /*
     * listenerMethodName is null
     */
    public void testCreateClassObjectStringStringString_MethodNull() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text", "source.label",
                null);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        button.setLabel(newLabel);
        assertEquals(MockButton.defaultName, target.getText());
        button.setLabel("New Value: set text2.");
        assertEquals(newLabel, target.getText());
    }

    /*
     * listenerMethodName is invalid
     */
    public void testCreateClassObjectStringStringString_MethodInvalid() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "text", "source.label",
                "propertyChange_invalid");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        button.setLabel(newLabel);
        assertNull(target.getText());
    }

    /*
     * public EventHandler(Object target, String action, String
     * eventPropertyName, String listenerMethodName)
     */
    public void testEventHandler() {
        MockTarget target = new MockTarget();
        String action = "text";
        String eventPropertyName = "source.label";
        String listenerMethodName = "propertyChange";
        EventHandler handler = new EventHandler(target, action,
                eventPropertyName, listenerMethodName);
        assertSame(target, handler.getTarget());
        assertSame(action, handler.getAction());
        assertSame(eventPropertyName, handler.getEventPropertyName());
        assertSame(listenerMethodName, handler.getListenerMethodName());
    }

    /*
     * target is null
     */
    public void testEventHandler_TargetNull() {
        String action = "text";
        String eventPropertyName = "source.label";
        String listenerMethodName = "propertyChange";
        try {
            EventHandler handler = new EventHandler(null, action,
                    eventPropertyName, listenerMethodName);
            fail("should throw NPE");
        } catch (NullPointerException e) {

        }
    }

    /*
     * action is null
     */
    public void testEventHandler_ActionNull() {
        MockTarget target = new MockTarget();
        String eventPropertyName = "source.label";
        String listenerMethodName = "propertyChange";
        try {
            EventHandler handler = new EventHandler(target, null,
                    eventPropertyName, listenerMethodName);
            fail("should throw NPE");
        } catch (NullPointerException e) {

        }
        EventHandler handler = new EventHandler(target, "action",
                eventPropertyName, listenerMethodName);
        assertSame(target, handler.getTarget());
        assertEquals("action", handler.getAction());
        assertSame(eventPropertyName, handler.getEventPropertyName());
        assertSame(listenerMethodName, handler.getListenerMethodName());
    }

    /*
     * EventProperty is null
     */
    public void testEventHandler_EventPropertyNull() {
        MockTarget target = new MockTarget();
        String action = "text";
        String listenerMethodName = "propertyChange";
        EventHandler handler = new EventHandler(target, action, null,
                listenerMethodName);
        assertSame(target, handler.getTarget());
        assertSame(action, handler.getAction());
        assertNull(handler.getEventPropertyName());
        assertSame(listenerMethodName, handler.getListenerMethodName());
    }

    /*
     * Method is null
     */
    public void testEventHandler_MethodNull() {
        MockTarget target = new MockTarget();
        String action = "text";
        String eventPropertyName = "source.label";
        EventHandler handler = new EventHandler(target, action,
                eventPropertyName, null);
        assertSame(target, handler.getTarget());
        assertSame(action, handler.getAction());
        assertSame(eventPropertyName, handler.getEventPropertyName());
        assertNull(handler.getListenerMethodName());
    }

    public void testInvoke_1() throws SecurityException, NoSuchMethodException {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "setCalled");

        String action = "text";
        String eventPropertyName = "source.label";
        EventHandler handler = new EventHandler(target, action,
                eventPropertyName, null);
        Method listenerMethod = PropertyChangeListener.class.getMethod(
                "propertyChange", new Class[] { PropertyChangeEvent.class });
        PropertyChangeEvent event = new PropertyChangeEvent(button, "label",
                "1", "5");
        handler.invoke(proxy, listenerMethod, new Object[] { event });
        assertEquals(button.getLabel(), target.getText());
        Method equalsMethod = Object.class.getMethod("equals",
                new Class[] { Object.class });
        assertEquals(Boolean.FALSE, handler.invoke(proxy, equalsMethod,
                new String[] { "mock" }));
        // Test null method with non-proxy Object.
        handler = new EventHandler(new Object(), "a", "a", "a");
        try {
            handler.invoke(new Object(), null, new Object[] {});
            fail("should throw NPE.");
        } catch (NullPointerException e) {
            // expected;
        }
    }

    public void testIncompatibleMethod() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "Text", "source");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        try {
            button.setLabel(newLabel);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {

        }
    }

    public void testCoverage_1() {
        MockTarget target = new MockTarget();
        MockButton button = new MockButton();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "Text", "");
        assertTrue(Proxy.isProxyClass(proxy.getClass()));

        button.addPropertyChangeListener(proxy);
        String newLabel = "New Value: set text.";
        try {
            button.setLabel(newLabel);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
        }

    }

    public void testInvoke_extend1() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "action1");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("action1", target.getActionRecord());
    }

    public void testInvoke_extend1_1() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "action4");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        try {
            support.firePropertyChange(event);
        } catch (Exception e) {

        }
        assertEquals("action4", target.getActionRecord());
    }

    public void testInvoke_extend2() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "action2");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        try {
            support.firePropertyChange(event);
            fail("Should throw exception");
        } catch (Exception e) {
            // e.printStackTrace();
        }

    }

    public void testInvoke_extend3() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "a", "source.a");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("boolean:true", target.getActionRecord());
    }

    public void testInvoke_extend4() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "b", "source.a");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("Boolean:true", target.getActionRecord());
    }

    public void testInvoke_extend4_BooleanObject() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "a", "source.booleanObject");

        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("boolean:true", target.getActionRecord());
    }

    public void testInvoke_extend5() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "int", "source.int");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("int:1", target.getActionRecord());
    }

    public void testInvoke_extend6() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "char", "source.char");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("char:a", target.getActionRecord());
    }

    public void testInvoke_extend7() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "byte", "source.byte");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("byte:10", target.getActionRecord());
    }

    public void testInvoke_extend8() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "short", "source.short");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("short:100", target.getActionRecord());
    }

    public void testInvoke_extend9() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "long", "source.long");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("long:1000", target.getActionRecord());
    }

    public void testInvoke_extend10() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "float", "source.float");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("float:2.2", target.getActionRecord());
    }

    public void testInvoke_extend11() {
        MockFish fish = new MockFish();
        MockFishTarget target = new MockFishTarget();
        PropertyChangeSupport support = new PropertyChangeSupport(fish);
        Object proxy = EventHandler.create(PropertyChangeListener.class,
                target, "double", "source.double");
        support.addPropertyChangeListener((PropertyChangeListener) proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(fish, "name", "1",
                "5");
        support.firePropertyChange(event);
        assertEquals("double:3.3", target.getActionRecord());
    }

    /**
     * @tests java.beans.EventHandler#create(java.lang.Class<T>,
     *        java.lang.Object, java.lang.String))
     */
    public void testEventHandlerCreate() {
        // Regression for HARMONY-429
        EventHandler.create(FredListener.class, new Untitled1(), "i", "i")
                .fireFredEvent(new FredEvent("bean2"));
    }

    /**
     * Checks some invalid property cases Regression for HARMONY-1884
     * 
     * Note: this test fails on RI and it is considered as Non-Bug Difference, 
     * please refer HARMONY-1884 for details
     */
    public void testInvalidProperties_HY1884() {
        BeanWithInvalidProps bean = new BeanWithInvalidProps();
        Object proxy;

        // "prop1" and "prop2" is neither the name of valid property nor the
        // name of any public method

        // setter without parameter
        proxy = EventHandler
                .create(PropertyChangeListener.class, bean, "prop1");
        try {
            ((PropertyChangeListener) proxy)
                    .propertyChange(new PropertyChangeEvent(bean, "prop1", "1",
                            "2"));
             fail();
        } catch (Exception e) {
            // expected
        }

        // "is" prefix for big Boolean
        proxy = EventHandler.create(PropertyChangeListener.class, bean,
                "goodProp3", "source.prop2");
        try{
            ((PropertyChangeListener) proxy)
                    .propertyChange(new PropertyChangeEvent(bean, "goodProp3",
                            Boolean.TRUE, Boolean.FALSE));
            fail("Should throw NPE");
        }catch(NullPointerException e){
        }
    }

    public static class BeanWithInvalidProps {
        // setter should have a parameter
        public void setProp1() {
        }

        // Introspector doesn't support "is" prefix for big Boolean
        public Boolean isProp2() {
            return new Boolean(true);
        }

        // needed to test prop2
        public void setGoodProp3(boolean value) {
        }
    }

    public interface FredListener extends EventListener {

        public void fireFredEvent(FredEvent event);
    }

    public static class FredEvent extends EventObject {

        private static final long serialVersionUID = 1L;

        private static int i;

        public FredEvent(Object source) {
            super(source);
        }

        public static int getI() {
            return i;
        }

        public static void setI(int j) {
            i = j;
        }
    }

    public static class Untitled1 {

        private int i;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            // System.out.println("Untitled1 : setI()");
            this.i = i;
        }
    }

    public static class MockFish {

        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isA() {
            return true;
        }

        public boolean isBooleanObject() {
            return true;
        }

        public int getInt() {
            return 1;
        }

        public char getChar() {
            return 'a';
        }

        public byte getByte() {
            return 10;
        }

        public short getShort() {
            return 100;
        }

        public long getLong() {
            return 1000;
        }

        public float getFloat() {
            return 2.2f;
        }

        public double getDouble() {
            return 3.3;
        }
    }

    public static class MockFishTarget {

        String actionRecord;

        public void action1() {
            this.actionRecord = "action1";
        }

        public void setAction2(String value) {
            this.actionRecord = "action2";
        }

        public void action4() {
            this.actionRecord = "action4";
        }

        public void action4(EventObject event) {
            this.actionRecord = "action4";
        }

        public String getActionRecord() {
            return actionRecord;
        }

        public void setA(boolean value) {
            this.actionRecord = "boolean:" + Boolean.valueOf(value).toString();
        }

        public void setB(Boolean value) {
            this.actionRecord = "Boolean:" + value.toString();
        }

        public void setInt(int value) {
            this.actionRecord = "int:" + value;
        }

        public void setChar(char value) {
            this.actionRecord = "char:" + value;
        }

        public void setShort(short value) {
            this.actionRecord = "short:" + value;
        }

        public void setByte(byte value) {
            this.actionRecord = "byte:" + value;
        }

        public void setLong(long value) {
            this.actionRecord = "long:" + value;
        }

        public void setFloat(float value) {
            this.actionRecord = "float:" + value;
        }

        public void setDouble(double value) {
            this.actionRecord = "double:" + value;
        }

    }

    public static class MockTarget {

        private boolean called;

        private String text;

        public MockTarget() {
            this.called = false;
        }

        public void setCalled() {
            this.called = true;
        }

        public boolean isCalled() {
            return this.called;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }
    }

    public class InvocationObject {

        private String text;

        private int intValue = -1;

        public InvocationObject() {
        }

        public void doSomething() {
            logMethodCall(this, "doSomething", new Object[] {});
        }

        public void doSomething(int intValue) {
            this.intValue = intValue;

            logMethodCall(this, "doSomething", new Object[] { new Integer(
                    intValue) });
        }

        public void setSomeObject(Object object) {
            logMethodCall(this, "setSomeObject", new Object[] { object });
        }

        public void setSomeText(String text) {
            this.text = text;

            logMethodCall(this, "setSomeText", new Object[] { text });
        }

        public String getSomeText() {
            return text;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;

            logMethodCall(this, "setIntValue", new Object[] { new Integer(
                    intValue) });
        }

        public void setSomeValue(int i) {
            logMethodCall(this, "setSomeValue", new Object[] { new Integer(
                    intValue) });
        }
        
        public void throwRuntimeException() {
            throw new RuntimeException("forced throw RuntimeException");
        }
    }
}
