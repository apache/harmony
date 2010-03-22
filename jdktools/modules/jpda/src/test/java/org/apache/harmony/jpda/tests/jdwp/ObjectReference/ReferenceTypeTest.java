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
 * Created on 25.02.2005
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
 * JDWP Unit test for ObjectReference.ReferenceType command.
 */
public class ReferenceTypeTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ObjectReference.ReferenceType command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/ReferenceTypeDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.ReferenceTypeDebuggee";
    }

    /**
     * This test exercises ObjectReference.ReferenceType command.
     * <BR>The test starts ReferenceTypeDebuggee class, gets objectIDs
     * as values of some static fields of this class and then for each objectID
     * executes ObjectReference.ReferenceType command and checks
     * that command returns expected refTypeTags and returned Reference Types have
     * expected signatures.
     */
    public void testReferenceType001() {
        String thisTestName = "testReferenceType001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        int testStatus = testStatusPassed;
        String failMessage = "";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket classesBySignatureCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        classesBySignatureCommand.setNextValueAsString(debuggeeSignature);

        ReplyPacket classesBySignatureReply = debuggeeWrapper.vmMirror.performCommand(classesBySignatureCommand);
        classesBySignatureCommand = null;

        checkReplyPacket(classesBySignatureReply, "VirtualMachine::ClassesBySignature command");

        classesBySignatureReply.getNextValueAsInt();
        // Number of returned reference types - is NOt used here

        classesBySignatureReply.getNextValueAsByte();
        // refTypeTag of class - is NOt used here

        long refTypeID = classesBySignatureReply.getNextValueAsReferenceTypeID();
        classesBySignatureReply = null;

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String neededFieldNames[] = {
                "class_ReferenceType001Object",
                "referenceTypeDebuggeeArray",
                "stringArrayField",
                "stringField",
        };

        long neededFieldIDs[] = checkFields(refTypeID, neededFieldNames);
        int neededFieldsNumber = neededFieldNames.length;

        logWriter.println
        ("=> Send ReferenceType::GetValues command and and get ObjectIDs to check...");
        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(neededFieldsNumber);
        for (int k=0; k < neededFieldsNumber; k++) {
            getValuesCommand.setNextValueAsFieldID(neededFieldIDs[k]);
        }
        
        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, "ReferenceType::GetValues command");

        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        if ( returnedValuesNumber != neededFieldsNumber ) {
            logWriter.println
            ("\n## FAILURE: ReferenceType::GetValues command returned unexpected number of values:");
            logWriter.println("## Expected number = " + neededFieldsNumber);
            logWriter.println("## Returned number = " + returnedValuesNumber);
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            assertTrue(false);
        }
        logWriter.println("=> Check for returned objectIDs...");
        byte expectedFieldTags[] = {
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.STRING_TAG,
        };
        String expectedSignatures[] = {
                "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/Class_ReferenceType001;",
                "[Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/ReferenceTypeDebuggee;",
                "[Ljava/lang/String;",
                "Ljava/lang/String;",
        };
        byte expectedRefTypeTags[] = {
                JDWPConstants.TypeTag.CLASS,
                JDWPConstants.TypeTag.ARRAY,
                JDWPConstants.TypeTag.ARRAY,
                JDWPConstants.TypeTag.CLASS,
        };
        ReplyPacket checkedReply = null;
        for (int k=0; k < neededFieldsNumber; k++) {
            Value fieldValue = getValuesReply.getNextValueAsValue();
            byte fieldTag = fieldValue.getTag();
            logWriter.println("\n=> Returned value for field: " + neededFieldNames[k] + ":");
            logWriter.println("=> Value tag = " + fieldTag + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
            if ( fieldTag != expectedFieldTags[k] ) {
                logWriter.println
                ("\n## FAILURE: ReferenceType::GetValues command returned unexpected value tag ");
                logWriter.println
                ("## Expected tag = " + expectedFieldTags[k]);
                assertTrue(false);
            }
            long objectIdValue = fieldValue.getLongValue();
            logWriter.println("=> objectId = " + objectIdValue);
            logWriter.println("=> CHECK: send " + thisCommandName + " for this objectID and check reply...");
            CommandPacket checkedCommand = new CommandPacket(
                    JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                    JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
            checkedCommand.setNextValueAsObjectID(objectIdValue);
            
            checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
            checkedCommand = null;
            checkReplyPacket(checkedReply, thisCommandName);

            byte refTypeTag = checkedReply.getNextValueAsByte();
            long objectRefTypeID = checkedReply.getNextValueAsReferenceTypeID();
            logWriter.println("=> Returned refTypeTag = " + refTypeTag
                    + "(" + JDWPConstants.TypeTag.getName(refTypeTag) + ")");
            if ( refTypeTag != expectedRefTypeTags[k] ) {
                logWriter.println
                ("\n## FAILURE: " + thisCommandName + " returned unexpected refTypeTag:");
                logWriter.println("## Expected refTypeTag = " + expectedRefTypeTags[k]
                    + "(" + JDWPConstants.TypeTag.getName(expectedRefTypeTags[k]) + ")");
                testStatus = testStatusFailed;
                failMessage = failMessage +
                    thisCommandName + " returned unexpected refTypeTag: " +
                    refTypeTag + "(" + JDWPConstants.TypeTag.getName(refTypeTag) + ")" +
                    ", Expected: " + expectedRefTypeTags[k] +
                    "(" + JDWPConstants.TypeTag.getName(expectedRefTypeTags[k]) + ")\n";
            }
            logWriter.println("=> ReferenceTypeID for this objectID = " + objectRefTypeID);
            logWriter.println("=> Get signature for returned ReferenceTypeID...");
            CommandPacket signatureCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
            signatureCommand.setNextValueAsReferenceTypeID(objectRefTypeID);
            
            ReplyPacket signatureReply = debuggeeWrapper.vmMirror.performCommand(signatureCommand);
            signatureCommand = null;
            checkReplyPacket(signatureReply, "ReferenceType::Signature command");

            String returnedSignature = signatureReply.getNextValueAsString();
            logWriter.println("=> Returned Signature = " + returnedSignature);

            if ( ! expectedSignatures[k].equals(returnedSignature) ) {
                logWriter.println("\n## FAILURE: Unexpected signature is returned:");
                logWriter.println("## Expected signature = " + expectedSignatures[k]);
                testStatus = testStatusFailed;
                failMessage = failMessage +
                    "Unexpected signature is returned: " + returnedSignature +
                    ", Expected: " + expectedSignatures[k] + "\n";
            }
        }

        if ( testStatus == testStatusPassed ) {
            logWriter.println
            ("\n=> CHECK PASSED: All expected reference types are got and have expected attributes");
        }

        assertAllDataRead(checkedReply);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        if (failMessage.length() > 0) {
            fail(failMessage);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReferenceTypeTest.class);
    }
}
