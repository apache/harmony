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

package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import java.util.Random;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class ReferringObjectsTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;

    static final int testStatusFailed = -1;
    
    static int maxReferrers;

    static final String thisCommandName = "ObjectReference.ReferringObjects command";

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/ReferringObjectsDebuggee;";
   
    static final String referreeObjSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/ReferringObjectsReferree001;";
    
    static final String referrerObjSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ObjectReference/ReferringObjectsReferrer001;";
    
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ObjectReference.ReferringObjectsDebuggee";
    }

    // ReferringObjects need canGetInstanceInfo VM capability support
    private boolean isCapability() {
        // check capability, relevant for this test
        logWriter.println("=> Check capability: canGetInstanceInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetInstanceInfo;
        return isCapability;
    }
    
    /**
     * This testcase exercises ObjectReference.ReferringObjects command.
     * <BR>The test starts ReferringObjectsDebuggee class, requests referree objectID, 
     * for this class by ReferenceType.Instances command, then performs ObjectReference.ReferringObjects 
     * command and checks that returned instances are equal to the expected referrer objects. Since maxReferrers
     * equals zero, all instances are returned.
     */
    public void testReferringObjects_MaxReferrersIsZero() {
        maxReferrers = 0;
        DoTestReferringObjects();
    }
    
    /**
     * This testcase exercises ObjectReference.ReferringObjects command.
     * <BR>The test starts ReferringObjectsDebuggee class, requests referree objectID, 
     * for this class by ReferenceType.Instances command, then performs ObjectReference.ReferringObjects 
     * command and checks that returned instances are equal to the expected referrer objects. Since maxReferrers
     * is more than the number of referrer objects, all instances are returned.
     */
    public void testReferringObjects_MaxReferrersIsLarge() {
        maxReferrers = 20;
        DoTestReferringObjects();
    }
    
    /**
     * This testcase exercises ObjectReference.ReferringObjects command.
     * <BR>The test starts ReferringObjectsDebuggee class, requests referree objectID, 
     * for this class by ReferenceType.Instances command, then performs ObjectReference.ReferringObjects 
     * command and checks that returned instances are equal to the expected referrer objects. Since maxReferrers
     * is less than the number of referrer objects, the number of instances returned is equal to maxReferrers.
     */
    public void testReferringObjects_MaxReferrersIsSmall() {
        maxReferrers = 1;
        DoTestReferringObjects();
    }
    
    /**
     * This is the real body of the testcase which exercises ObjectReference.ReferringObjects command.
     * <BR>The test starts ReferringObjectsDebuggee class, requests referree objectID, 
     * for this class by ReferenceType.Instances command, then performs ObjectReference.ReferringObjects 
     * command and checks that returned instances are equal to the expected referrer objects.
     */
    public void DoTestReferringObjects() {
        String thisTestName = "testReferringObjects_Normal";
        
        if (!isCapability()) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Get the number of referrer objects in ReferringObjectsDebugee
        long debuggeeRefTypeID = getClassIDBySignature(debuggeeSignature);
        long referringObjNumID = debuggeeWrapper.vmMirror.getFieldID(
                debuggeeRefTypeID, "referringObjNum");
        long[] fieldIDs = new long[1];
        fieldIDs[0] = referringObjNumID;
        Value[] values = debuggeeWrapper.vmMirror.getReferenceTypeValues(
                debuggeeRefTypeID, fieldIDs);
        int expectedReferringObjNum = values[0].getIntValue();
        
        logWriter.println("=> ReferringObjNum in debuggee is: " + expectedReferringObjNum);

        if(maxReferrers > 0) {
            expectedReferringObjNum = (maxReferrers > expectedReferringObjNum) ? expectedReferringObjNum : maxReferrers;
        }
        
        // Compose Instances command to get referree objectID
        CommandPacket InstancesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.InstancesCommand);
        
        long referreeObjTypeID = getClassIDBySignature(referreeObjSignature);
        InstancesCommand.setNextValueAsReferenceTypeID(referreeObjTypeID);
        InstancesCommand.setNextValueAsInt(1);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(InstancesCommand);
        InstancesCommand = null;
        
        // Get the number of instances that returned. 
        int objNum = checkedReply.getNextValueAsInt();
        // Get the tagged-objectID
        byte tag = checkedReply.getNextValueAsByte();
        long objectID = checkedReply.getNextValueAsObjectID();
        
        // Compose ReferringObjects commnad
        CommandPacket ReferringObjectsCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferringObjectsCommand);
        
        ReferringObjectsCommand.setNextValueAsObjectID(objectID);
        ReferringObjectsCommand.setNextValueAsInt(maxReferrers);
        
        // Perform ReferringObjects command and attain reply package
        checkedReply = debuggeeWrapper.vmMirror
                .performCommand(ReferringObjectsCommand);
        ReferringObjectsCommand = null;
        
        // Get referrer objects numbers
        int referringObjects = checkedReply.getNextValueAsInt();
        assertEquals(thisCommandName + "returned instances number is wrong.", expectedReferringObjNum, referringObjects,null,null);
   
        long referrerTypeID = getClassIDBySignature(referrerObjSignature);
        long referrerFieldID = debuggeeWrapper.vmMirror.getFieldID(
                referrerTypeID, "isReferrer");
        
        // Check the returned objects are referrer objects
        for (int i = 0; i < referringObjects; i++) {
            //Get the tagged-objectID
            tag = checkedReply.getNextValueAsByte();
            assertEquals(thisCommandName
                    + "returned object tag is invalid.", 'L', tag, null, null);
            
            objectID = checkedReply.getNextValueAsObjectID();
            logWriter.println("=> ObjectID is: " + objectID);
            values = debuggeeWrapper.vmMirror.getObjectReferenceValues(
                    objectID, new long[] { referrerFieldID });
            boolean isReferrer = values[0].getBooleanValue();
            if (!isReferrer) {
                printErrorAndFail(thisCommandName
                        + "returned object is not a referrer which references this object.");
            }
        }
        logWriter.println("=> CHECK: PASSED: expected instances are returned:");
        logWriter.println("=> Returned referringObjects number is" + referringObjects);
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        assertAllDataRead(checkedReply);
    }
    
    /**
     * This testcase exercises ObjectReference.ReferringObjects command. <BR>
     * Compose a ReferringObjects command with negative maxReferrers
     * The vm should throw a ILLEGAL_ARGUMENT exception.
     */
    public void testReferringObjects_IllegalArgument() {
        String thisTestName = "testReferringObjects_IllegalArgument";
        
        if (!isCapability()) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canGetInstanceInfo");
            return;
        }
        
        int maxReferrers = -1;
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Compose Instances command to get referree objectID
        CommandPacket InstancesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.InstancesCommand);
        
        long referreeObjTypeID = getClassIDBySignature(referreeObjSignature);
        InstancesCommand.setNextValueAsReferenceTypeID(referreeObjTypeID);
        InstancesCommand.setNextValueAsInt(1);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(InstancesCommand);
        InstancesCommand = null;
        
        // Get the number of instances that returned. 
        int objNum = checkedReply.getNextValueAsInt();
        // Get the tagged-objectID
        byte tag = checkedReply.getNextValueAsByte();
        long objectID = checkedReply.getNextValueAsObjectID();
        
        // Compose ReferringObjects commnad
        CommandPacket ReferringObjectsCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferringObjectsCommand);
        
        ReferringObjectsCommand.setNextValueAsObjectID(objectID);
        ReferringObjectsCommand.setNextValueAsInt(maxReferrers);
        
        // Perform ReferringObjects command and attain reply package
        checkedReply = debuggeeWrapper.vmMirror
                .performCommand(ReferringObjectsCommand);
        ReferringObjectsCommand = null;

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
        printErrorAndFail(thisCommandName + " should throw ILLEGAL_ARGUMENT exception when maxReferrers is negative.");
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReferringObjectsTest.class);
    }

}
