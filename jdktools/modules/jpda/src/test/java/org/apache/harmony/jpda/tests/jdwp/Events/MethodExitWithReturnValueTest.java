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

package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent.Event_METHOD_EXIT;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent.Event_METHOD_EXIT_WITH_RETURN_VALUE;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class MethodExitWithReturnValueTest extends JDWPEventTestCase {
    
    protected String getDebuggeeClassName() {
        return MethodExitWithReturnValueDebuggee.class.getName();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MethodExitWithReturnValueTest.class);
    }
    
    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own exceptionMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     */
    public void testMethodExitWithReturnValueException() {
        logWriter.println("==> Start testMethodExitWithReturnValue which method will throw IOException.");
        String methodExitClassNameRegexp = "org.apache.harmony.jpda.tests.jdwp.Events.MethodExitWithReturnValueDebuggee";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        // Set event request for MethodExitWithReturnValue
        ReplyPacket reply = debuggeeWrapper.vmMirror
                .setMethodExit(methodExitClassNameRegexp);
        checkReplyPacket(reply, "Set METHOD_EXIT_WITH_RETURN_VALUE event");

        // Inform debuggee desired return method
        synchronizer.sendMessage(MethodExitWithReturnValueDebuggee.EXCEPTION_TYPE);

        // Receive MethodExitWithReturnValue event
        CommandPacket receiveEvent = null;
        try {
            receiveEvent = debuggeeWrapper.vmMirror.receiveEvent();
        } catch (TestErrorException e) {
            printErrorAndFail("There is no event received.");
        }
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(receiveEvent);
        Event_METHOD_EXIT event = (Event_METHOD_EXIT)parsedEvents[0];
        
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        assertEquals("Invalid event kind,",
                JDWPConstants.EventKind.METHOD_EXIT,
                event.getEventKind(),
                JDWPConstants.EventKind.getName(JDWPConstants.EventKind.METHOD_EXIT),
                JDWPConstants.EventKind.getName(event.getEventKind()));

        long refID = event.getLocation().classID;
        String expectedSignature = "Lorg/apache/harmony/jpda/tests/jdwp/Events/MethodExitWithReturnValueDebuggee;";
        String actualSignature =  debuggeeWrapper.vmMirror.getReferenceTypeSignature(refID);
        assertEquals("Invalid class signature of method caller,",expectedSignature,actualSignature);
    }

    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own booleanMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     * and the returned value is as expected boolean value.
     */
    public void testMethodExitWithReturnValueOfBoolean() {
        runMethodExitWithReturn(MethodExitWithReturnValueDebuggee.BOOLEAN_TYPE);
    }
    
    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own shortMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     * and the returned value is as expected short value.
     */
    public void testMethodExitWithReturnValueOfShort() {
        runMethodExitWithReturn(MethodExitWithReturnValueDebuggee.SHORT_TYPE);
    }
    
    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own charMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     * and the returned value is as expected char value.
     */
    public void testMethodExitWithReturnValueOfChar() {
        runMethodExitWithReturn(MethodExitWithReturnValueDebuggee.CHAR_TYPE);
    }
    
    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own intMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     * and the returned value is as expected integer value.
     */
    public void testMethodExitWithReturnValueOfInt() {
        runMethodExitWithReturn(MethodExitWithReturnValueDebuggee.INT_TYPE);
    }
    
    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own longMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     * and the returned value is as expected long value.
     */
    public void testMethodExitWithReturnValueOfLong() {
        runMethodExitWithReturn(MethodExitWithReturnValueDebuggee.LONG_TYPE);
    }
    
    /**
     * This testcase is for METHOD_EXIT_WITH_RETURN_VALUE event. <BR>
     * It runs MethodExitWithReturnValueDebuggee that executed own doubleMethod. 
     * It then verify that requested METHOD_EXIT_WITH_RETURN_VALUE event occurs
     * and the returned value is as expected double value.
     */
    public void testMethodExitWithReturnValueOfDouble() {
        runMethodExitWithReturn(MethodExitWithReturnValueDebuggee.DOUBLE_TYPE);
    }
    
    
    /*
     * Internal function used to receive MethodExitWithReturnValue event.
     * Test the returned value according to the parameter's value
     */
    private void runMethodExitWithReturn(String type){
        logWriter.println("==> Start testMethodExitWithReturnValue with return type of " + type);
        String methodExitClassNameRegexp = "org.apache.harmony.jpda.tests.jdwp.Events.MethodExitWithReturnValueDebuggee";
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        // Set event request for MethodExitWithReturnValue
        ReplyPacket reply = debuggeeWrapper.vmMirror
                .setMethodExitWithReturnValue(methodExitClassNameRegexp);
        checkReplyPacket(reply, "Set METHOD_EXIT_WITH_RETURN_VALUE event");

        // Inform debuggee desired return method
        synchronizer.sendMessage(type.toString());

        // Receive MethodExitWithReturnValue event
        
        CommandPacket receiveEvent = null;
        try {
            receiveEvent = debuggeeWrapper.vmMirror.receiveEvent();
        } catch (TestErrorException e) {
            printErrorAndFail("There is no event received.");
        }
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(receiveEvent);

        // Check received event's basic information
        assertEquals("Invalid number of events,", 1, parsedEvents.length);
        logWriter.println("==> CHECK: receive 1 event");
        
        assertEquals(
                "Invalid event kind,",
                JDWPConstants.EventKind.METHOD_EXIT_WITH_RETURN_VALUE,
                parsedEvents[0].getEventKind(),
                JDWPConstants.EventKind
                        .getName(JDWPConstants.EventKind.METHOD_EXIT_WITH_RETURN_VALUE),
                JDWPConstants.EventKind.getName(parsedEvents[0].getEventKind()));
        logWriter.println("==> CHECK: received event's type is " + JDWPConstants.EventKind.METHOD_EXIT_WITH_RETURN_VALUE);
        
        // Check return value according to it's type
        Value value = ((Event_METHOD_EXIT_WITH_RETURN_VALUE) parsedEvents[0])
                .getReturnValue();
        
        if(type.equals(MethodExitWithReturnValueDebuggee.BOOLEAN_TYPE)){
            boolean b = value.getBooleanValue();
            logWriter.println("==> CHECK: booleanMethod() is invoked, return value:" + b);
            assertEquals("Invalid return value,",
                    MethodExitWithReturnValueDebuggee.EXPECTED_BOOLEAN, b);
        }else if(type.equals(MethodExitWithReturnValueDebuggee.SHORT_TYPE)){
            short s = value.getShortValue();
            logWriter.println("==> CHECK: shortMethod() is invoked, return value:" + s);
            assertEquals("Invalid return value,",
                    MethodExitWithReturnValueDebuggee.EXPECTED_SHORT, s);
        }else if(type.equals(MethodExitWithReturnValueDebuggee.CHAR_TYPE)){
            char c = value.getCharValue();
            logWriter.println("==> CHECK: charMethod() is invoked, return value:" + c);
            assertEquals("Invalid return value,",
                    MethodExitWithReturnValueDebuggee.EXPECTED_CHAR, c);
        }else if(type.equals(MethodExitWithReturnValueDebuggee.INT_TYPE)){
            int i = value.getIntValue();
            logWriter.println("==> CHECK: intMethod() is invoked, return value:" + i);
            assertEquals("Invalid return value,",
                    MethodExitWithReturnValueDebuggee.EXPECTED_INT, i);
        }else if(type.equals(MethodExitWithReturnValueDebuggee.LONG_TYPE)){
            long l = value.getLongValue();
            logWriter.println("==> CHECK: longMethod() is invoked, return value:" + l);
            assertEquals("Invalid return value,",
                    MethodExitWithReturnValueDebuggee.EXPECTED_LONG, l);
        }else if(type.equals(MethodExitWithReturnValueDebuggee.DOUBLE_TYPE)){
            double d = value.getDoubleValue();
            logWriter.println("==> CHECK: doubleMethod() is invoked, return value:" + d);
            assertEquals("Invalid return value,",
                    MethodExitWithReturnValueDebuggee.EXPECTED_DOUBLE, d);
        }
    }
}
