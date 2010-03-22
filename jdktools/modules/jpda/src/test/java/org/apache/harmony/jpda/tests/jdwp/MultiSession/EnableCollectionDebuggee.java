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
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class EnableCollectionDebuggee extends SyncDebuggee {
   
   static EnableCollectionObject001_01 checkedObject;
   static boolean checkedObject_Finalized = false; 
   static EnableCollectionObject001_02 patternObject;
   static boolean patternObject_Finalized = false; 

   public void run() {
       logWriter.println("--> Debuggee: EnableCollectionDebuggee: START");
       
       checkedObject = new EnableCollectionObject001_01();
       patternObject = new EnableCollectionObject001_02();

       synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
       String messageFromTest = synchronizer.receiveMessage();
       if ( messageFromTest.equals("TO_FINISH")) {
           logWriter.println("--> Debuggee: EnableCollectionDebuggee: FINISH");
           return;
       }
       
       logWriter.println("--> Debuggee: BEFORE System.gc():");
       logWriter.println("--> Debuggee: checkedObject = " + 
               checkedObject);
       logWriter.println("--> Debuggee: checkedObject_UNLOADed = " + 
               checkedObject_Finalized);
       logWriter.println("--> Debuggee: patternObject = " + 
               patternObject);
       logWriter.println("--> Debuggee: patternObject_UNLOADed = " + 
               patternObject_Finalized);

       checkedObject = null;
       patternObject = null;
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
       }
       longArray = null;
       System.gc();
       logWriter.println("--> Debuggee: AFTER System.gc():");
       logWriter.println("--> Debuggee: checkedObject = " + 
               checkedObject);
       logWriter.println("--> Debuggee: checkedObject_UNLOADed = " + 
               checkedObject_Finalized);
       logWriter.println("--> Debuggee: patternObject = " + 
               patternObject);
       logWriter.println("--> Debuggee: patternObject_UNLOADed = " + 
               patternObject_Finalized);

       String messageForTest = null;
       if ( checkedObject_Finalized ) {
           if ( patternObject_Finalized ) {
               messageForTest = "Checked Object is UNLOADed; Pattern Object is UNLOADed;";
           } else {
               messageForTest = "Checked Object is UNLOADed; Pattern Object is NOT UNLOADed;";
           }
       } else {
           if ( patternObject_Finalized ) {
               messageForTest = "Checked Object is NOT UNLOADed; Pattern Object is UNLOADed;";
           } else {
               messageForTest = "Checked Object is NOT UNLOADed; Pattern Object is NOT UNLOADed;";
           }
       }
       logWriter.println("--> Debuggee: Send to test message: \"" + messageForTest + "\"");
       synchronizer.sendMessage(messageForTest);
       synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

       logWriter.println("--> Debuggee: EnableCollectionDebuggee: FINISH");

   }

   public static void main(String [] args) {
       runDebuggee(EnableCollectionDebuggee.class);
   }

}

class EnableCollectionObject001_01 {
   protected void finalize() throws Throwable {
       EnableCollectionDebuggee.checkedObject_Finalized = true;
    super.finalize();
   }
}   

class EnableCollectionObject001_02 {
   protected void finalize() throws Throwable {
       EnableCollectionDebuggee.patternObject_Finalized = true;
       super.finalize();
   }
}   
