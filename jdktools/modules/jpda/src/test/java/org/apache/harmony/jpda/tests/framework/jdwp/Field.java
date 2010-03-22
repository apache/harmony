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
 * Created on 03.05.2005 
 */

package org.apache.harmony.jpda.tests.framework.jdwp;

/**
 * This class provides description of class field.
 * 
 */
public final class Field {

    private long id;

    private long classID;

    private String name;

    private String signature;

    private int modBits;

    private byte tag;

    /**
     * Default constructor.
     */
    public Field() {
        id = -1;
        classID = -1;
        name = "unknown";
        signature = "unknown";
        modBits = -1;
    }

    /**
     * Constructor initializing all members of the Field instance.
     * 
     * @param id
     *            field id
     * @param classID
     *            class id
     * @param name
     *            field name
     * @param signature
     *            signature signature of the field class
     * @param modBits
     *            field modifiers
     */
    public Field(long id, long classID, String name, String signature,
            int modBits) {
        this.id = id;
        this.classID = classID;
        this.name = name;
        this.modBits = modBits;
        setSignature(signature);
    }

    /**
     * Sets signature and detects type tag from it.
     * 
     * @param signature
     *            signature of the field class
     */
    private void setSignature(String signature) {
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
     * Gets field id.
     * 
     * @return long
     */
    public long getID() {
        return this.id;
    }

    /**
     * Gets id of the field reference type.
     * 
     * @return long
     */
    public long getClassID() {
        return classID;
    }

    /**
     * Gets field name.
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Gets signature of field type.
     * 
     * @return String
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Gets field modifiers.
     * 
     * @return int
     */
    public int getModBits() {
        return modBits;
    }

    /**
     * Gets field java type.
     * 
     * @return String
     */
    public String getType() {
        String type = "unknown type";
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
                                                                 * skip ending
                                                                 * ';'
                                                                 */)
                        .replaceAll("/", ".")
                        + "[]"; // skip ending ';'
                break;
            }
            break;
        case JDWPConstants.Tag.OBJECT_TAG:
            type = signature
                    .substring(1, signature.length() - 1 /* skip ending ';' */)
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
     * Compares two Field objects.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @return boolean
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Field)) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        Field field = (Field) obj;
        return this.id == field.id && this.classID == field.classID
                && this.name.equals(field.name)
                && this.signature.equals(field.signature)
                && this.modBits == field.modBits;
    }

    /**
     * Converts Field object to String.
     * 
     * @see java.lang.Object#toString()
     * @return String
     */
    public String toString() {
        String str = "Field: id=" + id + ", classID=" + classID + ", name='"
                + name + "', signature='" + signature + "', modBits=";
        String access = "";
        if ((this.modBits & JDWPConstants.FieldAccess.ACC_PRIVATE) == JDWPConstants.FieldAccess.ACC_PRIVATE) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_PRIVATE)
                    + " ";
        } else if ((this.modBits & JDWPConstants.FieldAccess.ACC_PROTECTED) == JDWPConstants.FieldAccess.ACC_PROTECTED) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_PROTECTED)
                    + " ";
        } else if ((this.modBits & JDWPConstants.FieldAccess.ACC_PUBLIC) == JDWPConstants.FieldAccess.ACC_PUBLIC) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_PUBLIC)
                    + " ";
        }
        if ((this.modBits & JDWPConstants.FieldAccess.ACC_FINAL) == JDWPConstants.FieldAccess.ACC_FINAL) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_FINAL)
                    + " ";
        }
        if ((this.modBits & JDWPConstants.FieldAccess.ACC_STATIC) == JDWPConstants.FieldAccess.ACC_STATIC) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_STATIC)
                    + " ";
        }
        if ((this.modBits & JDWPConstants.FieldAccess.ACC_TRANSIENT) == JDWPConstants.FieldAccess.ACC_TRANSIENT) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_TRANSIENT)
                    + " ";
        }
        if ((this.modBits & JDWPConstants.FieldAccess.ACC_VOLATILE) == JDWPConstants.FieldAccess.ACC_VOLATILE) {
            access += JDWPConstants.FieldAccess
                    .getName(JDWPConstants.FieldAccess.ACC_VOLATILE)
                    + " ";
        }

        return str + access;
    }

    /**
     * Tells whether this field is private.
     * 
     * @return boolean
     */
    public boolean isPrivate() {
        return (modBits & JDWPConstants.FieldAccess.ACC_PRIVATE) == JDWPConstants.FieldAccess.ACC_PRIVATE;
    }

    /**
     * Tells whether this field is protected.
     * 
     * @return boolean
     */
    public boolean isProtected() {
        return (modBits & JDWPConstants.FieldAccess.ACC_PROTECTED) == JDWPConstants.FieldAccess.ACC_PROTECTED;
    }

    /**
     * Tells whether this field is public.
     * 
     * @return boolean
     */
    public boolean isPublic() {
        return (modBits & JDWPConstants.FieldAccess.ACC_PUBLIC) == JDWPConstants.FieldAccess.ACC_PUBLIC;
    }

    /**
     * Tells whether this field is final.
     * 
     * @return boolean
     */
    public boolean isFinal() {
        return (modBits & JDWPConstants.FieldAccess.ACC_FINAL) == JDWPConstants.FieldAccess.ACC_FINAL;
    }

    /**
     * Tells whether this field is static.
     * 
     * @return boolean
     */
    public boolean isStatic() {
        return (modBits & JDWPConstants.FieldAccess.ACC_STATIC) == JDWPConstants.FieldAccess.ACC_STATIC;
    }
}
