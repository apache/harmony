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

package com.ibm.oti.net.www.protocol.file;


import java.net.URL;
import java.net.URLConnection;

/**
 * This is the handler that is responsible for reading files from the file
 * system.
 */

public class Handler extends java.net.URLStreamHandler {

	/**
	 * Answers a connection to the a file pointed by this <code>URL</code> in
	 * the file system
	 * 
	 * @return java.net.URLConnection A connection to the resource pointed by
	 *         this url.
	 * @param url
	 *            java.net.URL The URL to which the connection is pointing to
	 * 
	 */
	public URLConnection openConnection(URL url) {
		return new FileURLConnection(url);
	}

	/**
	 * Parse the <code>string</code>str into <code>URL</code> u which
	 * already have the context properties. The string generally have the
	 * following format: <code><center>/c:/windows/win.ini</center></code>.
	 * 
	 * @param u
	 *            java.net.URL The URL object that's parsed into
	 * @param str
	 *            java.lang.String The string equivalent of the specification
	 *            URL
	 * @param start
	 *            int The index in the spec string from which to begin parsing
	 * @param end
	 *            int The index to stop parsing
	 * 
	 * @see java.net.URLStreamHandler#toExternalForm(URL)
	 * @see java.net.URL
	 */
	protected void parseURL(URL u, String str, int start, int end) {
		if (end < start) {
			return;
		}
		String parseString = "";
		if (start < end) {
			parseString = str.substring(start, end).replace('\\', '/');
		}
		super.parseURL(u, parseString, 0, parseString.length());
	}
}
