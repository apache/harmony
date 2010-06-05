/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */

package org.apache.harmony.lang.management;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.harmony.lang.management.ManagementUtils;
import org.apache.harmony.lang.management.MemoryMXBeanImpl;


public class MemoryMXBeanImplTest extends SingleInstanceDynamicMXBeanImplTestBase {

    private MemoryMXBeanImpl notifierBean;

    protected void setUp() throws Exception {
        super.setUp();
        mb = (MemoryMXBeanImpl) ManagementFactory.getMemoryMXBean();
        notifierBean = (MemoryMXBeanImpl) mb;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttribute() throws Exception {
        // The good attributes...
        assertNotNull(mb.getAttribute("HeapMemoryUsage"));
        assertTrue(mb.getAttribute("HeapMemoryUsage") instanceof CompositeData);
        assertTrue(((CompositeData) (mb.getAttribute("HeapMemoryUsage")))
                .containsKey("committed"));

        assertNotNull(mb.getAttribute("NonHeapMemoryUsage"));
        assertTrue(mb.getAttribute("NonHeapMemoryUsage") instanceof CompositeData);
        assertTrue(((CompositeData) (mb.getAttribute("NonHeapMemoryUsage")))
                .containsKey("max"));

        assertNotNull(mb.getAttribute("ObjectPendingFinalizationCount"));
        assertTrue(mb.getAttribute("ObjectPendingFinalizationCount") instanceof Integer);

        assertNotNull(mb.getAttribute("Verbose"));
        assertTrue(mb.getAttribute("Verbose") instanceof Boolean);

        // A nonexistent attribute should throw an AttributeNotFoundException
        try {
            long rpm = ((Long) (mb.getAttribute("RPM")));
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Type mismatch should result in a casting exception
        try {
            String bad = (String) (mb.getAttribute("HeapMemoryUsage"));
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException ignore) {
        }
    }

    public final void testSetAttribute() throws Exception {
        // The one writable attribute of this type of bean
        Attribute attr = new Attribute("Verbose", new Boolean(true));
        mb.setAttribute(attr);

        // Now check the other attributes can't be set...
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);
        CompositeData cd = ManagementUtils.toMemoryUsageCompositeData(mu);
        attr = new Attribute("HeapMemoryUsage", cd);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("NonHeapMemoryUsage", cd);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("ObjectPendingFinalizationCount", new Long(38));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Try and set the Verbose attribute with an incorrect type.
        attr = new Attribute("Verbose", new Long(42));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an InvalidAttributeValueException.");
        } catch (InvalidAttributeValueException ignore) {
        }
    }

    public final void testInvoke() throws Exception {
        // Only one operation for this bean...
        Object retVal;
        retVal = mb.invoke("gc", new Object[] {}, null);
        assertNull(retVal);
    }

    public final void testGetAttributes() {
        AttributeList attributes = mb.getAttributes(attribs.keySet().toArray(
                new String[] {}));
        assertNotNull(attributes);
        assertTrue(attributes.size() == attribs.size());

        // Check through the returned values
        Iterator<?> it = attributes.iterator();
        while (it.hasNext()) {
            Attribute element = (Attribute) it.next();
            assertNotNull(element);
            String name = element.getName();
            Object value = element.getValue();
            if (attribs.containsKey(name)) {
                if (attribs.get(name).type.equals(Integer.TYPE.getName())) {
                    assertNotNull(value);
                    assertTrue(value instanceof Integer);
                    assertTrue((Integer) value > -1);
                }// end if a String value expected
                else if (attribs.get(name).type.equals(Boolean.TYPE.getName())) {
                    boolean tmp = ((Boolean) value).booleanValue();
                }// end else a long expected
                else if (attribs.get(name).type.equals(CompositeData.class
                        .getName())) {
                    assertNotNull(value);
                    assertTrue(value instanceof CompositeData);
                    // Sanity check on the contents of the returned
                    // CompositeData instance. For this kind of bean
                    // the "wrapped" type must be a MemoryUsage.
                    CompositeData cd = (CompositeData) value;
                    assertTrue(cd.containsKey("committed"));
                    assertTrue(cd.containsKey("init"));
                    assertTrue(cd.containsKey("max"));
                    assertTrue(cd.containsKey("used"));
                    assertFalse(cd.containsKey("trash"));
                }// end else a String array expected
            }// end if a known attribute
            else {
                fail("Unexpected attribute name returned!");
            }// end else an unknown attribute
        }// end while
    }

    public final void testSetAttributes() {
        // Ideal scenario...
        AttributeList attList = new AttributeList();
        Attribute verbose = new Attribute("Verbose", new Boolean(false));
        attList.add(verbose);
        AttributeList setAttrs = mb.setAttributes(attList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 1);
        assertTrue(((Attribute) (setAttrs.get(0))).getName().equals("Verbose"));

        // A failure scenario - a non-existent attribute...
        AttributeList badList = new AttributeList();
        Attribute garbage = new Attribute("H.R. Puffenstuff", new Long(2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);

        // Another failure scenario - a non-writable attribute...
        badList = new AttributeList();
        garbage = new Attribute("ObjectPendingFinalizationCount", new Integer(
                2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);

        // Yet another failure scenario - a wrongly-typed attribute...
        badList = new AttributeList();
        garbage = new Attribute("Verbose", new Long(2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);
    }

    public final void testGetMBeanInfo() {
        MBeanInfo mbi = mb.getMBeanInfo();
        assertNotNull(mbi);

        // Now make sure that what we got back is what we expected.

        // Class name
        assertTrue(mbi.getClassName().equals(mb.getClass().getName()));

        // No public constructors
        MBeanConstructorInfo[] constructors = mbi.getConstructors();
        assertNotNull(constructors);
        assertTrue(constructors.length == 0);

        // One public operation
        MBeanOperationInfo[] operations = mbi.getOperations();
        assertNotNull(operations);
        assertTrue(operations.length == 1);

        // One notification
        MBeanNotificationInfo[] notifications = mbi.getNotifications();
        assertNotNull(notifications);
        assertTrue(notifications.length == 1);

        // Description is just the class name (until I hear it should be
        // different)
        assertTrue(mbi.getDescription().equals(mb.getClass().getName()));

        // Four attributes - some writable.
        MBeanAttributeInfo[] attributes = mbi.getAttributes();
        assertNotNull(attributes);
        assertEquals(4, attributes.length);
        for (int i = 0; i < attributes.length; i++) {
            MBeanAttributeInfo info = attributes[i];
            assertNotNull(info);
            validateAttributeInfo(info);
        }// end for
    }

    // -----------------------------------------------------------------
    // Notification implementation tests follow ....
    // -----------------------------------------------------------------

    /*
     * Class under test for void
     * removeNotificationListener(NotificationListener, NotificationFilter,
     * Object)
     */
    public final void testRemoveNotificationListenerNotificationListenerNotificationFilterObject()
            throws Exception {
        // Register a listener
        NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED);
        SimpleTestListener listener = new SimpleTestListener();
        notifierBean.addNotificationListener(listener, filter, null);

        // Fire off a notification and ensure that the listener receives it.
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);
        MemoryNotificationInfo info = new MemoryNotificationInfo("Tim", mu, 42);
        CompositeData cd = ManagementUtils
                .toMemoryNotificationInfoCompositeData(info);
        Notification notification = new Notification(
                MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), 42);
        notification.setUserData(cd);
        notifierBean.sendNotification(notification);
        assertEquals(1, listener.getNotificationsReceivedCount());

        // Remove the listener
        notifierBean.removeNotificationListener(listener, filter, null);

        // Fire off a notification and ensure that the listener does
        // *not* receive it.
        listener.resetNotificationsReceivedCount();
        notification = new Notification(
                MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), 43);
        notification.setUserData(cd);
        notifierBean.sendNotification(notification);
        assertEquals(0, listener.getNotificationsReceivedCount());

        // Try and remove the listener one more time. Should result in a
        // ListenerNotFoundException being thrown.
        try {
            notifierBean.removeNotificationListener(listener, filter, null);
            fail("Should have thrown a ListenerNotFoundException!");
        } catch (ListenerNotFoundException e) {
        }
    }

    public final void testAddNotificationListener() throws Exception {
        // Add a listener with a handback object.
        SimpleTestListener listener = new SimpleTestListener();
        ArrayList<String> arr = new ArrayList<String>();
        arr.add("Hegemony or survival ?");
        notifierBean.addNotificationListener(listener, null, arr);

        // Fire off a notification and ensure that the listener receives it.
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);
        MemoryNotificationInfo info = new MemoryNotificationInfo("Lloyd", mu,
                42);
        CompositeData cd = ManagementUtils
                .toMemoryNotificationInfoCompositeData(info);
        Notification notification = new Notification(
                MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), 42);
        notification.setUserData(cd);
        notifierBean.sendNotification(notification);
        assertEquals(1, listener.getNotificationsReceivedCount());

        // Verify that the handback is as expected.
        assertNotNull(listener.getHandback());
        assertSame(arr, listener.getHandback());
        ArrayList arr2 = (ArrayList) listener.getHandback();
        assertTrue(arr2.size() == 1);
        assertEquals("Hegemony or survival ?", arr2.get(0));
    }

    /*
     * Class under test for void
     * removeNotificationListener(NotificationListener)
     */
    public final void testRemoveNotificationListenerNotificationListener()
            throws Exception {
        // Add a listener without a filter object.
        SimpleTestListener listener = new SimpleTestListener();
        notifierBean.addNotificationListener(listener, null, null);
        // Fire off a notification and ensure that the listener receives it.
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);
        MemoryNotificationInfo info = new MemoryNotificationInfo("Sinclair",
                mu, 42);
        CompositeData cd = ManagementUtils
                .toMemoryNotificationInfoCompositeData(info);
        Notification notification = new Notification(
                MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), 42);
        notification.setUserData(cd);
        notifierBean.sendNotification(notification);
        assertEquals(1, listener.getNotificationsReceivedCount());

        // Verify that the handback is as expected.
        assertNull(listener.getHandback());

        // Verify the user data of the notification.
        Notification n = listener.getNotification();
        assertNotNull(n);
        verifyNotificationUserData(n.getUserData());

        // Remove the listener
        notifierBean.removeNotificationListener(listener);

        // Fire off a notification and ensure that the listener does
        // *not* receive it.
        listener.resetNotificationsReceivedCount();
        notification = new Notification(
                MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), 43);
        notification.setUserData(cd);
        notifierBean.sendNotification(notification);
        assertEquals(0, listener.getNotificationsReceivedCount());

        // Try and remove the listener one more time. Should result in a
        // ListenerNotFoundException being thrown.
        try {
            notifierBean.removeNotificationListener(listener);
            fail("Should have thrown a ListenerNotFoundException!");
        } catch (ListenerNotFoundException ignore) {
        }
    }

    /**
     * @param userData
     */
    private void verifyNotificationUserData(Object userData) {
        // Should be a CompositeData instance
        assertTrue(userData instanceof CompositeData);
        CompositeData cd = (CompositeData) userData;
        assertTrue(cd.containsKey("poolName"));
        assertTrue(cd.containsKey("usage"));
        assertTrue(cd.containsKey("count"));
        assertTrue(cd.get("poolName") instanceof String);
        assertTrue(((String) cd.get("poolName")).length() > 0);
        assertTrue(cd.get("count") instanceof Long);
        assertTrue(((Long) cd.get("count")) > 0);
    }

    public final void testGetNotificationInfo() {
        MBeanNotificationInfo[] notifications = notifierBean
                .getNotificationInfo();
        assertNotNull(notifications);
        assertTrue(notifications.length > 0);
        for (int i = 0; i < notifications.length; i++) {
            MBeanNotificationInfo info = notifications[i];
            assertEquals(Notification.class.getName(), info.getName());
            assertEquals("Memory Notification", info.getDescription());
            String[] types = info.getNotifTypes();
            for (int j = 0; j < types.length; j++) {
                String type = types[j];
                assertTrue(type
                        .equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)
                        || type
                                .equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED));

            }// end for
        }// end for
    }

    @Override
    protected void populateTestAttributes() {
        attribs = new Hashtable<String, AttributeData>();
        attribs.put("HeapMemoryUsage", new AttributeData(CompositeData.class
                .getName(), true, false, false));
        attribs.put("NonHeapMemoryUsage", new AttributeData(CompositeData.class
                .getName(), true, false, false));
        attribs.put("ObjectPendingFinalizationCount", new AttributeData(
                Integer.TYPE.getName(), true, false, false));
        attribs.put("Verbose", new AttributeData(Boolean.TYPE.getName(), true,
                true, true));
    }
}
