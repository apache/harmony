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
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.Status004Test</code>.
 */
public class Status004Debuggee extends SyncDebuggee {

    static Status004Debuggee status004DebuggeeThis;
    static volatile boolean status004DebuggeeThreadStarted = false;

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
        logWriter.println("--> Debuggee: Status004Debuggee: START");
        status004DebuggeeThis = this;
        
        String status004DebuggeeThreadName = "Status004DebuggeeThread";
        Status004Debuggee_Thread status004DebuggeeThread
            = new Status004Debuggee_Thread(status004DebuggeeThreadName); 

        status004DebuggeeThread.start();

        while ( ! status004DebuggeeThreadStarted ) {
            waitMlsecsTime(1000);
        }
        logWriter.println("--> Debuggee: Status004Debuggee: will sleep for 10 seconds");
        waitMlsecsTime(10000); // to make sure that status004DebuggeeThread is sleeping

        synchronizer.sendMessage(status004DebuggeeThreadName);
        synchronizer.receiveMessage(); // signal to finish  
        
        try {
            status004DebuggeeThread.interrupt();
        } catch (Throwable thrown) {
            // ignore 
        }
        while ( status004DebuggeeThread.isAlive() ) {
            waitMlsecsTime(100);
        }

        logWriter.println("--> Debuggee: Status004Debuggee: FINISH");
        System.exit(0);
    }

    public static void main(String [] args) {
        runDebuggee(Status004Debuggee.class);
    }
}

class Status004Debuggee_Thread extends Thread {

    public Status004Debuggee_Thread(String name) {
        super(name);
    }

    public void run() {
        Status004Debuggee parent = Status004Debuggee.status004DebuggeeThis;
        parent.logWriter.println("--> Thread: " + getName() +  ": started...");
        parent.logWriter.println
        ("--> Thread: " + getName() +  ": will wait UNDEFINITELY");
        Status004Debuggee.status004DebuggeeThreadStarted = true;
        Object ObjectForWait = new Object();
        synchronized(ObjectForWait) {
            try {
                ObjectForWait.wait();
            } catch (Throwable throwable) {
                 // ignore
            }
        }
        parent.logWriter.println("--> Thread: " + getName() +  ": is finishibg...");
    }
}

