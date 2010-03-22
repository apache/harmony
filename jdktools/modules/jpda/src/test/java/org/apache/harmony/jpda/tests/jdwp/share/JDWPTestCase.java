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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 29.01.2005
 */
package org.apache.harmony.jpda.tests.jdwp.share;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.EventPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.Packet;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;

/**
 * Basic class for unit tests which use only one debuggee VM.
 */
public abstract class JDWPTestCase extends JDWPRawTestCase {

    /**
     * DebuggeeWrapper instance for launched debuggee VM.
     */
    protected JDWPUnitDebuggeeWrapper debuggeeWrapper;

    /**
     * EventPacket instance with received VM_START event.
     */
    protected EventPacket initialEvent = null;

    /**
     * Overrides inherited method to launch one debuggee VM, establish JDWP
     * connection, and wait for VM_START event.
     */
    protected void internalSetUp() throws Exception {
        super.internalSetUp();

        // launch debuggee process
        debuggeeWrapper = createDebuggeeWrapper();
        beforeDebuggeeStart(debuggeeWrapper);
        startDebuggeeWrapper();

        // receive and handle initial event
        receiveInitialEvent();

        // adjust JDWP types length
        debuggeeWrapper.vmMirror.adjustTypeLength();
        logWriter.println("Adjusted VM-dependent type lengths");
    }

    /**
     * Creates wrapper for debuggee process.
     */
    protected JDWPUnitDebuggeeWrapper createDebuggeeWrapper() {
        if (settings.getDebuggeeLaunchKind().equals("manual")) {
            return new JDWPManualDebuggeeWrapper(settings, logWriter);
        } else {
            return new JDWPUnitDebuggeeWrapper(settings, logWriter);
        }
    }

    /**
     * Starts wrapper for debuggee process.
     */
    protected void startDebuggeeWrapper() {
    	debuggeeWrapper.start();
        logWriter.println("Established JDWP connection with debuggee VM");
    }
    	
    /**
     * Receives initial VM_INIT event if debuggee is suspended on event.
     */
    protected void receiveInitialEvent() {
        if (settings.isDebuggeeSuspend()) {
            initialEvent = 
                debuggeeWrapper.vmMirror.receiveCertainEvent(JDWPConstants.EventKind.VM_INIT);
            logWriter.println("Received inital VM_INIT event");
        }
    }

    /**
     * Overrides inherited method to stop started debuggee VM and close all
     * connections.
     */
    protected void internalTearDown() {
        if (debuggeeWrapper != null) {
            debuggeeWrapper.stop();
            logWriter.println("Closed JDWP connection with debuggee VM");
        }
        super.internalTearDown();
    }

    /**
     * This method is invoked right before starting debuggee VM.
     */
    protected void beforeDebuggeeStart(JDWPUnitDebuggeeWrapper debuggeeWrapper) {

    }

    /**
     * Opens JDWP connection with debuggee (doesn't run debuggee and doesn't
     * establish synchronize connection).
     */
    public void openConnection() {
        debuggeeWrapper.openConnection();
        logWriter.println("Opened transport connection");
        debuggeeWrapper.vmMirror.adjustTypeLength();
        logWriter.println("Adjusted VM-dependent type lengths");
    }

    /**
     * Closes JDWP connection with debuggee (doesn't terminate debuggee and
     * doesn't stop synchronize connection).
     */
    public void closeConnection() {
        if (debuggeeWrapper != null) {
            debuggeeWrapper.disposeConnection();
            try {
                debuggeeWrapper.vmMirror.closeConnection();
            } catch (Exception e) {
                throw new TestErrorException(e);
            }
            logWriter.println("Closed transport connection");
        }
    }

    /**
     * Helper that returns reference type signature of input object ID.
     * 
     * @param objectID -
     *            debuggee object ID
     * @return object signature of reference type
     */
    protected String getObjectSignature(long objectID) {
        long classID = getObjectReferenceType(objectID);
        return getClassSignature(classID);
    }

    /**
     * Helper that returns reference type ID for input object ID.
     * 
     * @param objectID -
     *            debuggee object ID
     * @return reference type ID
     */
    protected long getObjectReferenceType(long objectID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
        command.setNextValueAsReferenceTypeID(objectID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(command);
        checkReplyPacket(reply, "ObjectReference::ReferenceType command");
        // byte refTypeTag =
        reply.getNextValueAsByte();
        long objectRefTypeID = reply.getNextValueAsReferenceTypeID();
        return objectRefTypeID;
    }

    /**
     * Helper for getting method ID of corresponding class and method name.
     * 
     * @param classID -
     *            class ID
     * @param methodName -
     *            method name
     * @return method ID
     */
    protected long getMethodID(long classID, String methodName) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        command.setNextValueAsClassID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(command);
        checkReplyPacket(reply, "ReferenceType::Methods command");
        int methods = reply.getNextValueAsInt();
        for (int i = 0; i < methods; i++) {
            long methodID = reply.getNextValueAsMethodID();
            String name = reply.getNextValueAsString(); // method name
            reply.getNextValueAsString(); // method signature
            reply.getNextValueAsInt(); // method modifiers
            if (name.equals(methodName)) {
                return methodID;
            }
        }
        return -1;
    }

    /**
     * Issues LineTable command.
     * 
     * @param classID -
     *            class ID
     * @param methodID -
     *            method ID
     * @return reply packet
     */
    protected ReplyPacket getLineTable(long classID, long methodID) {
        CommandPacket lineTableCommand = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        lineTableCommand.setNextValueAsReferenceTypeID(classID);
        lineTableCommand.setNextValueAsMethodID(methodID);
        ReplyPacket lineTableReply = debuggeeWrapper.vmMirror
                .performCommand(lineTableCommand);
        checkReplyPacket(lineTableReply, "Method::LineTable command");
        return lineTableReply;
    }

    /**
     * Helper for getting method name of corresponding class and method ID.
     * 
     * @param classID class id
     * @param methodID method id
     * @return String
     */
    protected String getMethodName(long classID, long methodID) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        packet.setNextValueAsClassID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ReferenceType::Methods command");
        int methods = reply.getNextValueAsInt();
        for (int i = 0; i < methods; i++) {
            long mid = reply.getNextValueAsMethodID();
            String name = reply.getNextValueAsString();
            reply.getNextValueAsString();
            reply.getNextValueAsInt();
            if (mid == methodID) {
                return name;
            }
        }
        return "unknown";
    }

    /**
     * Returns jni signature for selected classID
     * 
     * @param classID
     * @return jni signature for selected classID
     */
    protected String getClassSignature(long classID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        command.setNextValueAsReferenceTypeID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(command);
        checkReplyPacket(reply, "ReferenceType::Signature command");
        String signature = reply.getNextValueAsString();
        return signature;
    }

    /**
     * Returns classID for the selected jni signature
     * 
     * @param signature
     * @return classID for the selected jni signature
     */
    protected long getClassIDBySignature(String signature) {
        logWriter.println("=> Getting reference type ID for class: "
                + signature);
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        packet.setNextValueAsString(signature);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");
        int classes = reply.getNextValueAsInt();
        logWriter.println("=> Returned number of classes: " + classes);
        long classID = 0;
        for (int i = 0; i < classes; i++) {
            reply.getNextValueAsByte();
            classID = reply.getNextValueAsReferenceTypeID();
            reply.getNextValueAsInt();
            // we need the only class, even if there were multiply ones
            break;
        }
        assertTrue(
                "VirtualMachine::ClassesBySignature command returned invalid classID:<"
                        + classID + "> for signature " + signature, classID > 0);
        return classID;
    }

    /**
     * Returns reference type ID.
     * 
     * @param signature
     * @return type ID for the selected jni signature
     */
    protected long getReferenceTypeID(String signature) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        packet.setNextValueAsString(signature);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::ClassesBySignature command");
        int classes = reply.getNextValueAsInt();
        // this class may be loaded only once
        assertEquals("Invalid number of classes for reference type: "
                + signature + ",", 1, classes);
        byte refTypeTag = reply.getNextValueAsByte();
        long classID = reply.getNextValueAsReferenceTypeID();
        int status = reply.getNextValueAsInt();
        logWriter.println("VirtualMachine.ClassesBySignature: classes="
                + classes + " refTypeTag=" + refTypeTag + " typeID= " + classID
                + " status=" + status);
        assertAllDataRead(reply);
        assertEquals("", JDWPConstants.TypeTag.CLASS, refTypeTag);
        return classID;
    }

    /**
     * Helper function for resuming debuggee.
     */
    protected void resumeDebuggee() {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ResumeCommand);
        logWriter.println("Sending VirtualMachine::Resume command...");
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::Resume command");
        assertAllDataRead(reply);
    }

    /**
     * Performs string creation in debuggee.
     * 
     * @param value -
     *            content for new string
     * @return StringID of new created string
     */
    protected long createString(String value) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.CreateStringCommand);
        packet.setNextValueAsString(value);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "VirtualMachine::CreateString command");
        long stringID = reply.getNextValueAsStringID();
        return stringID;
    }

    /**
     * Returns corresponding string from string ID.
     * 
     * @param stringID -
     *            string ID
     * @return string value
     */
    protected String getStringValue(long stringID) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.StringReferenceCommandSet.CommandSetID,
                JDWPCommands.StringReferenceCommandSet.ValueCommand);
        packet.setNextValueAsObjectID(stringID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "StringReference::Value command");
        String returnedTestString = reply.getNextValueAsString();
        return returnedTestString;
    }

    /**
     * Multiple field verification routine.
     * 
     * @param refTypeID -
     *            reference type ID
     * @param checkedFieldNames -
     *            list of field names to be checked
     * @return list of field IDs
     */
    protected long[] checkFields(long refTypeID, String checkedFieldNames[]) {
        return checkFields(refTypeID, checkedFieldNames, null, null);
    }

    /**
     * Single field verification routine.
     * 
     * @param refTypeID -
     *            reference type ID
     * @param fieldName -
     *            name of single field
     * @return filed ID
     */
    protected long checkField(long refTypeID, String fieldName) {
        return checkFields(refTypeID, new String[] { fieldName }, null, null)[0];
    }

    /**
     * Multiple field verification routine.
     * 
     * @param refTypeID -
     *            reference type ID
     * @param checkedFieldNames -
     *            list of field names to be checked
     * @param expectedSignatures -
     *            list of expected field signatures
     * @param expectedModifiers -
     *            list of expected field modifiers
     * @return list of field IDs
     */
    protected long[] checkFields(long refTypeID, String checkedFieldNames[],
            String expectedSignatures[], int expectedModifiers[]) {

        boolean checkedFieldFound[] = new boolean[checkedFieldNames.length];
        long checkedFieldIDs[] = new long[checkedFieldNames.length];

        logWriter
                .println("=> Send ReferenceType::Fields command and get field ID(s)");

        CommandPacket fieldsCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.FieldsCommand);
        fieldsCommand.setNextValueAsReferenceTypeID(refTypeID);
        ReplyPacket fieldsReply = debuggeeWrapper.vmMirror
                .performCommand(fieldsCommand);
        fieldsCommand = null;
        checkReplyPacket(fieldsReply, "ReferenceType::Fields command");

        int returnedFieldsNumber = fieldsReply.getNextValueAsInt();
        logWriter
                .println("=> Returned fields number = " + returnedFieldsNumber);

        int checkedFieldsNumber = checkedFieldNames.length;
        final int fieldSyntheticFlag = 0xf0000000;

        int nameDuplicated = 0;
        String fieldNameDuplicated = null; // <= collects all duplicated fields
        int nameMissing = 0;
        String fieldNameMissing = null; // <= collects all missed fields

        for (int i = 0; i < returnedFieldsNumber; i++) {
            long returnedFieldID = fieldsReply.getNextValueAsFieldID();
            String returnedFieldName = fieldsReply.getNextValueAsString();
            String returnedFieldSignature = fieldsReply.getNextValueAsString();
            int returnedFieldModifiers = fieldsReply.getNextValueAsInt();
            logWriter.println("");
            logWriter.println("=> Field ID: " + returnedFieldID);
            logWriter.println("=> Field name: " + returnedFieldName);
            logWriter.println("=> Field signature: " + returnedFieldSignature);
            logWriter.println("=> Field modifiers: 0x"
                    + Integer.toHexString(returnedFieldModifiers));
            if ((returnedFieldModifiers & fieldSyntheticFlag) == fieldSyntheticFlag) {
                continue; // do not check synthetic fields
            }
            for (int k = 0; k < checkedFieldsNumber; k++) {
                if (!checkedFieldNames[k].equals(returnedFieldName)) {
                    continue;
                }
                if (checkedFieldFound[k]) {
                    logWriter.println("");
                    logWriter
                            .println("## FAILURE: The field is found repeatedly in the list");
                    logWriter.println("## Field Name: " + returnedFieldName);
                    logWriter.println("## Field ID: " + returnedFieldID);
                    logWriter.println("## Field Signature: "
                            + returnedFieldSignature);
                    logWriter.println("## Field Modifiers: 0x"
                            + Integer.toHexString(returnedFieldModifiers));
                    fieldNameDuplicated = (0 == nameDuplicated ? returnedFieldName
                            : fieldNameDuplicated + "," + returnedFieldName);
                    nameDuplicated++;
                    break;
                }
                checkedFieldFound[k] = true;
                checkedFieldIDs[k] = returnedFieldID;
                if (null != expectedSignatures) {
                    assertString(
                            "Invalid field signature is returned for field:"
                                    + returnedFieldName + ",",
                            expectedSignatures[k], returnedFieldSignature);
                }
                if (null != expectedModifiers) {
                    assertEquals(
                            "Invalid field modifiers are returned for field:"
                                    + returnedFieldName + ",",
                            expectedModifiers[k], returnedFieldModifiers);
                }
                break;
            }
        }

        for (int k = 0; k < checkedFieldsNumber; k++) {
            if (!checkedFieldFound[k]) {
                logWriter.println("");
                logWriter
                        .println("\n## FAILURE: Expected field is NOT found in the list of retuned fields:");
                logWriter.println("## Field name = " + checkedFieldNames[k]);
                fieldNameMissing = 0 == nameMissing ? checkedFieldNames[k]
                        : fieldNameMissing + "," + checkedFieldNames[k];
                nameMissing++;
                // break;
            }
        }

        // String thisTestName = this.getClass().getName();
        // logWriter.println("==> " + thisTestName + " for " + thisCommandName +
        // ": FAILED");

        if (nameDuplicated > 1) {
            fail("Duplicated fields are found in the retuned by FieldsCommand list: "
                    + fieldNameDuplicated);
        }
        if (nameDuplicated > 0) {
            fail("Duplicated field is found in the retuned by FieldsCommand list: "
                    + fieldNameDuplicated);
        }
        if (nameMissing > 1) {
            fail("Expected fields are NOT found in the retuned by FieldsCommand list: "
                    + fieldNameMissing);
        }
        if (nameMissing > 0) {
            fail("Expected field is NOT found in the retuned by FieldsCommand list: "
                    + fieldNameMissing);
        }

        logWriter.println("");
        if (1 == checkedFieldsNumber) {
            logWriter
                    .println("=> Expected field was found and field ID was got");
        } else {
            logWriter
                    .println("=> Expected fields were found and field IDs were got");
        }

        assertAllDataRead(fieldsReply);
        return checkedFieldIDs;
    }

    /**
     * Helper for checking reply packet error code. Calls junit fail if packet
     * error code does not equal to expected error code.
     * 
     * @param reply -
     *            returned from debuggee packet
     * @param message -
     *            additional message
     * @param errorCodeExpected -
     *            array of expected error codes
     */
    protected void checkReplyPacket(ReplyPacket reply, String message,
            int errorCodeExpected) {
        checkReplyPacket(reply, message, new int[] { errorCodeExpected });
    }

    /**
     * Helper for checking reply packet error code. Calls junit fail if packet
     * error code does not equal NONE.
     * 
     * @param reply -
     *            returned from debuggee packet
     * @param message -
     *            additional message
     */
    protected void checkReplyPacket(ReplyPacket reply, String message) {
        checkReplyPacket(reply, message, JDWPConstants.Error.NONE);
    }

    /**
     * Helper for checking reply packet error code. Calls junit fail if packet
     * error code does not equal to expected error code.
     * 
     * @param reply -
     *            returned from debuggee packet
     * @param message -
     *            additional message
     * @param expected -
     *            array of expected error codes
     */
    protected void checkReplyPacket(ReplyPacket reply, String message,
            int[] expected) {
        checkReplyPacket(reply, message, expected, true /* failSign */);
    }

    /**
     * Helper for checking reply packet error code. If reply packet does not
     * have error - returns true. Otherwise does not call junit fail - simply
     * prints error message and returns false. if packet error code does not
     * equal NONE.
     * 
     * @param reply -
     *            returned from debuggee packet
     * @param message -
     *            additional message
     * @return true if error is not found, or false otherwise
     */
    protected boolean checkReplyPacketWithoutFail(ReplyPacket reply,
            String message) {
        return checkReplyPacket(reply, message,
                new int[] { JDWPConstants.Error.NONE }, false /* failSign */);
    }

    /**
     * Helper for checking reply packet error code. If reply packet does not
     * have unexpected error - returns true. If reply packet has got unexpected
     * error: If failSign param = true - calls junit fail. Otherwise prints
     * message about error and returns false.
     * 
     * @param reply -
     *            returned from debuggee packet
     * @param message -
     *            additional message
     * @param expected -
     *            array of expected error codes
     * @param failSign -
     *            defines to call junit fail or not
     * @return true if unexpected errors are not found, or false otherwise
     */
    protected boolean checkReplyPacket(ReplyPacket reply, String message,
            int[] expected, boolean failSign) {
        // check reply code against expected
        int errorCode = reply.getErrorCode();
        for (int i = 0; i < expected.length; i++) {
            if (reply.getErrorCode() == expected[i]) {
                return true; // OK
            }
        }

        // replay code validation failed
        // start error message composition
        if (null == message) {
            message = "";
        } else {
            message = message + ", ";
        }

        // format error message
        if (expected.length == 1 && JDWPConstants.Error.NONE == expected[0]) {
            message = message + "Error Code:<" + errorCode + "("
                    + JDWPConstants.Error.getName(errorCode) + ")>";
        } else {
            message = message + "Unexpected error code:<" + errorCode + "("
                    + JDWPConstants.Error.getName(errorCode) + ")>"
                    + ", Expected error code"
                    + (expected.length == 1 ? ":" : "s:");
            for (int i = 0; i < expected.length; i++) {
                message = message + (i > 0 ? ",<" : "<") + expected[i] + "("
                        + JDWPConstants.Error.getName(expected[i]) + ")>";
            }
        }

        if (failSign) {
            printErrorAndFail(message);
        }
        logWriter.printError(message);
        return false;
    }

    /**
     * Helper for comparison numbers and printing string equivalents.
     * 
     * @param message -
     *            user message
     * @param expected -
     *            expected value
     * @param actual -
     *            actual value
     * @param strExpected -
     *            string equivalent of expected value
     * @param strActual -
     *            string equivalent of actual value
     */
    protected void assertEquals(String message, long expected, long actual,
            String strExpected, String strActual) {
        if (expected == actual) {
            return; // OK
        }

        if (null == message) {
            message = "";
        }

        if (null == strExpected) {
            strExpected = expected + "";
        } else {
            strExpected = expected + "(" + strExpected + ")";
        }

        if (null == strActual) {
            strActual = actual + "";
        } else {
            strActual = actual + "(" + strActual + ")";
        }

        printErrorAndFail(message + " expected:<" + strExpected + "> but was:<"
                + strActual + ">");
    }

    /**
     * Asserts that two strings are equal.
     * 
     * @param message -
     *            user message
     * @param expected -
     *            expected string
     * @param actual -
     *            actual string
     */
    protected void assertString(String message, String expected, String actual) {
        if (null == expected) {
            expected = "";
        }
        if (null == actual) {
            actual = "";
        }
        if (expected.equals(actual)) {
            return; // OK
        }
        printErrorAndFail(message + " expected:<" + expected + "> but was:<"
                + actual + ">");
    }

    /**
     * Helper for checking reply packet data has been read.
     * 
     * @param reply -
     *            reply packet from debuggee
     */
    protected void assertAllDataRead(Packet reply) {
        if (reply.isAllDataRead()) {
            return; // OK
        }
        printErrorAndFail("Not all data has been read");
    }

    /**
     * Prints error message in log writer and in junit fail.
     * 
     * @param message -
     *            error message
     */
    protected void printErrorAndFail(String message) {
        logWriter.printError(message);
        fail(message);
    }

    /**
     * Helper for setting static int field in class with new value.
     * 
     * @param classSignature -
     *            String defining signature of class
     * @param fieldName -
     *            String defining field name in specified class
     * @param newValue -
     *            int value to set for specified field
     * @return true, if setting is successfully, or false otherwise
     */
    protected boolean setStaticIntField(String classSignature,
            String fieldName, int newValue) {

        long classID = debuggeeWrapper.vmMirror.getClassID(classSignature);
        if (classID == -1) {
            logWriter
                    .println("## setStaticIntField(): Can NOT get classID for class signature = '"
                            + classSignature + "'");
            return false;
        }

        long fieldID = debuggeeWrapper.vmMirror.getFieldID(classID, fieldName);
        if (fieldID == -1) {
            logWriter
                    .println("## setStaticIntField(): Can NOT get fieldID for field = '"
                            + fieldName + "'");
            return false;
        }

        CommandPacket packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.SetValuesCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        packet.setNextValueAsInt(1);
        packet.setNextValueAsFieldID(fieldID);
        packet.setNextValueAsInt(newValue);

        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        int errorCode = reply.getErrorCode();
        if (errorCode != JDWPConstants.Error.NONE) {
            logWriter
                    .println("## setStaticIntField(): Can NOT set value for field = '"
                            + fieldName
                            + "' in class = '"
                            + classSignature
                            + "'; ClassType.SetValues command reurns error = "
                            + errorCode);
            return false;
        }
        return true;
    }

    /**
     * Removes breakpoint of the given event kind corresponding to the given
     * request id.
     * 
     * @param eventKind
     *            request event kind
     * @param requestID
     *            request id
     * @param verbose
     *            print or don't extra log info
     */
    protected void clearEvent(byte eventKind, int requestID, boolean verbose) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.ClearCommand);
        packet.setNextValueAsByte(eventKind);
        packet.setNextValueAsInt(requestID);
        if (verbose) {
            logWriter.println("Clearing event: "
                    + JDWPConstants.EventKind.getName(eventKind) + ", id: "
                    + requestID);
        }
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "EventRequest::Clear command");
        assertAllDataRead(reply);
    }
}
