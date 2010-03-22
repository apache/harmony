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
 * Created on 05.03.2005
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
 * JDWP Unit test for ObjectReference.IsCollected command.
 */
public class IsCollectedTest extends JDWPSyncTestCase {

    static final String thisCommandName = "ObjectReference.IsCollected command";

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/IsCollectedDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.IsCollectedDebuggee";
    }

    /**
     * This test exercises ObjectReference.IsCollected command.
     * <BR>The test starts IsCollectedDebuggee class, gets two
     * objectIDs as value of static fields of this class which (fields)
     * represent two checked objects. Then for the first objectID test executes
     * ObjectReference.DisableCollection command. After that Debuggee tries to
     * unload checked objects. Then the test executes
     * ObjectReference.IsCollected commands for both checked objects and checks
     * replies.
     */
    public void testIsCollected001() {
        String thisTestName = "testIsCollected001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        String failMessage = "";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        finalSyncMessage = "TO_FINISH";

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = "
                + refTypeID);

        String checkedFieldNames[] = { "checkedObject_01", "checkedObject_02", };
        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames);
        long checkedField_01ID = checkedFieldIDs[0];
        long checkedField_02ID = checkedFieldIDs[1];

        logWriter
                .println("=> Send ReferenceType::GetValues command for received fieldIDs and get ObjectIDs to check...");

        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(2);
        getValuesCommand.setNextValueAsFieldID(checkedField_01ID);
        getValuesCommand.setNextValueAsFieldID(checkedField_02ID);

        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror
                .performCommand(getValuesCommand);
        getValuesCommand = null;
        checkReplyPacket(getValuesReply, "ReferenceType::GetValues command");

        int returnedValuesNumber = getValuesReply.getNextValueAsInt();
        logWriter
                .println("=> Returned values number = " + returnedValuesNumber);
        assertEquals("Invalid number of values,", 2, returnedValuesNumber);

        Value checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        byte checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checkedObject_01 = "
                + checkedObjectFieldTag + "("
                + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");
        assertEquals("Invalid value tag for checkedObject_01",
                JDWPConstants.Tag.OBJECT_TAG, checkedObjectFieldTag,
                JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG),
                JDWPConstants.Tag.getName(checkedObjectFieldTag));

        long checkedObject_01ID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned ObjectID for checkedObject_01 = "
                + checkedObject_01ID);

        checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checkedObject_02 = "
                + checkedObjectFieldTag + "("
                + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");
        assertEquals("Invalid value tag for checkedObject_02",
                JDWPConstants.Tag.OBJECT_TAG, checkedObjectFieldTag,
                JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG),
                JDWPConstants.Tag.getName(checkedObjectFieldTag));

        long checkedObject_02ID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned ObjectID for checkedObject_02 = "
                + checkedObject_02ID);

        logWriter
                .println("\n=> Send ObjectReference::DisableCollection command for checkedObject_01...");

        CommandPacket disableCollectionCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.DisableCollectionCommand);
        disableCollectionCommand.setNextValueAsObjectID(checkedObject_01ID);

        ReplyPacket disableCollectionReply = debuggeeWrapper.vmMirror
                .performCommand(disableCollectionCommand);
        disableCollectionCommand = null;
        checkReplyPacket(disableCollectionReply,
                "ObjectReference::DisableCollection command");

        logWriter
                .println("=> Send to Debuggee signal to continue and try to unload checked objects...");
        finalSyncMessage = null;
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        String messageFromDebuggee = synchronizer.receiveMessage();
        logWriter.println("\n=> Received message from Debuggee = \""
                + messageFromDebuggee + "\"");

        logWriter.println("\n=> Send " + thisCommandName
                + " for checkedObject_01 and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.IsCollectedCommand);
        checkedCommand.setNextValueAsObjectID(checkedObject_01ID);

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);

        boolean checkedObject_01_IsCollected = checkedReply
                .getNextValueAsBoolean();
        logWriter.println("=> IsCollected for checkedObject_01 = "
                + checkedObject_01_IsCollected);

        if (messageFromDebuggee.indexOf("checkedObject_01 is UNLOADed;") != -1) {
            if (!checkedObject_01_IsCollected) {
                logWriter
                        .println("## FAILURE: Unexpected result for checkedObject_01 of "
                                + thisCommandName + ":");
                logWriter
                        .println("## checkedObject_01 is UNLOADed so IsCollected must be 'true'");
                failMessage = failMessage +
                    "Unexpected result for checkedObject_01 of "
                    + thisCommandName + "\n";
            }
        } else {
            if (checkedObject_01_IsCollected) {
                logWriter
                        .println("## FAILURE: Unexpected result for checkedObject_01 of "
                                + thisCommandName + ":");
                logWriter
                        .println("## checkedObject_01 is NOT UNLOADed so IsCollected must be 'false'");
                failMessage = failMessage +
                    "Unexpected result for checkedObject_01 of "
                    + thisCommandName + "\n";
            }
        }

        logWriter.println("=> PASSED for checkedObject_01");

        logWriter.println("\n=> Send " + thisCommandName
                + " for checkedObject_02 and check reply...");

        checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.IsCollectedCommand);
        checkedCommand.setNextValueAsObjectID(checkedObject_02ID);

        checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);

        boolean checkedObject_02_IsCollected = checkedReply
                .getNextValueAsBoolean();
        logWriter.println("=> IsCollected for checkedObject_02 = "
                + checkedObject_02_IsCollected);

        if (messageFromDebuggee.indexOf("checkedObject_02 is UNLOADed;") != -1) {
            if (!checkedObject_02_IsCollected) {
                logWriter
                        .println("## FAILURE: Unexpected result for checkedObject_02 of "
                                + thisCommandName + ":");
                logWriter
                        .println("## checkedObject_02 is UNLOADed so IsCollected must be 'true'");
                failMessage = failMessage +
                    "Unexpected result for checkedObject_02 of "
                    + thisCommandName + "\n";
            }
        } else {
            if (checkedObject_02_IsCollected) {
                logWriter
                        .println("## FAILURE: Unexpected result for checkedObject_02 of "
                                + thisCommandName + ":");
                logWriter
                        .println("## checkedObject_02 is NOT UNLOADed so IsCollected must be 'false'");
                failMessage = failMessage + 
                    "Unexpected result for checkedObject_02 of "
                    + thisCommandName + "\n";
            }
        }

        logWriter.println("=> PASSED for checkedObject_02");
        logWriter.println("=> Send to Debuggee signal to funish ...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": FINISH");

        if (failMessage.length() > 0) {
            fail(failMessage);
        }

        assertAllDataRead(checkedReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IsCollectedTest.class);
    }
}
