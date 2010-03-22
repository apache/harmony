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

package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent.Event_MONITOR_CONTENDED_ENTER;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class MonitorContendedEnterTest extends JDWPSyncTestCase {
    String monitorSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/MonitorWaitMockMonitor;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.Events.MonitorContendedEnterAndEnteredDebuggee";
    }

    public void testMonitorContendedEnterForClassMatch() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        // Set MONITOR_CONTENDED_ENTER request for MonitorContendedEnterAndEnteredDebuggee
        logWriter.println("==> Debuggee class pattern to match = " + getDebuggeeClassName() + "*");
        debuggeeWrapper.vmMirror.setMonitorContendedEnterForClassMatch(getDebuggeeClassName()+"*");

        // Verify received event
        verifyEvent();
    }

    private void verifyEvent(){
        // Inform debuggee that the request has been set
        logWriter.println("==> Request has been set.");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // Receive event of MONITOR_CONTENDED_ENTER
        logWriter.println("==> Receive Event.");
        CommandPacket receiveEvent = null;
        try {
            receiveEvent = debuggeeWrapper.vmMirror.receiveEvent();
        } catch (TestErrorException e) {
            printErrorAndFail("There is no event received.");
        }
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(receiveEvent);
        Event_MONITOR_CONTENDED_ENTER event = (ParsedEvent.Event_MONITOR_CONTENDED_ENTER)parsedEvents[0];
        
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,", JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER, event.getEventKind()
                , JDWPConstants.EventKind.getName(JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER)
                , JDWPConstants.EventKind.getName(event.getEventKind()));
        logWriter.println("==> CHECK: Event Kind: " + JDWPConstants.EventKind.getName(JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER));
        
        // Getting ID of the tested thread
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = debuggeeWrapper.vmMirror
                .getThreadID(MonitorContendedEnterAndEnteredDebuggee.TESTED_THREAD);
        assertEquals("Invalid tested thread id: ", testedThreadID
              , event.getThreadID());
        logWriter.println("==> CHECK: tested blocked thread id: " + testedThreadID );
        
        // Get monitor object from event packet
        long objID = event.getTaggedObject().objectID;
        // Check the ReferenceType of monitor object
        long refID = debuggeeWrapper.vmMirror.getReferenceType(objID);
        String actualSignature =  debuggeeWrapper.vmMirror.getReferenceTypeSignature(refID);
        assertEquals("Invalid monitor class signature: ", monitorSignature
                , actualSignature);
        logWriter.println("==> CHECK: monitor class signature: " + actualSignature);
    }

}
