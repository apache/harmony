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
 * Created on 24.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.ClassObject command.
 */
public class ClassObjectTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "ReferenceType.ClassObject command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/ClassObjectDebuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.ClassObjectDebuggee";
    }

    /**
     * This testcase exercises ReferenceType.ClassObject command.
     * <BR>The test starts ClassObjectDebuggee class, requests referenceTypeId
     * for this class by VirtualMachine.ClassesBySignature command, then
     * performs ReferenceType.ClassObject command and checks that returned
     * class object is object of reference type 'java/lang/Class'.
     */
    public void testClassObject001() {
        String thisTestName = "testClassObject001";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        //int testStatus = testStatusPassed;
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long refTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + refTypeID);
        
        String debuggeeExtraClassSignature
            = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/ClassObjectDebuggee_ExtraClass;";

        long debuggeeExtraClassRefTypeID = getClassIDBySignature(debuggeeExtraClassSignature);

        logWriter.println("=> referenceTypeID for Debuggee extra class = " + debuggeeExtraClassRefTypeID);
        logWriter.println("=> CHECK: send " + thisCommandName + " and check reply...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.ClassObjectCommand);
        checkedCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        checkReplyPacket(checkedReply, thisCommandName);
        
        long classObjectID = checkedReply.getNextValueAsObjectID();
        logWriter.println("=> Returned class ObjectID = " + classObjectID);
        logWriter.println("=> Get reference type for returned class Object...");

        CommandPacket referenceTypeCommand = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
        referenceTypeCommand.setNextValueAsObjectID(classObjectID);
        
        ReplyPacket referenceTypeReply = debuggeeWrapper.vmMirror.performCommand(referenceTypeCommand);
        referenceTypeCommand = null;
        
        checkReplyPacket(referenceTypeReply, "ObjectReference::ReferenceType command");
        
        referenceTypeReply.getNextValueAsByte();
        // refTypeTag of class - is NOT used here

        long refTypeIDOfClassObject = referenceTypeReply.getNextValueAsReferenceTypeID();
        referenceTypeReply = null;

        logWriter.println("=> ReferenceTypeID of ClassObject = " + refTypeIDOfClassObject);
        logWriter.println("=> Get signature for reference type of ClassObject...");

        CommandPacket signatureCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        signatureCommand.setNextValueAsReferenceTypeID(refTypeIDOfClassObject);
        
        ReplyPacket signatureReply = debuggeeWrapper.vmMirror.performCommand(signatureCommand);
        signatureCommand = null;
        checkReplyPacket(signatureReply, "ReferenceType::Signature command");
        
        String returnedSignature = signatureReply.getNextValueAsString();
        signatureReply = null;
        logWriter.println("=> Signature of reference type of ClassObject = " + returnedSignature);

        String expectedSignature = "Ljava/lang/Class;";
        assertString("Invalid signature of reference type of ClassObject,", expectedSignature, returnedSignature);
        logWriter.println("=> CHECK: PASSED: expected signature is returned");

        assertAllDataRead(checkedReply);

        CommandPacket classLoaderCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.ClassLoaderCommand);
        classLoaderCommand.setNextValueAsReferenceTypeID(refTypeID);
        
        ReplyPacket classLoaderReply = debuggeeWrapper.vmMirror.performCommand(classLoaderCommand);
        classLoaderCommand = null;
        checkReplyPacket(classLoaderReply, "ReferenceType::ClassLoader command");
        
        long classLoaderID = classLoaderReply.getNextValueAsObjectID();
        logWriter.println("=> ClassLoaderID of debuggee = " + classLoaderID);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClassObjectTest.class);
    }
}
