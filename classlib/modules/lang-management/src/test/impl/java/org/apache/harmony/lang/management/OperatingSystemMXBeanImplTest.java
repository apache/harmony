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
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.NotificationEmitter;

import org.apache.harmony.lang.management.OperatingSystemMXBeanImpl;


public class OperatingSystemMXBeanImplTest extends
        SingleInstanceDynamicMXBeanImplTestBase {

    private OperatingSystemMXBeanImpl notifierBean;

    protected void setUp() throws Exception {
        super.setUp();
        mb = (OperatingSystemMXBeanImpl) ManagementFactory
                .getOperatingSystemMXBean();
        notifierBean = (OperatingSystemMXBeanImpl) mb;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttribute() throws Exception {
        // The good attributes...
        assertTrue(mb.getAttribute("Arch") != null);
        assertTrue(mb.getAttribute("Arch") instanceof String);
        assertTrue(((Integer) (mb.getAttribute("AvailableProcessors"))) > -1);
        assertTrue(mb.getAttribute("Name") != null);
        assertTrue(mb.getAttribute("Name") instanceof String);
        assertTrue(mb.getAttribute("Version") != null);
        assertTrue(mb.getAttribute("Version") instanceof String);

        // A nonexistent attribute should throw an AttributeNotFoundException
        try {
            mb.getAttribute("RPM");
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Type mismatch should result in a casting exception
        try {
            String bad = (String) (mb.getAttribute("AvailableProcessors"));
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException ignore) {
        }
    }

    public final void testSetAttribute() throws Exception {
        // Nothing is writable for this type
        Attribute attr = new Attribute("Name", "Boris");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("Arch", "ie Bunker");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("Version", "27 and a half");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("AvailableProcessors", new Integer(2));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Try and set the Name attribute with an incorrect type.
        attr = new Attribute("Name", new Long(42));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }
    }

    public final void testGetAttributes() {
        AttributeList attributes = mb.getAttributes(attribs.keySet().toArray(
                new String[] {}));
        assertNotNull(attributes);
        assertEquals(attribs.size(), attributes.size());

        // Check through the returned values
        Iterator<?> it = attributes.iterator();
        while (it.hasNext()) {
            Attribute element = (Attribute) it.next();
            assertNotNull(element);
            String name = element.getName();
            Object value = element.getValue();
            if (name.equals("Arch")) {
                assertTrue(value instanceof String);
                assertEquals(System.getProperty("os.arch"), (String) value);
            } else if (name.equals("AvailableProcessors")) {
                assertTrue(((Integer) (value)) > -1);
            } else if (name.equals("Name")) {
                assertTrue(value instanceof String);
                assertEquals(System.getProperty("os.name"), (String) value);
            } else if (name.equals("Version")) {
                assertTrue(value instanceof String);
                assertEquals(System.getProperty("os.version"), (String) value);
            } else if (name.equals("TotalPhysicalMemory")) {
                assertTrue(value instanceof Long);
                assertTrue(((Long) (value)) > -1);
            } else if (name.equals("ProcessingCapacity")) {
                assertTrue(value instanceof Integer);
                assertTrue(((Integer) (value)) > -1);
            } else {
                fail("Unexpected attribute name returned!");
            }
        }// end while
    }

    public final void testSetAttributes() {
        // No writable attributes for this type - should get a failure...
        AttributeList badList = new AttributeList();
        Attribute garbage = new Attribute("Name", "Waiting for the moon");
        badList.add(garbage);
        AttributeList setAttrs = mb.setAttributes(badList);
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

        // No public operations
        MBeanOperationInfo[] operations = mbi.getOperations();
        assertNotNull(operations);
        assertTrue(operations.length == 0);

        // No public notifications
        MBeanNotificationInfo[] notifications = mbi.getNotifications();
        assertNotNull(notifications);
        assertTrue(notifications.length == 0);

        // Description is just the class name (until I hear it should be
        // different)
        assertTrue(mbi.getDescription().equals(mb.getClass().getName()));

        // 4 standard attributes
        MBeanAttributeInfo[] attributes = mbi.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.length == 4);
        for (int i = 0; i < attributes.length; i++) {
            MBeanAttributeInfo info = attributes[i];
            assertNotNull(info);
            validateAttributeInfo(info);
        }// end for
    }

    @Override
    protected void populateTestAttributes() {
        attribs = new Hashtable<String, AttributeData>();
        attribs.put("Arch", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("AvailableProcessors", new AttributeData(Integer.TYPE
                .getName(), true, false, false));
        attribs.put("Name", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("Version", new AttributeData(String.class.getName(), true,
                false, false));
    }
}
