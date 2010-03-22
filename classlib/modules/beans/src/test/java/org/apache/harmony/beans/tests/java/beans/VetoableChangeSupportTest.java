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

import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeListenerProxy;
import java.beans.VetoableChangeSupport;
import java.beans.beancontext.BeanContextChildSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextDelegateS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockVetoableChangeListener;
import org.apache.harmony.beans.tests.support.mock.NonSerializedVCListener;
import org.apache.harmony.beans.tests.support.mock.SerializedVCListener;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.util.SerializationTester;

/**
 * Unit test for VetoableChangeSupport
 */
public class VetoableChangeSupportTest extends TestCase {

    /*
     * Constructor a VetoableChangeSupport instance with normal input
     */
    public void testVetoableChangeSupport() {
        MockSource source = new MockSource();
        new VetoableChangeSupport(source);

    }

    /*
     * Class under test for void addVetoableChangeListener(String,
     * VetoableChangeListener)
     */
    public void testAddVetoableChangeListenerStringVetoableChangeListener() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);

        assertTrue(support.hasListeners(propertyName));
        assertFalse(support.hasListeners("text"));

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners(propertyName);
        VetoableChangeListener[] listeners3 = support
                .getVetoableChangeListeners("text");

        assertEquals(0, listeners3.length);
        assertEquals(1, listeners1.length);
        assertEquals(1, listeners2.length);

        assertSame(proxy, listeners2[0]);
        VetoableChangeListenerProxy wrappers = (VetoableChangeListenerProxy) listeners1[0];
        assertSame(proxy, wrappers.getListener());
        assertEquals(propertyName, wrappers.getPropertyName());

    }

    /*
     * add a null listener
     */
    public void testAddVetoableChangeListenerStringVetoableChangeListener_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, null);
        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners(propertyName);

        assertFalse(support.hasListeners(propertyName));
        assertFalse(support.hasListeners("text"));

        assertEquals(0, listeners1.length);
        assertEquals(0, listeners2.length);
    }

    /*
     * add a listener which has already been added.
     */
    public void testAddVetoableChangeListenerStringVetoableChangeListener_duplicate() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        {
            assertTrue(support.hasListeners(propertyName));
            assertFalse(support.hasListeners("text"));

            VetoableChangeListener[] listeners1 = support
                    .getVetoableChangeListeners();
            VetoableChangeListener[] listeners2 = support
                    .getVetoableChangeListeners(propertyName);
            VetoableChangeListener[] listeners3 = support
                    .getVetoableChangeListeners("text");

            assertEquals(0, listeners3.length);
            assertEquals(1, listeners1.length);
            assertEquals(1, listeners2.length);

            assertSame(proxy, listeners2[0]);
            VetoableChangeListenerProxy wrappers = (VetoableChangeListenerProxy) listeners1[0];
            assertSame(proxy, wrappers.getListener());
            assertEquals(propertyName, wrappers.getPropertyName());
        }

        support.addVetoableChangeListener(propertyName, proxy);
        {
            assertTrue(support.hasListeners(propertyName));
            assertFalse(support.hasListeners("text"));

            VetoableChangeListener[] listeners1 = support
                    .getVetoableChangeListeners();
            VetoableChangeListener[] listeners2 = support
                    .getVetoableChangeListeners(propertyName);
            VetoableChangeListener[] listeners3 = support
                    .getVetoableChangeListeners("text");

            assertEquals(0, listeners3.length);
            assertEquals(2, listeners1.length);
            assertEquals(2, listeners2.length);
            for (int i = 0; i < listeners2.length; i++) {
                assertSame(proxy, listeners2[i]);
                VetoableChangeListenerProxy wrappers = (VetoableChangeListenerProxy) listeners1[i];
                assertSame(proxy, wrappers.getListener());
                assertEquals(propertyName, wrappers.getPropertyName());
            }
        }
    }

    /*
     * add listener with null property name.
     */
    public void testAddVetoableChangeListenerStringVetoableChangeListener_property_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        String propertyName = null;
            support.addVetoableChangeListener(propertyName, proxy);
    }

    /*
     * add listeners to an invalid property
     */
    public void testAddVetoableChangeListenerStringVetoableChangeListener_property_invalid() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        String propertyName = "label_invalid";
        support.addVetoableChangeListener(propertyName, proxy);

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners("label_invalid");
        assertEquals(1, listeners1.length);
        assertEquals(1, listeners2.length);
        assertEquals("label_invalid",
                ((VetoableChangeListenerProxy) listeners1[0]).getPropertyName());
        assertSame(proxy, listeners2[0]);
        assertFalse(support.hasListeners("label"));
    }

    /*
     * add different listener with a particular property name.
     */
    public void testAddVetoableChangeListenerStringVetoableChangeListener_property_duplicate() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        support.addVetoableChangeListener(propertyName, proxy2);

        assertTrue(support.hasListeners(propertyName));
        assertFalse(support.hasListeners("text"));

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners(propertyName);
        VetoableChangeListener[] listeners3 = support
                .getVetoableChangeListeners("text");

        assertEquals(0, listeners3.length);
        assertEquals(2, listeners1.length);
        assertEquals(2, listeners2.length);
        for (int i = 0; i < listeners2.length; i++) {
            assertTrue((proxy == listeners2[i]) || (proxy2 == listeners2[i]));
            VetoableChangeListenerProxy wrappers = (VetoableChangeListenerProxy) listeners1[i];
            assertTrue((proxy == wrappers.getListener())
                    || (proxy2 == wrappers.getListener()));
            assertEquals(propertyName, wrappers.getPropertyName());
        }
    }

    /*
     * Class under test for void
     * addVetoableChangeListener(VetoableChangeListener)
     */
    public void testAddVetoableChangeListenerVetoableChangeListener() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);

        assertTrue(support.hasListeners("label"));
        assertTrue(support.hasListeners("text"));

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners("label");
        VetoableChangeListener[] listeners3 = support
                .getVetoableChangeListeners("text");

        assertEquals(1, listeners1.length);
        assertEquals(0, listeners2.length);
        assertEquals(0, listeners3.length);

        assertSame(proxy, listeners1[0]);
    }

    /*
     * add a null listener
     */
    public void testAddVetoableChangeListenerVetoableChangeListener_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);
        support.addVetoableChangeListener(null);

        assertFalse(support.hasListeners("label"));
        assertFalse(support.hasListeners("text"));

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners("label");
        VetoableChangeListener[] listeners3 = support
                .getVetoableChangeListeners("text");

        assertEquals(0, listeners1.length);
        assertEquals(0, listeners2.length);
        assertEquals(0, listeners3.length);
    }

    /*
     * add duplicated listeners
     */
    public void testAddVetoableChangeListenerVetoableChangeListener_duplicate() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy);

        assertTrue(support.hasListeners("label"));
        assertTrue(support.hasListeners("text"));

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners("label");
        VetoableChangeListener[] listeners3 = support
                .getVetoableChangeListeners("text");

        assertEquals(2, listeners1.length);
        assertEquals(0, listeners2.length);
        assertEquals(0, listeners3.length);
        for (VetoableChangeListener element : listeners1) {
            assertSame(proxy, element);
        }
    }

    /*
     * add two different listeners
     */
    public void testAddVetoableChangeListenerVetoableChangeListener_TwoDifferent() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "getText");
        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy2);

        assertTrue(support.hasListeners("label"));
        assertTrue(support.hasListeners("text"));

        VetoableChangeListener[] listeners1 = support
                .getVetoableChangeListeners();
        VetoableChangeListener[] listeners2 = support
                .getVetoableChangeListeners("label");
        VetoableChangeListener[] listeners3 = support
                .getVetoableChangeListeners("text");

        assertEquals(2, listeners1.length);
        assertEquals(0, listeners2.length);
        assertEquals(0, listeners3.length);
        for (VetoableChangeListener element : listeners1) {
            assertTrue((proxy == element) || (proxy2 == element));
        }
    }

    /*
     * add a VetoableChangeListenerProxy
     */
    public void testAddVetoableChangeListenerVetoableChangeListener_Proxy() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        proxy = new MockVetoableChangeListener();
        String propertyName = "label";

        VetoableChangeListenerProxy listenerProxy = new VetoableChangeListenerProxy(
                propertyName, proxy);
        assertFalse(support.hasListeners("label"));
        try{
            support.addVetoableChangeListener(listenerProxy);
            fail("should throw NPE");
        }catch(NullPointerException e){
            //expected
            e.printStackTrace();
        }
        assertTrue(support.hasListeners("label"));
        assertTrue(support.hasListeners(propertyName));
        assertFalse(support.hasListeners("text"));
        
        {
            VetoableChangeListener[] listeners1 = support
                    .getVetoableChangeListeners();
            VetoableChangeListener[] listeners2 = support
                    .getVetoableChangeListeners(propertyName);
            VetoableChangeListener[] listeners3 = support
                    .getVetoableChangeListeners("text");

            assertEquals(0, listeners3.length);
            assertEquals(1, listeners1.length);
            assertEquals(1, listeners2.length);

            assertSame(proxy, listeners2[0]);
            VetoableChangeListenerProxy wrappers = (VetoableChangeListenerProxy) listeners1[0];
            assertSame(proxy, wrappers.getListener());
            assertEquals(propertyName, wrappers.getPropertyName());
        }
        // add test for remove proxy
        support.removeVetoableChangeListener(listenerProxy);
        {
            VetoableChangeListener[] listeners1 = support
                    .getVetoableChangeListeners();
            VetoableChangeListener[] listeners2 = support
                    .getVetoableChangeListeners(propertyName);
            assertEquals(0, listeners1.length);
            assertEquals(0, listeners2.length);
        }

    }

    /*
     * Class under test for void fireVetoableChange(PropertyChangeEvent)
     */
    public void testFireVetoableChangePropertyChangeEvent()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(source, "label",
                "Label: old", "Label: new");
        support.fireVetoableChange(event);

        assertEquals("called", source.getText());
    }

    public void testFireVetoableChangePropertyChangeEvent_Veto()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        MockVetoListener normal1 = new MockVetoListener(false, "Norm1");
        support.addVetoableChangeListener(normal1);

        MockVetoListener veto1 = new MockVetoListener(true, "Veto1");
        support.addVetoableChangeListener(veto1);

        MockVetoListener normal2 = new MockVetoListener(false, "Norm2");
        support.addVetoableChangeListener(normal2);

        MockVetoListener veto2 = new MockVetoListener(true, "Veto2");
        support.addVetoableChangeListener(veto2);

        PropertyChangeEvent event = new PropertyChangeEvent(source, "label",
                "1", "5");
        try {
            support.fireVetoableChange(event);
        } catch (PropertyVetoException e) {

        }

        assertEquals(4, support.getVetoableChangeListeners().length);

        // Check the calling status of the first listener
        {
            ArrayList<Object> oldValues = normal1.getOldValues();
            assertEquals(2, oldValues.size());
            assertEquals("1", oldValues.get(0));
            assertEquals("5", oldValues.get(1));

            ArrayList<Object> newValues = normal1.getNewValues();
            assertEquals(2, newValues.size());
            assertEquals("5", newValues.get(0));
            assertEquals("1", newValues.get(1));
        }

        // Check the status of the second (Veto) listeners
        {
            ArrayList<Object> oldValues = veto1.getOldValues();
            assertEquals(2, oldValues.size());
            assertEquals("1", oldValues.get(0));
            assertEquals("5", oldValues.get(1));

            ArrayList<Object> newValues = veto1.getNewValues();
            assertEquals(2, newValues.size());
            assertEquals("5", newValues.get(0));
            assertEquals("1", newValues.get(1));
        }

        // Check the status of the third listeners
        {
            ArrayList<Object> oldValues = normal2.getOldValues();
            assertEquals(1, oldValues.size());
            assertEquals("5", oldValues.get(0));

            ArrayList<Object> newValues = normal2.getNewValues();
            assertEquals(1, newValues.size());
            assertEquals("1", newValues.get(0));
        }

        // Check the status of the fourth (Veto) listeners
        {
            ArrayList<Object> oldValues = veto2.getOldValues();
            assertEquals(1, oldValues.size());
            assertEquals("5", oldValues.get(0));

            ArrayList<Object> newValues = veto2.getNewValues();
            assertEquals(1, newValues.size());
            assertEquals("1", newValues.get(0));
        }
    }

    /*
     * fire a null event
     */
    public void testFireVetoableChangePropertyChangeEvent_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        try {
            support.fireVetoableChange(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * register for one property
     */
    public void testFireVetoableChangePropertyChangeEvent_property()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(source, "label",
                "Label: old", "Label: new");
        support.fireVetoableChange(event);

        assertEquals("called", source.getText());
    }

    public void testFireVetoableChangePropertyChangeEvent_property_invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label_invalid";
        support.addVetoableChangeListener(propertyName, proxy);
        PropertyChangeEvent event = new PropertyChangeEvent(source, "label",
                "Label: old", "Label: new");
        support.fireVetoableChange(event);

        assertEquals("text.default", source.getText());
    }

    /*
     * there are two same listeners, and another different listener.
     */
    public void testFireVetoableChangePropertyChangeEvent_DuplicateListener()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "increaseTop");
        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy2);

        PropertyChangeEvent event = new PropertyChangeEvent(source, "label",
                "Label: old", "Label: new");
        support.fireVetoableChange(event);
        assertEquals(2, source.getTop());
        assertEquals("called", source.getText());
    }

    /*
     * listener is null
     */
    public void testFireVetoableChangePropertyChangeEvent_listener_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        support.addVetoableChangeListener(null);
        PropertyChangeEvent event = new PropertyChangeEvent(source, "label",
                "Label: old", "Label: new");
            support.fireVetoableChange(event);
    }

    /*
     * Class under test for void fireVetoableChange(String, boolean, boolean)
     */
    public void testFireVetoableChangeStringbooleanboolean()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", true, false);

        assertEquals("called", source.getText());
    }

    /*
     * register a null listener
     */
    public void testFireVetoableChangeStringbooleanboolean_listener_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        support.addVetoableChangeListener(null);
            support.fireVetoableChange("label", true, false);
    }

    /*
     * register listener for property "label".
     */
    public void testFireVetoableChangeStringbooleanboolean_property()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label", true, false);

        assertEquals("called", source.getText());
    }

    /*
     * register a null listener
     */
    public void testFireVetoableChangeStringbooleanboolean_listener_null_property()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        EventHandler.create(VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", null);
            support.fireVetoableChange("label", true, false);
    }

    /*
     * two different listeners registered for all
     */
    public void testFireVetoableChangeStringbooleanboolean_twoListeners()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "increaseTop");
        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy2);

        support.fireVetoableChange("label", true, false);

        assertEquals("called", source.getText());
        assertEquals(1, source.getTop());
    }

    /*
     * two different listeners registered for property "label"
     */
    public void testFireVetoableChangeStringbooleanboolean_property_twoListeners()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "increaseTop");
        support.addVetoableChangeListener("label", proxy);
        support.addVetoableChangeListener("label", proxy2);

        support.fireVetoableChange("label", true, false);

        assertEquals("called", source.getText());
        assertEquals(1, source.getTop());
    }

    /*
     * null propertyname
     */
    public void testFireVetoableChangeStringbooleanboolean_Property_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "label", "source.text");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange(null, true, false);
        assertEquals(source.getText(), source.getLabel());
    }

    /*
     * register listener to label.
     */
    public void testFireVetoableChangeStringbooleanboolean_listener_Property_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "label", "source.text");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange(null, true, false);
        assertEquals("label.default", source.getLabel());
    }

    /*
     * register a null listener
     */
    public void testFireVetoableChangeStringbooleanboolean_listener_Null_Property_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "label", "source.text");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange(null, true, false);
        assertEquals("label.default", source.getLabel());
    }

    /*
     * invalid propertyname
     */
    public void testFireVetoableChangeStringbooleanboolean_Property_invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "label", "source.text");
        support.addVetoableChangeListener(proxy);
        String newText = "new Text value";
        source.setText(newText);

        support.fireVetoableChange("label_invalid", true, false);
        assertEquals(newText, source.getLabel());
    }

    /*
     * register listener for label property
     */
    public void testFireVetoableChangeStringbooleanboolean_listener_Property_invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "label", "source.text");
        support.addVetoableChangeListener("label", proxy);
        String newText = "new Text value";
        source.setText(newText);

        support.fireVetoableChange("text", true, false);
        assertEquals("label.default", source.getLabel());

        support.fireVetoableChange("label_invalid", true, false);
        assertEquals("label.default", source.getLabel());

        support.fireVetoableChange("label", true, false);
        assertEquals(newText, source.getLabel());
    }

    /*
     * oldvalue==newValue
     */
    public void testFireVetoableChangeStringbooleanboolean_SameValue()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);

        support.fireVetoableChange("label", true, true);
        assertEquals("text.default", source.getText());
    }

    /*
     * Class under test for void fireVetoableChange(String, int, int)
     * 
     * register listener for all
     */
    public void testFireVetoableChangeStringintint()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", 1, 2);

        assertEquals("called", source.getText());
    }

    /*
     * Propertyname is null register listener for all
     */
    public void testFireVetoableChangeStringintint_property_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange(null, 1, 2);

        assertEquals("called", source.getText());
    }

    /*
     * property name is invalid register listener for all
     */
    public void testFireVetoableChangeStringintint_property_Invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label_invalid", 1, 2);

        assertEquals("called", source.getText());
    }

    /*
     * oldvalue=newValue register listener for all
     */
    public void testFireVetoableChangeStringintint_SameValue()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", 1, 1);

        assertEquals("text.default", source.getText());
    }

    /*
     * Class under test for void fireVetoableChange(String, int, int)
     * 
     * register listener for one property - "label"
     */
    public void testFireVetoableChangeStringintint_listener()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label", 1, 2);
        assertEquals("called", source.getText());
    }

    /*
     * Propertyname is null register listener for one property - "label"
     */
    public void testFireVetoableChangeStringintint_listener_property_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange(null, 1, 2);

        assertEquals("text.default", source.getText());
    }

    /*
     * property name is invalid register listener for one property - "label"
     */
    public void testFireVetoableChangeStringintint_listener_property_Invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label_invalid", 1, 2);

        assertEquals("text.default", source.getText());
    }

    /*
     * oldvalue=newValue register listener for one property - "label"
     */
    public void testFireVetoableChangeStringintint_listener_SameValue()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label", 1, 1);

        assertEquals("text.default", source.getText());
    }

    public void testFireVetoableChangeStringintint_listener_Invalid_property()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label_invalid", proxy);
        support.fireVetoableChange("label", 1, 2);

        assertEquals("text.default", source.getText());
    }

    /*
     * Class under test for void fireVetoableChange(String, Object, Object)
     * 
     * register listener for all
     */
    public void testFireVetoableChangeStringObjectObject()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", "Label: old", "Label: new");
        assertEquals("called", source.getText());
    }

    /*
     * property name is null. register listener for all
     */
    public void testFireVetoableChangeStringObjectObject_property_Null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange(null, "Label: old", "Label: new");
        assertEquals("called", source.getText());
    }

    /*
     * property name is invalid. register listener for all
     */
    public void testFireVetoableChangeStringObjectObject_property_invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label_invalid", "Label: old", "Label: new");
        assertEquals("called", source.getText());
    }

    /*
     * oldValue=NewValue. register listener for all
     */
    public void testFireVetoableChangeStringObjectObject_SameValue()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", "Label: old", "Label: old");
        assertEquals("text.default", source.getText());
    }

    /*
     * oldValue=NewValue=null. register listener for all
     */
    public void testFireVetoableChangeStringObjectObject_SameValue_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", null, null);
        assertEquals("called", source.getText());
    }

    /*
     * Class under test for void fireVetoableChange(String, Object, Object)
     * 
     * register listener for one property - "label"
     */
    public void testFireVetoableChangeStringObjectObject_listener()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label", "Label: old", "Label: new");
        assertEquals("called", source.getText());
    }

    /*
     * property name is null. register listener for one property - "label"
     */
    public void testFireVetoableChangeStringObjectObject_listener_property_Null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange(null, "Label: old", "Label: new");
        assertEquals("text.default", source.getText());
    }

    /*
     * property name is invalid. register listener for one property - "label"
     */
    public void testFireVetoableChangeStringObjectObject_listener_property_invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label_invalid", "Label: old", "Label: new");
        assertEquals("text.default", source.getText());
    }

    /*
     * oldValue=NewValue. register listener for one property - "label"
     */
    public void testFireVetoableChangeStringObjectObject_listener_SameValue()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener(proxy);
        support.fireVetoableChange("label", "Label: old", "Label: old");
        assertEquals("text.default", source.getText());
    }

    /*
     * oldValue=NewValue=null. register listener for one property - "label"
     */
    public void testFireVetoableChangeStringObjectObject_listener_SameValue_null()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label", proxy);
        support.fireVetoableChange("label", null, null);
        assertEquals("called", source.getText());
    }

    /*
     * register listener to an invalid property
     */
    public void testFireVetoableChangeStringObjectObject_listener_invalid()
            throws PropertyVetoException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        support.addVetoableChangeListener("label_invalid", proxy);
        support.fireVetoableChange("label", "1", "2");
        assertEquals("text.default", source.getText());
    }

    public void testFireVetoableChangeException_revert_event() {
        final VetoableChangeSupport support = new VetoableChangeSupport(
                new Object());
        final StringBuffer sb = new StringBuffer();
        final String A_IN = "a", B_IN = "b", A_THROW = "A", B_THROW = "B";

        support.addVetoableChangeListener(new VetoableChangeListener() {
            public void vetoableChange(PropertyChangeEvent e)
                    throws PropertyVetoException {
                sb.append(A_IN);
                if (sb.length() == 4) {
                    sb.append(A_THROW);
                    throw new PropertyVetoException(A_THROW, e);
                }
            }
        });

        support.addVetoableChangeListener(new VetoableChangeListener() {
            public void vetoableChange(PropertyChangeEvent e)
                    throws PropertyVetoException {
                sb.append(B_IN);
                if (sb.length() == 2) {
                    sb.append(B_THROW);
                    throw new PropertyVetoException(B_THROW, e);
                }
            }
        });

        try {
            support.fireVetoableChange("propName", 0, 1);
        } catch (PropertyVetoException pve) {
            assertEquals("Illegal sequence:" + sb, "abBaAb", sb.toString());
            String message = pve.getMessage();
            assertEquals("Illegal exception:" + message, B_THROW, message);
            return;
        }
        fail("Unreachable path:" + sb);
    }

    /*
     * Class under test for void removeVetoableChangeListener(String,
     * VetoableChangeListener)
     * 
     * register listener for property
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_property() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        assertTrue(support.hasListeners(propertyName));

        support.removeVetoableChangeListener(propertyName, proxy);
        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * register listener for property, two same listeners
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_property_more() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        support.addVetoableChangeListener(propertyName, proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(2, support.getVetoableChangeListeners(propertyName).length);

        support.removeVetoableChangeListener(propertyName, proxy);
        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    /*
     * register listener for property, two different listeners
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_property_diff() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "getText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        support.addVetoableChangeListener(propertyName, proxy2);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(2, support.getVetoableChangeListeners(propertyName).length);

        support.removeVetoableChangeListener(propertyName, proxy);
        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
        assertSame(proxy2, support.getVetoableChangeListeners(propertyName)[0]);
    }

    /*
     * register listener for property, two different listeners
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_listener_diff() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "getText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners(propertyName).length);

        support.removeVetoableChangeListener(propertyName, proxy2);
        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_listener_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, null);

        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);

        support.removeVetoableChangeListener(propertyName, proxy);
        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * register listener for property.
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_all() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(proxy);
        assertTrue(support.hasListeners(propertyName));

        support.removeVetoableChangeListener(propertyName, proxy);
        assertTrue(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    /*
     * remove listener from null property
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_propertyName_Null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        assertTrue(support.hasListeners(propertyName));
            support.removeVetoableChangeListener(null, proxy);
    }

    /*
     * propertyname is invalid
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_propertyName_Invalid() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, proxy);
        assertTrue(support.hasListeners(propertyName));

        support.removeVetoableChangeListener(propertyName + "_invalid", proxy);
        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    /*
     * no listener attached to the property
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_propertyName_NoListener() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        assertFalse(support.hasListeners(propertyName));

        support.removeVetoableChangeListener(propertyName, proxy);
        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * listener null
     */
    public void testRemoveVetoableChangeListenerStringVetoableChangeListener_listener_null_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        String propertyName = "label";
        support.addVetoableChangeListener(propertyName, null);
        assertFalse(support.hasListeners(propertyName));

        support.removeVetoableChangeListener(propertyName, null);
        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * Class under test for void
     * removeVetoableChangeListener(VetoableChangeListener)
     * 
     * register listener for all
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_all() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(proxy);
        assertTrue(support.hasListeners(propertyName));

        support.removeVetoableChangeListener(proxy);
        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * two same listeners
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_all_more() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(2, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    /*
     * two different listeners
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_all_more_diff() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");
        VetoableChangeListener proxy2 = EventHandler.create(
                VetoableChangeListener.class, source, "getText");

        String propertyName = "label";
        support.addVetoableChangeListener(proxy);
        support.addVetoableChangeListener(proxy2);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(2, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
        assertEquals(proxy2, support.getVetoableChangeListeners()[0]);
    }

    /*
     * listener null
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_all_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(null);

        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(proxy);

        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * register for one property
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_property() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener("label", proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    /*
     * listener null
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(proxy);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(1, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(null);

        assertTrue(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(1, support.getVetoableChangeListeners().length);
    }

    /*
     * register null listener, remove null listener.
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_null_null() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        EventHandler.create(VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        support.addVetoableChangeListener(null);

        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(null);

        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    /*
     * Regression test for HARMONY-321
     */
    public void testFireVetoableChange_regression() {
        VetoableChangeSupport vcs = new VetoableChangeSupport(this);
        MockVetoListener2 vlistener = new MockVetoListener2();

        vcs.addVetoableChangeListener(vlistener);
        try {
            vcs.fireVetoableChange(vlistener.vetoedPropName, 0, 1);
            fail("PropertyVetoException expected");
        } catch (PropertyVetoException ok) {
        }

        assertEquals(Integer.valueOf(1), vlistener.event.getOldValue());
        assertEquals(Integer.valueOf(0), vlistener.event.getNewValue());
    }

    /*
     * no this listener
     */
    public void testRemoveVetoableChangeListenerVetoableChangeListener_invalid() {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        VetoableChangeListener proxy = EventHandler.create(
                VetoableChangeListener.class, source, "setText");

        String propertyName = "label";
        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners().length);

        support.removeVetoableChangeListener(proxy);

        assertFalse(support.hasListeners(propertyName));
        assertEquals(0, support.getVetoableChangeListeners(propertyName).length);
        assertEquals(0, support.getVetoableChangeListeners().length);
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        MockSource source = new MockSource();
        VetoableChangeSupport support = new VetoableChangeSupport(source);

        String propertyName1 = "text";
        SerializedVCListener serialized = new SerializedVCListener(
                propertyName1);
        support.addVetoableChangeListener(propertyName1, serialized);

        String propertyName2 = "top";
        NonSerializedVCListener nonSerialized = new NonSerializedVCListener(
                propertyName2);
        support.addVetoableChangeListener(propertyName2, nonSerialized);

        assertTrue(support.hasListeners(propertyName1));
        assertTrue(support.hasListeners(propertyName2));
        assertEquals(2, support.getVetoableChangeListeners().length);
        assertEquals(1,
                support.getVetoableChangeListeners(propertyName1).length);
        assertEquals(1,
                support.getVetoableChangeListeners(propertyName2).length);

        VetoableChangeSupport deserializedSupport = (VetoableChangeSupport) SerializationTester
                .getDeserilizedObject(support);

        assertTrue(deserializedSupport.hasListeners(propertyName1));
        assertFalse(deserializedSupport.hasListeners(propertyName2));
        assertEquals(1, deserializedSupport.getVetoableChangeListeners().length);
        assertEquals(1, deserializedSupport
                .getVetoableChangeListeners(propertyName1).length);
        assertEquals(0, deserializedSupport
                .getVetoableChangeListeners(propertyName2).length);

        assertEquals(
                support.getVetoableChangeListeners(propertyName1)[0],
                deserializedSupport.getVetoableChangeListeners(propertyName1)[0]);

    }

     public void testSerialization_Compatibility() throws Exception {
         MockSource source = new MockSource();
         VetoableChangeSupport support = new VetoableChangeSupport(source);
 
         final String propertyName1 = "text";
         SerializedVCListener serialized = new SerializedVCListener(
                 propertyName1);
         support.addVetoableChangeListener(propertyName1, serialized);
 
         final String propertyName2 = "top";
         NonSerializedVCListener nonSerialized = new NonSerializedVCListener(
                 propertyName2);
         support.addVetoableChangeListener(propertyName2, nonSerialized);
 
         assertTrue(support.hasListeners(propertyName1));
         assertTrue(support.hasListeners(propertyName2));
         assertEquals(2, support.getVetoableChangeListeners().length);
         assertEquals(1,
                 support.getVetoableChangeListeners(propertyName1).length);
         assertEquals(1,
                 support.getVetoableChangeListeners(propertyName2).length);
 
         SerializationTest.verifyGolden(this, support, new SerializableAssert(){
             public void assertDeserialized(Serializable orig, Serializable ser) {
                 VetoableChangeSupport support = (VetoableChangeSupport)orig;
                 VetoableChangeSupport deserializedSupport = (VetoableChangeSupport)ser;
                 
                 assertTrue(deserializedSupport.hasListeners(propertyName1));
                 assertFalse(deserializedSupport.hasListeners(propertyName2));
                 assertEquals(1, deserializedSupport.getVetoableChangeListeners().length);
                 assertEquals(1, deserializedSupport
                         .getVetoableChangeListeners(propertyName1).length);
                 assertEquals(0, deserializedSupport
                         .getVetoableChangeListeners(propertyName2).length);
 
                 assertEquals(
                         support.getVetoableChangeListeners(propertyName1)[0],
                         deserializedSupport.getVetoableChangeListeners(propertyName1)[0]);
             }
         });
     }

    public static class MockSource implements Serializable {

        private static final long serialVersionUID = 2592367737991345105L;

        private String name;

        private String text;

        private String label;

        private int top;

        private boolean visible;

        public MockSource() {
            this.name = getClass().getName();
            this.text = "text.default";
            this.label = "label.default";
        }

        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }

        /**
         * @param name
         *            The name to set.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return Returns the text.
         */
        public String getText() {
            return text;
        }

        /**
         * @param text
         *            The text to set.
         */
        public void setText(String text) {
            this.text = text;
        }

        public void setText() {
            this.text = "called";
        }

        /**
         * @return Returns the top.
         */
        public int getTop() {
            return top;
        }

        /**
         * @param top
         *            The top to set.
         */
        public void setTop(int top) {
            this.top = top;
        }

        public void increaseTop() {
            this.top++;
        }

        /**
         * @return Returns the visible.
         */
        public boolean isVisible() {
            return visible;
        }

        /**
         * @param visible
         *            The visible to set.
         */
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /**
         * @return Returns the label.
         */
        public String getLabel() {
            return label;
        }

        /**
         * @param label
         *            The label to set.
         */
        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class MockVetoListener implements VetoableChangeListener {

        ArrayList<Object> oldValues = new ArrayList<Object>();

        ArrayList<Object> newValues = new ArrayList<Object>();

        boolean veto;

        String prefix;

        public MockVetoListener(boolean veto, String prefix) {
            this.veto = veto;
            this.prefix = prefix;
        }

        public void vetoableChange(PropertyChangeEvent event)
                throws PropertyVetoException {
            oldValues.add(event.getOldValue());
            newValues.add(event.getNewValue());

            if (false) {
                System.out.println(prefix + " old: " + event.getOldValue()
                        + " new: " + event.getNewValue());
            }
            if (veto) {
                throw new PropertyVetoException("Veto Change", event);
            }
        }

        public ArrayList<Object> getOldValues() {
            return this.oldValues;
        }

        public ArrayList<Object> getNewValues() {
            return this.newValues;
        }

    }

    public static class MockVetoListener2 implements VetoableChangeListener {

        public PropertyChangeEvent event;

        public final String vetoedPropName = "prop";

        public void vetoableChange(PropertyChangeEvent e)
                throws PropertyVetoException {

            event = e;
            String propName = e.getPropertyName();

            if (propName.equals(vetoedPropName)
                    && e.getNewValue().equals(new Integer(1))) {
                throw new PropertyVetoException(
                        propName + " change is vetoed!", e);
            }
        }

    }

    /**
     * @tests java.beans.VetoableChangeSupport#VetoableChangeSupport(
     *        java.lang.Object)
     */
    public void testVetoableChangeSupport_null() {
        // Regression for HARMONY-228
        try {
            new VetoableChangeSupport(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests java.beans.VetoableChangeSupport#addVetoableChangeListener(java.lang.String,
     *        java.beans.VetoableChangeListener)
     */
    public void test_addPropertyChangeListenerNullNull() throws Exception {
        // Regression for HARMONY-441
        new VetoableChangeSupport("bean1")
                .addVetoableChangeListener(null, null);
    }

    public void test_readObject() throws Exception {
        // Regression for HARMONY-421
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new BeanContextChildSupport());
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        ois.readObject();

        ois.close();
        oos.close();
    }

    /**
     * The test checks the method add() with no property specified
     */
    public void testAddVetoableChangeListener() {
        VetoableChangeSupport vcs = new VetoableChangeSupport("bean1");
        VetoableChangeListener vcl = new VetoableChangeListener() {

            public void vetoableChange(PropertyChangeEvent pce) {
            }
        };
        vcs.addVetoableChangeListener(vcl);
        VetoableChangeListener[] vcls = vcs.getVetoableChangeListeners();

        assertNotNull("Returned listeners is null.", vcls);
        assertEquals(1, vcls.length);
        assertEquals(vcl, vcls[0]);
    }

    /**
     * The test checks the method add() for property specified
     */
    public void testAddVetoableChangeListenerByPropertyName() {
        VetoableChangeSupport vcs = new VetoableChangeSupport("bean1");
        VetoableChangeListener vcl = new VetoableChangeListener() {

            public void vetoableChange(PropertyChangeEvent pce) {
            }
        };
        vcs.addVetoableChangeListener("property1", vcl);
        VetoableChangeListener[] vcls = vcs
                .getVetoableChangeListeners("property1");

        assertNotNull("Returned listeners is null.", vcls);
        assertEquals(1, vcls.length);
        assertEquals(vcl, vcls[0]);
    }

    /**
     * The test checks the method add() for VetoableChangeListenerProxy
     */
    public void testAddVetoableChangeListenerProxy() {
        VetoableChangeSupport vcs = new VetoableChangeSupport("bean1");
        VetoableChangeListener vcl = new VetoableChangeListener() {

            public void vetoableChange(PropertyChangeEvent pce) {
            }
        };
        vcs.addVetoableChangeListener("property1", vcl);
        VetoableChangeListener[] vcls = vcs.getVetoableChangeListeners();

        assertNotNull("Returned listeners is null.", vcls);
        assertEquals(1, vcls.length);

        assertTrue("Listener is not of VetoableChangeListenerProxy type",
                vcls[0] instanceof VetoableChangeListenerProxy);
        assertEquals(vcl, ((VetoableChangeListenerProxy) vcls[0]).getListener());
        assertEquals("property1", ((VetoableChangeListenerProxy) vcls[0])
                .getPropertyName());
    }
    
    
    public void testSerializationForm(){
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(VetoableChangeSupport.class);
        assertNotNull(objectStreamClass.getField("source"));
        assertNotNull(objectStreamClass.getField("children"));
        assertNotNull(objectStreamClass.getField("vetoableChangeSupportSerializedDataVersion"));
    }
}
