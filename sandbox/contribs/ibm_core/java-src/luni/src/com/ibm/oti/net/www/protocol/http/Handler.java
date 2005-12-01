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

package com.ibm.oti.net.www.protocol.http;


import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.URLStreamHandler;

/**
 * This is the handler that manages all transactions between the client and a HTTP remote server.
 *
 */
public class Handler extends URLStreamHandler {

/**
 * Answers a connection to the HTTP server specified by this <code>URL</code>.
 *
 * @param 		u 		the URL to which the connection is pointing to
 * @return 		a connection to the resource pointed by this url.
 *
 * @thows		IOException 	if this handler fails to establish a connection
 */
protected URLConnection openConnection(URL u) throws IOException {
	return new HttpURLConnection(u, getDefaultPort());
}

/**
 * Return the default port.
 */
protected int getDefaultPort() {
	return 80;
}
}
