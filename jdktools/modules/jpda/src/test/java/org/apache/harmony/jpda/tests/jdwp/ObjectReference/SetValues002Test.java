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
 * Created on 17.03.2005
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
 * JDWP Unit test for ObjectReference.SetValues command with value which is unknown objectID.
 */
public class SetValues002Test extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ObjectReference.SetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/SetValues002Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.SetValues002Debuggee";
    }

    /**
     * This test exercises ObjectReference.SetValues command with value which is unknown objectID.
     * <BR>The test starts SetValues002Debuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then for this objectID test executes ObjectReference.SetValues command for
     * fieldID of referenceType but with value which is unknown objectID.
     * <BR>The test expects INVALID_OBJECT error.
     */
    public void testSetValues002() {

        String thisTestName = "testSetValues002";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
            "setValues002DebuggeeObject",
            "objectField",
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
        logWriter.println
            ("=> CHECK: send " + thisCommandName 
            + " for this ObjectID with value which is unknown objectID: INVALID_OBJECT error is expected...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.SetValuesCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        checkedCommand.setNextValueAsInt(checkedFieldsNumber-1);
        long unknownObjectID = checkedObjectID + 100;
        logWriter.println("=> unknown objectID = " + unknownObjectID);
        int fieldIndex = 1;
        for (; fieldIndex < checkedFieldsNumber; fieldIndex++) {
            checkedCommand.setNextValueAsFieldID(checkedFieldIDs[fieldIndex]);
            switch ( fieldIndex ) {
            case 1: // objectField
                checkedCommand.setNextValueAsObjectID(unknownObjectID);
                break;
            }
        }
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        short errorCode = checkedReply.getErrorCode();
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

        logWriter.println("=> Wait for Debuggee to check for set field...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetValues002Test.class);
    }
}
