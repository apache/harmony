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
 * @author Evgueni Brevnov, Serguei S. Zapreyev, Alexey V. Varlamov
 */

package java.lang.reflect;

import static org.apache.harmony.vm.ClassFormat.ACC_ENUM;
import static org.apache.harmony.vm.ClassFormat.ACC_SYNTHETIC;

import java.lang.annotation.Annotation;

import org.apache.harmony.lang.reflect.parser.Parser;
import org.apache.harmony.vm.VMGenericsAndAnnotations;
import org.apache.harmony.vm.VMStack;

/**
* @com.intel.drl.spec_ref 
*/
public final class Field extends AccessibleObject implements Member {

    /**
    *  @com.intel.drl.spec_ref
    */
    public Annotation[] getDeclaredAnnotations() {
        Annotation a[] = data.getAnnotations();
        Annotation aa[] = new Annotation[a.length];
        System.arraycopy(a, 0, aa, 0, a.length);
        return aa;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if(annotationClass == null) {
            throw new NullPointerException();
        }
        for (Annotation aa : data.getAnnotations()) {
            if(aa.annotationType() == annotationClass) {
                return (T) aa;
            }
        }
        return null;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Type getGenericType() throws GenericSignatureFormatError,
            TypeNotPresentException, MalformedParameterizedTypeException {
        if (data.genericType == null) {
            data.genericType = Parser.parseFieldGenericType(this, VMGenericsAndAnnotations
                    .getSignature(data.vm_member_id));
        }
        return data.genericType;
    }

    /**
    * @com.intel.drl.spec_ref 
    */
    public String toGenericString() {
        StringBuilder sb = new StringBuilder(80);
        // append modifiers if any
        int modifier = getModifiers();
        if (modifier != 0) {
            sb.append(Modifier.toString(modifier)).append(' ');
        }
        // append generic type
        appendGenericType(sb, getGenericType());
        sb.append(' ');
        // append full field name
        sb.append(getDeclaringClass().getName()).append('.').append(getName());
        return sb.toString();
    }

    /**
    * @com.intel.drl.spec_ref 
    */
    public boolean isSynthetic() {
        return (getModifiers() & ACC_SYNTHETIC) != 0;
    }

    /**
    * @com.intel.drl.spec_ref 
    */
    public boolean isEnumConstant() {
        return (getModifiers() & ACC_ENUM) != 0;
    }

    /**
     * cache of the field data
     */
    private final FieldData data;

    /**
     * Copy constructor
     * 
     * @param f original field
     */
    Field(Field f) {
        data = f.data;
        isAccessible = f.isAccessible;
    }

    /**
     * Only VM should call this constructor.
     * String parameters must be interned.
     * @api2vm
     */
    Field(long id, Class clss, String name, String desc, int m) {
        data = new FieldData(id, clss, name, desc, m);
    }
    
    /**
     * Called by VM to obtain this field's handle.
     * 
     * @return handle for this field
     * @api2vm
     */
    long getId() {
        return data.vm_member_id;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean equals(Object obj) {
        if (obj instanceof Field) {
            Field that = (Field)obj;
            if (data.vm_member_id == that.data.vm_member_id) {
                assert getDeclaringClass() == that.getDeclaringClass()
                    && getName() == that.getName();
                return true;
            }
        }
        return false;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public Object get(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getObject(obj, data.vm_member_id);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean getBoolean(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getBoolean(obj, data.vm_member_id);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public byte getByte(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getByte(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public char getChar(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getChar(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public Class<?> getDeclaringClass() {
        return data.declaringClass;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public double getDouble(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getDouble(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public float getFloat(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getFloat(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public int getInt(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getInt(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public long getLong(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getLong(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public int getModifiers() {
        return data.modifiers;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getName() {
        return data.name;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public short getShort(Object obj) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkGet(VMStack.getCallerClass(0), obj);
        return VMField.getShort(obj, data.vm_member_id);    
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public Class<?> getType() {
        return data.getType();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void set(Object obj, Object value) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setObject(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setBoolean(Object obj, boolean value)
        throws IllegalArgumentException, IllegalAccessException {        
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setBoolean(obj, data.vm_member_id, value);
    }
    
    /**
     * @com.intel.drl.spec_ref 
     */
    public void setByte(Object obj, byte value)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setByte(obj, data.vm_member_id, value);
    }
    
    /**
     * @com.intel.drl.spec_ref 
     */
    public void setChar(Object obj, char value)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setChar(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setDouble(Object obj, double value)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setDouble(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setFloat(Object obj, float value)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setFloat(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setInt(Object obj, int value) throws IllegalArgumentException,
        IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setInt(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setLong(Object obj, long value)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setLong(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setShort(Object obj, short value)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkSet(VMStack.getCallerClass(0), obj);
        VMField.setShort(obj, data.vm_member_id, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        // append modifiers if any
        int modifier = getModifiers();
        if (modifier != 0) {
            sb.append(Modifier.toString(modifier)).append(' ');
        }
        // append return type
        appendArrayType(sb, getType());
        sb.append(' ');
        // append full field name
        sb.append(getDeclaringClass().getName()).append('.').append(getName());
        return sb.toString();
    }

    /* NON API SECTION */

    /**
     * Checks that the specified obj is valid object for a getXXX operation.
     * 
     * @param callerClass caller class of a getXXX method
     * @param obj object to check
     * @return null if this field is static, otherwise obj one
     * @throws IllegalArgumentException if obj argument is not valid
     * @throws IllegalAccessException if caller doesn't have access permission
     */
    private Object checkGet(Class callerClass, Object obj)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkObject(getDeclaringClass(), getModifiers(), obj);
        if (!isAccessible) {
            reflectExporter.checkMemberAccess(callerClass, getDeclaringClass(),
                obj == null ? getDeclaringClass() : obj.getClass(), getModifiers());
        }
        return obj;
    }
    
    /**
     * Checks that the specified obj is valid object for a setXXX operation.
     * 
     * @param callerClass caller class of a setXXX method
     * @param obj object to check
     * @return null if this field is static, otherwise obj
     * @throws IllegalArgumentException if obj argument is not valid one
     * @throws IllegalAccessException if caller doesn't have access permission
     *         or this field is final
     */
    private Object checkSet(Class callerClass, Object obj)
        throws IllegalArgumentException, IllegalAccessException {
        obj = checkObject(getDeclaringClass(), getModifiers(), obj);
        if (Modifier.isFinal(getModifiers()) && !(isAccessible && obj != null)) {
            throw new IllegalAccessException(
                "Can not assign new value to the field with final modifier");
        }
        if (!isAccessible) {
            reflectExporter.checkMemberAccess(callerClass, getDeclaringClass(),
                obj == null ? getDeclaringClass() : obj.getClass(), getModifiers());
        }
        return obj;
    }

    /**
     * This method is used by serialization mechanism.
     * 
     * @return the signature of the field 
     */
    String getSignature() {
        return data.descriptor;
    }

    /**
     * Keeps an information about this field
     */
    private static class FieldData {
        
        final String name;
        final Class declaringClass;
        final int modifiers;
        private Class<?> type;
        private Annotation[] declaredAnnotations;
        Type genericType;
        final String descriptor;

        /**
         * field handle which is used to retrieve all necessary information
         * about this field object
         */
        final long vm_member_id;

        FieldData(long vm_id, Class clss, String name, String desc, int mods) {
            vm_member_id = vm_id;
            declaringClass = clss;
            this.name = name;
            modifiers = mods;
            descriptor = desc;
        }
        
        Annotation[] getAnnotations() {
            if (declaredAnnotations == null) {
                declaredAnnotations = VMGenericsAndAnnotations
                .getDeclaredAnnotations(vm_member_id);
            }
            return declaredAnnotations;
        }
        
        Class<?> getType() {
            if (type == null) {
                type = VMReflection.getFieldType(vm_member_id);
            }
            return type;
        }
    }

}
