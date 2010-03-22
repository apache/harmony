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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 03.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.AllClasses command.
 */
public class AllClassesTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.AllClasses command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.AllClasses command and checks that:
     * <BR>&nbsp;&nbsp; - number of reference types returned by command has
     *                    non-zero value;
     * <BR>&nbsp;&nbsp; - there are no classes with the 'ARRAY' or
     *                    'PRIMITIVE' bits in the status flag;
     */
    public void testAllClasses002() {
        logWriter.println("==> testAllClasses002: START...");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("==> Send VirtualMachine::AllClasses command...");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.AllClassesCommand);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::AllClasses command");

        long typeID;   
        String signature;   
        int status;

        int classes = reply.getNextValueAsInt();
        assertTrue(classes > 0);

        int count = 0;
        for (int i = 0; i < classes; i++) {

            reply.getNextValueAsByte();
            typeID = reply.getNextValueAsReferenceTypeID();
            signature = reply.getNextValueAsString();
            status = reply.getNextValueAsInt();

            if ( (status & JDWPConstants.ClassStatus.ARRAY) != 0 ){
                logWriter.println("## FAILURE: Unexpected status is returned:");
                logWriter.println("##          ReferenceTypeId = " + typeID);
                logWriter.println("##          Class signature: " + signature);
                logWriter.println("##          Class status = 0x" + Integer.toHexString(status) 
                    + "(" + JDWPConstants.ClassStatus.getName(status)+ ")");
                logWriter.println("##          Status \"0x"
                        + Integer.toHexString(JDWPConstants.ClassStatus.ARRAY) 
                        + "("
                        + JDWPConstants.ClassStatus.getName(JDWPConstants.ClassStatus.ARRAY)
                        + ")\" must NOT be returned!");
                count++;
            }
            if ( (status & JDWPConstants.ClassStatus.PRIMITIVE) != 0 ){
                logWriter.println("## FAILURE: Unexpected status is returned:");
                logWriter.println("##          ReferenceTypeId = " + typeID);
                logWriter.println("##          Class signature: " + signature);
                logWriter.println("##          Class status = 0x" + Integer.toHexString(status) 
                    + "(" + JDWPConstants.ClassStatus.getName(status)+ ")");
                logWriter.println("##          Status \"0x"
                        + Integer.toHexString(JDWPConstants.ClassStatus.PRIMITIVE) 
                        + "("
                        + JDWPConstants.ClassStatus.getName(JDWPConstants.ClassStatus.PRIMITIVE)
                        + ")\" must NOT be returned!");
                count++;
            }
        }
        assertEquals("count must be 0", 0, count);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> testAllClasses002: OK.");
    }

    /**
     * This testcase exercises VirtualMachine.AllClasses command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.AllClasses command and checks that:
     * <BR>&nbsp;&nbsp; - number of reference types returned by command has
     * non-zero value;
     * <BR>&nbsp;&nbsp; - refTypeTag takes one of the TypeTag constants:
     *                    'CLASS', 'INTERFACE', 'ARRAY';
     * <BR>&nbsp;&nbsp; - length of the signature string is not zero and starts with 'L' or
     *                    '[' symbols;
     * <BR>&nbsp;&nbsp; - signature of at least one class contains the "HelloWorld" string;
     * <BR>&nbsp;&nbsp; - All data were read from reply packet;
     */
    public void testAllClasses001() {
        logWriter.println("==> testAllClasses001: START...");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("==> Send VirtualMachine::AllClasses command...");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.AllClassesCommand);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::AllClasses command");

        byte refTypeTag;
        String refTypeTagName;
        long typeID;   
        String signature;   
        int status;
        String msgLine;
        boolean flagForHelloWorld = false;

        int classes = reply.getNextValueAsInt();
        logWriter.println("==> Number of reference types = " + classes);
        assertTrue(classes > 0);
        
        int printBound_1 = classes;
        int printBound_2 = 0;
        if ( classes > 50 ) {
            printBound_1 = 5;
            printBound_2 = classes - 5;
        }
        for (int i = 0; i < classes; i++) {

            boolean infoIsPrinted = false;
            refTypeTag = reply.getNextValueAsByte();
            try {
                refTypeTagName = JDWPConstants.TypeTag.getName(refTypeTag);
            } catch ( Throwable thrown ) {
                refTypeTagName = "UnknownTagName";
            }
            msgLine = "\n" + i + ". " + refTypeTagName;
            typeID = reply.getNextValueAsReferenceTypeID();
            signature = reply.getNextValueAsString();
            msgLine = msgLine + ": " + signature;
            status = reply.getNextValueAsInt();
            msgLine = msgLine + " " + JDWPConstants.ClassStatus.getName(status);
            if ( (i < printBound_1) || (i >= printBound_2) ) {
                logWriter.println(msgLine);
                logWriter.println("\treferenceTypeID = " + typeID);
                logWriter.println("\trefTypeTag = " + refTypeTagName
                        + "(" + refTypeTag + ")");
                logWriter.println("\tsignature = " + signature);
                if ( i == (printBound_1-1) ) {
                    logWriter.println("...\n...\n...");
                }
                infoIsPrinted = true;
            }
            
            try {
                assertTrue(refTypeTag == JDWPConstants.TypeTag.ARRAY
                        || refTypeTag == JDWPConstants.TypeTag.CLASS
                        || refTypeTag == JDWPConstants.TypeTag.INTERFACE);

                assertTrue(signature.length() > 0);
                assertTrue(signature.toCharArray()[0] == 'L' 
                        || signature.toCharArray()[0] == '[');
            } catch ( Throwable thrown) {
                // some assert is caught
                if ( !infoIsPrinted ) {
                    logWriter.println(msgLine);
                    logWriter.println("\treferenceTypeID = " + typeID);
                    logWriter.println("\trefTypeTag = " + refTypeTagName
                            + "(" + refTypeTag + ")");
                    logWriter.println("\tsignature = " + signature );
                }
                logWriter.println("## FAILURE is found out for this referenceType!\n");
                assertTrue(refTypeTag == JDWPConstants.TypeTag.ARRAY
                        || refTypeTag == JDWPConstants.TypeTag.CLASS
                        || refTypeTag == JDWPConstants.TypeTag.INTERFACE);

                assertTrue(signature.length() > 0);
                assertTrue(signature.toCharArray()[0] == 'L' 
                        || signature.toCharArray()[0] == '[');
            }

            if (signature.indexOf("HelloWorld") != -1)
                flagForHelloWorld = true;
        }
        assertAllDataRead(reply);
        assertTrue("HelloWorld has not been found in signatures of returned reference types",
                flagForHelloWorld);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> testAllClasses001: OK.");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllClassesTest.class);
    }
}
