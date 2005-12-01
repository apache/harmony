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

package com.ibm.io.nio;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.ibm.misc.util.NotYetImplementedException;
import com.ibm.platform.IFileSystem;
import com.ibm.platform.Platform;
import com.ibm.platform.struct.PlatformAddress;

/*
 * The file channel impl class is the bridge between the logical channels
 * described by the NIO channel framework, and the file system implementation
 * provided by the port layer.
 * 
 * This class is non-API, but implements the API of the FileChannel interface.
 * 
 */
public class FileChannelImpl extends FileChannel {

	// Reference to the portable file system code.
	private static final IFileSystem fileSystem = Platform.getFileSystem();

	// Handle to the open file
	private final long handle;

	/*
	 * The mode in which we are allowed to manipulate the underlying file.
	 * defined in the interface IFileSystem.
	 */
	private final int openMode;

	// The object that will track all outstanding locks on this channel.
	private final LockManager lockManager = new LockManager();

	private final Object repositioningLock = new Object();

	// Latch that shows when the underlying file has definitely been closed.
	private boolean isClosed = false;

	/*
	 * Create a new file channel implementation class that wraps the given file
	 * handle and operates in the specififed mode.
	 * 
	 */
	public FileChannelImpl(long handle, int openMode) {
		super();
		this.handle = handle;
		this.openMode = openMode;
	}

	private final boolean isReadOnly() {
		return openMode == IFileSystem.O_RDONLY;
	}

	private final boolean isWriteOnly() {
		return openMode == IFileSystem.O_WRONLY;
	}

	private final boolean isReadWrite() {
		return openMode == IFileSystem.O_RDWR;
	}

	private final boolean isReadable() {
		return isReadOnly() || isReadWrite();
	}

	private final boolean isWritable() {
		return isWriteOnly() || isReadWrite();
	}

	/*
	 * Helper method to throw an exception if the channel is already closed.
	 * Note that we don't bother to synchronize on this test since the file may
	 * be closed by operations beyond our control anyways.
	 */
	protected final void openCheck() throws ClosedChannelException {
		if (isClosed) {
			throw new ClosedChannelException();
		}
	}

	protected final void writeCheck() throws ClosedChannelException,
			NonWritableChannelException {
		openCheck();
		if (!isWritable()) {
			throw new NonWritableChannelException();
		}
	}

	protected final void readCheck() throws ClosedChannelException,
			NonReadableChannelException {
		openCheck();
		if (!isReadable()) {
			throw new NonReadableChannelException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.spi.AbstractInterruptibleChannel#implCloseChannel()
	 */
	public void closeChannel() throws IOException {
		fileSystem.close(handle);
	}

	protected FileLock basicLock(long position, long size, boolean shared,
			boolean wait) throws IOException {
		if ((position < 0) || (size < 0)) {
			throw new IllegalArgumentException(
					"Lock position and size must be non-negative."); //$NON-NLS-1$
		}
		int lockType;
		if (shared) {
			readCheck();
			lockType = IFileSystem.SHARED_LOCK_TYPE;
		} else {
			writeCheck();
			lockType = IFileSystem.EXCLUSIVE_LOCK_TYPE;
		}

		FileLock pendingLock = new FileLockImpl(this, position, size, shared);
		lockManager.addLock(pendingLock);

		if (fileSystem.lock(handle, position, size, lockType, wait)) {
			return pendingLock;
		}

		// Lock acquisition failed
		lockManager.removeLock(pendingLock);
		return null;
	}

	/*
	 * Acquire a lock on the receiver, blocks if the lock cannot be obtained
	 * immediately.
	 * 
	 * @see java.nio.channels.FileChannel#lock(long, long, boolean)
	 */
	public FileLock lock(long position, long size, boolean shared)
			throws IOException {
		return basicLock(position, size, shared, true);
	}

	/*
	 * Attempts to acquire the given lock, but does not block. If the lock
	 * cannot be acquired the method returns null.
	 * 
	 * @see java.nio.channels.FileChannel#tryLock(long, long, boolean)
	 */
	public FileLock tryLock(long position, long size, boolean shared)
			throws IOException {
		return basicLock(position, size, shared, false);
	}

	/*
	 * Non-API method to release a given lock on a file channel. Assumes that
	 * the lock will mark itself invalid after successful unlocking.
	 */
	void release(FileLock lock) throws IOException {
		openCheck();
		fileSystem.unlock(handle, lock.position(), lock.size());
		lockManager.removeLock(lock);
	}

	/*
	 * Flush the contents of the file to disk, and the metadata if asked.
	 */
	public void force(boolean metadata) throws IOException {
		openCheck();
		// Forcing data-only on a read-only file is a no-op.
		if (!metadata & isReadOnly()) {
			return;
		}
		fileSystem.fflush(handle, metadata);
	}

	public MappedByteBuffer map(MapMode mode, long position, long size)
			throws IOException {
		openCheck();

		if (mode == null) {
			throw new NullPointerException();
		}
		int mapMode;
		if (mode == MapMode.READ_ONLY) {
			mapMode = IFileSystem.MMAP_READ_ONLY;
		} else {
			if (mode == MapMode.READ_WRITE) {
				mapMode = IFileSystem.MMAP_READ_WRITE;
			} else {
				mapMode = IFileSystem.MMAP_WRITE_COPY;
			}
		}
		PlatformAddress address = fileSystem.mmap(handle, position, size,
				mapMode);

		throw new NotYetImplementedException();
	}

	/*
	 * Answers the current file position.
	 */
	public long position() throws IOException {
		openCheck();
		return fileSystem.seek(handle, 0L, IFileSystem.SEEK_CUR);
	}

	/*
	 * Sets the file pointer.
	 */
	public FileChannel position(long newPosition) throws IOException {
		if (newPosition < 0) {
			throw new IllegalArgumentException(
					"New position must be non-negative."); //$NON-NLS-1$
		}
		openCheck();

		synchronized (repositioningLock) {
			fileSystem.seek(handle, newPosition, IFileSystem.SEEK_SET);
		}
		return this;
	}

	public int read(ByteBuffer buffer, long position) throws IOException {
		synchronized (repositioningLock) {
			int bytesRead = 0;
			long preReadPosition = position();
			position(position);
			try {
				bytesRead = read(buffer);
			} finally {
				position(preReadPosition);
			}
			return bytesRead;
		}
	}

	public int read(ByteBuffer buffer) throws IOException {
		readCheck();
		int bytesRead;
		synchronized (repositioningLock) {
			if (buffer.isDirect()) {
				DirectByteBuffer directBuffer = (DirectByteBuffer) buffer;
				long address = directBuffer.getEffectiveAddress().toLong();
				bytesRead = (int) fileSystem.readDirect(handle, address, buffer
						.remaining());
			} else {
				HeapByteBuffer heapBuffer = (HeapByteBuffer) buffer;
				bytesRead = (int) fileSystem.read(handle, heapBuffer.array(),
						heapBuffer.arrayOffset(), buffer.remaining());
			}
			if (bytesRead > 0) {
				buffer.position(buffer.position() + bytesRead);
			}
		}
		return bytesRead;
	}

	public long read(ByteBuffer[] buffers, int offset, int length)
			throws IOException {
		readCheck();
		throw new NotYetImplementedException();
	}

	/*
	 * Answers the current file size, as an integer number of bytes.
	 */
	public long size() throws IOException {
		readCheck();
		synchronized (repositioningLock) {
			long currentPosition = fileSystem.seek(handle, 0L,
					IFileSystem.SEEK_CUR);
			long endOfFilePosition = fileSystem.seek(handle, 0L,
					IFileSystem.SEEK_END);
			fileSystem.seek(handle, currentPosition, IFileSystem.SEEK_SET);
			return endOfFilePosition;
		}
	}

	public long transferFrom(ReadableByteChannel src, long position, long count)
			throws IOException {
		writeCheck();
		throw new NotYetImplementedException();
	}

	public long transferTo(long position, long count, WritableByteChannel target)
			throws IOException {
		readCheck();
		throw new NotYetImplementedException();
	}

	public FileChannel truncate(long size) throws IOException {
		writeCheck();
		throw new NotYetImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
	 */

	public int write(ByteBuffer buffer, long position) throws IOException {
		synchronized (repositioningLock) {
			int bytesWritten = 0;
			long preWritePosition = position();
			position(position);
			try {
				bytesWritten = write(buffer);
			} finally {
				position(preWritePosition);
			}
			return bytesWritten;
		}
	}

	public int write(ByteBuffer buffer) throws IOException {
		writeCheck();
		int bytesWritten;
		synchronized (repositioningLock) {
			if (buffer.isDirect()) {
				DirectByteBuffer directBuffer = (DirectByteBuffer) buffer;
				long address = directBuffer.getEffectiveAddress().toLong();
				bytesWritten = (int) fileSystem.writeDirect(handle, address,
						buffer.remaining());
			} else {
				HeapByteBuffer heapBuffer = (HeapByteBuffer) buffer;
				bytesWritten = (int) fileSystem.write(handle, heapBuffer
						.array(), heapBuffer.arrayOffset(), buffer.remaining());
			}
			if (bytesWritten > 0) {
				buffer.position(buffer.position() + bytesWritten);
			}
		}
		return bytesWritten;
	}

	public long write(ByteBuffer[] buffers, int offset, int length)
			throws IOException {
		writeCheck();
		throw new NotYetImplementedException();
	}
}
