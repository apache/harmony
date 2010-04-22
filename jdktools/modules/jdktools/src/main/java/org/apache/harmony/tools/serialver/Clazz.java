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

package org.apache.harmony.tools.serialver;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.harmony.tools.ClassProvider;

/**
 * This class is a wrapper of BCEL's Class class.
 *
 * This class depends on Apache Byte Code Engineering Library (BCEL) 5.0 or
 * later. Please see http://jakarta.apache.org/bcel for more information about
 * this library.
 */
public class Clazz {

    /**
     * A wrapped class.
     */
    private JavaClass wrappedClass;

    /**
     * Class fields that will be used to calculate the serialversionUID. All
     * fields except private static and private transient. (SORTED)
     */
    private Vector<Field> fields;

    /**
     * Class constructors that will be used to calculate the serialversionUID.
     * Default constructor (if exists) and all non-private constructors.
     * (SORTED)
     */
    private Vector<Method> constructors;

    /**
     * Class methods that will be used to calculate the serialversionUID. All
     * non-private methods. (SORTED)
     */
    private Vector<Method> methods;

    /**
     * Field that represents existance SUID (if-exists).
     */
    private Field suidField;

    /**
     * Constructs a <code>Clazz</code> object.
     *
     * @param classProvider
     *            - a helper that provides the class information from a classpath.
     * @param className
     *            - a fully qualified name of a class.
     * @throws ClassNotFoundException
     *             - if a give class is not found
     */
    public Clazz(ClassProvider classProvider, String className)
                    throws ClassNotFoundException {
        try {
            wrappedClass = classProvider.getJavaClass(className);

            // Collect up all valid fields (non transient and private, and non
            // static private) and sort it.
            fields = sortFields(collectValidFields(wrappedClass));

            // Collect all valid constructors (non-private) and sort it.
            constructors = sortConstructors(collectConstructors(wrappedClass));

            // Collect all valid methods (non-private) and sort it.
            methods = sortMethods(collectMethods(wrappedClass));
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException();
        }
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
     * Returns the {@link Modifiers} of the wrapped class.
     *
     * @return a constant int
     */
    public int getModifiers() {
        return wrappedClass.getModifiers();
    }

    /**
     * Get all interfaces names that the wrapped class implement and sort it.
     *
     * @return interfaces names implemented (sorted).
     */
    public String[] getSortedInterfaces() {
        String[] ifNames = wrappedClass.getInterfaceNames();
        Arrays.sort(ifNames);
        return ifNames;
    }

    /**
     * Get all valid Fields (i.e., all excluding private static and private
     * transient) of the wrapped class and sort it.
     *
     * @return a Field Vector (Sorted).
     */
    public Vector<Field> getSortedValidFields() {
        return fields;
    }

    /**
     * Get all valid Constructors (i.e., non-private) of the wrapped class and
     * sort it.
     *
     * @return a Method Vector (Sorted).
     */
    public Vector<Method> getSortedValidConstructors() {
        return constructors;
    }

    /**
     * Get all valid Methods (i.e., non-private) of the wrapped class and sort
     * it.
     *
     * @return a Method Vector (Sorted).
     */
    public Vector<Method> getSortedValidMethods() {
        return methods;
    }

    /**
     * Get the existing serialVersionUID of the wrapped class.
     *
     * @return a long that is the serialVersionUID of the wrapped class.
     * @throws NoSuchFieldException
     *             if the field doesn't exist.
     */
    public long getExistantSerialVersionUID() throws NoSuchFieldException {
        return Long.parseLong(suidField.getAttributes()[0].toString());
    }

    /**
     * Verify if the wrapped class is serializable.
     *
     * @return true - if it's serializable<br />
     *         false otherwise.
     * @throws ClassNotFoundException
     *             if the wrappedclass isn't found.
     */
    public boolean isSerializable() throws ClassNotFoundException {
        boolean serial = false;
        for (JavaClass clazzInterface : wrappedClass.getAllInterfaces()) {
            if (clazzInterface.getClassName().equals("java.io.Serializable"))
                serial = true;
        }
        return serial;
    }

    /**
     * Verify if a give class has a serialVersionUID Field.
     *
     * @return true - if it has this field<br />
     *         false otherwise.
     */
    public boolean hasSerialVersionUID() {
        return suidField != null;
    }

    /**
     * Collect valid fields of the wrapped class. Valid fields includes all
     * except private static and private transient.
     *
     * @param clazz
     *            - the wrapped class.
     * @return a {@link Vector} of valid fields.
     */
    private Vector<Field> collectValidFields(JavaClass clazz) {
        Field[] allFields = clazz.getFields();
        Vector<Field> validFields = new Vector<Field>();
        suidField = null;

        int PRIVATE_STATIC = Modifier.PRIVATE | Modifier.STATIC;
        int PRIVATE_TRANSIENT = Modifier.PRIVATE | Modifier.TRANSIENT;

        // Excluding all PRIVATE_STATIC and PRIVATE_TRANSIENT fields
        for (int i = 0; i < allFields.length; i++) {
            if ((allFields[i].getModifiers() != PRIVATE_STATIC)
                && (allFields[i].getModifiers() != PRIVATE_TRANSIENT)) {
                // Adding
                validFields.add(allFields[i]);

                // Let's check if the Field is a existing SUID
                if (allFields[i].getName().equals("serialVersionUID")) {
                    suidField = allFields[i];
                }
            }
        }
        return validFields;
    }

    /**
     * Collect valid constructors of the wrapped class. Valid constructors
     * includes all except private.
     *
     * @param clazz
     *            - the wrapped class.
     * @return a {@link Vector} of valid constructors.
     */
    private Vector<Method> collectConstructors(JavaClass clazz) {
        Method methods[] = clazz.getMethods();
        Vector<Method> validConstructors = new Vector<Method>();

        // Excludes all constructors that are private
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String mName = m.getName();

            if (mName.equals("<clinit>") || mName.equals("<init>"))
                if (!m.isPrivate())
                    validConstructors.add(m);
        }

        return validConstructors;
    }

    /**
     * Collect valid methods of the wrapped class. Valid methods includes all
     * except private.
     *
     * @param clazz
     *            - the wrapped class.
     * @return a {@link Vector} of valid methods.
     */
    private Vector<Method> collectMethods(JavaClass clazz) {
        Method methods[] = clazz.getMethods();
        Vector<Method> validMethods = new Vector<Method>();

        // Excludes all methods that are private
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String mName = m.getName();

            // is not a constructor
            if (!mName.equals("<clinit>") && !mName.equals("<init>")) {
                if (!m.isPrivate()) {
                    validMethods.add(m);
                }
            }
        }

        return validMethods;
    }

    /**
     * Sort the valid fields of the wrapped class.
     *
     * @param vector
     *            - to be sorted.
     * @return a Field Vector sorted.
     */
    private Vector<Field> sortFields(Vector<Field> vector) {
        Collections.sort(vector, new Comparator<Field>() {
            public int compare(Field f1, Field f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        return vector;
    }

    /**
     * Sort the valid constructors of the wrapped class.
     *
     * @param vector
     *            - to be sorted.
     * @return a Method Vector sorted.
     */
    private Vector<Method> sortConstructors(Vector<Method> vector) {
        Collections.sort(vector, new Comparator<Method>() {
            public int compare(Method m1, Method m2) {
                if (m1.getName() == "<clinit>")
                    return 1;

                return m1.getSignature().compareTo(m2.getSignature());
            }
        });

        return vector;
    }

    /**
     * Sort the valid methods of the wrapped class.
     *
     * @param vector
     *            - to be sorted.
     * @return a Method Vector sorted.
     */
    private Vector<Method> sortMethods(Vector<Method> vector) {
        Collections.sort(vector, new Comparator<Method>() {
            public int compare(Method m1, Method m2) {
                int name = m1.getName().compareTo(m2.getName());
                if (name != 0) {
                    return name;
                } else {
                    return m1.getSignature().compareTo(m2.getSignature());
                }
            }
        });

        return vector;
    }
}
