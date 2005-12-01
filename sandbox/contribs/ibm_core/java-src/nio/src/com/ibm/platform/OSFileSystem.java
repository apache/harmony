/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.platform;


import java.io.IOException;

import com.ibm.misc.util.NotYetImplementedException;
import com.ibm.platform.struct.PlatformAddress;

/**
 * This is the portable implementation of the file system interface.
 * 
 */
public class OSFileSystem extends OSComponent implements IFileSystem {

	/**
	 * 
	 */
	public OSFileSystem() {
		super();
		// Auto-generated constructor stub
	}

	private final void validateLockArgs(int type, long start, long length) {
		if ((type != IFileSystem.SHARED_LOCK_TYPE)
				&& (type != IFileSystem.EXCLUSIVE_LOCK_TYPE)) {
			throw new IllegalArgumentException("Illegal lock type requested."); //$NON-NLS-1$
		}

		// Start position
		if (start < 0) {
			throw new IllegalArgumentException(
					"Lock start position must be non-negative"); //$NON-NLS-1$
		}

		// Length of lock stretch
		if (length < 0) {
			throw new IllegalArgumentException(
					"Lock length must be non-negative"); //$NON-NLS-1$
		}
	}

	private native int lockImpl(long fileDescriptor, long start, long length,
			int type, boolean wait);

	public boolean lock(long fileDescriptor, long start, long length, int type,
			boolean waitFlag) throws IOException {
		// Validate arguments
		validateLockArgs(type, start, length);
		int result = lockImpl(fileDescriptor, start, length, type, waitFlag);
		return result != -1;
	}

	private native int unlockImpl(long fileDescriptor, long start, long length);

	public void unlock(long fileDescriptor, long start, long length)
			throws IOException {
		// Validate arguments
		validateLockArgs(IFileSystem.SHARED_LOCK_TYPE, start, length);
		int result = unlockImpl(fileDescriptor, start, length);
		if (result == -1) {
			throw new IOException();
		}
	}

	private native int fflushImpl(long fd, boolean metadata);

	public void fflush(long fileDescriptor, boolean metadata)
			throws IOException {
		int result = fflushImpl(fileDescriptor, metadata);
		if (result == -1) {
			throw new IOException();
		}
	}

	/*
	 * File position seeking.
	 */

	private native long seekImpl(long fd, long offset, int whence);

	public long seek(long fileDescriptor, long offset, int whence)
			throws IOException {
		long pos = seekImpl(fileDescriptor, offset, whence);
		if (pos == -1) {
			throw new IOException();
		}
		return pos;
	}

	/*
	 * Direct read/write APIs work on addresses.
	 */
	private native long readDirectImpl(long fileDescriptor, long address,
			int length);

	public long readDirect(long fileDescriptor, long address, int length)
			throws IOException {
		long bytesRead = readDirectImpl(fileDescriptor, address, length);
		if (bytesRead < -1) {
			throw new IOException();
		}
		return bytesRead;
	}

	private native long writeDirectImpl(long fileDescriptor, long address,
			int length);

	public long writeDirect(long fileDescriptor, long address, int length)
			throws IOException {
		long bytesWritten = writeDirectImpl(fileDescriptor, address, length);
		if (bytesWritten < -1) {
			throw new IOException();
		}
		return bytesWritten;
	}

	/*
	 * Indirect read/writes work on byte[]'s
	 */
	private native long readImpl(long fileDescriptor, byte[] bytes, int offset,
			int length);

	public long read(long fileDescriptor, byte[] bytes, int offset, int length)
			throws IOException {
		long bytesRead = readImpl(fileDescriptor, bytes, offset, length);
		if (bytesRead < -1) {
			throw new IOException();
		}
		return bytesRead;
	}

	public long write(long fileDescriptor, byte[] bytes, int offset, int length)
			throws IOException {
		// Auto-generated method stub
		throw new NotYetImplementedException();
	}

	/*
	 * Scatter/gather calls.
	 */
	public long readv(long fileDescriptor, byte[] bytes, int[] offsets,
			int[] lengths) throws IOException {
		// Auto-generated method stub
		throw new NotYetImplementedException();
	}

	public long writev(long fileDescriptor, byte[] bytes, int[] offsets,
			int[] lengths) throws IOException {
		//  Auto-generated method stub
		throw new NotYetImplementedException();
	}

	/*
	 * Memory mapped file
	 */
	private native long mmapImpl(long fileDescriptor, long offset, long size,
			int mapMode);

	public PlatformAddress mmap(long fileDescriptor, long offset, long size,
			int mapMode) throws IOException {
		long address = mmapImpl(fileDescriptor, offset, size, mapMode);
		if (address == -1) {
			throw new IOException();
		}
		return PlatformAddress.on(address);

	}

	private native int closeImpl(long fileDescriptor);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.platform.IFileSystem#close(long)
	 */
	public void close(long fileDescriptor) throws IOException {
		int rc = closeImpl(fileDescriptor);
		if (rc == -1) {
			throw new IOException();
		}
	}
}
