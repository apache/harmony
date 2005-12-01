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
 * Bytes are objects (i.e. non-base types) which represent byte values.
 */
public final class Byte extends Number implements Comparable {

	static final long serialVersionUID = -7183698231559129828L;

	/**
	 * The value which the receiver represents.
	 */
	final byte value;

	/**
	 * Most positive and most negative possible byte values.
	 */
	public static final byte MAX_VALUE = (byte) 0x7F;

	public static final byte MIN_VALUE = (byte) 0x80;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new byte[0].getClass().getComponentType();

	// Note: This can't be set to "byte.class", since *that* is
	// defined to be "java.lang.Byte.TYPE";

	/**
	 * Constructs a new instance of the receiver which represents the byte
	 * valued argument.
	 * 
	 * @param value
	 *            the byte to store in the new instance.
	 */
	public Byte(byte value) {
		this.value = value;
	}

	/**
	 * Constructs a new instance of this class given a string.
	 * 
	 * @param string
	 *            a string representation of a single byte quantity.
	 * @throws NumberFormatException
	 *             if the argument could not be parsed as a byte quantity.
	 */
	public Byte(String string) throws NumberFormatException {
		this(parseByte(string));
	}

	/**
	 * Answers the byte value which the receiver represents
	 * 
	 * @return byte the value of the receiver.
	 */
	public byte byteValue() {
		return value;
	}

	public int compareTo(Byte object) {
		return value > object.value ? 1 : (value < object.value ? -1 : 0);
	}

	public int compareTo(Object object) {
		return compareTo((Byte) object);
	}

	/**
	 * Parses the string argument as if it was a byte value and returns the
	 * result. It is an error if the received string does not contain a
	 * representation of a single byte quantity. The string may be a hexadecimal
	 * ("0x..."), octal ("0..."), or decimal ("...") representation of a byte.
	 * 
	 * @param string
	 *            a string representation of a single byte quantity.
	 * @return Byte the value represented by the argument
	 * @throws NumberFormatException
	 *             if the argument could not be parsed as a byte quantity.
	 */
	public static Byte decode(String string) throws NumberFormatException {
		int intValue = Integer.decode(string).intValue();
		byte result = (byte) intValue;
		if (result == intValue)
			return new Byte(result);
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
	 * In this case, the argument must also be a Byte, and the receiver and
	 * argument must represent the same byte value.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		return (object == this) || (object instanceof Byte)
				&& (value == ((Byte) object).value);
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
	 * Parses the string argument as if it was a byte value and returns the
	 * result. Throws NumberFormatException if the string does not represent a
	 * single byte quantity.
	 * 
	 * @param string
	 *            a string representation of a single byte quantity.
	 * @return byte the value represented by the argument
	 * @throws NumberFormatException
	 *             if the argument could not be parsed as a byte quantity.
	 */
	public static byte parseByte(String string) throws NumberFormatException {
		int intValue = Integer.parseInt(string);
		byte result = (byte) intValue;
		if (result == intValue)
			return result;
		throw new NumberFormatException();
	}

	/**
	 * Parses the string argument as if it was a byte value and returns the
	 * result. Throws NumberFormatException if the string does not represent a
	 * single byte quantity. The second argument specifies the radix to use when
	 * parsing the value.
	 * 
	 * @param string
	 *            a string representation of a single byte quantity.
	 * @param radix
	 *            the radix to use when parsing.
	 * @return byte the value represented by the argument
	 * @throws NumberFormatException
	 *             if the argument could not be parsed as a byte quantity.
	 */
	public static byte parseByte(String string, int radix)
			throws NumberFormatException {
		int intValue = Integer.parseInt(string, radix);
		byte result = (byte) intValue;
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
	 *            byte the byte to convert.
	 * @return String a printable representation for the byte.
	 */
	public static String toString(byte value) {
		return Integer.toString(value);
	}

	/**
	 * Parses the string argument as if it was a byte value and returns a Byte
	 * representing the result. Throws NumberFormatException if the string
	 * cannot be parsed as a byte quantity.
	 * 
	 * @param string
	 *            a string representation of a single byte quantity.
	 * @return Byte the value represented by the argument
	 * @throws NumberFormatException
	 *             if the argument could not be parsed as a byte quantity.
	 */
	public static Byte valueOf(String string) throws NumberFormatException {
		return new Byte(parseByte(string));
	}

	/**
	 * Parses the string argument as if it was a byte value and returns a Byte
	 * representing the result. Throws NumberFormatException if the string
	 * cannot be parsed as a byte quantity. The second argument specifies the
	 * radix to use when parsing the value.
	 * 
	 * @param string
	 *            a string representation of a single byte quantity.
	 * @param radix
	 *            the radix to use when parsing.
	 * @return Byte the value represented by the argument
	 * @throws NumberFormatException
	 *             if the argument could not be parsed as a byte quantity.
	 */
	public static Byte valueOf(String string, int radix)
			throws NumberFormatException {
		return new Byte(parseByte(string, radix));
	}
}
