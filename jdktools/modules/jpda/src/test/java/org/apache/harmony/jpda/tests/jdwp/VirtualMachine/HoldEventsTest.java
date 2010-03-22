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

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.EventPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.exceptions.TimeoutException;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for VirtualMachine.HoldEvents command.
 */
public class HoldEventsTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.VirtualMachine.HoldEventsDebuggee";
    }

    /**
     * This testcase exercises VirtualMachine.HoldEvents command.
     * <BR>At first the test starts HoldEventsDebuggee.
     * <BR> Then the test sends request for TESTED_THREAD and
     * performs VirtualMachine.HoldEvents command.
     * Next, the test waits for debuggee to start the 'TESTED_THREAD'
     * thread and checks that no any events (including requested THREAD_START event)
     * are received during default timeout.
     * Then the test sends VirtualMachine.ReleaseEvents command 
     * and checks that expected THREAD_START event is received.
     */
    public void testHoldEvents001() {
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

        try {
            EventPacket event = debuggeeWrapper.vmMirror.receiveEvent(settings.getTimeout());
            logWriter.printError("unexpected event received: " + event);
            fail("unexpected event received");
        } catch (TimeoutException e) {
            logWriter.println("no events were received");
        } catch (Exception e) {
            throw new TestErrorException(e);
        }

        logWriter.println("send ReleaseEvents");
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ReleaseEventsCommand);
        debuggeeWrapper.vmMirror.performCommand(packet);
       
        EventPacket event = debuggeeWrapper.vmMirror.receiveCertainEvent(JDWPConstants.EventKind.THREAD_START);
        logWriter.println("expected event received: " + event);
        debuggeeWrapper.vmMirror.resume();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HoldEventsTest.class);
    }
}
