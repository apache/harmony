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

package org.apache.harmony.lang.management.tests.java.util.logging;

import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;

import junit.framework.TestCase;

/**
 * 
 */
public class LoggingMXBeanTest extends TestCase {

    private LoggingMXBean mb;

    private Enumeration<String> loggerNames;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        mb = LogManager.getLoggingMXBean();
        loggerNames = LogManager.getLogManager().getLoggerNames();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link java.util.logging.LoggingMXBean#getLoggerLevel(java.lang.String)}.
     */
    public void testGetLoggerLevel() {
        // Verify we get something sensible back for the known loggers...
        while (loggerNames.hasMoreElements()) {
            String loggerName = (String) loggerNames.nextElement();
            String level = mb.getLoggerLevel(loggerName);
            assertNotNull(level);
            if (level.length() > 0) {
                Level l = Level.parse(level);
                assertNotNull(l);
            }// end if not an empty string
        }// end while
    }

    /**
     * Test method for
     * {@link java.util.logging.LoggingMXBean#getLoggerLevel(java.lang.String)}.
     */
    public void testGetNonExistentLoggerLevel() {
        assertNull(mb.getLoggerLevel("madeuploggername"));
    }

    /**
     * Test method for {@link java.util.logging.LoggingMXBean#getLoggerNames()}.
     */
    public void testGetLoggerNames() {
        // The answer according to the bean ...
        List<String> loggerNamesFromBean = mb.getLoggerNames();
        assertNotNull(loggerNamesFromBean);

        while (loggerNames.hasMoreElements()) {
            String realLoggerName = loggerNames.nextElement();
            assertTrue(loggerNamesFromBean.contains(realLoggerName));
        }
    }

    /**
     * Test method for
     * {@link java.util.logging.LoggingMXBean#getParentLoggerName(java.lang.String)}.
     */
    public void testGetParentLoggerName() {
        // Verify we get something sensible back for the known loggers...
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            Logger logger = LogManager.getLogManager().getLogger(loggerName);
            Logger parent = logger.getParent();
            if (parent != null) {
                // The logger is not the root logger
                String parentName = logger.getParent().getName();
                assertEquals(parentName, mb.getParentLoggerName(loggerName));
            } else {
                // The logger is the root logger and has no parent.
                assertTrue(mb.getParentLoggerName(loggerName).equals(""));
            }
        }// end while
    }

    /**
     * Test method for
     * {@link java.util.logging.LoggingMXBean#getParentLoggerName(java.lang.String)}.
     */
    public void testNonExistentGetParentLoggerName() {
        // Ensure we get a null back if the named Logger is fictional...
        assertNull(mb.getParentLoggerName("made up name"));
    }

    /**
     * Test method for
     * {@link java.util.logging.LoggingMXBean#setLoggerLevel(java.lang.String, java.lang.String)}.
     */
    public void testSetLoggerLevel() {
        String loggerName = null;
        while (loggerNames.hasMoreElements()) {
            loggerName = (String) loggerNames.nextElement();

            // Store the logger's current log level.
            Logger logger = LogManager.getLogManager().getLogger(loggerName);
            Level originalLevel = logger.getLevel();

            // Set the logger to have a new level.
            mb.setLoggerLevel(loggerName, Level.SEVERE.getName());

            // Verify the set worked
            assertEquals(Level.SEVERE.getName(), logger.getLevel().getName());

            // Restore to original level. Need to take into account the fact
            // that the original level may have been null (level inherited
            // from parent)
            if (originalLevel != null) {
                mb.setLoggerLevel(loggerName, originalLevel.getName());
                assertEquals(originalLevel.getName(), logger.getLevel()
                        .getName());
            } else {
                mb.setLoggerLevel(loggerName, null);
                assertNull(logger.getLevel());
            }
        }// end while

        // Verify that we get an IllegalArgumentException if we supply a
        // bogus loggerName.
        try {
            mb.setLoggerLevel("Grant W McLennan", Level.SEVERE.getName());
            fail("Should have thrown IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }

        // Verify that we get an IllegalArgumentException if we supply a
        // bogus log level value.
        try {
            mb.setLoggerLevel(loggerName, "Scott Walker");
            fail("Should have thrown IllegalArgumentException!");
        } catch (IllegalArgumentException ignore) {
        }
    }
}
