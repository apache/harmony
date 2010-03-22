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
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class GetValues005Debuggee extends SyncDebuggee {
    
    static int intArrayField[]; // JDWP_TAG_ARRAY = 91
    static GetValues005Debuggee objectArrayField[]; // JDWP_TAG_ARRAY = 91
    static GetValues005Debuggee objectField; // JDWP_TAG_OBJECT = 76
    static String stringField; // JDWP_TAG_STRING = 115
    static Thread threadField; // JDWP_TAG_THREAD = 116
    static ThreadGroup threadGroupField; // JDWP_TAG_THREAD_GROUP = 103
    static Class classField; // JDWP_TAG_CLASS_OBJECT = 99
    static ClassLoader classLoaderField; // DWP_TAG_CLASS_LOADER = 108
    
    
    
    public void run() {
        logWriter.println("--> Debuggee: GetValues005Debuggee: START");

        intArrayField = new int[1];
        intArrayField[0]= 999;
        objectArrayField = new GetValues005Debuggee[1];
        objectArrayField[0] = new GetValues005Debuggee();
        objectField = new GetValues005Debuggee();
        stringField = "stringField";
        threadField = new GetValues005DebuggeeThread();
        threadGroupField = new ThreadGroup("ThreadGroupName");
        classField = GetValues005Debuggee.class;
        classLoaderField = classField.getClassLoader();

        intArrayField = null;
        objectArrayField = null;
        objectField = null;
        stringField = null;
        threadField = null;
        threadGroupField = null;
        classField = null;
        classLoaderField = null;

        logWriter.println("\n--> Debuggee: GetValues005Debuggee: Before ReferenceType::GetValues command:");
        logWriter.println("--> intArrayField value = " + intArrayField);
        logWriter.println("--> objectArrayField value = " + objectArrayField);
        logWriter.println("--> objectField value = " + objectField);
        logWriter.println("--> stringField value = " + stringField);
        logWriter.println("--> threadField value = " + threadField);
        logWriter.println("--> threadGroupField value = " + threadGroupField);
        logWriter.println("--> classField value = " + classField);
        logWriter.println("--> classLoaderField value = " + classLoaderField);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("--> Debuggee: GetValues005Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(GetValues005Debuggee.class);
    }
}

        
class GetValues005DebuggeeThread extends Thread {
    public void myMethod() {
    }
}
