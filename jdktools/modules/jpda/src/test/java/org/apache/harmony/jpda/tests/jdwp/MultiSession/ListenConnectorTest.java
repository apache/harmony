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
 * Created on 12.08.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test to check capacity for work of listening connector.
 */
public class ListenConnectorTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.MultiSession.ConnectorKindDebuggee";
    }

    /**
     * Sets kind of connection: listen kind.
     */
    protected void beforeDebuggeeStart(JDWPUnitDebuggeeWrapper debuggeeWrapper) {
        settings.setListenConnectorKind();
        logWriter.println("LISTEN connector kind");
        super.beforeDebuggeeStart(debuggeeWrapper);
    }

    /**
     * This testcase checks capacity for work of listening connector.
     * <BR>Before debuggee start it sets up connector kind to listening and starts
     * ConnectorKindDebuggee. Then testcase performs VirtualMachine.Version
     * command and checks it's correctness. 
     * It is expected that command returns not empty strings describing VM version.
     */
    public void testListenConnector() {

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.VersionCommand);

        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::Version command");

        String description = reply.getNextValueAsString();
        int jdwpMajor = reply.getNextValueAsInt();
        int jdwpMinor = reply.getNextValueAsInt();
        String vmVersion = reply.getNextValueAsString();
        String vmName = reply.getNextValueAsString();

        logWriter.println("description\t= " + description);
        logWriter.println("jdwpMajor\t= " + jdwpMajor);
        logWriter.println("jdwpMinor\t= " + jdwpMinor);
        logWriter.println("vmVersion\t= " + vmVersion);
        logWriter.println("vmName\t\t= " + vmName);

        if (!(description.length() > 0)) {
            logWriter.println("\n## FAILURE: description.length = 0");
            fail("description.length = 0");
        }

        if (!(vmVersion.length() > 0)) {
            logWriter.println("\n## FAILURE: vmVersion.length = 0");
            fail("vmVersion.length = 0");
        }

        if (!(vmName.length() > 0)) {
            logWriter.println("\n## FAILURE: vmName.length = 0");
            fail("vmName.length = 0");
        }
        
        logWriter.println("CHECK PASSED");
        logWriter.println("System property: "
                + System.getProperty("isDebuggeeRunning"));

        synchronizer.sendMessage("stop");
        synchronizer.receiveMessage("END");
        logWriter.println("==> testListenConnector001 PASSED!");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ListenConnectorTest.class);
    }
}
