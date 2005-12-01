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
 * FilteredInputStream is a class which takes an input stream and
 * <em>filters</em> the input in some way. The filtered view may be a buffered
 * view or one which uncompresses data before returning bytes read.
 * FilterInputStreams are meant for byte streams.
 * 
 * @see FilterOutputStream
 */
public class FilterInputStream extends InputStream {

	/**
	 * The target InputStream which is being filtered.
	 */
	protected InputStream in;

	/**
	 * Constructs a new FilterInputStream on the InputStream <code>in</code>.
	 * All reads are now filtered through this stream.
	 * 
	 * @param in
	 *            The non-null InputStream to filter reads on.
	 */
	protected FilterInputStream(InputStream in) {
		super();
		this.in = in;
	}

	/**
	 * Answers a int representing the number of bytes that are available before
	 * this FilterInputStream will block. This method returns the number of
	 * bytes available in the target stream.
	 * 
	 * @return the number of bytes available before blocking.
	 * 
	 * @throws IOException
	 *             If an error occurs in this stream.
	 */
	public int available() throws IOException {
		return in.available();
	}

	/**
	 * Close this FilterInputStream. This implementation closes the target
	 * stream.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this stream.
	 */
	public void close() throws IOException {
		in.close();
	}

	/**
	 * Set a Mark position in this FilterInputStream. The parameter
	 * <code>readLimit</code> indicates how many bytes can be read before a
	 * mark is invalidated. Sending reset() will reposition the Stream back to
	 * the marked position provided <code>readLimit</code> has not been
	 * surpassed.
	 * <p>
	 * This implementation sets a mark in the target stream.
	 * 
	 * @param readlimit
	 *            the number of bytes to be able to read before invalidating the
	 *            mark.
	 */
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	/**
	 * Answers a boolean indicating whether or not this FilterInputStream
	 * supports mark() and reset(). This implementation answers whether or not
	 * the target stream supports marking.
	 * 
	 * @return <code>true</code> if mark() and reset() are supported,
	 *         <code>false</code> otherwise.
	 */
	public boolean markSupported() {
		return in.markSupported();
	}

	/**
	 * Reads a single byte from this FilterInputStream and returns the result as
	 * an int. The low-order byte is returned or -1 of the end of stream was
	 * encountered. This implementation returns a byte from the target stream.
	 * 
	 * @return the byte read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public int read() throws IOException {
		return in.read();
	}

	/**
	 * Reads bytes from this FilterInputStream and stores them in byte array
	 * <code>buffer</code>. Answer the number of bytes actually read or -1 if
	 * no bytes were read and end of stream was encountered. This implementation
	 * reads bytes from the target stream.
	 * 
     * @param buffer
	 *            the byte array in which to store the read bytes.
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}

	/**
	 * Reads at most <code>count</code> bytes from this FilterInputStream and
	 * stores them in byte array <code>buffer</code> starting at
	 * <code>offset</code>. Answer the number of bytes actually read or -1 if
	 * no bytes were read and end of stream was encountered. This implementation
	 * reads bytes from the target stream.
	 * 
	 * @param buffer
	 *            the byte array in which to store the read bytes.
	 * @param offset
	 *            the offset in <code>buffer</code> to store the read bytes.
	 * @param count
	 *            the maximum number of bytes to store in <code>buffer</code>.
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public int read(byte[] buffer, int offset, int count) throws IOException {
		return in.read(buffer, offset, count);
	}

	/**
	 * Reset this FilterInputStream to the last marked location. If the
	 * <code>readlimit</code> has been passed or no <code>mark</code> has
	 * been set, throw IOException. This implementation resets the target
	 * stream.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public synchronized void reset() throws IOException {
		in.reset();
	}

	/**
	 * Skips <code>count</code> number of bytes in this InputStream.
	 * Subsequent <code>read()</code>'s will not return these bytes unless
	 * <code>reset()</code> is used. This implementation skips
	 * <code>count</code> number of bytes in the target stream.
	 * 
	 * @param count
	 *            the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 * 
	 * @throws IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public long skip(long count) throws IOException {
		return in.skip(count);
	}

}
