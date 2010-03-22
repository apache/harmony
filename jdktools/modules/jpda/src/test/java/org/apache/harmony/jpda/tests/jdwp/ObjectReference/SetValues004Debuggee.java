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
 * Created on 13.07.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class SetValues004Debuggee extends SyncDebuggee {
    
    static String passedStatus = "PASSED";
    static String failedStatus = "FAILED";
    static String status = passedStatus;

    static SetValues004Debuggee testedObject;

    int intArrayField[]; // JDWP_TAG_ARRAY = 91
    SetValues004Debuggee objectArrayField[]; // JDWP_TAG_ARRAY = 91
    SetValues004Debuggee objectField; // JDWP_TAG_OBJECT = 76
    String stringField; // JDWP_TAG_STRING = 115
    Thread threadField; // JDWP_TAG_THREAD = 116
    ThreadGroup threadGroupField; // JDWP_TAG_THREAD_GROUP = 103
    Class classField; // JDWP_TAG_CLASS_OBJECT = 99
    ClassLoader classLoaderField; // DWP_TAG_CLASS_LOADER = 108

    public void run() {

        logWriter.println("--> Debuggee: SetValues004Debuggee: START");
        testedObject = new SetValues004Debuggee();

        testedObject.intArrayField = new int[1];
        testedObject.intArrayField[0]= 999;
        testedObject.objectArrayField = new SetValues004Debuggee[1];
        testedObject.objectArrayField[0] = new SetValues004Debuggee();
        testedObject.objectField = new SetValues004Debuggee();
        testedObject.stringField = "stringField";
        testedObject.threadField = new SetValues004DebuggeeThread();
        testedObject.threadGroupField = new ThreadGroup("ThreadGroupName");
        testedObject.classField = SetValues004Debuggee.class;
        testedObject.classLoaderField = testedObject.classField.getClassLoader();

        logWriter.println("\n--> Debuggee: SetValues004Debuggee: Before ObjectReference::SetValues command:");
        logWriter.println("--> intArrayField value = " + testedObject.intArrayField);
        logWriter.println("--> objectArrayField value = " + testedObject.objectArrayField);
        logWriter.println("--> objectField value = " + testedObject.objectField);
        logWriter.println("--> stringField value = " + testedObject.stringField);
        logWriter.println("--> threadField value = " + testedObject.threadField);
        logWriter.println("--> threadGroupField value = " + testedObject.threadGroupField);
        logWriter.println("--> classField value = " + testedObject.classField);
        logWriter.println("--> classLoaderField value = " + testedObject.classLoaderField);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n--> Debuggee: SetValues004Debuggee: After ObjectReference::SetValues command:");
        logWriter.println("--> intArrayField value = " + testedObject.intArrayField);
        if ( testedObject.intArrayField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> objectArrayField value = " + testedObject.objectArrayField);
        if ( testedObject.objectArrayField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> objectField value = " + testedObject.objectField);
        if ( testedObject.objectField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> stringField value = " + testedObject.stringField);
        if ( testedObject.stringField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> threadField value = " + testedObject.threadField);
        if ( testedObject.threadField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> threadGroupField value = " + testedObject.threadGroupField);
        if ( testedObject.threadGroupField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> classField value = " + testedObject.classField);
        if ( testedObject.classField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> classLoaderField value = " + testedObject.classLoaderField);
        if ( testedObject.classLoaderField != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        if ( status.equals(failedStatus) ) {
            logWriter.println("\n##> Debuggee: Check status = FAILED");
        } else {   
            logWriter.println("\n--> Debuggee: Check status = PASSED");
        }

        logWriter.println("--> Debuggee: Send check status for SetValues004Test...\n");
        synchronizer.sendMessage(status);

        logWriter.println("--> Debuggee: SetValues004Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(SetValues004Debuggee.class);
    }
}

class SetValues004DebuggeeThread extends Thread {
    public void myMethod() {
    }
}
