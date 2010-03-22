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
 * Created on 28.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * This tests case exercises the JDWP command <code>VirtualMachine.ReleaseEvents</code>.
 * After the test sends <code>HoldEvents</code>, the debuggee
 * <code>org.apache.harmony.jpda.tests.jdwp.VirtualMachine.ReleaseEventsDebuggee</code> tries to start
 * the tested thread <code>TESTED_THREAD</code>.
 * 
 * <p>The following statements are checked: 
 * <ol>
 *      <li><i>testReleaseEvents001</i>
 *          It is expected the <code>THREAD_START</code> after sending
 *          sending <code>ReleaseEvents</code>.
 * 
 * </ol>
 */
/**
 * JDWP Unit test for VirtualMachine.ReleaseEvents command.
 */
public class ReleaseEventsTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.VirtualMachine.ReleaseEventsDebuggee";
    }

    /**
     * This testcase exercises VirtualMachine.ReleaseEvents command.
     * <BR>At first the test starts ReleaseEventsDebuggee.
     * <BR> Then the test sends request for TESTED_THREAD and
     * performs VirtualMachine.HoldEvents command.
     * <BR>Next, the test waits for debuggee to start the 'TESTED_THREAD'
     * thread and performs VirtualMachine.ReleaseEvents command.
     * <BR>After this the test expects that requested TESTED_THREAD event is sent to test.
     */
    public void testReleaseEvents001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        debuggeeWrapper.vmMirror.setThreadStart();
        
        //send HoldEvents command
        logWriter.println("send HoldEvents");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.HoldEventsCommand);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::HoldEvents command");

        logWriter.println("allow to start thread");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("send ReleaseEvents");
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ReleaseEventsCommand);
        
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ReleaseEvents command");

        CommandPacket event = debuggeeWrapper.vmMirror.receiveCertainEvent(
                JDWPConstants.EventKind.THREAD_START);
        if (event != null) {
            logWriter.println("expected event: "
                    + JDWPConstants.EventKind.getName(JDWPConstants.EventKind.THREAD_START)
                    + " was received");
        } else {
            logWriter.printError("no events were received");
            fail("no events were received");
        }
        debuggeeWrapper.vmMirror.resume();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReleaseEventsTest.class);
    }
}
