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

import com.ibm.platform.struct.PlatformAddress;

/**
 * TODO Type description
 * 
 */
public interface IFileSystem extends ISystemComponent {

	public final int SHARED_LOCK_TYPE = 1;

	public final int EXCLUSIVE_LOCK_TYPE = 2;

	public final int SEEK_SET = 1;

	public final int SEEK_CUR = 2;

	public final int SEEK_END = 4;

	public final int MMAP_READ_ONLY = 1;

	public final int MMAP_READ_WRITE = 2;

	public final int MMAP_WRITE_COPY = 4;

	public final int O_RDONLY = 0x00000000;

	public final int O_WRONLY = 0x00000001;

	public final int O_RDWR = 0x00000010;

	public final int O_APPEND = 0x00000100;

	public final int O_CREAT = 0x00001000;

	public final int O_EXCL = 0x00010000;

	public final int O_NOCTTY = 0x00100000;

	public final int O_NONBLOCK = 0x01000000;

	public final int O_TRUNC = 0x10000000;

	public long read(long fileDescriptor, byte[] bytes, int offset, int length)
			throws IOException;

	public long write(long fileDescriptor, byte[] bytes, int offset, int length)
			throws IOException;

	public long readv(long fileDescriptor, byte[] bytes, int[] offsets,
			int[] lengths) throws IOException;

	public long writev(long fileDescriptor, byte[] bytes, int[] offsets,
			int[] lengths) throws IOException;

	// Required to support direct byte buffers
	public long readDirect(long fileDescriptor, long address, int length)
			throws IOException;

	public long writeDirect(long fileDescriptor, long address, int length)
			throws IOException;

	public boolean lock(long fileDescriptor, long start, long length, int type,
			boolean waitFlag) throws IOException;

	public void unlock(long fileDescriptor, long start, long length)
			throws IOException;

	public long seek(long fileDescriptor, long offset, int whence)
			throws IOException;

	public void fflush(long fileDescriptor, boolean metadata)
			throws IOException;

	public PlatformAddress mmap(long fileDescriptor, long offset, long size,
			int mapMode) throws IOException;

	public void close(long fileDescriptor) throws IOException;
}
