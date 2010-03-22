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
 * Created on 09.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.TopLevelThreadGroups command.
 */
public class TopLevelThreadGroupsTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.TopLevelThreadGroups command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.TopLevelThreadGroups command 
     * and checks that:
     * <BR>&nbsp;&nbsp; - number of returned thread groups is equal to 1;
     * <BR>&nbsp;&nbsp; - there are no extra data in the reply packet;
     * <BR>Also the test prints information about returned thread groups.
     */
    public void testTopLevelThreadGroups001() {
        logWriter.println("\n==> testTopLevelThreadGroups001: START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("==> Send VirtualMachine::TopLevelThreadGroups command...");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.TopLevelThreadGroupsCommand);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::TopLevelThreadGroups command");

        int groups = reply.getNextValueAsInt();
        logWriter.println("==> Returned number of groups = " + groups);
        assertEquals(1, groups);

        for (int i = 0; i < groups; i++) {

            long threadGroupID = reply.getNextValueAsThreadGroupID() ;
            logWriter.println("\n==> Print info about ThreadGroup[" + i + "]... ");
            printThreadGroup(threadGroupID);

        }
        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    private void printThreadGroup(long rootID) {
        logWriter.println("==> ThreadGroupID = " + rootID);
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadGroupReferenceCommandSet.NameCommand);
        packet.setNextValueAsThreadGroupID(rootID);
        ReplyPacket replyParent = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(replyParent, "ThreadGroupReference::Name command");

        String threadGroupIDName = replyParent.getNextValueAsString();
        logWriter.println("==> threadGroupIDName = |" + threadGroupIDName +"|");

        logWriter.println("==> Send ThreadGroupReference::Children command...");
        packet = new CommandPacket(
                JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadGroupReferenceCommandSet.ChildrenCommand);
        packet.setNextValueAsThreadGroupID(rootID);
        ReplyPacket replyChilds = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(replyChilds, "ThreadGroupReference::Children command");
        
        int childThreads = replyChilds.getNextValueAsInt();
        logWriter.println("==> Returned child threads: " + childThreads);

        for (int j = 0; j < childThreads; j++) {
            long id = replyChilds.getNextValueAsThreadID();
            logWriter.println("\n==> childThreadID[" + j + "] = " + id);

            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.NameCommand);
            packet.setNextValueAsThreadID(id);
            replyParent = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(replyParent, "ThreadReference::Name command");
            
            String name = replyParent.getNextValueAsString();
            logWriter.println("==> childThreadName[" + j + "] = " + name);
        }

        int childGroups = replyChilds.getNextValueAsInt();
        logWriter.println("\n==> Returned child groups: " + childGroups);

        for (int j = 0; j < childGroups; j++) {
            long id = replyChilds.getNextValueAsThreadGroupID();
            logWriter.println("\n==> childGroupID[" + j + "] = " + id);

            packet = new CommandPacket(
                    JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadGroupReferenceCommandSet.NameCommand);
            packet.setNextValueAsThreadGroupID(id);
            replyParent = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(replyParent, "ThreadGroupReference::Name command");
            
            String name = replyParent.getNextValueAsString();
            logWriter.println("==> childGroupName[" + j + "] = " + name);
            
            logWriter.println("\n==> Print info about child ThreadGroup \"main\"... ");
            if ("main".equals(name)) {
                printThreadGroup(id);
            }
        }
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TopLevelThreadGroupsTest.class);
    }
}
