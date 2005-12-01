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

package com.ibm.oti.net.www.protocol.ftp;


import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

import com.ibm.oti.net.www.MimeTable;
import com.ibm.oti.util.Msg;

public class FtpURLConnection extends URLConnection {

	Socket controlSocket, dataSocket;

	ServerSocket acceptSocket;

	InputStream ctrlInput, inputStream;

	OutputStream ctrlOutput;

	private String replyCode, hostName;

	int dataPort;

	private static final int FTP_PORT = 21;

	private String PASSWORD = "";

	private String USERNAME = "anonymous";

	// FTP Reply Constants
	private static final int FTP_DATAOPEN = 125;

	private static final int FTP_OPENDATA = 150;

	private static final int FTP_OK = 200;

	private static final int FTP_USERREADY = 220;

	private static final int FTP_TRANSFEROK = 226;

	//private static final int FTP_PASV = 227;

	private static final int FTP_LOGGEDIN = 230;

	private static final int FTP_FILEOK = 250;

	private static final int FTP_PASWD = 331;

	//private static final int FTP_DATAERROR = 451;

	//private static final int FTP_ERROR = 500;

	private static final int FTP_NOTFOUND = 550;

	/**
	 * FtpURLConnection constructor comment.
	 * 
	 * @param url
	 *            java.net.URL
	 */
	protected FtpURLConnection(URL url) {
		super(url);
		hostName = url.getHost();
		String parse = url.getUserInfo();
		if (parse != null) {
			int split = parse.indexOf(':');
			if (split >= 0) {
				USERNAME = parse.substring(0, split);
				PASSWORD = parse.substring(split + 1);
			} else
				USERNAME = parse;
		}
	}

	/* Change the server directory to that specfied in the URL */
	private void cd() throws IOException {
		int idx = url.getFile().lastIndexOf('/');

		if (idx > 0) {
			String dir = url.getFile().substring(0, idx);
			write("CWD " + dir + "\r\n");
			int reply = getReply();
			if (reply != FTP_FILEOK && dir.length() > 0 && dir.charAt(0) == '/') {
				write("CWD " + dir.substring(1) + "\r\n");
				reply = getReply();
			}
			if (reply != FTP_FILEOK)
				throw new IOException(Msg.getString("K0094"));
		}
	}

	/**
	 * Establishes the connection to the resource specified by this
	 * <code>URL</code>
	 * 
	 * @see #connected
	 * @see java.io.IOException
	 * @see URLStreamHandler
	 */
	public void connect() throws IOException {
		int port = url.getPort();
		if (port <= 0)
			port = FTP_PORT;
		controlSocket = new Socket(hostName, port);
		connected = true;
		ctrlOutput = controlSocket.getOutputStream();
		ctrlInput = controlSocket.getInputStream();
		login();
		setType();
		if (!getDoInput())
			cd();

		try {
			acceptSocket = new ServerSocket(0);
			dataPort = acceptSocket.getLocalPort();
			/* Cannot set REUSEADDR so we need to send a PORT comannd */
			port();
			acceptSocket.setSoTimeout(3000);
			if (getDoInput())
				getFile();
			else
				sendFile();
			dataSocket = acceptSocket.accept();
			acceptSocket.close();
		} catch (InterruptedIOException e) {
			throw new IOException(Msg.getString("K0095"));
		}
		if (getDoInput())
			inputStream = new FtpURLInputStream(new BufferedInputStream(
					dataSocket.getInputStream()), controlSocket);

	}

	/*
	 * Answers the content type of the resource. Just takes a guess based on the
	 * name.
	 */
	public String getContentType() {
		String result = guessContentTypeFromName(url.getFile());
		if (result == null)
			return MimeTable.UNKNOWN;
		return result;
	}

	private void getFile() throws IOException {
		int reply;
		String file = url.getFile();
		write("RETR " + file + "\r\n");
		reply = getReply();
		if (reply == FTP_NOTFOUND && file.length() > 0 && file.charAt(0) == '/') {
			write("RETR " + file.substring(1) + "\r\n");
			reply = getReply();
		}
		if (!(reply == FTP_OPENDATA || reply == FTP_TRANSFEROK))
			throw new FileNotFoundException(Msg.getString("K0096", reply));
	}

	/**
	 * Creates a input stream for writing to this URL Connection.
	 * 
	 * @return InputStream The input stream to write to
	 * @exception IOException
	 *                Cannot read from URL ro error creating InputStream
	 * 
	 * @see #getContent()
	 * @see #getOutputStream()
	 * @see java.io.InputStream
	 * @see java.io.IOException
	 * 
	 */

	public InputStream getInputStream() throws IOException {
		if (!connected)
			connect();
		return inputStream;
	}

	/**
	 * Answers the permission object (in this case, SocketPermission) with the
	 * host and the port number as the target name and "resolve, connect" as the
	 * action list.
	 * 
	 * @return java.security.Permission the permission object required for this
	 *         connection
	 * @exception java.io.IOException
	 *                thrown when an IO exception occurs during the creation of
	 *                the permission object.
	 */
	public Permission getPermission() throws IOException {
		int port = url.getPort();
		if (port <= 0)
			port = FTP_PORT;
		return new SocketPermission(hostName + ":" + port, "connect, resolve");
	}

	/**
	 * Creates a output stream for writing to this URL Connection.
	 * 
	 * @return OutputStream The output stream to write to
	 * @exception IOException
	 *                Thrown when the OutputStream could not be created
	 * 
	 * @see #getContent()
	 * @see #getInputStream()
	 * @see java.io.IOException
	 * 
	 */

	public OutputStream getOutputStream() throws IOException {
		if (!connected)
			connect();
		return dataSocket.getOutputStream();
	}

	private int getReply() throws IOException {
		byte[] code = new byte[3];
		ctrlInput.read(code, 0, code.length);
		replyCode = new String(code, "ISO8859_1");
		boolean multiline = false;
		if (ctrlInput.read() == '-')
			multiline = true;
		readLine(); /* Skip the rest of the first line */
		if (multiline)
			while (readMultiLine()) {/* Read all of a multiline reply */
			}
		return Integer.parseInt(new String(code, "ISO8859_1"));
	}

	private void login() throws IOException {
		int reply;
		reply = getReply();
		if (reply == FTP_USERREADY) {
		} else {
			throw new IOException(Msg.getString("K0097", url.getHost()));
		}
		write("USER " + USERNAME + "\r\n");
		reply = getReply();
		if (reply == FTP_PASWD || reply == FTP_LOGGEDIN) {
		} else {
			throw new IOException(Msg.getString("K0098", url.getHost()));
		}
		if (reply == FTP_PASWD) {
			write("PASS " + PASSWORD + "\r\n");
			reply = getReply();
			if (!(reply == FTP_OK || reply == FTP_USERREADY || reply == FTP_LOGGEDIN))
				throw new IOException(Msg.getString("K0098", url.getHost()));
		}
	}

	private void port() throws IOException {
		write("PORT "
				+ controlSocket.getLocalAddress().getHostAddress().replace('.',
						',') + ',' + (dataPort >> 8) + ',' + (dataPort & 255)
				+ "\r\n");
		if (getReply() != FTP_OK)
			throw new IOException(Msg.getString("K0099"));
	}

	/* Read a line of text and return it for posible parsing */
	private String readLine() throws IOException {
		StringBuffer sb = new StringBuffer();
		int c;
		while ((c = ctrlInput.read()) != '\n') {
			sb.append((char) c);
		}
		return sb.toString();
	}

	private boolean readMultiLine() throws IOException {
		String line = readLine();
		if (line.length() < 4)
			return true;
		if (line.substring(0, 3).equals(replyCode)
				&& (line.charAt(3) == (char) 32))
			return false;
		return true;
	}

	/*
	 * Issue the STOR command to the server with the file as the parameter
	 */
	private void sendFile() throws IOException {
		int reply;
		write("STOR "
				+ url.getFile().substring(url.getFile().lastIndexOf('/') + 1,
						url.getFile().length()) + "\r\n");
		reply = getReply();
		if (!(reply == FTP_OPENDATA || reply == FTP_OK || reply == FTP_DATAOPEN))
			throw new IOException(Msg.getString("K009a"));
	}

	/**
	 * Set the flag if this <code>URLConnection</code> supports input (read).
	 * It cannot be set after the connection is made. FtpURLConnections cannot
	 * support both input and output
	 * 
	 * @param newValue
	 *            boolean
	 * 
	 * @exception IllegalAccessError
	 *                Exception thrown when this method attempts to change the
	 *                flag after connected
	 * 
	 * @see #doInput
	 * @see #getDoInput()
	 * @see java.lang.IllegalAccessError
	 * @see #setDoInput(boolean)
	 */
	public void setDoInput(boolean newValue) {
		if (connected) {
			throw new IllegalAccessError();
		}
		this.doInput = newValue;
		this.doOutput = !newValue;
	}

	/**
	 * Set the flag if this <code>URLConnection</code> supports output(read).
	 * It cannot be set after the connection is made.\ FtpURLConnections cannot
	 * support both input and output.
	 * 
	 * @param newValue
	 *            boolean
	 * 
	 * @exception IllegalAccessError
	 *                Exception thrown when this method attempts to change the
	 *                flag after connected
	 * 
	 * @see #doOutput
	 * @see java.lang.IllegalAccessError
	 * @see #setDoOutput(boolean)
	 */
	public void setDoOutput(boolean newValue) {
		if (connected) {
			throw new IllegalAccessError();
		}
		this.doOutput = newValue;
		this.doInput = !newValue;
	}

	/*
	 Set the type of the file transfer. Only Image is suported
	 */
	private void setType() throws IOException {
		write("TYPE I\r\n");
		if (getReply() != FTP_OK)
			throw new IOException(Msg.getString("K009b"));
	}

	private void write(String command) throws IOException {
		ctrlOutput.write(command.getBytes("ISO8859_1"));
	}
}
