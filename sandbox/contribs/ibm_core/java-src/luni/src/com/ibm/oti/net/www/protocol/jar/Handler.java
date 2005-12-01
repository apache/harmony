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

package com.ibm.oti.net.www.protocol.jar;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.ibm.oti.util.Msg;

public class Handler extends URLStreamHandler {
	/**
	 * Answers a connection to the jar file pointed by this <code>URL</code>
	 * in the file system
	 * 
	 * @return java.net.URLConnection A connection to the resource pointed by
	 *         this url.
	 * @param u
	 *            java.net.URL The URL to which the connection is pointing to
	 * 
	 * @thows IOException thrown if an IO error occurs when this method tries to
	 *        establish connection.
	 */
	protected URLConnection openConnection(URL u) throws IOException {
		return new JarURLConnection(u);
	}

	/**
	 * 
	 * @param url
	 *            URL the context URL
	 * @param spec
	 *            java.lang.String the spec string
	 * @param start
	 *            int the location to start parsing from
	 * @param limit
	 *            int the location where parsing ends
	 */
	protected void parseURL(URL url, String spec, int start, int limit) {
		String file = url.getFile();
		if (file == null)
			file = "";
		if (limit > start)
			spec = spec.substring(start, limit);
		else
			spec = "";
		if (spec.indexOf("!/") == -1 && (file.indexOf("!/") == -1))
			throw new NullPointerException(Msg.getString("K01b6"));
		if (spec.charAt(0) == '/')// File is absolute
			file = file.substring(0, file.indexOf('!') + 1) + spec;
		else
			file = file.substring(0, file.lastIndexOf('/') + 1) + spec;
		try {
			// check that the embedded url is valid
			new URL(file);
		} catch (MalformedURLException e) {
			throw new NullPointerException(e.toString());
		}
		setURL(url, "jar", "", -1, null, null, file, null, null);
	}

	/**
	 * Build and return the externalized string representation of url.
	 * 
	 * @return String the externalized string representation of url
	 * @param url
	 *            a URL
	 */
	protected String toExternalForm(URL url) {
		StringBuffer sb = new StringBuffer();
		sb.append("jar:");
		sb.append(url.getFile());
		String ref = url.getRef();
		if (ref != null)
			sb.append(ref);
		return sb.toString();
	}
}
