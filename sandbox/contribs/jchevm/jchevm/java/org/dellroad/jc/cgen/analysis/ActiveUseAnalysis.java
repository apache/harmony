
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
// $Id: ActiveUseAnalysis.java,v 1.3 2004/12/24 21:55:56 archiecobbs Exp $
//

package org.dellroad.jc.cgen.analysis;

import java.util.Iterator;
import org.dellroad.jc.cgen.ActiveUseCheckStmt;
import org.dellroad.jc.cgen.Util;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Analysis used by {@link ActiveUseTagger ActiveUseTagger}.
 */
public class ActiveUseAnalysis extends ForwardFlowAnalysis {

	private final UnitGraph graph;

	/**
	 * Equivalent to
	 * <code>ActiveUseAnalysis(new CompleteUnitGraph(body))</code>.
	 *
	 * @param body method body to analyze
	 */
	public ActiveUseAnalysis(Body body) {
		this(new CompleteUnitGraph(body));
	}

	/**
	 * Do active use analysis on body using the supplied unit graph.
	 *
	 * @param graph method body to analyze
	 */
	public ActiveUseAnalysis(UnitGraph graph) {
		super(graph);
		this.graph = graph;
		doAnalysis();
	}

	/**
	 * Determine if a first active use check for class <code>cl</code>
	 * would be needed before executing <code>unit</code>.
	 */
	public boolean isCheckNeeded(Unit unit, SootClass cl) {
		FlowSet inSet = (FlowSet)unitToBeforeFlow.get(unit);
		return !inSet.contains(cl);
	}

	protected Object entryInitialFlow() {
		ArraySparseSet set = new ArraySparseSet();
		addTransitive(set, graph.getBody()
		    .getMethod().getDeclaringClass());
		addTransitive(set, Scene.v().getSootClass("java.lang.Class"));
		return set;
	}

	protected Object newInitialFlow() {
		return new ArraySparseSet();
	}

	protected void flowThrough(Object in, Object obj, Object out) {
		Stmt stmt = (Stmt)obj;
		FlowSet inFlow = (FlowSet)in;
		FlowSet outFlow = (FlowSet)out;
		inFlow.copy(outFlow);
		if (ActiveUseCheckStmt.isActiveUseCheck(stmt)) {
			addTransitive(outFlow,
			    ActiveUseCheckStmt.getSootClass(stmt));
			return;
		}
		ActiveUseValueSwitch sw = new ActiveUseValueSwitch();
		for (Iterator i = stmt.getUseBoxes().iterator();
		    i.hasNext(); ) {
			((ValueBox)i.next()).getValue().apply(sw);
			SootClass cl = (SootClass)sw.getResult();
			if (cl != null)
				addTransitive(outFlow, cl);
		}
	}

	protected void merge(Object in1, Object in2, Object out) {
		FlowSet inSet1 = (FlowSet)in1;
		FlowSet inSet2 = (FlowSet)in2;
		FlowSet outSet = (FlowSet)out;
		inSet1.intersection(inSet2, outSet);
	}

	protected void copy(Object src, Object dst) {
		FlowSet srcSet = (FlowSet)src;
		FlowSet dstSet = (FlowSet)dst;
		srcSet.copy(dstSet);
	}

	// Add class and all superclasses
	private void addTransitive(FlowSet set, SootClass cl) {
		set.add(cl);
		while (cl.hasSuperclass()) {
			cl = cl.getSuperclass();
			set.add(cl);
		}
	}
}

