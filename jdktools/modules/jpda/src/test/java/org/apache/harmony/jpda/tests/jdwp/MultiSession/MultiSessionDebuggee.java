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
 * Created on 31.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * This class provides simple HelloWorld debuggee class used sync connection.
 */
public class MultiSessionDebuggee extends SyncDebuggee {

    

    public void run() {
        logWriter.println("Debuggee is started");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("FROM DEBUGGEE: thread was resumed" );
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        printWord();
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        logWriter.println("-> MultiSessionDebuggee: wait for signal to continue...");
        while ( true ) {
            if ( synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE) ) {
               logWriter.println("-> MultiSessionDebuggee: signal received!");
               break;
            }
        }
        
        logWriter.println("-> MultiSessionDebuggee: FINISH...");
    }

    public void printWord() {
        logWriter.println("Hello World ---- " );
    }

    public static void main(String[] args) {
        runDebuggee(MultiSessionDebuggee.class);
    }
}