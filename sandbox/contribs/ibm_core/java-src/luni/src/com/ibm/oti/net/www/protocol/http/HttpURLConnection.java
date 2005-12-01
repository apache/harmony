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

package com.ibm.oti.net.www.protocol.http;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.*;
import java.security.AccessController;
import com.ibm.oti.util.PriviAction;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * This subclass extends <code>HttpURLConnection</code> which in turns extends <code>URLConnection</code>
 * This is the actual class that "does the work", such as connecting, sending request and getting the content from
 * the remote server.
 *
 */

public class HttpURLConnection extends java.net.HttpURLConnection {
	int httpVersion = 1;	// Assume HTTP/1.1
	private final int defaultPort;
	InputStream is;
	private InputStream uis;
	OutputStream socketOut;
	private HttpOutputStream os;
	private boolean sentRequest = false;
	boolean sendChunked = false;
	private String proxyName;
	private int hostPort = -1;
	private int readTimeout = -1;

	// default request header
	private static Header defaultReqHeader = new Header();

	// request header that will be sent to the server
	private Header reqHeader;

	// response header received from the server
	private Header resHeader;

	private class LimitedInputStream extends InputStream {
		int bytesRemaining;
	public LimitedInputStream(int length) {
		bytesRemaining = length;
	}
	public void close() throws IOException {
		bytesRemaining = 0;
		closeSocket();
	}
	public int available() throws IOException {
		int result = is.available();
		if (result > bytesRemaining) return bytesRemaining;
		return result;
	}
	public int read() throws IOException {
		if (bytesRemaining <= 0) return -1;
		int result = is.read();
		bytesRemaining--;
		return result;
	}
	public int read(byte[] buf, int offset, int length) throws IOException {
		if (buf == null) throw new NullPointerException();
		// avoid int overflow
		if (offset < 0 || length < 0 || offset > buf.length || buf.length - offset < length)
			throw new ArrayIndexOutOfBoundsException();
		if (bytesRemaining <= 0) return -1;
		if (length > bytesRemaining) length = bytesRemaining;
		int result = is.read(buf, offset, length);
		if (result > 0) bytesRemaining -= result;
		return result;
	}
	public long skip(int amount) throws IOException {
		if (bytesRemaining <= 0) return -1;
		if (amount > bytesRemaining) amount = bytesRemaining;
		long result = is.skip(amount);
		if (result > 0) bytesRemaining -= result;
		return result;
	}
	}

	private class ChunkedInputStream extends InputStream {
		int bytesRemaining = -1;
		boolean atEnd = false;
	public ChunkedInputStream() throws IOException {
		readChunkSize();
	}
	public void close() throws IOException {
		atEnd = true;
		closeSocket();
	}
	public int available() throws IOException {
		int result = is.available();
		if (result > bytesRemaining) return bytesRemaining;
		return result;
	}
	private void readChunkSize() throws IOException {
		if (atEnd) return;
		if (bytesRemaining == 0) readln(); // read CR/LF
		String size = readln();
		int index = size.indexOf(";");
		if (index >= 0) size = size.substring(0, index);
		bytesRemaining = Integer.parseInt(size.trim(), 16);
		if (bytesRemaining == 0) {
			atEnd = true;
			readHeaders();
		}
	}
	public int read() throws IOException {
		if (bytesRemaining <= 0) readChunkSize();
		if (atEnd) return -1;
		bytesRemaining--;
		return is.read();
	}
	public int read(byte[] buf, int offset, int length) throws IOException {
		if (buf == null) throw new NullPointerException();
		// avoid int overflow
		if (offset < 0 || length < 0 || offset > buf.length || buf.length - offset < length)
			throw new ArrayIndexOutOfBoundsException();
		if (bytesRemaining <= 0) readChunkSize();
		if (atEnd) return -1;
		if (length > bytesRemaining) length = bytesRemaining;
		int result = is.read(buf, offset, length);
		if (result > 0) bytesRemaining -= result;
		return result;
	}
	public long skip(int amount) throws IOException {
		if (atEnd) return -1;
		if (bytesRemaining <= 0) readChunkSize();
		if (amount > bytesRemaining) amount = bytesRemaining;
		long result = is.skip(amount);
		if (result > 0) bytesRemaining -= result;
		return result;
	}
	}

	private class HttpOutputStream extends OutputStream {
		static final int MAX = 1024;
		ByteArrayOutputStream cache = new ByteArrayOutputStream(MAX+7);
		boolean writeToSocket, closed = false;
		int limit;
	public HttpOutputStream() {
		limit = -1;
	}
	public HttpOutputStream(int limit) {
		writeToSocket = true;
		this.limit = limit;
	}
	private void output(String output) throws IOException {
		socketOut.write(output.getBytes("ISO8859_1"));
	}
	private void sendCache(boolean close) throws IOException {
		int size = cache.size();
		if (size > 0 || close) {
			if (limit < 0) {
				if (size > 0) {
					output(Integer.toHexString(size) + "\r\n");
					cache.write('\r'); cache.write('\n');
				}
				if (close) {
					cache.write('0');
					cache.write('\r'); cache.write('\n');
					cache.write('\r'); cache.write('\n');
				}
			}
			socketOut.write(cache.toByteArray());
			cache.reset();
		}
	}
	public synchronized void flush() throws IOException {
		if (closed) throw new IOException(com.ibm.oti.util.Msg.getString("K0059"));
		if (writeToSocket) {
			sendCache(false);
			socketOut.flush();
		}
	}
	public synchronized void close() throws IOException {
		if (closed) return;
		closed = true;
		if (writeToSocket) {
			if (limit > 0) throw new IOException(com.ibm.oti.util.Msg.getString("K00a4"));
			sendCache(closed);
		}
	}
	public synchronized void write(int data) throws IOException {
		if (closed) throw new IOException(com.ibm.oti.util.Msg.getString("K0059"));
		if (limit >= 0) {
			if (limit == 0) throw new IOException(com.ibm.oti.util.Msg.getString("K00b2"));
			limit--;
		}
		cache.write(data);
		if (writeToSocket && cache.size() >= MAX)
			sendCache(false);
	}
	public synchronized void write(byte[] buffer, int offset, int count) throws IOException {
		if (closed) throw new IOException(com.ibm.oti.util.Msg.getString("K0059"));
		if (buffer==null) throw new NullPointerException();
		// avoid int overflow
		if (offset < 0 || count < 0 || offset > buffer.length || buffer.length - offset < count)
			throw new ArrayIndexOutOfBoundsException(com.ibm.oti.util.Msg.getString("K002f"));

		if (limit >= 0) {
			if (count > limit) throw new IOException(com.ibm.oti.util.Msg.getString("K00b2"));
			limit -= count;
		}
		if (!writeToSocket || cache.size() + count < MAX) {
			cache.write(buffer, offset, count);
		} else {
			if (limit < 0)
				output(Integer.toHexString(count + cache.size()) + "\r\n");
			socketOut.write(cache.toByteArray());
			cache.reset();
			socketOut.write(buffer, offset, count);
			if (limit < 0) output("\r\n");
		}
	}
	synchronized int size() {
		return cache.size();
	}
	synchronized byte[] toByteArray() {
		return cache.toByteArray();
	}
	boolean isCached() {
		return !writeToSocket;
	}
	boolean isChunked() {
		return writeToSocket && limit == -1;
	}
	}

/**
 * Creates a instance of the <code>HttpURLConnection</code>
 * using default port 80.
 * @param url URL	The URL this connection is connecting
 */
protected HttpURLConnection(URL url) {
	this(url, 80);
}

/**
 * Creates a instance of the <code>HttpURLConnection</code>
 * @param url URL	The URL this connection is connecting
 * @param port int	The default connection port
 */
protected HttpURLConnection(URL url, int port) {
	super(url);
	defaultPort = port;

	reqHeader = (Header)defaultReqHeader.clone();

}
/**
 * Establishes the connection to the remote HTTP server
 *
 * Any methods that requires a valid connection to the resource will call this method implicitly.
 * After the connection is established, <code>connected</code> is set to true.
 *
 *
 * @see 		#connected
 * @see 		java.io.IOException
 * @see 		URLStreamHandler
 */
public void connect() throws java.io.IOException {
	if (connected) return;
	Socket socket = new Socket(getHostAddress(), getHostPort());
	if (readTimeout >= 0) socket.setSoTimeout(readTimeout);
	connected = true;
	socketOut = socket.getOutputStream();
	is = new java.io.BufferedInputStream(socket.getInputStream());
}

/**
 * Sets the read timeout for the http connection.
 *
 *
 * @param	value	the read timeout value. Must be >= 0
 *
 * @see	java.net.Socket#setSoTimeout
 */
public void setReadTimeout(int value) {
	if (value < 0)
		throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0036"));
	readTimeout = value;
}

/**
 * Closes the connection with the HTTP server
 *
 *
 * @see			URLConnection#connect()
 */
public void disconnect() {
	try {closeSocket();} catch (IOException e) {}
}

void closeSocket() throws IOException {
	if (is != null)
		is.close();
}

void endRequest() throws IOException {
	if (os != null)
		os.close();
	sentRequest = false;
}

/**
 * Answers the default value for the field specified by <code>field</code>,
 * 			null if there's no such a field.
 *
 *
 */
public static String getDefaultRequestProperty(String field) {
	return defaultReqHeader.get(field);
}
/**
 * Answers an input stream from the server in the case of error such as the requested
 * file (txt, htm, html) is not found on the remote server.
 * <p>
 * If the content type is not what stated above, <code>FileNotFoundException</code>
 * is thrown.
 *
 * @return java.io.InputStream		the error input stream returned by the server.
 */
public InputStream getErrorStream() {
	if (connected && method != "HEAD" && responseCode >= HTTP_BAD_REQUEST)
		return uis;
	return null;
}
/**
 * Answers the value of the field at position <code>pos<code>.
 * Answers <code>null</code> if there is fewer than <code>pos</code> fields
 * in the response header.
 *
 *
 * @return java.lang.String		The value of the field
 * @param pos int				the position of the field from the top
 *
 * @see 		#getHeaderField(String)
 * @see 		#getHeaderFieldKey
 */
public String getHeaderField(int pos) {
	try {
		getInputStream();
		return resHeader.get(pos);
	} catch (IOException e) {
		return null;
	}
	}
/**
 * Answers the value of the field corresponding to the <code>key</code>
 * Answers <code>null</code> if there is no such field.
 *
 * If there are multiple fields with that key, the last field value is returned.
 *
 *
 * @return java.lang.String			The value of the header field
 * @param key java.lang.String		the name of the header field
 *
 * @see 		#getHeaderField(int)
 * @see 		#getHeaderFieldKey
 */
public String getHeaderField(String key) {
	try {
		getInputStream();
		return resHeader.get(key);
	} catch (IOException e) {
		return null;
	}
}
/**
 * This method answers the header field at position <code>pos</code> from the response header,
 * null if there is fewer fields than <code>pos</code>.
 *
 *
 * @return java.lang.String
 * @param pos int
 *
 * @see #getHeaderField(String)
 * @see #getHeaderField(int)
 */
public String getHeaderFieldKey(int pos) {
	try {
		getInputStream();
		return resHeader.getKey(pos);
	} catch (IOException e) {
		return null;
	}
}

/**
 * Provides an unmodifiable map of the connection header values.  The map keys are the String
 * header field names.  Each map value is a list of the header field values associated with
 * that key name.
 *
 * @return the mapping of header field names to values
 *
 * @since 1.4
 */
public Map getHeaderFields() {
	return resHeader.getFieldMap();
}

/**
 * Provides an unmodifiable map of the request properties.  The map keys are Strings, the map
 * values are each a List of Strings, with each request property name mapped to its corresponding property values.
 *
 * @return the mapping of request property names to values
 *
 * @since 1.4
 */
public Map getRequestProperties() {
	return reqHeader.getFieldMap();
}

/**
 * Creates an input stream for reading from this URL Connection.
 *
 *
 * @return 		InputStream		The input stream to read from
 * @exception 	UnknownServiceException 	Exception thrown when reading to URL isn't supported
 *
 * @see 		#getContent()
 * @see 		#getOutputStream()
 * @see 		java.io.InputStream
 * @see 		java.io.IOException
 */
public InputStream getInputStream() throws IOException {
	if (!doInput)
		throw new ProtocolException(com.ibm.oti.util.Msg.getString("K008d"));

	doRequest();

	// if the requested file does not exist, throw an exception
	//formerly the Error page from the server was returned
	//if the requested file was text/html
	//this has changed to return FileNotFoundException for all file types
	if (responseCode >= HTTP_BAD_REQUEST)
		throw new java.io.FileNotFoundException(url.toString());

	return uis;
}

private InputStream getContentStream() throws IOException {
	if (uis != null) return uis;

	String encoding = resHeader.get("Transfer-Encoding");
	if (encoding != null && encoding.toLowerCase().equals("chunked"))
		return uis = new ChunkedInputStream();

	String sLength = resHeader.get("Content-Length");
	if (sLength != null) {
		try {
			int length = Integer.parseInt(sLength);
			return uis = new LimitedInputStream(length);
		} catch (NumberFormatException e) {}
	}
	return uis = is;
}
/**
 * Creates an output stream for writing to this URL Connection.
 * <code>UnknownServiceException</code> will be thrown if this url denies write access
 *
 *
 * @return 		OutputStream	The output stream to write to
 * @exception 	UnknownServiceException 	thrown when writing to URL is not supported
 *
 * @see 		#getContent()
 * @see 		#getInputStream()
 * @see 		java.io.IOException
 *
 */
public OutputStream getOutputStream() throws IOException {
	if (!doOutput)
		throw new ProtocolException(com.ibm.oti.util.Msg.getString("K008e"));

	// you can't write after you read
	if (sentRequest)
		throw new ProtocolException(com.ibm.oti.util.Msg.getString("K0090"));

	if (os != null) return os;

	// they are requesting a stream to write to. This implies a POST method
	if (method == "GET")
		setRequestMethod("POST");
	// If the request method is neither PUT or POST, then you're not writing
	if (method != "PUT" && method != "POST")
		throw new ProtocolException(com.ibm.oti.util.Msg.getString("K008f", method));

	int limit = -1;
	String contentLength = reqHeader.get("Content-Length");
	if (contentLength != null) limit = Integer.parseInt(contentLength);

	String encoding = reqHeader.get("Transfer-Encoding");
	if (httpVersion > 0 && encoding != null) {
		encoding = encoding.toLowerCase();
		if ("chunked".equals(encoding)) {
			sendChunked = true;
			limit = -1;
		}
	}
	if ((httpVersion > 0 && sendChunked) || limit >= 0) {
		os = new HttpOutputStream(limit);
		doRequest();
		return os;
	}
	return os = new HttpOutputStream();

}
/**
 * Answers the permission required to make the connection
 *
 *
 * @return 		java.security.Permission	the connection required to make the connection.
 * @exception 	java.io.IOException
 *						thrown if an IO exception occurs while computing the permission.
 */
public java.security.Permission getPermission() throws java.io.IOException {
	return new SocketPermission(getHostName() + ":" + getHostPort(), "connect, resolve");
}
/**
 * Answers the value corresponds to the field in the request Header,
 * 			null if no such field exists
 *
 *
 * @return java.lang.String		The field to look up
 *
 * @see 		#getDefaultRequestProperty
 * @see 		#setDefaultRequestProperty
 * @see 		#setRequestProperty
 */
public String getRequestProperty(String field) {
	if (connected) throw new IllegalAccessError(com.ibm.oti.util.Msg.getString("K0091"));
	return reqHeader.get(field);
}
/**
 * Answers a line read from the input stream. Does not include the \n
 *
 *
 * @return java.lang.String
 */
String readln() throws IOException {
	boolean lastCr = false;
	StringBuffer result = new StringBuffer(80);
	int c = is.read();
	if (c < 0) return null;
	while (c != '\n') {
		if (lastCr) {
			result.append('\r');
			lastCr = false;
		}
		if (c == '\r') lastCr = true;
		else result.append((char)c);
		c = is.read();
		if (c < 0) break;
	}
	return result.toString();
}

private String requestString() {
	if (usingProxy() || proxyName != null) {
		return url.toString();
	}
	String file = url.getFile();
	if (file == null || file.length() == 0)
		file = "/";
	return file;
}
/**
 * Sends the request header to the remote HTTP server
 * Not all of them are guaranteed to have any effect on the content the
 * server will return, depending on if the server supports that field.
 *
 *
 * Examples :	Accept: text/*, text/html, text/html;level=1,
 * 				Accept-Charset: iso-8859-5, unicode-1-1;q=0.8
 */
private boolean sendRequest() throws java.io.IOException {
	byte[] request = createRequest();

	// make sure we have a connection
	if (!connected) connect();
	// send out the HTTP request
	socketOut.write(request);
	sentRequest = true;
	// send any output to the socket (i.e. POST data)
	if (os != null && os.isCached()) {
		socketOut.write(os.toByteArray());
	}
	if (os == null || os.isCached()) {
		readServerResponse();
		return true;
	}
	return false;
}

void readServerResponse() throws IOException {
	socketOut.flush();
	do {
		responseCode = -1;
		responseMessage = null;
		resHeader = new Header();
		String line = readln();
		// Add the response, it may contain ':' which we ignore
		if (line != null) {
			resHeader.setStatusLine(line.trim());
			readHeaders();
		}
	} while (getResponseCode() == 100);

	if (method == "HEAD" || (responseCode >= 100 && responseCode < 200) ||
		responseCode == HTTP_NO_CONTENT || responseCode == HTTP_NOT_MODIFIED)
	{
		closeSocket();
		uis = new LimitedInputStream(0);
	}
}

/**
 * Answers the reponse code returned by the remote HTTP server
 *
 *
 * @return int	the response code, -1 if no valid response code
 * @exception java.io.IOException 	thrown when there is a IO error during the retrieval.
 *
 * @see #getResponseMessage()
 */
public int getResponseCode() throws IOException {
	// Response Code Sample : "HTTP/1.0 200 OK"

	// Call connect() first since getHeaderField() doesn't return exceptions
	doRequest();
	if (responseCode != -1) return responseCode;
	String response = resHeader.getStatusLine();
	if (response == null || !response.startsWith("HTTP/"))
		return -1;
	response.trim();
	int mark = response.indexOf(" ") + 1;
	if (mark == 0) return -1;
	if (response.charAt(mark - 2) != '1')
		httpVersion = 0;
	int last = mark + 3;
	if (last > response.length()) last = response.length();
	responseCode = Integer.parseInt(response.substring(mark, last));
	if (last + 1 <= response.length())
		responseMessage = response.substring(last + 1);
	return responseCode;
}

void readHeaders() throws IOException {
	// parse the result headers until the first blank line
	String line;
	while (((line = readln())!=null) && (line.length() > 1)) {
		// Header parsing
		int idx;
		if ((idx = line.indexOf(":")) < 0)
			resHeader.add("", line.trim());
		else
			resHeader.add(line.substring(0, idx), line.substring(idx + 1).trim());
	}
}

private byte[] createRequest() throws IOException {
	StringBuffer output = new StringBuffer(256);
	output.append(method);
	output.append(' ');
	output.append(requestString());
	output.append(' ');
	output.append("HTTP/1.");
	if (httpVersion == 0) output.append("0\r\n");
	else output.append("1\r\n");
	if (reqHeader.get("User-Agent") == null) {
		output.append("User-Agent: ");
		String agent = getSystemProperty("http.agent");
		if (agent == null) {
			output.append("Java");
			output.append(getSystemProperty("java.version"));
		} else {
			output.append(agent);
		}
		output.append("\r\n");
	}
	if (reqHeader.get("Host") == null) {
		output.append("Host: ");
		output.append(url.getHost());
		int port = url.getPort();
		if (port > 0 && port != defaultPort) {
			output.append(':');
			output.append(Integer.toString(port));
		}
		output.append("\r\n");
	}
	if (httpVersion > 0 && reqHeader.get("Connection") == null)
		output.append("Connection: close\r\n");

	// if we are doing output make sure the approprate headers are sent
	if (os != null) {
		if (reqHeader.get("Content-Type") == null)
			output.append("Content-Type: application/x-www-form-urlencoded\r\n");
		if (os.isCached()) {
			if (reqHeader.get("Content-Length") == null) {
				output.append("Content-Length: ");
				output.append(Integer.toString(os.size()));
				output.append("\r\n");
			}
		} else if (os.isChunked())
			output.append("Transfer-Encoding: chunked\r\n");
	}

	// then the user-specified request headers, if any
	for (int i = 0; i < reqHeader.length(); i++) {
		String key = reqHeader.getKey(i);
		if (key != null) {
			String lKey = key.toLowerCase();
			if ((os != null && !os.isChunked()) || (!lKey.equals("transfer-encoding") &&
				!lKey.equals("content-length")))
			{
				output.append(key);
				output.append(": ");
				/* duplicates are allowed under certain conditions
				 * see http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
				 */
				output.append(reqHeader.get(i));
				output.append("\r\n");
			}
		}
	}
	// end the headers
	output.append("\r\n");
	return output.toString().getBytes("ISO8859_1");
}

/**
 * Sets the default request header fields to be sent to the remote server. This does
 * not affect the current URL Connection, only newly created ones.
 *
 * @param field		java.lang.String	The name of the field to be changed
 * @param value 	java.lang.String	The new value of the field
 */
public static void setDefaultRequestProperty(String field, String value) {
	defaultReqHeader.add(field, value);
}
/**
 * A slightly different implementation from this parent's <code>setIfModifiedSince()</code>
 * Since this HTTP impl supports IfModifiedSince as one of the header field, the request header
 * is updated with the new value.
 *
 *
 * @param newValue the number of millisecond since epoch
 *
 * @exception 	IllegalAccessError 	thrown when this method attempts to change the flag
 *						after connected
 */
public void setIfModifiedSince(long newValue) throws IllegalAccessError {
	super.setIfModifiedSince(newValue);
	// convert from millisecond since epoch to date string
	SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	String date = sdf.format(new java.util.Date(newValue));
	reqHeader.add("If-Modified-Since", date);
}
/**
 * Sets the value of the request header field <code> field </code> to <code>newValue</code>
 * Only the current URL Connection is affected. It can only be called before the connection is made
 * This method must be overridden by protocols  which support the value of the fields.
 *
 *
 * @param field java.lang.String		the name of field to be set
 * @param newValue java.lang.String		the new value for this field
 *
 * @see 		#getDefaultRequestProperty
 * @see 		#setDefaultRequestProperty
 * @see 		#getRequestProperty
 */
public void setRequestProperty(String field, String newValue) {
	if (connected) {
		throw new IllegalAccessError(com.ibm.oti.util.Msg.getString("K0092"));
	}
	if(field == null) {
		throw new NullPointerException();
	}
	reqHeader.set(field, newValue);
}

/**
 * Adds the given request property.  Will not overwrite any existing properties associated
 * with the given field name.
 *
 * @param field the request property field name
 * @param value the property value
 *
 * @since 1.4
 */
public void addRequestProperty(String field, String value) {
	if (connected) {
			throw new IllegalAccessError(com.ibm.oti.util.Msg.getString("K0092"));
	}
	if(field == null) {
		throw new NullPointerException();
	}
	reqHeader.add(field, value);
}

/**
 * Get the connection port. This is either the URL's port or the
 * proxy port if a proxy port has been set.
 */
private int getHostPort() {
	if (hostPort != -1) return hostPort;
	String portString = getSystemPropertyOrAlternative("http.proxyPort", "proxyPort");
	if (portString != null && usingProxy()) {
		hostPort = Integer.parseInt(portString);
	} else {
		hostPort = url.getPort();
	}

	if (hostPort < 0) hostPort = defaultPort;
	return hostPort;
}

/**
 * Get the InetAddress of the connection machine. This is either
 * the address given in the URL or the address of the proxy
 * server.
 */
private InetAddress getHostAddress() throws IOException {
	return InetAddress.getByName(getHostName());
}

/**
 * Get the hostname of the connection machine. This is either
 * the name given in the URL or the name of the proxy
 * server.
 */
private String getHostName() {
	if (proxyName != null) {
		return proxyName;
	}
	if (usingProxy()) {
	    proxyName = getSystemPropertyOrAlternative("http.proxyHost", "proxyHost");
		return proxyName;
	}
	return url.getHost();
}

private String getSystemPropertyOrAlternative(final String key, final String alternativeKey) {
	String value = getSystemProperty(key);
	if (value == null) { // For backward compatibility.
	    value = getSystemProperty(alternativeKey);
	}
	return value;
}

private String getSystemProperty(final String property) {
	return (String)AccessController.doPrivileged(new PriviAction(property));
}

/**
 * Answer whether the connection should use a proxy server.
 *
 * Need to check both proxy* and http.proxy* because of change between JDK 1.0 and JDK 1.1
 */
public boolean usingProxy() {
	// First check whether the user explicitly set whether to use a proxy.
    String proxySet = getSystemProperty("http.proxySet");
	if (proxySet != null) return proxySet.toLowerCase().equals("true");

	proxySet = getSystemProperty("proxySet");
	if (proxySet != null) return proxySet.toLowerCase().equals("true");

	// The user didn't explicitly set whether to use a proxy. Answer true if the user specified a proxyHost.
	if (getSystemProperty("http.proxyHost") != null) return true;
	return getSystemProperty("proxyHost") != null;
}

/**
 * Handles an HTTP request along with its redirects and authentication
 *
 *
 */
void doRequest() throws java.io.IOException {
	// do nothing if we've already sent the request
	if (sentRequest) {
		// If necessary, finish the request by
		// closing the uncached output stream.
		if (resHeader == null && os != null) {
			os.close();
			readServerResponse();
			getContentStream();
		}
		return;
	}

	int redirect = 0;
	while(true) {
		// send the request and process the results
		if (!sendRequest()) return;

		// authorization failed ?
		if (responseCode == HTTP_UNAUTHORIZED) {		// keep asking for username/password until authorized
			String challenge = resHeader.get("WWW-Authenticate");
			if (challenge == null) break;
			int idx = challenge.indexOf(" ");
			String scheme = challenge.substring(0, idx);
			int realm = challenge.indexOf("realm=\"") + 7;
			String prompt = null;
			if (realm != -1) {
				int end = challenge.indexOf('"', realm);
				if (end != -1) prompt = challenge.substring(realm, end);
			}

			// the following will use the user-defined authenticator to get the password
			PasswordAuthentication pa =
					Authenticator.requestPasswordAuthentication(getHostAddress(),
											getHostPort(),
											url.getProtocol(),
											prompt,
											scheme);
			if (pa == null) break;
			// drop everything and reconnect, might not be required for HTTP/1.1
			endRequest();
			closeSocket();
			connected = false;
			// base64 encode the username and password
			byte[] bytes = (pa.getUserName() + ":" + new String(pa.getPassword())).getBytes("ISO8859_1");
			String encoded = new String(com.ibm.oti.util.BASE64Encoder.encode(bytes), "ISO8859_1");
			setRequestProperty("Authorization", scheme + " " + encoded);
			continue;
		}

		// See if there is a server redirect to the URL, but only handle 1 level of
		// URL redirection from the server to avoid being caught in an infinite loop
		if (getInstanceFollowRedirects()) {
			if ((responseCode == HTTP_MULT_CHOICE || responseCode == HTTP_MOVED_PERM ||
				responseCode == HTTP_MOVED_TEMP || responseCode == HTTP_SEE_OTHER ||
				responseCode == HTTP_USE_PROXY) && os == null) {

				if (++redirect > 4)
					throw new ProtocolException(com.ibm.oti.util.Msg.getString("K0093"));
				String location = getHeaderField("Location");
			 	if (location != null) {
					// start over
					if (responseCode == HTTP_USE_PROXY) {
						int start = 0;
						if(location.startsWith(url.getProtocol() + ':'))
							start = url.getProtocol().length() + 1;
						if(location.startsWith("//", start))
							start += 2;
						setProxy(location.substring(start));
					}
					else {
						url = new URL(url, location);
						//update the port
						hostPort = -1;
					}
					endRequest();
					closeSocket();
					connected = false;
					continue;
				}
			}
		}

		break;
	}

	// Cache the content stream and read the first chunked header
	getContentStream();
}

private void setProxy(String proxy) {
	int index = proxy.indexOf(':');
	if (index == -1) {
		proxyName = proxy;
		hostPort = defaultPort;
	} else {
		proxyName = proxy.substring(0, index);
		String port = proxy.substring(index + 1);
		try {
			hostPort = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K00af", port));
		}
		if (hostPort < 0 || hostPort > 65535)
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K00b0"));
	}
}
}
