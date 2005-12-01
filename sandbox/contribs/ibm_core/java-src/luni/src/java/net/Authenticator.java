/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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


/**
 * This class is able to obtain authentication info for an connection, usually
 * from user. First the application has to set the default authenticator which
 * extends <code>Authenticator</code> by
 * <code>setDefault(Authenticator a)</code>.
 * <p>
 * It should overide <code>getPasswordAuthentication()</code> which dictates
 * how the authentication info should be obtained.
 * 
 * @see java.net.Authenticator.setDefault(java.net.ConnectionAuthenticator),
 * @see java.net.getPasswordAuthentication()
 */
public abstract class Authenticator {

	// the default authenticator that needs to be set
	private static Authenticator thisAuthenticator;

	private static final NetPermission requestPasswordAuthenticationPermission = new NetPermission(
			"requestPasswordAuthentication");

	private static final NetPermission setDefaultAuthenticatorPermission = new NetPermission(
			"setDefaultAuthenticator");

	// the requester connection info
	private String host;

	private InetAddress addr;

	private int port;

	private String protocol;

	private String prompt;

	private String scheme;

	/**
	 * This method is responsible for retrieving the username and password for
	 * the sender. The implementation varies. The subclass has to overwrites
	 * this.
	 * <p>
	 * It answers null by default.
	 * 
	 * @return java.net.PasswordAuthentication The password authenticaiton that
	 *         it obtains
	 */
	protected PasswordAuthentication getPasswordAuthentication() {
		return null;
	}

	/**
	 * Answers the port of the connection that requests authorization.
	 * 
	 * @return int the port of the connection
	 */
	protected final int getRequestingPort() {
		return this.port;
	}

	/**
	 * Answers the address of the connection that requests authorization or null
	 * if unknown.
	 * 
	 * @return InetAddress the address of the connection
	 */
	protected final InetAddress getRequestingSite() {
		return this.addr;
	}

	/**
	 * Answers the realm (prompt string) of the connection that requires
	 * authorization.
	 * 
	 * @return java.lang.String the prompt string of the connection
	 */
	protected final String getRequestingPrompt() {
		return this.prompt;
	}

	/**
	 * Answers the protocol of the connection that requests authorization.
	 * 
	 * @return java.lang.String the protocol of connection
	 */
	protected final String getRequestingProtocol() {
		return this.protocol;
	}

	/**
	 * Answers the scheme of the connection that requires authorization. Eg.
	 * Basic
	 * 
	 * @return java.lang.String the scheme of the connection
	 */
	protected final String getRequestingScheme() {
		return this.scheme;
	}

	/**
	 * If the permission check of the security manager does not result in a
	 * security exception, this method invokes the methods of the registered
	 * authenticator to get the authentication info.
	 * 
	 * @return java.net.PasswordAuthentication the authentication info
	 * 
	 * @param rAddr
	 *            java.net.InetAddress the address of the connection that
	 *            requests authentication
	 * @param rPort
	 *            int the port of the siconnectionte that requests
	 *            authentication
	 * @param rProtocol
	 *            java.lang.String the protocol of the connection that requests
	 *            authentication
	 * @param rPrompt
	 *            java.lang.String the realm of the connection that requests
	 *            authentication
	 * @param rScheme
	 *            java.lang.String the scheme of the connection that requests
	 *            authentication
	 */
	public static synchronized PasswordAuthentication requestPasswordAuthentication(
			InetAddress rAddr, int rPort, String rProtocol, String rPrompt,
			String rScheme) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(requestPasswordAuthenticationPermission);
		if (thisAuthenticator == null)
			return null;
		// set the requester info so it knows what it is requesting
		// authentication for
		thisAuthenticator.addr = rAddr;
		thisAuthenticator.port = rPort;
		thisAuthenticator.protocol = rProtocol;
		thisAuthenticator.prompt = rPrompt;
		thisAuthenticator.scheme = rScheme;

		// returns the authentication info obtained by the registered
		// Authenticator
		return thisAuthenticator.getPasswordAuthentication();
	}

	/**
	 * This method sets <code>a</code> to be the default authenticator. It
	 * will be called whenever the realm that the URL is pointing to requires
	 * authorization. If there is already an authenticator installed before or
	 * <code>a</code> is null, it will simply do nothing.
	 * 
	 * @param a
	 *            java.net.Authenticator The authenticator to be set.
	 */
	public static void setDefault(Authenticator a) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(setDefaultAuthenticatorPermission);
		if ((thisAuthenticator == null) && (a != null))
			thisAuthenticator = a;
	}

	/**
	 * If the permission check of the security manager does not result in a
	 * security exception, this method invokes the methods of the registered
	 * authenticator to get the authentication info.
	 * 
	 * @return java.net.PasswordAuthentication the authentication info
	 * 
	 * @param rHost
	 *            java.lang.String the host name of the connection that requests
	 *            authentication
	 * @param rAddr
	 *            java.net.InetAddress the address of the connection that
	 *            requests authentication
	 * @param rPort
	 *            int the port of the siconnectionte that requests
	 *            authentication
	 * @param rProtocol
	 *            java.lang.String the protocol of the connection that requests
	 *            authentication
	 * @param rPrompt
	 *            java.lang.String the realm of the connection that requests
	 *            authentication
	 * @param rScheme
	 *            java.lang.String the scheme of the connection that requests
	 *            authentication
	 */
	public static synchronized PasswordAuthentication requestPasswordAuthentication(
			String rHost, InetAddress rAddr, int rPort, String rProtocol,
			String rPrompt, String rScheme) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(requestPasswordAuthenticationPermission);
		if (thisAuthenticator == null)
			return null;
		// set the requester info so it knows what it is requesting
		// authentication for
		thisAuthenticator.host = rHost;
		thisAuthenticator.addr = rAddr;
		thisAuthenticator.port = rPort;
		thisAuthenticator.protocol = rProtocol;
		thisAuthenticator.prompt = rPrompt;
		thisAuthenticator.scheme = rScheme;

		// returns the authentication info obtained by the registered
		// Authenticator
		return thisAuthenticator.getPasswordAuthentication();
	}

	/**
	 * Return the host name of the connection that requests authentication, or
	 * null if unknown.
	 */
	protected final String getRequestingHost() {
		return host;
	}
}
