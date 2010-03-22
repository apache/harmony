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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vitaly A. Provodin
 */

/**
 * Created on 24.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.DebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * The class specifies debuggee for
 * <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.CurrentContendedMonitorTest</code>.
 * This debuggee starts the tested thread <code>TESTED_THREAD</code> which
 * invokes <code>wait()</code>.
 */
public class CurrentContendedMonitorDebuggee extends SyncDebuggee {

    public static final String TESTED_THREAD = "TestedThread";

    static Object waitForStart = new Object();

    static Object waitForFinish = new Object();

    DebuggeeThread thrd;

    public void run() {
        thrd = new DebuggeeThread(TESTED_THREAD, logWriter, synchronizer);
        try {
            synchronized (waitForStart) {
                thrd.start();
                try {
                    waitForStart.wait();
                } catch (InterruptedException e) {
                    throw new TestErrorException(e);
                }
            }
            logWriter.println("thread started");

            synchronized (waitForStart) {
                synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
            }

            synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        } finally {
            if (thrd.isAlive()) {
                logWriter.println("Thread is alive. Interrupt thread");
                thrd.interrupt();
            }
        }
    }

    class DebuggeeThread extends Thread {

        LogWriter logWriter;

        DebuggeeSynchronizer synchronizer;

        public DebuggeeThread(String name, LogWriter logWriter,
                DebuggeeSynchronizer synchronizer) {
            super(name);
            this.logWriter = logWriter;
            this.synchronizer = synchronizer;
        }

        public void run() {

            synchronized (CurrentContendedMonitorDebuggee.waitForFinish) {

                synchronized (CurrentContendedMonitorDebuggee.waitForStart) {
                    CurrentContendedMonitorDebuggee.waitForStart.notifyAll();

                    try {
                        logWriter.println("Thread waits on object..");
                        CurrentContendedMonitorDebuggee.waitForStart.wait();
                    } catch (InterruptedException e) {
                        logWriter.println("Expected " + e);
                        //synchronizer.sendMessage(e.toString());
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        runDebuggee(CurrentContendedMonitorDebuggee.class);
    }
}
