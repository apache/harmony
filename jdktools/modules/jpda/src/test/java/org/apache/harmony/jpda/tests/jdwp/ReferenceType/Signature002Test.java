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
 * Created on 04.03.2005
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
 * JDWP Unit test for ReferenceType..Signature command with incorrect ReferenceTypeIDs.
 */
public class Signature002Test extends JDWPSyncTestCase {

    static final String thisCommandName = "ReferenceType.Signature command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/Signature002Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.Signature002Debuggee";
    }

    /**
     * This testcase exercises ReferenceType.Signature command with incorrect ReferenceTypeIDs.
     * <BR>The test starts >Signature002Debuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then the test performs three variants of ReferenceType.Signature commands
     * and checks that commands return: 
     * <BR>&nbsp;&nbsp; - JDWP_ERROR_INVALID_CLASS, if objectID is passed as ReferenceTypeID;
     * <BR>&nbsp;&nbsp; - JDWP_ERROR_INVALID_OBJECT, if fieldID is passed as ReferenceTypeID;
     * <BR>&nbsp;&nbsp; - JDWP_ERROR_INVALID_OBJECT, if unknown ID is passed as ReferenceTypeID;
     */
    public void testSignature002() {
        String thisTestName = "testSignature002";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        finalSyncMessage = "TO_FINISH";

        long debuggeeRefTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + debuggeeRefTypeID);

        long checkedFieldID = checkField(debuggeeRefTypeID, "checkedObject");

        logWriter.println
        ("=> Send ReferenceType::GetValues command for received fieldID and get ObjectID to check...");

        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(debuggeeRefTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(checkedFieldID);
       
        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, "ReferenceType::GetValues command");
        
        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        if ( returnedValuesNumber != 1 ) {
            logWriter.println
            ("\n## FAILURE: ReferenceType::GetValues command returned unexpected number of values:");
            logWriter.println("## Expected number = 1");
            logWriter.println("## Returned number = " + returnedValuesNumber);
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            synchronizer.sendMessage("TO_FINISH");
            assertEquals("ReferenceType::GetValues command returned unexpected number of values,",
                    1, returnedValuesNumber);
        }

        Value checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        byte checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checked object= " + checkedObjectFieldTag
            + "(" + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");
        if ( checkedObjectFieldTag != JDWPConstants.Tag.OBJECT_TAG ) {
            finalSyncMessage = "TO_FINISH";
            printErrorAndFail(
            "ReferenceType::GetValues command returned unexpected tag: " +
            checkedObjectFieldTag + "(" +
            JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")" +
            ", Expected tag = " + JDWPConstants.Tag.OBJECT_TAG + "(OBJECT_TAG)");
        }
        
        long checkedObjectID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned checked ObjectID = " + checkedObjectID);

        logWriter.println
            ("\n=> CHECK: send " + thisCommandName + " for checked ObjectID: INVALID_CLASS is expected...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        checkedCommand.setNextValueAsReferenceTypeID(checkedObjectID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            if ( errorCode != JDWPConstants.Error.INVALID_CLASS ) {
                logWriter.println("## CHECK: FAILURE: " +  thisCommandName 
                    + " returns unexpected ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                fail(thisCommandName
                        + " returns unexpected ERROR = " + errorCode 
                        + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            } else { 
                logWriter.println("=> CHECK PASSED: Expected error (INVALID_CLASS) is returned");
            }
        } else {
            logWriter.println
            ("\n## FAILURE: " + thisCommandName + " does NOT return expected error - INVALID_CLASS");
            fail(thisCommandName + " does NOT return expected error - INVALID_CLASS");
        }

        logWriter.println
        ("\n=> CHECK: send " + thisCommandName + " for checked fieldID: INVALID_OBJECT is expected...");

        checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        checkedCommand.setNextValueAsReferenceTypeID(checkedFieldID);
        
        checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        
        errorCode = checkedReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            if ( errorCode != JDWPConstants.Error.INVALID_OBJECT ) {
                logWriter.println("## CHECK: FAILURE: " +  thisCommandName 
                    + " returns unexpected ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                fail(thisCommandName 
                        + " returns unexpected ERROR = " + errorCode 
                        + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            } else { 
                logWriter.println("=> CHECK PASSED: Expected error (INVALID_OBJECT) is returned");
            }
        } else {
            logWriter.println
            ("\n## FAILURE: " + thisCommandName + " does NOT return expected error - INVALID_OBJECT");
            fail(thisCommandName + " does NOT return expected error - INVALID_OBJECT");
        }

        logWriter.println
        ("\n=> CHECK: send " + thisCommandName + " for unknown ID: INVALID_OBJECT is expected...");

        long unknownID = debuggeeRefTypeID + 10;
        if ( unknownID == checkedFieldID ) {
            unknownID = unknownID + 100;   
        }
        logWriter.println("=> unknown ID = " + unknownID);
        checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        checkedCommand.setNextValueAsReferenceTypeID(unknownID);
        
        checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        
        errorCode = checkedReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            if ( errorCode != JDWPConstants.Error.INVALID_OBJECT ) {
                logWriter.println("## CHECK: FAILURE: " +  thisCommandName 
                    + " returns unexpected ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                fail(thisCommandName 
                        + " returns unexpected ERROR = " + errorCode 
                        + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            } else { 
                logWriter.println("=> CHECK PASSED: Expected error (INVALID_OBJECT) is returned");
            }
        } else {
            logWriter.println
            ("\n## FAILURE: " + thisCommandName + " does NOT return expected error - INVALID_OBJECT");
            fail(thisCommandName + " does NOT return expected error - INVALID_OBJECT");
        }

        finalSyncMessage = null;
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("\n==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Signature002Test.class);
    }
}
