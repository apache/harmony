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
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for requested VM_DEATH event.
 */

public class VMDeath002Test extends JDWPSyncTestCase {

    int requestID = 0;

    static String DEBUGGEE_CLASS_NAME = "org.apache.harmony.jpda.tests.jdwp.Events.EventDebuggee";

    protected String getDebuggeeClassName() {
        return DEBUGGEE_CLASS_NAME;
    }

    /**
     * This testcase is for requested VM_DEATH event.<BR>
     * It starts EventDebuggee class, send request for
     * VM_DEATH event, receives events and verifies that:
     * <BR>&nbsp;&nbsp; - requested VM_DEATH event
     * is received with the expected RequestID, returned by EventRequest.Set command;
     * <BR>&nbsp;&nbsp; - automatic VM_DEATH event
     * is received with the RequestID = 0;
     */
    public void testVMDeathRequest() {
        logWriter.println("==> testVMDeathRequest started");

        //check capability, relevant for this test
        logWriter.println("=> Check capability: canRequestVMDeathEvent");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canRequestVMDeathEvent;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canRequestVMDeathEvent");
            return;
        }
        boolean success = true;

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        //set request for VM_DEATH event with suspend policy SUSPEND_ALL
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        logWriter.println("=> Create request for VM_DEATH event with suspend policy: "
                + suspendPolicy + "/" 
                + JDWPConstants.SuspendPolicy.getName(suspendPolicy));
        CommandPacket setRequestCommand = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);

        setRequestCommand.setNextValueAsByte(JDWPConstants.EventKind.VM_DEATH);
        setRequestCommand.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        setRequestCommand.setNextValueAsInt(0);

        ReplyPacket setRequestReply = debuggeeWrapper.vmMirror
                .performCommand(setRequestCommand);
        checkReplyPacket(setRequestReply, "Set VM_DEATH event");

        requestID = setRequestReply.getNextValueAsInt();
        logWriter.println("==> RequestID = " + requestID);

        assertAllDataRead(setRequestReply);

        //release debuggee
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        //receive and parse event set
        logWriter.println("=> Wait for VM_DEATH event");
        CommandPacket eventPacket = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(eventPacket);
        int eventsCount = parsedEvents.length;
        logWriter.println("==> Received event set: count=" + eventsCount);

        //ckeck if received events are expected
        int requestedEvents = 0;
        int autoEvents = 0;
        int wrongEvents = 0;
        for (int i = 0; i < eventsCount; i++) {
            ParsedEvent event = parsedEvents[i];
            logWriter.println("=> Event #" + i + ";");
           
            // print event info
            byte eventSuspendPolicy = event.getSuspendPolicy();
            logWriter.println("===> SuspendPolicy=" + eventSuspendPolicy + "/"
                    + JDWPConstants.SuspendPolicy.getName(eventSuspendPolicy));
            byte eventKind = event.getEventKind();
            logWriter.println("===> EventKind=" + eventKind + "/"
                    + JDWPConstants.EventKind.getName(eventKind));
            int eventRequestID = event.getRequestID();
            logWriter.println("===> RequestID=" + eventRequestID);
           
            // check if event is expected
            if (eventKind == JDWPConstants.EventKind.VM_DEATH) {
                if (eventRequestID == requestID) {
                    requestedEvents++;
                    logWriter.println("===> found requested VM_DEATH event!");

                    // check suspend p[olicy for requested event
                    if (eventSuspendPolicy != suspendPolicy) {
                        logWriter.println("## FAILURE: requested VM_DEATH event "
                                              + "with unexpected SuspendPolicy: " 
                                              + eventSuspendPolicy);
                        success = false;
                    }
                } else if (parsedEvents[i].getRequestID() == 0) {
                    autoEvents++;
                    logWriter.println("===> found auto VM_DEATH event!");
                    // for automatical event suspend policy can be changed
                } else {
                    logWriter.println("## FAILURE: VM_DEATH event "
                                               + "with unexpected RequestID: " 
                                               + eventRequestID);
                    success = false;
                }
            } else {
                wrongEvents++;
                logWriter.println("## FAILURE: unexpected event kind: "
                                               + eventKind);
                success = false;
            }
        }
        
        // check number of found events
        {
            if (eventsCount != 2) {
                logWriter.println("## FAILURE: received wrong number of events: "
                                               + eventsCount);
                success = false;
            }

            if (requestedEvents > 1) {
                logWriter.println("## FAILURE: too many requested VM_DEATH events: " 
                                               + requestedEvents);
                success = false;
            } else if (requestedEvents < 1) {
                logWriter.println("## FAILURE: received no requested VM_DEATH events: "
                                               + requestedEvents);
                success = false;
            }

            if (autoEvents > 1) {
                logWriter.println("## FAILURE: too many auto VM_DEATH events: " 
                                               + autoEvents);
                success = false;
            } else if (autoEvents < 1) {
                logWriter.println("## FAILURE: received no auto VM_DEATH events: " 
                                               + autoEvents);
                success = false;
            }

            if (wrongEvents > 0) {
                logWriter.println("## FAILURE: Received unexpected events: " 
                                               + wrongEvents);
                success = false;
            }

            assertTrue("Failure in processing VM_DEATH event", success);
        }
        
        logWriter.println("=> Resume debuggee");
        resumeDebuggee();

        logWriter.println("==> test PASSED!");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(VMDeath002Test.class);
    }
}
