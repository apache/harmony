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

public class GetValuesDebuggee extends SyncDebuggee {
    
    static GetValuesDebuggee getValuesDebuggeeObject;

    int intField;
    long longField;
    String stringField;
    GetValuesDebuggee objectField;
    String[] stringArrayField;
    GetValuesDebuggee[] objectArrayField;
    Thread threadField;
    ThreadGroup threadGroupField;
    Class classField;
    ClassLoader classLoaderField;

    public void run() {
        logWriter.println("--> Debuggee: GetValuesDebuggee: START");
        getValuesDebuggeeObject = new GetValuesDebuggee();

        getValuesDebuggeeObject.intField = 9999;
        getValuesDebuggeeObject.longField = 999999;
        getValuesDebuggeeObject.objectField = new GetValuesDebuggee();
        getValuesDebuggeeObject.stringField = "stringField";
        getValuesDebuggeeObject.stringArrayField = new String[1];
        getValuesDebuggeeObject.stringArrayField[0] = "stringArrayField";
        getValuesDebuggeeObject.objectArrayField = new GetValuesDebuggee[1];
        getValuesDebuggeeObject.objectArrayField[0] = new GetValuesDebuggee();
        getValuesDebuggeeObject.threadField = new MyThread();
        getValuesDebuggeeObject.threadGroupField = new ThreadGroup("ThreadGroupName");
        getValuesDebuggeeObject.classField = GetValuesDebuggee.class;
        getValuesDebuggeeObject.classLoaderField = getValuesDebuggeeObject.classField.getClassLoader();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: GetValuesDebuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(GetValuesDebuggee.class);
    }

}

class MyThread extends Thread {
    public void myMethod() {
    }
}
        
        
