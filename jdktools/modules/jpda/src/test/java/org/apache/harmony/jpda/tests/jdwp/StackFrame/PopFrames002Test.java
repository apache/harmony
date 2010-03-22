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
 * @author Aleksander V. Budniy
 */

/**
 * Created on 1.05.2006
 */
package org.apache.harmony.jpda.tests.jdwp.StackFrame;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for StackFrame.PopFrames command.
 */
public class PopFrames002Test extends JDWPStackFrameTestCase {

    private String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/StackFrame/PopFramesDebuggee;";

    private String breakpointMethodName = "nestledMethod4";

    private String methodToPop = "nestledMethod2";

    private long timeoutToWait = 2000;

    private long timeOfMethodInvocation = timeoutToWait * 5;

    private static final byte NUMBER_OF_FRAMES_TO_POP = 3;

    protected String getDebuggeeClassName() {
        return PopFramesDebuggee.class.getName();
    }

    /**
     * This testcase exercises StackFrame.PopFrames command to discard several frames at once.
     * <BR>The test starts PopFramesDebuggee class, sets a breakpoint
     * in 'nestledMethod4', stops at breakpoint and prints stack.
     * <BR>Then the test performs StackFrame.PopFrame command to discard several frames at once,
     * prints stack and checks that discarded frames are not returned 
     * by ThreadReference.Frames command. 
     * <BR>Then the test resumes debuggee and checks stop on the same breakpoint.
     */
    public void testPopSeveralFrames() {
        logWriter.println("==> testPopSeveralFrames started");
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canPopFrames");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canPopFrames;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canPopFrames");
            return;
        }

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // find checked method
        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());

        // long methodID = getMethodID(refTypeID, breakpointMethodName);
        logWriter.println("=> Set breakpoint at the beginning of " + breakpointMethodName);
        long requestID = debuggeeWrapper.vmMirror.setBreakpointAtMethodBegin(
                refTypeID, breakpointMethodName);

        // release debuggee
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // receive event in nestledMethod4
        logWriter.println("=> Wait for breakpoint in " + breakpointMethodName);
        long breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> breakpointThreadID = " + breakpointThreadID);

        // print stack frames
        logWriter.println("");
        logWriter.println("=> Get frames before PopFrames command, thread = " + breakpointThreadID);
        FrameInfo[] frameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);
        logWriter.println("=> Frames before popFrame");
        printStackFrame(frameInfos.length, frameInfos);
        logWriter.println("=> Number of frames before command: "
                + frameInfos.length);

        // find stack frame for popped method
        logWriter.println("");
        logWriter.println("=> Find frameID of method = " + methodToPop + " for PopFrames command");
        long frameID = 0;
        long methodID = getMethodID(refTypeID, methodToPop);
        if (methodID == -1) {
            logWriter.println("##FAILURE: error during getting methodID of " + methodToPop);
        }
        boolean isMethodFound = false;
        for (int j = 0; j < frameInfos.length; j++) {
            if (frameInfos[j].location.methodID == methodID) {
                frameID = frameInfos[j].getFrameID();
                isMethodFound = true;
                break;
            }
        }
        if (!isMethodFound) {
            logWriter
                    .println("##FAILURE: there is no frame for checked method");
            fail("There is no frame for checked method");
        }

        logWriter.println("=> frameID for PopFrames command = " + frameID);
        
        // pop stack frames
        logWriter.println("=> Pop " + NUMBER_OF_FRAMES_TO_POP + " frames at once");

        logWriter.println("");
        logWriter.println("=> Perform PopFrames command for method = " + methodToPop + " with frameID = " + frameID);
        jdwpPopFrames(breakpointThreadID, frameID);

        logWriter.println("=> Get frames after PopFrames command, thread = " + breakpointThreadID);
        FrameInfo[] newFrameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);

        logWriter.println("");
        logWriter.println("=> Frames after popFrame");
        logWriter.println("=> newNumberOfFrames = " + newFrameInfos.length);

        printStackFrame(newFrameInfos.length, newFrameInfos);
        logWriter.println("=> Check that " + NUMBER_OF_FRAMES_TO_POP + " were discarded");
        int numberOfPoppedFrames = frameInfos.length - newFrameInfos.length;
        assertEquals("not all frames were discarded", numberOfPoppedFrames,
                NUMBER_OF_FRAMES_TO_POP);

        for (int i = numberOfPoppedFrames; i < (frameInfos.length); i++) {
            if (frameInfos[i].location.methodID != newFrameInfos[i
                    - numberOfPoppedFrames].location.methodID) {
                logWriter.println("## FAILURE: frames number " + i + " and "
                        + (i - numberOfPoppedFrames) + " are not equal");
                fail("frames number are not equal");
            }
        }

        logWriter.println("=> Ckeck PASSED");
        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("=> Wait for breakpoint in " + breakpointMethodName);
        breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> TEST popSeveralFrames PASSED");

    }

    /**
     * This testcase exercises StackFrame.PopFrames command performing it several times.
     * <BR>The test starts PopFramesDebuggee class, sets a breakpoint
     * in 'nestledMethod4', stops at breakpoint and prints stack.
     * <BR>Then the test performs StackFrame.PopFrame command several times
     * to discard frames one after another,
     * prints stack and checks that discarded frames are not returned 
     * by ThreadReference.Frames command. 
     * <BR>Then the test resumes debuggee and checks stop on the same breakpoint.
     */
    public void testPopSeveralTimes() {
        logWriter.println("==> testPopSeveralTimes started");

        //check capability, relevant for this test
        logWriter.println("=> Check capability: canPopFrames");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canPopFrames;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canPopFrames");
            return;
        }
        // pass nestledMethod1()
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // find checked method
        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());

        logWriter.println("=> Set breakpoint at the beginning of " + breakpointMethodName);
        long requestID = debuggeeWrapper.vmMirror.setBreakpointAtMethodBegin(
                refTypeID, breakpointMethodName);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // receive event
        logWriter.println("=> Wait for breakpoint in " + breakpointMethodName);
        long breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> breakpointThreadID = " + breakpointThreadID);

        // print stack frames
        logWriter.println("");
        logWriter.println("=> Get frames before PopFrames command, thread = " + breakpointThreadID);
        FrameInfo[] frameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);
        logWriter.println("=> Frames before popFrame");
        printStackFrame(frameInfos.length, frameInfos);
        int framesBeforePop = frameInfos.length;
        logWriter
                .println("=> Number of frames before command: " + framesBeforePop);

        logWriter.println("");
        logWriter.println("=> Pop " + NUMBER_OF_FRAMES_TO_POP
                + " frames one after another");
        logWriter.println("");
        for (int i = 0; i < 3; i++) {
            logWriter.println("=> Pop frame#" + i);
            frameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);
            logWriter.println("=> Perform PopFrames command with frameID = " + frameInfos[0].getFrameID());
            jdwpPopFrames(breakpointThreadID, frameInfos[0].getFrameID());
        }
        
        logWriter.println("=> Get frames after PopFrames command, thread = " + breakpointThreadID);
        FrameInfo[] newFrameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);

        logWriter.println("");
        logWriter.println("=> Frames after popFrame");
        logWriter.println("=> newNumberOfFrames = " + newFrameInfos.length);

        printStackFrame(newFrameInfos.length, newFrameInfos);
        
        logWriter.println("=> Check that " + NUMBER_OF_FRAMES_TO_POP + " frames were discarded");
        int numberOfPoppedFrames = framesBeforePop - newFrameInfos.length;
        assertEquals("not all frames were discarded", numberOfPoppedFrames,
                NUMBER_OF_FRAMES_TO_POP);

        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("=> Wait for breakpoint in " + breakpointMethodName);
        breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("==> TEST popSeveralTimes PASSED");
    }

    /**
     * This testcase exercises StackFrame.PopFrames command when thread is not suspended.
     * <BR>The test starts PopFramesDebuggee class, sets a breakpoint
     * in 'nestledMethod4', stops at breakpoint and prints stack.
     * <BR>Then the test performs ClassType.InvokeMethodCommand without waiting reply, and
     * waits to ensure that method was started. 
     * <BR>During working of method the test performs StackFrame.PopFrames command.
     * Then the test checks that StackFrame.PopFrames command
     * returns error: THREAD_NOT_SUSPENDED or INVALID_FRAMEID.
     * <BR>Next, the test receives reply from invoked method and resumes debuggee..
     */
    public void testPopFramesWithInvokeMethods() {
        logWriter.println("==> testPopFramesWithInvokeMethods started");

        //check capability, relevant for this test
        logWriter.println("=> Check capability: canPopFrames");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canPopFrames;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canPopFrames");
            return;
        }
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // find checked method
        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        
        logWriter.println("=> Set breakpoint at the beginning of " + breakpointMethodName);
        long requestID = debuggeeWrapper.vmMirror.setBreakpointAtMethodBegin(
                refTypeID, breakpointMethodName);

        // release debuggee
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // receive event
        logWriter.println("=> Wait for breakpoint in " + breakpointMethodName);
        long breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> breakpointThreadID = " + breakpointThreadID);

        // print stack frames
        logWriter.println("");
        logWriter.println("=> Get frames before PopFrames command, thread = " + breakpointThreadID);
        FrameInfo[] frameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);
        printStackFrame(frameInfos.length, frameInfos);
        logWriter.println("=> Number of frames before command: "
                + frameInfos.length);

        // find frameID to pop
        logWriter.println("");
        logWriter.println("=> Find frameID of method = " + methodToPop + " for PopFrames command");
        long frameID = 0;
        long methodID = getMethodID(refTypeID, methodToPop);
        boolean isMethodFound = false;
        for (int j = 0; j < frameInfos.length; j++) {
            if (frameInfos[j].location.methodID == methodID) {
                frameID = frameInfos[j].getFrameID();
                isMethodFound = true;
                break;
            }
        }
        if (!isMethodFound) {
            logWriter
                    .println("##FAILURE: there is no frame for checked method");
            fail("There is no frame for checked method");
        }

        logWriter.println("=> frameID for PopFrames command = " + frameID);
        
        // create JDWP command for MethodInvoke with a long running method
        long debuggeeRefTypeID = debuggeeWrapper.vmMirror
                .getClassID(debuggeeSignature);
        logWriter.println("=> Find toInvokeMethodID for method: " + PopFramesDebuggee.METHOD_TO_INVOKE_NAME);
        long toInvokeMethodID = debuggeeWrapper.vmMirror.getMethodID(
                debuggeeRefTypeID, PopFramesDebuggee.METHOD_TO_INVOKE_NAME);
        if (toInvokeMethodID == -1) {
            logWriter
                    .println("## FAILURE: Can NOT get toInvokeMethodID for method: "
                            + PopFramesDebuggee.METHOD_TO_INVOKE_NAME);
            fail("## Can NOT get toInvokeMethodID");
        }
        logWriter.println("=> toInvokeMethodID = " + toInvokeMethodID);
        
        int invokeCommandID = 0;
        CommandPacket invokeCommand = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.InvokeMethodCommand);
        invokeCommand.setNextValueAsClassID(debuggeeRefTypeID);
        invokeCommand.setNextValueAsThreadID(breakpointThreadID);
        invokeCommand.setNextValueAsMethodID(toInvokeMethodID);
        invokeCommand.setNextValueAsInt(1); // args number
        invokeCommand.setNextValueAsValue(new Value(timeOfMethodInvocation));
        invokeCommand
                .setNextValueAsInt(JDWPConstants.InvokeOptions.INVOKE_SINGLE_THREADED);

        // send MethodInvoke command, but not wait for reply
        try {
            logWriter
                    .println("=> Send InvokeMethod command for method: " + PopFramesDebuggee.METHOD_TO_INVOKE_NAME);
            invokeCommandID = debuggeeWrapper.vmMirror
                    .sendCommand(invokeCommand);
        } catch (Exception e) {
            logWriter.println("Exception during invokeCommand: " + e);
            throw new TestErrorException(e);
        }

        // wait to ensure that method invocation started
        logWriter.println("=> Wait " + timeoutToWait
                + " mls to ensure that method invocation started");
        Object waitObj = new Object();
        synchronized (waitObj) {
            try {
                waitObj.wait(timeoutToWait);
            } catch (InterruptedException e) {
                logWriter.println("##Exception while waiting on object: " + e);
                throw new TestErrorException(e);
            }
        }

        // perform PopFrame command
        logWriter.println("=> Perform PopFrames command for method = " + methodToPop + " with frameID = " + frameID + " and expect errors");
        CommandPacket popFramesCommand = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.PopFramesCommand);
        popFramesCommand.setNextValueAsThreadID(breakpointThreadID);
        popFramesCommand.setNextValueAsFrameID(frameID);

        ReplyPacket popFrameReply = debuggeeWrapper.vmMirror
                .performCommand(popFramesCommand);
        int res = popFrameReply.getErrorCode();
        logWriter.println("=> Returned error code: " + res + " ("
                + JDWPConstants.Error.getName(res) + ")");

        // check that PopFrames returns error, because thread is resumed by
        // InvokeMethod
        if (res == JDWPConstants.Error.NONE) {
            logWriter
                    .println("##PopFrames command returned no error for thread resumed by InvokeMethod");
            fail("##PopFrames command returned no error for thread resumed by InvokeMethod");
        }

        logWriter.println("=> Receive reply of invoked method: " + PopFramesDebuggee.METHOD_TO_INVOKE_NAME);
        try {
            ReplyPacket reply = debuggeeWrapper.vmMirror
                    .receiveReply(invokeCommandID);
            checkReplyPacket(reply, "ClassType::InvokeMethod command");
        } catch (Exception e) {
            logWriter
                    .println("##Exception while receiving reply for invoke command: "
                            + e);
            throw new TestErrorException(e);
        }

        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> TEST testPopFramesWithInvokeMethods PASSED");
    }

    void printStackFrame(int NumberOfFrames, FrameInfo[] frameInfos) {
        for (int i = 0; i < NumberOfFrames; i++) {
            logWriter.println(" ");
            logWriter
                    .println("=> #" + i + " frameID=" + frameInfos[i].frameID);
            String methodName = getMethodName(frameInfos[i].location.classID,
                    frameInfos[i].location.methodID);
            logWriter.println("=> method name=" + methodName);
        }
        logWriter.println("");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PopFrames002Test.class);
    }
}