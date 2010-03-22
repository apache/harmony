/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Roman S. Bushmanov
 */

package java.lang;

import junit.framework.TestCase;
import java.io.*;

import org.apache.harmony.test.ReversibleSecurityManager;

public class ThreadGroupTest extends TestCase {

    private static final String INTERRUPTED_MESSAGE = 
        "thread has been unexpectedly interrupted";

    // max time interval to wait for some events in ms
    private static final long waitDuration = 3000;

    // waiting time for some event
    private static long waitTime = 0;
    private boolean expired;
    private static PrintStream systemOut = System.out;
    private static PrintStream systemErr = System.err;

    class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        public boolean wasCalled = false;

        public void uncaughtException(Thread t, Throwable e) {
            wasCalled = true;
        }
    }

    class ThreadGroupHandler extends ThreadGroup {
        boolean handlerWasCalled = false;
        
        public ThreadGroupHandler(String name) {
            super(name);
        }

        public ThreadGroupHandler(ThreadGroup g, String name) {
            super(g, name);
        }

        public void uncaughtException(Thread t, Throwable e) {
            handlerWasCalled = true;
        }
    }
    
    class TestThread extends Thread {

        volatile boolean enough = false;

        boolean finished = false;

        TestThread(ThreadGroup group, String name) {
            super(group, name);
        }

        public void run() {
            while (!enough) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            finished = true;
        }
    }

    /**
     * Sleep for "interval" ms
     * @return true if waitTime is up
     */
    private static boolean doSleep(int interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException while sleeping");
        }
        waitTime -= interval;
        return waitTime <= 0;
    }
    
    /**
     * ThreadGroup(String)
     */
    public void testThreadGroupThreadGroupString() {
        ThreadGroup group = new ThreadGroup("group");
        assertSame("wrong parrent",
                   Thread.currentThread().getThreadGroup(), group.getParent());
    }

    /**
     * Create a ThreadGroup in a destroyed ThreadGroup
     */
    public void testThreadGroupThreadGroupString_InDestroyedGroup() {
        try {
            ThreadGroup group = new ThreadGroup("group");
            group.destroy();
            new ThreadGroup(group, "group1");
            fail("Constructor should throw IllegalThreadStateException!");
        } catch (IllegalThreadStateException e) {
            return;
        }
    }

    /**
     * test ThreadGroup(null)
     */
    public void testThreadGroupString_Null() {
        ThreadGroup group = null;
        try {
            group = new ThreadGroup(null);
        } catch (NullPointerException e) {
            fail("Constructor should accept null names!");
        }
        assertNull(group.getName());
    }

    /**
     * activeCount() in a new ThreadGroup
     */
    public void testActiveCount_NoThreads() {
        ThreadGroup group = new ThreadGroup("new");
        assertEquals(0, group.activeCount());
    }

    /**
     * activeCount() in a ThreadGroup with an empty subgroup
     */
    public void testActiveCount_CreateDestroySubgroup() {
        ThreadGroup group = new ThreadGroup("group");
        ThreadGroup group1 = new ThreadGroup(group, "group1");
        assertEquals(1, group.activeGroupCount());
        group1.destroy();
        assertEquals(0, group.activeGroupCount());
    }

    /**
     * activeCount() in a ThreadGroup with a few new threads
     */
    public void testActiveCount_NewThreads() {
        ThreadGroup group = new ThreadGroup("new");
        new Thread(group, "t1");
        new Thread(group, "t2");
        new Thread(group, "t3");
        assertEquals(0, group.activeCount());
    }

    /**
     * activeCount() in a ThreadGroup with a few started and 
     * terminated threads
     */
    public void testActiveCount_StartedTerminatedThreads() {
        ThreadGroup group = new ThreadGroup("new");
        ThreadTest.ThreadRunning t1 = new ThreadTest.ThreadRunning(group, "t1");
        ThreadTest.ThreadRunning t2 = new ThreadTest.ThreadRunning(group, "t2");
        ThreadTest.ThreadRunning t3 = new ThreadTest.ThreadRunning(group, "t3");
        t1.start();
        t2.start();
        t3.start();
        doSleep(100);
        assertEquals("incorrect number of started threads",
                     3, group.activeCount());
        t1.stopWork = true;
        t2.stopWork = true;
        t3.stopWork = true;
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        assertEquals("there should be no live threads", 0, group.activeCount());
    }

    /**
     * activeCount() in a ThreadGroup with a few threads running in a subgroups 
     */
    public void testActiveCount_Subgroup() {
        ThreadGroup parent = new ThreadGroup("parent");
        ThreadGroup child1 = new ThreadGroup(parent, "child1");
        ThreadTest.ThreadRunning t11 = new ThreadTest.ThreadRunning(parent,
                                                                    "t11");
        ThreadTest.ThreadRunning tc11 = new ThreadTest.ThreadRunning(child1,
                                                                     "tc11");
        ThreadTest.ThreadRunning tc12 = new ThreadTest.ThreadRunning(child1,
                                                                     "tc12");
        ThreadGroup gChild1 = new ThreadGroup(child1, "gChild1");
        ThreadTest.ThreadRunning tgc11 = new ThreadTest.ThreadRunning(gChild1,
                                                                      "tgc11");
        ThreadTest.ThreadRunning tgc12 = new ThreadTest.ThreadRunning(gChild1,
                                                                      "tgc12");
        ThreadGroup child2 = new ThreadGroup(parent, "child2");
        ThreadTest.ThreadRunning tc21 = new ThreadTest.ThreadRunning(child2,
                                                                     "tc21");
        ThreadTest.ThreadRunning tc22 = new ThreadTest.ThreadRunning(child2,
        "tc22");
        assertEquals("new threads should not be counted",
                     0, parent.activeCount());
        t11.start();
        tc11.start();
        tc12.start();
        tgc11.start();
        tgc12.start();
        tc21.start();
        tc22.start();
        doSleep(100);
        assertEquals("incorrect number of active threads",
                     7, parent.activeCount());
        t11.stopWork = true;
        tc11.stopWork = true;
        tc12.stopWork = true;
        tgc11.stopWork = true;
        tgc12.stopWork = true;
        tc21.stopWork = true;
        tc22.stopWork = true;
        try {
            t11.join();
            tc11.join();
            tc12.join();
            tgc11.join();
            tgc12.join();
            tc21.join();
            tc22.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        assertEquals("there should be no live threads", 0, parent.activeCount());
    }

    /** 
     * Verify activeGroupCount() for a group containing
     * a few subgroups
     */
    public void testActiveGroupCount_Subgroups() {
        ThreadGroup tg1 = new ThreadGroup("group 1");
        new ThreadGroup(tg1, "group 11");
        ThreadGroup tg12 = new ThreadGroup(tg1, "group 12");
        new ThreadGroup(tg1, "group 13");
        new ThreadGroup(tg12, "group 121");
        new ThreadGroup(tg12, "group 122");
        ThreadGroup tg123 = new ThreadGroup(tg12, "group 123");
        new Thread(tg123, "thread 1231");
        new Thread(tg123, "thread 1232");
        assertEquals(6, tg1.activeGroupCount());
    }

    /** 
     * Verify activeGroupCount() after destroying a group 
     * where one of subgroups is not empty
     */
    public void testActiveGroupCount_DestroyNonEmptySubgroup() {
        ThreadGroup tg1 = new ThreadGroup("group 1");
        ThreadGroup tg11 = new ThreadGroup(tg1, "group 11");
        ThreadGroup tg12 = new ThreadGroup(tg1, "group 12");
        ThreadGroup tg13 = new ThreadGroup(tg1, "group 13");
        ThreadGroup tg121 = new ThreadGroup(tg12, "group 121");
        ThreadGroup tg122 = new ThreadGroup(tg12, "group 122");
        ThreadGroup tg123 = new ThreadGroup(tg12, "group 123");
        new ThreadTest.ThreadRunning(tg122, "thread 1221").start();
        new ThreadTest.ThreadRunning(tg122, "thread 1222").start();
        // Non-empty subgroup tg122 should not be destroyed.
        // IllegalThreadStateException should be thrown.
        // Groups residing on the right from tg123 in the groups tree
        // should not be destroyed as well.
        try {
            tg1.destroy();
            fail("IllegalThreadStateException has not been thrown when " +
            "destroying non-empty subgroup");
        } catch (IllegalThreadStateException e) {
        }
        assertEquals("wrong group count in tg1", 0, tg1.activeGroupCount());
        assertEquals("wrong group count in tg12", 0, tg12.activeGroupCount());
        assertTrue("tg1 is not destroyed", tg1.isDestroyed());
        assertTrue("tg11 is not destroyed", tg11.isDestroyed());
        assertTrue("tg12 is not destroyed", tg12.isDestroyed());
        assertTrue("tg13 is destroyed", !tg13.isDestroyed());
        assertTrue("tg121 is not destroyed", tg121.isDestroyed());
        assertTrue("tg122 is destroyed", !tg122.isDestroyed());
        assertTrue("tg123 is destroyed", !tg123.isDestroyed());
    }

    /**
     * Verify the checkAccess() method
     */
    public void testCheckAccess() {
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new ReversibleSecurityManager());
        ThreadGroup tg1 = new ThreadGroup("tg1");
        try {
            tg1.checkAccess();
        } finally {
            System.setSecurityManager(sm);
        }
    }

    /**
     * Checks the destroy() method for a destroyed thread group.
     * IllegalThreadStateException should be thrown.
     */
    public void testDestroy_Destroyed() {
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadGroup tg2 = new ThreadGroup(tg1, "tg2");
        tg2.setDaemon(true);
        TestThread t2 = new TestThread(tg2, "t2");
        t2.start();
        t2.enough = true;
        try {
            t2.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        // tg2 is daemon and should be already destroyed
        try {
            tg2.destroy();
        	fail("IllegalThreadStateException should be thrown " + 
                 "when destroying a destroyed thread group");
        } catch (IllegalThreadStateException e) {
        }
    }

    /**
     * Checks the destroy() method for a group containing a destroyed subgroup.
     * IllegalThreadStateException should not be thrown.
     */
    public void testDestroy_DestroyedSubgroup() {
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadGroup tg2 = new ThreadGroup(tg1, "tg2");
        tg2.setDaemon(true);
        TestThread t2 = new TestThread(tg2, "t2");
        t2.start();
        t2.enough = true;
        try {
            t2.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }

        // tg1 should be destroyed because its subgroup tg2 has been destroyed
        // silently as a daemon ThreadGroup with no live threads
        try {
            tg1.destroy();
        } catch (IllegalThreadStateException e) {
        	fail("IllegalThreadStateException has been thrown when destroying" +
        			" a thread group containing a destroyed subgroup");
        }
    }

    /** 
     * Verify destroying a group where one of subgroups is not empty 
     * but all threads in it have finished.
     */
    public void testDestroy_FinishedThreads() {
        ThreadGroup tg1 = new ThreadGroup("group 1");
        ThreadGroup tg11 = new ThreadGroup(tg1, "group 11");
        ThreadGroup tg12 = new ThreadGroup(tg1, "group 12");
        ThreadGroup tg13 = new ThreadGroup(tg1, "group 13");
        ThreadGroup tg121 = new ThreadGroup(tg12, "group 121");
        ThreadGroup tg122 = new ThreadGroup(tg12, "group 122");
        ThreadGroup tg123 = new ThreadGroup(tg12, "group 123");
        ThreadTest.ThreadRunning t1 = new ThreadTest.ThreadRunning(tg123,
                "thread 1231");
        t1.start();
        ThreadTest.ThreadRunning t2 = new ThreadTest.ThreadRunning(tg123,
                "thread 1232");
        t2.start();
        t1.stopWork = true;
        t2.stopWork = true;
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        for (waitTime = waitDuration; t1.isAlive() && t2.isAlive() && !(expired = doSleep(10));) {
        }
        if (expired) {
            fail("thread have not finished for " + waitDuration + " ms");
        }
        try {
            tg1.destroy();
        } catch (IllegalThreadStateException e) {
            fail("IllegalThreadStateException should not been thrown " +
            "because threads have fnished");
        }
        assertTrue(tg1.getName() + " has not been destroyed", tg1.isDestroyed());
        assertTrue(tg11.getName() + " has not been destroyed", tg11.isDestroyed());
        assertTrue(tg12.getName() + " has not been destroyed", tg12.isDestroyed());
        assertTrue(tg13.getName() + " has not been destroyed", tg13.isDestroyed());
        assertTrue(tg121.getName() + " has not been destroyed", tg121.isDestroyed());
        assertTrue(tg122.getName() + " has not been destroyed", tg122.isDestroyed());
        assertTrue(tg123.getName() + " has not been destroyed", tg123.isDestroyed());
        assertEquals("tg1 should be empty", 0, tg1.activeGroupCount());
        assertEquals("tg12 should be empty", 0, tg12.activeGroupCount());
    }

    /**
     * Checks the destroy() method for a group with 3 subgroups.
     * Verifies the <code>destroyed</code> flag.
     */
    public void testDestroy_ThreeChildren() {
    	ThreadGroup groups[] = {new ThreadGroup("tg0"), null, null, null};
    	groups[1] = new ThreadGroup(groups[0], "tg1");
    	groups[2] = new ThreadGroup(groups[0], "tg2");
    	groups[3] = new ThreadGroup(groups[0], "tg3");
    	groups[0].destroy();
        assertTrue("group[0] has not been destroyed", groups[0].isDestroyed());
        assertTrue("group[1] has not been destroyed", groups[1].isDestroyed());
        assertTrue("group[2] has not been destroyed", groups[2].isDestroyed());
        assertTrue("group[3] has not been destroyed", groups[3].isDestroyed());
   }

    /**
     * Checks the destroy() method for a group with 2 subgroups.
     * Verifies the activeGroupCount() value.
     */
    public void testDestroy_TwoChildren() {
        ThreadGroup group = new ThreadGroup("group");
        new ThreadGroup(group, "group1");
        new ThreadGroup(group, "group2");
        assertEquals(2, group.activeGroupCount());
        group.destroy();
        assertEquals(0, group.activeGroupCount());
    }

    /**
     * Verify getMaxPriority()
     */
    public void testGetMaxPriority() {
        ThreadGroup tg = new ThreadGroup("tg");
        int groupMaxPriority = tg.getMaxPriority();
        assertEquals("incorrect priority",
                     Thread.currentThread().getThreadGroup().getMaxPriority(),
                     groupMaxPriority);
        Thread t = new Thread(tg, "t");
        assertTrue("incorect thread's priority", 
                   t.getPriority() <= groupMaxPriority);        
    }

    /**
     * Verify enumerate(Thread[])
     */
    public void testEnumerateThread() {
        ThreadGroup tg = new ThreadGroup("tg");
        ThreadGroup tg1 = new ThreadGroup(tg, "tg1");
        ThreadGroup tg2 = new ThreadGroup(tg1, "tg2");
        ThreadTest.ThreadRunning tArray[] = new ThreadTest.ThreadRunning[9];
        for (int i = 0; i < 3; i++) {
            tArray[i] = new ThreadTest.ThreadRunning(tg, "ttt");
            tArray[i].start();
            tArray[i + 3] = new ThreadTest.ThreadRunning(tg1, "ttt");
            tArray[i + 3].start();
            tArray[i + 6] = new ThreadTest.ThreadRunning(tg2, "ttt");
            tArray[i + 6].start();
        }
        doSleep(50);
        // estimate dimension as 9 threads + 1
        int estimateLength = 10;
        Thread list[];
        int count;
        while (true) {
            list = new Thread[estimateLength];
            count = tg.enumerate(list);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("ttt") > 0) {
                enumerateCount++;
            }
        }
        for (int i = 0; i < 9; i++) {
            tArray[i].stopWork = true;
        }
        assertEquals("incorrect number of threads in tg", 9, enumerateCount);
    }

    /**
     * Verify enumerate(Thread[], false)
     */
    public void testEnumerateThreadBoolean_False() {
        ThreadGroup tg = new ThreadGroup("tg");
        ThreadGroup tg1 = new ThreadGroup(tg, "tg1");
        ThreadGroup tg2 = new ThreadGroup(tg1, "tg2");
        ThreadTest.ThreadRunning tArray[] = new ThreadTest.ThreadRunning[9];
        for (int i = 0; i < 3; i++) {
            tArray[i] = new ThreadTest.ThreadRunning(tg, "ttt");
            tArray[i].start();
            tArray[i + 3] = new ThreadTest.ThreadRunning(tg1, "ttt");
            tArray[i + 3].start();
            tArray[i + 6] = new ThreadTest.ThreadRunning(tg2, "ttt");
            tArray[i + 6].start();
        }
        doSleep(50);
        // estimate dimension as 3 threads + 1
        int estimateLength = 4;
        Thread list[];
        int count;
        while (true) {
            list = new Thread[estimateLength];
            count = tg.enumerate(list, false);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("ttt") > 0) {
                enumerateCount++;
            }
        }
        for (int i = 0; i < 9; i++) {
            tArray[i].stopWork = true;
        }
        assertEquals("incorrect number of threads in tg", 3, enumerateCount);
    }

    /**
     * Verify enumerate(Thread[], true)
     */
    public void testEnumerateThread_True() {
        ThreadGroup tg = new ThreadGroup("tg");
        ThreadGroup tg1 = new ThreadGroup(tg, "tg1");
        ThreadGroup tg2 = new ThreadGroup(tg1, "tg2");
        ThreadTest.ThreadRunning tArray[] = new ThreadTest.ThreadRunning[9];
        for (int i = 0; i < 3; i++) {
            tArray[i] = new ThreadTest.ThreadRunning(tg, "ttt");
            tArray[i].start();
            tArray[i + 3] = new ThreadTest.ThreadRunning(tg1, "ttt");
            tArray[i + 3].start();
            tArray[i + 6] = new ThreadTest.ThreadRunning(tg2, "ttt");
            tArray[i + 6].start();
        }
        doSleep(50);
        // estimate dimension as 9 threads + 1
        int estimateLength = 10;
        Thread list[];
        int count;
        while (true) {
            list = new Thread[estimateLength];
            count = tg.enumerate(list, true);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("ttt") > 0) {
                enumerateCount++;
            }
        }
        for (int i = 0; i < 9; i++) {
            tArray[i].stopWork = true;
        }
        assertEquals("incorrect number of threads in tg", 9, enumerateCount);
    }

    /**
     * Verify enumerate(ThreadGroup[])
     */
    public void testEnumerateThreadGroup() {
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadGroup tg11 = new ThreadGroup(tg1, "tg11");
        new ThreadGroup(tg1, "tg12");
        new ThreadGroup(tg11, "tg111");
        new ThreadGroup(tg11, "tg112");
        // estimate dimension as 4 threads + 1
        int estimateLength = 5;
        ThreadGroup list[];
        int count;
        while (true) {
            list = new ThreadGroup[estimateLength];
            count = tg1.enumerate(list);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("tg1") > 0) {
                enumerateCount++;
            }
        }
        assertEquals("incorrect number of thread groups in tg",
                     4, enumerateCount);
    }

    /**
     * Verify enumerate(ThreadGroup[]) when there is a destroyed subgroup
     */
    public void testEnumerateThreadGroup_Destroyed() {
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadGroup tg11 = new ThreadGroup(tg1, "tg11");
        new ThreadGroup(tg1, "tg12");
        ThreadGroup tg111 = new ThreadGroup(tg11, "tg111");
        new ThreadGroup(tg11, "tg112");
        tg111.destroy();
        // estimate dimension as 4 threads + 1
        int estimateLength = 5;
        ThreadGroup list[];
        int count;
        while (true) {
            list = new ThreadGroup[estimateLength];
            count = tg1.enumerate(list);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("tg1") > 0) {
                enumerateCount++;
            }
        }
        assertEquals("incorrect number of thread groups in tg",
                     3, enumerateCount);
    }

    /**
     * Verify enumerate(ThreadGroup[], false)
     */
    public void testEnumerateThreadGroup_False() {
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadGroup tg11 = new ThreadGroup(tg1, "tg11");
        new ThreadGroup(tg1, "tg12");
        new ThreadGroup(tg11, "tg111");
        new ThreadGroup(tg11, "tg112");
        // estimate dimension as 4 threads + 1
        int estimateLength = 5;
        ThreadGroup list[];
        int count;
        while (true) {
            list = new ThreadGroup[estimateLength];
            count = tg1.enumerate(list, false);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("tg1") > 0) {
                enumerateCount++;
            }
        }
        assertEquals("incorrect number of thread groups in tg",
                     2, enumerateCount);
    }

    /**
     * Verify enumerate(ThreadGroup[], true)
     */
    public void testEnumerateThreadGroup_True() {
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadGroup tg11 = new ThreadGroup(tg1, "tg11");
        new ThreadGroup(tg1, "tg12");
        new ThreadGroup(tg11, "tg111");
        new ThreadGroup(tg11, "tg112");
        // estimate dimension as 4 threads + 1
        int estimateLength = 5;
        ThreadGroup list[];
        int count;
        while (true) {
            list = new ThreadGroup[estimateLength];
            count = tg1.enumerate(list, true);
            if (count == estimateLength) {
                estimateLength *= 2;
            } else {
                break;
            }
        }
        int enumerateCount = 0;
        for (int i = 0; i < count; i++) {
            if (list[i].toString().indexOf("tg1") > 0) {
                enumerateCount++;
            }
        }
        assertEquals("incorrect number of thread groups in tg",
                     4, enumerateCount);
    }

    /**
     * Verify getName()
     */
    public void testGetName() {
        String name = "newGroup";
        String childName = "newChildGroup";
        ThreadGroup tg = new ThreadGroup(name);
        assertEquals("wrong name", name, tg.getName());
        ThreadGroup tgChild = new ThreadGroup(tg, childName);
        assertEquals("wrong child name", childName, tgChild.getName());
    }

    /**
     * Verify getParent()
     */
    public void testGetParent() {
        ThreadGroup parent = new ThreadGroup("parent");
        ThreadGroup child = new ThreadGroup(parent, "child");
        ThreadGroup grandChild = new ThreadGroup(child, "grandChild");
        assertSame("improper parent of child", parent, child.getParent());
        assertSame("improper parent of grandchild",
                    child, grandChild.getParent());
    }

    /**
     * Verify getParent() of a destroyed group
     */
    public void testGetParent_DestroyedGroup() {
        ThreadGroup parent = new ThreadGroup("parent");
        ThreadGroup child = new ThreadGroup(parent, "child");
        ThreadGroup grandchild = new ThreadGroup(child, "grandchild");
        child.destroy();
        assertTrue("child has not been destroyed", child.isDestroyed());
        assertTrue("grandchild has not been destroyed", 
                   grandchild.isDestroyed());
        assertSame("improper parent of a destroyed group",
                   parent, child.getParent());
        assertSame("a destroyed group should stay parent",
                   child, grandchild.getParent());
    }

    /**
     * Verify getParent() of a top-level group
     */
    public void testGetParent_TopLevelGroup() {
        ThreadGroup parent = Thread.currentThread().getThreadGroup().getParent();
        int groupCount = 1000;
        while ((parent != null) && (--groupCount >= 0)) {
            parent = parent.getParent();
        }
        assertNull("top-level group's parent is not null", parent);
    }

    /**
     * Interrupt a running thread
     */
    public void testInterrupt() {
        ThreadGroup tg = new ThreadGroup("tg");
        ThreadGroup tg1 = new ThreadGroup(tg, "tg1");
        ThreadTest.ThreadRunning tArray[] = new ThreadTest.ThreadRunning[6];
        for (int i = 0; i < 3; i++) {
            tArray[i] = new ThreadTest.ThreadRunning(tg, "ttt");
            tArray[i].start();
            tArray[i + 3] = new ThreadTest.ThreadRunning(tg1, "ttt");
            tArray[i + 3].start();
        }
        doSleep(50);
        tg.interrupt();
        waitTime = waitDuration;
        for (int i = 0; i < 6; i++) {
            while (!tArray[i].isInterrupted() && !(expired = doSleep(10))) {
            }
            if (expired) {
                break;
            }
        }
        for (int i = 0; i < 6; i++) {
            tArray[i].stopWork = true;
        }
        if (expired) {
            fail("threads have not been interrupted");
        }
    }

    /**
     * Verify list()
     */
    public void testList() {
        File file = null;
        PrintStream newOut = null;
        try {
            file = File.createTempFile("JUnit_ThreadGroupListTest", ".tmp");
            newOut = new PrintStream(new FileOutputStream(file));
        } catch (java.io.IOException e) {
            fail("unexpected IOException 1: " + e);
        }
        try {
            System.setOut(newOut);
        } catch (SecurityException e) {
            return;
        }
        ThreadGroup tg = new ThreadGroup("tg");
        tg.list();
        newOut.close();
        System.setOut(systemOut);
        byte buf[] = new byte[100];
        try {
            FileInputStream inp = new FileInputStream(file);
            inp.read(buf);
            inp.close();
        } catch (java.io.IOException e) {
            fail("unexpected IOException 2: " + e);
        }
        file.delete();
        String toString = "java.lang.ThreadGroup[name=tg,maxpri=";
        assertEquals("thread group info has not been printed",
                     0, new String(buf).indexOf(toString));
    }
 
    /**
     * Verify parentOf()
     */
    public void testParentOf() {
        ThreadGroup tg = new ThreadGroup("tg");
        assertTrue("should be true for the argument", tg.parentOf(tg));
        ThreadGroup tg1 = new ThreadGroup(tg, "tg1");
        assertTrue("tg should be parent of tg1", tg.parentOf(tg1));
        ThreadGroup tg2 = new ThreadGroup(tg1, "tg2");
        assertTrue("tg1 should be parent of tg2", tg1.parentOf(tg2));
        assertTrue("tg should be parent of tg2", tg.parentOf(tg2));
    }

    /**
     * Verify that maxPriority is inherited by a created subgroup
     */
    public void testSetMaxPriority_CreateSubgroup() {
        ThreadGroup tg = new ThreadGroup("tg");
        int pri = Thread.MAX_PRIORITY - 1;
        tg.setMaxPriority(pri);
        ThreadGroup tg1 = new ThreadGroup(tg, "tg1");
        assertEquals("incorrect priority for the created subgroup",
                     pri, tg1.getMaxPriority());
    }

    /**
     * Decrease group's maxPriority.
     * Verify that threads in the thread group that already have a higher 
     * priority are not affected.
     */
    public void testSetMaxPriority_Decrease() {
        ThreadGroup group = new ThreadGroup("new");
        Thread t1 = new Thread();
        int threadPri = t1.getPriority();
        int newGroupMaxPri = threadPri - 1;
        group.setMaxPriority(newGroupMaxPri);
        assertEquals("incorrect group's priority",
                     newGroupMaxPri, group.getMaxPriority());
        assertEquals("thread's priority should not be affected",
                     threadPri, t1.getPriority());
    }

    /**
     * Verify that lower maxPriority is set recursively to all subgroups
     */
    public void testSetMaxPriority_DecreaseRecursively() {
        String gName = "tg";
        int i;
        ThreadGroup tg = new ThreadGroup(gName);
        int pri = Thread.MAX_PRIORITY - 1;
        tg.setMaxPriority(pri);
        for (i = 0; i < 3; i++) {
            new ThreadGroup(tg, gName + i);
        }
        ThreadGroup tg11 = new ThreadGroup(tg, gName + "11");
        for (i = 0; i < 3; i++) {
            new ThreadGroup(tg11, gName + i);
        }
        ThreadGroup tg22 = new ThreadGroup(tg11, gName + "22");
        for (i = 0; i < 3; i++) {
            new ThreadGroup(tg22, gName + i);
        }
        pri--;
        tg.setMaxPriority(pri);
        ThreadGroup list[] = new ThreadGroup[11];
        tg.enumerate(list);
        for (i = 0; i < 11; i++) {
            assertEquals("incorrect new priority for the group " + list[i],
                         pri, list[i].getMaxPriority());
        }
    }

    /**
     * Increase group's maxPriority.
     */
    public void testSetMaxPriority_Increase() {
        ThreadGroup group = new ThreadGroup("new");
        group.setMaxPriority(Thread.NORM_PRIORITY);
        int newGroupMaxPri = Thread.NORM_PRIORITY + 1;
        group.setMaxPriority(newGroupMaxPri);
        assertEquals("incorrect group's priority",
                     newGroupMaxPri, group.getMaxPriority());
    }

    /**
     * Verify that higher maxPriority is set recursively to all subgroups
     */
    public void testSetMaxPriority_IncreaseRecursively() {
        String gName = "tg";
        int i;
        ThreadGroup tg = new ThreadGroup(gName);
        int pri = Thread.NORM_PRIORITY;
        tg.setMaxPriority(pri);
        for (i = 0; i < 3; i++) {
            new ThreadGroup(tg, gName + i);
        }
        ThreadGroup tg11 = new ThreadGroup(tg, gName + "11");
        for (i = 0; i < 3; i++) {
            new ThreadGroup(tg11, gName + i);
        }
        ThreadGroup tg22 = new ThreadGroup(tg11, gName + "22");
        for (i = 0; i < 3; i++) {
            new ThreadGroup(tg22, gName + i);
        }
        pri++;
        tg.setMaxPriority(pri);
        ThreadGroup list[] = new ThreadGroup[11];
        tg.enumerate(list);
        for (i = 0; i < 11; i++) {
            assertEquals("incorrect new priority for the group " + list[i],
                         pri, list[i].getMaxPriority());
        }
    }

    /**
     * Try to set maxPriority which is higher than the parent's one
     */
    public void testSetMaxPriority_HigherParent() {
        ThreadGroup parent = new ThreadGroup("par");
        int parPriority = Thread.MAX_PRIORITY - 1;
        parent.setMaxPriority(parPriority);
        assertEquals("incorrect priority received",
                     parPriority, parent.getMaxPriority());
        ThreadGroup child = new ThreadGroup(parent, "ch");
        child.setMaxPriority(parPriority - 1);
        int newChildPriority = parPriority + 1;
        child.setMaxPriority(newChildPriority);
        // according to spec the smaller of newChildPriority and parPriority
        // should be set
        assertEquals("child priority should equal to parent's one",
                     parPriority, child.getMaxPriority());
    }

    /**
     * Try to set maxPriority which is out of range
     */
    public void testSetMaxPriority_OutOfRange() {
        ThreadGroup tg = new ThreadGroup("tg");
        int curPriority = tg.getMaxPriority();
        int newPriority = Thread.MAX_PRIORITY + 1;
        tg.setMaxPriority(newPriority);
        assertEquals("Assert1: group priority should not change",
                     curPriority, tg.getMaxPriority());
        newPriority = Thread.MIN_PRIORITY - 1;
        tg.setMaxPriority(newPriority);
        assertEquals("Assert2: group priority should be set to Thread.MIN_PRIORITY",
        		Thread.MIN_PRIORITY, tg.getMaxPriority());
    }

    /**
     * Verify setMaxPriority() of a system group
     */
    public void testSetMaxPriority_TopLevelGroup() {
        ThreadGroup system = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = system.getParent();
        int groupCount = 1000;
        while (parent != null && --groupCount >= 0) {
            system = parent;
            parent = system.getParent();
        }
        int newSystemPriority = system.getMaxPriority() - 1;
        try {
            system.setMaxPriority(newSystemPriority);
            assertEquals("priority has not changed",
                         newSystemPriority, system.getMaxPriority());
        } catch (SecurityException e) {
        }
    }

    /**
     * Verify set/isDaemon()
     */
    public void testSetDaemon() {
        ThreadGroup tg = new ThreadGroup("tg");
        assertFalse("a new group should not be daemon", tg.isDaemon());
        tg.setDaemon(true);
        assertTrue("daemon status has not been set", tg.isDaemon());
        ThreadGroup child = new ThreadGroup(tg, "child");
        assertTrue("a child of a daemon group should be daemon",
                   child.isDaemon());
        tg.setDaemon(false);
        assertFalse("daemon status has not been removed", tg.isDaemon());
    }

    /**
     * Verifies the suspend/resume() method
     */
    public void testSuspend() {
        ThreadGroup group = new ThreadGroup("Custom group");
        TestThread thread = new TestThread(group, "Custom thread");
        thread.start();
        group.suspend();
        assertFalse("the suspended thread should not finish", thread.finished);
        thread.enough = true;
        group.resume();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        assertTrue("thread has not finished", thread.finished);
    }
    
    /**
     * Verify toString() output 
     */
    public void testToString() {
        ThreadGroup tg = new ThreadGroup("tg");
        String toString = "java.lang.ThreadGroup[name=tg,maxpri=";
        assertEquals("incorrect representation", 
                     0, tg.toString().indexOf(toString));
    }

    public void testUncaughtExceptionHandlers() {
        ExceptionHandler defaultHandler = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
        Thread t = new Thread("test thread");
        Thread.UncaughtExceptionHandler handler = t.getUncaughtExceptionHandler();
        assertNotNull("handler should not be null", handler);
        assertSame("thread's thread group expected to be a handler", 
                   t.getThreadGroup(), handler);
        handler.uncaughtException(t, new RuntimeException());
        assertTrue("Default exception handler was not called",
                   defaultHandler.wasCalled);       
    }
    
    /**
     * Verify that thread's explicit exception handler is used
     */
   public void testUncaughtExceptionHandler_Explicit(){
       ExceptionHandler handler = new ExceptionHandler();
       Thread testThread = new Thread("test thread") {
           public void run() {
               throw new RuntimeException();    
           }
       };
       testThread.setUncaughtExceptionHandler(handler);
       testThread.start();
       for(int i=0; i<10 && testThread.isAlive(); i++){
           try{
               Thread.sleep(50);
           }catch(InterruptedException e){}
       }
       assertTrue("Thread's uncaught exception handler wasn't called", 
                  handler.wasCalled);
   }
   
    /**
     * Verify that thread's explicit exception handler is used first
     * even if a default UncaughtExceptionHandler is set
     */
    public void testUncaughtException_ExplicitDefault() {
        ExceptionHandler deh = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(deh);
        Thread t = new Thread("test thread") {
            public void run() {
                throw new RuntimeException();    
            }
        };
        ExceptionHandler eh = new ExceptionHandler();
        t.setUncaughtExceptionHandler(eh);
        t.start();
        waitTime = waitDuration;
        while (!eh.wasCalled && !(expired = doSleep(10))) {
        }
        assertFalse("thread's default exception handler should not been called",
                deh.wasCalled);
        if (expired) {
            fail("thread's exception handler has not been called");
        }
    }

    /**
     * Verify that uncaughtException() method of thread's parent ThreadGroup
     * is called even if a default UncaughtExceptionHandler is set
     */
    public void testUncaughtException_ThreadGroupDefault() {
        ExceptionHandler deh = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(deh);
        ThreadGroup grandPa = new ThreadGroup("grandPa");
        ThreadGroupHandler parent = new ThreadGroupHandler(grandPa, "parent");
        ThreadGroup child = new ThreadGroup(parent, "tg");
        String tName = "testHandler";
        Thread t = new Thread(child, tName) {
            public void run() {
                throw new RuntimeException();    
            }
        };
        t.start();
        waitTime = waitDuration;
        while (!parent.handlerWasCalled && !(expired = doSleep(10))) {
        }
        assertFalse("thread's default exception handler should not been called",
                deh.wasCalled);
        if (expired) {
            fail("threadGroup's uncaughtException() has not been called");
        }
    }

    /**
     * Verify that uncaughtException(Thread, Throwable) method
     * where Throwable is ThreadDeath does nothing
     */
    public void testUncaughtException_ThreadDeath() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        File file = null;
        PrintStream newErr = null;
        try {
            file = File.createTempFile("JUnit_ThreadUETest1", ".tmp");
            newErr = new PrintStream(new FileOutputStream(file));
        } catch (java.io.IOException e) {
            fail("unexpected IOException 1: " + e);
        }
        try {
            System.setErr(newErr);
        } catch (SecurityException e) {
            return;
        }
        Thread t = new Thread("testThreadDeath") {
            public void run() {
                throw new ThreadDeath();    
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        newErr.close();
        System.setErr(systemErr);
        byte buf[] = new byte[2];
        try {
            FileInputStream inp = new FileInputStream(file);
            inp.read(buf);
            inp.close();
        } catch (java.io.IOException e) {
            fail("unexpected IOException 2: " + e);
        }
        file.delete();
        assertTrue("Uncaught Exception message has not been printed",
                   buf[0] == 0);
    }
 
    /**
     * Verify that uncaughtException(Thread, Throwable) method
     * prints a proper message
     */
    public void testUncaughtException_NullPointerException() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        File file = null;
        PrintStream newErr = null;
        try {
            file = File.createTempFile("JUnit_ThreadUETest2", ".tmp");
            newErr = new PrintStream(new FileOutputStream(file));
        } catch (java.io.IOException e) {
            fail("unexpected IOException 1: " + e);
        }
        try {
            System.setErr(newErr);
        } catch (SecurityException e) {
            return;
        }
        Thread t = new Thread("testNullPointerException") {
            public void run() {
                throw new NullPointerException();    
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        newErr.close();
        System.setErr(systemErr);
        byte buf[] = new byte[100];
        try {
            FileInputStream inp = new FileInputStream(file);
            inp.read(buf);
            inp.close();
        } catch (java.io.IOException e) {
            fail("unexpected IOException 2: " + e);
        }
        file.delete();
        assertTrue("Uncaught Exception message has not been printed",
                   new String(buf).indexOf("NullPointerException") > 0);
    }
}