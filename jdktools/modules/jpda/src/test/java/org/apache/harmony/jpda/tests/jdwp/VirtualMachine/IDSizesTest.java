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
 * Created on 31.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPTestCase;


/**
 * JDWP Unit test for VirtualMachine.IDSizes command.
 */
public class IDSizesTest extends JDWPTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.SimpleHelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.IDSizes command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.IDSizes command 
     * and checks that all returned sizes are non-zero values.
     */
    public void testIDSizes001() {

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.IDSizesCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::IDSizes command");

        int fieldIDSize = reply.getNextValueAsInt();
        int methodIDSize = reply.getNextValueAsInt();
        int objectIDSize = reply.getNextValueAsInt();
        int referenceTypeIDSize = reply.getNextValueAsInt();
        int frameIDSize = reply.getNextValueAsInt();
        logWriter.println("fieldIDSize = " + fieldIDSize);
        logWriter.println("methodIDSize = " + methodIDSize);
        logWriter.println("objectIDSize = " + objectIDSize);
        logWriter.println("referenceTypeIDSize = " + referenceTypeIDSize);
        logWriter.println("frameIDSize = " + frameIDSize);

        assertTrue("Invalid fieldIDSize: " + fieldIDSize, fieldIDSize > 0);
        assertTrue("Invalid methodIDSize: " + methodIDSize, methodIDSize > 0);
        assertTrue("Invalid objectIDSize: " + objectIDSize, objectIDSize > 0);
        assertTrue("Invalid referenceTypeIDSize: " + referenceTypeIDSize, referenceTypeIDSize > 0);
        assertTrue("Invalid frameIDSize: " + frameIDSize, frameIDSize > 0);

        debuggeeWrapper.resume();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IDSizesTest.class);
    }
}
