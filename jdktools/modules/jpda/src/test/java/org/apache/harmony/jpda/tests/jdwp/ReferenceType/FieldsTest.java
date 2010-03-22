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
 * Created on 18.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.Fields command.
 */
public class FieldsTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.Fields command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/FieldsDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.FieldsDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.Fields command.
     * <BR>The test starts FieldsDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Fields command and checks that returned
     * list of fields corresponds to expected list with expected attributes.
     */
    public void testFields001() {
        String thisTestName = "testFields001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        int testStatus = testStatusPassed;
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket fieldsCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.FieldsCommand);
        fieldsCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket fieldsReply = debuggeeWrapper.vmMirror.performCommand(fieldsCommand);
        fieldsCommand = null;
        checkReplyPacket(fieldsReply, thisCommandName);

        int returnedFieldsNumber = fieldsReply.getNextValueAsInt();
        logWriter.println("=> Returned fields number = " + returnedFieldsNumber);

        String fieldNames[] = {
                "staticIntField",
                "stringField",
                "objectField"
        };
        
        String fieldSignatures[] = {
                "I",
                "Ljava/lang/String;",
                "Ljava/lang/Object;"
        };
            
        int fieldModifiers[] = {
                0x8,
                0x0,
                0x0
        };

        boolean fieldFound[] = {
                false,
                false,
                false
        };
        int expectedFieldsNumber = fieldNames.length;
        int fieldSyntheticFlag = 0xf0000000;
        String failMessage = null;

        logWriter.println("=> CHECK for all expected fields...");
        for (int i = 0; i < returnedFieldsNumber; i++) {
            long returnedFieldID = fieldsReply.getNextValueAsFieldID();
            String returnedFieldName = fieldsReply.getNextValueAsString();
            String returnedFieldSignature = fieldsReply.getNextValueAsString();
            int returnedFieldModifiers = fieldsReply.getNextValueAsInt();
            logWriter.println("\n=> Field ID = " + returnedFieldID);
            logWriter.println("=> Field name = " + returnedFieldName);
            logWriter.println("=> Field signature = " + returnedFieldSignature);
            logWriter.println("=> Field modifiers = 0x" + Integer.toHexString(returnedFieldModifiers));
            if ( (returnedFieldModifiers & fieldSyntheticFlag) == fieldSyntheticFlag ) {
                continue; // do not check synthetic fields
            }
            int k = 0;
            for (; k < expectedFieldsNumber; k++) {
                if (!fieldNames[k].equals(returnedFieldName)) {
                    continue;
                }
                if (fieldFound[k]) {
                    logWriter.println("\n## FAILURE: The field is found repeatedly in the list");
                    logWriter.println("## Field name = " + returnedFieldName);
                    testStatus = testStatusFailed;
                    failMessage = "The field is found repeatedly in the list: " +
                        returnedFieldName;
                    break;
                }
                fieldFound[k] = true;
                if (!fieldSignatures[k].equals(returnedFieldSignature) ) {
                    logWriter.println("\n## FAILURE: Unexpected field signature is returned:");
                    logWriter.println("## Field name = " + returnedFieldName);
                    logWriter.println("## Expected signature = " + fieldSignatures[k]);
                    logWriter.println("## Returned signature = " + returnedFieldSignature);
                    testStatus = testStatusFailed;
                    failMessage = "Unexpected signature is returned for field: " +
                        returnedFieldName + 
                        ", expected: " + fieldSignatures[k] +
                        ", returned: " + returnedFieldSignature;
                }
                if (fieldModifiers[k] != returnedFieldModifiers) {
                    logWriter.println("\n## FAILURE: Unexpected field modifiers are returned:");
                    logWriter.println("## Field name = " + returnedFieldName);
                    logWriter.println
                    ("## Expected modifiers = 0x" + Integer.toHexString(fieldModifiers[k]));
                    logWriter.println
                    ("## Returned modifiers = 0x" + Integer.toHexString(returnedFieldModifiers));
                    testStatus = testStatusFailed;
                    failMessage = "Unexpected modifiers are returned for field: " +
                        returnedFieldName +
                        ", expected: 0x" + Integer.toHexString(fieldModifiers[k]) +
                        ", returned: 0x" + Integer.toHexString(returnedFieldModifiers);
                }
                break;
            }
            if (k == expectedFieldsNumber) {
                // returned field is not found out in the list of expected fields
                logWriter.println("\n## FAILURE: It is found out unexpected returned field:");
                logWriter.println("## Field name = " + returnedFieldName);
                logWriter.println("## Field signature = " + returnedFieldSignature);
                logWriter.println
                ("## Field modifiers = 0x" + Integer.toHexString(returnedFieldModifiers));
                testStatus = testStatusFailed;
                failMessage =
                    "Unexpected returned field: " + returnedFieldName +
                    ", signature = " + returnedFieldSignature +
                    ", modifiers = 0x" + Integer.toHexString(returnedFieldModifiers);
            }
        }

        for (int k = 0; k < expectedFieldsNumber; k++) {
            if (!fieldFound[k]) {
                logWriter.println
                ("\n## FAILURE: Expected field is NOT found out in the list of retuned fields:");
                logWriter.println("## Field name = " + fieldNames[k]);
                testStatus = testStatusFailed;
                failMessage = "Expected field is NOT found in the list of retuned fields: " +
                    fieldNames[k];
            }
        }

        if (testStatus == testStatusPassed) {
            logWriter.println
            ("=> CHECK PASSED: All expected fields are found out and have expected attributes");
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        if (testStatus == testStatusFailed) {
            fail(failMessage);
        }

        assertAllDataRead(fieldsReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FieldsTest.class);
    }
}
