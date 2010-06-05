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
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.apache.harmony.lang.management.DynamicMXBeanImpl;
import org.apache.harmony.lang.management.MemoryPoolMXBeanImpl;


public class MemoryPoolMXBeanImplTest extends
        MultiInstanceDynamicMXBeanImplTestBase {

    protected void setUp() throws Exception {
        super.setUp();
        mbList = new ArrayList<DynamicMXBeanImpl>();
        List<MemoryPoolMXBean> allBeans = ManagementFactory
                .getMemoryPoolMXBeans();
        for (MemoryPoolMXBean bean : allBeans) {
            mbList.add((MemoryPoolMXBeanImpl) bean);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttributes() {
        for (DynamicMXBeanImpl mb : mbList) {
            AttributeList attributes = mb.getAttributes(attribs.keySet()
                    .toArray(new String[] {}));
            assertNotNull(attributes);
            assertTrue(attributes.size() <= attribs.size());

            Iterator<?> it = attributes.iterator();
            while (it.hasNext()) {
                Attribute element = (Attribute) it.next();
                assertNotNull(element);
                String name = element.getName();
                Object value = element.getValue();
                if (name.equals("CollectionUsage")) {
                    // Could return a null value if VM does not support
                    // the method.
                    if (value != null) {
                        assertTrue(value instanceof CompositeData);
                        MemoryUsage mu = MemoryUsage
                                .from((CompositeData) value);
                        assertTrue(mu.getCommitted() != -1);
                        assertTrue(mu.getUsed() != -1);
                    }
                } else if (name.equals("CollectionUsageThreshold")) {
                    assertTrue(value instanceof Long);
                    assertTrue(((Long) (value)) > -1);
                } else if (name.equals("CollectionUsageThresholdCount")) {
                    assertTrue(value instanceof Long);
                    assertTrue(((Long) (value)) > -1);
                } else if (name.equals("MemoryManagerNames")) {
                    assertTrue(value instanceof String[]);
                    String[] names = (String[]) value;
                    assertTrue(names.length > 0);
                    for (int i = 0; i < names.length; i++) {
                        String string = names[i];
                        assertNotNull(string);
                        assertTrue(string.length() > 0);
                    }// end for
                } else if (name.equals("Name")) {
                    assertTrue(value instanceof String);
                    String str = (String) value;
                    assertNotNull(str);
                } else if (name.equals("PeakUsage")) {
                    assertTrue(value instanceof CompositeData);
                    MemoryUsage mu = MemoryUsage.from((CompositeData) value);
                    assertNotNull(mu);
                    assertTrue(mu.getCommitted() != -1);
                    assertTrue(mu.getUsed() != -1);
                } else if (name.equals("Type")) {
                    assertTrue(value instanceof String);
                    String str = (String) value;
                    assertNotNull(str);
                } else if (name.equals("Usage")) {
                    assertTrue(value instanceof CompositeData);
                    MemoryUsage mu = MemoryUsage.from((CompositeData) value);
                    assertNotNull(mu);
                    assertTrue(mu.getCommitted() != -1);
                    assertTrue(mu.getUsed() != -1);
                } else if (name.equals("UsageThreshold")) {
                    assertTrue(value instanceof Long);
                    assertTrue(((Long) (value)) > -1);
                } else if (name.equals("UsageThresholdCount")) {
                    assertTrue(value instanceof Long);
                    assertTrue(((Long) (value)) > -1);
                } else if (name.equals("CollectionUsageThresholdExceeded")) {
                    assertTrue(value instanceof Boolean);
                } else if (name.equals("CollectionUsageThresholdSupported")) {
                    assertTrue(value instanceof Boolean);
                } else if (name.equals("UsageThresholdExceeded")) {
                    assertTrue(value instanceof Boolean);
                } else if (name.equals("UsageThresholdSupported")) {
                    assertTrue(value instanceof Boolean);
                } else if (name.equals("Valid")) {
                    assertTrue(value instanceof Boolean);
                } else {
                    fail("Unexpected attribute returned : " + name + " !!");
                }
            }// end while
        }// end for
    }

    public final void testSetAttributes() {
        for (DynamicMXBeanImpl mb : mbList) {
            // Only two attributes can be set for this platform bean type
            // - UsageThreshold and CollectionUsageThreshold
            AttributeList attList = new AttributeList();
            Attribute newUT = new Attribute("UsageThreshold", new Long(
                    100 * 1024));
            attList.add(newUT);
            AttributeList setAttrs = mb.setAttributes(attList);
            assertNotNull(setAttrs);
            assertTrue(setAttrs.size() <= 1);

            if (setAttrs.size() == 1) {
                assertTrue(((Attribute) (setAttrs.get(0))).getName().equals(
                        "UsageThreshold"));
                assertTrue(((Attribute) (setAttrs.get(0))).getValue() instanceof Long);
                long recoveredValue = (Long) ((Attribute) (setAttrs.get(0)))
                        .getValue();
                assertEquals(100 * 1024, recoveredValue);
            }

            attList = new AttributeList();
            Attribute newCUT = new Attribute("CollectionUsageThreshold",
                    new Long(250 * 1024));
            attList.add(newCUT);
            setAttrs = mb.setAttributes(attList);
            assertNotNull(setAttrs);
            assertTrue(setAttrs.size() <= 1);

            if (setAttrs.size() == 1) {
                assertTrue(((Attribute) (setAttrs.get(0))).getName().equals(
                        "CollectionUsageThreshold"));
                assertTrue(((Attribute) (setAttrs.get(0))).getValue() instanceof Long);
                long recoveredValue = (Long) ((Attribute) (setAttrs.get(0)))
                        .getValue();
                assertEquals(250 * 1024, recoveredValue);
            }

            // A failure scenario - a non-existent attribute...
            AttributeList badList = new AttributeList();
            Attribute garbage = new Attribute("Bantry", new Long(2888));
            badList.add(garbage);
            setAttrs = mb.setAttributes(badList);
            assertNotNull(setAttrs);
            assertTrue(setAttrs.size() == 0);

            // Another failure scenario - a non-writable attribute...
            badList = new AttributeList();
            garbage = new Attribute("Name", new String("george"));
            badList.add(garbage);
            setAttrs = mb.setAttributes(badList);
            assertNotNull(setAttrs);
            assertTrue(setAttrs.size() == 0);

            // Yet another failure scenario - a wrongly-typed attribute...
            badList = new AttributeList();
            garbage = new Attribute("CollectionUsageThreshold", new Boolean(
                    true));
            badList.add(garbage);
            setAttrs = mb.setAttributes(badList);
            assertNotNull(setAttrs);
            assertTrue(setAttrs.size() == 0);
        }// end for
    }

    public final void testGetMBeanInfo() {
        for (DynamicMXBeanImpl mb : mbList) {
            MBeanInfo mbi = mb.getMBeanInfo();
            assertNotNull(mbi);

            // Now make sure that what we got back is what we expected.

            // Class name
            assertTrue(mbi.getClassName().equals(mb.getClass().getName()));

            // No public constructors
            MBeanConstructorInfo[] constructors = mbi.getConstructors();
            assertNotNull(constructors);
            assertTrue(constructors.length == 0);

            // One public operation - resetPeakUsage
            MBeanOperationInfo[] operations = mbi.getOperations();
            assertNotNull(operations);
            assertTrue(operations.length == 1);
            assertEquals("resetPeakUsage", operations[0].getName());

            // No notifications
            MBeanNotificationInfo[] notifications = mbi.getNotifications();
            assertNotNull(notifications);
            assertTrue(notifications.length == 0);

            // Description is just the class name (until I hear it should be
            // different)
            assertTrue(mbi.getDescription().equals(mb.getClass().getName()));

            // Fifteen attributes - only two are writable.
            MBeanAttributeInfo[] attributes = mbi.getAttributes();
            assertNotNull(attributes);
            assertEquals(15, attributes.length);
            for (int i = 0; i < attributes.length; i++) {
                MBeanAttributeInfo info = attributes[i];
                assertNotNull(info);
                validateAttributeInfo(info);
            }// end for
        }// end for
    }

    public final void testGetAttribute() throws Exception {
        for (DynamicMXBeanImpl mb : mbList) {
            // The 14 good public attributes...
            {
                // If collection usage not supported then we can get a
                // null return here.
                CompositeData cd = (CompositeData) mb
                        .getAttribute("CollectionUsage");
                if (cd != null) {
                    MemoryUsage mu = MemoryUsage.from(cd);
                    assertTrue(mu.getCommitted() != -1);
                    assertTrue(mu.getUsed() != -1);
                }
            }

            {
                if (((MemoryPoolMXBean) mb)
                        .isCollectionUsageThresholdSupported()) {
                    Long l = (Long) mb.getAttribute("CollectionUsageThreshold");
                    assertNotNull(l);
                    assertTrue(l > -1);
                } else {
                    try {
                        Long l = (Long) mb
                                .getAttribute("CollectionUsageThreshold");
                    } catch (UnsupportedOperationException ignore) {
                    }
                }// end else collection usage threshold is not supported
            }

            {
                if (((MemoryPoolMXBean) mb)
                        .isCollectionUsageThresholdSupported()) {
                    Long l = (Long) mb
                            .getAttribute("CollectionUsageThresholdCount");
                    assertNotNull(l);
                    assertTrue(l > -1);
                } else {
                    try {
                        Long l = (Long) mb
                                .getAttribute("CollectionUsageThresholdCount");
                        fail("Should have thrown UnsupportedOperationException");
                    } catch (UnsupportedOperationException ignore) {
                    }
                }// end else collection usage threshold is not supported
            }

            {
                String[] names = (String[]) mb
                        .getAttribute("MemoryManagerNames");
                assertNotNull(names);
                for (int i = 0; i < names.length; i++) {
                    String string = names[i];
                    assertNotNull(string);
                    assertTrue(string.length() > 0);
                }// end for
            }

            {
                String name = (String) mb.getAttribute("Name");
                assertNotNull(name);
                assertTrue(name.length() > 0);
            }

            {
                CompositeData cd = (CompositeData) mb.getAttribute("PeakUsage");

                if (((MemoryPoolMXBean) mb).isValid()) {
                    assertNotNull(cd);
                    MemoryUsage mu = MemoryUsage.from(cd);
                    assertTrue(mu.getCommitted() != -1);
                    assertTrue(mu.getUsed() != -1);
                } else {
                    assertNull(cd);
                }
            }

            {
                String name = (String) mb.getAttribute("Type");
                assertNotNull(name);
                assertTrue(name.length() > 0);
            }

            {
                CompositeData cd = (CompositeData) mb.getAttribute("Usage");
                if (((MemoryPoolMXBean) mb).isValid()) {
                    assertNotNull(cd);
                    MemoryUsage mu = MemoryUsage.from(cd);
                    assertTrue(mu.getCommitted() != -1);
                    assertTrue(mu.getUsed() != -1);
                } else {
                    assertNull(cd);
                }
            }

            {
                if (((MemoryPoolMXBean) mb).isUsageThresholdSupported()) {
                    Long l = (Long) mb.getAttribute("UsageThreshold");
                    assertNotNull(l);
                    assertTrue(l > -1);
                } else {
                    try {
                        Long l = (Long) mb.getAttribute("UsageThreshold");
                        fail("Should have thrown UnsupportedOperationException");
                    } catch (UnsupportedOperationException ignore) {
                    }
                }// end else usage threshold not supported
            }

            {
                if (((MemoryPoolMXBean) mb).isUsageThresholdSupported()) {
                    Long l = (Long) mb.getAttribute("UsageThresholdCount");
                    assertNotNull(l);
                    assertTrue(l > -1);
                } else {
                    try {
                        Long l = (Long) mb.getAttribute("UsageThresholdCount");
                        fail("Should have thrown UnsupportedOperationException");
                    } catch (UnsupportedOperationException ignore) {
                    }
                }// end else usage threshold not supported
            }

            {
                if (((MemoryPoolMXBean) mb)
                        .isCollectionUsageThresholdSupported()) {
                    Boolean b = (Boolean) mb
                            .getAttribute("CollectionUsageThresholdExceeded");
                    assertNotNull(b);
                } else {
                    try {
                        Boolean b = (Boolean) mb
                                .getAttribute("CollectionUsageThresholdExceeded");
                        fail("Should have thrown UnsupportedOperationException");
                    } catch (UnsupportedOperationException ignore) {
                    }
                }// end else collection usage threshold not supported
            }

            {
                Boolean b = (Boolean) mb
                        .getAttribute("CollectionUsageThresholdSupported");
                assertNotNull(b);
            }

            {
                if (((MemoryPoolMXBean) mb).isUsageThresholdSupported()) {
                    Boolean b = (Boolean) mb
                            .getAttribute("UsageThresholdExceeded");
                    assertNotNull(b);
                } else {
                    try {
                        Boolean b = (Boolean) mb
                                .getAttribute("UsageThresholdExceeded");
                        fail("Should have thrown UnsupportedOperationException");
                    } catch (UnsupportedOperationException ignore) {
                    }
                }// end else usage threshold not supported
            }

            {
                Boolean b = (Boolean) mb
                        .getAttribute("UsageThresholdSupported");
                assertNotNull(b);
            }

            {
                Boolean b = (Boolean) mb.getAttribute("Valid");
                assertNotNull(b);
            }

            // A nonexistent attribute should throw an
            // AttributeNotFoundException
            try {
                long rpm = ((Long) (mb.getAttribute("RPM")));
                fail("Should have thrown an AttributeNotFoundException.");
            } catch (AttributeNotFoundException ignore) {
            }

            // Type mismatch should result in a casting exception
            try {
                Long bad = (Long) (mb.getAttribute("Name"));
                fail("Should have thrown a ClassCastException");
            } catch (ClassCastException ignore) {
            }
        }// end for
    }

    public void testSetUsageThresholdAttribute() throws Exception {
        for (DynamicMXBeanImpl mb : mbList) {
            if (((MemoryPoolMXBean) mb).isUsageThresholdSupported()) {
                long originalUT = (Long) mb.getAttribute("UsageThreshold");
                long newUT = originalUT + 1024;
                Attribute newUTAttr = new Attribute("UsageThreshold", new Long(
                        newUT));
                mb.setAttribute(newUTAttr);

                assertEquals(new Long(newUT), (Long) mb
                        .getAttribute("UsageThreshold"));
            } else {
                try {
                    Attribute newUTAttr = new Attribute("UsageThreshold",
                            new Long(100 * 1024));
                    mb.setAttribute(newUTAttr);
                    fail("Should have thrown UnsupportedOperationException!");
                } catch (UnsupportedOperationException ignore) {
                }
            }// end else usage threshold is not supported
        }
    }

    public void testSetCollectionUsageThresholdAttribute() throws Exception {
        for (DynamicMXBeanImpl mb : mbList) {
            if (((MemoryPoolMXBean) mb).isCollectionUsageThresholdSupported()) {
                long originalCUT = (Long) mb
                        .getAttribute("CollectionUsageThreshold");
                long newCUT = originalCUT + 1024;
                Attribute newCUTAttr = new Attribute(
                        "CollectionUsageThreshold", new Long(newCUT));
                mb.setAttribute(newCUTAttr);

                assertEquals(new Long(newCUT), (Long) mb
                        .getAttribute("CollectionUsageThreshold"));
            } else {
                try {
                    Attribute newCUTAttr = new Attribute(
                            "CollectionUsageThreshold", new Long(100 * 1024));
                    mb.setAttribute(newCUTAttr);
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }// end else collection usage threshold is not supported
        }// end for
    }

    @Override
    public void testSetAttribute() throws Exception {
        for (DynamicMXBeanImpl mb : mbList) {
            // Good case - set the UsageThreshold value
            if (((Boolean) mb.getAttribute("UsageThresholdSupported"))) {
                Attribute attr = new Attribute("UsageThreshold", new Long(
                        68 * 1024));
                mb.setAttribute(attr);
            } else {
                try {
                    Long l = (Long) mb.getAttribute("UsageThreshold");
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }// end else usage threshold not supported

            // Good case - set the CollectionUsageThreshold value
            if (((Boolean) mb.getAttribute("CollectionUsageThresholdSupported"))) {
                Attribute attr = new Attribute("CollectionUsageThreshold",
                        new Long(99 * 1024));
                mb.setAttribute(attr);
            } else {
                try {
                    Long l = (Long) mb.getAttribute("UsageThreshold");
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }// end else usage threshold not supported

            // Let's try and set some non-writable attributes.
            Attribute attr = new Attribute("UsageThresholdCount", new Long(25));
            try {
                mb.setAttribute(attr);
                fail("Should have thrown an AttributeNotFoundException.");
            } catch (AttributeNotFoundException e) {
            }

            // Try and set the UsageThreshold attribute with an incorrect
            // type.
            attr = new Attribute("UsageThreshold", "rubbish");
            try {
                mb.setAttribute(attr);
                fail("Should have thrown an InvalidAttributeValueException");
            } catch (InvalidAttributeValueException ignore) {
            }
        }// end for
    }

    public final void testInvoke() throws Exception {
        for (DynamicMXBeanImpl mb : mbList) {
            // We have one operation - resetPeakUsage() which should reset
            // *peak* usage to the current memory usage.
            Object retVal = mb.invoke("resetPeakUsage", new Object[] {}, null);
            assertNull(retVal);

            // Try and invoke a non-existent method...
            try {
                retVal = mb.invoke("madeupMethod", new Object[] { "fibber" },
                        new String[] { String.class.getName() });
                fail("Should have thrown a ReflectionException");
            } catch (ReflectionException ignore) {
            }

            MemoryUsage currentMU = MemoryUsage.from((CompositeData) mb
                    .getAttribute("Usage"));
            MemoryUsage peakMU = MemoryUsage.from((CompositeData) mb
                    .getAttribute("PeakUsage"));
            assertEquals(currentMU.getCommitted(), peakMU.getCommitted());
            assertEquals(currentMU.getInit(), peakMU.getInit());
            assertEquals(currentMU.getUsed(), peakMU.getUsed());
            assertEquals(currentMU.getMax(), peakMU.getMax());
        }// end for
    }

    @Override
    protected void populateTestAttributes() {
        attribs = new Hashtable<String, AttributeData>();
        attribs.put("CollectionUsage", new AttributeData(CompositeData.class
                .getName(), true, false, false));
        attribs.put("CollectionUsageThreshold", new AttributeData(Long.TYPE
                .getName(), true, true, false));
        attribs.put("CollectionUsageThresholdCount", new AttributeData(
                Long.TYPE.getName(), true, false, false));
        attribs.put("MemoryManagerNames", new AttributeData(
                "[Ljava.lang.String;", true, false, false));
        attribs.put("Name", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("PeakUsage", new AttributeData(CompositeData.class
                .getName(), true, false, false));
        attribs.put("Type", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("Usage", new AttributeData(CompositeData.class.getName(),
                true, false, false));
        attribs.put("UsageThreshold", new AttributeData(Long.TYPE.getName(),
                true, true, false));
        attribs.put("UsageThresholdCount", new AttributeData(Long.TYPE
                .getName(), true, false, false));
        attribs.put("CollectionUsageThresholdExceeded", new AttributeData(
                Boolean.TYPE.getName(), true, false, true));
        attribs.put("CollectionUsageThresholdSupported", new AttributeData(
                Boolean.TYPE.getName(), true, false, true));
        attribs.put("UsageThresholdExceeded", new AttributeData(Boolean.TYPE
                .getName(), true, false, true));
        attribs.put("UsageThresholdSupported", new AttributeData(Boolean.TYPE
                .getName(), true, false, true));
        attribs.put("Valid", new AttributeData(Boolean.TYPE.getName(), true,
                false, true));
    }
}
