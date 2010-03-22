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
 * Created on 17.03.2005
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import org.apache.harmony.jpda.tests.framework.TestErrorException;

/**
 * This class represent parsed EventPacket with received event set data.
 */
public class ParsedEvent {

    private byte suspendPolicy;

    private int requestID;

    private byte eventKind;

    /**
     * Create new instance with specified data.
     */
    protected ParsedEvent(byte suspendPolicy, Packet packet, byte eventKind) {
        this.suspendPolicy = suspendPolicy;
        this.requestID = packet.getNextValueAsInt();
        this.eventKind = eventKind;
    }

    /**
     * Returns RequestID of this event set.
     * 
     * @return RequestID of this event set
     */
    public int getRequestID() {
        return requestID;
    }

    /**
     * Returns suspend policy of this event set.
     * 
     * @return suspend policy of this event set
     */
    public byte getSuspendPolicy() {
        return suspendPolicy;
    }

    /**
     * @return Returns the eventKind.
     */
    public byte getEventKind() {
        return eventKind;
    }

    /**
     * The class extends ParsedEvent by associating it with a thread.
     */
    public static class EventThread extends ParsedEvent {

        private long threadID;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        protected EventThread(byte suspendPolicy, Packet packet, byte eventKind) {
            super(suspendPolicy, packet, eventKind);
            this.threadID = packet.getNextValueAsThreadID();
        }

        /**
         * @return Returns the thread id.
         */
        public long getThreadID() {
            return threadID;
        }
    }

    /**
     * The class extends EventThread by associating it with a location.
     */
    private static class EventThreadLocation extends EventThread {

        private Location location;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        protected EventThreadLocation(byte suspendPolicy, Packet packet,
                byte eventKind) {
            super(suspendPolicy, packet, eventKind);
            this.location = packet.getNextValueAsLocation();
        }

        /**
         * @return Returns the location.
         */
        public Location getLocation() {
            return location;
        }
    }
    
    /**
     * The class extends EventThread by associating it with monitor object and location.
     */
    private static class EventThreadMonitor extends EventThread {

        private TaggedObject taggedObject;
        private Location location;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        protected EventThreadMonitor(byte suspendPolicy, Packet packet,
                byte eventKind) {
            super(suspendPolicy, packet, eventKind);
            this.taggedObject = packet.getNextValueAsTaggedObject();
            this.location = packet.getNextValueAsLocation();
        }

        /**
         * @return Returns the location.
         */
        public Location getLocation() {
            return location;
        }
        
        /**
         * @return Returns the taggedObject.
         */
        public TaggedObject getTaggedObject() {
            return taggedObject;
        }
    }

    /**
     * The class implements JDWP VM_START event.
     */
    public static final class Event_VM_START extends EventThread {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_VM_START(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.VM_START);
        }
    };

    /**
     * The class implements JDWP SINGLE_STEP event.
     */
    public static final class Event_SINGLE_STEP extends EventThreadLocation {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_SINGLE_STEP(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.SINGLE_STEP);
        }
    }

    /**
     * The class implements JDWP BREAKPOINT event.
     */
    public static final class Event_BREAKPOINT extends EventThreadLocation {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_BREAKPOINT(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.BREAKPOINT);
        }
    }

    /**
     * The class implements JDWP METHOD_ENTRY event.
     */
    public static final class Event_METHOD_ENTRY extends EventThreadLocation {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_METHOD_ENTRY(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.METHOD_ENTRY);
        }
    }

    /**
     * The class implements JDWP METHOD_EXIT event.
     */
    public static final class Event_METHOD_EXIT extends EventThreadLocation {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_METHOD_EXIT(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.METHOD_EXIT);
        }
    }
    
    /**
     * The class implements JDWP METHOD_EXIT_WITH_RETURN_VALUE event.
     */
    public static final class Event_METHOD_EXIT_WITH_RETURN_VALUE extends EventThreadLocation {

        private Value returnValue;
        
        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_METHOD_EXIT_WITH_RETURN_VALUE(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.METHOD_EXIT_WITH_RETURN_VALUE);
            returnValue = packet.getNextValueAsValue();
        }
        
        public Value getReturnValue(){
			return returnValue;
        }
    }
    
    /**
     * The class implements JDWP MONITOR_CONTENDED_ENTER event.
     */
    public static final class Event_MONITOR_CONTENDED_ENTER extends EventThreadMonitor {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_MONITOR_CONTENDED_ENTER(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER);
        }

    }
    
    /**
     * The class implements JDWP MONITOR_CONTENDED_ENTERED event.
     */
    public static final class Event_MONITOR_CONTENDED_ENTERED extends EventThreadMonitor {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_MONITOR_CONTENDED_ENTERED(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.MONITOR_CONTENDED_ENTERED);
        }

    }
    
    /**
     * The class implements JDWP METHOD_EXIT_WITH_RETURN_VALUE event.
     */
    public static final class Event_MONITOR_WAIT extends EventThreadMonitor {

        private long timeout;
        
        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_MONITOR_WAIT(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.MONITOR_WAIT);
            this.timeout = packet.getNextValueAsLong();
        }
        
        public long getTimeout(){
            return timeout;
        }
    }
    
    /**
     * The class implements JDWP METHOD_EXIT_WITH_RETURN_VALUE event.
     */
    public static final class Event_MONITOR_WAITED extends EventThreadMonitor {

        private boolean timed_out;
        
        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_MONITOR_WAITED(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.MONITOR_WAITED);
            this.timed_out = packet.getNextValueAsBoolean();
        }
        
        public boolean getTimedout(){
            return timed_out;
        }
    }

    /**
     * The class implements JDWP EXCEPTION event.
     */
    public static final class Event_EXCEPTION extends EventThreadLocation {

        private TaggedObject exception;

        private Location catchLocation;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_EXCEPTION(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.EXCEPTION);
            exception = packet.getNextValueAsTaggedObject();
            catchLocation = packet.getNextValueAsLocation();
        }

        /**
         * @return Returns the location of the caught exception.
         */
        public Location getCatchLocation() {
            return catchLocation;
        }

        /**
         * @return Returns the exception.
         */
        public TaggedObject getException() {
            return exception;
        }
    }

    /**
     * The class implements JDWP THREAD_START event.
     */
    public static final class Event_THREAD_START extends EventThread {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_THREAD_START(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.THREAD_START);
        }
    };

    /**
     * The class implements JDWP THREAD_DEATH event.
     */
    public static final class Event_THREAD_DEATH extends EventThread {

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_THREAD_DEATH(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.THREAD_DEATH);
        }
    };

    /**
     * The class implements JDWP CLASS_PREPARE event.
     */
    public static final class Event_CLASS_PREPARE extends EventThread {

        private byte refTypeTag;

        private long typeID;

        private String signature;

        private int status;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        protected Event_CLASS_PREPARE(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.CLASS_PREPARE);
            refTypeTag = packet.getNextValueAsByte();
            typeID = packet.getNextValueAsReferenceTypeID();
            signature = packet.getNextValueAsString();
            status = packet.getNextValueAsInt();
        }

        /**
         * @return Returns the refTypeTag.
         */
        public byte getRefTypeTag() {
            return refTypeTag;
        }

        /**
         * @return Returns the signature.
         */
        public String getSignature() {
            return signature;
        }

        /**
         * @return Returns the status.
         */
        public int getStatus() {
            return status;
        }

        /**
         * @return Returns the typeID.
         */
        public long getTypeID() {
            return typeID;
        }
    };

    /**
     * The class implements JDWP CLASS_UNLOAD event.
     */
    public static final class Event_CLASS_UNLOAD extends ParsedEvent {

        private String signature;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_CLASS_UNLOAD(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.CLASS_UNLOAD);
            signature = packet.getNextValueAsString();
        }

        /**
         * @return Returns the signature.
         */
        public String getSignature() {
            return signature;
        }
    };

    /**
     * The class implements JDWP FIELD_ACCESS event.
     */
    public static final class Event_FIELD_ACCESS extends EventThreadLocation {

        private byte refTypeTag;

        private long typeID;

        private long fieldID;

        private TaggedObject object;

        /**
         * A constructor.
         * 
         * @param suspendPolicy
         * @param packet
         */
        private Event_FIELD_ACCESS(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.FIELD_ACCESS);
            refTypeTag = packet.getNextValueAsByte();
            typeID = packet.getNextValueAsReferenceTypeID();
            fieldID = packet.getNextValueAsFieldID();
            object = packet.getNextValueAsTaggedObject();
        }

        /**
         * @return Returns the fieldID.
         */
        public long getFieldID() {
            return fieldID;
        }

        /**
         * @return Returns the object.
         */
        public TaggedObject getObject() {
            return object;
        }

        /**
         * @return Returns the refTypeTag.
         */
        public byte getRefTypeTag() {
            return refTypeTag;
        }

        /**
         * @return Returns the typeID.
         */
        public long getTypeID() {
            return typeID;
        }
    };

    /**
     * The class implements JDWP FIELD_MODIFICATION event.
     */
    public static final class Event_FIELD_MODIFICATION extends
            EventThreadLocation {
        private byte refTypeTag;

        private long typeID;

        private long fieldID;

        private TaggedObject object;

        private Value valueToBe;

        /**
         * A constructor.
         * @param suspendPolicy
         * @param packet
         */
        private Event_FIELD_MODIFICATION(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet,
                    JDWPConstants.EventKind.FIELD_MODIFICATION);
            refTypeTag = packet.getNextValueAsByte();
            typeID = packet.getNextValueAsReferenceTypeID();
            fieldID = packet.getNextValueAsFieldID();
            object = packet.getNextValueAsTaggedObject();
            valueToBe = packet.getNextValueAsValue();
        }

        /**
         * @return Returns the fieldID.
         */
        public long getFieldID() {
            return fieldID;
        }

        /**
         * @return Returns the object.
         */
        public TaggedObject getObject() {
            return object;
        }

        /**
         * @return Returns the refTypeTag.
         */
        public byte getRefTypeTag() {
            return refTypeTag;
        }

        /**
         * @return Returns the typeID.
         */
        public long getTypeID() {
            return typeID;
        }

        /**
         * @return Returns the valueToBe.
         */
        public Value getValueToBe() {
            return valueToBe;
        }
    };

    /**
     * The class implements JDWP VM_DEATH event.
     */
    public static final class Event_VM_DEATH extends ParsedEvent {
        /**
         * A constructor.
         * @param suspendPolicy
         * @param packet
         */
        private Event_VM_DEATH(byte suspendPolicy, Packet packet) {
            super(suspendPolicy, packet, JDWPConstants.EventKind.VM_DEATH);
        }
    };

    /**
     * Returns array of ParsedEvent extracted from given EventPacket.
     * 
     * @param packet
     *            EventPacket to parse events
     * @return array of extracted ParsedEvents
     */
    public static ParsedEvent[] parseEventPacket(Packet packet) {

        Packet packetCopy = new Packet(packet.toBytesArray());

        // Suspend Policy field
        byte suspendPolicy = packetCopy.getNextValueAsByte();

        // Number of events
        int eventCount = packetCopy.getNextValueAsInt();

        ParsedEvent[] events = new ParsedEvent[eventCount];

        // For all events in packet
        for (int i = 0; i < eventCount; i++) {
            byte eventKind = packetCopy.getNextValueAsByte();
            switch (eventKind) {
            case JDWPConstants.EventKind.VM_START: {
                events[i] = new Event_VM_START(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.SINGLE_STEP: {
                events[i] = new Event_SINGLE_STEP(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.BREAKPOINT: {
                events[i] = new Event_BREAKPOINT(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.METHOD_ENTRY: {
                events[i] = new Event_METHOD_ENTRY(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.METHOD_EXIT: {
                events[i] = new Event_METHOD_EXIT(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.METHOD_EXIT_WITH_RETURN_VALUE: {
                events[i] = new Event_METHOD_EXIT_WITH_RETURN_VALUE(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.MONITOR_CONTENDED_ENTER: {
                events[i] = new Event_MONITOR_CONTENDED_ENTER(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.MONITOR_CONTENDED_ENTERED: {
                events[i] = new Event_MONITOR_CONTENDED_ENTERED(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.MONITOR_WAIT: {
                events[i] = new Event_MONITOR_WAIT(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.MONITOR_WAITED: {
                events[i] = new Event_MONITOR_WAITED(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.EXCEPTION: {
                events[i] = new Event_EXCEPTION(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.THREAD_START: {
                events[i] = new Event_THREAD_START(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.THREAD_DEATH: {
                events[i] = new Event_THREAD_DEATH(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.CLASS_PREPARE: {
                events[i] = new Event_CLASS_PREPARE(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.CLASS_UNLOAD: {
                events[i] = new Event_CLASS_UNLOAD(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.FIELD_ACCESS: {
                events[i] = new Event_FIELD_ACCESS(suspendPolicy, packetCopy);
                break;
            }
            case JDWPConstants.EventKind.FIELD_MODIFICATION: {
                events[i] = new Event_FIELD_MODIFICATION(suspendPolicy,
                        packetCopy);
                break;
            }
            case JDWPConstants.EventKind.VM_DEATH: {
                events[i] = new Event_VM_DEATH(suspendPolicy, packetCopy);
                break;
            }
            default: {
                throw new TestErrorException("Unexpected kind of event: "
                        + eventKind);
            }
            }
        }
        return events;
    }

}
