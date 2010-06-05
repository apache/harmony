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
 * @author Aleksander V. Budniy
 */

/**
 * Created on 8.7.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for verifying re-enabling of garbage collecting after re-connection.
 */
public class EnableCollectionTest extends JDWPSyncTestCase {

    String checkedFieldName = "checkedObject";
    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "MultiSession::EnableCollection command";
    
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/MultiSession/EnableCollectionDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.MultiSession.EnableCollectionDebuggee";
    }

    /**
     * This testcase verifies re-enabling of garbage collecting after re-connection.
     * <BR>It runs EnableCollectionDebuggee, disables garbage collecting for checked object
     * with ObjectReference.DisableCollection command and re-connects.
     * <BR>It is expected that checked object is garbage collected after re-connection.
     */
    public void testEnableCollection001() {
        String thisTestName = "testEnableCollection001";
        logWriter.println("==> testEnableCollection001 started..");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        finalSyncMessage = "TO_FINISH";

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        long checkedFieldID = debuggeeWrapper.vmMirror.getFieldID(refTypeID, checkedFieldName);
        
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
        assertEquals("Invalid returned number of values,", 1, returnedValuesNumber);

        Value checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        byte checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checked object= " + checkedObjectFieldTag
            + "(" + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");
        assertEquals("Invalid object tag,", JDWPConstants.Tag.OBJECT_TAG, checkedObjectFieldTag);

        long checkedObjectID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned checked ObjectID = " + checkedObjectID);

        logWriter.println
            ("\n=> CHECK: send " + thisCommandName + " for checked ObjectID...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.DisableCollectionCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, "ObjectReference::DisableCollection command");

        logWriter.println("=> CHECK: Reply is received without any error");

        logWriter.println("");
        logWriter.println("=> CLOSE CONNECTION..");
        closeConnection();
        logWriter.println("=> CONNECTION CLOSED");
        logWriter.println("");
        logWriter.println("=> OPEN NEW CONNECTION..");
        openConnection();
        logWriter.println("=> CONNECTION OPENED");
        
        logWriter.println("=> Resuming debuggee");
        
        // start the thread
        finalSyncMessage = null;
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        String messageFromDebuggee = synchronizer.receiveMessage();
        logWriter.println
        ("\n=> Received message from Debuggee = \"" + messageFromDebuggee + "\"" );
        if ( messageFromDebuggee.equals
                ("Checked Object is NOT UNLOADed; Pattern Object is UNLOADed;") ) {
            logWriter.println
                ("## FAILURE: Checked Object is NOT UNLOADed after " + thisCommandName);
            fail("Invalid message from debuggee.");
        } else {
            logWriter.println("=> PASSED: It is expected result" );
        }

        assertAllDataRead(checkedReply);

        logWriter.println("=> Send to Debuggee signal to finish ...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        logWriter.println("==> testEnableCollection001 PASSED!");
    }
    
    protected void beforeDebuggeeStart(JDWPUnitDebuggeeWrapper debuggeeWrapper) {
        settings.setAttachConnectorKind();
        if (settings.getTransportAddress() == null) {
            settings.setTransportAddress(TestOptions.DEFAULT_ATTACHING_ADDRESS);
        }
        logWriter.println("ATTACH connector kind");
        super.beforeDebuggeeStart(debuggeeWrapper);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(EnableCollectionTest.class);
    }
}
