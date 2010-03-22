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
 * Created on 10.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class GetValues002Debuggee extends SyncDebuggee {
    
    int nonStaticIntField;
    long nonStaticLongField;
    String nonStaticStringField;
    Object nonStaticObjectField;
    boolean nonStaticBooleanField;
    byte nonStaticByteField;
    char nonStaticCharField;
    short nonStaticShortField;
    float nonStaticFloatField;
    double nonStaticDoubleField;
    int[] nonStaticArrayField;
    
    static GetValues002Debuggee getValues002DebuggeeField;
    

    public void run() {
        logWriter.println("--> Debuggee: GetValues002Debuggee: START");
        getValues002DebuggeeField = new GetValues002Debuggee();

        getValues002DebuggeeField.nonStaticIntField = 99;
        getValues002DebuggeeField.nonStaticLongField = 2147483647;
        getValues002DebuggeeField.nonStaticStringField = "nonStaticStringField";
        getValues002DebuggeeField.nonStaticObjectField = new Object();
        getValues002DebuggeeField.nonStaticBooleanField = true;
        getValues002DebuggeeField.nonStaticByteField = 1;
        getValues002DebuggeeField.nonStaticCharField = 'a';
        getValues002DebuggeeField.nonStaticShortField = 2;
        getValues002DebuggeeField.nonStaticFloatField = 2;
        getValues002DebuggeeField.nonStaticDoubleField = 3.1;
        getValues002DebuggeeField.nonStaticArrayField = new int[10];

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: GetValues002Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(GetValues002Debuggee.class);
    }

}