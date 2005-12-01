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


import java.net.ProtocolException;
import java.net.URL;

/**
 * This abstract subclass of <code>URLConnection</code> defines method for
 * managing HTTP connection according to the description given by RFC 2068
 * 
 * @see ContentHandler
 * @see URL
 * @see URLConnection
 * @see URLStreamHandler
 */
public abstract class HttpURLConnection extends java.net.URLConnection {
	private String methodTokens[] = { "GET", "DELETE", "HEAD", "OPTIONS",
			"POST", "PUT", "TRACE" };

	protected String method = "GET"; // request method, DEFAULT: "GET"

	protected int responseCode = -1; // response code obtained from the request

	protected String responseMessage; // response message corresponds to the
										// response code

	protected boolean instanceFollowRedirects = followRedirects;

	private static boolean followRedirects = true;

	// 2XX: generally "OK"
	// 3XX: relocation/redirect
	// 4XX: client error
	// 5XX: server error
	/**
	 * Numeric status code, 202: Accepted
	 */
	public final static int HTTP_ACCEPTED = 202;

	/**
	 * Numeric status code, 502: Bad Gateway
	 */
	public final static int HTTP_BAD_GATEWAY = 502;

	/**
	 * Numeric status code, 405: Bad Method
	 */
	public final static int HTTP_BAD_METHOD = 405;

	/**
	 * Numeric status code, 400: Bad Request
	 */
	public final static int HTTP_BAD_REQUEST = 400;

	/**
	 * Numeric status code, 408: Client Timeout
	 */
	public final static int HTTP_CLIENT_TIMEOUT = 408;

	/**
	 * Numeric status code, 409: Conflict
	 */
	public final static int HTTP_CONFLICT = 409;

	/**
	 * Numeric status code, 201: Created
	 */
	public final static int HTTP_CREATED = 201;

	/**
	 * Numeric status code, 413: Entity too large
	 */
	public final static int HTTP_ENTITY_TOO_LARGE = 413;

	/**
	 * Numeric status code, 403: Forbidden
	 */
	public final static int HTTP_FORBIDDEN = 403;

	/**
	 * Numeric status code, 504: atewaytimeout
	 */
	public final static int HTTP_GATEWAY_TIMEOUT = 504;

	/**
	 * Numeric status code, 410: Gone
	 */
	public final static int HTTP_GONE = 410;

	/**
	 * Numeric status code, 500: Internal error
	 */
	public final static int HTTP_INTERNAL_ERROR = 500;

	/**
	 * Numeric status code, 411: Length required
	 */
	public final static int HTTP_LENGTH_REQUIRED = 411;

	/**
	 * Numeric status code, 301 Moved permanently
	 */
	public final static int HTTP_MOVED_PERM = 301;

	/**
	 * Numeric status code, 302: Moved temporarily
	 */
	public final static int HTTP_MOVED_TEMP = 302;

	/**
	 * Numeric status code, 300: Multiple choices
	 */
	public final static int HTTP_MULT_CHOICE = 300;

	/**
	 * Numeric status code, 204: No content
	 */
	public final static int HTTP_NO_CONTENT = 204;

	/**
	 * Numeric status code, 406: Not acceptable
	 */
	public final static int HTTP_NOT_ACCEPTABLE = 406;

	/**
	 * Numeric status code, 203: Not authoritative
	 */
	public final static int HTTP_NOT_AUTHORITATIVE = 203;

	/**
	 * Numeric status code, 404: Not found
	 */
	public final static int HTTP_NOT_FOUND = 404;

	/**
	 * Numeric status code, 501: Not implemented
	 */
	public final static int HTTP_NOT_IMPLEMENTED = 501;

	/**
	 * Numeric status code, 304: Not modified
	 */
	public final static int HTTP_NOT_MODIFIED = 304;

	/**
	 * Numeric status code, 200: OK
	 */
	public final static int HTTP_OK = 200;

	/**
	 * Numeric status code, 206: Partial
	 */
	public final static int HTTP_PARTIAL = 206;

	/**
	 * Numeric status code, 402: Payment required
	 */
	public final static int HTTP_PAYMENT_REQUIRED = 402;

	/**
	 * Numeric status code, 412: Precondition failed
	 */
	public final static int HTTP_PRECON_FAILED = 412;

	/**
	 * Numeric status code, 407: Proxy authentication required
	 */
	public final static int HTTP_PROXY_AUTH = 407;

	/**
	 * Numeric status code, 414: Request too long
	 */
	public final static int HTTP_REQ_TOO_LONG = 414;

	/**
	 * Numeric status code, 205: Reset
	 */
	public final static int HTTP_RESET = 205;

	/**
	 * Numeric status code, 303: See other
	 */
	public final static int HTTP_SEE_OTHER = 303;

	/**
	 * @deprecated Use HTTP_INTERNAL_ERROR
	 */
	public final static int HTTP_SERVER_ERROR = 500;

	/**
	 * Numeric status code, 305: Use proxy
	 */
	public final static int HTTP_USE_PROXY = 305;

	/**
	 * Numeric status code, 401: Unauthorized
	 */
	public final static int HTTP_UNAUTHORIZED = 401;

	/**
	 * Numeric status code, 415: Unsupported type
	 */
	public final static int HTTP_UNSUPPORTED_TYPE = 415;

	/**
	 * Numeric status code, 503: Unavailable
	 */
	public final static int HTTP_UNAVAILABLE = 503;

	/**
	 * Numeric status code, 505: Version not supported
	 */
	public final static int HTTP_VERSION = 505;

	/**
	 * Constructs a <code>HttpURLConnection</code> pointing to the resource
	 * specified by the <code>URL</code>.
	 * 
	 * @param url
	 *            the URL of this connection
	 * 
	 * @see URL
	 * @see URLConnection
	 */
	protected HttpURLConnection(URL url) {
		super(url);
	}

	/**
	 * Closes the connection with the HTTP server
	 * 
	 * @see URLConnection#connect()
	 * @see URLConnection#connected
	 */
	public abstract void disconnect();

	/**
	 * Answers a input stream from the server in the case of error such as the
	 * requested file (txt, htm, html) is not found on the remote server.
	 * <p>
	 * If the content type is not what stated above,
	 * <code>FileNotFoundException</code> is thrown.
	 * 
	 * @return the error input stream returned by the server.
	 */
	public java.io.InputStream getErrorStream() {
		return null;
	}

	/**
	 * Answers the value of <code>followRedirects</code> which indicates if
	 * this connection will follows a different URL redirected by the server. It
	 * is enabled by default.
	 * 
	 * @return The value of the flag
	 * 
	 * @see #setFollowRedirects
	 */
	public static boolean getFollowRedirects() {
		return followRedirects;
	}

	/**
	 * Answers the permission object (in this case, SocketPermission) with the
	 * host and the port number as the target name and "resolve, connect" as the
	 * action list.
	 * 
	 * @return the permission object required for this connection
	 * 
	 * @throws java.io.IOException
	 *             if an IO exception occurs during the creation of the
	 *             permission object.
	 */
	public java.security.Permission getPermission() throws java.io.IOException {
		int port = url.getPort();
		if (port < 0)
			port = 80;
		return new java.net.SocketPermission(url.getHost() + ":" + port,
				"connect, resolve");
	}

	/**
	 * Answers the request method which will be used to make the request to the
	 * remote HTTP server. All possible methods of this HTTP impl is listed in
	 * the class definition.
	 * 
	 * @return the request method string
	 * 
	 * @see #method
	 * @see #setRequestMethod
	 */
	public String getRequestMethod() {
		return method;
	}

	/**
	 * Answers the reponse code returned by the remote HTTP server
	 * 
	 * @return the response code, -1 if no valid response code
	 * 
	 * @throws java.io.IOException
	 *             if there is an IO error during the retrieval.
	 * 
	 * @see #getResponseMessage
	 */
	public int getResponseCode() throws java.io.IOException {
		// Response Code Sample : " HTTP/1.0 200 OK "

		// Call getInputStream() first since getHeaderField() doesn't return
		// exceptions
		getInputStream();
		String response = getHeaderField(0);
		if (response == null)
			return -1;
		response.trim();
		int mark = response.indexOf(" ") + 1;
		if (mark == 0)
			return -1;
		int last = mark + 3;
		if (last > response.length())
			last = response.length();
		responseCode = Integer.parseInt(response.substring(mark, last));
		if (last + 1 <= response.length())
			responseMessage = response.substring(last + 1);
		return responseCode;
	}

	/**
	 * Answers the response message returned the remote HTTP server
	 * 
	 * @return the response message. <code>null</code> if such response exists
	 * 
	 * @throws java.io.IOException
	 *             if there is an IO error during the retrieval.
	 * 
	 * @see #getResponseCode()
	 * @see java.io.IOException
	 */
	public String getResponseMessage() throws java.io.IOException {
		if (responseMessage != null)
			return responseMessage;
		getResponseCode();
		return responseMessage;
	}

	/**
	 * Sets the flag of whether this connection will follow redirects returned
	 * by the remote server. This method can only be called with the permission
	 * from the security manager
	 * 
	 * @param auto
	 *            The value to set
	 * 
	 * @see java.lang.SecurityManager#checkSetFactory()
	 */

	public static void setFollowRedirects(boolean auto) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkSetFactory();
		followRedirects = auto;
	}

	/**
	 * Sets the request command which will be sent to the remote HTTP server.
	 * This method can only be called before the connection is made.
	 * 
	 * @param method
	 *            The <code>non-null</code> string representing the method
	 * 
	 * @throws java.net.ProtocolException
	 *             Thrown when this is called after connected, or the method is
	 *             not supported by this HTTP impl.
	 * 
	 * @see #getRequestMethod()
	 * @see #method
	 */
	public void setRequestMethod(String method) throws ProtocolException {
		if (connected)
			throw new ProtocolException(com.ibm.oti.util.Msg.getString("K0037"));
		for (int i = 0; i < methodTokens.length; i++) {
			if (methodTokens[i].equals(method)) {
				// if there is a supported method that matches the desired
				// method, then set the current method and return
				this.method = methodTokens[i];
				return;
			}
		}
		// if none matches, then throw ProtocolException
		throw new ProtocolException();
	}

	/**
	 * Answers if this connection uses proxy.
	 * 
	 * @return true if this connection supports proxy, false otherwise.
	 */
	public abstract boolean usingProxy();

	/**
	 * Answers if this connection follows redirects.
	 * 
	 * @return true if this connection follows redirects, false otherwise.
	 */
	public boolean getInstanceFollowRedirects() {
		return instanceFollowRedirects;
	}

	/**
	 * Sets if this connection follows redirects.
	 * 
	 * @param followRedirects
	 *            true if this connection should follows redirects, false
	 *            otherwise.
	 */
	public void setInstanceFollowRedirects(boolean followRedirects) {
		instanceFollowRedirects = followRedirects;
	}

	/**
	 * Answers the date value in the form of milliseconds since epoch
	 * corresponding to the field <code>field</code>. Answers
	 * <code>defaultValue</code> if no such field can be found in the response
	 * header.
	 * 
	 * @param field
	 *            the field in question
	 * @param defaultValue
	 *            the default value if no field is found
	 * @return milliseconds since epoch
	 */
	public long getHeaderFieldDate(String field, long defaultValue) {
		return super.getHeaderFieldDate(field, defaultValue);
	}
}
