
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
// $Id: EscapingValueSwitch.java,v 1.1 2004/11/27 23:11:04 archiecobbs Exp $
//

package org.dellroad.jc.cgen.escape;

import java.util.*;
import org.dellroad.jc.cgen.*;
import soot.*;
import soot.jimple.*;

/**
 * Checks for values where the local can escape. The result
 * is <code>Boolean.TRUE</code> if so, <code>Boolean.FALSE</code> if not,
 * or <code>null</code> if the local can only escape as the 'return value'
 * of the <code>Value</code> itself.
 */
public class EscapingValueSwitch extends AbstractJimpleValueSwitch {

	private Local local;

	public Local getLocal() {
		return local;
	}

	public void setLocal(Local local) {
		this.local = local;
	}

	public void caseCastExpr(CastExpr v) {
		v.getOp().apply(this);
	}

	public void caseLocal(Local v) {
		setResult(v.equals(local) ? null : Boolean.FALSE);
	}

	public void caseInterfaceInvokeExpr(InterfaceInvokeExpr expr) {
		caseInstanceInvokeExpr(expr);
	}

	public void caseSpecialInvokeExpr(SpecialInvokeExpr expr) {
		caseInstanceInvokeExpr(expr);
	}

	public void caseVirtualInvokeExpr(VirtualInvokeExpr expr) {
		caseInstanceInvokeExpr(expr);
	}

	public void caseStaticInvokeExpr(StaticInvokeExpr expr) {
		handleInvokeParameters(expr);
	}

	private void caseInstanceInvokeExpr(InstanceInvokeExpr expr) {
		expr.getBase().apply(this);
		if (getResult() != Boolean.FALSE) {
			setResult(Boolean.TRUE);
			return;
		}
		handleInvokeParameters(expr);
	}

	private void handleInvokeParameters(InvokeExpr expr) {
		int argCount = expr.getArgCount();
		for (int i = 0; i < argCount; i++) {
			expr.getArg(i).apply(this);
			if (getResult() != Boolean.FALSE) {
				setResult(Boolean.TRUE);
				return;
			}
		}
		setResult(Boolean.FALSE);
	}

	public void defaultCase(Object obj) {
		setResult(Boolean.FALSE);
	}
}

