/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.lang.management;

import java.lang.management.ManagementPermission;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Runtime type for {@link java.lang.management.ThreadMXBean}
 * 
 * @since 1.5
 */
public final class ThreadMXBeanImpl extends DynamicMXBeanImpl implements
        ThreadMXBean {

    private static ThreadMXBeanImpl instance = new ThreadMXBeanImpl();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    private ThreadMXBeanImpl() {
        setMBeanInfo(ManagementUtils.getMBeanInfo(ThreadMXBean.class.getName()));
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>ThreadMXBeanImpl</code> singleton.
     */
    static ThreadMXBeanImpl getInstance() {
        return instance;
    }

    /**
     * @return an array of the identifiers of every thread in the virtual
     *         machine that has been detected as currently being in a deadlock
     *         situation.
     * @see #findMonitorDeadlockedThreads()
     */
    private native long[] findMonitorDeadlockedThreadsImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#findMonitorDeadlockedThreads()
     */
    public long[] findMonitorDeadlockedThreads() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }
        return this.findMonitorDeadlockedThreadsImpl();
    }

    /**
     * @return the identifiers of all of the threads currently alive in the
     *         virtual machine.
     * @see #getAllThreadIds()
     */
    private native long[] getAllThreadIdsImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getAllThreadIds()
     */
    public long[] getAllThreadIds() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }
        return this.getAllThreadIdsImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getCurrentThreadCpuTime()
     */
    public long getCurrentThreadCpuTime() {
        return getThreadCpuTime(Thread.currentThread().getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getCurrentThreadUserTime()
     */
    public long getCurrentThreadUserTime() {
        return getThreadUserTime(Thread.currentThread().getId());
    }

    /**
     * @return the number of currently alive daemon threads.
     * @see #getDaemonThreadCount()
     */
    private native int getDaemonThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getDaemonThreadCount()
     */
    public int getDaemonThreadCount() {
        return this.getDaemonThreadCountImpl();
    }

    /**
     * @return the peak number of live threads
     * @see #getPeakThreadCount()
     */
    private native int getPeakThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getPeakThreadCount()
     */
    public int getPeakThreadCount() {
        return this.getPeakThreadCountImpl();
    }

    /**
     * @return the number of currently alive threads.
     * @see #getThreadCount()
     */
    private native int getThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadCount()
     */
    public int getThreadCount() {
        return this.getThreadCountImpl();
    }

    /**
     * @param id
     *            the identifier for a thread. Must be a positive number greater
     *            than zero.
     * @return on virtual machines where thread CPU timing is supported and
     *         enabled, and there is a living thread with identifier
     *         <code>id</code>, the number of nanoseconds CPU time used by
     *         the thread. On virtual machines where thread CPU timing is
     *         supported but not enabled, or where there is no living thread
     *         with identifier <code>id</code> present in the virtual machine,
     *         a value of <code>-1</code> is returned.
     * @see #getThreadCpuTime(long)
     */
    private native long getThreadCpuTimeImpl(long id);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadCpuTime(long)
     */
    public long getThreadCpuTime(long id) {
        // Validate input.
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Thread id must be greater than 0.");
        }

        long result = -1;
        if (isThreadCpuTimeSupported()) {
            if (isThreadCpuTimeEnabled()) {
                result = this.getThreadCpuTimeImpl(id);
            }
        } else {
            throw new UnsupportedOperationException(
                    "CPU time measurement is not supported on this virtual machine.");
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long)
     */
    public ThreadInfo getThreadInfo(long id) {
        return getThreadInfo(id, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long[])
     */
    public ThreadInfo[] getThreadInfo(long[] ids) {
        return getThreadInfo(ids, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long[], int)
     */
    public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }

        // Validate inputs
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] <= 0) {
                throw new IllegalArgumentException(
                        "Thread id must be greater than 0.");
            }
        }

        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "maxDepth value cannot be negative.");
        }

        // Create an array and populate with individual ThreadInfos
        ThreadInfo[] tis = new ThreadInfo[ids.length];
        for (int i = 0; i < ids.length; i++) {
            tis[i] = this.getThreadInfoImpl(ids[i], maxDepth);
        }
        return tis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadInfo(long, int)
     */
    public ThreadInfo getThreadInfo(long id, int maxDepth) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("monitor"));
        }

        // Validate inputs
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Thread id must be greater than 0.");
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "maxDepth value cannot be negative.");
        }
        return this.getThreadInfoImpl(id, maxDepth);
    }

    /**
     * Returns the corresponding Thread instance for a given thread id
     * 
     * @param id
     *            id of the thread (must be > 0)
     * @return null if thread with the id specified does not exist
     */
    private native Thread getThreadByIdImpl(long id);

    /**
     * Returns the object the thread is either blocked or waiting on
     * 
     * @param thread
     *            thread
     * @return null if thread not blocked on an object
     */
    private native Object getObjectThreadIsBlockedOnImpl(Thread thread);

    /**
     * Returns the thread owning an object
     * 
     * @param obj
     *            object
     * @return null if object not owned, else Thread owner
     */
    private native Thread getThreadOwningObjectImpl(Object obj);

    /**
     * Returns whether the thread is suspended or not
     * 
     * @param thread
     *            thread
     * @return true if Thread.suspend() has been called on the thread, otherwise
     *         false
     */
    private native boolean isSuspendedImpl(Thread thread);

    /**
     * Returns the number of times the thread has waited
     * 
     * @param thread
     *            thread
     * @return number of times the thread has waited
     * 
     */
    private native long getThreadWaitedCountImpl(Thread thread);

    /**
     * Returns the amount of time the thread has waited (in milliseconds)
     * 
     * @param thread
     *            thread
     * @return time (in milliseconds) the thread has waited, or -1 if this
     *         feature is not supported
     * 
     */
    private native long getThreadWaitedTimeImpl(Thread thread);

    /**
     * Returns the amount of time the thread has blocked (in milliseconds)
     * 
     * @param thread
     *            thread
     * @return time (in milliseconds) the thread has blocked, or -1 if this
     *         feature is not supported
     * 
     */
    private native long getThreadBlockedTimeImpl(Thread thread);

    /**
     * Returns the number of times the thread has blocked on a monitor
     * 
     * @param thread
     *            thread
     * @return number of times the thread has blocked
     * 
     */
    private native long getThreadBlockedCountImpl(Thread thread);

    /**
     * Create an instance of the ThreadInfo class
     * 
     * @param threadId
     * @param threadName
     * @param threadState
     * @param suspended
     * @param inNative
     * @param blockedCount
     * @param blockedTime
     * @param waitedCount
     * @param waitedTime
     * @param lockName
     * @param lockOwnerId
     * @param lockOwnerName
     * @param stackTrace
     * @return
     */
    private native ThreadInfo createThreadInfoImpl(long threadId,
            String threadName, Thread.State threadState, boolean suspended,
            boolean inNative, long blockedCount, long blockedTime,
            long waitedCount, long waitedTime, String lockName,
            long lockOwnerId, String lockOwnerName,
            StackTraceElement[] stackTrace);

    /*
     * Get together information for a thread and create an instance of the
     * ThreadInfo class
     * 
     * @param id thread id @param maxDepth maximum depth of the stack trace
     * @return an instance of ThreadInfo for valid thread ids, otherwise null
     * 
     */
    private ThreadInfo getThreadInfoImpl(long id, int maxDepth) {
        final Thread thread = getThreadByIdImpl(id);
        if (null == thread) {
            return null;
        }

        // Generic thread info
        long threadId = thread.getId();
        String threadName = thread.getName();
        Thread.State threadState = thread.getState();

        // Waited and blocked information
        long waitedTime = -1;
        long blockedTime = -1;
        // Waited and blocked times to be -1 if ThreadContentionMonitoring
        // is not supported
        if (isThreadContentionMonitoringSupported()
                && isThreadContentionMonitoringEnabled()) {
            waitedTime = getThreadWaitedTimeImpl(thread);
            blockedTime = getThreadBlockedTimeImpl(thread);
        }

        // Get together information about any locks involved
        // i.e. thread blocked or waiting on a lock
        // see ThreadInfo spec for values if neither blocked nor waiting
        String lockName = null;
        long lockOwnerId = -1;
        String lockOwnerName = null;

        Object lock = getObjectThreadIsBlockedOnImpl(thread);
        if (lock != null) {
            // the format of the name is dictated by the ThreadInfo spec
            lockName = lock.getClass().getName() + '@'
                    + Integer.toHexString(System.identityHashCode(lock));
            Thread threadOwningLock = getThreadOwningObjectImpl(lock);
            // Possible race conditions must be catered for
            if (threadOwningLock != null) {
                lockOwnerId = threadOwningLock.getId();
                lockOwnerName = threadOwningLock.getName();
            }// end if non-null thread owning lock
        }// end if non-null lock

        // Get the stack trace together.
        // Do we have to prune it for max depth?
        StackTraceElement[] stackTrace = AccessController
                .doPrivileged(new PrivilegedAction<StackTraceElement[]>() {
                    public StackTraceElement[] run() {
                        return thread.getStackTrace();
                    }// end method run
                });

        boolean isInNative = false;
        if (stackTrace.length > 0) {
            isInNative = stackTrace[0].isNativeMethod();
        }
        if ((maxDepth < Integer.MAX_VALUE) && (stackTrace.length > maxDepth)) {
            StackTraceElement[] newStackTrace = new StackTraceElement[maxDepth];
            for (int i = 0; i < newStackTrace.length; i++) {
                newStackTrace[i] = stackTrace[i];
            }
            stackTrace = newStackTrace;
        }

        // Ask our native to instantiate a ThreadInfo for us
        ThreadInfo ti = createThreadInfoImpl(threadId, threadName, threadState,
                isSuspendedImpl(thread), isInNative,
                getThreadBlockedCountImpl(thread), blockedTime,
                getThreadWaitedCountImpl(thread), waitedTime, lockName,
                lockOwnerId, lockOwnerName, stackTrace);
        return ti;
    }

    /**
     * @param id
     *            the identifier for a thread. Must be a positive number greater
     *            than zero.
     * @return on virtual machines where thread CPU timing is supported and
     *         enabled, and there is a living thread with identifier
     *         <code>id</code>, the number of nanoseconds CPU time used by
     *         the thread running in user mode. On virtual machines where thread
     *         CPU timing is supported but not enabled, or where there is no
     *         living thread with identifier <code>id</code> present in the
     *         virtual machine, a value of <code>-1</code> is returned.
     *         <p>
     *         If thread CPU timing was disabled when the thread was started
     *         then the virtual machine is free to choose any measurement start
     *         time between when the virtual machine started up and when thread
     *         CPU timing was enabled with a call to
     *         {@link #setThreadCpuTimeEnabled(boolean)}.
     *         </p>
     * @see #getThreadUserTime(long)
     */
    private native long getThreadUserTimeImpl(long id);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getThreadUserTime(long)
     */
    public long getThreadUserTime(long id) {
        // Validate input.
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Thread id must be greater than 0.");
        }

        long result = -1;
        if (isThreadCpuTimeSupported()) {
            if (isThreadCpuTimeEnabled()) {
                result = this.getThreadUserTimeImpl(id);
            }
        } else {
            throw new UnsupportedOperationException(
                    "CPU time measurement is not supported on this virtual machine.");
        }
        return result;
    }

    /**
     * @return the total number of started threads.
     * @see #getTotalStartedThreadCount()
     */
    private native long getTotalStartedThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#getTotalStartedThreadCount()
     */
    public long getTotalStartedThreadCount() {
        return this.getTotalStartedThreadCountImpl();
    }

    /**
     * @return <code>true</code> if CPU timing of the current thread is
     *         supported, otherwise <code>false</code>.
     * @see #isCurrentThreadCpuTimeSupported()
     */
    private native boolean isCurrentThreadCpuTimeSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isCurrentThreadCpuTimeSupported()
     */
    public boolean isCurrentThreadCpuTimeSupported() {
        return this.isCurrentThreadCpuTimeSupportedImpl();
    }

    /**
     * @return <code>true</code> if thread contention monitoring is enabled,
     *         <code>false</code> otherwise.
     * @see #isThreadContentionMonitoringEnabled()
     */
    private native boolean isThreadContentionMonitoringEnabledImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadContentionMonitoringEnabled()
     */
    public boolean isThreadContentionMonitoringEnabled() {
        if (!isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException(
                    "Thread contention monitoring is not supported on this virtual machine.");
        }
        return this.isThreadContentionMonitoringEnabledImpl();
    }

    /**
     * @return <code>true</code> if thread contention monitoring is supported,
     *         <code>false</code> otherwise.
     * @see #isThreadContentionMonitoringSupported()
     */
    private native boolean isThreadContentionMonitoringSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadContentionMonitoringSupported()
     */
    public boolean isThreadContentionMonitoringSupported() {
        return this.isThreadContentionMonitoringSupportedImpl();
    }

    /**
     * @return <code>true</code> if thread CPU timing is enabled,
     *         <code>false</code> otherwise.
     * @see #isThreadCpuTimeEnabled()
     */
    private native boolean isThreadCpuTimeEnabledImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadCpuTimeEnabled()
     */
    public boolean isThreadCpuTimeEnabled() {
        if (!isThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                    "Thread CPU timing is not supported on this virtual machine.");
        }
        return this.isThreadCpuTimeEnabledImpl();
    }

    /**
     * @return <code>true</code> if the virtual machine supports the CPU
     *         timing of threads, <code>false</code> otherwise.
     * @see #isThreadCpuTimeSupported()
     */
    private native boolean isThreadCpuTimeSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#isThreadCpuTimeSupported()
     */
    public boolean isThreadCpuTimeSupported() {
        return this.isThreadCpuTimeSupportedImpl();
    }

    /**
     * @see #resetPeakThreadCount()
     */
    private native void resetPeakThreadCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#resetPeakThreadCount()
     */
    public void resetPeakThreadCount() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.resetPeakThreadCountImpl();
    }

    /**
     * @param enable
     *            enable thread contention monitoring if <code>true</code>,
     *            otherwise disable thread contention monitoring.
     */
    private native void setThreadContentionMonitoringEnabledImpl(boolean enable);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#setThreadContentionMonitoringEnabled(boolean)
     */
    public void setThreadContentionMonitoringEnabled(boolean enable) {
        if (!isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException(
                    "Thread contention monitoring is not supported on this virtual machine.");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setThreadContentionMonitoringEnabledImpl(enable);
    }

    /**
     * @param enable
     *            enable thread CPU timing if <code>true</code>, otherwise
     *            disable thread CPU timing
     */
    private native void setThreadCpuTimeEnabledImpl(boolean enable);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ThreadMXBean#setThreadCpuTimeEnabled(boolean)
     */
    public void setThreadCpuTimeEnabled(boolean enable) {
        if (!isThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException(
                    "Thread CPU timing is not supported on this virtual machine.");
        }

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setThreadCpuTimeEnabledImpl(enable);
    }
}
