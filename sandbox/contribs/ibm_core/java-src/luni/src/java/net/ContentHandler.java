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


import java.io.IOException;

/**
 * This class converts the content of a certain format into a Java type Object.
 * It is implemented differently for each content type in each platform. It is
 * created by <code>ContentHandlerFactory</code>
 * 
 * @see ContentHandlerFactory
 * @see URL
 * @see URLConnection#getContent()
 */
abstract public class ContentHandler {
	/**
	 * Answers the object pointed by the specified URL Connection
	 * <code>uConn</code>.
	 * 
	 * @return java.lang.Object the object refered by <code>uConn</code>
	 * @param uConn
	 *            java.net.URLConnection the url connection that points to the
	 *            desired object
	 * @exception java.io.IOException
	 *                thrown if an IO error occurs during the retrieval of the
	 *                object
	 */
	public abstract Object getContent(URLConnection uConn) throws IOException;

	/**
	 * Answers the object pointed by the specified URL Connection
	 * <code>uConn</code>.
	 * 
	 * @param uConn
	 *            java.net.URLConnection the url connection that points to the
	 *            desired object
	 * @param types
	 *            The list of acceptable content types
	 * @return Object The object of the resource pointed by this URL, or null if
	 *         the content does not match a specified content type.
	 * 
	 * @exception IOException
	 *                If an error occured obtaining the content.
	 */
	public Object getContent(URLConnection uConn, Class[] types)
			throws IOException {
		Object content = getContent(uConn);
		Class cl = content.getClass();
		for (int i = 0; i < types.length; i++) {
			if (cl == types[i]) {
				return content;
			}
		}
		return null;
	}
}
