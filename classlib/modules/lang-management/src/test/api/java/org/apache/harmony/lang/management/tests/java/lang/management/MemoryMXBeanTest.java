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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import junit.framework.TestCase;

public class MemoryMXBeanTest extends TestCase {

    private MemoryMXBean mb;
    
    protected void setUp() throws Exception {
        super.setUp();
        mb = ManagementFactory.getMemoryMXBean();
        assertNotNull(mb);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.lang.management.MemoryMXBean.getHeapMemoryUsage()'
     */
    public void testGetHeapMemoryUsage() {
        MemoryUsage mu = mb.getHeapMemoryUsage();
        assertNotNull(mu);
        assertTrue(mu.getCommitted() >= mu.getUsed());
        assertTrue(mu.getCommitted() <= mu.getMax());
        assertTrue(mu.getUsed() <= mu.getMax());
    }

    /*
     * Test method for 'java.lang.management.MemoryMXBean.getNonHeapMemoryUsage()'
     */
    public void testGetNonHeapMemoryUsage() {
        MemoryUsage mu = mb.getNonHeapMemoryUsage();
        assertNotNull(mu);
        assertTrue(mu.getCommitted() >= mu.getUsed());
        if (mu.getMax() != -1) {
            // If max is defined then used and committed will always
            // be less than or equal to it
            assertTrue(mu.getCommitted() <= mu.getMax());
            assertTrue(mu.getUsed() <= mu.getMax());
        }
    }

    /*
     * Test method for 'java.lang.management.MemoryMXBean.getObjectPendingFinalizationCount()'
     */
    public void testGetObjectPendingFinalizationCount() {
        assertTrue(mb.getObjectPendingFinalizationCount() > -1);
    }

    /*
     * Test method for 'java.lang.management.MemoryMXBean.setVerbose(boolean)'
     */
    public void testSetVerbose() {
        boolean initialVal = mb.isVerbose();
        mb.setVerbose(!initialVal);
        assertTrue(mb.isVerbose() != initialVal);
        mb.setVerbose(initialVal);
        assertTrue(mb.isVerbose() == initialVal);
    }
}
