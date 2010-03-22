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
 * Created on 04.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class IsCollectedDebuggee extends SyncDebuggee {
    
    static IsCollectedObject001_01 checkedObject_01;
    static boolean checkedObject_01_Finalized = false; 
    static IsCollectedObject001_02 checkedObject_02;
    static boolean checkedObject_02_Finalized = false; 
    static IsCollectedObject001_03 checkedObject_03;
    static boolean checkedObject_03_Finalized = false; 

    public void run() {
        logWriter.println("--> Debuggee: IsCollectedDebuggee: START");
        
        checkedObject_01 = new IsCollectedObject001_01();
        checkedObject_02 = new IsCollectedObject001_02();
        checkedObject_03 = new IsCollectedObject001_03();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        String messageFromTest = synchronizer.receiveMessage();
        if ( messageFromTest.equals("TO_FINISH")) {
            logWriter.println("--> Debuggee: IsCollectedDebuggee: FINISH");
            return;
        }
        
        checkedObject_01 = null;
        checkedObject_02 = null;
        checkedObject_03 = null;
        long[][] longArray;
        int i = 0;
        try {
            longArray = new long[1000000][];
            int arraysNumberLimit = 7; // max - longArray.length
            logWriter.println
            ("--> Debuggee: memory depletion - creating 'long[1000000]' arrays (" + arraysNumberLimit + ")..."); 
            for (; i < arraysNumberLimit; i++) {
                longArray[i] = new long[1000000];
            }
        } catch ( OutOfMemoryError outOfMem ) {
            logWriter.println("--> Debuggee: OutOfMemoryError!!!");
            // logWriter.println("--> Debuggee: i = " + i);
        }
        longArray = null;
        System.gc();
        logWriter.println("--> Debuggee: AFTER System.gc():");
        logWriter.println("--> Debuggee: checkedObject_01 = " + 
                checkedObject_01);
        logWriter.println("--> Debuggee: checkedObject_01_UNLOADed = " + 
                checkedObject_01_Finalized);
        logWriter.println("--> Debuggee: checkedObject_02 = " + 
                checkedObject_02);
        logWriter.println("--> Debuggee: checkedObject_02_UNLOADed = " + 
                checkedObject_02_Finalized);
        logWriter.println("--> Debuggee: checkedObject_03 = " + 
                checkedObject_03);
        logWriter.println("--> Debuggee: checkedObject_03_UNLOADed = " + 
                checkedObject_03_Finalized);

        String messageForTest = null;
        if ( checkedObject_01_Finalized ) {
            if ( checkedObject_02_Finalized ) {
                messageForTest = "checkedObject_01 is UNLOADed; checkedObject_02 is UNLOADed;";
            } else {
                messageForTest = "checkedObject_01 is UNLOADed; checkedObject_02 is NOT UNLOADed;";
            }
        } else {
            if ( checkedObject_02_Finalized ) {
                messageForTest = "checkedObject_01 is NOT UNLOADed; checkedObject_02 is UNLOADed;";
            } else {
                messageForTest = "checkedObject_01 is NOT UNLOADed; checkedObject_02 is NOT UNLOADed;";
            }
        }
        logWriter.println("--> Debuggee: Send to test message: \"" + messageForTest + "\"");
        synchronizer.sendMessage(messageForTest);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("--> Debuggee: IsCollectedDebuggee: FINISH");

    }

    public static void main(String [] args) {
        runDebuggee(IsCollectedDebuggee.class);
    }

}

class IsCollectedObject001_01 {
    protected void finalize() throws Throwable {
        IsCollectedDebuggee.checkedObject_01_Finalized = true;
        super.finalize();
    }
}   

class IsCollectedObject001_02 {
    protected void finalize() throws Throwable {
        IsCollectedDebuggee.checkedObject_02_Finalized = true;
        super.finalize();
    }
}   

class IsCollectedObject001_03 {
    protected void finalize() throws Throwable {
        IsCollectedDebuggee.checkedObject_03_Finalized = true;
        super.finalize();
    }
}   
