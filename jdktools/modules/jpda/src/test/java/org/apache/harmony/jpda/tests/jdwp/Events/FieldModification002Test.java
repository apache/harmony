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
 * @author Ruslan A. Scherbakov
 */

/**
 * Created on 14.06.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.EventPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for FIELD_MODIFICATION event.
 */
public class FieldModification002Test extends JDWPEventTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FieldModification002Test.class);
    }
    
    protected String getDebuggeeClassName() {
        return FieldModification002Debuggee.class.getName();
    }

    /**
     * This testcase is for FIELD_MODIFICATION event.
     * <BR>It FieldModification002Debuggee that modifies the value of its own fields 
     * and verifies that requested FIELD_MODIFICATION events occur and
     * correct type tag is returned for each event.
     */
    public void testFieldModifyEvent() {

        logWriter.println("FieldModification002Test started");
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canWatchFieldModification");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canWatchFieldModification;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canWatchFieldModification");
            return;
        }
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        String classSignature = "L" + getDebuggeeClassName().replace('.', '/') + ";";

        hookFieldModification(classSignature, "testBoolField", JDWPConstants.Tag.BOOLEAN_TAG);
        hookFieldModification(classSignature, "testByteField", JDWPConstants.Tag.BYTE_TAG);
        hookFieldModification(classSignature, "testCharField", JDWPConstants.Tag.CHAR_TAG);
        hookFieldModification(classSignature, "testShortField", JDWPConstants.Tag.SHORT_TAG);
        hookFieldModification(classSignature, "testIntField", JDWPConstants.Tag.INT_TAG);
        hookFieldModification(classSignature, "testLongField", JDWPConstants.Tag.LONG_TAG);
        hookFieldModification(classSignature, "testFloatField", JDWPConstants.Tag.FLOAT_TAG);
        hookFieldModification(classSignature, "testDoubleField", JDWPConstants.Tag.DOUBLE_TAG);
        hookFieldModification(classSignature, "testObjectField", JDWPConstants.Tag.OBJECT_TAG);
        hookFieldModification(classSignature, "testThreadField", JDWPConstants.Tag.THREAD_TAG);
        hookFieldModification(classSignature, "testThreadGroupField", JDWPConstants.Tag.THREAD_GROUP_TAG);
        hookFieldModification(classSignature, "testClassField", JDWPConstants.Tag.CLASS_OBJECT_TAG);
        hookFieldModification(classSignature, "testClassLoaderField", JDWPConstants.Tag.CLASS_LOADER_TAG);
        hookFieldModification(classSignature, "testStringField", JDWPConstants.Tag.STRING_TAG);
        hookFieldModification(classSignature, "testIntArrayField", JDWPConstants.Tag.ARRAY_TAG);
        hookFieldModification(classSignature, "testStringArrayField", JDWPConstants.Tag.ARRAY_TAG);
        hookFieldModification(classSignature, "testObjectArrayField", JDWPConstants.Tag.ARRAY_TAG);

        logWriter.println("FieldModification002Test done");
    }

    /**
     * Sets FIELD_MODIFICATION breakpoint,
     * synchrinizes debuggee,
     * expects field notification event,
     * verifies new value assigned to the field
     * and clears set breakpoint.
     * 
     * @param classSignature signature of class containing the given field
     * @param fieldName the name of field to break on modification
     * @param expectedTag expected type tag of new values assigned to the field
     */
    public void hookFieldModification(String classSignature, String fieldName, byte expectedTag) {

        // set breakpoint
        logWriter.println("Set hook for: " + fieldName);
        ReplyPacket reply = debuggeeWrapper.vmMirror.setFieldModification(classSignature, JDWPConstants.TypeTag.CLASS, fieldName);
        checkReplyPacket(reply, "Set FIELD_MODIFICATION event");
        int requestID = reply.getNextValueAsInt();
        assertAllDataRead(reply);

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

        Value value =
            ((ParsedEvent.Event_FIELD_MODIFICATION)parsedEvents[0]).getValueToBe();
        byte tag = value.getTag();
        assertEquals("Invalid value tag,",
                expectedTag,
                tag,
                JDWPConstants.Tag.getName(expectedTag),
                JDWPConstants.Tag.getName(tag));

        TaggedObject modifiedField =
            ((ParsedEvent.Event_FIELD_MODIFICATION)parsedEvents[0]).getObject();

        // assert that exception class is the expected one
        long typeID = getObjectReferenceType(modifiedField.objectID);
        String returnedExceptionSignature = getClassSignature(typeID);
        assertString("Invalid class signature,",
                classSignature, returnedExceptionSignature);

        logWriter.println("Field: " + fieldName +
                ", tag of new value: " + value +
                ", tag: " + (char)tag+ " - OK");

        // clear breakpoint
        clearEvent(JDWPConstants.EventKind.FIELD_MODIFICATION, requestID, false);
        
        // and resume target VM
        resumeDebuggee();
    }
}
