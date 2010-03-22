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
 * JDWP Unit test for ReferenceType.GetValues command.
 */
public class GetValuesTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.GetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/GetValuesDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.GetValuesDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.GetValues command.
     * <BR>The test starts GetValuesDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Fields command and gets fieldIDs for checked fields.
     * <BR>Then test performs ReferenceType.GetValues command for checked fields and checks
     * that returned field values are expected.
     */
    public void testGetValues001() {
        String thisTestName = "testGetValues001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
                "staticIntField",
                "staticLongField",
                "getValuesDebuggeeField",
                "staticStringField",
                "staticArrayField",
        };

        String checkedFieldSignatures[] = {
                "I",
                "J",
                "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/GetValuesDebuggee;",
                "Ljava/lang/String;",
                "[I",
        };

        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames, checkedFieldSignatures, null);
        int checkedFieldsNumber = checkedFieldNames.length;

        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");
        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(checkedFieldsNumber);
        for (int k=0; k < checkedFieldsNumber; k++) {
            getValuesCommand.setNextValueAsFieldID(checkedFieldIDs[k]);
        }
        
        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, thisCommandName);
        
        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        assertEquals("Invalid number of values,", checkedFieldsNumber, returnedValuesNumber);

        logWriter.println("=> CHECK for returned values...");
        byte expectedFieldTags[] = {
                JDWPConstants.Tag.INT_TAG,
                JDWPConstants.Tag.LONG_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.STRING_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
        };
        for (int k=0; k < checkedFieldsNumber; k++) {
            Value fieldValue = getValuesReply.getNextValueAsValue();
            byte fieldTag = fieldValue.getTag();
            logWriter.println
            ("\n=> Check for returned value for field: " + checkedFieldNames[k] + " ...");
            logWriter.println("=> Returned value tag = " + fieldTag 
                + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
            
            assertEquals("Invalid value tag is returned,", expectedFieldTags[k], fieldTag
                    , JDWPConstants.Tag.getName(expectedFieldTags[k])
                    , JDWPConstants.Tag.getName(fieldTag));

            switch ( fieldTag ) {
            case JDWPConstants.Tag.INT_TAG:
                int intValue = fieldValue.getIntValue();
                logWriter.println("=> Int value = " + intValue);
                // here expected value = 99 (staticIntField)
                int expectedIntValue = 99;
                assertEquals("Invalid int value,", expectedIntValue, intValue);
                break;
            case JDWPConstants.Tag.LONG_TAG:
                long longValue = fieldValue.getLongValue();
                logWriter.println("=> Long value = " + longValue);
                // here expected value = 2147483647 (staticLongField)
                long expectedLongValue = 2147483647;
                assertEquals("Invalid Long value,", expectedLongValue, longValue);
                break;
            case JDWPConstants.Tag.OBJECT_TAG:
                long objectIdValue = fieldValue.getLongValue();
                logWriter.println("=> ObjectID value = " + objectIdValue);
                break;
            case JDWPConstants.Tag.STRING_TAG:
                long stringIDValue = fieldValue.getLongValue();
                logWriter.println("=> StringID value = " + stringIDValue);
                break;
            case JDWPConstants.Tag.ARRAY_TAG:
                long arrayIDValue = fieldValue.getLongValue();
                logWriter.println("=> ArrayID value = " + arrayIDValue);
                break;
            }
        }

        assertAllDataRead(getValuesReply);

        logWriter.println
        ("=> CHECK PASSED: All expected field values are got and have expected attributes");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValuesTest.class);
    }
}
