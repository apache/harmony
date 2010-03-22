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
 * Created on 06.10.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for CombinedEvents002Test JDWP unit test.
 */
public class CombinedEvents002Debuggee extends SyncDebuggee {
    static final String TESTED_CLASS_NAME = 
        CombinedEvents002Debuggee_TestedClass.class.getName(); 
    static final String TESTED_CLASS_SIGNATURE = 
        "L" + TESTED_CLASS_NAME.replace('.','/') + ";";
    static final String TESTED_METHOD_NAME = "emptyTestedMethod";

    public static void main(String[] args) {
        runDebuggee(CombinedEvents002Debuggee.class);
    }
    
    public void run() {
        logWriter.println("--> CombinedEvents002Debuggee: Start...");
        
        logWriter.println("--> CombinedEvents002Debuggee: Send SGNL_READY signal to test...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        logWriter.println("--> CombinedEvents002Debuggee: Wait for SGNL_CONTINUE signal from test...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> CombinedEvents002Debuggee: OK - SGNL_CONTINUE signal received!");
        
        logWriter.println("--> CombinedEvents002Debuggee: " 
            + "Call CombinedEvents002Debuggee_TestedClass.emptyTestedMethod()...");
        CombinedEvents002Debuggee_TestedClass.emptyTestedMethod();
        logWriter.println("--> CombinedEvents002Debuggee: " 
                + "CombinedEvents002Debuggee_TestedClass.emptyTestedMethod() returned.");
        
        logWriter.println("--> CombinedEvents002Debuggee: Finishing...");
    }

}

class CombinedEvents002Debuggee_TestedClass {
    public static void emptyTestedMethod() {
    }
}

