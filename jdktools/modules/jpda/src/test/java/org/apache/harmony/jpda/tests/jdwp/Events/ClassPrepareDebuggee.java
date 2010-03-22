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
 * @author Anton V. Karnachuk
 */

/**
 * Created on 07.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for ClassPrepareTest unit test.
 * Loads Class2Prepare class to trace CLASS_PREPARE event.
 */
public class ClassPrepareDebuggee extends SyncDebuggee {

    public static void main(String[] args) {
        runDebuggee(ClassPrepareDebuggee.class);
    }

    public void run() {
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        logWriter.println("ClassPrepareDebuggee started");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        try {
            // Test class prepare
            logWriter.println("--> Try to load and prepare class Class2Prepare");
            Class.forName("org.apache.harmony.jpda.tests.jdwp.Events.Class2Prepare");
            
            // Prepare for SourceNameMatch case
            logWriter.println("--> Try to load and prepare SourceDebugExtensionMockClass");
            Class.forName("org.apache.harmony.jpda.tests.jdwp.Events.SourceDebugExtensionMockClass");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        logWriter.println("ClassPrepareDebuggee finished");
        
    }
    
}
