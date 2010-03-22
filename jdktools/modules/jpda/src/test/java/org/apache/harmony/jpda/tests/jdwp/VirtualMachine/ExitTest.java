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
 * Created on 31.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPTestCase;


/**
 * JDWP Unit test for VirtualMachine.Exit command.
 */
public class ExitTest extends JDWPTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.SimpleHelloWorld";
    }

    /**
     * This testcase exercises VirtualMachine.Exit command.
     * <BR>At first the test starts HelloWorld debuggee.
     * <BR> Then the test performs VirtualMachine.Exit command 
     * with exit code = 99 and checks that debuggee exit code, received
     * with help of debuggeeWrapper of test framework, is the same.
     */
    public void testExit001() {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ExitCommand);
        packet.setNextValueAsInt(99);
        
        debuggeeWrapper.vmMirror.performCommand(packet);
        
        int exitCode = 0;
        boolean exitLoop;

        long timeOut = settings.getTimeout();
        long startTime = System.currentTimeMillis();
        do {
            try {
                exitCode = debuggeeWrapper.process.exitValue();
                exitLoop = true;
            } catch (IllegalThreadStateException e) {
                exitLoop = false;
            }
            if (System.currentTimeMillis() - startTime > timeOut) {
                printErrorAndFail("Out of time: debugger did get "
                        + "no exit codes of debuggee");
                break;
            }
        } while (!exitLoop);

        logWriter.println("exitCode = " + exitCode);
        assertEquals("Invalid exit code,", 99, exitCode);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ExitTest.class);
    }
}