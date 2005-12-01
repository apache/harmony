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
 * Shorts are objects (non-base types) which represent short values.
 */
public final class Short extends Number implements Comparable {

	static final long serialVersionUID = 7515723908773894738L;

	/**
	 * The value which the receiver represents.
	 */
	final short value;

	/**
	 * Most positive and most negative possible short values.
	 */
	public static final short MAX_VALUE = (short) 0x7FFF;

	public static final short MIN_VALUE = (short) 0x8000;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new short[0].getClass().getComponentType();

	// Note: This can't be set to "short.class", since *that* is
	// defined to be "java.lang.Short.TYPE";

	/**
	 * Constructs a new instance of this class given a string.
	 * 
	 * @param string
	 *            a string representation of a short quantity.
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a short quantity.
	 */
	public Short(String string) throws NumberFormatException {
		this(parseShort(string));
	}

	/**
	 * Constructs a new instance of the receiver which represents the short
	 * valued argument.
	 * 
	 * @param value
	 *            the short to store in the new instance.
	 */
	public Short(short value) {
		this.value = value;
	}

	/**
	 * Answers the byte value which the receiver represents
	 * 
	 * @return byte the value of the receiver.
	 */
	public byte byteValue() {
		return (byte) value;
	}

	public int compareTo(Object object) {
		return compareTo((Short) object);
	}

	public int compareTo(Short object) {
		return value > object.value ? 1 : (value < object.value ? -1 : 0);
	}

	/**
	 * Parses the string argument as if it was a short value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * int quantity. The string may be a hexadecimal ("0x..."), octal ("0..."),
	 * or decimal ("...") representation of a byte.
	 * 
	 * @param string
	 *            a string representation of a short quantity.
	 * @return Short the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a short quantity.
	 */
	public static Short decode(String string) throws NumberFormatException {
		int intValue = Integer.decode(string).intValue();
		short result = (short) intValue;
		if (result == intValue)
			return new Short(result);
		throw new NumberFormatException();
	}

	/**
	 * Answers the double value which the receiver represents
	 * 
	 * @return double the value of the receiver.
	 */
	public double doubleValue() {
		return value;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison.
	 * <p>
	 * In this case, the argument must also be a Short, and the receiver and
	 * argument must represent the same short value.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		return (object == this) || (object instanceof Short)
				&& (value == ((Short) object).value);
	}

	/**
	 * Answers the float value which the receiver represents
	 * 
	 * @return float the value of the receiver.
	 */
	public float floatValue() {
		return value;
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
		return value;
	}

	/**
	 * Answers the int value which the receiver represents
	 * 
	 * @return int the value of the receiver.
	 */
	public int intValue() {
		return value;
	}

	/**
	 * Answers the long value which the receiver represents
	 * 
	 * @return long the value of the receiver.
	 */
	public long longValue() {
		return value;
	}

	/**
	 * Parses the string argument as if it was a short value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * short quantity.
	 * 
	 * @param string
	 *            a string representation of a short quantity.
	 * @return short the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a short quantity.
	 */
	public static short parseShort(String string) throws NumberFormatException {
		return parseShort(string, 10);
	}

	/**
	 * Parses the string argument as if it was a short value and returns the
	 * result. Throws NumberFormatException if the string does not represent a
	 * single short quantity. The second argument specifies the radix to use
	 * when parsing the value.
	 * 
	 * @param string
	 *            a string representation of a short quantity.
	 * @param radix
	 *            the radix to use when parsing.
	 * @return short the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a short quantity.
	 */
	public static short parseShort(String string, int radix)
			throws NumberFormatException {
		int intValue = Integer.parseInt(string, radix);
		short result = (short) intValue;
		if (result == intValue)
			return result;
		throw new NumberFormatException();
	}

	/**
	 * Answers the short value which the receiver represents
	 * 
	 * @return short the value of the receiver.
	 */
	public short shortValue() {
		return value;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return Integer.toString(value);
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * argument.
	 * 
	 * @param value
	 *            short the short to convert.
	 * @return String a printable representation for the short.
	 */
	public static String toString(short value) {
		return Integer.toString(value);
	}

	/**
	 * Parses the string argument as if it was a short value and returns a Short
	 * representing the result. Throws NumberFormatException if the string does
	 * not represent a single short quantity.
	 * 
	 * @param string
	 *            a string representation of a short quantity.
	 * @return Short the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a short quantity.
	 */
	public static Short valueOf(String string) throws NumberFormatException {
		return new Short(parseShort(string));
	}

	/**
	 * Parses the string argument as if it was a short value and returns a Short
	 * representing the result. Throws NumberFormatException if the string does
	 * not represent a short quantity. The second argument specifies the radix
	 * to use when parsing the value.
	 * 
	 * @param string
	 *            a string representation of a short quantity.
	 * @param radix
	 *            the radix to use when parsing.
	 * @return Short the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a short quantity.
	 */
	public static Short valueOf(String string, int radix)
			throws NumberFormatException {
		return new Short(parseShort(string, radix));
	}
}
