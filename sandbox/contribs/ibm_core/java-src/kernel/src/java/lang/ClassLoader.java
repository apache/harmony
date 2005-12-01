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

package java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;


/**
 * This class must be implemented by the vm vendor. The documented methods and
 * natives must be implemented to support other provided class implementations
 * in this package. ClassLoaders are used to dynamically load, link and install
 * classes into a running image.
 * 
 */
public abstract class ClassLoader {

	static ClassLoader systemClassLoader;

	static final void initializeClassLoaders() {
		return;
	}

	/**
	 * Constructs a new instance of this class with the system class loader as
	 * its parent.
	 * 
	 * @exception SecurityException
	 *                if a security manager exists and it does not allow the
	 *                creation of new ClassLoaders.
	 */
	protected ClassLoader() {
		super();
	}

	/**
	 * Constructs a new instance of this class with the given class loader as
	 * its parent.
	 * 
	 * @param parentLoader
	 *            ClassLoader the ClassLoader to use as the new class loaders
	 *            parent.
	 * @exception SecurityException
	 *                if a security manager exists and it does not allow the
	 *                creation of new ClassLoaders.
	 * @exception NullPointerException
	 *                if the parent is null.
	 */
	protected ClassLoader(ClassLoader parentLoader) {
		super();
	}

	/**
	 * Constructs a new class from an array of bytes containing a class
	 * definition in class file format.
	 * 
	 * @param classRep
	 *            byte[] a memory image of a class file.
	 * @param offset
	 *            int the offset into the classRep.
	 * @param length
	 *            int the length of the class file.
	 * @deprecated Use defineClass(String, byte[], int, int)
	 */
	protected final Class defineClass(byte[] classRep, int offset, int length)
			throws ClassFormatError {
		return null;
	}

	/**
	 * Constructs a new class from an array of bytes containing a class
	 * definition in class file format.
	 * 
	 * @param className
	 *            java.lang.String the name of the new class
	 * @param classRep
	 *            byte[] a memory image of a class file
	 * @param offset
	 *            int the offset into the classRep
	 * @param length
	 *            int the length of the class file
	 */
	protected final Class defineClass(String className, byte[] classRep,
			int offset, int length) throws ClassFormatError {
		return null;
	}

	/**
	 * Constructs a new class from an array of bytes containing a class
	 * definition in class file format and assigns the new class to the
	 * specified protection domain.
	 * 
	 * @param className
	 *            java.lang.String the name of the new class.
	 * @param classRep
	 *            byte[] a memory image of a class file.
	 * @param offset
	 *            int the offset into the classRep.
	 * @param length
	 *            int the length of the class file.
	 * @param protectionDomain
	 *            ProtectionDomain the protection domain this class should
	 *            belongs to.
	 */
	protected final Class defineClass(String className, byte[] classRep,
			int offset, int length, ProtectionDomain protectionDomain)
			throws java.lang.ClassFormatError {
		return null;
	}

	/**
	 * Overridden by subclasses, by default throws ClassNotFoundException. This
	 * method is called by loadClass() after the parent ClassLoader has failed
	 * to find a loaded class of the same name.
	 * 
	 * @return java.lang.Class the class or null.
	 * @param className
	 *            String the name of the class to search for.
	 * @exception ClassNotFoundException
	 *                always, unless overridden.
	 */
	protected Class findClass(String className) throws ClassNotFoundException {
		return null;
	}

	/**
	 * Attempts to find and return a class which has already been loaded by the
	 * virtual machine. Note that the class may not have been linked and the
	 * caller should call resolveClass() on the result if necessary.
	 * 
	 * @return java.lang.Class the class or null.
	 * @param className
	 *            String the name of the class to search for.
	 */
	protected final Class findLoadedClass(String className) {
		return null;
	};

	/**
	 * Attempts to load a class using the system class loader. Note that the
	 * class has already been been linked.
	 * 
	 * @return java.lang.Class the class which was loaded.
	 * @param className
	 *            String the name of the class to search for.
	 * @exception ClassNotFoundException
	 *                if the class can not be found.
	 */
	protected final Class findSystemClass(String className)
			throws ClassNotFoundException {
		return null;
	}

	/**
	 * Returns the specified ClassLoader's parent.
	 * 
	 * @return java.lang.ClassLoader the class or null.
	 * @exception SecurityException
	 *                if a security manager exists and it does not allow the
	 *                parent loader to be retrieved.
	 */
	public final ClassLoader getParent() {
		return null;
	}

	/**
	 * Answers an URL which can be used to access the resource described by
	 * resName, using the class loader's resource lookup algorithm. The default
	 * behavior is just to return null.
	 * 
	 * @return URL the location of the resource.
	 * @param resName
	 *            String the name of the resource to find.
	 * @see Class#getResource
	 */
	public URL getResource(String resName) {
		return null;
	}

	/**
	 * Answers an Enumeration of URL which can be used to access the resources
	 * described by resName, using the class loader's resource lookup algorithm.
	 * The default behavior is just to return an empty Enumeration.
	 * 
	 * @return Enumeration the locations of the resources.
	 * @param resName
	 *            String the name of the resource to find.
	 */
	public final Enumeration getResources(String resName) throws IOException {
		return null;
	}

	/**
	 * Answers a stream on a resource found by looking up resName using the
	 * class loader's resource lookup algorithm. The default behavior is just to
	 * return null.
	 * 
	 * @return InputStream a stream on the resource or null.
	 * @param resName
	 *            String the name of the resource to find.
	 * @see Class#getResourceAsStream
	 */
	public InputStream getResourceAsStream(String resName) {
		return null;
	}

	/**
	 * Returns the system class loader. This is the parent for new ClassLoader
	 * instances, and is typically the class loader used to start the
	 * application. If a security manager is present, and the caller's class
	 * loader is not null and the caller's class loader is not the same as or an
	 * ancestor of the system class loader, then this method calls the security
	 * manager's checkPermission method with a
	 * RuntimePermission("getClassLoader") permission to ensure it's ok to
	 * access the system class loader. If not, a SecurityException will be
	 * thrown.
	 * 
	 * @return java.lang.ClassLoader the system classLoader.
	 * @exception SecurityException
	 *                if a security manager exists and it does not allow access
	 *                to the system class loader.
	 */
	public static ClassLoader getSystemClassLoader() {
		return null;
	}

	/**
	 * Answers an URL specifing a resource which can be found by looking up
	 * resName using the system class loader's resource lookup algorithm.
	 * 
	 * @return URL a URL specifying a system resource or null.
	 * @param resName
	 *            String the name of the resource to find.
	 * @see Class#getResource
	 */
	public static URL getSystemResource(String resName) {
		return null;
	}

	/**
	 * Answers an Emuneration of URL containing all resources which can be found
	 * by looking up resName using the system class loader's resource lookup
	 * algorithm.
	 * 
	 * @return Enumeration an Enumeration of URL containing the system resources
	 * @param resName
	 *            String the name of the resource to find.
	 */
	public static Enumeration getSystemResources(String resName)
			throws IOException {
		return null;
	}

	/**
	 * Answers a stream on a resource found by looking up resName using the
	 * system class loader's resource lookup algorithm. Basically, the contents
	 * of the java.class.path are searched in order, looking for a path which
	 * matches the specified resource.
	 * 
	 * @return a stream on the resource or null.
	 * @param resName
	 *            the name of the resource to find.
	 * @see Class#getResourceAsStream
	 */
	public static InputStream getSystemResourceAsStream(String resName) {
		return null;
	}

	/**
	 * Invoked by the Virtual Machine when resolving class references.
	 * Equivalent to loadClass(className, false);
	 * 
	 * @return java.lang.Class the Class object.
	 * @param className
	 *            String the name of the class to search for.
	 * @exception ClassNotFoundException
	 *                If the class could not be found.
	 */
	public Class loadClass(String className) throws ClassNotFoundException {
		return null;
	}

	/**
	 * Loads the class with the specified name, optionally linking the class
	 * after load. Steps are: 1) Call findLoadedClass(className) to determine if
	 * class is loaded 2) Call loadClass(className, resolveClass) on the parent
	 * loader. 3) Call findClass(className) to find the class
	 * 
	 * @return java.lang.Class the Class object.
	 * @param className
	 *            String the name of the class to search for.
	 * @param resolveClass
	 *            boolean indicates if class should be resolved after loading.
	 * @exception ClassNotFoundException
	 *                If the class could not be found.
	 */
	protected Class loadClass(String className, boolean resolveClass)
			throws ClassNotFoundException {
		return null;
	}

	/**
	 * Forces a class to be linked (initialized). If the class has already been
	 * linked this operation has no effect.
	 * 
	 * @param clazz
	 *            Class the Class to link.
	 * @exception NullPointerException
	 *                if clazz is null.
	 * @see Class#getResource
	 */
	protected final void resolveClass(Class clazz) {
		return;
	}

	/**
	 * This method must be provided by the vm vendor, as it is used by other
	 * provided class implementations in this package. A sample implementation
	 * of this method is provided by the reference implementation. This method
	 * is used by SecurityManager.classLoaderDepth(), currentClassLoader() and
	 * currentLoadedClass(). Answers true if the receiver is a system class
	 * loader.
	 * <p>
	 * Note that this method has package visibility only. It is defined here to
	 * avoid the security manager check in getSystemClassLoader, which would be
	 * required to implement this method anywhere else.
	 * 
	 * @return boolean true if the receiver is a system class loader
	 * @see Class#getClassLoaderImpl()
	 */
	final boolean isSystemClassLoader() {
		return false;
	}

	/**
	 * Answers true if the receiver is ancestor of another class loader.
	 * <p>
	 * Note that this method has package visibility only. It is defined here to
	 * avoid the security manager check in getParent, which would be required to
	 * implement this method anywhere else.
	 * 
	 * @param child
	 *            ClassLoader, a child candidate
	 * @return boolean true if the receiver is ancestor of the parameter
	 */
	final boolean isAncestorOf(ClassLoader child) {
		return false;
	}

	/**
	 * Answers an URL which can be used to access the resource
	 * described by resName, using the class loader's resource lookup
	 * algorithm. The default behavior is just to return null.
	 * This should be implemented by a ClassLoader.
	 *
	 * @return		URL
	 *					the location of the resource.
	 * @param		resName String
	 *					the name of the resource to find.
	 */
	protected URL findResource(String resName) {
		return null;
	}

	/**
	 * Answers an Enumeration of URL which can be used to access the resources
	 * described by resName, using the class loader's resource lookup
	 * algorithm. The default behavior is just to return an empty Enumeration.
	 *
	 * @param		resName String
	 *					the name of the resource to find.

	 * @return		Enumeration
	 *					the locations of the resources.
	 *
	 * @throws IOException when an error occurs
	 */
	protected Enumeration findResources(String resName) throws IOException {
		return null;
	}

	/**
	 * Answers the absolute path of the file containing the library associated
	 * with the given name, or null. If null is answered, the system searches
	 * the directories specified by the system property "java.library.path".
	 * 
	 * @return String the library file name or null.
	 * @param libName
	 *            String the name of the library to find.
	 */
	protected String findLibrary(String libName) {
		return null;
	}

	/**
	 * Attempt to locate the requested package. If no package information can be
	 * located, null is returned.
	 * 
	 * @param name
	 *            The name of the package to find
	 * @return The package requested, or null
	 */
	protected Package getPackage(String name) {
		return null;
	}

	/**
	 * Return all the packages known to this class loader.
	 * 
	 * @return All the packages known to this classloader
	 */
	protected Package[] getPackages() {
		return null;
	}

	/**
	 * Define a new Package using the specified information.
	 * 
	 * @param name
	 *            The name of the package
	 * @param specTitle
	 *            The title of the specification for the Package
	 * @param specVersion
	 *            The version of the specification for the Package
	 * @param specVendor
	 *            The vendor of the specification for the Package
	 * @param implTitle
	 *            The implementation title of the Package
	 * @param implVersion
	 *            The implementation version of the Package
	 * @param implVendor
	 *            The specification vendor of the Package
	 * @return The Package created
	 * @exception IllegalArgumentException
	 *                if the Package already exists
	 */
	protected Package definePackage(String name, String specTitle,
			String specVersion, String specVendor, String implTitle,
			String implVersion, String implVendor, URL sealBase)
			throws IllegalArgumentException {
		return null;
	}

	/**
	 * Gets the signers of a class.
	 * 
	 * @param c
	 *            The Class object
	 * @return signers The signers for the class
	 */
	final Object[] getSigners(Class c) {
		return null;
	}

	/**
	 * Sets the signers of a class.
	 * 
	 * @param c
	 *            The Class object
	 * @param signers
	 *            The signers for the class
	 */
	protected final void setSigners(Class c, Object[] signers) {
		return;
	}

	/**
	 * This must be provided by the vm vendor. It is used by
	 * SecurityManager.checkMemberAccess() with depth = 3. Note that
	 * checkMemberAccess() assumes the following stack when called. <code>
	 *		<user code>								<- want this class <br>
	 *		Class.getDeclared*(); <br>
	 *		Class.checkMemberAccess(); <br>
	 *		SecurityManager.checkMemberAccess();	<- current frame <br>
	 * </code> Returns the ClassLoader of the method (including natives) at the
	 * specified depth on the stack of the calling thread. Frames representing
	 * the VM implementation of java.lang.reflect are not included in the list.
	 * Notes:
	 * <ul>
	 * <li>This method operates on the defining classes of methods on stack.
	 * NOT the classes of receivers.</li>
	 * <li>The item at depth zero is the caller of this method</li>
	 * </ul>
	 * 
	 * @param depth
	 *            the stack depth of the requested ClassLoader
	 * @return the ClassLoader at the specified depth
	 */
	static final ClassLoader getStackClassLoader(int depth) {
		return null;
	};

	/**
	 * This method must be included, as it is used by System.load(),
	 * System.loadLibrary(). The reference implementation of this method uses
	 * the getStackClassLoader() method. Returns the ClassLoader of the method
	 * that called the caller. i.e. A.x() calls B.y() calls callerClassLoader(),
	 * A's ClassLoader will be returned. Returns null for the bootstrap
	 * ClassLoader.
	 * 
	 * @return a ClassLoader or null for the bootstrap ClassLoader
	 */
	static ClassLoader callerClassLoader() {
		return null;
	}

	/**
	 * This method must be provided by the vm vendor, as it is called by
	 * java.lang.System.loadLibrary(). System.loadLibrary() cannot call
	 * Runtime.loadLibrary() because this method loads the library using the
	 * ClassLoader of the calling method. Loads and links the library specified
	 * by the argument.
	 * 
	 * @param libName
	 *            the name of the library to load
	 * @param loader
	 *            the classloader in which to load the library
	 * @exception UnsatisfiedLinkError
	 *                if the library could not be loaded
	 * @exception SecurityException
	 *                if the library was not allowed to be loaded
	 */
	static void loadLibraryWithClassLoader(String libName, ClassLoader loader) {
		return;
	}

	/**
	 * This method must be provided by the vm vendor, as it is called by
	 * java.lang.System.load(). System.load() cannot call Runtime.load() because
	 * the library is loaded using the ClassLoader of the calling method. Loads
	 * and links the library specified by the argument. No security check is
	 * done.
	 * 
	 * @param libName
	 *            the name of the library to load
	 * @param loader
	 *            the classloader in which to load the library
	 * @param libraryPath
	 *            the library path to search, or null
	 * @exception UnsatisfiedLinkError
	 *                if the library could not be loaded
	 */
	static void loadLibraryWithPath(String libName, ClassLoader loader,
			String libraryPath) {
		return;
	}

	/**
	 * Sets the assertion status of a class.
	 * 
	 * @param cname
	 *            Class name
	 * @param enable
	 *            Enable or disable assertion
	 */
	public void setClassAssertionStatus(String cname, boolean enable) {
		return;
	}

	/**
	 * Sets the assertion status of a package.
	 * 
	 * @param pname
	 *            Package name
	 * @param enable
	 *            Enable or disable assertion
	 */
	public void setPackageAssertionStatus(String pname, boolean enable) {
		return;
	}

	/**
	 * Sets the default assertion status of a classloader
	 * 
	 * @param enable
	 *            Enable or disable assertion
	 */
	public void setDefaultAssertionStatus(boolean enable) {
		return;
	}

	/**
	 * Clears the default, package and class assertion status of a classloader
	 * 
	 */
	public void clearAssertionStatus() {
		return;
	}

	/**
	 * Answers the assertion status of the named class Returns the assertion
	 * status of the class or nested class if it has been set. Otherwise returns
	 * the assertion status of its package or superpackage if that has been set.
	 * Otherwise returns the default assertion status. Returns 1 for enabled and
	 * 0 for disabled.
	 * 
	 * @return int the assertion status.
	 * @param cname
	 *            String the name of class.
	 */
	boolean getClassAssertionStatus(String cname) {
		return false;
	}

	/**
	 * Answers the assertion status of the named package Returns the assertion
	 * status of the named package or superpackage if that has been set.
	 * Otherwise returns the default assertion status. Returns 1 for enabled and
	 * 0 for disabled.
	 * 
	 * @return int the assertion status.
	 * @param pname
	 *            String the name of package.
	 */
	boolean getPackageAssertionStatus(String pname) {
		return false;
	}

	/**
	 * Answers the default assertion status
	 * 
	 * @return boolean the default assertion status.
	 */
	boolean getDefaultAssertionStatus() {
		return false;
	}
}
