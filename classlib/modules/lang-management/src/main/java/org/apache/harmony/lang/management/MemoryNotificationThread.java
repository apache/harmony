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

import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryUsage;

import javax.management.Notification;

/**
 * A thread that monitors and dispatches memory usage notifications from an
 * internal queue.
 * 
 * @since 1.5
 */
class MemoryNotificationThread extends Thread {

    private MemoryMXBeanImpl memBean;

    private MemoryPoolMXBeanImpl memPool;

    int internalID;

    /**
     * Basic constructor
     * 
     * @param mem
     *            The memory bean to send notifications through
     * @param myPool
     *            The memory pool bean we are sending notifications on behalf of
     * @param id
     *            The internal ID of the notification queue being monitored
     */
    MemoryNotificationThread(MemoryMXBeanImpl mem, MemoryPoolMXBeanImpl myPool,
            int id) {
        memBean = mem;
        memPool = myPool;
        internalID = id;
    }

    /**
     * Register a shutdown handler that will signal this thread to terminate,
     * then enter the native that services an internal notification queue.
     */
    public void run() {
        Thread myShutdownNotifier = new MemoryNotificationThreadShutdown(this);
        try {
            Runtime.getRuntime().addShutdownHook(myShutdownNotifier);
        } catch (IllegalStateException e) {
            /*
             * if by chance we are already shutting down when we try to register
             * the shutdown hook, allow this thread to terminate silently
             */
            return;
        }
        processNotificationLoop(internalID);
    }

    /**
     * Process notifications on an internal VM queue until a shutdown request is
     * received.
     * 
     * @param internalID
     *            The internal ID of the queue to service
     */
    private native void processNotificationLoop(int internalID);

    /**
     * A helper method called from within the native
     * {@link #processNotificationLoop(int)} method to construct and dispatch
     * notification objects.
     * 
     * @param min
     *            the initial amount in bytes of memory that can be allocated by
     *            this virtual machine
     * @param used
     *            the number of bytes currently used for memory
     * @param committed
     *            the number of bytes of committed memory
     * @param max
     *            the maximum number of bytes that can be used for memory
     *            management purposes
     * @param count
     *            the number of times that the memory usage of the memory pool
     *            in question has met or exceeded the relevant threshold
     * @param sequenceNumber
     *            the sequence identifier of the current notification
     * @param isCollectionUsageNotification
     *            a <code>boolean</code> indication of whether or not the new
     *            notification is as a result of the collection threshold being
     *            exceeded. If this value is <code>false</code> then the
     *            implication is that a memory threshold has been exceeded.
     */
    @SuppressWarnings("unused")
    // IMPORTANT: for use by VM
    private void dispatchNotificationHelper(long min, long used,
            long committed, long max, long count, long sequenceNumber,
            boolean isCollectionUsageNotification) {
        MemoryNotificationInfo info = new MemoryNotificationInfo(memPool
                .getName(), new MemoryUsage(min, used, committed, max), count);
        Notification n = new Notification(
                isCollectionUsageNotification ? MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED
                        : MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                "java.lang:type=Memory", sequenceNumber);
        n.setUserData(ManagementUtils
                .toMemoryNotificationInfoCompositeData(info));
        memBean.sendNotification(n);
    }
}
