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
 * JDWP Unit test for ThreadReference.OwnedMonitors command.
 */
public class OwnedMonitorsTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.OwnedMonitorsDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.OwnedMonitors command.
     * <BR>At first the test starts OwnedMonitorsDebuggee which runs
     * the tested thread 'TESTED_THREAD'. 
     * <BR>Then the test performs the ThreadReference.OwnedMonitors command 
     * for the tested thread and gets list of monitor objects. 
     * It is expected that this command returns at least two monitors 
     * owned by 'TESTED_THREAD' thread
     * <BR>After this for each received monitor object the test performs 
     * ObjectReference.MonitorInfo command.
     * It is expected that this command returns the 'TESTED_THREAD' thread
     * as owner for each monitor.
     */
    public void testOwnedMonitors001() {

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // checking capability relevant for this test
        logWriter.println("==> Check capability: canGetOwnedMonitorInfo");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetOwnedMonitorInfo;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM dosn't possess capability: canGetOwnedMonitorInfo");
            synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
            return;
        }

        // getting ID of the tested thread
        logWriter.println("==> testedThreadName = " + OwnedMonitorsDebuggee.TESTED_THREAD);
        logWriter.println("==> Get testedThreadID...");
        long testedThreadID = 
            debuggeeWrapper.vmMirror.getThreadID(OwnedMonitorsDebuggee.TESTED_THREAD);
        logWriter.println("==> testedThreadID = " + testedThreadID);
        logWriter.println("==> suspend testedThread...");
        debuggeeWrapper.vmMirror.suspendThread(testedThreadID);

        // getting the thread group ID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.OwnedMonitorsCommand);
        packet.setNextValueAsThreadID(testedThreadID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::OwnedMonitors command");
        
        int owned = reply.getNextValueAsInt();
        logWriter.println("owned monitors: " + owned);

        String ownerName;
        long ownerThread;

        for (int i =0; i < owned; i++) {
            TaggedObject tobj = reply.getNextValueAsTaggedObject();

            logWriter.println("\t" + i + " tagged-object tag: "
                    + JDWPConstants.Tag.getName(tobj.tag) + "(" + tobj.tag + ") "
                    + "ID: " + tobj.objectID);
            
            packet = new CommandPacket(
                    JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                    JDWPCommands.ObjectReferenceCommandSet.MonitorInfoCommand);
            packet.setNextValueAsObjectID(tobj.objectID);
            ReplyPacket replyObj = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(replyObj, "ObjectReference::MonitorInfo command");
            
            ownerThread = replyObj.getNextValueAsThreadID();
            logWriter.println("\t\t" + "ownerThread ID:   " + ownerThread);

            ownerName = debuggeeWrapper.vmMirror.getThreadName(ownerThread);
            logWriter.println("\t\t" + "ownerThread name: " + ownerName);

            if (ownerThread != testedThreadID) {
                printErrorAndFail("wrong owner thread: " + ownerThread);
            }
            if (!ownerName.equals(OwnedMonitorsDebuggee.TESTED_THREAD)) {
                printErrorAndFail("wrong owner thread name: " + ownerName);
            }
        }

        // check that at least two owned monitors are returned
        if (owned < 2) {
             printErrorAndFail("wrong number of owned monitors: " + owned + " (expected at least 2)");
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(OwnedMonitorsTest.class);
    }
}
