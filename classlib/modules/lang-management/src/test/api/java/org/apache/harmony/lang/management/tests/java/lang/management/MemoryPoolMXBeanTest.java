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
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;

import junit.framework.TestCase;

public class MemoryPoolMXBeanTest extends TestCase {

    private List<MemoryPoolMXBean> allBeans;

    protected void setUp() throws Exception {
        super.setUp();
        allBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mb : allBeans) {
            assertNotNull(mb);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.getCollectionUsage()'
     */
    public void testGetCollectionUsage() {
        for (MemoryPoolMXBean mb : allBeans) {
            // If this method is not supported then it returns null.
            MemoryUsage mu = mb.getCollectionUsage();
            if (mu != null) {
                assertTrue(mu.getCommitted() >= mu.getUsed());
                assertTrue(mu.getCommitted() <= mu.getMax());
                assertTrue(mu.getUsed() <= mu.getMax());
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.getCollectionUsageThreshold()'
     */
    public void testGetCollectionUsageThreshold() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isCollectionUsageThresholdSupported()) {
                assertTrue(mb.getCollectionUsageThreshold() > -1);
            } else {
                try {
                    mb.getCollectionUsageThreshold();
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.getCollectionUsageThresholdCount()'
     */
    public void testGetCollectionUsageThresholdCount() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isCollectionUsageThresholdSupported()) {
                assertTrue(mb.getCollectionUsageThresholdCount() > -1);
            } else {
                try {
                    mb.getCollectionUsageThresholdCount();
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.getMemoryManagerNames()'
     */
    public void testGetMemoryManagerNames() {
        for (MemoryPoolMXBean mb : allBeans) {
            String[] managers = mb.getMemoryManagerNames();
            assertNotNull(managers);
            for (String mgrName : managers) {
                assertNotNull(mgrName);
                assertTrue(mgrName.length() > 0);
            }
        }
    }

    /*
     * Test method for 'java.lang.management.MemoryPoolMXBean.getName()'
     */
    public void testGetName() {
        allBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mb : allBeans) {
            String name = mb.getName();
            assertNotNull(name);
            assertTrue(name.length() > 0);
        }
    }

    /*
     * Test method for 'java.lang.management.MemoryPoolMXBean.getPeakUsage()'
     */
    public void testGetPeakUsage() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isValid()) {
                MemoryUsage mu = mb.getPeakUsage();
                assertNotNull(mu);
                assertTrue(mu.getCommitted() >= mu.getUsed());
                if (mu.getMax() != -1) {
                    // If max is defined then used and committed will always
                    // be less than or equal to it
                    assertTrue(mu.getCommitted() <= mu.getMax());
                    assertTrue(mu.getUsed() <= mu.getMax());
                }
            } else {
                assertNull(mb.getPeakUsage());
            }
        }
    }

    /*
     * Test method for 'java.lang.management.MemoryPoolMXBean.getType()'
     */
    public void testGetType() {
        MemoryType[] allTypes = MemoryType.values();
        for (MemoryPoolMXBean mb : allBeans) {
            MemoryType type = mb.getType();
            assertNotNull(type);
            boolean isOfKnownType = false;
            for (MemoryType knownType : allTypes) {
                if (type.equals(knownType)) {
                    isOfKnownType = true;
                    break;
                }
            }
            assertTrue(isOfKnownType);
        }
    }

    /*
     * Test method for 'java.lang.management.MemoryPoolMXBean.getUsage()'
     */
    public void testGetUsage() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isValid()) {
                MemoryUsage mu = mb.getUsage();
                assertNotNull(mu);
                assertTrue(mu.getCommitted() >= mu.getUsed());
                if (mu.getMax() != -1) {
                    // If max is defined then used and committed will always
                    // be less than or equal to it
                    assertTrue(mu.getCommitted() <= mu.getMax());
                    assertTrue(mu.getUsed() <= mu.getMax());
                }
            } else {
                assertNull(mb.getUsage());
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.getUsageThreshold()'
     */
    public void testGetUsageThreshold() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isUsageThresholdSupported()) {
                assertTrue(mb.getUsageThreshold() > -1);
            } else {
                try {
                    mb.getUsageThreshold();
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.getUsageThresholdCount()'
     */
    public void testGetUsageThresholdCount() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isUsageThresholdSupported()) {
                assertTrue(mb.getUsageThresholdCount() > -1);
            } else {
                try {
                    mb.getUsageThresholdCount();
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.isCollectionUsageThresholdExceeded()'
     */
    public void testIsCollectionUsageThresholdExceeded() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isCollectionUsageThresholdSupported()) {
                // Not a lot to test here. Maybe it will throw an exception ?
                mb.isCollectionUsageThresholdExceeded();
            } else {
                try {
                    mb.isCollectionUsageThresholdExceeded();
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.isUsageThresholdExceeded()'
     */
    public void testIsUsageThresholdExceeded() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isUsageThresholdSupported()) {
                mb.isUsageThresholdExceeded();
            } else {
                try {
                    mb.isUsageThresholdExceeded();
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for 'java.lang.management.MemoryPoolMXBean.resetPeakUsage()'
     */
    public void testResetPeakUsage() {
        for (MemoryPoolMXBean mb : allBeans) {
            MemoryUsage current = mb.getUsage();
            mb.resetPeakUsage();
            MemoryUsage newPeak = mb.getPeakUsage();
            assertEquals(newPeak.getCommitted(), current.getCommitted());
            assertEquals(newPeak.getInit(), current.getInit());
            assertEquals(newPeak.getUsed(), current.getUsed());
            assertEquals(newPeak.getMax(), current.getMax());
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.setCollectionUsageThreshold(long)'
     */
    public void testSetCollectionUsageThreshold() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isCollectionUsageThresholdSupported()) {
                long before = mb.getCollectionUsageThreshold();
                mb.setCollectionUsageThreshold(before + (8 * 1024));
                long after = mb.getCollectionUsageThreshold();
                assertEquals((before + (8 * 1024)), after);
            } else {
                try {
                    mb.setCollectionUsageThreshold(1024);
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignored) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.setCollectionUsageThreshold(long)'
     */
    public void testSetCollectionUsageThresholdWithNegative() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isCollectionUsageThresholdSupported()) {
                try {
                    mb.setCollectionUsageThreshold(-1024);
                    fail("Should have thrown IllegalArgumentException");
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                try {
                    mb.setCollectionUsageThreshold(1024);
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignored) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.setUsageThreshold(long)'
     */
    public void testSetUsageThreshold() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isUsageThresholdSupported()) {
                long before = mb.getUsageThreshold();
                mb.setUsageThreshold(before + (8 * 1024));
                long after = mb.getUsageThreshold();
                assertEquals((before + (8 * 1024)), after);
            } else {
                try {
                    mb.setUsageThreshold(1024);
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.MemoryPoolMXBean.setUsageThreshold(long)'
     */
    public void testSetUsageThresholdWithNegative() {
        for (MemoryPoolMXBean mb : allBeans) {
            if (mb.isUsageThresholdSupported()) {
                try {
                    mb.setUsageThreshold(-1024);
                    fail("Should have thrown IllegalArgumentException");
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                try {
                    mb.setUsageThreshold(1024);
                    fail("Should have thrown UnsupportedOperationException");
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }
    }
}
