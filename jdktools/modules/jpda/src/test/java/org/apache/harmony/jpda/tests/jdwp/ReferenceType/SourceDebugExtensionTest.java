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
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.SourceDebugExtension command.
 */
public class SourceDebugExtensionTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.SourceDebugExtension command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/SourceDebugExtensionDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.SourceDebugExtensionDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.SourceDebugExtension command.
     * <BR>The test starts SourceDebugExtensionDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.SourceDebugExtension command and checks that 
     * no any unexpected ERROR is returned.
     */
    public void testSourceDebugExtension001() {
        String thisTestName = "testSourceDebugExtension001";
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canGetSourceDebugExtension");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetSourceDebugExtension;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canGetSourceDebugExtension");
            return;
        }
                
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SourceDebugExtensionCommand);
        checkedCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        short errorCode = checkedReply.getErrorCode();

        switch ( errorCode ) {
            case JDWPConstants.Error.NONE:
                logWriter.println("=> No any ERROR is returned");
                String SourceDebugExtension = checkedReply.getNextValueAsString();
                logWriter.println("=> Returned SourceDebugExtension = " + SourceDebugExtension);
                break;
            case JDWPConstants.Error.NOT_IMPLEMENTED:
                logWriter.println("=> ERROR is returned: "+ errorCode
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                logWriter.println("=> It is possible ERROR");
                break;
            case JDWPConstants.Error.ABSENT_INFORMATION:
                logWriter.println("=> ERROR is returned: "+ errorCode
                        + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                    logWriter.println("=> It is possible ERROR");
                break;
            default:
                logWriter.println("\n## FAILURE: " + thisCommandName + " returns unexpected ERROR = "
                    + errorCode + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                fail(thisCommandName + " returns unexpected ERROR = "
                    + errorCode + "(" + JDWPConstants.Error.getName(errorCode) + ")");
        }

        assertAllDataRead(checkedReply);

        logWriter.println("=> CHECK PASSED: No any unexpected ERROR is returned");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SourceDebugExtensionTest.class);
    }
}
