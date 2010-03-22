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
 * @author Evgueni Brevnov
 */

package java.lang.reflect;

/**
 * Provides the package private methods requirered for the
 * <code>java.lang.reflect</code> package implementation.
 * <p>
 * This class must be implemented according to the common policy for porting
 * interfaces - see the porting interface overview for more detailes.
 * <p>
 * <b>Note: </b> this class design is based on requirements for the
 * {@link Field}, {@link Method} and {@link Constructor} classes from the
 * <code>java.lang.reflect</code> package. Each class (Field, Method, Constructor)
 * should have a constructor that accepts an argument of the
 * {@link java.lang.Object} type. This argument serves as an identifier of
 * particular item. This identifier should be used later when one needs to 
 * operate with an item. For example, see {@link VMReflection#getFieldType(Object)
 * VMReflection.getFieldType(Object id)} method.
 * @api2vm
 */
 
final class VMReflection {

    /**
     * This class is not supposed to be instantiated.
     */
    private VMReflection() {
    }

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Method#getExceptionTypes() Method.getExceptionTypes()} method. But
     * it takes one additional id parameter.
     * 
     * @param id an identifier of the caller class.
     * @api2vm
     */
    static native Class<?>[] getExceptionTypes(long id);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Field#getType() Field.getType()} method. But it takes one
     * additional id parameter.
     * 
     * @param id an identifier of the caller class.
     * @api2vm
     */
    static native Class<?> getFieldType(long id);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Method#getReturnType() Method.getReturnType()} method. But it
     * takes one additional id parameter.
     * 
     * @param id an identifier of the caller class.
     * @api2vm
     */
    static native Class<?> getMethodReturnType(long id);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Method#getParameterTypes() Method.getParameterTypes()} method. But
     * it takes one additional id parameter.
     * 
     * @param id an identifier of the caller class.
     * @api2vm
     */
    static native Class<?>[] getParameterTypes(long id);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Method#invoke(java.lang.Object, java.lang.Object[])
     * Method.invoke(Object obj, Object[] args)} method. But it differs in
     * several ways.
     * <p>
     * First, it takes one additional <code>id</code> parameter. This parameter
     * is used as an identifier to invoke corresponding method.
     * <p>
     * Second, it doesn't perform access control so it doesn't throw an
     * <code>IllegalAccessException</code> exception. 
     * <p>
     * Third, it throws <code>IllegalArgumentException</code> only if the
     * <code>args</code> argument doesn't fit to the actual method parameters.  
     * <p>
     * Last, it doesn't throw an <code>NullPointerException</code> exception. If
     * the <code>id</code> argument corresponds to a static method then the
     * object argument must be null. An attempt to invoke a static method will
     * be made in this case. If the <code>id</code> argument corresponds to a
     * non-static method then corresponding object's method will be invoked.
     * <p>
     * <b>Note:</b> Under design yet. Subjected to change.
     * 
     * @param id the identifier of the method to be invoked.
     * @api2vm
     */
    static native Object invokeMethod(long id, Object object, Object... args)
        throws InvocationTargetException;

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Array#newInstance(Class, int[])
     * Array.newInstance(Class componentType, int[] dimensions)} method. But it
     * differs in several ways.
     * <p>
     * First, it it doesn't throw an <code>NullPointerException</code> exception.
     * <p>
     * Second, it throws an <code>IllegalArgumentException</code> exception only
     * if the implementation doesn't support specified number of dimensions.
     * <p>
     * <b>Note:</b> Under design yet. Subjected to change.
     * @api2vm
     */
    static native Object newArrayInstance(Class<?> type, int[] dimensions);

    /**
     * This method satisfies the requirements of the specification for the
     * {@link Constructor#newInstance(java.lang.Object[])
     * Constructor.newInstance(java.lang.Object[] initargs)} method. But it
     * differs in several ways.
     * <p>
     * First, it takes one additional <code>id</code> parameter. This parameter
     * is used as an identifier of the corresponding constructor.
     * <p>
     * Second, it doesn't perform access control so it doesn't throw an
     * <code>IllegalAccessException</code> exception.
     * <p>
     * Last, it doesn't throw an <code>InstantiationException</code> exception. 
     * The <code>id</code> argument must not correspond to an abstract class.
     * <p>
     * <b>Note:</b> Under design yet. Subjected to change.
     * 
     * @param id the identifier of the method to be invoked.
     * @api2vm
     */
    static native Object newClassInstance(long id, Object... args)
        throws InvocationTargetException;

}
