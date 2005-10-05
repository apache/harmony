
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
// $Id: ActiveUseCheckStmt.java,v 1.1 2004/12/24 21:55:56 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.util.Random;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInvokeStmt;

/**
 * Jimple statement that represents a class initialization check.
 * This is a hack, as we have to look like a normal Jimple stmt.
 */
public class ActiveUseCheckStmt extends JInvokeStmt {

	// Random prefix to avoid collisions
	private static final String PREFIX
	    = Long.toHexString(new Random().nextLong()
		+ new Object().hashCode());

	public ActiveUseCheckStmt(SootClass sc) {
		super(Jimple.v().newStaticInvokeExpr(getCheckMethod(),
		    StringConstant.v(PREFIX + "/" + sc.getName())));
	}

	public void toString(UnitPrinter up)
	{
	    up.literal(toString());
	}

	public String toString()
	{
	    return "activeusecheck " + getSootClass(this).getName();
	}

	public static SootClass getSootClass(Stmt stmt) {
		return Scene.v().getSootClass(getClassName(stmt));
	}

	private static SootMethod getCheckMethod() {
	    return Scene.v().getSootClass("java.lang.Class")
		.getMethod("java.lang.Class forName(java.lang.String)");
	}

	public static void insertActiveUseCheck(PatchingChain units,
	    Stmt stmt, SootClass cl) {
		ActiveUseCheckStmt check = new ActiveUseCheckStmt(cl);
		units.insertBefore(check, stmt);
		stmt.redirectJumpsToThisTo(check);
	}

	public static boolean isActiveUseCheck(Stmt stmt) {
		if (stmt instanceof ActiveUseCheckStmt)
			return true;
		return getClassName(stmt) != null;
	}

	private static String getClassName(Stmt stmt) {
		if (!(stmt instanceof InvokeStmt))
			return null;
		InvokeExpr expr = ((InvokeStmt)stmt).getInvokeExpr();
		if (!expr.getMethod().equals(getCheckMethod()))
			return null;
		Value arg = expr.getArg(0);
		if (!(arg instanceof StringConstant))
			return null;
		String s = ((StringConstant)arg).value;
		int i;
		if ((i = s.indexOf('/')) != PREFIX.length()
		    || !s.substring(0, i).equals(PREFIX))
		    	return null;
		return s.substring(PREFIX.length() + 1);
	}
}

