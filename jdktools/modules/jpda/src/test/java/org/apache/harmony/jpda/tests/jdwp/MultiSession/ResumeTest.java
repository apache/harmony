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
 * Created on 8.7.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for verifying auto resuming debuggee after re-connection.
 */
public class ResumeTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.MultiSession.ResumeDebuggee";
    }

    /**
     * This testcase verifies auto resuming debuggee after re-connection.
     * <BR>It runs ResumeDebuggee, suspends debuggee with VirtualMachine.Suspend
     * commands and re-connects.
     * <BR>It is expected that debuggee is auto resumed after re-connection.
     */
    public void testResume() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        for(int i = 0; i < 3; i++) {
            debuggeeWrapper.vmMirror.suspend();
        }

        logWriter.println("");
        logWriter.println("CLOSE CONNECTION");
        closeConnection();
        logWriter.println("CONNECTION IS CLOSED");

        logWriter.println("");
        logWriter.println("OPEN NEW CONNECTION");
        openConnection();
        logWriter.println("CONNECTION IS OPENED");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("DEBUGGEE WAS RESUMED");

        logWriter.println("TEST PASSED");
    }
    
    protected void beforeDebuggeeStart(JDWPUnitDebuggeeWrapper debuggeeWrapper) {
        settings.setAttachConnectorKind();
        if (settings.getTransportAddress() == null) {
            settings.setTransportAddress(TestOptions.DEFAULT_ATTACHING_ADDRESS);
        }
        logWriter.println("ATTACH connector kind");
        super.beforeDebuggeeStart(debuggeeWrapper);
    }

    public static void main(String[] args) {
       junit.textui.TestRunner.run(ResumeTest.class);
    }
}
