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
import java.lang.management.OperatingSystemMXBean;

import junit.framework.TestCase;

public class OperatingSystemMXBeanTest extends TestCase {

    private OperatingSystemMXBean mb;

    protected void setUp() throws Exception {
        super.setUp();
        mb = ManagementFactory.getOperatingSystemMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.lang.management.OperatingSystemMXBean.getArch()'
     */
    public void testGetArch() {
        assertEquals(mb.getArch(), System.getProperty("os.arch"));
    }

    /*
     * Test method for
     * 'java.lang.management.OperatingSystemMXBean.getAvailableProcessors()'
     */
    public void testGetAvailableProcessors() {
        assertEquals(mb.getAvailableProcessors(), Runtime.getRuntime()
                .availableProcessors());
    }

    /*
     * Test method for 'java.lang.management.OperatingSystemMXBean.getName()'
     */
    public void testGetName() {
        assertEquals(mb.getName(), System.getProperty("os.name"));
    }

    /*
     * Test method for 'java.lang.management.OperatingSystemMXBean.getVersion()'
     */
    public void testGetVersion() {
        assertEquals(mb.getVersion(), System.getProperty("os.version"));
    }
}
