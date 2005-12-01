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


import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/**
 * StringBuffer is a variable size contiguous indexable array of characters. The
 * length of the StringBuffer is the number of characters it contains. The
 * capacity of the StringBuffer is the number of characters it can hold.
 * <p>
 * Characters may be inserted at any position up to the length of the
 * StringBuffer, increasing the length of the StringBuffer. Characters at any
 * position in the StringBuffer may be replaced, which does not affect the
 * StringBuffer length.
 * <p>
 * The capacity of a StringBuffer may be specified when the StringBuffer is
 * created. If the capacity of the StringBuffer is exceeded, the capacity is
 * increased.
 * 
 * @see String
 */
public final class StringBuffer implements Serializable, CharSequence {
	
	static final long serialVersionUID = 3388685877147921107L;

	private static final int INITIAL_SIZE = 16;

	private int count;

	private boolean shared;

	private char[] value;

	/**
	 * Constructs a new StringBuffer using the default capacity.
	 */
	public StringBuffer() {
		this(INITIAL_SIZE);
	}

	/**
	 * Constructs a new StringBuffer using the specified capacity.
	 * 
	 * @param capacity
	 *            the initial capacity
	 */
	public StringBuffer(int capacity) {
		count = 0;
		shared = false;
		value = new char[capacity];
	}

	/**
	 * Constructs a new StringBuffer containing the characters in the specified
	 * string and the default capacity.
	 * 
	 * @param string
	 *            the string content with which to initialize the new
	 *            <code>StringBuffer</code> instance
	 * @exception NullPointerException
	 *                on supplying a <code>null</code> value of
	 *                <code>string</code>
	 */
	public StringBuffer(String string) {
		count = string.length();
		shared = false;
		value = new char[count + INITIAL_SIZE];
		string.getChars(0, count, value, 0);
	}

	/**
	 * Adds the character array to the end of this StringBuffer.
	 * 
	 * @param chars
	 *            the character array
	 * @return this StringBuffer
	 * 
	 * @exception NullPointerException
	 *                when chars is null
	 */
	public synchronized StringBuffer append(char chars[]) {
		int newSize = count + chars.length;
		if (newSize > value.length) {
			ensureCapacityImpl(newSize);
		} else if (shared) {
			value = (char[]) value.clone();
			shared = false;
		}
		System.arraycopy(chars, 0, value, count, chars.length);
		count = newSize;
		return this;
	}

	/**
	 * Adds the specified sequence of characters to the end of this
	 * StringBuffer.
	 * 
	 * @param chars
	 *            a character array
	 * @param start
	 *            the starting offset
	 * @param length
	 *            the number of characters
	 * @return this StringBuffer
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>length < 0, start < 0</code> or
	 *                <code>start + length > chars.length</code>
	 * @exception NullPointerException
	 *                when chars is null
	 */
	public synchronized StringBuffer append(char chars[], int start, int length) {
		// start + length could overflow, start/length maybe MaxInt
		if (start >= 0 && 0 <= length && length <= chars.length - start) {
			int newSize = count + length;
			if (newSize > value.length) {
				ensureCapacityImpl(newSize);
			} else if (shared) {
				value = (char[]) value.clone();
				shared = false;
			}
			System.arraycopy(chars, start, value, count, length);
			count = newSize;
			return this;
		}
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Adds the specified character to the end of this StringBuffer.
	 * 
	 * @param ch
	 *            a character
	 * @return this StringBuffer
	 */
	public synchronized StringBuffer append(char ch) {
		if (count >= value.length) {
			ensureCapacityImpl(count + 1);
		}
		if (shared) {
			value = (char[]) value.clone();
			shared = false;
		}
		value[count] = ch;
		count++;
		return this;
	}

	/**
	 * Adds the string representation of the specified double to the end of this
	 * StringBuffer.
	 * 
	 * @param d
	 *            the double
	 * @return this StringBuffer
	 */
	public StringBuffer append(double d) {
		return append(String.valueOf(d));
	}

	/**
	 * Adds the string representation of the specified float to the end of this
	 * StringBuffer.
	 * 
	 * @param f
	 *            the float
	 * @return this StringBuffer
	 */
	public StringBuffer append(float f) {
		return append(String.valueOf(f));
	}

	/**
	 * Adds the string representation of the specified integer to the end of
	 * this StringBuffer.
	 * 
	 * @param value
	 *            the integer
	 * @return this StringBuffer
	 */
	public StringBuffer append(int i) {
		return append(Integer.toString(i));
	}

	/**
	 * Adds the string representation of the specified long to the end of this
	 * StringBuffer.
	 * 
	 * @param l
	 *            the long
	 * @return this StringBuffer
	 */
	public StringBuffer append(long l) {
		return append(Long.toString(l));
	}

	/**
	 * Adds the string representation of the specified object to the end of this
	 * StringBuffer.
	 * 
	 * @param obj
	 *            the object
	 * @return this StringBuffer
	 */
	public StringBuffer append(Object obj) {
		return append(String.valueOf(obj));
	}

	/**
	 * Adds the specified string to the end of this StringBuffer.
	 * 
	 * @param string
	 *            the string
	 * @return this StringBuffer
	 */
	public synchronized StringBuffer append(String string) {
		if (string == null)
			string = String.valueOf(string);
		int adding = string.length();
		int newSize = count + adding;
		if (newSize > value.length) {
			ensureCapacityImpl(newSize);
		} else if (shared) {
			value = (char[]) value.clone();
			shared = false;
		}
		string.getChars(0, adding, value, count);
		count = newSize;
		return this;
	}

	/**
	 * Adds the string representation of the specified boolean to the end of
	 * this StringBuffer.
	 * 
	 * @param b
	 *            the boolean
	 * @return this StringBuffer
	 */
	public StringBuffer append(boolean b) {
		return append(String.valueOf(b));
	}

	/**
	 * Answers the number of characters this StringBuffer can hold without
	 * growing.
	 * 
	 * @return the capacity of this StringBuffer
	 * 
	 * @see #ensureCapacity
	 * @see #length
	 */
	public int capacity() {
		return value.length;
	}

	/**
	 * Answers the character at the specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the zero-based index in this StringBuffer
	 * @return the character at the index
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index >= length()</code>
	 */
	public synchronized char charAt(int index) {
		try {
			if (index < count)
				return value[index];
		} catch (IndexOutOfBoundsException e) {
		}
		throw new StringIndexOutOfBoundsException(index);
	}

	/**
	 * Deletes a range of characters.
	 * 
	 * @param start
	 *            the offset of the first character
	 * @param end
	 *            the offset one past the last character
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>start < 0, start > end</code> or
	 *                <code>end > length()</code>
	 */
	public synchronized StringBuffer delete(int start, int end) {
		if (start >= 0) {
			if (end > count)
				end = count;
			if (end > start) {
				int length = count - end;
				if (length > 0) {
					try {
						if (!shared) {
							System.arraycopy(value, end, value, start, length);
						} else {
							char[] newData = new char[value.length];
							System.arraycopy(value, 0, newData, 0, start);
							System
									.arraycopy(value, end, newData, start,
											length);
							value = newData;
							shared = false;
						}
					} catch (IndexOutOfBoundsException e) {
						throw new StringIndexOutOfBoundsException();
					}
				}
				count -= end - start;
				return this;
			}
			if (start == end)
				return this;
		}
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Deletes a single character
	 * 
	 * @param location
	 *            the offset of the character to delete
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>location < 0</code> or
	 *                <code>location >= length()</code>
	 */
	public synchronized StringBuffer deleteCharAt(int location) {
		if (0 <= location && location < count) {
			int length = count - location - 1;
			if (length > 0) {
				try {
					if (!shared) {
						System.arraycopy(value, location + 1, value, location,
								length);
					} else {
						char[] newData = new char[value.length];
						System.arraycopy(value, 0, newData, 0, location);
						System.arraycopy(value, location + 1, newData,
								location, length);
						value = newData;
						shared = false;
					}
				} catch (IndexOutOfBoundsException e) {
					throw new StringIndexOutOfBoundsException(location);
				}
			}
			count--;
			return this;
		}
		throw new StringIndexOutOfBoundsException(location);
	}

	/**
	 * Ensures that this StringBuffer can hold the specified number of
	 * characters without growing.
	 * 
	 * @param min
	 *            the minimum number of elements that this StringBuffer will
	 *            hold before growing
	 */
	public synchronized void ensureCapacity(int min) {
		if (min > value.length)
			ensureCapacityImpl(min);
	}

	private void ensureCapacityImpl(int min) {
		int twice = (value.length << 1) + 2;
		char[] newData = new char[min > twice ? min : twice];
		System.arraycopy(value, 0, newData, 0, count);
		value = newData;
		shared = false;
	}

	/**
	 * Copies the specified characters in this StringBuffer to the character
	 * array starting at the specified offset in the character array.
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
	 * @exception IndexOutOfBoundsException
	 *                when <code>start < 0, end > length(),
	 *				start > end, index < 0, end - start > buffer.length - index</code>
	 * @exception NullPointerException
	 *                when buffer is null
	 */
	public synchronized void getChars(int start, int end, char[] buffer,
			int index) {
		// NOTE last character not copied!
		try {
			if (start <= count && end <= count) {
				System.arraycopy(value, start, buffer, index, end - start);
				return;
			}
		} catch (IndexOutOfBoundsException e) {
		}
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Inserts the character array at the specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param chars
	 *            the character array to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 * @exception NullPointerException
	 *                when chars is null
	 */
	public synchronized StringBuffer insert(int index, char[] chars) {
		if (0 <= index && index <= count) {
			move(chars.length, index);
			System.arraycopy(chars, 0, value, index, chars.length);
			count += chars.length;
			return this;
		}
		throw new StringIndexOutOfBoundsException(index);
	}

	/**
	 * Inserts the specified sequence of characters at the specified offset in
	 * this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param chars
	 *            a character array
	 * @param start
	 *            the starting offset
	 * @param length
	 *            the number of characters
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>length < 0, start < 0,</code>
	 *				<code>start + length > chars.length, index < 0</code>
	 *                or <code>index > length()</code>
	 * @exception NullPointerException
	 *                when chars is null
	 */
	public synchronized StringBuffer insert(int index, char chars[], int start,
			int length) {
		if (0 <= index && index <= count) {
			// start + length could overflow, start/length maybe MaxInt
			if (start >= 0 && 0 <= length && length <= chars.length - start) {
				move(length, index);
				System.arraycopy(chars, start, value, index, length);
				count += length;
				return this;
			}
			throw new StringIndexOutOfBoundsException();
		}
		throw new StringIndexOutOfBoundsException(index);
	}

	/**
	 * Inserts the character at the specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param ch
	 *            the character to insert
	 * @return this StringBuffer
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public synchronized StringBuffer insert(int index, char ch) {
		if (0 <= index && index <= count) {
			move(1, index);
			value[index] = ch;
			count++;
			return this;
		}
		throw new StringIndexOutOfBoundsException(index);
	}

	/**
	 * Inserts the string representation of the specified double at the
	 * specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param d
	 *            the double to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public StringBuffer insert(int index, double d) {
		return insert(index, String.valueOf(d));
	}

	/**
	 * Inserts the string representation of the specified float at the specified
	 * offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param f
	 *            the float to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public StringBuffer insert(int index, float f) {
		return insert(index, String.valueOf(f));
	}

	/**
	 * Inserts the string representation of the specified integer at the
	 * specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param i
	 *            the integer to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public StringBuffer insert(int index, int i) {
		return insert(index, Integer.toString(i));
	}

	/**
	 * Inserts the string representation of the specified long at the specified
	 * offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param l
	 *            the long to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public StringBuffer insert(int index, long l) {
		return insert(index, Long.toString(l));
	}

	/**
	 * Inserts the string representation of the specified object at the
	 * specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param obj
	 *            the object to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public StringBuffer insert(int index, Object obj) {
		return insert(index, String.valueOf(obj));
	}

	/**
	 * Inserts the string at the specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param string
	 *            the string to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public synchronized StringBuffer insert(int index, String string) {
		if (0 <= index && index <= count) {
			if (string == null)
				string = String.valueOf(string);
			int min = string.length();
			move(min, index);
			string.getChars(0, min, value, index);
			count += min;
			return this;
		}
		throw new StringIndexOutOfBoundsException(index);
	}

	/**
	 * Inserts the string representation of the specified boolean at the
	 * specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the index at which to insert
	 * @param b
	 *            the boolean to insert
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index > length()</code>
	 */
	public StringBuffer insert(int index, boolean b) {
		return insert(index, String.valueOf(b));
	}

	/**
	 * Answers the size of this StringBuffer.
	 * 
	 * @return the number of characters in this StringBuffer
	 */
	public int length() {
		return count;
	}

	private void move(int size, int index) {
		int newSize;
		if (value.length - count >= size) {
			if (!shared) {
				System.arraycopy(value, index, value, index + size, count
						- index); // index == count case is no-op
				return;
			}
			newSize = value.length;
		} else {
			int a = count + size, b = (value.length << 1) + 2;
			newSize = a > b ? a : b;
		}

		char[] newData = new char[newSize];
		System.arraycopy(value, 0, newData, 0, index);
		// index == count case is no-op
		System.arraycopy(value, index, newData, index + size, count - index); 
		value = newData;
		shared = false;
	}

	/**
	 * Replace a range of characters with the characters in the specified
	 * String.
	 * 
	 * @param start
	 *            the offset of the first character
	 * @param end
	 *            the offset one past the last character
	 * @param string
	 *            a String
	 * @return this StringBuffer
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>start < 0</code> or <code>start > end</code>
	 */
	public synchronized StringBuffer replace(int start, int end, String string) {
		if (start >= 0) {
			if (end > count)
				end = count;
			if (end > start) {
				int stringLength = string.length();
				int diff = end - start - stringLength;
				if (diff > 0) { // replacing with fewer characters
					if (!shared) {
						 // index == count case is no-op
						System.arraycopy(value, end, value, start
								+ stringLength, count - end);
					} else {
						char[] newData = new char[value.length];
						System.arraycopy(value, 0, newData, 0, start);
						// index == count case is no-op
						System.arraycopy(value, end, newData, start
								+ stringLength, count - end);
						value = newData;
						shared = false;
					}
				} else if (diff < 0) {
					// replacing with more characters...need some room
					move(-diff, end);
				} else if (shared) {
					value = (char[]) value.clone();
					shared = false;
				}
				string.getChars(0, stringLength, value, start);
				count -= diff;
				return this;
			}
			if (start == end) {
				if (string == null)
					throw new NullPointerException();
				return insert(start, string);
			}
		}
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Reverses the order of characters in this StringBuffer.
	 * 
	 * @return this StringBuffer
	 */
	public synchronized StringBuffer reverse() {
		if (count < 2) {
			return this;
		}
		if (!shared) {
			for (int i = 0, end = count, mid = count / 2; i < mid; i++) {
				char temp = value[--end];
				value[end] = value[i];
				value[i] = temp;
			}
		} else {
			char[] newData = new char[value.length];
			for (int i = 0, end = count; i < count; i++) {
				newData[--end] = value[i];
			}
			value = newData;
			shared = false;
		}
		return this;
	}

	/**
	 * Sets the character at the specified offset in this StringBuffer.
	 * 
	 * @param index
	 *            the zero-based index in this StringBuffer
	 * @param ch
	 *            the character
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>index < 0</code> or
	 *                <code>index >= length()</code>
	 */
	public synchronized void setCharAt(int index, char ch) {
		if (shared) {
			value = (char[]) value.clone();
			shared = false;
		}
		if (0 <= index && index < count)
			value[index] = ch;
		else
			throw new StringIndexOutOfBoundsException(index);
	}

	/**
	 * Sets the length of this StringBuffer to the specified length. If there
	 * are more than length characters in this StringBuffer, the characters at
	 * end are lost. If there are less than length characters in the
	 * StringBuffer, the additional characters are set to <code>\\u0000</code>.
	 * 
	 * @param length
	 *            the new length of this StringBuffer
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>length < 0</code>
	 * 
	 * @see #length
	 */
	public synchronized void setLength(int length) {
		if (length > value.length)
			ensureCapacityImpl(length);
		if (count > length) {
			if (!shared) {
				// NOTE: delete & replace do not void characters orphaned at the
				// end
				try {
					Arrays.fill(value, length, count, (char) 0);
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new IndexOutOfBoundsException();
				}
			} else {
				char[] newData = new char[value.length];
				if (length > 0) {
					System.arraycopy(value, 0, newData, 0, length);
				}
				value = newData;
				shared = false;
			}
		}
		count = length;
	}

	/**
	 * Copies a range of characters into a new String.
	 * 
	 * @param start
	 *            the offset of the first character
	 * @return a new String containing the characters from start to the end of
	 *         the string
	 * 
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>start > length()</code>
	 */
	public synchronized String substring(int start) {
		if (0 <= start && start <= count) {
			shared = true;
			return new String(start, count - start, value);
		}
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
	 * @exception StringIndexOutOfBoundsException
	 *                when <code>start < 0, start > end</code> or
	 *                <code>end > length()</code>
	 */
	public synchronized String substring(int start, int end) {
		if (0 <= start && start <= end && end <= count) {
			shared = true;
			return new String(value, start, end - start);
		}
		throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Answers the contents of this StringBuffer.
	 * 
	 * @return a String containing the characters in this StringBuffer
	 */
	public synchronized String toString() {
		if (count >= 256 && count <= (value.length >> 1))
			return new String(value, 0, count);
		shared = true;
		return new String(0, count, value);
	}

	/*
	 * Return the underlying buffer and set the shared flag.
	 * 
	 */
	char[] shareValue() {
		shared = true;
		return value;
	}

	private synchronized void writeObject(ObjectOutputStream stream)
			throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		if (count > value.length)
			throw new InvalidObjectException(com.ibm.oti.util.Msg
					.getString("K0199"));
		shared = false;
	}

	/**
	 * Adds the specified StringBuffer to the end of this StringBuffer.
	 * 
	 * @param sbuffer
	 *            the StringBuffer
	 * @return this StringBuffer
	 * 
	 * @since 1.4
	 */
	public synchronized StringBuffer append(StringBuffer sbuffer) {
		if (sbuffer == null)
			return append((String) null);
		synchronized (sbuffer) {
			int adding = sbuffer.count;
			int newSize = count + adding;
			if (newSize > value.length) {
				ensureCapacityImpl(newSize);
			} else if (shared) {
				value = (char[]) value.clone();
				shared = false;
			}
			System.arraycopy(sbuffer.value, 0, value, count, adding);
			count = newSize;
		}
		return this;
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
	 * @exception IndexOutOfBoundsException
	 *                when <code>start < 0, start > end</code> or
	 *                <code>end > length()</code>
	 * 
	 * @since 1.4
	 */
	public CharSequence subSequence(int start, int end) {
		return substring(start, end);
	}

	/**
	 * Searches in this StringBuffer for the first index of the specified
	 * character. The search for the character starts at the beginning and moves
	 * towards the end.
	 * 
	 * 
	 * @param string
	 *            the string to find
	 * @return the index in this StringBuffer of the specified character, -1 if
	 *         the character isn't found
	 * 
	 * @see #lastIndexOf(String)
	 * 
	 * @since 1.4
	 */
	public int indexOf(String string) {
		return indexOf(string, 0);
	}

	/**
	 * Searches in this StringBuffer for the index of the specified character.
	 * The search for the character starts at the specified offset and moves
	 * towards the end.
	 * 
	 * @param subString
	 *            the string to find
	 * @param start
	 *            the starting offset
	 * @return the index in this StringBuffer of the specified character, -1 if
	 *         the character isn't found
	 * 
	 * @see #lastIndexOf(String,int)
	 * 
	 * @since 1.4
	 */
	public synchronized int indexOf(String subString, int start) {
		if (start < 0)
			start = 0;
		int subCount = subString.length();
		if (subCount > 0) {
			if (subCount + start > count)
				return -1;
			char firstChar = subString.charAt(0);
			while (true) {
				int i = start;
				boolean found = false;
				for (; i < count; i++)
					if (value[i] == firstChar) {
						found = true;
						break;
					}
				if (!found || subCount + i > count)
					return -1; // handles subCount > count || start >= count
				int o1 = i, o2 = 0;
				while (++o2 < subCount && value[++o1] == subString.charAt(o2)) {
					// Intentionally empty
				}
				if (o2 == subCount)
					return i;
				start = i + 1;
			}
		}
		return (start < count || start == 0) ? start : count;
	}

	/**
	 * Searches in this StringBuffer for the last index of the specified
	 * character. The search for the character starts at the end and moves
	 * towards the beginning.
	 * 
	 * @param string
	 *            the string to find
	 * @return the index in this StringBuffer of the specified character, -1 if
	 *         the character isn't found
	 * 
	 * @see #indexOf(String)
	 * 
	 * @since 1.4
	 */
	public synchronized int lastIndexOf(String string) {
		return lastIndexOf(string, count);
	}

	/**
	 * Searches in this StringBuffer for the index of the specified character.
	 * The search for the character starts at the specified offset and moves
	 * towards the beginning.
	 * 
	 * @param subString
	 *            the string to find
	 * @param start
	 *            the starting offset
	 * @return the index in this StringBuffer of the specified character, -1 if
	 *         the character isn't found
	 * 
	 * @see #indexOf(String,int)
	 * 
	 * @since 1.4
	 */
	public synchronized int lastIndexOf(String subString, int start) {
		int subCount = subString.length();
		if (subCount <= count && start >= 0) {
			if (subCount > 0) {
				if (start > count - subCount)
					start = count - subCount; // count and subCount are both
												// >= 1
				char firstChar = subString.charAt(0);
				while (true) {
					int i = start;
					boolean found = false;
					for (; i >= 0; --i)
						if (value[i] == firstChar) {
							found = true;
							break;
						}
					if (!found)
						return -1;
					int o1 = i, o2 = 0;
					while (++o2 < subCount
							&& value[++o1] == subString.charAt(o2)) {
						// Intentionally empty
					}
					if (o2 == subCount)
						return i;
					start = i - 1;
				}
			}
			return start < count ? start : count;
		}
		return -1;
	}

	/*
	 * Returns the character array for this StringBuffer.
	 */
	char[] getValue() {
		return value;
	}
}
