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
 * @author Vitaly A. Provodin, Aleksander V. Budniy
 */

/**
 * Created on 31.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ArrayType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for JDWP NewInstanceTest unit test which 
 * exercises ArrayType.NewInstance command.
 */
public class NewInstanceDebuggee extends SyncDebuggee {

    public void run() {
        String[] checkString = {"line1"};
        int[] checkInt = {1};
        int[][] ia = { { 123 }, { 23, 34 }, { 2, 4, 6, 8 } };
        checkClass[] clazz = {new checkClass()};
        checkClass[][] ca = {{new checkClass()}, {new checkClass()}};
        logWriter.println("-> array of classes "+ clazz[0].name + " is created");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("-> Hello World");
        logWriter.println("DUMP{" + checkString + checkInt + ia + ca + "}");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * Starts NewInstanceDebuggee with help of runDebuggee() method 
     * from <A HREF="../../share/Debuggee.html">Debuggee</A> super class.
     *  
     */
    public static void main(String [] args) {
        runDebuggee(NewInstanceDebuggee.class);
    }
}

class checkClass {
   public String name = "checkClass"; 
}
