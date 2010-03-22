/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * This class represents a constructor. Information about the constructor can be
 * accessed, and the constructor can be invoked dynamically.
 *
 * @param <T> the class that declares this constructor
 */
public final class Constructor<T> extends AccessibleObject implements GenericDeclaration,
        Member {

    /*
     * This class must be implemented by the VM vendor.
     */

    /**
     * Prevent this class from being instantiated
     */
    private Constructor(){
        //do nothing
    }

    public TypeVariable<Constructor<T>>[] getTypeParameters() {
        return null;
    }

    /**
     * Returns the string representation of the constructor's declaration,
     * including the type parameters.
     *
     * @return the string representation of the constructor's declaration
     * @since 1.5
     */
    public String toGenericString() {
        return null;
    }

    /**
     * Returns the generic parameter types as an array of {@code Type}
     * instances, in declaration order. If this constructor has no generic
     * parameters, an empty array is returned.
     *
     * @return the parameter types
     * @throws GenericSignatureFormatError
     *             if the generic constructor signature is invalid
     * @throws TypeNotPresentException
     *             if any parameter type points to a missing type
     * @throws MalformedParameterizedTypeException
     *             if any parameter type points to a type that cannot be
     *             instantiated for some reason
     * @since 1.5
     */
    public Type[] getGenericParameterTypes() {
        return null;
    }

    /**
     * Returns the exception types as an array of {@code Type} instances. If
     * this constructor has no declared exceptions, an empty array will be
     * returned.
     *
     * @return an array of generic exception types
     * @throws GenericSignatureFormatError
     *             if the generic constructor signature is invalid
     * @throws TypeNotPresentException
     *             if any exception type points to a missing type
     * @throws MalformedParameterizedTypeException
     *             if any exception type points to a type that cannot be
     *             instantiated for some reason
     * @since 1.5
     */
    public Type[] getGenericExceptionTypes() {
        return null;
    }

    /**
     * Returns an array of arrays that represent the annotations of the formal
     * parameters of this constructor. If there are no parameters on this
     * constructor, then an empty array is returned. If there are no annotations
     * set, then an array of empty arrays is returned.
     *
     * @return an array of arrays of {@code Annotation} instances
     * @since 1.5
     */
    public Annotation[][] getParameterAnnotations() {
        return null;
    }

    /**
     * Indicates whether or not this constructor takes a variable number of
     * arguments.
     *
     * @return {@code true} if a vararg is declare, otherwise
     *         {@code false}
     * @since 1.5
     */
    public boolean isVarArgs() {
        return false;
    }

    /**
     * Indicates whether or not this constructor is synthetic (artificially
     * introduced by the compiler).
     *
     * @return {@code true} if this constructor is synthetic, {@code false}
     *         otherwise
     */
    public boolean isSynthetic() {
        return false;
    }

    /**
     * Indicates whether or not the specified {@code object} is equal to this
     * constructor. To be equal, the specified object must be an instance
     * of {@code Constructor} with the same declaring class and parameter types
     * as this constructor.
     *
     * @param object
     *            the object to compare
     *
     * @return {@code true} if the specified object is equal to this
     *         constructor, {@code false} otherwise
     * @see #hashCode
     */
    @Override
    public boolean equals(Object object) {
        return false;
    }

    /**
     * Returns the class that declares this constructor.
     *
     * @return the declaring class
     */
    public Class<T> getDeclaringClass() {
        return null;
    }

    /**
     * Returns the exception types as an array of {@code Class} instances. If
     * this constructor has no declared exceptions, an empty array will be
     * returned.
     *
     * @return the declared exception classes
     */
    public Class<?>[] getExceptionTypes() {
        return null;
    }

    /**
     * Returns the modifiers for this constructor. The {@link Modifier} class
     * should be used to decode the result.
     *
     * @return the modifiers for this constructor
     * @see Modifier
     */
    public int getModifiers() {
        return 0;
    }

    /**
     * Returns the name of this constructor.
     *
     * @return the name of this constructor
     */
    public String getName() {
        return null;
    }

    /**
     * Returns an array of the {@code Class} objects associated with the
     * parameter types of this constructor. If the constructor was declared with
     * no parameters, an empty array will be returned.
     *
     * @return the parameter types
     */
    public Class<?>[] getParameterTypes() {
        return null;
    }

    /**
     * Returns an integer hash code for this constructor. Constructors which are
     * equal return the same value for this method. The hash code for a
     * Constructor is the hash code of the name of the declaring class.
     *
     * @return the hash code
     * @see #equals
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Returns a new instance of the declaring class, initialized by dynamically
     * invoking the constructor represented by this {@code Constructor} object.
     * This reproduces the effect of {@code new declaringClass(arg1, arg2, ... ,
     * argN)} This method performs the following:
     * <ul>
     * <li>A new instance of the declaring class is created. If the declaring
     * class cannot be instantiated (i.e. abstract class, an interface, an array
     * type, or a primitive type) then an InstantiationException is thrown.</li>
     * <li>If this Constructor object is enforcing access control (see
     * {@link AccessibleObject}) and this constructor is not accessible from the
     * current context, an IllegalAccessException is thrown.</li>
     * <li>If the number of arguments passed and the number of parameters do not
     * match, an IllegalArgumentException is thrown.</li>
     * <li>For each argument passed:
     * <ul>
     * <li>If the corresponding parameter type is a primitive type, the argument
     * is unwrapped. If the unwrapping fails, an IllegalArgumentException is
     * thrown.</li>
     * <li>If the resulting argument cannot be converted to the parameter type
     * via a widening conversion, an IllegalArgumentException is thrown.</li>
     * </ul>
     * <li>The constructor represented by this {@code Constructor} object is
     * then invoked. If an exception is thrown during the invocation, it is
     * caught and wrapped in an InvocationTargetException. This exception is
     * then thrown. If the invocation completes normally, the newly initialized
     * object is returned.
     * </ul>
     *
     * @param args
     *            the arguments to the constructor
     *
     * @return the new, initialized, object
     *
     * @exception InstantiationException
     *                if the class cannot be instantiated
     * @exception IllegalAccessException
     *                if this constructor is not accessible
     * @exception IllegalArgumentException
     *                if an incorrect number of arguments are passed, or an
     *                argument could not be converted by a widening conversion
     * @exception InvocationTargetException
     *                if an exception was thrown by the invoked constructor
     *
     * @see AccessibleObject
     */
    public T newInstance(Object... args) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return null;
    }

    /**
     * Returns a string containing a concise, human-readable description of this
     * constructor. The format of the string is:
     *
     * <ol>
     *   <li>modifiers (if any)
     *   <li>declaring class name
     *   <li>'('
     *   <li>parameter types, separated by ',' (if any)
     *   <li>')'
     *   <li>'throws' plus exception types, separated by ',' (if any)
     * </ol>
     *
     * For example:
     * {@code public String(byte[],String) throws UnsupportedEncodingException}
     *
     * @return a printable representation for this constructor
     */
    @Override
    public String toString() {
        return null;
    }
}
