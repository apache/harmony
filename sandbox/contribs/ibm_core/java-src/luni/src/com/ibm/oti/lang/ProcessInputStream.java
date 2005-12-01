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

package com.ibm.oti.lang;


class ProcessInputStream extends java.io.InputStream {

	private long handle;

	private java.io.FileDescriptor fd;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization();

	static {
		oneTimeInitialization();
	}

	/**
	 * Open an InputStream based on the handle.
	 */
	protected ProcessInputStream(long handle) {
		this.fd = new java.io.FileDescriptor();
		setFDImpl(fd, handle);
		this.handle = handle;
	}

	/**
	 * Answers a int representing then number of bytes that are available before
	 * this InputStream will block.
	 * 
	 * @return the number of bytes available before blocking.
	 * 
	 * @throws java.io.IOException
	 *             If an error occurs in this InputStream.
	 */
	public int available() throws java.io.IOException {
		synchronized (this) {
			if (handle == -1)
				return -1;
			return availableImpl();
		}
	}

	/**
	 * Native to determine the bytes available.
	 */
	private native int availableImpl() throws java.io.IOException;

	/*
	 * There is no way, at the library/vm level, to know when the stream will be
	 * available for closing. If the user doesn't close it in his code, the
	 * finalize() will run (eventually ?) and close the dangling OS
	 * fileDescriptor.
	 */
	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * Close the stream.
	 */
	public void close() throws java.io.IOException {
		synchronized (this) {
			if (handle == -1)
				return;
			closeImpl();
			handle = -1;
		}
	}

	/**
	 * Native to close the stream.
	 */
	private native void closeImpl() throws java.io.IOException;

	/**
	 * Reads a single byte from this InputStream and returns the result as an
	 * int. The low-order byte is returned or -1 of the end of stream was
	 * encountered.
	 * 
	 * @return the byte read or -1 if end of stream.
	 * 
	 * @throws java.io.IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public int read() throws java.io.IOException {
		byte buf[] = new byte[1];
		synchronized (this) {
			if (readImpl(buf, 0, 1, handle) == -1)
				return -1;
		}

		return buf[0];
	}

	/**
	 * Reads bytes from the Stream and stores them in byte array
	 * <code>buffer</code>. Answer the number of bytes actually read or -1 if
	 * no bytes were read and end of stream was encountered.
	 * 
	 * @param buffer
	 *            the byte array in which to store the read bytes.
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws java.io.IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public int read(byte[] buffer) throws java.io.IOException {
		synchronized (this) {
			return readImpl(buffer, 0, buffer.length, handle);
		}
	}

	/**
	 * Reads at most <code>nbytes</code> bytes from the Stream and stores them
	 * in byte array <code>buffer</code> starting at <code>offset</code>.
	 * Answer the number of bytes actually read or -1 if no bytes were read and
	 * end of stream was encountered.
	 * 
	 * @param buffer
	 *            the byte array in which to store the read bytes.
	 * @param offset
	 *            the offset in <code>buffer</code> to store the read bytes.
	 * @param nbytes
	 *            the maximum number of bytes to store in <code>buffer</code>.
	 * @return the number of bytes actually read or -1 if end of stream.
	 * 
	 * @throws java.io.IOException
	 *             If the stream is already closed or another IOException
	 *             occurs.
	 */
	public int read(byte[] buffer, int offset, int nbytes)
			throws java.io.IOException {
		synchronized (this) {
			if (handle == -1)
				return -1;
			if ((nbytes < 0 || nbytes > buffer.length)
					|| (offset < 0 || offset > buffer.length)
					|| ((nbytes + offset) > buffer.length))
				throw new ArrayIndexOutOfBoundsException();
			return readImpl(buffer, offset, nbytes, handle);
		}
	}

	/**
	 * Native to read into the buffer from the stream.
	 */
	private native int readImpl(byte[] buf, int offset, int nbytes, long hndl)
			throws java.io.IOException;

	/**
	 * Native to set the FileDescriptor handle.
	 */
	private native void setFDImpl(java.io.FileDescriptor fd, long handle);
}
