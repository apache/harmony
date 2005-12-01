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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import com.ibm.oti.util.Msg;
import com.ibm.oti.vm.VM;

/**
 * This subclass extends <code>URLConnection</code>.
 * <p>
 * 
 * This class is responsible for connecting and retrieving resources from a Jar
 * file which can be anywhere that can be refered to by an URL.
 * 
 */
public class JarURLConnection extends java.net.JarURLConnection {
	static Hashtable jarCache = new Hashtable();

	InputStream jarInput;

	private JarFile jarFile;

	private JarEntry jarEntry;

	ReferenceQueue cacheQueue = new ReferenceQueue();

	static TreeSet lru = new TreeSet(new LRUComparitor());

	static int Limit;
	static {
		Limit = ((Integer) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return Integer.getInteger("jar.cacheSize", 500);
					}
				})).intValue();
		VM.closeJars();
	}

	static final class CacheEntry extends WeakReference {
		Object key;

		CacheEntry(Object jar, String key, ReferenceQueue queue) {
			super(jar, queue);
			this.key = key;
		}
	}

	static final class LRUKey {
		JarFile jar;

		long ts;

		LRUKey(JarFile file, long time) {
			jar = file;
			ts = time;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return jar == ((LRUKey) obj).jar;
		}
	}

	static final class LRUComparitor implements Comparator {
		LRUComparitor() {
		}

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			if (((LRUKey) o1).ts > ((LRUKey) o2).ts) {
				return 1;
			}
			return ((LRUKey) o1).ts == ((LRUKey) o2).ts ? 0 : -1;
		}

		/**
		 * @param o1
		 *            an object to compare
		 * @param o2
		 *            an object to compare
		 * @return <code>true</code> if the objects are equal,
		 *         <code>false</code> otherwise.
		 */
		public boolean equals(Object o1, Object o2) {
			return o1.equals(o2);
		}
	}

	/**
	 * @param url
	 *            the URL of the JAR
	 * @throws java.net.MalformedURLException
	 *             if the URL is malformed
	 */
	public JarURLConnection(java.net.URL url)
			throws java.net.MalformedURLException {
		super(url);
	}

	/**
	 * @see java.net.URLConnection#connect()
	 */
	public void connect() throws IOException {
		jarFileURLConnection = getJarFileURL().openConnection();
		findJarFile(); // ensure the file can be found
		findJarEntry(); // ensure the entry, if any, can be found
		connected = true;
	}

	/**
	 * Answers the Jar file refered by this <code>URLConnection</code>
	 * 
	 * @return the JAR file referenced by this connection
	 * 
	 * @throws IOException
	 *             thrown if an IO error occurs while connecting to the
	 *             resource.
	 */
	public JarFile getJarFile() throws IOException {
		if (!connected)
			connect();
		return jarFile;
	}

	/**
	 * Answers the Jar file refered by this <code>URLConnection</code>
	 * 
	 * @throws IOException
	 *             if an IO error occurs while connecting to the resource.
	 */
	private void findJarFile() throws IOException {
		URL jarFileURL = getJarFileURL();
		if (jarFileURL.getProtocol().equals("file")) {
			String fileName = jarFileURL.getFile();
			String host = jarFileURL.getHost();
			if (host != null && host.length() > 0)
				fileName = "//" + host + fileName;
			jarFile = openJarFile(fileName, fileName, false);
			return;
		}

		final String externalForm = jarFileURLConnection.getURL()
				.toExternalForm();
		jarFile = (JarFile) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						try {
							return openJarFile(null, externalForm, false);
						} catch (IOException e) {
							return null;
						}
					}
				});
		if (jarFile != null)
			return;

		// Build a temp jar file
		final InputStream is = jarFileURLConnection.getInputStream();
		try {
			jarFile = (JarFile) AccessController
					.doPrivileged(new PrivilegedAction() {
						public Object run() {
							try {
								File tempJar = File.createTempFile("hyjar_",
										".tmp", null);
								FileOutputStream fos = new FileOutputStream(
										tempJar);
								byte[] buf = new byte[4096];
								int nbytes = 0;
								while ((nbytes = is.read(buf)) > -1)
									fos.write(buf, 0, nbytes);
								fos.close();
								String path = tempJar.getPath();
								return openJarFile(path, externalForm, true);
							} catch (IOException e) {
								return null;
							}
						}
					});
		} finally {
			is.close();
		}
		if (jarFile == null)
			throw new IOException();
	}

	JarFile openJarFile(String fileString, String key, boolean temp)
			throws IOException {
		CacheEntry entry;
		while ((entry = (CacheEntry) cacheQueue.poll()) != null)
			jarCache.remove(entry.key);
		entry = (CacheEntry) jarCache.get(key);
		JarFile jar = null;
		if (entry != null)
			jar = (JarFile) entry.get();
		if (jar == null && fileString != null) {
			int flags = ZipFile.OPEN_READ + (temp ? ZipFile.OPEN_DELETE : 0);
			jar = new JarFile(new File(fileString), true, flags);
			jarCache.put(key, new CacheEntry(jar, key, cacheQueue));
		} else {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkPermission(getPermission());
			if (temp)
				lru.remove(new LRUKey(jar, 0));
		}
		if (temp) {
			lru.add(new LRUKey(jar, new Date().getTime()));
			if (lru.size() > Limit)
				lru.remove(lru.first());
		}
		return jar;
	}

	/**
	 * Answers the JarEntry of the entry referenced by this
	 * <code>URLConnection</code>.
	 * 
	 * @return java.util.jar.JarEntry the JarEntry referenced
	 * 
	 * @throws IOException
	 *             if an IO error occurs while getting the entry
	 */
	public JarEntry getJarEntry() throws IOException {
		if (!connected)
			connect();
		return jarEntry;

	}

	/**
	 * Look up the JarEntry of the entry referenced by this
	 * <code>URLConnection</code>.
	 */
	private void findJarEntry() throws IOException {
		if (getEntryName() == null)
			return;
		jarEntry = jarFile.getJarEntry(getEntryName());
		if (jarEntry == null)
			throw new FileNotFoundException(getEntryName());
	}

	/**
	 * Creates an input stream for reading from this URL Connection.
	 * 
	 * @return the input stream
	 * 
	 * @throws IOException
	 *             if an IO error occurs while connecting to the resource.
	 */
	public InputStream getInputStream() throws IOException {
		if (!connected)
			connect();
		if (jarInput != null)
			return jarInput;
		if (jarEntry == null)
			throw new IOException(Msg.getString("K00fc"));
		return jarInput = jarFile.getInputStream(jarEntry);
	}

	/**
	 * Answers the content type of the resource. Test cases reveal that only if
	 * the URL is refering to a Jar file, that this method answers a non-null
	 * value - x-java/jar.
	 * 
	 * @return the content type
	 */
	public String getContentType() {
		// it could also return "x-java/jar" which jdk returns but here, we get
		// it from the URLConnection
		try {
			if (url.getFile().endsWith("!/"))
				return getJarFileURL().openConnection().getContentType();
		} catch (IOException ioe) {
		}
		// if there is an Jar Entry, get the content type from the name
		return guessContentTypeFromName(url.getFile());
	}

	/**
	 * Answers the content length of the resource. Test cases reveal that if the
	 * URL is refering to a Jar file, this method answers a content-length
	 * returned by URLConnection. If not, it will return -1.
	 * 
	 * @return the content length
	 */
	public int getContentLength() {
		try {
			if (url.getFile().endsWith("!/"))
				return getJarFileURL().openConnection().getContentLength();
		} catch (IOException e) {
		}
		return -1;
	}

	/**
	 * Answers the object pointed by this <code>URL</code>. If this
	 * URLConnection is pointing to a Jar File (no Jar Entry), this method will
	 * return a <code>JarFile</code> If there is a Jar Entry, it will return
	 * the object corresponding to the Jar entry content type.
	 * 
	 * @return a non-null object
	 * 
	 * @throws IOException
	 *             if an IO error occured
	 * 
	 * @see ContentHandler
	 * @see ContentHandlerFactory
	 * @see java.io.IOException
	 * @see #setContentHandlerFactory(ContentHandlerFactory)
	 */
	public Object getContent() throws java.io.IOException {
		if (!connected)
			connect();
		// if there is no Jar Entry, return a JarFile
		if (jarEntry == null)
			return jarFile;
		return super.getContent();
	}

	/**
	 * Answers the permission, in this case the subclass, FilePermission object
	 * which represents the permission necessary for this URLConnection to
	 * establish the connection.
	 * 
	 * @return the permission required for this URLConnection.
	 * 
	 * @throws IOException
	 *             thrown when an IO exception occurs while creating the
	 *             permission.
	 */
	public java.security.Permission getPermission() throws IOException {
		if (jarFileURLConnection != null)
			return jarFileURLConnection.getPermission();
		return getJarFileURL().openConnection().getPermission();
	}

	/**
	 * Closes the cached files.
	 */
	public static void closeCachedFiles() {
		java.util.Enumeration elemEnum = jarCache.elements();
		while (elemEnum.hasMoreElements()) {
			try {
				ZipFile zip = (ZipFile) ((CacheEntry) elemEnum.nextElement())
						.get();
				if (zip != null)
					zip.close();
			} catch (IOException e) {
			}
		}
	}
}
