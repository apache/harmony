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
import java.io.InterruptedIOException;
import java.security.AccessController;

import com.ibm.oti.util.Msg;
import com.ibm.oti.util.PriviAction;

/**
 * The default, concrete instance of datagram sockets. This class does not
 * support security checks. Alternative types of DatagramSocketImpl's may be
 * used by setting the <code>impl.prefix</code> system property.
 */

class PlainDatagramSocketImpl extends DatagramSocketImpl {

	private final static int SO_BROADCAST = 32;

	final static int IP_MULTICAST_ADD = 19;

	final static int IP_MULTICAST_DROP = 20;

	final static int IP_MULTICAST_TTL = 17;

	private boolean bindToDevice;

	private byte[] ipaddress = { 0, 0, 0, 0 };

	private int ttl = 1;

	private volatile boolean isNativeConnected = false;

	// for datagram and multicast sockets we have to set
	// REUSEADDR and REUSEPORT when REUSEADDR is set
	// for other types of sockets we need to just set REUSEADDR
	// therefore we have this other option which sets
	// both if supported by the platform.
	// this cannot be in SOCKET_OPTIONS because since it
	// is a public interface it ends up being public even
	// if it is not declared public
	static final int REUSEADDR_AND_REUSEPORT = 10001;

	// used to keep address to which the socket was connected to at the
	// native level
	private InetAddress connectedAddress = null;

	private int connectedPort = -1;

	// used to store the trafficClass value which is simply returned
	// as the value that was set. We also need it to pass it to methods
	// that specify an address packets are going to be sent to
	private int trafficClass = 0;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization(boolean jcl_IPv6_support);

	static {
		oneTimeInitialization(true);
	}

	/**
	 * Bind the datagram socket to the nominated localhost/port. Sockets must be
	 * bound prior to attempting to send or receive data.
	 * 
	 * @param port
	 *            the port on the localhost to bind
	 * @param addr
	 *            the address on the multihomed localhost to bind
	 * 
	 * @exception SocketException
	 *                if an error occured during bind, such as if the port was
	 *                already bound
	 */
	protected void bind(int port, InetAddress addr) throws SocketException {
		String prop = (String) AccessController.doPrivileged(new PriviAction(
				"bindToDevice"));
		boolean useBindToDevice = prop != null
				&& prop.toLowerCase().equals("true");
		bindToDevice = socketBindImpl2(fd, port, useBindToDevice, addr);
		if (0 != port) {
			localPort = port;
		} else {
			localPort = Socket.getSocketLocalPortImpl(fd, InetAddress
					.preferIPv6Addresses());
		}

		try {
			// Ignore failures
			setOption(SO_BROADCAST, Boolean.TRUE);
		} catch (IOException e) {
		}
	}

	/**
	 * Close the socket.
	 */
	protected void close() {

		synchronized (fd) {
			if (fd.valid()) {
				Socket.socketCloseImpl(fd);
				initializeSocket();
			}
		}
	}

	/**
	 * Allocate the socket descriptor in the IP stack.
	 */
	protected void create() throws SocketException {
		createDatagramSocketImpl(fd, Socket.preferIPv4Stack());
	}

	protected void finalize() {
		close();
	}

	/**
	 * Answer the local address from the IP stack. This method should not be
	 * called directly as it does not check the security policy.
	 * 
	 * @return InetAddress the local address to which the socket is bound.
	 * @see DatagramSocket
	 */
	InetAddress getLocalAddress() {
		return Socket.getSocketLocalAddressImpl(fd, InetAddress
				.preferIPv6Addresses());
	}

	/**
	 * Answer the nominated socket option. As the timeouts are not set as
	 * options in the IP stack, the field value is returned.
	 * 
	 * @return Object the nominated socket option value
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
			if (optID == SocketOptions.IP_MULTICAST_IF
					&& (Socket.getSocketFlags() & Socket.MULTICAST_IF) != 0) {

				return new Inet4Address(ipaddress);
			}
			return result;
		}
	}

	protected int getTimeToLive() throws IOException {
		// Call the native first so there will be an exception if the socket if
		// closed.
		int result = (((Byte) getOption(IP_MULTICAST_TTL)).byteValue()) & 0xFF;
		if ((Socket.getSocketFlags() & Socket.MULTICAST_TTL) != 0)
			return ttl;
		return result;
	}

	protected byte getTTL() throws IOException {
		// Call the native first so there will be an exception if the socket if
		// closed.
		byte result = ((Byte) getOption(IP_MULTICAST_TTL)).byteValue();
		if ((Socket.getSocketFlags() & Socket.MULTICAST_TTL) != 0)
			return (byte) ttl;
		return result;
	}

	/**
	 * Add this socket to the multicast group. A socket must joint a group
	 * before data may be received. A socket may be a member of multiple groups
	 * but may join any group once.
	 * 
	 * @param addr
	 *            the multicast group to be joined
	 * @exception java.io.IOException
	 *                may be thrown while joining a group
	 */
	protected void join(InetAddress addr) throws IOException {
		setOption(IP_MULTICAST_ADD, new GenericIPMreq(addr));
	}

	/**
	 * Add this socket to the multicast group. A socket must join a group before
	 * data may be received. A socket may be a member of multiple groups but may
	 * join any group once.
	 * 
	 * @param addr
	 *            the multicast group to be joined
	 * @param netInterface
	 *            the network interface on which the addresses should be dropped
	 * @exception java.io.IOException
	 *                may be thrown while joining a group
	 */
	protected void joinGroup(SocketAddress addr, NetworkInterface netInterface)
			throws IOException {
		if (addr instanceof InetSocketAddress) {
			InetAddress groupAddr = ((InetSocketAddress) addr).getAddress();
			setOption(IP_MULTICAST_ADD, new GenericIPMreq(groupAddr,
					netInterface));
		}
	}

	/**
	 * Remove the socket from the multicast group.
	 * 
	 * @param addr
	 *            the multicast group to be left
	 * @exception java.io.IOException
	 *                May be thrown while leaving the group
	 */
	protected void leave(InetAddress addr) throws IOException {
		setOption(IP_MULTICAST_DROP, new GenericIPMreq(addr));
	}

	/**
	 * Remove the socket from the multicast group.
	 * 
	 * @param addr
	 *            the multicast group to be left
	 * @param netInterface
	 *            the network interface on which the addresses should be dropped
	 * @exception java.io.IOException
	 *                May be thrown while leaving the group
	 */
	protected void leaveGroup(SocketAddress addr, NetworkInterface netInterface)
			throws IOException {
		if (addr instanceof InetSocketAddress) {
			InetAddress groupAddr = ((InetSocketAddress) addr).getAddress();
			setOption(IP_MULTICAST_DROP, new GenericIPMreq(groupAddr,
					netInterface));
		}
	}

	/**
	 * Connect the socket to a port and address
	 * 
	 * @param aFD
	 *            the FileDescriptor to associate with the socket
	 * @param port
	 *            the port to connect to
	 * @param trafficClass
	 *            the traffic Class to be used then the connection is made
	 * @param inetAddress
	 *            address to connect to.
	 * 
	 * @exception SocketException
	 *                if the connect fails
	 */
	protected static native void connectDatagramImpl2(FileDescriptor aFD,
			int port, int trafficClass, InetAddress inetAddress)
			throws SocketException;

	/**
	 * Disconnect the socket to a port and address
	 * 
	 * @param aFD
	 *            the FileDescriptor to associate with the socket
	 * 
	 * @exception SocketException
	 *                if the disconnect fails
	 */
	protected static native void disconnectDatagramImpl(FileDescriptor aFD)
			throws SocketException;

	/**
	 * Allocate a datagram socket in the IP stack. The socket is associated with
	 * the <code>aFD</code>.
	 * 
	 * @param aFD
	 *            the FileDescriptor to associate with the socket
	 * @param preferIPv4Stack
	 *            IP stack preference if underlying platform is V4/V6
	 * @exception SocketException
	 *                upon an allocation error
	 */
	protected static native void createDatagramSocketImpl(FileDescriptor aFD,
			boolean preferIPv4Stack) throws SocketException;

	/**
	 * Bind the socket to the port/localhost in the IP stack.
	 * 
	 * @param aFD
	 *            the socket descriptor
	 * @param port
	 *            the option selector
	 * @param bindToDevice
	 *            bind the socket to the specified interface
	 * @param inetAddress
	 *            address to connect to.
	 * @exception SocketException
	 *                thrown if bind operation fails
	 */
	protected static native boolean socketBindImpl2(FileDescriptor aFD,
			int port, boolean bindToDevice, InetAddress inetAddress)
			throws SocketException;

	/**
	 * Peek on the socket, update <code>sender</code> address and answer the
	 * sender port.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param sender
	 *            an InetAddress, to be updated with the sender's address
	 * @param receiveTimeout
	 *            the maximum length of time the socket should block, reading
	 * @return int the sender port
	 * 
	 * @exception IOException
	 *                upon an read error or timeout
	 */
	protected static native int peekDatagramImpl(FileDescriptor aFD,
			InetAddress sender, int receiveTimeout) throws IOException;

	/**
	 * Recieve data on the socket into the specified buffer. The packet fields
	 * <code>data</code> & <code>length</code> are passed in addition to
	 * <code>packet</code> to eliminate the JNI field access calls.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param packet
	 *            the DatagramPacket to receive into
	 * @param data
	 *            the data buffer of the packet
	 * @param offset
	 *            the offset in the data buffer
	 * @param length
	 *            the length of the data buffer in the packet
	 * @param receiveTimeout
	 *            the maximum length of time the socket should block, reading
	 * @param peek
	 *            indicates to peek at the data
	 * @exception IOException
	 *                upon an read error or timeout
	 */
	protected static native int receiveDatagramImpl2(FileDescriptor aFD,
			DatagramPacket packet, byte[] data, int offset, int length,
			int receiveTimeout, boolean peek) throws IOException;

	/**
	 * Recieve data on the connected socket into the specified buffer. The
	 * packet fields <code>data</code> & <code>length</code> are passed in
	 * addition to <code>packet</code> to eliminate the JNI field access
	 * calls.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param packet
	 *            the DatagramPacket to receive into
	 * @param data
	 *            the data buffer of the packet
	 * @param offset
	 *            the offset in the data buffer
	 * @param length
	 *            the length of the data buffer in the packet
	 * @param receiveTimeout
	 *            the maximum length of time the socket should block, reading
	 * @param peek
	 *            indicates to peek at the data
	 * @exception IOException
	 *                upon an read error or timeout
	 */
	protected static native int recvConnectedDatagramImpl(FileDescriptor aFD,
			DatagramPacket packet, byte[] data, int offset, int length,
			int receiveTimeout, boolean peek) throws IOException;

	/**
	 * Send the <code>data</code> to the nominated target <code>address</code>
	 * and <code>port</code>. These values are derived from the
	 * DatagramPacket to reduce the field calls within JNI.
	 * 
	 * @param fd
	 *            the socket FileDescriptor
	 * @param data
	 *            the data buffer of the packet
	 * @param offset
	 *            the offset in the data buffer
	 * @param length
	 *            the length of the data buffer in the packet
	 * @param port
	 *            the target host port
	 * @param trafficClass
	 *            the traffic class to be used when the datagram is sent
	 * @param inetAddress
	 *            address to connect to.
	 * 
	 * @exception IOException
	 *                upon an read error or timeout
	 */
	protected static native int sendDatagramImpl2(FileDescriptor fd,
			byte[] data, int offset, int length, int port,
			boolean bindToDevice, int trafficClass, InetAddress inetAddress)
			throws IOException;

	/**
	 * Send the <code>data</code> to the address and port to which the was
	 * connnected and <code>port</code>.
	 * 
	 * @param fd
	 *            the socket FileDescriptor
	 * @param data
	 *            the data buffer of the packet
	 * @param offset
	 *            the offset in the data buffer
	 * @param length
	 *            the length of the data buffer in the packet
	 * @param bindToDevice
	 *            not used, current kept in case needed as was the case for
	 *            sendDatagramImpl
	 * @exception IOException
	 *                upon an read error or timeout
	 */
	protected static native int sendConnectedDatagramImpl(FileDescriptor fd,
			byte[] data, int offset, int length, boolean bindToDevice)
			throws IOException;

	protected int peek(InetAddress sender) throws IOException {
		if (isNativeConnected) {
			// in this case we know the port and address from which the data
			// must have be been received as the socket is connected. However,
			// we still need to do the receive in order to know that there was
			// data received. We use a short buffer as we don't actually need
			// the packet, only the knowledge that it is there
			byte[] storageArray = new byte[10];
			DatagramPacket pack = new DatagramPacket(storageArray,
					storageArray.length);
			recvConnectedDatagramImpl(fd, pack, pack.getData(), pack
					.getOffset(), pack.getLength(), receiveTimeout, true); // peek
			sender.ipaddress = connectedAddress.getAddress();
			return connectedPort;
		}
		return peekDatagramImpl(fd, sender, receiveTimeout);
	}

	/**
	 * Answer the data that may be read at this socket. Any data larger than the
	 * packet buffer length will be discarded. The read will block until either
	 * data is is read or, if the timeout is defined, the operation times out.
	 * 
	 * @exception IOException
	 *                if an error or timeout occurs during a read
	 */
	protected void receive(DatagramPacket pack) throws java.io.IOException {
		try {
			if (isNativeConnected) {
				// do not peek
				recvConnectedDatagramImpl(fd, pack, pack.getData(), pack
						.getOffset(), pack.getLength(), receiveTimeout, false);
				updatePacketRecvAddress(pack);
			} else {
				receiveDatagramImpl2(fd, pack, pack.getData(),
						pack.getOffset(), pack.getLength(), receiveTimeout,
						false // do not peek
				);
			}
		} catch (InterruptedIOException e) {
			throw new SocketTimeoutException(e.getMessage());
		}
	}

	/**
	 * Send the data on this socket.
	 * 
	 * @exception IOException
	 *                if an error occurs during the write
	 */
	protected void send(DatagramPacket packet) throws IOException {

		if (isNativeConnected) {
			sendConnectedDatagramImpl(fd, packet.getData(), packet.getOffset(),
					packet.getLength(), bindToDevice);
		} else {
			sendDatagramImpl2(fd, packet.getData(), packet.getOffset(), packet
					.getLength(), packet.getPort(), bindToDevice, trafficClass,
					packet.getAddress());
		}
	}

	/**
	 * Set the nominated socket option. As the timeouts are not set as options
	 * in the IP stack, the value is stored in an instance field.
	 * 
	 * @exception SocketException
	 *                thrown if the option value is unsupported or invalid
	 */
	public void setOption(int optID, Object val) throws SocketException {
		// for datagram sockets on some platforms we have to set both the
		// REUSEADDR AND REUSEPORT so for REUSEADDR set this option option
		// which tells the VM to set the two values as appropriate for the
		// platform
		if (optID == SocketOptions.SO_REUSEADDR) {
			optID = REUSEADDR_AND_REUSEPORT;
		}

		if (optID == SocketOptions.SO_TIMEOUT) {
			receiveTimeout = ((Integer) val).intValue();
		} else {
			int flags = Socket.getSocketFlags();
			try {
				Socket.setSocketOptionImpl(fd, optID | (flags << 16), val);
			} catch (SocketException e) {
				// we don't throw an exception for IP_TOS even if the platform
				// won't let us set the requested value
				if (optID != SocketOptions.IP_TOS) {
					throw e;
				}
			}
			if (optID == SocketOptions.IP_MULTICAST_IF
					&& (flags & Socket.MULTICAST_IF) != 0) {
				InetAddress inet = (InetAddress) val;
				if (InetAddress.bytesToInt(inet.ipaddress, 0) == 0
						|| inet.equals(InetAddress.LOOPBACK)) {
					ipaddress = ((InetAddress) val).getAddress();
				} else {
					InetAddress local = null;
					try {
						local = InetAddress.getLocalHost();
					} catch (UnknownHostException e) {
						throw new SocketException("getLocalHost(): "
								+ e.toString());
					}
					if (inet.equals(local)) {
						ipaddress = ((InetAddress) val).getAddress();
					} else {
						throw new SocketException(val + " != getLocalHost(): "
								+ local);
					}
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

	protected void setTimeToLive(int ttl) throws java.io.IOException {
		setOption(IP_MULTICAST_TTL, new Byte((byte) (ttl & 0xFF)));
		if ((Socket.getSocketFlags() & Socket.MULTICAST_TTL) != 0) {
			this.ttl = ttl;
		}
	}

	protected void setTTL(byte ttl) throws java.io.IOException {
		setOption(IP_MULTICAST_TTL, new Byte(ttl));
		if ((Socket.getSocketFlags() & Socket.MULTICAST_TTL) != 0) {
			this.ttl = ttl;
		}
	}

	/**
	 * Connect the socket to the specified remote address and port.
	 * 
	 * @param inetAddr
	 *            the remote address
	 * @param port
	 *            the remote port
	 * 
	 * @exception SocketException
	 *                possibly thrown, if the datagram socket cannot be
	 *                connected to the specified remote address and port
	 */
	protected void connect(InetAddress inetAddr, int port)
			throws SocketException {

		connectDatagramImpl2(fd, port, trafficClass, inetAddr);

		// if we get here then we are connected at the native level
		try {
			connectedAddress = InetAddress.getByAddress(inetAddr.getAddress());
		} catch (UnknownHostException e) {
			// this is never expected to happen as we should not have gotten
			// here if the address is not resolvable
			throw new SocketException(Msg.getString(
					"K0317", inetAddr.getHostName())); //$NON-NLS-1$
		}
		connectedPort = port;
		isNativeConnected = true;
	}

	/**
	 * Disconnect the socket from the remote address and port.
	 */
	protected void disconnect() {
		try {
			disconnectDatagramImpl(fd);
		} catch (Exception e) {
			// there is currently no way to return an error so just eat any
			// exception
		}
		connectedPort = -1;
		connectedAddress = null;
		isNativeConnected = false;
	}

	/**
	 * Receive data into the supplied datagram packet by peeking. The data is
	 * not removed and will be received by another peekData() or receive() call.
	 * 
	 * This call will block until either data is received or, if a timeout is
	 * set, the timeout expires.
	 * 
	 * @param pack
	 *            the DatagramPacket used to store the data
	 * 
	 * @return the port the packet was received from
	 * 
	 * @exception IOException
	 *                if an error occurs
	 */
	protected int peekData(DatagramPacket pack) throws IOException {
		try {
			if (isNativeConnected) {
				recvConnectedDatagramImpl(fd, pack, pack.getData(), pack
						.getOffset(), pack.getLength(), receiveTimeout, true); // peek
				updatePacketRecvAddress(pack);
			} else {
				receiveDatagramImpl2(fd, pack, pack.getData(),
						pack.getOffset(), pack.getLength(), receiveTimeout,
						true); // peek
			}
		} catch (InterruptedIOException e) {
			throw new SocketTimeoutException(e.getMessage());
		}
		return pack.getPort();
	}

	/**
	 * Set the received address and port in the packet. We do this when the
	 * Datagram socket is connected at the native level and the
	 * recvConnnectedDatagramImpl does not update the packet with address from
	 * which the packet was received
	 * 
	 * @param packet
	 *            the packet to be updated
	 */
	private void updatePacketRecvAddress(DatagramPacket packet) {
		packet.setAddress(connectedAddress);
		packet.port = connectedPort;
	}
}
