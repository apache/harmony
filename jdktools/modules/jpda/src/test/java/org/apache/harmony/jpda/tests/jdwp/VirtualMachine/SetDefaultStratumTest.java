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
 * Created on 24.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for VirtualMachine.SetDefaultStratum command.
 */
public class SetDefaultStratumTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "VirtualMachine::SetDefaultStratum command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/HelloWorld;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.SetDefaultStratum command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR>Then the test checks that VirtualMachine.SetDefaultStratum command runs
     * without any error or returns NOT_IMPLEMENTED error.
     * Any other error is considered as test' failure.
     */
    public void testSetDefaultStratum001() {
        String thisTestName = "testSetDefaultStratum001";
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canSetDefaultStratum");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canSetDefaultStratum;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canSetDefaultStratum");
            return;
        }
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("=> CHECK1: send " + thisCommandName + " and check reply for ERROR...");
        String stratumID = "C++";
        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SetDefaultStratumCommand);
        checkedCommand.setNextValueAsString(stratumID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        short errorCode = checkedReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            if ( errorCode != JDWPConstants.Error.NOT_IMPLEMENTED ) {
                printErrorAndFail(thisCommandName 
                    + " returns unexpected ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            } else { 
                logWriter.println("=> CHECK PASSED: Expected error (NOT_IMPLEMENTED) is returned");
            }
        } else {
            logWriter.println
            ("=> CHECK PASSED: No any error is received");
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetDefaultStratumTest.class);
    }
}
