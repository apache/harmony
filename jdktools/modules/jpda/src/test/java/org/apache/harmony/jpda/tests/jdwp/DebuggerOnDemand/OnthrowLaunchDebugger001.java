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
 * Created on 11.06.2006
 */
package org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * This debugger is invoked by debuggee on demand. Debugger establishes synch connection with debuggee and with Test.
 * Debugger only performs <code>VirtualMachine::Version</code> command, sends message (OK or FAIL) to debugged, then releases debuggee.
 * 
 * @see org.apache.harmony.jpda.tests.jdwp.DebuggerOnDemand.OnthowDebuggerLaunchDebuggee
 */
public class OnthrowLaunchDebugger001 extends LaunchedDebugger {
    
    public void testDebugger002() {
        logWriter.println("***> OnthrowLaunchDebugger001 started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
                
        logWriter.println("**> Send VirtualMachine::Version command");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.VersionCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        logWriter.println("**> Check reply on Version command");
        if (!checkReplyPacketWithoutFail(reply, "VirtualMachine::Version command")) {
            testSynchronizer.sendMessage("FAIL");
            fail("error durign performing VirtualMachine::Version command");
        }
        testSynchronizer.sendMessage("OK");
        
        logWriter.println("**> Send SIGNAL_CONTINUE");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testSynchronizer.sendMessage("END");
        logWriter.println("***> OnthrowLaunchDebugger001 finished");
    }
           
    public static void main(String[] args) {
        junit.textui.TestRunner.run(OnthrowLaunchDebugger001.class);
    }
}
