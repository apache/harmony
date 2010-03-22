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
 * @author Alexei S. Vaskin
 */

/**
 * Created on 22.04.2005 
 */

package org.apache.harmony.jpda.tests.framework.jdwp;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class provides description of frame.
 * 
 */
public class Frame {

    protected long threadID;

    protected Location loc;

    protected long id;

    protected ArrayList vars;

    /**
     * Default constructor.
     */
    public Frame() {
        threadID = -1;
        id = -1L;
        loc = new Location();
        vars = null;
    }

    /**
     * Constructor initializing all members of the Frame instance.
     * 
     * @param threadID
     *            thread id
     * @param id
     *            frame id
     * @param location
     *            frame location
     * @param vars
     *            list of variables
     */
    Frame(long threadID, long id, Location location, ArrayList vars) {
        this.threadID = threadID;
        this.id = id;
        this.loc = location;
        this.vars = vars;
    }

    /**
     * Gets thread id.
     * 
     * @return long
     */
    public long getThreadID() {
        return threadID;
    }

    /**
     * Sets new thread id.
     * 
     * @param threadID
     *            new thread id
     */
    public void setThreadID(long threadID) {
        this.threadID = threadID;
    }

    /**
     * Gets frame id.
     * 
     * @return long
     */
    public long getID() {
        return id;
    }

    /**
     * Sets new frame id.
     * 
     * @param id
     *            new frame id
     */
    public void setID(long id) {
        this.id = id;
    }

    /**
     * Gets frame location.
     * 
     * @return Location
     */
    public Location getLocation() {
        return loc;
    }

    /**
     * Sets new frame location.
     * 
     * @param location
     *            new frame location
     */
    public void setLocation(Location location) {
        this.loc = location;
    }

    /**
     * Gets frame variables.
     * 
     * @return list of frame variables
     */
    public ArrayList getVars() {
        return vars;
    }

    /**
     * Sets new frame variables.
     * 
     * @param vars
     *            list of new frame variables
     */
    public void setVars(ArrayList vars) {
        this.vars = vars;
    }

    /**
     * Converts Frame object to String.
     * 
     * @see java.lang.Object#toString()
     * @return String
     */
    public String toString() {
        String string = "Frame: id=" + id + ", threadID=" + threadID
                + ", location=" + loc.toString() + "\n";
        string += "--- Variables ---";
        Iterator it = vars.iterator();
        while (it.hasNext()) {
            string += ((Variable) it.next()).toString();
        }
        return string;
    }

    /**
     * Compares two Frame objects.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @return boolean
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Frame)) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        Frame frame = (Frame) obj;
        if (this.threadID != frame.threadID || this.id != frame.id
                || !(this.loc.equals(frame.loc))) {
            return false;
        }

        if (vars.size() != frame.vars.size()) {
            return false;
        }

        if (!vars.equals(frame.vars)) {
            return false;
        }

        return true;
    }

    /**
     * This describing frame variable.
     * 
     */
    public final class Variable {
        private long codeIndex;

        private String name;

        private String signature;

        private int length;

        private int slot;

        private byte tag;

        private String type;

        /**
         * Constructor.
         * 
         */
        public Variable() {
            codeIndex = -1;
            name = "unknown";
            signature = "unknown";
            length = -1;
            slot = -1;
            tag = JDWPConstants.Tag.NO_TAG;
            type = "unknown type";
        }

        /**
         * Gets code index of variable.
         * 
         * @return long
         */
        public long getCodeIndex() {
            return codeIndex;
        }

        /**
         * Sets new code index for variable.
         * 
         * @param codeIndex
         */
        public void setCodeIndex(long codeIndex) {
            this.codeIndex = codeIndex;
        }

        /**
         * Gets variable name.
         * 
         * @return String
         */
        public String getName() {
            return name;
        }

        /**
         * Sets new variable name.
         * 
         * @param name
         *            new variable name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets signature of the variable reference type.
         * 
         * @return String
         */
        public String getSignature() {
            return signature;
        }

        /**
         * Sets new signature and detects value of a type tag.
         * 
         * @param signature
         */
        public void setSignature(String signature) {
            switch (signature.charAt(0)) {
            case '[':
                tag = JDWPConstants.Tag.ARRAY_TAG;
                break;
            case 'B':
                tag = JDWPConstants.Tag.BYTE_TAG;
                break;
            case 'C':
                tag = JDWPConstants.Tag.CHAR_TAG;
                break;
            case 'L':
                tag = JDWPConstants.Tag.OBJECT_TAG;
                break;
            case 'F':
                tag = JDWPConstants.Tag.FLOAT_TAG;
                break;
            case 'D':
                tag = JDWPConstants.Tag.DOUBLE_TAG;
                break;
            case 'I':
                tag = JDWPConstants.Tag.INT_TAG;
                break;
            case 'J':
                tag = JDWPConstants.Tag.LONG_TAG;
                break;
            case 'S':
                tag = JDWPConstants.Tag.SHORT_TAG;
                break;
            case 'V':
                tag = JDWPConstants.Tag.VOID_TAG;
                break;
            case 'Z':
                tag = JDWPConstants.Tag.BOOLEAN_TAG;
                break;
            case 's':
                tag = JDWPConstants.Tag.STRING_TAG;
                break;
            case 't':
                tag = JDWPConstants.Tag.THREAD_TAG;
                break;
            case 'g':
                tag = JDWPConstants.Tag.THREAD_GROUP_TAG;
                break;
            case 'l':
                tag = JDWPConstants.Tag.CLASS_LOADER_TAG;
                break;
            case 'c':
                tag = JDWPConstants.Tag.CLASS_OBJECT_TAG;
                break;
            }

            this.signature = signature;
        }

        /**
         * Gets variable length.
         * 
         * @return int
         */
        public int getLength() {
            return length;
        }

        /**
         * Sets new variable length.
         * 
         * @param length
         *            new variable length
         */
        public void setLength(int length) {
            this.length = length;
        }

        /**
         * Returns variable slot value.
         * 
         * @return int
         */
        public int getSlot() {
            return slot;
        }

        /**
         * Assigns new slot value.
         * 
         * @param slot
         *            new slot value
         */
        public void setSlot(int slot) {
            this.slot = slot;
        }

        /**
         * Gets variable type tag value.
         * 
         * @return byte
         */
        public byte getTag() {
            return tag;
        }

        /**
         * Gets variable java type.
         * 
         * @return String
         */
        public String getType() {
            switch (tag) {
            case JDWPConstants.Tag.ARRAY_TAG:
                switch (signature.charAt(1)) {
                case 'B':
                    type = "byte[]";
                    break;
                case 'C':
                    type = "char[]";
                    break;
                case 'J':
                    type = "long[]";
                    break;
                case 'F':
                    type = "float[]";
                    break;
                case 'D':
                    type = "double[]";
                    break;
                case 'I':
                    type = "int[]";
                    break;
                case 'S':
                    type = "short[]";
                    break;
                case 'V':
                    type = "void[]";
                    break;
                case 'Z':
                    type = "boolean[]";
                    break;
                case 's':
                    type = "java.Lang.String[]";
                    break;
                case 'L':
                    type = signature
                            .substring(2, signature.length() - 1 /*
                                                                     * skip
                                                                     * ending
                                                                     * ';'
                                                                     */)
                            .replaceAll("/", ".")
                            + "[]"; // skip ending ';'
                    break;
                }
                break;
            case JDWPConstants.Tag.OBJECT_TAG:
                type = signature
                        .substring(1, signature.length() - 1 /*
                                                                 * skip ending
                                                                 * ';'
                                                                 */)
                        .replaceAll("/", "."); // skip ending ';'
                break;
            case JDWPConstants.Tag.BOOLEAN_TAG:
                type = "boolean";
                break;
            case JDWPConstants.Tag.BYTE_TAG:
                type = "byte";
                break;
            case JDWPConstants.Tag.CHAR_TAG:
                type = "char";
                break;
            case JDWPConstants.Tag.DOUBLE_TAG:
                type = "double";
                break;
            case JDWPConstants.Tag.FLOAT_TAG:
                type = "float";
                break;
            case JDWPConstants.Tag.INT_TAG:
                type = "int";
                break;
            case JDWPConstants.Tag.LONG_TAG:
                type = "long";
                break;
            case JDWPConstants.Tag.SHORT_TAG:
                type = "short";
                break;
            case JDWPConstants.Tag.STRING_TAG:
                type = "string";
                break;
            default:
                break;
            }

            return type;
        }

        /**
         * Converts Variable object to String.
         * 
         * @see java.lang.Object#toString()
         * @return String
         */
        public String toString() {
            return "Variable: codeIndex=" + codeIndex + ", name=" + name
                    + ", signature=" + signature + ", length=" + length
                    + ", slot=" + slot + ", tag="
                    + JDWPConstants.Tag.getName(tag) + ", type=" + type;
        }

        /**
         * Compares two Variable objects.
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         * @return boolean
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof Variable)) {
                return false;
            }

            if (this.getClass() != obj.getClass()) {
                return false;
            }

            Variable var = (Variable) obj;
            return this.codeIndex == var.codeIndex
                    && this.name.equals(var.name)
                    && this.signature.equals(var.signature)
                    && this.length == var.length && this.slot == var.slot
                    && this.tag == var.tag && this.type.equals(var.type);

        }
    }
}
