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
 * Created on 03.03.2005
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
 * JDWP Unit test for ObjectReference.MonitorInfo command.
 */
public class MonitorInfoTest extends JDWPSyncTestCase {

    static final String thisCommandName = "ObjectReference.MonitorInfo command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/MonitorInfoDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.MonitorInfoDebuggee";
    }

    /**
     * This test exercises ObjectReference.MonitorInfo command.
     * <BR>The test starts MonitorInfoDebuggee class, gets objectID
     * as value of static field of this class which (field) represents checked object.
     * Then for this objectID test executes ObjectReference.MonitorInfo command for 
     * checked object and checks that command returns the expected monitor info:
     * <BR>monitorOwnerThreadID = 0, monitorEntryCount = 0, monitorWaiters = 0
     * <BR>Then test waits for Debuggee to continue and to enter in synchronized block
     * and again executes ObjectReference.MonitorInfo for checked object.
     * Then test checks that expected results are received:
     * <BR>monitorOwnerThreadID = 'not null', monitorEntryCount = 1, monitorWaiters = 0
     */
    public void testMonitorInfo001() {
        String thisTestName = "testMonitorInfo001";
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canGetMonitorInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetMonitorInfo;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canGetMonitorInfo");
            return;
        }

        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        String failMessage = "";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        finalSyncMessage = "TO_FINISH";

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);

        long checkedFieldID = checkField(refTypeID, "lockObject");

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
        assertEquals("Invalid number of values returned by ReferenceType::GetValues command,", 1, returnedValuesNumber);

        Value checkedObjectFieldValue = getValuesReply.getNextValueAsValue();
        byte checkedObjectFieldTag = checkedObjectFieldValue.getTag();
        logWriter.println("=> Returned field value tag for checked object= " + checkedObjectFieldTag
            + "(" + JDWPConstants.Tag.getName(checkedObjectFieldTag) + ")");
        assertEquals("Invalid value tag for checked object,", JDWPConstants.Tag.OBJECT_TAG, checkedObjectFieldTag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(checkedObjectFieldTag));

        long checkedObjectID = checkedObjectFieldValue.getLongValue();
        logWriter.println("=> Returned checked ObjectID = " + checkedObjectID);

        logWriter.println("=> Send VirtualMachine::Suspend command...");

        CommandPacket suspendCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SuspendCommand);
        
        ReplyPacket suspendReply = debuggeeWrapper.vmMirror.performCommand(suspendCommand);
        suspendCommand = null;
        checkReplyPacket(suspendReply, "VirtualMachine::Suspend command");
        
        logWriter.println
            ("\n=> CHECK 1: send " + thisCommandName + " for checked ObjectID and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.MonitorInfoCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if ( errorCode == JDWPConstants.Error.NOT_IMPLEMENTED ) {
            // it is possible case
            logWriter.println("=> " +  thisCommandName + " returns ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            logWriter.println("=> It is possible case - CHECK 1 PASSED"); 
            logWriter.println("=> Send to Debuggee signal to funish ...");
            synchronizer.sendMessage("TO_FINISH");
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        } else {
            checkReplyPacket(checkedReply, thisCommandName);
        }

        long monitorOwnerThreadID = checkedReply.getNextValueAsThreadID();
        logWriter.println("=> Returned monitorOwnerThreadID = " + monitorOwnerThreadID);
        if ( monitorOwnerThreadID != 0) {
            logWriter.println
                ("## FAILURE: " + thisCommandName + " returns unexpected monitorOwnerThreadID:" +
                        monitorOwnerThreadID);
            logWriter.println("## Expected monitorOwnerThreadID = 0");
            failMessage = failMessage +
                thisCommandName + " returns unexpected monitorOwnerThreadID: " +
                monitorOwnerThreadID +
                ", Expected: 0\n";
        }

        int monitorEntryCount = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned monitorEntryCount = " + monitorEntryCount);
        if ( monitorEntryCount != 0) {
            logWriter.println
                ("## FAILURE: " + thisCommandName + " returns unexpected monitorEntryCount:");
            logWriter.println("## Expected monitorEntryCount = 0");
            failMessage = failMessage +
                thisCommandName + " returns unexpected monitorEntryCount:" +
                monitorEntryCount +
                ", expected: 0\n";
        }

        int monitorWaiters = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned monitorWaiters = " + monitorWaiters);
        if ( monitorWaiters != 0) {
            logWriter.println
                ("## FAILURE: " + thisCommandName + " returns unexpected monitorWaiters:");
            logWriter.println("## Expected monitorWaiters = 0");
            failMessage = failMessage +
                thisCommandName + " returns unexpected monitorWaiters:" +
                monitorWaiters +
                ", expected: 0\n";
        }

        assertAllDataRead(checkedReply);
        logWriter.println("=> CHECK 1: PASSED - expected monitor info is received");
        checkedReply = null;

        logWriter.println("\n=> Send VirtualMachine::Resume command ...");

        resumeDebuggee();

        logWriter.println("=> Send to Debuggee signal to continue and to enter in synchronized block ...");
        finalSyncMessage = null;
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        logWriter.println("=> Send VirtualMachine::Suspend command...");

        suspendCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SuspendCommand);
        
        suspendReply = debuggeeWrapper.vmMirror.performCommand(suspendCommand);
        suspendCommand = null;
        checkReplyPacket(suspendReply, "VirtualMachine::Suspend command");
        
        logWriter.println
            ("\n=> CHECK 2: send " + thisCommandName + " for checked ObjectID when it is locked...");

        checkedCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.MonitorInfoCommand);
        checkedCommand.setNextValueAsObjectID(checkedObjectID);
        
        checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);
        
        monitorOwnerThreadID = checkedReply.getNextValueAsThreadID();
        logWriter.println("=> Returned monitorOwnerThreadID = " + monitorOwnerThreadID);
        if ( monitorOwnerThreadID == 0) {
            logWriter.println
                ("## FAILURE: " + thisCommandName + " returns unexpected monitorOwnerThreadID:");
            logWriter.println("## Expected monitorOwnerThreadID = 'not null'");
            failMessage = failMessage +
                thisCommandName + " returns unexpected monitorOwnerThreadID: 0" +
                ", Expected monitorOwnerThreadID: 'not null'\n";
        }

        monitorEntryCount = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned monitorEntryCount = " + monitorEntryCount);
        if ( monitorEntryCount != 1) {
            logWriter.println
                ("## FAILURE: " + thisCommandName + " returns unexpected monitorEntryCount:" +
                        monitorEntryCount);
            logWriter.println("## Expected monitorEntryCount = 1");
            failMessage = failMessage +
                thisCommandName + " returns unexpected monitorEntryCount: " +
                monitorEntryCount +
                ", expected: 1\n";
        }

        monitorWaiters = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned monitorWaiters = " + monitorWaiters);
        if ( monitorWaiters != 0) {
            logWriter.println
                ("## FAILURE: " + thisCommandName + " returns unexpected monitorWaiters:" +
                        monitorWaiters);
            logWriter.println("## Expected monitorWaiters = 0");
            failMessage = failMessage +
                thisCommandName + " returns unexpected monitorWaiters: " +
                monitorWaiters +
                ", expected: 0\n";
        }

        logWriter.println("=> CHECK 2: PASSED - expected monitor info is received");
        logWriter.println("\n=> Send VirtualMachine::Resume command ...");

        resumeDebuggee();
        
        logWriter.println("=> Send to Debuggee signal to funish ...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        
        assertAllDataRead(checkedReply);
        
        if (failMessage.length() > 0) {
            fail(failMessage);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MonitorInfoTest.class);
    }
}
