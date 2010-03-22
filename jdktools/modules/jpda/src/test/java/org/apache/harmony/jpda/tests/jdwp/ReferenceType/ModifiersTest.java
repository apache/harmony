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
 * Created on 17.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.Modifiers command.
 */
public class ModifiersTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.Modifiers command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/HelloWorld;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises ReferenceType.Modifiers command.
     * <BR>The test starts HelloWorld debuggee, requests referenceTypeId
     * for it by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Modifiers command and checks that returned
     * Modifiers contain expected flags: ACC_PUBLIC, ACC_SUPER;
     * but do NOT contain flags: ACC_FINAL, ACC_INTERFACE, ACC_ABSTRACT
     */
    public void testModifiers001() {
        String thisTestName = "testModifiers001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        String failMessage = "";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK1: send " + thisCommandName + " and check reply...");

        CommandPacket modifiersCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.ModifiersCommand);
        modifiersCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket modifiersReply = debuggeeWrapper.vmMirror.performCommand(modifiersCommand);
        modifiersCommand = null;
        checkReplyPacket(modifiersReply, thisCommandName);
        
        int returnedModifiers = modifiersReply.getNextValueAsInt();
/*
 * The value of the access_flags item is a mask of modifiers used with class and 
 * interface declarations. The access_flags modifiers are:
 * Flag Name      Value   Meaning                                               Used By  
 * ACC_PUBLIC     0x0001  Is public; may be accessed from outside its package.  Class, interface  
 * ACC_FINAL      0x0010  Is final; no subclasses allowed.                      Class  
 * ACC_SUPER      0x0020  Treat superclass methods specially in invokespecial.  Class, interface  
 * ACC_INTERFACE  0x0200  Is an interface.                                      Interface  
 * ACC_ABSTRACT   0x0400  Is abstract; may not be instantiated.                 Class, interface  
 */        
        logWriter.println("=> Returned modifiers = 0x" + Integer.toHexString(returnedModifiers));

        int publicFlag = 0x0001; // expected
        int finalFlag = 0x0010; // unexpected
        int superFlag = 0x0020; // expected
        int interfaceFlag = 0x0200; // unexpected
        int abstractFlag = 0x0400; // unexpected

        if ( (returnedModifiers & publicFlag) == 0 ) {
            logWriter.println
                ("## CHECK1: FAILURE: Returned modifiers do NOT contain expected ACC_PUBLIC flag(0x0001)");
            failMessage = failMessage +
                "Returned modifiers do NOT contain expected ACC_PUBLIC flag(0x0001);\n";
        }
        if ( (returnedModifiers & superFlag) == 0 ) {
            logWriter.println
                ("## CHECK1: FAILURE: Returned modifiers do NOT contain expected ACC_SUPER flag(0x0020)");
            failMessage = failMessage +
                "Returned modifiers do NOT contain expected ACC_SUPER flag(0x0020);\n";
        }
        if ( (returnedModifiers & finalFlag) != 0 ) {
            logWriter.println
                ("## CHECK1: FAILURE: Returned modifiers contain unexpected ACC_FINAL flag(0x0010)");
            failMessage = failMessage +
                "Returned modifiers contain unexpected ACC_FINAL flag(0x0010);\n";
        }
        if ( (returnedModifiers & interfaceFlag) != 0 ) {
            logWriter.println
                ("## CHECK1: FAILURE: Returned modifiers contain unexpected ACC_INTERFACE flag(0x0200)");
            failMessage = failMessage +
                "Returned modifiers contain unexpected ACC_INTERFACE flag(0x0200);\n";
        }
        if ( (returnedModifiers & abstractFlag) != 0 ) {
            logWriter.println
                ("## CHECK1: FAILURE: Returned modifiers contain unexpected ACC_ABSTRACT flag(0x0400)");
            failMessage = failMessage +
                "Returned modifiers contain unexpected ACC_ABSTRACT flag(0x0400);\n";
        }

        logWriter.println
        ("=> CHECK1: PASSED: expected modifiers are returned: ACC_PUBLIC flag(0x0001), ACC_SUPER flag(0x0020)");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        
        if (failMessage.length() > 0) {
            fail(failMessage);
        }

        assertAllDataRead(modifiersReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ModifiersTest.class);
    }
}
