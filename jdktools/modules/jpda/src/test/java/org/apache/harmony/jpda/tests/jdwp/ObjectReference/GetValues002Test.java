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
 * Created on 11.03.2005
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
 * JDWP Unit test for ObjectReference.GetValues command for static fields.
 */
public class GetValues002Test extends JDWPSyncTestCase {

    static final String thisCommandName = "ObjectReference::GetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/GetValues002Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.GetValues002Debuggee";
    }

    /**
     * This test exercises ObjectReference.GetValues command for static fields.
     * <BR>The test starts GetValues002Debuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then for this objectID test executes ObjectReference.GetValues command for special
     * set of fieldIDs and checks that command returns expected jdwpTags for all checked
     * fields and expected values for primitive fields.
     */
    public void testGetValues002() {
        String thisTestName = "testGetValues002";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
                "getValues002DebuggeeField",
                
                "staticLongField",
                "staticIntField",
                "staticStringField",
                "staticObjectField",
                "staticBooleanField",
                "staticByteField",
                "staticCharField",
                "staticShortField",
                "staticFloatField",
                "staticDoubleField",
                "staticArrayField",
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
                JDWPConstants.Tag.LONG_TAG,
                JDWPConstants.Tag.INT_TAG,
                JDWPConstants.Tag.STRING_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.BOOLEAN_TAG,
                JDWPConstants.Tag.BYTE_TAG,
                JDWPConstants.Tag.CHAR_TAG,
                JDWPConstants.Tag.SHORT_TAG,
                JDWPConstants.Tag.FLOAT_TAG,
                JDWPConstants.Tag.DOUBLE_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
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
                logWriter.println("=> Int value = " + intValue);
                int expectedIntValue = 99;
                assertEquals("Invalid int value,", expectedIntValue, intValue);
                break;
            case JDWPConstants.Tag.LONG_TAG:
                long longValue = fieldValue.getLongValue();
                logWriter.println("=> Long value = " + longValue);
                long expectedLongValue = 2147483647;
                assertEquals("Invalid long value,", expectedLongValue, longValue);
                break;
            case JDWPConstants.Tag.STRING_TAG:
                long stringIDValue = fieldValue.getLongValue();
                logWriter.println("=> StringID value = " + stringIDValue);
                break;
            case JDWPConstants.Tag.OBJECT_TAG:
                long objectIDValue = fieldValue.getLongValue();
                logWriter.println("=> ObjectID value = " + objectIDValue);
                break;
            case JDWPConstants.Tag.BOOLEAN_TAG:
                boolean booleanValue = fieldValue.getBooleanValue();
                logWriter.println("=> Boolean value = " + booleanValue);
                boolean expectedBooleanValue = true;
                assertEquals("Invalid boolean value,", expectedBooleanValue, booleanValue);
                break;
            case JDWPConstants.Tag.BYTE_TAG:
                byte byteValue = fieldValue.getByteValue();
                logWriter.println("=> Byte value = " + byteValue);
                byte expectedByteValue = 1;
                assertEquals("Invalid byte value,", expectedByteValue, byteValue);
                break;
            case JDWPConstants.Tag.CHAR_TAG:
                char charValue = fieldValue.getCharValue();
                logWriter.println("=> Char value = " + (int)charValue);
                char expectedCharValue = 97;
                assertEquals("Invalid char value,", expectedCharValue, charValue);
                break;
            case JDWPConstants.Tag.SHORT_TAG:
                short shortValue = fieldValue.getShortValue();
                logWriter.println("=> Short value = " + shortValue);
                short expectedShortValue = 2;
                assertEquals("Invalid short value,", expectedShortValue, shortValue);
                break;
            case JDWPConstants.Tag.FLOAT_TAG:
                float floatValue = fieldValue.getFloatValue();
                logWriter.println("=> Float value = " + floatValue);
                float expectedFloatValue = 2;
                assertEquals("Invalid float value,", expectedFloatValue, floatValue, 0);
                break;
            case JDWPConstants.Tag.DOUBLE_TAG:
                double doubleValue = fieldValue.getDoubleValue();
                logWriter.println("=> Double value = " + doubleValue);
                double expectedDoubleValue = 3.1;
                assertEquals("Invalid double value,", expectedDoubleValue, doubleValue, 0);
                break;
            case JDWPConstants.Tag.ARRAY_TAG:
                long arrayIDValue = fieldValue.getLongValue();
                logWriter.println("=> ArrayID value = " + arrayIDValue);
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
        junit.textui.TestRunner.run(GetValues002Test.class);
    }
}
