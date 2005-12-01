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

/**
 * This class was added so we can create sockets without options that were
 * needed for server sockets. It just overrides create so that we call new
 * natives which only set the options required for plain sockets. In order to
 * preserve behaviour of older versions the create PlainSocketImpl was left as
 * is and this new class was added. For newer versions an instance of this class
 * is used, for earlier versions the original PlainSocketImpl is used.
 */
class PlainSocketImpl2 extends PlainSocketImpl {

	/**
	 * Answer the result of attempting to create a stream socket in the IP
	 * stack. This version does not set certain options which were required for
	 * server sockets and which the initial vesrion ended up setting for both
	 * socket and serverSockets as the same method was used to create a socket
	 * for both. We have added a new method so that we can preserve the behavior
	 * of earlier versions
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @exception SocketException
	 *                if an error occurs while creating the socket
	 */
	static native void createStreamSocketImpl2(FileDescriptor aFD,
			boolean preferIPv4Stack) throws SocketException;

	/**
	 * Connect the underlying socket to the nominated remotehost/port.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param aport
	 *            the remote machine port to connect to
	 * @param trafficClass
	 *            the traffic class to be used when connecting
	 * @param inetAddress
	 *            the address to connect to
	 * @exception SocketException
	 *                if an error occurs while connecting
	 */
	static native void connectStreamSocketImpl2(FileDescriptor aFD, int aport,
			int trafficClass, InetAddress inetAddress) throws IOException;

	/**
	 * Connect the underlying socket to the nominated remotehost/port.
	 * 
	 * @param aFD
	 *            the socket FileDescriptor
	 * @param aport
	 *            the remote machine port to connect to
	 * @param timeout
	 *            timeout after which SocketTimeoutException will be thrown
	 * @param trafficClass
	 *            the traffic class to be used when connecting
	 * @param inetAddress
	 *            the address to connect to
	 * @exception SocketException
	 *                if an error occurs while connecting
	 * @exception SocketTimeoutException
	 *                if a timeout occurs while trying to connect
	 */
	static native void connectStreamWithTimeoutSocketImpl2(FileDescriptor aFD,
			int aport, int timeout, int trafficClass, InetAddress inetAddress)
			throws IOException;

	/**
	 * Creates a new unconnected socket. If streaming is true, create a stream
	 * socket, else a datagram socket. The deprecated datagram usage is not
	 * supported and will throw an exception.
	 * 
	 * @param isStreaming
	 *            true, if the socket is type streaming
	 * @exception SocketException
	 *                if an error occurs while creating the socket
	 */
	protected void create(boolean isStreaming) throws SocketException {
		this.streaming = isStreaming;
		if (isStreaming) {
			createStreamSocketImpl2(fd, Socket.preferIPv4Stack());
		} else {
			createDatagramSocketImpl(fd, Socket.preferIPv4Stack());
		}
	}

	/**
	 * Send the <code>data</code> to the nominated target <code>address</code>
	 * and <code>port</code>. These values are derived from the
	 * DatagramPacket to reduce the field calls within JNI.
	 * 
	 * @param fd
	 *            the socket FileDescriptor
	 * @param data
	 *            the data buffer of the packet
	 * @param length
	 *            the length of the data buffer in the packet
	 * @param port
	 *            the target host port
	 * @param inetAddress
	 *            the address to send the datagram on
	 * 
	 * @exception IOException
	 *                upon an read error or timeout
	 */
	static native int sendDatagramImpl2(FileDescriptor fd, byte[] data,
			int offset, int length, int port, InetAddress inetAddress)
			throws IOException;

	/**
	 * Bind the socket to the port/localhost in the IP stack.
	 * 
	 * @param aFD
	 *            the socket descriptor
	 * @param port
	 *            the option selector
	 * @param inetAddress
	 *            the address to be used
	 * 
	 * @throws SocketException
	 *             if bind operation fails
	 */
	static native void socketBindImpl2(FileDescriptor aFD, int port,
			InetAddress inetAddress) throws SocketException;
}
