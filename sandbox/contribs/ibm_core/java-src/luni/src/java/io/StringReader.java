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
 * StringReader is used as a character input stream on a String.
 * 
 * @see StringWriter
 */
public class StringReader extends Reader {
	private String str;

	private int markpos = -1;

	private int pos = 0;

	private int count;

	/**
	 * Construct a StringReader on the String <code>str</code>. The size of
	 * the reader is set to the <code>length()</code> of the String and the
	 * Object to synchronize access through is set to <code>str</code>.
	 * 
	 * @param str
	 *            the String to filter reads on.
	 */
	public StringReader(String str) {
		super(str);
		this.str = str;
		this.count = str.length();
	}

	/**
	 * This method closes this StringReader. Once it is closed, you can no
	 * longer read from it. Only the first invocation of this method has any
	 * effect.
	 */
	public void close() {
		synchronized (lock) {
			if (isOpen())
				str = null;
		}
	}

	/**
	 * Answer a boolean indicating whether or not this StringReader is open.
	 * @return str
	 */
	private boolean isOpen() {
		return str != null;
	}

	/**
	 * Set a Mark position in this Reader. The parameter <code>readLimit</code>
	 * is ignored for StringReaders. Sending reset() will reposition the reader
	 * back to the marked position provided the mark has not been invalidated.
	 * 
	 * @param readLimit
	 *            ignored for StringReaders.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting mark this StringReader.
	 */
	public void mark(int readLimit) throws IOException {
		if (readLimit >= 0) {
			synchronized (lock) {
				if (isOpen())
					markpos = pos;
				else
					throw new IOException(com.ibm.oti.util.Msg
							.getString("K0083")); //$NON-NLS-1$
			}
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Answers a boolean indicating whether or not this StringReader supports
	 * mark() and reset(). This method always returns true.
	 * 
	 * @return <code>true</code> if mark() and reset() are supported,
	 *         <code>false</code> otherwise. This implementation always
	 *         returns <code>true</code>.
	 */
	public boolean markSupported() {
		return true;
	}

	/**
	 * Reads a single character from this StringReader and returns the result as
	 * an int. The 2 higher-order bytes are set to 0. If the end of reader was
	 * encountered then return -1.
	 * 
	 * @return the character read or -1 if end of reader.
	 * 
	 * @throws IOException
	 *             If the StringReader is already closed.
	 */
	public int read() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (pos != count) {
					return str.charAt(pos++);
				}
				return -1;
			}
			throw new IOException(com.ibm.oti.util.Msg.getString("K0083")); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int read(char buf[], int offset, int len) throws IOException {
		// avoid int overflow
		if (0 <= offset && offset <= buf.length && 0 <= len
				&& len <= buf.length - offset) {
			synchronized (lock) {
				if (isOpen()) {
					if (pos == this.count) {
						return -1;
					}
					int end = pos + len > this.count ? this.count : pos + len;
					str.getChars(pos, end, buf, offset);
					int read = end - pos;
					pos = end;
					return read;
				}
				throw new IOException(com.ibm.oti.util.Msg.getString("K0083")); //$NON-NLS-1$
			}
		}
		throw new ArrayIndexOutOfBoundsException();
	}

	/**
	 * Answers a <code>boolean</code> indicating whether or not this
	 * StringReader is ready to be read without blocking. If the result is
	 * <code>true</code>, the next <code>read()</code> will not block. If
	 * the result is <code>false</code> this Reader may or may not block when
	 * <code>read()</code> is sent. The implementation in StringReader always
	 * returns <code>true</code> even when it has been closed.
	 * 
	 * @return <code>true</code> if the receiver will not block when
	 *         <code>read()</code> is called, <code>false</code> if unknown
	 *         or blocking will occur.
	 * 
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public boolean ready() throws IOException {
		synchronized (lock) {
			if (isOpen())
				return true;
			throw new IOException(com.ibm.oti.util.Msg.getString("K0083")); //$NON-NLS-1$
		}
	}

	/**
	 * Reset this StringReader's position to the last <code>mark()</code>
	 * location. Invocations of <code>read()/skip()</code> will occur from
	 * this new location. If this Reader was not marked, the StringReader is
	 * reset to the beginning of the String.
	 * 
	 * @throws IOException
	 *             If this StringReader has already been closed.
	 */
	public void reset() throws IOException {
		synchronized (lock) {
			if (isOpen())
				pos = markpos != -1 ? markpos : 0;
			else
				throw new IOException(com.ibm.oti.util.Msg.getString("K0083")); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Reader#skip(long)
	 */
	public long skip(long ns) throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (ns <= 0) {
					return 0;
				}
				long skipped = 0;
				if (ns < this.count - pos) {
					pos = pos + (int) ns;
					skipped = ns;
				} else {
					skipped = this.count - pos;
					pos = this.count;
				}
				return skipped;
			}
			throw new IOException(com.ibm.oti.util.Msg.getString("K0083")); //$NON-NLS-1$
		}
	}
}
