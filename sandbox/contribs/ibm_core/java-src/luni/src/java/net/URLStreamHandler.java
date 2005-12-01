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

/**
 * The abstract superclass of all classes that implement Protocol Handler.
 */
public abstract class URLStreamHandler {
	/**
	 * Establishes a connection to the resource specified by <code>URL</code>.
	 * Since different protocols may have unique ways of connecting, it must be
	 * overwritten by the subclass.
	 * 
	 * @return java.net.URLConnection
	 * @param u
	 *            java.net.URL
	 * 
	 * @exception IOException
	 *                thrown if an IO error occurs during connection
	 *                establishment
	 */
	protected abstract URLConnection openConnection(URL u) throws IOException;

	/**
	 * Parse the <code>string</code>str into <code>URL</code> using u's
	 * context. URL strings generally have the following format:
	 * <code><center>//www.company.com/java/file1.java#reference </center></code>
	 * The string is parsed in HTTP format. If the protocol has a different URL
	 * format this method must be overridden.
	 * 
	 * @param u
	 *            java.net.URL The URL to receive parsed values.
	 * @param str
	 *            java.lang.String The string URL spec from which u is derived
	 * @param start
	 *            int The index in the string from which to begin parsing
	 * @param end
	 *            int The index to stop parsing
	 * 
	 * @see #toExternalForm
	 * @see URL
	 */
	protected void parseURL(URL u, String str, int start, int end) {
		if (end < start)
			return;
		String parseString = "";
		if (start < end)
			parseString = str.substring(start, end);
		end -= start;
		int fileIdx = 0;

		// Default is to use info from context
		String host = u.getHost();
		int port = u.getPort();
		String ref = u.getRef();
		String file = u.getPath();
		String query = u.getQuery();
		String authority = u.getAuthority();
		String userInfo = u.getUserInfo();

		int refIdx = parseString.indexOf('#', 0);
		if (parseString.startsWith("//")) {
			int hostIdx = 2, portIdx = -1;
			port = -1;
			fileIdx = parseString.indexOf('/', hostIdx);
			if (fileIdx == -1) {
				fileIdx = end;
				// Use default
				file = "";
			}
			int hostEnd = fileIdx;
			if (refIdx != -1 && refIdx < fileIdx)
				hostEnd = refIdx;
			int userIdx = parseString.lastIndexOf('@', hostEnd);
			authority = parseString.substring(hostIdx, hostEnd);
			if (userIdx > -1) {
				userInfo = parseString.substring(hostIdx, userIdx);
				hostIdx = userIdx + 1;
			}

			portIdx = parseString.indexOf(':', userIdx == -1 ? hostIdx
					: userIdx);
			int endOfIPv6Addr = parseString.indexOf(']');
			// if there are square braces, ie. IPv6 address, use last ':'
			if (endOfIPv6Addr != -1) {
				try {
					if (parseString.length() > endOfIPv6Addr + 1) {
						char c = parseString.charAt(endOfIPv6Addr + 1);
						if (c == ':') {
							portIdx = endOfIPv6Addr + 1;
						} else {
							portIdx = -1;
						}
					} else {
						portIdx = -1;
					}
				} catch (Exception e) {
					System.out.println("***********");
					e.printStackTrace();
				}
			}

			if (portIdx == -1 || portIdx > fileIdx)
				host = parseString.substring(hostIdx, hostEnd);
			else {
				host = parseString.substring(hostIdx, portIdx);
				String portString = parseString.substring(portIdx + 1, hostEnd);
				if (portString.length() == 0)
					port = -1;
				else
					port = Integer.parseInt(portString);
			}
		}

		if (refIdx > -1)
			ref = parseString.substring(refIdx + 1, end);
		int fileEnd = (refIdx == -1 ? end : refIdx);

		int queryIdx = parseString.lastIndexOf('?', fileEnd);
		if (queryIdx > -1) {
			query = parseString.substring(queryIdx + 1, fileEnd);
			// Don't inherit file if query is changed
			if (queryIdx == 0 && file != null)
				file = "/";
			fileEnd = queryIdx;
		} else
		// Don't inherit query unless only the ref is changed
		if (refIdx != 0)
			query = null;

		boolean canonicalize = false;
		if (fileIdx > -1) {
			if (fileIdx < end && parseString.charAt(fileIdx) == '/')
				file = parseString.substring(fileIdx, fileEnd);
			else if (fileEnd > fileIdx) {
				if (file == null)
					file = "";
				else if (file.equals(""))
					file = "/";
				else if (file.startsWith("/"))
					canonicalize = true;
				int last = file.lastIndexOf('/') + 1;
				if (last == 0)
					file = parseString.substring(fileIdx, fileEnd);
				else
					file = file.substring(0, last)
							+ parseString.substring(fileIdx, fileEnd);
			}
		}
		if (file == null)
			file = "";

		if (host == null)
			host = "";

		if (canonicalize) {
			// modify file if there's any relative referencing
			int dirIndex;
			while ((dirIndex = file.indexOf("/./")) >= 0)
				file = file.substring(0, dirIndex + 1)
						+ file.substring(dirIndex + 3);
			if (file.endsWith("/."))
				file = file.substring(0, file.length() - 1);
			while ((dirIndex = file.indexOf("/../")) >= 0) {
				if (dirIndex != 0) {
					file = file.substring(0, file
							.lastIndexOf('/', dirIndex - 1))
							+ file.substring(dirIndex + 3);
				} else
					file = file.substring(dirIndex + 3);
			}
			if (file.endsWith("/..") && file.length() > 3)
				file = file.substring(0, file.lastIndexOf('/',
						file.length() - 4) + 1);
		}

		setURL(u, u.getProtocol(), host, port, authority, userInfo, file,
				query, ref);
	}

	/**
	 * Sets the fields of the <code>URL</code> with the supplied arguments
	 * 
	 * @param u
	 *            java.net.URL The non-null URL to be set
	 * @param protocol
	 *            java.lang.String The protocol
	 * @param host
	 *            java.lang.String The host name
	 * @param port
	 *            int The port number
	 * @param file
	 *            java.lang.String The file component
	 * @param ref
	 *            java.lang.String The reference
	 * 
	 * @see java.util.Set
	 * 
	 * @deprecated use setURL(URL, String String, int, String, String, String,
	 *             String, String)
	 */
	protected void setURL(URL u, String protocol, String host, int port,
			String file, String ref) {
		if (this != u.strmHandler)
			throw new SecurityException();
		u.set(protocol, host, port, file, ref);
	}

	/**
	 * Sets the fields of the <code>URL</code> with the supplied arguments
	 * 
	 * @param u
	 *            java.net.URL The non-null URL to be set
	 * @param protocol
	 *            java.lang.String The protocol
	 * @param host
	 *            java.lang.String The host name
	 * @param port
	 *            int The port number
	 * @param authority
	 *            java.lang.String The authority
	 * @param userInfo
	 *            java.lang.String The user info
	 * @param file
	 *            java.lang.String The file component
	 * @param query
	 *            java.lang.String The query
	 * @param ref
	 *            java.lang.String The reference
	 * 
	 * @see java.util.Set
	 */
	protected void setURL(URL u, String protocol, String host, int port,
			String authority, String userInfo, String file, String query,
			String ref) {
		if (this != u.strmHandler)
			throw new SecurityException();
		u.set(protocol, host, port, authority, userInfo, file, query, ref);
	}

	/**
	 * Answers the string equivalent of an URL using HTTP parsinf format.
	 * 
	 * @return java.lang.String the string representation of this URL
	 * @param url
	 *            java.net.URL the url object to be processed
	 * 
	 * @see #parseURL
	 * @see URL#toExternalForm()
	 */
	protected String toExternalForm(URL url) {
		StringBuffer answer = new StringBuffer(url.getProtocol().length()
				+ url.getFile().length() + 16);
		answer.append(url.getProtocol());
		answer.append(':');
		String authority = url.getAuthority();
		if (authority != null && authority.length() > 0) {
			answer.append("//");
			answer.append(url.getAuthority());
		}

		String file = url.getFile();
		String ref = url.getRef();
		// file is never null
		answer.append(file);
		if (ref != null) {
			answer.append('#');
			answer.append(ref);
		}
		return answer.toString();
	}

	/**
	 * Compares the two urls, and answers true if they represent the same URL.
	 * Two URLs are equal if they have the same file, host, port, protocol,
	 * query, and ref components.
	 * 
	 * @param url1
	 *            URL the first URL to compare
	 * @param url2
	 *            URL the second URL to compare
	 * @return <code>true</code> if the URLs are the same <code>false</code>
	 *         if the URLs are different
	 * 
	 * @see #hashCode
	 */
	protected boolean equals(URL url1, URL url2) {
		if (!sameFile(url1, url2))
			return false;
		String s1 = url1.getRef(), s2 = url2.getRef();
		if (s1 != s2 && (s1 == null || !s1.equals(s2)))
			return false;
		s1 = url1.getQuery();
		s2 = url2.getQuery();
		return s1 == s2 || (s1 != null && s1.equals(s2));
	}

	/**
	 * Return the default port.
	 */
	protected int getDefaultPort() {
		return -1;
	}

	/**
	 * Return the InetAddress for the host of the URL, or null.
	 */
	protected InetAddress getHostAddress(URL url) {
		try {
			String host = url.getHost();
			if (host == null || host.length() == 0)
				return null;
			return InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Answers a hash code for the URL object.
	 * 
	 * @return int the hashcode for hashtable indexing
	 */
	protected int hashCode(URL url) {
		return toExternalForm(url).hashCode();
	}

	/**
	 * Compares the two urls, and answers true if they have the same host
	 * components.
	 * 
	 * @return <code>true</code> if the hosts of the URLs are the same
	 *         <code>false</code> if the hosts are different
	 */
	protected boolean hostsEqual(URL url1, URL url2) {
		String host1 = url1.getHost(), host2 = url2.getHost();
		return host1 == host2
				|| (host1 != null && host1.equalsIgnoreCase(host2));
	}

	/**
	 * Answers true if the urls refer to the same file. Compares the protocol,
	 * host, port and file components.
	 * 
	 * @return boolean true if the same resource, false otherwise
	 */
	protected boolean sameFile(URL url1, URL url2) {
		String s1 = url1.getProtocol();
		String s2 = url2.getProtocol();
		if (s1 != s2 && (s1 == null || !s1.equals(s2)))
			return false;

		s1 = url1.getFile();
		s2 = url2.getFile();
		if (s1 != s2 && (s1 == null || !s1.equals(s2)))
			return false;
		if (!hostsEqual(url1, url2))
			return false;
		int p1 = url1.getPort();
		if (p1 == -1)
			p1 = getDefaultPort();
		int p2 = url2.getPort();
		if (p2 == -1)
			p2 = getDefaultPort();
		return p1 == p2;
	}
}
