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
 * Created on 07.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for METHOD_EXIT event.
 */
public class MethodExitTest extends JDWPEventTestCase {

    protected String getDebuggeeClassName() {
        return MethodEntryDebuggee.class.getName();
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(MethodExitTest.class);
    }
    
    /**
     * This testcase is for METHOD_EXIT event.
     * <BR>It runs MethodEntryDebuggee that executed its own method 
     * and verify that requested METHOD_EXIT event occurs.
     */
    public void testMethodExit() {
        logWriter.println("testMethodExit started");

        String methodExitClassNameRegexp = "org.apache.harmony.jpda.tests.jdwp.Events.MethodEntryDebuggee"; 
        //String methodExitClassNameSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/MethodEntryDebuggee;"; 

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        ReplyPacket reply = debuggeeWrapper.vmMirror.setMethodExit(methodExitClassNameRegexp);
        checkReplyPacket(reply, "Set METHOD_EXIT event");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        CommandPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);

        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.METHOD_EXIT,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.METHOD_EXIT),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));

        logWriter.println("MethodExitTest done");
    }
}
