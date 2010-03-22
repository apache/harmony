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

import java.io.UnsupportedEncodingException;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP unit test for ArrayReference.Length command.  
 *   
 */

public class LengthTest extends JDWPArrayReferenceTestCase {

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(LengthTest.class);
    }

    /**
     * This testcase exercises ArrayReference.Length command.
     * <BR>Starts <A HREF="ArrayReferenceDebuggee.html">ArrayReferenceDebuggee</A>. 
     * <BR>Receives fields with ReferenceType.fields command, 
     * checks length with ArrayReference.Length command.
     */
    public void testLength001() throws UnsupportedEncodingException {
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
            reply.getNextValueAsString();
            reply.getNextValueAsString();
            reply.getNextValueAsInt();
            
            switch (i) {
                case 0:
                    // int[] intArray = new int[10]
                    checkArrayLength(classID, fieldID, JDWPConstants.Error.NONE, 10);
                    break;
                case 1:
                    // String[] strArray = new String[8]
                    checkArrayLength(classID, fieldID, JDWPConstants.Error.NONE, 8);
                    break;
                case 2:
                    // Integer intField = new Integer(-1)
                    checkArrayLength(classID, fieldID, JDWPConstants.Error.INVALID_ARRAY, 0);
                    break;
            }
        }

        logWriter.println("test PASSED!");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
    
    private void checkArrayLength(long classID, long fieldID, int error, int length) {
        //System.err.println("classID="+classID);
        //System.err.println("fieldID="+fieldID);
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsFieldID(fieldID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::GetValues command");

        int values = reply.getNextValueAsInt();
        assertEquals("ReferenceType::GetValues returned invalid number of values,", 1, values);

        Value value = reply.getNextValueAsValue();
        //System.err.println("value="+value);
        long arrayID = value.getLongValue(); 
        logWriter.println("arrayID = " + arrayID);

        packet = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.LengthCommand);
        packet.setNextValueAsArrayID(arrayID);
        reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ArrayReference::Length command", error);

        if (reply.getErrorCode() == JDWPConstants.Error.NONE) {
            // do not check length for non-array fields
            int returnedLength = reply.getNextValueAsInt();
            assertEquals("ArrayReference::Length returned invalid length,",
                    length, returnedLength);
        }
    }
}
