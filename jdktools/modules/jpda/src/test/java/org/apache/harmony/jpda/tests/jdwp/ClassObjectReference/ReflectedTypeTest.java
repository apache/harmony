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
 * @author Viacheslav G. Rybalov
 */

/**
 * Created on 05.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassObjectReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ClassObjectReference.ReflectedType command.
 */

public class ReflectedTypeTest extends JDWPSyncTestCase {

    /**
     * Returns full name of debuggee class which is used by this test.
     * @return full name of debuggee class.
     */
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises ClassObjectReference.ReflectedType command.
     * <BR>Starts <A HREF="../share/debuggee/HelloWorld.html">HelloWorld</A> debuggee. 
     * <BR>Then checks the following four classes: 
     * <BR>&nbsp;&nbsp; - java/lang/Object; 
     * <BR>&nbsp;&nbsp; - java/lang/String;
     * <BR>&nbsp;&nbsp; - java/lang/Runnable; 
     * <BR>&nbsp;&nbsp; - HelloWorld.
     * <BR>&nbsp;&nbsp;
     * <BR>The following statements are checked: 
     * <BR>&nbsp;It is expected:
     * <BR>&nbsp;&nbsp; - refTypeTag takes one of the TypeTag constants: CLASS, INTERFACE;
     * <BR>&nbsp;&nbsp; - refTypeTag equals to refTypeTag returned by command 
     *  VirtualMachine.ClassesBySignature;
     * <BR>&nbsp;&nbsp; - typeID equals to typeID returned by the JDWP command 
     * VirtualMachine.ClassesBySignature;
     * <BR>&nbsp;&nbsp; - All data were read; 
     */
    public void testReflectedType001() {
        logWriter.println("==> testReflectedType001 START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        String[] testedClasses = {
            "java/lang/Object",
            "java/lang/String",
            "java/lang/Runnable",
            "org/apache/harmony/jpda/tests/jdwp/share/debuggee/HelloWorld",
        };

        byte expectedRefTypeTags[] = {
            JDWPConstants.TypeTag.CLASS,
            JDWPConstants.TypeTag.CLASS,
            JDWPConstants.TypeTag.INTERFACE,
            JDWPConstants.TypeTag.CLASS,
        };

        for (int i = 0; i < testedClasses.length; i++) {
            logWriter.println("\n==> Checked class: " + testedClasses[i]);

            // Get referenceTypeID
            logWriter.println
            ("==> Send VirtualMachine::ClassesBySignature command for checked class...");
            CommandPacket packet = new CommandPacket(
                    JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                    JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
            packet.setNextValueAsString("L" + testedClasses[i] + ";");
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");

            int classes = reply.getNextValueAsInt();
            logWriter.println("==> Number of returned classes = " + classes);
            //this class may be loaded only once
            byte refInitTypeTag = 0;
            long typeInitID = 0;
            int status = 0;

            for (int j = 0; j < classes; j++) {
                refInitTypeTag = reply.getNextValueAsByte();
                typeInitID = reply.getNextValueAsReferenceTypeID();
                status = reply.getNextValueAsInt();
                logWriter.println("==> refTypeId["+j+"] = " + typeInitID);
                logWriter.println("==> refTypeTag["+j+"] = " + refInitTypeTag + "(" 
                        + JDWPConstants.TypeTag.getName(refInitTypeTag) + ")");
                logWriter.println("==> classStatus["+j+"] = " + status + "(" 
                        + JDWPConstants.ClassStatus.getName(status) + ")");
                
                String classSignature = debuggeeWrapper.vmMirror.getClassSignature(typeInitID);
                logWriter.println("==> classSignature["+j+"] = " + classSignature);
                
                packet = new CommandPacket(
                    JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                    JDWPCommands.ReferenceTypeCommandSet.ClassLoaderCommand);
                packet.setNextValueAsReferenceTypeID(typeInitID);
                ReplyPacket reply2 = debuggeeWrapper.vmMirror.performCommand(packet);
                checkReplyPacket(reply, "ReferenceType::ClassLoader command");

                long classLoaderID = reply2.getNextValueAsObjectID();
                logWriter.println("==> classLoaderID["+j+"] = " + classLoaderID);

                if (classLoaderID != 0) {
                    String classLoaderSignature = getObjectSignature(classLoaderID);
                    logWriter.println("==> classLoaderSignature["+j+"] = " + classLoaderSignature);
                } else {
                    logWriter.println("==> classLoader is system class loader");
                }
            }

            assertEquals("VirtualMachine::ClassesBySignature returned invalid number of classes,",
                    1, classes);
            assertEquals("VirtualMachine::ClassesBySignature returned invalid TypeTag,",
                    expectedRefTypeTags[i], refInitTypeTag,
                    JDWPConstants.TypeTag.getName(expectedRefTypeTags[i]),
                    JDWPConstants.TypeTag.getName(refInitTypeTag));

            // Get ClassObject
            logWriter.println
            ("==> Send ReferenceType::ClassObject command for checked class...");
            packet = new CommandPacket(
                    JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                    JDWPCommands.ReferenceTypeCommandSet.ClassObjectCommand);
            packet.setNextValueAsReferenceTypeID(typeInitID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "ReferenceType::ClassObject command");

            long classObject = reply.getNextValueAsClassObjectID();
            assertAllDataRead(reply);
            logWriter.println("==> classObjectID=" + classObject);

            // Get ReflectedType
            logWriter.println
            ("==> Send ClassObjectReference::ReflectedType command for classObjectID...");
            packet = new CommandPacket(
                    JDWPCommands.ClassObjectReferenceCommandSet.CommandSetID,
                    JDWPCommands.ClassObjectReferenceCommandSet.ReflectedTypeCommand);
            packet.setNextValueAsObjectID(classObject);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "ClassObjectReference::ReflectedType command");

            byte refTypeTag = reply.getNextValueAsByte();
            long typeID = reply.getNextValueAsReferenceTypeID();
            logWriter.println("==> reflectedTypeId = " + typeID);
            logWriter.println("==> reflectedTypeTag = " + refTypeTag 
                    + "(" + JDWPConstants.TypeTag.getName(refTypeTag) + ")");

            assertEquals("ClassObjectReference::ReflectedType returned invalid reflected TypeTag,",
                    expectedRefTypeTags[i], refTypeTag,
                    JDWPConstants.TypeTag.getName(expectedRefTypeTags[i]),
                    JDWPConstants.TypeTag.getName(refTypeTag));
            assertEquals("ClassObjectReference::ReflectedType returned invalid reflected typeID,",
                    typeInitID, typeID);
            assertAllDataRead(reply);
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReflectedTypeTest.class);
    }
}
