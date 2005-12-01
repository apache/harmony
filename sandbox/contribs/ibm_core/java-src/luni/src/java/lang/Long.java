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
 * Integers are objects (i.e. non-base types) which represent long values.
 */
public final class Long extends Number implements Comparable {

	static final long serialVersionUID = 4290774380558885855L;

	/**
	 * The value which the receiver represents.
	 */
	final long value;

	/**
	 * Most positive and most negative possible long values.
	 */
	public static final long MAX_VALUE = 0x7FFFFFFFFFFFFFFFl;

	public static final long MIN_VALUE = 0x8000000000000000l;

	/**
	 * The java.lang.Class that represents this class.
	 */
	public static final Class TYPE = new long[0].getClass().getComponentType();

	// Note: This can't be set to "long.class", since *that* is
	// defined to be "java.lang.Long.TYPE";

	/**
	 * Constructs a new instance of the receiver which represents the int valued
	 * argument.
	 * 
	 * @param value
	 *            the long to store in the new instance.
	 */
	public Long(long value) {
		this.value = value;
	}

	/**
	 * Constructs a new instance of this class given a string.
	 * 
	 * @param string
	 *            a string representation of an long quantity.
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a long quantity.
	 */
	public Long(String string) throws NumberFormatException {
		this(parseLong(string));
	}

	/**
	 * Answers the byte value which the receiver represents
	 * 
	 * @return byte the value of the receiver.
	 */
	public byte byteValue() {
		return (byte) value;
	}

	public int compareTo(Long object) {
		return value > object.value ? 1 : (value < object.value ? -1 : 0);
	}

	public int compareTo(Object object) {
		return compareTo((Long) object);
	}

	/**
	 * Parses the string argument as if it was a long value and returns the
	 * result. Throws NumberFormatException if the string does not represent a
	 * long quantity. The string may be a hexadecimal ("0x..."), octal ("0..."),
	 * or decimal ("...") representation of a long.
	 * 
	 * @param string
	 *            a string representation of an int quantity.
	 * @return Long the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an int quantity.
	 */
	public static Long decode(String string) throws NumberFormatException {
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
				return new Long(0L);
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

		long result = parse(string, i, base, negative);
		return new Long(result);
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
	 * In this case, the argument must also be an Long, and the receiver and
	 * argument must represent the same long value.
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		return (o == this) || (o instanceof Long)
				&& (value == ((Long) o).value);
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
	 * Answers a Long representing the long value of the property named by the
	 * argument. If the property could not be found, or its value could not be
	 * parsed as a long, answer null.
	 * 
	 * @param string
	 *            The name of the desired integer property.
	 * @return Long A Long representing the value of the property.
	 */
	public static Long getLong(String string) {
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
	 * Answers a Long representing the long value of the property named by the
	 * argument. If the property could not be found, or its value could not be
	 * parsed as a long, answer a Long reperesenting the second argument.
	 * 
	 * @param string
	 *            The name of the desired long property.
	 * @return Long An Long representing the value of the property.
	 */
	public static Long getLong(String string, long defaultValue) {
		if (string == null || string.length() == 0)
			return new Long(defaultValue);
		String prop = System.getProperty(string);
		if (prop == null)
			return new Long(defaultValue);
		try {
			return decode(prop);
		} catch (NumberFormatException ex) {
			return new Long(defaultValue);
		}
	}

	/**
	 * Answers an Long representing the long value of the property named by the
	 * argument. If the property could not be found, or its value could not be
	 * parsed as an long, answer the second argument.
	 * 
	 * @param string
	 *            The name of the desired long property.
	 * @return Long An Long representing the value of the property.
	 */
	public static Long getLong(String string, Long defaultValue) {
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
		return (int) (value ^ (value >>> 32));
	}

	/**
	 * Answers the int value which the receiver represents
	 * 
	 * @return int the value of the receiver.
	 */
	public int intValue() {
		return (int) value;
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
	 * Parses the string argument as if it was a long value and returns the
	 * result. Throws NumberFormatException if the string does not represent a
	 * long quantity.
	 * 
	 * @param string
	 *            a string representation of a long quantity.
	 * @return long the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as a long quantity.
	 */
	public static long parseLong(String string) throws NumberFormatException {
		return parseLong(string, 10);
	}

	/**
	 * Parses the string argument as if it was an long value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * long quantity. The second argument specifies the radix to use when
	 * parsing the value.
	 * 
	 * @param string
	 *            a string representation of an long quantity.
	 * @param radix
	 *            the base to use for conversion.
	 * @return long the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an long quantity.
	 */
	public static long parseLong(String string, int radix)
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

	private static long parse(String string, int offset, int radix,
			boolean negative) {
		long max = Long.MIN_VALUE / radix;
		long result = 0, length = string.length();
		while (offset < length) {
			int digit = Character.digit(string.charAt(offset++), radix);
			if (digit == -1)
				throw new NumberFormatException(string);
			if (max > result)
				throw new NumberFormatException(string);
			long next = result * radix - digit;
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
	 * @param l
	 *            a long to get the binary representation of
	 * @return String the binary representation of the argument
	 */
	public static String toBinaryString(long l) {
		int count = 1;
		long j = l;

		if (l < 0)
			count = 64;
		else
			while ((j >>= 1) != 0)
				count++;

		char[] buffer = new char[count];
		do {
			buffer[--count] = (char) ((l & 1) + '0');
			l >>= 1;
		} while (count > 0);
		return new String(0, buffer.length, buffer);
	}

	/**
	 * Answers a string containing characters in the range 0..7, a..f which
	 * describe the hexadecimal representation of the argument.
	 * 
	 * @param l
	 *            a long to get the hex representation of
	 * @return String the hex representation of the argument
	 */
	public static String toHexString(long l) {
		int count = 1;
		long j = l;

		if (l < 0)
			count = 16;
		else
			while ((j >>= 4) != 0)
				count++;

		char[] buffer = new char[count];
		do {
			int t = (int) (l & 15);
			if (t > 9)
				t = t - 10 + 'a';
			else
				t += '0';
			buffer[--count] = (char) t;
			l >>= 4;
		} while (count > 0);
		return new String(0, buffer.length, buffer);
	}

	/**
	 * Answers a string containing characters in the range 0..7 which describe
	 * the octal representation of the argument.
	 * 
	 * @param l
	 *            a long to get the octal representation of
	 * @return String the octal representation of the argument
	 */
	public static String toOctalString(long l) {
		int count = 1;
		long j = l;

		if (l < 0)
			count = 22;
		else
			while ((j >>>= 3) != 0)
				count++;

		char[] buffer = new char[count];
		do {
			buffer[--count] = (char) ((l & 7) + '0');
			l >>>= 3;
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
		return Long.toString(value);
	}

	/**
	 * Answers a string containing characters in the range 0..9 which describe
	 * the decimal representation of the argument.
	 * 
	 * @param l
	 *            a long to get the representation of
	 * @return String the representation of the argument
	 */
	public static String toString(long l) {
		return toString(l, 10);
	}

	/**
	 * Answers a string containing characters in the range 0..9, a..z (depending
	 * on the radix) which describe the representation of the argument in that
	 * radix.
	 * 
	 * @param l
	 *            a long to get the representation of
	 * @param radix
	 *            the base to use for conversion.
	 * @return String the representation of the argument
	 */
	public static String toString(long l, int radix) {
		if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			radix = 10;
		if (l == 0)
			return "0";

		int count = 2;
		long j = l;
		boolean negative = l < 0;
		if (!negative) {
			count = 1;
			j = -l;
		}
		while ((l /= radix) != 0)
			count++;

		char[] buffer = new char[count];
		do {
			int ch = 0 - (int) (j % radix);
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
	 * Parses the string argument as if it was an long value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * long quantity.
	 * 
	 * @param string
	 *            a string representation of an long quantity.
	 * @return Long the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an long quantity.
	 */
	public static Long valueOf(String string) throws NumberFormatException {
		return new Long(parseLong(string));
	}

	/**
	 * Parses the string argument as if it was an long value and returns the
	 * result. Throws NumberFormatException if the string does not represent an
	 * long quantity. The second argument specifies the radix to use when
	 * parsing the value.
	 * 
	 * @param string
	 *            a string representation of an long quantity.
	 * @param radix
	 *            the base to use for conversion.
	 * @return Long the value represented by the argument
	 * @exception NumberFormatException
	 *                if the argument could not be parsed as an long quantity.
	 */
	public static Long valueOf(String string, int radix)
			throws NumberFormatException {
		return new Long(parseLong(string, radix));
	}
}
