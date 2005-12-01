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
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class establishes a connection to a URL using the jar protocol. Jar URLs
 * are specified as follows: <center><code>jar:<url>!/{entry}</code></center>
 * where "!/" is called a seperator.
 */
public abstract class JarURLConnection extends URLConnection {
	// the location of the seperator
	protected URLConnection jarFileURLConnection;

	private String entryName;

	private URL fileURL;

	// the file component of the URL
	private String file;

	/**
	 * Contructs an instance of <code>JarURLConnection</code>.
	 * 
	 * @param url
	 *            java.net.URL the URL that contains the location to connect to
	 */
	protected JarURLConnection(URL url) throws MalformedURLException {
		super(url);
		file = url.getFile();
		int sepIdx;
		// Support embedded jar URLs by using lastIndexOf()
		if ((sepIdx = file.lastIndexOf("!/")) < 0) {
			throw new MalformedURLException();
		}
		if (file.length() == sepIdx + 2) {
			return;
		}
		entryName = file.substring(sepIdx + 2, file.length());
	}

	/**
	 * Answers the attributes of the JarEntry referenced by this
	 * <code>JarURLConnection</code>.
	 * 
	 * @return java.util.jar.Attributes the attributes of the the JarEntry
	 * @exception java.io.IOException
	 *                thrown if an IO exception occurs while retrieving the
	 *                JarEntry
	 */
	public Attributes getAttributes() throws java.io.IOException {
		JarEntry jEntry = getJarEntry();
		return (jEntry == null) ? null : jEntry.getAttributes();
	}

	/**
	 * Answers the Certificates of the JarEntry referenced by this
	 * <code>URLConnection</code>. This method will return null until the
	 * InputStream has been completely verified
	 * 
	 * @return Certificate[] the Certificates of the JarEntry.
	 * @exception java.io.IOException
	 *                thrown if there is an IO exception occurs while getting
	 *                the JarEntry.
	 */
	public Certificate[] getCertificates() throws java.io.IOException {
		JarEntry jEntry = getJarEntry();
		if (jEntry == null)
			return null;

		return jEntry.getCertificates();

	}

	/**
	 * Answers the JarEntry name of the entry referenced by this
	 * <code>URLConnection</code>.
	 * 
	 * @return java.lang.String the JarEntry name
	 */
	public String getEntryName() {
		return entryName;
	}

	/**
	 * Answers the JarEntry of the entry referenced by this
	 * <code>URLConnection</code>.
	 * 
	 * @return java.util.jar.JarEntry the JarEntry referenced
	 */
	public JarEntry getJarEntry() throws IOException {
		if (!connected) {
			connect();
		}
		if (entryName == null) {
			return null;
		}
		// The entry must exist since the connect succeeded
		return getJarFile().getJarEntry(entryName);
	}

	/**
	 * Answers the Manifest associated with the Jar URL
	 * 
	 * @return java.util.jar.Manifest The JarFile's Manifest
	 */
	public Manifest getManifest() throws java.io.IOException {
		return getJarFile().getManifest();
	}

	/**
	 * Answers the the JarFile referenced by this <code>URLConnection</code>.
	 * 
	 * @return java.util.jar.JarFile the JarFile
	 * @exception java.io.IOException
	 *                thrown if an IO exception occurs while retrieving the Jar
	 *                file
	 */
	public abstract JarFile getJarFile() throws java.io.IOException;

	/**
	 * Answers the URL of the JarFile referenced by this
	 * <code>URLConnection</code>.
	 * 
	 * @return java.net.URL the URL of the JarFile.
	 */
	public URL getJarFileURL() {
		if (fileURL != null)
			return fileURL;
		try {
			// Support embedded jar URLs by using lastIndexOf()
			return fileURL = new URL(url.getFile().substring(0,
					url.getFile().lastIndexOf("!/")));
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Answers the main Attributes of the JarFile referenced by this
	 * <code>URLConnection</code>.
	 * 
	 * @return java.util.jar.Attributes the Attributes of the the JarFile
	 * @exception java.io.IOException
	 *                thrown if an IO exception occurs while retrieving the
	 *                JarFile
	 */
	public Attributes getMainAttributes() throws java.io.IOException {
		Manifest m = getJarFile().getManifest();
		return (m == null) ? null : m.getMainAttributes();
	}
}
