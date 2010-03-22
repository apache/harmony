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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.openmbean.TabularData;

import org.apache.harmony.lang.management.RuntimeMXBeanImpl;


public class RuntimeMXBeanImplTest extends SingleInstanceDynamicMXBeanImplTestBase {

    protected void setUp() throws Exception {
        super.setUp();
        mb = (RuntimeMXBeanImpl) ManagementFactory.getRuntimeMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttribute() throws Exception {
        // The good attributes...
        if (((Boolean) mb.getAttribute("BootClassPathSupported"))) {
            assertNotNull(mb.getAttribute("BootClassPath"));
            assertTrue(mb.getAttribute("BootClassPath") instanceof String);
        } else {
            try {
                String bcp = (String) mb.getAttribute("BootClassPath");
                fail("Should have thrown exception!");
            } catch (MBeanException ignore) {
            }
        }

        assertNotNull(mb.getAttribute("ClassPath"));
        assertTrue(mb.getAttribute("ClassPath") instanceof String);

        assertNotNull(mb.getAttribute("InputArguments"));
        assertTrue(mb.getAttribute("InputArguments") instanceof String[]);

        assertNotNull(mb.getAttribute("LibraryPath"));
        assertTrue(mb.getAttribute("LibraryPath") instanceof String);

        assertNotNull(mb.getAttribute("ManagementSpecVersion"));
        assertTrue(mb.getAttribute("ManagementSpecVersion") instanceof String);

        assertNotNull(mb.getAttribute("Name"));
        assertTrue(mb.getAttribute("Name") instanceof String);

        assertNotNull(mb.getAttribute("SpecName"));
        assertTrue(mb.getAttribute("SpecName") instanceof String);

        assertNotNull(mb.getAttribute("SpecVendor"));
        assertTrue(mb.getAttribute("SpecVendor") instanceof String);

        assertNotNull(mb.getAttribute("SpecVersion"));
        assertTrue(mb.getAttribute("SpecVersion") instanceof String);

        assertTrue(mb.getAttribute("StartTime") instanceof Long);
        assertTrue(((Long) mb.getAttribute("StartTime")) > -1);

        assertNotNull(mb.getAttribute("SystemProperties"));
        assertTrue(mb.getAttribute("SystemProperties") instanceof TabularData);
        assertTrue(((TabularData) (mb.getAttribute("SystemProperties"))).size() > 0);
        if (System.getSecurityManager() == null) {
            assertTrue(((TabularData) (mb.getAttribute("SystemProperties")))
                    .size() == System.getProperties().size());
        }// end if no security manager

        assertNotNull(mb.getAttribute("Uptime"));
        assertTrue(mb.getAttribute("Uptime") instanceof Long);
        assertTrue((Long) mb.getAttribute("Uptime") > -1);

        assertNotNull(mb.getAttribute("VmName"));
        assertTrue(mb.getAttribute("VmName") instanceof String);

        assertNotNull(mb.getAttribute("VmVendor"));
        assertTrue(mb.getAttribute("VmVendor") instanceof String);

        assertNotNull(mb.getAttribute("VmVersion"));
        assertTrue(mb.getAttribute("VmVersion") instanceof String);

        // A nonexistent attribute should throw an AttributeNotFoundException
        try {
            long rpm = ((Long) (mb.getAttribute("RPM")));
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Type mismatch should result in a casting exception
        try {
            String bad = (String) (mb.getAttribute("TotalLoadedClassCount"));
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }
    }

    public final void testSetAttribute() throws Exception {
        // Nothing is writable for this type
        Attribute attr = new Attribute("BootClassPath", "Boris");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("BootClassPath", "Pasternak");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("InputArguments", new ArrayList<String>());
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("LibraryPath", "Sterling Morrison");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("ManagementSpecVersion", "Moe Tucker");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("Name", "Julian Cope");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("SpecName", "Andy Partridge");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("SpecVendor", "Siouxie Sioux");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("SpecVersion", "Ari Up");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("StartTime", new Long(2333));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("SystemProperties", new HashMap<String, String>());
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("Uptime", new Long(1979));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("VmName", "Joe Strummer");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("VmVendor", "Paul Haig");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("VmVersion", "Jerry Dammers");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("BootClassPathSupported", new Boolean(false));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }

        // Try and set the Name attribute with an incorrect type.
        attr = new Attribute("Name", new Long(42));
        try {
            mb.setAttribute(attr);
            fail("Should have thrown an AttributeNotFoundException");
        } catch (AttributeNotFoundException ignore) {
        }
    }

    public final void testGetAttributes() {
        AttributeList attributes = mb.getAttributes(attribs.keySet().toArray(
                new String[] {}));
        assertNotNull(attributes);
        assertTrue(attributes.size() <= attribs.size());

        // Check through the returned values
        Iterator<?> it = attributes.iterator();
        while (it.hasNext()) {
            Attribute element = (Attribute) it.next();
            assertNotNull(element);
            String name = element.getName();
            Object value = element.getValue();
            if (attribs.containsKey(name)) {
                if (attribs.get(name).type.equals(String.class.getName())) {
                    assertNotNull(value);
                    assertTrue(value instanceof String);
                }// end if a String value expected
                else if (attribs.get(name).type.equals(Long.TYPE.getName())) {
                    assertTrue(((Long) (value)) > -1);
                }// end else a long expected
                else if (attribs.get(name).type.equals(Boolean.TYPE.getName())) {
                    boolean tmp = ((Boolean) value).booleanValue();
                }// end else a boolean expected
                else if (attribs.get(name).type.equals("[Ljava.lang.String;")) {
                    String[] tmp = (String[]) value;
                    assertNotNull(tmp);
                }// end else a String array expected
                else if (attribs.get(name).type.equals(TabularData.class
                        .getName())) {
                    assertNotNull(value);
                    assertTrue(value instanceof TabularData);
                    // Sanity check on the contents of the returned
                    // TabularData instance. Only one attribute of the
                    // RuntimeMXBean returns a TabularDataType -
                    // the SystemProperties.
                    TabularData td = (TabularData) value;
                    assertTrue(td.size() > 0);
                    if (System.getSecurityManager() == null) {
                        Properties props = System.getProperties();
                        assertTrue(td.size() == props.size());
                        Enumeration<?> propNames = props.propertyNames();
                        while (propNames.hasMoreElements()) {
                            String property = (String) propNames.nextElement();
                            String propVal = props.getProperty(property);
                            assertEquals(propVal, td.get(
                                    new String[] { property }).get("value"));
                        }// end while
                    }// end if no security manager
                }// end else a String array expected
            }// end if a known attribute
            else {
                fail("Unexpected attribute name returned!");
            }// end else an unknown attribute
        }// end while
    }

    public final void testSetAttributes() {
        // No writable attributes for this type - should get a failure...
        AttributeList badList = new AttributeList();
        Attribute garbage = new Attribute("Name", "City Sickness");
        Attribute trash = new Attribute("SpecVendor", "Marbles");
        badList.add(garbage);
        badList.add(trash);
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
        assertTrue(attributes.length == 16);
        for (int i = 0; i < attributes.length; i++) {
            MBeanAttributeInfo info = attributes[i];
            assertNotNull(info);
            validateAttributeInfo(info);
        }// end for
    }

    @Override
    protected void populateTestAttributes() {
        attribs = new Hashtable<String, AttributeData>();
        attribs.put("BootClassPath", new AttributeData(String.class.getName(),
                true, false, false));
        attribs.put("ClassPath", new AttributeData(String.class.getName(),
                true, false, false));
        attribs.put("InputArguments", new AttributeData("[Ljava.lang.String;",
                true, false, false));
        attribs.put("LibraryPath", new AttributeData(String.class.getName(),
                true, false, false));
        attribs.put("ManagementSpecVersion", new AttributeData(String.class
                .getName(), true, false, false));
        attribs.put("Name", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("SpecName", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("SpecVendor", new AttributeData(String.class.getName(),
                true, false, false));
        attribs.put("SpecVersion", new AttributeData(String.class.getName(),
                true, false, false));
        attribs.put("StartTime", new AttributeData(Long.TYPE.getName(), true,
                false, false));
        attribs.put("SystemProperties", new AttributeData(TabularData.class
                .getName(), true, false, false));
        attribs.put("Uptime", new AttributeData(Long.TYPE.getName(), true,
                false, false));
        attribs.put("VmName", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("VmVendor", new AttributeData(String.class.getName(), true,
                false, false));
        attribs.put("VmVersion", new AttributeData(String.class.getName(),
                true, false, false));
        attribs.put("BootClassPathSupported", new AttributeData(Boolean.TYPE
                .getName(), true, false, true));
    }
}
