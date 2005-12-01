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

package java.io;


/**
 * BufferedReader is a buffered character input reader. Buffering allows reading
 * from character streams more efficiently. If the default size of the buffer is
 * not practical, another size may be specified. Reading a character from a
 * Reader class usually involves reading a character from its Stream or
 * subsequent Reader. It is advisable to wrap a BufferedReader around those
 * Readers whose read operations may have high latency. For example, the
 * following code
 * 
 * <pre>
 * BufferedReader inReader = new BufferedReader(new FileReader(&quot;file.java&quot;));
 * </pre>
 * 
 * will buffer input for the file <code>file.java</code>.
 * 
 * @see BufferedWriter
 */

public class BufferedReader extends Reader {
	private Reader in;

	private char[] buf;

	private int marklimit = -1;

	private int count;

	private int markpos = -1;

	private int pos;

	/**
	 * Constructs a new BufferedReader on the Reader <code>in</code>. The
	 * default buffer size (8K) is allocated and all reads can now be filtered
	 * through this BufferedReader.
	 * 
	 * @param in
	 *            the Reader to buffer reads on.
	 */

	public BufferedReader(Reader in) {
		super(in);
		this.in = in;
		buf = new char[8192];
	}

	/**
	 * Constructs a new BufferedReader on the Reader <code>in</code>. The
	 * buffer size is specified by the parameter <code>size</code> and all
	 * reads can now be filtered through this BufferedReader.
	 * 
	 * @param in
	 *            the Reader to buffer reads on.
	 * @param size
	 *            the size of buffer to allocate.
	 */

	public BufferedReader(Reader in, int size) {
		super(in);
		if (size > 0) {
			this.in = in;
			buf = new char[size];
		} else
			throw new IllegalArgumentException(com.ibm.oti.util.Msg
					.getString("K0058")); //$NON-NLS-1$
	}

	/**
	 * Close the Reader. This implementation closes the Reader being filtered
	 * and releases the buffer used by this reader. If this BufferedReader has
	 * already been closed, nothing is done.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this BufferedReader.
	 */

	public void close() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				in.close();
				buf = null;
			}
		}
	}

	private int fillbuf() throws IOException {
		if (markpos == -1 || (pos - markpos >= marklimit)) {
			/* Mark position not set or exceeded readlimit */
			int result = in.read(buf, 0, buf.length);
			markpos = -1;
			pos = 0;
			count = result == -1 ? 0 : result;
			return result;
		}
		if (marklimit > buf.length) {
			/* Increase buffer size to accomodate the readlimit */
			char[] newbuf = new char[marklimit];
			System.arraycopy(buf, markpos, newbuf, 0, buf.length - markpos);
			buf = newbuf;
		} else if (markpos > 0) {
			System.arraycopy(buf, markpos, buf, 0, buf.length - markpos);
		}
		/* Set the new position and mark position */
		pos -= markpos;
		count = markpos = 0;
		int charsread = in.read(buf, pos, buf.length - pos);
		count = charsread == -1 ? pos : pos + charsread;
		return charsread;
	}

	/**
	 * Answer a boolean indicating whether or not this BufferedReader is open.
	 * 
	 * @return <code>true</code> if this reader is open, <code>false</code>
	 *         otherwise
	 */
	private boolean isOpen() {
		return buf != null;
	}

	/**
	 * Set a Mark position in this BufferedReader. The parameter
	 * <code>readLimit</code> indicates how many characters can be read before
	 * a mark is invalidated. Sending reset() will reposition the reader back to
	 * the marked position provided <code>readLimit</code> has not been
	 * surpassed.
	 * 
	 * @param readlimit
	 *            an int representing how many characters must be read
	 *            beforeinvalidating the mark.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting mark this BufferedReader.
	 */

	public void mark(int readlimit) throws IOException {
		if (readlimit >= 0) {
			synchronized (lock) {
				if (isOpen()) {
					marklimit = readlimit;
					markpos = pos;
				} else
					throw new IOException(com.ibm.oti.util.Msg
							.getString("K005b")); //$NON-NLS-1$
			}
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Answers a boolean indicating whether or not this Reader supports mark()
	 * and reset(). This implementation answers <code>true</code>.
	 * 
	 * @return <code>true</code> if mark() and reset() are supported,
	 *         <code>false</code> otherwise
	 */

	public boolean markSupported() {
		return true;
	}

	/**
	 * Reads a single character from this reader and returns the result as an
	 * int. The 2 higher-order characters are set to 0. If the end of reader was
	 * encountered then return -1. This implementation either returns a
	 * character from the buffer or if there are no characters available, fill
	 * the buffer then return a character or -1.
	 * 
	 * @return the character read or -1 if end of reader.
	 * 
	 * @throws IOException
	 *             If the BufferedReader is already closed or some other IO
	 *             error occurs.
	 */

	public int read() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				/* Are there buffered characters available? */
				if (pos < count || fillbuf() != -1)
					return buf[pos++];
				return -1;
			}
			throw new IOException(com.ibm.oti.util.Msg.getString("K005b")); //$NON-NLS-1$
		}
	}

	/**
	 * Reads at most <code>length</code> characters from this BufferedReader
	 * and stores them at <code>offset</code> in the character array
	 * <code>buffer</code>. Returns the number of characters actually read or
	 * -1 if the end of reader was encountered. If all the buffered characters
	 * have been used, a mark has not been set, and the requested number of
	 * characters is larger than this Readers buffer size, this implementation
	 * bypasses the buffer and simply places the results directly into
	 * <code>buffer</code>.
	 * 
	 * @param buffer
	 *            character array to store the read characters
	 * @param offset
	 *            offset in buf to store the read characters
	 * @param length
	 *            maximum number of characters to read
	 * @return number of characters read or -1 if end of reader.
	 * 
	 * @throws IOException
	 *             If the BufferedReader is already closed or some other IO
	 *             error occurs.
	 */

	public int read(char[] buffer, int offset, int length) throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (length == 0)
					return 0;
				// avoid int overflow
				if (0 <= offset && offset <= buffer.length && 0 < length
						&& length <= buffer.length - offset) {
					int required;
					if (pos < count) {
						/* There are bytes available in the buffer. */
						int copylength = count - pos >= length ? length : count
								- pos;
						System.arraycopy(buf, pos, buffer, offset, copylength);
						pos += copylength;
						if (copylength == length || !in.ready())
							return copylength;
						offset += copylength;
						required = length - copylength;
					} else
						required = length;

					while (true) {
						int read;
						/*
						 * If we're not marked and the required size is greater
						 * than the buffer, simply read the bytes directly
						 * bypassing the buffer.
						 */
						if (markpos == -1 && required >= buf.length) {
							read = in.read(buffer, offset, required);
							if (read == -1)
								return required == length ? -1 : length
										- required;
						} else {
							if (fillbuf() == -1)
								return required == length ? -1 : length
										- required;
							read = count - pos >= required ? required : count
									- pos;
							System.arraycopy(buf, pos, buffer, offset, read);
							pos += read;
						}
						required -= read;
						if (required == 0)
							return length;
						if (!in.ready())
							return length - required;
						offset += read;
					}
				}
				throw new ArrayIndexOutOfBoundsException();
			}
			throw new IOException(com.ibm.oti.util.Msg.getString("K005b")); //$NON-NLS-1$
		}
	}

	/**
	 * Answers a <code>String</code> representing the next line of text
	 * available in this BufferedReader. A line is represented by 0 or more
	 * characters followed by <code>'\n'</code>, <code>'\r'</code>,
	 * <code>'\r\n'</code> or end of stream. The <code>String</code> does
	 * not include the newline sequence.
	 * 
	 * @return the contents of the line or null if no characters were read
	 *         before end of stream.
	 * 
	 * @throws IOException
	 *             If the BufferedReader is already closed or some other IO
	 *             error occurs.
	 */

	public String readLine() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				char eol = '\0';
				StringBuffer result = new StringBuffer(80);
				/* Typical Line Length */

				while (true) {
					/* Are there buffered characters available? */
					if (pos >= count) {
						if (eol == '\n')
							return result.toString();
						// attempt to fill buffer
						if (fillbuf() == -1) // EOF case returns already read
												// characters or null.
							return result.length() > 0 || eol != '\0' ? result
									.toString() : null;
					}
					for (int charPos = pos; charPos < count; charPos++) {
						if (eol == '\0'
								&& (buf[charPos] == '\n' || buf[charPos] == '\r')) {
							eol = buf[charPos];
						} else if (eol == '\r' && (buf[charPos] == '\n')) {
							if (charPos > pos)
								result.append(buf, pos, charPos - pos - 1);
							pos = charPos + 1;
							return result.toString();
						} else if (eol != '\0') {
							if (charPos > pos)
								result.append(buf, pos, charPos - pos - 1);
							pos = charPos;
							return result.toString();
						}
					}
					if (eol != '\0') // eol could only be last character.
						result.append(buf, pos, count - pos - 1);
					else
						result.append(buf, pos, count - pos);
					pos = count;
				}
			}
			throw new IOException(com.ibm.oti.util.Msg.getString("K005b")); //$NON-NLS-1$
		}
	}

	/**
	 * Answers a <code>boolean</code> indicating whether or not this Reader is
	 * ready to be read without blocking. If the result is <code>true</code>,
	 * the next <code>read()</code> will not block. If the result is
	 * <code>false</code> this Reader may or may not block when
	 * <code>read()</code> is sent.
	 * 
	 * @return <code>true</code> if the receiver will not block when
	 *         <code>read()</code> is called, <code>false</code> if unknown
	 *         or blocking will occur.
	 * 
	 * @throws IOException
	 *             If the BufferedReader is already closed or some other IO
	 *             error occurs.
	 */

	public boolean ready() throws IOException {
		synchronized (lock) {
			if (isOpen())
				return ((count - pos) > 0) || in.ready();
			throw new IOException(com.ibm.oti.util.Msg.getString("K005b")); //$NON-NLS-1$
		}
	}

	/**
	 * Reset this BufferedReader's position to the last <code>mark()</code>
	 * location. Invocations of <code>read()/skip()</code> will occur from
	 * this new location. If this Reader was not marked, throw IOException.
	 * 
	 * @throws IOException
	 *             If a problem occured, the receiver does not support
	 *             <code>mark()/reset()</code>, or no mark has been set.
	 */

	public void reset() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (markpos != -1)
					pos = markpos;
				else
					throw new IOException(com.ibm.oti.util.Msg
							.getString("K005c")); //$NON-NLS-1$
			} else
				throw new IOException(com.ibm.oti.util.Msg.getString("K005b")); //$NON-NLS-1$
		}
	}

	/**
	 * Skips <code>amount</code> number of characters in this Reader.
	 * Subsequent <code>read()</code>'s will not return these characters
	 * unless <code>reset()</code> is used. Skipping characters may invalidate
	 * a mark if marklimit is surpassed.
	 * 
	 * @param amount
	 *            the maximum number of characters to skip.
	 * @return the number of characters actually skipped.
	 * 
	 * @throws IOException
	 *             If the BufferedReader is already closed or some other IO
	 *             error occurs.
	 */

	public long skip(long amount) throws IOException {
		if (amount >= 0) {
			synchronized (lock) {
				if (isOpen()) {
					if (amount < 1)
						return 0;
					if (count - pos >= amount) {
						pos += amount;
						return amount;
					}

					long read = count - pos;
					pos = count;
					while (read < amount) {
						if (fillbuf() == -1)
							return read;
						if (count - pos >= amount - read) {
							pos += amount - read;
							return amount;
						}
						// Couldn't get all the characters, skip what we read
						read += (count - pos);
						pos = count;
					}
					return amount;
				}
				throw new IOException(com.ibm.oti.util.Msg.getString("K005b")); //$NON-NLS-1$
			}
		}
		throw new IllegalArgumentException();
	}
}
