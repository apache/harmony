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

package java.nio;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel.MapMode;

/**
 * <code>MappedByteBuffer</code> is a special kind of direct byte buffer,
 * which maps a region of file to memory.
 * <p>
 * <code>MappedByteBuffer</code> can be created by calling
 * {@link java.nio.channels.FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long) FileChannel.map}.
 * Once created, the mapping between the byte buffer and the file region remains
 * valid until the byte buffer is garbage collected.
 * </p>
 * <p>
 * All or part of a <code>MappedByteBuffer</code>'s content may change or
 * become inaccessible at any time, since the mapped file region can be modified
 * by another thread or process at any time. If this happens, the behavior of
 * the <code>MappedByteBuffer</code> is undefined.
 * </p>
 * 
 */
public abstract class MappedByteBuffer extends ByteBuffer {

	/**
	 * The map mode used when creating this buffer.
	 */
	protected final MapMode mapMode;

	/**
	 * The mapped file.
	 */
	protected final File mappedFile;

	/**
	 * The offset of the mapped region. The size of the mapped region is defined
	 * by capacity.
	 */
	protected final long offset;

	/**
	 * The wrapped byte buffer.
	 */
	protected final ByteBuffer wrappedBuffer;

	/**
	 * Create a new mapped byte buffer, mapping to the specified region of file.
	 * A new direct byte buffer will be allocated.
	 * 
	 * @param mappedFile
	 * @param offset
	 * @param size
	 * @param mapMode
	 */
	protected MappedByteBuffer(File mappedFile, long offset, int size,
			MapMode mapMode) {
		super(size);
		this.mappedFile = mappedFile;
		this.offset = offset;
		this.mapMode = mapMode;
		this.wrappedBuffer = ByteBuffer.allocateDirect(size);

		load();
	}

	/**
	 * Create a new mapped byte buffer, mapping to the specified region of file.
	 * The specified byte buffer is used.
	 * 
	 * @param mappedFile
	 * @param offset
	 * @param size
	 * @param mapMode
	 * @param wrappedBuffer
	 */
	protected MappedByteBuffer(File mappedFile, long offset, int size,
			MapMode mapMode, ByteBuffer wrappedBuffer) {
		super(size);
		this.mappedFile = mappedFile;
		this.offset = offset;
		this.mapMode = mapMode;
		this.wrappedBuffer = wrappedBuffer;
		this.wrappedBuffer.clear();

		if (wrappedBuffer.capacity() != size) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Writes all changes made to this buffer's content back to the file.
	 * <p>
	 * If this buffer is not mapped in read/write mode, then this method does
	 * nothing.
	 * </p>
	 * 
	 * @return This buffer
	 */
	public final MappedByteBuffer force() {
		// TODO re-impl in a direct way
		if (mapMode == MapMode.READ_WRITE) {
			RandomAccessFile accessFile = null;
			try {
				accessFile = new RandomAccessFile(mappedFile, "rw"); //$NON-NLS-1$
				accessFile.seek(offset);
				for (int i = 0; i < capacity; i++) {
					accessFile.writeByte(wrappedBuffer.get(i));
				}
			} catch (IOException e) {
				// javadoc does not specify how to report this exception
				e.printStackTrace();
			} finally {
				if (accessFile != null) {
					try {
						accessFile.close();
					} catch (IOException e) {
						// nothing to do
					}
				}
			}
		}
		return this;
	}

	/**
	 * Returns true if this buffer's content is loaded.
	 * 
	 * @return True if this buffer's content is loaded.
	 */
	public final boolean isLoaded() {
		return true;
	}

	/**
	 * Loads this buffer's content into memory.
	 * 
	 * @return This buffer
	 */
	public final MappedByteBuffer load() {
		// TODO re-impl in a direct way
		RandomAccessFile accessFile = null;
		try {
			accessFile = new RandomAccessFile(mappedFile, "r"); //$NON-NLS-1$
			accessFile.seek(offset);
			for (int i = 0; i < capacity; i++) {
				wrappedBuffer.put(i, accessFile.readByte());
			}
		} catch (IOException e) {
			// javadoc does not specify how to report this exception
			e.printStackTrace();
		} finally {
			if (accessFile != null) {
				try {
					accessFile.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}
		return this;
	}

}
