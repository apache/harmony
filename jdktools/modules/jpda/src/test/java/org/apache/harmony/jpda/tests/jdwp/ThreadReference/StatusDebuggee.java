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
 * Created on 22.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.DebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;


/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.StatusTest</code>.
 * This debuggee starts the tested thread <code>TESTED_THREAD</code>.
 */
public class StatusDebuggee extends SyncDebuggee {

    public static final String TESTED_THREAD = "TestedThread";
    
    static Object waitForStart = new Object();
    static Object waitForFinish = new Object();
    
    public void run() {
        DebuggeeThread thrd = new DebuggeeThread(TESTED_THREAD,
                logWriter, synchronizer); 
        
        synchronized(waitForStart){
            thrd.start();
            try {
                waitForStart.wait();
            } catch (InterruptedException e) {

            }
        }

        synchronized(waitForFinish){
            logWriter.println("thread is finished");
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

            synchronized(StatusDebuggee.waitForFinish){

                synchronized(StatusDebuggee.waitForStart){

                    StatusDebuggee.waitForStart.notifyAll();

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
        runDebuggee(StatusDebuggee.class);
    }
}
