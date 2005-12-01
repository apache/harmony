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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.oti.util.Inet6Util;
import com.ibm.oti.util.Msg;
import com.ibm.oti.util.PriviAction;

/**
 * The Internet Protocol (IP) address class. This class encapsulates an IP
 * address and provides name and reverse name resolution functions. The address
 * is stored in network order, but as a signed (rather than unsigned) integer.
 */
public class InetAddress extends Object implements Serializable {

	final static byte[] any_bytes = { 0, 0, 0, 0 };

	final static byte[] localhost_bytes = { 127, 0, 0, 1 };

	static InetAddress ANY = new Inet4Address(any_bytes);

	final static InetAddress LOOPBACK = new Inet4Address(localhost_bytes,
			"localhost");

	static final long serialVersionUID = 3286316764910316507L;

	String hostName;

	int family = 2;

	byte[] ipaddress;

	// Fill in the JNI id caches
	private static native void oneTimeInitialization(boolean supportsIPv6);

	static {
		oneTimeInitialization(true);
	}

	/**
	 * Constructs an InetAddress.
	 */
	InetAddress() {
		super();
	}

	/**
	 * Constructs an InetAddress, representing the <code>address</code> and
	 * <code>hostName</code>.
	 * 
	 * @param address
	 *            network address
	 */
	InetAddress(byte[] address) {
		super();
		this.ipaddress = address;
	}

	/**
	 * Constructs an InetAddress, representing the <code>address</code> and
	 * <code>hostName</code>.
	 * 
	 * @param address
	 *            network address
	 */
	private InetAddress(byte[] address, String hostName) {
		super();
		this.ipaddress = address;
		this.hostName = hostName;
	}

	/**
	 * Returns the IP address of the argument <code>addr</code> as an array.
	 * The elements are in network order (the highest order address byte is in
	 * the zeroeth element).
	 * 
	 * @return byte[] the network address as a byte array
	 */
	static byte[] addressOf(int addr) {
		int temp = addr;
		byte array[] = new byte[4];
		array[3] = (byte) (temp & 0xFF);
		array[2] = (byte) ((temp >>>= 8) & 0xFF);
		array[1] = (byte) ((temp >>>= 8) & 0xFF);
		array[0] = (byte) ((temp >>>= 8) & 0xFF);
		return array;
	}

	CacheElement cacheElement() {
		return new CacheElement();
	}

	/**
	 * Compares this <code>InetAddress</code> against the specified object.
	 * 
	 * @param obj
	 *            the object to be tested for equality
	 * @return boolean true, if the objects are equal
	 */
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != this.getClass()) {
			return false;
		}

		// now check if their byte arrays match...
		byte[] objIPaddress = ((InetAddress) obj).ipaddress;
		for (int i = 0; i < objIPaddress.length; i++) {
			if (objIPaddress[i] != this.ipaddress[i])
				return false;
		}
		return true;
	}

	/**
	 * Returns the IP address of this <code>InetAddress</code> as an array.
	 * The elements are in network order (the highest order address byte is in
	 * the zeroeth element).
	 * 
	 * @return byte[] the address as a byte array
	 */
	public byte[] getAddress() {
		return (byte[]) ipaddress.clone();
	}

	/**
	 * Answer the IP addresses of a named host. The host name may either be a
	 * machine name or a dotted string IP address. If the host name is empty or
	 * null, an UnknownHostException is thrown. If the host name is a dotted IP
	 * string, an array with the corresponding single InetAddress is returned.
	 * 
	 * @param host
	 *            the hostName to be resolved to an address
	 * 
	 * @return InetAddress[] an array of addresses for the host
	 * @throws UnknownHostException
	 *             if the address lookup fails
	 */
	public static InetAddress[] getAllByName(String host)
			throws UnknownHostException {
		if (host == null || 0 == host.length())
			throw new UnknownHostException(Msg.getString("K0038"));

		if (isHostName(host)) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkConnect(host, -1);
			if (Socket.preferIPv4Stack()) {
				return getAliasesByNameImpl(host);
			}
			
			// ok we may have to re-order to make sure the
			// preferIPv6Addresses is respected
			InetAddress[] returnedAddresses = getAliasesByNameImpl(host);
			InetAddress[] orderedAddresses = null;
			if (returnedAddresses != null) {
				orderedAddresses = new InetAddress[returnedAddresses.length];
				int curPosition = 0;
				if (InetAddress.preferIPv6Addresses()) {
					for (int i = 0; i < returnedAddresses.length; i++) {
						if (returnedAddresses[i] instanceof Inet6Address) {
							orderedAddresses[curPosition] = returnedAddresses[i];
							curPosition++;
						}
					}
					for (int i = 0; i < returnedAddresses.length; i++) {
						if (returnedAddresses[i] instanceof Inet4Address) {
							orderedAddresses[curPosition] = returnedAddresses[i];
							curPosition++;
						}
					}
				} else {
					for (int i = 0; i < returnedAddresses.length; i++) {
						if (returnedAddresses[i] instanceof Inet4Address) {
							orderedAddresses[curPosition] = returnedAddresses[i];
							curPosition++;
						}
					}
					for (int i = 0; i < returnedAddresses.length; i++) {
						if (returnedAddresses[i] instanceof Inet6Address) {
							orderedAddresses[curPosition] = returnedAddresses[i];
							curPosition++;
						}
					}
				}
			}
			return orderedAddresses;
		}
		
		byte[] hBytes = Inet6Util.createByteArrayFromIPAddressString(host);
		return (new InetAddress[] { new InetAddress(hBytes) });
	}

	/**
	 * Answers the address of a host, given a host string name. The host string
	 * may be either a machine name or a dotted string IP address. If the
	 * latter, the hostName field will be determined upon demand.
	 * 
	 * @param host
	 *            the hostName to be resolved to an address
	 * @return InetAddress the InetAddress representing the host
	 * 
	 * @throws UnknownHostException
	 *             if the address lookup fails
	 */
	public static InetAddress getByName(String host)
			throws UnknownHostException {
		if (host == null || 0 == host.length())
			return InetAddress.LOOPBACK;
		if (host.equals("0")) {
			return InetAddress.ANY;
		}

		if (isHostName(host)) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkConnect(host, -1);
			return lookupHostByName(host);
		}
		
		return createHostNameFromIPAddress(host);
	}

	/**
	 * Answer the dotted string IP address representing this address.
	 * 
	 * @return String the corresponding dotted string IP address
	 */
	public String getHostAddress() {

		return inetNtoaImpl(bytesToInt(ipaddress, 0));
	}

	/**
	 * Answer the host name.
	 * 
	 * @return String the corresponding string name
	 */
	public String getHostName() {
		int address = 0;
		try {
			if (hostName == null) {
				address = bytesToInt(ipaddress, 0);
				hostName = (0 == address) ? inetNtoaImpl(address)
						: getHostByAddrImpl(ipaddress).hostName;
			}
		} catch (UnknownHostException e) {
			return hostName = inetNtoaImpl(address);
		}
		SecurityManager security = System.getSecurityManager();
		try {
			// Only check host names, not addresses
			if (security != null && isHostName(hostName))
				security.checkConnect(hostName, -1);
		} catch (SecurityException e) {
			address = bytesToInt(ipaddress, 0);
			return inetNtoaImpl(address);
		}
		return hostName;
	}

	/**
	 * Answers canonical name for the host associated with the inet address
	 * 
	 * @return String string containing the host name
	 */
	public String getCanonicalHostName() {
		int address = 0;
		try {

			address = bytesToInt(ipaddress, 0);
			hostName = (0 == address) ? inetNtoaImpl(address)
					: getHostByAddrImpl(ipaddress).hostName;
		} catch (UnknownHostException e) {
			return hostName = inetNtoaImpl(address);
		}
		SecurityManager security = System.getSecurityManager();
		try {
			// Only check host names, not addresses
			if (security != null && isHostName(hostName))
				security.checkConnect(hostName, -1);
		} catch (SecurityException e) {
			address = bytesToInt(ipaddress, 0);
			return inetNtoaImpl(address);
		}
		return hostName;
	}

	/**
	 * Answer the local host, if allowed by the security policy. Otherwise,
	 * answer the loopback address which allows this machine to be contacted.
	 * 
	 * @return InetAddress the InetAddress representing the local host
	 * @throws UnknownHostException
	 *             if the address lookup fails
	 */
	public static InetAddress getLocalHost() throws UnknownHostException {
		String host = getHostNameImpl();
		SecurityManager security = System.getSecurityManager();
		try {
			if (security != null)
				security.checkConnect(host, -1);
		} catch (SecurityException e) {
			return InetAddress.LOOPBACK;
		}
		return lookupHostByName(host);
	}

	/**
	 * Answer a hashcode for this IP address.
	 * 
	 * @return int the hashcode
	 */
	public int hashCode() {
		return bytesToInt(ipaddress, 0);
	}

	/**
	 * Answer true if the InetAddress is an IP multicast address.
	 * 
	 * @return boolean true, if the address is in the multicast group
	 */
	public boolean isMulticastAddress() {
		return ((ipaddress[0] & 255) >>> 4) == 0xE;
	}

	static synchronized InetAddress lookupHostByName(String host)
			throws UnknownHostException {
		int ttl = -1;

		String ttlValue = (String) AccessController
				.doPrivileged(new PriviAction("networkaddress.cache.ttl"));
		try {
			if (ttlValue != null)
				ttl = Integer.decode(ttlValue).intValue();
		} catch (NumberFormatException e) {
		}
		CacheElement element = null;
		if (ttl == 0)
			Cache.clear();
		else {
			element = Cache.get(host);
			if (element != null
					&& ttl > 0
					&& element.timeAdded + (ttl * 1000) < System
							.currentTimeMillis())
				element = null;
		}
		if (element != null)
			return element.inetAddress();
		
		// now try the negative cache
		String failedMessage = NegativeCache.getFailedMessage(host);
		if (failedMessage != null) {
			throw new UnknownHostException(host + " - " + failedMessage);
		}

		InetAddress anInetAddress;
		try {
			anInetAddress = getHostByNameImpl(host, preferIPv6Addresses());
		} catch (UnknownHostException e) {
			// put the entry in the negative cache
			NegativeCache.put(host, e.getMessage());
			throw new UnknownHostException(host + " - " + e.getMessage());
		}

		Cache.add(anInetAddress);
		return anInetAddress;
	}

	/**
	 * Query the IP stack for aliases for the host. The host is in string name
	 * form.
	 * 
	 * @param name
	 *            the host name to lookup
	 * @throws UnknownHostException
	 *             if an error occurs during lookup
	 */
	static native InetAddress[] getAliasesByNameImpl(String name)
			throws UnknownHostException;

	/**
	 * Query the IP stack for the host address. The host is in address form.
	 * 
	 * @param addr
	 *            the host address to lookup
	 * @throws UnknownHostException
	 *             if an error occurs during lookup
	 */
	static native InetAddress getHostByAddrImpl(byte[] addr)
			throws UnknownHostException;

	static int inetAddr(String host) throws UnknownHostException {
		return (host.equals("255.255.255.255")) ? 0xFFFFFFFF
				: inetAddrImpl(host);
	}

	/**
	 * Convert a string containing an Ipv4 Internet Protocol dotted address into
	 * a binary address. Note, the special case of '255.255.255.255' throws an
	 * exception, so this value should not be used as an argument. See also
	 * inetAddr(String).
	 */
	static native int inetAddrImpl(String host) throws UnknownHostException;

	/**
	 * Convert a binary address into a string containing an Ipv4 Internet
	 * Protocol dotted address.
	 */
	static native String inetNtoaImpl(int hipAddr);

	/**
	 * Query the IP stack for the host address. The host is in string name form.
	 * 
	 * @param name
	 *            the host name to lookup
	 * @param preferIPv6Addresses
	 *            address preference if underlying platform is V4/V6
	 * @return InetAddress the host address
	 * 
	 * @throws UnknownHostException
	 *             if an error occurs during lookup
	 */
	static native InetAddress getHostByNameImpl(String name,
			boolean preferIPv6Address) throws UnknownHostException;

	/**
	 * Query the IP stack for the host machine name.
	 * 
	 * @return String the host machine name
	 */
	static native String getHostNameImpl();

	static String getHostNameInternal(String host) throws UnknownHostException {
		if (host == null || 0 == host.length())
			return InetAddress.LOOPBACK.getHostAddress();
		if (isHostName(host))
			return lookupHostByName(host).getHostAddress();
		return host;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * address.
	 * 
	 * @return String the description, as host/address
	 */
	public String toString() {
		return getHostName() + "/" + getHostAddress();
	}

	class CacheElement {
		long timeAdded = System.currentTimeMillis();

		CacheElement next;

		public CacheElement() {
			super();
		}

		String hostName() {
			return hostName;
		}

		InetAddress inetAddress() {
			return InetAddress.this;
		}
	}

	static class Cache {
		static int maxSize = 5;

		private static int size = 0;

		private static CacheElement head;

		static void clear() {
			size = 0;
			head = null;
		}

		static void add(InetAddress value) {
			CacheElement newElement = value.cacheElement();
			if (size < maxSize)
				size++;
			else
				deleteTail();
			newElement.next = head; // If the head is null, this does no harm.
			head = newElement;
		}

		static CacheElement get(String name) {
			CacheElement previous = null;
			CacheElement current = head;
			boolean notFound = true;
			while ((null != current)
					&& (notFound = !(name.equals(current.hostName())))) {
				previous = current;
				current = current.next;
			}
			if (notFound)
				return null;
			moveToHead(current, previous);
			return current;
		}

		private static void deleteTail() {
			if (0 == size)
				return;
			if (1 == size)
				head = null;

			CacheElement previous = null;
			CacheElement current = head;
			while (null != current.next) {
				previous = current;
				current = current.next;
			}
			previous.next = null;
		}

		private static void moveToHead(CacheElement element,
				CacheElement elementPredecessor) {
			if (null == elementPredecessor) {
				head = element;
			} else {
				elementPredecessor.next = element.next;
				element.next = head;
				head = element;
			}
		}
	}

	/**
	 * Answer true if the string is a host name, false if it is an IP Address.
	 */

	private static boolean isHostName(String value) {
		return !(Inet6Util.isValidIPV4Address(value) || Inet6Util
				.isValidIP6Address(value));
	}

	/**
	 * Answer true if the address is a loop back address. Valid IPv4 loopback
	 * addresses are 127.d.d.d Valid IPv6 loopback address is ::1
	 * 
	 * @return boolean
	 */
	public boolean isLoopbackAddress() {
		return false;
	}

	/**
	 * Answers true if the address is a link local address.
	 * 
	 * Valid IPv6 link local addresses are FE80::0 through to
	 * FEBF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF
	 * 
	 * There are no valid IPv4 link local addresses.
	 * 
	 * @return boolean
	 */
	public boolean isLinkLocalAddress() {
		return false;
	}

	/**
	 * Answers true if the address is a site local address.
	 * 
	 * Valid IPv6 link local addresses are FEC0::0 through to
	 * FEFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF
	 * 
	 * There are no valid IPv4 site local addresses.
	 * 
	 * @return boolean
	 */
	public boolean isSiteLocalAddress() {
		return false;
	}

	/**
	 * Answers true if the address is a global multicast address.
	 * 
	 * Valid IPv6 link global multicast addresses are FFxE:/112 where x is a set
	 * of flags, and the additional 112 bits make up the global multicast
	 * address space
	 * 
	 * Valid IPv4 global multi-cast addresses are between: 224.0.1.0 to
	 * 238.255.255.255
	 * 
	 * @return boolean
	 */
	public boolean isMCGlobal() {
		return false;
	}

	/**
	 * Answers true if the address is a node local multicast address.
	 * 
	 * Valid IPv6 node local multicast addresses are FFx1:/112 where x is a set
	 * of flags, and the additional 112 bits make up the node local multicast
	 * address space
	 * 
	 * There are no valid IPv4 node local multicast addresses.
	 * 
	 * @return boolean
	 */
	public boolean isMCNodeLocal() {
		return false;
	}

	/**
	 * Answers true if the address is a link local multicast address.
	 * 
	 * Valid IPv6 link local multicast addresses are FFx2:/112 where x is a set
	 * of flags, and the additional 112 bits make up the node local multicast
	 * address space
	 * 
	 * Valid IPv4 link-local addresses are between: 224.0.0.0 to 224.0.0.255
	 * 
	 * @return boolean
	 */
	public boolean isMCLinkLocal() {
		return false;
	}

	/**
	 * Answers true if the address is a site local multicast address.
	 * 
	 * Valid IPv6 site local multicast addresses are FFx5:/112 where x is a set
	 * of flags, and the additional 112 bits make up the node local multicast
	 * address space
	 * 
	 * Valid IPv4 site-local addresses are between: 239.252.0.0 to
	 * 239.255.255.255
	 * 
	 * @return boolean
	 */
	public boolean isMCSiteLocal() {
		return false;
	}

	/**
	 * Answers true if the address is a organization local multicast address.
	 * 
	 * Valid IPv6 organization local multicast addresses are FFx8:/112 where x
	 * is a set of flags, and the additional 112 bits make up the node local
	 * multicast address space
	 * 
	 * Valid IPv4 organization-local addresses are between: 239.192.0.0 to
	 * 239.251.255.255
	 * 
	 * @return boolean
	 */
	public boolean isMCOrgLocal() {
		return false;
	}

	/**
	 * Method isAnyLocalAddress.
	 * 
	 * @return boolean
	 */
	public boolean isAnyLocalAddress() {
		return false;
	}

	/**
	 * Answers the InetAddress corresponding to the array of bytes. In the case
	 * of an IPv4 address there must be exactly 4 bytes and for IPv6 exactly 16
	 * bytes. If not, an UnknownHostException is thrown.
	 * 
	 * The IP address is not validated by a name service.
	 * 
	 * The high order byte is <code>ipAddress[0]<\code>.
	 *
	 * @param 		ipAddress	either a 4 (IPv4) or 16 (IPv6) byte array
	 * @return 		the InetAddress
	 *
	 * @throws		UnknownHostException
	 */
	public static InetAddress getByAddress(byte[] ipAddress)
			throws UnknownHostException {
		// simply call the method by the same name specifying the default scope
		// id of 0
		return getByAddress(ipAddress, 0);
	}

	/**
	 * Answers the InetAddress corresponding to the array of bytes. In the case
	 * of an IPv4 address there must be exactly 4 bytes and for IPv6 exactly 16
	 * bytes. If not, an UnknownHostException is thrown.
	 * 
	 * The IP address is not validated by a name service.
	 * 
	 * The high order byte is <code>ipAddress[0]<\code>.
	 *
	 * @param 		ipAddress	either a 4 (IPv4) or 16 (IPv6) byte array
	 * @param 		scope_id	the scope id for an IPV6 scoped address. If not a scoped
	 *                          address just pass in 0
	 * @return 		the InetAddress
	 *
	 * @throws		UnknownHostException
	 */
	static InetAddress getByAddress(byte[] ipAddress, int scope_id)
			throws UnknownHostException {
		byte[] copy_address;
		if (ipAddress.length == 4) {
			copy_address = new byte[4];
			for (int i = 0; i < 4; i++) {
				copy_address[i] = ipAddress[i];
			}
			return new Inet4Address(ipAddress);
		}

		if (ipAddress.length == 16) {
			// First check to see if the address is an IPv6-mapped
			// IPv4 address. If it is, then we can make it a IPv4
			// address, otherwise, we'll create an IPv6 address.
			if (isIPv4MappedAddress(ipAddress)) {
				copy_address = new byte[4];
				for (int i = 0; i < 4; i++) {
					copy_address[i] = ipAddress[12 + i];
				}
				return new Inet4Address(copy_address);
			}
			copy_address = (byte[]) ipAddress.clone();
			return new Inet6Address(copy_address, scope_id);
		}
		throw new UnknownHostException(Msg.getString("K0339"));
	}

	private static boolean isIPv4MappedAddress(byte ipAddress[]) {

		// Check if the address matches ::FFFF:d.d.d.d
		// The first 10 bytes are 0. The next to are -1 (FF).
		// The last 4 bytes are varied.
		for (int i = 0; i < 10; i++) {
			if (ipAddress[i] != 0) {
				return false;
			}
		}

		if (ipAddress[10] != -1 || ipAddress[11] != -1) {
			return false;
		}

		return true;

	}

	/**
	 * Answers the InetAddress corresponding to the array of bytes, and the
	 * given hostname. In the case of an IPv4 address there must be exactly 4
	 * bytes and for IPv6 exactly 16 bytes. If not, an UnknownHostException is
	 * thrown.
	 * 
	 * The host name and IP address are not validated.
	 * 
	 * The hostname either be a machine alias or a valid IPv6 or IPv4 address
	 * format.
	 * 
	 * The high order byte is <code>ipAddress[0]<\code>.
	 *
	 * @param 		hostName	string representation of hostname or ip address
	 * @param 		ipAddress	either a 4 (IPv4) or 16 (IPv6) byte array
	 * @return 		the InetAddress
	 *
	 * @throws 		UnknownHostException
	 */
	public static InetAddress getByAddress(String hostName, byte[] ipAddress)
			throws UnknownHostException {
		// just call the method by the same name passing in a default scope id
		// of 0
		return getByAddress(hostName, ipAddress, 0);
	}

	/**
	 * Answers the InetAddress corresponding to the array of bytes, and the
	 * given hostname. In the case of an IPv4 address there must be exactly 4
	 * bytes and for IPv6 exactly 16 bytes. If not, an UnknownHostException is
	 * thrown.
	 * 
	 * The host name and IP address are not validated.
	 * 
	 * The hostname either be a machine alias or a valid IPv6 or IPv4 address
	 * format.
	 * 
	 * The high order byte is <code>ipAddress[0]<\code>.
	 *
	 * @param 		hostName	string representation of hostname or ip address
	 * @param 		ipAddress	either a 4 (IPv4) or 16 (IPv6) byte array
	 * @param 		scope_id	the scope id for a scoped address.  If not a scoped address just pass
	 * 							in 0
	 * @return 		the InetAddress
	 *
	 * @throws 		UnknownHostException
	 */
	static InetAddress getByAddress(String hostName, byte[] ipAddress,
			int scope_id) throws UnknownHostException {
		byte[] copy_address;
		if (ipAddress.length == 4) {
			copy_address = new byte[4];
			for (int i = 0; i < 4; i++) {
				copy_address[i] = ipAddress[i];
			}
			return new Inet4Address(ipAddress, hostName);
		}

		if (ipAddress.length == 16) {
			// First check to see if the address is an IPv6-mapped
			// IPv4 address. If it is, then we can make it a IPv4
			// address, otherwise, we'll create an IPv6 address.
			if (isIPv4MappedAddress(ipAddress)) {
				copy_address = new byte[4];
				for (int i = 0; i < 4; i++) {
					copy_address[i] = ipAddress[12 + i];
				}
				return new Inet4Address(ipAddress, hostName);
			}
			
			copy_address = new byte[16];
			for (int i = 0; i < 16; i++) {
				copy_address[i] = ipAddress[i];
			}

			return new Inet6Address(ipAddress, hostName, scope_id);
		}
		throw new UnknownHostException(Msg.getString("K0332", hostName));
	}

	/**
	 * Takes the integer and chops it into 4 bytes, putting it into the byte
	 * array starting with the high order byte at the index start. This method
	 * makes no checks on the validity of the paramaters.
	 */
	static void intToBytes(int value, byte bytes[], int start) {
		// Shift the int so the current byte is right-most
		// Use a byte mask of 255 to single out the last byte.
		bytes[start] = (byte) ((value >> 24) & 255);
		bytes[start + 1] = (byte) ((value >> 16) & 255);
		bytes[start + 2] = (byte) ((value >> 8) & 255);
		bytes[start + 3] = (byte) (value & 255);
	}

	/**
	 * Takes the byte array and creates an integer out of four bytes starting at
	 * start as the high-order byte. This method makes no checks on the validity
	 * of the parameters.
	 */
	static int bytesToInt(byte bytes[], int start) {
		// First mask the byte with 255, as when a negative
		// signed byte converts to an integer, it has bits
		// on in the first 3 bytes, we are only concerned
		// about the right-most 8 bits.
		// Then shift the rightmost byte to align with its
		// position in the integer.
		int value = ((bytes[start + 3] & 255))
				| ((bytes[start + 2] & 255) << 8)
				| ((bytes[start + 1] & 255) << 16)
				| ((bytes[start] & 255) << 24);
		return value;
	}

	/**
	 * Creates an InetAddress based on an ipAddressString. No error handling is
	 * performed here.
	 */
	static InetAddress createHostNameFromIPAddress(String ipAddressString)
			throws UnknownHostException {

		InetAddress address = null;

		if (Inet6Util.isValidIPV4Address(ipAddressString)) {

			StringTokenizer tokenizer = new StringTokenizer(ipAddressString,
					".");
			String token = "";
			int tempInt = 0;
			byte[] byteAddress = new byte[4];
			for (int i = 0; i < 4; i++) {
				token = tokenizer.nextToken();
				tempInt = Integer.parseInt(token);
				byteAddress[i] = (byte) tempInt;
			}

			address = new Inet4Address(byteAddress);

		} else { // otherwise it must be ipv6

			if (ipAddressString.charAt(0) == '[') {
				ipAddressString = ipAddressString.substring(1, ipAddressString
						.length() - 1);
			}

			StringTokenizer tokenizer = new StringTokenizer(ipAddressString,
					":.%", true);
			ArrayList hexStrings = new ArrayList();
			ArrayList decStrings = new ArrayList();
			String scopeString = null;
			String token = "";
			String prevToken = "";
			String prevPrevToken = "";
			int doubleColonIndex = -1; // If a double colon exists, we need to
										// insert 0s.

			// Go through the tokens, including the seperators ':' and '.'
			// When we hit a : or . the previous token will be added to either
			// the hex list or decimal list. In the case where we hit a ::
			// we will save the index of the hexStrings so we can add zeros
			// in to fill out the string
			while (tokenizer.hasMoreTokens()) {
				prevPrevToken = prevToken;
				prevToken = token;
				token = tokenizer.nextToken();

				if (token.equals(":")) {
					if (prevToken.equals(":")) {
						doubleColonIndex = hexStrings.size();
					} else if (!prevToken.equals("")) {
						hexStrings.add(prevToken);
					}
				} else if (token.equals(".")) {
					decStrings.add(prevToken);
				} else if (token.equals("%")) {
					// add the last word before the % properly
					if (!prevToken.equals(":") && !prevToken.equals(".")) {
						if (prevPrevToken.equals(":")) {
							hexStrings.add(prevToken);
						} else if (prevPrevToken.equals(".")) {
							decStrings.add(prevToken);
						}
					}

					// the rest should be the scope string
					scopeString = tokenizer.nextToken();
					while (tokenizer.hasMoreTokens()) {
						scopeString = scopeString + tokenizer.nextToken();
					}
				}
			}

			if (prevToken.equals(":")) {
				if (token.equals(":")) {
					doubleColonIndex = hexStrings.size();
				} else {
					hexStrings.add(token);
				}
			} else if (prevToken.equals(".")) {
				decStrings.add(token);
			}

			// figure out how many hexStrings we should have
			// also check if it is a IPv4 address
			int hexStringsLength = 8;

			// If we have an IPv4 address tagged on at the end, subtract
			// 4 bytes, or 2 hex words from the total
			if (decStrings.size() > 0) {
				hexStringsLength -= 2;
			}

			// if we hit a double Colon add the appropriate hex strings
			if (doubleColonIndex != -1) {
				int numberToInsert = hexStringsLength - hexStrings.size();
				for (int i = 0; i < numberToInsert; i++) {
					hexStrings.add(doubleColonIndex, "0");
				}
			}

			byte ipByteArray[] = new byte[16];

			// Finally convert these strings to bytes...
			for (int i = 0; i < hexStrings.size(); i++) {
				Inet6Util.convertToBytes((String) hexStrings.get(i),
						ipByteArray, i * 2);
			}

			// Now if there are any decimal values, we know where they go...
			for (int i = 0; i < decStrings.size(); i++) {
				ipByteArray[i + 12] = (byte) (Integer
						.parseInt((String) decStrings.get(i)) & 255);
			}

			// now check to see if this guy is actually and IPv4 address
			// an ipV4 address is ::FFFF:d.d.d.d
			boolean ipV4 = true;
			for (int i = 0; i < 10; i++) {
				if (ipByteArray[i] != 0) {
					ipV4 = false;
					break;
				}
			}

			if (ipByteArray[10] != -1 || ipByteArray[11] != -1) {
				ipV4 = false;
			}

			if (ipV4) {
				byte ipv4ByteArray[] = new byte[4];
				for (int i = 0; i < 4; i++) {
					ipv4ByteArray[i] = ipByteArray[i + 12];
				}
				address = InetAddress.getByAddress(ipv4ByteArray);
			} else {
				int scopeId = 0;
				if (scopeString != null) {
					try {
						scopeId = Integer.parseInt(scopeString);
					} catch (Exception e) {
						// this should not occur as we should not get into this
						// function
						// unless the address is in a valid format
					}
				}
				address = InetAddress.getByAddress(ipByteArray, scopeId);
			}
		}

		return address;
	}

	static boolean preferIPv6Addresses() {
		String result = (String) AccessController.doPrivileged(new PriviAction(
				"java.net.preferIPv6Addresses"));
		if ("true".equals(result))
			return true;
		return false;
	}

	private static final ObjectStreamField[] serialPersistentFields = {
			new ObjectStreamField("address", Integer.TYPE),
			new ObjectStreamField("family", Integer.TYPE),
			new ObjectStreamField("hostName", String.class) };

	private void writeObject(ObjectOutputStream stream) throws IOException {
		ObjectOutputStream.PutField fields = stream.putFields();
		if (ipaddress == null) {
			fields.put("address", 0);
		} else {
			fields.put("address", bytesToInt(ipaddress, 0));
		}
		fields.put("family", family);
		fields.put("hostName", hostName);

		stream.writeFields();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField fields = stream.readFields();
		int addr = fields.get("address", 0);
		ipaddress = new byte[4];
		intToBytes(addr, ipaddress, 0);
		hostName = (String) fields.get("hostName", null);
		family = fields.get("family", 2);
	}

	private Object readResolve() throws ObjectStreamException {
		return new Inet4Address(ipaddress);
	}
}
