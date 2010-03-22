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
 * Created on 24.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.Interfaces command.
 */
public class InterfacesTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.Interfaces command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/InterfacesDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.InterfacesDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.Interfaces command.
     * <BR>The test starts InterfacesDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Interfaces command and checks that returned
     * list of interfaces corresponds to expected list.
     */
    public void testInterfaces001() {
        String thisTestName = "testInterfaces001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String checkedClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/CheckedClass_Interfaces001;";
        long refTypeID = getClassIDBySignature(checkedClassSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> Checked class = org.apache.harmony.jpda.tests.jdwp.ReferenceType.CheckedClass_Interfaces001");
        logWriter.println("=> referenceTypeID for Checked class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " for Checked class and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
            JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
            JDWPCommands.ReferenceTypeCommandSet.InterfacesCommand);
        checkedCommand.setNextValueAsReferenceTypeID(refTypeID);
        ReplyPacket checkedReply = 
            debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);
        
        int returnedInterfacesNumber = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned interfaces number = " + returnedInterfacesNumber);
        
        String interfacesSignatures[] = {
                "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/Interface_1_Interfaces001;",
                "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/Interface_2_Interfaces001;",
        };

        boolean interfacesFound[] = {
                false,
                false,
        };
        
        int expectedInterfacesNumber = interfacesSignatures.length;

        logWriter.println("=> CHECK for all expected interfaces...");
        for (int i=0; i < returnedInterfacesNumber; i++) {
            logWriter.println("\n=> Check for returned interface[" + i + "] ...");
            long returnedInterfaceID = checkedReply.getNextValueAsReferenceTypeID();
            logWriter.println("=> RefTypeID of interface = " + returnedInterfaceID);
            logWriter.println("=> Get signature for interface...");

            CommandPacket signatureCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
            signatureCommand.setNextValueAsReferenceTypeID(returnedInterfaceID);
            ReplyPacket signatureReply = 
                debuggeeWrapper.vmMirror.performCommand(signatureCommand);
            signatureCommand = null;
            checkReplyPacket(signatureReply, "ReferenceType::Signature command");

            String returnedSignature = signatureReply.getNextValueAsString();
            logWriter.println("=> Signature of interface = " + returnedSignature);
            signatureReply = null;
            logWriter.println("=> Signature of interface = " + returnedSignature);

            int k = 0;
            for (; k < expectedInterfacesNumber; k++) {
                if ( ! interfacesSignatures[k].equals(returnedSignature)) {
                    continue;
                }
                if ( interfacesFound[k] ) {
                    logWriter.println("\n## FAILURE: This interface is found repeatedly in the list");
                    fail("This interface is found repeatedly in the list");
                    break;
                }
                interfacesFound[k] = true;
                break;
            }
            if ( k == expectedInterfacesNumber ) {
                // returned interface is not found out in the list of expected interfaces
                logWriter.println("\n## FAILURE: It is unexpected interface");
                fail("It is unexpected interface");
            }
        }
        for (int k=0; k < expectedInterfacesNumber; k++) {
            if ( ! interfacesFound[k] ) {
                logWriter.println
                ("\n## FAILURE: Expected interface is NOT found in the returned list:");
                logWriter.println("=> Signature of interface = " +
                        interfacesSignatures[k]);
                fail("Expected interface is NOT found in the returned list: " +
                        interfacesSignatures[k]);
            }
        }

        assertAllDataRead(checkedReply);

        logWriter.println
        ("\n=> CHECK PASSED: All expected interfaces are found out");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("\n==> " + thisTestName + " for " + thisCommandName + ": OK.");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterfacesTest.class);
    }
}
