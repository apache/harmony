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

package org.apache.harmony.jpda.tests.framework.jdwp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.jpda.tests.framework.Breakpoint;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.framework.jdwp.Capabilities;
import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Event;
import org.apache.harmony.jpda.tests.framework.jdwp.EventMod;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.TransportWrapper;
import org.apache.harmony.jpda.tests.framework.jdwp.TypesLengths;
import org.apache.harmony.jpda.tests.framework.jdwp.Frame.Variable;
import org.apache.harmony.jpda.tests.framework.jdwp.exceptions.ReplyErrorCodeException;
import org.apache.harmony.jpda.tests.framework.jdwp.exceptions.TimeoutException;

/**
 * This class provides convenient way for communicating with debuggee VM using
 * JDWP packets.
 * <p>
 * Most methods can throw ReplyErrorCodeException if error occurred in execution
 * of corresponding JDWP command or TestErrorException if any other error
 * occurred.
 */
public class VmMirror {

    /** Target VM Capabilities. */
    public Capabilities targetVMCapabilities;

    /** Transport used to sent and receive packets. */
    private TransportWrapper connection;

    /** PacketDispatcher thread used for asynchronous reading packets. */
    private PacketDispatcher packetDispatcher;

    /** Test run options. */
    protected TestOptions config;

    /** Log to write messages. */
    protected LogWriter logWriter;

    /**
     * Creates new VmMirror instance for given test run options.
     * 
     * @param config
     *            test run options
     * @param logWriter
     *            log writer
     */
    public VmMirror(TestOptions config, LogWriter logWriter) {
        connection = null;
        this.config = config;
        this.logWriter = logWriter;
    }

    /**
     * Checks error code of given reply packet and throws
     * ReplyErrorCodeException if any error detected.
     * 
     * @param reply
     *            reply packet to check
     * @return ReplyPacket unchanged reply packet
     */
    public ReplyPacket checkReply(ReplyPacket reply) {
        if (reply.getErrorCode() != JDWPConstants.Error.NONE)
            throw new ReplyErrorCodeException(reply.getErrorCode());
        return reply;
    }

    /**
     * Sets breakpoint to given location.
     * 
     * @param typeTag
     * @param breakpoint
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setBreakpoint(byte typeTag, Breakpoint breakpoint) {

        return setBreakpoint(typeTag, breakpoint,
                JDWPConstants.SuspendPolicy.ALL);
    }

    /**
     * Sets breakpoint to given location.
     * 
     * @param typeTag
     * @param breakpoint
     * @param suspendPolicy
     *            Suspend policy for a breakpoint being created
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setBreakpoint(byte typeTag, Breakpoint breakpoint,
            byte suspendPolicy) {
        // Get Class reference ID
        long typeID = getTypeID(breakpoint.className, typeTag);

        // Get Method reference ID
        long methodID = getMethodID(typeID, breakpoint.methodName);

        // Fill location
        Location location = new Location(typeTag, typeID, methodID,
                breakpoint.index);

        // Set breakpoint
        return setBreakpoint(location, suspendPolicy);
    }

    /**
     * Sets breakpoint to given location.
     * 
     * @param location
     *            Location of breakpoint
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setBreakpoint(Location location) {

        return setBreakpoint(location, JDWPConstants.SuspendPolicy.ALL);
    }

    /**
     * Sets breakpoint to given location
     * 
     * @param location
     *            Location of breakpoint
     * @param suspendPolicy
     *            Suspend policy for a breakpoint being created
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setBreakpoint(Location location, byte suspendPolicy) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.BREAKPOINT;

        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };

        mods[0].loc = location;
        mods[0].modKind = EventMod.ModKind.LocationOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set breakpoint
        return setEvent(event);
    }

    /**
     * Sets breakpoint that triggers only on a certain occurrence to a given
     * location
     * 
     * @param typeTag
     * @param breakpoint
     * @param suspendPolicy
     *            Suspend policy for a breakpoint being created
     * @param count
     *            Limit the requested event to be reported at most once after a
     *            given number of occurrences
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setCountableBreakpoint(byte typeTag,
            Breakpoint breakpoint, byte suspendPolicy, int count) {
        long typeID = getTypeID(breakpoint.className, typeTag);

        // Get Method reference ID
        long methodID = getMethodID(typeID, breakpoint.methodName);

        byte eventKind = JDWPConstants.EventKind.BREAKPOINT;

        EventMod mod1 = new EventMod();
        mod1.modKind = EventMod.ModKind.LocationOnly;
        mod1.loc = new Location(typeTag, typeID, methodID, breakpoint.index);

        EventMod mod2 = new EventMod();
        mod2.modKind = EventMod.ModKind.Count;
        mod2.count = count;

        EventMod[] mods = new EventMod[] { mod1, mod2 };
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set breakpoint
        return setEvent(event);
    }

    /**
     * Sets breakpoint at the beginning of method with name <i>methodName</i>.
     * 
     * @param classID
     *            id of class with required method
     * @param methodName
     *            name of required method
     * @return requestID id of request
     */
    public long setBreakpointAtMethodBegin(long classID, String methodName) {
        long requestID;

        long methodID = getMethodID(classID, methodName);

        ReplyPacket lineTableReply = getLineTable(classID, methodID);
        if (lineTableReply.getErrorCode() != JDWPConstants.Error.NONE) {
            throw new TestErrorException(
                    "Command getLineTable returned error code: "
                            + lineTableReply.getErrorCode()
                            + " - "
                            + JDWPConstants.Error.getName(lineTableReply
                                    .getErrorCode()));
        }

        lineTableReply.getNextValueAsLong();
        // Lowest valid code index for the method

        lineTableReply.getNextValueAsLong();
        // Highest valid code index for the method

        // int numberOfLines =
        lineTableReply.getNextValueAsInt();

        long lineCodeIndex = lineTableReply.getNextValueAsLong();

        // set breakpoint inside checked method
        Location breakpointLocation = new Location(JDWPConstants.TypeTag.CLASS,
                classID, methodID, lineCodeIndex);

        ReplyPacket reply = setBreakpoint(breakpointLocation);
        checkReply(reply);

        requestID = reply.getNextValueAsInt();

        return requestID;
    }

    /**
     * Waits for stop on breakpoint and gets id of thread where it stopped.
     * 
     * @param requestID
     *            id of request for breakpoint
     * @return threadID id of thread, where we stop on breakpoint
     */
    public long waitForBreakpoint(long requestID) {
        // receive event
        CommandPacket event = null;
        event = receiveEvent();

        event.getNextValueAsByte();
        // suspendPolicy - is not used here

        // int numberOfEvents =
        event.getNextValueAsInt();

        long breakpointThreadID = 0;
        ParsedEvent[] eventParsed = ParsedEvent.parseEventPacket(event);

        if (eventParsed.length != 1) {
            throw new TestErrorException("Received " + eventParsed.length
                    + " events instead of 1 BREAKPOINT_EVENT");
        }

        // check if received event is for breakpoint
        if (eventParsed[0].getEventKind() == JDWPConstants.EventKind.BREAKPOINT) {
            breakpointThreadID = ((ParsedEvent.Event_BREAKPOINT) eventParsed[0])
                    .getThreadID();

        } else {
            throw new TestErrorException(
                    "Kind of received event is not BREAKPOINT_EVENT: "
                            + eventParsed[0].getEventKind());

        }

        if (eventParsed[0].getRequestID() != requestID) {
            throw new TestErrorException(
                    "Received BREAKPOINT_EVENT with another requestID: "
                            + eventParsed[0].getRequestID());
        }

        return breakpointThreadID;
    }

    /**
     * Removes breakpoint according to specified requestID.
     * 
     * @param requestID
     *            for given breakpoint
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket clearBreakpoint(int requestID) {

        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();

        // Set command. "2" - is ID of Clear command in EventRequest Command Set
        commandPacket
                .setCommand(JDWPCommands.EventRequestCommandSet.ClearCommand);

        // Set command set. "15" - is ID of EventRequest Command Set
        commandPacket
                .setCommandSet(JDWPCommands.EventRequestCommandSet.CommandSetID);

        // Set outgoing data
        // Set eventKind
        commandPacket.setNextValueAsByte(JDWPConstants.EventKind.BREAKPOINT);

        // Set suspendPolicy
        commandPacket.setNextValueAsInt(requestID);

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Removes all breakpoints.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket ClearAllBreakpoints() {

        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();

        // Set command. "3" - is ID of ClearAllBreakpoints command in
        // EventRequest Command Set
        commandPacket
                .setCommand(JDWPCommands.EventRequestCommandSet.ClearAllBreakpointsCommand);

        // Set command set. "15" - is ID of EventRequest Command Set
        commandPacket
                .setCommandSet(JDWPCommands.EventRequestCommandSet.CommandSetID);

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Requests debuggee VM capabilities. Function parses reply packet of
     * VirtualMachine::CapabilitiesNew command, creates and fills class
     * Capabilities with returned info.
     * 
     * @return ReplyPacket useless, already parsed reply packet.
     */
    public ReplyPacket capabilities() {

        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();

        // Set command. "17" - is ID of CapabilitiesNew command in
        // VirtualMachine Command Set
        commandPacket
                .setCommand(JDWPCommands.VirtualMachineCommandSet.CapabilitiesNewCommand);

        // Set command set. "1" - is ID of VirtualMachine Command Set
        commandPacket
                .setCommandSet(JDWPCommands.VirtualMachineCommandSet.CommandSetID);

        // Send packet
        ReplyPacket replyPacket = checkReply(performCommand(commandPacket));

        targetVMCapabilities = new Capabilities();

        // Set capabilities
        targetVMCapabilities.canWatchFieldModification = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canWatchFieldAccess = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetBytecodes = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetSyntheticAttribute = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetOwnedMonitorInfo = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetCurrentContendedMonitor = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetMonitorInfo = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canRedefineClasses = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canAddMethod = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.canUnrestrictedlyRedefineClasses = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canPopFrames = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.canUseInstanceFilters = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetSourceDebugExtension = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canRequestVMDeathEvent = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canSetDefaultStratum = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canGetInstanceInfo = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.reserved17 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.canGetMonitorFrameInfo = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canUseSourceNameFilters = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.canGetConstantPool = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.canForceEarlyReturn = replyPacket
                .getNextValueAsBoolean();
        targetVMCapabilities.reserved22 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved23 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved24 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved25 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved26 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved27 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved28 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved29 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved30 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved31 = replyPacket.getNextValueAsBoolean();
        targetVMCapabilities.reserved32 = replyPacket.getNextValueAsBoolean();

        return replyPacket;
    }

    /**
     * Resumes debuggee VM.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket resume() {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ResumeCommand);

        return checkReply(performCommand(commandPacket));
    }

    /**
     * Resumes specified thread on target Virtual Machine
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket resumeThread(long threadID) {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.ResumeCommand);

        commandPacket.setNextValueAsThreadID(threadID);
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Suspends debuggee VM.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket suspend() {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.SuspendCommand);

        return checkReply(performCommand(commandPacket));
    }

    /**
     * Suspends specified thread in debuggee VM.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket suspendThread(long threadID) {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.SuspendCommand);

        commandPacket.setNextValueAsThreadID(threadID);
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Disposes connection to debuggee VM.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket dispose() {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();
        commandPacket
                .setCommand(JDWPCommands.VirtualMachineCommandSet.DisposeCommand);
        commandPacket
                .setCommandSet(JDWPCommands.VirtualMachineCommandSet.CommandSetID);

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Exits debuggee VM process.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket exit(int exitCode) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();
        commandPacket
                .setCommand(JDWPCommands.VirtualMachineCommandSet.ExitCommand);
        commandPacket
                .setCommandSet(JDWPCommands.VirtualMachineCommandSet.CommandSetID);
        commandPacket.setNextValueAsInt(exitCode);

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Adjusts lengths for all VM-specific types.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket adjustTypeLength() {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();
        commandPacket
                .setCommand(JDWPCommands.VirtualMachineCommandSet.IDSizesCommand);
        commandPacket
                .setCommandSet(JDWPCommands.VirtualMachineCommandSet.CommandSetID);

        // Send packet
        ReplyPacket replyPacket = checkReply(performCommand(commandPacket));

        // Get FieldIDSize from ReplyPacket
        TypesLengths.setTypeLength(TypesLengths.FIELD_ID, replyPacket
                .getNextValueAsInt());

        // Get MethodIDSize from ReplyPacket
        TypesLengths.setTypeLength(TypesLengths.METHOD_ID, replyPacket
                .getNextValueAsInt());

        // Get ObjectIDSize from ReplyPacket
        TypesLengths.setTypeLength(TypesLengths.OBJECT_ID, replyPacket
                .getNextValueAsInt());

        // Get ReferenceTypeIDSize from ReplyPacket
        TypesLengths.setTypeLength(TypesLengths.REFERENCE_TYPE_ID, replyPacket
                .getNextValueAsInt());

        // Get FrameIDSize from ReplyPacket
        TypesLengths.setTypeLength(TypesLengths.FRAME_ID, replyPacket
                .getNextValueAsInt());

        // Adjust all other types lengths
        TypesLengths.setTypeLength(TypesLengths.ARRAY_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.STRING_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.THREAD_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.THREADGROUP_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.LOCATION_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.CLASS_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.CLASSLOADER_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        TypesLengths.setTypeLength(TypesLengths.CLASSOBJECT_ID, TypesLengths
                .getTypeLength(TypesLengths.OBJECT_ID));
        return replyPacket;
    }

    /**
     * Gets TypeID for specified type signature and type tag.
     * 
     * @param typeSignature
     *            type signature
     * @param classTypeTag
     *            type tag
     * @return received TypeID
     */
    public long getTypeID(String typeSignature, byte classTypeTag) {
        int classes = 0;
        byte refTypeTag = 0;
        long typeID = -1;

        // Request referenceTypeID for exception
        ReplyPacket classReference = getClassBySignature(typeSignature);

        // Get referenceTypeID from received packet
        classes = classReference.getNextValueAsInt();
        for (int i = 0; i < classes; i++) {
            refTypeTag = classReference.getNextValueAsByte();
            if (refTypeTag == classTypeTag) {
                typeID = classReference.getNextValueAsReferenceTypeID();
                classReference.getNextValueAsInt();
                break;
            } else {
                classReference.getNextValueAsReferenceTypeID();
                classReference.getNextValueAsInt();
                refTypeTag = 0;
            }
        }
        return typeID;
    }

    /**
     * Gets ClassID for specified class signature.
     * 
     * @param classSignature
     *            class signature
     * @return received ClassID
     */
    public long getClassID(String classSignature) {
        return getTypeID(classSignature, JDWPConstants.TypeTag.CLASS);
    }

    /**
     * Gets ThreadID for specified thread name.
     * 
     * @param threadName
     *            thread name
     * @return received ThreadID
     */
    public long getThreadID(String threadName) {
        ReplyPacket request = null;
        long threadID = -1;
        long thread = -1;
        String name = null;
        int threads = -1;

        // Get All Threads IDs
        request = getAllThreadID();

        // Get thread ID for threadName
        threads = request.getNextValueAsInt();
        for (int i = 0; i < threads; i++) {
            thread = request.getNextValueAsThreadID();
            name = getThreadName(thread);
            if (threadName.equals(name)) {
                threadID = thread;
                break;
            }
        }

        return threadID;
    }

    /**
     * Returns all running thread IDs.
     * 
     * @return received reply packet
     */
    public ReplyPacket getAllThreadID() {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.AllThreadsCommand);

        return checkReply(performCommand(commandPacket));
    }

    /**
     * Gets class signature for specified class ID.
     * 
     * @param classID
     *            class ID
     * @return received class signature
     */
    public String getClassSignature(long classID) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        commandPacket.setNextValueAsReferenceTypeID(classID);
        ReplyPacket replyPacket = checkReply(performCommand(commandPacket));
        return replyPacket.getNextValueAsString();
    }

    /**
     * Returns thread name for specified <code>threadID</code>.
     * 
     * @param threadID
     *            thread ID
     * @return thread name
     */
    public String getThreadName(long threadID) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.NameCommand);
        commandPacket.setNextValueAsThreadID(threadID);
        ReplyPacket replyPacket = checkReply(performCommand(commandPacket));
        return replyPacket.getNextValueAsString();
    }

    /**
     * Returns thread status for specified <code>threadID</code>.
     * 
     * @param threadID
     *            thread ID
     * @return thread status
     */
    public int getThreadStatus(long threadID) {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
        commandPacket.setNextValueAsThreadID(threadID);
        ReplyPacket replyPacket = checkReply(performCommand(commandPacket));
        return replyPacket.getNextValueAsInt();
    }

    /**
     * Returns name of thread group for specified <code>groupID</code>
     * 
     * @param groupID
     *            thread group ID
     * 
     * @return name of thread group
     */
    public String getThreadGroupName(long groupID) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ThreadGroupReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadGroupReferenceCommandSet.NameCommand);
        commandPacket.setNextValueAsReferenceTypeID(groupID);
        ReplyPacket replyPacket = checkReply(performCommand(commandPacket));
        return replyPacket.getNextValueAsString();
    }

    /**
     * Gets InterfaceID for specified interface signature.
     * 
     * @param interfaceSignature
     *            interface signature
     * @return received ClassID
     */
    public long getInterfaceID(String interfaceSignature) {
        return getTypeID(interfaceSignature, JDWPConstants.TypeTag.INTERFACE);
    }

    /**
     * Gets ArrayID for specified array signature.
     * 
     * @param arraySignature
     *            array signature
     * @return received ArrayID
     */
    public long getArrayID(String arraySignature) {
        return getTypeID(arraySignature, JDWPConstants.TypeTag.INTERFACE);
    }

    /**
     * Gets RequestID from specified ReplyPacket.
     * 
     * @param request
     *            ReplyPacket with RequestID
     * @return received RequestID
     */
    public int getRequestID(ReplyPacket request) {
        return request.getNextValueAsInt();
    }

    /**
     * Returns FieldID for specified class and field name.
     * 
     * @param classID
     *            ClassID to find field
     * @param fieldName
     *            field name
     * @return received FieldID
     */
    public long getFieldID(long classID, String fieldName) {
        ReplyPacket reply = getFieldsInClass(classID);
        return getFieldID(reply, fieldName);
    }

    /**
     * Gets FieldID from ReplyPacket.
     * 
     * @param request
     *            ReplyPacket for request
     * @param field
     *            field name to get ID for
     * @return received FieldID
     */
    public long getFieldID(ReplyPacket request, String field) {
        long fieldID = -1;
        String fieldName;
        // Get fieldID from received packet
        int count = request.getNextValueAsInt();
        for (int i = 0; i < count; i++) {
            fieldID = request.getNextValueAsFieldID();
            fieldName = request.getNextValueAsString();
            if (field.equals(fieldName)) {
                request.getNextValueAsString();
                request.getNextValueAsInt();
                break;
            } else {
                request.getNextValueAsString();
                request.getNextValueAsInt();
                fieldID = 0;
                fieldName = null;
            }
        }
        return fieldID;
    }

    /**
     * Gets Method ID for specified class and method name.
     * 
     * @param classID
     *            class to find method
     * @param methodName
     *            method name
     * @return received MethodID
     */
    public long getMethodID(long classID, String methodName) {
        ReplyPacket reply;
        int declared = 0;
        String method = null;
        long methodID = -1;

        // Get Method reference ID
        reply = getMethods(classID);

        // Get methodID from received packet
        declared = reply.getNextValueAsInt();
        for (int i = 0; i < declared; i++) {
            methodID = reply.getNextValueAsMethodID();
            method = reply.getNextValueAsString();
            if (methodName.equals(method)) {
                // If this method name is the same as requested
                reply.getNextValueAsString();
                reply.getNextValueAsInt();
                break;
            } else {
                // If this method name is not the requested one
                reply.getNextValueAsString();
                reply.getNextValueAsInt();
                methodID = -1;
                method = null;
            }
        }
        return methodID;
    }

    /**
     * Returns method name for specified pair of classID and methodID.
     * 
     * @param classID
     * @param methodID
     * @return method name
     */
    public String getMethodName(long classID, long methodID) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        packet.setNextValueAsReferenceTypeID(classID);
        ReplyPacket reply = performCommand(packet);

        int declared = reply.getNextValueAsInt();
        long mID;
        String value = null;
        String methodName = "";
        for (int i = 0; i < declared; i++) {
            mID = reply.getNextValueAsMethodID();
            methodName = reply.getNextValueAsString();
            reply.getNextValueAsString();
            reply.getNextValueAsInt();
            if (mID == methodID) {
                value = methodName;
                break;
            }
        }
        return value;
    }

    /**
     * Sets ClassPrepare event request for given class name pattern.
     * 
     * @param classRegexp
     *            Required class pattern. Matches are limited to exact matches
     *            of the given class pattern and matches of patterns that begin
     *            or end with '*'; for example, "*.Foo" or "java.*".
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setClassPrepared(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.CLASS_PREPARE;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Set ClassPrepare event request for given class ID.
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setClassPrepared(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.CLASS_PREPARE;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Sets ClassPrepare event request for given source name pattern.
     * 
     * @param sourceNamePattern
     *            Required source name pattern. Matches are limited to exact matches
     *            of the given source name pattern and matches of patterns that begin
     *            or end with '*'; for example, "*.Foo" or "java.*".
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setClassPreparedForSourceNameMatch(String sourceNamePattern) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.CLASS_PREPARE;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].sourceNamePattern = sourceNamePattern; 
        mods[0].modKind = EventMod.ModKind.SourceNameMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Sets ClassUnload event request for given class name pattern.
     * 
     * @param classSignature
     *            class signature
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setClassUnload(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.CLASS_UNLOAD;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Set ClassUnload event request for given class ID.
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setClassUnload(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.CLASS_UNLOAD;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Sets ClassLoad event request for given class signature.
     * 
     * @param classSignature
     *            class signature
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setClassLoad(String classSignature) {
        long typeID;

        // Request referenceTypeID for class
        typeID = getClassID(classSignature);

        // Set corresponding event
        return setClassLoad(typeID);
    }

    /**
     * Set ClassLoad event request for given class ID.
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setClassLoad(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.CLASS_LOAD;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Set MonitorContendedEnter event request for given class's reference type
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setMonitorContendedEnterForClassOnly(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    public ReplyPacket setMonitorContendedEnterForClassMatch(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Set MonitorContendedEntered event request for given class's reference type
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setMonitorContendedEnteredForClassOnly(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_CONTENDED_ENTERED;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    public ReplyPacket setMonitorContendedEnteredForClassMatch(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_CONTENDED_ENTERED;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Set MonitorWait event request for given class's reference type
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setMonitorWaitForClassOnly(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_WAIT;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Set MonitorWait event request for given given class name pattern.
     * 
     * @param classRegexp
     *            Required class pattern. Matches are limited to exact matches
     *            of the given class pattern and matches of patterns that begin
     *            or end with '*'; for example, "*.Foo" or "java.*".
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setMonitorWaitForClassMatch(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_WAIT;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Set MonitorWait event request for classes 
     * whose name does not match the given restricted regular expression.
     * 
     * @param classRegexp
     *            Exclude class pattern. Matches are limited to exact matches
     *            of the given class pattern and matches of patterns that begin
     *            or end with '*'; for example, "*.Foo" or "java.*".
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setMonitorWaitForClassExclude (String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_WAIT;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassExclude;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Set MonitorWaited event request for given class's reference type
     * 
     * @param referenceTypeID
     *            class referenceTypeID
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setMonitorWaitedForClassOnly(long referenceTypeID) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_WAITED;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].clazz = referenceTypeID;
        mods[0].modKind = EventMod.ModKind.ClassOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Set MonitorWaited event request for given given source name pattern.
     * 
     * @param classRegexp
     *            Required class pattern. Matches are limited to exact matches
     *            of the given class pattern and matches of patterns that begin
     *            or end with '*'; for example, "*.Foo" or "java.*".
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setMonitorWaitedForClassMatch(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_WAITED;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassMatch;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }
    
    /**
     * Set MonitorWaited event request for classes 
     * whose name does not match the given restricted regular expression.
     * 
     * @param classRegexp
     *            Required class pattern. Matches are limited to exact matches
     *            of the given class pattern and matches of patterns that begin
     *            or end with '*'; for example, "*.Foo" or "java.*".
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setMonitorWaitedForClassExclude (String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.MONITOR_WAITED;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].classPattern = classRegexp;
        mods[0].modKind = EventMod.ModKind.ClassExclude;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Set event request for given event.
     * 
     * @param event
     *            event to set request for
     * @return ReplyPacket for setting request
     */
    public ReplyPacket setEvent(Event event) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.EventRequestCommandSet.CommandSetID,
                JDWPCommands.EventRequestCommandSet.SetCommand);

        // Set eventKind
        commandPacket.setNextValueAsByte(event.eventKind);
        // Set suspendPolicy
        commandPacket.setNextValueAsByte(event.suspendPolicy);

        // Set modifiers
        commandPacket.setNextValueAsInt(event.modifiers);

        for (int i = 0; i < event.modifiers; i++) {

            commandPacket.setNextValueAsByte(event.mods[i].modKind);

            switch (event.mods[i].modKind) {
            case EventMod.ModKind.Count: {
                // Case Count
                commandPacket.setNextValueAsInt(event.mods[i].count);
                break;
            }
            case EventMod.ModKind.Conditional: {
                // Case Conditional
                commandPacket.setNextValueAsInt(event.mods[i].exprID);
                break;
            }
            case EventMod.ModKind.ThreadOnly: {
                // Case ThreadOnly
                commandPacket.setNextValueAsThreadID(event.mods[i].thread);
                break;
            }
            case EventMod.ModKind.ClassOnly: {
                // Case ClassOnly
                commandPacket
                        .setNextValueAsReferenceTypeID(event.mods[i].clazz);
                break;
            }
            case EventMod.ModKind.ClassMatch: {
                // Case ClassMatch
                commandPacket.setNextValueAsString(event.mods[i].classPattern);
                break;
            }
            case EventMod.ModKind.ClassExclude: {
                // Case ClassExclude
                commandPacket.setNextValueAsString(event.mods[i].classPattern);
                break;
            }
            case EventMod.ModKind.LocationOnly: {
                // Case LocationOnly
                commandPacket.setNextValueAsLocation(event.mods[i].loc);
                break;
            }
            case EventMod.ModKind.ExceptionOnly:
                // Case ExceptionOnly
                commandPacket
                        .setNextValueAsReferenceTypeID(event.mods[i].exceptionOrNull);
                commandPacket.setNextValueAsBoolean(event.mods[i].caught);
                commandPacket.setNextValueAsBoolean(event.mods[i].uncaught);
                break;
            case EventMod.ModKind.FieldOnly: {
                // Case FieldOnly
                commandPacket
                        .setNextValueAsReferenceTypeID(event.mods[i].declaring);
                commandPacket.setNextValueAsFieldID(event.mods[i].fieldID);
                break;
            }
            case EventMod.ModKind.Step: {
                // Case Step
                commandPacket.setNextValueAsThreadID(event.mods[i].thread);
                commandPacket.setNextValueAsInt(event.mods[i].size);
                commandPacket.setNextValueAsInt(event.mods[i].depth);
                break;
            }
            case EventMod.ModKind.InstanceOnly: {
                // Case InstanceOnly
                commandPacket.setNextValueAsObjectID(event.mods[i].instance);
                break;
            }
            case EventMod.ModKind.SourceNameMatch: {
                // Case SourceNameMatch 
                commandPacket.setNextValueAsString(event.mods[i].sourceNamePattern);
            }
            }
        }

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Gets method reference by signature.
     * 
     * @param classReferenceTypeID
     *            class referenceTypeID.
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket getMethods(long classReferenceTypeID) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();

        // Set command. "5" - is ID of Methods command in ReferenceType Command
        // Set
        commandPacket
                .setCommand(JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);

        // Set command set. "2" - is ID of ReferenceType Command Set
        commandPacket
                .setCommandSet(JDWPCommands.ReferenceTypeCommandSet.CommandSetID);

        // Set outgoing data
        // Set referenceTypeID
        commandPacket.setNextValueAsObjectID(classReferenceTypeID);

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Gets class reference by signature.
     * 
     * @param classSignature
     *            class signature.
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket getClassBySignature(String classSignature) {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        commandPacket.setNextValueAsString(classSignature);
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Gets class fields by class referenceTypeID.
     * 
     * @param referenceTypeID
     *            class referenceTypeID.
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket getFieldsInClass(long referenceTypeID) {
        CommandPacket commandPacket = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.FieldsCommand);
        commandPacket.setNextValueAsReferenceTypeID(referenceTypeID);
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Sets exception event request for given exception class signature.
     * 
     * @param exceptionSignature
     *            exception signature.
     * @param caught
     *            is exception caught
     * @param uncaught
     *            is exception uncaught
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setException(String exceptionSignature, boolean caught,
            boolean uncaught) {
        // Request referenceTypeID for exception
        long typeID = getClassID(exceptionSignature);
        return setException(typeID, caught, uncaught);
    }

    /**
     * Sets exception event request for given exception class ID.
     * 
     * @param exceptionID
     *            exception referenceTypeID.
     * @param caught
     *            is exception caught
     * @param uncaught
     *            is exception uncaught
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setException(long exceptionID, boolean caught,
            boolean uncaught) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.EXCEPTION;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[1];
        mods[0] = new EventMod();
        mods[0].modKind = EventMod.ModKind.ExceptionOnly;
        mods[0].caught = caught;
        mods[0].uncaught = uncaught;
        mods[0].exceptionOrNull = exceptionID;
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Sets exception event request for given exception class signature.
     * 
     * @param exceptionSignature
     *            exception signature.
     * @param caught
     *            is exception caught
     * @param uncaught
     *            is exception uncaught
     * @param count
     *            Limit the requested event to be reported at most once after a
     *            given number of occurrences
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setCountableException(String exceptionSignature,
            boolean caught, boolean uncaught, int count) {
        // Request referenceTypeID for exception
        long exceptionID = getClassID(exceptionSignature);
        byte eventKind = JDWPConstants.EventKind.EXCEPTION;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[2];
        mods[0] = new EventMod();
        mods[0].modKind = EventMod.ModKind.ExceptionOnly;
        mods[0].caught = caught;
        mods[0].uncaught = uncaught;
        mods[0].exceptionOrNull = exceptionID;

        mods[1] = new EventMod();
        mods[1].modKind = EventMod.ModKind.Count;
        mods[1].count = count;
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Sets METHOD_ENTRY event request for specified class name pattern.
     * 
     * @param classRegexp
     *            class name pattern or null for no pattern
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setMethodEntry(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.METHOD_ENTRY;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = null;
        if (classRegexp == null) {
            mods = new EventMod[0];
        } else {
            mods = new EventMod[1];
            mods[0] = new EventMod();
            mods[0].modKind = EventMod.ModKind.ClassMatch;
            mods[0].classPattern = classRegexp;
        }
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Sets METHOD_ENTRY event request for specified class name pattern.
     * 
     * @param classRegexp
     *            class name pattern or null for no pattern
     * @param count
     *            Limit the requested event to be reported at most once after a
     *            given number of occurrences
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setCountableMethodEntry(String classRegexp, int count) {
        byte eventKind = JDWPConstants.EventKind.METHOD_ENTRY;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = null;
        if (classRegexp == null) {
            mods = new EventMod[] { new EventMod() };
            mods[0].modKind = EventMod.ModKind.Count;
            mods[0].count = count;
        } else {
            mods = new EventMod[2];
            mods[0] = new EventMod();
            mods[0].modKind = EventMod.ModKind.ClassMatch;
            mods[0].classPattern = classRegexp;

            mods[1] = new EventMod();
            mods[1].modKind = EventMod.ModKind.Count;
            mods[1].count = count;
        }
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Sets METHOD_EXIT event request for specified class name pattern.
     * 
     * @param classRegexp
     *            class name pattern or null for no pattern
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setMethodExit(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.METHOD_EXIT;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = null;
        if (classRegexp == null) {
            mods = new EventMod[0];
        } else {
            mods = new EventMod[1];
            mods[0] = new EventMod();
            mods[0].modKind = EventMod.ModKind.ClassMatch;
            mods[0].classPattern = classRegexp;
        }
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }
    
    /**
     * Sets METHOD_EXIT_WITH_RETURN_VALUE event request for specified class name pattern.
     * 
     * @param classRegexp
     *            class name pattern or null for no pattern
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setMethodExitWithReturnValue(String classRegexp) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.METHOD_EXIT_WITH_RETURN_VALUE;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = null;
        if (classRegexp == null) {
            mods = new EventMod[0];
        } else {
            mods = new EventMod[1];
            mods[0] = new EventMod();
            mods[0].modKind = EventMod.ModKind.ClassMatch;
            mods[0].classPattern = classRegexp;
        }
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Sets METHOD_EXIT event request for specified class name pattern.
     * 
     * @param classRegexp
     *            classRegexp class name pattern or null for no pattern
     * @param count
     *            Limit the requested event to be reported at most once after a
     *            given number of occurrences
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setCountableMethodExit(String classRegexp, int count) {
        byte eventKind = JDWPConstants.EventKind.METHOD_EXIT;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = null;
        if (classRegexp == null) {
            mods = new EventMod[] { new EventMod() };
            mods[0].modKind = EventMod.ModKind.Count;
            mods[0].count = count;
        } else {
            mods = new EventMod[2];
            mods[0] = new EventMod();
            mods[0].modKind = EventMod.ModKind.ClassMatch;
            mods[0].classPattern = classRegexp;

            mods[1] = new EventMod();
            mods[1].modKind = EventMod.ModKind.Count;
            mods[1].count = count;
        }
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);

    }

    /**
     * Sets field access event request for specified class signature and field
     * name.
     * 
     * @param classTypeTag
     *            class Type Tag (class/interface/array)
     * @param classSignature
     *            class signature
     * @param fieldName
     *            field name
     * @return ReplyPacket if breakpoint is set
     * @throws ReplyErrorCodeException
     */
    public ReplyPacket setFieldAccess(String classSignature, byte classTypeTag,
            String fieldName) throws ReplyErrorCodeException {
        ReplyPacket request = null;
        long typeID = -1;
        long fieldID = -1;

        // Request referenceTypeID for class
        typeID = getClassID(classSignature);

        // Request fields in class
        request = getFieldsInClass(typeID);

        // Get fieldID from received packet
        fieldID = getFieldID(request, fieldName);

        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.FIELD_ACCESS;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].fieldID = fieldID;
        mods[0].declaring = typeID;
        mods[0].modKind = EventMod.ModKind.FieldOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set exception
        return setEvent(event);
    }

    /**
     * Sets field modification event request for specified class signature and
     * field name.
     * 
     * @param classTypeTag
     *            class Type Tag (class/interface/array)
     * @param classSignature
     *            class signature
     * @param fieldName
     *            field name
     * @return ReplyPacket for corresponding command
     * @throws ReplyErrorCodeException
     */
    public ReplyPacket setFieldModification(String classSignature,
            byte classTypeTag, String fieldName) throws ReplyErrorCodeException {
        ReplyPacket request = null;
        long typeID = -1;
        long fieldID = -1;

        // Request referenceTypeID for class
        typeID = getClassID(classSignature);

        // Request fields in class
        request = getFieldsInClass(typeID);

        // Get fieldID from received packet
        fieldID = getFieldID(request, fieldName);

        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.FIELD_MODIFICATION;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].fieldID = fieldID;
        mods[0].declaring = typeID;
        mods[0].modKind = EventMod.ModKind.FieldOnly;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Sets step event request for given thread name.
     * 
     * @param threadName
     *            thread name
     * @param stepSize
     * @param stepDepth
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setStep(String threadName, int stepSize, int stepDepth) {
        long typeID = -1;

        // Request referenceTypeID for class
        typeID = getThreadID(threadName);

        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.SINGLE_STEP;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        // EventMod[] mods = new EventMod[1];
        EventMod[] mods = new EventMod[] { new EventMod() };
        mods[0].thread = typeID;
        mods[0].modKind = EventMod.ModKind.Step;
        mods[0].size = stepSize;
        mods[0].depth = stepDepth;
        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Sets SINGLE_STEP event request for classes whose name does not match the
     * given restricted regular expression
     * 
     * @param classRegexp
     *            Disallowed class patterns. Matches are limited to exact
     *            matches of the given class pattern and matches of patterns
     *            that begin or end with '*'; for example, "*.Foo" or "java.*".
     * @param stepSize
     * @param stepDepth
     * @return ReplyPacket for setting request.
     */
    public ReplyPacket setStep(String[] classRegexp, long threadID,
            int stepSize, int stepDepth) {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.SINGLE_STEP;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        int modsSize = classRegexp.length + 1;
        EventMod[] mods = new EventMod[modsSize];
        for (int i = 0; i < classRegexp.length; i++) {
            mods[i] = new EventMod();
            mods[i].classPattern = classRegexp[i];
            mods[i].modKind = EventMod.ModKind.ClassExclude;
        }

        int index = modsSize - 1;
        mods[index] = new EventMod();
        mods[index].modKind = EventMod.ModKind.Step;
        mods[index].thread = threadID;
        mods[index].size = stepSize;
        mods[index].depth = stepDepth;

        Event event = new Event(eventKind, suspendPolicy, mods);

        // Set event
        return setEvent(event);
    }

    /**
     * Sets THREAD_START event request.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setThreadStart() {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.THREAD_START;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[0];
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Sets THREAD_END event request.
     * 
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket setThreadEnd() {
        // Prepare corresponding event
        byte eventKind = JDWPConstants.EventKind.THREAD_END;
        byte suspendPolicy = JDWPConstants.SuspendPolicy.ALL;
        EventMod[] mods = new EventMod[0];
        Event event = new Event(eventKind, suspendPolicy, mods);

        return setEvent(event);
    }

    /**
     * Clear an event request for specified request ID.
     * 
     * @param eventKind
     *            event type to clear
     * @param requestID
     *            request ID to clear
     * @return ReplyPacket for corresponding command
     */
    public ReplyPacket clearEvent(byte eventKind, int requestID) {
        // Create new command packet
        CommandPacket commandPacket = new CommandPacket();

        // Set command. "2" - is ID of Clear command in EventRequest Command Set
        commandPacket
                .setCommand(JDWPCommands.EventRequestCommandSet.ClearCommand);

        // Set command set. "15" - is ID of EventRequest Command Set
        commandPacket
                .setCommandSet(JDWPCommands.EventRequestCommandSet.CommandSetID);

        // Set outgoing data
        // Set event type to clear
        commandPacket.setNextValueAsByte(eventKind);

        // Set ID of request to clear
        commandPacket.setNextValueAsInt(requestID);

        // Send packet
        return checkReply(performCommand(commandPacket));
    }

    /**
     * Sends CommandPacket to debuggee VM and waits for ReplyPacket using
     * default timeout. All thrown exceptions are wrapped into
     * TestErrorException. Consider using checkReply() for checking error code
     * in reply packet.
     * 
     * @param command
     *            Command packet to be sent
     * @return received ReplyPacket
     */
    public ReplyPacket performCommand(CommandPacket command)
            throws TestErrorException {
        ReplyPacket replyPacket = null;
        try {
            replyPacket = packetDispatcher.performCommand(command);
        } catch (IOException e) {
            throw new TestErrorException(e);
        } catch (InterruptedException e) {
            throw new TestErrorException(e);
        }

        return replyPacket;
    }

    /**
     * Sends CommandPacket to debuggee VM and waits for ReplyPacket using
     * specified timeout.
     * 
     * @param command
     *            Command packet to be sent
     * @param timeout
     *            Timeout in milliseconds for waiting reply packet
     * @return received ReplyPacket
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public ReplyPacket performCommand(CommandPacket command, long timeout)
            throws IOException, InterruptedException, TimeoutException {

        return packetDispatcher.performCommand(command, timeout);
    }

    /**
     * Sends CommandPacket to debuggee VM without waiting for the reply. This
     * method is intended for special cases when there is need to divide
     * command's performing into two actions: command's sending and receiving
     * reply (e.g. for asynchronous JDWP commands' testing). After this method
     * the 'receiveReply()' method must be used latter for receiving reply for
     * sent command. It is NOT recommended to use this method for usual cases -
     * 'performCommand()' method must be used.
     * 
     * @param command
     *            Command packet to be sent
     * @return command ID of sent command
     * @throws IOException
     *             if any connection error occurred
     */
    public int sendCommand(CommandPacket command) throws IOException {
        return packetDispatcher.sendCommand(command);
    }

    /**
     * Waits for reply for command which was sent before by 'sendCommand()'
     * method. Default timeout is used as time limit for waiting. This method
     * (jointly with 'sendCommand()') is intended for special cases when there
     * is need to divide command's performing into two actions: command's
     * sending and receiving reply (e.g. for asynchronous JDWP commands'
     * testing). It is NOT recommended to use 'sendCommand()- receiveReply()'
     * pair for usual cases - 'performCommand()' method must be used.
     * 
     * @param commandId
     *            Command ID of sent before command, reply from which is
     *            expected to be received
     * @return received ReplyPacket
     * @throws IOException
     *             if any connection error occurred
     * @throws InterruptedException
     *             if reply packet's waiting was interrupted
     * @throws TimeoutException
     *             if timeout exceeded
     */
    public ReplyPacket receiveReply(int commandId) throws InterruptedException,
            IOException, TimeoutException {
        return packetDispatcher.receiveReply(commandId, config.getTimeout());
    }

    /**
     * Waits for reply for command which was sent before by 'sendCommand()'
     * method. Specified timeout is used as time limit for waiting. This method
     * (jointly with 'sendCommand()') is intended for special cases when there
     * is need to divide command's performing into two actions: command's
     * sending and receiving reply (e.g. for asynchronous JDWP commands'
     * testing). It is NOT recommended to use 'sendCommand()- receiveReply()'
     * pair for usual cases - 'performCommand()' method must be used.
     * 
     * @param commandId
     *            Command ID of sent before command, reply from which is
     *            expected to be received
     * @param timeout
     *            Specified timeout in milliseconds to wait for reply
     * @return received ReplyPacket
     * @throws IOException
     *             if any connection error occurred
     * @throws InterruptedException
     *             if reply packet's waiting was interrupted
     * @throws TimeoutException
     *             if timeout exceeded
     */
    public ReplyPacket receiveReply(int commandId, long timeout)
            throws InterruptedException, IOException, TimeoutException {
        return packetDispatcher.receiveReply(commandId, timeout);
    }

    /**
     * Waits for EventPacket using default timeout. All thrown exceptions are
     * wrapped into TestErrorException.
     * 
     * @return received EventPacket
     */
    public EventPacket receiveEvent() throws TestErrorException {
        try {
            return receiveEvent(config.getTimeout());
        } catch (IOException e) {
            throw new TestErrorException(e);
        } catch (InterruptedException e) {
            throw new TestErrorException(e);
        }
    }

    /**
     * Waits for EventPacket using specified timeout.
     * 
     * @param timeout
     *            Timeout in milliseconds to wait for event
     * @return received EventPacket
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public EventPacket receiveEvent(long timeout) throws IOException,
            InterruptedException, TimeoutException {

        return packetDispatcher.receiveEvent(timeout);
    }

    /**
     * Waits for expected event kind using default timeout. Throws
     * TestErrorException if received event is not of expected kind or not a
     * single event in the received event set.
     * 
     * @param eventKind
     *            Type of expected event -
     * @see JDWPConstants.EventKind
     * @return received EventPacket
     */
    public EventPacket receiveCertainEvent(byte eventKind)
            throws TestErrorException {

        EventPacket eventPacket = receiveEvent();
        ParsedEvent[] parsedEvents = ParsedEvent.parseEventPacket(eventPacket);

        if (parsedEvents.length == 1
                && parsedEvents[0].getEventKind() == eventKind)
            return eventPacket;

        switch (parsedEvents.length) {
        case (0):
            throw new TestErrorException(
                    "Unexpected event received: zero length");
        case (1):
            throw new TestErrorException("Unexpected event received: "
                    + parsedEvents[0].getEventKind());
        default:
            throw new TestErrorException(
                    "Unexpected event received: Event was grouped in a composite event");
        }
    }

    /**
     * Returns JDWP connection channel used by this VmMirror.
     * 
     * @return connection channel
     */
    public TransportWrapper getConnection() {
        return connection;
    }

    /**
     * Sets established connection channel to be used with this VmMirror and
     * starts reading packets.
     * 
     * @param connection
     *            connection channel to be set
     */
    public void setConnection(TransportWrapper connection) {
        this.connection = connection;
        packetDispatcher = new PacketDispatcher(connection, config, logWriter);
    }

    /**
     * Closes connection channel used with this VmMirror and stops reading
     * packets.
     * 
     */
    public void closeConnection() throws IOException {
        if (connection != null && connection.isOpen())
            connection.close();

        // wait for packetDispatcher is closed
        if (packetDispatcher != null) {
            try {
                packetDispatcher.join();
            } catch (InterruptedException e) {
                // do nothing but print a stack trace
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the count of frames on this thread's stack
     * 
     * @param threadID
     *            The thread object ID.
     * @return The count of frames on this thread's stack
     */
    public final int getFrameCount(long threadID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FrameCountCommand);
        command.setNextValueAsThreadID(threadID);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsInt();
    }

    /**
     * Returns a list containing all frames of a certain thread
     * 
     * @param threadID
     *            ID of the thread
     * @return A list of frames
     */
    public final List getAllThreadFrames(long threadID) {
        if (!isThreadSuspended(threadID)) {
            return new ArrayList(0);
        }

        ReplyPacket reply = getThreadFrames(threadID, 0, -1);
        int framesCount = reply.getNextValueAsInt();
        if (framesCount == 0) {
            return new ArrayList(0);
        }

        ArrayList<Frame> frames = new ArrayList<Frame>(framesCount);
        for (int i = 0; i < framesCount; i++) {
            Frame frame = new Frame();
            frame.setThreadID(threadID);
            frame.setID(reply.getNextValueAsFrameID());
            frame.setLocation(reply.getNextValueAsLocation());
            frames.add(frame);
        }

        return frames;
    }

    /**
     * Returns a set of frames of a certain suspended thread
     * 
     * @param threadID
     *            ID of the thread whose frames to obtain
     * @param startIndex
     *            The index of the first frame to retrieve.
     * @param length
     *            The count of frames to retrieve (-1 means all remaining).
     * @return ReplyPacket for corresponding command
     */
    public final ReplyPacket getThreadFrames(long threadID, int startIndex,
            int length) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.FramesCommand);
        command.setNextValueAsThreadID(threadID);
        command.setNextValueAsInt(startIndex); // start frame's index
        command.setNextValueAsInt(length); // get all remaining frames;
        return checkReply(performCommand(command));
    }

    /**
     * Returns variable information for the method
     * 
     * @param classID
     *            The class ID
     * @param methodID
     *            The method ID
     * @return A list containing all variables (arguments and locals) declared
     *         within the method.
     */
    public final List getVariableTable(long classID, long methodID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.VariableTableCommand);
        command.setNextValueAsReferenceTypeID(classID);
        command.setNextValueAsMethodID(methodID);
        // ReplyPacket reply =
        // debuggeeWrapper.vmMirror.checkReply(debuggeeWrapper.vmMirror.performCommand(command));
        ReplyPacket reply = performCommand(command);
        if (reply.getErrorCode() == JDWPConstants.Error.ABSENT_INFORMATION
                || reply.getErrorCode() == JDWPConstants.Error.NATIVE_METHOD) {
            return null;
        }

        checkReply(reply);

        reply.getNextValueAsInt(); // argCnt, is not used
        int slots = reply.getNextValueAsInt();
        if (slots == 0) {
            return null;
        }

        ArrayList<Variable> vars = new ArrayList<Variable>(slots);
        for (int i = 0; i < slots; i++) {
            Variable var = new Frame().new Variable();
            var.setCodeIndex(reply.getNextValueAsLong());
            var.setName(reply.getNextValueAsString());
            var.setSignature(reply.getNextValueAsString());
            var.setLength(reply.getNextValueAsInt());
            var.setSlot(reply.getNextValueAsInt());
            vars.add(var);
        }

        return vars;
    }

    /**
     * Returns values of local variables in a given frame
     * 
     * @param frame
     *            Frame whose variables to get
     * @return An array of Value objects
     */
    public final Value[] getFrameValues(Frame frame) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.GetValuesCommand);
        command.setNextValueAsThreadID(frame.getThreadID());
        command.setNextValueAsFrameID(frame.getID());
        int slots = frame.getVars().size();
        command.setNextValueAsInt(slots);
        Iterator it = frame.getVars().iterator();
        while (it.hasNext()) {
            Frame.Variable var = (Frame.Variable) it.next();
            command.setNextValueAsInt(var.getSlot());
            command.setNextValueAsByte(var.getTag());
        }

        ReplyPacket reply = checkReply(performCommand(command));
        reply.getNextValueAsInt(); // number of values , is not used
        Value[] values = new Value[slots];
        for (int i = 0; i < slots; i++) {
            values[i] = reply.getNextValueAsValue();
        }

        return values;
    }

    /**
     * Returns the immediate superclass of a class
     * 
     * @param classID
     *            The class ID whose superclass ID is to get
     * @return The superclass ID (null if the class ID for java.lang.Object is
     *         specified).
     */
    public final long getSuperclassId(long classID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.SuperclassCommand);
        command.setNextValueAsClassID(classID);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsClassID();
    }

    /**
     * Returns the runtime type of the object
     * 
     * @param objectID
     *            The object ID
     * @return The runtime reference type.
     */
    public final long getReferenceType(long objectID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.ReferenceTypeCommand);
        command.setNextValueAsObjectID(objectID);
        ReplyPacket reply = checkReply(performCommand(command));
        reply.getNextValueAsByte();
        return reply.getNextValueAsLong();
    }

    /**
     * Returns the class object corresponding to this type
     * 
     * @param refType
     *            The reference type ID.
     * @return The class object.
     */
    public final long getClassObjectId(long refType) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.ClassObjectCommand);
        command.setNextValueAsReferenceTypeID(refType);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsObjectID();
    }

    /**
     * Returns line number information for the method, if present.
     * 
     * @param refType
     *            The class ID
     * @param methodID
     *            The method ID
     * @return ReplyPacket for corresponding command.
     */
    public final ReplyPacket getLineTable(long refType, long methodID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        command.setNextValueAsReferenceTypeID(refType);
        command.setNextValueAsMethodID(methodID);
        // ReplyPacket reply =
        // debuggeeWrapper.vmMirror.checkReply(debuggeeWrapper.vmMirror.performCommand(command));
        // it is impossible to obtain line table information from native
        // methods, so reply checking is not performed
        ReplyPacket reply = performCommand(command);
        if (reply.getErrorCode() != JDWPConstants.Error.NONE) {
            if (reply.getErrorCode() == JDWPConstants.Error.NATIVE_METHOD) {
                return reply;
            }
        }

        return checkReply(reply);
    }

    /**
     * Returns the value of one or more instance fields.
     * 
     * @param objectID
     *            The object ID
     * @param fieldIDs
     *            IDs of fields to get
     * @return An array of Value objects representing each field's value
     */
    public final Value[] getObjectReferenceValues(long objectID, long[] fieldIDs) {
        int fieldsCount = fieldIDs.length;
        if (fieldsCount == 0) {
            return null;
        }

        CommandPacket command = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.GetValuesCommand);
        command.setNextValueAsReferenceTypeID(objectID);
        command.setNextValueAsInt(fieldsCount);
        for (int i = 0; i < fieldsCount; i++) {
            command.setNextValueAsFieldID(fieldIDs[i]);
        }

        ReplyPacket reply = checkReply(performCommand(command));
        reply.getNextValueAsInt(); // fields returned, is not used
        Value[] values = new Value[fieldsCount];
        for (int i = 0; i < fieldsCount; i++) {
            values[i] = reply.getNextValueAsValue();
        }

        return values;
    }

    /**
     * Returns the value of one or more static fields of the reference type
     * 
     * @param refTypeID
     *            The reference type ID.
     * @param fieldIDs
     *            IDs of fields to get
     * @return An array of Value objects representing each field's value
     */
    public final Value[] getReferenceTypeValues(long refTypeID, long[] fieldIDs) {
        int fieldsCount = fieldIDs.length;
        if (fieldsCount == 0) {
            return null;
        }

        CommandPacket command = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        command.setNextValueAsReferenceTypeID(refTypeID);
        command.setNextValueAsInt(fieldsCount);
        for (int i = 0; i < fieldsCount; i++) {
            command.setNextValueAsFieldID(fieldIDs[i]);
        }

        ReplyPacket reply = checkReply(performCommand(command));
        reply.getNextValueAsInt(); // fields returned, is not used
        Value[] values = new Value[fieldsCount];
        for (int i = 0; i < fieldsCount; i++) {
            values[i] = reply.getNextValueAsValue();
        }

        return values;
    }

    /**
     * Returns the value of the 'this' reference for this frame
     * 
     * @param threadID
     *            The frame's thread ID
     * @param frameID
     *            The frame ID.
     * @return The 'this' object ID for this frame.
     */
    public final long getThisObject(long threadID, long frameID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.ThisObjectCommand);
        command.setNextValueAsThreadID(threadID);
        command.setNextValueAsFrameID(frameID);
        ReplyPacket reply = checkReply(performCommand(command));
        TaggedObject taggedObject = reply.getNextValueAsTaggedObject();
        return taggedObject.objectID;
    }

    /**
     * Returns information for each field in a reference type including
     * inherited fields
     * 
     * @param classID
     *            The reference type ID
     * @return A list of Field objects representing each field of the class
     */
    public final List getAllFields(long classID) {
        ArrayList<Field> fields = new ArrayList<Field>(0);

        long superID = getSuperclassId(classID);
        if (superID != 0) {
            List superClassFields = getAllFields(superID);
            for (int i = 0; i < superClassFields.size(); i++) {
                fields.add((Field) superClassFields.toArray()[i]);
            }
        }

        ReplyPacket reply = getFieldsInClass(classID);
        int fieldsCount = reply.getNextValueAsInt();
        for (int i = 0; i < fieldsCount; i++) {
            Field field = new Field(reply.getNextValueAsFieldID(), classID,
                    reply.getNextValueAsString(), reply.getNextValueAsString(),
                    reply.getNextValueAsInt());
            fields.add(field);
        }

        return fields;
    }

    /**
     * Returns the reference type reflected by this class object
     * 
     * @param classObjectID
     *            The class object ID.
     * @return ReplyPacket for corresponding command
     */
    public final ReplyPacket getReflectedType(long classObjectID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ClassObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ClassObjectReferenceCommandSet.ReflectedTypeCommand);
        command.setNextValueAsClassObjectID(classObjectID);
        return checkReply(performCommand(command));
    }

    /**
     * Returns the JNI signature of a reference type. JNI signature formats are
     * described in the Java Native Interface Specification
     * 
     * @param refTypeID
     *            The reference type ID.
     * @return The JNI signature for the reference type.
     */
    public final String getReferenceTypeSignature(long refTypeID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.SignatureCommand);
        command.setNextValueAsReferenceTypeID(refTypeID);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsString();
    }

    /**
     * Returns the thread group that contains a given thread
     * 
     * @param threadID
     *            The thread object ID.
     * @return The thread group ID of this thread.
     */
    public final long getThreadGroupID(long threadID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.ThreadGroupCommand);
        command.setNextValueAsThreadID(threadID);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsThreadGroupID();
    }

    /**
     * Checks whether a given thread is suspended or not
     * 
     * @param threadID
     *            The thread object ID.
     * @return True if a given thread is suspended, false otherwise.
     */
    public final boolean isThreadSuspended(long threadID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ThreadReferenceCommandSet.CommandSetID,
                JDWPCommands.ThreadReferenceCommandSet.StatusCommand);
        command.setNextValueAsThreadID(threadID);
        ReplyPacket reply = checkReply(performCommand(command));
        reply.getNextValueAsInt(); // the thread's status; is not used
        return reply.getNextValueAsInt() == JDWPConstants.SuspendStatus.SUSPEND_STATUS_SUSPENDED;
    }

    /**
     * Returns JNI signature of method.
     * 
     * @param classID
     *            The reference type ID.
     * @param methodID
     *            The method ID.
     * @return JNI signature of method.
     */
    public final String getMethodSignature(long classID, long methodID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        command.setNextValueAsReferenceTypeID(classID);
        ReplyPacket reply = checkReply(performCommand(command));
        int methods = reply.getNextValueAsInt();
        String value = null;
        for (int i = 0; i < methods; i++) {
            long mID = reply.getNextValueAsMethodID();
            reply.getNextValueAsString(); // name of the method; is not used
            String methodSign = reply.getNextValueAsString();
            reply.getNextValueAsInt();
            if (mID == methodID) {
                value = methodSign;
                value = value.replaceAll("/", ".");
                int lastRoundBracketIndex = value.lastIndexOf(")");
                value = value.substring(0, lastRoundBracketIndex + 1);
                break;
            }
        }

        return value;
    }

    /**
     * Returns the characters contained in the string
     * 
     * @param objectID
     *            The String object ID.
     * @return A string value.
     */
    public final String getStringValue(long objectID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.StringReferenceCommandSet.CommandSetID,
                JDWPCommands.StringReferenceCommandSet.ValueCommand);
        command.setNextValueAsObjectID(objectID);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsString();
    }

    /**
     * Returns a range of array components
     * 
     * @param objectID
     *            The array object ID.
     * @return The retrieved values.
     */
    public Value[] getArrayValues(long objectID) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.LengthCommand);
        command.setNextValueAsArrayID(objectID);
        ReplyPacket reply = checkReply(performCommand(command));
        int length = reply.getNextValueAsInt();

        if (length == 0) {
            return null;
        }

        command = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.GetValuesCommand);
        command.setNextValueAsArrayID(objectID);
        command.setNextValueAsInt(0);
        command.setNextValueAsInt(length);
        reply = checkReply(performCommand(command));
        ArrayRegion arrayRegion = reply.getNextValueAsArrayRegion();

        Value[] values = new Value[length];
        for (int i = 0; i < length; i++) {
            values[i] = arrayRegion.getValue(i);
        }

        return values;
    }

    /**
     * Returns a source line number according to a corresponding line code index
     * in a method's line table.
     * 
     * @param classID
     *            The class object ID.
     * @param methodID
     *            The method ID.
     * @param codeIndex
     *            The line code index.
     * @return An integer line number.
     */
    public final int getLineNumber(long classID, long methodID, long codeIndex) {
        int lineNumber = -1;
        ReplyPacket reply = getLineTable(classID, methodID);
        if (reply.getErrorCode() != JDWPConstants.Error.NONE) {
            return lineNumber;
        }

        reply.getNextValueAsLong(); // start line index, is not used
        reply.getNextValueAsLong(); // end line index, is not used
        int lines = reply.getNextValueAsInt();
        for (int i = 0; i < lines; i++) {
            long lineCodeIndex = reply.getNextValueAsLong();
            lineNumber = reply.getNextValueAsInt();
            if (lineCodeIndex == codeIndex) {
                break;
            }

            if (lineCodeIndex > codeIndex) {
                --lineNumber;
                break;
            }
        }

        return lineNumber;
    }

    /**
     * Returns a line code index according to a corresponding line number in a
     * method's line table.
     * 
     * @param classID
     *            The class object ID.
     * @param methodID
     *            The method ID.
     * @param lineNumber
     *            A source line number.
     * @return An integer representing the line code index.
     */
    public final long getLineCodeIndex(long classID, long methodID,
            int lineNumber) {
        ReplyPacket reply = getLineTable(classID, methodID);
        if (reply.getErrorCode() != JDWPConstants.Error.NONE) {
            return -1L;
        }

        reply.getNextValueAsLong(); // start line index, is not used
        reply.getNextValueAsLong(); // end line index, is not used
        int lines = reply.getNextValueAsInt();
        for (int i = 0; i < lines; i++) {
            long lineCodeIndex = reply.getNextValueAsLong();
            if (lineNumber == reply.getNextValueAsInt()) {
                return lineCodeIndex;
            }
        }

        return -1L;
    }

    /**
     * Returns all variables which are visible within the given frame.
     * 
     * @param frame
     *            The frame whose visible local variables to retrieve.
     * @return A list of Variable objects representing each visible local
     *         variable within the given frame.
     */
    public final List getLocalVars(Frame frame) {
        List vars = getVariableTable(frame.getLocation().classID, frame
                .getLocation().methodID);
        if (vars == null) {
            return null;
        }

        // All variables that are not visible from within current frame must be
        // removed from the list
        long frameCodeIndex = frame.getLocation().index;
        for (int i = 0; i < vars.size(); i++) {
            Variable var = (Variable) vars.toArray()[i];
            long varCodeIndex = var.getCodeIndex();
            if (varCodeIndex > frameCodeIndex
                    || (frameCodeIndex >= varCodeIndex + var.getLength())) {
                vars.remove(i);
                --i;
                continue;
            }
        }

        return vars;
    }

    /**
     * Sets the value of one or more local variables
     * 
     * @param frame
     *            The frame ID.
     * @param vars
     *            An array of Variable objects whose values to set
     * @param values
     *            An array of Value objects to set
     */
    public final void setLocalVars(Frame frame, Variable[] vars, Value[] values) {
        if (vars.length != values.length) {
            throw new TestErrorException(
                    "Number of variables doesn't correspond to number of their values");
        }

        CommandPacket command = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.SetValuesCommand);
        command.setNextValueAsThreadID(frame.getThreadID());
        command.setNextValueAsFrameID(frame.getID());
        command.setNextValueAsInt(vars.length);
        for (int i = 0; i < vars.length; i++) {
            command.setNextValueAsInt(vars[i].getSlot());
            command.setNextValueAsValue(values[i]);
        }

        checkReply(performCommand(command));
    }

    /**
     * Sets the value of one or more instance fields
     * 
     * @param objectID
     *            The object ID.
     * @param fieldIDs
     *            An array of fields IDs
     * @param values
     *            An array of Value objects representing each value to set
     */
    public final void setInstanceFieldsValues(long objectID, long[] fieldIDs,
            Value[] values) {
        if (fieldIDs.length != values.length) {
            throw new TestErrorException(
                    "Number of fields doesn't correspond to number of their values");
        }

        CommandPacket command = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.SetValuesCommand);
        command.setNextValueAsObjectID(objectID);
        command.setNextValueAsInt(fieldIDs.length);
        for (int i = 0; i < fieldIDs.length; i++) {
            command.setNextValueAsFieldID(fieldIDs[i]);
            command.setNextValueAsUntaggedValue(values[i]);
        }

        checkReply(performCommand(command));
    }

    /**
     * Sets a range of array components. The specified range must be within the
     * bounds of the array.
     * 
     * @param arrayID
     *            The array object ID.
     * @param firstIndex
     *            The first index to set.
     * @param values
     *            An array of Value objects representing each value to set.
     */
    public final void setArrayValues(long arrayID, int firstIndex,
            Value[] values) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.ArrayReferenceCommandSet.CommandSetID,
                JDWPCommands.ArrayReferenceCommandSet.SetValuesCommand);
        command.setNextValueAsArrayID(arrayID);
        command.setNextValueAsInt(firstIndex);
        command.setNextValueAsInt(values.length);
        for (int i = 0; i < values.length; i++) {
            command.setNextValueAsUntaggedValue(values[i]);
        }

        checkReply(performCommand(command));
    }

    /**
     * Sets the value of one or more static fields
     * 
     * @param classID
     *            The class type ID.
     * @param fieldIDs
     *            An array of fields IDs
     * @param values
     *            An array of Value objects representing each value to set
     */
    public final void setStaticFieldsValues(long classID, long[] fieldIDs,
            Value[] values) {
        if (fieldIDs.length != values.length) {
            throw new TestErrorException(
                    "Number of fields doesn't correspond to number of their values");
        }

        CommandPacket command = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.SetValuesCommand);
        command.setNextValueAsClassID(classID);
        command.setNextValueAsInt(fieldIDs.length);
        for (int i = 0; i < fieldIDs.length; i++) {
            command.setNextValueAsFieldID(fieldIDs[i]);
            command.setNextValueAsUntaggedValue(values[i]);
        }

        checkReply(performCommand(command));
    }

    /**
     * Creates java String in target VM with the given value.
     * 
     * @param value
     *            The value of the string.
     * @return The string id.
     */
    public final long createString(String value) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.VirtualMachineCommandSet.CommandSetID,
                JDWPCommands.VirtualMachineCommandSet.CreateStringCommand);
        command.setNextValueAsString(value);
        ReplyPacket reply = checkReply(performCommand(command));
        return reply.getNextValueAsStringID();
    }

    /**
     * Processes JDWP PopFrames command from StackFrame command set.
     * 
     * @param frame
     *            The instance of Frame.
     */
    public final void popFrame(Frame frame) {
        CommandPacket command = new CommandPacket(
                JDWPCommands.StackFrameCommandSet.CommandSetID,
                JDWPCommands.StackFrameCommandSet.PopFramesCommand);
        command.setNextValueAsThreadID(frame.getThreadID());
        command.setNextValueAsFrameID(frame.getID());
        checkReply(performCommand(command));
    }

    /**
     * Invokes a member method of the given object.
     * 
     * @param objectID
     *            The object ID.
     * @param threadID
     *            The thread ID.
     * @param methodName
     *            The name of method for the invocation.
     * @param args
     *            The arguments for the invocation.
     * @param options
     *            The invocation options.
     * @return ReplyPacket for corresponding command
     */
    public final ReplyPacket invokeInstanceMethod(long objectID, long threadID,
            String methodName, Value[] args, int options) {
        long classID = getReferenceType(objectID);
        long methodID = getMethodID(classID, methodName);
        CommandPacket command = new CommandPacket(
                JDWPCommands.ObjectReferenceCommandSet.CommandSetID,
                JDWPCommands.ObjectReferenceCommandSet.InvokeMethodCommand);
        command.setNextValueAsObjectID(objectID);
        command.setNextValueAsThreadID(threadID);
        command.setNextValueAsClassID(classID);
        command.setNextValueAsMethodID(methodID);
        command.setNextValueAsInt(args.length);
        for (int i = 0; i < args.length; i++) {
            command.setNextValueAsValue(args[i]);
        }
        command.setNextValueAsInt(options);

        return checkReply(performCommand(command));
    }

    /**
     * Invokes a static method of the given class.
     * 
     * @param classID
     *            The class type ID.
     * @param threadID
     *            The thread ID.
     * @param methodName
     *            The name of method for the invocation.
     * @param args
     *            The arguments for the invocation.
     * @param options
     *            The invocation options.
     * @return ReplyPacket for corresponding command
     */
    public final ReplyPacket invokeStaticMethod(long classID, long threadID,
            String methodName, Value[] args, int options) {
        long methodID = getMethodID(classID, methodName);
        CommandPacket command = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.InvokeMethodCommand);
        command.setNextValueAsClassID(classID);
        command.setNextValueAsThreadID(threadID);
        command.setNextValueAsMethodID(methodID);
        command.setNextValueAsInt(args.length);
        for (int i = 0; i < args.length; i++) {
            command.setNextValueAsValue(args[i]);
        }
        command.setNextValueAsInt(options);

        return checkReply(performCommand(command));
    }
}
