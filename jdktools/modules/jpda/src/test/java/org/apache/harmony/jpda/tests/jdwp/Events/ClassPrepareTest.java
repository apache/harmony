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
 * Created on 06.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for CLASS_PREPARE event.
 */
public class ClassPrepareTest extends JDWPEventTestCase {

    protected String getDebuggeeClassName() {
        return ClassPrepareDebuggee.class.getName();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClassPrepareTest.class);
    }

    /**
     * This testcase is for CLASS_PREPARE event.
     * <BR>It runs ClassPrepareDebuggee to load Class2Prepare class
     * and verify that requested CLASS_PREPARE event occurs.
     */
    public void testClassPrepareEvent() {
        logWriter.println("testClassPrepareEvent started");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String class2prepareRegexp = "org.apache.harmony.jpda.tests.jdwp.Events.Class2Prepare"; 
        String class2prepareSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/Class2Prepare;"; 
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.setClassPrepared(class2prepareRegexp);
        checkReplyPacket(reply, "Set CLASS_PREPARE event");
        
        // start loading Class2Prepare class
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        CommandPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);
        
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,", JDWPConstants.EventKind.CLASS_PREPARE, parsedEvents[0].getEventKind()
                , JDWPConstants.EventKind.getName(JDWPConstants.EventKind.CLASS_PREPARE)
                , JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        assertEquals("Invalid signature of prepared class,", class2prepareSignature
                , ((ParsedEvent.Event_CLASS_PREPARE)parsedEvents[0]).getSignature());
        
        //logWriter.println("parsedEvent="+parsedEvents[0].getClass().getCanonicalName());
        //logWriter.println(((ParsedEvent.Event_CLASS_PREPARE)parsedEvents[0]).getSignature());
    }
}
