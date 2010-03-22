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
 * @author Anton V. Karnachuk
 */

/**
 * Created on 09.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ArrayReference;

import org.apache.harmony.jpda.tests.framework.jdwp.ArrayRegion;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ArrayReference.SetValues command.  
 *  
 */

public class SetValuesTest extends JDWPArrayReferenceTestCase {

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetValuesTest.class);
    }

    /**
     * This testcase exercises ArrayReference.SetValues command.
     * <BR>Starts <A HREF="ArrayReferenceDebuggee.html">ArrayReferenceDebuggee</A>. 
     * <BR>Receives fields with ReferenceType.fields command, 
     * sets values with ArrayReference.SetValues then checks changes.
     */
    public void testSetValues001() {
        logWriter.println("testLength001 started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // obtain classID
        long classID = getClassIDBySignature("Lorg/apache/harmony/jpda/tests/jdwp/ArrayReference/ArrayReferenceDebuggee;");

        // obtain fields
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.FieldsCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::Fields command");
        
        int declared = reply.getNextValueAsInt();
        for (int i = 0; i < declared; i++) {
            long fieldID = reply.getNextValueAsFieldID();
            String name = reply.getNextValueAsString();
            reply.getNextValueAsString();
            reply.getNextValueAsInt();

            if (name.equals("intArray")) {
                ArrayRegion valuesRegion = new ArrayRegion(
                        JDWPConstants.Tag.INT_TAG, 10);
                for (int j = 0; j < valuesRegion.getLength(); j++) {
                    valuesRegion.setValue(j, new Value(-j));
                }
                checkArrayValues(valuesRegion, classID, fieldID);
            } else if (name.equals("longArray")) {
                ArrayRegion valuesRegion = new ArrayRegion(
                        JDWPConstants.Tag.LONG_TAG, 10);
                for (int j = 0; j < valuesRegion.getLength(); j++) {
                    valuesRegion.setValue(j, new Value((long)-j));
                }
                checkArrayValues(valuesRegion, classID, fieldID);
            } else if (name.equals("byteArray")) {
                ArrayRegion valuesRegion = new ArrayRegion(
                        JDWPConstants.Tag.BYTE_TAG, 10);
                for (int j = 0; j < valuesRegion.getLength(); j++) {
                    valuesRegion.setValue(j, new Value((byte)-j));
                }
                checkArrayValues(valuesRegion, classID, fieldID);
            }
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    private void checkArrayValues(ArrayRegion valuesRegion, long classID,
                                  long fieldID) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsFieldID(fieldID);

        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::GetValues command");

        assertEquals("GetValuesCommand returned invalid number of values,", 1, reply.getNextValueAsInt());
        Value value = reply.getNextValueAsValue();
        //System.err.println("value="+value);
        long arrayID = value.getLongValue();
        int length = valuesRegion.getLength();

        checkArrayRegion(valuesRegion, arrayID, 0, length);
    }

    private void checkArrayRegion(ArrayRegion valuesRegion, long arrayID,
                                  int firstIndex, int length) {
        // set values
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.SetValuesCommand);
        packet.setNextValueAsArrayID(arrayID);
        packet.setNextValueAsInt(firstIndex);
        packet.setNextValueAsInt(length);
        for (int i = 0; i < length; i++) {
            packet.setNextValueAsUntaggedValue(valuesRegion.getValue(i));
        }
        packet.setNextValueAsInt(length);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ArrayReference::SetValues command");

        // get values
        packet = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.GetValuesCommand);
        packet.setNextValueAsArrayID(arrayID);
        packet.setNextValueAsInt(firstIndex);
        packet.setNextValueAsInt(length);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ArrayReference::GetValues command");
        
        // do not check values for non-array fields
        ArrayRegion region = reply.getNextValueAsArrayRegion();
        assertEquals("Invalud returned array length,", length, region.getLength());
        for (int i = 0; i < region.getLength(); i++) {
            Value value = region.getValue(i);
            logWriter.println(value.toString());
            assertEquals("ArrayReference::GetValues returned invalid value on index:<" + i + ">", 
                    value, valuesRegion.getValue(i));
        }
    }
}
