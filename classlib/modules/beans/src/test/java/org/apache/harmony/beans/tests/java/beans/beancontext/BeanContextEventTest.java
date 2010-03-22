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
import java.beans.beancontext.BeanContextEvent;
import java.beans.beancontext.BeanContextServicesSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.beancontext.Utils;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContext;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServiceProviderS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServicesListenerS;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

/**
 * Test BeanContextEvent
 */
public class BeanContextEventTest extends TestCase {

    private static class MockBeanContextEvent extends BeanContextEvent {

        private static final long serialVersionUID = -5990761357871915928L;

        /**
         * @param bc
         */
        protected MockBeanContextEvent(BeanContext bc) {
            super(bc);

            assertSame(bc, getSource());
            assertSame(bc, getBeanContext());
            assertNull(this.propagatedFrom);
        }
        
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BeanContextEventTest.class);
    }

    public void testBeanContextEvent_NullParam() {
        try {
            new MockBeanContextEvent(null);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testBeanContextEvent() {
        BeanContext ctx = new MockBeanContext();
        BeanContextEvent event = new MockBeanContextEvent(ctx);
        assertSame(ctx, event.getSource());
        assertSame(ctx, event.getBeanContext());
        assertNull(event.getPropagatedFrom());
        assertFalse(event.isPropagated());
    }

    public void testGetBeanContext() {
        BeanContext ctx = new MockBeanContext();
        BeanContextEvent event = new MockBeanContextEvent(ctx);
        assertSame(ctx, event.getBeanContext());
    }

    public void testGetPropagatedFrom() {
        BeanContext ctx = new MockBeanContext();
        BeanContextEvent event = new MockBeanContextEvent(ctx);
        assertNull(event.getPropagatedFrom());

        BeanContext ctx2 = new MockBeanContext();
        event.setPropagatedFrom(ctx2);
        assertSame(ctx2, event.getPropagatedFrom());

        event.setPropagatedFrom(ctx);
        assertSame(ctx, event.getPropagatedFrom());

        event.setPropagatedFrom(null);
        assertNull(event.getPropagatedFrom());
    }

    public void testIsPropagated() {
        BeanContext ctx = new MockBeanContext();
        BeanContextEvent event = new MockBeanContextEvent(ctx);
        assertFalse(event.isPropagated());

        BeanContext ctx2 = new MockBeanContext();
        event.setPropagatedFrom(ctx2);
        assertTrue(event.isPropagated());

        event.setPropagatedFrom(ctx);
        assertTrue(event.isPropagated());

        event.setPropagatedFrom(null);
        assertFalse(event.isPropagated());
    }

    public void testSetPropagatedFrom() {
        BeanContext ctx = new MockBeanContext();
        BeanContextEvent event = new MockBeanContextEvent(ctx);
        assertNull(event.getPropagatedFrom());

        BeanContext ctx2 = new MockBeanContext();
        event.setPropagatedFrom(ctx2);
        assertSame(ctx2, event.getPropagatedFrom());

        event.setPropagatedFrom(ctx);
        assertSame(ctx, event.getPropagatedFrom());

        event.setPropagatedFrom(null);
        assertNull(event.getPropagatedFrom());
    }
    
    public void testSerialization() throws Exception {
        final BeanContextServicesSupport ctx = new BeanContextServicesSupport(
                null, Locale.FRANCE, false, false);
        final BeanContextServicesSupport ctx2 = new BeanContextServicesSupport(
                ctx, Locale.ITALY, true, true);
        BeanContextEvent event = new MockBeanContextEvent(ctx2);
        event.setPropagatedFrom(ctx);
        SerializationTest.verifySelf(event, new SerializableAssert(){
            public void assertDeserialized(Serializable arg0, Serializable arg1) {
                BeanContextEvent e1 = (BeanContextEvent)arg0;
                BeanContextEvent e2 = (BeanContextEvent)arg1;
                assertNull((BeanContextServicesSupport)e2.getSource());
                assertNotNull((BeanContextServicesSupport)e1.getSource());
                assertEqualsSerially((BeanContextServicesSupport)e1.getPropagatedFrom(), (BeanContextServicesSupport)e2.getPropagatedFrom());
                assertEqualsSerially(ctx, (BeanContextServicesSupport)e2.getPropagatedFrom());
            }
        });
    }
    
    public static void assertEqualsSerially(BeanContextServicesSupport orig,
            BeanContextServicesSupport ser) {

        // check bcsListeners
        ArrayList origBcsListeners = (ArrayList) Utils.getField(orig,
                "bcsListeners");
        ArrayList serBcsListeners = (ArrayList) Utils.getField(ser,
                "bcsListeners");
        int i = 0, j = 0;
        while (i < origBcsListeners.size()) {
            Object l1 = origBcsListeners.get(i);
            if (l1 instanceof Serializable) {
                Object l2 = serBcsListeners.get(j);
                assertSame(l1.getClass(), l2.getClass());
                if (l1 instanceof MockBeanContextServicesListenerS) {
                    assertEquals(((MockBeanContextServicesListenerS) l1).id,
                            ((MockBeanContextServicesListenerS) l2).id);
                }
                j++;
            }
            i++;
        }
        assertEquals(j, serBcsListeners.size());

        // check services
        HashMap origServices = (HashMap) Utils.getField(orig, "services");
        HashMap serServices = (HashMap) Utils.getField(ser, "services");
        int count = 0;
        for (Iterator iter = origServices.keySet().iterator(); iter.hasNext();) {
            Object serviceClass = iter.next();
            Object bcssProvider = origServices.get(serviceClass);
            Object provider = Utils.getField(bcssProvider, "serviceProvider");
            if (provider instanceof Serializable) {
                assertTrue(serServices.containsKey(serviceClass));
                if (provider instanceof MockBeanContextServiceProviderS) {
                    Object serProvider = Utils.getField(serServices
                            .get(serviceClass), "serviceProvider");
                    assertEquals(
                            ((MockBeanContextServiceProviderS) provider).id,
                            ((MockBeanContextServiceProviderS) serProvider).id);
                }
                count++;
            }
        }
        assertEquals(count, serServices.size());
    }    

    public void testSerializationComptibility() throws Exception {
        final BeanContextServicesSupport ctx = new BeanContextServicesSupport(
                null, Locale.ITALY, true, true);
        final BeanContextServicesSupport ctx2 = new BeanContextServicesSupport(
                null, Locale.CHINA, true, true);
        
        BeanContextEvent event = new MockBeanContextEvent(ctx);
        event.setPropagatedFrom(ctx2);
        SerializationTest.verifyGolden(this, event, new SerializableAssert(){
            public void assertDeserialized(Serializable arg0, Serializable arg1) {
                BeanContextEvent e1 = (BeanContextEvent)arg0;
                BeanContextEvent e2 = (BeanContextEvent)arg1;
                assertNull((BeanContextServicesSupport)e2.getSource());
                assertNotNull((BeanContextServicesSupport)e1.getSource());
                assertEqualsSerially((BeanContextServicesSupport)e1.getPropagatedFrom(), (BeanContextServicesSupport)e2.getPropagatedFrom());
                assertEqualsSerially(ctx2, (BeanContextServicesSupport)e2.getPropagatedFrom());
            }
        });
    }
}
