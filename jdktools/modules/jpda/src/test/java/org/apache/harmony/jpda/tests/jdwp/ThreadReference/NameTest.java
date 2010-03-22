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
 * Created on 11.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.Name command.
 */
public class NameTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises ThreadReference.Name command.
     * <BR>At first the test starts HelloWorld debuggee. 
     * <BR> Then the tests performs the ThreadReference.Name command 
     * for every thread in debuggee. 
     * <BR>It is expected that the returned names are not empty.
     */
    public void testName001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        ReplyPacket thrdReply, reply = debuggeeWrapper.vmMirror.getAllThreadID();

        CommandPacket packet;
        long threadID;
        String threadName;
        int threads = reply.getNextValueAsInt();
        for (int i = 0 ;i < threads; i++) {
            threadID = reply.getNextValueAsThreadID();
            packet = new CommandPacket(
                    JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                    JDWPCommands.ThreadReferenceCommandSet.NameCommand);
            packet.setNextValueAsThreadID(threadID);
            
            thrdReply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(thrdReply, "ThreadReference::Name command");

            threadName = thrdReply.getNextValueAsString(); 
            logWriter.println("\tthreadID = " + threadID + " threadName = "
                    + threadName);
            if (threadName.length() == 0) {
                printErrorAndFail("Empty name for thread with ID=" + threadID);
            }
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(NameTest.class);
    }
}
