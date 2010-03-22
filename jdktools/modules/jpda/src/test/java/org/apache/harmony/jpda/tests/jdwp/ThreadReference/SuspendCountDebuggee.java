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
 * Created on 07.06.2006 
 */

package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class SuspendCountDebuggee extends SyncDebuggee {
    public static final int THREAD_NUMBER_LIMIT = 6; 
    public static final String THREAD_NAME_PATTERN = "SuspendCountDebuggee_Thread_"; 
    public static final String TO_FINISH_DEBUGGEE_FIELD_NAME = "debuggeToFinish"; 

    static SuspendCountDebuggee suspendCountDebuggeeThis;

    static volatile boolean allThreadsToFinish = false;
    static int debuggeToFinish = 0;
    static int createdThreadsNumber = 0;
    static volatile int startedThreadsNumber = 0;

    static SuspendCountDebuggee_Thread[] suspendCountDebuggeeThreads = null;

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
        
        logWriter.println("--> SuspendCountDebuggee: START...");
        suspendCountDebuggeeThis = this;

        logWriter.println("--> SuspendCountDebuggee: Create and start tested threads...");
        try {
            suspendCountDebuggeeThreads = new SuspendCountDebuggee_Thread[THREAD_NUMBER_LIMIT]; 
            for (int i=0; i < THREAD_NUMBER_LIMIT; i++) {
                suspendCountDebuggeeThreads[i]= new SuspendCountDebuggee_Thread(i);
                suspendCountDebuggeeThreads[i].start();
                createdThreadsNumber++;
            }
        } catch ( Throwable thrown) {
            logWriter.println
            ("--> SuspendCountDebuggee: Exception while creating threads: " + thrown);
        }
        logWriter.println
        ("--> SuspendCountDebuggee: Created threads number = " + createdThreadsNumber);
        
        while ( startedThreadsNumber != createdThreadsNumber ) {
            waitMlsecsTime(100);
        }
        if ( createdThreadsNumber != 0 ) {
            logWriter.println("--> SuspendCountDebuggee: All created threads are started!");
        }

        synchronizer.sendMessage(Integer.toString(createdThreadsNumber));
        if ( createdThreadsNumber == 0 ) {
            logWriter.println("--> SuspendCountDebuggee: FINISH...");
            System.exit(0);
        }
        String messageFromTest = synchronizer.receiveMessage();  // signal to continue or to finish
        
        if ( ! messageFromTest.equals("FINISH") ) {
            String mainThreadName = Thread.currentThread().getName();
            synchronizer.sendMessage(mainThreadName);
            while ( debuggeToFinish != 99 ) { // is set up by debugger - SuspendCountTest
                waitMlsecsTime(100);
            }
        }

        logWriter.println
        ("--> SuspendCountDebuggee: Send signal to all threads to finish and wait...");
        allThreadsToFinish = true;

        for (int i=0; i < createdThreadsNumber; i++) {
            while ( suspendCountDebuggeeThreads[i].isAlive() ) {
                waitMlsecsTime(10);
            }
        }
        logWriter.println
        ("--> SuspendCountDebuggee: All threads finished!");

        logWriter.println("--> SuspendCountDebuggee: FINISH...");

    }

    public static void main(String [] args) {
        runDebuggee(SuspendCountDebuggee.class);
    }

}

class SuspendCountDebuggee_Thread extends Thread {
    
    int threadKind;
    
    public SuspendCountDebuggee_Thread(int threadNumber) {
        super(SuspendCountDebuggee.THREAD_NAME_PATTERN + threadNumber);
        threadKind = threadNumber % 3;
    }

    public void run() {
        SuspendCountDebuggee parent = SuspendCountDebuggee.suspendCountDebuggeeThis;
        synchronized (parent) { 
            SuspendCountDebuggee.startedThreadsNumber++;
        }
        while ( ! SuspendCountDebuggee.allThreadsToFinish ) {
            switch ( threadKind ) {
            case 0:
                SuspendCountDebuggee.waitMlsecsTime(100);
                break;
            case 1:
                SuspendCountDebuggee.sleepMlsecsTime(100);
            }
        }
    }
}


