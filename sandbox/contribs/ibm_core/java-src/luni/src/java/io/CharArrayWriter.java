/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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

package java.io;


/**
 * CharArrayWriter is used as a character output stream on a character array.
 * The buffer used to store the written characters will grow as needed to
 * accomodate more characters as they are written.
 * 
 */
public class CharArrayWriter extends Writer {
	/**
	 * Buffer for characters
	 */
	protected char[] buf;

	/**
	 * The ending index of the buffer.
	 */
	protected int count;

	/**
	 * Constructs a new CharArrayWriter which has a buffer allocated with the
	 * default size of 32 characters. The buffer is also the <code>lock</code>
	 * used to synchronize access to this Writer.
	 * 
	 */
	public CharArrayWriter() {
		super();
		buf = new char[32];
		lock = buf;
	}

	/**
	 * Constructs a new CharArrayWriter which has a buffer allocated with the
	 * size of <code>initialSize</code> characters. The buffer is also the
	 * <code>lock</code> used to synchronize access to this Writer.
	 * 
	 * @param initialSize
	 *            the initial size of this CharArrayWriters buffer.
	 */
	public CharArrayWriter(int initialSize) {
		super();
		if (initialSize >= 0) {
			buf = new char[initialSize];
			lock = buf;
		} else
			throw new IllegalArgumentException(com.ibm.oti.util.Msg
					.getString("K005e")); //$NON-NLS-1$
	}

	/**
	 * Close this Writer. This is the concrete implementation required. This
	 * particular implementation does nothing.
	 * 
	 */
	public void close() {
		/* empty */
	}

	private void expand(int i) {
		/* Can the buffer handle @i more chars, if not expand it */
		if (count + i <= buf.length)
			return;

		char[] newbuf = new char[buf.length + (2 * i)];
		System.arraycopy(buf, 0, newbuf, 0, count);
		buf = newbuf;
	}

	/**
	 * Flush this Writer. This is the concrete implementation required. This
	 * particular implementation does nothing.
	 * 
	 */

	public void flush() {
		/* empty */
	}

	/**
	 * Reset this Writer. The current write position is reset to the beginning
	 * of the buffer. All written characters are lost and the size of this
	 * writer is now 0.
	 * 
	 */
	public void reset() {
		synchronized (lock) {
			count = 0;
		}
	}

	/**
	 * Answer the size of this Writer in characters. This number changes if this
	 * Writer is reset or as more characters are written to it.
	 * 
	 * @return int this CharArrayWriters current size in characters.
	 */
	public int size() {
		synchronized (lock) {
			return count;
		}
	}

	/**
	 * Answer the contents of the receiver as a char array. The array returned
	 * is a copy and any modifications made to this Writer after are not
	 * reflected in the result.
	 * 
	 * @return char[] this CharArrayWriters contents as a new char array.
	 */
	public char[] toCharArray() {
		synchronized (lock) {
			char[] result = new char[count];
			System.arraycopy(buf, 0, result, 0, count);
			return result;
		}
	}

	/**
	 * Answer the contents of this CharArrayWriter as a String. The String
	 * returned is a copy and any modifications made to this Writer after are
	 * not reflected in the result.
	 * 
	 * @return String this CharArrayWriters contents as a new String.
	 */
	public String toString() {
		synchronized (lock) {
			return new String(buf, 0, count);
		}
	}

	/**
	 * Writes <code>count</code> characters starting at <code>offset</code>
	 * in <code>buf</code> to this CharArrayWriter.
	 * 
	 * @param c
	 *            the non-null array containing characters to write.
	 * @param offset
	 *            offset in buf to retrieve characters
	 * @param len
	 *            maximum number of characters to write
	 */
	public void write(char[] c, int offset, int len) {
		// avoid int overflow
		if (0 <= offset && offset <= c.length && 0 <= len
				&& len <= c.length - offset) {
			synchronized (lock) {
				expand(len);
				System.arraycopy(c, offset, this.buf, this.count, len);
				this.count += len;
			}
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	/**
	 * Writes the specified character <code>oneChar</code> to this
	 * CharArrayWriter. This implementation writes the low order two bytes to
	 * the Stream.
	 * 
	 * @param oneChar
	 *            The character to write
	 */
	public void write(int oneChar) {
		synchronized (lock) {
			expand(1);
			buf[count++] = (char) oneChar;
		}
	}

	/**
	 * Writes <code>count</code> number of characters starting at
	 * <code>offset</code> from the String <code>str</code> to this
	 * CharArrayWriter.
	 * 
	 * @param str
	 *            the non-null String containing the characters to write.
	 * @param offset
	 *            the starting point to retrieve characters.
	 * @param len
	 *            the number of characters to retrieve and write.
	 */
	public void write(String str, int offset, int len) {
		// avoid int overflow
		if (0 <= offset && offset <= str.length() && 0 <= len
				&& len <= str.length() - offset) {
			synchronized (lock) {
				expand(len);
				str.getChars(offset, offset + len, buf, this.count);
				this.count += len;
			}
		} else
			throw new StringIndexOutOfBoundsException();
	}

	/**
	 * Writes the contents of this CharArrayWriter to another Writer. The output
	 * is all the characters that have been written to the receiver since the
	 * last reset or since the creation.
	 * 
	 * @param out
	 *            the non-null Writer on which to write the contents.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write the contents out.
	 */
	public void writeTo(Writer out) throws IOException {
		synchronized (lock) {
			out.write(buf, 0, count);
		}
	}
}
