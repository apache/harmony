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
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

import org.apache.harmony.lang.management.ClassLoadingMXBeanImpl;


public class ClassLoadingMXBeanImplTest extends
        SingleInstanceDynamicMXBeanImplTestBase {

    protected void setUp() throws Exception {
        super.setUp();
        mb = (ClassLoadingMXBeanImpl) ManagementFactory.getClassLoadingMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttribute() throws Exception {
        // The good attributes...
        assertTrue(((Integer) (mb.getAttribute("LoadedClassCount"))) > -1);
        assertTrue(((Long) (mb.getAttribute("TotalLoadedClassCount"))) > -1);
        assertTrue(((Long) (mb.getAttribute("UnloadedClassCount"))) > -1);

        // This could be true or false - just so long as we don't get an
        // exception raised...
        boolean verboseVal = ((Boolean) (mb.getAttribute("Verbose")));

        // A nonexistent attribute should throw an AttributeNotFoundException
        try {
            long rpm = ((Long) (mb.getAttribute("RPM")));
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Type mismatch should result in a casting exception
        try {
            String bad = (String) (mb.getAttribute("TotalLoadedClassCount"));
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException ignore) {
        }
    }

    public final void testSetAttribute() throws Exception {
        Attribute attr = null;
        // The one writable attribute of ClassLoadingMXBeanImpl...
        Boolean before = (Boolean) mb.getAttribute("Verbose");
        boolean newVal = !before;
        attr = new Attribute("Verbose", new Boolean(newVal));
        mb.setAttribute(attr);
        Boolean after = (Boolean) mb.getAttribute("Verbose");
        assert (newVal == after);

        // Let's try and set some non-writable attributes.
        attr = new Attribute("LoadedClassCount", new Integer(25));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("TotalLoadedClassCount", new Long(3300));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("UnloadedClassCount", new Long(38));
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

    public final void testGetAttributes() throws Exception {
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
            if (name.equals("Verbose")) {
                // This could be true or false - just so long as we don't
                // get an exception raised...
                boolean verboseVal = ((Boolean) value).booleanValue();
            } else if (name.equals("LoadedClassCount")) {
                assertTrue(((Integer) (value)) > -1);
            } else if (name.equals("TotalLoadedClassCount")) {
                assertTrue(((Long) (mb.getAttribute("TotalLoadedClassCount"))) > -1);
            } else if (name.equals("UnloadedClassCount")) {
                assertTrue(((Long) (value)) > -1);
            } else {
                fail("Unexpected attribute name returned!");
            }
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
        Attribute garbage = new Attribute("Bantry", new Long(2888));
        badList.add(garbage);
        setAttrs = mb.setAttributes(badList);
        assertNotNull(setAttrs);
        assertTrue(setAttrs.size() == 0);

        // Another failure scenario - a non-writable attribute...
        badList = new AttributeList();
        garbage = new Attribute("TotalLoadedClassCount", new Long(2888));
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

        // No public operations
        MBeanOperationInfo[] operations = mbi.getOperations();
        assertNotNull(operations);
        assertTrue(operations.length == 0);

        // No notifications
        MBeanNotificationInfo[] notifications = mbi.getNotifications();
        assertNotNull(notifications);
        assertTrue(notifications.length == 0);

        // Description is just the class name (until I hear it should be
        // different)
        assertTrue(mbi.getDescription().equals(mb.getClass().getName()));

        // Four attributes - only Verbose is writable.
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
        attribs.put("Verbose", new AttributeData(Boolean.TYPE.getName(), true,
                true, true));
        attribs.put("LoadedClassCount", new AttributeData(Integer.TYPE
                .getName(), true, false, false));
        attribs.put("TotalLoadedClassCount", new AttributeData(Long.TYPE
                .getName(), true, false, false));
        attribs.put("UnloadedClassCount", new AttributeData(
                Long.TYPE.getName(), true, false, false));
    }
}
