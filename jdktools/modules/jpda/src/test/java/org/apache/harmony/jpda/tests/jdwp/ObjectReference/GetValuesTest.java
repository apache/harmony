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
 * Created on 28.02.2005
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
 * JDWP Unit test for ObjectReference.GetValues command .
 */
public class GetValuesTest extends JDWPSyncTestCase {

    static final String thisCommandName = "ObjectReference::GetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/GetValuesDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.GetValuesDebuggee";
    }

    /**
     * This test exercises ObjectReference.GetValues command.
     * <BR>The test starts GetValuesDebuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then for this objectID test executes ObjectReference::GetValues command for special
     * set of fieldIDs and checks that command returns expected jdwpTags for all checked
     * fields and expected values for primitive fields.
     */
    public void testGetValues001() {
        String thisTestName = "testGetValues001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
                "getValuesDebuggeeObject",
                
                "intField",
                "longField",
                "objectField",
                "stringArrayField",
                "objectArrayField",
                "threadField",
                "threadGroupField",
                "classField",
                "classLoaderField",
                "stringField",
        };
        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames);
        int checkedFieldsNumber = checkedFieldNames.length;

        logWriter.println
        ("=> Send ReferenceType::GetValues command and and get ObjectID to check...");

        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(checkedFieldIDs[0]);

        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, "ReferenceType::GetValues command");

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
        logWriter.println("=> CHECK: send " + thisCommandName + " for this ObjectID and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.GetValuesCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        checkedCommand.setNextValueAsInt(checkedFieldsNumber-1);
        int fieldIndex = 1; // !!!
        for (; fieldIndex < checkedFieldsNumber; fieldIndex++) {
            checkedCommand.setNextValueAsFieldID(checkedFieldIDs[fieldIndex]);
        }

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);

        returnedValuesNumber = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        assertEquals("Invalid number of values,", checkedFieldsNumber - 1, returnedValuesNumber);

        byte expectedFieldTags[] = {
                0, // dummy
                JDWPConstants.Tag.INT_TAG,
                JDWPConstants.Tag.LONG_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.THREAD_TAG,
                JDWPConstants.Tag.THREAD_GROUP_TAG,
                JDWPConstants.Tag.CLASS_OBJECT_TAG,
                JDWPConstants.Tag.CLASS_LOADER_TAG,
                JDWPConstants.Tag.STRING_TAG,
        };

        logWriter.println("=> CHECK for returned values...");
        fieldIndex = 1; // !!!
        for (; fieldIndex < checkedFieldsNumber; fieldIndex++) {
            Value fieldValue = checkedReply.getNextValueAsValue();
            byte fieldTag = fieldValue.getTag();
            logWriter.println
            ("\n=> Check for returned value for field: " + checkedFieldNames[fieldIndex] + " ...");
            logWriter.println("=> Returned value tag = " + fieldTag 
                + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");

            assertEquals("Invalid value tag is returned,", expectedFieldTags[fieldIndex], fieldTag
                    , JDWPConstants.Tag.getName(expectedFieldTags[fieldIndex])
                    , JDWPConstants.Tag.getName(fieldTag));

            switch ( fieldTag ) {
            case JDWPConstants.Tag.INT_TAG:
                int intValue = fieldValue.getIntValue();
                logWriter.println("=> Returned value = " + intValue);
                // here expected value = 9999 (staticIntField)
                int expectedIntValue = 9999;
                assertEquals("Invalid int value,", expectedIntValue, intValue);
                break;
            case JDWPConstants.Tag.LONG_TAG:
                long longValue = fieldValue.getLongValue();
                logWriter.println("=> Returned value = " + longValue);
                // here expected value = 999999 (staticLongField)
                long expectedLongValue = 999999;
                assertEquals("Invalid long value,", expectedLongValue, longValue);
                break;
            case JDWPConstants.Tag.STRING_TAG:
            case JDWPConstants.Tag.OBJECT_TAG:
            case JDWPConstants.Tag.ARRAY_TAG:
            case JDWPConstants.Tag.THREAD_TAG:
            case JDWPConstants.Tag.THREAD_GROUP_TAG:
            case JDWPConstants.Tag.CLASS_OBJECT_TAG:
            case JDWPConstants.Tag.CLASS_LOADER_TAG:
                long objectIDValue = fieldValue.getLongValue();
                logWriter.println("=> ObjectId value = " + objectIDValue);
                break;
            }
        }

        assertAllDataRead(checkedReply);

        logWriter.println
        ("=> CHECK PASSED: All expected field values are got and have expected attributes");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValuesTest.class);
    }
}
