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
 * JDWP Unit test for ThreadReference.Interrupt command.
 */
public class InterruptTest extends JDWPSyncTestCase {

    static String waitForString = "java.lang.InterruptedException";
    boolean isReceived = false;

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.InterruptDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.Interrupt command.
     * <BR>At first the test starts InterruptDebuggee which runs
     * the tested thread 'TESTED_THREAD' and blocks it in invocation of
     * the 'wait()' method. 
     * <BR> Then the tests performs the ThreadReference.Interrupt command 
     * for tested thread. 
     * <BR>After sending Interrupt command, the test waits signals via synchronization
     * channel that 'InterruptedException' was thrown.
     */
    public void testInterrupt001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        logWriter.println("get thread ID");
        long threadID = 
            debuggeeWrapper.vmMirror.getThreadID(InterruptDebuggee.TESTED_THREAD);

        // getting the thread group ID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.InterruptCommand);
        packet.setNextValueAsThreadID(threadID);
        logWriter.println("send \"Interrupt\" command");
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        
        short err = reply.getErrorCode();
        logWriter.println("error = " + err);

        if (err == JDWPConstants.Error.NONE) {
            Thread thrd = new RecvThread();
            thrd.start();
            try {
                thrd.join(settings.getTimeout());
            } catch(InterruptedException e) {
                
            }
        }

        if (!isReceived) {
            printErrorAndFail(waitForString + " is not received");
        } else {
            logWriter.println(waitForString + " is received");
        }
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    class RecvThread extends Thread {
        
        public void run() {
            logWriter.println("wait for " + waitForString);
            isReceived = synchronizer.receiveMessage(waitForString);
        }

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterruptTest.class);
    }
}
