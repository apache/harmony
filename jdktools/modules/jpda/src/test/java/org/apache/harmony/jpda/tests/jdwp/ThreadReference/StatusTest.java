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
 * Created on 22.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.Status command.
 */
public class StatusTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.StatusDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.Status command for suspended Thread.
     * <BR>At first the test starts StatusDebuggee which runs
     * the tested thread.
     * <BR> At first the test suspends tested thread by ThreadReference.Suspend command. 
     * <BR> Then the tests performs the ThreadReference.Status command 
     * for tested thread. 
     * <BR>It is expected that:
     * <BR>&nbsp;&nbsp; - returned thread status is RUNNING status;
     * <BR>&nbsp;&nbsp; - returned suspend status is SUSPEND_STATUS_SUSPENDED status;
     */
    public void testStatus002() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        logWriter.println("get thread ID");
        long threadID = 
            debuggeeWrapper.vmMirror.getThreadID(StatusDebuggee.TESTED_THREAD);
        logWriter.println("suspend thread");
        debuggeeWrapper.vmMirror.suspendThread(threadID);

        // getting the thread group ID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
        packet.setNextValueAsThreadID(threadID);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::Status command");

        int threadStatus = reply.getNextValueAsInt();
        int suspendStatus = reply.getNextValueAsInt();

        logWriter.println("\t" + threadID + " "
                + "\"" + StatusDebuggee.TESTED_THREAD + "\" "
                + JDWPConstants.ThreadStatus.getName(threadStatus) + " "
                + JDWPConstants.SuspendStatus.getName(suspendStatus));

        if (threadStatus != JDWPConstants.ThreadStatus.RUNNING) {
            printErrorAndFail("Unexpected thread status: "
                    + JDWPConstants.ThreadStatus.getName(threadStatus));
        } else {
            logWriter.println("Expected thread status "
                    + JDWPConstants.ThreadStatus.getName(threadStatus));
        }

        if (suspendStatus != JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) {
            printErrorAndFail("Unexpected suspend status: "
                    + JDWPConstants.ThreadStatus.getName(suspendStatus));
        } else {
            logWriter.println("Expected suspend status "
                    + JDWPConstants.SuspendStatus.getName(suspendStatus));
        }

        logWriter.println("resume thread");
        debuggeeWrapper.vmMirror.resumeThread(threadID);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ThreadReference.Status command.
     * <BR>At first the test starts StatusDebuggee which runs
     * the tested thread.
     * <BR> Then the tests performs the ThreadReference.Status command 
     * for tested thread. 
     * <BR>It is expected that:
     * <BR>&nbsp;&nbsp; - returned thread status is RUNNING status;
     * <BR>&nbsp;&nbsp; - returned suspend status is not SUSPEND_STATUS_SUSPENDED status;
     */
    public void testStatus001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        logWriter.println("get thread ID");
        long threadID = 
            debuggeeWrapper.vmMirror.getThreadID(StatusDebuggee.TESTED_THREAD);

        // getting the thread group ID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
        packet.setNextValueAsThreadID(threadID);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::Status command");

        int threadStatus = reply.getNextValueAsInt();
        int suspendStatus = reply.getNextValueAsInt();

        logWriter.println("\t" + threadID + " "
                + "\"" + StatusDebuggee.TESTED_THREAD + "\" "
                + JDWPConstants.ThreadStatus.getName(threadStatus) + " "
                + JDWPConstants.SuspendStatus.getName(suspendStatus));

        if (threadStatus != JDWPConstants.ThreadStatus.RUNNING) {
            printErrorAndFail("Unexpected thread status: "
                    + JDWPConstants.ThreadStatus.getName(threadStatus));
        } else {
            logWriter.println("Expected thread status "
                    + JDWPConstants.ThreadStatus.getName(threadStatus));
        }

        if (suspendStatus == JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) {
            printErrorAndFail("Unexpected suspend status: "
                    + JDWPConstants.ThreadStatus.getName(suspendStatus));
        } else {
            logWriter.println("Expected suspend status "
                    + JDWPConstants.SuspendStatus.getName(suspendStatus));
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StatusTest.class);
    }
}
