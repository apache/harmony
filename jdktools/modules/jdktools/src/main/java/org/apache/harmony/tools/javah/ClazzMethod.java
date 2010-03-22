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

import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.harmony.tools.Mangler;

/**
 * This class is a wrapper of BCEL's Method class.
 *
 * This class depends on Apache Byte Code Engineering Library (BCEL) 5.0 or
 * later. Please see http://jakarta.apache.org/bcel for more information
 * about this library.
 */
public class ClazzMethod {

    /**
     * An owner class.
     */
    private Clazz clazz;

    /**
     * A wrapped method.
     */
    private Method wrappedMethod;

    /**
     * Indicates if a wrapped method is overloaded.
     */
    private boolean overloaded;

    /**
     * Constructs a <code>ClazzMethod</code> object.
     * 
     * @param clazz - an owner.
     * @param wrappedMethod - a wrapped Method class.
     * @param overloaded - <code>true</code> if the wrapped method
     * is overloaded; <code>false</code> otherwise.
     */
    public ClazzMethod(Clazz clazz, Method wrappedMethod, boolean overloaded) {
        this.clazz = clazz;
        this.wrappedMethod = wrappedMethod;
        this.overloaded = overloaded;
    }

    /**
     * Returns the argument types.
     * 
     * @return an array of the argument types.
     */
    private Type[] getArgumentTypes() {
        return Type.getArgumentTypes(wrappedMethod.getSignature());
    }

    /**
     * Returns the return type.
     * 
     * @return the return type.
     */
    private Type getReturnType() {
        return Type.getReturnType(wrappedMethod.getSignature());
    }

    /**
     * Returns a parameters signature.
     * 
     * @return a parameters signature string.
     */
    private String getParamsSignature() {
        StringBuffer result = new StringBuffer();

        Type types[] = getArgumentTypes();
        for (int i = 0; i < types.length; i++) {
            result.append(types[i].getSignature());
        }

        return result.toString();
    }

    /**
     * Returns a method signature.
     * 
     * @return a method signature string.
     */
    public String getSignature() {
        return wrappedMethod.getSignature();
    }

    /**
     * Determines if a wrapped method is static.
     * 
     * @return <code>true</code> if a wrapped method is static;
     * <code>false</code> otherwise.
     */
    private boolean isStatic() {
        return wrappedMethod.isStatic();
    }

    /**
     * Returns a name of a wrapped method.
     * 
     * @return a method name.
     */
    public String getName() {
        return wrappedMethod.getName();
    }

    /**
     * Returns a mangled name of a wrapped method.
     * 
     * @return a mangled method name.
     */
    public String getMangledName() {
        String result = Mangler.mangleMethodName(getName());
        if (overloaded) {
            result = result + "__" + Mangler.mangleMethodName(getParamsSignature());
        }
        return result;
    }

    /**
     * Returns a JNI-style representation of the method return data type.
     * 
     * @return a return type JNI-style representation.
     */
    private String getJNIReturnType() {
        return ClazzMethod.getJNIType(getReturnType());
    }

    /**
     * Returns a string that represents a method part of
     * a JNI-style header file.
     */
    public String toString() {
        String n = System.getProperty("line.separator");
        StringBuffer result = new StringBuffer();

        String methodName = Mangler.mangleUnicode(
                clazz.getName() + "." + getName() + getSignature());

        // Print a method comment.
        result.append("/*" + n);
        result.append(" * Method: " + methodName + n);

        // Print the thrown exceptions.
        ExceptionTable etable = wrappedMethod.getExceptionTable();
        if (etable != null) {
            String e = etable.toString();
            if (e.length() > 0) {
                result.append(" * Throws: ");
                result.append(e);
                result.append(n);
            }
        }

        result.append(" */" + n);

        // Print a method declaration in a readable way.
        result.append("JNIEXPORT " + getJNIReturnType() + " JNICALL" + n);
        result.append("Java_" + clazz.getMangledName() + "_" + getMangledName());

        result.append('(');
        result.append("JNIEnv *");
        if (isStatic()) {
            result.append(", jclass");
        } else {
            result.append(", jobject");
        }
        Type types[] = getArgumentTypes();
        for (int i = 0; i < types.length; i++) {
            result.append(", ");
            if (i == 0) {
                result.append(n);
                result.append("    ");
            }
            result.append(ClazzMethod.getJNIType(types[i]));
        }
        result.append(");" + n);
        result.append(n);

        return result.toString();
    }

    /**
     * Returns an alternative string that represents a method part of
     * a JNI-style header file.
     */
    public String toAlternativeString() {
        String n = System.getProperty("line.separator");
        StringBuffer result = new StringBuffer();

        // Print a method comment.
        result.append("/*" + n);
        result.append(" * Class:     " + Mangler.mangleUnicode(clazz.getName())
                + n);
        result.append(" * Method:    " + Mangler.mangleUnicode(getName()) + n);
        result.append(" * Signature: " + getSignature() + n);

        // Print the thrown exceptions.
        ExceptionTable etable = wrappedMethod.getExceptionTable();
        if (etable != null) {
            String e = etable.toString();
            if (e.length() > 0) {
                result.append(" * Throws:    ");
                result.append(e);
                result.append(n);
            }
        }

        result.append(" */" + n);

        // Print a method declaration in a readable way.
        result.append("JNIEXPORT " + getJNIReturnType() + " JNICALL "
                + "Java_" + clazz.getMangledName() + "_" + getMangledName() + n);

        result.append("  (JNIEnv *, ");
        if (isStatic()) {
            result.append("jclass");
        } else {
            result.append("jobject");
        }
        Type types[] = getArgumentTypes();
        for (int i = 0; i < types.length; i++) {
            result.append(", ");
            result.append(ClazzMethod.getJNIType(types[i]));
        }
        result.append(");" + n);
        result.append(n);

        return result.toString();
    }

    /**
     * Returns a JNI-style representation of the given data type passed
     * as a Class object.
     * 
     * @param type - a Class object that wraps a data type.
     * @return a string that represents a JNI-style data type.
     */
    public static String getJNIType(Type type) {
        StringBuffer result = new StringBuffer();

        String suffix = "";
        if (type instanceof ArrayType) {
            suffix = "Array";
            type = ((ArrayType) type).getElementType();
        }

        if (type instanceof ObjectType) {
            String objectType = "jobject";
            // The suffix length is 0 only if the given type is not an array.
            if (suffix.length() == 0) {
                if (type.equals(Type.STRING)) {
                    objectType = "jstring";
                } else if (type.equals(Type.THROWABLE)) {
                    objectType = "jthrowable";
                } else if (((ObjectType) type).getClassName()
                        .equals("java.lang.Class")) {
                    objectType = "jclass";
                }
            }
            result.append(objectType);
        } else if (type == Type.INT) {
            result.append("jint");
        } else if (type == Type.BYTE) {
            result.append("jbyte");
        } else if (type == Type.LONG) {
            result.append("jlong");
        } else if (type == Type.FLOAT) {
            result.append("jfloat");
        } else if (type == Type.DOUBLE) {
            result.append("jdouble");
        } else if (type == Type.SHORT) {
            result.append("jshort");
        } else if (type == Type.CHAR) {
            result.append("jchar");
        } else if (type == Type.BOOLEAN) {
            result.append("jboolean");
        } else if (type == Type.VOID) {
            result.append("void");
        }

        return result.append(suffix).toString();
    }

}
