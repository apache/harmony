
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
// $Id: StmtTagCopierSwitch.java,v 1.2 2004/12/21 01:54:03 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.jimple.*;

/**
 * Copies tags from Jimple Stmt's to ValueBoxes.
 * All tags are copied.
 */
public class StmtTagCopierSwitch extends AbstractStmtSwitch {

	public void caseInvokeStmt(InvokeStmt stmt) {
		stmt.getInvokeExprBox().addAllTagsOf(stmt);
	}

	public void caseAssignStmt(AssignStmt stmt) {
		stmt.getRightOpBox().addAllTagsOf(stmt);
		stmt.getLeftOpBox().addAllTagsOf(stmt);
	}

	public void caseIdentityStmt(IdentityStmt stmt) {
		stmt.getRightOpBox().addAllTagsOf(stmt);
	}

	public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
		stmt.getOpBox().addAllTagsOf(stmt);
	}

	public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
		stmt.getOpBox().addAllTagsOf(stmt);
	}

	public void caseIfStmt(IfStmt stmt) {
		stmt.getConditionBox().addAllTagsOf(stmt);
	}

	public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
		stmt.getKeyBox().addAllTagsOf(stmt);
	}

	public void caseReturnStmt(ReturnStmt stmt) {
		stmt.getOpBox().addAllTagsOf(stmt);
	}

	public void caseTableSwitchStmt(TableSwitchStmt stmt) {
		stmt.getKeyBox().addAllTagsOf(stmt);
	}

	public void caseThrowStmt(ThrowStmt stmt) {
		stmt.getOpBox().addAllTagsOf(stmt);
	}
}

