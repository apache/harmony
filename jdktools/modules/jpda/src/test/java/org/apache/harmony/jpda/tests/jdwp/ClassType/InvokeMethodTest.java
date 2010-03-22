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
 * JDWP unit test for ClassType.InvokeMethod command.
 */
public class InvokeMethodTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.InvokeMethodDebuggee";
    }

    /**
     * This testcase exercises ClassType.InvokeMethod command.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - send ClassType.InvokeMethod command for method,
     * which should not throw any Exception, and checks,
     * that returned value is expected int value and returned
     * exception object is null;
     * <BR>&nbsp;&nbsp; - send ClassType.InvokeMethod command for method,
     * which should throw some Exception, and checks, that
     * returned exception object is not null and has expected attributes;
     */
    public void testInvokeMethod001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Get referenceTypeID
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        String classSig = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/InvokeMethodDebuggee;";
        packet.setNextValueAsString(classSig);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");

        int classes = reply.getNextValueAsInt();
        assertEquals("VirtualMachine::ClassesBySignature returned invalid number of classes,",
                1, classes); //this class may be loaded only once
        byte refTypeTag = reply.getNextValueAsByte();
        long typeID = reply.getNextValueAsReferenceTypeID();
        int status = reply.getNextValueAsInt();
        assertAllDataRead(reply);
        assertEquals("VirtualMachine::ClassesBySignature returned Invalid type tag,",
                JDWPConstants.TypeTag.CLASS, refTypeTag,
                JDWPConstants.TypeTag.getName(JDWPConstants.TypeTag.CLASS),
                JDWPConstants.TypeTag.getName(refTypeTag));

        logWriter.println(" VirtualMachine.ClassesBySignature: classes="
                + classes + " refTypeTag=" + refTypeTag + " typeID= " + typeID
                + " status=" + status);

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
            logWriter.println("  methodID=" + methodID + " name=" + name
                    + " signature=" + signature + " modBits=" + modBits);
            if (name.equals("testMethod2")) {
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
        CommandPacket event = debuggeeWrapper.vmMirror.receiveEvent();
        byte suspendPolicy = event.getNextValueAsByte();
        int events = event.getNextValueAsInt();
        logWriter.println(" EVENT_THREAD event: suspendPolicy=" + suspendPolicy + " events=" + events);
        for (int i = 0; i < events; i++) {
            byte eventKind = event.getNextValueAsByte();
            int newRequestID = event.getNextValueAsInt();
            long threadID = event.getNextValueAsThreadID();
            //Location location =
                event.getNextValueAsLocation();
            logWriter.println("  EVENT_THREAD event " + i + ": eventKind="
                    + eventKind + " requestID=" + newRequestID + " threadID="
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
        
        // Make InvokeMethod without Exception
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.InvokeMethodCommand);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(1);
            packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(0);
        logWriter.println(" Send ClassType.InvokeMethod without Exception");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassType::InvokeMethod command");

        Value returnValue = reply.getNextValueAsValue();
        assertNotNull("Returned value is null", returnValue);
        assertEquals("Invalid returned value,", 234, returnValue.getIntValue());
        logWriter.println(" ClassType.InvokeMethod: returnValue.getIntValue()="
                + returnValue.getIntValue());

        TaggedObject exception = reply.getNextValueAsTaggedObject();
        assertNotNull("Returned exception is null", exception);
        assertTrue("Invalid exception object ID:<" + exception.objectID + ">", exception.objectID == 0);
        assertEquals("Invalid exception tag,", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));
        logWriter.println(" ClassType.InvokeMethod: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);
        assertAllDataRead(reply);

        // Make InvokeMethod with Exception
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.InvokeMethodCommand);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(1);
            packet.setNextValueAsValue(new Value(true));
        packet.setNextValueAsInt(0);
        logWriter.println(" Send ClassType.InvokeMethod with Exception");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassType::InvokeMethod command");

        returnValue = reply.getNextValueAsValue();
        logWriter.println(" ClassType.InvokeMethod: returnValue.getIntValue()="
                + returnValue.getIntValue());

        exception = reply.getNextValueAsTaggedObject();
        assertNotNull("Returned exception is null", exception);
        assertTrue("Invalid exception object ID:<" + exception.objectID + ">", exception.objectID != 0);
        assertEquals("Invalid exception tag,", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));
        logWriter.println(" ClassType.InvokeMethod: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);
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
     * This testcase exercises ClassType.InvokeMethod command.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - send ClassType.InvokeMethod command for method,
     * which actually does not belong to passed class (taking into account 
     * inheritance). 
     * <BR>Test expects that INVALID_METHODID error is returned by command.
     */
    public void testInvokeMethod002() {
        logWriter.println("==> testInvokeMethod002: START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("\n==> Getting debuggeeRefTypeID... ");
        String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/InvokeMethodDebuggee;";
        logWriter.println("==> debuggeeSignature = |" + debuggeeSignature + "|+");
        long debuggeeRefTypeID = debuggeeWrapper.vmMirror.getClassID(debuggeeSignature);
        if ( debuggeeRefTypeID == -1 ) {
            logWriter.println("## FAILURE: Can not get debuggeeRefTypeID!");
            fail("Can not get debuggeeRefTypeID!");
        }
        logWriter.println("==> debuggeeRefTypeID = " + debuggeeRefTypeID);

        logWriter.println("\n==> Getting testMethodID for debuggee method 'testMethod2'... ");
        String testMethodName = "testMethod2";
        long testMethodID = 
            debuggeeWrapper.vmMirror.getMethodID(debuggeeRefTypeID, testMethodName);
        if ( testMethodID == -1 ) {
            logWriter.println("## FAILURE: Can not get methodID!");
            fail("Can not get methodID!");
        }
        logWriter.println("==> testMethodID = " + testMethodID);

        logWriter.println("\n==> Setting EventRequest... ");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsByte((byte) 5);
        packet.setNextValueAsString("*.InvokeMethodDebuggee");

        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Set command");

        int requestID = reply.getNextValueAsInt();
        logWriter.println(" EventRequest.Set: requestID=" + requestID);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n==> Getting targetThreadID... ");
        long targetThreadID = 0;
        // Wait event
        CommandPacket event = debuggeeWrapper.vmMirror
                .receiveEvent();
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
                    + eventKind + " requestID=" + newRequestID + " threadID="
                    + threadID);
            if (newRequestID == requestID) {
                targetThreadID = threadID;
            }
        }
        logWriter.println("==> targetThreadID = " + targetThreadID);
        assertTrue("Invalid targetThreadID, must be != 0", targetThreadID != 0);

        logWriter.println("\n==> Clear EventRequest... ");
        packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.ClearCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsInt(requestID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Clear command");

        logWriter.println("\n==> Getting invalidClassRefTypeID... ");
        String invalidClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/testClass2;";
        logWriter.println("==> invalidClassSignature = |" + invalidClassSignature + "|+");
        long invalidClassRefTypeID = debuggeeWrapper.vmMirror.getClassID(invalidClassSignature);
        if ( invalidClassRefTypeID == -1 ) {
            logWriter.println("## FAILURE: Can not get invalidClassRefTypeID!");
            fail("Can not get invalidClassRefTypeID!");
        }
        logWriter.println("==> invalidClassRefTypeID = " + invalidClassRefTypeID);

        logWriter.println
        ("\n==> Send ClassType::InvokeMethod for invalidClassRefTypeID, testMethodID...");
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.InvokeMethodCommand);
        packet.setNextValueAsClassID(invalidClassRefTypeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(testMethodID);
        packet.setNextValueAsInt(1);
            packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(0);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        short errorCode = reply.getErrorCode();
        if (errorCode == JDWPConstants.Error.NONE) {
            logWriter.println
            ("## FAILURE: ClassType::InvokeMethod command does NOT return expected error - INVALID_METHODID");
            
            // next is only for extra info
            logWriter.println("\n==> Result if invoke method:");
            Value returnValue = reply.getNextValueAsValue();
            if (returnValue != null) {
                logWriter.println(" ClassType.InvokeMethod: returnValue.getIntValue()="
                        + returnValue.getIntValue());
            }

            TaggedObject exception = reply.getNextValueAsTaggedObject();
            if (exception != null) {
                logWriter.println(" ClassType.InvokeMethod: exception.tag="
                        + exception.tag + " exception.objectID=" + exception.objectID);
                if ( exception.objectID != 0 ) {
                    String exceptionSignature = getObjectSignature(exception.objectID);
                    logWriter.println(" exceptionSignature = " + exceptionSignature);
                }
            }
        }
        checkReplyPacket(reply, "ClassType::InvokeMethod command", JDWPConstants.Error.INVALID_METHODID);
        
        logWriter.println("==> PASSED: Expected error (INVALID_METHODID) is returned");
        logWriter.println("\n==> resumeDebuggee...");
        resumeDebuggee();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
    
    /**
     * This testcase exercises ClassType.InvokeMethod command.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - send ClassType.InvokeMethod command for method,
     * which actually is not static method. 
     * <BR>Test expects that INVALID_METHODID error is returned by command.
     */
    public void testInvokeMethod003() {
        logWriter.println("==> testInvokeMethod003: START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println("\n==> Getting debuggeeRefTypeID... ");
        String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/InvokeMethodDebuggee;";
        logWriter.println("==> debuggeeSignature = |" + debuggeeSignature + "|+");
        long debuggeeRefTypeID = debuggeeWrapper.vmMirror.getClassID(debuggeeSignature);
        if ( debuggeeRefTypeID == -1 ) {
            logWriter.println("## FAILURE: Can not get debuggeeRefTypeID!");
            fail("Can not get debuggeeRefTypeID!");
        }
        logWriter.println("==> debuggeeRefTypeID = " + debuggeeRefTypeID);

        logWriter.println("\n==> Getting nonStaticMethodID for debuggee method 'testMethod1'... ");
        String nonStaticMethodName = "testMethod1";
        long nonStaticMethodID = 
            debuggeeWrapper.vmMirror.getMethodID(debuggeeRefTypeID, nonStaticMethodName);
        if ( nonStaticMethodID == -1 ) {
            logWriter.println("## FAILURE: Can not get methodID!");
            fail("Can not get methodID!");
        }
        logWriter.println("==> nonStaticMethodID = " + nonStaticMethodID);

        logWriter.println("\n==> Setting EventRequest... ");
        CommandPacket packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsByte((byte) 5);
        packet.setNextValueAsString("*.InvokeMethodDebuggee");

        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Set command");

        int requestID = reply.getNextValueAsInt();
        logWriter.println(" EventRequest.Set: requestID=" + requestID);

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        logWriter.println("\n==> Getting targetThreadID... ");
        long targetThreadID = 0;
        // Wait event
        CommandPacket event = debuggeeWrapper.vmMirror
                .receiveEvent();
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
                    + eventKind + " requestID=" + newRequestID + " threadID="
                    + threadID);
            if (newRequestID == requestID) {
                targetThreadID = threadID;
            }
        }
        logWriter.println("==> targetThreadID = " + targetThreadID);
        assertTrue("Invalid targetThreadID, must be != 0", targetThreadID != 0);

        logWriter.println("\n==> Clear EventRequest... ");
        packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.ClearCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsInt(requestID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Clear command");

        logWriter.println
        ("\n==> Send ClassType::InvokeMethod for debuggeeRefTypeID, nonStaticMethodID...");
        packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.InvokeMethodCommand);
        packet.setNextValueAsClassID(debuggeeRefTypeID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsMethodID(nonStaticMethodID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(0);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        short errorCode = reply.getErrorCode();
        if (errorCode == JDWPConstants.Error.NONE) {
            logWriter.println
            ("## FAILURE: ClassType::InvokeMethod command does NOT return expected error - INVALID_METHODID");
            
            // next is only for extra info
            logWriter.println("\n==> Result if invoke method:");
            Value returnValue = reply.getNextValueAsValue();
            if (returnValue != null) {
                logWriter.println(" ClassType.InvokeMethod: returnValue.getIntValue()="
                        + returnValue.getIntValue());
            }

            TaggedObject exception = reply.getNextValueAsTaggedObject();
            if (exception != null) {
                logWriter.println(" ClassType.InvokeMethod: exception.tag="
                        + exception.tag + " exception.objectID=" + exception.objectID);
                if ( exception.objectID != 0 ) {
                    String exceptionSignature = getObjectSignature(exception.objectID);
                    logWriter.println(" exceptionSignature = " + exceptionSignature);
                }
            }
        }
        checkReplyPacket(reply, "ClassType::InvokeMethod command", JDWPConstants.Error.INVALID_METHODID);

        logWriter.println("==> PASSED: Expected error (INVALID_METHODID) is returned");
        logWriter.println("\n==> resumeDebuggee...");
        resumeDebuggee();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(InvokeMethodTest.class);
    }
}
