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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 10.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.CapabilitiesNew command.
 */
public class CapabilitiesNewTest extends JDWPSyncTestCase {

    static Object [][] capabilitiesFlags = {
            {"canWatchFieldModification", new Object()},   
            {"canWatchFieldAccess", new Object()},
            {"canGetBytecodes", new Object()},
            {"canGetSyntheticAttribute", new Object()},
            {"canGetOwnedMonitorInfo", new Object()},
            {"canGetCurrentContendedMonitor", new Object()},
            {"canGetMonitorInfo", new Object()},
            {"canRedefineClasses", new Object()},   
            {"canAddMethod", null},
            {"canUnrestrictedlyRedefineClasses", null},
            {"canPopFrames", new Object()},
            {"canUseInstanceFilters", new Object()},
            {"canGetSourceDebugExtension", new Object()},
            {"canRequestVMDeathEvent", new Object()},
            {"canSetDefaultStratum", null},
            {"reserved16", null},
            {"reserved17", null},
            {"reserved18", null},
            {"reserved19", null},
            {"reserved20", null},
            {"reserved21", null},
            {"reserved22", null},
            {"reserved23", null},
            {"reserved24", null},
            {"reserved25", null},
            {"reserved26", null},
            {"reserved27", null},
            {"reserved28", null},
            {"reserved29", null},
            {"reserved30", null},
            {"reserved31", null},
            {"reserved32", null} 
    }; 

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.CapabilitiesNew command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.CapabilitiesNew command and checks that:
     * all returned capabilities' values are expected values and that
     * there are no extra data in the reply packet;
     */
    public void testCapabilitiesNew001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.CapabilitiesNewCommand);
        logWriter.println("\trequest capabilities");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::CapabilitiesNew command");

        boolean flag;
        boolean testFailed = false;
        for (int i = 0; i < 32; i++) {
            flag = reply.getNextValueAsBoolean();   
            logWriter.println("\tReceived " + capabilitiesFlags[i][0] +  " = "
                    + flag);
            if ( (capabilitiesFlags[i][1] != null) !=  flag ) {
                testFailed = true;
                logWriter.println("\t   ## FAILURE: Expected " + capabilitiesFlags[i][0] + 
                        " = " + (capabilitiesFlags[i][1] != null));
            } else {
                logWriter.println("\t   OK - it is expected value!");
            }
        }
        if ( testFailed ) {
            printErrorAndFail("Unexpected received capabilities values found out");
        } else {
            logWriter.println("testCapabilitiesNew001 - OK!");
        }
        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CapabilitiesNewTest.class);
    }
}
