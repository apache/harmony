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

import java.io.ObjectStreamException;


public final class Inet4Address extends InetAddress {

	static final long serialVersionUID = 3286316764910316507L;

	Inet4Address(byte[] address) {
		ipaddress = address;
	}

	Inet4Address(byte[] address, String name) {
		ipaddress = address;
		hostName = name;
	}

	/**
	 * Answers true if the address is a mutlicast address.
	 * 
	 * @return boolean
	 * 
	 * Valid IPv4 mutlicast addresses are prefixed with 1110 = 0xE
	 */
	public boolean isMulticastAddress() {
		return (ipaddress[0] & 0xF0) == 0xE0;
	}

	/**
	 * Answers if the address is the ANY Address
	 * 
	 * @return boolean
	 */
	public boolean isAnyLocalAddress() {
		for (int i = 0; i < ipaddress.length; i++) {
			if (ipaddress[i] != 0)
				return false;
		}
		return true;
	}

	/**
	 * Answers true if the address is a loopback address.
	 * 
	 * @return boolean
	 * 
	 * Loopback ipv4 addresses are prefixed with: 011111111 = 127
	 */
	public boolean isLoopbackAddress() {
		return (ipaddress[0] & 255) == 127;
	}

	/**
	 * Answers false for all IPv4 addresses.
	 * 
	 * @return boolean
	 * 
	 * There are no valid IPv4 link local addresses.
	 */
	public boolean isLinkLocalAddress() {
		return false;
	}

	/**
	 * Answers false for all IPv4 addresses.
	 * 
	 * @return boolean
	 * 
	 * There are no valid IPv4 site local addresses.
	 */
	public boolean isSiteLocalAddress() {
		return false;
	}

	/**
	 * Answers true if an address is a global multicast address.
	 * 
	 * @return boolean true, if the address is in the global multicast group,
	 *         false otherwise
	 * 
	 * Valid MCGlobal IPv4 addresses are 224.0.1.0 - 238.255.255.255
	 */
	public boolean isMCGlobal() {

		// Check if we have a prefix of 1110
		if (!isMulticastAddress())
			return false;

		int address = InetAddress.bytesToInt(ipaddress, 0);
		// Now check the boundaries of the global space
		// if we have an address that is prefixed by something less
		// than 111000000000000000000001 (fortunately we don't have
		// to worry about sign after shifting 8 bits right) it is
		// not mutlicast. ( < 224.0.1.0)
		if (address >>> 8 < 0xE00001)
			return false;

		// Now check the high boundary which is prefixed by
		// 11101110 = 0xEE. If the value is higher than this than
		// it is not MCGlobal ( > 238.255.255.255 )
		if (address >>> 24 > 0xEE)
			return false;

		return true;

	}

	/**
	 * Answers false for all IPv4 addresses.
	 * 
	 * @return boolean
	 * 
	 * There are no valid IPv4 Node-local addresses
	 */
	public boolean isMCNodeLocal() {
		return false;
	}

	/**
	 * Answers true if the address is a link-local address.
	 * 
	 * @return boolean
	 * 
	 * The valid range for IPv4 link-local addresses is: 224.0.0.0 to
	 * 239.0.0.255 Hence a mask of 111000000000000000000000 = 0xE00000
	 */
	public boolean isMCLinkLocal() {
		return InetAddress.bytesToInt(ipaddress, 0) >>> 8 == 0xE00000;
	}

	/**
	 * Answers true if the address is a site-local address.
	 * 
	 * @return boolean
	 * 
	 * The valid range for IPv4 site-local addresses is: 239.255.0.0 to
	 * 239.255.255.255 Hence a mask of 11101111 11111111 = 0xEFFF.
	 */
	public boolean isMCSiteLocal() {
		return (InetAddress.bytesToInt(ipaddress, 0) >>> 16) == 0xEFFF;
	}

	/**
	 * Answers true if the address is a organization-local address.
	 * 
	 * @return boolean
	 * 
	 * The valid range for IPv4 org-local addresses is: 239.192.0.0 to
	 * 239.195.255.255 Hence masks of 11101111 11000000 to 11101111 11000011 are
	 * valid. 0xEFC0 to 0xEFC3
	 */
	public boolean isMCOrgLocal() {
		int prefix = InetAddress.bytesToInt(ipaddress, 0) >>> 16;
		return prefix >= 0xEFC0 && prefix <= 0xEFC3;
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

	/**
	 * Returns a String representation of the IP address.
	 * 
	 * @return String
	 * 
	 */
	public String getHostAddress() {
		String hostAddress = "";
		for (int i = 0; i < 4; i++) {
			hostAddress += ipaddress[i] & 255;
			if (i != 3)
				hostAddress += ".";
		}
		return hostAddress;
	}

	/**
	 * Overrides the basic hashcode function.
	 * 
	 * @return String
	 * 
	 */
	public int hashCode() {
		return InetAddress.bytesToInt(ipaddress, 0);
	}

	/**
	 * Returns true if obj is of the same type as the IPv4 address and they have
	 * the same IP address, false otherwise.
	 * 
	 * @return String
	 * 
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	private Object writeReplace() throws ObjectStreamException {
		return new InetAddress(ipaddress);
	}

}
