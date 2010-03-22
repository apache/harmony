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
 * Created on 03.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class MonitorInfoDebuggee extends SyncDebuggee {
    
    static Object lockObject;

    public void run() {
        logWriter.println("--> Debuggee: MonitorInfoDebuggee: START");
        lockObject = new Object();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String messageFromTest = synchronizer.receiveMessage();
        if ( messageFromTest.equals("TO_FINISH") ) {
            logWriter.println("--> Debuggee: MonitorInfoDebuggee: FINISH");
            return;
        }
        synchronized (lockObject) {
            synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
            synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        }
        logWriter.println("--> Debuggee: MonitorInfoDebuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(MonitorInfoDebuggee.class);
    }

}
