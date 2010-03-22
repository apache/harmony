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
 * Created on 11.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class GetValues002Debuggee extends SyncDebuggee {
    
    static int staticIntField;
    static long staticLongField;
    static String staticStringField;
    static Object staticObjectField;
    static boolean staticBooleanField;
    static byte staticByteField;
    static char staticCharField;
    static short staticShortField;
    static float staticFloatField;
    static double staticDoubleField;
    static int[] staticArrayField;
    
    static GetValues002Debuggee getValues002DebuggeeField;
    

    public void run() {
        logWriter.println("--> Debuggee: GetValues002Debuggee: START");
        getValues002DebuggeeField = new GetValues002Debuggee();

        staticIntField = 99;
        staticLongField = 2147483647;
        staticStringField = "staticStringField";
        staticObjectField = new Object();
        staticBooleanField = true;
        staticByteField = 1;
        staticCharField = 'a';
        staticShortField = 2;
        staticFloatField = 2;
        staticDoubleField = 3.1;
        staticArrayField = new int[10];

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: GetValues002Debuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(GetValues002Debuggee.class);
    }

}