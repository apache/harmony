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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The implementation of this class is provided, but the documented native must
 * be provided by the vm vendor.
 * 
 * Strings are objects which represent immutable arrays of characters.
 * 
 * @see StringBuffer
 */
public final class String implements Serializable, Comparable, CharSequence {
	static final long serialVersionUID = -6849794470754667710L;

	/**
	 * An PrintStream used for System.out which performs the 
	 * correct character conversion for the console, since the
	 * console may use a different conversion than the default
	 * file.encoding.
	 */
	static class ConsolePrintStream extends java.io.PrintStream {

		static {
			/**
			 * The encoding used for console conversions.
			 */
			String consoleEncoding = System.getProperty("console.encoding");
			if (consoleEncoding == null)
				consoleEncoding = "ISO8859_1";
		}

		/**
		 * Create a ConsolePrintStream on the specified OutputStream,
		 * usually System.out.
		 * 
		 * @param out the console OutputStream
		 */
		public ConsolePrintStream(java.io.OutputStream out) {
			super(out, true);

		}

		/**
		 * Override the print(String) method from PrintStream to perform
		 * the character conversion using the console character converter.
		 * 
		 * @param str the String to convert
		 */
		public void print(String str) {
			if (str == null)
				str = "null";

		}
	}

	/**
	 * CaseInsensitiveComparator compares Strings ignoring the case of the
	 * characters.
	 */
	private static final class CaseInsensitiveComparator implements Comparator,
			Serializable {
		static final long serialVersionUID = 8575799808933029326L;

		/**
		 * Compare the two objects to determine
		 * the relative ordering. 
		 *
		 * @param		o1	an Object to compare
		 * @param		o2	an Object to compare
		 * @return		an int < 0 if object1 is less than object2,
		 *				0 if they are equal, and > 0 if object1 is greater
		 *
		 * @exception	ClassCastException when objects are not the correct type
		 */
		public int compare(Object o1, Object o2) {
			return ((String) o1).compareToIgnoreCase((String) o2);
		}
	}

	/*
	 * A Comparator which compares Strings ignoring the case of the characters.
	 */
	public static final Comparator CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

	private static final char[] ascii;

	private final char[] value;

	private final int offset;

	private final int count;

	private int hashCode;

	static {
		ascii = new char[128];
		for (int i = 0; i < ascii.length; i++)
			ascii[i] = (char) i;
	}

	/**
	 * Answers an empty string.
	 */
	public String() {
		value = new char[0];
		offset = 0;
		count = 0;
	}

	private String(String s, char c) {
		offset = 0;
		value = new char[s.count + 1];
		count = s.count + 1;
		System.arraycopy(s.value, s.offset, value, 0, s.count);
		value[s.count] = c;
	}

	/**
	 * Converts the byte array to a String using the default encoding as
	 * specified by the file.encoding system property. If the system property is
	 * not defined, the default encoding is ISO8859_1 (ISO-Latin-1). If 8859-1
	 * is not available, an ASCII encoding is used.
	 * 
	 * @param data
	 *            the byte array to convert to a String
	 * 
	 * @throws NullPointerException
	 *             when data is null
	 * 
	 * @see #getBytes()
	 * @see #getBytes(int, int, byte[], int)
	 * @see #getBytes(String)
	 * @see #valueOf(boolean)
	 * @see #valueOf(char)
	 * @see #valueOf(char[])
	 * @see #valueOf(char[], int, int)
	 * @see #valueOf(double)
	 * @see #valueOf(float)
	 * @see #valueOf(int)
	 * @see #valueOf(long)
	 * @see #valueOf(Object)
	 * 
	 */
	public String(byte[] data) {
		this(data, 0, data.length);
	}

	/**
	 * Converts the byte array to a String, setting the high byte of every
	 * character to the specified value.
	 * 
	 * @param data
	 *            the byte array to convert to a String
	 * @param high
	 *            the high byte to use
	 * 
	 * @throws NullPointerException
	 *             when data is null
	 * 
	 * @deprecated Use String(byte[]) or String(byte[], String) instead
	 */
	public String(byte[] data, int high) {
		this(data, high, 0, data.length);
	}

	/**
	 * Converts the byte array to a String using the default encoding as
	 * specified by the file.encoding system property. If the system property is
	 * not defined, the default encoding is ISO8859_1 (ISO-Latin-1). If 8859-1
	 * is not available, an ASCII encoding is used.
	 * 
	 * @param data
	 *            the byte array to convert to a String
	 * @param start
	 *            the starting offset in the byte array
	 * @param length
	 *            the number of bytes to convert
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>length < 0, start < 0</code> or
	 *             <code>start + length > data.length</code>
	 * @throws NullPointerException
	 *             when data is null
	 * 
	 * @see #getBytes()
	 * @see #getBytes(int, int, byte[], int)
	 * @see #getBytes(String)
	 * @see #valueOf(boolean)
	 * @see #valueOf(char)
	 * @see #valueOf(char[])
	 * @see #valueOf(char[], int, int)
	 * @see #valueOf(double)
	 * @see #valueOf(float)
	 * @see #valueOf(int)
	 * @see #valueOf(long)
	 * @see #valueOf(Object)
	 * 
	 */
	public String(byte[] data, int start, int length) {
		value = new char[0];
		offset = 0;
		count = 0;
	}

	/**
	 * Converts the byte array to a String, setting the high byte of every
	 * character to the specified value.
	 * 
	 * @param data
	 *            the byte array to convert to a String
	 * @param high
	 *            the high byte to use
	 * @param start
	 *            the starting offset in the byte array
	 * @param length
	 *            the number of bytes to convert
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>length < 0, start < 0</code> or
	 *             <code>start + length > data.length</code>
	 * @throws NullPointerException
	 *             when data is null
	 * 
	 * @deprecated Use String(byte[], int, int) instead
	 */
	public String(byte[] data, int high, int start, int length) {
		if (data != null) {
			// start + length could overflow, start/length maybe MaxInt
			if (start >= 0 && 0 <= length && length <= data.length - start) {
				offset = 0;
				value = new char[length];
				count = length;
				high <<= 8;
				for (int i = 0; i < count; i++)
					value[i] = (char) (high + (data[start++] & 0xff));
			} else
				throw new StringIndexOutOfBoundsException();
		} else
			throw new NullPointerException();
	}

	/**
	 * Converts the byte array to a String using the specified encoding.
	 * 
	 * @param data
	 *            the byte array to convert to a String
	 * @param start
	 *            the starting offset in the byte array
	 * @param length
	 *            the number of bytes to convert
	 * @param encoding
	 *            the encoding
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>length < 0, start < 0</code> or
	 *             <code>start + length > data.length</code>
	 * @throws UnsupportedEncodingException
	 *             when encoding is not supported
	 * @throws NullPointerException
	 *             when data is null
	 * 
	 * @see #getBytes()
	 * @see #getBytes(int, int, byte[], int)
	 * @see #getBytes(String)
	 * @see #valueOf(boolean)
	 * @see #valueOf(char)
	 * @see #valueOf(char[])
	 * @see #valueOf(char[], int, int)
	 * @see #valueOf(double)
	 * @see #valueOf(float)
	 * @see #valueOf(int)
	 * @see #valueOf(long)
	 * @see #valueOf(Object)
	 * @see UnsupportedEncodingException
	 */
	public String(byte[] data, int start, int length, final String encoding)
			throws UnsupportedEncodingException {
		throw new UnsupportedEncodingException();
	}

	/**
	 * Converts the byte array to a String using the specified encoding.
	 * 
	 * @param data
	 *            the byte array to convert to a String
	 * @param encoding
	 *            the encoding
	 * 
	 * @throws UnsupportedEncodingException
	 *             when encoding is not supported
	 * @throws NullPointerException
	 *             when data is null
	 * 
	 * @see #getBytes()
	 * @see #getBytes(int, int, byte[], int)
	 * @see #getBytes(String)
	 * @see #valueOf(boolean)
	 * @see #valueOf(char)
	 * @see #valueOf(char[])
	 * @see #valueOf(char[], int, int)
	 * @see #valueOf(double)
	 * @see #valueOf(float)
	 * @see #valueOf(int)
	 * @see #valueOf(long)
	 * @see #valueOf(Object)
	 * @see UnsupportedEncodingException
	 */
	public String(byte[] data, String encoding)
			throws UnsupportedEncodingException {
		this(data, 0, data.length, encoding);
	}

	/**
	 * Initializes this String to contain the characters in the specified
	 * character array. Modifying the character array after creating the String
	 * has no effect on the String.
	 * 
	 * @param data
	 *            the array of characters
	 * 
	 * @throws NullPointerException
	 *             when data is null
	 */
	public String(char[] data) {
		this(data, 0, data.length);
	}

	/**
	 * Initializes this String to contain the specified characters in the
	 * character array. Modifying the character array after creating the String
	 * has no effect on the String.
	 * 
	 * @param data
	 *            the array of characters
	 * @param start
	 *            the starting offset in the character array
	 * @param length
	 *            the number of characters to use
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>length < 0, start < 0</code> or
	 *             <code>start + length > data.length</code>
	 * @throws NullPointerException
	 *             when data is null
	 */
	public String(char[] data, int start, int length) {
		// range check everything so a new char[] is not created
		// start + length could overflow, start/length maybe MaxInt
		if (start >= 0 && 0 <= length && length <= data.length - start) {
			offset = 0;
			value = new char[length];
			count = length;
			try {
				System.arraycopy(data, start, value, 0, count);
			} catch (IndexOutOfBoundsException e) {
				throw new StringIndexOutOfBoundsException();
			}
		} else
			throw new StringIndexOutOfBoundsException();
	}

	/*
	 * Internal version of string constructor. Does not range check, null check,
	 * or copy the character array.
	 */
	String(int start, int length, char[] data) {
		value = data;
		offset = start;
		count = length;
	}

	/**
	 * Creates a string that is a copy of another string
	 * 
	 * @param string
	 *            the String to copy
	 */
	public String(String string) {
		value = string.value;
		offset = string.offset;
		count = string.count;
	}

	/**
	 * Creates a string from the contents of a StringBuffer.
	 * 
	 * @param stringbuffer
	 *            the StringBuffer
	 */
	public String(StringBuffer stringbuffer) {
		value = new char[0];
		offset = 0;
		count = 0;
		synchronized (stringbuffer) {
		}
	}

	/*
	 * Creates a string that is s1 + v1.
	 */
	private String(String s1, int v1) {
		if (s1 == null)
			s1 = "null";
		String s2 = String.valueOf(v1);
		int len = s1.count + s2.count;
		value = new char[len];
		offset = 0;
		System.arraycopy(s1.value, s1.offset, value, 0, s1.count);
		System.arraycopy(s2.value, s2.offset, value, s1.count, s2.count);
		count = len;
	}

	/**
	 * Answers the character at the specified offset in this String.
	 * 
	 * @param index
	 *            the zero-based index in this string
	 * @return the character at the index
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>index < 0</code> or
	 *             <code>index >= length()</code>
	 */
	public char charAt(int index) {
		if (0 <= index && index < count)
			return value[offset + index];
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Compare the receiver to the specified Object to determine the relative
	 * ordering.
	 * 
	 * @param object
	 *            an Object
	 * @return an int < 0 if this String is less than the specified String, 0 if
	 *         they are equal, and > 0 if this String is greater
	 * 
	 * @throws ClassCastException
	 *             when object is not a String
	 */
	public int compareTo(Object object) {
		return compareTo((String) object);
	}

	/**
	 * Compares the specified String to this String using the Unicode values of
	 * the characters. Answer 0 if the strings contain the same characters in
	 * the same order. Answer a negative integer if the first non-equal
	 * character in this String has a Unicode value which is less than the
	 * Unicode value of the character at the same position in the specified
	 * string, or if this String is a prefix of the specified string. Answer a
	 * positive integer if the first non-equal character in this String has a
	 * Unicode value which is greater than the Unicode value of the character at
	 * the same position in the specified string, or if the specified String is
	 * a prefix of the this String.
	 * 
	 * @param string
	 *            the string to compare
	 * @return 0 if the strings are equal, a negative integer if this String is
	 *         before the specified String, or a positive integer if this String
	 *         is after the specified String
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 */
	public int compareTo(String string) {
		// Code adapted from K&R, pg 101
		int o1 = offset, o2 = string.offset, result;
		int end = offset + (count < string.count ? count : string.count);
		char[] target = string.value;
		while (o1 < end)
			if ((result = value[o1++] - target[o2++]) != 0)
				return result;
		return count - string.count;
	}

	/**
	 * Compare the receiver to the specified String to determine the relative
	 * ordering when the case of the characters is ignored.
	 * 
	 * @param string
	 *            a String
	 * @return an int < 0 if this String is less than the specified String, 0 if
	 *         they are equal, and > 0 if this String is greater
	 */
	public int compareToIgnoreCase(String string) {
		int o1 = offset, o2 = string.offset, result;
		int end = offset + (count < string.count ? count : string.count);
		char c1, c2;
		char[] target = string.value;
		while (o1 < end) {
			if ((c1 = value[o1++]) == (c2 = target[o2++]))
				continue;
			c1 = Character.toLowerCase(Character.toUpperCase(c1));
			c2 = Character.toLowerCase(Character.toUpperCase(c2));
			if ((result = c1 - c2) != 0)
				return result;
		}
		return count - string.count;
	}

	/**
	 * Concatenates this String and the specified string.
	 * 
	 * @param string
	 *            the string to concatenate
	 * @return a new String which is the concatenation of this String and the
	 *         specified String
	 * 
	 * @throws NullPointerException
	 *             if string is null
	 */
	public String concat(String string) {
		if (string.count > 0 && count > 0) {
			char[] buffer = new char[count + string.count];
			System.arraycopy(value, offset, buffer, 0, count);
			System.arraycopy(string.value, string.offset, buffer, count,
					string.count);
			return new String(0, buffer.length, buffer);
		}
		return count == 0 ? string : this;
	}

	/**
	 * Creates a new String containing the characters in the specified character
	 * array. Modifying the character array after creating the String has no
	 * effect on the String.
	 * 
	 * @param data
	 *            the array of characters
	 * @return the new String
	 * 
	 * @throws NullPointerException
	 *             if data is null
	 */
	public static String copyValueOf(char[] data) {
		return new String(data, 0, data.length);
	}

	/**
	 * Creates a new String containing the specified characters in the character
	 * array. Modifying the character array after creating the String has no
	 * effect on the String.
	 * 
	 * @param data
	 *            the array of characters
	 * @param start
	 *            the starting offset in the character array
	 * @param length
	 *            the number of characters to use
	 * @return the new String
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if <code>length < 0, start < 0</code> or
	 *             <code>start + length > data.length</code>
	 * @throws NullPointerException
	 *             if data is null
	 */
	public static String copyValueOf(char[] data, int start, int length) {
		return new String(data, start, length);
	}

	/**
	 * Compares the specified string to this String to determine if the
	 * specified string is a suffix.
	 * 
	 * @param suffix
	 *            the string to look for
	 * @return true when the specified string is a suffix of this String, false
	 *         otherwise
	 * 
	 * @throws NullPointerException
	 *             if suffix is null
	 */
	public boolean endsWith(String suffix) {
		return regionMatches(count - suffix.count, suffix, 0, suffix.count);
	}

	/**
	 * Compares the specified object to this String and answer if they are
	 * equal. The object must be an instance of String with the same characters
	 * in the same order.
	 * 
	 * @param object
	 *            the object to compare
	 * @return true if the specified object is equal to this String, false
	 *         otherwise
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		if (object == this)
			return true;
		if (object instanceof String) {
			String s = (String) object;
			if (count != s.count
					|| (hashCode != s.hashCode && hashCode != 0 && s.hashCode != 0))
				return false;
			return regionMatches(0, s, 0, count);
		}
		return false;
	}

	/**
	 * Compares the specified String to this String ignoring the case of the
	 * characters and answer if they are equal.
	 * 
	 * @param string
	 *            the string to compare
	 * @return true if the specified string is equal to this String, false
	 *         otherwise
	 */
	public boolean equalsIgnoreCase(String string) {
		if (string == this)
			return true;
		if (string == null || count != string.count)
			return false;

		int o1 = offset, o2 = string.offset;
		int end = offset + count;
		char c1, c2;
		char[] target = string.value;
		while (o1 < end) {
			if ((c1 = value[o1++]) != (c2 = target[o2++])
					&& Character.toUpperCase(c1) != Character.toUpperCase(c2)
					// Required for unicode that we test both cases
					&& Character.toLowerCase(c1) != Character.toLowerCase(c2))
				return false;
		}
		return true;
	}

	/**
	 * Converts this String to a byte encoding using the default encoding as
	 * specified by the file.encoding sytem property. If the system property is
	 * not defined, the default encoding is ISO8859_1 (ISO-Latin-1). If 8859-1
	 * is not available, an ASCII encoding is used.
	 * 
	 * @return the byte array encoding of this String
	 * 
	 * @see String
	 */
	public byte[] getBytes() {
		return null;
	}

	/**
	 * Converts this String to a byte array, ignoring the high order bits of
	 * each character.
	 * 
	 * @param start
	 *            the starting offset of characters to copy
	 * @param end
	 *            the ending offset of characters to copy
	 * @param data
	 *            the destination byte array
	 * @param index
	 *            the starting offset in the byte array
	 * 
	 * @throws NullPointerException
	 *             when data is null
	 * @throws IndexOutOfBoundsException
	 *             when
	 *             <code>start < 0, end > length(), index < 0, end - start > data.length - index</code>
	 * 
	 * @deprecated Use getBytes() or getBytes(String)
	 */
	public void getBytes(int start, int end, byte[] data, int index) {
		if (data != null) {
			// index < 0, and end - start > data.length - index are caught by
			// the catch below
			// end > count not caught below when start == end
			if (0 <= start && start <= end && end <= count) {
				end += offset;
				try {
					for (int i = offset + start; i < end; i++)
						data[index++] = (byte) value[i];
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new StringIndexOutOfBoundsException();
				}
			} else
				throw new StringIndexOutOfBoundsException();
		} else
			throw new NullPointerException();
	}

	/**
	 * Converts this String to a byte encoding using the specified encoding.
	 * 
	 * @param encoding
	 *            the encoding
	 * @return the byte array encoding of this String
	 * 
	 * @throws UnsupportedEncodingException
	 *             when the encoding is not supported
	 * 
	 * @see String
	 * @see UnsupportedEncodingException
	 */
	public byte[] getBytes(String encoding) throws UnsupportedEncodingException {
		return null;
	}

	/**
	 * Copies the specified characters in this String to the character array
	 * starting at the specified offset in the character array.
	 * 
	 * @param start
	 *            the starting offset of characters to copy
	 * @param end
	 *            the ending offset of characters to copy
	 * @param buffer
	 *            the destination character array
	 * @param index
	 *            the starting offset in the character array
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>start < 0, end > length(),
	 *				start > end, index < 0, end - start > buffer.length - index</code>
	 * @throws NullPointerException
	 *             when buffer is null
	 */
	public void getChars(int start, int end, char[] buffer, int index) {
		// NOTE last character not copied!
		// Fast range check.
		if (0 <= start && start <= end && end <= count)
			System.arraycopy(value, start + offset, buffer, index, end - start);
		else
			throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Answers an integer hash code for the receiver. Objects which are equal
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		if (hashCode == 0) {
			int hash = 0, multiplier = 1;
			for (int i = offset + count - 1; i >= offset; i--) {
				hash += value[i] * multiplier;
				int shifted = multiplier << 5;
				multiplier = shifted - multiplier;
			}
			hashCode = hash;
		}
		return hashCode;
	}

	/**
	 * Searches in this String for the first index of the specified character.
	 * The search for the character starts at the beginning and moves towards
	 * the end of this String.
	 * 
	 * @param c
	 *            the character to find
	 * @return the index in this String of the specified character, -1 if the
	 *         character isn't found
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int indexOf(int c) {
		return indexOf(c, 0);
	}

	/**
	 * Searches in this String for the index of the specified character. The
	 * search for the character starts at the specified offset and moves towards
	 * the end of this String.
	 * 
	 * @param c
	 *            the character to find
	 * @param start
	 *            the starting offset
	 * @return the index in this String of the specified character, -1 if the
	 *         character isn't found
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int indexOf(int c, int start) {
		if (start < count) {
			if (start < 0)
				start = 0;
			for (int i = offset + start; i < offset + count; i++) {
				if (value[i] == c)
					return i - offset;
			}
		}
		return -1;
	}

	/**
	 * Searches in this String for the first index of the specified string. The
	 * search for the string starts at the beginning and moves towards the end
	 * of this String.
	 * 
	 * @param string
	 *            the string to find
	 * @return the index in this String of the specified string, -1 if the
	 *         string isn't found
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 * 
	 */
	public int indexOf(String string) {
		return indexOf(string, 0);
	}

	/**
	 * Searches in this String for the index of the specified string. The search
	 * for the string starts at the specified offset and moves towards the end
	 * of this String.
	 * 
	 * @param subString
	 *            the string to find
	 * @param start
	 *            the starting offset
	 * @return the index in this String of the specified string, -1 if the
	 *         string isn't found
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int indexOf(String subString, int start) {
		if (start < 0)
			start = 0;
		int subCount = subString.count;
		if (subCount > 0) {
			if (subCount + start > count)
				return -1;
			char[] target = subString.value;
			int subOffset = subString.offset;
			char firstChar = target[subOffset];
			int end = subOffset + subCount;
			while (true) {
				int i = indexOf(firstChar, start);
				if (i == -1 || subCount + i > count)
					return -1; // handles subCount > count || start >= count
				int o1 = offset + i, o2 = subOffset;
				while (++o2 < end && value[++o1] == target[o2]) {
					// Intentionally empty
				}
				if (o2 == end) {
					return i;
				}
				start = i + 1;
			}
		}
		return start < count ? start : count;
	}

	/**
	 * Only this native must be implemented, the implementation for the rest of
	 * this class is provided.
	 * 
	 * Searches an internal table of strings for a string equal to this String.
	 * If the string is not in the table, it is added. Answers the string
	 * contained in the table which is equal to this String. The same string
	 * object is always answered for strings which are equal.
	 * 
	 * @return the interned string equal to this String
	 */
	public native String intern();

	/**
	 * Searches in this String for the last index of the specified character.
	 * The search for the character starts at the end and moves towards the
	 * beginning of this String.
	 * 
	 * @param c
	 *            the character to find
	 * @return the index in this String of the specified character, -1 if the
	 *         character isn't found
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int lastIndexOf(int c) {
		return lastIndexOf(c, count - 1);
	}

	/**
	 * Searches in this String for the index of the specified character. The
	 * search for the character starts at the specified offset and moves towards
	 * the beginning of this String.
	 * 
	 * @param c
	 *            the character to find
	 * @param start
	 *            the starting offset
	 * @return the index in this String of the specified character, -1 if the
	 *         character isn't found
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int lastIndexOf(int c, int start) {
		if (start >= 0) {
			if (start >= count)
				start = count - 1;
			for (int i = offset + start; i >= offset; --i) {
				if (value[i] == c)
					return i - offset;
			}
		}
		return -1;
	}

	/**
	 * Searches in this String for the last index of the specified string. The
	 * search for the string starts at the end and moves towards the beginning
	 * of this String.
	 * 
	 * @param string
	 *            the string to find
	 * @return the index in this String of the specified string, -1 if the
	 *         string isn't found
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int lastIndexOf(String string) {
		// Use count instead of count - 1 so lastIndexOf("") answers count
		return lastIndexOf(string, count);
	}

	/**
	 * Searches in this String for the index of the specified string. The search
	 * for the string starts at the specified offset and moves towards the
	 * beginning of this String.
	 * 
	 * @param subString
	 *            the string to find
	 * @param start
	 *            the starting offset
	 * @return the index in this String of the specified string, -1 if the
	 *         string isn't found
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 * 
	 * @see #lastIndexOf(int)
	 * @see #lastIndexOf(int, int)
	 * @see #lastIndexOf(String)
	 * @see #lastIndexOf(String, int)
	 */
	public int lastIndexOf(String subString, int start) {
		int subCount = subString.count;
		if (subCount <= count && start >= 0) {
			if (subCount > 0) {
				if (start > count - subCount)
					start = count - subCount; // count and subCount are both
				// >= 1
				char[] target = subString.value;
				int subOffset = subString.offset;
				char firstChar = target[subOffset];
				int end = subOffset + subCount;
				while (true) {
					int i = lastIndexOf(firstChar, start);
					if (i == -1)
						return -1;
					int o1 = offset + i, o2 = subOffset;
					while (++o2 < end && value[++o1] == target[o2]) {
						// Intentionally empty
					}
					if (o2 == end) {
						return i;
					}
					start = i - 1;
				}
			}
			return start < count ? start : count;
		}
		return -1;
	}

	/**
	 * Answers the size of this String.
	 * 
	 * @return the number of characters in this String
	 */
	public int length() {
		return count;
	}

	/**
	 * Compares the specified string to this String and compares the specified
	 * range of characters to determine if they are the same.
	 * 
	 * @param thisStart
	 *            the starting offset in this String
	 * @param string
	 *            the string to compare
	 * @param start
	 *            the starting offset in string
	 * @param length
	 *            the number of characters to compare
	 * @return true if the ranges of characters is equal, false otherwise
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 */
	public boolean regionMatches(int thisStart, String string, int start,
			int length) {
		if (string == null)
			throw new NullPointerException();
		if (start < 0 || string.count - start < length)
			return false;
		if (thisStart < 0 || count - thisStart < length)
			return false;
		if (length <= 0)
			return true;
		int o1 = offset + thisStart, o2 = string.offset + start;
		for (int i = 0; i < length; ++i) {
			if (value[o1 + i] != string.value[o2 + i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the specified string to this String and compares the specified
	 * range of characters to determine if they are the same. When ignoreCase is
	 * true, the case of the characters is ignored during the comparison.
	 * 
	 * @param ignoreCase
	 *            specifies if case should be ignored
	 * @param thisStart
	 *            the starting offset in this String
	 * @param string
	 *            the string to compare
	 * @param start
	 *            the starting offset in string
	 * @param length
	 *            the number of characters to compare
	 * @return true if the ranges of characters is equal, false otherwise
	 * 
	 * @throws NullPointerException
	 *             when string is null
	 */
	public boolean regionMatches(boolean ignoreCase, int thisStart,
			String string, int start, int length) {
		if (!ignoreCase)
			return regionMatches(thisStart, string, start, length);

		if (string != null) {
			if (thisStart < 0 || length > count - thisStart)
				return false;
			if (start < 0 || length > string.count - start)
				return false;

			thisStart += offset;
			start += string.offset;
			int end = thisStart + length;
			char c1, c2;
			char[] target = string.value;
			while (thisStart < end) {
				if ((c1 = value[thisStart++]) != (c2 = target[start++])
						&& Character.toUpperCase(c1) != Character
								.toUpperCase(c2)
						// Required for unicode that we test both cases
						&& Character.toLowerCase(c1) != Character
								.toLowerCase(c2)) {
					return false;
				}
			}
			return true;
		}
		throw new NullPointerException();
	}

	/**
	 * Copies this String replacing occurrences of the specified character with
	 * another character.
	 * 
	 * @param oldChar
	 *            the character to replace
	 * @param newChar
	 *            the replacement character
	 * @return a new String with occurrences of oldChar replaced by newChar
	 */
	public String replace(char oldChar, char newChar) {
		int index = indexOf(oldChar, 0);
		if (index == -1)
			return this;

		char[] buffer = new char[count];
		System.arraycopy(value, offset, buffer, 0, count);
		do {
			buffer[index++] = newChar;
		} while ((index = indexOf(oldChar, index)) != -1);
		return new String(0, count, buffer);
	}

	/**
	 * Compares the specified string to this String to determine if the
	 * specified string is a prefix.
	 * 
	 * @param prefix
	 *            the string to look for
	 * @return true when the specified string is a prefix of this String, false
	 *         otherwise
	 * 
	 * @throws NullPointerException
	 *             when prefix is null
	 */
	public boolean startsWith(String prefix) {
		return startsWith(prefix, 0);
	}

	/**
	 * Compares the specified string to this String, starting at the specified
	 * offset, to determine if the specified string is a prefix.
	 * 
	 * @param prefix
	 *            the string to look for
	 * @param start
	 *            the starting offset
	 * @return true when the specified string occurs in this String at the
	 *         specified offset, false otherwise
	 * 
	 * @throws NullPointerException
	 *             when prefix is null
	 */
	public boolean startsWith(String prefix, int start) {
		return regionMatches(start, prefix, 0, prefix.count);
	}

	/**
	 * Copies a range of characters into a new String.
	 * 
	 * @param start
	 *            the offset of the first character
	 * @return a new String containing the characters from start to the end of
	 *         the string
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>start < 0</code> or
	 *             <code>start > length()</code>
	 */
	public String substring(int start) {
		if (0 <= start && start <= count)
			return new String(offset + start, count - start, value);
		throw new StringIndexOutOfBoundsException(start);
	}

	/**
	 * Copies a range of characters into a new String.
	 * 
	 * @param start
	 *            the offset of the first character
	 * @param end
	 *            the offset one past the last character
	 * @return a new String containing the characters from start to end - 1
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>start < 0, start > end</code> or
	 *             <code>end > length()</code>
	 */
	public String substring(int start, int end) {
		// NOTE last character not copied!
		// Fast range check.
		if (0 <= start && start <= end && end <= count)
			return new String(offset + start, end - start, value);
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Copies the characters in this String to a character array.
	 * 
	 * @return a character array containing the characters of this String
	 */
	public char[] toCharArray() {
		char[] buffer = new char[count];
		System.arraycopy(value, offset, buffer, 0, count);
		return buffer;
	}

	/**
	 * Converts the characters in this String to lowercase, using the default
	 * Locale.
	 * 
	 * @return a new String containing the lowercase characters equivalent to
	 *         the characters in this String
	 */
	public String toLowerCase() {
		return toLowerCase(Locale.getDefault());
	}

	/**
	 * Converts the characters in this String to lowercase, using the specified
	 * Locale.
	 * 
	 * @param locale
	 *            the Locale
	 * @return a new String containing the lowercase characters equivalent to
	 *         the characters in this String
	 */
	public String toLowerCase(Locale locale) {
		for (int o = offset, end = offset + count; o < end; o++) {
			char ch = value[o];
			if (ch != Character.toLowerCase(ch)) {
				char[] buffer = new char[count];
				int i = o - offset;
				System.arraycopy(value, offset, buffer, 0, i); // not worth
				// checking for
				// i == 0 case
				if (locale.getLanguage() != "tr") { // Turkish
					while (i < count)
						buffer[i++] = Character.toLowerCase(value[o++]);
				} else {
					while (i < count)
						buffer[i++] = (ch = value[o++]) != 0x49 ? Character
								.toLowerCase(ch) : (char) 0x131;
				}
				return new String(0, count, buffer);
			}
		}
		return this;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return this String
	 */
	public String toString() {
		return this;
	}

	/**
	 * Converts the characters in this String to uppercase, using the default
	 * Locale.
	 * 
	 * @return a new String containing the uppercase characters equivalent to
	 *         the characters in this String
	 */
	public String toUpperCase() {
		return toUpperCase(Locale.getDefault());
	}

	private static final char[] upperValues = "SS\u0000\u02bcN\u0000J\u030c\u0000\u0399\u0308\u0301\u03a5\u0308\u0301\u0535\u0552\u0000H\u0331\u0000T\u0308\u0000W\u030a\u0000Y\u030a\u0000A\u02be\u0000\u03a5\u0313\u0000\u03a5\u0313\u0300\u03a5\u0313\u0301\u03a5\u0313\u0342\u1f08\u0399\u0000\u1f09\u0399\u0000\u1f0a\u0399\u0000\u1f0b\u0399\u0000\u1f0c\u0399\u0000\u1f0d\u0399\u0000\u1f0e\u0399\u0000\u1f0f\u0399\u0000\u1f08\u0399\u0000\u1f09\u0399\u0000\u1f0a\u0399\u0000\u1f0b\u0399\u0000\u1f0c\u0399\u0000\u1f0d\u0399\u0000\u1f0e\u0399\u0000\u1f0f\u0399\u0000\u1f28\u0399\u0000\u1f29\u0399\u0000\u1f2a\u0399\u0000\u1f2b\u0399\u0000\u1f2c\u0399\u0000\u1f2d\u0399\u0000\u1f2e\u0399\u0000\u1f2f\u0399\u0000\u1f28\u0399\u0000\u1f29\u0399\u0000\u1f2a\u0399\u0000\u1f2b\u0399\u0000\u1f2c\u0399\u0000\u1f2d\u0399\u0000\u1f2e\u0399\u0000\u1f2f\u0399\u0000\u1f68\u0399\u0000\u1f69\u0399\u0000\u1f6a\u0399\u0000\u1f6b\u0399\u0000\u1f6c\u0399\u0000\u1f6d\u0399\u0000\u1f6e\u0399\u0000\u1f6f\u0399\u0000\u1f68\u0399\u0000\u1f69\u0399\u0000\u1f6a\u0399\u0000\u1f6b\u0399\u0000\u1f6c\u0399\u0000\u1f6d\u0399\u0000\u1f6e\u0399\u0000\u1f6f\u0399\u0000\u1fba\u0399\u0000\u0391\u0399\u0000\u0386\u0399\u0000\u0391\u0342\u0000\u0391\u0342\u0399\u0391\u0399\u0000\u1fca\u0399\u0000\u0397\u0399\u0000\u0389\u0399\u0000\u0397\u0342\u0000\u0397\u0342\u0399\u0397\u0399\u0000\u0399\u0308\u0300\u0399\u0308\u0301\u0399\u0342\u0000\u0399\u0308\u0342\u03a5\u0308\u0300\u03a5\u0308\u0301\u03a1\u0313\u0000\u03a5\u0342\u0000\u03a5\u0308\u0342\u1ffa\u0399\u0000\u03a9\u0399\u0000\u038f\u0399\u0000\u03a9\u0342\u0000\u03a9\u0342\u0399\u03a9\u0399\u0000FF\u0000FI\u0000FL\u0000FFIFFLST\u0000ST\u0000\u0544\u0546\u0000\u0544\u0535\u0000\u0544\u053b\u0000\u054e\u0546\u0000\u0544\u053d\u0000".value;

	/**
	 * Return the index of the specified character into the upperValues table.
	 * The upperValues table contains three entries at each position. These
	 * three characters are the upper case conversion. If only two characters
	 * are used, the third character in the table is \u0000.
	 * 
	 * @param ch the char being converted to upper case
	 * 
	 * @return the index into the upperValues table, or -1
	 */
	private int upperIndex(int ch) {
		int index = -1;
		if (ch >= 0xdf) {
			if (ch <= 0x587) {
				if (ch == 0xdf)
					index = 0;
				else if (ch <= 0x149) {
					if (ch == 0x149)
						index = 1;
				} else if (ch <= 0x1f0) {
					if (ch == 0x1f0)
						index = 2;
				} else if (ch <= 0x390) {
					if (ch == 0x390)
						index = 3;
				} else if (ch <= 0x3b0) {
					if (ch == 0x3b0)
						index = 4;
				} else if (ch <= 0x587) {
					if (ch == 0x587)
						index = 5;
				}
			} else if (ch >= 0x1e96) {
				if (ch <= 0x1e9a)
					index = 6 + ch - 0x1e96;
				else if (ch >= 0x1f50 && ch <= 0x1ffc) {
					index = "\u000b\u0000\f\u0000\r\u0000\u000e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&'()*+,-./0123456789:;<=>\u0000\u0000?@A\u0000BC\u0000\u0000\u0000\u0000D\u0000\u0000\u0000\u0000\u0000EFG\u0000HI\u0000\u0000\u0000\u0000J\u0000\u0000\u0000\u0000\u0000KL\u0000\u0000MN\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000OPQ\u0000RS\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TUV\u0000WX\u0000\u0000\u0000\u0000Y".value[ch - 0x1f50];
					if (index == 0)
						index = -1;
				} else if (ch >= 0xfb00) {
					if (ch <= 0xfb06)
						index = 90 + ch - 0xfb00;
					else if (ch >= 0xfb13 && ch <= 0xfb17)
						index = 97 + ch - 0xfb13;
				}
			}
		}
		return index;
	}

	/**
	 * Converts the characters in this String to uppercase, using the specified
	 * Locale.
	 * 
	 * @param locale
	 *            the Locale
	 * @return a new String containing the uppercase characters equivalent to
	 *         the characters in this String
	 */
	public String toUpperCase(Locale locale) {
		boolean turkish = "tr".equals(locale.getLanguage());
		char[] output = null;
		int i = 0;
		for (int o = offset, end = offset + count; o < end; o++) {
			char ch = value[o];
			int index = upperIndex(ch);
			if (index == -1) {
				if (output != null && i >= output.length) {
					char[] newoutput = new char[output.length + (count / 6) + 2];
					System.arraycopy(output, 0, newoutput, 0, output.length);
					output = newoutput;
				}
				char upch = !turkish ? Character.toUpperCase(ch)
						: (ch != 0x69 ? Character.toUpperCase(ch)
								: (char) 0x130);
				if (ch != upch) {
					if (output == null) {
						output = new char[count];
						i = o - offset;
						System.arraycopy(value, offset, output, 0, i);

					}
					output[i++] = upch;
				} else if (output != null)
					output[i++] = ch;
			} else {
				int target = index * 3;
				char val3 = upperValues[target + 2];
				if (output == null) {
					output = new char[count + (count / 6) + 2];
					i = o - offset;
					System.arraycopy(value, offset, output, 0, i);
				} else if (i + (val3 == 0 ? 1 : 2) >= output.length) {
					char[] newoutput = new char[output.length + (count / 6) + 3];
					System.arraycopy(output, 0, newoutput, 0, output.length);
					output = newoutput;
				}

				char val = upperValues[target];
				output[i++] = val;
				val = upperValues[target + 1];
				output[i++] = val;
				if (val3 != 0)
					output[i++] = val3;
			}
		}
		if (output == null)
			return this;
		return output.length == i || output.length - i < 8 ? new String(0, i,
				output) : new String(output, 0, i);
	}

	/**
	 * Copies this String removing white space characters from the beginning and
	 * end of the string.
	 * 
	 * @return a new String with characters <code><= \\u0020</code> removed
	 *         from the beginning and the end
	 */
	public String trim() {
		int start = offset, last = offset + count - 1;
		int end = last;
		while ((start <= end) && (value[start] <= ' '))
			start++;
		while ((end >= start) && (value[end] <= ' '))
			end--;
		if (start == offset && end == last)
			return this;
		return new String(start, end - start + 1, value);
	}

	/**
	 * Creates a new String containing the characters in the specified character
	 * array. Modifying the character array after creating the String has no
	 * effect on the String.
	 * 
	 * @param data
	 *            the array of characters
	 * @return the new String
	 * 
	 * @throws NullPointerException
	 *             when data is null
	 */
	public static String valueOf(char[] data) {
		return new String(data, 0, data.length);
	}

	/**
	 * Creates a new String containing the specified characters in the character
	 * array. Modifying the character array after creating the String has no
	 * effect on the String.
	 * 
	 * @param data
	 *            the array of characters
	 * @param start
	 *            the starting offset in the character array
	 * @param length
	 *            the number of characters to use
	 * @return the new String
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when <code>length < 0, start < 0</code> or
	 *             <code>start + length > data.length</code>
	 * @throws NullPointerException
	 *             when data is null
	 */
	public static String valueOf(char[] data, int start, int length) {
		return new String(data, start, length);
	}

	/**
	 * Converts the specified character to its string representation.
	 * 
	 * @param value
	 *            the character
	 * @return the character converted to a string
	 */
	public static String valueOf(char value) {
		String s;
		if (value < 128)
			s = new String(value, 1, ascii);
		else
			s = new String(0, 1, new char[] { value });
		s.hashCode = value;
		return s;
	}

	/**
	 * Converts the specified double to its string representation.
	 * 
	 * @param value
	 *            the double
	 * @return the double converted to a string
	 */
	public static String valueOf(double value) {
		return Double.toString(value);
	}

	/**
	 * Converts the specified float to its string representation.
	 * 
	 * @param value
	 *            the float
	 * @return the float converted to a string
	 */
	public static String valueOf(float value) {
		return Float.toString(value);
	}

	/**
	 * Converts the specified integer to its string representation.
	 * 
	 * @param value
	 *            the integer
	 * @return the integer converted to a string
	 */
	public static String valueOf(int value) {
		return Integer.toString(value);
	}

	/**
	 * Converts the specified long to its string representation.
	 * 
	 * @param value
	 *            the long
	 * @return the long converted to a string
	 */
	public static String valueOf(long value) {
		return Long.toString(value);
	}

	/**
	 * Converts the specified object to its string representation. If the object
	 * is null answer the string <code>"null"</code>, otherwise use
	 * <code>toString()</code> to get the string representation.
	 * 
	 * @param value
	 *            the object
	 * @return the object converted to a string
	 */
	public static String valueOf(Object value) {
		return value != null ? value.toString() : "null";
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
	public static String valueOf(boolean value) {
		return value ? "true" : "false";
	}

	/**
	 * Answers whether the characters in the StringBuffer strbuf are the same as
	 * those in this String.
	 * 
	 * @param strbuf
	 *            the StringBuffer to compare this String to
	 * @return true when the characters in strbuf are identical to those in this
	 *         String. If they are not, false will be returned.
	 * 
	 * @throws NullPointerException
	 *             when strbuf is null
	 * 
	 * @since 1.4
	 */
	public boolean contentEquals(StringBuffer strbuf) {
		synchronized (strbuf) {
			int size = strbuf.length();
			if (count != size)
				return false;
			return regionMatches(0, new String(0, size, strbuf.getValue()), 0,
					size);
		}
	}

	/**
	 * Determines whether a this String matches a given regular expression.
	 * 
	 * @param expr
	 *            the regular expression to be matched
	 * @return true if the expression matches, otherwise false
	 * 
	 * @throws PatternSyntaxException
	 *             if the syntax of the supplied regular expression is not valid
	 * @throws NullPointerException
	 *             if expr is null
	 * 
	 * @since 1.4
	 */
	public boolean matches(String expr) {
		return Pattern.matches(expr, this);
	}

	/**
	 * Replace any substrings within this String that match the supplied regular
	 * expression expr, with the String substitute.
	 * 
	 * @param expr
	 *            the regular expression to match
	 * @param substitute
	 *            the string to replace the matching substring with
	 * @return the new string
	 * 
	 * @throws NullPointerException
	 *             if expr is null
	 * 
	 * @since 1.4
	 */
	public String replaceAll(String expr, String substitute) {
		return Pattern.compile(expr).matcher(this).replaceAll(substitute);
	}

	/**
	 * Replace any substrings within this String that match the supplied regular
	 * expression expr, with the String substitute.
	 * 
	 * @param expr
	 *            the regular expression to match
	 * @param substitute
	 *            the string to replace the matching substring with
	 * @return the new string
	 * 
	 * @throws NullPointerException
	 *             if expr is null
	 * 
	 * @since 1.4
	 */
	public String replaceFirst(String expr, String substitute) {
		return Pattern.compile(expr).matcher(this).replaceFirst(substitute);
	}

	/**
	 * Replace any substrings within this String that match the supplied regular
	 * expression expr, with the String substitute.
	 * 
	 * @param expr
	 *            the regular expression to match
	 * @return the new string
	 * 
	 * @throws NullPointerException
	 *             if expr is null
	 * 
	 * @since 1.4
	 */
	public String[] split(String expr) {
		return Pattern.compile(expr).split(this);
	}

	/**
	 * Splits this String using the supplied regular expression expr. max
	 * controls the number of times that the pattern is applied to the string.
	 * 
	 * @param expr
	 *            the regular expression used to divide the string
	 * @param max
	 *            the number of times to apply the pattern
	 * @return an array of Strings created by separating the string along
	 *         matches of the regular expression.
	 * 
	 * @throws NullPointerException
	 *             if expr is null
	 * 
	 * @since 1.4
	 */
	public String[] split(String expr, int max) {
		return Pattern.compile(expr).split(this, max);
	}

	/**
	 * Has the same result as the substring function, but is present so that
	 * String may implement the CharSequence interface.
	 * 
	 * @param start
	 *            the offset the first character
	 * @param end
	 *            the offset of one past the last character to include
	 * 
	 * @return the subsequence requested
	 * 
	 * @throws IndexOutOfBoundsException
	 *             when start or end is less than zero, start is greater than
	 *             end, or end is greater than the length of the String.
	 * 
	 * @see java.lang.CharSequence#subSequence(int, int)
	 * 
	 * @since 1.4
	 */
	public CharSequence subSequence(int start, int end) {
		return substring(start, end);
	}

	/*
	 * An implementation of a String.indexOf that is supposed to perform
	 * substantially better than the default algorithm if the the "needle" (the
	 * subString being searched for) is a constant string.
	 * 
	 * In the jit, if we encounter a call to String.indexOf(String), where the
	 * needle is a constant string, we compute the values cache, md2 and
	 * lastChar, and change the call to the following method. This code can be
	 * enabled by setting TR_FastIndexOf=1. It searches for the availablility of
	 * the following signature before doing the optimization.
	 */
	private static int indexOf(String haystackString, String needleString,
			int cache, int md2, char lastChar) {
		char[] haystack = haystackString.value;
		int haystackOffset = haystackString.offset;
		int haystackLength = haystackString.count;
		char[] needle = needleString.value;
		int needleOffset = needleString.offset;
		int needleLength = needleString.count;
		int needleLengthMinus1 = needleLength - 1;
		int haystackEnd = haystackOffset + haystackLength;
		outer_loop: for (int i = haystackOffset + needleLengthMinus1; i < haystackEnd;) {
			if (lastChar == haystack[i]) {
				for (int j = 0; j < needleLengthMinus1; ++j) {
					if (needle[j + needleOffset] != haystack[i + j
							- needleLengthMinus1]) {
						int skip = 1;
						if ((cache & (1 << haystack[i])) == 0)
							skip += j;
						i += Math.max(md2, skip);
						continue outer_loop;
					}

				}
				return i - needleLengthMinus1 - haystackOffset;
			}

			if ((cache & (1 << haystack[i])) == 0)
				i += needleLengthMinus1;
			i++;
		}

		return -1;
	}

	/*
	 * Returns the character array for this String.
	 */
	char[] getValue() {
		return value;
	}
}
