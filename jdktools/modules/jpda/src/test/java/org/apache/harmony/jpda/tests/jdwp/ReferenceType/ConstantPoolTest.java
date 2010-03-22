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

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.ConstantPool command.
 */
public class ConstantPoolTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    
    static final int testStatusFailed = -1;
    
    static final String thisCommandName = "ReferenceType.ConstantPool command";
    
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/ConstantPoolDebuggee;";
    
    static final String debuggeeClass = "org/apache/harmony/jpda/tests/jdwp/ReferenceType/ConstantPoolDebuggee.class";


    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.ConstantPoolDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.ConstantPool command.
     * <BR>The test starts ConstantPoolDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.ConstantPool command and checks that returned
     * constant entry count and costant pool bytes are expected.
     */
    public void testConstantPool001() {
        String thisTestName = "testConstantPool001";
        
        // Check capability, relevant for this test
        logWriter.println("=> Check capability: canGetConstantPool");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetConstantPool;
        if (!isCapability) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability: canGetConstantPool");
            return;
        }
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
   
        // Compose ConstantPool command 
        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket ConstantPoolCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.ConstantPoolCommand);
        ConstantPoolCommand.setNextValueAsReferenceTypeID(refTypeID);

        // Perform ConstantPool command and get reply package 
        ReplyPacket ConstantPoolReply = debuggeeWrapper.vmMirror.performCommand(ConstantPoolCommand);
        ConstantPoolCommand = null;
        checkReplyPacket(ConstantPoolReply, thisCommandName);
        
        // Attain entry count and constant pool byte size from reply package
        int returnedEntryCount = ConstantPoolReply.getNextValueAsInt();
        int returnedCpByteCount = ConstantPoolReply.getNextValueAsInt();

        // Attain entry count and constant pool content from class file
        
        // Read constant pool content from class file
        // length = magic num(4b) + major(2b) + minor(2b) + entry count(2b) + returnedCpByteCount
        int length = 10 + returnedCpByteCount;
        byte[] bytes = new byte[length];
        
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(debuggeeClass));
            int count = 0;
            int index = 0;
            while(length != 0){
                count = in.read(bytes,index,length);
                index += count;
                length -= count;
            }
        } catch (Exception e) {
            printErrorAndFail(thisCommandName + "has error in reading target class file!");
        }
        
        // Entry count is placed in byte 8 and byte 9 of class file
        short expectedEntryCount = (short)(bytes[8] << 8 | bytes[9]);
        
        // Compare entry count
        assertEquals(thisCommandName + "returned invalid entry count,", expectedEntryCount, returnedEntryCount, null, null);
        logWriter.println("=> CHECK: PASSED: expected entry count is returned:");
        logWriter.println("=> Signature = " + returnedEntryCount);

        int startIndex = 10;
        // Compare constant pool content
        for (int i = 0; i < returnedCpByteCount; i++){
            byte returnedCpByte = ConstantPoolReply.getNextValueAsByte();
            assertEquals(thisCommandName + "returned invalid entry count,", bytes[startIndex+i], returnedCpByte, null, null);
        }
        
        logWriter.println("=> CHECK: PASSED: expected constant pool bytes are returned:");
        logWriter.println("=> Constant Pool Byte Count = " + returnedCpByteCount);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");

        assertAllDataRead(ConstantPoolReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ConstantPoolTest.class);
    }
}
