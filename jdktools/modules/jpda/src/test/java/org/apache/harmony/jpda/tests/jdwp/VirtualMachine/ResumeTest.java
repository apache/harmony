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
 * @author Vitaly A. Provodin, Anatoly F. Bondarenko
 */

/**
 * Created on 10.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.exceptions.ReplyErrorCodeException;
import org.apache.harmony.jpda.tests.jdwp.ThreadReference.ResumeDebuggee;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;


/**
 * JDWP Unit test for VirtualMachine.Resume command.
 */
public class ResumeTest extends JDWPSyncTestCase {

    static final String debuggeeSignature = 
        "Lorg/apache/harmony/jpda/tests/jdwp/VirtualMachine/ResumeDebuggee;";

   protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.VirtualMachine.ResumeDebuggee";
    }

    /**
     * This testcase exercises VirtualMachine.Resume command.
     * <BR>At first the test starts ResumeDebuggee which starts and runs some tested threads. 
     * <BR> Then the test performs VirtualMachine.Suspend command and checks with help of 
     * ThreadReference.Status command that all debuggee tested threads are suspended. 
     * <BR> Then the test performs VirtualMachine.Resume command and checks with help of 
     * ThreadReference.Status command that all debuggee tested threads are resumed. 
     */
    public void testResume001() {
        logWriter.println("==> testResume001: START...");
        String debuggeeMessage = synchronizer.receiveMessage();
        int testedThreadsNumber = 0;
        try {
            testedThreadsNumber = Integer.valueOf(debuggeeMessage).intValue();
        } catch (NumberFormatException exception) {
            logWriter.println
                ("## FAILURE: Exception while getting number of started threads from debuggee = " + exception);
            setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            printErrorAndFail("\n## Can NOT get number of started threads from debuggee! ");
        }
        testedThreadsNumber++; // to add debuggee main thread
        logWriter.println("==>  Number of threads in debuggee to test = " + testedThreadsNumber);
        String[] testedThreadsNames = new String[testedThreadsNumber];
        long[] testedThreadsIDs = new long[testedThreadsNumber];
        String debuggeeMainThreadName = synchronizer.receiveMessage();
        for (int i = 0; i < testedThreadsNumber; i++) {
            if ( i < (testedThreadsNumber-1) ) {
                testedThreadsNames[i] = ResumeDebuggee.THREAD_NAME_PATTERN + i;
            } else {
                testedThreadsNames[i] = debuggeeMainThreadName;
            }
            testedThreadsIDs[i] = 0;
        }

        // getting ID of the tested thread
        ReplyPacket allThreadIDReply = null;
        try {
            allThreadIDReply = debuggeeWrapper.vmMirror.getAllThreadID();
        } catch (ReplyErrorCodeException exception) {
            logWriter.println
                ("## FAILURE: Exception in vmMirror.getAllThreadID() = " + exception);
            setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            printErrorAndFail("\n## Can NOT get all ThreadID in debuggee! ");
        }
        int threads = allThreadIDReply.getNextValueAsInt();
        logWriter.println("==>  Number of all threads in debuggee = " + threads);
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
            setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            printErrorAndFail("\n## Some of tested threads are not found!");
        }

        logWriter.println("\n==> Send VirtualMachine.Suspend command..."); 
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SuspendCommand);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        int errorCode = reply.getErrorCode();
        if ( errorCode !=  JDWPConstants.Error.NONE ) {
            setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            logWriter.println("## FAILURE: VirtualMachine.Suspend command returns error = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            printErrorAndFail("\nVirtualMachine.Suspend command FAILED!");
        } else {
            logWriter.println("==> VirtualMachine.Suspend command - OK.");
        }
        
        logWriter.println
        ("\n==> Check that all tested threads are suspended after VirtualMachine.Suspend command...");

        boolean statusCommandFailed = false;
        boolean suspendStatusFailed = false;
        for (int i = 0; i < testedThreadsNumber; i++) {
            logWriter.println("\n==> Check for Thread: threadID = " + testedThreadsIDs[i] 
                + "; threadName = " + testedThreadsNames[i]);

            logWriter.println("==> Send ThreadReference.Status command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(testedThreadsIDs[i]);
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
                logWriter.println("## FAILURE: Unexpected suspendStatus for checked thread!");
                logWriter.println("##          Expected suspendStatus  = "  
                    + JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED
                    + "(" + JDWPConstants.SuspendStatus.getName
                    (JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) +")");
                suspendStatusFailed = true;
                continue;
            }
        }
            
        String errorMessage = "";
        if ( statusCommandFailed ) {
            errorMessage = errorMessage + "## Error found out while ThreadReference.Status command performing!\n";
        }
        if ( suspendStatusFailed ) {
            errorMessage = errorMessage + "## Unexpected suspendStatus found out!\n";
        }

        logWriter.println("\n==> Send VirtualMachine.Resume command..."); 
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ResumeCommand);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        errorCode = reply.getErrorCode();
        if ( errorCode !=  JDWPConstants.Error.NONE ) {
            setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            logWriter.println("## FAILURE: VirtualMachine.Resume command returns error = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            printErrorAndFail("\nVirtualMachine.Resume command FAILED!");
        } else {
            logWriter.println("==> VirtualMachine.Resume command - OK.");
        }
        
        if ( ! errorMessage.equals("") ) {
            setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            printErrorAndFail("\ntestResume001 FAILED:\n" + errorMessage);
        }

        logWriter.println
        ("\n==> Check that all tested threads are resumed after VirtualMachine.Resume command...");

        for (int i = 0; i < testedThreadsNumber; i++) {
            logWriter.println("\n==> Check for Thread: threadID = " + testedThreadsIDs[i] 
                + "; threadName = " + testedThreadsNames[i]);

            logWriter.println("==> Send ThreadReference.Status command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(testedThreadsIDs[i]);
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
                    == JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED) {
                logWriter.println
                    ("## FAILURE: Thread still is suspended after VirtualMachine.Resume commands!");
                suspendStatusFailed = true;
            }
        }
            
        if ( statusCommandFailed ) {
            errorMessage = errorMessage + "## Error found out while ThreadReference.Status command performing!\n";
        }
        if ( suspendStatusFailed ) {
            errorMessage = errorMessage + "## Unexpected suspendStatus found out!\n";
        }
        
        setStaticIntField(debuggeeSignature, ResumeDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
        if ( ! errorMessage.equals("") ) {
            printErrorAndFail("\ntestResume001 FAILED:\n" + errorMessage);
        }

        logWriter.println("\n==> testResume001 - OK!");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ResumeTest.class);
    }
}
