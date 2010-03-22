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
package org.apache.harmony.jndi.tests.javax.naming.event;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;

import org.apache.harmony.jndi.tests.javax.naming.util.Log;

import junit.framework.TestCase;

public class NamingEventTest extends TestCase {

    static Log log = new Log(NamingEventTest.class);

    static Binding binding1 = new Binding("name_sample", "value_sample");

    static Binding binding2 = new Binding("name_sample2", "value_sample2");

    static EventContext eventctx = new EventContextMockUp();

    public void testConstructorAndGetters() {
        log.setMethod("testConstructorAndGetters()");
        NamingEvent event = null;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_CHANGED, binding1, binding2,
                "anything");

        assertEquals(eventctx, event.getEventContext());
        assertEquals(NamingEvent.OBJECT_CHANGED, event.getType());
        assertEquals(binding1, event.getNewBinding());
        assertEquals(binding2, event.getOldBinding());
        assertEquals("anything", event.getChangeInfo());

        assertEquals(eventctx, event.getSource());
    }

    public void testConstructorAndGetters_Null_EventContext() {
        log.setMethod("testConstructorAndGetters_Null_EventContext()");

        try {
            new NamingEvent(null, NamingEvent.OBJECT_CHANGED, binding1, binding2, "anything");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructorAndGetters_Null_Type() {
        log.setMethod("testConstructorAndGetters_Null_Type()");
        NamingEvent event = new NamingEvent(eventctx, -1, binding1, binding2, "anything");
        assertEquals(-1, event.getType());
    }

    public void testConstructor_ValidateArgs_ADDED() {
        log.setMethod("testConstructor_ValidateArgs_ADDED()");
        NamingEvent event = new NamingEvent(eventctx, NamingEvent.OBJECT_ADDED, binding1, null,
                null);

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_ADDED, null, binding2, "anything");
        assertNull(event.getNewBinding());
    }

    public void testConstructor_ValidateArgs_CHANGED() {
        log.setMethod("testConstructor_ValidateArgs_CHANGED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_CHANGED, binding1, binding2, null);

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_CHANGED, null, binding2,
                "anything");
        assertNull(event.getNewBinding());

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_CHANGED, binding1, null,
                "anything");
        assertNull(event.getOldBinding());
    }

    public void testConstructor_ValidateArgs_REMOVED() {
        log.setMethod("testConstructor_ValidateArgs_REMOVED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_REMOVED, null, binding2, null);

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_REMOVED, binding1, null,
                "anything");
        assertNull(event.getOldBinding());
    }

    public void testConstructor_ValidateArgs_RENAMED() {
        log.setMethod("testConstructor_ValidateArgs_RENAMED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_RENAMED, binding1, binding2, null);

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_RENAMED, null, binding2,
                "anything");
        assertNull(event.getNewBinding());

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_RENAMED, binding1, null,
                "anything");
        assertNull(event.getOldBinding());
    }

    public void testDispatch_ADDED() {
        log.setMethod("testDispatch_ADDED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_ADDED, binding1, binding2,
                "anything");

        event.dispatch(new TestAllListener(event));
    }

    public void testDispatch_REMOVED() {
        log.setMethod("testDispatch_REMOVED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_REMOVED, binding1, binding2,
                "anything");

        event.dispatch(new TestAllListener(event));
    }

    public void testDispatch_RENAMED() {
        log.setMethod("testDispatch_RENAMED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_RENAMED, binding1, binding2,
                "anything");

        event.dispatch(new TestAllListener(event));
    }

    public void testDispatch_CHANGED() {
        log.setMethod("testDispatch_CHANGED()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_CHANGED, binding1, binding2,
                "anything");

        event.dispatch(new TestAllListener(event));
    }

    public void testDispatch_ADDED_BadListenerType() {
        log.setMethod("testDispatch_ADDED_BadListenerType()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_ADDED, binding1, binding2,
                "anything");

        try {
            event.dispatch(new TestEmptyListener(event));
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
        }
    }

    public void testDispatch_REMOVED_BadListenerType() {
        log.setMethod("testDispatch_REMOVED_BadListenerType()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_REMOVED, binding1, binding2,
                "anything");

        try {
            event.dispatch(new TestEmptyListener(event));
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
        }
    }

    public void testDispatch_RENAMED_BadListenerType() {
        log.setMethod("testDispatch_RENAMED_BadListenerType()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_RENAMED, binding1, binding2,
                "anything");

        try {
            event.dispatch(new TestEmptyListener(event));
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
        }
    }

    public void testDispatch_CHANGED_BadListenerType() {
        log.setMethod("testDispatch_CHANGED_BadListenerType()");
        NamingEvent event;

        event = new NamingEvent(eventctx, NamingEvent.OBJECT_CHANGED, binding1, binding2,
                "anything");

        try {
            event.dispatch(new TestEmptyListener(event));
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
        }
    }

    class TestEmptyListener implements NamingListener {

        protected NamingEvent expectedEvent;

        public TestEmptyListener(NamingEvent expectedEvent) {
            this.expectedEvent = expectedEvent;
        }

        public void namingExceptionThrown(NamingExceptionEvent namingexceptionevent) {
            log.log("namingExceptionThrown called, " + namingexceptionevent);
        }

    }

    class TestAllListener extends TestEmptyListener implements NamespaceChangeListener,
            ObjectChangeListener {

        public TestAllListener(NamingEvent expectedEvent) {
            super(expectedEvent);
        }

        public void objectAdded(NamingEvent namingevent) {
            assertTrue(expectedEvent == namingevent);
        }

        public void objectRemoved(NamingEvent namingevent) {
            assertTrue(expectedEvent == namingevent);
        }

        public void objectRenamed(NamingEvent namingevent) {
            assertTrue(expectedEvent == namingevent);
        }

        public void objectChanged(NamingEvent namingevent) {
            assertTrue(expectedEvent == namingevent);
        }
    }

    static class EventContextMockUp implements EventContext {

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.event.EventContext#addNamingListener(javax.naming.Name,
         *      int, javax.naming.event.NamingListener)
         */
        public void addNamingListener(Name name, int i, NamingListener naminglistener)
                throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.event.EventContext#addNamingListener(java.lang.String,
         *      int, javax.naming.event.NamingListener)
         */
        public void addNamingListener(String s, int i, NamingListener naminglistener)
                throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.event.EventContext#removeNamingListener(javax.naming.event.NamingListener)
         */
        public void removeNamingListener(NamingListener naminglistener) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.event.EventContext#targetMustExist()
         */
        public boolean targetMustExist() throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#addToEnvironment(java.lang.String,
         *      java.lang.Object)
         */
        public Object addToEnvironment(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
         */
        public void bind(Name n, Object o) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
         */
        public void bind(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#close()
         */
        public void close() throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#composeName(javax.naming.Name,
         *      javax.naming.Name)
         */
        public Name composeName(Name n, Name pfx) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#composeName(java.lang.String,
         *      java.lang.String)
         */
        public String composeName(String s, String pfx) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#createSubcontext(javax.naming.Name)
         */
        public Context createSubcontext(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#createSubcontext(java.lang.String)
         */
        public Context createSubcontext(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
         */
        public void destroySubcontext(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#destroySubcontext(java.lang.String)
         */
        public void destroySubcontext(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        public Hashtable<?, ?> getEnvironment() throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#getNameInNamespace()
         */
        public String getNameInNamespace() throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#getNameParser(javax.naming.Name)
         */
        public NameParser getNameParser(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#getNameParser(java.lang.String)
         */
        public NameParser getNameParser(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#list(javax.naming.Name)
         */
        public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#list(java.lang.String)
         */
        public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#listBindings(javax.naming.Name)
         */
        public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#listBindings(java.lang.String)
         */
        public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#lookup(javax.naming.Name)
         */
        public Object lookup(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#lookup(java.lang.String)
         */
        public Object lookup(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#lookupLink(javax.naming.Name)
         */
        public Object lookupLink(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#lookupLink(java.lang.String)
         */
        public Object lookupLink(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
         */
        public void rebind(Name n, Object o) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
         */
        public void rebind(String s, Object o) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
         */
        public Object removeFromEnvironment(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#rename(javax.naming.Name,
         *      javax.naming.Name)
         */
        public void rename(Name nOld, Name nNew) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
         */
        public void rename(String sOld, String sNew) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#unbind(javax.naming.Name)
         */
        public void unbind(Name n) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.naming.Context#unbind(java.lang.String)
         */
        public void unbind(String s) throws NamingException {
            throw new UnsupportedOperationException("in EventContextMockUp");
        }

    }

}
