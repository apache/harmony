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
 * Created on 28.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.DisposeObjects command.
 */
public class DisposeObjectsTest extends JDWPSyncTestCase {

    static final String CHECKED_STRING = "Hello World!"; 

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.DisposeObjects command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.CreateString command 
     * for some string and checks that VirtualMachine.DisposeObjects command
     * for returned by CreateString command stringID with refCount = 0
     * does not dispose that stringID - ObjectReference::ReferenceType
     * command should return some referenceTypeID without any error.
     * <BR>Then the test check that repeated VirtualMachine.DisposeObjects command
     * with refCount = 1 disposes that stringID - ObjectReference::ReferenceType
     * command should return INVALID_OBJECT error. 
     */
    public void testDisposeObjects001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.CreateStringCommand);
        packet.setNextValueAsString(CHECKED_STRING);
        logWriter.println("\tcreate string: " + CHECKED_STRING);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);

        long stringID = reply.getNextValueAsStringID();
        logWriter.println("\tstring creared: stringID = " + stringID);

        logWriter.println("\tsend DisposeObjects for created string with refCount = 0" 
                + " - string should not be disposed...");
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.DisposeObjectsCommand);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsObjectID(stringID);
        packet.setNextValueAsInt(0);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::DisposeObjects command");

        logWriter.println
            ("\tsend ObjectReference::ReferenceType command for created string"
            + " to make sure that string is not disposed...");
        packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
        packet.setNextValueAsObjectID(stringID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ObjectReference::ReferenceType command");

        byte refTypeTag = reply.getNextValueAsByte();
        long refTypeID = reply.getNextValueAsReferenceTypeID();
        logWriter.println("\tReturned refTypeTag = " + refTypeTag
                + "(" + JDWPConstants.TypeTag.getName(refTypeTag) + ")");
        logWriter.println("\tReturned ReferenceTypeID for string = " + refTypeID);

        logWriter.println("\tsend DisposeObjects for created string with refCount = 1" 
                + " - string should be disposed...");
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.DisposeObjectsCommand);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsObjectID(stringID);
        packet.setNextValueAsInt(1);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::DisposeObjects command");

        logWriter.println
            ("\tsend ObjectReference::ReferenceType command for disposed string"
            + " - INVALID_OBJECT should be...");
        packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
        packet.setNextValueAsObjectID(stringID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        
        checkReplyPacket(reply, "ObjectReference::ReferenceType command", JDWPConstants.Error.INVALID_OBJECT);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DisposeObjectsTest.class);
    }
}
