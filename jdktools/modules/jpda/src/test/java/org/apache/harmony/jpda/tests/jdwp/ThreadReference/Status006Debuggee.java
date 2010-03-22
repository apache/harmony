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

import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.Status006Test</code>.
 */
public class Status006Debuggee extends SyncDebuggee {

    static Status006Debuggee status006DebuggeeThis;
    static volatile boolean status006DebuggeeThreadStarted = false;

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
        logWriter.println("--> Debuggee: Status006Debuggee: START");
        status006DebuggeeThis = this;
        
        String status006DebuggeeThreadName = "Status006DebuggeeThread";
        Status006Debuggee_Thread status006DebuggeeThread
            = new Status006Debuggee_Thread(status006DebuggeeThreadName); 

        status006DebuggeeThread.start();

        while ( ! status006DebuggeeThreadStarted ) {
            waitMlsecsTime(1000);
        }
        logWriter.println
        ("--> Debuggee: Status006Debuggee: is waiting for Status006DebuggeeThread to finish...");
        while ( status006DebuggeeThread.isAlive() ) {
            waitMlsecsTime(1000);
        }
        logWriter.println
        ("--> Debuggee: Status006Debuggee: Status006DebuggeeThread has finished");

        synchronizer.sendMessage("READY");
        synchronizer.receiveMessageWithoutException("Status006Debuggee(#1)");

        logWriter.println("--> Debuggee: Status006Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(Status006Debuggee.class);
    }
}

class Status006Debuggee_Thread extends Thread {

    public Status006Debuggee_Thread(String name) {
        super(name);
    }

    public void run() {
        Status006Debuggee parent = Status006Debuggee.status006DebuggeeThis;
        parent.logWriter.println("--> Thread: " + getName() +  ": started...");
        Status006Debuggee.status006DebuggeeThreadStarted = true;
        parent.synchronizer.sendMessage(getName());
        parent.synchronizer.receiveMessageWithoutException("Status006Debuggee_Thread(#1)");
        parent.logWriter.println("--> Thread: " + getName() +  ": is finishibg...");
    }
}

