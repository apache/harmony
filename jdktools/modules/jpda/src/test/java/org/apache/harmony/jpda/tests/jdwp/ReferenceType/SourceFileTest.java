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
 * @author Anatoly F. Bondarenko
 */

/**
 * Created on 21.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.SourceFile command.
 */
public class SourceFileTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.SourceFile command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/SourceFileDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.SourceFileDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.SourceFile command.
     * >BR>The test starts SourceFileDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.SourceFile command and checks that returned
     * source file name is equal to expected name.
     */
    public void testSourceFile001() {
        String thisTestName = "testSourceFile001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        finalSyncMessage = JPDADebuggeeSynchronizer.SGNL_CONTINUE;

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket sourceFileCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SourceFileCommand);
        sourceFileCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket sourceFileReply = debuggeeWrapper.vmMirror.performCommand(sourceFileCommand);
        sourceFileCommand = null;
        checkReplyPacket(sourceFileReply, thisCommandName);

        String returnedSourceFile = sourceFileReply.getNextValueAsString();
        String expectedSourceFile = "SourceFileDebuggee.java";

        assertString(thisCommandName + " returned invalid source file,",
                expectedSourceFile, returnedSourceFile);

        logWriter.println("=> CHECK: PASSED: expected source file is returned = " +
                returnedSourceFile);

        finalSyncMessage = null;
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");

        assertAllDataRead(sourceFileReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SourceFileTest.class);
    }
}
