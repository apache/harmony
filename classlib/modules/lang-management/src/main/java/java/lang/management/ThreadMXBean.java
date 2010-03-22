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

package java.lang.management;

/**
 * <p>
 * <code>ThreadMXBean</code> is an interface used by the management
 * system to access thread-related properties.
 * </p>
 * <p>
 * <code>ObjectName</code>: java.lang:type=Threading
 * </p>
 * 
 * @since 1.5
 */
public interface ThreadMXBean {
    
    /**
     * <p>
     * Finds cycles of threads that are in deadlock waiting to acquire and
     * object monitor.
     * </p>
     * 
     * @return An array of thread IDs of deadlocked.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("monitor")</code>.
     * @see Thread#getId()
     */
    long[] findMonitorDeadlockedThreads();

    /**
     * <p>
     * The ID of all currently live threads.
     * </p>
     * 
     * @return An array of thread IDs.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("monitor")</code>.
     * @see Thread#getId()
     */
    long[] getAllThreadIds();

    /**
     * <p>
     * The total CPU time (in nanoseconds) for the current thread. This is
     * convenience method for {@link #getThreadCpuTime(long) getThreadCpuTime}<code>(Thread.currentThread().getId());</code>.
     * </p>
     * 
     * @return The total CPU time for the current thread.
     * @throws UnsupportedOperationException if this is not supported.
     */
    long getCurrentThreadCpuTime();

    /**
     * <p>
     * The total user time (in nanoseconds) for the current thread. This is
     * convenience method for {@link #getThreadUserTime(long) getThreadUserTime}<code>(Thread.currentThread().getId());</code>.
     * </p>
     * 
     * @return The total user time for the current thread.
     * @throws UnsupportedOperationException if this is not supported.
     */
    long getCurrentThreadUserTime();

    /**
     * <p>
     * The current number of live daemon threads.
     * </p>
     * 
     * @return The number of daemon threads.
     */
    int getDaemonThreadCount();

    /**
     * <p>
     * The peak number of live threads since JVM start or the last reset.
     * </p>
     * 
     * @return The peak number of threads.
     * @see #resetPeakThreadCount()
     */
    int getPeakThreadCount();

    /**
     * <p>
     * The current number of live threads (daemon and non-daemon).
     * </p>
     * 
     * @return The number of threads.
     */
    int getThreadCount();

    /**
     * <p>
     * The total CPU time (in nanoseconds) for the given thread.
     * </p>
     * 
     * @param id The ID of the thread to get the CPU time for.
     * @return The total CPU time for the current thread or -1 if the thread is
     *         not alive or measurement is not enabled.
     * @throws IllegalArgumentException if <code>id</code> is less than or
     *         equal to 0.
     * @throws UnsupportedOperationException if this is not supported.
     */
    long getThreadCpuTime(long id);

    /**
     * <p>
     * The thread information for the given thread with a stack trace depth of
     * zero.
     * </p>
     * 
     * @param id The ID of the thread to get information for.
     * @return A {@link ThreadInfo} instance representing the information of the
     *         given thread or <code>null</code> if the thread is not alive or
     *         doesn't exist.
     * @throws IllegalArgumentException if <code>id</code> is less than or
     *         equal to 0.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("monitor")</code>.
     */
    ThreadInfo getThreadInfo(long id);

    /**
     * <p>
     * The thread information for the given threads with a stack trace depth of
     * zero.
     * </p>
     * 
     * @param ids An array of IDs of the threads to get information for.
     * @return An array of {@link ThreadInfo} instance representing the
     *         information of the given threads.
     * @throws IllegalArgumentException if and element in <code>ids</code> is
     *         less than or equal to 0.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("monitor")</code>.
     */
    ThreadInfo[] getThreadInfo(long[] ids);

    /**
     * <p>
     * The thread information for the given threads, which is qualified by a
     * maximum stack trace depth.
     * </p>
     * 
     * @param ids An array of IDs of the threads to get information for.
     * @param maxDepth The maximum depth of the stack trace to return in the
     *        {@link ThreadInfo}. If zero, then an empty array is stored. If
     *        {@link Integer#MAX_VALUE}, then the entire stack trace is
     *        returned.
     * @return An array of {@link ThreadInfo} instance representing the
     *         information of the given threads.
     * @throws IllegalArgumentException if and element in <code>ids</code> is
     *         less than or equal to 0.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("monitor")</code>.
     */
    ThreadInfo[] getThreadInfo(long[] ids, int maxDepth);

    /**
     * <p>
     * The thread information for the given thread with a stack trace depth of
     * zero.
     * </p>
     * 
     * @param id The ID of the thread to get information for.
     * @param maxDepth The maximum depth of the stack trace to return in the
     *        {@link ThreadInfo}. If zero, then an empty array is stored. If
     *        {@link Integer#MAX_VALUE}, then the entire stack trace is
     *        returned.
     * @return A {@link ThreadInfo} instance representing the information of the
     *         given thread or <code>null</code> if the thread is not alive or
     *         doesn't exist.
     * @throws IllegalArgumentException if <code>id</code> is less than or
     *         equal to 0.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("monitor")</code>.
     */
    ThreadInfo getThreadInfo(long id, int maxDepth);

    /**
     * <p>
     * The total user time (in nanoseconds) for the given thread.
     * </p>
     * 
     * @param id The ID of the thread to get the user time for.
     * @return The total user time for the current thread or -1 if the thread is
     *         not alive or measurement is not enabled.
     * @throws IllegalArgumentException if <code>id</code> is less than or
     *         equal to 0.
     * @throws UnsupportedOperationException if this is not supported.
     */
    long getThreadUserTime(long id);

    /**
     * <p>
     * The total number of threads that have been created and started within the
     * JVM.
     * </p>
     * 
     * @return The total number of created and started threads.
     */
    long getTotalStartedThreadCount();

    /**
     * <p>
     * Indicates whether or not current thread CPU time monitoring is supported.
     * </p>
     * 
     * @return <code>true</code> if supported, otherwise <code>false</code>.
     */
    boolean isCurrentThreadCpuTimeSupported();

    /**
     * <p>
     * Indicates whether or not thread contention monitoring is enabled.
     * </p>
     * 
     * @return <code>true</code> if enabled, otherwise <code>false</code>.
     */
    boolean isThreadContentionMonitoringEnabled();

    /**
     * <p>
     * Indicates whether or not thread contention monitoring is supported.
     * </p>
     * 
     * @return <code>true</code> if supported, otherwise <code>false</code>.
     */
    boolean isThreadContentionMonitoringSupported();

    /**
     * <p>
     * Indicates whether or not thread CPU time monitoring is enabled.
     * </p>
     * 
     * @return <code>true</code> if enabled, otherwise <code>false</code>.
     */
    boolean isThreadCpuTimeEnabled();

    /**
     * <p>
     * Indicates whether or not thread CPU time monitoring is supported.
     * </p>
     * 
     * @return <code>true</code> if supported, otherwise <code>false</code>.
     */
    boolean isThreadCpuTimeSupported();

    /**
     * <p>
     * Resets the peak thread count to the current thread count.
     * </p>
     * 
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     */
    void resetPeakThreadCount();

    /**
     * <p>
     * Enables or disables thread contention monitoring.
     * </p>
     * 
     * @param enable <code>true</code> to enable, <code>false</code> to
     *        disable.
     * @throws UnsupportedOperationException if this is not supported.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     * @see #isThreadContentionMonitoringSupported()
     */
    void setThreadContentionMonitoringEnabled(boolean enable);

    /**
     * <p>
     * Enables or disables thread CPU time monitoring.
     * </p>
     * 
     * @param enable <code>true</code> to enable, <code>false</code> to
     *        disable.
     * @throws UnsupportedOperationException if this is not supported.
     * @throws SecurityException if caller doesn't have
     *         <code>ManagementPermission("control")</code>.
     * @see #isThreadCpuTimeSupported()
     */
    void setThreadCpuTimeEnabled(boolean enable);
}
