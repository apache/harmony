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
 * Created on 11.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Event;
import org.apache.harmony.jpda.tests.framework.jdwp.EventMod;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent.*;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for THREAD_END event.
 */
public class ThreadEndTest extends JDWPEventTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ThreadEndTest.class);
    }
    
    /**
     * This testcase is for THREAD_END event.
     * <BR>It runs EventDebuggee and verifies that requested 
     * THREAD_END event occurs.
     */
    public void testThreadEndEvent001() {
        logWriter.println("==> testThreadEndEvent001 - STARTED...");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("=> set ThreadEndEvent...");
        ReplyPacket reply;
        byte eventKind = JDWPConstants.EventKind.THREAD_END;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.NONE;
        EventMod[] mods = new EventMod[0];
        Event eventToSet = new Event(eventKind, suspendPolicy, mods);

        reply = debuggeeWrapper.vmMirror.setEvent(eventToSet);
        checkReplyPacket(reply, "Set THREAD_END event");

        logWriter.println("=> set ThreadEndEvent - DONE");

        // start the thread
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // wait for thread start and finish
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("=> vmMirror.receiveEvent()...");
        CommandPacket event = debuggeeWrapper.vmMirror.receiveEvent();

        assertNotNull("Invalid (null) event received", event);
        logWriter.println("=> Event received!");
        
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
        logWriter.println("=> Number of events = " + parsedEvents.length);
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        logWriter.println("=> EventKind() = " + parsedEvents[0].getEventKind()
                + " (" + JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()) + ")");
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.THREAD_END,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.THREAD_END),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        logWriter.println("=> EventRequestID() = " + parsedEvents[0].getRequestID());

        long threadID = ((Event_THREAD_DEATH)parsedEvents[0]).getThreadID();
        logWriter.println("=> threadID = " + threadID);
        String threadName = debuggeeWrapper.vmMirror.getThreadName(threadID);
        logWriter.println("=> threadName = " + threadName);
        assertEquals("Invalid thread name", EventDebuggee.testedThreadName, threadName);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> testThreadEndEvent001 - OK");
    }
}
