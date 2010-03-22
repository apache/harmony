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
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.Status005Test</code>.
 */
public class Status005Debuggee extends SyncDebuggee {

    static Status005Debuggee status005DebuggeeThis;
    static volatile boolean status005DebuggeeThreadStarted = false;

    static Object lockObject = new Object();

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
        logWriter.println("--> Debuggee: Status005Debuggee: START");
        status005DebuggeeThis = this;
        
        String status005DebuggeeThreadName = "Status005DebuggeeThread";
        Status005Debuggee_Thread status005DebuggeeThread
            = new Status005Debuggee_Thread(status005DebuggeeThreadName); 

        synchronized(lockObject) {
            logWriter.println
            ("--> Debuggee: Status005Debuggee: has entered in synchronized(lockObject) block");
            status005DebuggeeThread.start();

            while ( ! status005DebuggeeThreadStarted ) {
                waitMlsecsTime(1000);
            }
            logWriter.println("--> Debuggee: Status005Debuggee: will sleep for 10 seconds");
            waitMlsecsTime(10000); // to make sure that status005DebuggeeThread is sleeping

            synchronizer.sendMessage(status005DebuggeeThreadName);
            synchronizer.receiveMessage(); // signal to finish
        }
        logWriter.println
        ("--> Debuggee: Status005Debuggee: has exited from synchronized(lockObject) block");
        
        while ( status005DebuggeeThread.isAlive() ) {
            waitMlsecsTime(100);
        }

       logWriter.println("--> Debuggee: Status005Debuggee: FINISH");
        System.exit(0);
    }

    public static void main(String [] args) {
        runDebuggee(Status005Debuggee.class);
    }
}

class Status005Debuggee_Thread extends Thread {

    public Status005Debuggee_Thread(String name) {
        super(name);
    }

    public void run() {
        Status005Debuggee parent = Status005Debuggee.status005DebuggeeThis;
        parent.logWriter.println("--> Thread: " + getName() +  ": started...");
        parent.logWriter.println
        ("--> Thread: " + getName() +  ": will try to enter in synchronized(lockObject) block");
        Status005Debuggee.status005DebuggeeThreadStarted = true;
        synchronized(Status005Debuggee.lockObject) {
            parent.logWriter.println
            ("--> Thread: " + getName() +  ": has entered in synchronized(lockObject) block");
        }
        parent.logWriter.println("--> Thread: " + getName() +  ": is finishibg...");
    }
}

