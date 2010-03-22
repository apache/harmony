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
 * Created on 21.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.NestedTypes command.
 */
public class NestedTypesTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.NestedTypes command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/NestedTypesDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.NestedTypesDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.NestedTypes command.
     * The test starts NestedTypesDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.NestedTypes command and checks that returned
     * list of nested classes corresponds to expected list.
     */
    public void testNestedTypes001() {
        String thisTestName = "testNestedTypes001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        String failMessage = "";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.NestedTypesCommand);
        checkedCommand.setNextValueAsReferenceTypeID(refTypeID);
        ReplyPacket checkedReply = 
            debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);
        
        int returnedNestedTypesNumber = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned nested types number = " + returnedNestedTypesNumber);

        String nestedTypeSignatures[] = {
                "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/NestedTypesDebuggee$StatInterf_1;",
                "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/NestedTypesDebuggee$StatClass_1;",
                "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/NestedTypesDebuggee$NonStatClass_1;",
        };
            
        byte nestedTypeTags[] = {
                JDWPConstants.TypeTag.INTERFACE,
                JDWPConstants.TypeTag.CLASS,
                JDWPConstants.TypeTag.CLASS,
        };

        boolean nestedTypeFound[] = {
                false,
                false,
                false,
        };
        
        int expectedNestedTypesNumber = nestedTypeSignatures.length;

        logWriter.println("=> CHECK for all returned NestedTypes...");
        for (int i = 0; i < returnedNestedTypesNumber; i++) {
            logWriter.println("\n=> Check for returned nested type[" + i + "] ...");
            byte returnedRefTypeTag = checkedReply.getNextValueAsByte();
            logWriter.println("=> RefTypeTag of nested type = " + returnedRefTypeTag + "("
                + JDWPConstants.TypeTag.getName(returnedRefTypeTag) + ")");
            long returnedRefTypeID = checkedReply.getNextValueAsReferenceTypeID();
            logWriter.println("=> RefTypeID of nested type = " + returnedRefTypeID);
            logWriter.println("=> Get signature for nested type...");
            CommandPacket signatureCommand = new CommandPacket(
            JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
            JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
            signatureCommand.setNextValueAsReferenceTypeID(returnedRefTypeID);
            ReplyPacket signatureReply = 
                debuggeeWrapper.vmMirror.performCommand(signatureCommand);
            signatureCommand = null;
            checkReplyPacket(signatureReply, "ReferenceType::Signature command");
            
            String returnedSignature = signatureReply.getNextValueAsString();
            signatureReply = null;
            logWriter.println("=> Signature of nested type = " + returnedSignature);

            int k = 0;
            for (; k < expectedNestedTypesNumber; k++) {
                if ( ! nestedTypeSignatures[k].equals(returnedSignature)) {
                    continue;
                }
                if ( nestedTypeFound[k] ) {
                    logWriter.println("\n## FAILURE: This nested type is found out repeatedly in the list");
                    failMessage = failMessage +
                        "This nested type is found repeatedly in the list;\n";
                    break;
                }
                nestedTypeFound[k] = true;
                if ( nestedTypeTags[k] != returnedRefTypeTag ) {
                    logWriter.println("\n## FAILURE: Unexpected RefTypeTag is returned:");
                    logWriter.println("## Expected RefTypeTag = " + nestedTypeTags[k] + "("
                    + JDWPConstants.TypeTag.getName(nestedTypeTags[k]) + ")");
                    failMessage = failMessage +
                        "Unexpected RefTypeTag is returned:" +
                        returnedRefTypeTag + "(" +
                        JDWPConstants.TypeTag.getName(returnedRefTypeTag) + ")" +
                        ", Expected: " + nestedTypeTags[k] + "(" +
                        JDWPConstants.TypeTag.getName(nestedTypeTags[k]) + ");\n";
                }
                break;
            }
            if ( k == expectedNestedTypesNumber ) {
                // returned nested type is not found out in the list of expected nested types
                logWriter.println("\n## FAILURE: It is unexpected nested type");
                failMessage = failMessage +
                    "It is unexpected nested type;\n";
            }
        }
        
        for (int k=0; k < expectedNestedTypesNumber; k++) {
            if ( ! nestedTypeFound[k] ) {
                logWriter.println
                ("\n## FAILURE: Expected nested type is NOT found out in the returned list:");
                logWriter.println("=> Signature of nested type = " + nestedTypeSignatures[k]);
                failMessage = failMessage +
                    "Expected nested type is NOT found in the returned list: " + nestedTypeSignatures[k];
            }
        }

        finalSyncMessage = JPDADebuggeeSynchronizer.SGNL_CONTINUE;
        if (failMessage.length() > 0) {
            fail(failMessage);
        }
        assertAllDataRead(checkedReply);
        finalSyncMessage = null;

        logWriter.println
        ("\n=> CHECK PASSED: All expected nested types are found out and have expected attributes");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("\n==> " + thisTestName + " for " + thisCommandName + ": OK.");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(NestedTypesTest.class);
    }
}
