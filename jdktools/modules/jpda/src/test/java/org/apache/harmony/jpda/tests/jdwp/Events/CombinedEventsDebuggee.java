/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Aleksander V. Budniy
 */

/**
 * Created on 25.05.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;



/**
 * Debuggee for JDWP unit tests for CombinedEvents.
 * Calls it's own sampleMethod method.
 */
public class CombinedEventsDebuggee extends SyncDebuggee {
    public static CombinedEventsDebuggee combinedEventsDebuggee = null;

    public static void main(String[] args) {
        runDebuggee(CombinedEventsDebuggee.class);
    }
    
    public void sampleMethod() {
        logWriter.println("-> CombinedEventsDebuggee: inside of sampleMethod()!");
    }
    
       
    public void run() {
        logWriter.println("-> CombinedEventsDebuggee: Starting...");
        combinedEventsDebuggee = this;
        
        //DBG synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        String mainThreadName = Thread.currentThread().getName();
        synchronizer.sendMessage(mainThreadName);
              
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        logWriter.println("-> CombinedEventsDebuggee: Before calling sampleMethod");
        
        sampleMethod();
        // new CombinedEventsDebuggee_Extra().proxiMethod();
        
        logWriter.println("-> CombinedEventsDebuggee: Finishing...");
    }
}

class CombinedEventsDebuggee_Extra {
    public void proxiMethod() {
        CombinedEventsDebuggee.combinedEventsDebuggee.sampleMethod();
    }
}
