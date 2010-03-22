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

import org.apache.harmony.jpda.tests.framework.Breakpoint;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for BREAKPOINT event.
 */
public class BreakpointTest extends JDWPEventTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BreakpointTest.class);
    }
    
    protected String getDebuggeeClassName() {
        return BreakpointDebuggee.class.getName();
    }
    
    /**
     * This testcase is for BREAKPOINT event.
     * <BR>It runs BreakpointDebuggee and set breakpoint to its breakpointTest 
     * method, then verifies that requested BREAKPOINT event occurs.
     */
    public void testSetBreakpointEvent() {
        logWriter.println("testSetBreakpointEvent started");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        Breakpoint breakpoint = new Breakpoint("Lorg/apache/harmony/jpda/tests/jdwp/Events/BreakpointDebuggee;", "breakpointTest", 1);
        ReplyPacket reply;
        reply = debuggeeWrapper.vmMirror.setBreakpoint(JDWPConstants.TypeTag.CLASS, breakpoint);
        checkReplyPacket(reply, "Set BREAKPOINT event");

        logWriter.println("starting thread");

        // execute the breakpoint
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        CommandPacket event = null;
        event = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);

        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.BREAKPOINT,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.BREAKPOINT),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        
        logWriter.println("BreakpointTest done");
    }
}
