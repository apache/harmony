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
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for FIELD_MODIFICATION event.
 */
public class FieldModificationTest extends JDWPEventTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FieldModificationTest.class);
    }
    
    protected String getDebuggeeClassName() {
        return FieldDebuggee.class.getName();
    }
    
    /**
     * This testcase is for FIELD_MODIFICATION event.
     * <BR>It runs FieldDebuggee that modifies the value of its own field 
     * and verify that requested FIELD_MODIFICATION event occurs.
     */
    public void testFieldModifyEvent() {

        logWriter.println("FieldModificationTest started");
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canWatchFieldModification");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canWatchFieldModification;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canWatchFieldModification");
            return;
        }
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String classSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/FieldDebuggee;";
        ReplyPacket reply = debuggeeWrapper.vmMirror.setFieldModification(classSignature, JDWPConstants.TypeTag.CLASS, "testIntField");
        checkReplyPacket(reply, "Set FIELD_MODIFICATION event");
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        EventPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(event);

        // assert that event is the expected one
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.FIELD_MODIFICATION,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.FIELD_MODIFICATION),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));

        TaggedObject modifiedField =((ParsedEvent.Event_FIELD_MODIFICATION)parsedEvents[0]).getObject();

        // assert that exception class is the expected one
        long typeID = getObjectReferenceType(modifiedField.objectID);
        String returnedExceptionSignature = getClassSignature(typeID);
        assertString("Invalid class signature,",
                classSignature, returnedExceptionSignature);

        logWriter.println("FieldModificationTest done");
    }
}
