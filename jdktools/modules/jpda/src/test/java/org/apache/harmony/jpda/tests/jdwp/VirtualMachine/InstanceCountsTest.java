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

package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class InstanceCountsTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;

    static final int testStatusFailed = -1;

    static final String thisCommandName = "VirtualMachine.InstanceCounts command ";

    static final String mockClass1Signature = "Lorg/apache/harmony/jpda/tests/jdwp/VirtualMachine/MockClass1;";
    
    static final String mockClass2Signature = "Lorg/apache/harmony/jpda/tests/jdwp/VirtualMachine/MockClass2;";

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/VirtualMachine/InstanceCountsDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.VirtualMachine.InstanceCountsDebuggee";
    }
    
    // InstanceCounts need canGetInstanceInfo VM capability support
    private boolean isCapability() {
        // check capability, relevant for this test
        logWriter.println("=> Check capability: canGetInstanceInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetInstanceInfo;
        return isCapability;
    }

    /**
     * This testcase exercises VirtualMachine.InstanceCounts command.
     * <BR>The test starts InstanceCountsDebuggee class, requests referenceTypeId, 
     * MockClass1, MockClass2 for this class by VirtualMachine.ClassesBySignature command, 
     * then performs VirtualMachine.InstanceCounts command and checks that returned
     * Reachable Objects of MockClass1 and MockClass2 are equal to the actual ones.
     */
    public void testInstanceCounts_Normal() {
        String thisTestName = "testInstanceCounts_Normal";

        if (!isCapability()) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long debuggeeRefTypeID = getClassIDBySignature(debuggeeSignature);
        long mockClassRefTypeIDOfClass1 = getClassIDBySignature(mockClass1Signature);
        long mockClassRefTypeIDOfClass2 = getClassIDBySignature(mockClass2Signature);

        //Get the number of reachable objects of MockClass1 and MockClass2 in debuggee class
        long reachableObjNumOfClass1ID = debuggeeWrapper.vmMirror.getFieldID(
                debuggeeRefTypeID, "reachableObjNumOfClass1");
        long reachableObjNumOfClass2ID = debuggeeWrapper.vmMirror.getFieldID(
                debuggeeRefTypeID, "reachableObjNumOfClass2");
        
        long[] fieldIDs = new long[2];
        fieldIDs[0] = reachableObjNumOfClass1ID;
        fieldIDs[1] = reachableObjNumOfClass2ID;

        Value[] values = debuggeeWrapper.vmMirror.getReferenceTypeValues(
                debuggeeRefTypeID, fieldIDs);
        int expectedObjNumOfClass1 = values[0].getIntValue();
        int expectedObjNumOfClass2 = values[1].getIntValue();
        
        logWriter.println("=> ReachableObjNum of MockClass1 in debuggee is: " + expectedObjNumOfClass1);
        logWriter.println("=> ReachableObjNum of MockClass2 in debuggee is: " + expectedObjNumOfClass2);
        
        logWriter.println("=> CHECK: send " + thisCommandName
                + " and check reply for ERROR...");

        // Compose InstanceCounts commnad
        CommandPacket InstanceCountsCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.InstanceCountsCommand);
        
        final int refTypesCount = 2;
        InstanceCountsCommand.setNextValueAsInt(refTypesCount);
        InstanceCountsCommand.setNextValueAsReferenceTypeID(mockClassRefTypeIDOfClass1);
        InstanceCountsCommand.setNextValueAsReferenceTypeID(mockClassRefTypeIDOfClass2);

        // Perform InstanceCounts command and attain reply package
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstanceCountsCommand);
        InstanceCountsCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if (errorCode != JDWPConstants.Error.NONE) {
            if (errorCode == JDWPConstants.Error.NOT_IMPLEMENTED) {
                logWriter.println("=> CHECK PASSED: Expected error (NOT_IMPLEMENTED) is returned");
                return;
            }
            else if(errorCode == JDWPConstants.Error.ILLEGAL_ARGUMENT) {
                logWriter.println("=> CHECK PASSED: Expected error (ILLEGAL_ARGUMENT) is returned");
                return;
            }
        }

        //Check the reference types count that returned. 
        int returnedRefTypesCount = checkedReply.getNextValueAsInt();
        assertEquals(thisCommandName + "returned reference types count is wrong.",
                refTypesCount, returnedRefTypesCount, null, null);
        logWriter.println("=> CHECK: PASSED: expected reference types count is returned:");
        logWriter.println("=> Returned reference types count is " + returnedRefTypesCount);

        long returnedObjNumOfClass1 = checkedReply.getNextValueAsLong();
        assertEquals(thisCommandName + "returned instance count of MockClass1 is wrong.",
                expectedObjNumOfClass1, returnedObjNumOfClass1, null, null);
        logWriter.println("=> CHECK: PASSED: expected instance count of MockClass1 is returned:");
        logWriter.println("=> Returned instance count of MockClass1 is " + returnedObjNumOfClass1);

        long returnedObjNumOfClass2 = checkedReply.getNextValueAsLong();
        assertEquals(thisCommandName + "returned instance count of MockClass2 is wrong.",
                expectedObjNumOfClass2, returnedObjNumOfClass2, null, null);
        logWriter.println("=> CHECK: PASSED: expected instance count of MockClass2 is returned:");
        logWriter.println("=> Returned instance count of MockClass2 is " + returnedObjNumOfClass2);
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        assertAllDataRead(checkedReply);
    }

    /**
     * This testcase exercises ReferenceType.InstanceCounts command. <BR>
     * Compose a InstanceCounts command with negative reference types count
     * The vm should throw a ILLEGAL_ARGUMENT exception.
     */
    public void testInstanceCounts_IllegalArgument() {
        String thisTestName = "testInstanceCounts_IllegalArgument";
        
        if (!isCapability()) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }
        
        int refTypesCount = -1;
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Compose InstanceCounts commnad
        CommandPacket InstanceCountsCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.InstanceCountsCommand);
        
        InstanceCountsCommand.setNextValueAsInt(refTypesCount);
        InstanceCountsCommand.setNextValueAsReferenceTypeID(0);
        
        // Perform InstanceCounts command and attain reply package
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstanceCountsCommand);
        InstanceCountsCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if (errorCode != JDWPConstants.Error.NONE) {
            if (errorCode == JDWPConstants.Error.NOT_IMPLEMENTED) {
                logWriter.println("=> CHECK PASSED: Expected error (NOT_IMPLEMENTED) is returned");
                return;
            }
            else if(errorCode == JDWPConstants.Error.ILLEGAL_ARGUMENT) {
                logWriter.println("=> CHECK PASSED: Expected error (ILLEGAL_ARGUMENT) is returned");
                return;
            }
        }
        printErrorAndFail(thisCommandName + " should throw ILLEGAL_ARGUMENT exception when refTypesCount is negative.");
    }
    

    /**
     * This testcase exercises ReferenceType.InstanceCounts command. <BR>
     * Compose a InstanceCounts command with zero reference types count.
     * The data following the reference types count should be ignored.
     * And debuggee vm should return a package with zero reference types count.
     */
    public void testInstanceCounts_Zero() {
        String thisTestName = "testInstanceCounts_Zero";
        
        if (!isCapability()) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }
        
        int refTypesCount = 0;
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Compose InstanceCounts commnad
        CommandPacket InstanceCountsCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.InstanceCountsCommand);
        
        InstanceCountsCommand.setNextValueAsInt(refTypesCount);
        InstanceCountsCommand.setNextValueAsReferenceTypeID(0);
        
        // Perform InstanceCounts command and attain reply package
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstanceCountsCommand);
        InstanceCountsCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if (errorCode != JDWPConstants.Error.NONE) {
            if (errorCode == JDWPConstants.Error.NOT_IMPLEMENTED) {
                logWriter.println("=> CHECK PASSED: Expected error (NOT_IMPLEMENTED) is returned");
                return;
            }
            else if(errorCode == JDWPConstants.Error.ILLEGAL_ARGUMENT) {
                logWriter.println("=> CHECK PASSED: Expected error (ILLEGAL_ARGUMENT) is returned");
                return;
            }
        }
        
        int returnedRefTypesCount = checkedReply.getNextValueAsInt();
        assertEquals(thisCommandName + "returned reference types count is wrong.",
                refTypesCount, returnedRefTypesCount, null, null);
        
        logWriter.println("=> CHECK: PASSED: expected reference types count is returned:");
        logWriter.println("=> Returned reference types count is " + returnedRefTypesCount);
        assertAllDataRead(checkedReply); 
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(InstanceCountsTest.class);

    }

}
