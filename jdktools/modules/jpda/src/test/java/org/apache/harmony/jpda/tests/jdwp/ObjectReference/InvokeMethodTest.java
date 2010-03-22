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
 * Created on 15.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ObjectReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TaggedObject;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * Unit test for NewInstance command.
 * Runs <code>InvokeMethodDebuggee</code>, creates NewInstance of testClass2:  
 * Case 1: with options
 * Case 2: without options
 * 
 * Then invokes it's testMethod3:
 * Case 1: with and without exceptions
 * Case 2: nonvirtual child method without exception and nonvirtual super method without exception
 */

/**
 * JDWP unit test for ObjectReference.InvokeMethod command.
 */
public class InvokeMethodTest extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.InvokeMethodDebuggee";
    }

    protected int setEventRequest() {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsByte(JDWPConstants.SuspendPolicy.ALL);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsByte((byte) 5);
        packet.setNextValueAsString("*.InvokeMethodDebuggee");

        logWriter.println("\nSend EventRequest::Set command...");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Set command");
        
        int requestID = reply.getNextValueAsInt();
        logWriter.println(" EventRequest.Set: requestID=" + requestID);
        assertTrue(reply.isAllDataRead());
        return requestID;
    }

    protected long waitEvent(int requestID) {
        long targetThreadID = 0;

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
                    + eventKind + " requestID=" + newRequestID + " threadID="
                    + threadID);
            if (newRequestID == requestID) {
                targetThreadID = threadID;
            }
        }
        assertAllDataRead(event);
        assertTrue("targetThreadID must be != 0", targetThreadID != 0);
        return targetThreadID;
    }

    protected void clearEvent(int requestID) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.ClearCommand);
        packet.setNextValueAsByte(JDWPConstants.EventKind.METHOD_ENTRY);
        packet.setNextValueAsInt(requestID);
        logWriter.println("\nSend EventRequest::Clear command...");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Clear command");
        assertAllDataRead(reply);
    }

    protected TaggedObject makeNewInstance(long typeID, long threadID,
            long constructorID, int testNumber) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.NewInstanceCommand);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsThreadID(threadID);
        packet.setNextValueAsMethodID(constructorID);
        if ( testNumber == 1 ) {
            packet.setNextValueAsInt(1); // number of parameters
            packet.setNextValueAsValue(new Value(false));
        }
        if ( testNumber == 2 ) {
            packet.setNextValueAsInt(0); // number of parameters
        }
        packet.setNextValueAsInt(0);
        logWriter.println("\nSend ClassType.NewInstance");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassType::NewInstance command");

        TaggedObject newObject = reply.getNextValueAsTaggedObject();
        logWriter.println(" ClassType.NewInstance: newObject.tag="
                + newObject.tag + " newObject.objectID=" + newObject.objectID);

        TaggedObject exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ClassType.NewInstance: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);

        assertTrue("newObject must be != null", newObject != null);
        assertTrue("newObject.objectID must be != 0", newObject.objectID != 0);
        assertEquals("Invalid object tag,", JDWPConstants.Tag.OBJECT_TAG, newObject.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(newObject.tag));

        assertTrue("exception must be != null", exception != null);
        assertTrue("exception.objectID must be == 0", exception.objectID == 0);
        assertEquals("Invalid exception.tag,", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));

        assertAllDataRead(reply);
        return newObject;
    }

    /**
     * This testcase exercises ObjectReference.InvokeMethod command.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - send ObjectReference.InvokeMethod command for method,
     * which should not throw any Exception, and checks,
     * that returned value is int value and returned
     * exception object is null;
     * <BR>&nbsp;&nbsp; - send ObjectReference.InvokeMethod command for method,
     * which should throw some Exception, and checks, that
     * returned exception object is not null and has expected attributes;
     */
    public void testInvokeMethod001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Get referenceTypeID
        String classSig = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/testClass2;";
        long typeID = getReferenceTypeID(classSig);

        // Get methodIDs
        long targetMethodID = getMethodID(typeID, "testMethod3");
        long targetConstructorID = getMethodID(typeID, "<init>");

        // Set EventRequest
        int requestID = setEventRequest();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // Wait event
        long targetThreadID = waitEvent(requestID);

        //  Let's clear event request
        clearEvent(requestID);

        // Make NewInstance
        TaggedObject newObject = makeNewInstance(typeID, targetThreadID,
                targetConstructorID, 1 /* test number */);

        //  Make InvokeMethod without exception
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.InvokeMethodCommand);
        packet.setNextValueAsObjectID(newObject.objectID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(0);
        logWriter.println("\nSend ObjectReference.InvokeMethod without exception...");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ObjectReference::InvokeMethod command");

        Value returnValue = reply.getNextValueAsValue();
        logWriter.println(" ObjectReference.InvokeMethod: returnValue.getIntValue()="
                        + returnValue.getIntValue());

        TaggedObject exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ObjectReference.InvokeMethod: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);

        assertTrue("returnValue must be != null", returnValue != null);
        assertEquals("Invalid returned value,", 345, returnValue.getIntValue());

        assertTrue("exception must be != null", exception != null);
        assertTrue("exception.objectID must be == 0", exception.objectID == 0);
        assertEquals("invalid exception.tag,", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));

        assertAllDataRead(reply);

        //  Make InvokeMethod with exception
        packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.InvokeMethodCommand);
        packet.setNextValueAsObjectID(newObject.objectID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsClassID(typeID);
        packet.setNextValueAsMethodID(targetMethodID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(true));
        packet.setNextValueAsInt(0);
        logWriter.println("\nSend ObjectReference.InvokeMethod with exception...");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ObjectReference::InvokeMethod command");
        
        returnValue = reply.getNextValueAsValue();
        logWriter.println(" ObjectReference.InvokeMethod: returnValue.getIntValue()="
                + returnValue.getIntValue());

        exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ObjectReference.InvokeMethod: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);

        assertTrue("exception must be != null", exception != null);
        assertTrue("exception.objectID must be != 0", exception.objectID != 0);
        assertEquals("Invalid exception.tag", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));

        assertAllDataRead(reply);

        //  Let's resume application
        resumeDebuggee();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ObjectReference.InvokeMethod command with INVOKE_NONVIRTUAL InvokeOption.
     * <BR>At first the test starts debuggee. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - send ObjectReference.InvokeMethod command for nonvirtual
     * child method (from subclass), which should not throw any Exception, and checks,
     * that returned value is expected int value and returned
     * exception object is null;
     * <BR>&nbsp;&nbsp; - send ObjectReference.InvokeMethod command for nonvirtual
     * super method (from super class), which should not throw any Exception, and checks,
     * that returned value is expected int value and returned
     * exception object is null;
     */
    public void testInvokeMethod002() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // Get referenceTypeID of super class
        String classSig = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/testClass2;";
        long typeIDSuper = getReferenceTypeID(classSig);

        // Get referenceTypeID of child class
        classSig = "Lorg/apache/harmony/jpda/tests/jdwp/share/debuggee/testClass3;";
        long typeIDChild = getReferenceTypeID(classSig);

        // Get methodID
        long targetMethodIDSuper = getMethodID(typeIDSuper, "testMethod3");
        long targetMethodIDChild = getMethodID(typeIDChild, "testMethod3");
        long targetConstructorID = getMethodID(typeIDChild, "<init>");

        // Set EventRequest
        int requestID = setEventRequest();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        // Wait event
        long targetThreadID = waitEvent(requestID);

        //  Let's clear event request
        clearEvent(requestID);

        // Make NewInstance
        TaggedObject newObject = makeNewInstance(typeIDChild, targetThreadID,
                targetConstructorID, 2 /* test number */);

        //  Make InvokeMethod: nonvirtual child method without exception
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.InvokeMethodCommand);
        packet.setNextValueAsObjectID(newObject.objectID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsClassID(typeIDChild);
        packet.setNextValueAsMethodID(targetMethodIDChild);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(JDWPConstants.InvokeOptions.INVOKE_NONVIRTUAL);
        logWriter.println
        ("\nSend ObjectReference.InvokeMethod:: nonvirtual child method without exception...");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ObjectReference::InvokeMethod command");

        Value returnValue = reply.getNextValueAsValue();
        logWriter.println(" ObjectReference.InvokeMethod: returnValue.getIntValue()="
                + returnValue.getIntValue());

        TaggedObject exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ObjectReference.InvokeMethod: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);

        assertTrue("returnValue must be != null", returnValue != null);
        assertEquals("Invalid value,", 456, returnValue.getIntValue());
        
        assertTrue("exception must be != null", exception != null);
        assertTrue("exception.objectID must be == 0", exception.objectID == 0);
        assertEquals("Invalid exception.tag", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));

        assertAllDataRead(reply);

        //  Make InvokeMethod: nonvirtual super method without exception
        packet = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.InvokeMethodCommand);
        packet.setNextValueAsObjectID(newObject.objectID);
        packet.setNextValueAsThreadID(targetThreadID);
        packet.setNextValueAsClassID(typeIDSuper);
        packet.setNextValueAsMethodID(targetMethodIDSuper);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsValue(new Value(false));
        packet.setNextValueAsInt(JDWPConstants.InvokeOptions.INVOKE_NONVIRTUAL);
        logWriter.println
        ("\nSend ObjectReference.InvokeMethod: nonvirtual super method without exception...");
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ObjectReference::InvokeMethod command");

        returnValue = reply.getNextValueAsValue();
        logWriter.println(" ObjectReference.InvokeMethod: returnValue.getIntValue()="
                + returnValue.getIntValue());

        exception = reply.getNextValueAsTaggedObject();
        logWriter.println(" ObjectReference.InvokeMethod: exception.tag="
                + exception.tag + " exception.objectID=" + exception.objectID);
        
        assertTrue("returnValue must be != null", returnValue != null);
        assertEquals("Invalid value,", 345, returnValue.getIntValue());
        
        assertTrue("exception must be != null", exception != null);
        assertTrue("exception.objectID must be == 0" ,exception.objectID == 0);
        assertEquals("Invalid exception.tag", JDWPConstants.Tag.OBJECT_TAG, exception.tag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG)
                , JDWPConstants.Tag.getName(exception.tag));

        assertAllDataRead(reply);

        //  Let's resume application
        resumeDebuggee();

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InvokeMethodTest.class);
    }

}