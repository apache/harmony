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
package org.apache.harmony.vm;

/**
 * Provides the methods to get an information about the execution stack. These
 * methods may be used by different Java APIs such as security classes, SQL
 * packages, Class, ClassLoader and so on.
 * <p>
 * Note, that some of the methods do almost the same things, still the different
 * methods must be used for different tasks to reach the better performance.
 * <p>
 * This class must be implemented according to the common policy for porting
 * interfaces - see the porting interface overview for more details.
 * 
 * @author Evgueni Brevnov, Roman S. Bushmanov
 * 
 * @api2vm
 */
public final class VMStack {

    /**
     * This class is not supposed to be instantiated.
     */
    private VMStack() {
    }

    /**
     * Returns the class from the specified depth in the stack. If the
     * specified depth is equal to zero then the caller of the caller of this
     * method should be returned. Reflection stack frames should not be taken
     * into account. 
     * 
     * @param depth the stack depth to get a caller class from. It is not
     *        negative one.
     * @return class a class from the stack. If there is no class in specified
     *         depth, null is returned.
     * @api2vm
     */
    public static native Class<?> getCallerClass(int depth);

    /**
     * Collects and returns the stack of the current thread as an array of
     * classes. Resulting array should contain maxSize elements at the maximum.
     * Note that reflection stack frames should not be taken into account. The
     * caller of the caller of the caller of this method is stored as a first
     * element of the array. If considerPrivileged is true then the last
     * element of the array should be the caller of the most recent privileged
     * method.  
     * <p>
     * This method may be used by security checks implementation. It is not
     * supposed to be used by Throwable class.
     * 
     * @param maxSize maximum size of resulting array. If maxSize is less than
     * zero array may contain any number of elements.
     * @param considerPrivileged indicates that privileged methods should be
     *        taken into account. It means if considerPrivileged is true the
     *        last element of resulting array should be the caller of the most
     *        recent privileged method. If considerPrivileged is false then
     *        privileged methods don't affect resulting array.
     *        
     * @return a stack of invoked methods as an array of classes.
     * @api2vm
     */
    public static native Class[] getClasses(int maxSize, boolean considerPrivileged);

    /**
     * Saves stack information of currently executing thread. Returned object
     * can be used as a handler to obtain an array of
     * <code>java.lang.StackTraceElement</code> by means of the
     * {@link VMStack#getStackTrace() VMStack.getStackTrace()} method.
     *   
     * @return handler of the current stack. 
     */
    public static native Object getStackState();

    /**
     * Collects and returns the classes of invoked methods as an array of the
     * {@link Class} objects. This method may be used by
     * <code>java.lang.Throwable</code> class implementation.
     * <p>
     * Resulting stack should contain native stack frames as well as reflection
     * stack frames.
     * <p>
     * <b>Note</b>, that it returns classes for all stack, without any checks.
     * It's fast, simple version of {@link VMStack#getClasses() VMStack.getClasses()}
     * method, and used from Throwable class implementation.
     *
     * @param state handler returned by the
     *        {@link VMStack#getStackState() VMStack.getStackState()} method.
     * @return array of <code>Class</code> elements. If stack is
     *         empty then null should be returned.
     * @api2vm
     */
    public static native Class[] getStackClasses(Object state);

    /**
     * Collects and returns the stack of invoked methods as an array of the
     * {@link StackTraceElement} objects. This method may be used by
     * <code>java.lang.Throwable</code> class implementation.
     * <p>
     * Resulting stack should contain native stack frames as well as reflection
     * stack frames.
     * <p>
     * <b>Note</b>, that stack frames corresponding to exception creation should
     * be excluded form the resulting array. The most top (recently invoked)
     * method is stored as a first element of the array.
     * 
     * @param state handler returned by the
     *        {@link VMStack#getStackState() VMStack.getStackState()} method.
     * @return array of <code>StackTraceElement</code> elements. If stack is
     *         empty then array of length 0 should be returned.
     * @api2vm
     */
    public static native StackTraceElement[] getStackTrace(Object state);

    public static native StackTraceElement[] getThreadStackTrace(Thread t);
}
