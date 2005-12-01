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


import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.security.AccessController;

import com.ibm.oti.util.Msg;
import com.ibm.oti.util.PriviAction;

/**
 * A concrete connected-socket implementation.
 */
class PlainSocketImpl extends SocketImpl {

	// For SOCKS support. A SOCKS bind() uses the last
	// host connected to in its request.
	static private InetAddress lastConnectedAddress;

	static private int lastConnectedPort;

	private boolean tcpNoDelay = true;

	private Object connectLock = new Object();

	// used to store the trafficClass value which is simply returned
	// as the value that was set. We also need it to pass it to methods
	// that specify an address packets are going to be sent to
	private int trafficClass = 0;

	/**
	 * Accepts a connection on the provided socket, by calling the IP stack.
	 * 
	 * @param newImpl
	 *            the socket to accept connections on
	 * @exception SocketException
	 *                if an error occurs while accepting
	 */
	protected void accept(SocketImpl newImpl) throws IOException {
		if (usingSocks()) {
			((PlainSocketImpl) newImpl).socksBind();
			((PlainSocketImpl) newImpl).socksAccept();
			return;
		}

		try {
			acceptStreamSocketImpl(fd, newImpl, newImpl.fd, receiveTimeout);
		} catch (InterruptedIOException e) {
			throw new SocketTimeoutException(e.getMessage());
		}
		newImpl.localport = getLocalPort();
	}

	/**
	 * Answer the number of bytes that may be read from this socket without
	 * blocking. This call does not block.
	 * 
	 * @return int the number of bytes that may be read without blocking
	 * @exception SocketException
	 *                if an error occurs while peeking
	 */

	protected synchronized int available() throws IOException {
		// we need to check if the input has been shutdown. If so
		// we should return that there is no data to be read
		if (shutdownInput == true) {
			return 0;
		}
		return availableStreamImpl(fd);
	}

	/**
	 * Binds this socket to the specified local host/port. Binding to the 0 port
	 * implies binding to any available port. By not making the assignment to
	 * the instVar, the getLocalPort method will lazily go to the stack and
	 * query for the assigned port
	 * 
	 * @param anAddr
	 *            the local machine address to bind the socket to
	 * @param aPort
	 *            the port on the local machine to bind the socket to
	 * @exception IOException
	 *                if an error occurs while binding
	 */
	protected void bind(InetAddress anAddr, int aPort) throws IOException {
		if (usingSocks()) {
			socksBind();
			return;
		}

		PlainSocketImpl2.socketBindImpl2(fd, aPort, anAddr);
		address = anAddr;
		if (0 != aPort) {
			localport = aPort;
		} else {
			localport = Socket.getSocketLocalPortImpl(fd, InetAddress
					.preferIPv6Addresses());
		}
	}

	/**
	 * Close the socket. Usage thereafter is invalid.
	 * 
	 * @exception IOException
	 *                if an error occurs while closing
	 */
	protected void close() throws IOException {
		synchronized (fd) {
			if (fd.valid()) {
				if ((Socket.getSocketFlags() & Socket.FLAG_SHUTDOWN) != 0) {
					try {
						shutdownOutput();
					} catch (Exception e) {
					}
				}
				Socket.socketCloseImpl(fd);
				initializeSocket();
			}
		}
	}

	/**
	 * Connects this socket to the specified remote host/port. This method
	 * assumes the sender has verified the host with the security policy.
	 * 
	 * @param aHost
	 *            the remote host to connect to
	 * @param aPort
	 *            the remote port to connect to
	 * @exception IOException
	 *                if an error occurs while connecting
	 */
	protected void connect(String aHost, int aPort) throws IOException {
		InetAddress anAddr = InetAddress.getHostByNameImpl(aHost, InetAddress
				.preferIPv6Addresses());
		connect(anAddr, aPort);
	}

	/**
	 * Connects this socket to the specified remote host address/port.
	 * 
	 * @param anAddr
	 *            the remote host address to connect to
	 * @param aPort
	 *            the remote port to connect to
	 * @exception IOException
	 *                if an error occurs while connecting
	 */
	protected void connect(InetAddress anAddr, int aPort) throws IOException {
		connect(anAddr, aPort, 0);
	}

	/**
	 * Connects this socket to the specified remote host address/port.
	 * 
	 * @param anAddr
	 *            the remote host address to connect to
	 * @param aPort
	 *            the remote port to connect to
	 * @param timeout
	 *            a timeout where supported. 0 means no timeout
	 * @exception IOException
	 *                if an error occurs while connecting
	 */
	private void connect(InetAddress anAddr, int aPort, int timeout)
			throws IOException {
		InetAddress address = anAddr.equals(InetAddress.ANY) ? InetAddress.LOOPBACK
				: anAddr;

		try {
			if (streaming) {
				if (usingSocks())
					socksConnect(anAddr, aPort, 0);
				else {
					if (timeout == 0) {
						PlainSocketImpl2.connectStreamSocketImpl2(fd, aPort,
								trafficClass, address);
					} else {
						PlainSocketImpl2.connectStreamWithTimeoutSocketImpl2(
								fd, aPort, timeout, trafficClass, address);
					}
				}
			}
		} catch (ConnectException e) {
			throw new ConnectException(anAddr + ":" + aPort + " - "
					+ e.getMessage());
		}
		this.address = anAddr;
		this.port = aPort;
	}

	/**
	 * Creates a new unconnected socket. If streaming is true, create a stream
	 * socket, else a datagram socket. The deprecated datagram usage is not
	 * supported and will throw an exception.
	 * 
	 * @param streaming
	 *            true, if the socket is type streaming
	 * @exception SocketException
	 *                if an error occurs while creating the socket
	 */
	protected void create(boolean streaming) throws SocketException {
		this.streaming = streaming;
		if (streaming) {
			createStreamSocketImpl(fd, Socket.preferIPv4Stack());
		} else {
			createDatagramSocketImpl(fd, Socket.preferIPv4Stack());
		}
	}

	protected void finalize() throws IOException {
		close();
	}

	/**
	 * Answer the socket input stream.
	 * 
	 * @return InputStream an InputStream on the socket
	 * @exception IOException
	 *                thrown if an error occurs while accessing the stream
	 */
	protected synchronized InputStream getInputStream() throws IOException {
		if (!fd.valid())
			throw new SocketException(Msg.getString("K003d"));

		return new SocketInputStream(this);
	}

	/**
	 * Answer the nominated socket option. Receive timeouts are maintained in
	 * Java, rather than in the JNI code.
	 * 
	 * @param optID
	 *            the socket option to retrieve
	 * @return Object the option value
	 * @exception SocketException
	 *                thrown if an error occurs while accessing the option
	 */
	public Object getOption(int optID) throws SocketException {
		if (optID == SocketOptions.SO_TIMEOUT) {
			return new Integer(receiveTimeout);
		} else if (optID == SocketOptions.IP_TOS) {
			return new Integer(trafficClass);
		} else {
			// Call the native first so there will be
			// an exception if the socket if closed.
			Object result = Socket.getSocketOptionImpl(fd, optID);
			if (optID == SocketOptions.TCP_NODELAY
					&& (Socket.getSocketFlags() & Socket.TCP_NODELAY) != 0) {
				return new Boolean(tcpNoDelay);
			}
			return result;
		}
	}

	/**
	 * Answer the socket output stream.
	 * 
	 * @return OutputStream an OutputStream on the socket
	 * @exception IOException
	 *                thrown if an error occurs while accessing the stream
	 */
	protected synchronized OutputStream getOutputStream() throws IOException {
		if (!fd.valid())
			throw new SocketException(Msg.getString("K003d"));

		return new SocketOutputStream(this);
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
	protected void listen(int backlog) throws IOException {
		if (usingSocks()) {
			// Do nothing for a SOCKS connection. The listen occurs on the
			// server during the bind.
			return;
		}
		listenStreamSocketImpl(fd, backlog);
	}

	/**
	 * Set the nominated socket option. Receive timeouts are maintained in Java,
	 * rather than in the JNI code.
	 * 
	 * @param optID
	 *            the socket option to set
	 * @param val
	 *            the option value
	 * @exception SocketException
	 *                thrown if an error occurs while setting the option
	 */
	public void setOption(int optID, Object val) throws SocketException {
		if (optID == SocketOptions.SO_TIMEOUT) {
			receiveTimeout = ((Integer) val).intValue();
		} else {
			try {
				Socket.setSocketOptionImpl(fd, optID, val);
				if (optID == SocketOptions.TCP_NODELAY
						&& (Socket.getSocketFlags() & Socket.TCP_NODELAY) != 0)
					tcpNoDelay = ((Boolean) val).booleanValue();
			} catch (SocketException e) {

				// we don't through an exception for IP_TOS even if the platform
				// won't let us set the requested value
				if (optID != SocketOptions.IP_TOS) {
					throw e;
				}
			}

			// save this value as it is acutally used differently for IPv4 and
			// IPv6 so we cannot get the value using the getOption. The option
			// is actually only set for IPv4 and a masked version of the value
			// will be set as only a subset of the values are allowed on the
			// socket. Therefore we need to retain it to return the value that
			// was set. We also need the value to be passed into a number of
			// natives so that it can be used properly with IPv6
			if (optID == SocketOptions.IP_TOS) {
				trafficClass = ((Integer) val).intValue();
			}
		}
	}

	/**
	 * Answer whether to use a SOCKS proxy.
	 * 
	 * @return boolean
	 */
	static boolean usingSocks() {
		String proxySet = (String) AccessController
				.doPrivileged(new PriviAction("socksProxySet"));
		if (proxySet != null)
			return proxySet.toLowerCase().equals("true");

		return AccessController.doPrivileged(new PriviAction("socksProxyHost")) != null;
	}

	/**
	 * Get the SOCKS proxy server port.
	 */
	private int socksGetServerPort() {
		int portValue = -1;

		String proxyPort = (String) AccessController
				.doPrivileged(new PriviAction("socksProxyPort"));

		if (proxyPort != null) {
			portValue = Integer.parseInt(proxyPort);
		}
		if (portValue < 0)
			portValue = 1080;

		return portValue;
	}

	/**
	 * Get the InetAddress of the SOCKS proxy server.
	 */
	private InetAddress socksGetServerAddress() throws UnknownHostException {
		String proxyName = (String) AccessController
				.doPrivileged(new PriviAction("socksProxyHost"));

		InetAddress anAddr = InetAddress.getHostByNameImpl(proxyName,
				InetAddress.preferIPv6Addresses());
		return anAddr;
	}

	/**
	 * Connect using a SOCKS server.
	 */
	private void socksConnect(InetAddress applicationServerAddress,
			int applicationServerPort, int timeout) throws IOException {
		try {
			if (timeout == 0) {
				PlainSocketImpl2.connectStreamSocketImpl2(fd,
						socksGetServerPort(), trafficClass,
						socksGetServerAddress());
			} else {
				PlainSocketImpl2.connectStreamWithTimeoutSocketImpl2(fd,
						socksGetServerPort(), timeout, trafficClass,
						socksGetServerAddress());
			}

		} catch (Exception e) {
			throw new SocketException(Msg.getString("K003e", e));
		}

		socksRequestConnection(applicationServerAddress, applicationServerPort);

		lastConnectedAddress = applicationServerAddress;
		lastConnectedPort = applicationServerPort;
	}

	/**
	 * Request a SOCKS connection to the application server given. If the
	 * request fails to complete successfully, an exception is thrown.
	 */
	private void socksRequestConnection(InetAddress applicationServerAddress,
			int applicationServerPort) throws IOException {
		socksSendRequest(Socks4Message.COMMAND_CONNECT,
				applicationServerAddress, applicationServerPort);
		Socks4Message reply = socksReadReply();
		if (reply.getCommandOrResult() != Socks4Message.RETURN_SUCCESS) {
			throw new IOException(reply.getErrorString(reply
					.getCommandOrResult()));
		}
	}

	/**
	 * Perform an accept for a SOCKS bind.
	 */
	void socksAccept() throws IOException {
		Socks4Message reply = socksReadReply();
		if (reply.getCommandOrResult() != Socks4Message.RETURN_SUCCESS) {
			throw new IOException(reply.getErrorString(reply
					.getCommandOrResult()));
		}
	}

	/**
	 * Bind using a SOCKS server.
	 */
	private void socksBind() throws IOException {
		try {
			PlainSocketImpl2.connectStreamSocketImpl2(fd, socksGetServerPort(),
					trafficClass, socksGetServerAddress());
		} catch (Exception e) {
			throw new IOException(Msg.getString("K003f", e));
		}

		// There must be a connection to an application host for the bind to
		// work.
		if (lastConnectedAddress == null)
			throw new SocketException(Msg.getString("K0040"));

		// Use the last connected address and port in the bind request.
		socksSendRequest(Socks4Message.COMMAND_BIND, lastConnectedAddress,
				lastConnectedPort);
		Socks4Message reply = socksReadReply();

		if (reply.getCommandOrResult() != Socks4Message.RETURN_SUCCESS) {
			throw new IOException(reply.getErrorString(reply
					.getCommandOrResult()));
		}

		// A peculiarity of socks 4 - if the address returned is 0, use the
		// original socks server address.
		if (reply.getIP() == 0) {
			address = socksGetServerAddress();
		} else {
			// IPv6 support not yet required as
			// currently the Socks4Message.getIP() only returns int,
			// so only works with IPv4 4byte addresses
			byte[] replyBytes = new byte[4];
			InetAddress.intToBytes(reply.getIP(), replyBytes, 0);
			address = new InetAddress(replyBytes);
		}
		localport = reply.getPort();
	}

	/**
	 * Send a SOCKS V4 request.
	 */
	private void socksSendRequest(int command, InetAddress address, int port)
			throws IOException {
		Socks4Message request = new Socks4Message();
		request.setCommandOrResult(command);
		request.setPort(port);
		request.setIP(address.getAddress());
		request.setUserId("default");

		getOutputStream().write(request.getBytes(), 0, request.getLength());
	}

	/**
	 * Read a SOCKS V4 reply.
	 */
	private Socks4Message socksReadReply() throws IOException {
		Socks4Message reply = new Socks4Message();
		int bytesRead = 0;
		while (bytesRead < Socks4Message.REPLY_LENGTH) {
			bytesRead += getInputStream().read(reply.getBytes(), bytesRead,
					Socks4Message.REPLY_LENGTH - bytesRead);
		}
		return reply;
	}

	/**
	 * Connect the socket to the host/port specified by the SocketAddress with a
	 * specified timeout.
	 * 
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
	protected void connect(SocketAddress remoteAddr, int timeout)
			throws IOException {
		InetSocketAddress inetAddr = (InetSocketAddress) remoteAddr;
		connect(inetAddr.getAddress(), inetAddr.getPort(), timeout);
	}

	/**
	 * Answer if the socket supports urgent data.
	 */
	protected boolean supportsUrgentData() {
		return !streaming || SocketImpl.supportsUrgentDataImpl(fd);
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
	protected void sendUrgentData(int value) throws IOException {
		SocketImpl.sendUrgentDataImpl(fd, (byte) value);
	}
}
