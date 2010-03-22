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
 * JDWP Unit test for ThreadReference.SuspendCount command.
 */
public class SuspendCountTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String debuggeeSignature = 
            "Lorg/apache/harmony/jpda/tests/jdwp/ThreadReference/SuspendCountDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.SuspendCountDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.SuspendCount command.
     * <BR>At first the test starts SuspendCountDebuggee which starts and runs some tested threads.
     * <BR>After the tested threads starts, the test suspends every 
     * tested thread in debuggee some times and 
     * check that ThreadReference.SuspendCount command returns 
     * expected number of times thread was suspended.
     * <BR>Then the test suspends all debuggee by VirtualMachine.Suspend command
     * and check that ThreadReference.SuspendCount command returns 1 value
     * for all tested threads and main thread.
     * <BR>Then the test resumes all debuggee by VirtualMachine.Resume command
     * and check that ThreadReference.SuspendCount command returns 0 value
     * for all tested threads and main thread. 
     */
    public void testSuspendCount001() {
        logWriter.println("==> testSuspendCount001: START...");
        String debuggeeMessage = synchronizer.receiveMessage();
        int testedThreadsNumber = 0;
        try {
            testedThreadsNumber = Integer.valueOf(debuggeeMessage).intValue();
        } catch (NumberFormatException exception) {
            logWriter.println
                ("## FAILURE: Exception while getting number of started threads from debuggee = " + exception);
            synchronizer.sendMessage("FINISH");
            printErrorAndFail("Can NOT get number of started threads from debuggee! ");
        }
        if ( testedThreadsNumber == 0 ) {
            logWriter.println("==>  There are no started threads in debuggee to test" 
                    + " - testSuspendCount001 finishes!");
            synchronizer.sendMessage("FINISH");
            return;
        }
        logWriter.println("==>  Number of started threads in debuggee to test = " + testedThreadsNumber);
        String[] testedThreadsNames = new String[testedThreadsNumber+1]; // +1 is for main debuggee thread
        long[] testedThreadsIDs = new long[testedThreadsNumber+1];
        for (int i = 0; i < testedThreadsNumber; i++) {
            testedThreadsNames[i] = SuspendCountDebuggee.THREAD_NAME_PATTERN + i;
            testedThreadsIDs[i] = 0;
        }

        // getting ID of the tested thread
        ReplyPacket allThreadIDReply = null;
        try {
            allThreadIDReply = debuggeeWrapper.vmMirror.getAllThreadID();
        } catch (ReplyErrorCodeException exception) {
            logWriter.println
                ("## FAILURE: Exception in vmMirror.getAllThreadID() = " + exception);
            synchronizer.sendMessage("FINISH");
            printErrorAndFail("Can NOT get all ThreadID in debuggee! ");
        }
        int threads = allThreadIDReply.getNextValueAsInt();
        logWriter.println("==>  Number of all threads in debuggee = " + threads);
        String[] allThreadsNames = new String[threads];
        long[] allThreadsIDs = new long[threads];
        boolean suspendCommandFailed = false;
        boolean suspendCountCommandFailed = false;
        boolean resumeCommandFailed = false;
        
        int suspendNumber = 0;
        for (int i = 0; i < threads; i++) {
            long threadID = allThreadIDReply.getNextValueAsThreadID();
            allThreadsIDs[i] = threadID;
            String threadName = null;
            try {
                threadName = debuggeeWrapper.vmMirror.getThreadName(threadID); 
            } catch (ReplyErrorCodeException exception) {
                logWriter.println
                    ("==> WARNING: Can NOT get thread name for threadID = " + threadID);
                continue;
            }
            allThreadsNames[i] = threadName;
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

            logWriter.println("==> Send ThreadReference.SuspendCount command..."); 
            CommandPacket packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.SuspendCountCommand);
            packet.setNextValueAsThreadID(threadID);
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.SuspendCount command") ) {
                suspendCountCommandFailed = true;
            } else {
                int suspendCount = reply.getNextValueAsInt();
                logWriter.println("==> ThreadReference.SuspendCount command returns suspendCount = " + suspendCount); 
                if ( suspendCount != 0 ) {
                    logWriter.println("## FAILURE: Unexpected suspendCount for thread = " + threadName);
                    logWriter.println("##          Expected suspendCount  = 0");  
                    suspendCountCommandFailed = true;
                }
            }

            suspendNumber++;
            logWriter.println("==> Send ThreadReference.Suspend command number of times = " 
                    + suspendNumber + "..."); 
            int j = 0;
            for (; j < suspendNumber; j++) {
                packet = new CommandPacket(
                        JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                        JDWPCommands.ThreadReferenceCommandSet.SuspendCommand);
                packet.setNextValueAsThreadID(threadID);
                reply = debuggeeWrapper.vmMirror.performCommand(packet);
                if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.Suspend command") ) {
                    break;
                }
            }
            if ( j < suspendNumber ) {
                suspendCommandFailed = true;
                continue;
            }

            logWriter.println("==> Send ThreadReference.SuspendCount command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.SuspendCountCommand);
            packet.setNextValueAsThreadID(threadID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.SuspendCount command") ) {
                suspendCountCommandFailed = true;
            } else {
                int suspendCount = reply.getNextValueAsInt();
                logWriter.println("==> ThreadReference.SuspendCount command returns suspendCount = " + suspendCount); 
                if ( suspendCount != suspendNumber ) {
                    logWriter.println("## FAILURE: Unexpected suspendCount for thread = " + threadName);
                    logWriter.println("##          Expected suspendCount  = " + suspendNumber);  
                    suspendCountCommandFailed = true;
                }
            }

            logWriter.println("==> Send ThreadReference.Resume command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.ResumeCommand);
            packet.setNextValueAsThreadID(threadID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.Resume command") ) {
                resumeCommandFailed = true;
            }

            logWriter.println("==> Send ThreadReference.SuspendCount command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.SuspendCountCommand);
            packet.setNextValueAsThreadID(threadID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.SuspendCount command") ) {
                suspendCountCommandFailed = true;
            } else {
                int suspendCount = reply.getNextValueAsInt();
                logWriter.println("==> ThreadReference.SuspendCount command returns suspendCount = " + suspendCount); 
                if ( suspendCount != (suspendNumber -1) ) {
                    logWriter.println("## FAILURE: Unexpected suspendCount for thread = " + threadName);
                    logWriter.println("##          Expected suspendCount  = " + (suspendNumber-1));  
                    suspendCountCommandFailed = true;
                }
            }
            
            if ( suspendNumber == 1 ) {
                continue;
            }

            logWriter.println("==> Send ThreadReference.Resume command number of times = " 
                    + (suspendNumber - 1) + "..."); 
            j = 0;
            for (; j < (suspendNumber-1); j++) {
                packet = new CommandPacket(
                        JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                        JDWPCommands.ThreadReferenceCommandSet.ResumeCommand);
                packet.setNextValueAsThreadID(threadID);
                reply = debuggeeWrapper.vmMirror.performCommand(packet);
                if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.Resume command") ) {
                    break;
                }
            }
            if ( j < (suspendNumber-1) ) {
                resumeCommandFailed = true;
                continue;
            }

            logWriter.println("==> Send ThreadReference.SuspendCount command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.SuspendCountCommand);
            packet.setNextValueAsThreadID(threadID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.SuspendCount command") ) {
                suspendCountCommandFailed = true;
                continue;
            }
            int suspendCount = reply.getNextValueAsInt();
            logWriter.println("==> ThreadReference.SuspendCount command returns suspendCount = " + suspendCount); 
            if ( suspendCount != 0 ) {
                logWriter.println("## FAILURE: Unexpected suspendCount for thread = " + threadName);
                logWriter.println("##          Expected suspendCount  = 0");  
                suspendCountCommandFailed = true;
            }
        }

        String errorMessage = "";
        if ( suspendCountCommandFailed ) {
            errorMessage = errorMessage + "## Error found out while ThreadReference.SuspendCount command performing!\n";
        }
        if ( suspendCommandFailed ) {
            errorMessage = errorMessage + "## Error found out while ThreadReference.Suspend command performing!\n";
        }
        if ( resumeCommandFailed ) {
            errorMessage = errorMessage + "## Error found out while ThreadReference.Resume command performing!\n";
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
            synchronizer.sendMessage("FINISH");
            printErrorAndFail("\ntestSuspendCount001 FAILED:\n" + errorMessage);
        }
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("\n==> Check ThreadReference.SuspendCount command when all debuggee is suspended...");
        testedThreadsNames[testedThreadsNumber] = synchronizer.receiveMessage(); // debuggee main thread name
        
        testedThreadsIDs[testedThreadsNumber] = 0; // debuggee main thread ID
        for (int i = 0; i < threads; i++) {
            if ( testedThreadsNames[testedThreadsNumber].equals(allThreadsNames[i]) ) {
                testedThreadsIDs[testedThreadsNumber] = allThreadsIDs[i];
                break;
            }
        }
        if ( testedThreadsIDs[testedThreadsNumber] == 0 ) {
            setStaticIntField(debuggeeSignature, SuspendCountDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            logWriter.println("## FAILURE: Debuggee main thread is not found out among debuggee threads!");
            logWriter.println("##          Thread name = " + testedThreadsNames[testedThreadsNumber]);
            printErrorAndFail("\nCan NOT found out debuggee main thread!");
        }
        
        logWriter.println("\n==> Send VirtualMachine.Suspend command..."); 
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SuspendCommand);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        int errorCode = reply.getErrorCode();
        if ( errorCode !=  JDWPConstants.Error.NONE ) {
            setStaticIntField(debuggeeSignature, SuspendCountDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);
            logWriter.println("## FAILURE: VirtualMachine.Suspend command returns error = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            printErrorAndFail("\nVirtualMachine.Suspend command FAILED!");
        }
        
        for (int i = 0; i < (testedThreadsNumber+1); i++) {
            logWriter.println("==> Send ThreadReference.SuspendCount command for thread = " 
                    + testedThreadsNames[i] + " ..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.SuspendCountCommand);
            packet.setNextValueAsThreadID(testedThreadsIDs[i]);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.SuspendCount command") ) {
                suspendCountCommandFailed = true;
                continue;
            }
            int suspendCount = reply.getNextValueAsInt();
            logWriter.println("==> ThreadReference.SuspendCount command returns suspendCount = " + suspendCount); 
            if ( suspendCount != 1 ) {
                logWriter.println("## FAILURE: Unexpected suspendCount for thread = " + testedThreadsNames[i]);
                logWriter.println("##          Expected suspendCount  = 1");  
                suspendCountCommandFailed = true;
            }
        }

        logWriter.println("\n==> Send VirtualMachine.Resume command ..."); 
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ResumeCommand);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        if ( ! checkReplyPacketWithoutFail(reply, "VirtualMachine.Resume command") ) {
            resumeCommandFailed = true;
        } else {
            logWriter.println("\n==> Check ThreadReference.SuspendCount command after debuggee is resumed..."); 
            for (int i = 0; i < (testedThreadsNumber+1); i++) {
                logWriter.println("==> Send ThreadReference.SuspendCount command for thread = " 
                        + testedThreadsNames[i] + " ..."); 
                packet = new CommandPacket(
                        JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                        JDWPCommands.ThreadReferenceCommandSet.SuspendCountCommand);
                packet.setNextValueAsThreadID(testedThreadsIDs[i]);
                reply = debuggeeWrapper.vmMirror.performCommand(packet);
                if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.SuspendCount command") ) {
                    suspendCountCommandFailed = true;
                    continue;
                }
                int suspendCount = reply.getNextValueAsInt();
                logWriter.println("==> ThreadReference.SuspendCount command returns suspendCount = " + suspendCount); 
                if ( suspendCount != 0 ) {
                    logWriter.println("## FAILURE: Unexpected suspendCount for thread = " + testedThreadsNames[i]);
                    logWriter.println("##          Expected suspendCount  = 0");  
                    suspendCountCommandFailed = true;
                }
            }
        }

        setStaticIntField(debuggeeSignature, SuspendCountDebuggee.TO_FINISH_DEBUGGEE_FIELD_NAME, 99);

        if ( suspendCountCommandFailed ) {
            errorMessage = "## Error found out while ThreadReference.SuspendCount command performing!\n";
        }

        if ( resumeCommandFailed ) {
            errorMessage = "## Error found out while VirtualMachine.Resume command performing!\n";
        }

        if ( ! errorMessage.equals("") ) {
            printErrorAndFail("\ntestSuspendCount001 FAILED:\n" + errorMessage);
        }
        logWriter.println("\n==> testSuspendCount001 - OK!");
    }
/*
    protected int setStaticIntField (String classSignature, String fieldName, int newValue) {
        
        long classID = debuggeeWrapper.vmMirror.getClassID(classSignature);
        if ( classID == -1 ) {
            logWriter.println
            ("## setStaticIntField(): Can NOT get classID for class signature = '" 
                    + classSignature + "'");
            return -1;
        }

        long fieldID = 
            debuggeeWrapper.vmMirror.getFieldID(classID, fieldName);
        if ( fieldID == -1 ) {
            logWriter.println
            ("## setStaticIntField(): Can NOT get fieldID for field = '" 
                    + fieldName + "'");
            return -1;
        }
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.SetValuesCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsFieldID(fieldID);
        packet.setNextValueAsInt(newValue);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        int errorCode = reply.getErrorCode();
        if ( errorCode !=  JDWPConstants.Error.NONE ) {
            logWriter.println
            ("## setStaticIntField(): Can NOT set value for field = '" 
                    + fieldName + "' in class = '" + classSignature 
                    + "'; ClassType.SetValues command reurns error = " + errorCode);
            return -1;
        }
        return 0;
    }
*/
    public static void main(String[] args) {
        junit.textui.TestRunner.run(SuspendCountTest.class);
    }
}
