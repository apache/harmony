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
 * Created on 10.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import java.io.File;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for VirtualMachine.ClassPaths command.
 */
public class ClassPathsTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.ClassPaths command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.ClassPaths command and checks that:
     * <BR>&nbsp;&nbsp; - the 'baseDir' directory exists;
     * <BR>&nbsp;&nbsp; - amount of bootclasspaths is greater than 0;
     * <BR>&nbsp;&nbsp; - length of strings representing classpaths, bootclasspaths is not zero;
     * <BR>&nbsp;&nbsp; - there are no extra data in the reply packet;
     */
    public void testClassPaths001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassPathsCommand);
        logWriter.println("\trequest class paths");
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ClassPaths command");

        String baseDir = reply.getNextValueAsString();
        logWriter.println("baseDir = " + baseDir);
        assertTrue("baseDir = " + baseDir + " doesn't exists",
                new File(baseDir).exists());

        int classpaths = reply.getNextValueAsInt();
        logWriter.println("classpaths = " + classpaths);
        for (int i = 0; i < classpaths; i++) {
            String path = reply.getNextValueAsString();
            logWriter.println("\t" + path);
            if(!(path.length() > 0)){
                logWriter.println("Path length = "+path.length());
            }
        }

        int bootclasspaths = reply.getNextValueAsInt();
        logWriter.println("bootclasspaths = " + bootclasspaths);
        assertTrue(bootclasspaths > 0);
        for (int i = 0; i < bootclasspaths; i++) {
            String path = reply.getNextValueAsString();
            logWriter.println("\t" + path);
            assertTrue("Invalid path", path.length() > 0);
        }

        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClassPathsTest.class);
    }
}
