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
 * Created on 20.06.2006
 */

package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class ResumeDebuggee extends SyncDebuggee {
    public static final int THREAD_NUMBER_LIMIT = 10; 
    public static final String THREAD_NAME_PATTERN = "ResumeDebuggee_Thread_"; 
    public static final String TO_FINISH_DEBUGGEE_FIELD_NAME = "debuggeToFinish"; 

    static ResumeDebuggee ResumeDebuggeeThis;

    static volatile boolean allThreadsToFinish = false;
    static int debuggeToFinish = 0;
    static int createdThreadsNumber = 0;
    static volatile int startedThreadsNumber = 0;

    static ResumeDebuggee_Thread[] ResumeDebuggeeThreads = null;

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
        
        logWriter.println("--> ResumeDebuggee: START...");
        ResumeDebuggeeThis = this;
        logWriter.println("--> ResumeDebuggee: Create thread groups...");

        logWriter.println("--> ResumeDebuggee: Create and start tested threads...");
        try {
            ResumeDebuggeeThreads = new ResumeDebuggee_Thread[THREAD_NUMBER_LIMIT]; 
            for (int i=0; i < THREAD_NUMBER_LIMIT; i++) {
                ResumeDebuggeeThreads[i] = new ResumeDebuggee_Thread(i);
                ResumeDebuggeeThreads[i].start();
                createdThreadsNumber++;
            }
        } catch ( Throwable thrown) {
            logWriter.println
            ("--> ResumeDebuggee: Exception while creating threads: " + thrown);
        }
        logWriter.println
        ("--> ResumeDebuggee: Created threads number = " + createdThreadsNumber);
        
        while ( startedThreadsNumber != createdThreadsNumber ) {
            waitMlsecsTime(100);
        }
        if ( createdThreadsNumber != 0 ) {
            logWriter.println("--> ResumeDebuggee: All created threads are started!");
        }

        synchronizer.sendMessage(Integer.toString(createdThreadsNumber));

        String mainThreadName = Thread.currentThread().getName();
        synchronizer.sendMessage(mainThreadName);

        logWriter.println("--> ResumeDebuggee: Wait for signal from test to finish...");
        while ( debuggeToFinish != 99 ) { // is set up by debugger - ResumeTest
            waitMlsecsTime(100);
        }

        logWriter.println
        ("--> ResumeDebuggee: Send signal to all threads to finish and wait...");
        allThreadsToFinish = true;

        for (int i=0; i < createdThreadsNumber; i++) {
            while ( ResumeDebuggeeThreads[i].isAlive() ) {
                waitMlsecsTime(10);
            }
        }
        logWriter.println
        ("--> ResumeDebuggee: All threads finished!");

        logWriter.println("--> ResumeDebuggee: FINISH...");

    }

    public static void main(String [] args) {
        runDebuggee(ResumeDebuggee.class);
    }

}

class ResumeDebuggee_Thread extends Thread {
    
    int threadKind;
    
    public ResumeDebuggee_Thread(int threadNumber) {
        super(ResumeDebuggee.THREAD_NAME_PATTERN + threadNumber);
        threadKind = threadNumber % 3;
    }

    public void run() {
        ResumeDebuggee parent = ResumeDebuggee.ResumeDebuggeeThis;
        synchronized (parent) { 
            ResumeDebuggee.startedThreadsNumber++;
        }
        while ( ! ResumeDebuggee.allThreadsToFinish ) {
            switch ( threadKind ) {
            case 0:
                ResumeDebuggee.waitMlsecsTime(100);
                break;
            case 1:
                ResumeDebuggee.sleepMlsecsTime(100);
            }
        }
    }
}


