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
 * Created on 30.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class RedefineClassesDebuggee extends SyncDebuggee {
    
    static RedefineClassesDebuggee redefineClassesDebuggee;
    static RedefineClass_Debuggee redefineClass_DebuggeeObject = null;

    public void run() {
        redefineClassesDebuggee = this;
        
        logWriter.println("--> Debuggee: RedefineClassesDebuggee: START");
        logWriter.println("--> Debuggee: BEFORE redefine: RedefineClass_Debuggee.testMethod() = "
            + RedefineClass_Debuggee.testMethod());
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        String testMethodResult = RedefineClass_Debuggee.testMethod();
        logWriter.println("--> Debuggee: After redefine: RedefineClass_Debuggee.testMethod() = "
                + testMethodResult);
        synchronizer.sendMessage(testMethodResult);
        logWriter.println("--> Debuggee: RedefineClassesDebuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(RedefineClassesDebuggee.class);
    }

}

// Next class will be redefined
class RedefineClass_Debuggee {
    
    static String testMethod() {
        return "testMethod_Result_Before_Redefine";
        // return "testMethod_Result_After_Redefine";
    }
}   

