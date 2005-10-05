
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
// $Id: StackAllocTag.java,v 1.3 2005/02/20 21:14:31 archiecobbs Exp $
//

package org.dellroad.jc.cgen.escape;

import soot.tagkit.Tag;

/**
 * Tag for stack-allocatable allocations.
 */
public class StackAllocTag implements Tag {

	private final static String NAME = "StackAllocTag";

	int id;

	public StackAllocTag(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public byte[] getValue() {
		return new byte[] {
		    (byte)(id >> 24), (byte)(id >> 16),
		    (byte)(id >> 8), (byte)id
		};
	}

	public String getName() {
		return NAME;
	}

	public String toString() {
		return NAME;
	}
}

