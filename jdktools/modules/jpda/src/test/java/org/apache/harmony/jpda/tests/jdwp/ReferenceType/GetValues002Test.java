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
 * Created on 10.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.ClassLoader command for NON-static fields.
 */
public class GetValues002Test extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.GetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/GetValues002Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.GetValues002Debuggee";
    }

    /**
     * This test exercises ReferenceType.GetValues command for NON-static fields.
     * <BR>The test starts GetValues002Debuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Fields command and gets fieldIDs for checked fields
     * which are non static.
     * Then test performs ReferenceType.GetValues command for checked fields and checks
     * that command returns INVALID_FIELDID error.
     */
    public void testGetValues002() {
        String thisTestName = "testGetValues002";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
                "nonStaticLongField",
                "nonStaticIntField",
                "nonStaticStringField",
                "nonStaticObjectField",
                "nonStaticBooleanField",
                "nonStaticByteField",
                "nonStaticCharField",
                "nonStaticShortField",
                "nonStaticFloatField",
                "nonStaticDoubleField",
                "nonStaticArrayField",
        };

        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames);
        int checkedFieldsNumber = checkedFieldNames.length;

        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");
        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(checkedFieldsNumber);
        for (int k = 0; k < checkedFieldsNumber; k++) {
            getValuesCommand.setNextValueAsFieldID(checkedFieldIDs[k]);
        }
        
        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;

        short errorCode = getValuesReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            if ( errorCode != JDWPConstants.Error.INVALID_FIELDID ) {
                logWriter.println("## Reply packet CHECK: FAILURE: " +  thisCommandName 
                    + " returns unexpected ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                fail(thisCommandName
                        + " returned unexpected ERROR = " + errorCode 
                        + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            }
            logWriter.println("=> CHECK PASSED: Expected error (INVALID_FIELDID) is returned");
            synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            return;
        }
        logWriter.println
        ("\n## FAILURE: " + thisCommandName + " does NOT return expected error - INVALID_FIELDID");

        // next is only for extra info
        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        logWriter.println("=> CHECK for returned values...");
        byte expectedFieldTags[] = {
                JDWPConstants.Tag.LONG_TAG,
                JDWPConstants.Tag.INT_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.BOOLEAN_TAG,
                JDWPConstants.Tag.BYTE_TAG,
                JDWPConstants.Tag.CHAR_TAG,
                JDWPConstants.Tag.SHORT_TAG,
                JDWPConstants.Tag.FLOAT_TAG,
                JDWPConstants.Tag.DOUBLE_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
        };

        for (int k = 0; k < returnedValuesNumber; k++) {
            Value fieldValue = getValuesReply.getNextValueAsValue();
            byte fieldTag = fieldValue.getTag();
            logWriter.println
            ("\n=> Check for returned value for field: " + checkedFieldNames[k] + " ...");
            logWriter.println("=> Returned value tag = " + fieldTag 
                + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
            if ( fieldTag != expectedFieldTags[k] ) {
                break;
            }
            switch ( fieldTag ) {
            case JDWPConstants.Tag.INT_TAG:
                int intValue = fieldValue.getIntValue();
                logWriter.println("=> Int value = " + intValue);
                break;
            case JDWPConstants.Tag.LONG_TAG:
                long longValue = fieldValue.getLongValue();
                logWriter.println("=> Long value = " + longValue);
                break;
            case JDWPConstants.Tag.OBJECT_TAG:
                long objectIDValue = fieldValue.getLongValue();
                logWriter.println("=> ObjectID value = " + objectIDValue);
                break;
            case JDWPConstants.Tag.BOOLEAN_TAG:
                boolean booleanValue = fieldValue.getBooleanValue();
                logWriter.println("=> Boolean value = " + booleanValue);
                break;
            case JDWPConstants.Tag.BYTE_TAG:
                byte byteValue = fieldValue.getByteValue();
                logWriter.println("=> Byte value = " + byteValue);
                break;
            case JDWPConstants.Tag.CHAR_TAG:
                char charValue = fieldValue.getCharValue();
                logWriter.println("=> Char value = " + (int)charValue);
                break;
            case JDWPConstants.Tag.SHORT_TAG:
                short shortValue = fieldValue.getShortValue();
                logWriter.println("=> Short value = " + shortValue);
                break;
            case JDWPConstants.Tag.FLOAT_TAG:
                float floatValue = fieldValue.getFloatValue();
                logWriter.println("=> Float value = " + floatValue);
                break;
            case JDWPConstants.Tag.DOUBLE_TAG:
                double doubleValue = fieldValue.getDoubleValue();
                logWriter.println("=> Double value = " + doubleValue);
                break;
            }
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");

        fail(thisCommandName + " does NOT return expected error - INVALID_FIELDID");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValues002Test.class);
    }
}
