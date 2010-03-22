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

/**
 * A thread hooked as a VM shutdown hook to tell a MemoryNotificationThread to
 * terminate.
 * 
 * @since 1.5
 */
class MemoryNotificationThreadShutdown extends Thread {
    private MemoryNotificationThread myVictim;

    /**
     * Basic constructor
     * 
     * @param victim
     *            The thread to notify on shutdown
     */
    MemoryNotificationThreadShutdown(MemoryNotificationThread victim) {
        myVictim = victim;
    }

    /**
     * Shutdown hook code that coordinates the termination of a memory
     * notification thread.
     */
    public void run() {
        sendShutdownNotification(myVictim.internalID);
        try {
            // wait for the notification thread to terminate
            myVictim.join();
        } catch (InterruptedException e) {
            // don't care
        }
    }

    /**
     * Wipes any pending notifications and puts a shutdown request notification
     * on an internal notification queue.
     * 
     * @param id
     *            The internal id of the queue to shut down
     */
    private native void sendShutdownNotification(int id);
}
