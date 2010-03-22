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

public class GetValues003Debuggee extends SyncDebuggee {
    
    static GetValues003Debuggee testedObject;

    int intArrayField[]; // JDWP_TAG_ARRAY = 91
    GetValues003Debuggee objectArrayField[]; // JDWP_TAG_ARRAY = 91
    GetValues003Debuggee objectField; // JDWP_TAG_OBJECT = 76
    String stringField; // JDWP_TAG_STRING = 115
    Thread threadField; // JDWP_TAG_THREAD = 116
    ThreadGroup threadGroupField; // JDWP_TAG_THREAD_GROUP = 103
    Class classField; // JDWP_TAG_CLASS_OBJECT = 99
    ClassLoader classLoaderField; // DWP_TAG_CLASS_LOADER = 108
    
    
    
    public void run() {
        logWriter.println("--> Debuggee: GetValues003Debuggee: START");
        testedObject = new GetValues003Debuggee();

        testedObject.intArrayField = new int[1];
        testedObject.intArrayField[0]= 999;
        testedObject.objectArrayField = new GetValues003Debuggee[1];
        testedObject.objectArrayField[0] = new GetValues003Debuggee();
        testedObject.objectField = new GetValues003Debuggee();
        testedObject.stringField = "stringField";
        testedObject.threadField = new GetValues003DebuggeeThread();
        testedObject.threadGroupField = new ThreadGroup("ThreadGroupName");
        testedObject.classField = GetValues003Debuggee.class;
        testedObject.classLoaderField = testedObject.classField.getClassLoader();

        testedObject.intArrayField = null;
        testedObject.objectArrayField = null;
        testedObject.objectField = null;
        testedObject.stringField = null;
        testedObject.threadField = null;
        testedObject.threadGroupField = null;
        testedObject.classField = null;
        testedObject.classLoaderField = null;

        logWriter.println("\n--> Debuggee: GetValues003Debuggee: Before ObjectReference::GetValues command:");
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

        logWriter.println("--> Debuggee: GetValues003Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(GetValues003Debuggee.class);
    }
}

class GetValues003DebuggeeThread extends Thread {
    public void myMethod() {
    }
}
