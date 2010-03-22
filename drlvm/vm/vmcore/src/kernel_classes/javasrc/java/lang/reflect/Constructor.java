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
public final class Constructor<T> extends AccessibleObject implements Member, GenericDeclaration {

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
        for (Annotation a : data.getDeclaredAnnotations()) {
            if (a.annotationType() == annotationClass) {
                return (A) a; // warning here, but it's known its type is A
            }
        }
        return null;
    }

    /**
    *  @com.intel.drl.spec_ref
    */
    public Type[] getGenericExceptionTypes() throws GenericSignatureFormatError, TypeNotPresentException, MalformedParameterizedTypeException {
        if (data.genericExceptionTypes == null) {
            data.genericExceptionTypes = Parser.getGenericExceptionTypes(this, VMGenericsAndAnnotations
                    .getSignature(data.vm_member_id));
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
    @SuppressWarnings("unchecked")
    public TypeVariable<Constructor<T>>[] getTypeParameters() throws GenericSignatureFormatError {
        if (data.typeParameters == null) {
            data.typeParameters = (TypeVariable<Constructor<T>>[])
                    Parser.getTypeParameters(this, VMGenericsAndAnnotations
                    .getSignature(data.vm_member_id));
        }
        return (TypeVariable<Constructor<T>>[]) data.typeParameters.clone();
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
            data.genericExceptionTypes = Parser.getGenericExceptionTypes(this, VMGenericsAndAnnotations
                    .getSignature(data.vm_member_id));
        }
        // append modifiers if any
        int modifier = getModifiers();
        if (modifier != 0) {
            sb.append(Modifier.toString(modifier & ~ACC_VARARGS)).append(' ');
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
        // append constructor name
        appendArrayType(sb, getDeclaringClass());
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
     * cache of the constructor data
     */
    private final ConstructorData data;

    /**
     * Copy constructor
     * 
     * @param c original constructor
     */
    Constructor(Constructor <T> c) {
        data = c.data;
        isAccessible = c.isAccessible;
    }

    /**
     * Only VM should call this constructor.
     * String parameters must be interned.
     * @api2vm
     */
    Constructor(long id, Class<T> clss, String name, String desc, int m) {
        data = new ConstructorData(id, clss, name, desc, m);
    }

    /**
     * Called by VM to obtain this constructor's handle.
     * 
     * @return handle for this constructor
     * @api2vm
     */
    long getId() {
        return data.vm_member_id;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean equals(Object obj) {
        if (obj instanceof Constructor) {
            Constructor another = (Constructor)obj;
            if (data.vm_member_id == another.data.vm_member_id){
                assert getDeclaringClass() == another.getDeclaringClass()
                && Arrays.equals(getParameterTypes(), another.getParameterTypes());
                return true;
            }
        }
        return false;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public Class<T> getDeclaringClass() {
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
        return data.getName();
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
    public int hashCode() {
        return getDeclaringClass().getName().hashCode();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    @SuppressWarnings("unchecked")
    public T newInstance(Object... args) throws InstantiationException,
        IllegalAccessException, IllegalArgumentException,
        InvocationTargetException {
        if (Modifier.isAbstract(getDeclaringClass().getModifiers())) {
            throw new InstantiationException("Can not instantiate abstract "
                + getDeclaringClass());
        }
        
        // check parameter validity
        checkInvokationArguments(data.getParameterTypes(), args);
        
        if (!isAccessible) {
            reflectExporter.checkMemberAccess(VMStack.getCallerClass(0),
                                              getDeclaringClass(),
                                              getDeclaringClass(),
                                              getModifiers());
        }
        return (T)VMReflection.newClassInstance(data.vm_member_id, args);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        // append modifiers if any
        int modifier = getModifiers();
        if (modifier != 0) {
            // VARARGS incorrectly recognized
            final int MASK = ~ACC_VARARGS;  
            sb.append(Modifier.toString(modifier & MASK)).append(' ');            
        }
        // append constructor name
        appendArrayType(sb, getDeclaringClass());
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
     * This method is used by serialization mechanism.
     * 
     * @return the signature of the constructor 
     */
    String getSignature() {
        return data.descriptor;
    }

    /**
     * Keeps an information about this constructor
     */
    private class ConstructorData {

        /**
         * constructor handle which is used to retrieve all necessary
         * information about this constructor object
         */
        final long vm_member_id;

        Annotation[] declaredAnnotations;

        final Class<T> declaringClass;

        Class<?>[] exceptionTypes;

        Type[] genericExceptionTypes;

        Type[] genericParameterTypes;

        final int modifiers;

        String name;

        Annotation[][] parameterAnnotations;

        Class<?>[] parameterTypes;

        TypeVariable<Constructor<T>>[] typeParameters;
        final String descriptor;

        /**
         * @param obj constructor handler 
         */
        public ConstructorData(long vm_id, Class<T> clss, String name, String desc, int mods) {
            vm_member_id = vm_id;
            declaringClass = clss;
            this.name = null;
            modifiers = mods;
            descriptor = desc;
        }
        
        String getName() {
            if (name == null) {
                name = declaringClass.getName();
            }
            return name;
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
        public Class[] getExceptionTypes() {
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
    }
}
