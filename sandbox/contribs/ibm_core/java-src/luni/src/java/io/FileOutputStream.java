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

package java.io;


import java.nio.channels.FileChannel;

/**
 * FileOutputStream is a class whose underlying stream is represented by a file
 * in the operating system. The bytes that are written to this stream are passed
 * directly to the underlying operating system equivalent function. Since
 * overhead may be high in writing to the OS, FileOutputStreams are usually
 * wrapped with a BufferedOutputStream to reduce the number of times the OS is
 * called.
 * <p>
 * <code>BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream("aFile.txt"));</code>
 * 
 * @see FileInputStream
 */
public class FileOutputStream extends OutputStream {

	/**
	 * The FileDescriptor representing this FileOutputStream.
	 */
	FileDescriptor fd;

	// The unique file channel associated with this FileInputStream (lazily
	// initialized).
	private FileChannel channel;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization();

	static {
		oneTimeInitialization();
	}

	/**
	 * Constructs a new FileOutputStream on the File <code>file</code>. If
	 * the file exists, it is written over. See the constructor which can append
	 * to the file if so desired.
	 * 
	 * @param file
	 *            the File on which to stream reads.
	 * 
	 * @throws FileNotFoundException
	 *             If the <code>file</code> cannot be opened for writing.
	 * 
	 * @see java.lang.SecurityManager#checkWrite(FileDescriptor)
	 */
	public FileOutputStream(File file) throws FileNotFoundException {
		this(file, false);
	}

	/**
	 * Constructs a new FileOutputStream on the File <code>file</code>. If
	 * the file exists, it is written over. The parameter <code>append</code>
	 * determines whether or not the file is opened and appended to or just
	 * opened empty.
	 * 
	 * @param file
	 *            the File on which to stream reads.
	 * @param append
	 *            a boolean indicating whether or not to append to an existing
	 *            file.
	 * 
	 * @throws FileNotFoundException
	 *             If the <code>file</code> cannot be opened for writing.
	 * 
	 * @see java.lang.SecurityManager#checkWrite(FileDescriptor)
	 * @see java.lang.SecurityManager#checkWrite(String)
	 */
	public FileOutputStream(File file, boolean append)
			throws FileNotFoundException {
		super();
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkWrite(file.getPath());
		fd = new FileDescriptor();
		if (openImpl(file.properPath(true), append) != 0)
			throw new FileNotFoundException(file.getPath());

	}

	/**
	 * Constructs a new FileOutputStream on the FileDescriptor <code>fd</code>.
	 * The file must already be open, therefore no <code>FileIOException</code>
	 * will be thrown.
	 * 
	 * @param fd
	 *            the FileDescriptor on which to stream writes.
	 * 
	 * @see java.lang.SecurityManager#checkWrite(FileDescriptor)
	 */
	public FileOutputStream(FileDescriptor fd) {
		super();
		if (fd != null) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkWrite(fd);
			this.fd = fd;
		} else
			throw new NullPointerException(com.ibm.oti.util.Msg
					.getString("K006c")); //$NON-NLS-1$
	}

	/**
	 * Constructs a new FileOutputStream on the file named <code>fileName</code>.
	 * If the file exists, it is written over. See the constructor which can
	 * append to the file if so desired. The <code>fileName</code> may be
	 * absolute or relative to the System property <code>"user.dir"</code>.
	 * 
	 * @param filename
	 *            the file on which to stream writes.
	 * 
	 * @throws FileNotFoundException
	 *             If the <code>filename</code> cannot be opened for writing.
	 */
	public FileOutputStream(String filename) throws FileNotFoundException {
		this(filename, false);
	}

	/**
	 * Constructs a new FileOutputStream on the file named <code>filename</code>.
	 * If the file exists, it is written over. The parameter <code>append</code>
	 * determines whether or not the file is opened and appended to or just
	 * opened empty. The <code>filename</code> may be absolute or relative to
	 * the System property <code>"user.dir"</code>.
	 * 
	 * @param filename
	 *            the file on which to stream writes.
	 * @param append
	 *            a boolean indicating whether or not to append to an existing
	 *            file.
	 * 
	 * @throws FileNotFoundException
	 *             If the <code>filename</code> cannot be opened for writing.
	 */
	public FileOutputStream(String filename, boolean append)
			throws FileNotFoundException {
		super();
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkWrite(filename);
		fd = new FileDescriptor();
		File f = new File(filename);
		if (openImpl(f.properPath(true), append) != 0)
			throw new FileNotFoundException(filename);

	}

	/**
	 * Close the FileOutputStream. This implementation closes the underlying OS
	 * resources allocated to represent this stream.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to close this FileOutputStream.
	 */
	public void close() throws IOException {
		closeImpl();
	}

	private native void closeImpl() throws IOException;

	/**
	 * Frees any resources allocated to represent this FileOutputStream before
	 * it is garbage collected. This method is called from the Java Virtual
	 * Machine.
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to finalize this
	 *             FileOutputStream.
	 */
	protected void finalize() throws IOException {
		if (fd != null)
			close();
	}

	/**
	 * Answers the FileChannel equivalent to this output stream.
	 * <p>
	 * The file channel is write-only and has an initial position within the
	 * file that is the same as the current position of this FileOutputStream
	 * within the file. All changes made to the underlying file descriptor state
	 * via the channel are visible by the output stream and vice versa.
	 * </p>
	 * 
	 * @return the file channel representation for this FileOutputStream.
	 */
	public synchronized FileChannel getChannel() {
		if (channel == null) {
			channel = FileChannelFactory.getFileChannel(fd.descriptor,
					FileChannelFactory.O_WRONLY);
		}
		return channel;
	}

	/**
	 * Answers a FileDescriptor which represents the lowest level representation
	 * of a OS stream resource.
	 * 
	 * @return a FileDescriptor representing this FileOutputStream.
	 * 
	 * @throws IOException
	 *             If the Stream is already closed and there is no
	 *             FileDescriptor.
	 */
	public final FileDescriptor getFD() throws IOException {
		if (fd != null)
			return fd;
		throw new IOException();
	}

	private native int openImpl(byte[] fileName, boolean openAppend);

	/**
	 * Writes the entire contents of the byte array <code>buffer</code> to
	 * this FileOutputStream.
	 * 
	 * @param buffer
	 *            the buffer to be written
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             FileOutputStream.
	 */
	public void write(byte[] buffer) throws IOException {
		write(buffer, 0, buffer.length);
	}

	/**
	 * Writes <code>count</code> <code>bytes</code> from the byte array
	 * <code>buffer</code> starting at <code>offset</code> to this
	 * FileOutputStream.
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
	 *             FileOutputStream.
	 * @throws IndexOutOfBoundsException
	 *             If offset or count are outside of bounds.
	 * @throws NullPointerException
	 *             If buffer is <code>null</code>.
	 */
	public void write(byte[] buffer, int offset, int count) throws IOException {
		if (fd == null)
			throw new IOException();
		writeImpl(buffer, offset, count, getFD().descriptor);
	}

	private native void writeImpl(byte[] buffer, int offset, int count,
			long descriptor) throws IOException;

	/**
	 * Writes the specified byte <code>oneByte</code> to this
	 * FileOutputStream. Only the low order byte of <code>oneByte</code> is
	 * written.
	 * 
	 * @param oneByte
	 *            the byte to be written
	 * 
	 * @throws IOException
	 *             If an error occurs attempting to write to this
	 *             FileOutputStream.
	 */
	public void write(int oneByte) throws IOException {
		if (fd != null) {
			writeByteImpl(oneByte, getFD().descriptor);
		} else
			throw new IOException();
	}

	private native void writeByteImpl(int oneByte, long descriptor)
			throws IOException;

}
