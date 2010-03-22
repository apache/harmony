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

import java.security.AccessController;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.harmony.lang.RuntimePermissionCollection;
import org.apache.harmony.security.fortress.SecurityUtils;
import org.apache.harmony.vm.VMStack;

/**
 * @com.intel.drl.spec_ref 
 */
public class Thread implements Runnable {

    /**
     * @com.intel.drl.spec_ref
     */
    public static final int MAX_PRIORITY = 10;

    /**
     * @com.intel.drl.spec_ref
     */
    public static final int MIN_PRIORITY = 1;

    /**
     * @com.intel.drl.spec_ref
     */
    public static final int NORM_PRIORITY = 5;

    /**
     * Indent string used to print stack trace
     */
    private static final String STACK_TRACE_INDENT = "    ";

    /**
     * This thread's thread group
     */
    ThreadGroup group;

    /**
     * This thread's context class loader
     */
    private ClassLoader contextClassLoader;

    /**
     * Indicates whether this thread was marked as daemon
     */
    private boolean daemon;

    /**
     * Thread's name
     */
    private String name;

    /**
     * Thread's priority
     */
    private int priority;

    /**
     * Stack size to be passed to VM for thread execution
     */
    private long stackSize;

    /**
     * Indicates if the thread was already started
     */
    boolean started = false;

    
    /**
     * Indicates if the thread is alive.
     */
    boolean isAlive = false;

    /**
     * Thread's target - a <code>Runnable</code> object whose <code>run</code>
     * method should be invoked
     */
    private Runnable target;


    /**
     * Uncaught exception handler for this thread
     */
    private UncaughtExceptionHandler exceptionHandler = null;

    /**
     * Default uncaught exception handler
     */
    private static UncaughtExceptionHandler defaultExceptionHandler = null;

    /**
     * Thread's ID
     */
    private long threadId;

    /**
     * Counter used to generate thread's ID
     */
    private static long threadOrdinalNum = 0;

    /**
     * Synchronization is done using internal lock.
     */
    Object lock = new Object();

    /**
     * used to generate a default thread name
     */
    private static final String THREAD = "Thread-";

    /**
     * System thread group for keeping helper threads.
     */
    static ThreadGroup systemThreadGroup = null;
    
    /**
     * Main thread group.
     */
    static ThreadGroup mainThreadGroup = null;

    /*
     * Number of threads that was created w/o garbage collection.
     */ 
    private static int currentGCWatermarkCount = 0;

    /*
     * Max number of threads to be created w/o GC, required collect dead Thread 
     * references.
     */
    private static final int GC_WATERMARK_MAX_COUNT = 700;

    /*
     * ThreadLocal values: local and inheritable
     *
     */
    ThreadLocal.Values localValues;
    ThreadLocal.Values inheritableValues;
    
    /**
     * @com.intel.drl.spec_ref
     */
    public Thread() {
        this(null, null, THREAD, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(Runnable target) {
        this(null, target, THREAD, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(Runnable target, String name) {
        this(null, target, name, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(String name) {
        this(null, null, name, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(ThreadGroup group, Runnable target) {
        this(group, target, THREAD, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }

    /**
     * Creates a new thread object for the thread attached to VM.     
     * The first attached thread is the main thread.
     *
     * @param group determines the thread group to place the thread in
     * @param name thread's name
     * @param nativeAddr address of the attached native thread
     * @param stackeSize size of the thread's stack
     * @param priority thread's priority
     * @param daemon true if the thread is daemon, false otherwise
     */
    Thread(ThreadGroup group, String name, long nativeAddr,
        long stackSize, int priority, boolean daemon) {

        ClassLoader contextLoader = null;
        
        if (group == null) {
            if (systemThreadGroup == null) {
                // This is main thread.
                systemThreadGroup = new ThreadGroup();
                mainThreadGroup = new ThreadGroup(systemThreadGroup, "main");
                group = mainThreadGroup;
            } else {
                group = mainThreadGroup;
            }
        }

        this.group = group;
        this.stackSize = stackSize;
        this.priority = priority;
        this.daemon = daemon;
        this.threadId = getNextThreadId();
        this.name = (name != null) ? name : THREAD + threadId; 
        // Each thread created from JNI has bootstrap class loader as
        // its context class loader. The only exception is the main thread
        // which has system class loader as its context class loader.
        this.contextClassLoader = contextLoader;
        this.target = null;
        // The thread is actually running.
        this.isAlive = true;
        this.started = true;

        ThreadWeakRef newRef = new ThreadWeakRef(this);
        newRef.setNativeAddr(nativeAddr);

        SecurityUtils.putContext(this, AccessController.getContext());
        // adding the thread to the thread group should be the last action
        group.add(this);

	Thread parent = currentThread();
        if (parent != null && parent.inheritableValues != null) {
            inheritableValues = new ThreadLocal.Values(parent.inheritableValues);
        } 

    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(ThreadGroup group, Runnable target, String name,
                  long stackSize) {

        Thread currentThread = VMThreadManager.currentThread();
        SecurityManager securityManager = System.getSecurityManager();
        
        ThreadGroup threadGroup = null;
        if (group != null) {
            if (securityManager != null) {
                securityManager.checkAccess(group);
            }
            threadGroup = group;
        } else if (securityManager != null) {
            threadGroup = securityManager.getThreadGroup();
        }
        if (threadGroup == null) {
            threadGroup = currentThread.group;
        }

        threadGroup.checkGroup();

        this.group = threadGroup;
        this.daemon = currentThread.daemon;
        this.contextClassLoader = currentThread.contextClassLoader;
        this.target = target;
        this.stackSize = stackSize;
        this.priority = currentThread.priority;
        this.threadId = getNextThreadId();
        // throws NullPointerException if the given name is null
        this.name = (name != THREAD) ? this.name = name.toString() :
            THREAD + threadId;


    
        checkGCWatermark();
        
        ThreadWeakRef oldRef = ThreadWeakRef.poll();
        ThreadWeakRef newRef = new ThreadWeakRef(this);

        long oldPointer = (oldRef == null) ? 0 : oldRef.getNativeAddr();
        long newPointer = VMThreadManager.init(this, newRef, oldPointer);
        if (newPointer == 0) {
            throw new OutOfMemoryError("Failed to create new thread");
        }
        newRef.setNativeAddr(newPointer);

        SecurityUtils.putContext(this, AccessController.getContext());
        checkAccess();

	Thread parent = currentThread();
        if (parent != null && parent.inheritableValues != null) {
            inheritableValues = new ThreadLocal.Values(parent.inheritableValues);
        } 

    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread(ThreadGroup group, String name) {
        this(group, null, name, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static int activeCount() {
        return currentThread().group.activeCount();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Thread currentThread() {
        return VMThreadManager.currentThread();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void dumpStack() {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        System.err.println("Stack trace");
        for (int i = 0; i < stack.length; i++) {
            System.err.println(STACK_TRACE_INDENT + stack[i]);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static int enumerate(Thread[] list) {
        return currentThread().group.enumerate(list);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static boolean holdsLock(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return VMThreadManager.holdsLock(object);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static boolean interrupted() {
        return VMThreadManager.isInterrupted();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void sleep(long millis) throws InterruptedException {
        sleep(millis, 0);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void sleep(long millis, int nanos)
        throws InterruptedException {
        if (millis < 0 || nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                "Arguments don't match the expected range!");
        }
        int status = VMThreadManager.sleep(millis, nanos);
        if (status == VMThreadManager.TM_ERROR_INTERRUPT) {
            throw new InterruptedException();        
        } else if (status != VMThreadManager.TM_ERROR_NONE) {
            throw new InternalError(
                "Thread Manager internal error " + status);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void yield() {
        int status = VMThreadManager.yield();
        if (status != VMThreadManager.TM_ERROR_NONE) {
            throw new InternalError(
                "Thread Manager internal error " + status);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void checkAccess() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkAccess(this);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public int countStackFrames() {
        return 0; //deprecated
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public void destroy() {
        // this method is not implemented
        throw new NoSuchMethodError();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager
                .checkPermission(RuntimePermissionCollection.GET_STACK_TRACE_PERMISSION);
            securityManager
                .checkPermission(RuntimePermissionCollection.MODIFY_THREAD_GROUP_PERMISSION);
        }
        
        // find the initial ThreadGroup in the tree
        ThreadGroup parent = new ThreadGroup(currentThread().getThreadGroup(), "Temporary");
        ThreadGroup newParent = parent.getParent();
        parent.destroy();
        while (newParent != null) {
            parent = newParent;
            newParent = parent.getParent();
        }
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
        Map<Thread, StackTraceElement[]> map = new HashMap<Thread, StackTraceElement[]>(count + 1);
        for (int i = 0; i < count; i++) {
            StackTraceElement[] ste = liveThreads[i].getStackTrace();
            if (ste.length != 0) {
                map.put(liveThreads[i], ste);
            }
        }
        return map;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public ClassLoader getContextClassLoader() {
        synchronized (lock) {
            // First, if the conditions
            //    1) there is a security manager
            //    2) the caller's class loader is not null
            //    3) the caller's class loader is not the same as or an
            //    ancestor of contextClassLoader
            // are satisfied we should perform a security check.
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                //the first condition is satisfied
                ClassLoader callerClassLoader = VMClassRegistry
                    .getClassLoader(VMStack.getCallerClass(0));
                if (callerClassLoader != null) {
                    //the second condition is satisfied
                    ClassLoader classLoader = contextClassLoader;
                    while (classLoader != null) {
                        if (classLoader == callerClassLoader) {
                            //the third condition is not satisfied
                            return contextClassLoader;
                        }
                        classLoader = classLoader.getParent();
                    }
                    //the third condition is satisfied
                    securityManager
                        .checkPermission(RuntimePermissionCollection.GET_CLASS_LOADER_PERMISSION);
                }
            }
            return contextClassLoader;
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final String getName() {
        return name;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public StackTraceElement[] getStackTrace() {
        if (currentThread() != this) {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager
                    .checkPermission(RuntimePermissionCollection.GET_STACK_TRACE_PERMISSION);
            }
        }
        StackTraceElement ste[] = VMStack.getThreadStackTrace(this);
        return ste != null ? ste : new StackTraceElement[0];
    }
    
    /**
     * @com.intel.drl.spec_ref
     */
    public final ThreadGroup getThreadGroup() {
        return group;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public long getId() {        
        return threadId;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void interrupt() {
        synchronized (lock) {
            checkAccess();
            int status = VMThreadManager.interrupt(this);
            if (status != VMThreadManager.TM_ERROR_NONE) {
                throw new InternalError(
                    "Thread Manager internal error " + status);
            }
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final boolean isAlive() {
        synchronized (lock) {
            return this.isAlive;
        }
    }


    /**
     * @com.intel.drl.spec_ref
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public boolean isInterrupted() {
        return VMThreadManager.isInterrupted(this);
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final synchronized void join() throws InterruptedException {
        while (isAlive()) {
            wait();
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final synchronized void join(long millis) throws InterruptedException {
        if (millis == 0) {
            join();
        } else {
            long end = System.currentTimeMillis() + millis;
            while(isAlive()) {
                wait(millis);
                millis = end - System.currentTimeMillis();
                if (millis <= 0) {
                   break;
                }
            }
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final synchronized void join(long millis, int nanos)
        throws InterruptedException {
        if (millis < 0 || nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException();
        } else if (millis == 0 && nanos == 0) {
            join();
        } else {
            long end = System.nanoTime() + 1000000*millis + (long)nanos;
            long rest;
            while (isAlive()) {
                wait(millis, nanos);
                rest = end - System.nanoTime();
                if (rest <= 0)
                   break;
                nanos  = (int)(rest % 1000000);
                millis = rest / 1000000;
            }
        }
    }

    /**
     *  Note that this is unsnchronized - the assumption is that
     *  hythread does the synchronization for us
     *  
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void resume() {
            checkAccess();
            int status = VMThreadManager.resume(this);
            if (status != VMThreadManager.TM_ERROR_NONE) {
                throw new InternalError(
                    "Thread Manager internal error " + status);
            }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    void runImpl() {
        synchronized (lock) {
            this.isAlive = true;
            this.started = true;
            lock.notifyAll();
        }

        run();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void setContextClassLoader(ClassLoader classLoader) {
        synchronized (lock) {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager
                    .checkPermission(RuntimePermissionCollection.SET_CONTEXT_CLASS_LOADER_PERMISSION);
            }
            contextClassLoader = classLoader;
        }
    }

    /**
     * @com.intel.drl.spec_ref We assume that 'active thread' means the same as
     *                         'alive thread'.
     */
    public final void setDaemon(boolean daemon) {
        synchronized (lock) {
            checkAccess();
            if (isAlive()) {
                throw new IllegalThreadStateException();
            }
            this.daemon = daemon;
        }
    }

    /**
     * @com.intel.drl.spec_ref New name should not be <code>null</code>.
     * @throws NullPointerException if new name is <code>null</code>
     */
    public final void setName(String name) {
        checkAccess();
        // throws NullPointerException if the given name is null
        this.name = name.toString();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public final void setPriority(int priority) {
        checkAccess();
        if (priority > MAX_PRIORITY || priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("Wrong Thread priority value");
        }
        ThreadGroup threadGroup = group;
        this.priority = (priority > threadGroup.maxPriority)
            ? threadGroup.maxPriority : priority;
        int status = VMThreadManager.setPriority(this, this.priority);
        if (status != VMThreadManager.TM_ERROR_NONE) {
            //throw new InternalError("Thread Manager internal error " + status);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public synchronized void start() {
        synchronized (lock) {
            if (started) {
                //this thread was started
                throw new IllegalThreadStateException(
                        "This thread was already started!");
            }

            // adding the thread to the thread group
            group.add(this);

            
            if (VMThreadManager.start(this, stackSize, daemon, priority) != 0) {
                throw new OutOfMemoryError("Failed to create new thread");
            } 
            

            // wjw -- why are we *waiting* for a child thread to actually start running?
            // this *guarantees* two context switches
            // nothing in j.l.Thread spec says we have to do this
            // my guess is that this actually masks an underlying race condition that we need to fix.
           
            boolean interrupted = false;
            while(!this.started) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }

            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Performs premortal actions. First it processes uncaught exception if any.
     * Second removes current thread from its thread group.
     * VM calls this method when current thread is detaching from VM.
     * 
     * @param uncaughtException uncaught exception or null
     */
    void detach(Throwable uncaughtException) {
        try {
            if (uncaughtException != null) {
                getUncaughtExceptionHandler().uncaughtException(this, uncaughtException);
            }
        } finally {
            group.remove(this);
            synchronized(this) {
                isAlive = false;
                notifyAll();
            }
        }
    }

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public Thread.State  getState() {

	boolean dead = false;
	synchronized(lock) {
		if(started && !isAlive() ) dead = true;
	}
	if (dead) return State.TERMINATED;
     
        int state = (VMThreadManager.getState(this));

        if (0 != (state & VMThreadManager.TM_THREAD_STATE_TERMINATED)) {         
            return State.TERMINATED;
        } else if  (0 != (state & VMThreadManager.TM_THREAD_STATE_WAITING_WITH_TIMEOUT)) {
            return State.TIMED_WAITING;
        } else if (0 != (state & VMThreadManager.TM_THREAD_STATE_WAITING) 
                || 0 != (state & VMThreadManager.TM_THREAD_STATE_PARKED)) {
            return State.WAITING;
        } else if (0 != (state & VMThreadManager.TM_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER)) {
            return State.BLOCKED;

        } else if (0 != (state & VMThreadManager.TM_THREAD_STATE_RUNNABLE)) {
            return State.RUNNABLE;

        //TODO track down all situations where a thread is really in RUNNABLE state 
        // but TM_THREAD_STATE_RUNNABLE is not set.  In the meantime, leave the following
        // TM_THREAD_STATE_ALIVE test as it is.
        } else if (0 != (state & VMThreadManager.TM_THREAD_STATE_ALIVE)) {
            return State.RUNNABLE;
        } else { 
            return State.NEW;  // by deduction, if its none of the above
        }
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void stop() {
        synchronized (lock) {
        	if (isAlive()) {
                stop(new ThreadDeath());
        	}
        }
    }

    /**
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void stop(Throwable throwable) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkAccess(this);
            if (currentThread() != this || !(throwable instanceof ThreadDeath)) {
                securityManager.checkPermission(
                    RuntimePermissionCollection.STOP_THREAD_PERMISSION);
            }
        }
        if (throwable == null) {
            throw new NullPointerException("The argument is null!");
        }
        synchronized (lock) {
            if (isAlive()) {
                int status = VMThreadManager.stop(this, throwable);
                if (status != VMThreadManager.TM_ERROR_NONE) {
                    throw new InternalError(
                        "Thread Manager internal error " + status);
                }
            }
        }
    }

    /**
     *  Note that this is unsnchronized - the assumption is that
     *  hythread does the synchronization for us
     *  
     * @com.intel.drl.spec_ref
     * @deprecated
     */
    public final void suspend() {
        checkAccess();

        int status = VMThreadManager.suspend(this);
        if (status != VMThreadManager.TM_ERROR_NONE) {
            throw new InternalError("Thread Manager internal error " + status);
        }
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public String toString() {
        ThreadGroup threadGroup = group;
        return "Thread[" + name + "," + priority + ","
            + ( (threadGroup == null) ? "" : threadGroup.name) + "]";
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultExceptionHandler;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(RuntimePermissionCollection.SET_DEFAULT_UNCAUGHT_EXCEPTION_HANDLER_PERMISSION);
        }
        defaultExceptionHandler = eh;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        if (exceptionHandler != null) {
            return exceptionHandler;
        }
        return getThreadGroup();
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(RuntimePermissionCollection.MODIFY_THREAD_PERMISSION);
        }
        exceptionHandler = eh;
    }

    /**
     * generates a unique thread ID
     */
    private static synchronized long getNextThreadId() 
    {
        return ++threadOrdinalNum;
    }

    /*
     * Checks if more then GC_WATERMARK_MAX_COUNT threads was created and calls
     * System.gc() to ensure that dead thread references was collected.
     */
    private void checkGCWatermark() 
    {
        if (++currentGCWatermarkCount % GC_WATERMARK_MAX_COUNT == 0) 
        {
            System.gc();
        }
    }
    
    /**
     * @com.intel.drl.spec_ref
     */
    public static interface UncaughtExceptionHandler 
    {

        /**
         * @com.intel.drl.spec_ref
         */
        void uncaughtException(Thread t, Throwable e);
    }
}
