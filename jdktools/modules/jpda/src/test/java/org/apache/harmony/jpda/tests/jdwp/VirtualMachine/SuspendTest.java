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
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.Suspend command.
 */
public class SuspendTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.Suspend command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.Suspend commands 
     * and checks with help of ThreadReference.Status command that all threads in debuggee 
     * have suspend status = SUSPEND_STATUS_SUSPENDED, i.e. all
     * debuggee threads are suspended.
     */
    public void testSuspend001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SuspendCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::Suspend command");

        reply = debuggeeWrapper.vmMirror.getAllThreadID(); 
        
        long threadID;
        int threadStatus, suspendStatus;
        String threadName;
        ReplyPacket replyName;

        int threads = reply.getNextValueAsInt();
        logWriter.println("Number of threads = " + threads);
        assertTrue("Invalid number of threads: " + threads, threads > 0);

        for (int i = 0; i < threads; i++) {

            threadID = reply.getNextValueAsThreadID() ;
            threadName = debuggeeWrapper.vmMirror.getThreadName(threadID); 

            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(threadID);
            
            replyName = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "ThreadReference::Status command");

            threadStatus = replyName.getNextValueAsInt();
            suspendStatus = replyName.getNextValueAsInt();

            logWriter.println("\t" + threadID + " "
                    + "\"" + threadName + "\" "
                    + JDWPConstants.ThreadStatus.getName(threadStatus) + " "
                    + JDWPConstants.SuspendStatus.getName(suspendStatus));
            if (suspendStatus
                    != JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) {
                printErrorAndFail("thread ID=" + threadID + "; name=\""
                        + threadName + "\"" + " is not in suspended state");
            }
        }

        resumeDebuggee();
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SuspendTest.class);
    }
}
