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
 * @author Anatoly F. Bondarenko
 */

/**
 * Created on 31.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

// import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.Status003Test</code>.
 */
public class Status003Debuggee extends SyncDebuggee {

    static Status003Debuggee status003DebuggeeThis;
    static volatile boolean status003DebuggeeThreadStarted = false;

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
        logWriter.println("--> Debuggee: Status003Debuggee: START");
        status003DebuggeeThis = this;
        
        String status003DebuggeeThreadName = "Status003DebuggeeThread";
        Status003Debuggee_Thread status003DebuggeeThread
            = new Status003Debuggee_Thread(status003DebuggeeThreadName); 

        status003DebuggeeThread.start();

        while ( ! status003DebuggeeThreadStarted ) {
            waitMlsecsTime(1000);
        }
        logWriter.println("--> Debuggee: Status003Debuggee: will sleep for 10 seconds");
        waitMlsecsTime(10000); // to make sure that status003DebuggeeThread is sleeping

        synchronizer.sendMessage(status003DebuggeeThreadName);
        synchronizer.receiveMessage(); // signal to finish  
 
        try {
            status003DebuggeeThread.interrupt();
        } catch (Throwable thrown) {
            // ignore 
        }
        while ( status003DebuggeeThread.isAlive() ) {
            waitMlsecsTime(100);
        }

        logWriter.println("--> Debuggee: Status003Debuggee: FINISH");
        System.exit(0);
    }

    public static void main(String [] args) {
        runDebuggee(Status003Debuggee.class);
    }
}

class Status003Debuggee_Thread extends Thread {

    public Status003Debuggee_Thread(String name) {
        super(name);
    }

    public void run() {
        Status003Debuggee parent = Status003Debuggee.status003DebuggeeThis;
        parent.logWriter.println("--> Thread: " + getName() +  ": started...");
        long mlsecTimeToSleep = 1000 * 60 * 3;
        parent.logWriter.println
        ("--> Thread: " + getName() +  ": will wait " + mlsecTimeToSleep + " mlsecs");
        Status003Debuggee.status003DebuggeeThreadStarted = true;
        Object ObjectForWait = new Object();
        synchronized(ObjectForWait) {
            try {
                ObjectForWait.wait(mlsecTimeToSleep);
            } catch (Throwable throwable) {
                 // ignore
            }
        }
        parent.logWriter.println("--> Thread: " + getName() +  ": is finishibg...");
    }
}

