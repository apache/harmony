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
 * JDWP Unit test for VirtualMachine.CreateString command.
 */
public class CreateStringTest extends JDWPSyncTestCase {

    static final String CHECKED_STRING = "Hello World!"; 

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.CreateString command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.CreateString command 
     * for some string and checks that StringReference.Value command
     * returns the same string.
     */
    public void testCreateString001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.CreateStringCommand);
        packet.setNextValueAsString(CHECKED_STRING);
        logWriter.println("\tcreate string: " + CHECKED_STRING);

        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::CreateString command");

        long stringID = reply.getNextValueAsStringID();

        packet = new CommandPacket(
                JDWPCommands.StringReferenceCommandSet.CommandSetID,
                JDWPCommands.StringReferenceCommandSet.ValueCommand);
        packet.setNextValueAsObjectID(stringID);
        logWriter.println("\trequest string value");

        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "StringReference::Value command");

        String value = reply.getNextValueAsString();
        logWriter.println("\tgot string value: " + value);

        assertString("StringReference::Value command returned invalid string value,", CHECKED_STRING, value);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CreateStringTest.class);
    }
}
