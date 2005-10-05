
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
// $Id: StackAlloc.java,v 1.2 2004/12/17 15:35:46 archiecobbs Exp $
//

package org.dellroad.jc.cgen.escape;

import java.util.*;
import soot.*;
import soot.jimple.*;

/**
 * Holds information about a potential stack-allocatable 'new' statement.
 */
class StackAlloc {

	AssignStmt stmt;
	Local local;
	Value value;		// the 'new' expression
	Boolean escapes; 	// TRUE = yes, FALSE = no, null = unknown
	int size;
	int cost;

	public StackAlloc(Stmt ostmt) {
		this.stmt = (AssignStmt)ostmt;
		this.local = (Local)stmt.getLeftOp();
		this.value = stmt.getRightOp();
	}
}

