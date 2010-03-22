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
 * Created on 5.06.2006
 */
package org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand;

import org.apache.harmony.jpda.tests.share.Debuggee;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * This class provides simple debuggee class with deferred synch establishing.
 * 
 * When debuggee throws exception, agent launches debugger, that connects to debuggee. 
 * Then debuggee establishes synch connection with debugger and invokes tested method.
 */
public class OnthowDebuggerLaunchDebuggee extends Debuggee {

    public void onStart() {
        super.onStart();

        logWriter.println("DEBUGGEE: started");
        
        // prepare for connection with debugger
        logWriter.println("DEBUGGEE: bind for synch connection with debugger");
        synchronizer = createSynchronizer();
        synchronizer.bindServer();

        // throw tested exception to launch debugger
        try {
            logWriter.println("DEBUGGEE: throw ExceptionForDebugger");
            throw new ExceptionForDebugger();
        } catch (ExceptionForDebugger e) {
            logWriter.println("DEBUGGEE: caught ExceptionForDebugger: " + e);
        }

        // listen for connection with debugger
        synchronizer.startServer();
        logWriter.println("DEBUGGEE: established synch connection with debugger");
    }

    public void run() {
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testMethod();
    }

    void testMethod() {
        logWriter.println("DEBUGGEE: testMethod invoked");
    }

    protected JPDADebuggeeSynchronizer createSynchronizer() {
        return new JPDADebuggeeSynchronizer(logWriter, settings);
    }

    public void onFinish() {
        logWriter.println("DEBUGGEE: finished");

        if (synchronizer != null) {
            synchronizer.stop();
            logWriter.println("DEBUGGEE: closed synch connection with debugger");
        }
        super.onFinish();
    }

    public static void main(String[] args) {
        runDebuggee(OnthowDebuggerLaunchDebuggee.class);
    }

    protected JPDADebuggeeSynchronizer synchronizer;
}

@SuppressWarnings("serial")
class ExceptionForDebugger extends Exception {
}
