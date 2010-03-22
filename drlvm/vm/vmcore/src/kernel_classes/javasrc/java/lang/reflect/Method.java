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

import static org.apache.harmony.vm.ClassFormat.ACC_BRIDGE;
import static org.apache.harmony.vm.ClassFormat.ACC_SYNTHETIC;
import static org.apache.harmony.vm.ClassFormat.ACC_VARARGS;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.apache.harmony.lang.reflect.parser.Parser;
import org.apache.harmony.vm.VMGenericsAndAnnotations;
import org.apache.harmony.vm.VMStack;

/**
* @com.intel.drl.spec_ref 
*/
public final class Method extends AccessibleObject implements Member, GenericDeclaration {

    /**
    *  @com.intel.drl.spec_ref
    */
    public boolean isBridge() {
        return (getModifiers() & ACC_BRIDGE) != 0;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public boolean isVarArgs() {
        return (getModifiers() & ACC_VARARGS) != 0;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Annotation[][] getParameterAnnotations() {
        Annotation a[][] = data.getParameterAnnotations();
        Annotation aa[][] = new Annotation[a.length][]; 
        for (int i = 0; i < a.length; i++ ) {
            aa[i] = new Annotation[a[i].length];
            System.arraycopy(a[i], 0, aa[i], 0, a[i].length);
        }
        return aa;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Annotation[] getDeclaredAnnotations() {
        Annotation a[] = data.getDeclaredAnnotations();
        Annotation aa[] = new Annotation[a.length];
        System.arraycopy(a, 0, aa, 0, a.length);
        return aa;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        if(annotationClass == null) {
            throw new NullPointerException();
        }
        for (Annotation aa : data.getDeclaredAnnotations()) {
            if(aa.annotationType() == annotationClass) {

                return (A) aa;
            }
        }
        return null;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Type[] getGenericExceptionTypes() throws GenericSignatureFormatError, TypeNotPresentException, MalformedParameterizedTypeException {
        if (data.genericExceptionTypes == null) {
            data.genericExceptionTypes = Parser.getGenericExceptionTypes(this, VMGenericsAndAnnotations.getSignature(data.vm_member_id));
        }
        return (Type[])data.genericExceptionTypes.clone();
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Type[] getGenericParameterTypes() throws GenericSignatureFormatError, TypeNotPresentException, MalformedParameterizedTypeException {
        if (data.genericParameterTypes == null) {
            data.genericParameterTypes = Parser.getGenericParameterTypes(this, VMGenericsAndAnnotations.getSignature(data.vm_member_id));
        }

        return (Type[])data.genericParameterTypes.clone();
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Type getGenericReturnType() throws GenericSignatureFormatError, TypeNotPresentException, MalformedParameterizedTypeException {
        if (data.genericReturnType == null) {
            data.genericReturnType = Parser.getGenericReturnTypeImpl(this, VMGenericsAndAnnotations
                    .getSignature(data.vm_member_id));            
        }
        return data.genericReturnType;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    @SuppressWarnings("unchecked")
    public TypeVariable<Method>[] getTypeParameters() throws GenericSignatureFormatError {
        if (data.typeParameters == null) {
            data.typeParameters = Parser.getTypeParameters(this,
                    VMGenericsAndAnnotations.getSignature(data.vm_member_id));
        }
        return (TypeVariable<Method>[]) data.typeParameters.clone();
    }

    /**
    * @com.intel.drl.spec_ref 
    */
    public String toGenericString() {
        StringBuilder sb = new StringBuilder(80);
        // data initialization
        if (data.genericParameterTypes == null) {
            data.genericParameterTypes = Parser.getGenericParameterTypes(this, VMGenericsAndAnnotations.getSignature(data.vm_member_id));
        }
        if (data.genericExceptionTypes == null) {
            data.genericExceptionTypes = Parser.getGenericExceptionTypes(this, VMGenericsAndAnnotations.getSignature(data.vm_member_id));
        }
        // append modifiers if any
        int modifier = getModifiers();
        if (modifier != 0) {
            sb.append(Modifier.toString(modifier & ~(ACC_BRIDGE + ACC_VARARGS))).append(' ');
        }
        // append type parameters
        if (data.typeParameters != null && data.typeParameters.length > 0) {
            sb.append('<');
            for (int i = 0; i < data.typeParameters.length; i++) {
                appendGenericType(sb, data.typeParameters[i]);
                if (i < data.typeParameters.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("> ");
        }
        // append return type
        appendGenericType(sb, getGenericReturnType());
        sb.append(' ');
        // append method name
        appendArrayType(sb, getDeclaringClass());
        sb.append("."+getName());
        // append parameters
        sb.append('(');
        appendArrayGenericType(sb, data.genericParameterTypes);
        sb.append(')');
        // append exeptions if any
        if (data.genericExceptionTypes.length > 0) {
            sb.append(" throws ");
            appendArrayGenericType(sb, data.genericExceptionTypes);
        }
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
    public Object getDefaultValue() {
        return VMGenericsAndAnnotations.getDefaultValue(data.vm_member_id);
    }   

    /**
     * cache of the method data
     */
    private final MethodData data;

    /**
     * Copy constructor
     * 
     * @param m original method
     */
    Method(Method m) {
        data = m.data;
        isAccessible = m.isAccessible;
    }
    
    /**
     * Only VM should call this constructor.
     * String parameters must be interned.
     * @api2vm
     */
    Method(long id, Class clss, String name, String desc, int m) {
        data = new MethodData(id, clss, name, desc, m);
    }

    /**
     * Called by VM to obtain this method's handle.
     * 
     * @return handle for this method
     * @api2vm
     */
    long getId() {
        return data.vm_member_id;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean equals(Object obj) {
        if (obj instanceof Method) {
            Method another = (Method)obj;
            if (data.vm_member_id == another.data.vm_member_id){
                assert getDeclaringClass() == another.getDeclaringClass()
                && getName() == another.getName()
                && getReturnType() == another.getReturnType()
                && Arrays.equals(getParameterTypes(), another.getParameterTypes());
                return true;
            }
        }
        return false;
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
    public Class<?>[] getExceptionTypes() {
        return (Class[])data.getExceptionTypes().clone();
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
    public Class<?>[] getParameterTypes() {
        return (Class[])data.getParameterTypes().clone();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public Class<?> getReturnType() {
        return data.getReturnType();
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
    public Object invoke(Object obj, Object... args)
        throws IllegalAccessException, IllegalArgumentException,
        InvocationTargetException {
    	
        obj = checkObject(getDeclaringClass(), getModifiers(), obj);
        
        // check parameter validity
        checkInvokationArguments(data.getParameterTypes(), args);
        
        if (!isAccessible) {
            reflectExporter.checkMemberAccess(
                VMStack.getCallerClass(0), getDeclaringClass(),
                obj == null ? getDeclaringClass() : obj.getClass(),
                getModifiers()
            );
        }
        return VMReflection.invokeMethod(data.vm_member_id, obj, args);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // append modifiers if any
        int modifier = getModifiers();
        if (modifier != 0) {
            // BRIDGE & VARARGS recognized incorrectly
            final int MASK = ~(ACC_BRIDGE + ACC_VARARGS);
            sb.append(Modifier.toString(modifier & MASK)).append(' ');            
        }
        // append return type
        appendArrayType(sb, getReturnType());
        sb.append(' ');
        // append full method name
        sb.append(getDeclaringClass().getName()).append('.').append(getName());
        // append parameters
        sb.append('(');
        appendArrayType(sb, data.getParameterTypes());
        sb.append(')');
        // append exeptions if any
        Class[] exn = data.getExceptionTypes(); 
        if (exn.length > 0) {
            sb.append(" throws ");
            appendSimpleType(sb, exn);
        }
        return sb.toString();
    }

    /* NON API SECTION */

    /**
     * This method is required by serialization mechanism.
     * 
     * @return the signature of the method 
     */
    String getSignature() {
        return data.descriptor;
    }

    /**
     * Keeps an information about this method
     */
    private class MethodData {

        /**
         * method handle which is used to retrieve all necessary information
         * about this method object
         */
        final long vm_member_id;

        Annotation[] declaredAnnotations;

        final Class<?> declaringClass;

        private Class<?>[] exceptionTypes;

        Type[] genericExceptionTypes;

        Type[] genericParameterTypes;

        Type genericReturnType;

        String methSignature;

        final int modifiers;

        final String name;
        
        final String descriptor;

        /**
         * declared method annotations
         */
        Annotation[][] parameterAnnotations;

        /**
         * method parameters
         */
        Class<?>[] parameterTypes;

        /** 
         * method return type
         */
        private Class<?> returnType;

        /**
         * method type parameters
         */
        TypeVariable<Method>[] typeParameters;

        /**
         * @param obj method handler
         */
        public MethodData(long vm_id, Class clss, String name, String desc, int mods) {
            vm_member_id = vm_id;
            declaringClass = clss;
            this.name = name;
            modifiers = mods;
            descriptor = desc;
        }
        
        public Annotation[] getDeclaredAnnotations() {
            if (declaredAnnotations == null) {
                declaredAnnotations = VMGenericsAndAnnotations
                    .getDeclaredAnnotations(vm_member_id);
            }
            return declaredAnnotations;
        }
        
        /**
         * initializes exeptions
         */
        public Class<?>[] getExceptionTypes() {
            if (exceptionTypes == null) {
                exceptionTypes = VMReflection.getExceptionTypes(vm_member_id);
            }
            return exceptionTypes;
        }

        public Annotation[][] getParameterAnnotations() {
            if (parameterAnnotations == null) {
                parameterAnnotations = VMGenericsAndAnnotations
                    .getParameterAnnotations(vm_member_id);
            }
            return parameterAnnotations;
        }

        /**
         * initializes parameters
         */
        public Class[] getParameterTypes() {
            if (parameterTypes == null) {
                parameterTypes = VMReflection.getParameterTypes(vm_member_id);
            }
            return parameterTypes;
        }

        /**
         * initializes return type
         */
        public Class<?> getReturnType() {
            if (returnType == null) {
                returnType = VMReflection.getMethodReturnType(vm_member_id);
            }
            return returnType;
        }
    }
}
