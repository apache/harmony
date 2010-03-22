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
 * @author Anton V. Karnachuk, Aleksander V. Budniy
 */

/**
 * Created on 16.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.StackFrame;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for StackFrame.PopFrames command.
 */
public class PopFramesTest extends JDWPStackFrameTestCase {

    private String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/StackFrame/PopFramesDebuggee;";

    private String breakpointMethodName = "nestledMethod4";

    private String methodToPop = "nestledMethod4";

    protected String getDebuggeeClassName() {
        return PopFramesDebuggee.class.getName();
    }

    /**
     * This testcase exercises StackFrame.PopFrames command to discard one top frame.
     * <BR>The test starts PopFramesDebuggee class, sets a breakpoint
     * in 'nestledMethod4', stops at breakpoint and prints stack.
     * <BR>Then the test performs StackFrame.PopFrame command for one top frame,
     * prints stack and checks that discarded frame is not returned 
     * by ThreadReference.Frames command. 
     * <BR>Then the test resumes debuggee and checks stop on the same breakpoint.
     */
    public void testPopFramesTest001() {
        logWriter.println("==> testPopFramesTest001 started");
        
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
        for (int i = 0; i < frameInfos.length; i++) {
            if (frameInfos[i].location.methodID == methodID) {
                frameID = frameInfos[i].getFrameID();
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

        logWriter.println("");
        logWriter.println("=> Perform PopFrames command for method = " + methodToPop + " with frameID = " + frameID);
        // pop stack frame
        CommandPacket popFramesCommand = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.PopFramesCommand);
        popFramesCommand.setNextValueAsThreadID(breakpointThreadID);
        popFramesCommand.setNextValueAsFrameID(frameID);

        ReplyPacket err = debuggeeWrapper.vmMirror
                .performCommand(popFramesCommand);
        checkReplyPacket(err, "StackFrame::PopFrames command");

        logWriter.println("=> Get frames after PopFrames command, thread = " + breakpointThreadID);
        FrameInfo[] newFrameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);

        logWriter.println("");
        logWriter.println("=> Frames after popFrame");
        logWriter.println("=> newNumberOfFrames = " + newFrameInfos.length);

        printStackFrame(newFrameInfos.length, newFrameInfos);

        // check if expected frames are popped
        logWriter.println("=> Check that only one frame was discarded: frameID = " + frameID + ", method = " + methodToPop);
        int numberOfPoppedFrames = frameInfos.length - newFrameInfos.length;
        assertEquals("frame is not discarded", numberOfPoppedFrames, 1);

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

        logWriter.println("=> Wait for breakpoint in " + breakpointMethodName);
        breakpointThreadID = debuggeeWrapper.vmMirror
                .waitForBreakpoint(requestID);

        logWriter.println("=> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("==> TEST PASSED");

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
        logWriter.println(" ");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PopFramesTest.class);
    }
}