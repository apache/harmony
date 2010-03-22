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
 * Created on 15.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.ThreadGroup command.
 */
public class ThreadGroupTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.ThreadGroupDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.ThreadGroup command.
     * <BR>At first the test starts ThreadGroupDebuggee which creates 
     * 'TESTED_GROUP' ThreadGroup and starts the 'TESTED_THREAD' thread which 
     * belongs to that thread group.
     * <BR>After the tested thread starts but does not finish, test requests all 
     * debuggee threads by VirtualMachine.AllThreads command and looks for tested thread.
     * <BR>If the tested thread is not found the test fails.
     * <BR>If the tested thread is found the test checks that 
     * ThreadReference.ThreadGroup command for tested thread returns 'TESTED_GROUP' thread group.
     */
    public void testThreadGroup001() {
        logWriter.println("wait for SGNL_READY");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        CommandPacket packet;
        logWriter.println("get all threads");
        ReplyPacket replyThread, reply = debuggeeWrapper.vmMirror.getAllThreadID();
        int threads = reply.getNextValueAsInt();
        logWriter.println("exercise threads = " + threads);

        long threadID, groupID;
        String groupName, threadName;
        int count = 0;

        for (int i = 0; i < threads; i++) {
            threadID = reply.getNextValueAsThreadID();
            
            // getting the thread group ID
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.ThreadGroupCommand);
            packet.setNextValueAsThreadID(threadID);
            
            replyThread = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(replyThread, "ThreadReference::ThreadGroup command");

            groupID = replyThread.getNextValueAsThreadGroupID();

            groupName = debuggeeWrapper.vmMirror.getThreadGroupName(groupID);
            threadName = debuggeeWrapper.vmMirror.getThreadName(threadID);

            logWriter.println("\tthreadID=" + threadID
                    + "; threadName=" + threadName
                    + "; groupID=" + groupID
                    + "; groupName=" + groupName);

            if (threadName.equals(ThreadGroupDebuggee.TESTED_THREAD)) {
                if (!groupName.equals(ThreadGroupDebuggee.TESTED_GROUP)) {
                    printErrorAndFail("unexpected group name, it is expected: "
                            + ThreadGroupDebuggee.TESTED_GROUP);
                }
                count++;
            }
        }
        if (count == 0) {
            printErrorAndFail("Tested thread is not found in all_threads list.");
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("waiting for finishing thread");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ThreadGroupTest.class);
    }
}
