
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
// $Id: NullCheckStmt.java,v 1.3 2004/07/16 15:11:32 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInvokeStmt;

/**
 * Jimple statement that represents a null pointer check.
 * This is a hack, as we have to look like a normal Jimple stmt.
 */
public class NullCheckStmt extends JInvokeStmt {

	public NullCheckStmt(Value value) {
		super(Jimple.v().newSpecialInvokeExpr(
		    (Local)value, getNullCheckMethod()));
	}

	public Value getNullCheckValue() {
	    return ((InstanceInvokeExpr)getInvokeExpr()).getBase();
	}

	public void toString(UnitPrinter up)
	{
	    up.literal("nullcheck ");
	    getNullCheckValue().toString(up);
	}

	public String toString()
	{
	    return "nullcheck " + getNullCheckValue();
	}

	// Use a method we know is always going to be available...
	public static SootMethod getNullCheckMethod() {
	    return Scene.v().getSootClass("java.lang.Object")
		.getMethodByName("getClass");
	}

	public static void insertNullCheck(PatchingChain units,
	    Stmt stmt, Value value) {
		NullCheckStmt nullCheck = new NullCheckStmt(value);
		units.insertBefore(nullCheck, stmt);
		stmt.redirectJumpsToThisTo(nullCheck);
	}

	public static boolean isNullCheck(Stmt stmt) {

		// Check the obvious first
		if (stmt instanceof NullCheckStmt)
			return true;

		// It seems our NullCheckStmt's get replicated
		// sometimes and turn into plain old InvokeStmt's
		if (stmt instanceof InvokeStmt
		    && stmt.getInvokeExpr().getMethod() == getNullCheckMethod())
			return true;
		return false;
	}
}

