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


/**
 * The SocketOutputStream supports the streamed writing of bytes to the socket.
 * Multiple streams may be opened on a socket, so care should be taken to manage
 * opened streams and coordinate write operations between threads.
 */
import java.io.IOException;
import java.io.OutputStream;

class SocketOutputStream extends OutputStream {

	SocketImpl socket;

	/**
	 * Constructs a SocketOutputStream for the <code>socket</code>. Write
	 * operations are forwarded to the <code>socket</code>.
	 * 
	 * @param socket
	 *            the socket to be written
	 * @see Socket
	 */

	public SocketOutputStream(SocketImpl socket) {
		super();
		this.socket = socket;
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
	 * Write the <code>buffer</code> to the socket.
	 * 
	 * @param buffer
	 *            the buffer to write
	 * @exception IOException
	 *                thrown if an error occurs during the write
	 */
	public void write(byte[] buffer) throws IOException {
		socket.write(buffer, 0, buffer.length);
	}

	/**
	 * Write the <code>count</code> number of bytes from the
	 * <code>buffer</code> to the socket, starting at <code>offset</code>.
	 * 
	 * @param buffer
	 *            the buffer to write
	 * @param offset
	 *            the offset in buffer to start writing
	 * @param count
	 *            the number of bytes to write
	 * @exception IOException,
	 *                IndexOutOfBoundsException thrown if an error occurs during
	 *                the write
	 */
	public void write(byte[] buffer, int offset, int count) throws IOException {
		// avoid int overflow
		if (buffer != null) {
			if (0 <= offset && offset <= buffer.length && 0 <= count
					&& count <= buffer.length - offset) {
				socket.write(buffer, offset, count);
			} else
				throw new ArrayIndexOutOfBoundsException(com.ibm.oti.util.Msg
						.getString("K002f"));
		} else
			throw new NullPointerException(com.ibm.oti.util.Msg
					.getString("K0047"));
	}

	/**
	 * Write a single byte, the lowest-order byte from an <code>int</code> to
	 * the socket.
	 * 
	 * @param oneByte
	 *            the value to write
	 * @exception IOException
	 *                thrown if an error occurs during the write
	 */
	public void write(int oneByte) throws IOException {
		byte[] buffer = new byte[1];
		buffer[0] = (byte) (oneByte & 0xFF);

		socket.write(buffer, 0, 1);
	}
}
