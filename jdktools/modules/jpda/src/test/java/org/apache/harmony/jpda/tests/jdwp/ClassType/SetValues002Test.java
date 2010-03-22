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
 * Created on 05.07.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ClassType.SetValues command with incorrect types of values.
 */
public class SetValues002Test extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ClassType::SetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ClassType/SetValues002Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ClassType.SetValues002Debuggee";
    }

    /**
     * The test checks ClassType.SetValues command for
     * field of Debuggee class with value which has other 
     * referenceType than field to set.
     * The test expects the field should not be set.
     */
    public void testSetValues002() {
        String thisTestName = "testSetValues002";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket classesBySignatureCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        classesBySignatureCommand.setNextValueAsString(debuggeeSignature);
        ReplyPacket classesBySignatureReply =
            debuggeeWrapper.vmMirror.performCommand(classesBySignatureCommand);
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
        
        String checkedFieldNames[] = {
            "SetValues002DebuggeeObject",
            "objectField",
        };
        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames);
        int checkedFieldsNumber = checkedFieldNames.length;

        logWriter.println
            ("=> Send ReferenceType::GetValues command and get ObjectID for value to set...");

        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(checkedFieldIDs[0]);
        ReplyPacket getValuesReply =
            debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, "ReferenceType::GetValues command");

        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        assertEquals("ReferenceType::GetValues returned invalid values number,",
                1, returnedValuesNumber);

        Value objectFieldValueToSet = getValuesReply.getNextValueAsValue();
        byte objectFieldValueToSetTag = objectFieldValueToSet.getTag();
        logWriter.println
            ("=> Returned field value tag for checked object= " + objectFieldValueToSetTag
            + "(" + JDWPConstants.Tag.getName(objectFieldValueToSetTag) + ")");
        assertEquals("ReferenceType::GetValues returned invalid value tag,",
                JDWPConstants.Tag.OBJECT_TAG, objectFieldValueToSetTag,
                JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG),
                JDWPConstants.Tag.getName(objectFieldValueToSetTag));

        long objectFieldID = objectFieldValueToSet.getLongValue();
        logWriter.println("=> Returned ObjectID = " + objectFieldID);
        logWriter.println
            ("=> CHECK: send " + thisCommandName 
            + " for Debuggee class with value which has other referenceType than field to set...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.SetValuesCommand);
        checkedCommand.setNextValueAsClassID(refTypeID);
        checkedCommand.setNextValueAsInt(checkedFieldsNumber-1);
        int fieldIndex = 1;
        for (; fieldIndex < checkedFieldsNumber; fieldIndex++) {
            checkedCommand.setNextValueAsFieldID(checkedFieldIDs[fieldIndex]);
            switch ( fieldIndex ) {
            case 1: // objectField
                checkedCommand.setNextValueAsObjectID(objectFieldID);
                break;
            }
        }
        ReplyPacket checkedReply =
            debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if ( errorCode == JDWPConstants.Error.NONE ) {
            logWriter.println("=> " +  thisCommandName 
                    + " run without any ERROR!");
        } else {
            logWriter.println("=> " +  thisCommandName 
                    + " returns ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
        }
        
        logWriter.println("=> Wait for Debuggee's status about check for set field...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        boolean debuggeeStatus = synchronizer.receiveMessage("PASSED");
        if ( ! debuggeeStatus ) {
            logWriter.println("## " + thisTestName + ": Debuggee returned status FAILED");
            fail("Debuggee returned status FAILED");
        } else {
            logWriter.println("=> " + thisTestName + ": Debuggee returned status PASSED");
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": OK");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetValues002Test.class);
    }
}
