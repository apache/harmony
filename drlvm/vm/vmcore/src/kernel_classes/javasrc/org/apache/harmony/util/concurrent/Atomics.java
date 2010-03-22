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
/**
 * @author Artem A. Aliev, Andrey Y. Chernyshev, Sergey V. Dmitriev
 */
package org.apache.harmony.util.concurrent;

import java.lang.reflect.Field;

/**
 * Allows to atomically update the contents of fields for the specific object. The primary purpose
 * of this class is to provide the low-level atomic field access operations useful for
 * implementing the classes from java.util.concurrent.atomic package.
 *
 * @see java.util.concurrent.atomic
 */
public final class Atomics {

    private Atomics() {};

     /**
      * Returns offset of the first array's element 
      * @param arrayClass class of the array
      *
      * @return offset of the array's first element
      */
    public static native int arrayBaseOffset(Class arrayClass);

     /**
      * Returns size of the array's element
      * @param arrayClass class of the array
      *
      * @return size of the array's element
      */
    public static native int arrayIndexScale(Class arrayClass);

     /*
      * Writes new value to the object's field (by given offset) in volatile manner
      * @param obj object which field needs to be set
      * @param offset the field offset within the given object
      * @param value value to set
      */
    public static native void setIntVolatile(Object obj, long offset, int value);

     /*
      * Reads value from the object's field (by given offset) in volatile manner
      * @param obj object which field needs to be read
      * @param offset the field offset within the given object
      * @return the field's value
      */
    public static native int getIntVolatile(Object obj, long offset);

     /*
      * Writes new value to the object's field (by given offset) in volatile manner
      * @param obj object which field needs to be set
      * @param offset the field offset within the given object
      * @param value value to set
      */
    public static native void setLongVolatile(Object obj, long offset, long value);

     /*
      * Reads value from the object's field (by given offset) in volatile manner
      * @param obj object which field needs to be read
      * @param offset the field offset within the given object
      * @return the field's value
      */
    public static native long getLongVolatile(Object obj, long offset);

         /*
      * Writes new value to the object's field (by given offset) in volatile manner
      * @param obj object which field needs to be set
      * @param offset the field offset within the given object
      * @param value value to set
      */
    public static native void setObjectVolatile(Object obj, long offset, Object value);

     /*
      * Reads value from the object's field (by given offset) in volatile manner
      * @param obj object which field needs to be read
      * @param offset the field offset within the given object
      * @return the field's value
      */
    public static native Object getObjectVolatile(Object obj, long offset);

     /**
      * Returns offset of the given field.
      * @param field the field for which offset is returned
      *
      * @return offset of the given field
      */
    public static native long getFieldOffset(Field field);

    /**
     * Atomically sets an integer field to x if it currently contains the expected value.
     * @param o object those integer field needs to be set
     * @param field the field to be set
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetInt(Object o, long offset, int expected, int x);

    /**
     * Atomically sets a boolean field to x if it currently contains the expected value.
     * @param o object those boolean field needs to be set
     * @param field the field to be set
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetBoolean(Object o, long offset, boolean expected, boolean x);


    /**
     * Atomically sets a long field to x if it currently contains the expected value.
     * @param o object those long field needs to be set
     * @param field the field to be set
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetLong(Object o, long offset, long expected, long x);


    /**
     * Atomically sets a reference type field to x if it currently contains the expected value.
     * @param o object those reference type field needs to be set
     * @param field the field to be set
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetObject(Object o, long offset, Object expected, Object x);


    /**
     * Atomically sets an element within array of integers to x if it currently contains the expected value.
     * @param arr array those integer element needs to be set
     * @param index an index within an array
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetInt(int[] arr, int index, int expected, int x);


    /**
     * Atomically sets an element within array of booleans to x if it currently contains the expected value.
     * @param arr array those boolean element needs to be set
     * @param index an index within an array
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetBoolean(boolean[] arr, int index, boolean expected, boolean x);


    /**
     * Atomically sets an element within array of longs to x if it currently contains the expected value.
     * @param arr array those long element needs to be set
     * @param index an index within an array
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetLong(long[] arr, int index, long expected, long x);


    /**
     * Atomically sets an element within array of objects to x if it currently contains the expected value.
     * @param arr array those object element needs to be set
     * @param index an index within an array
     * @param expected expected field value
     * @param x value to set.
     *
     * @return true if the value was set.
     * False return indicates that the actual value was not equal to the expected value.
     */
    public static native boolean compareAndSetObject(Object[] arr, int index, Object expected, Object x);
}
