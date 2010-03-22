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
 * @author Anton V. Karnachuk
 */

/**
 * Created on 11.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.EventPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for caught EXCEPTION event.
 */
public class ExceptionTest extends JDWPEventTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ExceptionTest.class);
    }
    
    protected String getDebuggeeClassName() {
        return ExceptionDebuggee.class.getName();
    }
    
    /**
     * This testcase is for caught EXCEPTION event.
     * <BR>It runs ExceptionDebuggee that throws caught DebuggeeException.
     * The test verifies that requested EXCEPTION event occurs and reported exception
     * object is not null and is instance of expected class with expected tag.
     */
    public void testExceptionEvent() {
        logWriter.println(">> testExceptionEvent: STARTED...");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String exceptionSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/DebuggeeException;";
        boolean isCatch = true;
        boolean isUncatch = true;
        logWriter.println("\n>> testExceptionEvent: => setException(...)...");
        debuggeeWrapper.vmMirror.setException(exceptionSignature, isCatch, isUncatch);
        logWriter.println(">> testExceptionEvent: setException(...) DONE");

        logWriter.println("\n>> testExceptionEvent: send to Debuggee SGNL_CONTINUE...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n>> testExceptionEvent: => receiveEvent()...");
        EventPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        logWriter.println(">> testExceptionEvent: Event is received! Check it ...");
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
        
        // assert that event is the expected one 
        logWriter.println(">> testExceptionEvent: parsedEvents.length = " + parsedEvents.length);
        logWriter.println(">> testExceptionEvent: parsedEvents[0].getEventKind() = " + parsedEvents[0].getEventKind());
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.EXCEPTION,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.EXCEPTION),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        TaggedObject returnedException =((ParsedEvent.Event_EXCEPTION)parsedEvents[0]).getException();
        
        // assert that exception ObjectID is not null
        logWriter.println(">> testExceptionEvent: returnedException.objectID = " + returnedException.objectID);
        assertTrue("Returned exception object is null.", returnedException.objectID != 0);
        
        // assert that exception tag is OBJECT
        logWriter.println(">> testExceptionEvent: returnedException.tag = " + returnedException.objectID);
        assertEquals("Returned exception tag is not OBJECT.", JDWPConstants.Tag.OBJECT_TAG, returnedException.tag);
        
        // assert that exception class is the expected one
        long typeID = getObjectReferenceType(returnedException.objectID);
        String returnedExceptionSignature = getClassSignature(typeID);
        logWriter.println(">> testExceptionEvent: returnedExceptionSignature = |" + returnedExceptionSignature+"|");
        assertString("Invalid signature of returned exception,", exceptionSignature, returnedExceptionSignature);
        
        // resume debuggee 
        logWriter.println("\n>> testExceptionEvent: resume debuggee...");
        debuggeeWrapper.vmMirror.resume();
    }

    /**
     * This testcase is for caught EXCEPTION event and reported location.
     * <BR>It runs ExceptionDebuggee that throws caught DebuggeeException.
     * Tte test verifies that requested EXCEPTION event occurs, thread is not null,
     * reported location is not null and is equal to location of the top stack frame.
     */
    public void testExceptionEventLocation() {
        logWriter.println(">> testExceptionEventLocation: STARTED...");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String exceptionSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/DebuggeeException;";
        boolean isCatch = true;
        boolean isUncatch = true;
        logWriter.println("\n>> testExceptionEventLocation: => setException(...)...");
        debuggeeWrapper.vmMirror.setException(exceptionSignature, isCatch, isUncatch);
        logWriter.println(">> testExceptionEventLocation: setException(...) DONE");

        logWriter.println("\n>> testExceptionEventLocation: send to Debuggee SGNL_CONTINUE...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n>> testExceptionEventLocation: => receiveEvent()...");
        EventPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        logWriter.println(">> testExceptionEventLocation: Event is received! Check it ...");
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
        
        // assert that event is the expected one 
        logWriter.println(">> testExceptionEventLocation: parsedEvents.length = " + parsedEvents.length);
        logWriter.println(">> testExceptionEventLocation: parsedEvents[0].getEventKind() = " + parsedEvents[0].getEventKind());
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.EXCEPTION,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.EXCEPTION),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        long returnedThread = ((ParsedEvent.Event_EXCEPTION)parsedEvents[0]).getThreadID();
        Location returnedExceptionLoc =((ParsedEvent.Event_EXCEPTION)parsedEvents[0]).getLocation();
        
        // assert that exception thread is not null
        logWriter.println(">> testExceptionEventLocation: returnedThread = " + returnedThread);
        assertTrue("Returned exception ThreadID is null,", returnedThread != 0);

        // assert that exception location is not null
        logWriter.println(">> testExceptionEventLocation: returnedExceptionLoc = " + returnedExceptionLoc);
        assertTrue("Returned exception location is null,", returnedExceptionLoc != null);

        // assert that top stack frame location is not null
        Location topFrameLoc = getTopFrameLocation(returnedThread);
        logWriter.println(">> testExceptionEventLocation: topFrameLoc = " + topFrameLoc);
        assertTrue("Returned top stack frame location is null,", topFrameLoc != null);

        // assert that locations of exception and top frame are equal
        assertEquals("Different exception and top frame location tag,", returnedExceptionLoc.tag, topFrameLoc.tag);
        assertEquals("Different exception and top frame location class,", returnedExceptionLoc.classID, topFrameLoc.classID);
        assertEquals("Different exception and top frame location method,", returnedExceptionLoc.methodID, topFrameLoc.methodID);
        assertEquals("Different exception and top frame location index,", returnedExceptionLoc.index, topFrameLoc.index);

        // resume debuggee 
        logWriter.println("\n>> testExceptionEventLocation: resume debuggee...");
        debuggeeWrapper.vmMirror.resume();
    }

    @SuppressWarnings("unused")
    private Location getTopFrameLocation(long threadID) {

        // getting frames of the thread
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FramesCommand);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsInt(0);
        packet.setNextValueAsInt(1);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        debuggeeWrapper.vmMirror.checkReply(reply);

        // assert that only one top frame is returned
        int framesCount = reply.getNextValueAsInt();
        assertEquals("Invalid number of top stack frames,", 1, framesCount);

        long frameID = reply.getNextValueAsFrameID();
        Location loc = reply.getNextValueAsLocation();

        return loc;
    }

}
