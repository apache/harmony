/* Copyright 2005, 2005 The Apache Software Foundation or its licensors, as applicable
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


import java.util.Enumeration;

/**
 * This class provides is used to pass the information required in an ip_mreq or
 * ip6_mreq structure to java natives. We don't have accessor methods as it is
 * more straight forward in the natives to simply access the fields directly
 */
final class GenericIPMreq {

	// private members
	private InetAddress multiaddr;

	private InetAddress interfaceAddr;

	private boolean isIPV6Address;

	private int interfaceIdx;

	/**
	 * This constructor is used to create an instance of the object
	 * 
	 * @param addr
	 *            multicast address to join/leave
	 * 
	 */
	GenericIPMreq(InetAddress addr) {
		this.multiaddr = addr;
		this.interfaceAddr = null;
		this.interfaceIdx = 0;
		init();
	}

	/**
	 * This constructor is used to create an instance of the object
	 * 
	 * @param addr
	 *            multicast address to join/leave
	 * @param netInterface
	 *            the NetworkInterface object identifying the interface on which
	 *            to join/leave
	 * 
	 */
	GenericIPMreq(InetAddress addr, NetworkInterface netInterface) {
		this.multiaddr = addr;
		if (null != netInterface) {
			this.interfaceIdx = netInterface.getIndex();

			// here we need to get the first IPV4 address as we only use it if
			// we
			// are settting the interface for an IPV4 multicast socket. For
			// adds/drops on
			// IPV6 addresses we use the index within the networkInterface
			this.interfaceAddr = null;
			Enumeration theAddresses = netInterface.getInetAddresses();
			if ((addr instanceof Inet4Address) && (theAddresses != null)) {
				boolean found = false;
				while ((theAddresses.hasMoreElements()) && (found != true)) {
					InetAddress theAddress = (InetAddress) theAddresses
							.nextElement();
					if (theAddress instanceof Inet4Address) {
						this.interfaceAddr = theAddress;
						found = true;
					}
				}
			}
		} else {
			// network interface is null so we just want to defer the decision
			// to
			// the system
			this.interfaceIdx = 0;
			this.interfaceAddr = null;
		}
		init();
	}

	/**
	 * This method does any required initialization for the constructors
	 */
	private void init() {
		// set the flag indicating if the multicast address is an IPV6 address
		// or not
		isIPV6Address = ((this.multiaddr != null) && (this.multiaddr instanceof Inet6Address));
	}
}
