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
 * Integers are objects (non-base types) which represent int values.
 */
public final class Integer extends Number implements Comparable {

	static final long serialVersionUID = 1360826667806852920L;

	/**
	 * The value which the receiver represents.
	 */
	final int value;

	/**
	 * Most positive and most negative possible int values.
	 */
	public static final int MAX_VALUE = 0x7FFFFFFF;

	public static final int MIN_VALUE = 0x80000000;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new int[0].getClass().getComponentType();

	// Note: This can't be set to "int.class", since *that* is
	// defined to be "java.lang.Integer.TYPE";

	/**
	 * Constructs a new instance of the receiver which represents the int valued
	 * argument.
	 * 
	 * @param value
	 *            the int to store in the new instance.
	 */
	public Integer(int value) {
		this.value = value;
	}

	/**
	 * Constructs a new instance of this class given a string.
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public Integer(String string) throws NumberFormatException {
		this(parseInt(string));
	}

	/**
	 * Answers the byte value which the receiver represents
	 * 
	 * @return byte the value of the receiver.
	 */
	public byte byteValue() {
		return (byte) value;
	}

	public int compareTo(Integer object) {
		return value > object.value ? 1 : (value < object.value ? -1 : 0);
	}

	public int compareTo(Object object) {
		return compareTo((Integer) object);
	}

	/**
	 * Parses the string argument as if it was an int value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * int quantity. The string may be a hexadecimal ("0x..."), octal ("0..."),
	 * or decimal ("...") representation of an integer
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @return Integer the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public static Integer decode(String string) throws NumberFormatException {
		int length = string.length(), i = 0;
		if (length == 0)
			throw new NumberFormatException();
		char firstDigit = string.charAt(i);
		boolean negative = firstDigit == '-';
		if (negative) {
			if (length == 1)
				throw new NumberFormatException(string);
			firstDigit = string.charAt(++i);
		}

		int base = 10;
		if (firstDigit == '0') {
			if (++i == length)
				return new Integer(0);
			if ((firstDigit = string.charAt(i)) == 'x' || firstDigit == 'X') {
				if (i == length)
					throw new NumberFormatException(string);
				i++;
				base = 16;
			} else {
				base = 8;
			}
		} else if (firstDigit == '#') {
			if (i == length)
				throw new NumberFormatException(string);
			i++;
			base = 16;
		}

		int result = parse(string, i, base, negative);
		return new Integer(result);
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
	 * In this case, the argument must also be an Integer, and the receiver and
	 * argument must represent the same int value.
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		return (o == this) || (o instanceof Integer)
				&& (value == ((Integer) o).value);
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
	 * Answers an Integer representing the integer value of the property named
	 * by the argument. If the property could not be found, or its value could
	 * not be parsed as an integer, answer null.
	 * 
	 * @param string
	 *            The name of the desired integer property.
	 * @return Integer An Integer representing the value of the property.
	 */
	public static Integer getInteger(String string) {
		if (string == null || string.length() == 0)
			return null;
		String prop = System.getProperty(string);
		if (prop == null)
			return null;
		try {
			return decode(prop);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * Answers an Integer representing the integer value of the property named
	 * by the argument. If the property could not be found, or its value could
	 * not be parsed as an integer, answer an Integer reperesenting the second
	 * argument.
	 * 
	 * @param string
	 *            The name of the desired integer property.
	 * @return Integer An Integer representing the value of the property.
	 */
	public static Integer getInteger(String string, int defaultValue) {
		if (string == null || string.length() == 0)
			return new Integer(defaultValue);
		String prop = System.getProperty(string);
		if (prop == null)
			return new Integer(defaultValue);
		try {
			return decode(prop);
		} catch (NumberFormatException ex) {
			return new Integer(defaultValue);
		}
	}

	/**
	 * Answers an Integer representing the integer value of the property named
	 * by the argument. If the property could not be found, or its value could
	 * not be parsed as an integer, answer the second argument.
	 * 
	 * @param string
	 *            The name of the desired integer property.
	 * @return Integer An Integer representing the value of the property.
	 */
	public static Integer getInteger(String string, Integer defaultValue) {
		if (string == null || string.length() == 0)
			return defaultValue;
		String prop = System.getProperty(string);
		if (prop == null)
			return defaultValue;
		try {
			return decode(prop);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
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
	 * Parses the string argument as if it was an int value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * int quantity.
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @return int the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public static int parseInt(String string) throws NumberFormatException {
		return parseInt(string, 10);
	}

	/**
	 * Parses the string argument as if it was an int value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * int quantity. The second argument specifies the radix to use when parsing
	 * the value.
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @param radix
	 *            the base to use for conversion.
	 * @return int the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public static int parseInt(String string, int radix)
			throws NumberFormatException {
		if (string == null || radix < Character.MIN_RADIX
				|| radix > Character.MAX_RADIX)
			throw new NumberFormatException();
		int length = string.length(), i = 0;
		if (length == 0)
			throw new NumberFormatException(string);
		boolean negative = string.charAt(i) == '-';
		if (negative && ++i == length)
			throw new NumberFormatException(string);

		return parse(string, i, radix, negative);
	}

	private static int parse(String string, int offset, int radix,
			boolean negative) throws NumberFormatException {
		int max = Integer.MIN_VALUE / radix;
		int result = 0, length = string.length();
		while (offset < length) {
			int digit = Character.digit(string.charAt(offset++), radix);
			if (digit == -1)
				throw new NumberFormatException(string);
			if (max > result)
				throw new NumberFormatException(string);
			int next = result * radix - digit;
			if (next > result)
				throw new NumberFormatException(string);
			result = next;
		}
		if (!negative) {
			result = -result;
			if (result < 0)
				throw new NumberFormatException(string);
		}
		return result;
	}

	/**
	 * Answers the short value which the receiver represents
	 * 
	 * @return short the value of the receiver.
	 */
	public short shortValue() {
		return (short) value;
	}

	/**
	 * Answers a string containing '0' and '1' characters which describe the
	 * binary representation of the argument.
	 * 
	 * @param i
	 *            an int to get the binary representation of
	 * @return String the binary representation of the argument
	 */
	public static String toBinaryString(int i) {
		int count = 1, j = i;

		if (i < 0)
			count = 32;
		else
			while ((j >>>= 1) != 0)
				count++;

		char[] buffer = new char[count];
		do {
			buffer[--count] = (char) ((i & 1) + '0');
			i >>>= 1;
		} while (count > 0);
		return new String(0, buffer.length, buffer);
	}

	/**
	 * Answers a string containing characters in the range 0..9, a..f which
	 * describe the hexadecimal representation of the argument.
	 * 
	 * @param i
	 *            an int to get the hex representation of
	 * @return String the hex representation of the argument
	 */
	public static String toHexString(int i) {
		int count = 1, j = i;

		if (i < 0)
			count = 8;
		else
			while ((j >>>= 4) != 0)
				count++;

		char[] buffer = new char[count];
		do {
			int t = i & 15;
			if (t > 9)
				t = t - 10 + 'a';
			else
				t += '0';
			buffer[--count] = (char) t;
			i >>>= 4;
		} while (count > 0);
		return new String(0, buffer.length, buffer);
	}

	/**
	 * Answers a string containing characters in the range 0..7 which describe
	 * the octal representation of the argument.
	 * 
	 * @param i
	 *            an int to get the octal representation of
	 * @return String the hex representation of the argument
	 */
	public static String toOctalString(int i) {
		int count = 1, j = i;

		if (i < 0)
			count = 11;
		else
			while ((j >>>= 3) != 0)
				count++;

		char[] buffer = new char[count];
		do {
			buffer[--count] = (char) ((i & 7) + '0');
			i >>>= 3;
		} while (count > 0);
		return new String(0, buffer.length, buffer);
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
	 * Answers a string containing characters in the range 0..9 which describe
	 * the decimal representation of the argument.
	 * 
	 * @param i
	 *            an int to get the representation of
	 * @return String the representation of the argument
	 */
	public static String toString(int i) {
		return toString(i, 10);
	}

	/**
	 * Answers a string containing characters in the range 0..9, a..z (depending
	 * on the radix) which describe the representation of the argument in that
	 * radix.
	 * 
	 * @param i
	 *            an int to get the representation of
	 * @param radix
	 *            the base to use for conversion.
	 * @return String the representation of the argument
	 */
	public static String toString(int i, int radix) {
		if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			radix = 10;
		if (i == 0)
			return "0";

		int count = 2, j = i;
		boolean negative = i < 0;
		if (!negative) {
			count = 1;
			j = -i;
		}
		while ((i /= radix) != 0)
			count++;

		char[] buffer = new char[count];
		do {
			int ch = 0 - (j % radix);
			if (ch > 9)
				ch = ch - 10 + 'a';
			else
				ch += '0';
			buffer[--count] = (char) ch;
		} while ((j /= radix) != 0);
		if (negative)
			buffer[0] = '-';
		return new String(0, buffer.length, buffer);
	}

	/**
	 * Parses the string argument as if it was an int value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * int quantity.
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @return Integer the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public static Integer valueOf(String string) throws NumberFormatException {
		return new Integer(parseInt(string));
	}

	/**
	 * Parses the string argument as if it was an int value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * int quantity. The second argument specifies the radix to use when parsing
	 * the value.
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @param radix
	 *            the base to use for conversion.
	 * @return Integer the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public static Integer valueOf(String string, int radix)
			throws NumberFormatException {
		return new Integer(parseInt(string, radix));
	}
}
