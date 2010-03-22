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

package org.apache.harmony.beans.tests.java.beans.beancontext;

import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextServiceAvailableEvent;
import java.beans.beancontext.BeanContextServiceRevokedEvent;
import java.beans.beancontext.BeanContextServices;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextDelegateS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServices;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.util.SerializationTester;

/**
 * Test BeanContextServiceAvailableEvent
 */
@SuppressWarnings("unchecked")
public class BeanContextServiceAvailableEventTest extends TestCase {

    private static class MockBeanContextServiceAvailableEvent extends
            BeanContextServiceAvailableEvent {

        private static final long serialVersionUID = 796722290390289532L;

        /**
         * @param bcs
         * @param sc
         */
        public MockBeanContextServiceAvailableEvent(BeanContextServices bcs,
                Class sc) {
            super(bcs, sc);
            assertSame(sc, this.serviceClass);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BeanContextServiceAvailableEventTest.class);
    }

    public void testBeanContextServiceAvailableEvent_NullParam() {
        BeanContextServices services = new MockBeanContextServices();

        try {
            new MockBeanContextServiceAvailableEvent(null, BeanContext.class);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        BeanContextServiceAvailableEvent event = new MockBeanContextServiceAvailableEvent(
                services, null);
        assertNull(event.getServiceClass());
        assertSame(services, event.getSource());
        assertSame(services, event.getSourceAsBeanContextServices());
    }

    public void testBeanContextServiceAvailableEvent() {
        BeanContextServices services = new MockBeanContextServices();
        BeanContextServiceAvailableEvent event = new MockBeanContextServiceAvailableEvent(
                services, BeanContext.class);
        assertSame(BeanContext.class, event.getServiceClass());
        assertSame(services, event.getSource());
        assertSame(services, event.getSourceAsBeanContextServices());
    }

    public void testGetSourceAsBeanContextServices() {
        BeanContextServices services = new MockBeanContextServices();
        BeanContextServiceAvailableEvent event = new MockBeanContextServiceAvailableEvent(
                services, BeanContext.class);
        assertSame(services, event.getSource());
        assertSame(services, event.getSourceAsBeanContextServices());
    }

    public void testGetServiceClass() {
        BeanContextServices services = new MockBeanContextServices();
        BeanContextServiceAvailableEvent event = new MockBeanContextServiceAvailableEvent(
                services, BeanContext.class);
        assertSame(BeanContext.class, event.getServiceClass());
    }

    public void testGetCurrentServiceSelectors() {
        BeanContextServices services = new MockBeanContextServices();
        BeanContextServiceAvailableEvent event = new MockBeanContextServiceAvailableEvent(
                services, BeanContext.class);

        Iterator expectedIt = services
                .getCurrentServiceSelectors(BeanContext.class);
        Iterator it = event.getCurrentServiceSelectors();
        while (expectedIt.hasNext()) {
            assertSame(expectedIt.next(), it.next());
        }
        assertFalse(expectedIt.hasNext());
        assertFalse(it.hasNext());
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        BeanContextServiceAvailableEvent event = new BeanContextServiceAvailableEvent(
                new MockBeanContextServices(), ArrayList.class);
        event.setPropagatedFrom(new MockBeanContextDelegateS("from ID"));

        assertEqualsSerially(event,
                (BeanContextServiceAvailableEvent) SerializationTester
                        .getDeserilizedObject(event));
    }


     public void testSerialization_Compatibility() throws Exception {
         BeanContextServiceAvailableEvent event = new BeanContextServiceAvailableEvent(
                 new MockBeanContextServices(), ArrayList.class);
         event.setPropagatedFrom(new MockBeanContextDelegateS("from ID"));
         SerializationTest.verifyGolden(this, event, new SerializableAssert(){
             public void assertDeserialized(Serializable orig, Serializable ser) {
                 assertEqualsSerially((BeanContextServiceAvailableEvent) orig,
                         (BeanContextServiceAvailableEvent) ser);
             }
         });
     }

    private void assertEqualsSerially(BeanContextServiceAvailableEvent orig,
            BeanContextServiceAvailableEvent ser) {
        assertNull(ser.getSource());

        // check propagatedFrom
        if (orig.getPropagatedFrom() instanceof Serializable) {
            BeanContext origFrom = orig.getPropagatedFrom();
            BeanContext serFrom = ser.getPropagatedFrom();
            assertEquals(origFrom.getClass(), serFrom.getClass());
            if (origFrom instanceof MockBeanContextDelegateS) {
                assertEquals(((MockBeanContextDelegateS) origFrom).id,
                        ((MockBeanContextDelegateS) serFrom).id);
            }
        }

        // check serviceClass
        assertEquals(orig.getServiceClass(), ser.getServiceClass());
    }
}
