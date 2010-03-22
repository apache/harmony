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
 * @author Anatoly F. Bondarenko
 */

/**
 * Created on 19.06.2006
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
 * JDWP Unit test for ThreadReference.ThreadGroup command.
 */
public class ThreadGroup002Test extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String debuggeeSignature = 
            "Lorg/apache/harmony/jpda/tests/jdwp/ThreadReference/ThreadGroup002Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.ThreadGroup002Debuggee";
    }

    /**
     * This testcase exercises ThreadReference.ThreadGroup command.
     * <BR>At first the test starts ThreadGroup002Debuggee which creates some thread  
     * groups and starts some tested threads which belong to different created thread groups.
     * <BR>After the tested threads start, at first the test wait for the some first
     * tested threads to finish. 
     * <BR> Then the test for every tested thread does:
     * <BR>&nbsp;&nbsp; - performs ThreadReference.Status command;
     * <BR>&nbsp;&nbsp; - performs the ThreadReference.ThreadGroup command;
     * <BR>&nbsp;&nbsp; - performs the ThreadGroupReference.name command;
     * <BR>It is expected that 
     * <BR>&nbsp;&nbsp; - all threads with status ZOMBIE are only finished tested threads;
     * <BR>&nbsp;&nbsp; - all threads without status ZOMBIE are only NOT finished tested threads;
     * <BR>&nbsp;&nbsp; - if status of thread is ZOMBIE then returned groupID must be null;
     * <BR>&nbsp;&nbsp; - if status of thread is not ZOMBIE then returned groupID must not be null;
     * <BR>&nbsp;&nbsp; - thread group name should be expected name for thread which is not ZOMBIE;
     */
    public void testThreadGroup002() {
        logWriter.println("==> testThreadGroup002: START...");
        String debuggeeMessage = synchronizer.receiveMessage();
        int testedThreadsNumber = 0;
        try {
            testedThreadsNumber = Integer.valueOf(debuggeeMessage).intValue();
        } catch (NumberFormatException exception) {
            logWriter.println
                ("## FAILURE: Exception while getting number of started threads from debuggee = " + exception);
            synchronizer.sendMessage("FINISH");
            printErrorAndFail("\n## Can NOT get number of started threads from debuggee! ");
        }
        testedThreadsNumber++; // to add debuggee main thread
        logWriter.println("==>  Number of threads in debuggee to test = " + testedThreadsNumber);
        String[] testedThreadsNames = new String[testedThreadsNumber];
        String[] testedThreadGroupsNames = new String[testedThreadsNumber];
        long[] testedThreadsIDs = new long[testedThreadsNumber];
        String debuggeeMainThreadName = synchronizer.receiveMessage();
        String debuggeeMainThreadGroupName = synchronizer.receiveMessage();
        for (int i = 0; i < testedThreadsNumber; i++) {
            if ( i < (testedThreadsNumber-1) ) {
                testedThreadsNames[i] = ThreadGroup002Debuggee.THREAD_NAME_PATTERN + i;
                testedThreadGroupsNames[i] = ThreadGroup002Debuggee.THREAD_GROUP_NAME_PATTERN + (i%2);
            } else {
                testedThreadsNames[i] = debuggeeMainThreadName;
                testedThreadGroupsNames[i] = debuggeeMainThreadGroupName;
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
            synchronizer.sendMessage("FINISH");
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
            synchronizer.sendMessage("FINISH");
            printErrorAndFail("\n## Some of tested threads are not found!");
        }

        logWriter.println("==> Send signal to debuggee to continue and to finish some first threads...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("==> Wait signal from the debuggee that some first threads finished...");
        String messageFromDebuggee = synchronizer.receiveMessageWithoutException("testThreadGroup002");
        if ( ! JPDADebuggeeSynchronizer.SGNL_READY.equals(messageFromDebuggee) ) {
            logWriter.println("## FAILURE: Could NOT receive expected signal from debuggee!");
            printErrorAndFail("\n## Could NOT receive expected signal from debuggee! ");
        }
        int finishedTestedThreadsNumber = testedThreadsNumber/2;
        logWriter.println
        ("==> Number of debuggee's finished threads = " +  finishedTestedThreadsNumber);
        
        CommandPacket packet = null;
        ReplyPacket reply = null;
        String errorMessage = "";

        boolean statusCommandFailed = false;
        boolean threadStatusFailed = false;
        boolean groupIDFailed = false;
        boolean threadGroupCommandFailed = false;
        boolean groupNameFailed = false;

        logWriter.println
        ("\n==> Check that ThreadReference.ThreadGroup command returns expected thread group for each tsted thread...");
        
        for (int threadCount = 0; threadCount < testedThreadsNumber; threadCount++) {
            logWriter.println("\n==> Check for Thread: threadID = " + testedThreadsIDs[threadCount] 
                    + "; threadName = " + testedThreadsNames[threadCount]);
            logWriter.println("==> Send ThreadReference.Status command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
            packet.setNextValueAsReferenceTypeID(testedThreadsIDs[threadCount]);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.Status command") ) {
                statusCommandFailed = true;
                continue;
            }

            int threadStatus = reply.getNextValueAsInt();
            //int suspendStatus =
            reply.getNextValueAsInt();

            logWriter.println("==> thread status of checked thread = " + threadStatus + "("
                    + JDWPConstants.ThreadStatus.getName(threadStatus) + ")");
            
            logWriter.println("==> Send ThreadReference.ThreadGroup command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.ThreadGroupCommand);
            packet.setNextValueAsThreadID(testedThreadsIDs[threadCount]);
            
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.ThreadGroup command") ) {
                threadGroupCommandFailed = true;
                continue;
            }

            long threadGroupID = reply.getNextValueAsThreadGroupID();
            logWriter.println("==> thread groupID for checked thread = " + threadGroupID);
            if (threadStatus == JDWPConstants.ThreadStatus.ZOMBIE) {
                if ( threadCount >= finishedTestedThreadsNumber ) {
                    logWriter.println("## FAILURE: Unexpected status for checked thread!");
                    logWriter.println("##          Thread witn number = " + threadCount +
                            " should NOT be ZOMBIE!");  
                    threadStatusFailed = true;
                    continue;
                }
                // according to JVMTI spec groupID is NULL if the thread has died
                if ( threadGroupID != 0 ) {
                    logWriter.println("## FAILURE: Unexpected thread groupID for checked thread with status = ZOMBIE!");
                    logWriter.println("##          Expected thread groupID = 0");  
                    groupIDFailed = true;
                }
                continue;
            } else {
                if ( threadCount < finishedTestedThreadsNumber ) {
                    logWriter.println("## FAILURE: Unexpected status for checked thread!");
                    logWriter.println("##          Thread witn number = " + threadCount +
                            " should be ZOMBIE!");  
                    threadStatusFailed = true;
                    continue;
                }
                if ( threadGroupID == 0 ) {
                    logWriter.println("## FAILURE: Unexpected thread groupID for checked thread with status != ZOMBIE!");
                    logWriter.println("##          Expected thread groupID != 0");  
                    groupIDFailed = true;
                    continue;
                }
            }
            
            logWriter.println("==> Getting thread group name by ThreadGroupReference.Name command..."); 
            packet = new CommandPacket(
                    JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadGroupReferenceCommandSet.NameCommand);
            packet.setNextValueAsThreadID(threadGroupID);
            
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            if ( ! checkReplyPacketWithoutFail(reply, "ThreadReference.ThreadGroup command") ) {
                threadGroupCommandFailed = true;
                continue;
            }

            String threadGroupName = reply.getNextValueAsString();
            logWriter.println("==> thread group name for checked thread = '" + threadGroupName + "'");

            if ( ! testedThreadGroupsNames[threadCount].equals(threadGroupName) ) {
                logWriter.println("## FAILURE: Unexpected thread group name for checked thread!");
                logWriter.println("##          Expected thread group name = '" + 
                        testedThreadGroupsNames[threadCount] + "'");  
                groupNameFailed = true;
            }
        }

        if ( statusCommandFailed ) {
            errorMessage = errorMessage + 
            "## Error found out while ThreadReference.Status command performing!\n";
        }

        if ( threadStatusFailed ) {
            errorMessage = errorMessage + 
            "## Unexpected thread status found out for some tested threads!\n";
        }

        if ( groupIDFailed ) {
            errorMessage = errorMessage + 
            "## Unexpected thread groupID found out for some tested threads!\n";
        }

        if ( threadGroupCommandFailed ) {
            errorMessage = errorMessage + 
            "## Error found out while ThreadReference.ThreadGroup command performing!\n";
        }

        if ( groupNameFailed ) {
            errorMessage = errorMessage + 
            "## Unexpected thread group name found out for some tested threads!\n";
        }

        logWriter.println("==> Send signal to debuggee to finish...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        if ( ! errorMessage.equals("") ) {
            printErrorAndFail("\ntestThreadGroup002 FAILED:\n" + errorMessage);
        }

        logWriter.println("\n==> testThreadGroup002 - OK!");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ThreadGroup002Test.class);
    }
}
