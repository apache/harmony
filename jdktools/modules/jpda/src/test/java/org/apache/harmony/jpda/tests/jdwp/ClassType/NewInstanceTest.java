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
 * Created on 11.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ClassType.NewInstance command.
 */
public class NewInstanceTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.InvokeMethodDebuggee";
    }

    /**
     * This testcase exercises ClassType.NewInstance command.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - send ClassType.NewInstance command for class,
     * constructor of which should not throw any Exception, and checks,
     * that returned new object has expected attributes and returned
     * exception object is null;
     * <BR>&nbsp;&nbsp; - send ClassType.NewInstance command for class,
     * constructor of which should throw some Exception, and checks,
     * that returned new object is null and returned exception object 
     * is not null and has expected attributes;
     */
    public void testNewInstance001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Get referenceTypeID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        String classSig = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/testClass2;";
        packet.setNextValueAsString(classSig);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");

        int classes = reply.getNextValueAsInt();
        assertTrue(classes == 1); //this class may be loaded only once
        byte refTypeTag = reply.getNextValueAsByte();
        long typeID = reply.getNextValueAsReferenceTypeID();
        int status = reply.getNextValueAsInt();
        logWriter.println(" VirtualMachine.ClassesBySignature: classes="
                + classes + "; refTypeTag=" + refTypeTag + "; typeID= " + typeID
                + "; status=" + status);

        assertAllDataRead(reply);
        assertEquals("VirtualMachine::ClassesBySignature returned invalid TypeTag,", JDWPConstants.TypeTag.CLASS, refTypeTag
                , JDWPConstants.TypeTag.getName(JDWPConstants.TypeTag.CLASS)
                , JDWPConstants.TypeTag.getName(refTypeTag));

        // Get methodID
        packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        packet.setNextValueAsClassID(typeID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::Methods command");

        int declared = reply.getNextValueAsInt();
        logWriter.println(" ReferenceType.Methods: declared=" + declared);
        long targetMethodID = 0;
        for (int i = 0; i < declared; i++) {
            long methodID = reply.getNextValueAsMethodID();
            String name = reply.getNextValueAsString();
            String signature = reply.getNextValueAsString();
            int modBits = reply.getNextValueAsInt();
            logWriter.println("  methodID=" + methodID + "; name=" + name
                    + ";  signature=" + signature + "; modBits=" + modBits);
            if (name.equals("<init>")) {
                targetMethodID = methodID;
            }
        }
        assertAllDataRead(reply);

        // Set EventRequest
        packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsByte((byte) 5);
        packet.setNextValueAsString("*.InvokeMethodDebuggee");

        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Set command");

        int requestID = reply.getNextValueAsInt();
        logWriter.println(" EventRequest.Set: requestID=" + requestID);
        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        long targetThreadID = 0;
        // Wait event
        CommandPacket event = debuggeeWrapper.vmMirror
                .receiveCertainEvent(JDWPConstants.EventKind.METHOD_ENTRY);
        byte suspendPolicy = event.getNextValueAsByte();
        int events = event.getNextValueAsInt();
        logWriter.println(" EVENT_THREAD event: suspendPolicy=" + suspendPolicy
                + " events=" + events);
        for (int i = 0; i < events; i++) {
            byte eventKind = event.getNextValueAsByte();
            int newRequestID = event.getNextValueAsInt();
            long threadID = event.getNextValueAsThreadID();
            //Location location =
                event.getNextValueAsLocation();
            logWriter.println("  EVENT_THREAD event " + i + ": eventKind="
                    + eventKind + "; requestID=" + newRequestID + "; threadID="
                    + threadID);
            if (newRequestID == requestID) {
                targetThreadID = threadID;
            }
        }
        assertAllDataRead(event);
        assertTrue("Invalid targetThreadID, must be != 0", targetThreadID != 0);

        //  Let's clear event request
        packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.ClearCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsInt(requestID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Clear command");
        assertAllDataRead(reply);

        // Make NewInstance without Exception
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.NewInstanceCommand);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(0);
        logWriter.println(" Send ClassType.NewInstance (without Exception)");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassType::NewInstance command");

        TaggedObject newObject = reply.getNextValueAsTaggedObject();
        logWriter.println(" ClassType.NewInstance: newObject.tag="
                + newObject.tag + "; newObject.objectID=" + newObject.objectID);
        TaggedObject exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ClassType.NewInstance: exception.tag="
                + exception.tag + "; exception.objectID=" + exception.objectID);

        assertNotNull("newObject is null", newObject);
        assertTrue("newObject.objectID is 0", newObject.objectID != 0);
        assertEquals("ClassType::NewInstance returned invalid newObject.tag,", JDWPConstants.Tag.OBJECT_TAG, newObject.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(newObject.tag));

        assertNotNull("exception is null", newObject);
        assertEquals("ClassType::NewInstance returned invalid exception.objectID,", 0, exception.objectID);
        assertTrue(exception.tag == JDWPConstants.Tag.OBJECT_TAG);
        assertAllDataRead(reply);

        //  Let's check object reference
        packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
        packet.setNextValueAsObjectID(newObject.objectID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ObjectReference::ReferenceType command");

        byte newRefTypeTag = reply.getNextValueAsByte();
        long newTypeID = reply.getNextValueAsReferenceTypeID();
        logWriter.println(" ObjectReference.ReferenceType: refTypeTag="
                + newRefTypeTag + "; typeID= " + newTypeID);
        assertEquals("ObjectReference::ReferenceType returned invalid newRefTypeTag,",
                JDWPConstants.TypeTag.CLASS, newRefTypeTag,
                JDWPConstants.TypeTag.getName(JDWPConstants.TypeTag.CLASS),
                JDWPConstants.TypeTag.getName(newRefTypeTag));
        assertEquals("Invalid type ID,", typeID, newTypeID);
        assertAllDataRead(reply);

        // Make NewInstance with Exception
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.NewInstanceCommand);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(true));
        packet.setNextValueAsInt(0);
        logWriter.println(" Send ClassType.NewInstance (with Exception)");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassType::NewInstance command");

        newObject = reply.getNextValueAsTaggedObject();
        logWriter.println(" ClassType.NewInstance: newObject.tag="
                + newObject.tag + "; newObject.objectID=" + newObject.objectID);
        exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ClassType.NewInstance: exception.tag="
                + exception.tag + "; exception.objectID=" + exception.objectID);
        assertNotNull("newObject is null", newObject);
        assertEquals("ClassType::NewInstance returned invalid newObject.objectID,",
                0, newObject.objectID);
        assertEquals("ClassType::NewInstance returned invalid invalid newObject.tag,",
                JDWPConstants.Tag.OBJECT_TAG, newObject.tag,
                JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG),
                JDWPConstants.Tag.getName(newObject.tag));

        assertNotNull("ClassType::NewInstance returned invalid exception = null", newObject);
        assertTrue("ClassType::NewInstance returned invalid exception.objectID = 0",
                exception.objectID != 0);
        assertEquals("ClassType::NewInstance returned invalid invalid exception.tag,",
                JDWPConstants.Tag.OBJECT_TAG, exception.tag,
                JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG),
                JDWPConstants.Tag.getName(exception.tag));
        assertAllDataRead(reply);

        //  Let's resume application
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ResumeCommand);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::Resume command");
        assertAllDataRead(reply);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ClassType.NewInstance command.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following check: 
     * <BR>&nbsp;&nbsp; - send ClassType.NewInstance command for class
     * and passes invalid arguments' list for constructor.
     * <BR>The testcase expects that ILLEGAL_ARGUMENT error is returned. 
     */
    public void testNewInstance002() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Get referenceTypeID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        String classSig = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/InvokeMethodDebuggee$testClass1;";
        packet.setNextValueAsString(classSig);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");

        int classes = reply.getNextValueAsInt();
        assertEquals("VirtualMachine::ClassesBySignature returned invalid cluss number,",
                1, classes); //this class may be loaded only once
        byte refTypeTag = reply.getNextValueAsByte();
        long typeID = reply.getNextValueAsReferenceTypeID();
        int status = reply.getNextValueAsInt();
        logWriter.println(" VirtualMachine.ClassesBySignature: classes="
                + classes + "; refTypeTag=" + refTypeTag + "; typeID= " + typeID
                + "; status=" + status);

        assertAllDataRead(reply);
        assertEquals("VirtualMachine::ClassesBySignature returned invalid TypeTag,",
                JDWPConstants.TypeTag.CLASS, refTypeTag,
                JDWPConstants.TypeTag.getName(JDWPConstants.TypeTag.CLASS),
                JDWPConstants.TypeTag.getName(refTypeTag));

        // Get methodID
        packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        packet.setNextValueAsClassID(typeID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::Methods command");

        int declared = reply.getNextValueAsInt();
        logWriter.println(" ReferenceType.Methods: declared=" + declared);
        long targetMethodID = 0;
        for (int i = 0; i < declared; i++) {
            long methodID = reply.getNextValueAsMethodID();
            String name = reply.getNextValueAsString();
            String signature = reply.getNextValueAsString();
            int modBits = reply.getNextValueAsInt();
            logWriter.println("  methodID=" + methodID + "; name=" + name
                    + ";  signature=" + signature + "; modBits=" + modBits);
            if (name.equals("<init>")) {
                targetMethodID = methodID;
            }
        }
        assertAllDataRead(reply);

        // Set EventRequest
        packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsByte((byte) 5);
        packet.setNextValueAsString("*.InvokeMethodDebuggee");

        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Set command");

        int requestID = reply.getNextValueAsInt();
        logWriter.println(" EventRequest.Set: requestID=" + requestID);
        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        long targetThreadID = 0;
        // Wait event
        CommandPacket event = debuggeeWrapper.vmMirror
                .receiveCertainEvent(JDWPConstants.EventKind.METHOD_ENTRY);
        byte suspendPolicy = event.getNextValueAsByte();
        int events = event.getNextValueAsInt();
        logWriter.println(" EVENT_THREAD event: suspendPolicy=" + suspendPolicy
                + " events=" + events);
        for (int i = 0; i < events; i++) {
            byte eventKind = event.getNextValueAsByte();
            int newRequestID = event.getNextValueAsInt();
            long threadID = event.getNextValueAsThreadID();
            //Location location =
                event.getNextValueAsLocation();
            logWriter.println("  EVENT_THREAD event " + i + ": eventKind="
                    + eventKind + "; requestID=" + newRequestID + "; threadID="
                    + threadID);
            if (newRequestID == requestID) {
                targetThreadID = threadID;
            }
        }
        assertAllDataRead(event);
        assertTrue("Invalid targetThreadID, must be != 0", targetThreadID != 0);

        //  Let's clear event request
        packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.ClearCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsInt(requestID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Clear command");
        assertAllDataRead(reply);

        // Make NewInstance without Exception
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.NewInstanceCommand);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(0); // Providing of 'this' arg missed!
        packet.setNextValueAsInt(0); // This int value will be interpreted as
                                     // reference!?
        logWriter.println(" Send ClassType.NewInstance");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);

        short error = reply.getErrorCode();
        logWriter.println(" ClassType.NewInstance: ErrorCode=" + error
            + "(" + JDWPConstants.Error.getName(error) + ")");
        assertEquals("ClassType.NewInstance returned invalid error code,",
                JDWPConstants.Error.ILLEGAL_ARGUMENT, error,
                JDWPConstants.Error.getName(JDWPConstants.Error.ILLEGAL_ARGUMENT),
                JDWPConstants.Error.getName(error));
        logWriter.println(" It is EXPECTED ERROR!");
        assertAllDataRead(reply);

        //  Let's resume application
        packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ResumeCommand);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::Resume command");
        assertAllDataRead(reply);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(NewInstanceTest.class);
    }
}