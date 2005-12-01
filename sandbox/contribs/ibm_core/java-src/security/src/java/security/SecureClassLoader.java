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

package java.security;


import java.util.Hashtable;

/**
 * SecureClassLoaders are used to dynamically load, link and install classes
 * into a running image. Additionally, they (optionally) associate the classes
 * they create with a code source and provide mechanisms to allow the relevant
 * permissions to be retrieved.
 * 
 */

public class SecureClassLoader extends ClassLoader {

	/**
	 * Maintain a map of CodeSources to ProtectionDomains so we do not have to
	 * create a new one each time.
	 */
	private Hashtable domainForCodeSource = new Hashtable(10);

	/**
	 * Constructs a new instance of this class with the system class loader as
	 * its parent.
	 * 
	 * @exception SecurityException
	 *                if a security manager exists and it does not allow the
	 *                creation of new ClassLoaders.
	 */
	protected SecureClassLoader() {
		super();
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkCreateClassLoader();
	}

	/**
	 * Constructs a new instance of this class with the given class loader as
	 * its parent.
	 * 
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
	protected SecureClassLoader(ClassLoader parentLoader) {
		super(parentLoader);
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkCreateClassLoader();
	}

	/**
	 * Constructs a new class from an array of bytes containing a class
	 * definition in class file format.
	 * 
	 * 
	 * @return java.lang.Class the newly defined class.
	 * @param className
	 *            java.lang.String the name of the new class.
	 * @param classRep
	 *            byte[] a memory image of a class file.
	 * @param offset
	 *            int the offset into the classRep.
	 * @param length
	 *            int the length of the class file.
	 * @param cs
	 *            CodeSource the code source that the new class will be
	 *            associated with.
	 */

	protected final Class defineClass(String className, byte[] classRep,
			int offset, int length, final CodeSource cs) {
		ProtectionDomain pd = null;
		if (cs != null) {
			pd = (ProtectionDomain) domainForCodeSource.get(cs);
			if (pd == null) {
				pd = new ProtectionDomain(cs, getPermissions(cs));
				domainForCodeSource.put(cs, pd);
			}
		}
		return defineClass(className, classRep, offset, length, pd);
	}

	/**
	 * Answers the permission collection for the given code source. By default,
	 * it just asks the installed Policy object.
	 * 
	 * 
	 * @return PermissionCollection the permission collection for the code
	 *         source.
	 * @param codesource
	 *            java.security.CodeSource the code source to check permissions
	 *            for.
	 */
	protected PermissionCollection getPermissions(CodeSource codesource) {
		return Policy.getPolicyImpl().getPermissions(codesource);
	}

}
