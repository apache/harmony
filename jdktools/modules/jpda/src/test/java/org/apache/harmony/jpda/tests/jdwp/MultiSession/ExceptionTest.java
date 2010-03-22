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
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.jdwp.Events.ExceptionDebuggee;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for verifying canceling of EXCEPTION event after re-connection.
 */
public class ExceptionTest extends JDWPEventTestCase {

    protected String getDebuggeeClassName() {
        return ExceptionDebuggee.class.getName();
    }

    /**
     * This testcase verifies canceling of EXCEPTION event after re-connection.
     * <BR>It runs ExceptionDebuggee, sets request for EXCEPTION event
     * and re-connects.
     * <BR>It is expected that only auto VM_DEATH event occurs after re-connection,
     * but no any other event, including EXCEPTION event.
     */
    public void testException001() {
        logWriter.println(">> testException001: STARTED...");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String exceptionSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/DebuggeeException;";
        boolean isCatch = true;
        boolean isUncatch = true;
        logWriter.println("\n>> testExceptionEvent: => setException(...)...");
        debuggeeWrapper.vmMirror.setException(exceptionSignature, isCatch,
                isUncatch);
        logWriter.println(">> testExceptionEvent: setException(...) DONE");

        logWriter.println("");
        logWriter.println("=> CLOSE CONNECTION");
        closeConnection();
        logWriter.println("=> CONNECTION CLOSED");

        logWriter.println("");
        logWriter.println("=> OPEN NEW CONNECTION");
        openConnection();
        logWriter.println("=> CONNECTION OPENED");

        logWriter.println("=> Resuming debuggee");

        // start the thread
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // receive event
        logWriter.println("=> Wait for event..");
        CommandPacket eventPacket = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(eventPacket);
        int eventsCount = parsedEvents.length;
        logWriter.println("=> Received event set: count=" + eventsCount);

        // ckeck if received events are expected
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

        logWriter.println("==> testException001 PASSED!");
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
        junit.textui.TestRunner.run(ExceptionTest.class);
    }
}
