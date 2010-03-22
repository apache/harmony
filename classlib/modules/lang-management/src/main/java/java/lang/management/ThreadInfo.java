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

import javax.management.openmbean.CompositeData;

import org.apache.harmony.lang.management.ManagementUtils;


/**
 * <p>
 * Thread information.
 * </p>
 * 
 * @since 1.5
 */
public class ThreadInfo {

    /**
     * Receives a {@link CompositeData}representing a <code>ThreadInfo</code>
     * object and attempts to return the root <code>ThreadInfo</code>
     * instance.
     * 
     * @param cd
     *            a <code>CompositeDate</code> that represents a
     *            <code>ThreadInfo</code>.
     * @return if <code>cd</code> is non- <code>null</code>, returns a new
     *         instance of <code>ThreadInfo</code>. If <code>cd</code> is
     *         <code>null</code>, returns <code>null</code>.
     * @throws IllegalArgumentException
     *             if argument <code>cd</code> does not correspond to a
     *             <code>ThreadInfo</code> with the following attributes:
     *             <ul>
     *             <li><code>threadId</code>(<code>java.lang.Long</code>)
     *             <li><code>threadName</code>(
     *             <code>java.lang.String</code>)
     *             <li><code>threadState</code>(
     *             <code>java.lang.String</code>)
     *             <li><code>suspended</code>(
     *             <code>java.lang.Boolean</code>)
     *             <li><code>inNative</code>(<code>java.lang.Boolean</code>)
     *             <li><code>blockedCount</code>(
     *             <code>java.lang.Long</code>)
     *             <li><code>blockedTime</code>(<code>java.lang.Long</code>)
     *             <li><code>waitedCount</code>(<code>java.lang.Long</code>)
     *             <li><code>waitedTime<code> (<code>java.lang.Long</code>)
     *             <li><code>lockName</code> (<code>java.lang.String</code>)
     *             <li><code>lockOwnerId</code> (<code>java.lang.Long</code>)
     *             <li><code>lockOwnerName</code> (<code>java.lang.String</code>)
     *             <li><code>stackTrace</code> (<code>javax.management.openmbean.CompositeData[]</code>)
     *             </ul>
     *             Each element of the <code>stackTrace</code> array must 
     *             correspond to a <code>java.lang.StackTraceElement</code>
     *             and have the following attributes :
     *             <ul>
     *             <li><code>className</code> (<code>java.lang.String</code>)
     *             <li><code>methodName</code> (<code>java.lang.String</code>)
     *             <li><code>fileName</code> (<code>java.lang.String</code>)
     *             <li><code>lineNumber</code> (<code>java.lang.Integer</code>)
     *             <li><code>nativeMethod</code> (<code>java.lang.Boolean</code>)
     *             </ul>
     */
    public static ThreadInfo from(CompositeData cd) {
        ThreadInfo result = null;

        if (cd != null) {
            // Does cd meet the necessary criteria to create a new
            // ThreadInfo ? If not then exit on an IllegalArgumentException
            ManagementUtils.verifyFieldNumber(cd, 13);
            String[] attributeNames = { "threadId", "threadName",
                    "threadState", "suspended", "inNative", "blockedCount",
                    "blockedTime", "waitedCount", "waitedTime", "lockName",
                    "lockOwnerId", "lockOwnerName", "stackTrace" };
            ManagementUtils.verifyFieldNames(cd, attributeNames);
            String[] attributeTypes = { "java.lang.Long", "java.lang.String",
                    "java.lang.String", "java.lang.Boolean",
                    "java.lang.Boolean", "java.lang.Long", "java.lang.Long",
                    "java.lang.Long", "java.lang.Long", "java.lang.String",
                    "java.lang.Long", "java.lang.String",
                    (new CompositeData[0]).getClass().getName() };
            ManagementUtils
                    .verifyFieldTypes(cd, attributeNames, attributeTypes);

            // Extract the values of the attributes and use them to construct
            // a new ThreadInfo.
            Object[] attributeVals = cd.getAll(attributeNames);
            long threadIdVal = ((Long) attributeVals[0]).longValue();
            String threadNameVal = (String) attributeVals[1];
            String threadStateStringVal = (String) attributeVals[2];

            // Verify that threadStateStringVal contains a string that can be
            // successfully used to create a Thread.State.
            Thread.State threadStateVal = null;
            try {
                threadStateVal = Thread.State.valueOf(threadStateStringVal);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "CompositeData contains an unexpected threadState value.",
                        e);
            }

            boolean suspendedVal = ((Boolean) attributeVals[3]).booleanValue();
            boolean inNativeVal = ((Boolean) attributeVals[4]).booleanValue();
            long blockedCountVal = ((Long) attributeVals[5]).longValue();
            long blockedTimeVal = ((Long) attributeVals[6]).longValue();
            long waitedCountVal = ((Long) attributeVals[7]).longValue();
            long waitedTimeVal = ((Long) attributeVals[8]).longValue();
            String lockNameVal = attributeVals[9] != null ? (String) attributeVals[9]
                    : null;
            long lockOwnerIdVal = ((Long) attributeVals[10]).longValue();
            String lockOwnerNameVal = attributeVals[11] != null ? (String) attributeVals[11]
                    : null;
            CompositeData[] stackTraceDataVal = (CompositeData[]) attributeVals[12];
            StackTraceElement[] stackTraceVals = getStackTracesFromCompositeData(stackTraceDataVal);

            result = new ThreadInfo(threadIdVal, threadNameVal, threadStateVal,
                    suspendedVal, inNativeVal, blockedCountVal, blockedTimeVal,
                    waitedCountVal, waitedTimeVal, lockNameVal, lockOwnerIdVal,
                    lockOwnerNameVal, stackTraceVals);
        }// end if cd is not null

        return result;
    }

    /**
     * Returns an array of {@link StackTraceElement}whose elements have been
     * created from the corresponding elements of the
     * <code>stackTraceDataVal</code> argument.
     * 
     * @param stackTraceDataVal
     *            an array of {@link CompositeData}objects, each one
     *            representing a <code>StackTraceElement</code>.
     * @return an array of <code>StackTraceElement</code> objects built using
     *         the data discovered in the corresponding elements of
     *         <code>stackTraceDataVal</code>.
     * @throws IllegalArgumentException
     *             if any of the elements of <code>stackTraceDataVal</code> do
     *             not correspond to a <code>StackTraceElement</code> with the
     *             following attributes:
     *             <ul>
     *             <li><code>className</code>(<code>java.lang.String</code>)
     *             <li><code>methodName</code>(
     *             <code>java.lang.String</code>)
     *             <li><code>fileName</code>(<code>java.lang.String</code>)
     *             <li><code>lineNumbercode> (<code>java.lang.Integer</code>)
     *             <li><code>nativeMethod</code> (<code>java.lang.Boolean</code>)
     *             </ul>
     */
    private static StackTraceElement[] getStackTracesFromCompositeData(
            CompositeData[] stackTraceDataVal) {
        StackTraceElement[] result = new StackTraceElement[stackTraceDataVal.length];

        for (int i = 0; i < stackTraceDataVal.length; i++) {
            CompositeData data = stackTraceDataVal[i];

            // Verify the element
            ManagementUtils.verifyFieldNumber(data, 5);
            String[] attributeNames = { "className", "methodName",
                    "fileName", "lineNumber", "nativeMethod" };
            ManagementUtils.verifyFieldNames(data, attributeNames);
            String[] attributeTypes = { "java.lang.String", "java.lang.String",
                    "java.lang.String", "java.lang.Integer",
                    "java.lang.Boolean" };
            ManagementUtils.verifyFieldTypes(data, attributeNames,
                    attributeTypes);

            // Get hold of the values from the data object to use in the
            // creation of a new StackTraceElement.
            Object[] attributeVals = data.getAll(attributeNames);
            String classNameVal = (String) attributeVals[0];
            String methodNameVal = (String) attributeVals[1];
            String fileNameVal = (String) attributeVals[2];
            int lineNumberVal = ((Integer) attributeVals[3]).intValue();
            boolean nativeMethodVal = ((Boolean) attributeVals[4])
                    .booleanValue();
            // if native method indicator is true, modify lineNumberVal to magic value
            if(nativeMethodVal) {
                lineNumberVal = -2;
            }
            StackTraceElement element = new StackTraceElement(classNameVal,
                    methodNameVal, fileNameVal, lineNumberVal);
            result[i] = element;
        }

        return result;
    }

    private long threadId;

    private String threadName;

    private Thread.State threadState;

    private boolean suspended;

    private boolean inNative;

    private long blockedCount;

    private long blockedTime;

    private long waitedCount;

    private long waitedTime;

    private String lockName;

    private long lockOwnerId;

    private String lockOwnerName;

    private StackTraceElement[] stackTraces = new StackTraceElement[0];

    private String TOSTRING_VALUE;

    /**
     * Creates a new <code>ThreadInfo</code> instance.
     * 
     * @param threadIdVal
     * @param threadNameVal
     * @param threadStateVal
     * @param suspendedVal
     * @param inNativeVal
     * @param blockedCountVal
     * @param blockedTimeVal
     * @param waitedCountVal
     * @param waitedTimeVal
     * @param lockNameVal
     * @param lockOwnerIdVal
     * @param lockOwnerNameVal
     * @param stackTraceVal
     */
    private ThreadInfo(long threadIdVal, String threadNameVal,
            Thread.State threadStateVal, boolean suspendedVal,
            boolean inNativeVal, long blockedCountVal, long blockedTimeVal,
            long waitedCountVal, long waitedTimeVal, String lockNameVal,
            long lockOwnerIdVal, String lockOwnerNameVal,
            StackTraceElement[] stackTraceVal) {
        super();
        this.threadId = threadIdVal;
        this.threadName = threadNameVal;
        this.threadState = threadStateVal;
        this.suspended = suspendedVal;
        this.inNative = inNativeVal;
        this.blockedCount = blockedCountVal;
        this.blockedTime = blockedTimeVal;
        this.waitedCount = waitedCountVal;
        this.waitedTime = waitedTimeVal;
        this.lockName = lockNameVal;
        this.lockOwnerId = lockOwnerIdVal;
        this.lockOwnerName = lockOwnerNameVal;
        this.stackTraces = stackTraceVal;
    }

    /**
     * Returns the number of times that the thread represented by this
     * <code>ThreadInfo</code> has been blocked on any monitor objects. The
     * count is from the start of the thread's life.
     * 
     * @return the number of times the corresponding thread has been blocked on
     *         a monitor.
     */
    public long getBlockedCount() {
        return this.blockedCount;
    }

    /**
     * If thread contention monitoring is supported and enabled, returns the
     * total amount of time that the thread represented by this
     * <code>ThreadInfo</code> has spent blocked on any monitor objects. The
     * time is measued in milliseconds and will be measured over the time period
     * since thread contention was most recently enabled.
     * 
     * @return if thread contention monitoring is currently enabled, the number
     *         of milliseconds that the thread associated with this
     *         <code>ThreadInfo</code> has spent blocked on any monitors. If
     *         thread contention monitoring is supported but currently disabled,
     *         <code>-1</code>.
     * @throws UnsupportedOperationException
     *             if the virtual machine does not support thread contention
     *             monitoring.
     * @see ThreadMXBean#isThreadContentionMonitoringSupported()
     * @see ThreadMXBean#isThreadContentionMonitoringEnabled()
     */
    public long getBlockedTime() {
        return this.blockedTime;
    }

    /**
     * If the thread represented by this <code>ThreadInfo</code> is currently
     * blocked on or waiting on a monitor object, returns a string
     * representation of that monitor object.
     * <p>
     * The monitor's string representation is comprised of the following
     * component parts:
     * <ul>
     * <li><code>monitor</code> class name
     * <li><code>@</code>
     * <li><code>Integer.toHexString(System.identityHashCode(monitor))</code>
     * </ul>
     * </p>
     * @return if blocked or waiting on a monitor, a string representation of
     *         the monitor object. Otherwise, <code>null</code>.
     * @see Integer#toHexString(int)
     * @see System#identityHashCode(java.lang.Object)
     */
    public String getLockName() {
        return this.lockName;
    }

    /**
     * If the thread represented by this <code>ThreadInfo</code> is currently
     * blocked on or waiting on a monitor object, returns the thread identifier
     * of the thread which owns the monitor.
     * 
     * @return the thread identifier of the other thread which holds the monitor
     *         that the thread associated with this <code>ThreadInfo</code> is
     *         blocked or waiting on. If this <code>ThreadInfo</code>'s
     *         associated thread is currently not blocked or waiting, or there
     *         is no other thread holding the monitor, returns a <code>-1</code>.
     */
    public long getLockOwnerId() {
        return this.lockOwnerId;
    }

    /**
     * If the thread represented by this <code>ThreadInfo</code> is currently
     * blocked on or waiting on a monitor object, returns the name of the thread
     * which owns the monitor.
     * 
     * @return the name of the other thread which holds the monitor that the
     *         thread associated with this <code>ThreadInfo</code> is blocked
     *         or waiting on. If this <code>ThreadInfo</code>'s associated
     *         thread is currently not blocked or waiting, or there is no other
     *         thread holding the monitor, returns a <code>null</code>
     *         reference.
     */
    public String getLockOwnerName() {
        return lockOwnerName;
    }

    /**
     * If available, returns the stack trace for the thread represented by this
     * <code>ThreadInfo</code> instance. The stack trace is returned in an
     * array of {@link StackTraceElement}objects with the &quot;top&quot of the
     * stack encapsulated in the first array element and the &quot;bottom&quot;
     * of the stack in the last array element.
     * <p>
     * If this <code>ThreadInfo</code> was created without any stack trace
     * information (e.g. by a call to {@link ThreadMXBean#getThreadInfo(long)})
     * then the returned array will have a length of zero.
     * </p>
     * 
     * @return the stack trace for the thread represented by this
     *         <code>ThreadInfo</code>.
     */
    public StackTraceElement[] getStackTrace() {
        return this.stackTraces;
    }

    /**
     * Returns the thread identifier of the thread represented by this
     * <code>ThreadInfo</code>.
     * 
     * @return the identifer of the thread corresponding to this
     *         <code>ThreadInfo</code>.
     */
    public long getThreadId() {
        return this.threadId;
    }

    /**
     * Returns the name of the thread represented by this
     * <code>ThreadInfo</code>.
     * 
     * @return the name of the thread corresponding to this
     *         <code>ThreadInfo</code>.
     */
    public String getThreadName() {
        return this.threadName;
    }

    /**
     * Returns the thread state value of the thread represented by this
     * <code>ThreadInfo</code>.
     * 
     * @return the thread state of the thread corresponding to this
     *         <code>ThreadInfo</code>.
     * @see Thread#getState()
     */
    public Thread.State getThreadState() {
        return this.threadState;
    }

    /**
     * The number of times that the thread represented by this
     * <code>ThreadInfo</code> has gone to the &quot;wait&quot; or &quot;timed
     * wait&quot; state.
     * 
     * @return the numer of times the corresponding thread has been in the
     *         &quot;wait&quot; or &quot;timed wait&quot; state.
     */
    public long getWaitedCount() {
        return this.waitedCount;
    }

    /**
     * If thread contention monitoring is supported and enabled, returns the
     * total amount of time that the thread represented by this
     * <code>ThreadInfo</code> has spent waiting for notifications. The time
     * is measued in milliseconds and will be measured over the time period
     * since thread contention was most recently enabled.
     * 
     * @return if thread contention monitoring is currently enabled, the number
     *         of milliseconds that the thread associated with this
     *         <code>ThreadInfo</code> has spent waiting notifications. If
     *         thread contention monitoring is supported but currently disabled,
     *         <code>-1</code>.
     * @throws UnsupportedOperationException
     *             if the virtual machine does not support thread contention
     *             monitoring.
     * @see ThreadMXBean#isThreadContentionMonitoringSupported()
     * @see ThreadMXBean#isThreadContentionMonitoringEnabled()
     */
    public long getWaitedTime() {
        return this.waitedTime;
    }

    /**
     * Returns a <code>boolean</code> indication of whether or not the thread
     * represented by this <code>ThreadInfo</code> is currently in a native
     * method.
     * 
     * @return if the corresponding thread <i>is </i> executing a native method
     *         then <code>true</code>, otherwise <code>false</code>.
     */
    public boolean isInNative() {
        return this.inNative;
    }

    /**
     * Returns a <code>boolean</code> indication of whether or not the thread
     * represented by this <code>ThreadInfo</code> is currently suspended.
     * 
     * @return if the corresponding thread <i>is </i> suspened then
     *         <code>true</code>, otherwise <code>false</code>.
     */
    public boolean isSuspended() {
        return this.suspended;
    }

    @Override
    public String toString() {
        // Since ThreadInfos are immutable the string value need only be
        // calculated the one time
        if (TOSTRING_VALUE == null) {
            StringBuilder buff = new StringBuilder();
            buff.append("Thread ");
            buff.append(threadName);
            buff.append(" (Id = ");
            buff.append(threadId);
            buff.append(") ");
            buff.append(threadState.toString());
            if (lockName != null) {
                buff.append(" " + lockName);
            }
            TOSTRING_VALUE = buff.toString().trim();
        }
        return TOSTRING_VALUE;
    }
}
