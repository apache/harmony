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
 * <code>BufferedInputStream</code> is a class which takes an input stream and
 * <em>buffers</em> the input. In this way, costly interaction with the
 * original input stream can be minimized by reading buffered amounts of data
 * infrequently. The drawback is that extra space is required to hold the buffer
 * and that copying takes place when reading that buffer.
 * 
  * @see BufferedOutputStream
 */
public class BufferedInputStream extends FilterInputStream {
	/**
	 * The buffer containing the current bytes read from the target InputStream.
	 */
	protected byte[] buf;

	/**
	 * The total number of bytes inside the byte array <code>buf</code>.
	 */
	protected int count;

	/**
	 * The current limit, which when passed, invalidates the current mark.
	 */
	protected int marklimit;

	/**
	 * The currently marked position. -1 indicates no mark has been set or the
	 * mark has been invalidated.
	 */
	protected int markpos = -1;

	/**
	 * The current position within the byte array <code>buf</code>.
	 */
	protected int pos;

	/**
	 * Constructs a new <code>BufferedInputStream</code> on the InputStream
	 * <code>in</code>. The default buffer size (2K) is allocated and all
	 * reads can now be filtered through this stream.
	 * 
	 * @param in
	 *            the InputStream to buffer reads on.
	 */
	public BufferedInputStream(InputStream in) {
		super(in);
		buf = new byte[2048];
	}

	/**
	 * Constructs a new BufferedInputStream on the InputStream <code>in</code>.
	 * The buffer size is specified by the parameter <code>size</code> and all
	 * reads can now be filtered through this BufferedInputStream.
	 * 
	 * @param in
	 *            the InputStream to buffer reads on.
	 * @param size
	 *            the size of buffer to allocate.
	 */
	public BufferedInputStream(InputStream in, int size) {
		super(in);
		if (size > 0)
			buf = new byte[size];
		else
			throw new IllegalArgumentException(com.ibm.oti.util.Msg
					.getString("K0058")); //$NON-NLS-1$
	}

	/**
	 * Answers a int representing then number of bytes that are available before
	 * this BufferedInputStream will block. This method returns the number of
	 * bytes available in the buffer plus those available in the target stream.
	 * 
	 * @return the number of bytes available before blocking.
	 * 
	 * @throws IOException
	 *             If an error occurs in this stream.
	 */
	public synchronized int available() throws IOException {
		if (buf == null) {
			throw new IOException(com.ibm.oti.util.Msg.getString("K0059")); //$NON-NLS-1$
		}
		return count - pos + in.available();
	}

	/**
	 * Close this BufferedInputStream. This implementation closes the target
	 * stream and releases any resources associated with it.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this stream.
	 */
	public synchronized void close() throws IOException {
		super.close();
		buf = null;
	}

	private int fillbuf() throws IOException {
		if (markpos == -1 || (pos - markpos >= marklimit)) {
			/* Mark position not set or exceeded readlimit */
			int result = in.read(buf);
			markpos = -1;
			pos = 0;
			count = result == -1 ? 0 : result;
			return result;
		}
		if (marklimit > buf.length) {
			/* Increase buffer size to accomodate the readlimit */
			byte[] newbuf = new byte[marklimit];
			System.arraycopy(buf, markpos, newbuf, 0, buf.length - markpos);
			buf = newbuf;
		} else if (markpos > 0) {
			System.arraycopy(buf, markpos, buf, 0, buf.length - markpos);
		}
		/* Set the new position and mark position */
		pos -= markpos;
		count = markpos = 0;
		int bytesread = in.read(buf, pos, buf.length - pos);
		count = bytesread <= 0 ? pos : pos + bytesread;
		return bytesread;
	}

	/**
	 * Set a Mark position in this BufferedInputStream. The parameter
	 * <code>readLimit</code> indicates how many bytes can be read before a
	 * mark is invalidated. Sending reset() will reposition the Stream back to
	 * the marked position provided <code>readLimit</code> has not been
	 * surpassed. The underlying buffer may be increased in size to allow
	 * <code>readlimit</code> number of bytes to be supported.
	 * 
	 * @param readlimit
	 *            the number of bytes to be able to read before invalidating the
	 *            mark.
	 */
	public synchronized void mark(int readlimit) {
		marklimit = readlimit;
		markpos = pos;
	}

	/**
	 * Answers a boolean indicating whether or not this BufferedInputStream
	 * supports mark() and reset(). This implementation answers
	 * <code>true</code>.
	 * 
	 * @return <code>true</code> for BufferedInputStreams.
	 */
	public boolean markSupported() {
		return true;
	}

	/**
	 * Reads a single byte from this BufferedInputStream and returns the result
	 * as an int. The low-order byte is returned or -1 of the end of stream was
	 * encountered. If the underlying buffer does not contain any available
	 * bytes then it is filled and the first byte is returned.
	 * 
	 * @return the byte read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public synchronized int read() throws IOException {
		if (buf != null) {
			/* Are there buffered bytes available? */
			if (pos >= count && fillbuf() == -1)
				return -1; /* no, fill buffer */

			/* Did filling the buffer fail with -1 (EOF)? */
			if (count - pos > 0)
				return buf[pos++] & 0xFF;
			return -1;
		}
		throw new IOException(com.ibm.oti.util.Msg.getString("K0059")); //$NON-NLS-1$
	}

	/**
	 * Reads at most <code>length</code> bytes from this BufferedInputStream
	 * and stores them in byte array <code>buffer</code> starting at offset
	 * <code>offset</code>. Answer the number of bytes actually read or -1 if
	 * no bytes were read and end of stream was encountered. If all the buffered
	 * bytes have been used, a mark has not been set, and the requested number
	 * of bytes is larger than the receiver's buffer size, this implementation
	 * bypasses the buffer and simply places the results directly into
	 * <code>buffer</code>.
	 * 
	 * @param buffer
	 *            the byte array in which to store the read bytes.
	 * @param offset
	 *            the offset in <code>buffer</code> to store the read bytes.
	 * @param length
	 *            the maximum number of bytes to store in <code>buffer</code>.
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public synchronized int read(byte[] buffer, int offset, int length)
			throws IOException {
		if (buf != null && buffer != null) {
			// avoid int overflow
			if (0 <= offset && offset <= buffer.length && 0 <= length
					&& length <= buffer.length - offset) {
				if (length == 0)
					return 0;

				int required;
				if (pos < count) {
					/* There are bytes available in the buffer. */
					int copylength = count - pos >= length ? length : count
							- pos;
					System.arraycopy(buf, pos, buffer, offset, copylength);
					pos += copylength;
					if (copylength == length || in.available() == 0)
						return copylength;
					offset += copylength;
					required = length - copylength;
				} else
					required = length;

				while (true) {
					int read;
					/*
					 * If we're not marked and the required size is greater than
					 * the buffer, simply read the bytes directly bypassing the
					 * buffer.
					 */
					if (markpos == -1 && required >= buf.length) {
						read = in.read(buffer, offset, required);
						if (read == -1)
							return required == length ? -1 : length - required;
					} else {
						if (fillbuf() == -1)
							return required == length ? -1 : length - required;
						read = count - pos >= required ? required : count - pos;
						System.arraycopy(buf, pos, buffer, offset, read);
						pos += read;
					}
					required -= read;
					if (required == 0)
						return length;
					if (in.available() == 0)
						return length - required;
					offset += read;
				}
			}
			throw new ArrayIndexOutOfBoundsException();
		}
		if (buf == null) {
			throw new IOException(com.ibm.oti.util.Msg.getString("K0059")); //$NON-NLS-1$
		}
		throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0047")); //$NON-NLS-1$
	}

	/**
	 * Reset this BufferedInputStream to the last marked location. If the
	 * <code>readlimit</code> has been passed or no <code>mark</code> has
	 * been set, throw IOException. This implementation resets the target
	 * stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */

	public synchronized void reset() throws IOException {
		if (markpos != -1) {
			if (buf != null) {
				pos = markpos;
			} else
				throw new IOException(com.ibm.oti.util.Msg.getString("K0059")); //$NON-NLS-1$
		} else
			throw new IOException(com.ibm.oti.util.Msg.getString("K005a")); //$NON-NLS-1$
	}

	/**
	 * Skips <code>amount</code> number of bytes in this BufferedInputStream.
	 * Subsequent <code>read()</code>'s will not return these bytes unless
	 * <code>reset()</code> is used.
	 * 
	 * @param amount
	 *            the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public synchronized long skip(long amount) throws IOException {
		if (amount < 1)
			return 0;

		if (count - pos >= amount) {
			pos += amount;
			return amount;
		}
		long read = count - pos;
		pos = count;

		if (markpos != -1) {
			if (amount <= marklimit) {
				if (fillbuf() == -1)
					return read;
				if (count - pos >= amount - read) {
					pos += amount - read;
					return amount;
				}
				// Couldn't get all the bytes, skip what we read
				read += (count - pos);
				pos = count;
				return read;
			}
			markpos = -1;
		}
		return read + in.skip(amount - read);
	}
}
