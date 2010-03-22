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

import org.apache.harmony.jpda.tests.framework.TestErrorException;
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
    
    /*
     * This testcase is for CLASS_PREPARE event.
     * <BR>It runs ClassPrepareDebuggee to load Class2Prepare class
     * and verify that requested CLASS_PREPARE event occurs in case of SourceNameMatch.
     * Class2Prepare doesn't contain source name field in SourceDebugExtension attribute.
     * expectedSourceNamePattern is used to assign the source name's pattern
     */
    public void testClassPrepareEventWithoutSourceDebugExtension(String expectedSourceNamePattern){
        String expectedClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/Class2Prepare;"; 
        logWriter.println("==> testClassPrepareEventForSourceNameMatch started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Set ClassPrepare Event in case of SourceNameMatch
        ReplyPacket reply = debuggeeWrapper.vmMirror.setClassPreparedForSourceNameMatch(expectedSourceNamePattern);
        checkReplyPacket(reply, "Set CLASS_PREPARE event");
        
        // start loading SourceDebugExtensionMockClass class
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        CommandPacket receiveEvent = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(receiveEvent);
        
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,", JDWPConstants.EventKind.CLASS_PREPARE, parsedEvents[0].getEventKind()
                , JDWPConstants.EventKind.getName(JDWPConstants.EventKind.CLASS_PREPARE)
                , JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        assertEquals("Invalid signature of prepared class,", expectedClassSignature, ((ParsedEvent.Event_CLASS_PREPARE)parsedEvents[0]).getSignature());
    }
    
    /**
     * Test ClassPrepareEvent without SourceDebugExtension attribute
     * matching exactly.
     */
    public void testClassPrepareEventWithoutSourceDebugExtension001(){
        String expectedSourceNamePattern = "Class2Prepare.java";
        testClassPrepareEventWithoutSourceDebugExtension(expectedSourceNamePattern);
        
    }
    
    /**
     * Test ClassPrepareEvent without SourceDebugExtension attribute
     * matching the former part.
     */
    public void testClassPrepareEventWithoutSourceDebugExtension002(){
        String expectedSourceNamePattern = "*Class2Prepare.java";
        testClassPrepareEventWithoutSourceDebugExtension(expectedSourceNamePattern);
    }
    
    /**
     * Test ClassPrepareEvent without SourceDebugExtension attribute
     * matching the latter part.
     */
    public void testClassPrepareEventWithoutSourceDebugExtension003(){
        String expectedSourceNamePattern = "Class2Prepare.*";
        testClassPrepareEventWithoutSourceDebugExtension(expectedSourceNamePattern);
    }
    
    /*
     * This testcase is for CLASS_PREPARE event.
     * <BR>It runs ClassPrepareDebuggee to load SourceDebugExtensionMockClass 
     * and verify that requested CLASS_PREPARE event occurs in case of SourceNameMatch.
     * SourceDebugExtensionMockClass contains source name field in SourceDebugExtension attribute.
     * expectedSourceNamePattern is used to assign the source name's pattern
     */
    private void testClassPrepareEventWithSourceDebugExtension(String expectedSourceNamePattern){
        String expectedClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/SourceDebugExtensionMockClass;";

        logWriter.println("==> testClassPrepareEventForSourceNameMatch started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Set ClassPrepare Event in case of SourceNameMatch
        ReplyPacket reply = debuggeeWrapper.vmMirror.setClassPreparedForSourceNameMatch(expectedSourceNamePattern);
        checkReplyPacket(reply, "Set CLASS_PREPARE event");

        // start loading SourceDebugExtensionMockClass class
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        CommandPacket receiveEvent = null;
        try {
            receiveEvent = debuggeeWrapper.vmMirror.receiveEvent();
        } catch (TestErrorException e) {
            printErrorAndFail("There is no event received.");
        }
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(receiveEvent);
        
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,", JDWPConstants.EventKind.CLASS_PREPARE, parsedEvents[0].getEventKind()
                , JDWPConstants.EventKind.getName(JDWPConstants.EventKind.CLASS_PREPARE)
                , JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        assertEquals("Invalid signature of prepared class,", expectedClassSignature, ((ParsedEvent.Event_CLASS_PREPARE)parsedEvents[0]).getSignature());
    }

    /**
     * Test ClassPrepareEvent with SourceDebugExtension attribute.
     * matching exactly.
     */
    public void testClassPrepareEventWithSourceDebugExtension001(){
        String expectedSourceNamePattern = "helloworld_jsp.java";
        testClassPrepareEventWithSourceDebugExtension(expectedSourceNamePattern);
    }

    /**
     * Test ClassPrepareEvent with SourceDebugExtension attribute
     * matching the former part.
     */
    public void testClassPrepareEventWithSourceDebugExtension002(){
        String expectedSourceNamePattern = "*helloworld_jsp.java";
        testClassPrepareEventWithSourceDebugExtension(expectedSourceNamePattern);
    }

    /**
     * Test ClassPrepareEvent with SourceDebugExtension attribute.
     * matching the latter part.
     */
    public void testClassPrepareEventWithSourceDebugExtension003(){
        String expectedSourceNamePattern = "helloworld*";
        testClassPrepareEventWithSourceDebugExtension(expectedSourceNamePattern);
    }

}
