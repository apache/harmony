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
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent.Event_MONITOR_WAITED;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class MonitorWaitedTest extends JDWPSyncTestCase {
    String monitorSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/MonitorWaitMockMonitor;";
    
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.Events.MonitorWaitAndWaitedDebuggee";
    }

    public void testMonitorWaitedForClassOnly() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Attain Object class id
        long monitorRefTypeID = getClassIDBySignature(monitorSignature);
        logWriter.println("==> Object ReferenceType ID = " + monitorRefTypeID);

        // Set MonitorWaited request for MockMonitorClass
        debuggeeWrapper.vmMirror.setMonitorWaitedForClassOnly(monitorRefTypeID);

        // Verify received event
        verifyEvent();
    }

    public void testMonitorWaitedForClassMatchExact() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String classPattern = "org.apache.harmony.jpda.tests.jdwp.Events.MonitorWaitMockMonitor";
        logWriter.println("==> tested class match pattern: " + classPattern);

        // Set MonitorWaited request for MockMonitorClass
        debuggeeWrapper.vmMirror.setMonitorWaitedForClassMatch(classPattern);

        // Verify received event
        verifyEvent();
    }
    
    public void testMonitorWaitedForClassMatchFirst() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String classPattern = "org.apache.harmony.jpda.tests.jdwp.Events.*";
        logWriter.println("==> tested class match pattern: " + classPattern);

        // Set MonitorWaited request for MockMonitorClass
        debuggeeWrapper.vmMirror.setMonitorWaitedForClassMatch(classPattern);

        // Verify received event
        verifyEvent();
    }
    
    public void testMonitorWaitedForClassMatchSecond() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String classPattern = "*MonitorWaitMockMonitor";
        logWriter.println("==> tested class match pattern: " + classPattern);

        // Set MonitorWaited request for MockMonitorClass
        debuggeeWrapper.vmMirror.setMonitorWaitedForClassMatch(classPattern);

        // Verify received event
        verifyEvent();
    }
    
    public void testMonitorWaitedForClassExclude() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String classPattern = "MockPatter*";
        logWriter.println("==> tested class exclude pattern: " + classPattern);

        // Set MonitorWaited request for MockMonitorClass
        debuggeeWrapper.vmMirror.setMonitorWaitedForClassExclude(classPattern);

        // Verify received event
        verifyEvent();
    }

    private void verifyEvent(){
        // Inform debuggee that the request has been set
        logWriter.println("==> Request has been set.");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // Receive event of MonitorWaited
        logWriter.println("==> Receive Event.");
        CommandPacket receiveEvent = null;
        try {
            receiveEvent = debuggeeWrapper.vmMirror.receiveEvent();
        } catch (TestErrorException e) {
            printErrorAndFail("There is no event received.");
        }
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(receiveEvent);
        Event_MONITOR_WAITED event = (ParsedEvent.Event_MONITOR_WAITED)parsedEvents[0];
        
        // Check event basic message
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,", JDWPConstants.EventKind.MONITOR_WAITED, parsedEvents[0].getEventKind()
                , JDWPConstants.EventKind.getName(JDWPConstants.EventKind.MONITOR_WAITED)
                , JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        logWriter.println("==> CHECK: Event Kind: " + JDWPConstants.EventKind.getName(JDWPConstants.EventKind.MONITOR_WAITED));
        
        // Check tested thread message
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = debuggeeWrapper.vmMirror.getThreadID("main");
        assertEquals("Invalid tested thread id: ", testedThreadID, event.getThreadID());
        logWriter.println("==> CHECK: tested blocked thread id: " + testedThreadID );

        // Check the ReferenceType of monitor object
        long objID = event.getTaggedObject().objectID;
        long refID = debuggeeWrapper.vmMirror.getReferenceType(objID);
        String actualSignature =  debuggeeWrapper.vmMirror.getReferenceTypeSignature(refID);
        assertEquals("Invalid monitor class signature: ", monitorSignature
                , actualSignature);
        logWriter.println("==> CHECK: monitor class signature: " + actualSignature);
        
        // Check wait's timeout
        assertEquals("Invalid reference type of prepared class,", true
                , event.getTimedout());
        logWriter.println("==> CHECK: Wait time out: " + MonitorWaitAndWaitedDebuggee.TIMEOUT);
    }

}
