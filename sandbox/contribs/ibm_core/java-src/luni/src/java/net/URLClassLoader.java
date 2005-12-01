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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import com.ibm.oti.util.InvalidJarIndexException;

/**
 * This class loader is responsible for loading classes and resources from a
 * list of URLs which can refer to either directories or JAR files. Classes
 * loaded by this <code>URLClassLoader</code> are granted permission to access
 * the URLs contained in the URL search list.
 */
public class URLClassLoader extends SecureClassLoader {

	private static URL[] NO_PATH = new URL[0];

	URL[] urls, orgUrls;

	private IdentityHashMap resCache = new IdentityHashMap(32);

	private URLStreamHandlerFactory factory;

	HashMap extensions;

	Hashtable[] indexes;

	private AccessControlContext currentContext = null;

	static class SubURLClassLoader extends URLClassLoader {
		// The subclass that overwrites the loadClass() method
		private boolean checkingPackageAccess = false;

		SubURLClassLoader(URL[] urls) {
			super(urls, ClassLoader.getSystemClassLoader());
		}

		SubURLClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		/**
		 * Overrides the loadClass() of <code>ClassLoader</code>. It calls
		 * the security manager's <code>checkPackageAccess()</code> before
		 * attempting toload the class.
		 * 
		 * @return java.lang.Class the Class object.
		 * @param className
		 *            String the name of the class to search for.
		 * @param resolveClass
		 *            boolean indicates if class should be resolved after
		 *            loading.
		 * @exception ClassNotFoundException
		 *                If the class could not be found.
		 */
		protected synchronized Class loadClass(String className,
				boolean resolveClass) throws ClassNotFoundException {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null && !checkingPackageAccess) {
				int index = className.lastIndexOf('.');
				if (index > 0) { // skip if class is from a default package
					try {
						checkingPackageAccess = true;
						sm.checkPackageAccess(className.substring(0, index));
					} finally {
						checkingPackageAccess = false;
					}
				}
			}
			return super.loadClass(className, resolveClass);
		}
	}

	/**
	 * Constructs a new instance of this class. The newly created instance will
	 * have the system ClassLoader as its parent. URLs that end with "/" are
	 * assumed to be directories, otherwise they are assumed to be Jar files.
	 * 
	 * @param urls
	 *            java.net.URL[] the URLs to search
	 * 
	 * @exception SecurityException
	 *                if a security manager exists and its
	 *                checkCreateClassLoader method doesn't allow creation of
	 *                new ClassLoaders
	 */

	public URLClassLoader(URL[] urls) {
		this(urls, ClassLoader.getSystemClassLoader(), null);
	}

	/**
	 * Constructs a new instance of this class. The newly created instance will
	 * have the specified ClassLoader as its parent. URLs that end with "/" are
	 * assumed to be directories, otherwise they are assumed to be Jar files.
	 * 
	 * @param urls
	 *            java.net.URL[] the URLs to search
	 * 
	 * @param parent
	 *            ClassLoader the ClassLoader to assign as this loader's parent.
	 * 
	 * @exception SecurityException
	 *                if a security manager exists and its
	 *                checkCreateClassLoader method doesn't allow creation of
	 *                new ClassLoaders
	 */

	public URLClassLoader(URL[] urls, ClassLoader parent) {
		this(urls, parent, null);
	}

	/**
	 * Adds the specified URL to the search list.
	 * 
	 * @param url
	 *            java.net.URL the new URL
	 */
	protected void addURL(URL url) {
		try {
			URL search = createSearchURL(url);
			urls = addURL(urls, search);
			orgUrls = addURL(orgUrls, url);
			extensions.put(search, null);
		} catch (MalformedURLException e) {
		}
	}

	/**
	 * Returns an array with the given URL added to the given array.
	 * 
	 * @param urlArray
	 *            java.net.URL[] the source array
	 * @param url
	 *            java.net.URL the URL to be added
	 * @return java.net.URL[] an array made of the given array and the new URL
	 */
	URL[] addURL(URL[] urlArray, URL url) {
		URL[] newPath = new URL[urlArray.length + 1];
		System.arraycopy(urlArray, 0, newPath, 0, urlArray.length);
		newPath[urlArray.length] = url;
		Hashtable[] newIndexes = new Hashtable[indexes.length + 1];
		System.arraycopy(indexes, 0, newIndexes, 0, indexes.length);
		indexes = newIndexes;
		return newPath;
	}

	/**
	 * Answers an enumeration of URLs that contain the specified resource.
	 * 
	 * @return Enumeration the enumeration of URLs that contain the specified
	 *         resource.
	 * @param name
	 *            java.lang.String the name of the requested resource
	 * @exception java.io.IOException
	 *                thrown if an IO Exception occurs while attempting to
	 *                connect
	 */
	public Enumeration findResources(final String name) throws IOException {
		if (name == null)
			return null;
		Vector result = (Vector) AccessController.doPrivileged(
				new PrivilegedAction() {
					public Object run() {
						return findResources(urls, name, new Vector());
					}
				}, currentContext);
		SecurityManager sm;
		int length = result.size();
		if (length > 0 && (sm = System.getSecurityManager()) != null) {
			Vector reduced = new Vector(length);
			for (int i = 0; i < length; i++) {
				URL url = (URL) result.elementAt(i);
				try {
					sm.checkPermission(url.openConnection().getPermission());
					reduced.addElement(url);
				} catch (IOException e) {
				} catch (SecurityException e) {
				}
			}
			result = reduced;
		}
		return result.elements();
	}

	/**
	 * Answers a Vector of URLs among the given ones that contain the specified
	 * resource.
	 * 
	 * @return Vector the enumeration of URLs that contain the specified
	 *         resource.
	 * @param searchURLs
	 *            java.net.URL[] the array to be searched
	 * @param name
	 *            java.lang.String the name of the requested resource
	 */
	Vector findResources(URL[] searchURLs, String name, Vector result) {
		boolean findInExtensions = searchURLs == urls;
		for (int i = 0; i < searchURLs.length; i++) {
			if (searchURLs[i] != null) {
				URL[] search = new URL[] { searchURLs[i] };
				URL res = findResourceImpl(search, name);
				if (search[0] == null)
					searchURLs[i] = null;
				else {
					if (res != null && !result.contains(res))
						result.addElement(res);
					if (findInExtensions) {
						findInExtensions(explore(searchURLs[i], i), name, i,
								result, false);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Answers an Object[] containing a Class, a URL, and a Vector of URLs, 2 of
	 * which are null, according to the caller, which is identified by the int
	 * type.
	 * 
	 * @return Object[] a 3-element array : {Class, URL, Vector}. The non-null
	 *         element contains the resource(s) found, which are searched in in
	 *         indexes[i].
	 * @param i
	 *            int the index of 'indexes' array to use.
	 * @param name
	 *            String the resource to look for : either a resource or a
	 *            class.
	 * @param resources
	 *            boolean indicates that a Vector of URL should be returned as
	 *            the non-null element in Object[].
	 * @param url
	 *            boolean if true a URL should be returned as the non null
	 *            element, if false a Class should be returned.
	 */
	Object findInIndex(int i, String name, Vector resources, boolean url) {
		Hashtable index = indexes[i];
		if (index != null) {
			int pos = name.lastIndexOf("/");
			// only keep the directory part of the resource
			// as index.list only keeps track of directories and root files
			String indexedName = (pos > 0) ? name.substring(0, pos) : name;
			URL[] jarURLs;
			if (resources != null) {
				jarURLs = (URL[]) index.get(indexedName);
				if (jarURLs != null)
					findResources(jarURLs, name, resources);
			} else if (url) {
				jarURLs = (URL[]) index.get(indexedName);
				if (jarURLs != null) {
					return findResourceImpl(jarURLs, name);
				}
			} else {
				String clsName = name;
				String partialName = clsName.replace('.', '/');
				int position;
				if ((position = partialName.lastIndexOf('/')) != -1) {
					String packageName = partialName.substring(0, position);
					jarURLs = (URL[]) index.get(packageName);
				} else {
					String className = partialName.substring(0, partialName
							.length())
							+ ".class";
					jarURLs = (URL[]) index.get(className);
				}
				if (jarURLs != null) {
					Class c = findClassImpl(jarURLs, clsName);
					// InvalidJarException is thrown when a mapping for a class
					// is not valid, ie we cant find the class by following the
					// mapping.
					if (c == null) {
						throw new InvalidJarIndexException();
					}
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Answers an Object[] containing a Class, a URL, and a Vector of URLs, 2 of
	 * which are null, according to the caller, which is identified by the int
	 * type.
	 * 
	 * @return Object[] a 3-element array : {Class, URL, Vector}. The non-null
	 *         element contains the resource(s) found, which are searched in
	 *         newExtensions.
	 * @param newExtensions
	 *            URL[] the URLs to look in for.
	 * @param name
	 *            String the ressource to look for : either a resource or a
	 *            class.
	 * @param i
	 *            int the index of 'indexes' array to use.
	 * @param resources
	 *            boolean indicates that a Vector of URL should be returned as
	 *            the non-null element in Object[].
	 * @param url
	 *            boolean if true a URL should be returned as the non null
	 *            element, if false a Class should be returned.
	 */
	Object findInExtensions(URL[] newExtensions, String name, int i,
			Vector resources, boolean url) {
		if (newExtensions != null) {
			for (int k = 0; k < newExtensions.length; k++) {
				if (newExtensions[k] != null) {
					URL[] search = new URL[] { newExtensions[k] };
					if (resources != null) {
						URL res = findResourceImpl(search, name);
						if (search[0] == null) { // the URL does not exist
							newExtensions[k] = null;
						} else {
							if (res != null && !resources.contains(res))
								resources.addElement(res);
							findInExtensions(explore(newExtensions[k], i),
									name, i, resources, url);
						}
					} else {
						Object result;
						if (url)
							result = findResourceImpl(search, name);
						else
							result = findClassImpl(search, name);
						if (result != null)
							return result;
						if (search[0] == null) { // the URL does not exist
							newExtensions[k] = null;
						} else {
							result = findInExtensions(explore(newExtensions[k],
									i), name, i, resources, url);
							if (result != null)
								return result;
						}
					}
				}
			}
		} else {
			try {
				return findInIndex(i, name, resources, url);
			} catch (InvalidJarIndexException ex) {
				// Ignore misleading/wrong jar index
				return null;
			}
		}
		return null;
	}

	/**
	 * Converts an input stream into a byte array.
	 * 
	 * @return byte[] the byte array
	 * @param is
	 *            java.io.InputStream the input stream
	 */
	private static byte[] getBytes(InputStream is, boolean readAvailable)
			throws IOException {
		if (readAvailable) {
			byte[] buf = new byte[is.available()];
			is.read(buf, 0, buf.length);
			is.close();
			return buf;
		}
		byte[] buf = new byte[4096];
		int size = is.available();
		if (size < 1024)
			size = 1024;
		ByteArrayOutputStream bos = new ByteArrayOutputStream(size);
		int count;
		while ((count = is.read(buf)) > 0)
			bos.write(buf, 0, count);
		return bos.toByteArray();
	}

	/**
	 * Answers the permissions for the given code source. First this method
	 * retrieves the permissions from the system policy. If the protocol is
	 * "file:/" then a new permission, <code>FilePermission</code>, granting
	 * the read permission to the file is added to the permission collection.
	 * Otherwise, connecting to and accepting connections from the URL is
	 * granted.
	 * 
	 * @return PermissionCollection
	 * @param codesource
	 *            CodeSource
	 */
	protected PermissionCollection getPermissions(final CodeSource codesource) {
		PermissionCollection pc = super.getPermissions(codesource);
		URL u = codesource.getLocation();
		if (u.getProtocol().equals("jar")) {
			try {
				// Create a URL for the resource the jar refers to
				u = ((JarURLConnection) u.openConnection()).getJarFileURL();
			} catch (IOException e) {
				// This should never occur. If it does continue using the jar
				// URL
			}
		}
		if (u.getProtocol().equals("file")) {
			String path = u.getFile();
			String host = u.getHost();
			if (host != null && host.length() > 0)
				path = "//" + host + path;

			if (File.separatorChar != '/')
				path = path.replace('/', File.separatorChar);
			if (isDirectory(u))
				pc.add(new FilePermission(path + "-", "read"));
			else
				pc.add(new FilePermission(path, "read"));
		} else {
			String host = u.getHost();
			if (host.length() == 0)
				host = "localhost";
			pc.add(new SocketPermission(host, "connect, accept"));
		}
		return pc;
	}

	/**
	 * Answers the search list of this URLClassLoader
	 * 
	 * @return java.net.URL[]
	 */
	public URL[] getURLs() {
		return orgUrls;
	}

	/**
	 * Determines if the URL is pointing to a directory.
	 * 
	 * @return boolean
	 * @param url
	 *            java.net.URL
	 */
	private static boolean isDirectory(URL url) {
		String file = url.getFile();
		return (file.length() > 0 && file.charAt(file.length() - 1) == '/');
	}

	/**
	 * Answers an instance of <code>URLClassLoader</code>.
	 * <code>loadClass()</code> of the new instance will call the
	 * SecurityManager's <code>checkPackageAccess()</code> before loading a
	 * class.
	 * 
	 * @return java.net.URLClassLoader the new instance of
	 *         <code>URLClassLoader</code>
	 * @param urls
	 *            java.net.URL[] a list of URLs that is passed to the new
	 *            URLClassloader
	 */
	public static URLClassLoader newInstance(final URL[] urls) {
		URLClassLoader sub = (URLClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return new SubURLClassLoader(urls);
					}
				});
		sub.currentContext = AccessController.getContext();
		return sub;
	}

	/**
	 * Answers an instance of <code>URLClassLoader</code>.
	 * <code>loadClass()</code> of the new instance will call security
	 * manager's <code>checkPackageAccess()</code> before loading a class.
	 * 
	 * @return URLClassLoader the new instance of <code>URLClassLoader</code>
	 * 
	 * @param urls
	 *            URL[] the list of URLs that is passed to the new
	 *            <code>URLClassloader</code>
	 * @param parentCl
	 *            ClassLoader the parent class loader that is passed to the new
	 *            <code>URLClassloader</code>
	 */
	public static URLClassLoader newInstance(final URL[] urls,
			final ClassLoader parentCl) {
		URLClassLoader sub = (URLClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return new SubURLClassLoader(urls, parentCl);
					}
				});
		sub.currentContext = AccessController.getContext();
		return sub;
	}

	/**
	 * Constructs a new instance of this class. The newly created instance will
	 * have the specified ClassLoader as its parent and use the specified
	 * factory to create stream handlers. URLs that end with "/" are assumed to
	 * be directories, otherwise they are assumed to be Jar files.
	 * 
	 * @param searchUrls
	 *            java.net.URL[] the URLs that will be searched in the order
	 *            they were specified for resource
	 * 
	 * @param parent
	 *            ClassLoader the ClassLoader name of the resource to find.
	 * 
	 * @param factory
	 *            java.net.URLStreamHandlerFactory the factory that will used to
	 *            create stream (protocol) handlers
	 * @exception SecurityException
	 *                if a security manager exists and its
	 *                checkCreateClassLoader method doesn't allow creation of
	 *                new ClassLoaders
	 */
	public URLClassLoader(URL[] searchUrls, ClassLoader parent,
			URLStreamHandlerFactory factory) {
		super(parent);
		// Required for pre-v1.2 security managers to work
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkCreateClassLoader();
		this.factory = factory;
		// capture the context of the thread that creates this URLClassLoader
		currentContext = AccessController.getContext();
		int nbUrls = searchUrls.length;
		urls = new URL[nbUrls];
		orgUrls = new URL[nbUrls];
		// Search each jar for CLASS-PATH attribute in manifest
		extensions = new HashMap(nbUrls * 2);
		for (int i = 0; i < nbUrls; i++) {
			try {
				urls[i] = createSearchURL(searchUrls[i]);
				extensions.put(urls[i], null);
			} catch (MalformedURLException e) {
			}
			orgUrls[i] = searchUrls[i];
		}
		// Search each jar for META-INF/INDEX.LIST
		indexes = new Hashtable[nbUrls];
	}

	/**
	 * Locates and loads the specified class, searching this URLClassLoader's
	 * list of URLS.
	 * 
	 * @return Class the class that has been loaded.
	 * 
	 * @param clsName
	 *            String the name of the class.
	 * 
	 * @exception java.lang.ClassNotFoundException
	 *                if the class cannot be loaded
	 */
	protected Class findClass(final String clsName)
			throws ClassNotFoundException {
		Class cls = (Class) AccessController.doPrivileged(
				new PrivilegedAction() {
					public Object run() {
						return findClassImpl(urls, clsName);
					}
				}, currentContext);
		if (cls != null)
			return cls;
		throw new ClassNotFoundException(clsName);
	}

	/**
	 * Answers an URL that will be checked if it contains the class or resource.
	 * If the file component of the URL is not a directory, a Jar URL will be
	 * created.
	 * 
	 * @return java.net.URL a test URL
	 * 
	 */
	private URL createSearchURL(URL url) throws MalformedURLException {
		if (url == null) {
			return url;
		}
		
		String protocol = url.getProtocol();

		if (isDirectory(url) || protocol.equals("jar")) {
			return url;
		}
		if (factory == null) {
			return new URL("jar", "", -1, url.toString() + "!/");
		}
		return new URL("jar", "", -1, url.toString() + "!/", factory
				.createURLStreamHandler(protocol));
	}

	/**
	 * Answers a URL referencing the specified resource or null if no resource
	 * could be found.
	 * 
	 * @return URL URL for the resource.
	 * @param name
	 *            java.lang.String the name of the requested resource
	 */
	public URL findResource(final String name) {
		if (name == null)
			return null;
		URL result = (URL) AccessController.doPrivileged(
				new PrivilegedAction() {
					public Object run() {
						return findResourceImpl(urls, name);
					}
				}, currentContext);
		SecurityManager sm;
		if (result != null && (sm = System.getSecurityManager()) != null) {
			try {
				sm.checkPermission(result.openConnection().getPermission());
			} catch (IOException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}
		}
		return result;
	}

	/**
	 * Answers a URL among the given ones referencing the specified resource or
	 * null if no resource could be found.
	 * 
	 * @return URL URL for the resource.
	 * @param searchList
	 *            java.net.URL[] the array to be searched
	 * @param resName
	 *            java.lang.String the name of the requested resource
	 */
	URL findResourceImpl(URL[] searchList, String resName) {
		boolean findInExtensions = searchList == urls;
		int i = 0;
		while (i < searchList.length) {
			if (searchList[i] != null) {
				JarFile jf = null;
				try {
					URL currentUrl = searchList[i];
					String protocol = currentUrl.getProtocol();
					if (protocol.equals("jar")) {
						jf = (JarFile) resCache.get(currentUrl);
						if (jf == null) {
							// If the connection for currentUrl or resURL is
							// used,
							// getJarFile() will throw an exception if the entry
							// doesn't exist.
							URL jarURL = ((JarURLConnection) currentUrl
									.openConnection()).getJarFileURL();
							try {
								JarURLConnection juc = (JarURLConnection) new URL(
										"jar", "", jarURL.toExternalForm()
												+ "!/").openConnection();
								jf = juc.getJarFile();
								resCache.put(currentUrl, jf);
							} catch (IOException e) {
								// Don't look for this jar file again
								searchList[i] = null;
								throw e;
							}
						}
						String entryName;
						if (currentUrl.getFile().endsWith("!/"))
							entryName = resName;
						else {
							String file = currentUrl.getFile();
							int sepIdx = file.lastIndexOf("!/");
							if (sepIdx == -1) {
								// Invalid URL, don't look here again
								searchList[i] = null;
								continue;
							}
							sepIdx += 2;
							entryName = new StringBuffer(file.length() - sepIdx
									+ resName.length()).append(
									file.substring(sepIdx)).append(resName)
									.toString();
						}
						if (jf.getEntry(entryName) != null)
							return targetURL(currentUrl, resName);
					} else if (protocol.equals("file")) {
						String baseFile = currentUrl.getFile();
						String host = currentUrl.getHost();
						int hostLength = 0;
						if (host != null) {
							hostLength = host.length();
						}
						StringBuffer buf = new StringBuffer(2 + hostLength
								+ baseFile.length() + resName.length());
						if (hostLength > 0) {
							buf.append("//").append(host);
						}
						// baseFile always ends with '/'
						buf.append(baseFile);
						String fixedResName = resName;
						// Do not create a UNC path, i.e. \\host
						while (fixedResName.startsWith("/")
								|| fixedResName.startsWith("\\")) {
							fixedResName = fixedResName.substring(1);
						}
						buf.append(fixedResName);
						String filename = buf.toString();
						if (new File(filename).exists()) {
							return targetURL(currentUrl, fixedResName);
						}
					} else {
						URL resURL = targetURL(currentUrl, resName);
						URLConnection uc = resURL.openConnection();
						try {
							uc.getInputStream().close();
						} catch (SecurityException e) {
							return null;
						}
						// HTTP can return a stream on a non-existent file
						// So check for the return code;
						if (!resURL.getProtocol().equals("http"))
							return resURL;
						int code;
						if ((code = ((HttpURLConnection) uc).getResponseCode()) >= 200
								&& code < 300)
							return resURL;
					}
				} catch (MalformedURLException e) {
					// Keep iterating through the URL list
				} catch (IOException e) {
				} catch (SecurityException e) {
				}
				if ((jf != null) && findInExtensions) {
					if (indexes[i] != null) {
						try {
							URL result = (URL) findInIndex(i, resName, null,
									true);
							if (result != null) {
								return result;
							}
						} catch (InvalidJarIndexException ex) {
							// Ignore invalid/misleading JAR index file
						}
					} else {
						URL result = (URL) findInExtensions(explore(
								searchList[i], i), resName, i, null, true);
						if (result != null)
							return result;
					}
				}
			}
			++i;
		}
		return null;
	}

	/**
	 * Define a new Package using information extracted from the specified
	 * Manifest.
	 * 
	 * @param packageName
	 *            The name of the package
	 * @param manifest
	 *            The Manifest for the Package
	 * @param url
	 *            The code source for the Package
	 * @return The Package created
	 * 
	 * @exception IllegalArgumentException
	 *                if the Package already exists
	 */
	protected Package definePackage(String packageName, Manifest manifest,
			URL url) throws IllegalArgumentException {
		Attributes mainAttributes = manifest.getMainAttributes();
		String dirName = packageName.replace('.', '/') + "/";
		Attributes packageAttributes = manifest.getAttributes(dirName);
		boolean noEntry = false;
		if (packageAttributes == null) {
			noEntry = true;
			packageAttributes = mainAttributes;
		}
		String specificationTitle = packageAttributes
				.getValue(Attributes.Name.SPECIFICATION_TITLE);
		if (specificationTitle == null && !noEntry)
			specificationTitle = mainAttributes
					.getValue(Attributes.Name.SPECIFICATION_TITLE);
		String specificationVersion = packageAttributes
				.getValue(Attributes.Name.SPECIFICATION_VERSION);
		if (specificationVersion == null && !noEntry)
			specificationVersion = mainAttributes
					.getValue(Attributes.Name.SPECIFICATION_VERSION);
		String specificationVendor = packageAttributes
				.getValue(Attributes.Name.SPECIFICATION_VENDOR);
		if (specificationVendor == null && !noEntry)
			specificationVendor = mainAttributes
					.getValue(Attributes.Name.SPECIFICATION_VENDOR);
		String implementationTitle = packageAttributes
				.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
		if (implementationTitle == null && !noEntry)
			implementationTitle = mainAttributes
					.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
		String implementationVersion = packageAttributes
				.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
		if (implementationVersion == null && !noEntry)
			implementationVersion = mainAttributes
					.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
		String implementationVendor = packageAttributes
				.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
		if (implementationVendor == null && !noEntry)
			implementationVendor = mainAttributes
					.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);

		return definePackage(packageName, specificationTitle,
				specificationVersion, specificationVendor, implementationTitle,
				implementationVersion, implementationVendor, isSealed(manifest,
						dirName) ? url : null);
	}

	private boolean isSealed(Manifest manifest, String dirName) {
		Attributes mainAttributes = manifest.getMainAttributes();
		String value = mainAttributes.getValue(Attributes.Name.SEALED);
		boolean sealed = value != null && value.toLowerCase().equals("true");
		Attributes attributes = manifest.getAttributes(dirName);
		if (attributes != null) {
			value = attributes.getValue(Attributes.Name.SEALED);
			if (value != null)
				sealed = value.toLowerCase().equals("true");
		}
		return sealed;
	}

	/**
	 * returns URLs referenced in the string classpath.
	 * 
	 * @param root
	 *            the jar URL that classpath is related to
	 * @param classpath
	 *            the relative URLs separated by spaces
	 * 
	 * @return URL[] the URLs contained in the string classpath.
	 */
	private URL[] getInternalURLs(URL root, String classpath) {
		// Class-path attribute is composed of space-separated values.
		StringTokenizer tokenizer = new java.util.StringTokenizer(classpath);
		Vector addedURLs = new Vector();
		String file = root.getFile();
		file = file.substring(0, file.lastIndexOf("/",
				file.lastIndexOf("!/") - 1) + 1);
		String protocol = root.getProtocol();
		String host = root.getHost();
		int port = root.getPort();
		while (tokenizer.hasMoreElements()) {
			String element = tokenizer.nextToken();
			if (!element.equals("")) {
				try {
					URL newURL = new URL(protocol, host, port, file + element
							+ "!/");
					if (!extensions.containsKey(newURL)) {
						extensions.put(newURL, null);
						addedURLs.add(newURL);
					}
				} catch (MalformedURLException e) {
					// Nothing is added
				}
			}
		}
		URL[] newURLs = (URL[]) addedURLs.toArray(new URL[] {});
		return newURLs;
	}

	/*
	 * @param in java.io.InputStream the stream to read lines from @return
	 * ArrayList a list of String lines
	 */
	private ArrayList readLines(InputStream in) throws IOException {
		byte[] buff = new byte[144];
		ArrayList lines = new ArrayList();
		int pos = 0;
		int next;
		while ((next = in.read()) != -1) {
			if (next == '\n') {
				lines.add(new String(buff, 0, pos, "UTF8"));
				pos = 0;
				continue;
			}
			if (next == '\r') {
				lines.add(new String(buff, 0, pos, "UTF8"));
				pos = 0;
				if ((next = in.read()) == '\n')
					continue;
			}
			if (pos == buff.length) {
				byte[] newBuf = new byte[buff.length * 2];
				System.arraycopy(buff, 0, newBuf, 0, buff.length);
				buff = newBuf;
			}
			buff[pos++] = (byte) next;
		}
		if (pos > 0)
			lines.add(new String(buff, 0, pos, "UTF8"));
		return lines;
	}

	private URL targetURL(URL base, String name) throws MalformedURLException {
		String file = new StringBuffer(base.getFile().length() + name.length())
				.append(base.getFile()).append(name).toString();
		return new URL(base.getProtocol(), base.getHost(), base.getPort(),
				file, null);
	}

	/*
	 * @param searchURLs java.net.URL[] the URLs to search in @param clsName
	 * java.lang.String the class name to be found @return Class the class found
	 * or null if not found
	 */
	Class findClassImpl(URL[] searchURLs, String clsName) {
		boolean readAvailable = false;
		boolean findInExtensions = searchURLs == urls;
		final String name = new StringBuffer(clsName.replace('.', '/')).append(
				".class").toString();
		for (int i = 0; i < searchURLs.length; i++) {
			if (searchURLs[i] != null) {
				Manifest manifest = null;
				InputStream is = null;
				JarEntry entry = null;
				JarFile jf = null;
				byte[] clBuf = null;
				try {
					URL thisURL = searchURLs[i];
					String protocol = thisURL.getProtocol();
					if (protocol.equals("jar")) {
						jf = (JarFile) resCache.get(thisURL);
						if (jf == null) {
							// If the connection for testURL or thisURL is used,
							// getJarFile() will throw an exception if the entry
							// doesn't exist.
							URL jarURL = ((JarURLConnection) thisURL
									.openConnection()).getJarFileURL();
							try {
								JarURLConnection juc = (JarURLConnection) new URL(
										"jar", "", jarURL.toExternalForm()
												+ "!/").openConnection();
								jf = juc.getJarFile();
								resCache.put(thisURL, jf);
							} catch (IOException e) {
								// Don't look for this jar file again
								searchURLs[i] = null;
								throw e;
							}
						}
						if (thisURL.getFile().endsWith("!/")) {
							entry = jf.getJarEntry(name);
						} else {
							String file = thisURL.getFile();
							int sepIdx = file.lastIndexOf("!/");
							if (sepIdx == -1) {
								// Invalid URL, don't look here again
								searchURLs[i] = null;
								continue;
							}
							sepIdx += 2;
							String entryName = new StringBuffer(file.length()
									- sepIdx + name.length()).append(
									file.substring(sepIdx)).append(name)
									.toString();
							entry = jf.getJarEntry(entryName);
						}
						if (entry != null) {
							readAvailable = true;
							is = jf.getInputStream(entry);
							manifest = jf.getManifest();
						}
					} else if (protocol.equals("file")) {
						String filename = thisURL.getFile();
						String host = thisURL.getHost();
						if (host != null && host.length() > 0) {
							filename = new StringBuffer(host.length()
									+ filename.length() + name.length() + 2)
									.append("//").append(host).append(filename)
									.append(name).toString();
						} else {
							filename = new StringBuffer(filename.length()
									+ name.length()).append(filename).append(
									name).toString();
						}
						File file = new File(filename);
						// Don't throw exceptions for speed
						if (file.exists()) {
							is = new FileInputStream(file);
							readAvailable = true;
						} else
							continue;
					} else {
						is = targetURL(thisURL, name).openStream();
					}
				} catch (MalformedURLException e) {
					// Keep iterating through the URL list
				} catch (IOException e) {
				}
				if (is != null) {
					URL codeSourceURL = null;
					Certificate[] certificates = null;
					CodeSource codeS = null;
					try {
						codeSourceURL = findInExtensions ? orgUrls[i]
								: ((JarURLConnection) searchURLs[i]
										.openConnection()).getJarFileURL();
					} catch (IOException e) {
						codeSourceURL = searchURLs[i];
					}
					if (is != null) {
						try {
							clBuf = getBytes(is, readAvailable);
							is.close();
						} catch (IOException e) {
							return null;
						}
					}
					if (entry != null)
						certificates = entry.getCertificates();
					// Use the original URL, not the possible jar URL
					codeS = new CodeSource(codeSourceURL, certificates);
					int dotIndex = clsName.lastIndexOf(".");
					if (dotIndex != -1) {
						String packageName = clsName.substring(0, dotIndex);
						synchronized (this) {
							Package packageObj = getPackage(packageName);
							if (packageObj == null) {
								if (manifest != null)
									definePackage(packageName, manifest,
											codeSourceURL);
								else
									definePackage(packageName, null, null,
											null, null, null, null, null);
							} else {
								boolean exception = false;
								if (manifest != null) {
									String dirName = packageName.replace('.',
											'/')
											+ "/";
									if (isSealed(manifest, dirName))
										exception = !packageObj
												.isSealed(codeSourceURL);
								} else
									exception = packageObj.isSealed();
								if (exception)
									throw new SecurityException(
											com.ibm.oti.util.Msg
													.getString("K004c"));
							}
						}
					}
					return defineClass(clsName, clBuf, 0, clBuf.length, codeS);
				}
				if ((jf != null) && findInExtensions) {
					if (indexes[i] != null) {
						try {
							Class c = (Class) findInIndex(i, clsName, null,
									false);
							if (c != null) {
								return c;
							}
						} catch (InvalidJarIndexException ex) {
							// Ignore misleading/wrong jar index
						}
					} else {
						Class c = (Class) findInExtensions(explore(
								searchURLs[i], i), clsName, i, null, false);
						if (c != null)
							return c;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param url
	 *            URL the URL to explore
	 * @param indexNumber
	 *            int the index in extensions to consider
	 * 
	 * @return URL[] the URLs of bundled extensions that have been found (ie the
	 *         URL of jar files in the class-path attribute), or null if none.
	 *         if an INDEX.LIST has been found, an zero-lengthed array is
	 *         returned
	 */
	URL[] explore(URL url, int indexNumber) {
		URL[] internal;
		synchronized (extensions) {
			internal = (URL[]) extensions.get(url);
		}
		if (internal != null)
			return internal;
		if (indexes[indexNumber] != null)
			return null;

		if (!url.getProtocol().equals("jar"))
			return null;

		JarFile jf = (JarFile) resCache.get(url);
		// Add mappings from INDEX.LIST
		ZipEntry ze = jf.getEntry("META-INF/INDEX.LIST");
		if (ze != null) {
			if (url.equals(urls[indexNumber])) {
				try {
					Hashtable index = new Hashtable(15);
					InputStream indexIS = jf.getInputStream(ze);
					List lines = readLines(indexIS);
					indexIS.close();
					ListIterator iterator = lines.listIterator();
					// Ignore the 2 first lines (index version)
					iterator.next();
					iterator.next();
					// Add mappings from resource to jar file
					URL fileURL = ((JarURLConnection) url.openConnection())
							.getJarFileURL();
					String file = fileURL.getFile();
					String parentFile = new File(file).getParent();
					parentFile = parentFile.replace(File.separatorChar, '/');
					if (parentFile.charAt(0) != '/')
						parentFile = "/" + parentFile;
					URL parentURL = new URL(fileURL.getProtocol(), fileURL
							.getHost(), fileURL.getPort(), parentFile);
					while (iterator.hasNext()) {
						URL jar = new URL("jar:" + parentURL.toExternalForm()
								+ "/" + (String) iterator.next() + "!/");
						String resource = null;
						while (iterator.hasNext()
								&& !(resource = (String) iterator.next())
										.equals("")) {
							if (index.containsKey(resource)) {
								URL[] jars = (URL[]) index.get(resource);
								URL[] newJars = new URL[jars.length + 1];
								System.arraycopy(jars, 0, newJars, 0,
										jars.length);
								newJars[jars.length] = jar;
								index.put(resource, newJars);
							} else {
								URL[] jars = { jar };
								index.put(resource, jars);
							}
						}
					}
					indexes[indexNumber] = index;
				} catch (MalformedURLException e) {
					// Ignore this jar's index
				} catch (IOException e) {
					// Ignore this jar's index
				}
			}
			return null;
		}

		// Returns URLs referenced by the class-path attribute.
		Manifest manifest = null;
		try {
			manifest = jf.getManifest();
		} catch (IOException e) {
		}
		String classpath = null;
		if (manifest != null)
			classpath = manifest.getMainAttributes().getValue(
					Attributes.Name.CLASS_PATH);
		synchronized (extensions) {
			internal = (URL[]) extensions.get(url);
			if (internal == null) {
				internal = classpath != null ? getInternalURLs(url, classpath)
						: NO_PATH;
				extensions.put(url, internal);
			}
		}
		return internal;
	}
}
