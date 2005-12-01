/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
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


import java.nio.channels.FileChannel;

import com.ibm.io.nio.FileChannelImpl;

/**
 * A simple factory to provide a generic way to create FileChannel
 * implementation from within the java.io package.
 */
class FileChannelFactory {
	static final int O_RDONLY = 0x00000000;

	static final int O_WRONLY = 0x00000001;

	static final int O_RDWR = 0x00000010;

	static FileChannel getFileChannel(long fd, int mode) {
		return new FileChannelImpl(fd, mode);
	}
}
