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
 * Created on 25.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadGroupReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadGroupReference.Parent command.
 */
public class ParentTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadGroupReference.NameDebuggee";
    }

    /**
     * This testcase exercises ThreadGroupReference.Parent command.
     * <BR>At first the test starts NameDebuggee.
     * <BR> Then the test with help of the ThreadGroupReference.Parent command checks
     * that the parent group of the group of the tested thread is group
     * with name 'PARENT_GROUP'.
     *  
     */
    public void testParent001() {
        logWriter.println("wait for SGNL_READY");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        CommandPacket packet;
        long threadID = debuggeeWrapper.vmMirror.getThreadID(NameDebuggee.TESTED_THREAD);

        long groupID;
        String groupName;

        // getting the thread group ID
        packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.ThreadGroupCommand);
        packet.setNextValueAsThreadID(threadID);
       
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::ThreadGroup command");

        groupID = reply.getNextValueAsThreadGroupID();

        packet = new CommandPacket(
                JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadGroupReferenceCommandSet.ParentCommand);
        packet.setNextValueAsThreadID(groupID);
        
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadGroupReference::Parent command");

        groupID = reply.getNextValueAsThreadGroupID();

        groupName = debuggeeWrapper.vmMirror.getThreadGroupName(groupID);

        logWriter.println("\tgroupID=" + groupID
                    + "; groupName=" + groupName);

        assertString("ThreadGroupReference::Parent command returned invalid group name,",
                NameDebuggee.PARENT_GROUP, groupName);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ParentTest.class);
    }
}
