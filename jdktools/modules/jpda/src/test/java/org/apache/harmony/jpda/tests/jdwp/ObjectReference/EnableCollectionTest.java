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
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ObjectReference.EnableCollection command.
 */
public class EnableCollectionTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ObjectReference::EnableCollection command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/EnableCollectionDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.EnableCollectionDebuggee";
    }

    /**
     * This testcase exercises ObjectReference.EnableCollection command.
     * <BR>The test starts EnableCollectionDebuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then for this objectID test executes ObjectReference::DisableCollection command 
     * and ObjectReference.EnableCollection command. After that Debuggee tries to
     * unload checked object and checks if checked object is unloaded.
     * <BR>If so the test passes. Otherwise it fails in case when pattern object is unloaded.
     */
    public void testEnableCollection001() {
        String thisTestName = "testEnableCollection001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        finalSyncMessage = "TO_FINISH";

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        logWriter.println
        ("=> Send ReferenceType::Fields command and get fieldID for field representing checked object...");

        long checkedFieldID = checkFields(refTypeID, new String[] { "checkedObject" })[0];

        logWriter.println
        ("=> Send ReferenceType::GetValues command for received fieldID and get ObjectID to check...");

        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(refTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(checkedFieldID);

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
        assertEquals("invalid value tag for checked object,", JDWPConstants.Tag.OBJECT_TAG, checkedObjectFieldTag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(checkedObjectFieldTag));

        long checkedObjectID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned checked ObjectID = " + checkedObjectID);

        logWriter.println
            ("\n=> Send ObjectReference::DisableCollection command for checked ObjectID...");

        CommandPacket disableCollectionCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.DisableCollectionCommand);
        disableCollectionCommand.setNextValueAsObjectID(checkedObjectID);

        ReplyPacket disableCollectionReply = debuggeeWrapper.vmMirror.performCommand(disableCollectionCommand);
        disableCollectionCommand = null;
        checkReplyPacket(disableCollectionReply, "ObjectReference::DisableCollection command");

        logWriter.println
        ("\n=> CHECK: Send " + thisCommandName + " for checked ObjectID...");

        CommandPacket checkedCommand = new CommandPacket(
            JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
            JDWPCommands.ObjectReferenceCommandSet.EnableCollectionCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);

        logWriter.println("=> CHECK: Reply is received without any error");

        logWriter.println("=> Send to Debuggee signal to continue and try to unload checked ObjectID...");
        finalSyncMessage = null;
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        String messageFromDebuggee = synchronizer.receiveMessage();
        logWriter.println
        ("\n=> Received message from Debuggee = \"" + messageFromDebuggee + "\"" );
        if ( messageFromDebuggee.equals
                ("Checked Object is NOT UNLOADed; Pattern Object is UNLOADed;") ) {
            logWriter.println
                ("## FAILURE: Checked Object is NOT UNLOADed after " + thisCommandName );
            fail("Checked Object is NOT UNLOADed after " + thisCommandName);
        } else {
            logWriter.println("=> PASSED: It is expected result" );
        }

        assertAllDataRead(checkedReply);

        logWriter.println("=> Send to Debuggee signal to funish ...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EnableCollectionTest.class);
    }
}
