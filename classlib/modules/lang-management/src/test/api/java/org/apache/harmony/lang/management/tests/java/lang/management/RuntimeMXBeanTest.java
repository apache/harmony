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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import tests.support.Support_Excludes;

public class RuntimeMXBeanTest extends TestCase {

    private RuntimeMXBean mb;

    protected void setUp() throws Exception {
        super.setUp();
        mb = ManagementFactory.getRuntimeMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getBootClassPath()'
     */
    public void testGetBootClassPath() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        if (mb.isBootClassPathSupported()) {
            String bootclasspath = mb.getBootClassPath();
            assertNotNull(bootclasspath);
            assertTrue(bootclasspath.length() > 0);
        } else {
            try {
                mb.getBootClassPath();
                fail("Should have thrown an UnsupportedOperationException");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getClassPath()'
     */
    public void testGetClassPath() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getClassPath(), System.getProperty("java.class.path"));
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getInputArguments()'
     */
    public void testGetInputArguments() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        List<String> args = mb.getInputArguments();
        assertNotNull(args);
        for (String string : args) {
            assertNotNull(string);
            assertTrue(string.length() > 0);
        }
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getLibraryPath()'
     */
    public void testGetLibraryPath() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getLibraryPath(), System
                .getProperty("java.library.path"));
    }

    /*
     * Test method for
     * 'java.lang.management.RuntimeMXBean.getManagementSpecVersion()'
     */
    public void testGetManagementSpecVersion() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        String specVersion = mb.getManagementSpecVersion();
        assertNotNull(specVersion);
        assertTrue(specVersion.length() > 0);
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getName()'
     */
    public void testGetName() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        String name = mb.getName();
        assertNotNull(name);
        assertTrue(name.length() > 0);
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getSpecName()'
     */
    public void testGetSpecName() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getSpecName(), System
                .getProperty("java.vm.specification.name"));
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getSpecVendor()'
     */
    public void testGetSpecVendor() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getSpecVendor(), System
                .getProperty("java.vm.specification.vendor"));
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getSpecVersion()'
     */
    public void testGetSpecVersion() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getSpecVersion(), System
                .getProperty("java.vm.specification.version"));
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getStartTime()'
     */
    public void testGetStartTime() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertTrue(mb.getStartTime() > -1);
    }

    /*
     * Test method for
     * 'java.lang.management.RuntimeMXBean.getSystemProperties()'
     */
    public void testGetSystemProperties() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        Map<String, String> props = mb.getSystemProperties();
        assertNotNull(props);
        assertTrue(props.size() > 0);
        assertTrue(props.size() == System.getProperties().size());
        for (Map.Entry<String, String> entry : props.entrySet()) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
        }
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getUptime()'
     */
    public void testGetUptime() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertTrue(mb.getUptime() > -1);
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getVmName()'
     */
    public void testGetVmName() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getVmName(), System.getProperty("java.vm.name"));
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getVmVendor()'
     */
    public void testGetVmVendor() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getVmVendor(), System.getProperty("java.vm.vendor"));
    }

    /*
     * Test method for 'java.lang.management.RuntimeMXBean.getVmVersion()'
     */
    public void testGetVmVersion() {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(mb.getVmVersion(), System.getProperty("java.vm.version"));
    }
}
