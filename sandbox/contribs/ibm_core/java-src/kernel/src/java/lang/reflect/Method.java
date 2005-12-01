/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/**
 * This class must be implemented by the vm vendor. This class models a method.
 * Information about the method can be accessed, and the method can be invoked
 * dynamically.
 * 
 */
public final class Method extends AccessibleObject implements Member {
	/**
	 * Compares the specified object to this Method and answer if they are
	 * equal. The object must be an instance of Method with the same defining
	 * class and parameter types.
	 * 
	 * @param object
	 *            the object to compare
	 * @return true if the specified object is equal to this Method, false
	 *         otherwise
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		return false;
	}

	/**
	 * Return the java.lang.Class associated with the class that defined this
	 * constructor.
	 * 
	 * @return the declaring class
	 */
	public Class getDeclaringClass() {
		return null;
	}

	/**
	 * Return an array of the java.lang.Class objects associated with the
	 * exceptions declared to be thrown by this method. If the method was not
	 * declared to throw any exceptions, the array returned will be empty.
	 * 
	 * @return the declared exception classes
	 */
	public Class[] getExceptionTypes() {
		return null;
	}

	/**
	 * Return the modifiers for the modelled constructor. The Modifier class
	 * should be used to decode the result.
	 * 
	 * @return the modifiers
	 * @see java.lang.reflect.Modifier
	 */
	public int getModifiers() {
		return 0;
	}

	/**
	 * Return the name of the modelled method.
	 * 
	 * @return the name
	 */
	public String getName() {
		return null;
	}

	/**
	 * Return an array of the java.lang.Class objects associated with the
	 * parameter types of this method. If the method was declared with no
	 * parameters, the array returned will be empty.
	 * 
	 * @return the parameter types
	 */
	public Class[] getParameterTypes() {
		return null;
	}

	/**
	 * Return the java.lang.Class associated with the return type of this
	 * method.
	 * 
	 * @return the return type
	 */
	public Class getReturnType() {
		return null;
	}

	/**
	 * Answers an integer hash code for the receiver. Objects which are equal
	 * answer the same value for this method. The hash code for a Method is the
	 * hash code of the method's name.
	 * 
	 * @return the receiver's hash
	 * @see #equals
	 */
	public int hashCode() {
		return 0;
	}

	/**
	 * Return the result of dynamically invoking the modelled method. This
	 * reproduces the effect of
	 * <code>receiver.methodName(arg1, arg2, ... , argN)</code> This method
	 * performs the following:
	 * <ul>
	 * <li>If the modelled method is static, the receiver argument is ignored.
	 * </li>
	 * <li>Otherwise, if the receiver is null, a NullPointerException is
	 * thrown.</li>
	 * If the receiver is not an instance of the declaring class of the method,
	 * an IllegalArgumentException is thrown.
	 * <li>If this Method object is enforcing access control (see
	 * AccessibleObject) and the modelled method is not accessible from the
	 * current context, an IllegalAccessException is thrown.</li>
	 * <li>If the number of arguments passed and the number of parameters do
	 * not match, an IllegalArgumentException is thrown.</li>
	 * <li>For each argument passed:
	 * <ul>
	 * <li>If the corresponding parameter type is a base type, the argument is
	 * unwrapped. If the unwrapping fails, an IllegalArgumentException is
	 * thrown.</li>
	 * <li>If the resulting argument cannot be converted to the parameter type
	 * via a widening conversion, an IllegalArgumentException is thrown.</li>
	 * </ul>
	 * <li>If the modelled method is static, it is invoked directly. If it is
	 * non-static, the modelled method and the receiver are then used to perform
	 * a standard dynamic method lookup. The resulting method is then invoked.
	 * </li>
	 * <li>If an exception is thrown during the invocation it is caught and
	 * wrapped in an InvocationTargetException. This exception is then thrown.
	 * </li>
	 * <li>If the invocation completes normally, the return value is itself
	 * returned. If the method is declared to return a base type, the return
	 * value is first wrapped. If the return type is void, null is returned.
	 * </li>
	 * </ul>
	 * 
	 * @param args
	 *            the arguments to the constructor
	 * @return the new, initialized, object
	 * @exception java.lang.NullPointerException
	 *                if the receiver is null for a non-static method
	 * @exception java.lang.IllegalAccessException
	 *                if the modelled method is not accessible
	 * @exception java.lang.IllegalArgumentException
	 *                if an incorrect number of arguments are passed, the
	 *                receiver is incompatible with the declaring class, or an
	 *                argument could not be converted by a widening conversion
	 * @exception java.lang.reflect.InvocationTargetException
	 *                if an exception was thrown by the invoked constructor
	 * @see java.lang.reflect.AccessibleObject
	 */
	public Object invoke(Object receiver, Object args[])
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		return null;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver. The format of the string is modifiers (if any) return type
	 * declaring class name '.' method name '(' parameter types, separated by
	 * ',' ')' If the method throws exceptions, ' throws ' exception types,
	 * separated by ',' For example:
	 * <code>public native Object java.lang.Method.invoke(Object,Object) throws IllegalAccessException,IllegalArgumentException,InvocationTargetException</code>
	 * 
	 * @return a printable representation for the receiver
	 */
	public String toString() {
		return null;
	}
}
