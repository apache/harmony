/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
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


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.security.AccessController;

import com.ibm.oti.util.PriviAction;

/**
 * InputStreamReader is class for turning a byte Stream into a character Stream.
 * Data read from the source input stream is converted into characters by either
 * a default or provided character converter. By default, the encoding is
 * assumed to ISO8859_1. The InputStreamReader contains a buffer of bytes read
 * from the source input stream and converts these into characters as needed.
 * The buffer size is 8K.
 * 
 * 
 * @see OutputStreamWriter
 */
public class InputStreamReader extends Reader {
	private InputStream in;

	private static final int BUFFER_SIZE = 8192;

	CharsetDecoder decoder;

	ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SIZE);

	CharBuffer chars = CharBuffer.allocate(BUFFER_SIZE);

	/**
	 * Constructs a new InputStreamReader on the InputStream <code>in</code>.
	 * Now character reading can be filtered through this InputStreamReader.
	 * This constructor assumes the default conversion of ISO8859_1
	 * (ISO-Latin-1).
	 * 
	 * @param in
	 *            the InputStream to convert to characters.
	 */
	public InputStreamReader(InputStream in) {
		super(in);
		this.in = in;
		String encoding = (String) AccessController
				.doPrivileged(new PriviAction("file.encoding", "ISO8859_1")); //$NON-NLS-1$//$NON-NLS-2$
		decoder = Charset.forName(encoding).newDecoder();
		chars.limit(0);
	}

	/**
	 * Constructs a new InputStreamReader on the InputStream <code>in</code>.
	 * Now character reading can be filtered through this InputStreamReader.
	 * This constructor takes a String parameter <code>enc</code> which is the
	 * name of an encoding. If the encoding cannot be found, an
	 * UnsupportedEncodingException error is thrown.
	 * 
	 * @param in
	 *            the InputStream to convert to characters.
	 * @param enc
	 *            a String describing the character converter to use.
	 * 
	 * @throws UnsupportedEncodingException
	 *             if the encoding cannot be found.
	 */
	public InputStreamReader(InputStream in, final String enc)
			throws UnsupportedEncodingException {
		super(in);
		enc.length();
		this.in = in;
		try {
			decoder = Charset.forName(enc).newDecoder();
		} catch (IllegalArgumentException e) {
			throw new UnsupportedEncodingException();
		}
		chars.limit(0);
	}

	/**
	 * Constructs a new InputStreamReader on the InputStream <code>in</code>
	 * and CharsetDecoder <code>dec</code>. Now character reading can be
	 * filtered through this InputStreamReader.
	 * 
	 * @param in
	 *            the InputStream to convert to characters
	 * @param dec
	 *            a CharsetDecoder used by the character convertion
	 */
	public InputStreamReader(InputStream in, CharsetDecoder dec) {
		super(in);
		dec.averageCharsPerByte();
		this.in = in;
		decoder = dec;
		chars.limit(0);
	}

	/**
	 * Constructs a new InputStreamReader on the InputStream <code>in</code>
	 * and Charset <code>charset</code>. Now character reading can be
	 * filtered through this InputStreamReader.
	 * 
	 * @param in
	 *            the InputStream to convert to characters
	 * @param charset
	 *            the Charset that specify the character converter
	 */
	public InputStreamReader(InputStream in, Charset charset) {
		super(in);
		this.in = in;
		decoder = charset.newDecoder();
		chars.limit(0);
	}

	/**
	 * Close this InputStreamReader. This implementation closes the source
	 * InputStream and releases all local storage.
	 * 
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this
	 *             InputStreamReader.
	 */
	public void close() throws IOException {
		synchronized (lock) {
			decoder = null;
			if (in != null) {
				in.close();
				in = null;
			}
		}
	}

	/**
	 * Answer the String which identifies the encoding used to convert bytes to
	 * characters. The value <code>null</code> is returned if this Reader has
	 * been closed.
	 * 
	 * @return the String describing the converter or null if this Reader is
	 *         closed.
	 */
	public String getEncoding() {
		return decoder.charset().name();
	}

	/**
	 * Reads a single character from this InputStreamReader and returns the
	 * result as an int. The 2 higher-order characters are set to 0. If the end
	 * of reader was encountered then return -1. The byte value is either
	 * obtained from converting bytes in this readers buffer or by first filling
	 * the buffer from the source InputStream and then reading from the buffer.
	 * 
	 * @return the character read or -1 if end of reader.
	 * 
	 * @throws IOException
	 *             If the InputStreamReader is already closed or some other IO
	 *             error occurs.
	 */
	public int read() throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (chars.limit() == chars.position()) {
					fillBuf();
				}
				if (chars.limit() == 0) {
					return -1;
				}
				return chars.get();
			}
			throw new IOException("InputStreamReader is closed."); //$NON-NLS-1$
		}
	}

	/**
	 * Reads at most <code>count</code> characters from this Reader and stores
	 * them at <code>offset</code> in the character array <code>buf</code>.
	 * Returns the number of characters actually read or -1 if the end of reader
	 * was encountered. The bytes are either obtained from converting bytes in
	 * this readers buffer or by first filling the buffer from the source
	 * InputStream and then reading from the buffer.
	 * 
	 * @param buf
	 *            character array to store the read characters
	 * @param offset
	 *            offset in buf to store the read characters
	 * @param length
	 *            maximum number of characters to read
	 * @return the number of characters read or -1 if end of reader.
	 * 
	 * @throws IOException
	 *             If the InputStreamReader is already closed or some other IO
	 *             error occurs.
	 */
	public int read(char[] buf, int offset, int length) throws IOException {
		synchronized (lock) {
			if (isOpen()) {
				if (length == 0)
					return 0;
				if (offset < 0 || length < 0 || offset + length > buf.length) {
					throw new IndexOutOfBoundsException();
				}
				// read at least once
				if (chars.limit() == chars.position()) {
					fillBuf();
				}
				int position = chars.position();
				int availableChars = chars.limit() - position;
				// read at least once for one byte
				int needChars = length;
				while (availableChars < needChars) {
					System.arraycopy(chars.array(), position, buf, offset,
							availableChars);
					chars.position(position + availableChars);
					needChars -= availableChars;
					offset += availableChars;
					if (in.available() <= 0) {
						return needChars == length ? -1 : length - needChars;
					}
					fillBuf();
					position = chars.position();
					availableChars = chars.limit();
					if (availableChars == 0) {
						return needChars == length ? -1 : length - needChars;
					}
				}
				System.arraycopy(chars.array(), position, buf, offset,
						needChars);
				chars.position(chars.position() + needChars);
				return length;
			}
			throw new IOException("InputStreamReader is closed."); //$NON-NLS-1$
		}
	}

	/*
	 * Answer a boolean indicating whether or not this InputStreamReader is
	 * open.
	 */
	private boolean isOpen() {
		return in != null;
	}

	/*
	 * refill the buffer from wrapped InputStream
	 */
	private void fillBuf() throws IOException {
		chars.clear();
		int read = 0;
		try {
			read = in.read(bytes.array());
		} catch (IOException e) {
			chars.limit(0);
			throw e;
		}
		if (read == -1) {
			chars.limit(0);
			return;
		}
		bytes.limit(read);
		boolean endOfInput = read < BUFFER_SIZE;
		CoderResult result = decoder.decode(bytes, chars, endOfInput);
		if (result.isError()) {
			throw new IOException(result.toString());
		}
		bytes.clear();
		chars.flip();
	}

	/**
	 * Answers a <code>boolean</code> indicating whether or not this
	 * InputStreamReader is ready to be read without blocking. If the result is
	 * <code>true</code>, the next <code>read()</code> will not block. If
	 * the result is <code>false</code> this Reader may or may not block when
	 * <code>read()</code> is sent. This implementation answers
	 * <code>true</code> if there are bytes available in the buffer or the
	 * source InputStream has bytes available.
	 * 
	 * @return <code>true</code> if the receiver will not block when
	 *         <code>read()</code> is called, <code>false</code> if unknown
	 *         or blocking will occur.
	 * 
	 * @throws IOException
	 *             If the InputStreamReader is already closed or some other IO
	 *             error occurs.
	 */
	public boolean ready() throws IOException {
		synchronized (lock) {
			if (in == null) {
				throw new IOException("This reader has been closed!"); //$NON-NLS-1$
			}
			try {
				return chars.limit() > chars.position() || in.available() > 0;
			} catch (IOException e) {
				return false;
			}

		}
	}
}
