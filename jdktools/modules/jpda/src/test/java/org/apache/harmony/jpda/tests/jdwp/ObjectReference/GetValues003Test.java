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
 * Created on 13.07.2005
 */

package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ObjectReference.GetValues command for Object fields with value=null.
 */
public class GetValues003Test extends JDWPSyncTestCase {

    static final String thisCommandName = "ObjectReference::GetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/GetValues003Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.GetValues003Debuggee";
    }

    /**
     * This test exercises ObjectReference.GetValues command for static fields.
     * <BR>The test starts GetValues003Debuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then for this objectID test executes ObjectReference.GetValues command for
     * fields of different referenceTypes with value=null for all fields.
     * The test expects the all returned values should be represented by expected
     * JDWP tag with null value.
     */
    public void testGetValues003() {
        String thisTestName = "testGetValues003";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
                "testedObject",
                
                "intArrayField",
                "objectArrayField",
                "objectField",
                "stringField",
                "threadField",
                "threadGroupField",
                "classField",
                "classLoaderField",
        };
        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames);
        int checkedFieldsNumber = checkedFieldNames.length;

        logWriter.println
        ("=> Send ReferenceType::GetValues command and get ObjectID to check...");

        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(checkedFieldIDs[0]);
        ReplyPacket getValuesReply =
            debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, "ReferenceType::GetValue command");

        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        assertEquals("Invalid number of values,", 1, returnedValuesNumber);

        Value checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        byte checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checked object= " + checkedObjectFieldTag
            + "(" + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");
        assertEquals("Invalid value tag for checked object,", JDWPConstants.Tag.OBJECT_TAG, checkedObjectFieldTag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(checkedObjectFieldTag));

        long checkedObjectID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned checked ObjectID = " + checkedObjectID);

        logWriter.println
        ("=> CHECK: send " + thisCommandName 
        + " for this ObjectID for fields of different referenceTypes with with null values...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.GetValuesCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        checkedCommand.setNextValueAsInt(checkedFieldsNumber-1);
        int fieldIndex = 1; // !!!
        for (; fieldIndex < checkedFieldsNumber; fieldIndex++) {
            checkedCommand.setNextValueAsFieldID(checkedFieldIDs[fieldIndex]);
        }
        ReplyPacket checkedReply =
            debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        checkReplyPacket(checkedReply, thisCommandName);

        returnedValuesNumber = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        assertEquals("Invalid number of values,", checkedFieldsNumber - 1, returnedValuesNumber);

        byte expectedFieldTags[] = {
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.STRING_TAG,
                JDWPConstants.Tag.THREAD_TAG,
                JDWPConstants.Tag.THREAD_GROUP_TAG,
                JDWPConstants.Tag.CLASS_OBJECT_TAG,
                JDWPConstants.Tag.CLASS_LOADER_TAG,
        };
        logWriter.println("=> CHECK for returned values...");
        for (int i = 0; i < returnedValuesNumber; i++) {
            Value fieldValue = checkedReply.getNextValueAsValue();
            byte fieldTag = fieldValue.getTag();
            logWriter.println
            ("\n=> Check for returned value for field: " + checkedFieldNames[i+1] + " ...");
            logWriter.println("=> Returned value tag = " + fieldTag 
                + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
            if ( (fieldTag != expectedFieldTags[i]) && (fieldTag != JDWPConstants.Tag.OBJECT_TAG) ) {
                logWriter.println("\n## FAILURE:  Unexpected value tag is returned");
                logWriter.println("## Expected value tag = " + expectedFieldTags[i]
                + "(" + JDWPConstants.Tag.getName(expectedFieldTags[i]) + ")"
                + " or = " + JDWPConstants.Tag.OBJECT_TAG
                + "(" + JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG) + ")");
                fail("Unexpected value tag is returned");
            }
            long objectIDValue = fieldValue.getLongValue();
            logWriter.println("=> ObjectId value = " + objectIDValue);
            assertEquals("Invalid objectID value is returned,", 0, objectIDValue);
        }

        assertAllDataRead(checkedReply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": OK.");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValues003Test.class);
    }
}
