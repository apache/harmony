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

package org.apache.harmony.lang.management.tests.java.lang.management;

import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;

import junit.framework.TestCase;

public class LoggingMXBeanTest extends TestCase {

    LoggingMXBean lmb;

    Enumeration<String> loggerNamesFromMgr;

    protected void setUp() throws Exception {
        super.setUp();
        lmb = LogManager.getLoggingMXBean();

        // Logger names from the log manager...
        loggerNamesFromMgr = LogManager.getLogManager().getLoggerNames();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.util.logging.LoggingMXBean.getLoggerLevel(String)'
     */
    public void testGetLoggerLevel() throws Exception {
        // Verify we get something sensible back for the known loggers...
        while (loggerNamesFromMgr.hasMoreElements()) {
            String logName = (String) loggerNamesFromMgr.nextElement();
            String level = lmb.getLoggerLevel(logName);
            assertNotNull(level);
            if (level.length() > 0) {
                Level.parse(level);
            }// end if not an empty string
        }// end while

        // Ensure we get a null back if the named Logger is fictional...
        assertNull(lmb.getLoggerLevel("made up name"));
    }

    /*
     * Test method for 'java.util.logging.LoggingMXBean.getLoggerNames()'
     */
    public void testGetLoggerNames() throws Exception {
        // Logger names from the bean...
        List<String> namesFromBean = lmb.getLoggerNames();
        assertNotNull(namesFromBean);
        while (loggerNamesFromMgr.hasMoreElements()) {
            String mgrName = loggerNamesFromMgr.nextElement();
            assertTrue(namesFromBean.contains(mgrName));
        }// end while
    }

    /*
     * Test method for
     * 'java.util.logging.LoggingMXBean.getParentLoggerName(String)'
     */
    public void testGetParentLoggerName() throws Exception {
        // Verify we get something sensible back for the known loggers...
        while (loggerNamesFromMgr.hasMoreElements()) {
            String logName = (String) loggerNamesFromMgr.nextElement();
            Logger logger = LogManager.getLogManager().getLogger(logName);
            Logger parent = logger.getParent();
            if (parent != null) {
                // The logger is not the root logger
                String parentName = logger.getParent().getName();
                assertEquals(parentName, lmb.getParentLoggerName(logName));
            } else {
                // The logger is the root logger and has no parent.
                assertTrue(lmb.getParentLoggerName(logName).equals(""));
            }
        }// end while

        // Ensure we get a null back if the named Logger is fictional...
        assertNull(lmb.getParentLoggerName("made up name"));
    }

    /*
     * Test method for 'java.util.logging.LoggingMXBean.setLoggerLevel(String,
     * String)'
     */
    public void testSetLoggerLevel() throws Exception {
        String logName = null;
        while (loggerNamesFromMgr.hasMoreElements()) {
            logName = (String) loggerNamesFromMgr.nextElement();

            // Store the logger's current log level.
            Logger logger = LogManager.getLogManager().getLogger(logName);
            if (levelNotSevere(logger.getLevel())) {
                // Set the logger to have a new level.
                lmb.setLoggerLevel(logName, Level.SEVERE.getName());

                // Verify the set worked
                assertEquals(Level.SEVERE.getName(), logger.getLevel()
                        .getName());
            }
        }// end while

        // Verify that we get an IllegalArgumentException if we supply a
        // bogus loggerName.
        try {
            lmb.setLoggerLevel("Ella Fitzgerald", Level.SEVERE.getName());
            fail("Should have thrown IllegalArgumentException for a bogus log name!");
        } catch (IllegalArgumentException e) {
            // ignored
        }

        // Verify that we get an IllegalArgumentException if we supply a
        // bogus log level value.
        try {
            lmb.setLoggerLevel(logName, "Scott Walker");
            fail("Should have thrown IllegalArgumentException for a bogus log level!");
        } catch (IllegalArgumentException e) {
            // ignored
        }
    }

    private boolean levelNotSevere(Level level) {
        if (level == null) {
            return true;
        }
        if (level.equals(Level.SEVERE)) {
            return true;
        }
        return false;
    }

}
