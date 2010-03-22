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
 * Created on 06.06.2006 
 */

package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class SuspendDebuggee extends SyncDebuggee {
    public static final int THREAD_NUMBER_LIMIT = 9; 
    public static final String THREAD_NAME_PATTERN = "SuspendDebuggee_Thread_"; 

    static SuspendDebuggee suspendDebuggeeThis;

    static volatile boolean allThreadsToFinish = false;
    static int createdThreadsNumber = 0;
    static volatile int startedThreadsNumber = 0;

    static SuspendDebuggee_Thread[] suspendDebuggeeThreads = null;

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
    
    static void sleepMlsecsTime(long mlsecsTime) { 
        try {
            Thread.sleep(mlsecsTime);
        } catch (Throwable throwable) {
             // ignore
        }
    }
    
    public void run() {
        
        logWriter.println("--> SuspendDebuggee: START...");
        suspendDebuggeeThis = this;

        logWriter.println("--> SuspendDebuggee: Create and start tested threads...");
        try {
            suspendDebuggeeThreads = new SuspendDebuggee_Thread[THREAD_NUMBER_LIMIT]; 
            for (int i=0; i < THREAD_NUMBER_LIMIT; i++) {
                suspendDebuggeeThreads[i]= new SuspendDebuggee_Thread(i);
                suspendDebuggeeThreads[i].start();
                createdThreadsNumber++;
            }
        } catch ( Throwable thrown) {
            logWriter.println
            ("--> SuspendDebuggee: Exception while creating threads: " + thrown);
        }
        logWriter.println
        ("--> SuspendDebuggee: Created threads number = " + createdThreadsNumber);
        
        while ( startedThreadsNumber != createdThreadsNumber ) {
            waitMlsecsTime(100);
        }
        if ( createdThreadsNumber != 0 ) {
            logWriter.println("--> SuspendDebuggee: All created threads are started!");
        }

        synchronizer.sendMessage(Integer.toString(createdThreadsNumber));
        if ( createdThreadsNumber == 0 ) {
            logWriter.println("--> SuspendDebuggee: FINISH...");
            return;
        }
        synchronizer.receiveMessage(); // signal to finish

        logWriter.println
        ("--> SuspendDebuggee: Send signal to all threads to finish and wait...");
        allThreadsToFinish = true;

        for (int i=0; i < createdThreadsNumber; i++) {
            while ( suspendDebuggeeThreads[i].isAlive() ) {
                waitMlsecsTime(10);
            }
        }
        logWriter.println
        ("--> SuspendDebuggee: All threads finished!");

        logWriter.println("--> SuspendDebuggee: FINISH...");

    }

    public static void main(String [] args) {
        runDebuggee(SuspendDebuggee.class);
    }

}

class SuspendDebuggee_Thread extends Thread {
    
    int threadKind;
    
    public SuspendDebuggee_Thread(int threadNumber) {
        super(SuspendDebuggee.THREAD_NAME_PATTERN + threadNumber);
        threadKind = threadNumber % 3;
    }

    public void run() {
        SuspendDebuggee parent = SuspendDebuggee.suspendDebuggeeThis;
        synchronized (parent) { 
            SuspendDebuggee.startedThreadsNumber++;
        }
        while ( ! SuspendDebuggee.allThreadsToFinish ) {
            switch ( threadKind ) {
            case 0:
                SuspendDebuggee.waitMlsecsTime(100);
                break;
            case 1:
                SuspendDebuggee.sleepMlsecsTime(100);
            }
        }
    }
}


