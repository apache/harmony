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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import com.ibm.oti.net.www.MimeTable;
import com.ibm.oti.util.Msg;
import com.ibm.oti.util.PriviAction;
import com.ibm.oti.util.Util;

/**
 * The URLConnection class is responsible for establishing a connection to an
 * URL for a given protocol. The correct URLConnection subclass to call is
 * deteremined by <code<URLStreamHandler.openConnection().
 */
public abstract class URLConnection {
	protected URL url;

	private String contentType;

	private static String defaultRequestProperty = ""; // initial default value

	private static boolean defaultAllowUserInteraction = false;

	private static boolean defaultUseCaches = false;

	ContentHandler defaultHandler = new DefaultContentHandler();

	private long lastModified = -1;

	protected long ifModifiedSince;

	private String requestProperty = defaultRequestProperty;

	protected boolean useCaches = defaultUseCaches;

	protected boolean connected = false;

	protected boolean doOutput = false;

	protected boolean doInput = true;

	protected boolean allowUserInteraction = defaultAllowUserInteraction;

	private static ContentHandlerFactory contentHandlerFactory;

	/**
	 * Cache for storing Content handler
	 */
	static Hashtable contentHandlers = new Hashtable();

	/**
	 * A hashtable that maps the filename extension (key) to a MIME-type
	 * (element)
	 */

	private static FileNameMap fileNameMap;

	/**
	 * Creates a URLConnection pointing to the resource specified by the
	 * <code>url</code>
	 */
	protected URLConnection(URL url) {
		this.url = url;
	}

	/**
	 * Establishes the connection to the resource specified by this
	 * <code>URL</code> with this <code>method</code>, along with other
	 * options that can only be set before this connection is made.
	 * 
	 * @throws IOException
	 *             If an error occurs while connecting
	 * 
	 * @see java.io.IOException
	 * @see URLStreamHandler
	 */
	public abstract void connect() throws IOException;

	/**
	 * Answers the value of <code>allowUserInteraction</code> which indicates
	 * if this connection allows user interaction
	 * 
	 * @return the value of the flag
	 * 
	 * @see #getDefaultRequestProperty
	 * @see #setDefaultRequestProperty
	 * @see #allowUserInteraction
	 */
	public boolean getAllowUserInteraction() {
		return allowUserInteraction;
	}

	/**
	 * Answers the object pointed to by this <code>URL</code>. It first
	 * attempts to get the content type from <code>getContentType()</code>,
	 * which looks for the response header field "Content-Type". If none is
	 * found, it will guess the content type from the filename extension. If
	 * that fails, it will guess by inspecting the stream.
	 * 
	 * @return a non-null object
	 * 
	 * @throws IOException
	 *             if an IO error occured
	 * 
	 * @see ContentHandler
	 * @see ContentHandlerFactory
	 * @see IOException
	 * @see #setContentHandlerFactory
	 * 
	 */
	public Object getContent() throws java.io.IOException {
		if (!connected)
			connect();

		if ((contentType = getContentType()) == null)
			if ((contentType = guessContentTypeFromName(url.getFile())) == null)
				contentType = guessContentTypeFromStream(getInputStream());
		if (contentType != null)
			return getContentHandler(contentType).getContent(this);
		return null;
	}

	/**
	 * Answers the object pointed to by this <code>URL</code>. It first
	 * attempts to get the content type from <code>getContentType()</code>,
	 * which looks for the response header field "Content-Type". If none is
	 * found, it will guess the content type from the filename extension. If
	 * that fails, it will guess by inspecting the stream.
	 * 
	 * @param types
	 *            The list of acceptable content types
	 * @return Object The object of the resource pointed by this URL, or null if
	 *         the content does not match a specified content type.
	 * 
	 * @throws IOException
	 *             If an error occured obtaining the content.
	 */
	public Object getContent(Class[] types) throws IOException {
		if (!connected)
			connect();

		if ((contentType = getContentType()) == null)
			if ((contentType = guessContentTypeFromName(url.getFile())) == null)
				contentType = guessContentTypeFromStream(getInputStream());
		if (contentType != null)
			return getContentHandler(contentType).getContent(this, types);
		return null;
	}

	/**
	 * Answers the Content encoding type of the response body, null if no such
	 * field is found in the header response.
	 * 
	 * @return The content encoding type
	 * 
	 * @see #getContentType
	 */
	public String getContentEncoding() {
		return getHeaderField("Content-Encoding");
	}

	/**
	 * Answers the specific ContentHandler that will handle the type
	 * <code>contentType</code>
	 * 
	 * @param type
	 *            The type that needs to be handled
	 * @return An instance of the Content Handler
	 */
	private ContentHandler getContentHandler(String type) throws IOException {
		// Replace all non-alphanumeric character by '_'
		final String typeString = parseTypeString(type.replace('/', '.'));

		// if there's a cached content handler, use it
		Object cHandler = contentHandlers.get(type);
		if (cHandler != null)
			return (ContentHandler) cHandler;

		if (contentHandlerFactory != null) {
			cHandler = contentHandlerFactory.createContentHandler(type);
			if (!(cHandler instanceof ContentHandler))
				throw new UnknownServiceException();
			contentHandlers.put(type, cHandler);
			return (ContentHandler) cHandler;
		}

		// search through the package list for the right class for the Content
		// Type
		String packageList = (String) AccessController
				.doPrivileged(new PriviAction("java.content.handler.pkgs"));
		if (packageList != null) {
			final StringTokenizer st = new StringTokenizer(packageList, "|");
			while (st.countTokens() > 0) {
				try {
					Class cl = Class.forName(st.nextToken() + "." + typeString,
							true, ClassLoader.getSystemClassLoader());
					cHandler = (ContentHandler) cl.newInstance();
				} catch (Exception e) {
				}
			}
		}

		if (cHandler == null) {
			cHandler = AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					try {
						String className = "com.ibm.oti.www.content."
								+ typeString;
						return Class.forName(className).newInstance();
					} catch (ClassNotFoundException e) {
					} catch (IllegalAccessException e) {
					} catch (InstantiationException e) {
					}
					return null;
				}
			});
		}
		if (cHandler != null) {
			if (!(cHandler instanceof ContentHandler))
				throw new UnknownServiceException();
			contentHandlers.put(type, cHandler); // if we got the handler,
			// cache it for next time
			return (ContentHandler) cHandler;
		}

		return defaultHandler;
	}

	/**
	 * Answers the length of the content or body in the response header in
	 * bytes. Answer -1 if <code> Content-Length </code> cannot be found in the
	 * response header.
	 * 
	 * @return The length of the content
	 * 
	 * @see #getContentType
	 */
	public int getContentLength() {
		return getHeaderFieldInt("Content-Length", -1);
	}

	/**
	 * Answers the type of the content. Answers <code> null </code> if there's
	 * no such field.
	 * 
	 * @return The type of the content
	 * 
	 * @see #guessContentTypeFromName
	 * @see #guessContentTypeFromStream
	 */
	public String getContentType() {
		return getHeaderField("Content-Type");
	}

	/**
	 * Answers the date in milliseconds since epoch when this response header
	 * was created, or 0 if the field <code>Date</code> is not found in the
	 * header.
	 * 
	 * @return Date in millisecond since epoch
	 * 
	 * @see #getExpiration
	 * @see #getLastModified
	 * @see java.util.Date
	 * 
	 */
	public long getDate() {
		return getHeaderFieldDate("Date", 0);
	}

	/**
	 * Answers whether this connection allow user interaction by default.
	 * 
	 * @return the value of <code>defaultAllowUserInteraction</code>
	 * 
	 * @see #getAllowUserInteraction
	 * @see #setDefaultAllowUserInteraction
	 * @see #setAllowUserInteraction
	 * @see #allowUserInteraction
	 */
	public static boolean getDefaultAllowUserInteraction() {
		return defaultAllowUserInteraction;
	}

	/**
	 * Answers the default value for the field specified by <code>field</code>,
	 * null if there's no such field.
	 * 
	 * @param field
	 *            the field to get the request property for
	 * @return the field to be looked up
	 * 
	 * @deprecated Use getRequestProperty().
	 */
	public static String getDefaultRequestProperty(String field) {
		return null;
	}

	/**
	 * Answers whether this connection use caches by default.
	 * 
	 * @return true if this connection use caches by default, false otherwise
	 * 
	 * @see #getUseCaches
	 * @see #setDefaultUseCaches
	 * @see #setUseCaches
	 * @see #useCaches
	 */
	public boolean getDefaultUseCaches() {
		return defaultUseCaches;
	}

	/**
	 * Answers whether this connection supports input.
	 * 
	 * @return true if this connection supports input, false otherwise
	 * 
	 * @see #setDoInput
	 * @see #doInput
	 */
	public boolean getDoInput() {
		return doInput;
	}

	/**
	 * Answers whether this connection supports output.
	 * 
	 * @return true if this connection supports output, false otherwise
	 * 
	 * @see #setDoOutput
	 * @see #doOutput
	 */
	public boolean getDoOutput() {
		return doOutput;
	}

	/**
	 * Answers the date in milliseconds since epoch when this response header
	 * expires or 0 if the field <code>Expires</code> is not found in the
	 * header.
	 * 
	 * @return Date in milliseconds since epoch
	 * 
	 * @see #getHeaderField(int)
	 * @see #getHeaderField(String)
	 * @see #getHeaderFieldDate(String, long)
	 * @see #getHeaderFieldInt(String, int)
	 * @see #getHeaderFieldKey(int)
	 */
	public long getExpiration() {
		return getHeaderFieldDate("Expires", 0);
	}

	/**
	 * Answers the MIME table of this URL connection.
	 * 
	 * @return FileNameMap
	 */
	public static FileNameMap getFileNameMap() {
		// Must use lazy initialization or there is a bootstrap problem
		// trying to load the MimeTable resource from a .jar before
		// JarURLConnection has finished initialization.
		if (fileNameMap == null) {
			fileNameMap = new MimeTable();
		}
		return fileNameMap;
	}

	/**
	 * Answers the value of the field at position <code>pos<code>.
	 * Answers <code>null</code> if there are fewer than <code>pos</code> fields
	 * in the response header.
	 *
	 * @param 		pos 		the position of the field
	 * @return 		The value of the field
	 *
	 * @see 		#getHeaderFieldDate
	 * @see 		#getHeaderFieldInt
	 * @see 		#getHeaderFieldKey
	 */
	public String getHeaderField(int pos) {
		return null;
	}

	/**
	 * Provides an unmodifiable map of the connection header values. The map
	 * keys are the String header field names. Each map value is a List of the
	 * header field values associated with that key name.
	 * 
	 * @return the mapping of header field names to values
	 * 
	 * @since 1.4
	 */
	public Map getHeaderFields() {
		return Collections.EMPTY_MAP;
	}

	/**
	 * Provides an unmodifiable map of the request properties. The map keys are
	 * Strings, the map values are each a List of Strings, with each request
	 * property name mapped to its corresponding property values.
	 * 
	 * @return the mapping of request property names to values
	 * 
	 * @since 1.4
	 */
	public Map getRequestProperties() {
		return Collections.EMPTY_MAP;
	}

	/**
	 * Adds the given request property. Will not overwrite any existing
	 * properties associated with the given field name.
	 * 
	 * @param field
	 *            the request property field name
	 * @param newValue
	 *            the property value
	 * 
	 * @since 1.4
	 */
	public void addRequestProperty(String field, String newValue) {
	}

	/**
	 * Answers the value of the field corresponding to the <code>key</code>
	 * Answers <code>null</code> if there is no such field.
	 * 
	 * @param key
	 *            the name of the header field
	 * @return The value of the header field
	 * 
	 * @see #getHeaderFieldDate
	 * @see #getHeaderFieldInt
	 * @see #getHeaderFieldKey
	 */
	public String getHeaderField(String key) {
		return null;
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
	 * 
	 * @see #ifModifiedSince
	 * @see #setIfModifiedSince
	 */
	public long getHeaderFieldDate(String field, long defaultValue) {
		String date = getHeaderField(field);
		if (date == null)
			return defaultValue;
		return Util.parseDate(date);
	}

	/**
	 * Answers the integer value of the specified field. Answers default value
	 * <code>defaultValue</code> if no such field exists.
	 * 
	 * @param field
	 *            the field to return
	 * @param defaultValue
	 *            to be returned if <code>field></code> does not exist
	 * @return value of the field
	 */
	public int getHeaderFieldInt(String field, int defaultValue) {
		try {
			return Integer.parseInt(getHeaderField(field));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Answers the name of the field at position specified by <code>posn</code>,
	 * null if there are fewer than <code>posn</code> fields.
	 * 
	 * @param posn
	 *            the position to look for; the first field being 0
	 * @return the name of the field
	 * 
	 * @see #getHeaderFieldDate
	 * @see #getHeaderFieldInt
	 * @see #getHeaderField(int)
	 * @see #getHeaderField(String)
	 * @see #getHeaderFieldDate(String, long)
	 * @see #getHeaderFieldInt(String, int)
	 * @see #getHeaderFieldKey(int)
	 */
	public String getHeaderFieldKey(int posn) {
		return null;
	}

	/**
	 * Answers the value of <code>ifModifiedSince</code> of this connection in
	 * milliseconds since epoch
	 * 
	 * @return the time since epoch
	 * 
	 * @see #ifModifiedSince
	 * @see #setIfModifiedSince
	 * @see java.text.SimpleDateFormat
	 */
	public long getIfModifiedSince() {
		return ifModifiedSince;
	}

	/**
	 * Creates an InputStream for reading from this URL Connection. It throws
	 * UnknownServiceException by default. This method should be overridden by
	 * its subclasses
	 * 
	 * @return The InputStream to read from
	 * 
	 * @throws IOException
	 *             If an InputStream could not be created
	 * 
	 * @see #getContent()
	 * @see #getContent(Class[])
	 * @see #getOutputStream
	 * @see java.io.InputStream
	 * @see java.io.IOException
	 * 
	 */
	public InputStream getInputStream() throws IOException {
		throw new UnknownServiceException(Msg.getString("K004d"));
	}

	/**
	 * Answers the value of the field <code>Last-Modified</code> in the
	 * response header, 0 if no such field exists
	 * 
	 * @return the value of the field last modified
	 * 
	 * @see java.util.Date
	 * @see #getDate
	 * @see #getExpiration
	 */
	public long getLastModified() {
		if (lastModified != -1)
			return lastModified;
		return lastModified = getHeaderFieldDate("Last-Modified", 0);
	}

	/**
	 * Creates an OutputStream for writing to this URL Connection. It throws
	 * UnknownServiceException by default. This method should be overridden by
	 * subclasses.
	 * 
	 * @return The OutputStream to write to
	 * 
	 * @throws IOException
	 *             If an OutputStream could not be created
	 * 
	 * @see #getContent()
	 * @see #getContent(Class[])
	 * @see #getInputStream
	 * @see java.io.IOException
	 * 
	 */
	public OutputStream getOutputStream() throws IOException {
		throw new UnknownServiceException(Msg.getString("K005f"));
	}

	/**
	 * Answers the permissions necessary to make the connection. Depending on
	 * the protocol, this can be any of the permission subclasses. The
	 * permission returned may also depend on the state of the connection, E.G
	 * In the case of HTTP, redirection can change the applicable permission if
	 * the host changed.
	 * 
	 * <p>
	 * By default, this methods returns <code>AllPermission</code>.
	 * Subclasses should override this and return the appropriate permission
	 * object.
	 * 
	 * @return the permission object governing the connection
	 * 
	 * @throws IOException
	 *             if an IO exception occurs during the creation of the
	 *             permission object.
	 */
	public java.security.Permission getPermission() throws IOException {
		return new java.security.AllPermission();
	}

	/**
	 * Answers the value corresponding to the field in the request Header, null
	 * if no such field exists.
	 * 
	 * @param field
	 *            the field to get the property for
	 * @return the field to look up
	 * 
	 * @see #getDefaultRequestProperty
	 * @see #setDefaultRequestProperty
	 * @see #setRequestProperty
	 */
	public String getRequestProperty(String field) {
		return null;
	}

	/**
	 * Answers the <code>URL</code> of this connection
	 * 
	 * @return the url of this conneciton
	 * 
	 * @see URL
	 * @see #URLConnection(URL)
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Answers whether this connection uses caches
	 * 
	 * @return the value of the flag
	 */
	public boolean getUseCaches() {
		return useCaches;
	}

	/**
	 * Determines the MIME type of the file specified by the
	 * <code> string </code> url, using the filename extension. Any fragment
	 * identifier is removed before processing.
	 * 
	 * @param url
	 *            the MIME type of the file.
	 * @return the string representation of an URL
	 * 
	 * @see FileNameMap
	 * @see FileNameMap#getContentTypeFor(String)
	 * @see #getContentType
	 * @see #guessContentTypeFromStream
	 * 
	 */
	public static String guessContentTypeFromName(String url) {
		return getFileNameMap().getContentTypeFor(url);
	}

	/**
	 * Examines the bytes of the input stream and returns the MIME type, null if
	 * no content type can be deduced.
	 * 
	 * @param is
	 *            the input stream for the URL
	 * @return the type of the input stream
	 * 
	 * @throws IOException
	 *             If an IO error occurs
	 */
	public static String guessContentTypeFromStream(InputStream is)
			throws IOException {
		if (!is.markSupported())
			return null;
		is.mark(4);
		char[] chars = new char[4];
		for (int i = 0; i < chars.length; i++)
			chars[i] = (char) is.read();
		is.reset();
		if ((chars[0] == 'P') && (chars[1] == 'K'))
			return "application/zip";
		if ((chars[0] == 'G') && (chars[1] == 'I'))
			return "image/gif";
		if (new String(chars).trim().startsWith("<"))
			return "text/html";
		return null;
	}

	/**
	 * Performs any necessary string parsing on the input string such as
	 * converting non-alphanumeric character into underscore.
	 * 
	 * @param typeString
	 *            the parsed string
	 * @return the string to be parsed
	 */
	private String parseTypeString(String typeString) {
		StringBuffer typeStringBuffer = new StringBuffer(typeString);
		for (int i = 0; i < typeStringBuffer.length(); i++) {
			// if non-alphanumeric, replace it with '_'
			char c = typeStringBuffer.charAt(i);
			if (!(Character.isLetter(c) || Character.isDigit(c) || c == '.'))
				typeStringBuffer.setCharAt(i, '_');
		}
		return typeStringBuffer.toString();
	}

	/**
	 * Sets the flag indicating whether this connection allows user interaction
	 * This can only be called prior to connection establishment.
	 * 
	 * @param newValue
	 *            the value of the flag to be set
	 * 
	 * @throws SecurityException
	 *             if this method attempts to change the flag after a connection
	 *             has been established
	 */
	public void setAllowUserInteraction(boolean newValue) {
		if (connected) {
			throw new SecurityException(Msg.getString("K0037"));
		}
		this.allowUserInteraction = newValue;
	}

	/**
	 * Sets the current content handler factory to be
	 * <code>contentFactory</code>. It can only do so with the permission of
	 * the security manager. The ContentFactory can only be specified ince
	 * during the lifetime of an application.
	 * 
	 * @param contentFactory
	 *            the factory
	 * 
	 * @throws Error
	 *             if a ContentFactory has been created before SecurityException
	 *             if the security manager does not allow this action
	 * 
	 * @see ContentHandler
	 * @see ContentHandlerFactory
	 * @see java.lang.SecurityException
	 * @see java.lang.SecurityManager#checkSetFactory()
	 */
	public static synchronized void setContentHandlerFactory(
			ContentHandlerFactory contentFactory) {
		if (contentHandlerFactory != null)
			throw new Error(Msg.getString("K004e"));
		SecurityManager sManager = System.getSecurityManager();
		if (sManager != null)
			sManager.checkSetFactory();
		contentHandlerFactory = contentFactory;
	}

	/**
	 * Set whether user interaction is allowed by default. Existing
	 * URLConnections are unaffected.
	 * 
	 * @param allows
	 *            allow user interaction
	 */
	public static void setDefaultAllowUserInteraction(boolean allows) {
		defaultAllowUserInteraction = allows;
	}

	/**
	 * Sets the <code>field</code> in the default request header with the
	 * value <code>value</code>
	 * 
	 * @param field
	 *            the request header field to be set
	 * @param value
	 *            the new value
	 * 
	 * @deprecated Use getRequestProperty().
	 */
	public static void setDefaultRequestProperty(String field, String value) {
	}

	/**
	 * Set whether caches are used by deafult. Existing URLConnections are
	 * unaffected.
	 * 
	 * @param newValue
	 *            the value of the flag to be set
	 * 
	 * @see #getDefaultUseCaches
	 * @see #getUseCaches
	 * @see #setUseCaches
	 * @see #useCaches
	 */
	public void setDefaultUseCaches(boolean newValue) {
		if (connected) {
			throw new IllegalAccessError(Msg.getString("K0037"));
		}
		defaultUseCaches = newValue;
	}

	/**
	 * Sets whether this URLConnection allows input. It cannot be set after the
	 * connection is made.
	 * 
	 * @param newValue
	 *            boolean
	 * 
	 * @throws IllegalAccessError
	 *             Exception thrown when this method attempts to change the
	 *             value after connected
	 * 
	 * @see #doInput
	 * @see #getDoInput
	 * @see #setDoInput
	 * @see java.lang.IllegalAccessError
	 */
	public void setDoInput(boolean newValue) {
		if (connected) {
			throw new IllegalAccessError(Msg.getString("K0037"));
		}
		this.doInput = newValue;
	}

	/**
	 * Sets whether this URLConnection allows output. It cannot be set after the
	 * connection is made.
	 * 
	 * @param newValue
	 *            boolean
	 * 
	 * @throws IllegalAccessError
	 *             Exception thrown when this method attempts to change the
	 *             value after connected
	 * 
	 * @see #doOutput
	 * @see #getDoOutput
	 * @see #setDoOutput
	 * @see java.lang.IllegalAccessError
	 */
	public void setDoOutput(boolean newValue) {
		if (connected) {
			throw new IllegalAccessError(Msg.getString("K0037"));
		}
		this.doOutput = newValue;
	}

	/**
	 * With permission from the security manager, this method sets the
	 * <code>map</code> to be the MIME Table of this URL connection.
	 * 
	 * @param map
	 *            the MIME table to be set.
	 */
	public static void setFileNameMap(FileNameMap map) {
		SecurityManager manager = System.getSecurityManager();
		if (manager != null)
			manager.checkSetFactory();
		fileNameMap = map;
	}

	/**
	 * Sets the header field <code>ifModifiedSince</code>.
	 * 
	 * @param newValue
	 *            number of milliseconds since epoch
	 */
	public void setIfModifiedSince(long newValue) {
		if (connected) {
			throw new IllegalAccessError(Msg.getString("K0037"));
		}
		this.ifModifiedSince = newValue;
	}

	/**
	 * Sets the value of the request header field <code> field </code> to
	 * <code>newValue</code> Only the current URL Connection is affected. It
	 * can only be called before the connection is made
	 * 
	 * @param field
	 *            the field
	 * @param newValue
	 *            the field's new value
	 * 
	 * @see #getDefaultRequestProperty
	 * @see #setDefaultRequestProperty
	 * @see #getRequestProperty
	 */
	public void setRequestProperty(String field, String newValue) {

	}

	/**
	 * Sets the flag indicating if this connection uses caches. This value
	 * cannot be set after the connection is made.
	 * 
	 * @param newValue
	 *            the value of the flag to be set
	 * 
	 * @throws IllegalAccessError
	 *             Exception thrown when this method attempts to change the
	 *             value after connected
	 * 
	 * @see #getDefaultUseCaches
	 * @see #setDefaultUseCaches
	 * @see #getUseCaches
	 * @see #useCaches
	 * @see java.lang.IllegalAccessError
	 */
	public void setUseCaches(boolean newValue) {
		if (connected) {
			throw new IllegalAccessError(Msg.getString("K0037"));
		}
		this.useCaches = newValue;
	}

	/**
	 * Answers the name of the class of the <code>URLConnection </code>
	 * 
	 * @return The string representation of this <code>URLConnection</code>
	 * 
	 * @see #getURL
	 * @see #URLConnection(URL)
	 */
	public String toString() {
		return getClass().getName() + ":" + url.toString();
	}

	class DefaultContentHandler extends java.net.ContentHandler {

		/**
		 * @param u
		 *            the URL connection
		 * 
		 * @see java.net.ContentHandler#getContent(java.net.URLConnection)
		 */
		public Object getContent(URLConnection u) throws IOException {
			return u.getInputStream();
		}
	}
}
