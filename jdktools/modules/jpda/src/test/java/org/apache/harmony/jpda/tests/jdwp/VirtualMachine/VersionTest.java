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
 * Created on 29.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.Version command.
 */
public class VersionTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.Version command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.Version command 
     * and checks that:
     * <BR>&nbsp;&nbsp; - length of returned description is greater than 0;
     * <BR>&nbsp;&nbsp; - length of returned vmVersion is greater than 0;
     * <BR>&nbsp;&nbsp; - length of returned vmName is greater than 0;
     */
    public void testVersion001() {

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.VersionCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet); 

        String description = reply.getNextValueAsString();
        int    jdwpMajor   = reply.getNextValueAsInt();
        int    jdwpMinor   = reply.getNextValueAsInt();
        String vmVersion   = reply.getNextValueAsString();
        String vmName      = reply.getNextValueAsString();

        logWriter.println("description\t= " + description);
        logWriter.println("jdwpMajor\t= " + jdwpMajor);
        logWriter.println("jdwpMinor\t= " + jdwpMinor);
        logWriter.println("vmVersion\t= " + vmVersion);
        logWriter.println("vmName\t\t= " + vmName);

        if (!(description.length() > 0)) {
            printErrorAndFail("description.length = 0");
        }

        if (!(vmVersion.length() > 0)) {
            printErrorAndFail("vmVersion.length = 0");
        }

        if (!(vmName.length() > 0)) {
            printErrorAndFail("vmName.length = 0");
        }

        logWriter.println("CHECK PASSED");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(VersionTest.class);
    }
}
