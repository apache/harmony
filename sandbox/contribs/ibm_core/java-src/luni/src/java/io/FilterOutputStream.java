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
 * FilteredOutputStream is a class which takes an output stream and
 * <em>filters</em> the output in some way. The filtered view may be a
 * buffered output or one which compresses data before actually writing the
 * bytes. FilterOutputStreams are meant for byte streams.
 * 
 * @see FilterInputStream
 */
public class FilterOutputStream extends OutputStream {

	/**
	 * The target OutputStream for this filter.
	 */
	protected OutputStream out;

	/**
	 * Constructs a new FilterOutputStream on the OutputStream <code>out</code>.
	 * All writes are now filtered through this stream.
	 * 
	 * @param out
	 *            the target OutputStream to filter writes on.
	 */
	public FilterOutputStream(OutputStream out) {
		this.out = out;
	}

	/**
	 * Close this FilterOutputStream. This implementation closes the target
	 * stream.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this stream.
	 */
	public void close() throws IOException {
		try {
			flush();
		} catch (IOException e) {
		}
		/* Make sure we clean up this stream if exception fires */
		out.close();
	}

	/**
	 * Flush this FilterOutputStream to ensure all pending data is sent out to
	 * the target OutputStream. This implementation flushes the target
	 * OutputStream.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to flush this
	 *             FilterOutputStream.
	 */
	public void flush() throws IOException {
		out.flush();
	}

	/**
	 * Writes the entire contents of the byte array <code>buffer</code> to
	 * this FilterOutputStream. This implementation writes the
	 * <code>buffer</code> to the target stream.
	 * 
	 * @param buffer
	 *            the buffer to be written
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             FilterOutputStream.
	 */
	public void write(byte buffer[]) throws IOException {
		write(buffer, 0, buffer.length);
	}

	/**
	 * Writes <code>count</code> <code>bytes</code> from the byte array
	 * <code>buffer</code> starting at <code>offset</code> to this
	 * FilterOutputStream. This implementation writes the <code>buffer</code>
	 * to the target OutputStream.
	 * 
	 * @param buffer
	 *            the buffer to be written
	 * @param offset
	 *            offset in buffer to get bytes
	 * @param count
	 *            number of bytes in buffer to write
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             FilterOutputStream.
	 * @throws IndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */
	public void write(byte buffer[], int offset, int count) throws IOException {
		// avoid int overflow, check null buffer
		if (offset <= buffer.length && 0 <= offset && 0 <= count
				&& count <= buffer.length - offset) {
			for (int i = 0; i < count; i++)
				// Call write() instead of out.write() since subclasses could
				// override the write() method.
				write(buffer[offset + i]);
		} else
			throw new ArrayIndexOutOfBoundsException(com.ibm.oti.util.Msg
					.getString("K002f")); //$NON-NLS-1$
	}

	/**
	 * Writes the specified byte <code>oneByte</code> to this
	 * FilterOutputStream. Only the low order byte of <code>oneByte</code> is
	 * written. This implementation writes the byte to the target OutputStream.
	 * 
	 * @param oneByte
	 *            the byte to be written
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             FilterOutputStream.
	 */
	public void write(int oneByte) throws IOException {
		out.write(oneByte);
	}
}
