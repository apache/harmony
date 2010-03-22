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
 * Created on 13.07.2005
 */

package org.apache.harmony.jpda.tests.jdwp.ArrayReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ArrayReference.SetValues command with specific values.  
 */
public class SetValues002Test extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ArrayReference::SetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ArrayReference/SetValues002Debuggee;";

    /**
     * Returns full name of debuggee class which is used by this test.
     * @return full name of debuggee class.
     */
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ArrayReference.SetValues002Debuggee";
    }

    /**
     * This testcase exercises ArrayReference.SetValues command for
     * array of elements of referenceType with value=null.
     * <BR>The test expects array element should be set by null value.
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
        // Number of returned reference types - is NOT used here

        classesBySignatureReply.getNextValueAsByte();
        // refTypeTag of class - is NOT used here

        long refTypeID = classesBySignatureReply.getNextValueAsReferenceTypeID();
        classesBySignatureReply = null;

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        String checkedFieldNames[] = {
                "objectArrayField",
        };
        long checkedFieldIDs[] = checkFields(refTypeID, checkedFieldNames);

        logWriter.println("=> Send ReferenceType::GetValues command and get ArrayID to check...");

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
        assertEquals("ReferenceType::GetValues returned invalid number of values,",
                1, returnedValuesNumber);
        logWriter.println("=> Returned values number = " + returnedValuesNumber);

        Value checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        byte checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checked object= " + checkedObjectFieldTag
            + "(" + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");

        assertEquals("ReferenceType::GetValues returned invalid object field tag,",
                JDWPConstants.Tag.ARRAY_TAG, checkedObjectFieldTag,
                JDWPConstants.Tag.getName(JDWPConstants.Tag.ARRAY_TAG),
                JDWPConstants.Tag.getName(checkedObjectFieldTag));
        
        long checkedObjectID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned checked ArrayID = " + checkedObjectID);
        logWriter.println("=> CHECK: send " + thisCommandName 
            + " for this ArrayID for element of referenceType with null values...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.SetValuesCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        checkedCommand.setNextValueAsInt(0); // first index
        checkedCommand.setNextValueAsInt(1); // elements' number
        checkedCommand.setNextValueAsObjectID(0); // null value
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, "ArrayReference::SetValues command");

        logWriter.println("=> Wait for Debuggee's status about check for set field...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        boolean debuggeeStatus = synchronizer.receiveMessage("PASSED");
        if (!debuggeeStatus) {
            logWriter.println("## " + thisTestName + ": Debuggee returned status FAILED");
            //logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FAILED");
            fail("Debuggee returned status FAILED");
        } else {
            logWriter.println("=> " + thisTestName + ": Debuggee returned status PASSED");
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": OK.");
    }

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetValues002Test.class);
    }
}
