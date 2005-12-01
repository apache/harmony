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

package java.net;


import java.io.IOException;
import java.io.InputStream;

import com.ibm.oti.util.Msg;

/**
 * The SocketInputStream supports the streamed reading of bytes from a socket.
 * Multiple streams may be opened on a socket, so care should be taken to manage
 * opened streams and coordinate read operations between threads.
 */
class SocketInputStream extends InputStream {

	SocketImpl socket;

	/**
	 * Constructs a SocketInputStream for the <code>socket</code>. Read
	 * operations are forwarded to the <code>socket</code>.
	 * 
	 * @param socket
	 *            the socket to be read
	 * @see Socket
	 */
	public SocketInputStream(SocketImpl socket) {
		super();
		this.socket = socket;
	}

	/**
	 * Answer the number of bytes that may be read without blocking. Zero
	 * indicates a read operation would block. This call itself does not block,
	 * but may throw an IOException.
	 * 
	 * @return int the number of bytes that may be read without blocking
	 * @exception IOException
	 *                thrown if an error occurs during the test
	 */
	public int available() throws IOException {
		return socket.available();
	}

	/**
	 * Close the stream and the underlying socket.
	 * 
	 * @exception IOException
	 *                thrown if an error occurs during the close
	 */
	public void close() throws IOException {
		socket.close();
		super.close();
	}

	/**
	 * Read a single byte from the socket, answering the value as an
	 * <code>int</code>. This call may block indefinitely, depending upon
	 * whether data is available and whether the read timeout option has been
	 * set on the socket. A value of -1 indicates 'end-of-file'.
	 * 
	 * @return int the value read
	 * @exception IOException
	 *                thrown if an error occurs during the read
	 */
	public int read() throws IOException {
		byte[] buffer = new byte[1];
		int result = socket.read(buffer, 0, 1);
		return (-1 == result) ? result : buffer[0] & 0xFF;
	}

	/**
	 * Read a buffer.length number of bytes from the socket, into the
	 * <code>buffer</code>. This call may block indefinitely, depending upon
	 * whether data is available and whether the read timeout option has been
	 * set on the socket. The number of bytes actually read is returned; a value
	 * of -1 indicates 'end-of-file'.
	 * 
	 * @param buffer
	 *            the buffer to read into
	 * @return int the number of bytes actually read
	 * @exception IOException
	 *                thrown if an error occurs during the read
	 */
	public int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}

	/**
	 * Read a <code>count</code> number of bytes from the socket, into the
	 * <code>buffer</code> at an <code>offset</code>. This call may block
	 * indefinitely, depending upon whether data is available and whether the
	 * read timeout option has been set on the socket. The number of bytes
	 * actually read is returned; a value of -1 indicates 'end-of-file'.
	 * 
	 * @param buffer
	 *            the buffer to read into
	 * @param offset
	 *            the offset into the buffer to start filling
	 * @param count
	 *            the maximum number of bytes to read
	 * @return int the number of bytes actually read
	 * @exception IOException,
	 *                ArrayIndexOutOfBoundsException thrown if the argument
	 *                bounds are incorrect or an error occurs during the read
	 */
	public int read(byte[] buffer, int offset, int count) throws IOException {
		if (null == buffer)
			throw new IOException(Msg.getString("K0047"));
		
		if (0 == count)
			return 0;

		if (0 > offset || offset >= buffer.length)
			throw new ArrayIndexOutOfBoundsException(Msg.getString("K002e"));
		if (0 > count || offset + count > buffer.length)
			throw new ArrayIndexOutOfBoundsException(Msg.getString("K002f"));

		return socket.read(buffer, offset, count);
	}

	/**
	 * Skips <code>n</code> number of bytes in this InputStream. Subsequent
	 * <code>read()</code>'s will not return these bytes unless
	 * <code>reset()</code> is used. This method may perform multiple reads to
	 * read <code>n</code> bytes. This implementation reads <code>n</code>
	 * bytes into a temporary buffer.
	 * 
	 * @param n
	 *            the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 * 
	 * @exception java.io.IOException
	 *                If the stream is already closed or another IOException
	 *                occurs.
	 */
	public long skip(long n) throws IOException {
		return (0 == n) ? 0 : super.skip(n);
	}
}
