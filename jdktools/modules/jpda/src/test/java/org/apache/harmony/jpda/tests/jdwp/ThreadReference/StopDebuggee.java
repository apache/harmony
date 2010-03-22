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
import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;


/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.StopTest</code>.
 * This debuggee starts the tested thread <code>TESTED_THREAD</code> which waits for 
 * 'Stop' command with NullPointerException exception.
 */
public class StopDebuggee extends SyncDebuggee {

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

    static Object waitTimeObjectWithException = new Object();
    static void waitMlsecsTimeWithException(long mlsecsTime) throws Throwable { 
        synchronized(waitTimeObject) {
            try {
                waitTimeObject.wait(mlsecsTime);
            } catch (Throwable throwable) {
                 throw throwable;
            }
        }
    }

    public static String testStatus = "PASSED";
    public static final String TESTED_THREAD = "TestedThread";
    public static final String FIELD_NAME = "exception";
//    public static final String EXCEPTION_SIGNATURE = "Ljava/lang/NullPointerException;";
    public static NullPointerException exception = new NullPointerException(); 
    
    static Object waitForStart = new Object();
    
    public void run() {
        logWriter.println("StopDebuggee: started");
        DebuggeeThread thrd = new DebuggeeThread(TESTED_THREAD,
                logWriter, synchronizer); 
        
        synchronized(waitForStart){
            thrd.start();
            try {
                waitForStart.wait();
            } catch (InterruptedException e) {
                logWriter.println("StopDebuggee:" + e + " is caught while waitForStart.wait()");
            }
        }

        logWriter.println("StopDebuggee: Wait for TestedThread to finish...");
        while ( thrd.isAlive() ) {
            waitMlsecsTime(1000);
        }
        logWriter.println("StopDebuggee: TestedThread finished");

        synchronizer.sendMessage(testStatus);
        logWriter.println("StopDebuggee: finishing...");
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
            logWriter.println(getName() +  ": started");
            synchronized(waitForStart){
                waitForStart.notifyAll();
            }

            logWriter.println(getName() +  ": Wait for 'Stop' command with NullPointerException...");
            try {
                synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
                waitMlsecsTimeWithException(TestOptions.DEFAULT_TIMEOUT);
                logWriter.println(getName() +  ": FAILED: TIMEOUT is run out - No any exception is caught");
                testStatus = "FAILED";
            } catch (Throwable thrown) {
                logWriter.println(getName() +  ": Exception is caught: " + thrown);
                if ( thrown.equals(exception) ) {
                    logWriter.println(getName() +  ": PASSED: It is expected Exception");
                } else {
                    logWriter.println(getName() +  ": FAILED: It is unexpected Exception");
                    testStatus = "FAILED";
                }
            }
        }
    }

    public static void main(String [] args) {
        runDebuggee(StopDebuggee.class);
    }
}
