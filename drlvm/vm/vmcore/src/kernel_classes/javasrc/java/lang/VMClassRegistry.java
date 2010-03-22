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
 * @author Evgueni Brevnov, Roman S. Bushmanov
 */ 

package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import org.apache.harmony.drlvm.VMHelper;


/**
 * Provides the class information methods required for the
 * {@link java.lang.Class Class} implementation, and class loading/resolution
 * methods for the {@link java.lang.ClassLoader ClassLoader} implementation.
 * <p>
 * An implementation of the <code>java.lang.Class</code> class should not relay
 * on default object initialization by the VM. In other words the VM is free to
 * skip execution of the <code>java.lang.Class</code> private constructor. 
 * <p>
 * This class must be implemented according to the common policy for porting
 * interfaces - see the porting interface overview for more detailes.
 * 
 * @api2vm
 */
final class VMClassRegistry 
{
    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getSimpleName() Class.getSimpleName()}
     * method.
     * 
     * @param clazz a class to perform an operation on.
     * @return the simple name of the specified class 
     * 
     * @api2vm
     */
    static native String getSimpleName(Class<?> clazz);

    /**
     * Returns the nearest enclosing class of the specified Class instance, 
     * or <code>null</code> if the specified class is a top-level class.
     * <br>This information is gathered from corresponding class-file structures 
     * (either EnclosingMethod or InnerClasses attribute, if any present). 
     * 
     * @param clazz a class to perform an operation on.
     * @return the immediately enclosing class of the specified class or null 
     * 
     * @api2vm
     */    
    static native Class getEnclosingClass(Class<?> clazz);

    /**
     * If the specified class is a local or anonymous class defined 
     * within a method or constructor, returns that closest
     * enclosing reflection member. Otherwise returns <code>null</code>.
     * Note, instance initializers and static initializers 
     * are not reflectable and will never be considered.
     * <br>This information is gathered from corresponding class-file structure 
     * (EnclosingMethod attribute, if present). 
     * 
     * @param clazz a class to perform an operation on.
     * @return the immediately enclosing member for the specified class or null 
     * 
     * @api2vm
     */
    static native Member getEnclosingMember(Class<?> clazz);

    /**
     * This class is not supposed to be instantiated.
     */
    private VMClassRegistry() 
    {
    }
   
    /**
     * Loads the specified class with the bootstrap classloader.  
     * @throws LinkageError (or any subtype) if loading failed
     * @api2vm
     */
    static native Class<?> loadBootstrapClass(String name);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Object#getClass() Object.getClass()} method.
     * @api2vm
     */
    static native Class<? extends Object> getClassNative(Object obj);

    static Class<? extends Object> getClass(Object obj) {
        if (VMHelper.isVMMagicPackageSupported()) {
            return (Class<? extends Object>)VMHelper.getManagedClass(obj).toObjectReference().toObject();
        }
        return getClassNative(obj);
    }


    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getClassLoader() Class.getClassLoader()} method except
     * the clazz parameter may be null. In this case context class loader
     * of the current thread is returned.
     * 
     * @api2vm
     */
    static ClassLoader getClassLoader(Class<?> clazz) {
        if (clazz != null) {
            assert(getClassLoader0(clazz) == clazz.definingLoader);
            return clazz.definingLoader;
        }
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getClassLoader() Class.getClassLoader()} method.
     * 
     * @api2vm
     */
    static native ClassLoader getClassLoader0(Class<?> clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getComponentType() Class.getComponentType()} method.
     * @api2vm
     */
    static native Class<?> getComponentType(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getDeclaredClasses() Class.getDeclaredClasses()}
     *  method.
     * @api2vm
     */
    static native Class[] getDeclaredClasses(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getDeclaredConstructors() Class.getDeclaredConstructors()}
     * method.
     * @api2vm
     */
    static native <U> Constructor<U>[] getDeclaredConstructors(Class<U> clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getDeclaredFields() Class.getDeclaredFields()} method.
     * @api2vm
     */
    static native Field[] getDeclaredFields(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getDeclaredMethods() Class.getDeclaredMethods()} method. 
     * @api2vm
     */
    static native Method[] getDeclaredMethods(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getDeclaringClass() Class.getDeclaringClass()} method.
     * @api2vm
     */
    static native Class<?> getDeclaringClass(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getInterfaces() Class.getInterfaces()} method.
     * @api2vm
     */
    static native Class[] getInterfaces(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getModifiers() Class.getModifiers()} method.
     * @api2vm
     */
    static native int getModifiers(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getName() Class.getName()} method.
     * @api2vm
     */
    static native String getName(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#getSuperclass() Class.getSuperclass()} method.
     * @api2vm
     */
    static native <U> Class<? super U> getSuperclass(Class<U> clazz);

    /**
     * This method returns a list describing the system packages, 
     * in format of {{name, url}}. That is, the list consists of 
     * pairs "{name, url}", organized as the 2-dimensional array[N][2].
     * The "name" is a Java package name.    
     * The "url" points to the jar file from which the corresponding package 
     * is loaded. If package comes not from a jar, then url is null.
     * 
     * @param len number of packages caller already knows. If this number is
     * equal to the actual number of system packages defined by VM, 
     * this method will skip array creation and return null.
     * @return a set of packages defined by bootstrap class loader or null
     * @api2vm
     */
    static native String[][] getSystemPackages(int len);

    /**
     * This method is used for the
     * {@link Class#forName(java.lang.String, boolean, java.lang.ClassLoader)
     * Class.forName(String name, boolean initialize, ClassLoader loader)}
     * method implementation. If the initialize parameter is true then this 
     * method should be invoked in order to initialize a class. The specified
     * clazz parameter must not be null.
     * 
     * @param clazz a class to perform an operation on.
     * @throws ExceptionInInitializerError if initialization fails.
     * @api2vm
     */
    static native void initializeClass(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#isArray() Class.isArray()} method.
     * @api2vm
     */
    static native boolean isArray(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#isAssignableFrom(java.lang.Class)
     * Class.isAssignableFrom(Class cls)} method.
     * @api2vm
     */
    static native boolean isAssignableFrom(Class clazz, Class<?> fromClazz); //XXX: does it have any sense?

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#isInstance(java.lang.Object) Class.isInstance(Object obj)}
     * method.
     * @api2vm
     */
    static native boolean isInstance(Class clazz, Object obj);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Class#isPrimitive() Class.isPrimitive()} method.
     * @api2vm
     */
    static native boolean isPrimitive(Class clazz);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link ClassLoader#resolveClass(java.lang.Class)
     * ClassLoader.resolveClass(Class c)} method. Except that it doesn't throw
     * <code>NullPointerException</code> but throws <code>LinkagError</code>
     * exception. The specified clazz parameter must not be null.
     * 
     * @throws LinkageError if linking fails.
     * @api2vm
     */
    static native void linkClass(Class<?> clazz);

    /**
     * This method is used for the
     * {@link Class#forName(java.lang.String, boolean, java.lang.ClassLoader)
     * Class.forName(String name, boolean initialize, ClassLoader loader)}
     * method implementation. If the name parameter represents an array then this  
     * method should be invoked in order to load an array class. For example, an
     * expression (loadArray(Integer.TYPE, 1) == new int[0].getClass()) must be
     * true. 
     * <p>
     * <b>Note:</b> Under design yet. Subjected to change.
     * 
     * @param componentType the type of array components. It must not be null.
     * @param dimensions array dimension. It must be greater or equal to 0.
     * @return a class which represents array
     * @api2vm
     */
    static native Class loadArray(Class componentType, int dimensions);

    /**
     * This method is used for implementation of the
     * {@link Runtime#load(java.lang.String) Runtime.load(String filename)}
     * method.
     * @param filename full library name.
     * @param loader the library will be loaded into the specified class loader
     *        namespace
     * @throws UnsatisfiedLinkError if library can not be loaded for any reason  
     * @api2vm
     */
    static native void loadLibrary(String filename, ClassLoader loader);
}
