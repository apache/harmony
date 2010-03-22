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
 * Created on 12.23.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import org.apache.harmony.jpda.tests.framework.TestErrorException;

/**
 * This class provides types length for VM-independent and VM-dependent types.
 */
public class TypesLengths {

    // Type IDs
    public static final byte BYTE_ID = 1;

    public static final byte BOOLEAN_ID = 2;

    public static final byte INT_ID = 3;

    public static final byte LONG_ID = 4;

    public static final byte SHORT_ID = 5;

    public static final byte FLOAT_ID = 6;

    public static final byte DOUBLE_ID = 7;

    public static final byte VOID_ID = 8;

    public static final byte OBJECT_ID = 9;

    public static final byte ARRAY_ID = 10;

    public static final byte STRING_ID = 11;

    public static final byte THREAD_ID = 12;

    public static final byte THREADGROUP_ID = 13;

    public static final byte METHOD_ID = 14;

    public static final byte FIELD_ID = 15;

    public static final byte FRAME_ID = 16;

    public static final byte LOCATION_ID = 17;

    public static final byte REFERENCE_TYPE_ID = 18;

    public static final byte CLASS_ID = 19;

    public static final byte CLASSLOADER_ID = 20;

    public static final byte CLASSOBJECT_ID = 21;

    public static final byte CHAR_ID = 22;

    // Type lengths in bytes (VM-independent)

    private static int byteLength = 1;

    private static int booleanLength = 1;

    private static int intLength = 4;

    private static int longLength = 8;

    private static int shortLength = 2;

    private static int floatLength = 4;

    private static int doubleLength = 8;

    private static int voidLength = 0;

    private static int charLength = 2;

    // Type lengths in bytes (VM-dependent)

    private static int objectLength;

    private static int arrayLength;

    private static int stringLength;

    private static int threadLength;

    private static int threadGroupLength;

    private static int methodLength;

    private static int fieldLength;

    private static int frameLength;

    private static int locationLength;

    private static int referenceLength;

    private static int classLength;

    private static int classLoaderLength;

    private static int classObjectLength;

    /**
     * Gets types length for type ID.
     * 
     * @param typeID
     *            Type ID
     * @return type length
     */
    public static int getTypeLength(byte typeID) throws TestErrorException {
        switch (typeID) {
        case BYTE_ID: {
            return byteLength;
        }
        case BOOLEAN_ID: {
            return booleanLength;
        }
        case INT_ID: {
            return intLength;
        }
        case LONG_ID: {
            return longLength;
        }
        case SHORT_ID: {
            return shortLength;
        }
        case FLOAT_ID: {
            return floatLength;
        }
        case DOUBLE_ID: {
            return doubleLength;
        }
        case VOID_ID: {
            return voidLength;
        }
        case OBJECT_ID: {
            return objectLength;
        }
        case ARRAY_ID: {
            return arrayLength;
        }
        case STRING_ID: {
            return stringLength;
        }
        case THREAD_ID: {
            return threadLength;
        }
        case THREADGROUP_ID: {
            return threadGroupLength;
        }
        case METHOD_ID: {
            return methodLength;
        }
        case FIELD_ID: {
            return fieldLength;
        }
        case FRAME_ID: {
            return frameLength;
        }
        case LOCATION_ID: {
            return locationLength;
        }
        case REFERENCE_TYPE_ID: {
            return referenceLength;
        }
        case CLASS_ID: {
            return classLength;
        }
        case CLASSLOADER_ID: {
            return classLoaderLength;
        }
        case CLASSOBJECT_ID: {
            return classObjectLength;
        }
        case CHAR_ID: {
            return charLength;
        }
        default:
            throw new TestErrorException("Unexpected type ID: " + typeID);
        }
    }

    /**
     * Sets types length for type ID
     * 
     * @param typeID Type ID
     * @param typeLength type length
     */
    public static void setTypeLength(byte typeID, int typeLength)
            throws TestErrorException {
        switch (typeID) {
        case BYTE_ID: {
            byteLength = typeLength;
            return;
        }
        case BOOLEAN_ID: {
            booleanLength = typeLength;
            return;
        }
        case INT_ID: {
            intLength = typeLength;
            return;
        }
        case LONG_ID: {
            longLength = typeLength;
            return;
        }
        case SHORT_ID: {
            shortLength = typeLength;
            return;
        }
        case FLOAT_ID: {
            floatLength = typeLength;
            return;
        }
        case DOUBLE_ID: {
            doubleLength = typeLength;
            return;
        }
        case VOID_ID: {
            voidLength = typeLength;
            return;
        }
        case OBJECT_ID: {
            objectLength = typeLength;
            return;
        }
        case ARRAY_ID: {
            arrayLength = typeLength;
            return;
        }
        case STRING_ID: {
            stringLength = typeLength;
            return;
        }
        case THREAD_ID: {
            threadLength = typeLength;
            return;
        }
        case THREADGROUP_ID: {
            threadGroupLength = typeLength;
            return;
        }
        case METHOD_ID: {
            methodLength = typeLength;
            return;
        }
        case FIELD_ID: {
            fieldLength = typeLength;
            return;
        }
        case FRAME_ID: {
            frameLength = typeLength;
            return;
        }
        case LOCATION_ID: {
            locationLength = typeLength;
            return;
        }
        case REFERENCE_TYPE_ID: {
            referenceLength = typeLength;
            return;
        }
        case CLASS_ID: {
            classLength = typeLength;
            return;
        }
        case CLASSLOADER_ID: {
            classLoaderLength = typeLength;
            return;
        }
        case CLASSOBJECT_ID: {
            classObjectLength = typeLength;
            return;
        }
        case CHAR_ID: {
            classObjectLength = charLength;
            return;
        }
        default:
            throw new TestErrorException("Unexpected type ID: " + typeID);
        }
    }
}
