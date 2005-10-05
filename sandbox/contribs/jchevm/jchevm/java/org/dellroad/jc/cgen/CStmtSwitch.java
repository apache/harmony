
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
// $Id: CStmtSwitch.java,v 1.16 2005/03/20 17:16:08 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.*;
import java.util.*;
import org.dellroad.jc.cgen.analysis.ActiveUseTag;
import soot.*;
import soot.baf.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.tags.*;

/**
 * Converts Jimple statements into C statements.
 */
public class CStmtSwitch extends AbstractStmtSwitch {
	CMethod cm;
	CodeWriter out;

	public CStmtSwitch(CMethod cm) {
		this.cm = cm;
		this.out = cm.out;
	}

	public void caseBreakpointStmt(BreakpointStmt stmt) {
		out.println("// Breakpoint: " + stmt);
	}

	public void caseInvokeStmt(InvokeStmt stmt) {
/********
		// Special case C.include() for included code
		if (v instanceof StaticInvokeExpr) {
			StaticInvokeExpr i = (StaticInvokeExpr)v;
			SootMethod method = i.getMethod();
			SootClass cls = method.getDeclaringClass();
			Value arg;
			if (cls.getName().equals("C")
			    && method.getName().equals("include")
			    && i.getArgCount() == 1
			    && (arg = i.getArg(0)) instanceof StringConstant) {
				out.println(((StringConstant)arg).value);
				return;
			}
		}
*********/
		// Get invoke expression
		ValueBox exprBox = stmt.getInvokeExprBox();

		// Special case NullCheckStmt
		if (NullCheckStmt.isNullCheck(stmt)) {
			NullCheckTag tag = (NullCheckTag)
			    exprBox.getTag("NullCheckTag");
			if (tag != null && !tag.needCheck())
				return;
			out.println(new CExpr(CExpr.FUNCTION,
			    "_JC_EXPLICIT_NULL_CHECK", "env",
			    ((SpecialInvokeExpr)exprBox.getValue())
			      .getBaseBox()) + ";");
			return;
		}

		// Special case ActiveUseCheckStmt
		if (ActiveUseCheckStmt.isActiveUseCheck(stmt)) {
			ActiveUseTag tag = (ActiveUseTag)
			    exprBox.getTag("ActiveUseTag");
			if (tag != null && !tag.isCheckNeeded())
				return;
			out.println("_JC_ACTIVE_USE(env, " + C.name(
			    ActiveUseCheckStmt.getSootClass(stmt)) + ");");
			return;
		}

		// Just a normal invocation
		out.println(C.value(exprBox) + ";");
	}

	public void caseAssignStmt(AssignStmt stmt) {

		// Output array store check if needed
		Value lhs = stmt.getLeftOp();
		Type lht = lhs.getType();
		if (lhs instanceof ArrayRef
		    && Util.isReference(lht)
		    && (Util.hasSubtypes(lht)
		      || !stmt.getRightOp().getType().equals(lht))) {
			out.println(new CExpr(CExpr.FUNCTION,
			   "_JC_ARRAYSTORE_CHECK", "env",
			   C.value(((ArrayRef)lhs).getBaseBox()),
			   C.value(stmt.getRightOpBox())) + ";");
		}

		// Output assignment
		out.println(C.value(stmt.getLeftOpBox())
		    + " = " + C.value(stmt.getRightOpBox()) + ";");
	}

	public void caseIdentityStmt(IdentityStmt stmt) {
		out.println(C.value(stmt.getLeftOpBox())
		    + " = " + C.value(stmt.getRightOpBox()) + ";");
	}

	public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
		out.println(new CExpr(CExpr.FUNCTION,
		    "_JC_MONITOR_ENTER", "env", stmt.getOpBox()) + ";");
	}

	public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
		out.println(new CExpr(CExpr.FUNCTION,
		    "_JC_MONITOR_EXIT", "env", stmt.getOpBox()) + ";");
	}

	public void caseGotoStmt(GotoStmt stmt) {
		branch(stmt, stmt.getTarget());
	}

	public void caseIfStmt(IfStmt stmt) {
		Stmt target = stmt.getTarget();
		out.print("if (" + C.value(stmt.getConditionBox()) + ")");
		if (stmt.equals(target) || cm.bodyChain.follows(stmt, target)) {
			out.println(" {");
			out.indent();
			backwardBranch(target);
			out.undent();
			out.println("}");
		} else {
			out.println();
			out.indent();
			forwardBranch(target);
			out.undent();
		}
	}

	private void branch(Unit from, Unit target) {
		if (from.equals(target) || cm.bodyChain.follows(from, target))
			backwardBranch(target);
		else
			forwardBranch(target);
	}

	private void backwardBranch(Unit target) {
		out.println("_JC_PERIODIC_CHECK(env);");
		out.println("goto label" + cm.getInfo(target).labelIndex + ";");
	}

	private void forwardBranch(Unit target) {
		out.println("goto label" + cm.getInfo(target).labelIndex + ";");
	}

	// Represents one case in a tableswitch or lookupswitch
	private static class Case implements Comparable {
		final int value;
		final Unit target;
		Case(int value, Unit target) {
			this.value = value;
			this.target = target;
		}
		public int compareTo(Object o) {
			Case that = (Case)o;
			return (this.value < that.value ? -1 :
			    this.value > that.value ? +1 : 0);
		}
	}

	public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
		int numCases = stmt.getTargetCount();
		Case[] cases = new Case[numCases];
		for (int i = 0; i < numCases; i++) {
			cases[i] = new Case(stmt.getLookupValue(i),
			    stmt.getTarget(i));
		}
		handleSwitch(stmt, stmt.getKeyBox(),
		    cases, stmt.getDefaultTarget());
	}

	private void handleSwitch(Stmt stmt, ValueBox key,
	    Case[] cases, Unit defaultTarget) {

		// Output switch statement
		out.println("switch (" + C.value(key) + ") {");

		// Sort cases (should already be sorted)
		Arrays.sort(cases);

		// Compress out cases that just branch to the default target
		int count = 0;
		for (int i = 0; i < cases.length; i++) {
			if (cases[i].target.equals(defaultTarget))
				continue;
			cases[count++] = cases[i];
		}

		// Output normal cases, compacting contiguous case ranges
		int previousValue = 0;
		int previousStartValue = 0;
		Unit previousTarget = null;
		boolean caseOpen = false;

		for (int i = 0; i < count; i++) {
			int value = cases[i].value;
			Unit target = cases[i].target;
			boolean closePrevious;

			// Determine whether we should close previous range
			if (caseOpen
			    && (value != previousValue + 1
			      || !target.equals(previousTarget))) {
				if (previousValue != previousStartValue)
					out.print(" ... " + previousValue);
				out.println(":");
				out.indent();
				branch(stmt, previousTarget);
				out.undent();
				caseOpen = false;
			}

			// Can we continue an already open case?
			if (caseOpen && i < count - 1) {
				previousValue = value;
				continue;
			}

			// Start a new case if necessary
			if (!caseOpen) {
				out.print("case " + value);
				previousStartValue = value;
				previousTarget = target;
			}

			// Is this the last value? If so we must stop
			if (i == count - 1) {
				if (caseOpen)
					out.print(" ... " + value);
				out.println(":");
				out.indent();
				branch(stmt, target);
				out.undent();
				break;
			}

			// Continue with open case
			previousValue = value;
			caseOpen = true;
		}

		// Output the default case
		out.println("default:");
		out.indent();
		branch(stmt, defaultTarget);
		out.undent();
		out.println('}');
	}

	public void caseNopStmt(NopStmt stmt) {
	}

	public void caseRetStmt(RetStmt stmt) {
		defaultCase(stmt);
	}

	public void caseReturnStmt(ReturnStmt stmt) {
		if (!cm.traps.isEmpty())
			out.println("_JC_CANCEL_TRAPS(env, catch);");
		out.println("return " + C.value(stmt.getOpBox()) + ";");
	}

	public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
		if (!cm.traps.isEmpty())
			out.println("_JC_CANCEL_TRAPS(env, catch);");
		out.println("return;");
	}

	public void caseTableSwitchStmt(TableSwitchStmt stmt) {
		int numCases = stmt.getHighIndex() - stmt.getLowIndex() + 1;
		Case[] cases = new Case[numCases];
		for (int i = 0; i < numCases; i++) {
			cases[i] = new Case(stmt.getLowIndex() + i,
			    stmt.getTarget(i));
		}
		handleSwitch(stmt, stmt.getKeyBox(),
		    cases, stmt.getDefaultTarget());
	}

	public void caseThrowStmt(ThrowStmt stmt) {
		out.println(new CExpr(CExpr.FUNCTION,
		    "_JC_THROW", "env", C.value(stmt.getOpBox())) + ";");
	}

	public void defaultCase(Object o) {
		throw new RuntimeException("unhandled case");
	}
}

