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

import java.util.Enumeration;
import java.util.Vector;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;
import org.apache.harmony.tools.ClassProvider;
import org.apache.harmony.tools.Mangler;

/**
 * This class is a wrapper of BCEL's Class class.
 *
 * This class depends on Apache Byte Code Engineering Library (BCEL) 5.0 or
 * later. Please see http://jakarta.apache.org/bcel for more information
 * about this library.
 */
public class Clazz {

    /**
     * A wrapped class.
     */
    private JavaClass wrappedClass;

    /**
     * Inner classes of a wrapped class.
     */
    private String innerClassNames[];

    /**
     * Fields that will be included in a header file.
     */
    private Vector fields;

    /**
     * Methods that will be included in a header file.
     */
    private Vector methods;

    /**
     * Constructs a <code>Clazz</code> object.
     * 
     * @param classProvider - a helper that provides the class information.
     * @param className - a fully qualified name of a class.
     */
    public Clazz(ClassProvider classProvider, String className) 
            throws ClassNotFoundException {
        wrappedClass = classProvider.getJavaClass(className);

        // Assign an empty array by default.
        Vector foundInners = new Vector();
        // Get the class attributes.
        Attribute attrs[] = wrappedClass.getAttributes();
        for (int i = 0; i < attrs.length; i++) {
            // Find the InnerClasses attribute, if any.
            if (attrs[i] instanceof InnerClasses) {
                // The InnerClasses attribute is found.
                InnerClasses innerAttr = (InnerClasses) attrs[i];

                // Get an array of the inner classes.
                InnerClass inners[] = innerAttr.getInnerClasses();
                for (int j = 0; j < inners.length; j++) {

                    // Get the inner class name from a constant pool.
                    String innerClassName = Utility.compactClassName(
                            innerAttr.getConstantPool().getConstantString(
                                    inners[j].getInnerClassIndex(), 
                                    Constants.CONSTANT_Class),
                            false);

                    // The inner class has the InnerClasses attribute as well
                    // as its outer class. So, we should ignore such an inner 
                    // class.
                    if (!innerClassName.equals(className)) {
                        foundInners.addElement(innerClassName);
                    }
                }
                break;
            }
        }
        // Fill in the inner class array with the found inner classes.
        innerClassNames = new String[foundInners.size()];
        foundInners.toArray(innerClassNames);

        // Collect up fields of the given class and all its ancestors.
        fields = new Vector();
        JavaClass clss = wrappedClass;
        while (true) {
            searchForFields(clss);
            String superClassName = clss.getSuperclassName();
            if (clss.getClassName().equals(superClassName)) {
                break;
            }
            // Retrieve the next super class.
            clss = classProvider.getJavaClass(superClassName);
        }

        // Collect up methods.
        methods = new Vector();
        searchForMethods(wrappedClass);
    }

    /**
     * Returns a name of a wrapped class.
     * 
     * @return a class name.
     */
    public String getName() {
        return wrappedClass.getClassName();
    }

    /**
     * Returns a mangled name of a wrapped method.
     * 
     * @return a mangled method name.
     */
    public String getMangledName() {
        return Mangler.mangleClassName(getName());
    }

    /**
     * Returns an array of inner classes of this class.
     * 
     * @return an array of inner class names.
     */
    public String[] getInnerClassNames() {
        return innerClassNames;
    }

    /**
     * Searches the given class for the static final fields type of which
     * is a primitive data type.
     * 
     * @param clss - a searched class.
     */
    private void searchForFields(JavaClass clss) {
        Field field[] = clss.getFields();
        for (int i = 0; i < field.length; i++) {
            Field f = field[i];
            if (f.isStatic() && f.isFinal()) {
                if (Type.getReturnType(f.getSignature()) instanceof BasicType) {
                    fields.addElement(new ClazzField(this, f));
                }
            }
        }
    }

    /**
     * Searches the given class for the native methods.
     * 
     * @param clss - a searched class.
     */
    private void searchForMethods(JavaClass clss) {
        Method method[] = clss.getMethods();
        for (int i = 0; i < method.length; i++) {
            Method m = method[i];
            if (m.isNative()) {
                boolean overloaded = false;
                // Check if the current method is overloaded.
                for (int j = 0; j < method.length; j++) {
                    Method mj = method[j];
                    if (mj != m 
                            && mj.isNative() 
                            && mj.getName().equals(m.getName())) {
                        overloaded = true;
                    }
                }
                methods.addElement(new ClazzMethod(this, m, overloaded));
            }
        }
    }

    /**
     * Returns a string that represents a class part of
     * a JNI-style header file.
     */
    public String toString() {
        String n = System.getProperty("line.separator");
        StringBuffer result = new StringBuffer();

        if (fields.size() > 0) {
            result.append(n);
            result.append("/* Static final fields */" + n);
            result.append(n);

            for (Enumeration e = fields.elements(); e.hasMoreElements();) {
                result.append((ClazzField) e.nextElement());
            }
        }

        if (methods.size() > 0) {
            result.append(n);
            result.append("/* Native methods */" + n);
            result.append(n);

            for (Enumeration e = methods.elements(); e.hasMoreElements();) {
                result.append(((ClazzMethod) e.nextElement()).toAlternativeString());
            }
        }

        return result.toString();
    }
}
