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
 * Created on 24.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.CurrentContendedMonitor command.
 */
public class CurrentContendedMonitorTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.CurrentContendedMonitorDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.CurrentContendedMonitor command.
     * <BR>At first the test starts CurrentContendedMonitorDebuggee which runs 
     * the tested thread 'TESTED_THREAD'.
     * <BR> Then the test performs the ThreadReference.CurrentContendedMonitor command 
     * for the tested thread. 
     * <BR>After getting monitor object from command, the test
     * performs the ObjectReference.MonitorInfo command for this monitor object 
     * and checks that the waiter for this monitor is the 'TESTED_THREAD' thread.
     *  
     */
    public void testCurrentContendedMonitor001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        //check capability, relevant for this test
        debuggeeWrapper.vmMirror.capabilities();
        logWriter.println("=> Check capability: canGetMonitorInfo");
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetMonitorInfo;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canGetMonitorInfo");
            return;
        }
        logWriter.println("=> Check capability: canGetCurrentContendedMonitor");
        isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetCurrentContendedMonitor;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canGetCurrentContendedMonitor");
            return;
        }
       
        // getting ID of the tested thread
        logWriter.println
        ("==> testedThreadName = " + CurrentContendedMonitorDebuggee.TESTED_THREAD);
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = 
            debuggeeWrapper.vmMirror.getThreadID(CurrentContendedMonitorDebuggee.TESTED_THREAD);
        logWriter.println("==> testedThreadID = " + testedThreadID);
        logWriter.println("==> suspend testedThread...");
        debuggeeWrapper.vmMirror.suspendThread(testedThreadID);

        // getting the thread group ID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.CurrentContendedMonitorCommand);
        packet.setNextValueAsThreadID(testedThreadID);
        logWriter.println("send \"CurrentContendedMonitor\" command");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::CurrentContendedMonitor command");
        
        TaggedObject tobj = reply.getNextValueAsTaggedObject();
        
        logWriter.println("\ttagged-object tag: "
                + JDWPConstants.Tag.getName(tobj.tag) + "(" + tobj.tag + ") "
                + "ID: " + tobj.objectID);
        
        packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.MonitorInfoCommand);
        packet.setNextValueAsObjectID(tobj.objectID);
        ReplyPacket replyObj = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(replyObj, "ObjectReference::MonitorInfo command");
        
        replyObj.getNextValueAsThreadID();
        replyObj.getNextValueAsInt();
        int waiters = replyObj.getNextValueAsInt();
        long waiterID;
        String waiterName;
        for (int i = 0; i < waiters; i++) {
            waiterID = replyObj.getNextValueAsThreadID();
            waiterName = debuggeeWrapper.vmMirror.getThreadName(waiterID);
            logWriter.println("\twaiter: "
                    + " " + waiterName
                    + "(" + waiterID + ")");
            if (waiterID != testedThreadID) {
                logWriter.printError("wrong owner: " + waiterID);
                assertEquals("ObjectReference::MonitorInfo returned wrong owner ID,",
                        testedThreadID, waiterID);
            }
            assertString("ObjectReference::MonitorInfo  returned invalid waiter name,",
                    OwnedMonitorsDebuggee.TESTED_THREAD, waiterName);
        }

        // interrupt
        packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.InterruptCommand);
        packet.setNextValueAsThreadID(testedThreadID);
        logWriter.println("send \"Interrupt\" command");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::Interrupt command");
        
        short err = reply.getErrorCode();
        if (err != JDWPConstants.Error.NONE) {
            logWriter.printError("Unexpected " + JDWPConstants.Error.getName(err));
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CurrentContendedMonitorTest.class);
    }
}
