
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
// $Id: SourceLocator.java,v 1.1.1.1 2004/02/20 05:15:26 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import soot.Singletons;
import soot.util.ClassInputStream;
import org.dellroad.jc.ClassfileFinder;

/**
 * Implementation of Soot's {@link soot.util.SourceLocator} interface
 * used by the {@link SootCodeGenerator} class. In order for
 * Soot to pick up and analyze the correct class files, we must
 * provide our own implementation which retrieves them from the
 * JC virtual machine. This ensures that no matter how any particular
 * ClassLoader retrieves a class file, that exact same class file
 * will be made available to Soot for analysis.
 */
public class SourceLocator extends soot.util.SourceLocator {

	private ClassfileFinder finder;

	public SourceLocator(Singletons.Global g) {
		super(g);
	}

	public void setFinder(ClassfileFinder finder) {
		this.finder = finder;
	}

	/**
	 * Retrieve the classfile contents for the named class,
	 * as loaded by the ClassLoader associated with this object.
	 */
	public InputStream getInputStreamOf(String className)
	    throws ClassNotFoundException {
		return new ClassInputStream(
		    new ByteArrayInputStream(
		    finder.getClassfile(className)));
	}
}

