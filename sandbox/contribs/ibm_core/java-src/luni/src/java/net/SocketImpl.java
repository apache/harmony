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

package java.net;


import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * The abstract superclass of all classes that implement streaming sockets.
 * 
 * Streaming sockets are wrapped by two classes, ServerSocket and Socket at the
 * server and client end of a connection respectively. At the server there are
 * two types of sockets engaged in communication, the <code>ServerSocket</code>
 * on a well known port (hereafter refered to as the listener) used to establish
 * a connection and the resulting <code>Socket</code> (hereafter refered to as
 * host).
 * 
 * Some of the <code>SocketImpl</code> instance variable values must be
 * interpreted in the context of the wrapper. See the getter methods for these
 * details.
 */
public abstract class SocketImpl implements SocketOptions {

	protected InetAddress address;

	protected int port;

	protected FileDescriptor fd;

	protected int localport;

	int receiveTimeout;

	boolean streaming = true;

	boolean shutdownInput = false;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization(boolean jcl_supports_ipv6);

	static {
		oneTimeInitialization(true);
	}

	/**
	 * Construct a connection-oriented SocketImpl.
	 * 
	 * @see SocketImplFactory
	 */
	public SocketImpl() {
		initializeSocket();
	}

	/**
	 * Accepts a connection on the provided socket.
	 * 
	 * @param newSocket
	 *            the socket to accept connections on
	 * @exception SocketException
	 *                if an error occurs while accepting
	 */
	protected abstract void accept(SocketImpl newSocket) throws IOException;

	/**
	 * Answer the number of bytes that may be read from this socket without
	 * blocking. This call does not block.
	 * 
	 * @return int the number of bytes that may be read without blocking
	 * @exception SocketException
	 *                if an error occurs while peeking
	 */
	protected abstract int available() throws IOException;

	/**
	 * Binds this socket to the specified local host/port.
	 * 
	 * @param address
	 *            the local machine address to bind the socket to
	 * @param port
	 *            the port on the local machine to bind the socket to
	 * @exception IOException
	 *                if an error occurs while binding
	 */
	protected abstract void bind(InetAddress address, int port)
			throws IOException;

	/**
	 * Close the socket. Usage thereafter is invalid.
	 * 
	 * @exception IOException
	 *                if an error occurs while closing
	 */
	protected abstract void close() throws IOException;

	/**
	 * Connects this socket to the specified remote host/port.
	 * 
	 * @param host
	 *            the remote host to connect to
	 * @param port
	 *            the remote port to connect to
	 * @exception IOException
	 *                if an error occurs while connecting
	 */
	protected abstract void connect(String host, int port) throws IOException;

	/**
	 * Connects this socket to the specified remote host address/port.
	 * 
	 * @param address
	 *            the remote host address to connect to
	 * @param port
	 *            the remote port to connect to
	 * @exception IOException
	 *                if an error occurs while connecting
	 */
	protected abstract void connect(InetAddress address, int port)
			throws IOException;

	/**
	 * Creates a new unconnected socket. If streaming is true, create a stream
	 * socket, else a datagram socket.
	 * 
	 * @param isStreaming
	 *            true, if the socket is type streaming
	 * @exception SocketException
	 *                if an error occurs while creating the socket
	 */
	protected abstract void create(boolean isStreaming) throws IOException;

	/**
	 * Answer the socket's file descriptor.
	 * 
	 * @return FileDescriptor the socket FileDescriptor
	 */
	protected FileDescriptor getFileDescriptor() {
		return fd;
	}

	/**
	 * Answer the socket's address. Refering to the class comment: Listener: The
	 * local machines IP address to which this socket is bound. Host: The client
	 * machine, to which this socket is connected. Client: The host machine, to
	 * which this socket is connected.
	 * 
	 * @return InetAddress the socket address
	 */
	protected InetAddress getInetAddress() {
		return address;
	}

	/**
	 * Answer the socket input stream.
	 * 
	 * @return InputStream an InputStream on the socket
	 * @exception IOException
	 *                thrown if an error occurs while accessing the stream
	 */
	protected abstract InputStream getInputStream() throws IOException;

	/**
	 * Answer the socket's localport. The field is initialized to -1 and upon
	 * demand will go to the IP stack to get the bound value. See the class
	 * comment for the context of the local port.
	 * 
	 * @return int the socket localport
	 */

	protected int getLocalPort() {
		return localport;
	}

	/**
	 * Answer the nominated socket option.
	 * 
	 * @param optID
	 *            the socket option to retrieve
	 * @return Object the option value
	 * @exception SocketException
	 *                thrown if an error occurs while accessing the option
	 */
	public abstract Object getOption(int optID) throws SocketException;

	/**
	 * Answer the socket output stream.
	 * 
	 * @return OutputStream an OutputStream on the socket
	 * @exception IOException
	 *                thrown if an error occurs while accessing the stream
	 */
	protected abstract OutputStream getOutputStream() throws IOException;

	/**
	 * Answer the socket's remote port. This value is not meaningful when the
	 * socketImpl is wrapped by a ServerSocket.
	 * 
	 * @return int the remote port the socket is connected to
	 */
	protected int getPort() {
		return port;
	}

	/**
	 * Initialize the socket. By default, read & accept operations are blocking.
	 * The port fields are created with -1 values, to flag them as unset.
	 */
	void initializeSocket() {
		fd = new FileDescriptor();
		receiveTimeout = 0;
	}

	/**
	 * Listen for connection requests on this stream socket. Incoming connection
	 * requests are queued, up to the limit nominated by backlog. Additional
	 * requests are rejected. listen() may only be invoked on stream sockets.
	 * 
	 * @param backlog
	 *            the max number of outstanding connection requests
	 * @exception IOException
	 *                thrown if an error occurs while listening
	 */

	protected abstract void listen(int backlog) throws IOException;

	/**
	 * Answer the result of attempting to accept a connection request to this
	 * stream socket in the IP stack.
	 * 
	 * @param fdServer
	 *            the server socket FileDescriptor
	 * @param newSocket
	 *            the host socket a connection will be accepted on
	 * @param fdnewSocket
	 *            the FileDescriptor for the host socket
	 * @param timeout
	 *            the timeout that the server should listen for
	 * @exception SocketException
	 *                if an error occurs while accepting connections
	 */
	static native void acceptStreamSocketImpl(FileDescriptor fdServer,
			SocketImpl newSocket, FileDescriptor fdnewSocket, int timeout)
			throws IOException;

	/**
	 * Answer the number of bytes available to read on the socket without
	 * blocking.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @exception SocketException
	 *                if an error occurs while peeking
	 */
	static native int availableStreamImpl(FileDescriptor aFD)
			throws SocketException;

	/**
	 * Answer the result of attempting to create a stream socket in the IP
	 * stack.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @exception SocketException
	 *                if an error occurs while creating the socket
	 */
	static native void createStreamSocketImpl(FileDescriptor aFD,
			boolean preferIPv4Stack) throws SocketException;

	/**
	 * Allocate a datagram socket in the IP stack. The socket is associated with
	 * the <code>aFD</code>.
	 * 
	 * @param aFD
	 *            the FileDescriptor to associate with the socket
	 * 
	 * @exception SocketException
	 *                upon an allocation error
	 */

	static native void createDatagramSocketImpl(FileDescriptor aFD,
			boolean preferIPv4Stack) throws SocketException;

	/**
	 * Answer the result of attempting to listen on a stream socket in the IP
	 * stack.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param backlog
	 *            the number of connection requests that may be queued before
	 *            requests are rejected
	 * @exception SocketException
	 *                if an error occurs while listening
	 */
	static native void listenStreamSocketImpl(FileDescriptor aFD, int backlog)
			throws SocketException;

	/**
	 * Recieve at most <code>count</code> bytes into the buffer
	 * <code>data</code> at the <code>offset</code> on the socket.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param data
	 *            the receive buffer
	 * @param offset
	 *            the offset into the buffer
	 * @param count
	 *            the max number of bytes to receive
	 * @param timeout
	 *            the max time the read operation should block waiting for data
	 * @return int the actual number of bytes read
	 * @exception SocketException
	 *                if an error occurs while reading
	 */
	static native int receiveStreamImpl(FileDescriptor aFD, byte[] data,
			int offset, int count, int timeout) throws IOException;

	/**
	 * Send <code>count</code> bytes from the buffer <code>data</code> at
	 * the <code>offset</code>, on the socket.
	 * 
	 * @param data
	 *            the send buffer
	 * @param offset
	 *            the offset into the buffer
	 * @param count
	 *            the number of bytes to receive
	 * @return int the actual number of bytes sent
	 * @exception SocketException
	 *                if an error occurs while writing
	 */
	static native int sendStreamImpl(FileDescriptor fd, byte[] data,
			int offset, int count) throws IOException;

	/**
	 * In the IP stack, read at most <code>count</code> bytes off the socket
	 * into the <code>buffer</code>, at the <code>offset</code>. If the
	 * timeout is zero, block indefinitely waiting for data, otherwise wait the
	 * specified period (in milliseconds).
	 * 
	 * @param buffer
	 *            the buffer to read into
	 * @param offset
	 *            the offset into the buffer
	 * @param count
	 *            the max number of bytes to read
	 * @return int the actual number of bytes read
	 * @exception IOException
	 *                thrown if an error occurs while reading
	 */

	int read(byte[] buffer, int offset, int count) throws IOException {
		if (shutdownInput)
			return -1;
		try {
			int read = receiveStreamImpl(fd, buffer, offset, count,
					receiveTimeout);
			if (read == -1)
				shutdownInput = true;
			return read;
		} catch (InterruptedIOException e) {
			throw new SocketTimeoutException(e.getMessage());
		}
	}

	/**
	 * Set the nominated socket option.
	 * 
	 * @param optID
	 *            the socket option to set
	 * @param val
	 *            the option value
	 * @exception SocketException
	 *                thrown if an error occurs while setting the option
	 */

	public abstract void setOption(int optID, Object val)
			throws SocketException;

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * socket.
	 * 
	 * @return String the description
	 */
	public String toString() {
		return new StringBuffer(100).append("Socket[addr=").append(
				getInetAddress()).append(",port=").append(port).append(
				",localport=").append(getLocalPort()).append("]").toString();
	}

	/**
	 * In the IP stack, write at most <code>count</code> bytes on the socket
	 * from the <code>buffer</code>, from the <code>offset</code>.
	 * 
	 * @param buffer
	 *            the buffer to read into
	 * @param offset
	 *            the offset into the buffer
	 * @param count
	 *            the number of bytes to write
	 * @return int the actual number of bytes written
	 * @exception IOException
	 *                thrown if an error occurs while writing
	 */

	int write(byte[] buffer, int offset, int count) throws IOException {
		if (!streaming) {
			PlainSocketImpl2.sendDatagramImpl2(fd, buffer, offset, count, port,
					address);
		}
		return sendStreamImpl(fd, buffer, offset, count);
	}

	/**
	 * Shutdown the input portion of the socket.
	 */
	protected void shutdownInput() throws IOException {
		shutdownInput = true;
		shutdownInputImpl(fd);
	}

	private native void shutdownInputImpl(FileDescriptor descriptor) throws IOException;

	/**
	 * Shutdown the output portion of the socket.
	 */
	protected void shutdownOutput() throws IOException {
		shutdownOutputImpl(fd);
	}

	private native void shutdownOutputImpl(FileDescriptor descriptor)
			throws IOException;

	/**
	 * Connect the socket to the host/port specified by the SocketAddress with a
	 * specified timeout.
	 * 
	 * @param remoteAddr
	 *            the remote machine address and port to connect to
	 * @param timeout
	 *            the millisecond timeout value, the connect will block
	 *            indefinitely for a zero value.
	 * 
	 * @exception IOException
	 *                if a problem occurs during the connect
	 */
	protected abstract void connect(SocketAddress remoteAddr, int timeout)
			throws IOException;

	/**
	 * Answer if the socket supports urgent data. Subclasses should override
	 * this method.
	 * 
	 * @return false, subclasses must override
	 */
	protected boolean supportsUrgentData() {
		return false;
	}

	/**
	 * Send the single byte of urgent data on the socket.
	 * 
	 * @param value
	 *            the byte of urgent data
	 * 
	 * @exception IOException
	 *                when an error occurs sending urgent data
	 */
	protected abstract void sendUrgentData(int value) throws IOException;

	static native boolean supportsUrgentDataImpl(FileDescriptor fd);

	static native boolean sendUrgentDataImpl(FileDescriptor fd, byte value);
}
