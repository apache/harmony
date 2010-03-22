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
 * @author Aleksey V. Yantsen
 */

/**
 * Created on 10.25.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

/**
 * This class defines various constants from JDWP specifications.
 * Each class has getName function to convert a constant value
 * to string equivalent.
 */
public class JDWPConstants {

    /**
     * JDWP ThreadStatus constants
     */
    public static class ThreadStatus {

        public static final byte ZOMBIE = 0;

        public static final byte RUNNING = 1;

        public static final byte SLEEPING = 2;

        public static final byte MONITOR = 3;

        public static final byte WAIT = 4;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param status
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int status) {
            switch (status) {
            case ZOMBIE:
                return "ZOMBIE";
            case RUNNING:
                return "RUNNING";
            case SLEEPING:
                return "SLEEPING";
            case MONITOR:
                return "MONITOR";
            case WAIT:
                return "WAIT";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP SuspendStatus constants
     */
    public static class SuspendStatus {

        public static final byte SUSPEND_STATUS_SUSPENDED = 1;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param status
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int status) {
            if (status == SUSPEND_STATUS_SUSPENDED)
                return "SUSPENDED";
            return "NONE";
        }
    }

    /**
     * JDWP ClassStatus constants
     */
    public static class ClassStatus {

        public static final byte VERIFIED = 1;

        public static final byte PREPARED = 2;

        public static final byte INITIALIZED = 4;

        public static final byte ERROR = 8;

        // it looks like JDWP spec becomes out of date
        // see JVMTI specification for GetClassStatus:
        // 
        public static final byte ARRAY = 16;

        public static final byte PRIMITIVE = 32;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param status
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int status) {

            String returnValue = "";
            if ((status & VERIFIED) == VERIFIED)
                returnValue += "|VERIFIED";
            if ((status & PREPARED) == PREPARED)
                returnValue += "|PREPARED";
            if ((status & INITIALIZED) == INITIALIZED)
                returnValue += "|INITIALIZED";
            if ((status & ERROR) == ERROR)
                returnValue += "|ERROR";
            if ((status & ARRAY) == ARRAY)
                returnValue += "|ARRAY";
            if ((status & PRIMITIVE) == PRIMITIVE)
                returnValue += "|PRIMITIVE";

            if (returnValue.equals("")) {
                returnValue = "NONE";
            } else {
                returnValue = returnValue.substring(1);
            }

            return returnValue;
        }
    }

    /**
     * JDWP TypeTag constants
     */
    public static class TypeTag {

        public static final byte CLASS = 1;

        public static final byte INTERFACE = 2;

        public static final byte ARRAY = 3;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param refTypeTag
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(byte refTypeTag) {
            switch (refTypeTag) {
            case CLASS:
                return "CLASS";
            case INTERFACE:
                return "INTERFACE";
            case ARRAY:
                return "ARRAY";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP Tag constants
     */
    public static class Tag {

        public static final byte ARRAY_TAG = 91;

        public static final byte BYTE_TAG = 66;

        public static final byte CHAR_TAG = 67;

        public static final byte OBJECT_TAG = 76;

        public static final byte FLOAT_TAG = 70;

        public static final byte DOUBLE_TAG = 68;

        public static final byte INT_TAG = 73;

        public static final byte LONG_TAG = 74;

        public static final byte SHORT_TAG = 83;

        public static final byte VOID_TAG = 86;

        public static final byte BOOLEAN_TAG = 90;

        public static final byte STRING_TAG = 115;

        public static final byte THREAD_TAG = 116;

        public static final byte THREAD_GROUP_TAG = 103;

        public static final byte CLASS_LOADER_TAG = 108;

        public static final byte CLASS_OBJECT_TAG = 99;

        public static final byte NO_TAG = 0;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param tag
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(byte tag) {
            switch (tag) {
            case ARRAY_TAG:
                return "ARRAY_TAG";
            case BYTE_TAG:
                return "BYTE_TAG";
            case CHAR_TAG:
                return "CHAR_TAG";
            case OBJECT_TAG:
                return "OBJECT_TAG";
            case FLOAT_TAG:
                return "FLOAT_TAG";
            case DOUBLE_TAG:
                return "DOUBLE_TAG";
            case INT_TAG:
                return "INT_TAG";
            case LONG_TAG:
                return "LONG_TAG";
            case SHORT_TAG:
                return "SHORT_TAG";
            case VOID_TAG:
                return "VOID_TAG";
            case BOOLEAN_TAG:
                return "BOOLEAN_TAG";
            case STRING_TAG:
                return "STRING_TAG";
            case THREAD_TAG:
                return "THREAD_TAG";
            case THREAD_GROUP_TAG:
                return "THREAD_GROUP_TAG";
            case CLASS_LOADER_TAG:
                return "CLASS_LOADER_TAG";
            case CLASS_OBJECT_TAG:
                return "CLASS_OBJECT_TAG";
            case NO_TAG:
                return "NO_TAG";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP EventKind constants
     */
    public static class EventKind {

        public static final byte SINGLE_STEP = 1;

        public static final byte BREAKPOINT = 2;

        public static final byte FRAME_POP = 3;

        public static final byte EXCEPTION = 4;

        public static final byte USER_DEFINED = 5;

        public static final byte THREAD_START = 6;

        public static final byte THREAD_END = 7;

        public static final byte THREAD_DEATH = THREAD_END;

        public static final byte CLASS_PREPARE = 8;

        public static final byte CLASS_UNLOAD = 9;

        public static final byte CLASS_LOAD = 10;

        public static final byte FIELD_ACCESS = 20;

        public static final byte FIELD_MODIFICATION = 21;

        public static final byte EXCEPTION_CATCH = 30;

        public static final byte METHOD_ENTRY = 40;

        public static final byte METHOD_EXIT = 41;
        
        // METHOD_EXIT_WITH_RETURN_VALUE
        // MONITOR_CONTENDED_ENTER,MONITOR_CONTENDED_ENTER
        // MONITOR_WAIT, MONITOR_WAITED are new events for Java 6
        public static final byte METHOD_EXIT_WITH_RETURN_VALUE = 42;
        
        public static final byte MONITOR_CONTENDED_ENTER = 43;
        
        public static final byte MONITOR_CONTENDED_ENTERED = 44;
        
        public static final byte MONITOR_WAIT = 45;
        
        public static final byte MONITOR_WAITED = 46;   

        public static final byte VM_INIT = 90;

        public static final byte VM_START = VM_INIT;

        public static final byte VM_DEATH = 99;

        public static final byte VM_DISCONNECTED = 100;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param eventKind
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(byte eventKind) {
            switch (eventKind) {
            case SINGLE_STEP:
                return "SINGLE_STEP";
            case BREAKPOINT:
                return "BREAKPOINT";
            case FRAME_POP:
                return "FRAME_POP";
            case EXCEPTION:
                return "EXCEPTION";
            case USER_DEFINED:
                return "USER_DEFINED";
            case THREAD_START:
                return "THREAD_START";
            case THREAD_END:
                return "THREAD_END";
            case CLASS_PREPARE:
                return "CLASS_PREPARE";
            case CLASS_UNLOAD:
                return "CLASS_UNLOAD";
            case CLASS_LOAD:
                return "CLASS_LOAD";
            case FIELD_ACCESS:
                return "FIELD_ACCESS";
            case FIELD_MODIFICATION:
                return "FIELD_MODIFICATION";
            case EXCEPTION_CATCH:
                return "EXCEPTION_CATCH";
            case METHOD_ENTRY:
                return "METHOD_ENTRY";
            case METHOD_EXIT:
                return "METHOD_EXIT";
            case METHOD_EXIT_WITH_RETURN_VALUE:
                return "METHOD_EXIT_WITH_RETURN_VALUE";
            case MONITOR_CONTENDED_ENTER:
                return "MONITOR_CONTENDED_ENTER";
            case MONITOR_CONTENDED_ENTERED:
                return "MONITOR_CONTENDED_ENTERED";
            case MONITOR_WAIT:
                return "MONITOR_WAIT";
            case MONITOR_WAITED:
                return "MONITOR_WAITED";
            case VM_INIT:
                return "VM_INIT";
            case VM_DEATH:
                return "VM_DEATH";
            case VM_DISCONNECTED:
                return "VM_DISCONNECTED";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP Error constants
     */
    public static class Error {

        public static final int NONE = 0;

        public static final int INVALID_THREAD = 10;

        public static final int INVALID_THREAD_GROUP = 11;

        public static final int INVALID_PRIORITY = 12;

        public static final int THREAD_NOT_SUSPENDED = 13;

        public static final int THREAD_SUSPENDED = 14;

        public static final int INVALID_OBJECT = 20;

        public static final int INVALID_CLASS = 21;

        public static final int CLASS_NOT_PREPARED = 22;

        public static final int INVALID_METHODID = 23;

        public static final int INVALID_LOCATION = 24;

        public static final int INVALID_FIELDID = 25;

        public static final int INVALID_FRAMEID = 30;

        public static final int NO_MORE_FRAMES = 31;

        public static final int OPAQUE_FRAME = 32;

        public static final int NOT_CURRENT_FRAME = 33;

        public static final int TYPE_MISMATCH = 34;

        public static final int INVALID_SLOT = 35;

        public static final int DUPLICATE = 40;

        public static final int NOT_FOUND = 41;

        public static final int INVALID_MONITOR = 50;

        public static final int NOT_MONITOR_OWNER = 51;

        public static final int INTERRUPT = 52;

        public static final int INVALID_CLASS_FORMAT = 60;

        public static final int CIRCULAR_CLASS_DEFENITION = 61;

        public static final int FAILS_VERIFICATION = 62;

        public static final int ADD_METHOD_NOT_IMPLEMENTED = 63;

        public static final int SCHEMA_CHANGE_NOT_IMPLEMENTED = 64;

        public static final int INVALID_TYPESTATE = 65;

        public static final int HIERARCHY_CHANGE_NOT_IMPLEMENTED = 66;

        public static final int DELETE_METHOD_NOT_IMPLEMENTED = 67;

        public static final int UNSUPPORTED_VERSION = 68;

        public static final int NAMES_DONT_MATCH = 69;

        public static final int CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 70;

        public static final int METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 71;

        public static final int NOT_IMPLEMENTED = 99;

        public static final int NULL_POINTER = 100;

        public static final int ABSENT_INFORMATION = 101;

        public static final int INVALID_EVENT_TYPE = 102;

        public static final int ILLEGAL_ARGUMENT = 103;

        public static final int OUT_OF_MEMORY = 110;

        public static final int ACCESS_DENIED = 111;

        public static final int VM_DEAD = 112;

        public static final int INTERNAL = 113;

        public static final int UNATTACHED_THREAD = 115;

        public static final int INVALID_TAG = 500;

        public static final int ALREADY_INVOKING = 502;

        public static final int INVALID_INDEX = 503;

        public static final int INVALID_LENGTH = 504;

        public static final int INVALID_STRING = 506;

        public static final int INVALID_CLASS_LOADER = 507;

        public static final int INVALID_ARRAY = 508;

        public static final int TRANSPORT_LOAD = 509;

        public static final int TRANSPORT_INIT = 510;

        public static final int NATIVE_METHOD = 511;

        public static final int INVALID_COUNT = 512;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param errorCode
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int errorCode) {
            switch (errorCode) {
            case NONE:
                return "NONE";
            case INVALID_THREAD:
                return "INVALID_THREAD";
            case INVALID_THREAD_GROUP:
                return "INVALID_THREAD_GROUP";
            case INVALID_PRIORITY:
                return "INVALID_PRIORITY";
            case THREAD_NOT_SUSPENDED:
                return "THREAD_NOT_SUSPENDED";
            case THREAD_SUSPENDED:
                return "THREAD_SUSPENDED";
            case INVALID_OBJECT:
                return "INVALID_OBJECT";
            case INVALID_CLASS:
                return "INVALID_CLASS";
            case CLASS_NOT_PREPARED:
                return "CLASS_NOT_PREPARED";
            case INVALID_METHODID:
                return "INVALID_METHODID";
            case INVALID_LOCATION:
                return "INVALID_LOCATION";
            case INVALID_FIELDID:
                return "INVALID_FIELDID";
            case INVALID_FRAMEID:
                return "INVALID_FRAMEID";
            case NO_MORE_FRAMES:
                return "NO_MORE_FRAMES";
            case OPAQUE_FRAME:
                return "OPAQUE_FRAME";
            case NOT_CURRENT_FRAME:
                return "NOT_CURRENT_FRAME";
            case TYPE_MISMATCH:
                return "TYPE_MISMATCH";
            case INVALID_SLOT:
                return "INVALID_SLOT";
            case DUPLICATE:
                return "DUPLICATE";
            case NOT_FOUND:
                return "NOT_FOUND";
            case INVALID_MONITOR:
                return "INVALID_MONITOR";
            case NOT_MONITOR_OWNER:
                return "NOT_MONITOR_OWNER";
            case INTERRUPT:
                return "INTERRUPT";
            case INVALID_CLASS_FORMAT:
                return "INVALID_CLASS_FORMAT";
            case CIRCULAR_CLASS_DEFENITION:
                return "CIRCULAR_CLASS_DEFENITION";
            case FAILS_VERIFICATION:
                return "FAILS_VERIFICATION";
            case ADD_METHOD_NOT_IMPLEMENTED:
                return "ADD_METHOD_NOT_IMPLEMENTED";
            case SCHEMA_CHANGE_NOT_IMPLEMENTED:
                return "SCHEMA_CHANGE_NOT_IMPLEMENTED";
            case INVALID_TYPESTATE:
                return "INVALID_TYPESTATE";
            case HIERARCHY_CHANGE_NOT_IMPLEMENTED:
                return "HIERARCHY_CHANGE_NOT_IMPLEMENTED";
            case DELETE_METHOD_NOT_IMPLEMENTED:
                return "DELETE_METHOD_NOT_IMPLEMENTED";
            case UNSUPPORTED_VERSION:
                return "UNSUPPORTED_VERSION";
            case NAMES_DONT_MATCH:
                return "NAMES_DONT_MATCH";
            case CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
                return "CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED";
            case METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
                return "METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED";
            case NOT_IMPLEMENTED:
                return "NOT_IMPLEMENTED";
            case NULL_POINTER:
                return "NULL_POINTER";
            case ABSENT_INFORMATION:
                return "ABSENT_INFORMATION";
            case INVALID_EVENT_TYPE:
                return "INVALID_EVENT_TYPE";
            case ILLEGAL_ARGUMENT:
                return "ILLEGAL_ARGUMENT";
            case OUT_OF_MEMORY:
                return "OUT_OF_MEMORY";
            case ACCESS_DENIED:
                return "ACCESS_DENIED";
            case VM_DEAD:
                return "VM_DEAD";
            case INTERNAL:
                return "INTERNAL";
            case UNATTACHED_THREAD:
                return "UNATTACHED_THREAD";
            case INVALID_TAG:
                return "INVALID_TAG";
            case ALREADY_INVOKING:
                return "ALREADY_INVOKING";
            case INVALID_INDEX:
                return "INVALID_INDEX";
            case INVALID_LENGTH:
                return "INVALID_LENGTH";
            case INVALID_STRING:
                return "INVALID_STRING";
            case INVALID_CLASS_LOADER:
                return "INVALID_CLASS_LOADER";
            case INVALID_ARRAY:
                return "INVALID_ARRAY";
            case TRANSPORT_LOAD:
                return "TRANSPORT_LOAD";
            case TRANSPORT_INIT:
                return "TRANSPORT_INIT";
            case NATIVE_METHOD:
                return "NATIVE_METHOD";
            case INVALID_COUNT:
                return "INVALID_COUNT";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP StepDepth constants
     */
    public static class StepDepth {

        public static final byte INTO = 0;

        public static final byte OVER = 1;

        public static final byte OUT = 2;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param code
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int code) {
            switch (code) {
            case INTO:
                return "INTO";
            case OVER:
                return "OVER";
            case OUT:
                return "OUT";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP StepSize constants
     */
    public static class StepSize {

        public static final byte MIN = 0;

        public static final byte LINE = 1;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param code
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int code) {
            switch (code) {
            case MIN:
                return "MIN";
            case LINE:
                return "LINE";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP SuspendPolicy constants
     */
    public static class SuspendPolicy {

        public static final byte NONE = 0;

        public static final byte EVENT_THREAD = 1;

        public static final byte ALL = 2;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param code
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int code) {
            switch (code) {
            case NONE:
                return "NONE";
            case EVENT_THREAD:
                return "EVENT_THREAD";
            case ALL:
                return "ALL";
            default:
                return "<unknown>";
            }
        }
    }

    /**
     * JDWP InvokeOptions constants
     */
    public static class InvokeOptions {
        public static final byte INVOKE_SINGLE_THREADED = 0x01;

        public static final byte INVOKE_NONVIRTUAL = 0x02;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param code
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int code) {
            String buf = "NONE";
            if ((code & INVOKE_SINGLE_THREADED) != 0) {
                buf += "|INVOKE_SINGLE_THREADED";
            }
            if ((code & INVOKE_NONVIRTUAL) != 0) {
                buf += "|INVOKE_NONVIRTUAL";
            }
            if ((code & ~(INVOKE_SINGLE_THREADED | INVOKE_NONVIRTUAL)) != 0) {
                buf += "|<unknown>";
            }
            return buf;
        }
    }

    /**
     * Field access flags
     */
    public static class FieldAccess {

        /**
         * Is public; may be accessed from outside its package; Any field.
         */
        public static final int ACC_PUBLIC = 0x0001;

        /**
         * Is private; usable only within the defining class; Class field.
         */
        public static final int ACC_PRIVATE = 0x0002;

        /**
         * Is protected; may be accessed within subclasses; Class field.
         */
        public static final int ACC_PROTECTED = 0x0004;

        /**
         * Is static; Any field.
         */
        public static final int ACC_STATIC = 0x0008;

        /**
         * Is final; no further overriding or assignment after initialization;
         * Any field.
         */
        public static final int ACC_FINAL = 0x0010;

        /**
         * Is volatile; cannot be cached; Class field.
         */
        public static final int ACC_VOLATILE = 0x0040;

        /**
         * Is transient; not written or read by a persistent object manager;
         * Class field.
         */
        public static final int ACC_TRANSIENT = 0x0080;

        /**
         * Gets name for corresponding constant value.
         * 
         * @param code
         *            a constant from ones declared in this class
         * @return String
         */
        public static String getName(int code) {
            switch (code) {
            case ACC_PUBLIC:
                return "ACC_PUBLIC";
            case ACC_PRIVATE:
                return "ACC_PRIVATE";
            case ACC_PROTECTED:
                return "ACC_PROTECTED";
            case ACC_STATIC:
                return "ACC_STATIC";
            case ACC_FINAL:
                return "ACC_FINAL";
            case ACC_VOLATILE:
                return "ACC_VOLATILE";
            case ACC_TRANSIENT:
                return "ACC_TRANSIENT";
            default:
                return "<unknown>";
            }
        }
    }
}
