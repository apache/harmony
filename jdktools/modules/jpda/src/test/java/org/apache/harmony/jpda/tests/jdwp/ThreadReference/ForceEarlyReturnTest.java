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

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants.Tag;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for ThreadReference.ForceEarlyReturn command.
 */
public class ForceEarlyReturnTest extends JDWPSyncTestCase {

    static final String thisCommandName = "ThreadReference.ForceEarlyReturn command ";

    static String thisTestName;

    static String testThreadName;

    static final int EXPECTED_INT = 5;

    static final short EXPECTED_SHORT = 20;

    static final char EXPECTED_CHAR = 'A';

    static final byte EXPECTED_BYTE = 30;

    static final boolean EXPECTED_BOOLEAN = true;

    static Value expectedValue;

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ThreadReference/ForceEarlyReturnDebuggee;";

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

    private String toString(Value value) {

        switch (value.getTag()) {
        case JDWPConstants.Tag.BOOLEAN_TAG:
            return (new Boolean(value.getBooleanValue())).toString();
        case JDWPConstants.Tag.BYTE_TAG:
            return (new Byte(value.getByteValue())).toString();
        case JDWPConstants.Tag.CHAR_TAG:
            return (new Character(value.getCharValue())).toString();
        case JDWPConstants.Tag.INT_TAG:
            return (new Integer(value.getIntValue())).toString();
        case JDWPConstants.Tag.SHORT_TAG:
            return (new Short(value.getShortValue())).toString();
        }
        return "";
    }

    public void RunTestForceEarlyReturn() {
        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        if (!isCapability()) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability: canForceEarlyReturn");
            return;
        }
        // Tell debuggee to start a thread
        synchronizer.sendMessage(testThreadName);

        // Wait until the func_Int is processing.
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Getting ID of the tested thread
        logWriter.println("==> testedThreadName = " + testThreadName);
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = debuggeeWrapper.vmMirror
                .getThreadID(testThreadName);
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
        forceEarlyReturnPacket.setNextValueAsValue(expectedValue);

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
        if (!actualValue.equals(toString(expectedValue))) {
            printErrorAndFail(thisCommandName
                    + "returned value is not set by ForceEarlyReturn command"
                    + " expected:<" + expectedValue.toString() + "> but was:<"
                    + actualValue + ">");
        }
        logWriter
                .println("==> CHECK: PASSED: returned value does set by ForceEarlyReturn command.");
        logWriter.println("==> Returned value: " + actualValue);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Int value is returned.
     */
    public void testForceEarlyReturn_ReturnInt() {
        thisTestName = "testForceEarlyReturn_ReturnInt";
        testThreadName = ForceEarlyReturnDebuggee.THREAD_INT;
        expectedValue = new Value(EXPECTED_INT);
        RunTestForceEarlyReturn();
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Short value is returned.
     */
    public void testForceEarlyReturn_ReturnShort() {
        thisTestName = "testForceEarlyReturn_ReturnShort";
        testThreadName = ForceEarlyReturnDebuggee.THREAD_SHORT;
        expectedValue = new Value(EXPECTED_SHORT);
        RunTestForceEarlyReturn();
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Byte value is returned.
     */
    public void testForceEarlyReturn_ReturnByte() {
        thisTestName = "testForceEarlyReturn_ReturnByte";
        testThreadName = ForceEarlyReturnDebuggee.THREAD_BYTE;
        expectedValue = new Value(EXPECTED_BYTE);
        RunTestForceEarlyReturn();
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Char value is returned.
     */
    public void testForceEarlyReturn_ReturnChar() {
        thisTestName = "testForceEarlyReturn_ReturnChar";
        testThreadName = ForceEarlyReturnDebuggee.THREAD_CHAR;
        expectedValue = new Value(EXPECTED_CHAR);
        RunTestForceEarlyReturn();
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread and gets
     * the returned value of the called method. The returned value should be
     * equal to the value which is used in ForceEarlyReturn Command. In this
     * testcase, an Boolean value is returned.
     */
    public void testForceEarlyReturn_ReturnBoolean() {
        thisTestName = "testForceEarlyReturn_ReturnBoolean";
        testThreadName = ForceEarlyReturnDebuggee.THREAD_BOOLEAN;
        expectedValue = new Value(EXPECTED_BOOLEAN);
        RunTestForceEarlyReturn();
    }

    /**
     * This testcase exercises ThreadReference.ForceEarlyReturn command. <BR>
     * At first the test starts ForceEarlyReturnDebuggee and send it the thread
     * name through which to start a specific thread. Then the test performs the
     * ThreadReference.ForceEarlyReturn command for the tested thread without
     * suspending the VM. In this testcase, THREAD_NOT_SUSPENDED exception is returned.
     */
    public void testForceEarlyReturn_NotSuspended() {
        thisTestName = "testForceEarlyReturn_NotSuspended";
        testThreadName = "test";
        expectedValue = new Value(Tag.VOID_TAG, 0);

        logWriter.println("==> " + thisTestName + " for " + thisCommandName
                + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        if (!isCapability()) {
            logWriter
                    .println("##WARNING: this VM dosn't possess capability: canForceEarlyReturn");
            return;
        }
        // Tell debuggee to start a thread
        synchronizer.sendMessage(testThreadName);

        // Wait thread signal.
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Getting ID of the tested thread
        logWriter.println("==> testedThreadName = " + testThreadName);
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = debuggeeWrapper.vmMirror
                .getThreadID(testThreadName);
        logWriter.println("==> Get testedThreadID is" + testedThreadID);

        // Compose the ForceEarlyReturn command
        CommandPacket forceEarlyReturnPacket = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.ForceEarlyReturnCommand);
        forceEarlyReturnPacket.setNextValueAsThreadID(testedThreadID);
        forceEarlyReturnPacket.setNextValueAsValue(expectedValue);

        // Perform the command
        logWriter.println("==> Perform " + thisCommandName);
        ReplyPacket forceEarlyReturnReply = debuggeeWrapper.vmMirror
                .performCommand(forceEarlyReturnPacket);
        forceEarlyReturnPacket = null;

        short errorCode = forceEarlyReturnReply.getErrorCode();
        if (errorCode != JDWPConstants.Error.NONE) {
            if (errorCode == JDWPConstants.Error.THREAD_NOT_SUSPENDED) {
                logWriter
                        .println("=> CHECK PASSED: Expected error (THREAD_NOT_SUSPENDED) is returned");
                synchronizer.sendMessage("ThreadExit");
                synchronizer
                        .sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
                return;
            }
        }
        printErrorAndFail(thisCommandName
                + " should throw exception when VM is not suspended.");

    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ForceEarlyReturnTest.class);

    }

}
