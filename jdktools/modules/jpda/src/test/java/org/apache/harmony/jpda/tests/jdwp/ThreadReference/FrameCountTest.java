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
 * Created on 18.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.FrameCount command.
 */
public class FrameCountTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.FramesDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.CurrentContendedMonitor command.
     * <BR>At first the test starts FramesDebuggee which recursively invokes 
     * the method 'FramesDebuggee.recursiveMethod' thereby the specified depth 
     * 'FramesDebuggee.DEPTH' of recursion is reached. 
     * <BR> Then the tests requests list of frames for main thread by invoking
     * the JDWP command ThreadReference.Frames and compares size of
     * this list with the value got via invoking ThreadReference.FrameCount command.
     * <BR>The test expects that these values are equal, otherwise the test fail.
     *  
     */
    public void testFrameCount001() {
        logWriter.println("==> testFrameCount001 START "); 
        String testedThreadName = synchronizer.receiveMessage();

        logWriter.println
        ("==> testedThreadName = |" + testedThreadName +"|"); 
        long threadID = debuggeeWrapper.vmMirror.getThreadID(testedThreadName);
        logWriter.println("==> threadID = " + threadID); 
        debuggeeWrapper.vmMirror.suspendThread(threadID);
        
        int expectedCount = getFramesCount(threadID);
        logWriter.println("\texpected count = " + expectedCount);
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FrameCountCommand);
        packet.setNextValueAsThreadID(threadID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::FrameCount command");

        int frameCount = reply.getNextValueAsInt();
        logWriter.println("\tframe count = " + frameCount);

        if (frameCount != expectedCount) {
            printErrorAndFail("Unexpected frame count = " + frameCount
                    + ", expected value " + expectedCount);
        }
        debuggeeWrapper.vmMirror.resumeThread(threadID);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    private int getFramesCount(long threadID) {

        logWriter.println("getting frames of the thread");

        short err;
        
        // getting frames of the thread
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FramesCommand);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsInt(0);
        packet.setNextValueAsInt(-1);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        err = reply.getErrorCode(); 
        if ( err != JDWPConstants.Error.NONE) {
            logWriter.println("\tthreadID=" + threadID
                    + " - " + JDWPConstants.Error.getName(err));
            return 0;
        }
        int framesCount = reply.getNextValueAsInt();
        return framesCount;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FrameCountTest.class);
    }
}
