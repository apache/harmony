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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.apache.harmony.lang.management.LoggingMXBeanImpl;
import org.apache.harmony.lang.management.ManagementUtils;


public class LoggingMXBeanImplTest extends SingleInstanceDynamicMXBeanImplTestBase {

    private Enumeration<String> loggerNames;

    protected void setUp() throws Exception {
        super.setUp();
        mb = (LoggingMXBeanImpl) LogManager.getLoggingMXBean();
        loggerNames = LogManager.getLogManager().getLoggerNames();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testIsSingleton() {
        // Verify we always get the same instance
        LoggingMXBean bean = ManagementUtils.getLoggingBean();
        assertSame(mb, bean);
    }

    // -----------------------------------------------------------------
    // DynamicMBean behaviour tests follow ....
    // -----------------------------------------------------------------

    public final void testGetAttribute() throws Exception {
        // Only one readable attribute for this type.
        assertNotNull(mb.getAttribute("LoggerNames"));
        assertTrue(mb.getAttribute("LoggerNames") instanceof String[]);

        // A nonexistent attribute should throw an AttributeNotFoundException
        try {
            long rpm = ((Long) (mb.getAttribute("RPM")));
            fail("Should have thrown an AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        // Type mismatch should result in a casting exception
        try {
            String bad = (String) (mb.getAttribute("LoggerNames"));
            fail("Should have thrown a ClassCastException");
        } catch (ClassCastException ignore) {
        }
    }

    public final void testSetAttribute() throws Exception {
        // Nothing is writable for this type
        Attribute attr = new Attribute("LoggerLevel", "Boris");
        try {
            mb.setAttribute(attr);
            fail("Should have thrown AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }

        attr = new Attribute("LoggerNames",
                new String[] { "Strummer", "Jones" });
        try {
            mb.setAttribute(attr);
            fail("Should have thrown AttributeNotFoundException.");
        } catch (AttributeNotFoundException ignore) {
        }
    }

    public final void testGetAttributes() {
        AttributeList attributes = mb.getAttributes(attribs.keySet().toArray(
                new String[] {}));
        assertNotNull(attributes);
        assertTrue(attributes.size() == 1);

        // Check the returned value
        Attribute element = (Attribute) attributes.get(0);
        assertNotNull(element);
        assertEquals("LoggerNames", element.getName());
        Object value = element.getValue();
        assertNotNull(value);
        assertTrue(value instanceof String[]);
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

    public final void testInvoke() throws Exception {
        // 3 different operations that can be invoked on this kind of bean.
        String logName = null;
        while (loggerNames.hasMoreElements()) {
            logName = (String) loggerNames.nextElement();
            // Store the logger's current log level.
            Logger logger = LogManager.getLogManager().getLogger(logName);
            Level originalLevel = logger.getLevel();
            Logger parentLogger = logger.getParent();

            // Operation #1 --- getLoggerLevel(String)
            Object retVal = mb.invoke("getLoggerLevel",
                    new Object[] { logName }, new String[] { String.class
                            .getName() });
            assertNotNull(retVal);
            assertTrue(retVal instanceof String);
            if (originalLevel != null) {
                assertEquals(originalLevel.getName(), (String) retVal);
            } else {
                assertEquals("", (String) retVal);
            }

            // Operation #2 --- getParentLoggerName(String)
            retVal = mb.invoke("getParentLoggerName", new Object[] { logName },
                    new String[] { String.class.getName() });
            assertNotNull(retVal);
            assertTrue(retVal instanceof String);
            if (parentLogger != null) {
                assertEquals(parentLogger.getName(), (String) retVal);
            } else {
                assertEquals("", (String) retVal);
            }

            // Call getParentLoggerName(String) again with a bad argument type.
            try {
                retVal = mb.invoke("getParentLoggerName",
                        new Object[] { new Long(311) },
                        new String[] { Long.TYPE.getName() });
                fail("Should have thrown ReflectionException !!");
            } catch (ReflectionException ignore) {
            }

            // Operation #3 --- setLoggerLevel(String, String)
            retVal = mb.invoke("setLoggerLevel", new Object[] { logName,
                    Level.SEVERE.getName() }, new String[] {
                    String.class.getName(), String.class.getName() });
            // Verify the set worked
            assertEquals(Level.SEVERE.getName(), logger.getLevel().getName());
        }// end while

        // Try invoking a bogus operation ...
        try {
            Object retVal = mb.invoke("GetUpStandUp", new Object[] {
                    new Long(7446), new Long(54) }, new String[] {
                    "java.lang.Long", "java.lang.Long" });
            fail("Should have thrown ReflectionException.");
        } catch (ReflectionException ignore) {
        }
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

        // Three public operations
        MBeanOperationInfo[] operations = mbi.getOperations();
        assertNotNull(operations);
        assertTrue(operations.length == 3);

        // No notifications
        MBeanNotificationInfo[] notifications = mbi.getNotifications();
        assertNotNull(notifications);
        assertTrue(notifications.length == 0);

        // Description is just the class name (until I hear it should be
        // different)
        assertTrue(mbi.getDescription().equals(mb.getClass().getName()));

        // One attribute - not writable.
        MBeanAttributeInfo[] attributes = mbi.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.length == 1);
        MBeanAttributeInfo attribute = attributes[0];
        assertTrue(attribute.isReadable());
        assertFalse(attribute.isWritable());
        assertEquals("LoggerNames", attribute.getName());
        assertEquals(String[].class.getName(), attribute.getType());
    }

    @Override
    protected void populateTestAttributes() {
        attribs = new Hashtable<String, AttributeData>();
        attribs.put("LoggerNames", new AttributeData("[Ljava.lang.String;",
                true, false, false));
    }
}
