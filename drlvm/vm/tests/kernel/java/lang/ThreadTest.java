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

package java.lang;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;
import org.apache.harmony.test.ReversibleSecurityManager;

/**
 * This class provides an implementation of J2SE v. 1.5 API Specification of
 * unit.java.lang.ThreadTest class.
 */
public class ThreadTest extends TestCase {

    private static final String INTERRUPTED_MESSAGE = 
        "thread has been unexpectedly interrupted";

    // max time interval to wait for some events in ms
    private static final long waitDuration = 60000;

    // waiting time for some event
    private long waitTime = 0;

    private boolean expired;
    private SecurityManager sm = null;
    
    private enum Action {WAIT, SLEEP, JOIN}
    
    private class RunProject extends Thread {
        private Team team;
        RunProject(Team t) {
            this.team = t;
        }

        public void run () {
            team.work();
        }
    }

    private class Team {
        public volatile int i = 0; 
        volatile boolean stopProject = false;
        
        public synchronized void work() {
            while (!stopProject) {
                i++;
            }
        }
        public void stopWork() {
            stopProject = true;
        }
    }

    private class TestThread extends Thread {
        
        public InterruptedException e = null;
        
        public void run() {
            try {
                synchronized (this) {
                    this.notify();
                    this.wait();
                }
            } catch(InterruptedException e) {
                this.e = e;
            }
        }
    }
    
    static class ThreadRunning extends Thread {
        volatile boolean stopWork = false;
        long startTime;
        public volatile int i = 0;

        ThreadRunning() {
            super();
        }
        
        ThreadRunning(String name) {
            super(name);
        }

        ThreadRunning(Runnable target, String name) {
            super(target, name);
        }

        ThreadRunning(ThreadGroup g, String name) {
            super(g, name);
        }

        ThreadRunning(ThreadGroup g, Runnable target) {
            super(g, target);
        }

        ThreadRunning(ThreadGroup g, Runnable target, String name) {
            super(g, target, name);
        }

        public void run () {
            startTime = System.currentTimeMillis();
            while (!stopWork) {
                i++;
            }
        }
       
        public long getStartTime() {
           return startTime;
        }
    }

    private class ThreadWaiting extends Thread {
        public volatile boolean started = false;
        private long millis;
        private int nanos;
        private Action action;
        private boolean exceptionReceived = false;
        private long startTime;
        private long endTime;
        private Object lock;
              
        ThreadWaiting(Action action, long millis, int nanos, Object lock) {
            this.millis = millis;
            this.nanos = nanos;
            this.action = action;
            this.lock = lock;
        }

        public void run () {
            switch (action) {
                case WAIT:
                    synchronized (lock) {
                        this.started = true;
                        lock.notify();
                    }
                    synchronized (this) {
                        try {
                            this.wait(millis, nanos);
                        } catch (InterruptedException e) {
                            exceptionReceived = true;
                        }
                    }
               case SLEEP:
                    try {
                        synchronized (lock) {
                            started = true;
                            lock.notify();
                        }
                        this.startTime = System.currentTimeMillis();
                        Thread.sleep(millis, nanos);
                        this.endTime = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        exceptionReceived = true;
                    }
                case JOIN:
                    try {
                        synchronized (lock) {
                            started = true;
                            lock.notify();
                        }
                        this.join(millis, nanos);
                    } catch (InterruptedException e) {
                        exceptionReceived = true;
                    }
            }
        }
        
        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }

    private class ThreadRunningAnotherThread extends Thread {
        int field = 0;
        volatile boolean stop = false;
        boolean childIsDaemon = false;
        Thread curThread = null;
        
        public ThreadRunningAnotherThread() {
            super();
        }

        public ThreadRunningAnotherThread(String name) {
            super(name);
        }

        public void run () {
            Thread child = new Thread();
            curThread = Thread.currentThread();
            childIsDaemon = child.isDaemon();
            while (!stop) {
                field++;
            }
        }
    }
    
    private static class ThreadYielding extends Thread {
        private int item;
        public static final int dim = 200;
        public static int list[] = new int[dim];
        private static int index = 0;

        public ThreadYielding(int item) {
            this.item = item;
        }

        private static synchronized int getNextIndex() {
            return index++;
        }

        public synchronized void setItem() {
            list[getNextIndex()] = this.item;
        }

        public void run () {
            for (int i = 0; i < dim / 2; i++) {
                setItem();
                Thread.yield();
            }
        }
    }
    
    class Square implements Runnable {
        volatile boolean stop = false;
        boolean once;
        int number;
        int squaredNumber;
        
        Square(int number) {
            this(number, false);
        }

        Square(int number, boolean once) {
            this.number = number;
            this.once = once;
        }

        public void run() {
            while (!stop) {
                squaredNumber = number * number;
                if (once) {
                    break;
                }
            }
        }
    }

    /**
     * Sleep for "interval" ms
     * @return true if waitTime is up
     */
    private boolean doSleep(int interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException while sleeping");
        }
        waitTime -= interval;
        return waitTime <= 0;
    }
    
    /**
     * Verify that the toString() method displays the thread's name,
     * priority and thread group. 
     */
    public void testToString() {
        Thread t = new Thread();
        String info = t.toString();
        String name = t.getName();
        assertTrue("thread's name is not displayed", info.indexOf(name) >= 0);
        String stringPriority = new Integer(t.getPriority()).toString();
        assertTrue("thread's priority is not displayed", 
                   info.indexOf("," + stringPriority + ",") > 0);
        String groupName = t.getThreadGroup().getName();
        assertTrue("thread's group is not displayed", info.indexOf(groupName) > 0);
    }

    /**
     * Thread()
     */
    public void testThread() {
        Thread t = new Thread();
        assertTrue("incorrect thread name", 
                   t.toString().indexOf("Thread-") >= 0);
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
    }

    /**
     * Verify that a thread created by a daemon thread is daemon
     */
    public void testThread_Daemon() {
        ThreadRunningAnotherThread t = new ThreadRunningAnotherThread();
        t.setDaemon(true);
        t.start();
        t.stop = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        assertTrue("the child thread of a daemon thread is non-daemon", 
                   t.childIsDaemon);
    }

    /**
     * Verify that a thread created by a non-daemon thread is not daemon
     */
    public void testThread_NotDaemon() {
        ThreadRunningAnotherThread t = new ThreadRunningAnotherThread();
        t.setDaemon(false);
        t.start();
        t.stop = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        assertFalse("the child thread of a non-daemon thread is daemon", 
                    t.childIsDaemon);
    }

    /**
     * Thread(Runnable)
     */
    public void testThreadRunnable() {
        Square s = new Square(25);
        Thread t = new Thread(s);
        t.start();
        waitTime = waitDuration;
        while (s.squaredNumber == 0 && !(expired = doSleep(10))) {
        }
        assertEquals("incorrect thread name", 0, t.getName().indexOf("Thread-"));
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
        s.stop = true;
        assertEquals("thread has not run", 625, s.squaredNumber);
    }

    /**
     * Thread(Runnable, String)
     */
    public void testThreadRunnableString() {
        Square s = new Square(25);
        String name = "squaring";
        Thread t = new Thread(s, name);
        t.start();
        waitTime = waitDuration;
        while (s.squaredNumber == 0 && !(expired = doSleep(10))) {
        }
        assertEquals("incorrect thread name", name, t.getName());
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
        s.stop = true;
        assertEquals("thread has not run", 625, s.squaredNumber);
    }

    /**
     * Thread(Runnable, String)
     */
    public void testThreadRunnableString_NullNotNull() {
        String name = "newThread";
        ThreadRunning t = new ThreadRunning((Runnable) null, name);
        assertEquals("incorrect thread name", name, t.getName());
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
        t.start();
        // thread's run() method should be called if Runnable==null
        waitTime = waitDuration;
        while (t.i == 0 && !(expired = doSleep(10))) {
        }
        t.stopWork = true;
        assertTrue("thread's run() method has not started", t.i != 0);
    }

    /**
     * Thread(String)
     */
    public void testThreadString() {
        String name = "threadString";
        Thread t = new Thread(name);
        assertTrue("incorrect thread name", 
                   t.toString().indexOf(name) >= 0);
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
    }

    /**
     * Verify creating a thread with the null name.
     * NullPointerException should be thrown.
     */
    public void testThreadStringNull() {
        String threadName = null;
        try {
            new Thread(threadName);
            fail ("NullPointerException should be thrown when creating "
                  + "a thread with null name");
        } catch (NullPointerException e) {
            return;
        }
    }

    /**
     * Thread(ThreadGroup, Runnable)
     */
    public void testThreadThreadGroupRunnable() {
        Square s = new Square(25);
        ThreadGroup tg = new ThreadGroup("newGroup");
        Thread t = new Thread(tg, s);
        t.start();
        waitTime = waitDuration;
        while (s.squaredNumber == 0 && !(expired = doSleep(10))) {
        }
        assertEquals("incorrect thread name", 0, t.getName().indexOf("Thread-"));
        assertSame("incorrect thread group", tg, t.getThreadGroup());
        s.stop = true;
        assertEquals("thread has not run", 625, s.squaredNumber);
    }

    /**
     * Thread(ThreadGroup, Runnable) where both arguments are null
     */
    public void testThreadThreadGroupRunnable_NullNull() {
        ThreadRunning t = new ThreadRunning((ThreadGroup) null, (Runnable) null);
        assertEquals("incorrect thread name", 0, t.getName().indexOf("Thread-"));
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
        t.start();
        // thread's run() method should be called if Runnable==null
        waitTime = waitDuration;
        while (t.i == 0 && !(expired = doSleep(10))) {
        }
        t.stopWork = true;
        assertTrue("thread's run() method has not started", t.i != 0);
    }

    /**
     * Thread(ThreadGroup, Runnable, String)
     */
    public void testThreadThreadGroupRunnableString() {
        ThreadGroup tg = new ThreadGroup("newGroup");
        String name = "t1";
        Square s = new Square(25);
        Thread t = new Thread(tg, s, name);
        t.start();
        waitTime = waitDuration;
        while (s.squaredNumber == 0 && !(expired = doSleep(10))) {
        }
        assertEquals("incorrect thread name", 0, t.getName().indexOf(name));
        assertSame("incorrect thread group", tg, t.getThreadGroup());
        s.stop = true;
        assertEquals("thread has not run", 625, s.squaredNumber);
    }

    /**
     * Thread(ThreadGroup, Runnable, String) where all arguments are null
     */
    public void testThreadThreadGroupRunnableString_NullNullNull() {
        try {
            new Thread(null, null, null);
            fail("NullPointerException has not been thrown");
        } catch (NullPointerException e) {
            return;
        }
    }

    /**
     * Thread(ThreadGroup, Runnable, String) where both
     * ThreadGroup and Runnable are null
     */
    public void testThreadThreadGroupRunnableString_NullNullNotNull() {
        String name = "t1";
        ThreadRunning t1 = new ThreadRunning(null, null, name);
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t1.getThreadGroup());
        t1.start();
        // thread's run() method should be called if Runnable==null
        waitTime = waitDuration;
        while (t1.i == 0 && !(expired = doSleep(10))) {
        }
        t1.stopWork = true;
        assertTrue("thread's run() method has not started", t1.i != 0);
    }

    /**
     * Thread(ThreadGroup, Runnable, String) where ThreadGroup is null
     */
    public void testThreadThreadGroupRunnableString_NullNotNullNotNull() {
        String name = "t1";
        Square s = new Square(25);
        Thread t = new Thread(null, s, name);
        t.start();
        waitTime = waitDuration;
        while (s.squaredNumber == 0 && !(expired = doSleep(10))) {
        }
        assertEquals("incorrect thread name", 0, t.getName().indexOf(name));
        assertSame("incorrect thread group", 
                   Thread.currentThread().getThreadGroup(), t.getThreadGroup());
        s.stop = true;
        assertEquals("thread has not run", 625, s.squaredNumber);
    }

    /**
     * Thread(ThreadGroup, Runnable, String, long)
     */
    public void testThreadThreadGroupRunnableStringlong() {
        ThreadGroup tg = new ThreadGroup("newGroup");
        String name = "t1";
        Square s = new Square(25);
        Thread t = new Thread(tg, s, name, 0);
        t.start();
        waitTime = waitDuration;
        StackTraceElement ste[] = t.getStackTrace();
        while (ste.length == 0 && !(expired = doSleep(10))) {
            ste = t.getStackTrace();
        }
        s.stop = true;
        if (expired) {
            fail("stack dump of thread t1 is empty");
        }
    }

    /**
     * Thread(ThreadGroup, Runnable, String, long)
     */
    public void testThreadThreadGroupRunnableStringlong_Long_MAX_VALUE() {
        ThreadGroup tg = new ThreadGroup("newGroup");
        String name = "t1";
        Square s = new Square(25);
        StackTraceElement ste[] = null; 
        try {
            Thread t;
            try {
                t = new Thread(tg, s, name, Long.MAX_VALUE);
            } catch (OutOfMemoryError e) {
                // fall back to default stack size if can't allocate
                // Long.MAX_VALUE bytes for stack
                t = new Thread(tg, s, name, 0);
            }
            t.start();
            waitTime = waitDuration;
            ste = t.getStackTrace();
            while (ste.length == 0 && !(expired = doSleep(10))) {
                ste = t.getStackTrace();
            }
            s.stop = true;
            if (expired) {
                fail("stack dump of thread t1 is empty");
            }
        } catch (OutOfMemoryError er) {
            fail("OutOfMemoryError when stack size is Long.MAX_VALUE");
        }
    }

    /**
     * Thread(ThreadGroup, String)
     */
    public void testThreadThreadGroupString() {
        String name = "newThread";
        ThreadGroup tg = new ThreadGroup("newGroup");
        Thread t = new Thread(tg, name);
        assertEquals("incorrect thread name", name, t.getName());
        assertSame("incorrect thread group", tg, t.getThreadGroup());
    }

    /**
     * Get active threads count; should be > 0
     */
    public void testActiveCount() {
        int activeCount = Thread.activeCount();
        assertTrue("The active threads count must be >0.", activeCount > 0);
    }

    /**
     * Verify currentThread()
     */
    public void testCurrentThread() {
        String name = "runThread";
        ThreadRunningAnotherThread t = new ThreadRunningAnotherThread(name);
        t.start();
        waitTime = waitDuration;
        while (t.curThread == null && !(expired = doSleep(10))) {
        }
        assertEquals("incorect current thread name", name, t.curThread.getName());
        t.stop = true;
    }

    /**
     * Verify currentThread()
     */
    public void testCurrentThread_Main() {
        String name = "ain";
        Thread t = Thread.currentThread();
        assertTrue("incorect current thread name", t.getName().indexOf(name) > 0);
    }

    public void testEnumerate() {
        ThreadRunning t1 = new ThreadRunning("ttt1");
        t1.start();
        ThreadRunning t2 = new ThreadRunning("ttt2");
        t2.start();
        ThreadGroup tg1 = new ThreadGroup("tg1");
        ThreadRunning t11 = new ThreadRunning(tg1, "ttt11");
        t11.start();
        ThreadRunning t12 = new ThreadRunning(tg1, "ttt12");
        t12.start();
        ThreadGroup tg12 = new ThreadGroup(tg1, "tg12");
        ThreadRunning t121 = new ThreadRunning(tg12, "ttt121");
        t121.start();
        ThreadRunning t122 = new ThreadRunning(tg12, "ttt122");
        t122.start();
        // estimate dimension as 6 created threads 
        // plus 10 for some other threads 
        int estimateLength = 16;
        Thread list[];
        int count;
        while (true) {
            list = new Thread[estimateLength];
            count = Thread.enumerate(list);
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
        t1.stopWork = true;
        t2.stopWork = true;
        t11.stopWork = true;
        t12.stopWork = true;
        t121.stopWork = true;
        t122.stopWork = true;        
        assertEquals("some threads are missed", 6, enumerateCount);
    }

    /**
     * Test for holdsLock(Object obj)
     */
    public void testHoldsLock_False() {
        Object lock = new Object();
        assertFalse("lock should not be held", Thread.holdsLock(lock));
    }

    /**
     * Test for holdsLock(Object obj)
     */
    public void testHoldsLock_True() {
        Object lock = new Object();
        synchronized (lock) {
            assertTrue("lock should be held", Thread.holdsLock(lock));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                fail(INTERRUPTED_MESSAGE);
            }
            assertTrue("lock should be held after sleeping",
                       Thread.holdsLock(lock));
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                fail(INTERRUPTED_MESSAGE);
            }
            assertTrue("lock should be obtained after waiting",
                      Thread.holdsLock(lock));
        }
        assertFalse("lock should not be held", Thread.holdsLock(lock));
    }
    
    /**
     * Test for holdsLock(null)
     */
    public void testHoldsLock_Null() {
        try {
            Thread.holdsLock(null);
            fail("NullPointerException has not been thrown");
        } catch (NullPointerException e) {
            return;
        }
    }

    /**
     * Verify that interrupt status is cleared by the interrupted()
     */
    public void testInterrupted() {
        class ThreadInterrupt extends Thread {
            private boolean interrupted1 = false;
            private boolean interrupted2;
            
            public void run() {
                interrupt();
                interrupted1 = Thread.interrupted();
                interrupted2 = Thread.interrupted();
            }
        };
        ThreadInterrupt t = new ThreadInterrupt();
        t.start();
        for (waitTime = waitDuration; !t.interrupted1 && !(expired = doSleep(10));) {
        }
        assertTrue("interrupt status has not changed to true", t.interrupted1);
        assertFalse("interrupt status has not changed to false", t.interrupted2);
    }

    /**
     * Test for void sleep(long)
     */
    public void testSleeplong() {
        Object lock = new Object();
        long millis = 2000;
        ThreadWaiting tW = new ThreadWaiting(Action.SLEEP, millis, 0, lock);
        try {
            synchronized (lock) {
                tW.start();
                while (!tW.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        try {
            tW.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        long duration = tW.getEndTime() - tW.getStartTime();
        // we allow the test to wait 2.5% less
        long atLeast = millis - 50;
        assertTrue("thread has not slept enough: expected " + atLeast
                + " but was " + duration,
                   duration >= atLeast);
    }

    /**
     * Test for void sleep(long, int)
     */
    public void testSleeplongint() {
        Object lock = new Object();
        long millis = 2000;
        int nanos = 123456;
        ThreadWaiting tW = new ThreadWaiting(Action.SLEEP, millis, nanos, lock);
        try {
            synchronized (lock) {
                tW.start();
                while (!tW.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
        	fail(INTERRUPTED_MESSAGE);
        }
        try {
            tW.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        long duration = tW.getEndTime() - tW.getStartTime();
        duration *= 1000000; // nano
        // we allow the test to wait 2.5% less
        long atLeast = (millis - 50) * 1000000;
        assertTrue("thread has not slept enough: expected " + atLeast 
            + " but was " + duration,
            duration >= atLeast);
    }

    /**
     * Test for void yield()
     */
    public void testYield() {   
        ThreadYielding t1 = new ThreadYielding(1);
        ThreadYielding t2 = new ThreadYielding(2);
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        int oneCount = 0;
        int threadNum = ThreadYielding.dim;
        for (int i = 0; i < threadNum; i++) {
            if (ThreadYielding.list[i] == 1) {
                oneCount++;
            }
        }
        // We suppose that threads t1 and t2 alternate with each other.
        // The might be a case when some another thread (not t2) runs 
        // while t1 is yelding. In this case the 'list' might start with 1s 
        // and end with 2s and look like threads does not alternate.  
        // We cannot treat this as failure nevertheless.
        // We just make sure that both threads have finished successfully.
        assertTrue("threads have not finished successfully", 
            oneCount == threadNum / 2);
    }
    
    /**
     * Test for checkAccess when there is a SecurityManager
     */
    public void testCheckAccess() {
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new ReversibleSecurityManager());
        Thread t = new Thread();
        try {
            t.checkAccess();
        } finally {
            System.setSecurityManager(sm);
        }
    }

    public void testDestroy() {
        Thread t = new Thread();
        try {
            t.destroy();
            fail("the destroy method should not be implemented");
        } catch (NoSuchMethodError er) {
            return;
        }
    }

    /**
     * Get context ClassLoader of a newly created thread 
     */
    public void testGetContextClassLoader() {
        Thread t = new Thread();
        assertSame("improper ClassLoader",
                   Thread.currentThread().getContextClassLoader(),
                   t.getContextClassLoader());
    }
    
    /**
     * Get context ClassLoader of main thread 
     */
    public void testGetContextClassLoader_Main() {
        ClassLoader cl = null;

        // find the root ThreadGroup
        ThreadGroup parent = new ThreadGroup(Thread.currentThread().getThreadGroup(),
                "Temporary");
        ThreadGroup newParent = parent.getParent();
        while (newParent != null) {
            parent = newParent;
            newParent = parent.getParent();
        }
        
        // enumerate threads and select "main" thread
        int threadsCount = parent.activeCount() + 1;
        int count;
        Thread[] liveThreads;
        while (true) {
            liveThreads = new Thread[threadsCount];
            count = parent.enumerate(liveThreads);
            if (count == threadsCount) {
                threadsCount *= 2;
            } else {
                break;
            }
        }
        for (int i = 0; i < count; i++) {
            if (liveThreads[i].toString().indexOf("ain]") > 0) {
                cl = liveThreads[i].getContextClassLoader();
                break;
            }
        }
        assertSame("improper ClassLoader", cl, ClassLoader.getSystemClassLoader());
    }

    /**
     * Check that IDs of different threads differ
     */
    public void testGetIdUnique() {
        Thread t1 = new Thread("thread1");
        Thread t2 = new Thread("thread2");
        assertTrue("Thread id must be unique", t1.getId() != t2.getId());
    }

    /**
     * Check that ID of a thread does not change
     */
    public void testGetIdUnchanged() {
        ThreadRunning t1 = new ThreadRunning();
        long tIdNew = t1.getId();
        t1.start();
        waitTime = waitDuration;
        while (t1.i == 0 && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("thread has not started");
        }
        long tIdRun = t1.getId();
        assertEquals("Thread ID after start should not change", tIdNew, tIdRun);
        t1.stopWork = true;
        try {
            t1.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        long tIdTerm = t1.getId();
        assertEquals("Thread ID after termination should not change", tIdRun, tIdTerm);
    }

    /**
     * Verify the getName() method for a thread created with the default name.
     * It should start with "Thread-".
     */
    public void testGetNameDefault() {
        Thread t = new Thread();
        String name = t.getName();
        assertEquals("Thread name must start with 'Thread-'", 0, name.indexOf("Thread-"));       
    }

    /**
     * Verify the getName() method for a thread created with the given name.
     */
    public void testGetName() {
        Thread t = new Thread("newThread");
        String name = t.getName();
        assertEquals("newThread", name);       
    }

    /**
     * Verify the getPriority() method for a newly created thread.
     */
    public void testGetPriority() {
        Thread t = new Thread();
        int p = t.getPriority();
        assertTrue("The thread's priority is out of range",
                   Thread.MIN_PRIORITY <= p && p <= Thread.MAX_PRIORITY);
    }

    /**
     * Get the stack trace of a thread.
     * Should be empty for new and terminated threads.
     * Should not be empty for running threads.
     */
    public void testGetStackTrace() {
        ThreadRunning tR = new ThreadRunning();
        
        // get stack trace of a new thread
        StackTraceElement ste[] = tR.getStackTrace();
        assertEquals("stack dump of a new thread is not empty", ste.length, 0);
        tR.start();

        // get stack trace of a running thread
        waitTime = waitDuration;
        do {
            ste = tR.getStackTrace();
        } while (ste.length == 0 && !(expired = doSleep(10)));
        if (expired) {
            fail("stack dump of a running thread is empty");
        } else {
            assertTrue("incorrect length", ste.length >= 1);
/*
             // commented: sometimes it returns Thread.runImpl 
             assertEquals("incorrect class name", 
                         "java.lang.ThreadTest$ThreadRunning", 
                         ste[0].getClassName());
            assertEquals("incorrect method name", 
                         "run", ste[0].getMethodName());
*/
        }
        
        // get stack trace of a terminated thread
        tR.stopWork = true;
        try {
            tR.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        waitTime = waitDuration;
        do {
            ste = tR.getStackTrace();
        } while (ste.length != 0 && !(expired = doSleep(10)));
        if (expired) {
            fail("stack dump of a terminated thread is not empty");
        }
    }

    /**
     * Verify getAllStackTraces()
     */
    public void testGetAllStackTraces() {
        StackTraceElement ste[] = null;
        ThreadRunning tR = new ThreadRunning("MyThread");
        tR.start();

        Map<Thread, StackTraceElement[]> m = Thread.getAllStackTraces();
        tR.stopWork = true;
        assertTrue("getAllStackTraces() returned an empty Map", m.size() > 0);

        // verify stack traces of all threads
        Set<Thread> threadSet = m.keySet();
        for (Thread t : threadSet) {
            ste = m.get(t);
            assertNotNull("improper stack trace for thread " + t, ste);
        }
    }

    /**
     * Get the thread group of a thread created in the current thread's
     * thread group.
     */
    public void testGetThreadGroup() {
        Thread t = new Thread();
        ThreadGroup threadGroup = t.getThreadGroup();
        ThreadGroup curThreadGroup = Thread.currentThread().getThreadGroup();
        assertEquals("incorrect value returned by getThreadGroup()",
                     curThreadGroup, threadGroup);
    }

    /**
     * Get the thread group of a thread created in the specified thread group.
     */
    public void testGetThreadGroup1() {
        ThreadGroup tg = new ThreadGroup("group1");
        Thread t = new Thread(tg, "t1");
        ThreadGroup threadGroup = t.getThreadGroup();
        assertEquals("incorrect value returned by getThreadGroup()",
                     tg, threadGroup);
    }

    /**
     * Get the thread group of a dead thread.
     */
    public void testGetThreadGroup_DeadThread() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        t.stopWork = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        assertNull("Thread group of a dead thread must be null",
                   t.getThreadGroup());
    }

    /**
     * Get the state of a blocked thread.
     */
     public void testGetStateBlocked() {
        Team team = new Team();
        RunProject pr1 = new RunProject(team);
        pr1.start();
        waitTime = waitDuration;
        while (team.i == 0 && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("pr1 has not been started");
        }
        RunProject pr2 = new RunProject(team);
        pr2.start();
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = pr2.getState();
        } while (!state.equals(Thread.State.BLOCKED) && !(expired = doSleep(10)));
        team.stopWork();
        if (expired) {
            fail("BLOCKED state has not been set");
        }
    }
    
    /**
     * Get the state of a new thread.
     */
    public void testGetStateNew() {
        ThreadRunning tR = new ThreadRunning();
        Thread.State state = tR.getState();
        assertEquals(Thread.State.NEW, state);
    }

    /**
     * Get the state of a new thread.
     */
    public void testGetStateNew1() {
        Square s = new Square(15);
        Thread t = new Thread(s);
        assertEquals(Thread.State.NEW, t.getState());
    }

    /**
     * Get the state of a runnable thread.
     */
    public void testGetStateRunnable() {
        ThreadRunning tR = new ThreadRunning();
        tR.start();
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = tR.getState();
        } while (!state.equals(Thread.State.RUNNABLE) && !(expired = doSleep(10)));
        tR.stopWork = true;
        if (expired) {
            fail("RUNNABLE state has not been set");
        }
    }

    /**
     * Get the state of a runnable thread.
     */
    public void testGetStateRunnable1() {
        Square s = new Square(15);
        Thread t = new Thread(s);
        t.start();
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = t.getState();
        } while (!state.equals(Thread.State.RUNNABLE) && !(expired = doSleep(10)));
        s.stop = true;
        if (expired) {
            fail("RUNNABLE state has not been set");
        }
    }

    /**
     * Get the state of a terminated thread.
     */
     public void testGetStateTerminated() {
        ThreadRunning tR = new ThreadRunning();
        tR.start();
        tR.stopWork = true;
        try {
            tR.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = tR.getState();
        } while (!state.equals(Thread.State.TERMINATED) && !(expired = doSleep(10)));
        if (expired) {
            fail("TERMINATED state has not been set");
        }
    }

    /**
     * Get the state of a terminated thread.
     */
     public void testGetStateTerminated1() {
        Square s = new Square(15);
        Thread tR = new Thread(s);
        tR.start();
        s.stop = true;
        try {
            tR.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        Thread.State state = tR.getState();
        waitTime = waitDuration;
        while (!state.equals(Thread.State.TERMINATED) && !(expired = doSleep(10))) {
            state = tR.getState();
        }
        if (expired) {
            fail("TERMINATED state has not been set");
        }
    }

    /**
     * Get the state of a timed waiting thread.
     */
     public void testGetStateTimedWaiting() {
        Object lock = new Object();
        ThreadWaiting tW = new ThreadWaiting(Action.WAIT, 6000, 0, lock);
        try {
            synchronized (lock) {
                tW.start();
                while (!tW.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = tW.getState();
        } while (!state.equals(Thread.State.TIMED_WAITING) && !(expired = doSleep(10)));
        synchronized (tW) {
            tW.notify();
        }
        if (expired) {    
            fail("TIMED_WAITING state has not been set");
        }
    }

    /**
     * Get the state of a waiting thread.
     */
     public void testGetStateWaiting() {
        Object lock = new Object();
        ThreadWaiting tW = new ThreadWaiting(Action.WAIT, 0, 0, lock);
        try {
            synchronized (lock) {
                tW.start();
                while (!tW.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = tW.getState();
        } while (!state.equals(Thread.State.WAITING) && !(expired = doSleep(10)));
        synchronized (tW) {
            tW.notify();
        }
        if (expired) {
            fail("WAITING state has not been set");
        }
    }

    class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        public boolean wasCalled = false;

        public void uncaughtException(Thread t, Throwable e) {
            wasCalled = true;
        }
    }

    /**
     * Test for getUncaughtExceptionHandler()
     */
    public void testGetUncaughtExceptionHandler() {
        ThreadGroup tg = new ThreadGroup("test thread group");
        Thread t = new Thread(tg, "test thread");
        Thread.UncaughtExceptionHandler hndlr = t.getUncaughtExceptionHandler();
        assertSame("Thread's thread group is expected to be a handler",
                   tg, hndlr);
    }
   
    /**
     * Test getUncaughtExceptionHandler() for a terminated thread
     */
     public void testGetUncaughtExceptionHandler_Null() {
        ThreadGroup tg = new ThreadGroup("test thread group");
        Thread t = new Thread(tg, "test thread");
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        Thread.State state;
        waitTime = waitDuration;
        do {
            state = t.getState();
        } while (!state.equals(Thread.State.TERMINATED) && !(expired = doSleep(10)));
        if (expired) {
            fail("TERMINATED state has not been set");
        }
        assertNull("handler should be null for a terminated thread",
                   t.getUncaughtExceptionHandler());
    }

    /**
     * Test for setUncaughtExceptionHandler()
     */
    public void testSetUncaughtExceptionHandler() {
        ThreadGroup tg = new ThreadGroup("test thread group");
        Thread t = new Thread(tg, "test thread");
        ExceptionHandler eh = new ExceptionHandler();
        t.setUncaughtExceptionHandler(eh);
        assertSame("the handler has not been set",
                   eh, t.getUncaughtExceptionHandler());
    }
   
    /**
     * Test for setUncaughtExceptionHandler(null)
     */
    public void testSetUncaughtExceptionHandler_Null() {
        ThreadGroup tg = new ThreadGroup("test thread group");
        Thread t = new Thread(tg, "test thread");
        t.setUncaughtExceptionHandler(null);
        assertSame("Thread's thread group is expected to be a handler",
                   tg, t.getUncaughtExceptionHandler());
    }
   
    /**
     * Test set/get DefaultUncaughtExceptionHandler()
     */
    public void testSetDefaultUncaughtExceptionHandler() {
        ExceptionHandler eh = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(eh);
        assertSame("the default handler has not been set",
                   eh, Thread.getDefaultUncaughtExceptionHandler());
    }
    
    /**
     * Test set/get DefaultUncaughtExceptionHandler(null)
     */
    public void testSetDefaultUncaughtExceptionHandler_Null() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        assertNull("default handler should be null",
                   Thread.getDefaultUncaughtExceptionHandler());
    }

    /**
     * Interrupt a newly created thread
     */
    public void testInterrupt_New() {
        Thread t  = new Thread();
        t.interrupt();
        waitTime = waitDuration;
        while (!t.isInterrupted() && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("interrupt status has not changed to true");
        }
    }

    /**
     * Interrupt a running thread
     */
    public void testInterrupt_RunningThread() {
        ThreadRunning t  = new ThreadRunning();
        t.start();
        waitTime = waitDuration;
        while (t.i == 0 && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("unexpected: thread's run() method has not started");
        }
        t.interrupt();
        waitTime = waitDuration;
        while (!t.isInterrupted() && !(expired = doSleep(10))) {
        }
        t.stopWork = true;
        if (expired) {
            fail("interrupt status has not changed to true");
        }
    }

    /**
     * Interrupt the current thread
     */
    public void testInterrupt_CurrentThread() {
        Thread t = new Thread() {
            public void run() {
                interrupt();   
            }
        };
        t.start();
        waitTime = waitDuration;
        while (!t.isInterrupted() && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("interrupt status has not changed to true");
        }
    }

    /**
     * Interrupt a terminated thread
     */
    public void testInterrupt_Terminated() {
        ThreadRunning t  = new ThreadRunning();
        t.start();
        waitTime = waitDuration;
        while (t.i == 0 && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("thread' run() method has not started");
        }
        t.stopWork = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        t.interrupt();
        assertTrue("interrupt status has not changed to true", 
                   t.isInterrupted());
    }

    /**
     * Interrupt a joining thread
     */
    public void testInterrupt_Joining() {
        Object lock = new Object();
        ThreadWaiting t = new ThreadWaiting(Action.JOIN, 10000, 0, lock);
        try {
            synchronized (lock) {
                t.start();
                while (!t.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        t.interrupt();
        waitTime = waitDuration;
        while (!t.exceptionReceived && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("joining thread has not received the InterruptedException");
        }
        assertFalse("interrupt status has not been cleared", 
                   t.isInterrupted());
    }

    /**
     * Interrupt a sleeping thread
     */
    public void testInterrupt_Sleeping() {
        Object lock = new Object();
        ThreadWaiting t = new ThreadWaiting(Action.SLEEP, 10000, 0, lock);
        try {
            synchronized (lock) {
                t.start();
                while (!t.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        waitTime = waitDuration;
        while (!t.isAlive() && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("thread has not started for " + waitDuration + "ms");
        }
        t.interrupt();
        waitTime = waitDuration;
        while (!t.exceptionReceived && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("sleeping thread has not received the InterruptedException");
        }
        assertFalse("interrupt status has not been cleared", 
                   t.isInterrupted());
    }

    /**
     * Interrupt a waiting thread
     */
    public void testInterrupt_Waiting() {
        Object lock = new Object();
        ThreadWaiting t = new ThreadWaiting(Action.WAIT, 10000, 0, lock);
        try {
            synchronized (lock) {
                t.start();
                while (!t.started) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        waitTime = waitDuration;
        Thread.State ts = t.getState();
        while (ts != Thread.State.TIMED_WAITING && !(expired = doSleep(10))) {
            ts = t.getState();
        }
        if (expired) {
            fail("TIMED_WAITING state has not been reached");
        }
        t.interrupt();
        waitTime = waitDuration;
        while (!t.exceptionReceived && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("waiting thread has not received the InterruptedException");
        }
        assertFalse("interrupt status has not been cleared", 
                t.isInterrupted());
    }

    static final int COUNT = 100;
    volatile int base;
    
    /**
     * Check that interrupt and notify happen exactly once for each
     * <code>notify()</code> and <code>interrupt()</code> call.
     */
    public void testInterrupt_Staging() {
        ThreadStaging t = new ThreadStaging();

        base = 0;
        t.start();

        try {
            for (base = 0; base < COUNT; ) {
                synchronized (t) {
                    t.waitStage("notify");
                    t.notify();

                    t.waitStage("interrupt");
                    t.interrupt();
                }
            }
        }  catch (InterruptedException e) {
            fail("Unexpected exception: " + e);
        }
    }

    private class ThreadStaging extends Thread {
        static final long TIMEOUT = 100;
        int stage;
        
        public void run() {
            for (stage = 0; stage < COUNT; ) {

                try {
                    waitBase();
                } catch (InterruptedException e) {
                    fail("Unexpected exception: " + e);
                }
                assertEquals("Stages are not synchronized after interrupt", stage, base);

                try {
                    waitBase();
                    fail("The thread should be interrupted");
                } catch (InterruptedException e) {
                    assertEquals("Stages are not synchronized after interrupt", stage, base);
                    continue;
                }
                fail("The thread should be interrupted by (InterruptedException");
            }
        }

        public synchronized void waitStage(String stageName) throws InterruptedException {
            for (int i = 0; (base == stage) && (i < COUNT); i++) {
                wait(TIMEOUT);
            }
            assertEquals("waitFor " + stageName + ": stages are not synchronized before", stage, ++base);
        }

        synchronized void waitBase() throws InterruptedException {
            stage++;
            notify();
            wait(TIMEOUT);
        }
    }

    /**
     * Verify that the current thread is alive
     */
    public void testIsAliveCurrent() {
        assertTrue("The current thread must be alive!", Thread.currentThread().isAlive());
    }
    
    /**
     * Verify that a thread is alive just after start
     */
    public void testIsAlive() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        assertTrue("The started thread must be alive!", t.isAlive());
        t.stopWork = true;
    }
    
    /**
     * Verify the isAlive() method for a newly created, running
     * and finished thread
     */
    public void testIsAlive1() {
        TestThread t = new TestThread();
        assertFalse("Newly created and not started thread must not be alive!",
                    t.isAlive());
        try {
            synchronized (t) {
                t.start();
                t.wait();
            }
            if (!t.isAlive()) {
                if (t.e != null) {
                    fail("The thread was interrupted");
                }
                fail("The thread must be alive");
            }
            synchronized (t) {
                t.notify();
            }
            waitTime = waitDuration;
            while (t.isAlive() && !(expired = doSleep(10))) {
                t.join();
            }
            if (expired) {
                fail("thread has not finished for " + waitDuration + "ms");
            }
        } catch (InterruptedException e) {
            fail("Current thread was interrupted");
        }
    }

    /**
     * Verify the isAlive() method for a few threads
     */
    public void testIsAlive2() {
        ThreadRunning t1 = new ThreadRunning();
        ThreadRunning t2 = new ThreadRunning();
        ThreadRunning t3 = new ThreadRunning();
        assertFalse("t1 has not started and must not be alive", t1.isAlive());
        assertFalse("t2 has not started and must not be alive", t2.isAlive());
        assertFalse("t3 has not started and must not be alive", t3.isAlive());
        t1.start();
        t2.start();
        t3.start();
        assertTrue("t1 must be alive", t1.isAlive());
        assertTrue("t2 must be alive", t2.isAlive());
        assertTrue("t3 must be alive", t3.isAlive());
        t1.stopWork = true;
        t2.stopWork = true;
        t3.stopWork = true;
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            fail("threads have been interrupted");          
        }
        assertFalse("t1 has finished and must not be alive", t1.isAlive());
        assertFalse("t2 has finished and must not be alive", t2.isAlive());
        assertFalse("t3 has finished and must not be alive", t3.isAlive());
    }
    
    public void testIsDaemonFalse() {
        Thread t = new Thread();
        assertFalse("thread should not be daemon", t.isDaemon());        
    }

    public void testIsDaemonTrue() {
        Thread t = new Thread();
        t.setDaemon(true);
        assertTrue("thread should be daemon", t.isDaemon());        
    }

    /** 
     * Check that interrupt status is not affected by isInterrupted()
     */
    public void testIsInterrupted() {
        ThreadRunning t  = new ThreadRunning();
        t.start();
        waitTime = waitDuration;
        while (t.i == 0 && !(expired = doSleep(10))) {
        }
        if (expired) {
            fail("unexpected: thread's run() method has not started");
        }
        t.interrupt();
        waitTime = waitDuration;
        while (!t.isInterrupted() && !(expired = doSleep(10))) {
        }
        t.stopWork = true;
        if (expired) {
            fail("interrupt status has not changed to true"); 
        }
        assertTrue("interrupt status has been cleared by the previous call", 
                   t.isInterrupted());
    }

    /**
     * Test for void join(long)
     */
    public void testJoinlong() {
        long millis = 2000;
        ThreadRunning t = new ThreadRunning();
        t.start();
        long joinStartTime = 0;
        long curTime = 0;
        try {
            joinStartTime = System.currentTimeMillis();
            t.join(millis);
            curTime = System.currentTimeMillis();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        long duration = curTime - joinStartTime;
        // we allow the test to wait 2.5% less
        long atLeast = (millis - 50);
        t.stopWork = true;
        assertTrue("join should wait for at least " + atLeast + 
                " but waited for " + duration,
                duration >= atLeast);
        }

    /**
     * Test for void join(long, int)
     */
    public void testJoinlongint() {
        long millis = 2000;
        int nanos = 999999;
        ThreadRunning t = new ThreadRunning();
        t.start();
        long joinStartTime = 0;
        long curTime = 0;
        try {
            joinStartTime = System.currentTimeMillis();
            t.join(millis, nanos);
            curTime = System.currentTimeMillis();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        long duration = 1000000 * (curTime - joinStartTime);
        // we allow the test to wait 2.5% less
        long atLeast = (millis - 50) * 1000000 + nanos;
        t.stopWork = true;
        assertTrue("join should wait for at least " + atLeast + 
            " but waited for " + duration,
            duration >= atLeast);
    }

    /**
     * Test for run(). Should do nothing.
     */
    public void testRun() {
        Thread t = new Thread();
        Thread.State tsBefore = t.getState();
        t.run();
        Thread.State tsAfter = t.getState();
        assertEquals("run() should do nothing", tsBefore, tsAfter);        
    }

    /**
     * Test for run(). Should call run() of a Runnable object.
     */
    public void testRun_Runnable() {
        Square s = new Square(25, true);
        Thread t = new Thread(s);
        t.run();
        assertEquals("thread has not run", 625, s.squaredNumber);
    }

    public void testSetContextClassLoader() {
        Class c = null;
        try {        
            c = Class.forName("java.lang.String");
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException for java.lang.String");           
        }
        ClassLoader cl = c.getClassLoader();
        ThreadRunningAnotherThread t = new ThreadRunningAnotherThread();
        ClassLoader clt = t.getContextClassLoader();
        t.setContextClassLoader(cl);
        ClassLoader clNew = t.getContextClassLoader();
        assertSame("incorrect ClassLoader has been set",
                   cl, clNew);
        assertNotSame("ClassLoader has not changed", clt, clNew);
    }

    /**
     * Make a thread daemon
     */
    public void testSetDaemon() {
        Thread t = new Thread();
        assertFalse("Assert 0: the newly created thread must not be daemon",
                    t.isDaemon());
        t.setDaemon(true);
        assertTrue("Assert 1: the thread must be daemon", t.isDaemon());
    }

    /**
     * Try to make a running thread daemon
     */
    public void testSetDaemonLiveThread() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        try {
            t.setDaemon(true);
            t.stopWork = true;
            try {
                t.join();
            } catch (InterruptedException e) {
                fail(INTERRUPTED_MESSAGE);
            }
            fail("IllegalThreadStateException has not been thrown");            
        } catch (IllegalThreadStateException e) {
            return;
        }
    }

    /**
     * Make a dead thread daemon
     */
    public void testSetDaemonDeadThread() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        t.stopWork = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        boolean threadDaemonStatus = t.isDaemon();
        try {
            t.setDaemon(!threadDaemonStatus);
        } catch (IllegalThreadStateException e) {
            fail("IllegalThreadStateException should not be thrown"
                 + " for a dead thread");
        }
    }

    /**
     * Verify the setName() method 
     */
    public void testSetName() {
        Thread thread = new Thread();
        String newName = "qwerty";
        thread.setName(newName);
        assertEquals("setName() has not set the new name",
                     newName, thread.getName());
    }

    /**
     * Verify the setName(null) method 
     */
    public void testSetNameNull() {
        Thread thread = new Thread();
        try {
            thread.setName(null);
        } catch (NullPointerException e) {
            return;
        }
        fail("setName() should not accept null names");
    }

    /**
     * Verify the setName() method when a SecurityManager is set. 
     */
    public void testSetName_CheckAccess() {
        sm = System.getSecurityManager();
        System.setSecurityManager(new ReversibleSecurityManager());
        Thread thread = new Thread();
        String newName = "qwerty";
        thread.setName(newName);
        String gotName = thread.getName();
        System.setSecurityManager(sm);
        assertEquals("setName() has not set the new name",
                     newName, gotName);
    }

    /**
     * Verify the setPriority() method to a dead thread. 
     * NullPointerException is expected
     */
    public void testSetPriorityDeadThread() {
        ThreadGroup tg = new ThreadGroup("group1");
        int maxTGPriority = Thread.MAX_PRIORITY - 1;
        tg.setMaxPriority(maxTGPriority);
        ThreadRunning t = new ThreadRunning(tg, "running");
        t.start();
        t.stopWork = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);          
        }
        int newPriority = Thread.MAX_PRIORITY;
        try {
            t.setPriority(newPriority);
            fail("NullPointerException has not been thrown");          
        } catch (NullPointerException e) {
            return;
        }
    }

    /**
     * Verify the setPriority() method with new priority higher 
     * than the maximum permitted priority for the thread's group. 
     */
    public void testSetPriorityGreaterMax() {
        ThreadGroup tg = new ThreadGroup("group1");
        int maxTGPriority = Thread.MAX_PRIORITY - 1;
        tg.setMaxPriority(maxTGPriority);
        Thread t = new Thread(tg, "t");
        t.setPriority(Thread.MAX_PRIORITY);
        assertEquals(maxTGPriority, t.getPriority());
    }

    /**
     * Verify the setPriority() method with new priority lower 
     * than the current one. 
     */
    public void testSetPriorityLower() {
        Thread t = new Thread();
        int p = t.getPriority();
        int newPriority = p - 1;
        if (newPriority >= Thread.MIN_PRIORITY) {
            t.setPriority(newPriority);
            assertEquals(newPriority, t.getPriority());
        }
    }

    /**
     * Verify the setPriority() method with new priority out of the legal range. 
     */
    public void testSetPriorityOutOfRange() {
        Thread t = new Thread();
        try {
            t.setPriority(Thread.MAX_PRIORITY + 2);
            fail("IllegalArgumentException should be thrown when setting "
                 + "illegal priority");
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    /**
     * Verify the setPriority() method when a SecurityManager is set. 
     */
    public void testSetPriority_CheckAccess() {
        sm = System.getSecurityManager();
        System.setSecurityManager(new ReversibleSecurityManager());
        Thread t = new Thread();
        int p = t.getPriority();
        t.setPriority(p);
        int newP = t.getPriority();
        System.setSecurityManager(sm);
        assertEquals(p, newP);
    }

    /**
     * Start the already started thread
     */
    public void testStart_Started() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        try {
            t.start();
            t.stopWork = true;
            fail("IllegalThreadStateException is expected when starting " +
                 "a started thread");
        } catch (IllegalThreadStateException e) {
            t.stopWork = true;
        }
    }

    /**
     * Start the already finished thread
     */
    public void testStart_Finished() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        t.stopWork = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            fail(INTERRUPTED_MESSAGE);
        }
        try {
            t.start();
            fail("IllegalThreadStateException is expected when starting " +
                 "a finished thread");
        } catch (IllegalThreadStateException e) {
            return;
        }
    }
}
