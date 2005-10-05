
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
// $Id: Constants.java,v 1.1.1.1 2004/02/20 05:15:25 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

/**
 * Constants that define the sizes of certain automatically
 * generated hash tables used by generated C code.
 */
public interface Constants {
	/**
	 * Size of interface method lookup hash tables
	 * and ``quick'' interface method lookup tables.
	 * Must be a power of two and equal to the definition
	 * in <code>jc_defs.h</code>.
	 */
	public static final int IMETHOD_HASHSIZE = 128;

	/**
	 * Size of instanceof hash tables.
	 * Must be a power of two and equal to the definition
	 * in <code>jc_defs.h</code>.
	 */
	public static final int INSTANCEOF_HASHSIZE = 128;
}

