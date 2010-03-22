/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.tools.javah;

import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.Type;
import org.apache.harmony.tools.Mangler;

/**
 * This class is a wrapper of BCEL's Field class.
 *
 * This class depends on Apache Byte Code Engineering Library (BCEL) 5.0 or
 * later. Please see http://jakarta.apache.org/bcel for more information
 * about this library.
 */
public class ClazzField {

    /**
     * An owner class.
     */
    private Clazz clazz;

    /**
     * A wrapped field.
     */
    private Field wrappedField;

    /**
     * Constructs a <code>ClazzField</code> object.
     * 
     * @param clazz - an owner.
     * @param field - a wrapped Field object.
     */
    public ClazzField(Clazz clazz, Field wrappedField) {
        this.clazz = clazz;
        this.wrappedField = wrappedField;
    }

    /**
     * Returns a type of the wrapped field.
     * 
     * @return a field type.
     */
    private Type getType() {
        return Type.getReturnType(wrappedField.getSignature());
    }

    /**
     * Returns a name of the wrapped field.
     * 
     * @return a field name.
     */
    public String getName() {
        return wrappedField.getName();
    }

    /**
     * Returns a mangled name of a wrapped field.
     * 
     * @return a mangled field name.
     */
    public String getMangledName() {
        return Mangler.mangleFieldName(clazz.getName())
                + "_" + Mangler.mangleFieldName(getName());
    }

    /**
     * Returns a string representation of a native value
     * based on a wrapped field value.
     * 
     * @return a string that represents a wrapped field value
     * as a native data type.
     * @throws Exception - if the wrapped field value is inaccessible.
     */
    public String getNativeValue() throws Exception {
        return ClazzField.getNativeValue(getType(), 
                wrappedField.getConstantValue());
    }

    /**
     * Returns a string that represents a field part of
     * a JNI-style header file.
     */
    public String toString() {
        String n = System.getProperty("line.separator");
        StringBuffer result = new StringBuffer();

        String mangledName = getMangledName();
        try {
            String field = "#undef " + mangledName + n
                    + "#define " + mangledName + " " + getNativeValue() + n;

            // We add the field string only if there was no exception.
            result.append(field);
            result.append(n);
        } catch (Exception e) {
            result.append("/* Static field " + getName()
                    + " is not accessible */" + n);
            result.append(n);
        }

        return result.toString();
    }

    /**
     * Returns a string representation of a given object native value.
     * 
     * @param type - a Class object that wraps a data type.
     * @param value - an object that wraps a value of a primitive data type.
     * @return a string that represents a native data type.
     */
    public static String getNativeValue(Type type, ConstantValue value) {
        StringBuffer result = new StringBuffer();

        if (type == Type.INT) {
            result.append(value.toString()).append('L');
        } else if (type == Type.BYTE) {
            result.append(value.toString()).append('L');
        } else if (type == Type.LONG) {
            result.append(value.toString()).append("LL");
        } else if (type == Type.FLOAT) {
            result.append(value.toString()).append('f');
        } else if (type == Type.DOUBLE) {
            result.append(value.toString());
        } else if (type == Type.SHORT) {
            result.append(value.toString()).append('L');
        } else if (type == Type.CHAR) {
            result.append(value.toString()).append('L');
        } else if (type == Type.BOOLEAN) {
            result.append(value.toString()).append('L');
        }

        return result.toString();
    }
}
