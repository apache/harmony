/* Copyright 2003, 2005 The Apache Software Foundation or its licensors, as applicable
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
import java.io.ObjectStreamField;

public final class Inet6Address extends InetAddress {

	static final long serialVersionUID = 6880410070516793377L;

	final static byte[] any_bytes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0 };

	static InetAddress ANY = new Inet6Address(any_bytes);

	int scope_id = 0;

	boolean scope_id_set = false;

	boolean scope_ifname_set = false;

	String ifname = null;

	Inet6Address(byte address[]) {
		ipaddress = address;
		scope_id = 0;
	}

	Inet6Address(byte address[], String name) {
		hostName = name;
		ipaddress = address;
		scope_id = 0;
	}

	/**
	 * Constructs an InetAddress, representing the <code>address</code> and
	 * <code>hostName</code> and <code>scope_id</code>
	 * 
	 * @param address
	 *            network address
	 * @param name
	 *            Name assocaited with the address
	 * @param scope_id
	 *            The scope id for link or site local addresses
	 */
	Inet6Address(byte address[], String name, int scope_id) {
		hostName = name;
		ipaddress = address;
		this.scope_id = scope_id;
		if (scope_id != 0) {
			scope_id_set = true;
		}
	}

	/**
	 * Constructs an InetAddress, representing the <code>address</code> and
	 * <code>hostName</code> and <code>scope_id</code>
	 * 
	 * @param address
	 *            network address
	 * @param scope_id
	 *            The scope id for link or site local addresses
	 */
	Inet6Address(byte address[], int scope_id) {
		ipaddress = address;
		this.scope_id = scope_id;
		if (scope_id != 0) {
			scope_id_set = true;
		}
	}

	/**
	 * Answer true if the InetAddress is an IP multicast address.
	 * 
	 * Valid IPv6 multicast address have the binary prefixed with 11111111 or FF
	 * (hex).
	 * 
	 * @return boolean true, if the address is in the multicast group, false
	 *         otherwise
	 */
	public boolean isMulticastAddress() {

		// mutlicast addresses are prefixed with 11111111 (255)
		return ipaddress[0] == -1;
	}

	/**
	 * Answer true if the InetAddress is the unspecified adddress "::".
	 * 
	 * @return boolean true, if the address is in the multicast group, false
	 *         otherwise
	 */
	public boolean isAnyLocalAddress() {
		for (int i = 0; i < ipaddress.length; i++) {
			if (ipaddress[i] != 0)
				return false;
		}
		return true;
	}

	/**
	 * Answer true if the InetAddress is the loopback address
	 * 
	 * The valid IPv6 loopback address is ::1
	 * 
	 * @return boolean true if the address is the loopback, false otherwise
	 */
	public boolean isLoopbackAddress() {

		// The last word must be 1
		if (ipaddress[15] != 1) {
			return false;
		}

		// All other words must be 0
		for (int i = 0; i < 15; i++) {
			if (ipaddress[i] != 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Answer true if the InetAddress is a link-local address.
	 * 
	 * A valid IPv6 link-local address is prefixed with 1111111010
	 * 
	 * @return boolean true, if it is a link-local address, false otherwise
	 */
	public boolean isLinkLocalAddress() {

		// the first 10 bits need to be 1111111010 (1018)
		return (ipaddress[0] == -2) && ((ipaddress[1] & 255) >>> 6) == 2;
	}

	/**
	 * Answer true if the InetAddress is a site-local address.
	 * 
	 * A valid IPv6 site-local address is prefixed with 1111111011
	 * 
	 * @return boolean true, if it is a site-local address, false otherwise
	 */
	public boolean isSiteLocalAddress() {

		// the first 10 bits need to be 1111111011 (1019)
		return (ipaddress[0] == -2) && ((ipaddress[1] & 255) >>> 6) == 3;
	}

	/**
	 * Answer true if the InetAddress is a global multicast address.
	 * 
	 * A valid IPv6 global mutlicast address is 11111111xxxx1110
	 * 
	 * @return boolean true, if it is a global mutlicast address, false
	 *         otherwise
	 */
	public boolean isMCGlobal() {

		// the first byte should be 0xFF and the lower 4 bits
		// of the second byte should be 0xE
		return (ipaddress[0] == -1) && (ipaddress[1] & 14) == 14;

	}

	/**
	 * Answer true if the InetAddress is a node-local multicast address.
	 * 
	 * A valid IPv6 node-local mutlicast address is prefixed with
	 * 11111111xxxx0001
	 * 
	 * @return boolean true, if it is a node-local mutlicast address, false
	 *         otherwise
	 */
	public boolean isMCNodeLocal() {

		// the first byte should be 0xFF and the lower 4 bits
		// of the second byte should be 0x1
		return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 1;

	}

	/**
	 * Answer true if the InetAddress is a link-local multicast address.
	 * 
	 * A valid IPv6 link-local mutlicast address is prefixed with
	 * 11111111xxxx0010
	 * 
	 * @return boolean true, if it is a link-local mutlicast address, false
	 *         otherwise
	 */
	public boolean isMCLinkLocal() {

		// the first byte should be 0xFF and the lower 4 bits
		// of the second byte should be 0x2
		return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 2;

	}

	/**
	 * Answer true if the InetAddress is a site-local multicast address.
	 * 
	 * A valid IPv6 site-local mutlicast address is prefixed with
	 * 11111111xxxx0101
	 * 
	 * @return boolean true, if it is a site-local mutlicast address, false
	 *         otherwise
	 */
	public boolean isMCSiteLocal() {

		// the first byte should be 0xFF and the lower 4 bits
		// of the second byte should be 0x5
		return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 5;

	}

	/**
	 * Answer true if the InetAddress is a org-local multicast address.
	 * 
	 * A valid IPv6 org-local mutlicast address is prefixed with
	 * 11111111xxxx1000
	 * 
	 * @return boolean true, if it is a org-local mutlicast address, false
	 *         otherwise
	 */
	public boolean isMCOrgLocal() {

		// the first byte should be 0xFF and the lower 4 bits
		// of the second byte should be 0x8
		return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 8;
	}

	/**
	 * Returns the byte array representation of the IP address.
	 * 
	 * @return byte[]
	 * 
	 */
	public byte[] getAddress() {
		return ipaddress;
	}

	public String getHostAddress() {
		StringBuffer hostAddress = new StringBuffer();
		String tempString;
		int length = ipaddress.length / 2;
		for (int i = 0; i < length; i++) {

			tempString = Integer.toHexString(ipaddress[i * 2] & 255);
			if (tempString.length() == 1) {
				tempString = "0" + tempString;
			}
			hostAddress.append(tempString);
			tempString = Integer.toHexString(ipaddress[i * 2 + 1] & 255);
			if (tempString.length() == 1) {
				tempString = "0" + tempString;
			}
			hostAddress.append(tempString);
			if (i + 1 < length) {
				hostAddress.append(":");
			}
		}

		return hostAddress.toString().toUpperCase();
	}

	public int hashCode() {
		/* Returns the low order int as the hash code */
		return bytesToInt(ipaddress, 12);
	}

	/**
	 * Returns true if obj is of the same type as the IPv6 address and they have
	 * the same IP address, false otherwise. the scope id does not seem to be
	 * part of the comparison
	 * 
	 * @return String
	 * 
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * An IPv4 compatible address is prefixed with 96 bits of 0's. The last
	 * 32-bits are varied corresponding with the 32-bit IPv4 address space.
	 */
	public boolean isIPv4CompatibleAddress() {
		for (int i = 0; i < 12; i++) {
			if (ipaddress[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private static final ObjectStreamField[] serialPersistentFields = {
			new ObjectStreamField("ipaddress", new byte[0].getClass()),
			new ObjectStreamField("scope_id", Integer.TYPE),
			new ObjectStreamField("scope_id_set", Boolean.TYPE) };

	private void writeObject(ObjectOutputStream stream) throws IOException {
		ObjectOutputStream.PutField fields = stream.putFields();
		if (ipaddress == null) {
			fields.put("ipaddress", null);
		} else {
			fields.put("ipaddress", ipaddress);
		}

		fields.put("scope_id", scope_id);
		fields.put("scope_id_set", scope_id_set);

		stream.writeFields();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField fields = stream.readFields();
		ipaddress = (byte[]) fields.get("ipaddress", null);
		scope_id = fields.get("scope_id", 0);
		scope_id_set = fields.get("scope_id_set", false);
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * address.
	 * 
	 * @return String the description, as host/address
	 */
	public String toString() {
		if (scope_id != 0) {
			return super.toString() + "%" + scope_id;
		}
		return super.toString();
	}
}
