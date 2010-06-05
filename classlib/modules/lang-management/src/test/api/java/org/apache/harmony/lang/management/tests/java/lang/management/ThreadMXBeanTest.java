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
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import junit.framework.TestCase;

public class ThreadMXBeanTest extends TestCase {

    private ThreadMXBean mb;

    protected void setUp() throws Exception {
        super.setUp();
        mb = ManagementFactory.getThreadMXBean();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.findMonitorDeadlockedThreads()'
     */
    public void testFindMonitorDeadlockedThreads() {
        // Check that if there are no deadlocked threads we get back
        // a null rather than a zero length array.
        long[] ids = mb.findMonitorDeadlockedThreads();
        if (ids != null) {
            assertTrue(ids.length != 0);
        }
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getAllThreadIds()'
     */
    public void testGetAllThreadIds() {
        int count = mb.getThreadCount();
        long[] ids = mb.getAllThreadIds();
        assertNotNull(ids);
        assertEquals(count, ids.length);
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.getCurrentThreadCpuTime()'
     */
    public void testGetCurrentThreadCpuTime() {
        // Outcome depends on whether or not CPU time measurement is supported
        // and enabled.
        if (mb.isCurrentThreadCpuTimeSupported()) {
            if (mb.isThreadCpuTimeEnabled()) {
                assertTrue(mb.getCurrentThreadCpuTime() > -1);
            } else {
                assertEquals(-1, mb.getCurrentThreadCpuTime());
            }
        } else {
            try {
                mb.getCurrentThreadCpuTime();
                fail("Should have thrown an UnsupportedOperationException!");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.getCurrentThreadUserTime()'
     */
    public void testGetCurrentThreadUserTime() {
        // Outcome depends on whether or not CPU time measurement is supported
        // and enabled.
        if (mb.isCurrentThreadCpuTimeSupported()) {
            if (mb.isThreadCpuTimeEnabled()) {
                assertTrue(mb.getCurrentThreadUserTime() > -1);
            } else {
                assertEquals(-1, mb.getCurrentThreadUserTime());
            }
        } else {
            try {
                mb.getCurrentThreadUserTime();
                fail("Should have thrown an UnsupportedOperationException!");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.getDaemonThreadCount()'
     */
    public void testGetDaemonThreadCount() {
        assertTrue(mb.getDaemonThreadCount() > -1);
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getPeakThreadCount()'
     */
    public void testGetPeakThreadCount() {
        assertTrue(mb.getPeakThreadCount() > -1);
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getThreadCount()'
     */
    public void testGetThreadCount() {
        assertTrue(mb.getThreadCount() > -1);
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.getThreadCpuTime(long)'
     */
    public void testGetThreadCpuTime() {
        // Outcome depends on whether or not CPU time measurement is supported
        // and enabled.
        if (mb.isThreadCpuTimeSupported()) {
            if (mb.isThreadCpuTimeEnabled()) {
                // Good case
                assertTrue(mb.getThreadCpuTime(Thread.currentThread().getId()) > -1);

                // Should throw a wobbler if a bad Thread id is passed in.
                try {
                    mb.getThreadCpuTime(-122);
                    fail("Should have thrown an IllegalArgumentException!");
                } catch (IllegalArgumentException ignore) {
                }
            } else {
                // Should return -1 if CPU time measurement is currently
                // disabled.
                assertEquals(-1, mb.getThreadCpuTime(Thread.currentThread()
                        .getId()));
            }
        } else {
            try {
                mb.getThreadCpuTime(100);
                fail("Should have thrown an UnsupportedOperationException!");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getThreadInfo(long)'
     */
    public void testGetThreadInfoLong() {
        // Should throw exception if a Thread id of 0 or less is input
        try {
            mb.getThreadInfo(0);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // For now, just check we don't get a null returned if we pass in
        // a Thread id which is definitely valid (i.e. our Thread id)...
        assertNotNull(mb.getThreadInfo(Thread.currentThread().getId()));
    }

    public final void testGetThreadInfoLongForUnstartedThread() {
        // Create another thread in the VM.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignore) {
                }
            }
        };
        Thread thread = new Thread(r);
        // deliberately not starting
        long thrdId = thread.getId();
        assertNull(mb.getThreadInfo(thrdId));
    }

    public final void testGethThreadInfoLongForTerminatedThread() {
        // Create another thread in the VM.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignore) {
                }
            }
        };
        Thread thread = new Thread(r);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ignore) {
        }
        long thrdId = thread.getId();
        assertNull(mb.getThreadInfo(thrdId));
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getThreadInfo(long[])'
     */
    public void testGetThreadInfoLongArray() {
        // Should throw exception if a Thread id of 0 or less is input
        try {
            long[] input = new long[] { 0 };
            mb.getThreadInfo(input);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // For now, just check we don't get a null returned if we pass in
        // a couple of Thread ids which are definitely valid (i.e. our Thread
        // id)...
        long[] input = new long[] { Thread.currentThread().getId(),
                Thread.currentThread().getId() };
        ThreadInfo[] tiArray = mb.getThreadInfo(input);
        assertNotNull(tiArray);
        for (ThreadInfo info : tiArray) {
            assertNotNull(info);
        }
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getThreadInfo(long[],
     * int)'
     */
    public void testGetThreadInfoLongArrayInt() {
        // Should throw exception if a Thread id of 0 or less is input
        try {
            long[] input = new long[] { 0 };
            mb.getThreadInfo(input, 0);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Should throw exception if maxDepth is negative
        try {
            long[] input = new long[] { Thread.currentThread().getId() };
            mb.getThreadInfo(input, -2445);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // For now, just check we don't get a null returned if we pass in
        // a Thread id which is definitely valid (i.e. our Thread id)...
        long[] input = new long[] { Thread.currentThread().getId(),
                Thread.currentThread().getId() };
        ThreadInfo[] tiArray = mb.getThreadInfo(input, 0);
        assertNotNull(tiArray);
        for (ThreadInfo info : tiArray) {
            assertNotNull(info);
        }
    }

    /*
     * Test method for 'java.lang.management.ThreadMXBean.getThreadInfo(long,
     * int)'
     */
    public void testGetThreadInfoLongInt() {
        // Should throw exception if a Thread id of 0 or less is input
        try {
            mb.getThreadInfo(0, 0);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }

        // Should throw exception if maxDepth is negative
        try {
            mb.getThreadInfo(0, -44);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException ignore) {
        }
        // For now, just check we don't get a null returned if we pass in
        // a Thread id which is definitely valid (i.e. our Thread id)...
        assertNotNull(mb.getThreadInfo(Thread.currentThread().getId(), 0));
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.getThreadUserTime(long)'
     */
    public void testGetThreadUserTime() {
        // Outcome depends on whether or not CPU time measurement is supported
        // and enabled.
        if (mb.isThreadCpuTimeSupported()) {
            if (mb.isThreadCpuTimeEnabled()) {
                // Good case
                assertTrue(mb.getThreadUserTime(Thread.currentThread().getId()) > -1);

                // Should throw a wobbler if a bad Thread id is passed in.
                try {
                    mb.getThreadUserTime(-122);
                    fail("Should have thrown an IllegalArgumentException!");
                } catch (IllegalArgumentException ignore) {
                }
            } else {
                // Should return -1 if CPU time measurement is currently
                // disabled.
                assertEquals(-1, mb.getThreadUserTime(Thread.currentThread()
                        .getId()));
            }
        } else {
            try {
                mb.getThreadUserTime(100);
                fail("Should have thrown an UnsupportedOperationException!");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.getTotalStartedThreadCount()'
     */
    public void testGetTotalStartedThreadCount() {
        long before = mb.getTotalStartedThreadCount();

        // Create another thread in the VM.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignore) {
                }
            }
        };
        Thread thread = new Thread(r);
        thread.start();

        // Sleep a while as on some VM implementations the new thread may
        // not start immediately.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
        }

        long after = mb.getTotalStartedThreadCount();
        assertTrue(after > before);
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.isCurrentThreadCpuTimeSupported()'
     */
    public void testIsCurrentThreadCpuTimeSupported() {
        // Should get the same response as a call to the
        // method isThreadCpuTimeSupported().
        assertEquals(mb.isCurrentThreadCpuTimeSupported(), mb
                .isThreadCpuTimeSupported());
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.resetPeakThreadCount()'
     */
    public void testResetPeakThreadCount() {
        int currentThreadCount = mb.getThreadCount();
        mb.resetPeakThreadCount();
        assertEquals(currentThreadCount, mb.getPeakThreadCount());
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.setThreadContentionMonitoringEnabled(boolean)'
     */
    public void testSetThreadContentionMonitoringEnabled() {
        // Response depends on whether or not thread contention
        // monitoring is supported.
        if (mb.isThreadContentionMonitoringSupported()) {
            // Disable tcm
            while (mb.isThreadContentionMonitoringEnabled()) {
                mb.setThreadContentionMonitoringEnabled(false);
                Thread.yield();
            }// end while

            // Check that a ThreadInfo returns -1 where expected.
            ThreadInfo info = mb.getThreadInfo(Thread.currentThread().getId());
            assertEquals(-1, info.getBlockedTime());
            assertEquals(-1, info.getWaitedTime());

            // re-enable tcm
            while (!mb.isThreadContentionMonitoringEnabled()) {
                mb.setThreadContentionMonitoringEnabled(true);
                Thread.yield();
            }// end while

            // Check that waited time and blocked time are now no longer
            // set to -1.
            ThreadInfo info2 = mb.getThreadInfo(Thread.currentThread().getId());
            assertTrue(info2.getBlockedTime() > -1);
            assertTrue(info2.getWaitedTime() > -1);
        } else {
            try {
                mb.isThreadContentionMonitoringEnabled();
                fail("Should have thrown UnsupportedOperationException!");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }

    /*
     * Test method for
     * 'java.lang.management.ThreadMXBean.setThreadCpuTimeEnabled(boolean)'
     */
    public void testSetThreadCpuTimeEnabled() {
        // Depends on whether or not Thread CPU timing is actually
        // supported on the current VM.
        if (mb.isThreadCpuTimeSupported()) {
            // Disable thread CPU time measuring
            while (mb.isThreadCpuTimeEnabled()) {
                mb.setThreadCpuTimeEnabled(false);
                Thread.yield();
            }// end while

            // Check that we get a -1 for the thread CPU time
            long time = mb.getThreadCpuTime(Thread.currentThread().getId());
            assertEquals(-1, time);

            // re-enable thread CPU time measuring
            while (!mb.isThreadCpuTimeEnabled()) {
                mb.setThreadCpuTimeEnabled(true);
                Thread.yield();
            }// end while

            // Check that we no longer get a -1 for the thread CPU time
            time = mb.getThreadCpuTime(Thread.currentThread().getId());
            assertTrue(time > -1);
        } else {
            try {
                mb.setThreadCpuTimeEnabled(false);
                fail("Should have thrown an UnsupportedOperationException!");
            } catch (UnsupportedOperationException ignore) {
            }
        }
    }
}
