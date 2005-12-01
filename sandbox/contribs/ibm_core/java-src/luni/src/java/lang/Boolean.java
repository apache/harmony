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

package java.lang;


/**
 * Booleans are objects (i.e. non-base types) which represent boolean values.
 */
public final class Boolean implements java.io.Serializable {

	static final long serialVersionUID = -3665804199014368530L;

	/**
	 * The boolean value of the receiver.
	 */
	private final boolean value;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new boolean[0].getClass()
			.getComponentType();

	// Note: This can't be set to "boolean.class", since *that* is
	// defined to be "java.lang.Boolean.TYPE";

	/**
	 * The instance of the receiver which represents truth.
	 */
	public static final Boolean TRUE = new Boolean(true);

	/**
	 * The instance of the receiver which represents falsehood.
	 */
	public static final Boolean FALSE = new Boolean(false);

	/**
	 * Constructs a new instance of this class given a string. If the string is
	 * equal to "true" using a non-case sensitive comparison, the result will be
	 * a Boolean representing true, otherwise it will be a Boolean representing
	 * false.
	 * 
	 * @param string
	 *            The name of the desired boolean.
	 */
	public Boolean(String string) {
		this(toBoolean(string));
	}

	/**
	 * Constructs a new instance of this class given true or false.
	 * 
	 * @param value
	 *            true or false.
	 */
	public Boolean(boolean value) {
		this.value = value;
	}

	/**
	 * Answers true if the receiver represents true and false if the receiver
	 * represents false.
	 * 
	 * @return true or false.
	 */
	public boolean booleanValue() {
		return value;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison.
	 * <p>
	 * In this case, the argument must also be a Boolean, and the receiver and
	 * argument must represent the same boolean value (i.e. both true or both
	 * false).
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		return (o == this)
				|| ((o instanceof Boolean) && (value == ((Boolean) o).value));
	}

	/**
	 * Answers true if the system property described by the argument equal to
	 * "true" using case insensitive comparison, and false otherwise.
	 * 
	 * @param string
	 *            The name of the desired boolean.
	 * @return The boolean value.
	 */
	public static boolean getBoolean(String string) {
		if (string == null || string.length() == 0)
			return false;
		return (toBoolean(System.getProperty(string)));
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return value ? 1231 : 1237;
	}

	/**
	 * Answers true if the argument is equal to "true" using case insensitive
	 * comparison, and false otherwise.
	 * 
	 * @param string
	 *            The name of the desired boolean.
	 * @return the boolean value.
	 */
	static boolean toBoolean(String string) {
		return (string != null) && (string.toLowerCase().equals("true"));
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return String.valueOf(value);
	}

	/**
	 * Converts the specified boolean to its string representation. When the
	 * boolean is true answer <code>"true"</code>, otherwise answer
	 * <code>"false"</code>.
	 * 
	 * @param value
	 *            the boolean
	 * @return the boolean converted to a string
	 */
	public static String toString(boolean value) {
		return String.valueOf(value);
	}

	/**
	 * Answers a Boolean representing true if the argument is equal to "true"
	 * using case insensitive comparison, and a Boolean representing false
	 * otherwise.
	 * 
	 * @param string
	 *            The name of the desired boolean.
	 * @return the boolean value.
	 */
	public static Boolean valueOf(String string) {
		return toBoolean(string) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Answers Boolean.TRUE if the argument is equal to "true" using case
	 * insensitive comparison, and Boolean.FALSE representing false otherwise.
	 * 
	 * @param b
	 *            the boolean value.
	 * @return Boolean.TRUE or Boolean.FALSE Global true/false objects.
	 */
	public static Boolean valueOf(boolean b) {
		return b ? Boolean.TRUE : Boolean.FALSE;
	}
}
