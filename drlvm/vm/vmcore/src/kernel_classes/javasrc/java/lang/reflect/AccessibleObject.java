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
 * @author Evgueni Brevnov, Serguei S. Zapreyev
 */

package java.lang.reflect;

import java.lang.annotation.Annotation;

import org.apache.harmony.lang.reflect.ReflectPermissionCollection;
import org.apache.harmony.lang.reflect.Reflection;

/**
 * @com.intel.drl.spec_ref 
 */
public class AccessibleObject implements AnnotatedElement {
    /**
    *
    *  @com.intel.drl.spec_ref
    * 
    **/
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) == null ? false : true; // it seems to be correct for Method, Constructor, Field type object
    }

    /**
    *
    *  @com.intel.drl.spec_ref
    * 
    **/
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations(); // it seems to be correct for Method, Constructor, Field type object
    }

    /**
    *
    *  @com.intel.drl.spec_ref
    * 
    **/
    public Annotation[] getDeclaredAnnotations() {
        return (Annotation[]) null; // because of the method is overwritten in the Method, Constructor, Field classes
    }

    /**
    *
    *  @com.intel.drl.spec_ref
    * 
    **/
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return (T) null; // because of the method is overwritten in the Method, Constructor, Field classes
    }

    /**
     * one dimensional array
     */
    private static final String DIMENSION_1 = "[]";
    
    /**
     * two dimensional array
     */
    private static final String DIMENSION_2 = "[][]";

    /**
     * three dimensional array
     */
    private static final String DIMENSION_3 = "[][][]";
    
    /**
     * used to exchange information with the java.lang package
     */
    static final ReflectExporter reflectExporter;
        
    /**
     * indicates whether this object is accessible or not
     */
    boolean isAccessible = false;

    static {
        reflectExporter = new ReflectExporter();
        Reflection.setReflectAccessor(reflectExporter);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    protected AccessibleObject() {
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setAccessible(AccessibleObject[] objs, boolean flag)
        throws SecurityException {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
            sc.checkPermission(ReflectPermissionCollection.SUPPRESS_ACCESS_CHECKS_PERMISSION);
        }
        for (int i = 0; i < objs.length; i++) {
            objs[i].setAccessible0(flag);
        }
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean isAccessible() {
        return isAccessible;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public void setAccessible(boolean flag) throws SecurityException {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
            sc.checkPermission(ReflectPermissionCollection.SUPPRESS_ACCESS_CHECKS_PERMISSION);
        }
        setAccessible0(flag);
    }

    /*
     * NON API SECTION
     */

    /**
     * Checks obj argument for correctness.
     * 
     * @param declaringClass declaring class of the field/method member 
     * @param memberModifier field/method member modifier
     * @param obj object to check for correctness
     * @return null if accessing static member, otherwise obj 
     * @throws IllegalArgumentException if obj is not valid object
     * @throws NullPointerException if obj or declaringClass argument is null
     */
    Object checkObject(Class declaringClass, int memberModifier, Object obj)
        throws IllegalArgumentException {
        if (Modifier.isStatic(memberModifier)) {
            return null;
        } else if (!declaringClass.isInstance(obj)) {
            if (obj == null) {
                throw new NullPointerException(
                    "The specified object is null but the method is not static");
            }
            throw new IllegalArgumentException(
                "The specified object should be an instance of " + declaringClass);
        }
        return obj;
    }

    /**
     * Appends the specified class name to the buffer. The class may represent
     * a simple type, a reference type or an array type.
     * 
     * @param sb buffer
     * @param obj the class which name should be appended to the buffer
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendArrayType(StringBuilder sb, Class<?> obj) {
        if (!obj.isArray()) {
            sb.append(obj.getName());
            return;
        }
        int dimensions = 1;
        Class simplified = obj.getComponentType();
        obj = simplified;
        while (simplified.isArray()) {
            obj = simplified;
            dimensions++;
        }
        sb.append(obj.getName());
        switch (dimensions) {
        case 1:
            sb.append(DIMENSION_1);
            break;
        case 2:
            sb.append(DIMENSION_2);
            break;
        case 3:
            sb.append(DIMENSION_3);
            break;
        default:
            for (; dimensions > 0; dimensions--) {
                sb.append(DIMENSION_1);
            }
        }
    }

    /**
     * Appends names of the specified array classes to the buffer. The array
     * elements may represent a simple type, a reference type or an array type.
     * Output format: java.lang.Object[], java.io.File, void
     * 
     * @param sb buffer
     * @param objs array of classes to print the names
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendArrayType(StringBuilder sb, Class[] objs) {
        if (objs.length > 0) {
            appendArrayType(sb, objs[0]);
            for (int i = 1; i < objs.length; i++) {
                sb.append(',');
                appendArrayType(sb, objs[i]);
            }
        }
    }

    /**
     * Appends names of the specified array classes to the buffer. The array
     * elements may represent a simple type, a reference type or an array type.
     * Output format: java.lang.Object[], java.io.File, void
     * 
     * @param sb buffer
     * @param objs array of classes to print the names
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendArrayGenericType(StringBuilder sb, Type[] objs) {
        if (objs.length > 0) {
            appendGenericType(sb, objs[0]);
            for (int i = 1; i < objs.length; i++) {
                sb.append(',');
                appendGenericType(sb, objs[i]);
            }
        }
    }

    /**
     * Appends the generic type representation to the buffer.
     * 
     * @param sb buffer
     * @param obj the generic type which representation should be appended to the buffer
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendGenericType(StringBuilder sb, Type obj) {
        if (obj instanceof TypeVariable) {
            sb.append(((TypeVariable)obj).getName());
		} else if (obj instanceof ParameterizedType) {
            sb.append(obj.toString());
		} else if (obj instanceof GenericArrayType) { //XXX: is it a working branch?
			Type simplified = ((GenericArrayType)obj).getGenericComponentType();
            appendGenericType(sb, simplified);
            sb.append("[]");
		} else if (obj instanceof Class) {
			Class c = ((Class<?>)obj);
			if (c.isArray()){
				String as[] = c.getName().split("\\[");
				int len = as.length-1;
			    if (as[len].length() > 1){
				    sb.append(as[len].substring(1, as[len].length()-1));
			    } else {
					char ch = as[len].charAt(0);
				    if (ch == 'I')
				        sb.append("int");
				    else if (ch == 'B')
				        sb.append("byte");
				    else if (ch == 'J')
				        sb.append("long");
				    else if (ch == 'F')
				        sb.append("float");
				    else if (ch == 'D')
				        sb.append("double");
				    else if (ch == 'S')
				        sb.append("short");
				    else if (ch == 'C')
				        sb.append("char");
				    else if (ch == 'Z')
				        sb.append("boolean");
					else if (ch == 'V') //XXX: is it a working branch?
				        sb.append("void");
			    }
				for (int i = 0; i < len; i++){
					sb.append("[]");
				}
			} else {
				sb.append(c.getName());
			}
        }
    }

    /**
     * Appends names of the specified array classes to the buffer. The array
     * elements may represent a simple type, a reference type or an array type.
     * In case if the specified array element represents an array type its
     * internal will be appended to the buffer.   
     * Output format: [Ljava.lang.Object;, java.io.File, void
     * 
     * @param sb buffer
     * @param objs array of classes to print the names
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendSimpleType(StringBuilder sb, Class<?>[] objs) {
        if (objs.length > 0) {
            sb.append(objs[0].getName());
            for (int i = 1; i < objs.length; i++) {
                sb.append(',');
                sb.append(objs[i].getName());
            }
        }
    }

    /**
     * Changes accessibility to the specified.
     * 
     * @param flag accessible flag
     * @throws SecurityException if this object represents a constructor of the
     *         Class
     */
    private void setAccessible0(boolean flag) throws SecurityException {
        if (flag && this instanceof Constructor
            && ((Constructor<?>)this).getDeclaringClass() == Class.class) {
            throw new SecurityException(
                "Can not make the java.lang.Class class constructor accessible");
        }
        isAccessible = flag;
    }

    /**
     * Ensures that actual parameters are compartible with types of
     * formal parameters. For reference types, argument can be either 
     * null or assignment-compatible with formal type. 
     * For primitive types, argument must be non-null wrapper instance. 
     * @param types formal parameter' types
     * @param args runtime arguments
     * @throws IllegalArgumentException if arguments are incompartible
     */
    static void checkInvokationArguments(Class<?>[] types, Object[] args) {
        if ((args == null) ? types.length != 0
                : args.length != types.length) {
            throw new IllegalArgumentException(
                    "Invalid number of actual parameters");
        }
        for (int i = types.length - 1; i >= 0; i--) {
            if (types[i].isPrimitive()) {
                if (args[i] instanceof Number 
                    || args[i] instanceof Character
                    || args[i] instanceof Boolean) {
                    // more accurate conversion testing better done on VM side                    
                    continue;
                }
            } else if (args[i] == null || types[i].isInstance(args[i])) {
                continue;
            }
            throw new IllegalArgumentException("Actual parameter: "
                  + (args[i] == null ? "<null>" : args[i].getClass().getName())   
                  + " is incompatible with " + types[i].getName());
        }
    }
}
