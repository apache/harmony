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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

public class ManagementFactoryTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getClassLoadingMXBean()'
     */
    public void testGetClassLoadingMXBean() {
        ClassLoadingMXBean mb = ManagementFactory.getClassLoadingMXBean();
        assertNotNull(mb);

        // Verify that there is only instance of the class loading bean
        ClassLoadingMXBean mb2 = ManagementFactory.getClassLoadingMXBean();
        assertNotNull(mb2);
        assertSame(mb, mb2);
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getCompilationMXBean()'
     */
    public void testGetCompilationMXBean() {
        CompilationMXBean mb = ManagementFactory.getCompilationMXBean();
        assertNotNull(mb);

        // Verify that there is only instance of this bean
        CompilationMXBean mb2 = ManagementFactory.getCompilationMXBean();
        assertNotNull(mb2);
        assertSame(mb, mb2);
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()'
     */
    public void testGetGarbageCollectorMXBeans() {
        List<GarbageCollectorMXBean> allBeans = ManagementFactory
                .getGarbageCollectorMXBeans();
        assertNotNull(allBeans);
        assertTrue(allBeans.size() > 0);
        for (GarbageCollectorMXBean mb : allBeans) {
            assertNotNull(mb);
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getMemoryManagerMXBeans()'
     */
    public void testGetMemoryManagerMXBeans() {
        List<MemoryManagerMXBean> allBeans = ManagementFactory
                .getMemoryManagerMXBeans();
        assertNotNull(allBeans);
        assertTrue(allBeans.size() > 0);
        for (MemoryManagerMXBean mb : allBeans) {
            assertNotNull(mb);
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getMemoryMXBean()'
     */
    public void testGetMemoryMXBean() {
        MemoryMXBean mb = ManagementFactory.getMemoryMXBean();
        assertNotNull(mb);

        // Verify that there is only instance of this bean
        MemoryMXBean mb2 = ManagementFactory.getMemoryMXBean();
        assertNotNull(mb2);
        assertSame(mb, mb2);
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getMemoryPoolMXBeans()'
     */
    public void testGetMemoryPoolMXBeans() {
        List<MemoryPoolMXBean> allBeans = ManagementFactory
                .getMemoryPoolMXBeans();
        assertNotNull(allBeans);
        assertTrue(allBeans.size() > 0);
        for (MemoryPoolMXBean mb : allBeans) {
            assertNotNull(mb);
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getOperatingSystemMXBean()'
     */
    public void testGetOperatingSystemMXBean() {
        OperatingSystemMXBean mb = ManagementFactory.getOperatingSystemMXBean();
        assertNotNull(mb);

        // Verify that there is only instance of this bean
        OperatingSystemMXBean mb2 = ManagementFactory
                .getOperatingSystemMXBean();
        assertNotNull(mb2);
        assertSame(mb, mb2);
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getPlatformMBeanServer()'
     */
    public void testGetPlatformMBeanServer() {
        MBeanServer pServer = ManagementFactory.getPlatformMBeanServer();
        assertNotNull(pServer);

        // Verify that subsequent calls always return the same server object.
        MBeanServer pServer2 = ManagementFactory.getPlatformMBeanServer();
        assertNotNull(pServer2);
        assertSame(pServer, pServer2);

        // Verify the default domain is "DefaultDomain"
        assertEquals("DefaultDomain", pServer.getDefaultDomain());
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getRuntimeMXBean()'
     */
    public void testGetRuntimeMXBean() {
        RuntimeMXBean mb = ManagementFactory.getRuntimeMXBean();
        assertNotNull(mb);

        // Verify that there is only instance of this bean
        RuntimeMXBean mb2 = ManagementFactory.getRuntimeMXBean();
        assertNotNull(mb2);
        assertSame(mb, mb2);
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.getThreadMXBean()'
     */
    public void testGetThreadMXBean() {
        ThreadMXBean mb = ManagementFactory.getThreadMXBean();
        assertNotNull(mb);

        // Verify that there is only instance of this bean
        ThreadMXBean mb2 = ManagementFactory.getThreadMXBean();
        assertNotNull(mb2);
        assertSame(mb, mb2);
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy() throws Exception {
        // Test that the general case fails as expected..

        // MXBean name deliberately in wrong format
        try {
            ManagementFactory
                    .newPlatformMXBeanProxy(ManagementFactory
                            .getPlatformMBeanServer(),
                            "java,lang:type=ClassLoading",
                            ClassLoadingMXBean.class);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // MXBean name in correct format but deliberately bogus
        try {
            ManagementFactory
                    .newPlatformMXBeanProxy(ManagementFactory
                            .getPlatformMBeanServer(),
                            "java.lang:type=ClassStroking",
                            ClassLoadingMXBean.class);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Named MXBean does not implement the supplied MXBean interface
        try {
            ManagementFactory
                    .newPlatformMXBeanProxy(ManagementFactory
                            .getPlatformMBeanServer(), "java.lang:type=Memory",
                            ClassLoadingMXBean.class);
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Named MXBean does not implement the supplied MXBean interface
        List<MemoryPoolMXBean> allMPoolBeans = ManagementFactory
                .getMemoryPoolMXBeans();
        for (MemoryPoolMXBean bean : allMPoolBeans) {
            try {
                ManagementFactory
                        .newPlatformMXBeanProxy(ManagementFactory
                                .getPlatformMBeanServer(), bean.getName(),
                                ClassLoadingMXBean.class);
                fail("should have thrown IllegalArgumentException");
            } catch (IllegalArgumentException ignore) {
            }
        }// end for

        // Named MXBean does not implement the supplied MXBean interface
        List<GarbageCollectorMXBean> allGCBeans = ManagementFactory
                .getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : allGCBeans) {
            try {
                ManagementFactory.newPlatformMXBeanProxy(
                        ManagementFactory.getPlatformMBeanServer(), bean
                                .getName(), ThreadMXBean.class);
                fail("should have thrown IllegalArgumentException");
            } catch (IllegalArgumentException ignore) {
            }
        }// end for

        // Named MXBean does not implement the supplied MXBean interface
        List<MemoryManagerMXBean> allMMBeans = ManagementFactory
                .getMemoryManagerMXBeans();
        for (MemoryManagerMXBean bean : allMMBeans) {
            try {
                ManagementFactory.newPlatformMXBeanProxy(
                        ManagementFactory.getPlatformMBeanServer(), bean
                                .getName(), ThreadMXBean.class);
                fail("should have thrown IllegalArgumentException");
            } catch (IllegalArgumentException ignore) {
            }
        }// end for 
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_ClassLoadingMXBean()
            throws Exception {
        ClassLoadingMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "java.lang:type=ClassLoading", ClassLoadingMXBean.class);
        assertNotNull(proxy);
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory
                .getClassLoadingMXBean();
        assertEquals(proxy.getLoadedClassCount(), classLoadingMXBean
                .getLoadedClassCount());
        assertEquals(proxy.getTotalLoadedClassCount(), classLoadingMXBean
                .getTotalLoadedClassCount());
        assertEquals(proxy.getUnloadedClassCount(), classLoadingMXBean
                .getUnloadedClassCount());
        assertEquals(proxy.isVerbose(), classLoadingMXBean.isVerbose());

        boolean initialVal = proxy.isVerbose();
        proxy.setVerbose(!initialVal);
        assertTrue(proxy.isVerbose() != initialVal);
        proxy.setVerbose(initialVal);
        assertEquals(initialVal, proxy.isVerbose());
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_CompilationMXBean() throws Exception {
        CompilationMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "java.lang:type=Compilation", CompilationMXBean.class);
        assertNotNull(proxy);
        CompilationMXBean mb = ManagementFactory.getCompilationMXBean();
        assertEquals(mb.getName(), proxy.getName());
        assertEquals(mb.isCompilationTimeMonitoringSupported(), proxy
                .isCompilationTimeMonitoringSupported());
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_GarbageCollectorMXBean()
            throws Exception {
        List<GarbageCollectorMXBean> allBeans = ManagementFactory
                .getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean mb : allBeans) {
            GarbageCollectorMXBean proxy = ManagementFactory
                    .newPlatformMXBeanProxy(ManagementFactory
                            .getPlatformMBeanServer(),
                            "java.lang:type=GarbageCollector,name="
                                    + mb.getName(),
                            GarbageCollectorMXBean.class);
            assertEquals(mb.getName(), proxy.getName());
            assertEquals(mb.isValid(), proxy.isValid());
            assertEquals(mb.getCollectionCount(), proxy.getCollectionCount());
            String[] poolNames1 = mb.getMemoryPoolNames();
            String[] poolNames2 = proxy.getMemoryPoolNames();
            assertEquals(poolNames1.length, poolNames2.length);
            for (int i = 0; i < poolNames1.length; i++) {
                assertEquals(poolNames1[i], poolNames2[i]);
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_MemoryManagerMXBean()
            throws Exception {
        List<MemoryManagerMXBean> allBeans = ManagementFactory
                .getMemoryManagerMXBeans();
        for (MemoryManagerMXBean mb : allBeans) {
            // Not every memory manager will be registered with the server
            // connection. Only test those that are...
            if (ManagementFactory.getPlatformMBeanServer().isRegistered(
                    new ObjectName("java.lang:type=MemoryManager,name="
                            + mb.getName()))) {
                MemoryManagerMXBean proxy = ManagementFactory
                        .newPlatformMXBeanProxy(ManagementFactory
                                .getPlatformMBeanServer(),
                                "java.lang:type=MemoryManager,name="
                                        + mb.getName(),
                                MemoryManagerMXBean.class);
                assertEquals(mb.getName(), proxy.getName());
                assertEquals(mb.isValid(), proxy.isValid());
                String[] poolNames1 = mb.getMemoryPoolNames();
                String[] poolNames2 = proxy.getMemoryPoolNames();
                assertEquals(poolNames1.length, poolNames2.length);
                for (int i = 0; i < poolNames1.length; i++) {
                    assertEquals(poolNames1[i], poolNames2[i]);
                }
            }// end if memory manager is registered with server connection
        }// end for all known memory manager beans
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_MemoryMXBean() throws Exception {
        MemoryMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "java.lang:type=Memory", MemoryMXBean.class);
        assertNotNull(proxy);
        MemoryMXBean mb = ManagementFactory.getMemoryMXBean();
        // RI's MemoryUsage does not appear to override equals() so we have to
        // go the long way round to check that the answers match...
        MemoryUsage mu1 = mb.getHeapMemoryUsage();
        MemoryUsage mu2 = proxy.getHeapMemoryUsage();
        assertEquals(mu1.getInit(), mu2.getInit());
        assertEquals(mu1.getMax(), mu2.getMax());

        mu1 = mb.getNonHeapMemoryUsage();
        mu2 = proxy.getNonHeapMemoryUsage();
        assertEquals(mu1.getInit(), mu2.getInit());
        assertEquals(mu1.getMax(), mu2.getMax());

        assertEquals(mb.isVerbose(), proxy.isVerbose());
        // changes made to proxy should be seen in the "real bean" and
        // vice versa
        boolean initialValue = proxy.isVerbose();
        mb.setVerbose(!initialValue);
        assertEquals(!initialValue, proxy.isVerbose());
        proxy.setVerbose(initialValue);
        assertEquals(initialValue, mb.isVerbose());
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_MemoryPoolMXBean() throws Exception {
        List<MemoryPoolMXBean> allBeans = ManagementFactory
                .getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mb : allBeans) {
            MemoryPoolMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                    ManagementFactory.getPlatformMBeanServer(),
                    "java.lang:type=MemoryPool,name=" + mb.getName(),
                    MemoryPoolMXBean.class);
            // Not doing an exhaustive check on properties...
            assertEquals(mb.getName(), proxy.getName());
            assertEquals(mb.isValid(), proxy.isValid());
            assertEquals(mb.getType(), proxy.getType());
            assertEquals(mb.isCollectionUsageThresholdSupported(), proxy
                    .isCollectionUsageThresholdSupported());
            String[] names1 = mb.getMemoryManagerNames();
            String[] names2 = mb.getMemoryManagerNames();
            assertEquals(names1.length, names2.length);
            for (int i = 0; i < names1.length; i++) {
                assertEquals(names1[i], names2[i]);
            }
        }// end for all known memory manager beans
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_OperatingSystemMXBean()
            throws Exception {
        OperatingSystemMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "java.lang:type=OperatingSystem", OperatingSystemMXBean.class);
        assertNotNull(proxy);
        OperatingSystemMXBean mb = ManagementFactory.getOperatingSystemMXBean();
        assertEquals(mb.getArch(), proxy.getArch());
        assertEquals(mb.getAvailableProcessors(), proxy
                .getAvailableProcessors());
        assertEquals(mb.getVersion(), proxy.getVersion());
        assertEquals(mb.getName(), proxy.getName());
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_RuntimeMXBean() throws Exception {
        RuntimeMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "java.lang:type=Runtime", RuntimeMXBean.class);
        assertNotNull(proxy);
        RuntimeMXBean mb = ManagementFactory.getRuntimeMXBean();
        // Not an exhaustive check...
        assertEquals(mb.getName(), proxy.getName());
        assertEquals(mb.getClassPath(), proxy.getClassPath());
        assertEquals(mb.getStartTime(), proxy.getStartTime());
        List<String> args1 = mb.getInputArguments();
        List<String> args2 = proxy.getInputArguments();
        assertEquals(args1.size(), args2.size());
        for (String argument : args1) {
            assertTrue(args2.contains(argument));
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ManagementFactory.newPlatformMXBeanProxy(MBeanServerConnection,
     * String, Class<T>) <T>'
     */
    public void testNewPlatformMXBeanProxy_ThreadMXBean() throws Exception {
        ThreadMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "java.lang:type=Threading", ThreadMXBean.class);
        assertNotNull(proxy);
        ThreadMXBean mb = ManagementFactory.getThreadMXBean();
        // Not an exhaustive check...
        assertEquals(mb.isCurrentThreadCpuTimeSupported(), proxy
                .isCurrentThreadCpuTimeSupported());
        assertEquals(mb.isThreadContentionMonitoringSupported(), proxy
                .isThreadContentionMonitoringSupported());
        assertEquals(mb.isThreadCpuTimeSupported(), proxy
                .isThreadCpuTimeSupported());
        ThreadInfo info1 = mb.getThreadInfo(Thread.currentThread().getId());
        ThreadInfo info2 = proxy.getThreadInfo(Thread.currentThread().getId());
        // RI does not appear to override equals() for ThreadInfo so we take
        // the scenic route to check for equality...
        assertEquals(info1.getThreadId(), info2.getThreadId());
        assertEquals(info1.getBlockedCount(), info2.getBlockedCount());
        assertEquals(info1.getBlockedTime(), info2.getBlockedTime());
        assertEquals(info1.getThreadName(), info2.getThreadName());
        assertEquals(info1.getWaitedCount(), info2.getWaitedCount());
        assertEquals(info1.getLockName(), info2.getLockName());
    }
}
