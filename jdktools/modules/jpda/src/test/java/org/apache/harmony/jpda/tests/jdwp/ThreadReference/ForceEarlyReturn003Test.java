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

package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class ForceEarlyReturn003Test extends JDWPSyncTestCase {

    static final String thisCommandName = "ThreadReference.ForceEarlyReturn command ";

    static final float EXPECTED_FLOAT = 0.5f;

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.ForceEarlyReturnDebuggee";
    }

    // ForceEarlyReturn needs canForceEarlyReturn VM capability support
    private boolean isCapability() {
        // check capability, relevant for this test
        logWriter.println("=> Check capability: canForceEarlyReturn");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canForceEarlyReturn;
        return isCapability;
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Float value is returned.
     */
    public void testForceEarlyReturn_ReturnFloat() {
        String thisTestName = "testForceEarlyReturn_ReturnFloat";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        if (!isCapability()) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability:canForceEarlyReturn");
            return;
        }
        // Tell debuggee to start a thread named THREAD_FLOAT
        synchronizer.sendMessage(ForceEarlyReturnDebuggee.THREAD_FLOAT);

        // Wait until the func_FLOAT is processing.
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Getting ID of the tested thread
        logWriter.println("==> testedThreadName = "
                + ForceEarlyReturnDebuggee.THREAD_FLOAT);
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = debuggeeWrapper.vmMirror
                .getThreadID(ForceEarlyReturnDebuggee.THREAD_FLOAT);
        logWriter.println("==> Get testedThreadID is" + testedThreadID);

        // Suspend the VM before perform command
        logWriter.println("==> testedThreadID = " + testedThreadID);
        logWriter.println("==> suspend testedThread...");
        debuggeeWrapper.vmMirror.suspendThread(testedThreadID);

        // Compose the ForceEarlyReturn command
        CommandPacket forceEarlyReturnPacket = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.ForceEarlyReturnCommand);
        forceEarlyReturnPacket.setNextValueAsThreadID(testedThreadID);
        forceEarlyReturnPacket.setNextValueAsValue(new Value(EXPECTED_FLOAT));

        // Perform the command
        logWriter.println("==> Perform " + thisCommandName);
        ReplyPacket forceEarlyReturnReply = debuggeeWrapper.vmMirror
                .performCommand(forceEarlyReturnPacket);
        forceEarlyReturnPacket = null;

        checkReplyPacket(forceEarlyReturnReply,
                "ThreadReference::ForceEarlyReturn command");

        // Resume the thread
        logWriter.println("==> testedThreadID = " + testedThreadID);
        logWriter.println("==> resume testedThread...");
        debuggeeWrapper.vmMirror.resumeThread(testedThreadID);

        String actualValue = synchronizer.receiveMessage();
        // Check the early returned value
        if (!actualValue.equals(new Float(EXPECTED_FLOAT).toString())) {
            printErrorAndFail(thisCommandName
                    + "returned value is not set by ForceEarlyReturn command"
                    + " expected:<" + EXPECTED_FLOAT + "> but was:<"
                    + actualValue + ">");
        }
        logWriter
                .println("==> CHECK: PASSED: returned value does set by ForceEarlyReturn command.");
        logWriter.println("==> Returned value: " + actualValue);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ForceEarlyReturn003Test.class);
    }

}
