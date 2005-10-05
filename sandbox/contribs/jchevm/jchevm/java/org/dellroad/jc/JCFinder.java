
//
// Copyright 2005 The Apache Software Foundation or its licensors,
// as applicable.
// 
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
// $Id: JCFinder.java,v 1.1.1.1 2004/02/20 05:15:19 archiecobbs Exp $
//

package org.dellroad.jc;

/**
 * Gateway into the JC virtual machine for retrieving class files.
 */
public class JCFinder implements ClassfileFinder {

	private final ClassLoader loader;

	/**
	 * Create a new finder that uses the given class loader
	 * to pull in not-before-seen class files as necessary.
	 */
	public JCFinder(ClassLoader loader) {
		this.loader = loader;
	}

	public byte[] getClassfile(String className)
	    throws ClassNotFoundException {
		try {
			return getClassfile(className, loader);
		} catch (NoClassDefFoundError e) {
			throw new ClassNotFoundException(e.getMessage());
		}
	}

	public long getClassfileHash(String className)
	    throws ClassNotFoundException {
		try {
			return getClassfileHash(className, loader);
		} catch (NoClassDefFoundError e) {
			throw new ClassNotFoundException(e.getMessage());
		}
	}

	private static native byte[] getClassfile(String className,
	    ClassLoader loader);

	private static native long getClassfileHash(String className,
	    ClassLoader loader);
}

