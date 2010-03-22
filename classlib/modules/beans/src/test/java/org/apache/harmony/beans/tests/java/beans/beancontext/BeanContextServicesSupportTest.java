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

import java.beans.PropertyVetoException;
import java.beans.beancontext.BeanContextServiceAvailableEvent;
import java.beans.beancontext.BeanContextServiceProvider;
import java.beans.beancontext.BeanContextServiceRevokedEvent;
import java.beans.beancontext.BeanContextServices;
import java.beans.beancontext.BeanContextServicesListener;
import java.beans.beancontext.BeanContextServicesSupport;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.beancontext.MethodInvocationRecords;
import org.apache.harmony.beans.tests.support.beancontext.Utils;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextChild;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServiceProvider;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServiceProviderS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServiceRevokedListener;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServices;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServicesListener;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextServicesListenerS;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.util.SerializationTester;

/**
 * Test BeanContextServicesSupport
 */
@SuppressWarnings("unchecked")
public class BeanContextServicesSupportTest extends TestCase {

    public static class MockBeanContextServicesSupport extends
            BeanContextServicesSupport {

        private static final long serialVersionUID = -5521269152428572350L;

        public MethodInvocationRecords records;

        public MockBeanContextServicesSupport() {
            super();
        }

        public MockBeanContextServicesSupport(BeanContextServices peer) {
            super(peer);
        }

        public MockBeanContextServicesSupport(BeanContextServices peer,
                Locale lcle) {
            super(peer, lcle);
        }

        public MockBeanContextServicesSupport(BeanContextServices peer,
                Locale lcle, boolean dtime) {
            super(peer, lcle, dtime);
        }

        public MockBeanContextServicesSupport(BeanContextServices peer,
                Locale lcle, boolean dTime, boolean visible) {
            super(peer, lcle, dTime, visible);
        }

        public ArrayList bcsListeners() {
            return bcsListeners;
        }

        public BeanContextServicesSupport.BCSSProxyServiceProvider proxy() {
            return proxy;
        }

        public int serializable() {
            return serializable;
        }

        public HashMap services() {
            return services;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#initialize()
         */
        @Override
        public void initialize() {
            super.initialize();
            if (records == null) {
                records = new MethodInvocationRecords();
            }
            records.add("initialize", null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextServicesSupport#addService(java.lang.Class,
         *      java.beans.beancontext.BeanContextServiceProvider, boolean)
         */
        @Override
        public boolean addService(Class serviceClass,
                BeanContextServiceProvider bcsp, boolean fireEvent) {
            return super.addService(serviceClass, bcsp, fireEvent);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#childJustRemovedHook(java.lang.Object,
         *      java.beans.beancontext.BeanContextSupport.BCSChild)
         */
        @Override
        protected void childJustRemovedHook(Object child, BCSChild bcsc) {
            super.childJustRemovedHook(child, bcsc);
            records.add("childJustRemovedHook", child, bcsc, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextServicesSupport#createBCSSServiceProvider(java.lang.Class,
         *      java.beans.beancontext.BeanContextServiceProvider)
         */
        @Override
        protected BCSSServiceProvider createBCSSServiceProvider(Class sc,
                BeanContextServiceProvider bcsp) {
            BCSSServiceProvider result = super.createBCSSServiceProvider(sc,
                    bcsp);
            records.add("createBCSSServiceProvider", sc, bcsp, result);
            return result;
        }

        public static BeanContextServicesListener publicGetChildBeanContextServicesListener(
                Object child) {
            return getChildBeanContextServicesListener(child);
        }

        public void publicFireServiceAdded(BeanContextServiceAvailableEvent evt) {
            fireServiceAdded(evt);
        }

        public void publicFireServiceAdded(Class cls) {
            fireServiceAdded(cls);
        }

        public void publicFireServiceRevoked(BeanContextServiceRevokedEvent evt) {
            fireServiceRevoked(evt);
        }

        public void publicFireServiceRevoked(Class cls, boolean revokeNow) {
            fireServiceRevoked(cls, revokeNow);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextServicesListener#serviceAvailable(java.beans.beancontext.BeanContextServiceAvailableEvent)
         */
        @Override
        public void serviceAvailable(BeanContextServiceAvailableEvent bcssae) {
            super.serviceAvailable(bcssae);
            records.add("serviceAvailable", bcssae, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextServiceRevokedListener#serviceRevoked(java.beans.beancontext.BeanContextServiceRevokedEvent)
         */
        @Override
        public void serviceRevoked(BeanContextServiceRevokedEvent bcssre) {
            super.serviceRevoked(bcssre);
            records.add("serviceRevoked", bcssre, null);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BeanContextServicesSupportTest.class);
    }

    public void testAddBeanContextServicesListener_NullParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        try {
            support.addBeanContextServicesListener(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testAddBeanContextServicesListener() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();

        support.addBeanContextServicesListener(l1);
        assertEquals(1, support.bcsListeners().size());
        assertTrue(support.bcsListeners().contains(l1));
    }

    /*
     * Class under test for boolean addService(java.lang.Class,
     * java.beans.beancontext.BeanContextServiceProvider)
     */
    public void testAddServiceClassBeanContextServiceProvider_NullParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();

        try {
            support.addService(null, provider);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.addService(Collection.class, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for boolean addService(java.lang.Class,
     * java.beans.beancontext.BeanContextServiceProvider)
     */
    public void testAddServiceClassBeanContextServiceProvider_ParentContext() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        boolean result = support.addService(Collection.class, provider);
        assertTrue(result);
        assertEquals(1, support.services().size());
        assertEquals(0, childSupport.services().size());
        Object bcssProvider = support.services().get(Collection.class);
        support.records.assertRecord("createBCSSServiceProvider",
                Collection.class, provider, bcssProvider);
        support.records.assertEndOfRecords();
        BeanContextServiceAvailableEvent evt = (BeanContextServiceAvailableEvent) childSupport.records
                .getArg(0);
        childSupport.records.assertRecord("serviceAvailable", evt, null);
        assertSame(support, evt.getSourceAsBeanContextServices());
        assertSame(Collection.class, evt.getServiceClass());
        childSupport.records.assertEndOfRecords();
        assertSame(support, l1.lastAvailableEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l1.lastAvailableEvent.getServiceClass());
        assertSame(support, l2.lastAvailableEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l2.lastAvailableEvent.getServiceClass());
    }

    /*
     * Class under test for boolean addService(java.lang.Class,
     * java.beans.beancontext.BeanContextServiceProvider)
     */
    public void testAddServiceClassBeanContextServiceProvider_ChildContext() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        boolean result = childSupport.addService(Collection.class, provider);
        assertTrue(result);
        assertEquals(0, support.services().size());
        assertEquals(1, childSupport.services().size());
        Object bcssProvider = childSupport.services().get(Collection.class);
        childSupport.records.assertRecord("createBCSSServiceProvider",
                Collection.class, provider, bcssProvider);
        childSupport.records.assertEndOfRecords();
        support.records.assertEndOfRecords();
        assertNull(l1.lastAvailableEvent);
        assertSame(childSupport, l2.lastAvailableEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l2.lastAvailableEvent.getServiceClass());
    }

    /*
     * Class under test for boolean addService(java.lang.Class,
     * java.beans.beancontext.BeanContextServiceProvider)
     */
    public void testAddServiceClassBeanContextServiceProvider_Exist() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        boolean result = support.addService(Collection.class, provider);
        assertTrue(result);
        assertEquals(1, support.services().size());
        assertEquals(0, childSupport.services().size());
        Object bcssProvider = support.services().get(Collection.class);
        support.records.assertRecord("createBCSSServiceProvider",
                Collection.class, provider, bcssProvider);
        support.records.assertEndOfRecords();
        BeanContextServiceAvailableEvent evt = (BeanContextServiceAvailableEvent) childSupport.records
                .getArg(0);
        childSupport.records.assertRecord("serviceAvailable", evt, null);
        assertSame(support, evt.getSourceAsBeanContextServices());
        assertSame(Collection.class, evt.getServiceClass());
        childSupport.records.assertEndOfRecords();
        assertSame(support, l1.lastAvailableEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l1.lastAvailableEvent.getServiceClass());
        assertSame(support, l2.lastAvailableEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l2.lastAvailableEvent.getServiceClass());
        l1.clearLastEvent();
        l2.clearLastEvent();

        // add exist
        MockBeanContextServiceProvider another = new MockBeanContextServiceProvider();
        result = support.addService(Collection.class, another);
        assertFalse(result);
        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        assertNull(l1.lastAvailableEvent);
        assertNull(l2.lastAvailableEvent);
    }

    /*
     * Class under test for boolean addService(java.lang.Class,
     * java.beans.beancontext.BeanContextServiceProvider, boolean)
     */
    public void testAddServiceClassBeanContextServiceProviderboolean_FalseParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        boolean result = support.addService(Collection.class, provider, false);
        assertTrue(result);
        assertEquals(1, support.services().size());
        assertEquals(0, childSupport.services().size());
        Object bcssProvider = support.services().get(Collection.class);
        support.records.assertRecord("createBCSSServiceProvider",
                Collection.class, provider, bcssProvider);
        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        assertNull(l1.lastAvailableEvent);
        assertNull(l2.lastAvailableEvent);
    }

    public void testBcsPreDeserializationHook() {
        // covered by serialization test
    }

    public void testBcsPreSerializationHook() {
        // covered by serialization test
    }

    /*
     * Class under test for void BeanContextServicesSupport()
     */
    public void testBeanContextServicesSupport() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        assertEquals(0, support.bcsListeners().size());
        assertNull(support.proxy());
        assertEquals(0, support.serializable());
        assertTrue(support.services().isEmpty());
        assertSame(support, support.getBeanContextServicesPeer());
        assertSame(Locale.getDefault(), support.getLocale());
        assertFalse(support.isDesignTime());
    }

    /*
     * Class under test for void
     * BeanContextServicesSupport(java.beans.beancontext.BeanContextServices)
     */
    public void testBeanContextServicesSupportBeanContextServices() {
        MockBeanContextServices services = new MockBeanContextServices();
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport(
                services);
        assertEquals(0, support.bcsListeners().size());
        assertNull(support.proxy());
        assertEquals(0, support.serializable());
        assertTrue(support.services().isEmpty());
        assertSame(services, support.getBeanContextServicesPeer());
        assertSame(Locale.getDefault(), support.getLocale());
        assertFalse(support.isDesignTime());
    }

    /*
     * Class under test for void
     * BeanContextServicesSupport(java.beans.beancontext.BeanContextServices,
     * java.util.Locale)
     */
    public void testBeanContextServicesSupportBeanContextServicesLocale() {
        MockBeanContextServices services = new MockBeanContextServices();
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport(
                services, Locale.ITALY);
        assertEquals(0, support.bcsListeners().size());
        assertNull(support.proxy());
        assertEquals(0, support.serializable());
        assertTrue(support.services().isEmpty());
        assertSame(services, support.getBeanContextServicesPeer());
        assertSame(Locale.ITALY, support.getLocale());
        assertFalse(support.isDesignTime());
    }

    /*
     * Class under test for void
     * BeanContextServicesSupport(java.beans.beancontext.BeanContextServices,
     * java.util.Locale, boolean)
     */
    public void testBeanContextServicesSupportBeanContextServicesLocaleboolean() {
        MockBeanContextServices services = new MockBeanContextServices();
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport(
                services, Locale.ITALY, true);
        assertEquals(0, support.bcsListeners().size());
        assertNull(support.proxy());
        assertEquals(0, support.serializable());
        assertTrue(support.services().isEmpty());
        assertSame(services, support.getBeanContextServicesPeer());
        assertSame(Locale.ITALY, support.getLocale());
        assertTrue(support.isDesignTime());
    }

    /*
     * Class under test for void
     * BeanContextServicesSupport(java.beans.beancontext.BeanContextServices,
     * java.util.Locale, boolean, boolean)
     */
    public void testBeanContextServicesSupportBeanContextServicesLocalebooleanboolean() {
        MockBeanContextServices services = new MockBeanContextServices();
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport(
                services, Locale.ITALY, true, true);
        assertEquals(0, support.bcsListeners().size());
        assertNull(support.proxy());
        assertEquals(0, support.serializable());
        assertTrue(support.services().isEmpty());
        assertSame(services, support.getBeanContextServicesPeer());
        assertSame(Locale.ITALY, support.getLocale());
        assertTrue(support.isDesignTime());
    }

    public void testChildJustRemovedHook() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);

        MockBeanContextServiceRevokedListener rl = new MockBeanContextServiceRevokedListener();
        Object service = support.getService(child, child, Collection.class,
                null, rl);
        assertSame(Collections.EMPTY_SET, service);
        assertNull(rl.lastEvent);
        support.records.clear();
        provider.records.clear();

        support.remove(child);
        support.records.assertRecord("childJustRemovedHook", child,
                MethodInvocationRecords.IGNORE, null);
        support.records.assertEndOfRecords();
        provider.records.assertRecord("releaseService", support, child,
                service, null);
        provider.records.assertEndOfRecords();
        assertNull(rl.lastEvent);
    }

    public void testCreateBCSChild() {
        // covered in super's testcase
    }

    public void testCreateBCSSServiceProvider() {
        // covered by addService
    }

    /*
     * Class under test for void
     * fireServiceAdded(java.beans.beancontext.BeanContextServiceAvailableEvent)
     */
    public void testFireServiceAddedBeanContextServiceAvailableEvent() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        BeanContextServiceAvailableEvent evt = new BeanContextServiceAvailableEvent(
                support, Collection.class);
        support.publicFireServiceAdded(evt);

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        assertSame(evt, l1.lastAvailableEvent);
        assertNull(l2.lastAvailableEvent);
    }

    /*
     * Class under test for void fireServiceAdded(java.lang.Class)
     */
    public void testFireServiceAddedClass() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        support.publicFireServiceAdded(Collection.class);

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        assertSame(Collection.class, l1.lastAvailableEvent.getServiceClass());
        assertSame(support, l1.lastAvailableEvent
                .getSourceAsBeanContextServices());
        assertNull(l2.lastAvailableEvent);
    }

    /*
     * Class under test for void
     * fireServiceRevoked(java.beans.beancontext.BeanContextServiceRevokedEvent)
     */
    public void testFireServiceRevokedBeanContextServiceRevokedEvent() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        BeanContextServiceRevokedEvent evt = new BeanContextServiceRevokedEvent(
                support, Collection.class, false);
        support.publicFireServiceRevoked(evt);

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        assertSame(evt, l1.lastRevokedEvent);
        assertNull(l2.lastRevokedEvent);
    }

    /*
     * Class under test for void fireServiceRevoked(java.lang.Class, boolean)
     */
    public void testFireServiceRevokedClassboolean() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        support.publicFireServiceRevoked(Collection.class, false);

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        assertSame(Collection.class, l1.lastRevokedEvent.getServiceClass());
        assertSame(support, l1.lastRevokedEvent
                .getSourceAsBeanContextServices());
        assertFalse(l1.lastRevokedEvent.isCurrentServiceInvalidNow());
        assertNull(l2.lastRevokedEvent);
    }

    public void testGetBeanContextServicesPeer_Self() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        assertSame(support, support.getBeanContextServicesPeer());
    }

    public void testGetBeanContextServicesPeer_Another() {
        MockBeanContextServices services = new MockBeanContextServices();
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport(
                services);
        assertSame(services, support.getBeanContextServicesPeer());
    }

    public void testGetChildBeanContextServicesListener_NullParam() {
        BeanContextServicesListener result = MockBeanContextServicesSupport
                .publicGetChildBeanContextServicesListener(null);
        assertNull(result);
    }

    public void testGetChildBeanContextServicesListener_Is() {
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        BeanContextServicesListener result = MockBeanContextServicesSupport
                .publicGetChildBeanContextServicesListener(l);
        assertSame(l, result);
    }

    public void testGetChildBeanContextServicesListener_IsNot() {
        BeanContextServicesListener result = MockBeanContextServicesSupport
                .publicGetChildBeanContextServicesListener("is not");
        assertNull(result);
    }

    public void testGetCurrentServiceClasses() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        Iterator iter = support.getCurrentServiceClasses();
        assertFalse(iter.hasNext());

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        iter = support.getCurrentServiceClasses();
        assertTrue(iter.hasNext());
        assertSame(Collection.class, iter.next());
        iter.remove();
        assertFalse(iter.hasNext());
        assertEquals(1, support.services().size());

        iter = childSupport.getCurrentServiceClasses();
        assertFalse(iter.hasNext());
    }

    public void testGetCurrentServiceSelectors_NullParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        Iterator iter = support.getCurrentServiceSelectors(null);
        assertNull(iter);
    }

    public void testGetCurrentServiceSelectors_NonExist() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        Iterator iter = support.getCurrentServiceSelectors(Collection.class);
        assertNull(iter);
        // Regression for HARMONY-1397
        class TestServiceProvider implements BeanContextServiceProvider {
            public Object getService(BeanContextServices p0, Object p1,
                    Class p2, Object p3) {
                return null;
            }

            public void releaseService(BeanContextServices p0, Object p1,
                    Object p2) {
            }

            public Iterator getCurrentServiceSelectors(BeanContextServices p0,
                    Class p1) {
                return null;
            }
        }
        support.addService(BeanContextServicesSupportTest.class,
                new TestServiceProvider());
        assertNotNull(support
                .getCurrentServiceSelectors(BeanContextServicesSupportTest.class));
    }

    public void testGetCurrentServiceSelectors() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);

        Iterator iter = support.getCurrentServiceSelectors(Collection.class);
        assertTrue(iter.hasNext());
        assertSame(Integer.class, iter.next());
        iter.remove();
        assertFalse(iter.hasNext());
        provider.records.assertRecord("getCurrentServiceSelectors", support,
                Collection.class, MethodInvocationRecords.IGNORE);
        provider.records.assertEndOfRecords();
    }

    public void testGetService_NullParam() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServiceRevokedListener l = new MockBeanContextServiceRevokedListener();

        try {
            support.getService(null, requestor, Collection.class, selector, l);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.getService(child, null, Collection.class, selector, l);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.getService(child, requestor, null, selector, l);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        Object result = support.getService(child, requestor, Collection.class,
                null, l);
        assertSame(Collections.EMPTY_SET, result);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, null, result);
        provider.records.assertEndOfRecords();

        try {
            support.getService(child, requestor, Collection.class, selector,
                    null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetService_NonChild() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServiceRevokedListener l = new MockBeanContextServiceRevokedListener();

        try {
            support.getService(new MockBeanContextChild(), requestor,
                    Collection.class, selector, l);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        provider.records.assertEndOfRecords();
    }

    public void testGetService_NoService() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServiceRevokedListener l = new MockBeanContextServiceRevokedListener();

        Object result = support.getService(child, requestor, List.class,
                selector, l);
        assertNull(result);
    }

    public void testGetService_ThisContext() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        support.records.clear();

        Object result = support.getService(child, requestor, Collection.class,
                selector, l);
        assertSame(Collections.EMPTY_SET, result);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, result);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();

        support.remove(child);
        support.records.assertRecord("childJustRemovedHook", child,
                MethodInvocationRecords.IGNORE, null);
        support.records.assertEndOfRecords();
        provider.records.assertRecord("releaseService", support, requestor,
                result, null);
        provider.records.assertEndOfRecords();
        assertNull(l.lastRevokedEvent);
    }

    public void testGetService_ParentContext() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        childSupport.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        support.records.clear();
        childSupport.records.clear();

        Object result = childSupport.getService(child, requestor,
                Collection.class, selector, l);
        assertSame(Collections.EMPTY_SET, result);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, result);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();

        childSupport.remove(child);
        childSupport.records.assertRecord("childJustRemovedHook", child,
                MethodInvocationRecords.IGNORE, null);
        childSupport.records.assertEndOfRecords();
        provider.records.assertRecord("releaseService", support, requestor,
                result, null);
        provider.records.assertEndOfRecords();
        assertNull(l.lastRevokedEvent);
    }

    public void testHasService_NullParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        try {
            support.hasService(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testHasService_ParentService() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        assertFalse(childSupport.hasService(Collection.class));

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        assertTrue(childSupport.hasService(Collection.class));
    }

    public void testHasService_ThisService() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        assertFalse(support.hasService(Collection.class));

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        assertTrue(support.hasService(Collection.class));
    }

    public void testInitialize() {
        // covered by other testcases
    }

    public void testInitializeBeanContextResources() {
        // covered by super testcases, nothing more to test here
    }

    public void testReleaseBeanContextResources()
            throws TooManyListenersException, PropertyVetoException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        childSupport.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        support.records.clear();
        childSupport.records.clear();

        Object result = childSupport.getService(child, requestor,
                Collection.class, selector, l);
        assertSame(Collections.EMPTY_SET, result);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, result);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();

        childSupport.setBeanContext(null);
        support.records.assertRecord("childJustRemovedHook", childSupport,
                MethodInvocationRecords.IGNORE, null);
        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        provider.records.assertRecord("releaseService", support, requestor,
                result, null);
        provider.records.assertEndOfRecords();
        assertSame(Collection.class, l.lastRevokedEvent.getServiceClass());
        assertSame(childSupport, l.lastRevokedEvent
                .getSourceAsBeanContextServices());
        assertTrue(l.lastRevokedEvent.isCurrentServiceInvalidNow());
    }

    public void testReleaseService_NullParam() throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        support.records.clear();

        Object service = support.getService(child, requestor, Collection.class,
                selector, l);
        assertSame(Collections.EMPTY_SET, service);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, service);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();

        try {
            support.releaseService(null, requestor, service);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.releaseService(child, null, service);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.releaseService(child, requestor, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testReleaseService_WrongChildOrRequestor()
            throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServiceRevokedListener l = new MockBeanContextServiceRevokedListener();
        support.records.clear();

        Object service = support.getService(child, requestor, Collection.class,
                selector, l);
        assertSame(Collections.EMPTY_SET, service);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, service);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();

        try {
            support.releaseService(new MockBeanContextChild(), requestor,
                    service);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        support.releaseService(child, "xxx", service); // nothing happens
        provider.records.assertEndOfRecords();
        assertNull(l.lastEvent);
    }

    public void testReleaseService_WrongService()
            throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServiceRevokedListener l = new MockBeanContextServiceRevokedListener();
        support.records.clear();

        Object service = support.getService(child, requestor, Collection.class,
                selector, l);
        assertSame(Collections.EMPTY_SET, service);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, service);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();

        support.releaseService(child, requestor, "xxxx service"); // nothing
        // happens
        provider.records.assertEndOfRecords();
        assertNull(l.lastEvent);
    }
    
    /*
     * regression test for HARMONY-4272
     */
    public void testReleaseService_WithNullServiceRecords()
            throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child); 
        support.releaseService(child, child, new Object()); 
    }

    public void testReleaseService() throws TooManyListenersException,
            PropertyVetoException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        childSupport.add(child);
        Object requestor = "a requestor";
        Object selector = "a selector";
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        support.records.clear();
        childSupport.records.clear();

        Object service = childSupport.getService(child, requestor,
                Collection.class, selector, l);
        assertSame(Collections.EMPTY_SET, service);
        provider.records.assertRecord("getService", support, requestor,
                Collection.class, selector, service);
        provider.records.assertEndOfRecords();
        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();

        try {
            support.releaseService(child, requestor, service);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        childSupport.releaseService(child, requestor, service);
        childSupport.records.assertEndOfRecords();
        support.records.assertEndOfRecords();
        provider.records.assertRecord("releaseService", support, requestor,
                service, null);
        provider.records.assertEndOfRecords();
    }

    public void testRemoveBeanContextServicesListener_NullParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        try {
            support.removeBeanContextServicesListener(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testRemoveBeanContextServicesListener() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesListener l = new MockBeanContextServicesListener();
        assertEquals(0, support.bcsListeners().size());

        support.addBeanContextServicesListener(l);
        assertEquals(1, support.bcsListeners().size());
        assertSame(l, support.bcsListeners().get(0));

        support.removeBeanContextServicesListener(l);
        assertEquals(0, support.bcsListeners().size());
    }

    public void testRevokeService_NullParam() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);

        try {
            support.revokeService(null, provider, false);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.revokeService(Collection.class, null, false);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testRevokeService_NonServiceClass() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        support.records.clear();
        provider.records.clear();

        support.revokeService(List.class, provider, false);
        assertNull(l1.lastRevokedEvent);
        support.records.assertEndOfRecords();
        provider.records.assertEndOfRecords();
    }

    public void testRevokeService_NonWrongProvider() {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        support.records.clear();
        provider.records.clear();

        try {
            support.revokeService(Collection.class,
                    new MockBeanContextServiceProvider(), false);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertNull(l1.lastRevokedEvent);
        support.records.assertEndOfRecords();
        provider.records.assertEndOfRecords();
    }

    public void testRevokeService_ParentContext()
            throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        support.addService(Collection.class, provider);
        MockBeanContextChild child = new MockBeanContextChild();
        childSupport.add(child);
        String requestor = "requestor";
        String selector = "selector";
        MockBeanContextServiceRevokedListener rl = new MockBeanContextServiceRevokedListener();
        Object service = childSupport.getService(child, requestor,
                Collection.class, selector, rl);
        assertNotNull(service);

        support.records.clear();
        childSupport.records.clear();
        provider.records.clear();
        l1.clearLastEvent();
        l2.clearLastEvent();

        support.revokeService(Collection.class, provider, false);
        assertEquals(0, support.services().size());

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        provider.records.assertEndOfRecords();
        assertSame(support, l1.lastRevokedEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l1.lastRevokedEvent.getServiceClass());
        assertFalse(l1.lastRevokedEvent.isCurrentServiceInvalidNow());
        assertNull(l2.lastRevokedEvent);
        assertSame(childSupport, rl.lastEvent.getSourceAsBeanContextServices());
        assertSame(Collection.class, rl.lastEvent.getServiceClass());
        assertFalse(rl.lastEvent.isCurrentServiceInvalidNow());

        support.records.clear();
        childSupport.records.clear();
        provider.records.clear();
        l1.clearLastEvent();
        l2.clearLastEvent();
        rl.clearLastEvent();

        childSupport.releaseService(child, requestor, service);

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        provider.records.assertRecord("releaseService", support, requestor,
                service, null);
        provider.records.assertEndOfRecords();
        assertNull(rl.lastEvent);
    }

    public void testRevokeService_ChildContext()
            throws TooManyListenersException {
        MockBeanContextServicesSupport support = new MockBeanContextServicesSupport();
        MockBeanContextServicesSupport childSupport = new MockBeanContextServicesSupport();
        support.add(childSupport);
        MockBeanContextServicesListener l1 = new MockBeanContextServicesListener();
        MockBeanContextServicesListener l2 = new MockBeanContextServicesListener();
        support.addBeanContextServicesListener(l1);
        childSupport.addBeanContextServicesListener(l2);
        support.records.assertRecord("initialize", null);
        childSupport.records.assertRecord("initialize", null);

        MockBeanContextServiceProvider provider = new MockBeanContextServiceProvider();
        childSupport.addService(Collection.class, provider);
        MockBeanContextServices child = new MockBeanContextServices();
        childSupport.add(child);
        String requestor = "requestor";
        String selector = "selector";
        MockBeanContextServiceRevokedListener rl = new MockBeanContextServiceRevokedListener();
        Object service = childSupport.getService(child, requestor,
                Collection.class, selector, rl);
        assertNotNull(service);

        support.records.clear();
        childSupport.records.clear();
        provider.records.clear();
        l1.clearLastEvent();
        l2.clearLastEvent();

        childSupport.revokeService(Collection.class, provider, true);
        assertTrue(support.services().size() == 0);

        support.records.assertEndOfRecords();
        childSupport.records.assertEndOfRecords();
        provider.records.assertEndOfRecords();
        assertNull(l1.lastRevokedEvent);
        assertSame(childSupport, l2.lastRevokedEvent
                .getSourceAsBeanContextServices());
        assertSame(Collection.class, l2.lastRevokedEvent.getServiceClass());
        assertTrue(l2.lastRevokedEvent.isCurrentServiceInvalidNow());
        assertSame(childSupport, rl.lastEvent.getSourceAsBeanContextServices());
        assertSame(Collection.class, rl.lastEvent.getServiceClass());
        assertTrue(rl.lastEvent.isCurrentServiceInvalidNow());
    }

    public void testServiceAvailable() {
        // covered by testAddService
    }

    public void testServiceRevoked() {
        MockChildBeanContextServicesSupport mockChildBeanContextServicesSupport = new MockChildBeanContextServicesSupport();
        BeanContextServicesSupport beanContextServicesSupport = new BeanContextServicesSupport();
        beanContextServicesSupport.add(mockChildBeanContextServicesSupport);
        BeanContextServiceRevokedEvent beanContextServiceRevokedEvent = new BeanContextServiceRevokedEvent(new BeanContextServicesSupport(), Collection.class,false);
        beanContextServicesSupport.serviceRevoked(beanContextServiceRevokedEvent);
        assertTrue(mockChildBeanContextServicesSupport.revokeCalled);        
    }
    
    public static class MockChildBeanContextServicesSupport extends
            BeanContextServicesSupport {
        private static final long serialVersionUID = 1L;

        public boolean revokeCalled = false;

        public void serviceRevoked(BeanContextServiceRevokedEvent bcssre) {
            revokeCalled = true;
        }
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        BeanContextServicesSupport support = new BeanContextServicesSupport(
                null, Locale.ITALY, true, true);
        support
                .addBeanContextServicesListener(new MockBeanContextServicesListener());
        support
                .addBeanContextServicesListener(new MockBeanContextServicesListenerS(
                        "l2"));
        support
                .addBeanContextServicesListener(new MockBeanContextServicesListenerS(
                        "l3"));
        support
                .addBeanContextServicesListener(new MockBeanContextServicesListener());
        support.addService(Collection.class,
                new MockBeanContextServiceProvider());
        support.addService(List.class,
                new MockBeanContextServiceProviderS("p1"));
        support
                .addService(Set.class,
                        new MockBeanContextServiceProviderS("p2"));
        support.addService(Map.class, new MockBeanContextServiceProvider());

        assertEqualsSerially(support,
                (BeanContextServicesSupport) SerializationTester
                        .getDeserilizedObject(support));
    }
    

    static int serviceRevoked = 0;

	static int serviceAvailable = 0;

	private static class MyListener implements BeanContextServicesListener {

		public void serviceRevoked(BeanContextServiceRevokedEvent event) {
			serviceRevoked++;
		}

		public void serviceAvailable(BeanContextServiceAvailableEvent event) {
			serviceAvailable++;
		}

	}

	private static class MySupport extends BeanContextServicesSupport {

		public void serviceRevoked(BeanContextServiceRevokedEvent event) {
			serviceRevoked++;
		}
		
		public void serviceAvailable(BeanContextServiceAvailableEvent event) {
			serviceAvailable++;
		}
	}

	private static class MyProvider implements BeanContextServiceProvider {

		public void releaseService(BeanContextServices s, Object requestor,
				Object service) {
		}

		public Iterator getCurrentServiceSelectors(BeanContextServices s,
				Class serviceClass) {

			return null;
		}

		public Object getService(BeanContextServices s, Object requestor,
				Class serviceClass, Object serviceSelector) {

			return null;

		}

	}

	public void test_serviceRevoked_LBeanContextServiceRevokedEvent() {
		BeanContextServicesSupport support = new BeanContextServicesSupport();

		support.add(new MySupport());
		support.addBeanContextServicesListener(new MyListener());
		Class c = Object.class;

		support.addService(c, new MyProvider());

		BeanContextServiceRevokedEvent revokeEvent = new BeanContextServiceRevokedEvent(
				support, c, false);

		support.serviceRevoked(revokeEvent);
        assertEquals(0, serviceRevoked);
        assertEquals(2, serviceAvailable);
        
	}

	public void test_serviceAvailable_LBeanContextServiceRevokedEvent() {
		BeanContextServicesSupport support = new BeanContextServicesSupport();

		support.add(new MySupport());
		support.addBeanContextServicesListener(new MyListener());
		Class c = Object.class;

		support.addService(c, new MyProvider());

		BeanContextServiceAvailableEvent availableEvent = new BeanContextServiceAvailableEvent(
				support, c);
	    support.serviceAvailable(availableEvent);
        assertEquals(0, serviceRevoked);
        assertEquals(2, serviceAvailable);
        
	}
	
    
	
     public void testSerialization_Compatibility() throws Exception {
         BeanContextServicesSupport support = new BeanContextServicesSupport(
                 null, Locale.ITALY, true, true);
         support
                 .addBeanContextServicesListener(new MockBeanContextServicesListener());
         support
                 .addBeanContextServicesListener(new MockBeanContextServicesListenerS(
                         "l2"));
         support
                 .addBeanContextServicesListener(new MockBeanContextServicesListenerS(
                         "l3"));
         support
                 .addBeanContextServicesListener(new MockBeanContextServicesListener());
         support.addService(Collection.class,
                 new MockBeanContextServiceProvider());
         support.addService(List.class,
                 new MockBeanContextServiceProviderS("p1"));
         support
                 .addService(Set.class,
                         new MockBeanContextServiceProviderS("p2"));
         support.addService(Map.class, new MockBeanContextServiceProvider());
         SerializationTest.verifyGolden(this, support, new SerializableAssert(){
             public void assertDeserialized(Serializable initial, Serializable deserialized) {
                 assertEqualsSerially((BeanContextServicesSupport) initial,
                         (BeanContextServicesSupport) deserialized);
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
    
    //Regression for HARMONY-3830
    public void testAddService_with_fireEvent_false() {
        MyBeanContextServicesSupport myBeanContextServicesSupport = new MyBeanContextServicesSupport();
        boolean result = myBeanContextServicesSupport.addService(
                MyService.class, new MyBeanContextServiceProvider(), false);
        assertTrue(result);
    }

    public static class MyBeanContextServicesSupport extends
            BeanContextServicesSupport {
        private static final long serialVersionUID = 1L;

        public boolean addService(Class serviceClass,
                BeanContextServiceProvider bcsp, boolean fireEvent) {
            return super.addService(serviceClass, bcsp, fireEvent);
        }
    }

    public static class MyService implements Serializable {
        private static final long serialVersionUID = 1L;
    }
    
    public static class MyBeanContextServiceProvider implements
            BeanContextServiceProvider {
        public Iterator getCurrentServiceSelectors(BeanContextServices arg0,
                Class arg1) {
            return null;
        }

        public Object getService(BeanContextServices arg0, Object arg1,
                Class arg2, Object arg3) {
            return null;
        }
        
        public void releaseService(BeanContextServices arg0, Object arg1,
                Object arg2) {
        }
    }
    
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serviceRevoked = 0;
        serviceAvailable = 0;
    }
}
