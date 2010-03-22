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
 * Created on 18.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.DebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;


/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.ThreadGroupTest</code>.
 * This debuggee is started as follow:
 * <ol>
 *      <li>the tested group <code>TESTED_GROUP</code> is created 
 *      <li>the tested thread <code>TESTED_THREAD</code> is started so this
 *          thread belongs to that thread group
 * </ol>
 * For different goals of tests, the debuggee sends the <code>SGNL_READY</code>
 * signal to and waits for the <code>SGNL_CONTINUE</code> signal from debugger
 * in two places:
 * <ul>
 *      <li>right away when the tested thread has been started
 *      <li>when the tested thread has been finished
 * </ul>
 */
public class ThreadGroupDebuggee extends SyncDebuggee {

    public static final String TESTED_GROUP = "TestedGroup";
    public static final String TESTED_THREAD = "TestedThread";
    
    static Object waitForStart = new Object();
    static Object waitForFinish = new Object();
    
    static Object waitTimeObject = new Object();
    static void waitMlsecsTime(long mlsecsTime) { 
        synchronized(waitTimeObject) {
            try {
                waitTimeObject.wait(mlsecsTime);
            } catch (Throwable throwable) {
                 // ignore
            }
        }
    }
    
    public void run() {
        ThreadGroup thrdGroup = new ThreadGroup(TESTED_GROUP);
        DebuggeeThread thrd = new DebuggeeThread(thrdGroup, TESTED_THREAD,
                logWriter, synchronizer); 
        
        synchronized(waitForStart){
            thrd.start();
            try {
                waitForStart.wait();
            } catch (InterruptedException e) {
                
            }
        }

        while ( thrd.isAlive() ) {
            waitMlsecsTime(100);
        }

//        synchronized(waitForFinish){
            logWriter.println("thread is finished");
//        }
        logWriter.println("send SGNL_READY");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    class DebuggeeThread extends Thread {

        LogWriter logWriter;
        DebuggeeSynchronizer synchronizer;

        public DebuggeeThread(ThreadGroup thrdGroup, String name,
                LogWriter logWriter, DebuggeeSynchronizer synchronizer) {
            super(thrdGroup, name);
            this.logWriter = logWriter;
            this.synchronizer = synchronizer;
        }

        public void run() {

            synchronized(ThreadGroupDebuggee.waitForFinish){

                synchronized(ThreadGroupDebuggee.waitForStart){

                    ThreadGroupDebuggee.waitForStart.notifyAll();

                    logWriter.println(getName() +  ": started");
                    synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

                    logWriter.println(getName() +  ": wait for SGNL_CONTINUE");
                    synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
                    logWriter.println(getName() +  ": finished");
                }
            }
        }
    }

    public static void main(String [] args) {
        runDebuggee(ThreadGroupDebuggee.class);
    }
}
