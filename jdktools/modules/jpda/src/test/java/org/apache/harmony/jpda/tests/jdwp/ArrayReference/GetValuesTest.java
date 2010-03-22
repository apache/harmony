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

import org.apache.harmony.jpda.tests.framework.jdwp.ArrayRegion;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP unit test for ArrayReference.GetValues command.  
 */

public class GetValuesTest extends JDWPArrayReferenceTestCase {

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValuesTest.class);
    }

    /**
     * This testcase exercises ArrayReference.GetValues command.
     * <BR>Starts <A HREF="ArrayReferenceDebuggee.html">ArrayReferenceDebuggee</A>. 
     * <BR>Receives fields with ReferenceType.fields command, 
     * receives values with ArrayReference.GetValues then checks them.
     */
    public void testGetValues001() throws UnsupportedEncodingException {
        logWriter.println("==> GetValuesTest.testGetValues001 started...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        String debuggeeSig = "Lorg/apache/harmony/jpda/tests/jdwp/ArrayReference/ArrayReferenceDebuggee;";

        // obtain classID
        long classID = getClassIDBySignature(debuggeeSig);

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

            if (name.equals("threadArray")) {
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'threadArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 1,
                        1, JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.THREAD_TAG, false);
            }
            if (name.equals("threadGroupArray")) {
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'threadGroupArray...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 1,
                        1, JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.THREAD_GROUP_TAG, false);
            }
            if (name.equals("classArray")) {
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'classArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 1,
                        1, JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.CLASS_OBJECT_TAG, false);
            }
            if (name.equals("ClassLoaderArray")) {
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'ClassLoaderArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 1,
                        1, JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.CLASS_LOADER_TAG, false);
            }
            if (name.equals("myThreadArray")) {
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'myThreadArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 1,
                        1, JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.THREAD_TAG, false);
            }
            if (name.equals("objectArrayArray")) {
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'objectArrayArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 1,
                        1, JDWPConstants.Tag.ARRAY_TAG, JDWPConstants.Tag.ARRAY_TAG, false);
            }
            if (name.equals("intArray")) {
                // int[] intArray = new int[10]
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'int[] intArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 10,
                4, JDWPConstants.Tag.INT_TAG, JDWPConstants.Tag.INT_TAG, true);
            } 
            if (name.equals("strArray")) {
                // String[] strArray = new String[8]
                logWriter.println
                    ("\n==> testGetValues001: check for array field: 'String[] strArray'...");
                checkArrayValues(classID, fieldID, JDWPConstants.Error.NONE, 8,
                4, JDWPConstants.Tag.OBJECT_TAG, JDWPConstants.Tag.STRING_TAG, true);
            } 
            if (name.equals("intField")) {
                // Integer intField = new Integer(-1)
                logWriter.println
                ("\n==> testGetValues001: check for non-array field: 'Integer intField = new Integer(-1)'...");
               checkArrayValues(classID, fieldID, JDWPConstants.Error.INVALID_ARRAY, 0,
               4, (byte)0, (byte)0, false);
            }
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
    
    private void checkArrayValues(long classID, long fieldID, int error, int length,
            int checksNumber, byte expectedArrayTag, byte expectedElementTag, boolean checkValues)
    throws UnsupportedEncodingException {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        // set referenceTypeID
        packet.setNextValueAsReferenceTypeID(classID);
        // repeat 1 time
        packet.setNextValueAsInt(1);
        // set fieldID
        packet.setNextValueAsFieldID(fieldID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::GetValues command");

        assertEquals("ReferenceType::GetValues command returned invalid int value,", reply.getNextValueAsInt(), 1);
        Value value = reply.getNextValueAsValue();
        long arrayID = value.getLongValue(); 
        logWriter.println("==> testGetValues001: checked arrayID = " + arrayID);
        
        logWriter.println("==> testGetValues001: checkArrayRegion: arrayID = " + arrayID
                + "; Expected error = " + error + "(" + JDWPConstants.Error.getName(error) + ")"
                + "; firstIndex = 0; length = " + length);
        checkArrayRegion
        (arrayID, error, 0, length, expectedArrayTag, expectedElementTag, checkValues);
        logWriter.println("==> PASSED!");
        
        if ( checksNumber > 1 ) {
            logWriter.println("==> testGetValues001: checkArrayRegion: arrayID = " + arrayID
                + "; Expected error = " + error+ "(" + JDWPConstants.Error.getName(error) + ")"
                + "; firstIndex = 1; length = " + (length-1));
            checkArrayRegion
            (arrayID, error, 1, length-1, expectedArrayTag, expectedElementTag, checkValues);
            logWriter.println("==> PASSED!");
        
            logWriter.println("==> testGetValues001: checkArrayRegion: arrayID = " + arrayID
                + "; Expected error = " + error+ "(" + JDWPConstants.Error.getName(error) + ")"
                + "; firstIndex = 0; length = " + (length-1));
            checkArrayRegion
            (arrayID, error, 0, length-1, expectedArrayTag, expectedElementTag, checkValues);
            logWriter.println("==> PASSED!");
        
            logWriter.println("==> testGetValues001: checkArrayRegion: arrayID = " + arrayID
                + "; Expected error = " + error+ "(" + JDWPConstants.Error.getName(error) + ")"
                + "; firstIndex = " + (length-1) + " length = 1");
            checkArrayRegion
            (arrayID, error, length-1, 1, expectedArrayTag, expectedElementTag, checkValues);
            logWriter.println("==> PASSED!");
        }
    }

    private void checkArrayRegion(long arrayID, int error, int firstIndex, int length,
            byte expectedArrayTag, byte expectedElementTag, boolean checkValues)
        throws UnsupportedEncodingException {

        CommandPacket packet = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.GetValuesCommand);
        packet.setNextValueAsArrayID(arrayID);
        packet.setNextValueAsInt(firstIndex);
        packet.setNextValueAsInt(length);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ArrayReference::GetValues command", error);

        if (reply.getErrorCode() == JDWPConstants.Error.NONE) {
            // do not check values for non-array fields
            ArrayRegion region = reply.getNextValueAsArrayRegion();
            //System.err.println("length="+region.length);
            byte arrayTag = region.getTag();
            logWriter.println("==> arrayTag =  " + arrayTag
                    + "(" + JDWPConstants.Tag.getName(arrayTag) + ")");
            logWriter.println("==> arrayLength =  "+region.getLength());
            Value value_0 = region.getValue(0);
            byte elementTag = value_0.getTag();
            logWriter.println("==> elementTag =  " + elementTag
                    + "(" + JDWPConstants.Tag.getName(elementTag) + ")");

            assertEquals("ArrayReference::GetValues returned invalid array tag,",
                    expectedArrayTag, arrayTag,
                    JDWPConstants.Tag.getName(expectedArrayTag),
                    JDWPConstants.Tag.getName(arrayTag));
            assertEquals("ArrayReference::GetValues returned invalid array length,",
                    length, region.getLength());
            assertEquals("ArrayReference::GetValues returned invalid element tag",
                    expectedElementTag, elementTag,
                    JDWPConstants.Tag.getName(expectedElementTag),
                    JDWPConstants.Tag.getName(elementTag));

            if (checkValues) {
                for (int i = 0; i < region.getLength(); i++) {
                    Value value = region.getValue(i);
                    if (value.getTag() == JDWPConstants.Tag.INT_TAG) {
                        assertEquals("ArrayReference::GetValues returned invalid value on index:<" + i + ">,",
                                value.getIntValue(), i + firstIndex);
                    }
                    else if (value.getTag() == JDWPConstants.Tag.STRING_TAG) {
                        long stringID = value.getLongValue();
                        String s = getStringValue(stringID);
                        assertEquals("ArrayReference::GetValues returned invalid value on index:<" + i + ">,",
                                Integer.parseInt(s), i + firstIndex);
                    }
                }
            }
        }
    }
}
