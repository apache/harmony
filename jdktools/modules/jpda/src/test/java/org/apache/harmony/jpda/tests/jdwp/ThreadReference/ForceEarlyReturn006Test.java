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
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants.Tag;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class ForceEarlyReturn006Test extends JDWPSyncTestCase {

    static final String thisCommandName = "ThreadReference.ForceEarlyReturn command ";

    static final String testObjSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ThreadReference/TestObject;";

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

    // Get the objectID of test thread
    private long getObjectID() {
        // Compose Instances command to get tested thread objectID
        CommandPacket InstancesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.InstancesCommand);

        long testThreadTypeID = getClassIDBySignature(testObjSignature);
        InstancesCommand.setNextValueAsReferenceTypeID(testThreadTypeID);
        InstancesCommand.setNextValueAsInt(1);

        ReplyPacket checkedReply = debuggeeWrapper.vmMirror
                .performCommand(InstancesCommand);
        InstancesCommand = null;

        // Get the number of instances that returned.
        int objNum = checkedReply.getNextValueAsInt();
        // Get the tagged-objectID
        byte tag = checkedReply.getNextValueAsByte();
        long objectID = checkedReply.getNextValueAsObjectID();
        return objectID;
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Object value is returned.
     */
    public void testForceEarlyReturn_ReturnObject() {
        String thisTestName = "testForceEarlyReturn_ReturnObject";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        if (!isCapability()) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability:canForceEarlyReturn");
            return;
        }

        // Get test object id
        long testObjID = getObjectID();
        logWriter.println("==> test object id is: " + testObjID);

        // Tell debuggee to start a thread named THREAD_OBJECT
        synchronizer.sendMessage(ForceEarlyReturnDebuggee.THREAD_OBJECT);

        // Wait until the func_Object is processing.
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Getting ID of the tested thread
        logWriter.println("==> testedThreadName = "
                + ForceEarlyReturnDebuggee.THREAD_OBJECT);
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = debuggeeWrapper.vmMirror
                .getThreadID(ForceEarlyReturnDebuggee.THREAD_OBJECT);
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
        forceEarlyReturnPacket.setNextValueAsValue(new Value(Tag.OBJECT_TAG,
                testObjID));

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

        String isTestObj = synchronizer.receiveMessage();
        // Check the early returned value
        if (!isTestObj.equals("TRUE")) {
            printErrorAndFail(thisCommandName
                    + "returned value is not test object set by ForceEarlyReturn command");
        }
        logWriter
                .println("==> CHECK: PASSED: returned value does set by ForceEarlyReturn command.");
        logWriter.println("==> Returned value: " + "void");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ForceEarlyReturn006Test.class);
    }

}
