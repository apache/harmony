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
 * Created on 25.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class ReferenceTypeDebuggee extends SyncDebuggee {
    
    static Class_ReferenceType001 class_ReferenceType001Object;
    static ReferenceTypeDebuggee[] referenceTypeDebuggeeArray;
    static String[] stringArrayField;
    static String stringField = "stringField";

    public void run() {
        logWriter.println("--> Debuggee: ReferenceTypeDebuggee: START");
        class_ReferenceType001Object = new Class_ReferenceType001();
        referenceTypeDebuggeeArray = new ReferenceTypeDebuggee[1];
        referenceTypeDebuggeeArray[0] = new ReferenceTypeDebuggee();
        stringArrayField = new String[1];
        stringArrayField[0] = "stringArrayField";
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: ReferenceTypeDebuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(ReferenceTypeDebuggee.class);
    }

}

class Class_ReferenceType001 {}
