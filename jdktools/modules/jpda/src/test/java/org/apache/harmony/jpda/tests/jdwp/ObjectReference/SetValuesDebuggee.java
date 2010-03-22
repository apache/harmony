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
 * Created on 28.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class SetValuesDebuggee extends SyncDebuggee {
    
    static SetValuesDebuggee setValuesDebuggeeObject;
    static String passedStatus = "PASSED";
    static String failedStatus = "FAILED";
    static String status = passedStatus;

    int intField;
    long longField;
    SetValuesDebuggee objectField;
    static int staticIntField;
    private int privateIntField;
    final int finalIntField = 12345;

    public void run() {
        logWriter.println("--> Debuggee: SetValuesDebuggee: START");
        setValuesDebuggeeObject = new SetValuesDebuggee();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: SetValuesDebuggee: CHECK for set fields...");
        String status = passedStatus;

        logWriter.println("\n--> intField value = " + setValuesDebuggeeObject.intField);
        if ( setValuesDebuggeeObject.intField != 1111 ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = 1111");
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        logWriter.println("\n--> longField value = " + setValuesDebuggeeObject.longField);
        if ( setValuesDebuggeeObject.longField != 22222222 ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = 22222222");
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        logWriter.println("\n--> objectField value = " + setValuesDebuggeeObject.objectField);
        if ( ! setValuesDebuggeeObject.equals(setValuesDebuggeeObject.objectField) ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + setValuesDebuggeeObject);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        logWriter.println("\n--> staticIntField value = " + staticIntField);
        if ( staticIntField != 5555 ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = 5555");
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        logWriter.println("\n--> privateIntField value = " + setValuesDebuggeeObject.privateIntField);
        if ( setValuesDebuggeeObject.privateIntField != 7777 ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = 7777");
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        logWriter.println("\n--> finalIntField value = " + setValuesDebuggeeObject.finalIntField);
        if ( setValuesDebuggeeObject.finalIntField != 12345 ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = 12345");
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        if ( status.equals(failedStatus) ) {
            logWriter.println("\n##> Debuggee: Check status = FAILED");
        } else {   
            logWriter.println("\n--> Debuggee: Check status = PASSED");
        }

        logWriter.println("--> Debuggee: Send check status for SetValuesTest...\n");
        synchronizer.sendMessage(status);
        logWriter.println("--> Debuggee: SetValuesDebuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(SetValuesDebuggee.class);
    }

}

        
