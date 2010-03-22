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
 * JDWP Unit test for ReferenceType.MethodsWithGeneric command.
 */
public class MethodsWithGenericTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.MethodsWithGeneric command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/MethodsWithGenericDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.MethodsWithGenericDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.ClassObject command.
     * <BR>The test starts MethodsWithGenericDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.MethodsWithGeneric command and checks that returned
     * list of methods corresponds to expected list of methods with expected attributes.
     */
    public void testMethodsWithGeneric001() {
        String thisTestName = "testMethodsWithGeneric001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        int testStatus = testStatusPassed;
        String failMessage = "";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket methodsWithGenericCommand = new CommandPacket(
            JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
            JDWPCommands.ReferenceTypeCommandSet.MethodsWithGenericCommand);
        methodsWithGenericCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket methodsWithGenericReply = debuggeeWrapper.vmMirror.performCommand(methodsWithGenericCommand);
        methodsWithGenericCommand = null;
        checkReplyPacket(methodsWithGenericReply, thisCommandName);

        int returnedMethodsNumber = methodsWithGenericReply.getNextValueAsInt();
        logWriter.println("=> Returned methods number = " + returnedMethodsNumber);

        String methodNames[] = {
                "staticTestMethod",
                "objectTestMethod",
                "run",
                "main",
                "<init>"
        };
        
        String methodSignatures[] = {
                "(D)Z",
                "(Ljava/util/Collection;)Ljava/lang/Object;",
                "()V",
                "([Ljava/lang/String;)V",
                "()V"
        };
            
        String methodGenericSignatures[] = {
                "",
                "",
                "",
                "",
                ""
        };
            
        int methodModifiers[] = {
                0x8, // ACC_STATIC flag
                0x0,
                0x1, // ACC_PUBLIC flag
                0x9, // ACC_STATIC | ACC_PUBLIC flags
                0x1  // ACC_PUBLIC flag
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

        logWriter.println("=> CHECK for all expected methods...");
        for (int i = 0; i < returnedMethodsNumber; i++) {
            long returnedMethodID = methodsWithGenericReply.getNextValueAsMethodID();
            String returnedMethodName = methodsWithGenericReply.getNextValueAsString();
            String returnedMethodSignature = methodsWithGenericReply.getNextValueAsString();
            String returnedGenericSignature = methodsWithGenericReply.getNextValueAsString();
            int returnedMethodModifiers = methodsWithGenericReply.getNextValueAsInt();
            logWriter.println("\n=> Method ID = " + returnedMethodID);
            logWriter.println("=> Method name = " + returnedMethodName);
            logWriter.println("=> Method signature = \"" + returnedMethodSignature + "\"");
            logWriter.println("=> Method generic signature = \"" + returnedGenericSignature + "\"");
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
                        "The method is found repeatedly in the list: " +
                        returnedMethodName + ";\n";
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
                        "Unexpected method signature is returned:" +
                        ", Method = " + returnedMethodName +
                        ", Expected = " + methodSignatures[k] +
                        ", Returned = " + returnedMethodSignature + ";\n";
                }
                if ( ! methodGenericSignatures[k].equals(returnedGenericSignature) ) {
                    logWriter.println("\n## FAILURE: Unexpected method generic signature is returned:");
                    logWriter.println("## Method name = " + returnedMethodName);
                    logWriter.println
                    ("## Expected generic signature = " + methodGenericSignatures[k]);
                    logWriter.println
                    ("## Returned generic signature = " + returnedGenericSignature);
                    testStatus = testStatusFailed;
                    failMessage = failMessage +
                        "Unexpected method generic signature is returned:" +
                        ", Method = " + returnedMethodName +
                        ", Expected = " + methodGenericSignatures[k] +
                        ", Returned = " + returnedGenericSignature + ";\n";
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
                        "Unexpected method modifiers are returned:" +
                        ", Method name = " + returnedMethodName +
                        ", Expected modifiers = 0x" + Integer.toHexString(methodModifiers[k]) +
                        ", Returned modifiers = 0x" + Integer.toHexString(returnedMethodModifiers) + ";\n";
                }
                break;
            }
            if ( k == expectedMetodsNumber ) {
                // returned method is not found out in the list of expected methos
                logWriter.println("\n## FAILURE: It is found out unexpected returned method:");
                logWriter.println("## Method name = " + returnedMethodName);
                logWriter.println("## Method signature = " + returnedMethodSignature);
                logWriter.println("## Method generic signature = " + returnedGenericSignature);
                logWriter.println
                ("## Method modifiers = 0x" + Integer.toHexString(returnedMethodModifiers));
                testStatus = testStatusFailed;
                failMessage = failMessage +
                    "Unexpected method has been found: " +
                    ", name = " + returnedMethodName +
                    ", signature = " + returnedMethodSignature +
                    ", generic signature = " + returnedGenericSignature +
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
                    "Expected method is NOT found in the list of retuned methods: " +
                    " name = " + methodNames[k];
            }
        }
        
        if (testStatus == testStatusPassed) {
            logWriter.println
            ("=> CHECK PASSED: All expected methods are found out and have expected attributes");
        }

        assertAllDataRead(methodsWithGenericReply);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
        
        if (testStatus == testStatusFailed) {
            fail(failMessage);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MethodsWithGenericTest.class);
    }
}
