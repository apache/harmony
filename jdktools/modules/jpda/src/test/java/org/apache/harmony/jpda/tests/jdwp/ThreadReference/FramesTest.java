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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 15.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import java.util.Vector;
import java.util.Enumeration;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for ThreadReference.Frames command.
 */
public class FramesTest extends JDWPSyncTestCase {

    short err;
    long threadID;

    class FrameStruct {
        long frameID;
        Location loc;
        FrameStruct(long frameID, Location loc) {
            this.frameID = frameID;
            this.loc = loc;
        }
    }

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.FramesDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.Frames command.
     * <BR>At first the test starts FramesDebuggee which recursively invokes 
     * the method 'FramesDebuggee.recursiveMethod' thereby the specified depth 
     * 'FramesDebuggee.DEPTH' of recursion is reached. 
     * <BR> Then the tests performs the ThreadReference.Frames command 
     * for the main thread with parameters: 
     * <BR>startFrame=0, length=(amount_of_all_frames + 1)
     * <BR>It is expected the error INVALID_LENGTH is returned.
     */
    public void testFrames005() {
        logWriter.println("==> testFrames005 START "); 
        String testedThreadName = synchronizer.receiveMessage();

        logWriter.println
        ("==> testedThreadName = |" + testedThreadName +"|"); 
        threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);
        logWriter.println("==> threadID = " + threadID); 
        debuggeeWrapper.vmMirror.suspendThread(threadID);
        
        Vector allFrames = getFrames(0, -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        String methodName, classSignature;
        int i = 0;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
            logWriter.println("\t" + i + ". frameID=" + frame.frameID
                    + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
        }

        allFrames = getFrames(0, allFrames.size() + 1);
        if (err == JDWPConstants.Error.INVALID_LENGTH) {
            logWriter.println("Caught expected error - " + JDWPConstants.Error.getName(err)
                    + "(" + err + ")");
        } else {
            printErrorAndFail("unexpected behaviour: error is "
                    + JDWPConstants.Error.getName(err) + "(" + err + ")"
                    + " but  must be "
                    + JDWPConstants.Error.getName(JDWPConstants.Error.INVALID_LENGTH)
                    + "(" + JDWPConstants.Error.INVALID_LENGTH + ")");
        }
        logWriter.println("==> testFrames005 OK. "); 
        debuggeeWrapper.vmMirror.resumeThread(threadID);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ThreadReference.Frames command.
     * <BR>At first the test starts FramesDebuggee which recursively invokes 
     * the method 'FramesDebuggee.recursiveMethod' thereby the specified depth 
     * 'FramesDebuggee.DEPTH' of recursion is reached. 
     * <BR> Then the tests performs the ThreadReference.Frames command 
     * for the main thread with parameters: 
     * <BR>startFrame=(amount_of_all_frames + 1), length=-1
     * <BR>It is expected the error INVALID_INDEX is returned.
     */
    public void testFrames004() {
        logWriter.println("==> testFrames004 START "); 
        String testedThreadName = synchronizer.receiveMessage();

        logWriter.println
        ("==> testedThreadName = |" + testedThreadName +"|"); 
        threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);
        logWriter.println("==> threadID = " + threadID); 
        debuggeeWrapper.vmMirror.suspendThread(threadID);
        
        Vector allFrames = getFrames(0, -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        String methodName, classSignature;
        int i = 0;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
            logWriter.println("\t" + i + ". frameID=" + frame.frameID
                       + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
        }

        allFrames = getFrames(allFrames.size() + 1, -1);
        if (err == JDWPConstants.Error.INVALID_INDEX) {
            logWriter.println("Caught expected error - " + JDWPConstants.Error.getName(err)
                    + "(" + err + ")");
        } else {
            printErrorAndFail("unexpected behaviour: error is "
                    + JDWPConstants.Error.getName(err) + "(" + err + ")"
                    + " but  must be "
                    + JDWPConstants.Error.getName(JDWPConstants.Error.INVALID_INDEX)
                    + "(" + JDWPConstants.Error.INVALID_INDEX + ")");
        }
                    
        logWriter.println("==> testFrames004 OK. "); 
        debuggeeWrapper.vmMirror.resumeThread(threadID);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ThreadReference.Frames command.
     * <BR>At first the test starts FramesDebuggee which recursively invokes 
     * the method 'FramesDebuggee.recursiveMethod' thereby the specified depth 
     * 'FramesDebuggee.DEPTH' of recursion is reached. 
     * <BR> Then the tests performs the ThreadReference.Frames command 
     * for the main thread with parameters: 
     * <BR>startFrame=amount_of_all_frames, length=-1
     * <BR>It is expected an empty set of frames is returned.
     */
    public void testFrames003() {
        logWriter.println("==> testFrames003 START "); 
        String testedThreadName = synchronizer.receiveMessage();

        logWriter.println
        ("==> testedThreadName = |" + testedThreadName +"|"); 
        threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);
        logWriter.println("==> threadID = " + threadID); 
        debuggeeWrapper.vmMirror.suspendThread(threadID);
        
        Vector allFrames = getFrames(0, -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        String methodName, classSignature;
        int i = 0;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
               logWriter.println("\t" + i + ". frameID=" + frame.frameID
                       + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
        }

        allFrames = getFrames(allFrames.size(), -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        if (allFrames.size() == 0) {
            logWriter.println("empty set of frames is returned");
        } else {
            printErrorAndFail("it is expected an empty set of frames, but frameCount = "
                    + allFrames.size());
        }
                    
        logWriter.println("==> testFrames003 OK. "); 
        debuggeeWrapper.vmMirror.resumeThread(threadID);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ThreadReference.Frames command.
     * <BR>At first the test starts FramesDebuggee which recursively invokes 
     * the method 'FramesDebuggee.recursiveMethod' thereby the specified depth 
     * 'FramesDebuggee.DEPTH' of recursion is reached. 
     * <BR>Then the test by ThreadReference.Frames command 
     * requests all frames and looks for the first frame
     * which has location in the 'recursiveMethod'.
     * <BR>The index of such frame is passed as startFrame parameter for
     * the second invocation of ThreadReference.Frames command.
     * <BR>The length (the second parameter) is set to 'FramesDebuggee.DEPTH'.
     * <BR>It is expected that the amount of returned frames is equal to
     * 'FramesDebuggee.DEPTH' and all returned frames have locations in
     * 'recursiveMethod'.
     */
    public void testFrames002() {
        logWriter.println("==> testFrames002 START "); 
        String testedThreadName = synchronizer.receiveMessage();

        logWriter.println
        ("==> testedThreadName = |" + testedThreadName +"|"); 
        threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);
        logWriter.println("==> threadID = " + threadID); 
        debuggeeWrapper.vmMirror.suspendThread(threadID);
        
        Vector allFrames = getFrames(0, -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        String methodName, classSignature;
        int frameNumber = -1;
        int i = 0;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
            if (frameNumber < 0 && FramesDebuggee.METHOD_NAME.equals(methodName)) {
                frameNumber = i;
            }
               logWriter.println("\t" + i + ". frameID=" + frame.frameID
                       + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
        }

        if (frameNumber < 0) {
            printErrorAndFail("frameNumber is unexpectedly equal to " + frameNumber);
        }
        
        allFrames = getFrames(frameNumber, FramesDebuggee.DEPTH);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        i = frameNumber;
        int methodCount = 0;
        String unexpectedMethods = null;
        String depthError = null;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
            logWriter.println("\t" + i + ". frameID=" + frame.frameID
                    + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
               
               if (methodName.equals(FramesDebuggee.METHOD_NAME)) {
                   methodCount++;
               } else {
                   logWriter.printError("unexpected method - " + methodName);
                   unexpectedMethods = null == unexpectedMethods ? methodName : unexpectedMethods + "," + methodName;
               }
        }
        
        if (methodCount != FramesDebuggee.DEPTH) {
            logWriter.printError(depthError = ("Number of " + FramesDebuggee.METHOD_NAME + " in frames "
                    + methodCount + "  but expected " + FramesDebuggee.DEPTH));
        }
        
        debuggeeWrapper.vmMirror.resumeThread(threadID);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        if (null != unexpectedMethods) {
            logWriter.println("==> testFrames002 FAILED "); 
            fail("unexpected method(s): " + unexpectedMethods);
        }
        else if (null != depthError) {
            logWriter.println("==> testFrames002 FAILED "); 
            fail(depthError);
        } else {   
            logWriter.println("==> testFrames002 OK. "); 
        }
    }

    /**
     * This testcase exercises ThreadReference.Frames command.
     * <BR>At first the test starts FramesDebuggee which recursively invokes 
     * the method 'FramesDebuggee.recursiveMethod' thereby the specified depth 
     * 'FramesDebuggee.DEPTH' of recursion is reached. 
     * <BR>Then the test by ThreadReference.Frames command 
     * requests all frames and looks for the first frame
     * which has location in the 'recursiveMethod'.
     * <BR>The index of such frame is passed as startFrame parameter for
     * the second invocation of ThreadReference.Frames command.
     * <BR>The length (the second parameter) is set to '-1'.
     * <BR>It is expected that the amount of returned frames which are located in
     * 'recursiveMethod' is equal to 'FramesDebuggee.DEPTH'.
     */
    public void testFrames001() {
        logWriter.println("==> testFrames001 START "); 
        String testedThreadName = synchronizer.receiveMessage();

        logWriter.println
        ("==> testedThreadName = |" + testedThreadName +"|"); 
        threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);
        logWriter.println("==> threadID = " + threadID); 
        debuggeeWrapper.vmMirror.suspendThread(threadID);
        
        Vector allFrames = getFrames(0, -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        String methodName, classSignature;
        int frameNumber = -1;
        int i = 0;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
            if (frameNumber < 0 && FramesDebuggee.METHOD_NAME.equals(methodName)) {
                frameNumber = i;
            }
            logWriter.println("\t" + i + ". frameID=" + frame.frameID
                    + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
        }

        if (frameNumber < 0) {
            logWriter.printError("frameNumber is unexpectedly equal to " + frameNumber);
            assertTrue("Invalid frameNumber", frameNumber > 0);
        }
        
        allFrames = getFrames(frameNumber, -1);
        if (err != JDWPConstants.Error.NONE) {
            printErrorAndFail("Unexpected ERROR = " + err 
                    + "(" + JDWPConstants.Error.getName(err) + ")");
        }
        i = frameNumber;
        int methodCount = 0;
        boolean testCondition;
        String unexpectedMethods = null;
        String depthError = null;
        for (Enumeration e = allFrames.elements(); e.hasMoreElements(); i++) {
            FrameStruct frame = (FrameStruct )e.nextElement();
            methodName = getMethodName(frame.loc.classID, frame.loc.methodID);
            classSignature = getClassSignature(frame.loc.classID);
            logWriter.println("\t" + i + ". frameID=" + frame.frameID
                    + " - " + classSignature
                    + methodName
                    + "(" + frame.loc.index + ")");
               testCondition = (i == frameNumber
                       && !methodName.equals(FramesDebuggee.METHOD_NAME));
               if (testCondition) {
                   logWriter.printError("unexpected method name of the first frame - "
                           + methodName);
                   unexpectedMethods = null == unexpectedMethods ? methodName : unexpectedMethods + "," + methodName;
               }
               
               if (methodName.equals(FramesDebuggee.METHOD_NAME)) {
                   methodCount++;
               }
        }

        if (methodCount != FramesDebuggee.DEPTH) {
            logWriter.printError(depthError = ("Number of " + FramesDebuggee.METHOD_NAME + " in frames "
                    + methodCount + "  but expected " + FramesDebuggee.DEPTH));
        }

        debuggeeWrapper.vmMirror.resumeThread(threadID);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        if (null != unexpectedMethods) {
            logWriter.println("==> testFrames001 FAILED "); 
            fail("unexpected method(s): " + unexpectedMethods);
        }
        else if (null != depthError) {
            logWriter.println("==> testFrames001 FAILED "); 
            fail(depthError);
        } else {   
            logWriter.println("==> testFrames001 OK. "); 
        }
    }

    private Vector getFrames(int startFrame, int length) {

        Vector<FrameStruct> frames = new Vector<FrameStruct>();
        
        logWriter.println("startFrame=" + startFrame
                + "; length=" + length);

        // getting frames of the thread
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FramesCommand);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsInt(startFrame);
        packet.setNextValueAsInt(length);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        err = reply.getErrorCode(); 
        if ( err != JDWPConstants.Error.NONE) {
            logWriter.println("\tthreadID=" + threadID
                    + " - " + JDWPConstants.Error.getName(err));
            return null;
        }
        int framesCount = reply.getNextValueAsInt();
        long frameID;
        Location loc;
        logWriter.println("framesCount=" + framesCount);
        for (int j = 0; j < framesCount; j++) {
               frameID = reply.getNextValueAsFrameID();
               loc = reply.getNextValueAsLocation();
               frames.add(new FrameStruct(frameID, loc));
           }
        return frames;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FramesTest.class);
    }
}
