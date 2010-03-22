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
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;

import junit.framework.TestCase;

/**
 * Unit test of PropertyEditorSupport
 */
public class PropertyEditorSupportTest extends TestCase {

    /**
     * @tests java.beans.PropertyEditorSupport#PropertyEditorSupport()
     */
    public void testPropertyEditorSupport() {
        // Regression for HARMONY-516
        PropertyEditorSupport support = new PropertyEditorSupport();

        assertEquals("null", support.getAsText());
        assertNull(support.getValue());
        assertNull(support.getCustomEditor());
        assertEquals("???", support.getJavaInitializationString());
        assertNull(support.getTags());
        assertFalse(support.supportsCustomEditor());
        assertFalse(support.isPaintable());
        assertSame(support, support.getSource());

        Object value = new String[] { "java.awt.Color.orange" };
        support.setValue(value);
        assertEquals(value.toString(), support.getAsText());
    }

    /**
     * @tests java.beans.PropertyEditorSupport#PropertyEditorSupport(
     *        java.lang.Object)
     */
    public void testPropertyEditorSupportObject() {
        MockSource source = new MockSource();
        PropertyEditorSupport support = new PropertyEditorSupport(source);

        assertEquals("null", support.getAsText());
        assertNull(support.getValue());
        assertNull(support.getCustomEditor());
        assertEquals("???", support.getJavaInitializationString());
        assertNull(support.getTags());
        assertFalse(support.supportsCustomEditor());
        assertFalse(support.isPaintable());
        assertSame(source, support.getSource());
    }

    /*
     * source null
     */
    public void testPropertyEditorSupportObject_null() {
        try {
            new PropertyEditorSupport(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * public void addPropertyChangeListener(PropertyChangeListener listener)
     */
    public void testAddPropertyChangeListener() {
        MockTarget target = new MockTarget();
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.addPropertyChangeListener(EventHandler.create(
                PropertyChangeListener.class, target, "setCalled"));
        support.firePropertyChange();

        assertEquals("called", target.getLabel());
    }

    public void testAddPropertyChangeListener_source() {
        MockTarget target = new MockTarget();
        MockSource source = new MockSource();
        PropertyEditorSupport support = new PropertyEditorSupport(source);
        support.addPropertyChangeListener(EventHandler.create(
                PropertyChangeListener.class, target, "eventSource", "source"));
        support.firePropertyChange();
        assertSame(source, target.getEventSource());
    }

    public void testAddPropertyChangeListener_source_null() {
        MockTarget target = new MockTarget();
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.addPropertyChangeListener(EventHandler.create(
                PropertyChangeListener.class, target, "eventSource", ""));
        support.firePropertyChange();
        PropertyChangeEvent event = (PropertyChangeEvent) target
                .getEventSource();

        assertNull(event.getPropertyName());
        assertSame(support, event.getSource());
    }

    public void testFirePropertyChange_noListener() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.firePropertyChange();
    }

    /*
     * listener is null
     */
    public void testAddPropertyChangeListener_null() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.addPropertyChangeListener(null);
        try {
            support.firePropertyChange();
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testPaintValue() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.paintValue(null, null);
    }

    public void testRemovePropertyChangeListener() {
        MockTarget target = new MockTarget();
        PropertyEditorSupport support = new PropertyEditorSupport();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "eventSource", "source");
        support.addPropertyChangeListener(proxy);
        support.firePropertyChange();
        assertSame(support, target.getEventSource());

        target.setEventSource(null);
        support.removePropertyChangeListener(proxy);
        support.firePropertyChange();
        assertNull(target.getEventSource());
    }

    public void testRemovePropertyChangeListener_null() {
        MockTarget target = new MockTarget();
        PropertyEditorSupport support = new PropertyEditorSupport();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "eventSource", "source");
        support.addPropertyChangeListener(proxy);
        support.firePropertyChange();
        assertSame(support, target.getEventSource());

        target.setEventSource(null);
        support.removePropertyChangeListener(null);
        support.firePropertyChange();
        assertSame(support, target.getEventSource());
    }

    /*
     * remove a different listener
     */
    public void testRemovePropertyChangeListener_diff() {
        MockTarget target = new MockTarget();
        PropertyEditorSupport support = new PropertyEditorSupport();
        PropertyChangeListener proxy = EventHandler.create(
                PropertyChangeListener.class, target, "eventSource", "source");
        support.addPropertyChangeListener(proxy);
        support.firePropertyChange();
        assertSame(support, target.getEventSource());

        target.setEventSource(null);
        PropertyChangeListener proxy2 = EventHandler.create(
                PropertyChangeListener.class, target, "eventSource", "source");
        support.removePropertyChangeListener(proxy2);
        support.firePropertyChange();
        assertSame(support, target.getEventSource());
    }

    /*
     * remove null listener
     */
    public void testRemovePropertyChangeListener_null_null() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.addPropertyChangeListener(null);
        try {
            support.firePropertyChange();
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }

        support.removePropertyChangeListener(null);
        support.firePropertyChange();
    }

    public void testSetAsText() {
        // Regression for HARMONY-1113
        PropertyEditorSupport support;

        support = new PropertyEditorSupport();
        try {
            support.setAsText("100");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        support = new PropertyEditorSupport();
        support.setValue(new Object());
        try {
            support.setAsText("string");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        support = new PropertyEditorSupport();
        support.setValue(new String());
        support.setAsText("string");
    }

    public void testSetValue() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        String[] value = new String[] { "This is a sample value." };
        support.setValue(value);

        assertEquals(value, support.getValue());
        assertEquals(value.toString(), support.getAsText());

        assertNull(support.getCustomEditor());
        assertEquals("???", support.getJavaInitializationString());
        assertNull(support.getTags());
        assertFalse(support.supportsCustomEditor());
        assertFalse(support.isPaintable());

    }

    public void testSetValue_null() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        support.setValue(null);

        assertNull(support.getValue());
        assertEquals("null", support.getAsText());

        assertNull(support.getCustomEditor());
        assertEquals("???", support.getJavaInitializationString());
        assertNull(support.getTags());
        assertFalse(support.supportsCustomEditor());
        assertFalse(support.isPaintable());

        MockTarget target = new MockTarget();
        support.setValue(target);

        assertSame(target, support.getValue());
        assertEquals(target.toString(), support.getAsText());
        assertNull(support.getCustomEditor());
        assertEquals("???", support.getJavaInitializationString());
        assertNull(support.getTags());
        assertFalse(support.supportsCustomEditor());
        assertFalse(support.isPaintable());
    }

    /**
     * @tests java.beans.PropertyEditorSupport#setSource(Object)
     * @tests java.beans.PropertyEditorSupport#getSource()
     */
    public void test_setSourceLjava_lang_Object() {

        // Regression for HARMONY-1409
        PropertyEditorSupport support = new PropertyEditorSupport();

        Object source = new Object();

        support.setSource(source);
        assertSame(source, support.getSource());

        support.setSource(null);
    }
    
    public void testSetValue_Event() {
        PropertyEditorSupport support = new PropertyEditorSupport();
        Object source = new Object();
        support.setSource(source);
        MockPropertyChangeListener listener = new MockPropertyChangeListener();
        support.addPropertyChangeListener(listener);
        
        support.setValue(new Object());        
        PropertyChangeEvent event = listener.getEvent();
        assertNotNull(event);
        assertEquals(source, event.getSource());
        assertNull(event.getNewValue());
        assertNull(event.getOldValue());
        assertNull(event.getPropagationId());
        assertNull(event.getPropertyName());        
    }

    public static class MockSource {

        String id;

        String text;

        public MockSource() {
            this.id = "0001";
            this.text = getClass().getName();
        }

        /**
         * @return Returns the id.
         */
        public String getId() {
            return id;
        }

        /**
         * @param id
         *            The id to set.
         */
        public void setId(String id) {
            this.id = id;
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
    }

    public static class MockTarget {

        String id;

        String label;

        Object eventSource;

        public MockTarget() {
            this.id = "0001";
            this.label = getClass().getName();
        }

        /**
         * @return Returns the id.
         */
        public String getId() {
            return id;
        }

        /**
         * @param id
         *            The id to set.
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return Returns the text.
         */
        public String getLabel() {
            return label;
        }

        /**
         * @param text
         *            The text to set.
         */
        public void setLabel(String label) {
            this.label = label;
        }

        public void setCalled() {
            this.label = "called";
        }

        /**
         * @return Returns the eventSource.
         */
        public Object getEventSource() {
            return eventSource;
        }

        /**
         * @param eventSource
         *            The eventSource to set.
         */
        public void setEventSource(Object eventSource) {
            this.eventSource = eventSource;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MockTarget)) {
                return false;
            }

            MockTarget other = (MockTarget) o;
            return ((this.id == null ? other.id == null : this.id
                    .equals(other.id))
                    && (this.eventSource == null ? other.eventSource == null
                            : this.eventSource.equals(other.eventSource)) && (this.label == null ? other.label == null
                    : this.label.equals(other.label)));
        }
    }
    
    public class MockPropertyChangeListener implements PropertyChangeListener
    {

        private PropertyChangeEvent event;        
        public void propertyChange(PropertyChangeEvent event) {
            
            this.event = event;
        }
        
        public PropertyChangeEvent getEvent()
        {
            return event;
        }
        
    }
}
