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

import java.awt.Button;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.Visibility;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextChildSupport;
import java.beans.beancontext.BeanContextMembershipEvent;
import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextProxy;
import java.beans.beancontext.BeanContextServicesSupport;
import java.beans.beancontext.BeanContextSupport;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.beancontext.MethodInvocationRecords;
import org.apache.harmony.beans.tests.support.beancontext.Utils;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContext;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextChild;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextChildS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextDelegateS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextMembershipListener;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextMembershipListenerS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextProxy;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockBeanContextProxyS;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockPropertyChangeListener;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockVetoChangeListener;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockVetoableChangeListener;
import org.apache.harmony.beans.tests.support.beancontext.mock.MockVisibility;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.util.SerializationTester;

/**
 * Test BeanContextSupport
 */
@SuppressWarnings("unchecked")
public class BeanContextSupportTest extends TestCase {

    private static class MockBeanContextSupport extends BeanContextSupport {

        private static final long serialVersionUID = -4165267256277214588L;

        transient MethodInvocationRecords records;

        transient boolean vetoAddRemove = false;
        
        transient boolean waitOnChildInHooks = true;

        /**
         * 
         */
        public MockBeanContextSupport() {
            super();
        }

        /**
         * @param peer
         */
        public MockBeanContextSupport(BeanContext peer) {
            super(peer);
        }

        /**
         * @param peer
         * @param lcle
         */
        public MockBeanContextSupport(BeanContext peer, Locale lcle) {
            super(peer, lcle);
        }

        /**
         * @param peer
         * @param lcle
         * @param dtime
         */
        public MockBeanContextSupport(BeanContext peer, Locale lcle,
                boolean dtime) {
            super(peer, lcle, dtime);
        }

        /**
         * @param peer
         * @param lcle
         * @param dTime
         * @param visible
         */
        public MockBeanContextSupport(BeanContext peer, Locale lcle,
                boolean dTime, boolean visible) {
            super(peer, lcle, dTime, visible);
        }

        public ArrayList bcmListeners() {
            return bcmListeners;
        }

        public Iterator publicBcsChildren() {
            return this.bcsChildren();
        }

        public static boolean publicClassEquals(Class c1, Class c2) {
            return classEquals(c1, c2);
        }

        public Object[] publicCopyChildren() {
            return copyChildren();
        }

        public static BeanContextChild publicGetChildBeanContextChild(
                Object child) {
            return getChildBeanContextChild(child);
        }

        public static BeanContextMembershipListener publicGetChildBeanContextMembershipListener(
                Object child) {
            return getChildBeanContextMembershipListener(child);
        }

        public static PropertyChangeListener publicGetChildPropertyChangeListener(
                Object child) {
            return getChildPropertyChangeListener(child);
        }

        public static Serializable publicGetChildSerializable(Object child) {
            return getChildSerializable(child);
        }

        public static VetoableChangeListener publicGetChildVetoableChangeListener(
                Object child) {
            return getChildVetoableChangeListener(child);
        }

        public static Visibility publicGetChildVisibility(Object child) {
            return getChildVisibility(child);
        }

        public boolean publicRemove(Object targetChild, boolean callChildBC) {
            return remove(targetChild, callChildBC);
        }

        public HashMap children() {
            return children;
        }

        public boolean designTime() {
            return designTime;
        }

        public Locale locale() {
            return locale;
        }

        public boolean isOkToUseGui() {
            return okToUseGui;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#bcsPreDeserializationHook(java.io.ObjectInputStream)
         */
        @Override
        protected void bcsPreDeserializationHook(ObjectInputStream ois)
                throws IOException, ClassNotFoundException {
            super.bcsPreDeserializationHook(ois);
            if (records == null) {
                records = new MethodInvocationRecords();
            }
            records.add("bcsPreDeserializationHook", ois, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#bcsPreSerializationHook(java.io.ObjectOutputStream)
         */
        @Override
        protected void bcsPreSerializationHook(ObjectOutputStream oos)
                throws IOException {
            super.bcsPreSerializationHook(oos);
            records.add("bcsPreSerializationHook", oos, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#childDeserializedHook(java.lang.Object,
         *      java.beans.beancontext.BeanContextSupport.BCSChild)
         */
        @Override
        protected void childDeserializedHook(Object child, BCSChild bcsc) {
            super.childDeserializedHook(child, bcsc);
            records.add("childDeserializedHook", child, bcsc, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#childJustAddedHook(java.lang.Object,
         *      java.beans.beancontext.BeanContextSupport.BCSChild)
         */
        @Override
        protected void childJustAddedHook(Object child, BCSChild bcsc) {
            if (waitOnChildInHooks) {
                // check lock
                try {
                    child.wait(1);
                } catch (InterruptedException e) {
                    // never occur
                }
            }
            super.childJustAddedHook(child, bcsc);
            records.add("childJustAddedHook", child, bcsc, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#childJustRemovedHook(java.lang.Object,
         *      java.beans.beancontext.BeanContextSupport.BCSChild)
         */
        @Override
        protected void childJustRemovedHook(Object child, BCSChild bcsc) {
            if (waitOnChildInHooks) {
                // check lock
                try {
                    child.wait(1);
                } catch (InterruptedException e) {
                    // never occur
                }
            }
            super.childJustRemovedHook(child, bcsc);
            records.add("childJustRemovedHook", child, bcsc, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#createBCSChild(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        protected BCSChild createBCSChild(Object targetChild, Object peer) {
            BCSChild result = super.createBCSChild(targetChild, peer);
            records.add("createBCSChild", targetChild, peer, result);
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#initialize()
         */
        @Override
        protected void initialize() {
            super.initialize();
            if (records == null) {
                records = new MethodInvocationRecords();
            }
            records.add("initialize", null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            super.propertyChange(pce);
            records.add("propertyChange", pce, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#validatePendingAdd(java.lang.Object)
         */
        @Override
        protected boolean validatePendingAdd(Object targetChild) {
            boolean result = vetoAddRemove ? false : super
                    .validatePendingAdd(targetChild);
            records.add("validatePendingAdd", targetChild, Boolean
                    .valueOf(result));
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.beancontext.BeanContextSupport#validatePendingRemove(java.lang.Object)
         */
        @Override
        protected boolean validatePendingRemove(Object targetChild) {
            boolean result = vetoAddRemove ? false : super
                    .validatePendingRemove(targetChild);
            records.add("validatePendingRemove", targetChild, Boolean
                    .valueOf(result));
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.beans.VetoableChangeListener#vetoableChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void vetoableChange(PropertyChangeEvent pce)
                throws PropertyVetoException {
            super.vetoableChange(pce);
            records.add("vetoableChange", pce, null);
        }
    }

    private static class BadChild implements BeanContextChild, BeanContextProxy {

        public void setBeanContext(BeanContext bc) throws PropertyVetoException {
            // Auto-generated method stub
        }

        public BeanContext getBeanContext() {
            // Auto-generated method stub
            return null;
        }

        public void addPropertyChangeListener(String name,
                PropertyChangeListener pcl) {
            // Auto-generated method stub
        }

        public void removePropertyChangeListener(String name,
                PropertyChangeListener pcl) {
            // Auto-generated method stub
        }

        public void addVetoableChangeListener(String name,
                VetoableChangeListener vcl) {
            // Auto-generated method stub
        }

        public void removeVetoableChangeListener(String name,
                VetoableChangeListener vcl) {
            // Auto-generated method stub
        }

        public BeanContextChild getBeanContextProxy() {
            // Auto-generated method stub
            return null;
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BeanContextSupportTest.class);

        // MockBeanContextSupport support = new MockBeanContextSupport();
        // BeanContextChild childPeer = new MockBeanContextChild();
        // BeanContextProxy child = new MockBeanContextProxy(childPeer);
        // support.add(child);
        // System.out.println(support.records);
        // System.out.println(support.children());
    }

    public void testAdd_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        try {
            support.add(null);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testAdd_NonBCC() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockPropertyChangeListener l2 = new MockPropertyChangeListener();
        MockVetoableChangeListener l3 = new MockVetoableChangeListener();
        support.addBeanContextMembershipListener(l1);
        support.addPropertyChangeListener("children", l2);
        support.addVetoableChangeListener("children", l3);

        Integer child = new Integer(1000);
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        assertNull(l2.lastEvent);
        assertNull(l3.lastEvent);
    }

    public void testAdd_BCC() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockPropertyChangeListener l2 = new MockPropertyChangeListener();
        MockVetoableChangeListener l3 = new MockVetoableChangeListener();
        support.addBeanContextMembershipListener(l1);
        support.addPropertyChangeListener("children", l2);
        support.addVetoableChangeListener("children", l3);

        BeanContextChild child = new MockBeanContextChild();
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        assertNull(l2.lastEvent);
        assertNull(l3.lastEvent);

        assertSame(support, child.getBeanContext());
    }

    public void testAdd_BCP() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.waitOnChildInHooks = false;
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockPropertyChangeListener l2 = new MockPropertyChangeListener();
        MockVetoableChangeListener l3 = new MockVetoableChangeListener();
        support.addBeanContextMembershipListener(l1);
        support.addPropertyChangeListener("children", l2);
        support.addVetoableChangeListener("children", l3);

        BeanContextChild childPeer = new MockBeanContextChild();
        BeanContextProxy child = new MockBeanContextProxy(childPeer);
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, childPeer,
                support.children().get(child));
        support.records.assertRecord("createBCSChild", childPeer, child,
                support.children().get(childPeer));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertRecord("childJustAddedHook", childPeer, support
                .children().get(childPeer), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, Arrays
                .asList(new Object[] { child, childPeer }));
        assertNull(l2.lastEvent);
        assertNull(l3.lastEvent);

        assertSame(support, childPeer.getBeanContext());
        assertEquals(2, support.size());
    }

    public void testAdd_Exist() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);
        Integer child = new Integer(1000);
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        support.records.clear();
        l1.clearLastEvent();

        support.add(child);
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);
    }

    public void testAdd_Veto() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockPropertyChangeListener l2 = new MockPropertyChangeListener();
        MockVetoableChangeListener l3 = new MockVetoableChangeListener();
        support.addBeanContextMembershipListener(l1);
        support.addPropertyChangeListener("children", l2);
        support.addVetoableChangeListener("children", l3);

        support.vetoAddRemove = true;
        BeanContextChild child = new MockBeanContextChild();
        try {
            support.add(child);
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
        support.records.assertRecord("initialize", null);
        support.records
                .assertRecord("validatePendingAdd", child, Boolean.FALSE);
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);
        assertNull(l2.lastEvent);
        assertNull(l3.lastEvent);

        assertNull(child.getBeanContext());
    }

    public void testAddAll() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockPropertyChangeListener l2 = new MockPropertyChangeListener();
        MockVetoableChangeListener l3 = new MockVetoableChangeListener();
        support.addBeanContextMembershipListener(l1);
        support.addPropertyChangeListener("children", l2);
        support.addVetoableChangeListener("children", l3);
        support.records.assertRecord("initialize", null);

        try {
            //Regression for HARMONY-2350
            support.addAll(Collections.EMPTY_LIST);
            fail();
        } catch (UnsupportedOperationException e) {
            // expected
        }
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);
        assertNull(l2.lastEvent);
        assertNull(l3.lastEvent);
    }

    public void testAddBeanContextMembershipListener_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        try {
            support.addBeanContextMembershipListener(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testAddBeanContextMembershipListener() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockBeanContextMembershipListener l2 = new MockBeanContextMembershipListener();

        support.addBeanContextMembershipListener(l1);
        support.addBeanContextMembershipListener(l2);

        l1.clearLastEvent();
        l2.clearLastEvent();
        Object child = new MockBeanContextChild();
        support.add(child);
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        assertTrue(l2.lastEventAdd);
        assertMembershipEvent(l2.lastEvent, support, null, child);

        l1.clearLastEvent();
        l2.clearLastEvent();
        support.add(child);
        assertNull(l1.lastEvent);
        assertNull(l2.lastEvent);

        support.removeBeanContextMembershipListener(l1);
        l1.clearLastEvent();
        l2.clearLastEvent();
        support.remove(child);
        assertNull(l1.lastEvent);
        assertTrue(l2.lastEventRemove);
        assertMembershipEvent(l2.lastEvent, support, null, child);
    }

    public void testAvoidingGui() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        assertTrue(support.isOkToUseGui());
        assertFalse(support.avoidingGui());
        assertFalse(support.needsGui());

        support.dontUseGui();
        assertFalse(support.isOkToUseGui());
        // assertFalse(support.avoidingGui());
        assertFalse(support.needsGui());

        support.okToUseGui();
        assertTrue(support.isOkToUseGui());
        assertFalse(support.avoidingGui());
        assertFalse(support.needsGui());
    }

    public void testAvoidingGui_VisibleChild() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add(new MockVisibility(true, false));
        assertFalse(support.avoidingGui());
    }

    public void testBcsChildren() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add(new Integer(1));
        support.add(new MockBeanContextChild());

        Collection expectedChildren = support.children().values();
        Iterator it = support.publicBcsChildren();
        int count = 0;
        while (it.hasNext()) {
            count++;
            assertTrue(expectedChildren.contains(it.next()));
        }
        assertEquals(count, expectedChildren.size());
    }

    public void testBcsPreDeserializationHook() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("string value");
        support.add(new Integer(129));
        support.add(Locale.CHINESE);
        support.records.clear();

        byte bytes[] = serialize(support);
        support.records.assertRecord("bcsPreSerializationHook",
                MethodInvocationRecords.IGNORE, null);
        support.records.assertEndOfRecords();

        MockBeanContextSupport copy = (MockBeanContextSupport) deserialize(bytes);
        copy.records.assertRecord("initialize", null);
        copy.records.assertRecord("bcsPreDeserializationHook",
                MethodInvocationRecords.IGNORE, null);
        Iterator it = support.iterator();
        while (it.hasNext()) {
            Object expectedChild = it.next();
            assertTrue(copy.contains(expectedChild));
            copy.records.assertRecord("childDeserializedHook", expectedChild,
                    MethodInvocationRecords.IGNORE, null);
        }
        copy.records.assertEndOfRecords();
    }

    public void testBcsPreSerializationHook() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("string value");
        support.add(new Integer(129));
        support.add(Locale.CHINESE);
        support.records.clear();

        serialize(support);
        support.records.assertRecord("bcsPreSerializationHook",
                MethodInvocationRecords.IGNORE, null);
        support.records.assertEndOfRecords();
    }

    /*
     * Class under test for void BeanContextSupport()
     */
    public void testBeanContextSupport() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        assertSame(support, support.getBeanContextPeer());
        assertTrue(support.bcmListeners().isEmpty());
        assertTrue(support.children().isEmpty());
        assertFalse(support.designTime());
        assertEquals(Locale.getDefault(), support.locale());
        assertTrue(support.isOkToUseGui());
    }

    /*
     * Class under test for void
     * BeanContextSupport(java.beans.beancontext.BeanContext)
     */
    public void testBeanContextSupportBeanContext() {
        BeanContext ctx = new MockBeanContext();
        MockBeanContextSupport support = new MockBeanContextSupport(ctx);

        assertSame(ctx, support.getBeanContextPeer());
        assertTrue(support.bcmListeners().isEmpty());
        assertTrue(support.children().isEmpty());
        assertFalse(support.designTime());
        assertEquals(Locale.getDefault(), support.locale());
        assertTrue(support.isOkToUseGui());
    }

    /*
     * Class under test for void
     * BeanContextSupport(java.beans.beancontext.BeanContext, java.util.Locale)
     */
    public void testBeanContextSupportBeanContextLocale() {
        BeanContext ctx = new MockBeanContext();
        MockBeanContextSupport support = new MockBeanContextSupport(ctx,
                Locale.CANADA_FRENCH);

        assertSame(ctx, support.getBeanContextPeer());
        assertTrue(support.bcmListeners().isEmpty());
        assertTrue(support.children().isEmpty());
        assertFalse(support.designTime());
        assertEquals(Locale.CANADA_FRENCH, support.locale());
        assertTrue(support.isOkToUseGui());
    }

    /*
     * Class under test for void
     * BeanContextSupport(java.beans.beancontext.BeanContext, java.util.Locale,
     * boolean)
     */
    public void testBeanContextSupportBeanContextLocaleboolean() {
        BeanContext ctx = new MockBeanContext();
        MockBeanContextSupport support = new MockBeanContextSupport(ctx,
                Locale.CANADA_FRENCH, true);

        assertSame(ctx, support.getBeanContextPeer());
        assertTrue(support.bcmListeners().isEmpty());
        assertTrue(support.children().isEmpty());
        assertTrue(support.designTime());
        assertEquals(Locale.CANADA_FRENCH, support.locale());
        assertTrue(support.isOkToUseGui());
    }

    /*
     * Class under test for void
     * BeanContextSupport(java.beans.beancontext.BeanContext, java.util.Locale,
     * boolean, boolean)
     */
    public void testBeanContextSupportBeanContextLocalebooleanboolean() {
        BeanContext ctx = new MockBeanContext();
        MockBeanContextSupport support = new MockBeanContextSupport(ctx,
                Locale.CANADA_FRENCH, true, false);

        assertSame(ctx, support.getBeanContextPeer());
        assertTrue(support.bcmListeners().isEmpty());
        assertTrue(support.children().isEmpty());
        assertTrue(support.designTime());
        assertEquals(Locale.CANADA_FRENCH, support.locale());
        assertFalse(support.isOkToUseGui());
    }

    public void testBeanContextSupport_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport(null, null,
                true, true);
        assertSame(support, support.getBeanContextPeer());
        assertTrue(support.children().isEmpty());
        assertTrue(support.designTime());
        assertEquals(Locale.getDefault(), support.locale());
        assertTrue(support.isOkToUseGui());
    }

    public void testChildDeserializedHook() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("string value");
        support.add(new Integer(129));
        support.add(Locale.CHINESE);
        support.records.clear();

        byte bytes[] = serialize(support);
        support.records.assertRecord("bcsPreSerializationHook",
                MethodInvocationRecords.IGNORE, null);
        support.records.assertEndOfRecords();

        MockBeanContextSupport copy = (MockBeanContextSupport) deserialize(bytes);
        copy.records.assertRecord("initialize", null);
        copy.records.assertRecord("bcsPreDeserializationHook",
                MethodInvocationRecords.IGNORE, null);
        Iterator it = support.iterator();
        while (it.hasNext()) {
            Object expectedChild = it.next();
            assertTrue(copy.contains(expectedChild));
            copy.records.assertRecord("childDeserializedHook", expectedChild,
                    MethodInvocationRecords.IGNORE, null);
        }
        copy.records.assertEndOfRecords();
    }

    public void testChildJustAddedHook() {
        // covered by testAdd
    }

    public void testChildJustRemovedHook() {
        // covered by testRemove
    }

    public void testClassEquals() {
        assertTrue(MockBeanContextSupport.publicClassEquals(Integer.class,
                Integer.class));
        assertFalse(MockBeanContextSupport.publicClassEquals(Integer.class,
                Double.class));
        try {
            MockBeanContextSupport.publicClassEquals(null, null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    public void testClear() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        try {
            support.clear();
            fail();
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testContains() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        Object c1 = "string value";
        Object c2 = new Integer(129);
        Object c3 = new MockBeanContextChild();
        support.add(c1);
        support.add(c2);
        support.add(c3);

        assertTrue(support.children().containsKey(c1));
        assertTrue(support.children().containsKey(c2));
        assertTrue(support.children().containsKey(c3));
        assertFalse(support.children().containsKey(null));
        assertFalse(support.children().containsKey("xxx"));
        assertEquals(3, support.children().size());

        assertTrue(support.contains(c1));
        assertTrue(support.contains(c2));
        assertTrue(support.contains(c3));
        assertFalse(support.contains(null));
        assertFalse(support.contains("xxx"));
        assertEquals(3, support.size());
    }

    public void testContainsAll() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        Object c1 = "string value";
        Object c2 = new Integer(129);
        Object c3 = new MockBeanContextChild();
        support.add(c1);
        support.add(c2);
        support.add(c3);
        ArrayList<Object> l = new ArrayList<Object>();
        l.add(c1);
        l.add(c2);
        l.add(c3);

        assertTrue(support.containsAll(l));
        l.add(null);
        assertFalse(support.containsAll(l));
    }

    public void testContainsKey() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        Object c1 = "string value";
        Object c2 = new Integer(129);
        Object c3 = new MockBeanContextChild();
        support.add(c1);
        support.add(c2);
        support.add(c3);

        assertTrue(support.children().containsKey(c1));
        assertTrue(support.children().containsKey(c2));
        assertTrue(support.children().containsKey(c3));
        assertFalse(support.children().containsKey(null));
        assertFalse(support.children().containsKey("xxx"));
        assertEquals(3, support.children().size());

        assertTrue(support.containsKey(c1));
        assertTrue(support.containsKey(c2));
        assertTrue(support.containsKey(c3));
        assertFalse(support.containsKey(null));
        assertFalse(support.containsKey("xxx"));
        assertEquals(3, support.size());
    }

    public void testCopyChildren() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        Object c1 = "string value";
        Object c2 = new Integer(129);
        Object c3 = new MockBeanContextChild();
        support.add(c1);
        support.add(c2);
        support.add(c3);

        Object children[] = support.publicCopyChildren();
        List<Object> childrenList = Arrays.asList(children);
        assertEquals(3, childrenList.size());
        assertTrue(childrenList.contains(c1));
        assertTrue(childrenList.contains(c2));
        assertTrue(childrenList.contains(c3));
    }

    public void testCreateBCSChild() {
        // covered by testAdd
    }

    public void testDeserialize() {
        // covered by testBcsPreDeserializationHook()
    }

    public void testDontUseGui() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        assertTrue(support.isOkToUseGui());
        assertFalse(support.avoidingGui()); // always false
        assertFalse(support.needsGui()); // always false

        support.dontUseGui();
        assertFalse(support.isOkToUseGui());
        // assertFalse(support.avoidingGui()); // always false?
        assertFalse(support.needsGui()); // always false
    }

    public void testFireChildrenAdded() {
        // covered by testAdd
    }

    public void testFireChildrenRemoved() {
        // covered by testRemove
    }

    public void testGetBeanContextPeer() {
        // covered by testConstructor
    }

    public void testGetChildBeanContextChild_NullParam() {
        BeanContextChild result = MockBeanContextSupport
                .publicGetChildBeanContextChild(null);
        assertNull(result);
    }

    public void testGetChildBeanContextChild_BeanContextChild() {
        MockBeanContextChild child = new MockBeanContextChild();
        BeanContextChild result = MockBeanContextSupport
                .publicGetChildBeanContextChild(child);
        assertSame(child, result);

        // Regression for HARMONY-1393
        class TestBeanException extends BeanContextChildSupport implements
                BeanContextProxy {
            private static final long serialVersionUID = -8544245159647566063L;
            private final BeanContextChildSupport childSupport = new BeanContextChildSupport();

            public BeanContextChild getBeanContextProxy() {
                return childSupport;
            }
        }
        TestBeanException bean = new TestBeanException();
        try {
            MockBeanContextSupport.publicGetChildBeanContextChild(bean);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetChildBeanContextChild_BeanContextProxy() {
        MockBeanContextChild child = new MockBeanContextChild();
        MockBeanContextProxy proxy = new MockBeanContextProxy(child);
        BeanContextChild result = MockBeanContextSupport
                .publicGetChildBeanContextChild(proxy);
        assertSame(child, result);
    }

    public void testGetChildBeanContextChild_Neither() {
        Integer child = new Integer(129);
        BeanContextChild result = MockBeanContextSupport
                .publicGetChildBeanContextChild(child);
        assertNull(result);
    }

    public void testGetChildBeanContextChild_Both() {
        try {
            MockBeanContextSupport
                    .publicGetChildBeanContextChild(new BadChild());
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetChildBeanContextMembershipListener_NullParam() {
        BeanContextMembershipListener result = MockBeanContextSupport
                .publicGetChildBeanContextMembershipListener(null);
        assertNull(result);
    }

    public void testGetChildBeanContextMembershipListener() {
        MockBeanContextMembershipListener child = new MockBeanContextMembershipListener();
        BeanContextMembershipListener result = MockBeanContextSupport
                .publicGetChildBeanContextMembershipListener(child);
        assertSame(child, result);
    }

    public void testGetChildBeanContextMembershipListener_WrongClass() {
        BeanContextMembershipListener result = MockBeanContextSupport
                .publicGetChildBeanContextMembershipListener(new Integer(129));
        assertNull(result);
    }

    public void testGetChildPropertyChangeListener_NullParam() {
        PropertyChangeListener result = MockBeanContextSupport
                .publicGetChildPropertyChangeListener(null);
        assertNull(result);
    }

    public void testGetChildPropertyChangeListener() {
        MockBeanContextSupport child = new MockBeanContextSupport();
        PropertyChangeListener result = MockBeanContextSupport
                .publicGetChildPropertyChangeListener(child);
        assertSame(child, result);
    }

    public void testGetChildPropertyChangeListener_WrongClass() {
        PropertyChangeListener result = MockBeanContextSupport
                .publicGetChildPropertyChangeListener(new Integer(129));
        assertNull(result);
    }

    public void testGetChildSerializable_NullParam() {
        Serializable result = MockBeanContextSupport
                .publicGetChildSerializable(null);
        assertNull(result);
    }

    public void testGetChildSerializable() {
        MockBeanContextSupport child = new MockBeanContextSupport();
        Serializable result = MockBeanContextSupport
                .publicGetChildSerializable(child);
        assertSame(child, result);
    }

    public void testGetChildSerializable_WrongClass() {
        Serializable result = MockBeanContextSupport
                .publicGetChildSerializable(new BadChild());
        assertNull(result);
    }

    public void testGetChildVetoableChangeListener_NullParam() {
        VetoableChangeListener result = MockBeanContextSupport
                .publicGetChildVetoableChangeListener(null);
        assertNull(result);
    }

    public void testGetChildVetoableChangeListener() {
        MockBeanContextSupport child = new MockBeanContextSupport();
        VetoableChangeListener result = MockBeanContextSupport
                .publicGetChildVetoableChangeListener(child);
        assertSame(child, result);
    }

    public void testGetChildVetoableChangeListener_WrongClass() {
        VetoableChangeListener result = MockBeanContextSupport
                .publicGetChildVetoableChangeListener(new Integer(129));
        assertNull(result);
    }

    public void testGetChildVisibility_NullParam() {
        Visibility result = MockBeanContextSupport
                .publicGetChildVisibility(null);
        assertNull(result);
    }

    public void testGetChildVisibility() {
        MockBeanContextSupport child = new MockBeanContextSupport();
        Visibility result = MockBeanContextSupport
                .publicGetChildVisibility(child);
        assertSame(child, result);
    }

    public void testGetChildVisibility_WrongClass() {
        Visibility result = MockBeanContextSupport
                .publicGetChildVisibility(new Integer(129));
        assertNull(result);
    }

    public void testGetLocale() throws PropertyVetoException {
        BeanContext ctx = new MockBeanContext();
        MockBeanContextSupport support = new MockBeanContextSupport(ctx,
                Locale.CANADA_FRENCH);

        assertSame(Locale.CANADA_FRENCH, support.getLocale());

        support.setLocale(Locale.CHINA);
        assertSame(Locale.CHINA, support.getLocale());
    }

    public void testGetResource_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);

        try {
            support.getResource(null, child);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.getResource("", null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetResource_NonChild() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();

        try {
            support.getResource("", child);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetResource_NotExist() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);

        URL url = support
                .getResource(
                        "org/apache/harmony/beans/tests/java/beans/beancontext/mock/nonexist",
                        child);
        assertNull(url);
    }

    public void testGetResource() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);

        final String RESOURCE_NAME = "org/apache/harmony/beans/tests/support/beancontext/mock/mockdata.txt";
        URL url = support.getResource(RESOURCE_NAME, child);
        assertTrue(url.toString().endsWith(RESOURCE_NAME));
        
        BeanContextSupport beanContextSupport = new BeanContextSupport();
        beanContextSupport.add(child);
        url = beanContextSupport.getResource(RESOURCE_NAME, child);
        assertTrue(url.toString().endsWith(RESOURCE_NAME));
    }

    public void testGetResourceAsStream_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);

        try {
            support.getResourceAsStream(null, child);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            support.getResourceAsStream("", null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetResourceAsStream_NonChild() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();

        try {
            support.getResourceAsStream("", child);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetResourceAsStream_NotExist() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);

        InputStream ins = support
                .getResourceAsStream(
                        "org/apache/harmony/beans/tests/java/beans/beancontext/mock/nonexist",
                        child);
        assertNull(ins);
    }

    public void testGetResourceAsStream() throws IOException {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextChild child = new MockBeanContextChild();
        support.add(child);

        final String RESOURCE_NAME = "org/apache/harmony/beans/tests/support/beancontext/mock/mockdata.txt";
        InputStream ins = support.getResourceAsStream(RESOURCE_NAME, child);
        assertEquals("mockdata", new BufferedReader(new InputStreamReader(ins))
                .readLine());
        ins.close();
    }

    public void testInitialize() {
        // covered by other testcases
    }

    public void testInstantiateChild_NullParam() throws IOException,
            ClassNotFoundException {
        MockBeanContextSupport support = new MockBeanContextSupport();

        try {
            support.instantiateChild(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testInstantiateChild() throws IOException,
            ClassNotFoundException {
        MockBeanContextSupport support = new MockBeanContextSupport();

        MockBeanContextChild child = (MockBeanContextChild) support
                .instantiateChild(MockBeanContextChild.class.getName());
        assertTrue(support.contains(child));
        assertEquals(1, support.size());
    }

    public void testIsDesignTime() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        assertFalse(support.isDesignTime());

        support.setDesignTime(true);
        assertTrue(support.isDesignTime());
    }

    public void testIsEmpty() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        assertTrue(support.isEmpty());

        support.add("a child");
        assertFalse(support.isEmpty());
    }

    public void testIsSerializing() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        assertFalse(support.isSerializing());
    }

    public void testIterator() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("string value");
        support.add(new Integer(129));
        support.add(Locale.CHINESE);

        Iterator iter = support.iterator();
        for (int i = 0; i < 3; i++) {
            assertTrue(support.contains(iter.next()));
            iter.remove();
        }
        assertFalse(iter.hasNext());
        assertEquals(3, support.size());
    }

    public void testNeedsGui_NoVisibleChild() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        assertFalse(support.needsGui());

        support.add("a child");
        assertFalse(support.needsGui());

        support.add(new MockBeanContextChild());
        assertFalse(support.needsGui());
    }

    public void testNeedsGui_ComponentChild() {
        /*
         * MockBeanContextSupport support = new MockBeanContextSupport();
         * assertFalse(support.needsGui());
         * 
         * Component child = new Component() {/* mock
         */
        // };
        /*
         * support.add(child); assertTrue(support.needsGui());
         */
    }

    public void testNeedsGui_ContainerChild() {
        // MockBeanContextSupport support = new MockBeanContextSupport();
        // assertFalse(support.needsGui());
        //
        // Container child = new Container() {/* mock */
        // };
        // support.add(child);
        // assertTrue(support.needsGui());
    }

    public void testNeedsGui_VisibilityChild() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        assertFalse(support.needsGui());

        Visibility child = new MockVisibility(false, true);
        support.add(child);
        assertTrue(support.needsGui());
    }

    public void testNeedsGui_VisiblePeer() {
        // MockBeanContextSupport peer = new MockBeanContextSupport();
        // peer.add(new Container() {/* mock */
        // });
        // MockBeanContextSupport support = new MockBeanContextSupport(peer);
        //
        // assertTrue(support.needsGui());
    }

    public void testOkToUseGui() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        assertTrue(support.isOkToUseGui());
        assertFalse(support.avoidingGui());
        assertFalse(support.needsGui());

        support.dontUseGui();
        assertFalse(support.isOkToUseGui());
        // assertFalse(support.avoidingGui());
        assertFalse(support.needsGui());

        support.okToUseGui();
        assertTrue(support.isOkToUseGui());
        assertFalse(support.avoidingGui());
        assertFalse(support.needsGui());
    }

    public void testPropertyChange() throws PropertyVetoException {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockPropertyChangeListener l1 = new MockPropertyChangeListener();
        support.addPropertyChangeListener("locale", l1);
        support.records.assertRecord("initialize", null);

        support.setLocale(Locale.ITALY);
        support.records.assertEndOfRecords();
        assertSame(support, l1.lastEvent.getSource());
        assertSame(Locale.getDefault(), l1.lastEvent.getOldValue());
        assertSame(Locale.ITALY, l1.lastEvent.getNewValue());

        support.addPropertyChangeListener("locale", support);
        support.setLocale(Locale.CANADA);
        PropertyChangeEvent evt = (PropertyChangeEvent) support.records
                .getArg(0);
        assertSame(support, evt.getSource());
        assertSame(Locale.ITALY, l1.lastEvent.getOldValue());
        assertSame(Locale.CANADA, l1.lastEvent.getNewValue());
        support.records.assertRecord("propertyChange", evt, null);
        support.records.assertEndOfRecords();
    }

    public void testReadChildren() throws PropertyVetoException, IOException,
            ClassNotFoundException {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.setLocale(Locale.ITALY);
        support.add("string value");
        support.add(new Integer(129));
        support.add(Locale.CHINESE);

        byte data[] = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        support.writeChildren(oout);
        oout.close();
        data = bout.toByteArray();
        support.records.clear();

        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        ObjectInputStream oin = new ObjectInputStream(bin);
        support.readChildren(oin);
        oin.close();

        Iterator it = support.iterator();
        while (it.hasNext()) {
            Object expectedChild = it.next();
            support.records.assertRecord("childDeserializedHook",
                    expectedChild, MethodInvocationRecords.IGNORE, null);
        }
        assertEquals(3, support.size());
        support.records.assertEndOfRecords();
    }

    public void test_readChildren_NPE_scenario1() throws Exception {
        BeanContextSupport beanContextSupport = new BeanContextSupport();
        beanContextSupport.add(beanContextSupport);
        assertEquals(1, beanContextSupport.size());
        assertFalse(beanContextSupport.isSerializing());
        try {
            beanContextSupport.readChildren((ObjectInputStream) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void test_readChildren_NPE_scenario2() throws Exception {
        BeanContextSupport beanContextSupport = new BeanContextSupport();
        beanContextSupport.readChildren((ObjectInputStream) null);
    }

    public void test_readChildren_NPE_scenario3() throws Exception {
        BeanContextSupport beanContextSupport = new BeanContextSupport();
        beanContextSupport.add(new Object());
        beanContextSupport.readChildren((ObjectInputStream) null);
    }

    public void test_readChildren_NPE_scenario4() throws Exception {
        BeanContextSupport beanContextSupport = new BeanContextSupport();
        beanContextSupport.add("Serializable");
        try {
            beanContextSupport.readChildren((ObjectInputStream) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
        beanContextSupport.remove("Serializable");
        beanContextSupport.readChildren((ObjectInputStream) null);
    }

    public void testRemoveAll() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.records.assertRecord("initialize", null);
        try {
            support.removeAll(Collections.EMPTY_LIST);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        support.records.assertEndOfRecords();
    }

    public void testRemoveBeanContextMembershipListener_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        try {
            support.removeBeanContextMembershipListener(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testRemoveBeanContextMembershipListener() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        MockBeanContextMembershipListener l2 = new MockBeanContextMembershipListener();

        support.addBeanContextMembershipListener(l1);
        support.addBeanContextMembershipListener(l2);

        l1.clearLastEvent();
        l2.clearLastEvent();
        Object child = new MockBeanContextChild();
        support.add(child);
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        assertTrue(l2.lastEventAdd);
        assertMembershipEvent(l2.lastEvent, support, null, child);

        support.removeBeanContextMembershipListener(l1);
        l1.clearLastEvent();
        l2.clearLastEvent();
        support.remove(child);
        assertNull(l1.lastEvent);
        assertTrue(l2.lastEventRemove);
        assertMembershipEvent(l2.lastEvent, support, null, child);

        support.removeBeanContextMembershipListener(l1);
        l1.clearLastEvent();
        l2.clearLastEvent();
        support.remove(child);
        assertNull(l1.lastEvent);
        assertNull(l2.lastEvent);
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        try {
            support.remove(null);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_NonBCC() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);
        Integer child = new Integer(1000);
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        support.records.clear();
        l1.clearLastEvent();

        Object bcsChild = support.children().get(child);
        support.remove(child);
        support.records.assertRecord("validatePendingRemove", child,
                Boolean.TRUE);
        support.records.assertRecord("childJustRemovedHook", child, bcsChild,
                null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventRemove);
        assertMembershipEvent(l1.lastEvent, support, null, child);

        assertEquals(0, support.size());
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_BCC() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);

        BeanContextChild child = new MockBeanContextChild();
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        support.records.clear();
        l1.clearLastEvent();

        Object bcsChild = support.children().get(child);
        support.remove(child);
        support.records.assertRecord("validatePendingRemove", child,
                Boolean.TRUE);
        support.records.assertRecord("childJustRemovedHook", child, bcsChild,
                null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventRemove);
        assertMembershipEvent(l1.lastEvent, support, null, child);

        assertNull(child.getBeanContext());
        assertEquals(0, support.size());
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_BCP() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.waitOnChildInHooks = false;
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);

        BeanContextChild childPeer = new MockBeanContextChild();
        BeanContextProxy child = new MockBeanContextProxy(childPeer);
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, childPeer,
                support.children().get(child));
        support.records.assertRecord("createBCSChild", childPeer, child,
                support.children().get(childPeer));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertRecord("childJustAddedHook", childPeer, support
                .children().get(childPeer), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, Arrays
                .asList(new Object[] { child, childPeer }));
        support.records.clear();
        l1.clearLastEvent();

        Object bcsChild = support.children().get(child);
        Object bcsChildPeer = support.children().get(childPeer);
        support.remove(child);
        support.records.assertRecord("validatePendingRemove", child,
                Boolean.TRUE);
        support.records.assertRecord("childJustRemovedHook", child, bcsChild,
                null);
        support.records.assertRecord("childJustRemovedHook", childPeer,
                bcsChildPeer, null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventRemove);
        assertMembershipEvent(l1.lastEvent, support, null, Arrays
                .asList(new Object[] { child, childPeer }));

        assertNull(childPeer.getBeanContext());
        assertEquals(0, support.size());
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_BCP2() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.waitOnChildInHooks = false;
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);

        BeanContextChild childPeer = new MockBeanContextChild();
        BeanContextProxy child = new MockBeanContextProxy(childPeer);
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, childPeer,
                support.children().get(child));
        support.records.assertRecord("createBCSChild", childPeer, child,
                support.children().get(childPeer));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertRecord("childJustAddedHook", childPeer, support
                .children().get(childPeer), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, Arrays
                .asList(new Object[] { child, childPeer }));
        support.records.clear();
        l1.clearLastEvent();

        Object bcsChild = support.children().get(child);
        Object bcsChildPeer = support.children().get(childPeer);
        support.remove(childPeer);
        support.records.assertRecord("validatePendingRemove", childPeer,
                Boolean.TRUE);
        support.records.assertRecord("childJustRemovedHook", childPeer,
                bcsChildPeer, null);
        support.records.assertRecord("childJustRemovedHook", child, bcsChild,
                null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventRemove);
        assertMembershipEvent(l1.lastEvent, support, null, Arrays
                .asList(new Object[] { child, childPeer }));

        assertNull(childPeer.getBeanContext());
        assertEquals(0, support.size());
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_NonExist() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);

        support.remove(new MockBeanContextChild());
        support.records.assertRecord("initialize", null);
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);

        assertEquals(0, support.size());
    }

    /*
     * Class under test for boolean remove(java.lang.Object)
     */
    public void testRemoveObject_Veto() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);

        BeanContextChild child = new MockBeanContextChild();
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        support.records.clear();
        l1.clearLastEvent();

        support.children().get(child);
        support.vetoAddRemove = true;
        try {
            support.remove(child);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
        support.records.assertRecord("validatePendingRemove", child,
                Boolean.FALSE);
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);

        assertSame(support, child.getBeanContext());
        assertEquals(1, support.size());
    }

    /*
     * Class under test for boolean remove(java.lang.Object, boolean)
     */
    public void testRemoveObjectboolean() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockBeanContextMembershipListener l1 = new MockBeanContextMembershipListener();
        support.addBeanContextMembershipListener(l1);

        BeanContextChild child = new MockBeanContextChild();
        support.add(child);
        support.records.assertRecord("initialize", null);
        support.records.assertRecord("validatePendingAdd", child, Boolean.TRUE);
        support.records.assertRecord("createBCSChild", child, null, support
                .children().get(child));
        support.records.assertRecord("childJustAddedHook", child, support
                .children().get(child), null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventAdd);
        assertMembershipEvent(l1.lastEvent, support, null, child);
        support.records.clear();
        l1.clearLastEvent();

        Object bcsChild = support.children().get(child);
        support.publicRemove(child, false); // don't call child's setBeanContext
        support.records.assertRecord("validatePendingRemove", child,
                Boolean.TRUE);
        support.records.assertRecord("childJustRemovedHook", child, bcsChild,
                null);
        support.records.assertEndOfRecords();
        assertTrue(l1.lastEventRemove);
        assertMembershipEvent(l1.lastEvent, support, null, child);

        assertSame(support, child.getBeanContext());
        assertEquals(0, support.size());
    }

    public void testRetainAll() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        try {
            support.retainAll(Collections.EMPTY_LIST);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testSerialize() {
        // covered by testBcsPreSerializationHook
    }

    public void testSetDesignTime() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockPropertyChangeListener l1 = new MockPropertyChangeListener();
        MockVetoableChangeListener l2 = new MockVetoableChangeListener();
        support.addPropertyChangeListener("designTime", l1);
        support.addVetoableChangeListener("designTime", l2);
        assertFalse(support.isDesignTime());
        support.records.assertRecord("initialize", null);
        support.records.assertEndOfRecords();

        support.setDesignTime(true);
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);
        assertNull(l2.lastEvent);
    }

    public void testSetLocale_NullParam() throws PropertyVetoException {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.setLocale(Locale.ITALY);
        assertSame(Locale.ITALY, support.getLocale());
        support.records.assertRecord("initialize", null);
        support.records.assertEndOfRecords();

        support.setLocale(null);
        assertSame(Locale.ITALY, support.getLocale());
        support.records.assertEndOfRecords();
    }

    public void testSetLocale() throws PropertyVetoException {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockPropertyChangeListener l1 = new MockPropertyChangeListener();
        MockVetoableChangeListener l2 = new MockVetoableChangeListener();
        support.addPropertyChangeListener("locale", l1);
        support.addVetoableChangeListener("locale", l2);
        assertSame(Locale.getDefault(), support.getLocale());
        support.records.assertRecord("initialize", null);
        support.records.assertEndOfRecords();

        support.setLocale(Locale.ITALY);
        assertSame(Locale.ITALY, support.getLocale());
        support.records.assertEndOfRecords();
        assertSame(support, l1.lastEvent.getSource());
        assertEquals("locale", l1.lastEvent.getPropertyName());
        assertEquals(Locale.getDefault(), l1.lastEvent.getOldValue());
        assertEquals(Locale.ITALY, l1.lastEvent.getNewValue());
        assertSame(support, l2.lastEvent.getSource());
        assertEquals("locale", l2.lastEvent.getPropertyName());
        assertEquals(Locale.getDefault(), l2.lastEvent.getOldValue());
        assertEquals(Locale.ITALY, l2.lastEvent.getNewValue());
    }

    public void testSetLocale_Veto() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockPropertyChangeListener l1 = new MockPropertyChangeListener();
        MockVetoChangeListener l2 = new MockVetoChangeListener();
        support.addPropertyChangeListener("locale", l1);
        support.addVetoableChangeListener("locale", l2);
        assertSame(Locale.getDefault(), support.getLocale());
        support.records.assertRecord("initialize", null);
        support.records.assertEndOfRecords();

        try {
            support.setLocale(Locale.ITALY);
            fail("PropertyVetoException expected");
        } catch (PropertyVetoException e) {
            // expected
        }
        assertSame(Locale.getDefault(), support.getLocale());
        support.records.assertEndOfRecords();
        assertNull(l1.lastEvent);
        assertSame(support, l2.lastEvent.getSource());
        assertEquals("locale", l2.lastEvent.getPropertyName());
        assertEquals(Locale.getDefault(), l2.lastEvent.getNewValue());
        assertEquals(Locale.ITALY, l2.lastEvent.getOldValue());
    }

    public void testSize() {
        @SuppressWarnings("serial")
        class TestBean extends Component implements BeanContextProxy {
            public BeanContextChildSupport childSupport = new BeanContextChildSupport();

            public BeanContextChild getBeanContextProxy() {
                return childSupport;
            }
        }
        
        // Regression test for HARMONY-1829
        BeanContextSupport obj = new BeanContextSupport();
        obj.add(new TestBean());
        assertEquals(2, obj.size());
    }

    /*
     * Class under test for java.lang.Object[] toArray()
     */
    public void testToArray() {
        MockBeanContextSupport support = new MockBeanContextSupport();

        Object[] array = support.toArray();
        assertEquals(0, array.length);

        support.add(new Integer(1000));
        support.add("a child");
        support.add(new MockBeanContextChild());
        array = support.toArray();
        assertEquals(3, array.length);
        int count = 0;
        for (Object element : array) {
            if (element instanceof Integer) {
                assertEquals(new Integer(1000), element);
                count += 1;
            }
            if (element instanceof String) {
                assertEquals("a child", element);
                count += 2;
            }
            if (element instanceof MockBeanContextChild) {
                count += 4;
            }
        }
        assertEquals(7, count);
    }

    /*
     * Class under test for java.lang.Object[] toArray(java.lang.Object[])
     */
    public void testToArrayObjectArray_NullParam() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        try {
            support.toArray(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for java.lang.Object[] toArray(java.lang.Object[])
     */
    public void testToArrayObjectArray_WrongType() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("a");
        support.add("b");
        support.add("c");
        try {
            support.toArray(new Integer[0]);
            fail("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    /*
     * Class under test for java.lang.Object[] toArray(java.lang.Object[])
     */
    public void testToArrayObjectArray_LesserLength() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("a");
        support.add("b");
        support.add("c");
        String in[] = new String[] { "1" };
        String out[] = (String[]) support.toArray(in);
        assertNotSame(in, out);
        List<String> expected = Arrays.asList(new String[] { "a", "b", "c" });
        assertEquals(expected.size(), out.length);
        for (String element : out) {
            assertTrue(expected.contains(element));
        }
    }

    /*
     * Class under test for java.lang.Object[] toArray(java.lang.Object[])
     */
    public void testToArrayObjectArray_RightLength() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("a");
        support.add("b");
        support.add("c");
        String in[] = new String[3];
        String out[] = (String[]) support.toArray(in);
        assertSame(in, out);
        List<String> expected = Arrays.asList(new String[] { "a", "b", "c" });
        assertEquals(expected.size(), out.length);
        for (String element : out) {
            assertTrue(expected.contains(element));
        }
    }

    /*
     * Class under test for java.lang.Object[] toArray(java.lang.Object[])
     */
    public void testToArrayObjectArray_GreaterLength() {
        MockBeanContextSupport support = new MockBeanContextSupport();
        support.add("a");
        support.add("b");
        support.add("c");
        String in[] = new String[5];
        String out[] = (String[]) support.toArray(in);
        assertSame(in, out);
        List<String> expected = Arrays.asList(new String[] { "a", "b", "c"});
        for (int i = 0; i < expected.size(); i++) {
            assertTrue(expected.contains(out[i]));
        }
        assertNull(out[3]);
        assertNull(out[4]);
    }

    public void testValidatePendingAdd() {
        // covered by testAdd
    }

    public void testValidatePendingRemove() {
        // covered by testRemove
    }

    public void testVetoableChange() throws PropertyVetoException {
        MockBeanContextSupport support = new MockBeanContextSupport();
        MockVetoableChangeListener l1 = new MockVetoableChangeListener();
        support.addVetoableChangeListener("locale", l1);
        support.records.assertRecord("initialize", null);

        support.setLocale(Locale.ITALY);
        support.records.assertEndOfRecords();
        assertSame(support, l1.lastEvent.getSource());
        assertSame(Locale.getDefault(), l1.lastEvent.getOldValue());
        assertSame(Locale.ITALY, l1.lastEvent.getNewValue());

        support.addVetoableChangeListener("locale", support);
        support.setLocale(Locale.CANADA);
        PropertyChangeEvent evt = (PropertyChangeEvent) support.records
                .getArg(0);
        assertSame(support, evt.getSource());
        assertEquals("locale", evt.getPropertyName());
        assertSame(Locale.ITALY, l1.lastEvent.getOldValue());
        assertSame(Locale.CANADA, l1.lastEvent.getNewValue());
        support.records.assertRecord("vetoableChange", evt, null);
        support.records.assertEndOfRecords();
    }

    public void testWriteChildren() {
        // covered by testReadChildren()
    }

    public void testSerialization_NoPeer() throws IOException,
            ClassNotFoundException {
        BeanContextSupport support = new BeanContextSupport(null, Locale.ITALY,
                true, true);
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListener());
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListenerS(
                        "l2"));
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListenerS(
                        "l3"));
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListener());
        support.add("abcd");
        support.add(new MockBeanContextChild());
        support.add(new MockBeanContextChildS("a child"));
        support.add(new MockBeanContextChild());
        support.add("1234");
        support.add(new MockBeanContextProxyS("proxy",
                new MockBeanContextChildS("b child")));

        assertEqualsSerially(support, (BeanContextSupport) SerializationTester
                .getDeserilizedObject(support));
    }

    public void testSerialization_Peer() throws IOException,
            ClassNotFoundException {
        MockBeanContextDelegateS mock = new MockBeanContextDelegateS("main id");
        BeanContextSupport support = mock.support;
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListener());
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListenerS(
                        "l2"));
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListenerS(
                        "l3"));
        support
                .addBeanContextMembershipListener(new MockBeanContextMembershipListener());
        support.add("abcd");
        support.add(new MockBeanContextChild());
        support.add(new MockBeanContextChildS("a child"));
        support.add(new MockBeanContextChild());
        support.add("1234");
        support.add(new MockBeanContextProxyS("proxy",
                new MockBeanContextChildS("b child")));

        MockBeanContextDelegateS serMock = (MockBeanContextDelegateS) SerializationTester
                .getDeserilizedObject(mock);
        assertEquals(mock.id, serMock.id);
        assertSame(mock, mock.support.beanContextChildPeer);
        assertSame(serMock, serMock.support.beanContextChildPeer);
        assertEqualsSerially(mock.support, serMock.support);
    }

     public void testSerialization_Compatibility() throws Exception {
         MockBeanContextDelegateS mock = new MockBeanContextDelegateS("main id");
         BeanContextSupport support = mock.support;
         support.addBeanContextMembershipListener(new MockBeanContextMembershipListener());
         support.addBeanContextMembershipListener(new MockBeanContextMembershipListenerS("l2"));
         support.addBeanContextMembershipListener(new MockBeanContextMembershipListenerS("l3"));
         support.addBeanContextMembershipListener(new MockBeanContextMembershipListener());
         support.add("abcd");
         support.add(new MockBeanContextChild());
         support.add(new MockBeanContextChildS("a child"));
         support.add(new MockBeanContextChild());
         support.add("1234");
         SerializationTest.verifyGolden(this, mock, new SerializableAssert(){
             public void assertDeserialized(Serializable orig, Serializable ser) {
                 MockBeanContextDelegateS serMock = (MockBeanContextDelegateS) ser;
                 MockBeanContextDelegateS mock = (MockBeanContextDelegateS) orig;
                 assertEquals(mock.id, serMock.id);
                 assertSame(mock, mock.support.beanContextChildPeer);
                 assertSame(serMock, serMock.support.beanContextChildPeer);
                 assertEqualsSerially(mock.support, serMock.support);
             }
         });
     }
 
    private byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(obj);
            oout.close();
            return bout.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private Object deserialize(byte[] bytes) {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(bin);
            Object result = oin.readObject();
            oin.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void assertMembershipEvent(BeanContextMembershipEvent evt,
            BeanContext ctx, BeanContext pFrom, Object changes) {
        assertSame(ctx, evt.getSource());
        assertSame(ctx, evt.getBeanContext());
        assertSame(pFrom, evt.getPropagatedFrom());
        if (changes instanceof Collection) {
            Collection changeCollection = (Collection) changes;
            assertEquals(changeCollection.size(), evt.size());
            for (Iterator iter = changeCollection.iterator(); iter.hasNext();) {
                assertTrue(evt.contains(iter.next()));
            }
        } else {
            assertEquals(1, evt.size());
            assertTrue(evt.contains(changes));
        }
    }

    public static void assertEqualsSerially(BeanContextSupport orig,
            BeanContextSupport ser) {

        // check bcmListeners
        ArrayList origBcmListeners = (ArrayList) Utils.getField(orig,
                "bcmListeners");
        ArrayList serBcmListeners = (ArrayList) Utils.getField(ser,
                "bcmListeners");
        int i = 0, j = 0;
        while (i < origBcmListeners.size()) {
            Object l1 = origBcmListeners.get(i);
            if (l1 instanceof Serializable) {
                Object l2 = serBcmListeners.get(j);
                assertSame(l1.getClass(), l2.getClass());
                if (l1 instanceof MockBeanContextMembershipListenerS) {
                    assertEquals(((MockBeanContextMembershipListenerS) l1).id,
                            ((MockBeanContextMembershipListenerS) l2).id);
                }
                j++;
            }
            i++;
        }
        assertEquals(j, serBcmListeners.size());

        // check children
        HashMap origChildren = (HashMap) Utils.getField(orig, "children");
        HashMap serChildren = (HashMap) Utils.getField(ser, "children");
        int count = 0;
        for (Iterator iter = origChildren.keySet().iterator(); iter.hasNext();) {
            Object child = iter.next();
            if (child instanceof Serializable) {
                if (child instanceof String) {
                    assertTrue(serChildren.containsKey(child));
                }
                if (child instanceof MockBeanContextChildS) {
                    assertTrue(serChildren.containsKey(child));
                    MockBeanContextChildS serChild = (MockBeanContextChildS) Utils
                            .getField(serChildren.get(child), "child");
                    assertSame(ser.getBeanContextPeer(), serChild
                            .getBeanContext());
                }
                if (child instanceof MockBeanContextProxyS) {
                    assertTrue(serChildren.containsKey(child));
                }
                count++;
            }
        }
        assertEquals(count, serChildren.size());

        // check other fields
        assertEquals(Utils.getField(orig, "locale"), Utils.getField(ser,
                "locale"));
        assertEquals(Utils.getField(orig, "okToUseGui"), Utils.getField(ser,
                "okToUseGui"));
        assertEquals(Utils.getField(orig, "designTime"), Utils.getField(ser,
                "designTime"));
    }


    public void testPropertyChangePropertyChangeEvent() {
        BeanContextServicesSupport s = new BeanContextServicesSupport();
        PropertyChangeSupport p= new PropertyChangeSupport(new Object());

        p.addPropertyChangeListener(s);
        p.firePropertyChange(null, new Object(), new Object());
    }
    
    //Regression Test for HARMONY-3757
    public void testSelfSerializatoin() throws Exception {
        BeanContextSupport beanContextSupport = new BeanContextSupport();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(beanContextSupport);
        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        Object obj = oin.readObject();
        assertTrue(obj instanceof BeanContextSupport);
    }
    
    public void testAvoidGui() throws Exception
    {
        MockBeanContextSupport1 mockBeanContextSupport1 = new MockBeanContextSupport1();
        mockBeanContextSupport1.setOkToUseGui(false);
        assertFalse(mockBeanContextSupport1.avoidingGui());
        
        mockBeanContextSupport1 = new MockBeanContextSupport1();
        mockBeanContextSupport1.setOkToUseGui(true);
        assertFalse(mockBeanContextSupport1.avoidingGui());
        
        mockBeanContextSupport1 = new MockBeanContextSupport1();
        Component component = new Button();
        mockBeanContextSupport1.add(component);
        mockBeanContextSupport1.setOkToUseGui(false);
        assertTrue(mockBeanContextSupport1.needsGui());
        assertTrue(mockBeanContextSupport1.avoidingGui());
        
        mockBeanContextSupport1 = new MockBeanContextSupport1();
        component = new Button();
        mockBeanContextSupport1.add(component);        
        mockBeanContextSupport1.setOkToUseGui(true);
        assertTrue(mockBeanContextSupport1.needsGui());
        assertFalse(mockBeanContextSupport1.avoidingGui());     
    }
    
    
    public class MockBeanContextSupport1 extends BeanContextSupport
    {
        private static final long serialVersionUID = 1L;

        public void setOkToUseGui(boolean ok)
        {
            this.okToUseGui = ok;
        }      
    }

}
