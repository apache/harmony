
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
// $Id: ClassfileFinder.java,v 1.1.1.1 2004/02/20 05:15:19 archiecobbs Exp $
//

package org.dellroad.jc;

/**
 * Represents objects capable of retrieving class files.
 */
public interface ClassfileFinder {

	/**
	 * Retrieve the class file that is used to define the named class.
	 * This is a capability specific to the JC virtual machine.
	 *
	 * @param className Class name (with slashes, not dots)
	 * @throws ClassNotFoundException if class is not found
	 */
	public byte[] getClassfile(String className)
		throws ClassNotFoundException;

	/**
	 * Return the hash of the bytes that would be returned by
	 * {@link #getClassfile getClassfile()} with the same arguments.
	 * The hash of a class file is defined as the last 16 bytes
	 * of the MD5 of the class file.
	 *
	 * @param className Class name (with slashes, not dots)
	 * @throws ClassNotFoundException if class is not found
	 */
	public long getClassfileHash(String className)
		throws ClassNotFoundException;
}

