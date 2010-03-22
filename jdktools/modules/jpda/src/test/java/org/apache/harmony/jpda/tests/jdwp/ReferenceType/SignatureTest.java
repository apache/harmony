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
 * Created on 16.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.Signature command.
 */
public class SignatureTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.Signature command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/HelloWorld;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises ReferenceType.Signature command.
     * <BR>The test starts HelloWorld debuggee, requests referenceTypeId
     * for it by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Signature command and checks that returned
     * signature is equal to expected signature:
     * <BR>'Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/HelloWorld;'
     */
    public void testSignature001() {
        String thisTestName = "testSignature001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK1: send " + thisCommandName + " and check reply...");

        CommandPacket signatureCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        signatureCommand.setNextValueAsReferenceTypeID(refTypeID);

        ReplyPacket signatureReply = debuggeeWrapper.vmMirror.performCommand(signatureCommand);
        signatureCommand = null;
        checkReplyPacket(signatureReply, thisCommandName);

        String returnedSignature = signatureReply.getNextValueAsString();

        if ( ! debuggeeSignature.equals(returnedSignature) ) {
            printErrorAndFail(thisCommandName + " returned invalid signature" +
                    ", Expected = " + debuggeeSignature +
                    ", Returned = " + returnedSignature);
        } else {
            logWriter.println("=> CHECK1: PASSED: expected signature is returned = "
                    + returnedSignature);
        }
        assertAllDataRead(signatureReply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SignatureTest.class);
    }
}
