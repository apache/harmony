
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
// $Id: EscapingStmtSwitch.java,v 1.1 2004/11/27 23:11:04 archiecobbs Exp $
//

package org.dellroad.jc.cgen.escape;

import java.util.*;
import org.dellroad.jc.cgen.*;
import soot.*;
import soot.jimple.*;

/**
 * Checks for statements where the local can escape. The result
 * is <code>Boolean.TRUE</code> if so, <code>Boolean.FALSE</code> if not,
 * or <code>null</code> if the statement assigns the local to another local.
 */
public class EscapingStmtSwitch extends AbstractStmtSwitch {

	private final EscapingValueSwitch escapingValueSwitch;
	private Local local;

	public EscapingStmtSwitch() {
		escapingValueSwitch = new EscapingValueSwitch();
	}

	public Local getLocal() {
		return local;
	}

	public void setLocal(Local local) {
		this.local = local;
		escapingValueSwitch.setLocal(local);
	}

	public void caseAssignStmt(AssignStmt stmt) {
		Value lhs = stmt.getLeftOp();
		Value rhs = stmt.getRightOp();

		// Check if local can escape via the expression itself
		rhs.apply(escapingValueSwitch);
		Object valueResult = escapingValueSwitch.getResult();
		if (valueResult != null) {
			setResult(valueResult);
			return;
		}

		// If we get here, the expression is the local..

		// Check for assignment of local to a field
		// XXX OK if field is in another non-escaping local
		if (lhs instanceof FieldRef) {
			setResult(Boolean.TRUE);
			return;
		}

		// Check for assignment of local to an array element
		// XXX OK if field is in another non-escaping array
		if (lhs instanceof ArrayRef) {
			setResult(Boolean.TRUE);
			return;
		}

		// Assignment must be to some (possibly the same) local
		if (!(lhs instanceof Local))
			throw new RuntimeException("unexpected");

		// Check for assignment of local to itself
		if (lhs.equals(local)) {
			setResult(Boolean.FALSE);
			return;
		}

		// Escape depends on whether the assigned-to local can escape
		setResult(null);
	}

	public void caseInvokeStmt(InvokeStmt stmt) {
		if (NullCheckStmt.isNullCheck(stmt)) {
			setResult(Boolean.FALSE);
			return;
		}
		stmt.getInvokeExpr().apply(escapingValueSwitch);
		Boolean result = (Boolean)escapingValueSwitch.getResult();
		if (result == null)
			result = Boolean.FALSE;	// return value is ignored
		setResult(result);
	}

	public void caseReturnStmt(ReturnStmt stmt) {
		setResult(Boolean.TRUE);
	}

	public void caseThrowStmt(ThrowStmt stmt) {
		setResult(Boolean.TRUE);
	}

	public void defaultCase(Object obj) {
		setResult(Boolean.FALSE);
	}
}

