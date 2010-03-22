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
 * @author Aleksander V. Budniy
 */

/**
 * Created on 8.7.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.EventMod;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for verifying canceling of SINGLE_STEP event after re-connection.
 */
public class SingleStepTest extends JDWPSyncTestCase {

    private String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/MultiSession/SingleStepDebuggee;";

    private String DEBUGGEE_CLASS_NAME = "org.apache.harmony.jpda.tests.jdwp.MultiSession.SingleStepDebuggee";

    protected String getDebuggeeClassName() {
        return DEBUGGEE_CLASS_NAME;
    }

    /**
     * This testcase verifies canceling of SINGLE_STEP event after re-connection.
     * <BR>It runs SingleStepDebuggee and sets request for BREAKPOINT event.
     * After BREAKPOINT event occurs the testcase sets request for SINGLE_STEP event
     * and re-connects.
     * <BR>It is expected that only auto VM_DEATH event occurs after re-connection,
     * but no any other event, including SINGLE_STEP event.
     */
    public void testSingleStep001() {
        stepFunction(JDWPConstants.StepSize.LINE, JDWPConstants.StepDepth.OVER);
    }

    void stepFunction(byte StepSize, byte StepDepth) {
        logWriter.println("=> testSingleStep started");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        //find checked method
        long refTypeID = debuggeeWrapper.vmMirror.getClassID(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = "
                + refTypeID);
        logWriter
                .println("=> Send ReferenceType::Methods command and get methodIDs ");

        long requestID = debuggeeWrapper.vmMirror.setBreakpointAtMethodBegin(
                refTypeID, "breakpointTest");
        logWriter.println("=> breakpointID = " + requestID);
        logWriter.println("=> starting thread");

        //execute the breakpoint
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        long breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> breakpointThreadID = " + breakpointThreadID);

        // Sending a SINGLE_STEP request

        CommandPacket setRequestCommand = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);

        setRequestCommand
                .setNextValueAsByte(JDWPConstants.EventKind.SINGLE_STEP);
        setRequestCommand.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        setRequestCommand.setNextValueAsInt(1);
        setRequestCommand.setNextValueAsByte(EventMod.ModKind.Step);
        setRequestCommand.setNextValueAsThreadID(breakpointThreadID);
        setRequestCommand.setNextValueAsInt(StepSize);
        setRequestCommand.setNextValueAsInt(StepDepth);

        ReplyPacket setRequestReply = debuggeeWrapper.vmMirror
                .performCommand(setRequestCommand);
        
        checkReplyPacket(setRequestReply, "Set SINGLE_STEP event");

        requestID = setRequestReply.getNextValueAsInt();
        logWriter.println("=> RequestID = " + requestID);
        
        assertAllDataRead(setRequestReply);

        logWriter.println("");
        logWriter.println("=> CLOSE CONNECTION..");
        closeConnection();
        logWriter.println("=> CONNECTION IS CLOSED");

        logWriter.println("");
        logWriter.println("=> OPEN NEW CONNECTION..");
        openConnection();
        logWriter.println("=> CONNECTION IS OPENED");

        //resume debuggee
        //ReplyPacket reply = debuggeeWrapper.vmMirror.resume();
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        //      receive event
        logWriter.println("=> Wait for event..");
        CommandPacket eventPacket = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(eventPacket);
        int eventsCount = parsedEvents.length;
        logWriter.println("=> Received event set: count=" + eventsCount);

        //ckeck if received events are expected
        int result = 0;
        int autoEvents = 0;
        int wrongEvents = 0;
        for (int i = 0; i < eventsCount; i++) {
            ParsedEvent event = parsedEvents[i];
            logWriter.println("=> Event #" + i + ";");

            // print event info
            byte eventSuspendPolicy = event.getSuspendPolicy();
            logWriter.println("=> SuspendPolicy=" + eventSuspendPolicy + "/"
                    + JDWPConstants.SuspendPolicy.getName(eventSuspendPolicy));
            byte eventKind = event.getEventKind();
            logWriter.println("=> EventKind=" + eventKind + "/"
                    + JDWPConstants.EventKind.getName(eventKind));
            int eventRequestID = event.getRequestID();
            logWriter.println("=> RequestID=" + eventRequestID);

            // check if event is expected
            if (eventKind == JDWPConstants.EventKind.VM_DEATH) {
                if (parsedEvents[i].getRequestID() == 0) {
                    autoEvents++;
                    logWriter.println("=> found auto VM_DEATH event!");
                    // for automatical event suspend policy can be changed
                } else {
                    logWriter.println("## FAILURE: VM_DEATH event "
                            + "with unexpected RequestID: " + eventRequestID);
                    result = 1;
                }
            } else {
                wrongEvents++;
                logWriter.println("## FAILURE: unexpected event kind: "
                        + eventKind);
                result = 2;
            }
        }

        if (1 == result)
            fail("VM_DEATH event with unexpected RequestID");
        else if (2 == result)
            fail("Unexpected event kind");

        logWriter.println("==> testSingleStep001 PASSED!");
    }

    protected void beforeDebuggeeStart(JDWPUnitDebuggeeWrapper debuggeeWrapper) {
        settings.setAttachConnectorKind();
        if (settings.getTransportAddress() == null) {
            settings.setTransportAddress(TestOptions.DEFAULT_ATTACHING_ADDRESS);
        }
        logWriter.println("ATTACH connector kind");
        super.beforeDebuggeeStart(debuggeeWrapper);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SingleStepTest.class);
    }
}
