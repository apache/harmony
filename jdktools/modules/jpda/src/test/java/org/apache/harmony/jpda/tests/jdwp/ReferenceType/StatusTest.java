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
 * JDWP Unit test for ReferenceType.Status command.
 */
public class StatusTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.Status command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/StatusDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.StatusDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.Status command.
     * <BR>The test starts StatusDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Status command and checks that returned
     * status' bits are expected bits.
     */
    public void testStatus001() {
        String thisTestName = "testStatus001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.StatusCommand);
        checkedCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);
        
        int returnedStatus = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned status value = 0x" + Integer.toHexString(returnedStatus));
        
        String statusBitsNames[] = {
                "VERIFIED",
                "PREPARED",
                "INITIALIZED",
                "ERROR",
        };

        int statusBitsValues[] = {
                0x01,
                0x02,
                0x04,
                0x08,
        };
        
        int expectedStatusBits[] = {
                0x01,
                0x02,
                0x04,
                0x00,
        };

        String failMessage = "";
        int checkedStatusBitsNumber = statusBitsNames.length;
        logWriter.println("=> CHECK for all returned bits of status...");
        for (int i = 0; i < checkedStatusBitsNumber; i++) {
            int returnedStatusBit = returnedStatus & statusBitsValues[i];
            if ( expectedStatusBits[i] != returnedStatusBit ) {
                logWriter.println("\n## FAILURE: " + thisCommandName
                        + " returns unexpected value for status bit \"" + statusBitsNames[i] + "\"");
                logWriter.println
                ("## Expected status bit = 0x" + Integer.toHexString(expectedStatusBits[i]));
                logWriter.println
                ("## Returned status bit = 0x" + Integer.toHexString(returnedStatusBit));
                failMessage = failMessage +
                    thisCommandName
                    + " returns unexpected value for status bit \"" + statusBitsNames[i] + "\"" +
                    ", Expected = 0x" + Integer.toHexString(expectedStatusBits[i]) +
                    ", Returned = 0x" + Integer.toHexString(returnedStatusBit) + "; ";
            } else {
                logWriter.println("\n=> Expected value for status bit \"" + statusBitsNames[i]
                    + "\" (0x" + Integer.toHexString(statusBitsValues[i]) + ") is returned: 0x"
                    + Integer.toHexString(returnedStatusBit));
            }
        }

        if (failMessage.length() == 0) {
            logWriter.println
            ("\n=> CHECK: PASSED: returned status value contains all expected status' bits");
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");

        if (failMessage.length() > 0) {
            fail(failMessage);
        }
        
        assertAllDataRead(checkedReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StatusTest.class);
    }
}
