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

package com.ibm.oti.vm;

/**
 * This class must be implemented by the vm vendor. Represents the running
 * virtual machine. All VM specific API are implemented on this class.
 * <p>
 * Note that all methods in VM are static. There is no singleton instance which
 * represents the actively running VM.
 */
public final class VM {

	/**
	 * This method must be provided by the vm vendor, as it is used by
	 * com.ibm.oti.vm.MsgHelp.setLocale() to get the bootstrap ClassLoader.
	 * MsgHelp uses the bootstrap ClassLoader to find the resource bundle of
	 * messages packaged with the bootstrap classes. Returns the ClassLoader of
	 * the method (including natives) at the specified depth on the stack of the
	 * calling thread. Frames representing the VM implementation of
	 * java.lang.reflect are not included in the list. This is not a public
	 * method as it can return the bootstrap class loader, which should not be
	 * accessed by non-bootstrap classes. Notes:
	 * <ul>
	 * <li>This method operates on the defining classes of methods on stack.
	 * NOT the classes of receivers.</li>
	 * <li>The item at depth zero is the caller of this method</li>
	 * </ul>
	 * 
	 * @param depth
	 *            the stack depth of the requested ClassLoader
	 * @return the ClassLoader at the specified depth
	 * @see java.lang.ClassLoader#getStackClassLoader
	 */
	static final ClassLoader getStackClassLoader(int depth) {
		return null;
	};

	/**
	 * This method must be provided by the vm vendor, as it is used by other
	 * provided class implementations. For example,
	 * java.io.ObjectInputStream.readObject() and
	 * java.io.ObjectInputStream.resolveProxyClass(). It is also useful for
	 * other classes, such as java.rmi.server.RMIClassLoader. Walk the stack and
	 * answer the most recent non-null and non-bootstrap ClassLoader on the
	 * stack of the calling thread. If no such ClassLoader is found, null is
	 * returned. Notes: 1) This method operates on the defining classes of
	 * methods on stack. NOT the classes of receivers.
	 * 
	 * @return the first non-bootstrap ClassLoader on the stack
	 */
	static public final ClassLoader getNonBootstrapClassLoader() {
		return null;
	};

	/**
	 * Initialize the classloader.
	 * 
	 * @param loader
	 *            ClassLoader the ClassLoader instance
	 * @param bootLoader
	 *            boolean true for the bootstrap class loader
	 */
	public final static void initializeClassLoader(ClassLoader loader,
			boolean bootLoader) {
		return;
	};

	/**
	 * Native used to find and load a class using the VM
	 * 
	 * @return java.lang.Class the class or null.
	 * @param className
	 *            String the name of the class to search for.
	 * @param classLoader
	 *            the classloader to do the work
	 */
	static Class findClassOrNull(String className, ClassLoader classLoader) {
		return null;
	};

	/**
	 * This method must be included, as it is used by
	 * ResourceBundle.getBundle(), and other places as well. The reference
	 * implementation of this method uses the getStackClassLoader() method.
	 * Returns the ClassLoader of the method that called the caller. i.e. A.x()
	 * calls B.y() calls callerClassLoader(), A's ClassLoader will be returned.
	 * Returns null for the bootstrap ClassLoader.
	 * 
	 * @return a ClassLoader or null for the bootstrap ClassLoader
	 * @throws SecurityException
	 *             when called from a non-bootstrap Class
	 */
	public static ClassLoader callerClassLoader() {
		return null;
	}

	/**
	 * Native used to dump a string to the system console for debugging.
	 * 
	 * @param str
	 *            String the String to display
	 */
	public static void dumpString(String str) {
		return;
	};

	/**
	 * Get the classpath entry that was used to load the class that is the arg.
	 * <p>
	 * This method is for internal use only.
	 * 
	 * @param targetClass
	 *            Class the class to set the classpath of.
	 * @see java.lang.Class
	 */
	static int getCPIndexImpl(Class targetClass) {
		return 0;
	};

	/**
	 * Does internal initializaion required by VM.
	 * 
	 */
	static void initializeVM() {
	}

	/**
	 * Registers a new virtual-machine shutdown hook. This is equivalent to the
	 * 1.3 API of the same name.
	 * 
	 * @param hook
	 *            the hook (a Thread) to register
	 */
	public static void addShutdownHook(Thread hook) {
		return;
	}

	/**
	 * De-registers a previously-registered virtual-machine shutdown hook. This
	 * is equivalent to the 1.3 API of the same name.
	 * 
	 * @param hook
	 *            the hook (a Thread) to de-register
	 * @return true if the hook could be de-registered
	 */
	public static boolean removeShutdownHook(Thread hook) {
		return false;
	}

	/**
	 * This method must be provided by the vm vendor. Called to signal that the
	 * com.ibm.oti.net.www.protocol.jar.JarURLConnection class has been loaded
	 * and JarURLConnection.closeCachedFiles() should be called on VM shutdown.
	 * 
	 * @see #shutdown()
	 */
	public static void closeJars() {
		return;
	}

	/**
	 * This method must be provided by the vm vendor. Called to signal that the
	 * com.ibm.oti.util.DeleteOnExit class has been loaded and
	 * DeleteOnExit.deleteOnExit() should be called on VM shutdown.
	 * 
	 * @see #shutdown()
	 */
	public static void deleteOnExit() {
		return;
	}

	static final int CPE_TYPE_UNKNOWN = 0;

	static final int CPE_TYPE_DIRECTORY = 1;

	static final int CPE_TYPE_JAR = 2;

	static final int CPE_TYPE_TCP = 3;

	static final int CPE_TYPE_UNUSABLE = 5;

	/**
	 * Return the type of the specified entry on the class path for a ClassLoader.
	 * Valid tyes are:
	 * 		CPE_TYPE_UNKNOWN
	 *		CPE_TYPE_DIRECTORY
	 * 		CPE_TYPE_JAR
	 *		CPE_TYPE_TCP - this is obsolete
	 *		CPE_TYPE_UNUSABLE
	 * 
	 * @param classLoader the ClassLoader
	 * @param cpIndex the index on the class path
	 * 
	 * @return a int which specifies the class path entry type
	 */
	static final int getClassPathEntryType(Object classLoader, int cpIndex) {
		return 0;
	};

	/**
	 * Returns command line arguments passed to the VM. Internally these are
	 * broken into optionString and extraInfo. This only returns the
	 * optionString part.
	 * <p>
	 * 
	 * @return a String array containing the optionString part of command line
	 *         arguments
	 */
	public static String[] getVMArgs() {
		return null;
	};

	/**
	 * Return the number of entries on the bootclasspath.
	 * 
	 * @return an int which is the number of entries on the bootclasspath
	 */
	static int getClassPathCount() {
		return 0;
	};

	/**
	 * Return the specified bootclasspath entry.
	 * 
	 * @param index the index of the bootclasspath entry 
	 * 
	 * @return a byte array containing the bootclasspath entry
	 * 			specified in the vm options
	 */
	static byte[] getPathFromClassPath(int index) {
		return null;
	};

}
