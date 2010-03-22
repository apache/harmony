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
 * Created on 18.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.Methods command.
 */
public class MethodsTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.Methods command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/MethodsDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.MethodsDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.Methods command.
     * <BR>The test starts MethodsDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.Methods command and checks that returned
     * list of methods corresponds to expected list of methods with expected attributes.
     */
    public void testMethods001() {
        String thisTestName = "testMethods001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        int testStatus = testStatusPassed;
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket methodsCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        methodsCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket methodsReply = debuggeeWrapper.vmMirror.performCommand(methodsCommand);
        methodsCommand = null;
        checkReplyPacket(methodsReply, thisCommandName);
        
        int returnedMethodsNumber = methodsReply.getNextValueAsInt();
        logWriter.println("=> Returned methods number = " + returnedMethodsNumber);

        String methodNames[] = {
                "staticTestMethod",
                "objectTestMethod",
                "run",
                "main",
                "<init>"
        };
        
        String methodSignatures[] = {
                "(J)I",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                "()V",
                "([Ljava/lang/String;)V",
                "()V"
        };
            
        int methodModifiers[] = {
                0x8,
                0x0,
                0x1,
                0x9,
                0x1
        };

        boolean methodFound[] = {
                false,
                false,
                false,
                false,
                false
        };
        int expectedMetodsNumber = methodNames.length;
        int methodSyntheticFlag = 0xf0000000;
        String failMessage = "";

        logWriter.println("=> CHECK for all expected methods...");
        for (int i = 0; i < returnedMethodsNumber; i++) {
            long returnedMethodID = methodsReply.getNextValueAsMethodID();
            String returnedMethodName = methodsReply.getNextValueAsString();
            String returnedMethodSignature = methodsReply.getNextValueAsString();
            int returnedMethodModifiers = methodsReply.getNextValueAsInt();
            logWriter.println("\n=> Method ID = " + returnedMethodID);
            logWriter.println("=> Method name = " + returnedMethodName);
            logWriter.println("=> Method signature = " + returnedMethodSignature);
            logWriter.println("=> Method modifiers = 0x" + Integer.toHexString(returnedMethodModifiers));
            if ( (returnedMethodModifiers & methodSyntheticFlag) == methodSyntheticFlag ) {
                continue; // do not check synthetic methods
            }
            int k = 0;
            for (; k < expectedMetodsNumber; k++) {
                if ( ! methodNames[k].equals(returnedMethodName)) {
                    continue;
                }
                if ( methodFound[k] ) {
                    logWriter.println("\n## FAILURE: The method is found out repeatedly in the list");
                    logWriter.println("## Method name = " + returnedMethodName);
                    testStatus = testStatusFailed;
                    failMessage = failMessage +
                        "The method '" + returnedMethodName +
                        "' is found repeatedly in the list;\n";
                    break;
                }
                methodFound[k] = true;
                if ( ! methodSignatures[k].equals(returnedMethodSignature) ) {
                    logWriter.println("\n## FAILURE: Unexpected method signature is returned:");
                    logWriter.println("## Method name = " + returnedMethodName);
                    logWriter.println("## Expected signature = " + methodSignatures[k]);
                    logWriter.println("## Returned signature = " + returnedMethodSignature);
                    testStatus = testStatusFailed;
                    failMessage = failMessage +
                        "Unexpected signature is returned for method: " +
                        returnedMethodName +
                        ", Expected: " + methodSignatures[k] +
                        ", Returned: " + returnedMethodSignature + ";\n";
                }
                if ( methodModifiers[k] != returnedMethodModifiers ) {
                    logWriter.println("\n## FAILURE: Unexpected method modifiers are returned:");
                    logWriter.println("## Method name = " + returnedMethodName);
                    logWriter.println
                    ("## Expected modifiers = 0x" + Integer.toHexString(methodModifiers[k]));
                    logWriter.println
                    ("## Returned modifiers = 0x" + Integer.toHexString(returnedMethodModifiers));
                    testStatus = testStatusFailed;
                    failMessage = failMessage +
                        "Unexpected modifiers are returned for method: " +
                        returnedMethodName +
                        ", Expected: 0x" + Integer.toHexString(methodModifiers[k]) +
                        ", Returned: 0x" + Integer.toHexString(returnedMethodModifiers) + ";\n";
                }
                break;
            }
            if ( k == expectedMetodsNumber ) {
                // returned method is not found out in the list of expected methods
                logWriter.println("\n## FAILURE: It is found out unexpected returned method:");
                logWriter.println("## Method name = " + returnedMethodName);
                logWriter.println("## Method signature = " + returnedMethodSignature);
                logWriter.println
                ("## Method modifiers = 0x" + Integer.toHexString(returnedMethodModifiers));
                testStatus = testStatusFailed;
                failMessage = failMessage +
                    "Unexpected returned method is found:" +
                    ", name = " + returnedMethodName +
                    ", signature = " + returnedMethodSignature +
                    ", modifiers = 0x" + Integer.toHexString(returnedMethodModifiers) + ";\n";
            }
        }
        for (int k=0; k < expectedMetodsNumber; k++) {
            if ( ! methodFound[k] ) {
                logWriter.println
                ("\n## FAILURE: Expected method is NOT found out in the list of retuned methods:");
                logWriter.println("## Method name = " + methodNames[k]);
                testStatus = testStatusFailed;
                failMessage = failMessage +
                    "Expected method is NOT found in the list of retuned methods:" +
                    " name = " + methodNames[k];
            }
        }
        if ( testStatus == testStatusPassed ) {
            logWriter.println
            ("=> CHECK PASSED: All expected methods are found out and have expected attributes");
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        if (testStatus == testStatusFailed) {
            fail(failMessage);
        }
        assertAllDataRead(methodsReply);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MethodsTest.class);
    }
}
