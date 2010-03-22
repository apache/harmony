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
 * Created on 05.07.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class SetValues002Debuggee extends SyncDebuggee {
    
    static String passedStatus = "PASSED";
    static String failedStatus = "FAILED";
    static String status = passedStatus;

    static SetValues002Debuggee SetValues002DebuggeeObject;

    static SetValues002Debuggee_ExtraClass objectField;
    static SetValues002Debuggee_ExtraClass objectFieldCopy;

    public void run() {
        logWriter.println("--> Debuggee: SetValues002Debuggee: START");
        SetValues002DebuggeeObject = new SetValues002Debuggee();
        objectField = new SetValues002Debuggee_ExtraClass();
        objectFieldCopy = objectField;

        logWriter.println("\n--> Debuggee: Before ClassType::SetValues command:");
        logWriter.println("--> objectField value = " + objectField);
        logWriter.println("--> value to set = " + SetValues002DebuggeeObject);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n--> Debuggee:  After ClassType::SetValues command:");
        logWriter.println("--> objectField value = " + objectField);
        if ( ! objectFieldCopy.equals(objectField) ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + objectFieldCopy);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: PASSED: Expected value");
        }

        if ( status.equals(failedStatus) ) {
            logWriter.println("\n##> Debuggee: Check status = FAILED");
        } else {   
            logWriter.println("\n--> Debuggee: Check status = PASSED");
        }

        logWriter.println("--> Debuggee: Send check status for SetValues002Test...\n");
        synchronizer.sendMessage(status);

        logWriter.println("--> Debuggee: SetValues002Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(SetValues002Debuggee.class);
    }
}

class SetValues002Debuggee_ExtraClass {
   
}
        
