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


class ProcessOutputStream extends java.io.OutputStream {

	private long handle;

	private java.io.FileDescriptor fd;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization();

	static {
		oneTimeInitialization();
	}

	/**
	 * Open an OutputStream based on the handle.
	 */
	protected ProcessOutputStream(long handle) {
		this.fd = new java.io.FileDescriptor();
		setFDImpl(fd, handle);
		this.handle = handle;
	}

	/*
	 * There is no way, at the library/vm level, to know when the stream will be
	 * available for closing. If the user doesn't close it in its code, the
	 * finalize() will run (eventually ?) and close the dandling OS
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
	 * Native to set the FileDescriptor handle.
	 */
	private native void setFDImpl(java.io.FileDescriptor fd, long handle);

	/**
	 * Writes the entire contents of the byte array <code>buf</code> to this
	 * OutputStream.
	 * 
	 * @param buf
	 *            the buffer to be written
	 * 
	 * @throws java.io.IOException
	 *             If an error occurs attempting to write to this OutputStream.
	 */
	public void write(byte[] buf) throws java.io.IOException {
		synchronized (this) {
			writeImpl(buf, 0, buf.length, handle);
		}
	}

	/**
	 * Writes <code>nbytes</code> <code>bytes</code> from the byte array
	 * <code>buf</code> starting at <code>offset</code> to this
	 * OutputStream.
	 * 
	 * @param buf
	 *            the buffer to be written
	 * @param offset
	 *            offset in buffer to get bytes
	 * @param nbytes
	 *            number of bytes in buffer to write
	 * 
	 * @throws java.io.IOException
	 *             If an error occurs attempting to write to this OutputStream.
	 * @throws java.lang.IndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 */
	public void write(byte[] buf, int offset, int nbytes)
			throws java.io.IOException {
		synchronized (this) {
			if (handle == -1)
				return;
			writeImpl(buf, offset, nbytes, handle);
		}
	}

	/**
	 * Writes the specified byte <code>oneByte</code> to this OutputStream.
	 * Only the low order byte of <code>oneByte</code> is written.
	 * 
	 * @param oneByte
	 *            the byte to be written
	 * 
	 * @throws java.io.IOException
	 *             If an error occurs attempting to write to this OutputStream.
	 */
	public void write(int oneByte) throws java.io.IOException {
		byte buf[] = new byte[1];
		buf[0] = (byte) oneByte;
		synchronized (this) {
			writeImpl(buf, 0, 1, handle);
		}
	}

	/**
	 * Native to write the buffer to the stream.
	 */
	private native void writeImpl(byte[] buf, int offset, int nbytes, long hndl)
			throws java.io.IOException;
}
