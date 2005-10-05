
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
// $Id: FinalizeDetector.java,v 1.1 2004/12/17 15:35:46 archiecobbs Exp $
//

package org.dellroad.jc.cgen.analysis;

import java.util.*;
import org.dellroad.jc.cgen.*;
import soot.*;
import soot.jimple.*;

/**
 * Instance of this class detect 'new' expressions of objects that
 * override finalize(). The result is either <code>Boolean.TRUE</code>
 * or <code>Boolean.FALSE</code>.
 */
public class FinalizeDetector extends AbstractJimpleValueSwitch {

	public void caseNewExpr(NewExpr v) {
		SootClass sc = v.getBaseType().getSootClass();
		SootMethod finalize = sc.getMethod("finalize",
		    Collections.EMPTY_LIST, VoidType.v());
		setResult(Boolean.valueOf(
		    !finalize.getDeclaringClass().getName()
		      .equals("java.lang.Object")));
	}

	public void defaultCase(Object obj) {
		setResult(Boolean.FALSE);
	}
}

