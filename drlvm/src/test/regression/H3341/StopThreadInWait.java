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

package org.apache.harmony.drlvm.tests.regression.h3341;

import junit.framework.TestCase;

/**
 * Test case for StopThread() jvmti function applied to thread that hangs on
 * Object.wait().
 * Checks that StopThread() forces target thread to exit wait() and throw the
 * requested exception immediately before the wait() timeout is over.
 * Since we test thread synchronization methods behaviour the test doesn't use
 * synchronization object's for it's own purposes. Thus It's execution is based
 * on timeouts.
 */
public class StopThreadInWait extends TestCase {

    static final int TIMEOUT_FOR_WAIT = 10000;
    static final int TIMEOUT_BEFORE_STOPPING = 1000;
    static final int TIMEOUT_FOR_EXCEPTION = 1000;

    static boolean tooLate = false;

    public static void main(String args[]) {
        (new StopThreadInWait()).test();
    }

    public void test() {
        Thread waitingThread = new WaitingThread();
        waitingThread.start();

        try {
            Thread.sleep(TIMEOUT_BEFORE_STOPPING);
            try {
                System.err.println("[Java]: Throwing an exception");
                // pass execution to the agent
                throw new InvokeAgentException(waitingThread,
                        new ThreadDeath());
            } catch (Exception e) {
                System.err.println("[Java]: Exception caught");
            }

            Thread.sleep(TIMEOUT_FOR_EXCEPTION);
            tooLate = true;

            System.err.println("[Java]: joining test_thread...");
            waitingThread.join();
        } catch(InterruptedException exc) {
            exc.printStackTrace();
        }

        System.err.println("[Java]: test done");
        assertTrue(Status.status);
    }
}

class WaitingThread extends Thread {

    public WaitingThread() {
        super();
    }

    public void run() {
        Object lock = new Object();

        synchronized(lock) {
            try {
                System.out.println("[java]: test thread falling in wait");
                lock.wait(StopThreadInWait.TIMEOUT_FOR_WAIT);
            } catch(ThreadDeath exc) {
                if (! StopThreadInWait.tooLate) {
                    Status.status = true;

                    System.out.println("[java]: test thread has caught " +
                            "ThreadDeath exception in time");
                } else {
                    System.out.println("[java]: test thread has caught " +
                            "ThreadDeath exception too late");
                }
            } catch(Throwable exc) {
                exc.printStackTrace();
            }
        }
    }
};

class InvokeAgentException extends Exception {

    Thread thread;
    Throwable stopException;

    InvokeAgentException(Thread thread, Throwable exception) {
        this.thread = thread;
        stopException = exception;
    }
}

class Status {
    /** the field should be modified by jvmti agent to determine test result. */
    public static boolean status = false;
}
