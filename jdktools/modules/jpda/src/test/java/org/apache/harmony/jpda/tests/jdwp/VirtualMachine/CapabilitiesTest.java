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
 * JDWP Unit test for VirtualMachine.Capabilities command.
 */
public class CapabilitiesTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.Capabilities command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.Capabilities command and checks that:
     * all returned capabilities' values are expected values and that
     * there are no extra data in the reply packet;
     */
    public void testCapabilities001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.CapabilitiesCommand);
        logWriter.println("\trequest capabilities");
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::Capabilities command");
        
        boolean canWatchFieldModification     = reply.getNextValueAsBoolean();   
        boolean canWatchFieldAccess         = reply.getNextValueAsBoolean();   
        boolean canGetBytecodes             = reply.getNextValueAsBoolean();    
        boolean canGetSyntheticAttribute     = reply.getNextValueAsBoolean();    
        boolean canGetOwnedMonitorInfo         = reply.getNextValueAsBoolean();   
        boolean canGetCurrentContendedMonitor = reply.getNextValueAsBoolean();   
        boolean canGetMonitorInfo             = reply.getNextValueAsBoolean();

        logWriter.println("\tcanWatchFieldModification\t= "
                + canWatchFieldModification);   
        assertTrue("canWatchFieldModification must be true", canWatchFieldModification);
        
        logWriter.println("\tcanWatchFieldAccess\t\t= " + canWatchFieldAccess);
        assertTrue("canWatchFieldAccess must be true", canWatchFieldAccess);
        
        logWriter.println("\tcanGetBytecodes\t\t\t= " + canGetBytecodes);     
        assertTrue("canGetBytecodes must be true", canGetBytecodes);
        
        logWriter.println("\tcanGetSyntheticAttribute\t= "
                + canGetSyntheticAttribute);    
        assertTrue("canGetSyntheticAttribute must be true", canGetSyntheticAttribute);
        
        logWriter.println("\tcanGetOwnedMonitorInfo\t\t= "
                + canGetOwnedMonitorInfo);   
        assertTrue("canGetOwnedMonitorInfo must be true", canGetOwnedMonitorInfo);
        
        logWriter.println("\tcanGetCurrentContendedMonitor\t= "
                + canGetCurrentContendedMonitor);   
        assertTrue("canGetCurrentContendedMonitor must be true", canGetCurrentContendedMonitor);
        
        logWriter.println("\tcanGetMonitorInfo\t\t= " + canGetMonitorInfo);
        assertTrue("canGetMonitorInfo must be true", canGetMonitorInfo);

        assertAllDataRead(reply);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CapabilitiesTest.class);
    }
}
