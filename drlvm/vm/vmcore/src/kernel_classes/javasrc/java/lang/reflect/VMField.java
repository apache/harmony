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
 * @author Alexey V. Varlamov 
 */
package java.lang.reflect;

final class VMField {
    
    /**
     * This class is not supposed to be instantiated.
     */
    private VMField() {
    }

    /**
     * Obtaines a value of the field with specified identifier. If the
     * <code>id</code> argument corresponds to a static field then the
     * <code>object</code> argument must be null. The value of a static field
     * will be returned in this case. If the <code>id</code> argument
     * corresponds to non-static field then object's field value will be
     * returned.
     * <p>
     * This method is used for the {@link Field#get(Object) Field.get(Object obj)}
     * method implementation.
     * <p>
     * 
     * @param object the object to get a field value from.
     * @param id an identifier of the caller class.
     * @return a value of the object. The values of primitive type are wrapped
     *         by corresponding object from the <code>java.lang</code> package.
     * @throws ExceptionInInitializerError if initialization fails.
     * @api2vm
     */
    static native Object getObject(Object object, long field_id);
    
    static native boolean getBoolean(Object object, long field_id);
    static native char getChar(Object object, long field_id);
    static native byte getByte(Object object, long field_id);
    static native short getShort(Object object, long field_id);
    static native int getInt(Object object, long field_id);
    static native long getLong(Object object, long field_id);
    static native float getFloat(Object object, long field_id);
    static native double getDouble(Object object, long field_id);
    

    /**
     * Sets a value for the field with specified identifier. If the
     * <code>id</code> argument corresponds to a static field then the
     * <code>object</code> argument must be null. An attempt to set a new value
     * to a static field will be made in this case. If the <code>id</code>
     * argument corresponds to a non-static field then an attempt to assign new 
     * value to object's field will be made.
     * <p>
     * This method is used for the {@link Field#set(Object, Object)
     * Field.set(Object obj, Object value)} method implementation.
     * <p>
     * 
     * @param object the object to set a field value in.
     * @param id an identifier of the caller class.
     * @param value a new field value. If the field has primitive type then the
     *        value argument should be unwrapped.
     * @throws ExceptionInInitializerError if initialization fails.
     * @api2vm
     */
    static native void setObject(Object object, long field_id, Object value);
    
    static native void setBoolean(Object object, long field_id, boolean value);
    static native void setChar(Object object, long field_id, char value);
    static native void setByte(Object object, long field_id, byte value);
    static native void setShort(Object object, long field_id, short value);
    static native void setInt(Object object, long field_id, int value);
    static native void setLong(Object object, long field_id, long value);
    static native void setFloat(Object object, long field_id, float value);
    static native void setDouble(Object object, long field_id, double value);
}
