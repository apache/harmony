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

package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class InstancesTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;

    static final int testStatusFailed = -1;
    
    static int maxInstances;

    static final String thisCommandName = "ReferenceType.Instances command";
    
    static String thisTestName;

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/InstancesDebuggee;";

    static final String mockClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/MockClass;";

    static final String stringSignature = "Ljava/lang/String;";

    static final String intArraySignature = "[I";
    @Override
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.InstancesDebuggee";
    }

    /**
     * All test cases is based on this process. <BR>
     * It starts InstancesDebuggee class, requests referenceTypeId for
     * MockClass class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command and checks that returned reachable
     * objects are expected ones.
     */
    private void runTestInstances() {
        // check capability, relevant for this test
        logWriter.println("=> Check capability: canGetInstanceInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetInstanceInfo;
        if (!isCapability) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long mockClassRefTypeID = getClassIDBySignature(mockClassSignature);
        long debuggeeRefTypeID = getClassIDBySignature(debuggeeSignature);

        //Get the number of reachable objects in debuggee class
        long reachableObjNumID = debuggeeWrapper.vmMirror.getFieldID(
                debuggeeRefTypeID, "reachableObjNum");
        long[] fieldIDs = new long[1];
        fieldIDs[0] = reachableObjNumID;

        Value[] values = debuggeeWrapper.vmMirror.getReferenceTypeValues(
                debuggeeRefTypeID, fieldIDs);
        int expectedReachableObjNum = values[0].getIntValue();
        
        logWriter.println("=> ReachableObjNum in debuggee is: " + expectedReachableObjNum);
        
        //maxInstances is maximum number of instances to return. 
        //So expectedReachableObjNum should be less than maxInstances
        if (expectedReachableObjNum > maxInstances && maxInstances > 0) {
            expectedReachableObjNum = maxInstances;
        }

        logWriter.println("=> CHECK: send " + thisCommandName
                + " and check reply for ERROR...");

        CommandPacket InstancesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.InstancesCommand);
        InstancesCommand.setNextValueAsReferenceTypeID(mockClassRefTypeID);
        InstancesCommand.setNextValueAsInt(maxInstances);

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstancesCommand);
        InstancesCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if (errorCode != JDWPConstants.Error.NONE) {
            if (errorCode == JDWPConstants.Error.NOT_IMPLEMENTED) {
                logWriter
                        .println("=> CHECK PASSED: Expected error (NOT_IMPLEMENTED) is returned");
                return;
            }
            else if(errorCode == JDWPConstants.Error.ILLEGAL_ARGUMENT) {
                logWriter
                        .println("=> CHECK PASSED: Expected error (ILLEGAL_ARGUMENT) is returned");
                return;
            }

        }

        //Get the number of instances that returned. 
        int reachableInstancesNum = checkedReply.getNextValueAsInt();
        assertEquals(thisCommandName + "returned instances number is wrong.",
                expectedReachableObjNum, reachableInstancesNum, null, null);

        long mockClassFieldID = debuggeeWrapper.vmMirror.getFieldID(
                mockClassRefTypeID, "isReachable");
        for (int i = 0; i < reachableInstancesNum; i++) {
            //Get the tagged-objectID
            byte tag = checkedReply.getNextValueAsByte();
            assertEquals(thisCommandName
                    + "returned object tag is invalid.", 'L', tag, null, null);
            
            long objectID = checkedReply.getNextValueAsObjectID();
            logWriter.println("=> ObjectID is: " + objectID);
            values = debuggeeWrapper.vmMirror.getObjectReferenceValues(
                    objectID, new long[] { mockClassFieldID });
            boolean isReachable = values[0].getBooleanValue();
            if (!isReachable) {
                printErrorAndFail(thisCommandName
                        + "returned object is not reachable.");
            }
        }
        logWriter.println("=> CHECK: PASSED: expected instances are returned:");
        logWriter.println("=> Returned reachable instances number is" + expectedReachableObjNum);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        assertAllDataRead(checkedReply);

    }
    
    /**
     * This testcase exercises ReferenceType.Instances command. <BR>
     * The test starts InstancesDebuggee class, requests referenceTypeId for
     * MockClass class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command and checks that returned reachable
     * objects are expected ones. Maximum number of instances is zero, so all instances 
     * are returned.
     */
    public void testInstances001() {
        thisTestName = "testInstances001";
        maxInstances = 0;
        runTestInstances();
    }
    
    /**
     * This testcase exercises ReferenceType.Instances command. <BR>
     * The test starts InstancesDebuggee class, requests referenceTypeId for
     * MockClass class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command. Since maximum number of instances is negtive, so  
     * ILLEGAL_ARGUMENT exception are replied.
     */
    public void testInstances002() {
        thisTestName = "testInstances002";
        maxInstances = -1;
        runTestInstances();
    }
    
    /**
     * This testcase exercises ReferenceType.Instances command. <BR>
     * The test starts InstancesDebuggee class, requests referenceTypeId for
     * MockClass class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command and checks that returned reachable
     * objects are expected ones. Maximum number of instances is more than the reachable
     * objects of this reference type, so all instances are returned.
     */
    public void testInstances003() {
        thisTestName = "testInstances003";
        maxInstances = 20;
        runTestInstances();
    }
    
    /**
     * This testcase exercises ReferenceType.Instances command. <BR>
     * The test starts InstancesDebuggee class, requests referenceTypeId for
     * MockClass class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command and checks that returned reachable
     * objects are expected ones. Maximum number of instances is less than the reachable
     * objects of this reference type, so maximum number of instances are returned.
     */
    public void testInstances004() {
        thisTestName = "testInstances004";
        maxInstances = 1;
        runTestInstances();
    }
    
    /**
     * It starts InstancesDebuggee class, requests referenceTypeId for String
     * class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command and checks that returned reachable String
     * objects are expected.
     */
    public void testInstances_String() {
        String thisTestName = "testInstances_String";

        // check capability, relevant for this test
        logWriter.println("=> Check capability: canGetInstanceInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetInstanceInfo;
        if (!isCapability) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long stringRefTypeID = getClassIDBySignature(stringSignature);
        maxInstances = 10;

        logWriter.println("=> CHECK: send " + thisCommandName
                + " and check reply for ERROR...");

        CommandPacket InstancesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.InstancesCommand);
        InstancesCommand.setNextValueAsReferenceTypeID(stringRefTypeID);
        InstancesCommand.setNextValueAsInt(maxInstances);

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstancesCommand);
        InstancesCommand = null;
        checkReplyPacket(checkedReply, thisTestName);

        // Get the number of instances that returned.
        int reachableInstancesNum = checkedReply.getNextValueAsInt();

        for (int i = 0; i < reachableInstancesNum; i++) {
            // Get the tagged-objectID
            byte tag = checkedReply.getNextValueAsByte();
            assertEquals(thisCommandName + "returned String tag is invalid.",
                    's', tag, null, null);
            long objectID = checkedReply.getNextValueAsObjectID();
            logWriter.println("=> ObjectID is: " + objectID);

        }
        logWriter.println("=> CHECK: PASSED: expected instances are returned:");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": FINISH");
        assertAllDataRead(checkedReply);

    }

    /**
     * It starts InstancesDebuggee class, requests referenceTypeId for Array
     * class by VirtualMachine.ClassesBySignature command, then performs
     * ReferenceType.Instances command and checks that returned reachable Array
     * objects are expected.
     */
    public void testInstances_Array() {
        String thisTestName = "testInstances_Array";

        // check capability, relevant for this test
        logWriter.println("=> Check capability: canGetInstanceInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetInstanceInfo;
        if (!isCapability) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long intArrayRefTypeID = getClassIDBySignature(intArraySignature);
        maxInstances = 10;

        logWriter.println("=> CHECK: send " + thisCommandName
                + " and check reply for ERROR...");

        CommandPacket InstancesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.InstancesCommand);
        InstancesCommand.setNextValueAsReferenceTypeID(intArrayRefTypeID);
        InstancesCommand.setNextValueAsInt(maxInstances);

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstancesCommand);
        InstancesCommand = null;
        checkReplyPacket(checkedReply, thisTestName);

        // Get the number of instances that returned.
        int reachableInstancesNum = checkedReply.getNextValueAsInt();

        for (int i = 0; i < reachableInstancesNum; i++) {
            // Get the tagged-objectID
            byte tag = checkedReply.getNextValueAsByte();
            assertEquals(thisCommandName + "returned Array tag is invalid.",
                    '[', tag, null, null);
            long objectID = checkedReply.getNextValueAsObjectID();
            logWriter.println("=> ObjectID is: " + objectID);

        }
        logWriter.println("=> CHECK: PASSED: expected instances are returned:");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": FINISH");
        assertAllDataRead(checkedReply);

    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InstancesTest.class);

    }

}
