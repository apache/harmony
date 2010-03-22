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
import java.util.Hashtable;
import java.util.Iterator;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.openmbean.CompositeData;

import org.apache.harmony.lang.management.ThreadMXBeanImpl;



public class ThreadMXBeanImplTest extends SingleInstanceDynamicMXBeanImplTestBase {

    protected void setUp() throws Exception {
        super.setUp();
        mb = (ThreadMXBeanImpl) ManagementFactory.getThreadMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttribute() throws Exception {
        // The good attributes...
        assertNotNull(mb.getAttribute("AllThreadIds"));
        assertTrue(mb.getAttribute("AllThreadIds") instanceof long[]);

        assertNotNull(mb.getAttribute("DaemonThreadCount"));
        assertTrue(mb.getAttribute("DaemonThreadCount") instanceof Integer);

        assertNotNull(mb.getAttribute("PeakThreadCount"));
        assertTrue(mb.getAttribute("PeakThreadCount") instanceof Integer);

        assertNotNull(mb.getAttribute("ThreadCount"));
        assertTrue(mb.getAttribute("ThreadCount") instanceof Integer);

        assertNotNull(mb.getAttribute("TotalStartedThreadCount"));
        assertTrue(mb.getAttribute("TotalStartedThreadCount") instanceof Long);

        assertNotNull(mb.getAttribute("CurrentThreadCpuTimeSupported"));
        assertTrue(mb.getAttribute("CurrentThreadCpuTimeSupported") instanceof Boolean);

        if ((Boolean) mb.getAttribute("CurrentThreadCpuTimeSupported")) {
            assertNotNull(mb.getAttribute("CurrentThreadCpuTime"));
            assertTrue(mb.getAttribute("CurrentThreadCpuTime") instanceof Long);

            assertNotNull(mb.getAttribute("CurrentThreadUserTime"));
            assertTrue(mb.getAttribute("CurrentThreadUserTime") instanceof Long);
        } else {
            try {
                long t1 = (Long) mb.getAttribute("CurrentThreadCpuTime");
                fail("Should have thrown MBeanException");
            } catch (MBeanException ignore) {
            }

            try {
                long t2 = (Long) mb.getAttribute("CurrentThreadUserTime");
                fail("Should have thrown MBeanException");
            } catch (MBeanException ignore) {
            }
        }

        assertNotNull(mb.getAttribute("ThreadContentionMonitoringSupported"));
        assertTrue((mb.getAttribute("ThreadContentionMonitoringSupported")) instanceof Boolean);

        if ((Boolean) mb.getAttribute("ThreadContentionMonitoringSupported")) {
            assertNotNull(mb.getAttribute("ThreadContentionMonitoringEnabled"));
            assertTrue(mb.getAttribute("ThreadContentionMonitoringEnabled") instanceof Boolean);
        } else {
            try {
                boolean b = ((Boolean) (mb
                        .getAttribute("ThreadContentionMonitoringEnabled")))
                        .booleanValue();
                fail("Should have thrown MBeanException");
            } catch (MBeanException ignore) {
            }
        }

        assertNotNull(mb.getAttribute("ThreadCpuTimeSupported"));
        assertTrue((mb.getAttribute("ThreadCpuTimeSupported")) instanceof Boolean);

        if ((Boolean) mb.getAttribute("ThreadCpuTimeSupported")) {
            assertNotNull(mb.getAttribute("ThreadCpuTimeEnabled"));
            assertTrue((mb.getAttribute("ThreadCpuTimeEnabled")) instanceof Boolean);
        } else {
            try {
                boolean b = ((Boolean) (mb.getAttribute("ThreadCpuTimeEnabled")))
                        .booleanValue();
                fail("Should have thrown MBeanException");
            } catch (MBeanException ignore) {
            }
        }

        // A nonexistent attribute should throw an AttributeNotFoundException
        try {
            long rpm = ((Long) (mb.getAttribute("RPM")));
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Type mismatch should result in a casting exception
        try {
            String bad = (String) (mb.getAttribute("CurrentThreadUserTime"));
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException ignore) {
        }
    }

    public final void testSetAttribute() throws Exception {
        // There are only two writable attributes in this type.

        Attribute attr = new Attribute("ThreadContentionMonitoringEnabled",
                new Boolean(true));

        if ((Boolean) mb.getAttribute("ThreadContentionMonitoringSupported")) {
            mb.setAttribute(attr);
        } else {
            try {
                mb.setAttribute(attr);
                fail("Should have thrown MBeanException!");
            } catch (MBeanException ignore) {
            }
        }

        attr = new Attribute("ThreadCpuTimeEnabled", new Boolean(true));
        if ((Boolean) mb.getAttribute("ThreadCpuTimeSupported")) {
            mb.setAttribute(attr);
        } else {
            try {
                mb.setAttribute(attr);
                fail("Should have thrown MBeanException!");
            } catch (MBeanException ignore) {
            }
        }

        // The rest of the attempted sets should fail

        attr = new Attribute("AllThreadIds", new long[] { 1L, 2L, 3L, 4L });
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("CurrentThreadCpuTime", 1415L);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("DaemonThreadCount", 1415);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("PeakThreadCount", 1415);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("ThreadCount", 1415);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("TotalStartedThreadCount", 1415L);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("CurrentThreadCpuTimeSupported", true);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("ThreadContentionMonitoringEnabled", true);

        if ((Boolean) mb.getAttribute("ThreadContentionMonitoringSupported")) {
            mb.setAttribute(attr);
        } else {
            try {
                mb.setAttribute(attr);
                fail("Should have thrown an AttributeNotFoundException.");
            } catch (AttributeNotFoundException ignore) {
            }
        }

        attr = new Attribute("ThreadContentionMonitoringSupported", true);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("ThreadCpuTimeEnabled", true);
        if ((Boolean) mb.getAttribute("ThreadCpuTimeSupported")) {
            mb.setAttribute(attr);
        } else {
            try {
                mb.setAttribute(attr);
                fail("Should have thrown an MBeanException.");
            } catch (MBeanException ignore) {
            }
        }

        attr = new Attribute("ThreadCpuTimeSupported", true);
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Try and set an attribute with an incorrect type.
        attr = new Attribute("ThreadContentionMonitoringEnabled", new Long(42));
        if ((Boolean) mb.getAttribute("ThreadContentionMonitoringSupported")) {
            try {
                mb.setAttribute(attr);
                fail("Should have thrown an InvalidAttributeValueException");
            } catch (InvalidAttributeValueException ignore) {
            }
        }
    }

    public final void testGetAttributes() {
        AttributeList attributes = mb.getAttributes(attribs.keySet().toArray(
                new String[] {}));
        assertNotNull(attributes);
        assertTrue(attribs.size() >= attributes.size());

        // Check through the returned values
        Iterator<?> it = attributes.iterator();
        while (it.hasNext()) {
            Attribute element = (Attribute) it.next();
            assertNotNull(element);
            String name = element.getName();
            Object value = element.getValue();
            if (attribs.containsKey(name)) {
                if (attribs.get(name).type.equals(Long.TYPE.getName())) {
                    // Values of -1 are permitted for this kind of bean.
                    // e.g. -1 can be returned from
                    // getCurrentThreadCpuTime()
                    // if CPU time measurement is currently disabled.
                    assertTrue(((Long) (value)) > -2);
                }// end else a long expected
                else if ((attribs.get(name).type)
                        .equals(Boolean.TYPE.getName())) {
                    boolean tmp = ((Boolean) value).booleanValue();
                }// end else a boolean expected
                else if (attribs.get(name).type.equals(Integer.TYPE.getName())) {
                    // Values of -1 are permitted for this kind of bean.
                    // e.g. -1 can be returned from
                    // getCurrentThreadCpuTime()
                    // if CPU time measurement is currently disabled.
                    assertTrue(((Integer) (value)) > -2);
                }// end else a long expected
                else if (attribs.get(name).type.equals("[J")) {
                    long[] tmp = (long[]) value;
                    assertNotNull(tmp);
                }// end else a String array expected
                else {
                    fail("Unexpected attribute type returned! : " + name
                            + " , value = " + value);
                }
            }// end if a known attribute
            else {
                fail("Unexpected attribute name returned!");
            }// end else an unknown attribute
        }// end while
    }

    public final void testSetAttributes() {
        // Ideal scenario...
        AttributeList attList = new AttributeList();
        Attribute tcme = new Attribute("ThreadContentionMonitoringEnabled",
                new Boolean(false));
        Attribute tcte = new Attribute("ThreadCpuTimeEnabled",
                new Boolean(true));
        attList.add(tcme);
        attList.add(tcte);
        AttributeList setAttrs = mb.setAttributes(attList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() <= 2);

        // A failure scenario - a non-existent attribute...
        AttributeList badList = new AttributeList();
        Attribute garbage = new Attribute("Auchenback", new Long(2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);

        // Another failure scenario - a non-writable attribute...
        badList = new AttributeList();
        garbage = new Attribute("ThreadCount", new Long(2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);

        // Yet another failure scenario - a wrongly-typed attribute...
        badList = new AttributeList();
        garbage = new Attribute("ThreadCpuTimeEnabled", new Long(2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);
    }

    public final void testInvoke() throws Exception {
        // This type of bean has 8 different operations that can be invoked
        // on it.
        Object retVal = mb.invoke("findMonitorDeadlockedThreads",
                new Object[] {}, null);
        // Can get a null return if there are currently no deadlocked
        // threads.

        // Good case.
        retVal = mb.invoke("getThreadCpuTime", new Object[] { new Long(Thread
                .currentThread().getId()) },
                new String[] { Long.TYPE.getName() });
        assertNotNull(retVal);
        assertTrue(retVal instanceof Long);

        // Force exception by passing in a negative Thread id
        try {
            retVal = mb.invoke("getThreadCpuTime",
                    new Object[] { new Long(-757) }, new String[] { Long.TYPE
                            .getName() });
            fail("Should have thrown an IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        // Good case. long
        retVal = mb.invoke("getThreadInfo", new Object[] { new Long(Thread
                .currentThread().getId()) },
                new String[] { Long.TYPE.getName() });
        assertNotNull(retVal);
        assertTrue(retVal instanceof CompositeData);
        CompositeData cd = (CompositeData) retVal;
        assertTrue(cd.containsKey("threadId"));

        // Force exception by passing in a negative Thread id. long
        try {
            retVal = mb.invoke("getThreadInfo",
                    new Object[] { new Long(-5353) }, new String[] { Long.TYPE
                            .getName() });
            fail("Should have thrown an IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        // Good case. long, int
        retVal = mb.invoke("getThreadInfo", new Object[] {
                new Long(Thread.currentThread().getId()), new Integer(0) },
                new String[] { Long.TYPE.getName(), Integer.TYPE.getName() });
        // TODO Can't test until we can get back ThreadInfo objects
        // from the getThreadInfo(long) method.
        assertNotNull(retVal);
        assertTrue(retVal instanceof CompositeData);
        cd = (CompositeData) retVal;
        assertTrue(cd.containsKey("threadId"));

        // Force exception by passing in a negative Thread id. long, int
        try {
            retVal = mb.invoke("getThreadInfo", new Object[] { new Long(-8467),
                    new Integer(0) }, new String[] { Long.TYPE.getName(),
                    Integer.TYPE.getName() });
            fail("Should have thrown an IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        // Good case. long[], int
        retVal = mb.invoke("getThreadInfo",
                new Object[] { new long[] { Thread.currentThread().getId() },
                        new Integer(0) }, new String[] { "[J",
                        Integer.TYPE.getName() });
        // TODO Can't test until we can get back ThreadInfo objects
        // from the getThreadInfo(long) method.
        assertNotNull(retVal);
        assertTrue(retVal instanceof CompositeData[]);
        CompositeData[] cdArray = (CompositeData[]) retVal;
        assertTrue(cdArray[0].containsKey("threadId"));

        // Force exception by passing in a negative Thread id. long[], int
        try {
            retVal = mb.invoke("getThreadInfo", new Object[] {
                    new long[] { -54321L }, new Integer(0) }, new String[] {
                    "[J", Integer.TYPE.getName() });
            fail("Should have thrown an IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        // Good case. long[]
        retVal = mb.invoke("getThreadInfo", new Object[] { new long[] { Thread
                .currentThread().getId() } }, new String[] { "[J" });
        assertNotNull(retVal);
        assertTrue(retVal instanceof CompositeData[]);
        cdArray = (CompositeData[]) retVal;
        assertTrue(cdArray[0].containsKey("threadId"));

        // Force exception by passing in a negative Thread id. long[]
        try {
            retVal = mb.invoke("getThreadInfo",
                    new Object[] { new long[] { -74747L } },
                    new String[] { "[J" });
            fail("Should have thrown an IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        // Good case.
        retVal = mb.invoke("getThreadUserTime", new Object[] { new Long(Thread
                .currentThread().getId()) },
                new String[] { Long.TYPE.getName() });
        assertNotNull(retVal);
        assertTrue(retVal instanceof Long);

        // Force exception by passing in a negative Thread id
        try {
            retVal = mb.invoke("getThreadUserTime", new Object[] { new Long(
                    -757) }, new String[] { Long.TYPE.getName() });
            fail("Should have thrown an IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        retVal = mb.invoke("resetPeakThreadCount", new Object[] {}, null);
        assertNull(retVal);
        // Verify that after this operation is invoked, the
        // peak thread count equals the value of current thread count
        // taken prior to this method call.
        assertTrue(((Integer) mb.getAttribute("PeakThreadCount"))
                .equals((Integer) mb.getAttribute("ThreadCount")));
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

        // No public operations
        MBeanOperationInfo[] operations = mbi.getOperations();
        assertNotNull(operations);
        assertTrue(operations.length == 8);

        // No notifications
        MBeanNotificationInfo[] notifications = mbi.getNotifications();
        assertNotNull(notifications);
        assertTrue(notifications.length == 0);

        // Description is just the class name (until I hear it should be
        // different)
        assertTrue(mbi.getDescription().equals(mb.getClass().getName()));

        // Sixteen attributes - none writable.
        MBeanAttributeInfo[] attributes = mbi.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.length == 12);
        for (int i = 0; i < attributes.length; i++) {
            MBeanAttributeInfo info = attributes[i];
            assertNotNull(info);
            validateAttributeInfo(info);
        }// end for
    }

    @Override
    protected void populateTestAttributes() {
        attribs = new Hashtable<String, AttributeData>();
        attribs
                .put("AllThreadIds",
                        new AttributeData("[J", true, false, false));
        attribs.put("CurrentThreadCpuTime", new AttributeData(Long.TYPE
                .getName(), true, false, false));
        attribs.put("CurrentThreadUserTime", new AttributeData(Long.TYPE
                .getName(), true, false, false));
        attribs.put("DaemonThreadCount", new AttributeData(Integer.TYPE
                .getName(), true, false, false));
        attribs.put("PeakThreadCount", new AttributeData(
                Integer.TYPE.getName(), true, false, false));
        attribs.put("ThreadCount", new AttributeData(Integer.TYPE.getName(),
                true, false, false));
        attribs.put("TotalStartedThreadCount", new AttributeData(Long.TYPE
                .getName(), true, false, false));
        attribs.put("CurrentThreadCpuTimeSupported", new AttributeData(
                Boolean.TYPE.getName(), true, false, true));
        attribs.put("ThreadContentionMonitoringEnabled", new AttributeData(
                Boolean.TYPE.getName(), true, true, true));
        attribs.put("ThreadContentionMonitoringSupported", new AttributeData(
                Boolean.TYPE.getName(), true, false, true));
        attribs.put("ThreadCpuTimeEnabled", new AttributeData(Boolean.TYPE
                .getName(), true, true, true));
        attribs.put("ThreadCpuTimeSupported", new AttributeData(Boolean.TYPE
                .getName(), true, false, true));
    }
}
