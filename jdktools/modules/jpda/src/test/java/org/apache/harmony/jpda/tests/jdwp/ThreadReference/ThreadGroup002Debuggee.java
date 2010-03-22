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
 * Created on 19.06.2006
 */

package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class ThreadGroup002Debuggee extends SyncDebuggee {
    public static final int THREAD_NUMBER_LIMIT = 6; 
    public static final String THREAD_NAME_PATTERN = "ThreadGroup002Debuggee_Thread_"; 
    public static final String THREAD_GROUP_NAME_PATTERN = "ThreadGroup002Debuggee_Thread_Group_"; 

    static ThreadGroup002Debuggee ThreadGroup002DebuggeeThis;

    static volatile boolean allThreadsToFinish = false;
    static int createdThreadsNumber = 0;
    static volatile int startedThreadsNumber = 0;

    static int firstThreadsNumber = 0;

    static ThreadGroup002Debuggee_Thread[] ThreadGroup002DebuggeeThreads = null;
    static ThreadGroup[] ThreadGroup002DebuggeeThreadGroups = new ThreadGroup[2];

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
        
        logWriter.println("--> ThreadGroup002Debuggee: START...");
        ThreadGroup002DebuggeeThis = this;
        logWriter.println("--> ThreadGroup002Debuggee: Create thread groups...");
        ThreadGroup002DebuggeeThreadGroups[0] = 
            new ThreadGroup(THREAD_GROUP_NAME_PATTERN + 0);
        ThreadGroup002DebuggeeThreadGroups[1] = 
            new ThreadGroup(THREAD_GROUP_NAME_PATTERN + 1);

        logWriter.println("--> ThreadGroup002Debuggee: Create and start tested threads...");
        try {
            ThreadGroup002DebuggeeThreads = new ThreadGroup002Debuggee_Thread[THREAD_NUMBER_LIMIT]; 
            for (int i=0; i < THREAD_NUMBER_LIMIT; i++) {
                ThreadGroup002DebuggeeThreads[i] = 
                    new ThreadGroup002Debuggee_Thread(ThreadGroup002DebuggeeThreadGroups[i%2], i);
                ThreadGroup002DebuggeeThreads[i].start();
                createdThreadsNumber++;
            }
        } catch ( Throwable thrown) {
            logWriter.println
            ("--> ThreadGroup002Debuggee: Exception while creating threads: " + thrown);
        }
        logWriter.println
        ("--> ThreadGroup002Debuggee: Created threads number = " + createdThreadsNumber);
        
        while ( startedThreadsNumber != createdThreadsNumber ) {
            waitMlsecsTime(100);
        }
        if ( createdThreadsNumber != 0 ) {
            logWriter.println("--> ThreadGroup002Debuggee: All created threads are started!");
        }

        synchronizer.sendMessage(Integer.toString(createdThreadsNumber));

        String mainThreadName = Thread.currentThread().getName();
        synchronizer.sendMessage(mainThreadName);

        String mainThreadGroupName = Thread.currentThread().getThreadGroup().getName();
        synchronizer.sendMessage(mainThreadGroupName);


        logWriter.println("--> ThreadGroup002Debuggee: Wait for signal from test to continue...");
        String messageFromTest = synchronizer.receiveMessage();  // signal to continue or to finish
        
        if ( ! messageFromTest.equals("FINISH") ) {
            logWriter.println
            ("--> ThreadGroup002Debuggee: Send signal to the first threads to finish...");
            
            firstThreadsNumber = createdThreadsNumber/2;
            for (int i=0; i < firstThreadsNumber; i++) {
                while ( ThreadGroup002DebuggeeThreads[i].isAlive() ) {
                    waitMlsecsTime(10);
                }
            }
            logWriter.println
            ("--> ThreadGroup002Debuggee: First threads finished - number of finished threads = " +
                    firstThreadsNumber);
            
            synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
            
            logWriter.println("--> ThreadGroup002Debuggee: Wait for signal from test to finish...");
            synchronizer.receiveMessage();  // signal to finish
        }

        logWriter.println
        ("--> ThreadGroup002Debuggee: Send signal to all threads to finish and wait...");
        allThreadsToFinish = true;

        for (int i=0; i < createdThreadsNumber; i++) {
            while ( ThreadGroup002DebuggeeThreads[i].isAlive() ) {
                waitMlsecsTime(10);
            }
        }
        logWriter.println
        ("--> ThreadGroup002Debuggee: All threads finished!");

        logWriter.println("--> ThreadGroup002Debuggee: FINISH...");

    }

    public static void main(String [] args) {
        runDebuggee(ThreadGroup002Debuggee.class);
    }

}

class ThreadGroup002Debuggee_Thread extends Thread {
    
    int threadNumber;
    int threadKind;
    
    public ThreadGroup002Debuggee_Thread(ThreadGroup group, int threadNumber) {
        super(group, ThreadGroup002Debuggee.THREAD_NAME_PATTERN + threadNumber);
        this.threadNumber = threadNumber;
        threadKind = threadNumber % 3;
    }

    public void run() {
        ThreadGroup002Debuggee parent = ThreadGroup002Debuggee.ThreadGroup002DebuggeeThis;
        synchronized (parent) { 
            ThreadGroup002Debuggee.startedThreadsNumber++;
        }
        while ( ! ThreadGroup002Debuggee.allThreadsToFinish ) {
            switch ( threadKind ) {
            case 0:
                ThreadGroup002Debuggee.waitMlsecsTime(100);
                break;
            case 1:
                ThreadGroup002Debuggee.sleepMlsecsTime(100);
            }
            if (threadNumber < ThreadGroup002Debuggee.firstThreadsNumber ) {
                return;
            }
        }
    }
}


