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
 * @author Mikhail Danilov
 */
package org.apache.harmony.awt.wtk;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.harmony.awt.internal.nls.Messages;

/**
 * Class synchronizer is to protect AWT state integrity in multithreading environment.
 * It is supposed to have a child class per native platform.
 * The only instance is created on the first use of one of the core AWT classes.
 * Registers WTK on the dispatch thread startup.
 * It is just a special kind of mutex.
 *
 */

public class Synchronizer {
    //TODO: think about java.util.concurrent use for faster blocking/awaking operations

    /**
     * This field holds the counter of lock operation.
     * To free synchronizer unlock method must be called $acquestCounter times.
     * Equals to 0 when synchronizer is free.
     */
    protected int acquestCounter;

    /**
     * This field holds the owner of synchronizer.
     * Owner of synchronizer is a last thread that successfully locked synchronizer and
     * still havn't freed it. Equals to null when synchronizer is free.
     */
    protected Thread owner;

    /**
     * The event dispatch thread
     */
    protected Thread dispatchThread;
    
    /**
     * Indicates whether the event dispatch thread, which has the highest 
     * priority, is waiting to acquire the lock.
     */
    private boolean isDispatchThreadWaiting;
    
    /**
     * This object is used for threads locking. A locked thread could be 
     * unlocked only in one of two ways: the unlock() method of this class has 
     * been invoked; the thread has been interrupted from another thread.
     */
    private final Object lock = new Object();
    
    private final Map<Thread, Integer> storedStates = new IdentityHashMap<Thread, Integer>();

    /**
     * Acquire the lock for this synchronizer. Nested lock is supported.
     * If the mutex is already locked by another thread, the current thread will be put
     * into wait queue until the lock becomes available.
     * All user threads are served in FIFO order. Dispatch thread has higher priority.
     * Supposed to be used in Toolkit.lockAWT() only.
     */
    public void lock() {
        final Thread curThread = Thread.currentThread();

        synchronized (lock) {
            if (owner == null) {
                owner = curThread;
                acquestCounter = 1;
                return;
            } else if (owner == curThread) {
                acquestCounter++;
                return;
            }
            
            isDispatchThreadWaiting = (curThread == dispatchThread);
            park(curThread);
        }
    }
    
    /**
     * Release the lock for this synchronizer.
     * If wait queue is not empty the first waiting thread acquires the lock.
     * Supposed to be used in Toolkit.unlockAWT() only.
     */
    public void unlock() {
        synchronized (lock) {
            if (owner == null) {
                // awt.20=Can't unlock not locked resource.
                throw new RuntimeException(Messages.getString("awt.20")); //$NON-NLS-1$
            }

            if (owner != Thread.currentThread()) {
                // awt.21=Not owner can't unlock resource.
                throw new RuntimeException(Messages.getString("awt.21")); //$NON-NLS-1$
            }

            acquestCounter--;

            if (acquestCounter == 0) {
                if (isDispatchThreadWaiting) {
                    isDispatchThreadWaiting = false;
                    owner = dispatchThread;
                    acquestCounter = 1;
                    dispatchThread.interrupt();
                } else {
                    owner = null;
                    lock.notify();
                }
            }
        }
    }

    /**
     * Stores state of this synchronizer and frees it.
     * Supposed to be used in Toolkit.unsafeInvokeAndWaitUnderAWTLock() only in pair with
     * lockAndRestoreState().
     * Do not call it directly.
     */
    public void storeStateAndFree() {
        final Thread curThread = Thread.currentThread();

        if (owner != curThread) {
            // awt.22=Not owner can't free resource.
            throw new RuntimeException(Messages.getString("awt.22")); //$NON-NLS-1$
        }

        if (storedStates.containsKey(curThread)) {
            // awt.23=One thread can't store state several times in a row.
            throw new RuntimeException(Messages.getString("awt.23")); //$NON-NLS-1$
        }

        storedStates.put(curThread, new Integer(acquestCounter));
        acquestCounter = 1;
        unlock();
    }

    /**
     * Locks this synchronizer and restores it's state.
     * Supposed to be used in Toolkit.unsafeInvokeAndWaitUnderAWTLock() only in pair with
     * storeStateAndFree().
     * Do not call it directly.
     */
    public void lockAndRestoreState() {
        final Integer counter;
        final Thread curThread = Thread.currentThread();

        if (owner == curThread) {
            // awt.24=Owner can't overwrite resource state. Lock operations
            // may be lost.
            throw new RuntimeException(Messages.getString("awt.24")); //$NON-NLS-1$
        }

        counter = storedStates.remove(curThread);

        if (counter == null) {
            // awt.25=No state stored for current thread.
            throw new RuntimeException(Messages.getString("awt.25")); //$NON-NLS-1$
        }

        lock();
        acquestCounter = counter.intValue();
    }

    /**
     * Sets references to WTK and event dispatch thread.
     * Called on toolkit startup.
     *
     * @param wtk - reference to WTK instance
     * @param dispatchThread - reference to event dispatch thread
     */
    public void setEnvironment(WTK wtk, Thread dispatchThread) { 
        this.dispatchThread = dispatchThread;
    }
    
    private void park(final Thread t) {
        synchronized (lock) {
            try {
                lock.wait();
                
                if (owner == null) { 
                    owner = t;
                    acquestCounter = 1;
                } else {
                    park(t);
                }
            } catch (InterruptedException ex) {
                // event dispatch thread unlocked or the waiting thread has been 
                // interrupted from another thread
            }
        }
    }
}
