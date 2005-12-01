/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
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

public class InetSocketAddress extends SocketAddress {

	static final long serialVersionUID = 5076001401234631237L;

	private String hostName;

	private InetAddress addr;

	private int port;

	public InetSocketAddress(int port) {
		this((InetAddress) null, port);
	}

	public InetSocketAddress(InetAddress address, int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException();
		if (address == null)
			addr = InetAddress.ANY;
		else
			addr = address;
		hostName = addr.getHostName();
		this.port = port;
	}

	public InetSocketAddress(String host, int port) {
		if (host == null || port < 0 || port > 65535)
			throw new IllegalArgumentException();
		hostName = host;
		this.port = port;
		try {
			addr = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
		}
	}

	public final int getPort() {
		return port;
	}

	public final InetAddress getAddress() {
		return addr;
	}

	public final String getHostName() {
		return hostName;
	}

	public final boolean isUnresolved() {
		return addr == null;
	}

	public String toString() {
		String host;
		if (addr != null)
			host = addr.toString();
		else
			host = hostName;
		return host + ":" + port; //$NON-NLS-1$
	}

	public final boolean equals(Object socketAddr) {
		if (this == socketAddr)
			return true;
		if (!(socketAddr instanceof InetSocketAddress))
			return false;
		InetSocketAddress iSockAddr = (InetSocketAddress) socketAddr;

		// check the ports as we always need to do this
		if (port != iSockAddr.port) {
			return false;
		}

		// we only use the hostnames in the comparison if the addrs were not
		// resolved
		if ((addr == null) && (iSockAddr.addr == null)) {
			return hostName.equals(iSockAddr.hostName);
		} else {
			// addrs were resolved so use them for the comparison
			if (addr == null) {
				// if we are here we know iSockAddr is not null so just return
				// false
				return false;
			}
			return addr.equals(iSockAddr.addr);
		}
	}

	public final int hashCode() {
		if (addr == null)
			return hostName.hashCode() + port;
		return addr.hashCode() + port;
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		if (addr == null) {
			try {
				addr = InetAddress.getByName(hostName);
			} catch (UnknownHostException e) {
			}
		}
	}

}
