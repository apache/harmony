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
 * JDWP Unit test for ThreadGroupReference.Children command.
 */
public class ChildrenTest extends JDWPSyncTestCase {


    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadGroupReference.ChildrenDebuggee";
    }

    /**
     * This testcase exercises ThreadGroupReference.Children command.
     * <BR>At first the test starts ChildrenDebuggee.
     * <BR> Then the test with help of the ThreadGroupReference.Children command checks
     * that the group 'PARENT_GROUP' has one child thread - 'TESTED_THREAD' 
     * and one child group - 'CHILD_GROUP'.
     *  
     */
    public void testChildren001() {
        logWriter.println("==> ChildrenTest.testChildren001 START...");
        logWriter.println("==> Wait for SGNL_READY...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        logWriter.println("==> Get testedThreadID...");
        CommandPacket packet;
        long threadID = debuggeeWrapper.vmMirror.getThreadID(NameDebuggee.TESTED_THREAD);
        logWriter.println("==> testedThreadID = " + threadID);

        long groupID;
        String groupName;

        // getting the thread group ID
        logWriter.println("==> Send ThreadReference.ThreadGroup command for testedThreadID...");
        packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.ThreadGroupCommand);
        packet.setNextValueAsThreadID(threadID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference.ThreadGroup command");
        
        groupID = reply.getNextValueAsThreadGroupID();
        logWriter.println("==> groupID = " + groupID);

        logWriter.println("==> Send ThreadGroupReference.Children command...");
        packet = new CommandPacket(
                JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadGroupReferenceCommandSet.ChildrenCommand);
        packet.setNextValueAsThreadID(groupID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadGroupReference.Children command");

        logWriter.println("\n==> Children of the group: \""
                + debuggeeWrapper.vmMirror.getThreadGroupName(groupID)
                + "\": ");

        int childThreads = reply.getNextValueAsInt();
        if (childThreads != 1) {
            logWriter.println("## FAILURE: Unexpected number of child threads = " + childThreads);
            logWriter.println("## Expected number of child threads = 1");
            assertEquals("Invalid number of child threads,", 1, childThreads);
        }
        long childThreadID = reply.getNextValueAsThreadID();
        String threadName = debuggeeWrapper.vmMirror.getThreadName(childThreadID);
        logWriter.println
        ("==> thread: threadID = " + childThreadID + "; threadName = " + threadName);
                
        if (threadID != childThreadID) {
            logWriter.println("## FAILURE: Unexpected ID of child thread = " + childThreadID);
            logWriter.println("## Expected ID of child thread = " + threadID);
            assertEquals("Invalid ID of child thread,", threadID, childThreadID);
        }
        if (!threadName.equals(NameDebuggee.TESTED_THREAD)) {
            logWriter.println("## FAILURE: unexpected thread name, it is expected: "
                    + NameDebuggee.TESTED_THREAD);
            assertString("Invalid thread name,", NameDebuggee.TESTED_THREAD, threadName);
        }

        int childGroups = reply.getNextValueAsInt();
        if (childGroups != 1) {
            logWriter.println("## FAILURE: Unexpected number of child groups " + childGroups);
            logWriter.println("## Expected number = 1");
            assertEquals("Invalid number of child groups,", 1, childGroups);
        }

        groupID = reply.getNextValueAsThreadGroupID();
        groupName = debuggeeWrapper.vmMirror.getThreadGroupName(groupID);

        logWriter.println("\n==> group: groupID = " + groupID + "; groupName = " + groupName);

        assertString("Invalid group name,", NameDebuggee.CHILD_GROUP, groupName);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ChildrenTest.class);
    }
}
