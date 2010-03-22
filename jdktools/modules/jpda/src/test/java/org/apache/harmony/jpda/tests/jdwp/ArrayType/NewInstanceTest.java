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
 * Created on 10.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ArrayType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ArrayType.NewInstance command.
 */

public class NewInstanceTest extends JDWPSyncTestCase {

    /**
     * Returns full name of debuggee class which is used by this test.
     * @return full name of debuggee class.
     */
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ArrayType.NewInstanceDebuggee";
    }

    /**
     * This testcase exercises ArrayType.NewInstance command.
     * <BR>Starts <A HREF="../share/debuggee/HelloWorld.html">HelloWorld</A> debuggee. 
     * <BR>Creates new instance of array by ArrayType.NewInstance command,
     * check it length by ArrayReference.Length command.
     */
    public void testNewInstance001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        String[] testedArrayRefs = {
            "[Lorg/apache/harmony/jpda/tests/jdwp/ArrayType/checkClass;",
            "[Ljava/lang/String;",
            "[I",
            "[[Lorg/apache/harmony/jpda/tests/jdwp/ArrayType/checkClass;",
            "[[Ljava/lang/String;",
            "[[I"
        };
        for (int i = 0; i < testedArrayRefs.length; i++) {
            logWriter.println("Checking reference " + testedArrayRefs[i]);

            // Get referenceTypeID
            CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
            packet.setNextValueAsString(testedArrayRefs[i]);
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");

            int classes = reply.getNextValueAsInt();
            if (classes <= 0) {
                logWriter.println("## WARNING: class " + testedArrayRefs[i]+ " is not loadded ");
                continue;
            }

            byte refInitTypeTag = reply.getNextValueAsByte();
            long typeArrayID = reply.getNextValueAsReferenceTypeID();
            int status = reply.getNextValueAsInt();

            assertEquals("VirtualMachine::ClassesBySignature returned invalid TypeTag,",
                    JDWPConstants.TypeTag.ARRAY, refInitTypeTag,
                    JDWPConstants.TypeTag.getName(JDWPConstants.TypeTag.ARRAY),
                    JDWPConstants.TypeTag.getName(refInitTypeTag));

            logWriter.println(" VirtualMachine.ClassesBySignature: classes="
                + classes + " refTypeTag=" + refInitTypeTag + " typeID= "
                + typeArrayID + " status=" + status);

            // Make NewInstance
            packet = new CommandPacket(
                JDWPCommands.ArrayTypeCommandSet.CommandSetID,
                JDWPCommands.ArrayTypeCommandSet.NewInstanceCommand);
            packet.setNextValueAsReferenceTypeID(typeArrayID);
            packet.setNextValueAsInt(10);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "ArrayType::NewInstance command");
            
            TaggedObject newArray = reply.getNextValueAsTaggedObject();
            assertAllDataRead(reply);
            assertNotNull("ArrayType::NewInstance returned null newArray", newArray);

            logWriter.println("ArrayType.NewInstance: newArray.tag="
                + newArray.tag + " newArray.objectID=" + newArray.objectID);

            if (newArray.tag != JDWPConstants.Tag.ARRAY_TAG) {
                logWriter.println("##FAILURE: typeTag is not ARRAY_TAG");
                fail("Returned tag " + JDWPConstants.Tag.getName(newArray.tag)
                    + "(" + newArray.tag + ") is not ARRAY_TAG");
            }

            // Let's check array length
            packet = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.LengthCommand);
            packet.setNextValueAsObjectID(newArray.objectID);
            reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "ArrayReference::Length command");

            int arrayLength = reply.getNextValueAsInt();
            logWriter.println("ArrayReference.Length: arrayLength=" + arrayLength);
            assertEquals("ArrayReference::Length command returned ivalid array length,",
                    10, arrayLength);
            assertAllDataRead(reply);
        }
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(NewInstanceTest.class);
    }

}