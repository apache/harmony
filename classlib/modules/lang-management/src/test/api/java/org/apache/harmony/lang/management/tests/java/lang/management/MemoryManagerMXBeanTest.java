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
import java.lang.management.MemoryManagerMXBean;
import java.util.List;

import junit.framework.TestCase;

public class MemoryManagerMXBeanTest extends TestCase {

    private List<MemoryManagerMXBean> allBeans;

    protected void setUp() throws Exception {
        super.setUp();
        allBeans = ManagementFactory.getMemoryManagerMXBeans();
        assertNotNull(allBeans);
        for (MemoryManagerMXBean mb : allBeans) {
            assertNotNull(mb);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryManagerMXBean.getMemoryPoolNames()'
     */
    public void testGetMemoryPoolNames() {
        for (MemoryManagerMXBean mb : allBeans) {
            String[] managedPools = mb.getMemoryPoolNames();
            assertNotNull(managedPools);
            for (String poolName : managedPools) {
                assertNotNull(poolName);
                assertTrue(poolName.length() > 0);
            }// end for all managed pools
        }// end for all garbage collector beans
    }

    /*
     * Test method for 'java.lang.management.MemoryManagerMXBean.getName()'
     */
    public void testGetName() {
        for (MemoryManagerMXBean mb : allBeans) {
            String name = mb.getName();
            assertNotNull(name);
            assertTrue(name.length() > 0);
        }
    }

}
