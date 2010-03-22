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
 * @author Anatoly F. Bondarenko
 */

/**
 * Created on 06.10.2006
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.Location;
import org.apache.harmony.jpda.tests.framework.jdwp.ParsedEvent;

/**
 * CombinedEventsTestCase class provides auxiliary methods 
 * for JDWP unit tests for Co-located events.
 */
public abstract class CombinedEventsTestCase extends JDWPSyncTestCase {

    static Object waitTimeObject = new Object();
    static protected void waitMlsecsTime(long mlsecsTime) { 
        synchronized(waitTimeObject) {
            try {
                waitTimeObject.wait(mlsecsTime);
            } catch (Throwable throwable) {
                 // ignore
            }
        }
    }

    void printMethodLineTable(long classID, String className /* may be null */, String methodName) {
        long methodID = debuggeeWrapper.vmMirror.getMethodID(classID, methodName);
        if ( methodID == -1 ) {
            logWriter.println
            ("## printMethodLineTable(): Can NOT get methodID for classID = " 
                    + classID + "; Method name = " + methodName);
            return;
        }
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        packet.setNextValueAsClassID(classID);
        packet.setNextValueAsMethodID(methodID);
        ReplyPacket lineTableReply = debuggeeWrapper.vmMirror.performCommand(packet);
        if ( ! checkReplyPacketWithoutFail(lineTableReply,  "printMethodLineTable(): Method.LineTable command") ) {
            return;
        }
        if ( className == null ) {
            logWriter.println("=== Line Table for method: " + methodName + " ===");
        } else {
            logWriter.println("=== Line Table for method: " + methodName + " of class: " 
                    + className + " ===");
        }
        long methodStartCodeIndex = lineTableReply.getNextValueAsLong();
        logWriter.println("==> Method Start Code Index = " + methodStartCodeIndex);
        long methodEndCodeIndex = lineTableReply.getNextValueAsLong();
        logWriter.println("==> Method End Code Index = " + methodEndCodeIndex);
        
        int linesNumber = lineTableReply.getNextValueAsInt();
        logWriter.println("==> Number of lines = " + linesNumber);
        for (int i=0; i < linesNumber; i++) {
            long lineCodeIndex = lineTableReply.getNextValueAsLong();
            int lineNumber = lineTableReply.getNextValueAsInt();
            logWriter.println("====> Line Number " + lineNumber + " : Initial code index = " + lineCodeIndex);
        }
        logWriter.println("=== End of Line Table " + methodName + " ===");
    }

    long getMethodStartCodeIndex(long classID, String methodName) {
        long methodID = debuggeeWrapper.vmMirror.getMethodID(classID, methodName);
        if ( methodID == -1 ) {
            logWriter.println
            ("## getMethodStartCodeIndex(): Can NOT get methodID for classID = " 
                    + classID + "; Method name = " + methodName);
            return -1;
        }
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        packet.setNextValueAsClassID(classID);
        packet.setNextValueAsMethodID(methodID);
        ReplyPacket lineTableReply = debuggeeWrapper.vmMirror.performCommand(packet);
        if ( ! checkReplyPacketWithoutFail
                (lineTableReply,  "getMethodStartCodeIndex(): Method.LineTable command") ) {
            return -1;
        }
        long methodStartCodeIndex = lineTableReply.getNextValueAsLong();
        return methodStartCodeIndex;
    }

    @SuppressWarnings("unused")
    long getMethodEndCodeIndex(long classID, String methodName) {
        long methodID = debuggeeWrapper.vmMirror.getMethodID(classID, methodName);
        if ( methodID == -1 ) {
            logWriter.println
            ("## getMethodEndCodeIndex(): Can NOT get methodID for classID = " 
                    + classID + "; Method name = " + methodName);
            return -1;
        }
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        packet.setNextValueAsClassID(classID);
        packet.setNextValueAsMethodID(methodID);
        ReplyPacket lineTableReply = debuggeeWrapper.vmMirror.performCommand(packet);
        if ( ! checkReplyPacketWithoutFail
                (lineTableReply,  "getMethodEndCodeIndex(): Method.LineTable command") ) {
            return -1;
        }
        long methodStartCodeIndex = lineTableReply.getNextValueAsLong();
        long methodEndCodeIndex = lineTableReply.getNextValueAsLong();
        return methodEndCodeIndex;
    }

    protected Location getMethodEntryLocation(long classID, String methodName) {
        long methodID = debuggeeWrapper.vmMirror.getMethodID(classID, methodName);
        if ( methodID == -1 ) {
            logWriter.println
            ("## getClassMethodEntryLocation(): Can NOT get methodID for classID = " 
                    + classID + "; Method name = " + methodName);
            return null;
        }
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        packet.setNextValueAsClassID(classID);
        packet.setNextValueAsMethodID(methodID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        if ( ! checkReplyPacketWithoutFail
                (reply,  "getMethodEntryLocation(): Method.LineTable command") ) {
            return null;
        }
        long methodStartCodeIndex = reply.getNextValueAsLong();
        Location location = new Location();
        location.tag = JDWPConstants.TypeTag.CLASS;
        location.classID =  classID;
        location.methodID = methodID;
        location.index = methodStartCodeIndex;
        return location;
    }

    @SuppressWarnings("unused")
    protected Location getMethodEndLocation(long classID, String methodName) {
        long methodID = debuggeeWrapper.vmMirror.getMethodID(classID, methodName);
        if ( methodID == -1 ) {
            logWriter.println
            ("## getClassMethodEndLocation(): Can NOT get methodID for classID = " 
                    + classID + "; Method name = " + methodName);
            return null;
        }
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.MethodCommandSet.CommandSetID,
                JDWPCommands.MethodCommandSet.LineTableCommand);
        packet.setNextValueAsClassID(classID);
        packet.setNextValueAsMethodID(methodID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        if ( ! checkReplyPacketWithoutFail
                (reply,  "getMethodEndLocation(): Method.LineTable command") ) {
            return null;
        }
        long methodStartCodeIndex = reply.getNextValueAsLong();
        long methodEndCodeIndex = reply.getNextValueAsLong();
        Location location = new Location();
        location.tag = JDWPConstants.TypeTag.CLASS;
        location.classID =  classID;
        location.methodID = methodID;
        location.index = methodEndCodeIndex;
        return location;
    }

    protected boolean checkEventsLocation(ParsedEvent[] parsedEvents, Location expectedLocation) {
        boolean success = true;
        for (int i = 0; i < parsedEvents.length; i++) {
            byte eventKind = parsedEvents[i].getEventKind();
            long eventThreadID = 0;
            Location eventLocation = null;
            switch ( eventKind ) {
            case JDWPConstants.EventKind.METHOD_ENTRY:
                eventLocation = ((ParsedEvent.Event_METHOD_ENTRY)parsedEvents[i]).getLocation();
                eventThreadID = ((ParsedEvent.EventThread)parsedEvents[i]).getThreadID();
                break;
            case JDWPConstants.EventKind.SINGLE_STEP:
                eventLocation = ((ParsedEvent.Event_SINGLE_STEP)parsedEvents[i]).getLocation();
                eventThreadID = ((ParsedEvent.EventThread)parsedEvents[i]).getThreadID();
                break;
            case JDWPConstants.EventKind.BREAKPOINT:
                eventLocation = ((ParsedEvent.Event_BREAKPOINT)parsedEvents[i]).getLocation();
                eventThreadID = ((ParsedEvent.EventThread)parsedEvents[i]).getThreadID();
                break;
            case JDWPConstants.EventKind.METHOD_EXIT:
                eventLocation = ((ParsedEvent.Event_METHOD_EXIT)parsedEvents[i]).getLocation();
                eventThreadID = ((ParsedEvent.EventThread)parsedEvents[i]).getThreadID();
                break;
            default:
                logWriter.println("");
                logWriter.println("=> Chcek location for event " 
                    + ": Event kind = " + eventKind + "(" 
                    + JDWPConstants.EventKind.getName(eventKind) +")");
                logWriter.println("=> WARNING: This event is not suitable to check location!");
                continue;
            }
            logWriter.println("");
            logWriter.println("=> Chcek location for event " 
                + ": Event kind = " + eventKind + "(" 
                + JDWPConstants.EventKind.getName(eventKind) +"); eventThreadID = "
                + eventThreadID);
            long eventClassID = eventLocation.classID;
            logWriter.println("=> ClassID in event = " + eventClassID);
            if ( expectedLocation != null ) {
                if ( expectedLocation.classID != eventClassID ) {
                    logWriter.println("## FAILURE: Unexpected ClassID in event!");
                    logWriter.println("##          Expected ClassID  = " + expectedLocation.classID );
                    success = false;
                } else {
                    logWriter.println("=> OK - it is expected ClassID");
                }
            }
            long eventMethodID = eventLocation.methodID;
            logWriter.println("=> MethodID in event = " + eventMethodID);
            if ( expectedLocation != null ) {
                if ( expectedLocation.methodID != eventMethodID ) {
                    logWriter.println("## FAILURE: Unexpected MethodID in event!");
                    logWriter.println("##          Expected MethodID = " + expectedLocation.methodID);
                    success = false;
                } else {
                    logWriter.println("=> OK - it is expected MethodID");
                }
            }
            long eventCodeIndex = eventLocation.index;
            logWriter.println("=> CodeIndex in event = " + eventCodeIndex);
            if ( expectedLocation != null ) {
                if ( expectedLocation.index != eventCodeIndex ) {
                    logWriter.println("## FAILURE: Unexpected CodeIndex in event!");
                    logWriter.println("##          Expected CodeIndex = " 
                        + expectedLocation.index);
                    success = false;
                } else {
                    logWriter.println("=> OK - it is expected CodeIndex)");
                }
            }
        }
        logWriter.println("");
        if ( expectedLocation != null ) {
            if ( ! success ) {
                String failureMessage = "## FAILURE: Unexpected events' locations are found out!";
                logWriter.println(failureMessage);
            } else {
                logWriter.println("=> OK - all checked events have expected location!");
            }
        }
        return success;
    }

 }
