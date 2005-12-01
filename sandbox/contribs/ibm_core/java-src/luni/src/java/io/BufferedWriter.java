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


import java.security.AccessController;

import com.ibm.oti.util.PriviAction;

/**
 * BufferedWriter is for writing buffered character output. Characters written
 * to this Writer are buffered internally before being commited to the target
 * Writer.
 * 
 * @see BufferedReader
 */
public class BufferedWriter extends Writer {
	private Writer out;

	private char buf[];

	private int pos;

	private final String lineSeparator = (String) AccessController
			.doPrivileged(new PriviAction("line.separator")); //$NON-NLS-1$

	/**
	 * Constructs a new BufferedReader with <code>out</code> as the Writer on
	 * which to buffer write operations. The buffer size is set to the default,
	 * which is 8K.
	 * 
	 * @param out
	 *            The Writer to buffer character writing on
	 */

	public BufferedWriter(Writer out) {
		super(out);
		this.out = out;
		buf = new char[8192];
	}

	/**
	 * Constructs a new BufferedReader with <code>out</code> as the Writer on
	 * which buffer write operations. The buffer size is set to
	 * <code>size</code>.
	 * 
	 * @param out
	 *            The Writer to buffer character writing on.
	 * @param size
	 *            The size of the buffer to use.
	 */

	public BufferedWriter(Writer out, int size) {
		super(out);
		if (size > 0) {
			this.out = out;
			this.buf = new char[size];
		} else
			throw new IllegalArgumentException(com.ibm.oti.util.Msg
					.getString("K0058")); //$NON-NLS-1$
	}

	/**
	 * Close this BufferedWriter. The contents of the buffer are flushed, the
	 * target writer is closed, and the buffer is released. Only the first
	 * invocation of close has any effect.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this Writer.
	 */

	public void close() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				flush();
				out.close();
				buf = null;
				out = null;
			}
		}
	}

	/**
	 * Flush this BufferedWriter. The contents of the buffer are committed to
	 * the target writer and it is then flushed.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to flush this Writer.
	 */

	public void flush() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (pos > 0)
					out.write(buf, 0, pos);
				pos = 0;
				out.flush();
			} else
				throw new IOException(com.ibm.oti.util.Msg.getString("K005d")); //$NON-NLS-1$
		}
	}

	/**
	 * Answer a boolean indicating whether or not this BufferedWriter is open.
	 * 
	 * @return <code>true</code> if this reader is open, <code>false</code>
	 *         otherwise
	 */
	private boolean isOpen() {
		return out != null;
	}

	/**
	 * Write a newline to thie Writer. A newline is determined by the System
	 * property "line.separator". The target writer may or may not be flushed
	 * when a newline is written.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this Writer.
	 */

	public void newLine() throws IOException {
		write(lineSeparator, 0, lineSeparator.length());
	}

	/**
	 * Writes out <code>count</code> characters starting at
	 * <code>offset</code> in <code>buf</code> to this BufferedWriter. If
	 * <code>count</code> is greater than this Writers buffer then flush the
	 * contents and also write the characters directly to the target Writer.
	 * 
	 * @param cbuf
	 *            the non-null array containing characters to write.
	 * @param offset
	 *            offset in buf to retrieve characters
	 * @param count
	 *            maximum number of characters to write
	 * 
	 * @throws IOException
	 *             If this Writer has already been closed or some other
	 *             IOException occurs.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */

	public void write(char[] cbuf, int offset, int count) throws IOException {
		// avoid int overflow
		if (0 <= offset && offset <= cbuf.length && 0 <= count
				&& count <= cbuf.length - offset) {
			synchronized (lock) {
				if (isOpen()) {
					if (pos == 0 && count >= this.buf.length) {
						out.write(cbuf, offset, count);
						return;
					}
					int available = this.buf.length - pos;
					if (count < available)
						available = count;
					if (available > 0) {
						System
								.arraycopy(cbuf, offset, this.buf, pos,
										available);
						pos += available;
					}
					if (pos == this.buf.length) {
						out.write(this.buf, 0, this.buf.length);
						pos = 0;
						if (count > available) {
							offset += available;
							available = count - available;
							if (available >= this.buf.length) {
								out.write(cbuf, offset, available);
								return;
							}
							System.arraycopy(cbuf, offset, this.buf, pos,
									available);
							pos += available;
						}
					}
				} else
					throw new IOException(com.ibm.oti.util.Msg
							.getString("K005d")); //$NON-NLS-1$
			}
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	/**
	 * Writes the character
	 * <code>oneChar<code> BufferedWriter.  If the buffer is filled by
	 * writing this character, flush this Writer.  Only the lower 2 bytes are written.
	 *
	 * @param 		oneChar	The Character to write out.
	 *
	 * @throws 		IOException 	If this Writer has already been closed or some other IOException occurs.
	 */
	public void write(int oneChar) throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (pos >= buf.length) {
					out.write(buf, 0, buf.length);
					pos = 0;
				}
				buf[pos++] = (char) oneChar;
			} else
				throw new IOException(com.ibm.oti.util.Msg.getString("K005d")); //$NON-NLS-1$
		}
	}

	/**
	 * Writes out <code>count</code> characters starting at
	 * <code>offset</code> in <code>str</code> to this BufferedWriter. If
	 * <code>count</code> is greater than this Writers buffer then flush the
	 * contents and also write the characters directly to the target Writer.
	 * 
	 * @param str
	 *            the non-null String containing characters to write
	 * @param offset
	 *            offset in str to retrieve characters
	 * @param count
	 *            maximum number of characters to write
	 * 
	 * @throws IOException
	 *             If this Writer has already been closed or some other
	 *             IOException occurs.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */

	public void write(String str, int offset, int count) throws IOException {
		// avoid int overflow
		if (0 <= offset && offset <= str.length() && 0 <= count
				&& count <= str.length() - offset) {
			synchronized (lock) {
				if (isOpen()) {
					if (pos == 0 && count >= buf.length) {
						char[] chars = new char[count];
						str.getChars(offset, offset + count, chars, 0);
						out.write(chars, 0, count);
						return;
					}
					int available = buf.length - pos;
					if (count < available)
						available = count;
					if (available > 0) {
						str.getChars(offset, offset + available, buf, pos);
						pos += available;
					}
					if (pos == buf.length) {
						out.write(this.buf, 0, this.buf.length);
						pos = 0;
						if (count > available) {
							offset += available;
							available = count - available;
							if (available >= buf.length) {
								char[] chars = new char[count];
								str.getChars(offset, offset + available, chars,
										0);
								out.write(chars, 0, available);
								return;
							}
							str.getChars(offset, offset + available, buf, pos);
							pos += available;
						}
					}
				} else
					throw new IOException(com.ibm.oti.util.Msg
							.getString("K005d")); //$NON-NLS-1$
			}
		} else
			throw new StringIndexOutOfBoundsException();
	}
}
