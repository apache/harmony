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

package java.util.zip;


/**
 * The Inflater class is used to decompress bytes using the DEFLATE compression
 * algorithm. Inflation is performed by the ZLIB compression library.
 * 
 * @see DeflaterOutputStream
 * @see Inflater
 */
public class Inflater {

	private boolean finished = false; // Set by the inflateImpl native

	private boolean needsDictionary = false; // Set by the inflateImpl native

	private boolean noHeader = false;

	private long streamHandle = -1;

	private byte[] inputBuffer = null;

	int inRead = 0, inLength = 0;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization();

	static {
		oneTimeInitialization();
	}

	/**
	 * Release any resources associated with this Inflater. Any unused
	 * input/ouput is discarded. This is also called by the finalize method.
	 */
	public synchronized void end() {
		if (streamHandle != -1) {
			endImpl(streamHandle);
			inRead = 0;
			inputBuffer = null;
			inLength = 0;
			streamHandle = -1;
		}
	}

	private native synchronized void endImpl(long handle);

	protected void finalize() {
		end();
	}

	/**
	 * Indicates if the Inflater has inflated the entire defalted stream. If
	 * deflated bytes remain and needsInput returns true this method will return
	 * false. This method should be called after all deflated input is supplied
	 * to the Inflater.
	 * 
	 * @return True if all input has been inflated, false otherwise
	 */
	public synchronized boolean finished() {
		return finished;
	}

	/**
	 * Returns the Adler32 checksum of either all bytes inflated, or the
	 * checksum of the preset dictionary if one has been supplied.
	 * 
	 * @return The Adler32 checksum associayted with this Inflater.
	 */
	public synchronized int getAdler() {
		if (streamHandle == -1)
			throw new IllegalStateException();
		return getAdlerImpl(streamHandle);
	}

	private native synchronized int getAdlerImpl(long handle);

	/**
	 * Returns the number of bytes of current input remaining to be read by the
	 * inflater
	 * 
	 * @return Number of bytes of unred input.
	 */
	public synchronized int getRemaining() {
		return inLength - inRead;
	}

	/**
	 * Returns total number of bytes of input read by the Inflater.
	 * 
	 * @return Total bytes read
	 */
	public synchronized int getTotalIn() {
		if (streamHandle == -1)
			throw new IllegalStateException();

		return getTotalInImpl(streamHandle);
	}

	private synchronized native int getTotalInImpl(long handle);

	/**
	 * Returns total number of bytes of input output by the Inflater.
	 * 
	 * @return Total bytes output
	 */
	public synchronized int getTotalOut() {
		if (streamHandle == -1)
			throw new IllegalStateException();

		return getTotalOutImpl(streamHandle);
	}

	private native synchronized int getTotalOutImpl(long handle);

	/**
	 * Inflates bytes from current input and stores them in buf.
	 * 
	 * @param buf
	 *            Buffer to output inflated bytes
	 * @return Number of bytes inflated
	 * @exception DataFormatException
	 *                If the underlying stream is corrupted or was not DEFLATED
	 * 
	 */
	public int inflate(byte[] buf) throws DataFormatException {
		return inflate(buf, 0, buf.length);
	}

	/**
	 * Inflates up to nbytes bytes from current input and stores them in buf
	 * starting at off.
	 * 
	 * @param buf
	 *            Buffer to output inflated bytes
	 * @param off
	 *            Offset in buffer into wcih to store inflated bytes
	 * @param nbytes
	 *            Number of inflated bytes to store
	 * @exception DataFormatException
	 *                If the underlying stream is corrupted or was not DEFLATED
	 * @return Number of bytes inflated
	 */
	public synchronized int inflate(byte[] buf, int off, int nbytes)
			throws DataFormatException {
		// avoid int overflow, check null buf
		if (off <= buf.length && nbytes >= 0 && off >= 0
				&& buf.length - off >= nbytes) {
			if (streamHandle == -1)
				throw new IllegalStateException();

			boolean neededDict = needsDictionary;
			needsDictionary = false;
			int result = inflateImpl(buf, off, nbytes, streamHandle);
			if (needsDictionary && neededDict)
				throw new DataFormatException(com.ibm.oti.util.Msg
						.getString("K0324"));
			return result;
		}
		throw new ArrayIndexOutOfBoundsException();
	}

	private native synchronized int inflateImpl(byte[] buf, int off,
			int nbytes, long handle);

	/**
	 * Constructs a new Inflater instance.
	 */
	public Inflater() {
		this(false);
	}

	/**
	 * Constructs a new Inflater instance. If noHeader is true the Inflater will
	 * not attempt to read a ZLIB header.
	 * 
	 * @param noHeader
	 *            If true, read a ZLIB header from input.
	 */
	public Inflater(boolean noHeader) {
		this.noHeader = noHeader;
		streamHandle = createStream(noHeader);
	}

	/**
	 * Indicates whether the input bytes were compressed with a preset
	 * dictionary. This method should be called prior to inflate() to determine
	 * if a dictionary is required. If so setDictionary() should be called with
	 * the appropriate dictionary prior to calling inflate().
	 * 
	 * @return true if a preset dictionary is required for inflation.
	 * @see #setDictionary(byte[])
	 * @see #setDictionary(byte[], int, int)
	 */
	public synchronized boolean needsDictionary() {
		if (inputBuffer == null)
			throw new IllegalStateException();
		return needsDictionary;
	}

	public synchronized boolean needsInput() {
		return inRead == inLength;
	}

	/**
	 * Resets the Inflater.
	 */
	public synchronized void reset() {
		if (streamHandle == -1)
			throw new NullPointerException();
		inputBuffer = null;
		finished = false;
		needsDictionary = false;
		inLength = inRead = 0;
		resetImpl(streamHandle);
	}

	private native synchronized void resetImpl(long handle);

	/**
	 * Sets the preset dictionary to be used for inflation to buf.
	 * needsDictioanry() can be called to determine whether the current input
	 * was deflated using a preset dictionary.
	 * 
	 * @param buf
	 *            The buffer containing the dictionary bytes
	 * @see #needsDictionary
	 */
	public synchronized void setDictionary(byte[] buf) {
		setDictionary(buf, 0, buf.length);
	}

	public synchronized void setDictionary(byte[] buf, int off, int nbytes) {
		if (streamHandle == -1)
			throw new IllegalStateException();
		// avoid int overflow, check null buf
		if (off <= buf.length && nbytes >= 0 && off >= 0
				&& buf.length - off >= nbytes)
			setDictionaryImpl(buf, off, nbytes, streamHandle);
		else
			throw new ArrayIndexOutOfBoundsException();
	}

	private native synchronized void setDictionaryImpl(byte[] buf, int off,
			int nbytes, long handle);

	/**
	 * Sets the current input to buf. This method should only be called if
	 * needsInput() returns true.
	 * 
	 * @param buf
	 *            input buffer
	 * @see #needsInput
	 */
	public synchronized void setInput(byte[] buf) {
		setInput(buf, 0, buf.length);
	}

	/**
	 * Sets the current input to the region of buf starting at off and ending at
	 * nbytes - 1. This method should only be called if needsInput() returns
	 * true.
	 * 
	 * @param buf
	 *            input buffer
	 * @param off
	 *            offest to read from in buffer
	 * @param nbytes
	 *            number of bytes to read
	 * @see #needsInput
	 */
	public synchronized void setInput(byte[] buf, int off, int nbytes) {
		if (streamHandle == -1)
			throw new IllegalStateException();
		// avoid int overflow, check null buf
		if (off <= buf.length && nbytes >= 0 && off >= 0
				&& buf.length - off >= nbytes) {
			inputBuffer = buf;
			inRead = 0;
			inLength = nbytes;
			setInputImpl(buf, off, nbytes, streamHandle);
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private native synchronized void setInputImpl(byte[] buf, int off,
			int nbytes, long handle);

	private native long createStream(boolean noHeader1);
}
