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
 * JDWP Unit test for VirtualMachine.AllThreads command.
 */
public class AllThreadsTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.VirtualMachine.AllThreadsDebuggee";
    }

    /**
     * This testcase exercises VirtualMachine.AllThreads command.
     * <BR>At first the test starts AllThreadsDebuggee which runs the TESTED_THREAD thread
     * which starts and finishes.
     * <BR> Then the test performs VirtualMachine.AllThreads command and checks that
     * the tested thread is not returned by command;
     */
    public void testAllThreads003() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("waiting for finishing thread");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("send AllThreads cmd");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.AllThreadsCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::AllThreads command");

        long threadID;
        int threadStatus, suspendStatus;
        String threadName;
        ReplyPacket replyName;

        int threads = reply.getNextValueAsInt();
        logWriter.println("Number of threads = " + threads);
        assertTrue("Number of threads must be > 0", threads > 0);

        for (int i = 0; i < threads; i++) {

            threadID = reply.getNextValueAsThreadID();
            threadName = debuggeeWrapper.vmMirror.getThreadName(threadID);

            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(threadID);
            
            replyName = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(replyName, "ThreadReference::Status command"); 

            threadStatus = replyName.getNextValueAsInt();
            suspendStatus = replyName.getNextValueAsInt();

            logWriter.println("\t" + threadID + " "
                    + "\"" + threadName + "\" "
                    + JDWPConstants.ThreadStatus.getName(threadStatus) + " "
                    + JDWPConstants.SuspendStatus.getName(suspendStatus));

            if (threadName.equals(AllThreadsDebuggee.TESTED_THREAD)) {
                printErrorAndFail(AllThreadsDebuggee.TESTED_THREAD
                        + " must not be in all_thread list");
            }
        }
        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises VirtualMachine.AllThreads command.
     * <BR>At first the test starts AllThreadsDebuggee which runs the TESTED_THREAD thread
     * which starts but does not finish.
     * <BR> Then the test performs VirtualMachine.AllThreads command and checks that
     * all threads returned by command have only valid thread status:
     * <BR>'RUNNING' or 'MONITOR' or 'SLEEPING' or 'ZOMBIE' or 'WAIT' status;
     */
    public void testAllThreads002() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("send AllThreads cmd");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.AllThreadsCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::AllThreads command");

        long threadID;
        int threadStatus, suspendStatus;
        String threadName;
        int count = 0;
        ReplyPacket replyName;

        int threads = reply.getNextValueAsInt();
        logWriter.println("Number of threads = " + threads);
        assertTrue("Number of threads must be > 0", threads > 0);

        boolean threadStatusFailed = false;
        for (int i = 0; i < threads; i++) {

            threadID = reply.getNextValueAsThreadID();
            threadName = debuggeeWrapper.vmMirror.getThreadName(threadID);

            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(threadID);
                        
            replyName = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(replyName, "ThreadReference::Status command");

            threadStatus = replyName.getNextValueAsInt();
            suspendStatus = replyName.getNextValueAsInt();

            logWriter.println("\t" + threadID + " "
                    + "\"" + threadName + "\" "
                    + JDWPConstants.ThreadStatus.getName(threadStatus) + " "
                    + JDWPConstants.SuspendStatus.getName(suspendStatus));
            if (threadStatus == JDWPConstants.ThreadStatus.RUNNING
                    || threadStatus == JDWPConstants.ThreadStatus.MONITOR
                    || threadStatus == JDWPConstants.ThreadStatus.SLEEPING
                    || threadStatus == JDWPConstants.ThreadStatus.ZOMBIE
                    || threadStatus == JDWPConstants.ThreadStatus.WAIT) {
            } else {
                logWriter.println
                ("## FAILURE: Unknown thread status is found out!");
                threadStatusFailed = true;
                count++;
            }
        }
        if (threadStatusFailed) {
            printErrorAndFail("\nThreads with unknown thread status found out!\n" +
                    "Number of such threads = " + count +"\n");
        }
        assertAllDataRead(reply);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("waiting for finishing thread");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises VirtualMachine.AllThreads command.
     * <BR>At first the test starts AllThreadsDebuggee which runs the TESTED_THREAD thread
     * which starts but does not finish.
     * <BR> Then the test performs VirtualMachine.AllThreads command and checks that
     * the tested thread is returned by command in the list of threads;
     */
    public void testAllThreads001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("send AllThreads cmd");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.AllThreadsCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::AllThreads command");

        long threadID;
        String threadName;
        int count = 0;

        int threads = reply.getNextValueAsInt();
        logWriter.println("Number of threads = " + threads);
        assertTrue("Number of threads must be > 0", threads > 0);

        for (int i = 0; i < threads; i++) {
            threadID = reply.getNextValueAsThreadID();
            threadName = debuggeeWrapper.vmMirror.getThreadName(threadID);

            if (threadName.equals(AllThreadsDebuggee.TESTED_THREAD)) {
                count++;
                logWriter.println("\t" + threadID + " "
                        + "\"" + threadName + "\" found");
            }
        }
        if (count != 1) {
            if (count == 0) {
                printErrorAndFail(AllThreadsDebuggee.TESTED_THREAD + " not found");
            }
            if (count > 1 || count < 0) {
                printErrorAndFail(AllThreadsDebuggee.TESTED_THREAD
                        + " unexpected amount: " + count);
            }
        }
        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("waiting for finishing thread");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllThreadsTest.class);
    }
}
