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
 * @author Aleksander V. Budniy
 */

/**
 * Created on 1.05.2006
 */
package org.apache.harmony.jpda.tests.jdwp.StackFrame;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class PopFramesDebuggee extends SyncDebuggee {
    
    public static final String METHOD_TO_INVOKE_NAME = "methodToInvoke";
    
    public static long methodToInvoke(long timeToSleep) {
        
        Object obj = new Object();
        synchronized (obj) {
            try {
                obj.wait(timeToSleep);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return timeToSleep;
    }

    public static void main(String [] args) {
        runDebuggee(PopFramesDebuggee.class);
    }

    public void run() {
        logWriter.println("Entering nestledMethod1");
        nestledMethod1();
    }
    
    private void nestledMethod1() {
        logWriter.println("Entering nestledMethod2");
        nestledMethod2();
    }

    private void nestledMethod2() {
        logWriter.println("Entering nestledMethod3");
        //next line for breakpoint
        nestledMethod3();
    }
    
    private void nestledMethod3() {
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("Entering nestledMethod4");
        //next line for breakpoint
        nestledMethod4();
    } 

    private void nestledMethod4() {
        boolean boolLocalVariable = true;
        int intLocalVariable = -512;
        String strLocalVariable = "test string";
        logWriter.println("nestledMethod4 achieved");
        logWriter.println("boolLocalVariable = " + boolLocalVariable);
        logWriter.println("intLocalVariable = " + intLocalVariable);
        logWriter.println("strLocalVariable = " +strLocalVariable);
                       
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("PopFramesDebuggee is ended");        
    }

}