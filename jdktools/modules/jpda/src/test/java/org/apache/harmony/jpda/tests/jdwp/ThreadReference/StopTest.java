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
 * Created on 22.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ThreadReference.Stop command.
 */
public class StopTest extends JDWPSyncTestCase {

    static String SIGNATURE = "Lorg/apache/harmony/jpda/tests/jdwp/ThreadReference/StopDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ThreadReference.StopDebuggee";
    }

    /**
     * This testcase exercises ThreadReference.Stop command.
     * <BR>At first the test starts StopDebuggee which runs
     * the tested thread 'TESTED_THREAD'.
     * <BR>After the tested thread starts, the test performs ThreadReference.Stop command 
     * for the tested thread and waits for Debuggee message if the tested thread
     * is interrupted with NullPointerException. 
     * <BR>If so the test PASSED, otherwise FAILED. 
     */
    public void testStop001() {
        logWriter.println("testStop001: STARTED...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // getting ID of the tested thread
        logWriter.println("testStop001: get threadID to Stop...");
        long threadID = 
            debuggeeWrapper.vmMirror.getThreadID(StopDebuggee.TESTED_THREAD);
        logWriter.println("testStop001: ID of the tested thread to Stop = " + threadID);

        long classID = 
            debuggeeWrapper.vmMirror.getClassID(SIGNATURE);

        long fieldID = 
            debuggeeWrapper.vmMirror.getFieldID(classID, StopDebuggee.FIELD_NAME);

        // getting throwable 
        logWriter.println("testStop001: get throwable for Stop command...");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsFieldID(fieldID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::GetValues command");
        
        int values = reply.getNextValueAsInt();
        if (values != 1) {
            logWriter.println("## testStop001: Unexpected number of values = " + values);
            logWriter.println("## Expected number of values = 1");
            fail("Unexpected number of values: " + values + ", expected: 1");
        }
        Value fieldValue = reply.getNextValueAsValue();
        logWriter.println("testStop001: throwable = " + fieldValue);

        packet = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.StopCommand);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsObjectID(fieldValue.getLongValue());
        logWriter.println("testStop001: send \"Stop\" command");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ThreadReference::Stop command");

        logWriter.println("testStop001: wait for Debuggee message about test status...");
        String testStatus = synchronizer.receiveMessage();
        logWriter.println("testStop001: Received from Debuggee test status = " + testStatus);
        if ( ! testStatus.equals("PASSED") ) {
            logWriter.println("## testStop001: FAILED");
            fail("Bad message received from debuggee: " + testStatus);
        } else {
            logWriter.println("testStop001: PASSED");
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StopTest.class);
    }
}
