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
package org.apache.harmony.jpda.tests.jdwp.ArrayReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for JDWP SetValues002Test unit test which 
 * exercises ArrayReference.SetValues command.
 */
public class SetValues002Debuggee extends SyncDebuggee {
    
    static String passedStatus = "PASSED";
    static String failedStatus = "FAILED";
    static String status = passedStatus;

    static SetValues002Debuggee objectArrayField[]; // JDWP_TAG_ARRAY = 91

    public void run() {
        logWriter.println("--> Debuggee: SetValues002Debuggee: START");

        objectArrayField = new SetValues002Debuggee[1];
        objectArrayField[0] = new SetValues002Debuggee();

        logWriter.println("\n--> Debuggee: SetValues002Debuggee: Before ObjectReference::SetValues command:");
        logWriter.println("--> objectArrayField[0] value = " + objectArrayField[0]);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n--> Debuggee: SetValues002Debuggee: After ObjectReference::SetValues command:");
        logWriter.println("--> objectArrayField[0] value = " + objectArrayField[0]);
        if ( objectArrayField[0] != null ) {
            logWriter.println("##> Debuggee: FAILURE: Unexpected value");
            logWriter.println("##> Expected value = " + null);
            status = failedStatus;
        } else {
            logWriter.println("--> Debuggee: OK. Expected value");
        }

        logWriter.println("--> Debuggee: Send check status for SetValues002Test...\n");
        synchronizer.sendMessage(status);

        logWriter.println("--> Debuggee: SetValues002Debuggee: FINISH");
    }

    /**
     * Starts SetValues002Debuggee with help of runDebuggee() method 
     * from <A HREF="../../share/Debuggee.html">Debuggee</A> super class.
     *  
     */
    public static void main(String [] args) {
        runDebuggee(SetValues002Debuggee.class);
    }
}
