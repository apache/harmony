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
 * Created on 5.06.2006
 */
package org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * This debugger is invoked by debuggee on demand. 
 * Upon launching debugger establishes synch connection with debuggee and with Test.
 * Debugger gets RefTypeID of tested class, sets breakpoint inside testMethod, 
 * waits for breakpoint, gets frames, and prints them. Then it releases debuggee.
 * After every relevant step, debugger send message (OK or FAIL) to Test.
 * 
 * @see org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthowDebuggerLaunchDebuggee
 */

public class OnthrowLaunchDebugger002 extends LaunchedDebugger {
    
    protected String getDebuggeeClassName() {
        return "";
    }
    
    String breakpointMethodName = "testMethod";
    
    public void testDebugger() {
        logWriter.println("***> Debugger started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // find checked method
        long refTypeID = 0;
        try {
            refTypeID = getClassIDBySignature(DEBUGGEE_CLASS_SIGNATURE);
        } catch (TestErrorException e) {
            logWriter.println("##EXCPETION: " + e);
            testSynchronizer.sendMessage("FAIL");
            fail("exception during getting class signature");
        }
                
        logWriter.println("**> Set breakpoint at the beginning of " + breakpointMethodName);
        long requestID = 0;
        try {
            requestID = debuggeeWrapper.vmMirror.setBreakpointAtMethodBegin(
                        refTypeID, breakpointMethodName);
        } catch (TestErrorException e) {
            logWriter.println("##EXCEPTION: " + e);
            testSynchronizer.sendMessage("FAIL");
            fail("exception setting breakpoint");
        }
        
        logWriter.println("**> RequestID = " + requestID);
        logWriter.println("**> Release debuggee");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        
        // receive event
        logWriter.println("**> Wait for breakpoint in " + breakpointMethodName);
        long breakpointThreadID = 0;
        try {
            breakpointThreadID = debuggeeWrapper.vmMirror.waitForBreakpoint(requestID);
        } catch (TestErrorException e) {
            logWriter.println("##EXCEPTION: " + e);
            testSynchronizer.sendMessage("FAIL");
            fail("exception during waiting for breakpoint");
        }
                
        testSynchronizer.sendMessage("OK");
        logWriter.println("**> breakpointThreadID = " + breakpointThreadID);
        
        //print stack frames
        logWriter.println("");
        logWriter.println("**> Get frames, thread = " + breakpointThreadID);
        FrameInfo[] frameInfos = null;
        try {
            frameInfos = jdwpGetFrames(breakpointThreadID, 0, -1);
        } catch (TestErrorException e) {
            logWriter.println("##EXCEPTION: " + e);
            testSynchronizer.sendMessage("FAIL");
            fail("exception during getting frames");
        }
        try {
            printStackFrame(frameInfos.length, frameInfos);
        } catch (TestErrorException e) {
            logWriter.println("##EXCEPTION: " + e);
            testSynchronizer.sendMessage("FAIL");
            fail("exception during printing frames");
        }
        testSynchronizer.sendMessage("OK");
        
        logWriter.println("**> Resume debuggee");
        debuggeeWrapper.vmMirror.resume();
               
        testSynchronizer.sendMessage("END");
        logWriter.println("***> Debugger finished");
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(OnthrowLaunchDebugger002.class);
    }
}
