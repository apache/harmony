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
 * Created on 14.07.2005
 */

package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.EventMod;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for SINGLE_STEP event.
 */
public class SingleStepTest extends JDWPEventTestCase {

    private String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/SingleStepDebuggee;";

    private String DEBUGGEE_CLASS_NAME = "org.apache.harmony.jpda.tests.jdwp.Events.SingleStepDebuggee";

    protected String getDebuggeeClassName() {
        return DEBUGGEE_CLASS_NAME;
    }

    /**
     * This test case exercises SINGLE_STEP event.<BR>
     * Runs stepFunction() function four times to checks
     * SINGLE_STEP event with LINE and OVER steps.
     *  
     */
    public void testSingleStep1() {
        stepFunction(JDWPConstants.StepSize.LINE, JDWPConstants.StepDepth.OVER);

    }

    /**
     * This test case exercises SINGLE_STEP event.<BR>
     * Runs stepFunction() function four times to checks
     * SINGLE_STEP event with LINE and INTO steps.
     *  
     */
    public void testSingleStep2() {
        stepFunction(JDWPConstants.StepSize.LINE, JDWPConstants.StepDepth.INTO);
    }

    /**
     * This test case exercises SINGLE_STEP event.<BR>
     * Runs stepFunction() function four times to checks
     * SINGLE_STEP event with MIN and OVER steps.
     *  
     */
    public void testSingleStep3() {
        stepFunction(JDWPConstants.StepSize.MIN, JDWPConstants.StepDepth.OVER);
    }

    /**
     * This test case exercises SINGLE_STEP event.<BR>
     * Runs stepFunction() function four times to checks
     * SINGLE_STEP event with MIN and INTO steps.
     *  
     */
    public void testSingleStep4() {
        stepFunction(JDWPConstants.StepSize.MIN, JDWPConstants.StepDepth.INTO);
    }

    /**
     * Runs SingleStepDebuggee and sets breakpoint to its
     * breakpointTest method, sends a request for single step event, then
     * verifies that requested SINGLE_STEP event with StepSize and StepDepth
     * steps occurs.
     */

    void stepFunction(byte StepSize, byte StepDepth) {
        logWriter.println("=> testSingleStep started");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        //find checked method
        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> Send ReferenceType::Methods command and get methodIDs ");

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

        //resume debuggee
        resumeDebuggee();

        //receive event
        logWriter.println("==> Wait for SINGLE_STEP event");
        CommandPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);

        //check if received event is expected
        logWriter.println("==> Received " + parsedEvents.length + " events");

        // trace events
        for (int i = 0; i < parsedEvents.length; i++) {
            logWriter.println("");
            logWriter.println("==> Event #" + i + ";");
            logWriter.println("==> EventKind: " + parsedEvents[i].getEventKind() + "("
                    + JDWPConstants.EventKind.getName(parsedEvents[i].getEventKind()) + ")");
            logWriter.println("==> RequestID: " + parsedEvents[i].getRequestID());
        }

        // check all
        assertEquals("Received wrong number of events,", 1, parsedEvents.length);
        assertEquals("Received wrong event request ID,", requestID, parsedEvents[0].getRequestID());
        assertEquals("Invalid event kind,", JDWPConstants.EventKind.SINGLE_STEP,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.SINGLE_STEP),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));

        // clear SINGLE_STEP event
        logWriter.println("==> Clearing SINGLE_STEP event..");
        //ReplyPacket reply =
            debuggeeWrapper.vmMirror.clearEvent(JDWPConstants.EventKind.SINGLE_STEP, (int) requestID);
        checkReplyPacket(setRequestReply, "Clear SINGLE_STEP event");
        logWriter.println("==> SINGLE_STEP event has been cleared");

        // resuming debuggee
        logWriter.println("==> Resuming debuggee");
        resumeDebuggee();
        logWriter.println("==> Test PASSED!");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SingleStepTest.class);
    }
}
