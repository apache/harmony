
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
// $Id: ObjectGenerator.java,v 1.1.1.1 2004/02/20 05:15:19 archiecobbs Exp $
//

package org.dellroad.jc;

import java.io.File;

/**
 * Interface implemented by classes that generate JC ELF object
 * files from raw class files.
 */
public interface ObjectGenerator {

	/**
	 * Generate the ELF object file for the named class
	 * and write it into the file.
	 *
	 * @param className Class name (with slashes, not dots)
	 * @param finder Object capable of retrieving class files
	 * @param file Where to put resulting ELF object
	 */
	public void generateObject(String className,
	    ClassfileFinder finder, File file) throws Exception;

	/**
	 * Determine if an ELF object file is valid for the named class
	 * and all other classes (available through the provided
	 * <code>finder</code>) on which the object file may depend.
	 *
	 * @param className Class name (with slashes, not dots)
	 * @param finder Class finder capable of retrieving class files
	 * @param file Where to put resulting ELF object
	 */
	public boolean objectIsValid(String className,
	    ClassfileFinder finder, File file) throws Exception;
}

