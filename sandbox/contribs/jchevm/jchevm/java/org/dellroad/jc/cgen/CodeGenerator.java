
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
// $Id: CodeGenerator.java,v 1.2 2004/07/13 03:36:20 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.OutputStream;
import org.dellroad.jc.ClassfileFinder;

/**
 * Interface for objects that can generate C source code and header files
 * that conform to the JC C source file standard.
 */
public interface CodeGenerator {

	/**
	 * Generate the C header file for the given class.
	 *
	 * @param className name of the class who's header file we're generating
	 * @param finder a way to acquire Java class files
	 * @param output where to write the generated header file to
	 */
	public void generateH(String className, ClassfileFinder finder,
	    OutputStream output) throws Exception;

	/**
	 * Generate the C source file for the given class.
	 *
	 * @param className name of the class who's C file we're generating
	 * @param finder a way to acquire Java class files
	 * @param output where to write the generated C file to
	 */
	public void generateC(String className, ClassfileFinder finder,
	    OutputStream output) throws Exception;

	/**
	 * Reset state.
	 *
	 * <p>
	 * Currently only one CodeGenerator instance will be
	 * in use at a time. Before and after each use, this
	 * method is invoked. A good oportunity to free objects
	 * that are no longer needed, (re)initialize state, etc.
	 */
	public void reset();
}

