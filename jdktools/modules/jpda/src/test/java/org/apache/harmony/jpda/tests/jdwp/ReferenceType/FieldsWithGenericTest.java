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
 * JDWP Unit test for ReferenceType.FieldsWithGeneric command.
 */
public class FieldsWithGenericTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.FieldsWithGeneric command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/FieldsWithGenericDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.FieldsWithGenericDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.FieldsWithGeneric command.
     * <BR>The test starts FieldsWithGenericDebuggee class,
     * requests referenceTypeId for this class by VirtualMachine.ClassesBySignature
     * command, then performs ReferenceType.FieldsWithGeneric command 
     * and checks that returned list of fields corresponds to expected list
     * with expected attributes.
     */
    public void testFieldsWithGeneric001() {
        String thisTestName = "testFieldsWithGeneric001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        int testStatus = testStatusPassed;
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket fieldsWithGenericCommand = new CommandPacket(
            JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
            JDWPCommands.ReferenceTypeCommandSet.FieldsWithGenericCommand);
        fieldsWithGenericCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket fieldsWithGenericReply = debuggeeWrapper.vmMirror.performCommand(fieldsWithGenericCommand);
        fieldsWithGenericCommand = null;
        checkReplyPacket(fieldsWithGenericReply, thisCommandName);

        int returnedFieldsNumber = fieldsWithGenericReply.getNextValueAsInt();
        logWriter.println("=> Returned fields number = " + returnedFieldsNumber);

        String fieldNames[] = {
                "staticLongField",
                "stringArrayField",
                "objectArrayField",
                "classObjectField"
        };
        
        String fieldSignatures[] = {
                "J",
                "[Ljava/lang/String;",
                "[Ljava/lang/Object;",
                "Ljava/lang/Class;"
        };
            
        String fieldGenericSignatures[] = {
                "",
                "",
                "",
                ""
        };
            
        int fieldModifiers[] = {
                0x8, // ACC_STATIC flag
                0x0,
                0x0,
                0x0
        };

        boolean fieldFound[] = {
                false,
                false,
                false,
                false
        };
        int expectedFieldsNumber = fieldNames.length;
        int fieldSyntheticFlag = 0xf0000000;
        String message = null;

        logWriter.println("=> CHECK for all expected fields...");
        for (int i = 0; i < returnedFieldsNumber; i++) {
            long returnedFieldID = fieldsWithGenericReply.getNextValueAsFieldID();
            String returnedFieldName = fieldsWithGenericReply.getNextValueAsString();
            String returnedFieldSignature = fieldsWithGenericReply.getNextValueAsString();
            String returnedGenericSignature = fieldsWithGenericReply.getNextValueAsString();
            int returnedFieldModifiers = fieldsWithGenericReply.getNextValueAsInt();
            logWriter.println("\n=> Field ID = " + returnedFieldID);
            logWriter.println("=> Field name = " + returnedFieldName);
            logWriter.println("=> Field signature = \"" + returnedFieldSignature + "\"");
            logWriter.println("=> Field generic signature = \"" + returnedGenericSignature + "\"");
            logWriter.println("=> Field modifiers = 0x" + Integer.toHexString(returnedFieldModifiers));
            if ( (returnedFieldModifiers & fieldSyntheticFlag) == fieldSyntheticFlag ) {
                continue; // do not check synthetic fields
            }
            int k = 0;
            for (; k < expectedFieldsNumber; k++) {
                if ( ! fieldNames[k].equals(returnedFieldName)) {
                    continue;
                }
                if ( fieldFound[k] ) {
                    logWriter.println("\n## FAILURE: The field is found out repeatedly in the list");
                    logWriter.println("## Field name = " + returnedFieldName);
                    testStatus = testStatusFailed;
                    message = "The field is found repeatedly in the list: " +
                        returnedFieldName;
                    break;
                }
                fieldFound[k] = true;
                if ( ! fieldSignatures[k].equals(returnedFieldSignature) ) {
                    logWriter.println("\n## FAILURE: Unexpected field signature is returned:");
                    logWriter.println("## Field name = " + returnedFieldName);
                    logWriter.println("## Expected signature = " + fieldSignatures[k]);
                    logWriter.println("## Returned signature = " + returnedFieldSignature);
                    testStatus = testStatusFailed;
                    message = "Unexpected signature is returned for field: " + returnedFieldName +
                        ", expected: " + fieldSignatures[k] +
                        ", returned: " + returnedFieldSignature;
                }
                if ( ! fieldGenericSignatures[k].equals(returnedGenericSignature) ) {
                    logWriter.println("\n## FAILURE: Unexpected field generic signature is returned:");
                    logWriter.println("## Field name = " + returnedFieldName);
                    logWriter.println
                    ("## Expected generic signature = \"" + fieldGenericSignatures[k] + "\"");
                    logWriter.println
                    ("## Returned generic signature = \"" + returnedGenericSignature + "\"");
                    testStatus = testStatusFailed;
                    message = "Unexpected generic signature is returned for filed: " +
                        returnedFieldName +
                        ", expected: \"" + fieldGenericSignatures[k] + "\"" +
                        ", returned: \"" + returnedGenericSignature + "\"";
                }
                if ( fieldModifiers[k] != returnedFieldModifiers ) {
                    logWriter.println("\n## FAILURE: Unexpected field modifiers are returned:");
                    logWriter.println("## Field name = " + returnedFieldName);
                    logWriter.println
                    ("## Expected modifiers = 0x" + Integer.toHexString(fieldModifiers[k]));
                    logWriter.println
                    ("## Returned modifiers = 0x" + Integer.toHexString(returnedFieldModifiers));
                    testStatus = testStatusFailed;
                    message = "Unexpected modifiers are returned for field: " + returnedFieldName +
                        ", expected: 0x" + Integer.toHexString(fieldModifiers[k]) +
                        ", returned: 0x" + Integer.toHexString(returnedFieldModifiers);
                }
                break;
            }
            if ( k == expectedFieldsNumber ) {
                // returned field is not found out in the list of expected fields
                logWriter.println("\n## FAILURE: It is found out unexpected returned field:");
                logWriter.println("## Field name = " + returnedFieldName);
                logWriter.println("## Field signature = " + returnedFieldSignature);
                logWriter.println("## Field generic signature = \"" + returnedGenericSignature + "\"");
                logWriter.println
                ("## Field modifiers = 0x" + Integer.toHexString(returnedFieldModifiers));
                testStatus = testStatusFailed;
                message =
                    "Unexpected returned field: " +
                    returnedFieldName +
                    ", signature = " + returnedFieldSignature +
                    ", generic signature = \"" + returnedGenericSignature + "\"" +
                    ", modifiers = 0x" + Integer.toHexString(returnedFieldModifiers);
            }
        }

        for (int k = 0; k < expectedFieldsNumber; k++) {
            if (!fieldFound[k]) {
                logWriter.println
                ("\n## FAILURE: Expected field is NOT found out in the list of retuned fields:");
                logWriter.println("## Field name = " + fieldNames[k]);
                testStatus = testStatusFailed;
                message =
                    "Expected field is NOT found in the list of retuned fields: " +
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
            fail(message);
        }
        
        assertAllDataRead(fieldsWithGenericReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FieldsWithGenericTest.class);
    }
}
