/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Anatoly F. Bondarenko
 */

/**
 * Created on 26.10.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.EventMod;
import org.apache.harmony.jpda.tests.framework.jdwp.EventPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for possible combined (co-located) events:
 * METHOD_ENTRY, SINGLE_STEP, BREAKPOINT, METHOD_EXIT.
 */
public class CombinedEvents003Test extends CombinedEventsTestCase {

    private String debuggeeSignature = 
        "Lorg/apache/harmony/jpda/tests/jdwp/Events/CombinedEvents003Debuggee;";

    private String methodForEvents = "emptyMethod";

    private String methodEntryClassNameRegexp = 
        "org.apache.harmony.jpda.tests.jdwp.Events.CombinedEvents003Debuggee";
    
    private boolean eventVmDeathReceived = false;

    protected String getDebuggeeClassName() {
        return CombinedEvents003Debuggee.class.getName();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CombinedEvents003Test.class);
    }

    /**
     * This test case checks events:
     * METHOD_ENTRY, SINGLE_STEP, BREAKPOINT, METHOD_EXIT
     * for empty method.
     */

    public void testCombinedEvents003_01() {
        logWriter.println("==> testCombinedEvents003_01 started");

        byte[] EXPECTED_EVENTS_ARRAY = { JDWPConstants.EventKind.METHOD_ENTRY,
                JDWPConstants.EventKind.BREAKPOINT,
                JDWPConstants.EventKind.SINGLE_STEP,
                JDWPConstants.EventKind.METHOD_EXIT};

        String debuggeeMainThreadName = synchronizer.receiveMessage();

        long debuggeeClassID = debuggeeWrapper.vmMirror
                .getClassID(debuggeeSignature);
        logWriter.println("=> debuggeeClassID = " + debuggeeClassID);
        
        long threadID = debuggeeWrapper.vmMirror.getThreadID(debuggeeMainThreadName);
        logWriter.println("=> threadID = " + threadID);
        
        long runMethodID = debuggeeWrapper.vmMirror.getMethodID(debuggeeClassID, "run");
        logWriter.println("=> runMethodID = " + runMethodID);

        long dummyMethodID = debuggeeWrapper.vmMirror.getMethodID(debuggeeClassID, "dummyMethod");
        logWriter.println("=> dummyMethodID = " + dummyMethodID);

        logWriter.println("");
        logWriter.println("=> Info for tested method '" + methodForEvents + "':");
        long testedMethodID = debuggeeWrapper.vmMirror.getMethodID(debuggeeClassID, methodForEvents);
        if (testedMethodID == -1 ) {
            String failureMessage = "## FAILURE: Can NOT get MethodID for class '"
                + getDebuggeeClassName() + "'; Method name = " + methodForEvents;
            printErrorAndFail(failureMessage);
        }
        logWriter.println("=> testedMethodID = " + testedMethodID);
        printMethodLineTable(debuggeeClassID, null, methodForEvents);
        
        // set requests for events that will be checked
        logWriter.println("");
        logWriter.println("=> Set request for BREAKPOINT event in debuggee: "
                + getDebuggeeClassName() + ", beginning of method: "
                + methodForEvents);
        Location combinedEventsLocation = getMethodEntryLocation(debuggeeClassID, methodForEvents);
        if ( combinedEventsLocation == null ) {
            String failureMessage = "## FAILURE: Can NOT get MethodEntryLocation for method '"
                + methodForEvents + "'";
            printErrorAndFail(failureMessage);
        }
        ReplyPacket reply = debuggeeWrapper.vmMirror.setBreakpoint(combinedEventsLocation);
        int breakpointRequestID = reply.getNextValueAsInt();
        logWriter.println("=> Breakpoint requestID = " + breakpointRequestID);

        logWriter.println("=> Set request for METHOD_ENTRY event in debuggee: "
                + getDebuggeeClassName());
        reply = debuggeeWrapper.vmMirror
                .setMethodEntry(methodEntryClassNameRegexp);
        checkReplyPacket(reply, "Set METHOD_ENTRY event");
        int methodEntryRequestID = reply.getNextValueAsInt();
        logWriter.println("=> METHOD_ENTRY requestID = " + methodEntryRequestID);
        logWriter.println("=> Set request for METHOD_EXIT event in debuggee: "
                + getDebuggeeClassName());
        reply = debuggeeWrapper.vmMirror
                .setMethodExit(methodEntryClassNameRegexp);
        checkReplyPacket(reply, "Set METHOD_EXIT event");
        int methodExitRequestID = reply.getNextValueAsInt();
        logWriter.println("=> METHOD_EXIT requestID = " + methodExitRequestID);

        logWriter.println("=> Set request for SINGLE_STEP event in class "
                + debuggeeSignature);
        CommandPacket setRequestCommand = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);
        setRequestCommand
                .setNextValueAsByte(JDWPConstants.EventKind.SINGLE_STEP);
        setRequestCommand.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        setRequestCommand.setNextValueAsInt(2);
        setRequestCommand.setNextValueAsByte(EventMod.ModKind.Step);
        setRequestCommand.setNextValueAsThreadID(threadID);
        setRequestCommand.setNextValueAsInt(JDWPConstants.StepSize.MIN);
        setRequestCommand.setNextValueAsInt(JDWPConstants.StepDepth.INTO);
        setRequestCommand.setNextValueAsByte(EventMod.ModKind.ClassOnly);
        setRequestCommand.setNextValueAsReferenceTypeID(debuggeeClassID);

        ReplyPacket setRequestReply = debuggeeWrapper.vmMirror
                .performCommand(setRequestCommand);
        checkReplyPacket(setRequestReply, "EventRequest::Set command");
        int stepRequestID = setRequestReply.getNextValueAsInt();
        logWriter.println("=> SINGLE_STEP requestID = " + stepRequestID);

        logWriter.println("");
        logWriter.println("=> Send SGNL_CONTINUE signal to debuggee...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("=> Try to receive and check combined events: "
            + " METHOD_ENTRY, SINGLE_STEP, BREAKPOINT, METHOD_EXIT events; ignore single SINGLE_STEP event");
        receiveAndCheckEvents(EXPECTED_EVENTS_ARRAY, combinedEventsLocation);
        if ( eventVmDeathReceived ) {
            logWriter.println("==> testCombinedEvents001 is FINISHing as VM_DEATH is received!");
            return;
        }

        logWriter.println("");
        logWriter.println("=> Clean request for METHOD_ENTRY event...");
        ReplyPacket clearReply = debuggeeWrapper.vmMirror.clearEvent(
                JDWPConstants.EventKind.METHOD_ENTRY, methodEntryRequestID);
        checkReplyPacket(clearReply, "EventRequest::Clear");

        logWriter.println("");
        logWriter.println("=> Clean request for SINGLE_STEP event...");
        clearReply = debuggeeWrapper.vmMirror.clearEvent(
                JDWPConstants.EventKind.SINGLE_STEP, stepRequestID);
        checkReplyPacket(clearReply, "EventRequest::Clear");

        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        // check that no other events, except VM_DEATH, will be received
        checkVMDeathEvent();

        logWriter.println("");
        logWriter.println("==> testCombinedEvents003_01 PASSED");
    }
    
    void receiveAndCheckEvents(byte[] EXPECTED_EVENTS_ARRAY, Location expectedLocation) {
        int EXPECTED_EVENTS_COUNT = EXPECTED_EVENTS_ARRAY.length;
        for (;;) {
            logWriter.println("=>");
            logWriter.println("=> Receiving events...");
            EventPacket event = debuggeeWrapper.vmMirror.receiveEvent();
            ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);

            // print all received events
            logWriter.println("=> Received event packet with events number = " + parsedEvents.length + " :");
            
            for (int i = 0; i < parsedEvents.length; i++) {
                logWriter.println("");
                logWriter.println("=> Event #" + (i+1) + " in packet -");
                logWriter.println("=> EventKind: "
                        + parsedEvents[i].getEventKind()
                        + "["
                        + JDWPConstants.EventKind.getName(parsedEvents[i]
                                .getEventKind()) + "]");
                logWriter.println("=> RequestID: "
                        + parsedEvents[i].getRequestID());
                if ( parsedEvents[i].getEventKind() == JDWPConstants.EventKind.VM_DEATH ) {
                    eventVmDeathReceived = true;
                }
            }
            if ( eventVmDeathReceived ) {
                return;
            }
            checkEventsLocation(parsedEvents, null); // DBG__

            // the following code checks received events
            if (parsedEvents.length == 1) {
                debuggeeWrapper.vmMirror.resume();
                continue;
/* DBG__                
                if (parsedEvents[0].getEventKind() == JDWPConstants.EventKind.SINGLE_STEP) {
                    logWriter.println("");
                    logWriter.println("=> Resume debuggee");
                    logWriter.println("");
                    debuggeeWrapper.vmMirror.resume();
                    continue;
                } else {
                    logWriter.println("##FAILURE: received unexpected event: "
                            + parsedEvents[0].getEventKind()
                            + "["
                            + JDWPConstants.EventKind.getName(parsedEvents[0]
                                    .getEventKind())
                            + "] instead of SINGLE_STEP");
                    fail("received event is not SINGLE_STEP event: "
                            + parsedEvents[0].getEventKind()
                            + "["
                            + JDWPConstants.EventKind.getName(parsedEvents[0]
                                    .getEventKind()) + "]");
                }
// DBG__ */                
            //DBG__} else if (parsedEvents.length == EXPECTED_EVENTS_COUNT) {
            } else if (parsedEvents.length > 1) {
                logWriter.println("");
                logWriter
                        .println("=> Combined events are received. Check events..");
                boolean success = true;
                for (int i = 0; i < parsedEvents.length; i++) {
                    boolean isFound = false;
                    for (int j = 0; j < EXPECTED_EVENTS_COUNT; j++) {
                        if (parsedEvents[i].getEventKind() == EXPECTED_EVENTS_ARRAY[j]) {
                            EXPECTED_EVENTS_ARRAY[j] = 0;
                            isFound = true;
                            break;
                        }
                    }
                    if (!isFound) {
                        logWriter.println("##FAILURE: received unexpected event: "
                                        + parsedEvents[i].getEventKind()
                                        + "["
                                        + JDWPConstants.EventKind
                                                .getName(parsedEvents[0]
                                                        .getEventKind()) + "]");
                        success = false;
                    }
                }
                if (!success) {
                    logWriter.println("");
                    logWriter.println("##FAILURE: the following expected events were not received: ");
                    for (int k = 0; k < EXPECTED_EVENTS_COUNT; k++) {
                        if (EXPECTED_EVENTS_ARRAY[k] != 0)
                            logWriter.println("  #"
                                    + k
                                    + ": "
                                    + EXPECTED_EVENTS_ARRAY[k]
                                    + "["
                                    + JDWPConstants.EventKind
                                            .getName(EXPECTED_EVENTS_ARRAY[k])
                                    + "]");
                    }
                    fail("not all expected events were received");
                }
                for (int i = 0; i < parsedEvents.length; i++) {
                    byte eventKind = parsedEvents[i].getEventKind();
                    long eventThreadID = ((ParsedEvent.EventThread)parsedEvents[i]).getThreadID();
                    logWriter.println("");
                    logWriter.println("=> Chcek location for combined event N " + (i+1) 
                        + ": Event kind = " + eventKind + "(" 
                        + JDWPConstants.EventKind.getName(eventKind) +"); eventThreadID = "
                        + eventThreadID);
                    Location eventLocation = null;
                    switch ( eventKind ) {
                    case JDWPConstants.EventKind.METHOD_ENTRY:
                        eventLocation = ((ParsedEvent.Event_METHOD_ENTRY)parsedEvents[i]).getLocation();
                        break;
                    case JDWPConstants.EventKind.SINGLE_STEP:
                        eventLocation = ((ParsedEvent.Event_SINGLE_STEP)parsedEvents[i]).getLocation();
                        break;
                    case JDWPConstants.EventKind.BREAKPOINT:
                        eventLocation = ((ParsedEvent.Event_BREAKPOINT)parsedEvents[i]).getLocation();
                        break;
                    case JDWPConstants.EventKind.METHOD_EXIT:
                        eventLocation = ((ParsedEvent.Event_METHOD_EXIT)parsedEvents[i]).getLocation();
                        break;
                    }
                    long eventClassID = eventLocation.classID;
                    logWriter.println("=> ClassID in event = " + eventClassID);
                    if ( expectedLocation.classID != eventClassID ) {
                        logWriter.println("## FAILURE: Unexpected ClassID in event!");
                        logWriter.println("##          Expected ClassID  = " + expectedLocation.classID );
                        success = false;
                    } else {
                        logWriter.println("=> OK - it is expected ClassID");
                    }
                    long eventMethodID = eventLocation.methodID;
                    logWriter.println("=> MethodID in event = " + eventMethodID);
                    if ( expectedLocation.methodID != eventMethodID ) {
                        logWriter.println("## FAILURE: Unexpected MethodID in event!");
                        logWriter.println("##          Expected MethodID = " + expectedLocation.methodID);
                        success = false;
                    } else {
                        logWriter.println("=> OK - it is expected MethodID");
                    }
                    long eventCodeIndex = eventLocation.index;
                    logWriter.println("=> CodeIndex in event = " + eventCodeIndex);
                    if ( expectedLocation.index != eventCodeIndex ) {
                        logWriter.println("## FAILURE: Unexpected CodeIndex in event!");
                        logWriter.println("##          Expected CodeIndex = " 
                            + expectedLocation.index);
                        success = false;
                    } else {
                        logWriter.println("=> OK - it is expected CodeIndex)");
                    }
                }
                logWriter.println("");
                if ( ! success ) {
                    String failureMessage = "## FAILURE: Unexpected events' locations are found out!";
                    logWriter.println(failureMessage);
                    //DBG__printErrorAndFail(failureMessage);
                } else {
                    logWriter.println("=> OK - all combined events have expected location!");
                }
                break;
            } else {
                logWriter.println("##FAILURE: received unexpected number of events: "
                                + parsedEvents.length
                                + " instead of 1 or "
                                + EXPECTED_EVENTS_COUNT);
                fail("received unexpected number of events: "
                        + parsedEvents.length);
            }
        }
    }

    void checkVMDeathEvent() {
        if ( eventVmDeathReceived ) {
            return;
        }
        logWriter.println("=> Wait for VM_DEATH event...");
        while ( true ) { //DBG_
            logWriter.println("=> Receiving events...");
            EventPacket event = debuggeeWrapper.vmMirror.receiveEvent();
            ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
            if (parsedEvents.length != 1
                    || parsedEvents[0].getEventKind() != JDWPConstants.EventKind.VM_DEATH) {
                // print all received events
                logWriter.println("##FAILURE: Received unexpected events");
                logWriter.println("=> Events received: " + parsedEvents.length);
                for (int i = 0; i < parsedEvents.length; i++) {
                    logWriter.println("");
                    logWriter.println("=> Event #" + i + ";");
                    logWriter.println("=> EventKind: "
                            + parsedEvents[i].getEventKind()
                            + "["
                            + JDWPConstants.EventKind.getName(parsedEvents[i]
                                    .getEventKind()) + "]");
                    logWriter.println("=> RequestID: "
                            + parsedEvents[i].getRequestID());
                }
                checkEventsLocation(parsedEvents, null); // DBG__
                logWriter.println("=> Resume debuggee"); //DBG__
                debuggeeWrapper.vmMirror.resume(); //DBG__
                continue; //DBG__
                //DBG__fail("unexpected events received");
            }
            logWriter.println("=> OK - VM_DEATH event was received. ");
            break; //DBG__
        } // DBG__
    }
}
