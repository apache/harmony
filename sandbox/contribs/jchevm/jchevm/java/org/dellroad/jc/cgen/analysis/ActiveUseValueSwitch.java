
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
// $Id: ActiveUseValueSwitch.java,v 1.2 2004/12/22 23:39:06 archiecobbs Exp $
//

package org.dellroad.jc.cgen.analysis;

import soot.jimple.*;
import soot.grimp.AbstractGrimpValueSwitch;
import soot.grimp.NewInvokeExpr;
import soot.toolkits.scalar.FlowSet;

/**
 * Determine the SootClass which will definitely be initialized after
 * evaluating the value, if any.
 */
public class ActiveUseValueSwitch extends AbstractGrimpValueSwitch {

	public void caseNewInvokeExpr(NewInvokeExpr v) {
		setResult(v.getMethod().getDeclaringClass());
	}

	public void caseSpecialInvokeExpr(SpecialInvokeExpr v) {
		setResult(v.getMethod().getDeclaringClass());
	}

	public void caseStaticInvokeExpr(StaticInvokeExpr v) {
		setResult(v.getMethod().getDeclaringClass());
	}

	public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
		setResult(v.getMethod().getDeclaringClass());
	}

	public void caseNewExpr(NewExpr v) {
		setResult(v.getBaseType().getSootClass());
	}

	public void caseInstanceFieldRef(InstanceFieldRef v) {
		setResult(v.getField().getDeclaringClass());
	}

	public void caseStaticFieldRef(StaticFieldRef v) {
		setResult(v.getField().getDeclaringClass());
	}
}

