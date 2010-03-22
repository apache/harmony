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

import org.apache.harmony.jpda.tests.framework.jdwp.exceptions.*;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.Suspend command.
 */
public class SuspendTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.SuspendDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.Suspend command.
     * <BR>At first the test starts SuspendDebuggee which starts and runs some tested threads.
     * <BR>After the tested threads starts, the test for every tested thread does:
     * <BR>&nbsp;&nbsp; - suspends thread by ThreadReference.Suspend command;
     * <BR>&nbsp;&nbsp; - performs the ThreadReference.Status command for tested thread;
     * <BR>It is expected that for every tested thread returned suspend 
     * status is SUSPEND_STATUS_SUSPENDED status;
     */
    public void testSuspend001() {
        logWriter.println("==> testSuspend001: START...");
        String debuggeeMessage = synchronizer.receiveMessage();
        int testedThreadsNumber = 0;
        try {
            testedThreadsNumber = Integer.valueOf(debuggeeMessage).intValue();
        } catch (NumberFormatException exception) {
            logWriter.println
                ("## FAILURE: Exception while getting number of started threads from debuggee = " + exception);
            printErrorAndFail("Can NOT get number of started threads from debuggee! ");
        }
        if ( testedThreadsNumber == 0 ) {
            logWriter.println("==>  There are no started threads in debuggee to test!");
            return;
        }
        logWriter.println("==>  Number of started threads in debuggee to test = " + testedThreadsNumber);
        String[] testedThreadsNames = new String[testedThreadsNumber];
        long[] testedThreadsIDs = new long[testedThreadsNumber];
        for (int i = 0; i < testedThreadsNumber; i++) {
            testedThreadsNames[i] = SuspendDebuggee.THREAD_NAME_PATTERN + i;
            testedThreadsIDs[i] = 0;
        }

        // getting ID of the tested thread
        ReplyPacket allThreadIDReply = null;
        try {
            allThreadIDReply = debuggeeWrapper.vmMirror.getAllThreadID(); 
        } catch (ReplyErrorCodeException exception) {
            logWriter.println
                ("## FAILURE: Exception in vmMirror.getAllThreadID() = " + exception);
            printErrorAndFail("Can NOT get all ThreadID in debuggee! ");
        }
        int threads = allThreadIDReply.getNextValueAsInt();
        logWriter.println("==>  Number of all threads in debuggee = " + threads);
        boolean suspendCommandFailed = false;
        boolean statusCommandFailed = false;
        boolean suspendStatusFailed = false;
        boolean resumeThreadFailed = false;
        
        for (int i = 0; i < threads; i++) {
            long threadID = allThreadIDReply.getNextValueAsThreadID();
            String threadName = null;
            try {
                threadName = debuggeeWrapper.vmMirror.getThreadName(threadID); 
            } catch (ReplyErrorCodeException exception) {
                logWriter.println
                    ("==> WARNING: Can NOT get thread name for threadID = " + threadID);
                continue;
            }
            int k = 0;
            for (; k < testedThreadsNumber; k++) {
                if ( threadName.equals(testedThreadsNames[k]) ) {
                    testedThreadsIDs[k] = threadID;
                    break;
                }
            }
            if ( k == testedThreadsNumber ) {
                // it is not thread to test
                continue;
            }
            
            logWriter.println("\n==> Check for Thread: threadID = " + threadID 
                    + "; threadName = " + threadName);

            // suspending the thread
            logWriter.println("==> Send ThreadReference.Suspend command..."); 
            CommandPacket packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.SuspendCommand);
            packet.setNextValueAsThreadID(threadID);
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.Suspend command") ) {
                suspendCommandFailed = true;
                continue;
            }

            logWriter.println("==> Send ThreadReference.Status command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(threadID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.Status command") ) {
                statusCommandFailed = true;
                continue;
            }

            int threadStatus = reply.getNextValueAsInt();
            int suspendStatus = reply.getNextValueAsInt();

            logWriter.println("==> threadStatus = " + threadStatus + "("
                    + JDWPConstants.ThreadStatus.getName(threadStatus) + ")");
            logWriter.println("==> suspendStatus = " + suspendStatus + "("
                    + JDWPConstants.SuspendStatus.getName(suspendStatus) + ")");
            if (suspendStatus
                    != JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) {
                logWriter.println("## FAILURE: Unexpected suspendStatus for thread = " + threadName);
                logWriter.println("##          Expected suspendStatus  = "  
                    + JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED
                    + "(" + JDWPConstants.SuspendStatus.getName
                    (JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) +")");
                suspendStatusFailed = true;
                continue;
            }

            // resuming the thread
            int resumeErr = debuggeeWrapper.vmMirror.resumeThread(threadID).getErrorCode();
            if (resumeErr != JDWPConstants.Error.NONE) {
                logWriter.println("## FAILURE: Can NOT resume thread = " + threadName);
                logWriter.println("##          Received ERROR while resume thread = " + resumeErr
                    + "(" + JDWPConstants.Error.getName(resumeErr) +")");
                resumeThreadFailed = true;
            }
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        String errorMessage = "";
        if ( suspendCommandFailed ) {
            errorMessage = "## Error found out while ThreadReference.Suspend command performing!\n";
        }
        if ( statusCommandFailed ) {
            errorMessage = errorMessage + "## Error found out while ThreadReference.Status command performing!\n";
        }
        if ( suspendStatusFailed ) {
            errorMessage = errorMessage + "## Unexpected suspendStatus found out!\n";
        }
        if ( resumeThreadFailed ) {
            errorMessage = errorMessage + "## Error found out while resuming thread!\n";
        }

        boolean testedThreadNotFound = false;
        for (int i = 0; i < testedThreadsNumber; i++) {
            if ( testedThreadsIDs[i] == 0 ) {
                logWriter.println("## FAILURE: Tested thread is not found out among debuggee threads!");
                logWriter.println("##          Thread name = " + testedThreadsNames[i]);
                testedThreadNotFound = true;
            }
        }

        if ( testedThreadNotFound ) {
            errorMessage = errorMessage + "## Some of tested threads are not found!\n";
        }

        if ( ! errorMessage.equals("") ) {
            printErrorAndFail("\ntestSuspend001 FAILED:\n" + errorMessage);
        }
        logWriter.println("\n==> testSuspend001 - OK!");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SuspendTest.class);
    }
}
