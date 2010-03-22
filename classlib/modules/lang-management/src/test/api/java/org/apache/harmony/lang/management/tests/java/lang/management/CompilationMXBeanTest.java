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

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

import junit.framework.TestCase;

public class CompilationMXBeanTest extends TestCase {

    CompilationMXBean mb;

    protected void setUp() throws Exception {
        super.setUp();
        mb = ManagementFactory.getCompilationMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.lang.management.CompilationMXBean.getName()'
     */
    public void testGetName() throws Exception {
        assertNotNull(mb.getName());
    }

    /*
     * Test method for
     * 'java.lang.management.CompilationMXBean.getTotalCompilationTime()'
     */
    public void testGetTotalCompilationTime() throws Exception {
        if (mb.isCompilationTimeMonitoringSupported()) {
            assertTrue(mb.getTotalCompilationTime() > -1);
        } else {
            try {
                mb.getTotalCompilationTime();
                fail("Should have thrown unsupported operation exception!");
            } catch (UnsupportedOperationException e) {
                // ignore
            }
        }
    }
}
