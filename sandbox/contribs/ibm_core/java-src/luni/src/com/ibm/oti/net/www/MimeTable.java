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

package com.ibm.oti.net.www;


import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

/**
 * Instances of this class map file extensions to MIME content types based on a
 * default MIME table.
 * <p>
 * The default values can be overridden by modifying the contents of the file
 * "types.properties".
 */

public class MimeTable implements FileNameMap {

	public static final String UNKNOWN = "content/unknown";

	/**
	 * A hash table containing the mapping between extensions and mime types.
	 */
	public static final Properties types = new Properties();

	// Default mapping.
	static {
		types.put("text", "text/plain");
		types.put("txt", "text/plain");
		types.put("htm", "text/html");
		types.put("html", "text/html");
	}

	/**
	 * Contructs a MIME table using the default values defined in this class.
	 * <p>
	 * It then augments this table by reading pairs of extensions and matching
	 * content types from the file "types.properties", which is represented in
	 * standard java.util.Properties.load(...) format.
	 */
	public MimeTable() {
		InputStream str = (InputStream) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return MimeTable.this.getClass().getResourceAsStream(
								"types.properties");
					}
				});

		try {
			if (str != null)
				types.load(str);
		} catch (IOException ex) {
		}
	}

	/**
	 * Determines the MIME type for the given filename.
	 * 
	 * @return java.lang.String The mime type associated with the file's
	 *         extension or null if no mapping is known.
	 * @param filename
	 *            java.lang.String The file whose extension will be mapped.
	 */
	public String getContentTypeFor(String filename) {
		if (filename.endsWith("/"))
			// a directory, return html
			return (String) types.get("html");
		int lastCharInExtension = filename.lastIndexOf('#');
		if (lastCharInExtension < 0)
			lastCharInExtension = filename.length();
		int firstCharInExtension = filename.lastIndexOf('.') + 1;
		String ext = "";
		if (firstCharInExtension > filename.lastIndexOf('/'))
			ext = filename.substring(firstCharInExtension, lastCharInExtension);
		return (String) types.get(ext.toLowerCase());
	}

}
