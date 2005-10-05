
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
// $Id: ClassConstant.java,v 1.1 2004/07/13 03:36:20 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.util.Random;
import soot.jimple.StringConstant;

/**
 * Hack for representing class constants, which Soot doesn't yet support.
 * We fake them as string constants with a special random prefix.
 * This class provides methods for converting back and forth.
 */
public class ClassConstant {

	// Random prefix to avoid collisions
	private static final String PREFIX
	    = Long.toHexString(new Random().nextLong()
	      + new Object().hashCode());

	private ClassConstant() {
	}

	public static StringConstant v(String className) {
		if (className.length() == 0 || className.charAt(0) == '[') {
			throw new RuntimeException("bogus class name `"
			    + className + "'");
		}
		return StringConstant.v(PREFIX + "/" + className);
	}

	public static boolean isClassConstant(StringConstant c) {
		int i;
		return (i = c.value.indexOf('/')) == PREFIX.length()
		    && c.value.substring(0, i).equals(PREFIX);
	}

	public static String getClassName(StringConstant c) {
		return c.value.substring(PREFIX.length() + 1);
	}
}

