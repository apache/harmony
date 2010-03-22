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
 * Created on 06.10.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for METHOD_ENTRY, METHOD_EXIT events for empty method.
 */
public class CombinedEvents002Test extends CombinedEventsTestCase {
    static final String TESTED_CLASS_NAME = 
        CombinedEvents002Debuggee.TESTED_CLASS_NAME; 
    static final String TESTED_CLASS_SIGNATURE = 
        CombinedEvents002Debuggee.TESTED_CLASS_SIGNATURE;
    static final String TESTED_METHOD_NAME = CombinedEvents002Debuggee.TESTED_METHOD_NAME;

    protected String getDebuggeeClassName() {
        return CombinedEvents002Debuggee.class.getName();
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(CombinedEvents002Test.class);
    }
    
    /**
     * This testcase is for METHOD_ENTRY, METHOD_EXIT events for empty method.
     * <BR>It runs CombinedEvents002Debuggee that executed its own empty method 
     * and verify that requested METHOD_ENTRY, METHOD_EXIT events occur
     * for empty method.
     */
    public void testCombinedEvents002_01() {
        logWriter.println("==> testCombinedEvents002_01: Start...");

        logWriter.println("==> Wait for SGNL_READY signal from debuggee...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("==> OK - SGNL_READY signal received!");
        
        long testedClassID = 
            debuggeeWrapper.vmMirror.getClassID(TESTED_CLASS_SIGNATURE);
        if ( testedClassID == -1 ) {
            String failureMessage = "## FAILURE: Can NOT get ClassID for '"
                + TESTED_CLASS_SIGNATURE + "'";
            printErrorAndFail(failureMessage);
        }
        logWriter.println("==> Tested Class Name = '" + TESTED_CLASS_NAME + "'");
        logWriter.println("==> testedClassID = " + testedClassID);
        
        logWriter.println("==> ");
        logWriter.println("==> Info for tested method '" + TESTED_METHOD_NAME + "':");
        long testedMethodID = debuggeeWrapper.vmMirror.getMethodID(testedClassID, TESTED_METHOD_NAME);
        if (testedMethodID == -1 ) {
            String failureMessage = "## FAILURE: Can NOT get MethodID for class '"
                + TESTED_CLASS_NAME + "'; Method name = " + TESTED_METHOD_NAME;
            printErrorAndFail(failureMessage);
        }
        logWriter.println("==> testedMethodID = " + testedMethodID);
        printMethodLineTable(testedClassID, null, TESTED_METHOD_NAME);
        long testedMethodStartCodeIndex = getMethodStartCodeIndex(testedClassID, TESTED_METHOD_NAME);
        if ( testedMethodStartCodeIndex == -1 ) {
            String failureMessage = "## FAILURE: Can NOT get MethodStartCodeIndex for method '"
                + TESTED_METHOD_NAME + "' ";
            printErrorAndFail(failureMessage);
        }
        long testedMethodEndCodeIndex = getMethodEndCodeIndex(testedClassID, TESTED_METHOD_NAME);
        if ( testedMethodEndCodeIndex == -1 ) {
            String failureMessage = "## FAILURE: Can NOT get MethodEndCodeIndex for method '"
                + TESTED_METHOD_NAME + "' ";
            printErrorAndFail(failureMessage);
        }

        logWriter.println("==> ");
        logWriter.println("==> Set request for METHOD_ENTRY event for '" + TESTED_CLASS_NAME + "'... ");
        ReplyPacket reply = debuggeeWrapper.vmMirror.setMethodEntry(TESTED_CLASS_NAME);
        checkReplyPacket(reply, "Set METHOD_ENTRY event.");  //DBG needless ?
        logWriter.println("==> OK - request for METHOD_ENTRY event is set!");

        logWriter.println("==> Set request for METHOD_EXIT event for '" + TESTED_CLASS_NAME + "'... ");
        reply = debuggeeWrapper.vmMirror.setMethodExit(TESTED_CLASS_NAME);
        checkReplyPacket(reply, "Set METHOD_EXIT event.");  //DBG needless ?
        logWriter.println("==> OK - request for METHOD_EXIT event is set!");
        
        logWriter.println("==> Send SGNL_CONTINUE signal to debuggee...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("==> ");
        logWriter.println("==> Receiving events... ");
        CommandPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
        byte[] expectedEventKinds 
            = {JDWPConstants.EventKind.METHOD_ENTRY, JDWPConstants.EventKind.METHOD_EXIT};
        
        int receivedEventsNumber = parsedEvents.length;
        byte[] receivedEventKinds = new byte[(receivedEventsNumber==1)? 2 : receivedEventsNumber];
        logWriter.println("==> Number of received events in event packet = " + receivedEventsNumber);
        for (int i=0; i < receivedEventsNumber; i++) {
            receivedEventKinds[i] = parsedEvents[i].getEventKind();
            logWriter.println("==> Received event[" + i + "] kind = " 
                +  receivedEventKinds[i]
                + "(" + JDWPConstants.EventKind.getName(receivedEventKinds[i]) + ")");
        }
        if ( receivedEventsNumber > 2 ) {
            String failureMessage = "## FAILURE: Unexpected number of received events in packet = "
                + receivedEventsNumber + "\n## Expected number of received events in packet = 1 or 2";
            printErrorAndFail(failureMessage);
        }
        
        logWriter.println("==> ");
        logWriter.println("==> Check received event #1...");
        if ( receivedEventKinds[0] != expectedEventKinds[0] ) {
            String failureMessage = "## FAILURE: Unexpected event is received: event kind = "
                + receivedEventKinds[0] + "("
                + JDWPConstants.EventKind.getName(receivedEventKinds[0]) + ")"
                + "\n## Expected event kind = " + expectedEventKinds[0] + "("
                + JDWPConstants.EventKind.getName(expectedEventKinds[0]) + ")";
            printErrorAndFail(failureMessage);
        }
        boolean testCaseIsOk = true;
        Location location = ((ParsedEvent.Event_METHOD_ENTRY)parsedEvents[0]).getLocation();
        long eventClassID = location.classID;
        logWriter.println("==> ClassID in event = " + eventClassID);
        if ( testedClassID != eventClassID ) {
            logWriter.println("## FAILURE: Unexpected ClassID in event!");
            logWriter.println("##          Expected ClassID (testedClassID) = " + testedClassID );
            testCaseIsOk = false;
        } else {
            logWriter.println("==> OK - it is expected ClassID (testedClassID)");
        }
        long eventMethodID = location.methodID;
        logWriter.println("==> MethodID in event = " + eventMethodID);
        if ( testedMethodID != eventMethodID ) {
            logWriter.println("## FAILURE: Unexpected MethodID in event!");
            logWriter.println("##          Expected MethodID (testedMethodID) = " + testedMethodID );
            testCaseIsOk = false;
        } else {
            logWriter.println("==> OK - it is expected MethodID (testedMethodID)");
        }
        long eventCodeIndex = location.index;
        logWriter.println("==> CodeIndex in event = " + eventCodeIndex);
        if ( testedMethodStartCodeIndex != eventCodeIndex ) {
            logWriter.println("## FAILURE: Unexpected CodeIndex in event!");
            logWriter.println("##          Expected CodeIndex (testedMethodStartCodeIndex) = " 
                + testedMethodStartCodeIndex );
            testCaseIsOk = false;
        } else {
            logWriter.println("==> OK - it is expected CodeIndex (testedMethodStartCodeIndex)");
        }

        if ( receivedEventsNumber == 1 ) {
            logWriter.println("==> ");
            logWriter.println("==> Resume debuggee VM...");
            debuggeeWrapper.vmMirror.resume();     
            logWriter.println("==> Receiving events... ");
            event = debuggeeWrapper.vmMirror.receiveEvent();
            parsedEvents = ParsedEvent.parseEventPacket(event);
            
            receivedEventsNumber = parsedEvents.length;
            logWriter.println("==> Number of received events in event packet = " + receivedEventsNumber);
            receivedEventKinds[1] = parsedEvents[0].getEventKind();
            for (int i=0; i < receivedEventsNumber; i++) {
                logWriter.println("==> Received event[" + i + "] kind = " 
                    +  parsedEvents[i].getEventKind()
                    + "(" + JDWPConstants.EventKind.getName(parsedEvents[i].getEventKind()) + ")");
            }
            if ( receivedEventsNumber != 1 ) {
                String failureMessage = "## FAILURE: Unexpected number of received events in packet = "
                    + receivedEventsNumber + "\n## Expected number of received events in packet = 1";
                printErrorAndFail(failureMessage);
            }
        }
        logWriter.println("==> ");
        logWriter.println("==> Check received event #2...");
        if ( receivedEventKinds[1] != expectedEventKinds[1] ) {
            String failureMessage = "## FAILURE: Unexpected event is received: event kind = "
                + receivedEventKinds[1] + "("
                + JDWPConstants.EventKind.getName(receivedEventKinds[1]) + ")"
                + "\n## Expected event kind = " + expectedEventKinds[1] + "("
                + JDWPConstants.EventKind.getName(expectedEventKinds[1]) + ")";
            printErrorAndFail(failureMessage);
        }
        location = ((ParsedEvent.Event_METHOD_EXIT)parsedEvents[0]).getLocation();
        eventClassID = location.classID;
        logWriter.println("==> ClassID in event = " + eventClassID);
        if ( testedClassID != eventClassID ) {
            logWriter.println("## FAILURE: Unexpected ClassID in event!");
            logWriter.println("##          Expected ClassID (testedClassID) = " + testedClassID );
            testCaseIsOk = false;
        } else {
            logWriter.println("==> OK - it is expected ClassID (testedClassID)");
        }
        eventMethodID = location.methodID;
        logWriter.println("==> MethodID in event = " + eventMethodID);
        if ( testedMethodID != eventMethodID ) {
            logWriter.println("## FAILURE: Unexpected MethodID in event!");
            logWriter.println("##          Expected MethodID (testedMethodID) = " + testedMethodID );
            testCaseIsOk = false;
        } else {
            logWriter.println("==> OK - it is expected MethodID (testedMethodID)");
        }
        eventCodeIndex = location.index;
        logWriter.println("==> CodeIndex in event = " + eventCodeIndex);
        if ( testedMethodEndCodeIndex != eventCodeIndex ) {
            logWriter.println("## FAILURE: Unexpected CodeIndex in event!");
            logWriter.println("##          Expected CodeIndex (testedMethodEndCodeIndex) = " 
                + testedMethodStartCodeIndex );
            testCaseIsOk = false;
        } else {
            logWriter.println("==> OK - it is expected CodeIndex (testedMethodEndCodeIndex)");
        }
        if ( ! testCaseIsOk ) {
            String failureMessage = "## FAILURE: Unexpected events attributes are found out!";
            printErrorAndFail(failureMessage);
        }
        
        logWriter.println("==> Resume debuggee VM...");
        debuggeeWrapper.vmMirror.resume();     
        logWriter.println("==> testCombinedEvents002_01: PASSED! ");
    }
}
