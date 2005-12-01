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
 * BufferedOutputStream is a class which takes an output stream and
 * <em>buffers</em> the writes to that stream. In this way, costly interaction
 * with the original output stream can be minimized by writing buffered amounts
 * of data infrequently. The drawback is that extra space is required to hold
 * the buffer and copying takes place when writing that buffer.
 * 
 * @see BufferedInputStream
 */
public class BufferedOutputStream extends FilterOutputStream {
	/**
	 * The buffer containing the bytes to be written to the target OutputStream.
	 */
	protected byte[] buf;

	/**
	 * The total number of bytes inside the byte array <code>buf</code>.
	 */
	protected int count;

	/**
	 * Constructs a new BufferedOutputStream on the OutputStream
	 * <code>out</code>. The default buffer size (512 bytes) is allocated and
	 * all writes are now filtered through this stream.
	 * 
	 * @param out
	 *            the OutputStream to buffer writes on.
	 * 
	 */
	public BufferedOutputStream(OutputStream out) {
		super(out);
		buf = new byte[512];
	}

	/**
	 * Constructs a new BufferedOutputStream on the OutputStream
	 * <code>out</code>. The buffer size is set to <code>size</code> and
	 * all writes are now filtered through this stream.
	 * 
     * @param out
	 *            the OutputStream to buffer writes on.
	 * @param size
	 *            the size of the buffer in bytes.
	 * 
	 */
	public BufferedOutputStream(OutputStream out, int size) {
		super(out);
		if (size > 0)
			buf = new byte[size];
		else
			throw new IllegalArgumentException(com.ibm.oti.util.Msg
					.getString("K0058")); //$NON-NLS-1$
	}

	/**
	 * Flush this BufferedOutputStream to ensure all pending data is written out
	 * to the target OutputStream. In addition, the target stream is also
	 * flushed.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to flush this
	 *             BufferedOutputStream.
	 */
	public synchronized void flush() throws IOException {
		if (count > 0)
			out.write(buf, 0, count);
		count = 0;
		out.flush();
	}

	/**
	 * Writes <code>count</code> <code>bytes</code> from the byte array
	 * <code>buffer</code> starting at <code>offset</code> to this
	 * BufferedOutputStream. If there is room in the buffer to hold the bytes,
	 * they are copied in. If not, the buffered bytes plus the bytes in
	 * <code>buffer</code> are written to the target stream, the target is
	 * flushed, and the buffer is cleared.
	 * 
	 * @param buffer
	 *            the buffer to be written
	 * @param offset
	 *            offset in buffer to get bytes
	 * @param length
	 *            number of bytes in buffer to write
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             BufferedOutputStream.
	 * @throws NullPointerException
	 *             If buffer is null.
	 * @throws IndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */
	public synchronized void write(byte[] buffer, int offset, int length)
			throws IOException {
		if (buffer != null) {
			// avoid int overflow
			if (0 <= offset && offset <= buffer.length && 0 <= length
					&& length <= buffer.length - offset) {
				if (count == 0 && length >= buf.length) {
					out.write(buffer, offset, length);
					return;
				}
				int available = buf.length - count;
				if (length < available)
					available = length;
				if (available > 0) {
					System.arraycopy(buffer, offset, buf, count, available);
					count += available;
				}
				if (count == buf.length) {
					out.write(buf, 0, buf.length);
					count = 0;
					if (length > available) {
						offset += available;
						available = length - available;
						if (available >= buf.length) {
							out.write(buffer, offset, available);
						} else {
							System.arraycopy(buffer, offset, buf, count,
									available);
							count += available;
						}
					}
				}
			} else
				throw new ArrayIndexOutOfBoundsException(com.ibm.oti.util.Msg
						.getString("K002f")); //$NON-NLS-1$
		} else
			throw new NullPointerException(com.ibm.oti.util.Msg
					.getString("K0047")); //$NON-NLS-1$
	}

	/**
	 * Writes the specified byte <code>oneByte</code> to this
	 * BufferedOutputStream. Only the low order byte of <code>oneByte</code>
	 * is written. If there is room in the buffer, the byte is copied in and the
	 * count incremented. Otherwise, the buffer plus <code>oneByte</code> are
	 * written to the target stream, the target is flushed, and the buffer is
	 * reset.
	 * 
	 * @param oneByte
	 *            the byte to be written
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             BufferedOutputStream.
	 */
	public synchronized void write(int oneByte) throws IOException {
		if (count == buf.length) {
			out.write(buf, 0, count);
			count = 0;
		}
		buf[count++] = (byte) oneByte;
	}
}
