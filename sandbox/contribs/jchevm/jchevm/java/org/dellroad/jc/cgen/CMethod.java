
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
// $Id: CMethod.java,v 1.21 2005/05/14 21:58:24 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.*;
import java.util.*;
import org.dellroad.jc.cgen.analysis.ActiveUseTagger;
import org.dellroad.jc.cgen.analysis.FollowsAnalysis;
import org.dellroad.jc.cgen.escape.StackAllocTag;
import soot.*;
import soot.baf.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.annotation.tags.*;
import soot.tagkit.*;
import soot.toolkits.graph.CompleteUnitGraph;

/**
 * Implements JC's C code generation algorithm for implementing Java methods.
 */
public class CMethod {

	private static final ThreadLocal CURRENT_METHOD = new ThreadLocal();

	private static final boolean DEBUG = false;

	final SootMethod m;
	final CFile cfile;
	final CodeWriter out;
	StmtBody body;
	final MethodOptimizer optimizer;
	final boolean includeLineNumbers;
	int lineNumberTableLength;
	CompleteUnitGraph graph;
	PatchingChain bodyChain;
	Map infoMap;			// Stmt -> StmtInfo
	List traps;			// Trap's

	public CMethod(CFile cfile, SootMethod m, MethodOptimizer optimizer,
	    boolean includeLineNumbers, Set deps) {
		this.cfile = cfile;
		this.out = cfile.out;
		this.m = m;
		this.optimizer = optimizer;
		this.includeLineNumbers = includeLineNumbers;
		processBody(deps);
	}

	// Analysis information about a Stmt
	static class StmtInfo {
		char javaLineNumber = 0;		// java source line #
		boolean branchTarget = false;		// target of branch/trap
		boolean trapBoundary = false;		// trap begin or end
		boolean backwardTrapTarget = false;	// back-branching trap
		int lineNumberIndex = -1;		// line # table index
		short labelIndex = -1;			// goto label index
		short region = 0;			// trap region
		boolean needsRegionUpdate = false;	// update new region
	}

	// Provides access to the current method being output
	public static CMethod current() {
		return (CMethod)CURRENT_METHOD.get();
	}

	// Get the StmtInfo corresponding to 'unit'. Create one if needed.
	StmtInfo getInfo(Unit unit) {
		StmtInfo info;
		if ((info = (StmtInfo)infoMap.get(unit)) == null) {
			info = new StmtInfo();
			infoMap.put(unit, info);
		}
		return info;
	}

	// Primp, tweak, and optimize the method body
	private void processBody(Set deps) {

		// Skip if we're not going to generate a normal body
		if (!m.isConcrete())
			return;

		// Wrap synchronized methods with monitorenter/exit statements
		if (m.isSynchronized())
			wrapSynchronized(m.retrieveActiveBody());

		// Debug
		if (DEBUG)
			dumpBody(m, "BEFORE OPTIMIZATION");

		// Perform useful optimizations on method body
		graph = optimizer.optimize(m, deps);

		// Debug
		if (DEBUG)
			dumpBody(m, "AFTER OPTIMIZATION");

		// Get body and Unit chain
		body = (JimpleBody)graph.getBody();
		bodyChain = body.getUnits();

		// Copy tags from statements to value boxes
		StmtTagCopierSwitch tagCopier = new StmtTagCopierSwitch();
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			stmt.apply(tagCopier);
		}

		// Elide leading '$' from locals' names
		for (Iterator i = body.getLocals().iterator(); i.hasNext(); ) {
			Local local = (Local)i.next();
			String name = local.getName();
			if (name.charAt(0) == '$')
				local.setName(name.substring(1));
		}
	}

	// Analyze the body to derive various bits of information we need.
	// This method is not allowed to change the method body
	private void analyzeBody() {

		// Skip if we're not going to generate a normal body
		if (!m.isConcrete())
			return;

		// Initialize Stmt info map
		infoMap = new HashMap();

		// Mark trap targets that represent possible backward branches
		traps = new ArrayList(body.getTraps());
		for (int i = 0; i < traps.size(); i++) {
			Trap trap = (Trap)traps.get(i);
			Unit handler = trap.getHandlerUnit();
			if (bodyChain.follows(trap.getEndUnit(), handler))
				getInfo(handler).backwardTrapTarget = true;
		}

		// Mark statements that change the Java source line number
		if (includeLineNumbers) {
			int lastLineNumber = -1;
			for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
				Unit unit = (Unit)i.next();
				LineNumberTag tag = (LineNumberTag)unit.getTag(
				    "LineNumberTag");
				if (tag == null)
					continue;
				int lineNumber = tag.getLineNumber();
				if (lineNumber == lastLineNumber)
					continue;
				getInfo(unit).javaLineNumber = (char)lineNumber;
				lastLineNumber = lineNumber;
			}
		}

		// Mark branch targets as branch targets needing labels
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Unit unit = (Unit)i.next();
			for (Iterator j = unit.getUnitBoxes().iterator();
			    j.hasNext(); ) {
				UnitBox ub = (UnitBox)j.next();
				getInfo(ub.getUnit()).branchTarget = true;
			}
		}

		// Mark trap handlers as branch targets and region updates
		for (Iterator i = traps.iterator(); i.hasNext(); ) {
			Trap trap = (Trap)i.next();
			StmtInfo info = getInfo(trap.getHandlerUnit());
			info.branchTarget = true;
			info.needsRegionUpdate = true;
		}

		// Mark trap range boundaries
		for (Iterator i = traps.iterator(); i.hasNext(); ) {
			Trap trap = (Trap)i.next();
			getInfo(trap.getBeginUnit()).trapBoundary = true;
			getInfo(trap.getEndUnit()).trapBoundary = true;
		}

		// Assign trap regions to units
		short region = 0;
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			StmtInfo info = getInfo((Unit)i.next());
			if (info.trapBoundary)
				region++;
			info.region = region;
		}

		// Annotate all Units into which control can flow
		// such that a new trap region is entered.
		boolean first = true;
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Unit unit = (Unit)i.next();

			// Get this Unit's region
			StmtInfo info = getInfo(unit);
			region = info.region;

			// Special case first unit
			if (first) {
				if (region != 0)
					getInfo(unit).needsRegionUpdate = true;
				first = false;
			}

			// Annotate any subsequent statements in other regions
			if (unit.fallsThrough()) {
				info = getInfo((Unit)bodyChain.getSuccOf(unit));
				if (info.region != region)
					info.needsRegionUpdate = true;
			}
			for (Iterator j = unit.getUnitBoxes().iterator();
			    j.hasNext(); ) {
				info = getInfo(((UnitBox)j.next()).getUnit());
				if (info.region != region)
					info.needsRegionUpdate = true;
			}
		}

		// Scan StmtInfo's and mark for line numbers and branch labels
		StmtInfo previous = new StmtInfo();
		lineNumberTableLength = 0;
		short nextLabelIndex = 0;
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Unit unit = (Unit)i.next();
			StmtInfo info = getInfo(unit);

			// Inherit Java line number from previous unit
			if (info.javaLineNumber == 0)
				info.javaLineNumber = previous.javaLineNumber;

			// Line number table entry if Java line number changes
			if (info.javaLineNumber != previous.javaLineNumber)
			    	info.lineNumberIndex = lineNumberTableLength++;

			// We need a label if any branch or trap targets us
			if (info.branchTarget)
				info.labelIndex = nextLabelIndex++;

			// Update for next go round
			previous = info;
		}

		// Tag static method and field refs for active use
		new ActiveUseTagger(graph);
	}

	/**
	 * Make a synchronized method look as if it was contained inside
	 * a big synchronized block. This relieves the VM itself from having
	 * to explicitly do the synchronization operations. Note that this
	 * is only done for non-native methods. The VM is still responsible
	 * for handling synchronization for native methods.
	 */
	public static void wrapSynchronized(Body body) {

		// Check for applicability
		SootMethod method = body.getMethod();
		if (body.getUnits().size() == 0 || !method.isSynchronized())
			return;
		SootClass c = method.getDeclaringClass();

		// Remember original first statment
		PatchingChain units = body.getUnits();
		Unit firstStmt = (Unit)units.getFirst();

		// Lock monitor at the start of the method
		Local thisRef = null;
		StringConstant classConstant = null;
		if (!method.isStatic()) {
			thisRef = Jimple.v().newLocal("thisRef", RefType.v(c));
			body.getLocals().addLast(thisRef);
			units.addFirst(Jimple.v().newEnterMonitorStmt(
			    (Value)thisRef));
			units.addFirst(Jimple.v().newIdentityStmt(thisRef,
			    Jimple.v().newThisRef(RefType.v(c))));
		} else {
			classConstant = ClassConstant.v(
			    method.getDeclaringClass().getName());
			units.addFirst(Jimple.v().newEnterMonitorStmt(
			    classConstant));
		}

		// Unlock monitor before each return statement. We don't
		// need to unlock before throw statements because the trap
		// handler we add below will automatically handle those cases.
		HashSet returns = new HashSet();
		for (Iterator i = units.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			if (stmt instanceof ReturnStmt
			    || stmt instanceof ReturnVoidStmt)
				returns.add(stmt);
		}
		for (Iterator i = returns.iterator(); i.hasNext(); ) {
			units.insertBefore(Jimple.v().newExitMonitorStmt(
			    method.isStatic() ?
			      (Value)classConstant : (Value)thisRef),
			    i.next());
		}

		// Add a trap and handler to unlock monitor after any exception
		SootClass jlt = Scene.v().getSootClass("java.lang.Throwable");
		Local e = Jimple.v().newLocal("exception", RefType.v(jlt));
		body.getLocals().addLast(e);
		units.addLast(Jimple.v().newIdentityStmt(e,
		    Jimple.v().newCaughtExceptionRef()));
		Unit trapHandler = (Unit)body.getUnits().getLast();
		units.addLast(Jimple.v().newExitMonitorStmt(
		    method.isStatic() ? (Value)classConstant : (Value)thisRef));
		units.addLast(Jimple.v().newThrowStmt(e));
		body.getTraps().addLast(
		    Jimple.v().newTrap(jlt, firstStmt,
		    trapHandler, trapHandler));
	}

	public void outputMethodInfo() {

		// Analyze method
		if (infoMap == null)
			analyzeBody();

		// Determine what to do
		boolean generateBody = !cfile.c.isInterface()
		    || Util.isClassInit(m);
		boolean doLineNumTable = generateBody
		    && !m.isAbstract()
		    && includeLineNumbers
		    && lineNumberTableLength > 0;
		boolean doTrapTable = m.isConcrete() && !traps.isEmpty();

		// Declare line number and trap table
		if (doLineNumTable) {
			out.println("static _jc_linenum "
			    + cfile.prefix + "$linenum_table$"
			    + C.name(m) + "[];");
			out.println();
		}

		// Output exception trap table
		if (doTrapTable) {
			out.println("static _jc_trap_info " + cfile.prefix
			    + "$trap_table$" + C.name(m) + "[] = {");
			out.indent();
			SootClass throwable = Scene.v()
			    .getSootClass("java.lang.Throwable");
			for (int i = 0; i < traps.size(); i++) {
				Trap trap = (Trap)traps.get(i);
				SootClass type = trap.getException();
				out.println("_JC_TRAP("
				    + (type.equals(throwable) ? "_JC_NULL" :
					"&_jc_" + C.name(type) + "$type")
				    + ", " + getInfo(trap.getBeginUnit()).region
				    + ", " + getInfo(trap.getEndUnit()).region
				    + "),");
			}
			out.undent();
			out.println("};");
			out.println();
		}

		// Output parameter 'ptypes'
		out.println("static const unsigned char " + cfile.prefix
		    + "$param_ptypes$" + C.name(m) + "[] = { ");
		for (int i = 0; i < m.getParameterCount(); i++) {
			out.print(Util._JC_TYPE(m.getParameterType(i)));
			out.print(", ");
		}
		out.println(Util._JC_TYPE(m.getReturnType()) + " };");
		out.println();

		// Output exception list
		List elist = m.getExceptions();
		if (elist.size() > 0) {
			out.println("static _jc_type *const " + cfile.prefix
			    + "$exceptions$" + C.name(m) + "[] = {");
			out.indent();
			for (Iterator i = elist.iterator(); i.hasNext(); ) {
				SootClass ec = (SootClass)i.next();
				out.print("&_jc_" + C.name(ec) + "$type");
				if (i.hasNext())
					out.print(",");
				out.println();
			}
			out.undent();
			out.println("};");
			out.println();
		}

		// Output parameter types
		if (m.getParameterCount() > 0) {
			out.println("static _jc_type *const " + cfile.prefix
			    + "$param_types$" + C.name(m) + "[] = {");
			out.indent();
			for (int i = 0; i < m.getParameterCount(); i++) {
				out.print("&"
				    + C.jc_type(m.getParameterType(i)));
				if (i < m.getParameterCount() - 1)
					out.print(",");
				out.println();
			}
			out.undent();
			out.println("};");
			out.println();
		}

		// Output initial method info stuff
		out.println("_jc_method " + cfile.prefix
		    + "$method_info$" + C.name(m) + " = {");
		out.indent();
		out.println(".name=\t\t\t" + C.string(m.getName()) + ",");
		out.println(".signature=\t\t"
		    + C.string(Util.signature(m)) + ",");
		out.println(".class=\t\t\t&" + cfile.prefix + "$type,");
		if (m.getParameterCount() > 0) {
			out.println(".param_types=\t\t&"
			    + cfile.prefix + "$param_types$" + C.name(m) + ",");
		}
		out.println(".return_type=\t\t&"
		    + C.jc_type(m.getReturnType()) + ",");
		out.println(".param_ptypes=\t\t&"
		    + cfile.prefix + "$param_ptypes$" + C.name(m) + ",");

		out.println(".signature_hash=\t_JC_JLONG(0x"
		    + Long.toHexString(Util.sigHash(m)) + "),");
		out.println(".access_flags=\t\t" + C.accessDefs(m) + ",");

		// Output more method info stuff
		if (generateBody) {
			out.println(".function=\t\t&"
			    + cfile.prefix + "$method$" + C.name(m) + ",");
		}
		if (!cfile.c.isInterface() && Util.isVirtual(m)) {
			out.println(".vtable_index=\t\t_JC_OFFSETOF(struct "
			    + cfile.prefix + "$vtable, "
			    + C.name(cfile.c) + "." + C.name(m) + ")"
			    + " / sizeof(void *),");
		}
		out.println(".num_parameters=\t"
		    + m.getParameterCount() + ",");
		if (m.getExceptions().size() > 0) {
			out.println(".num_exceptions=\t"
			    + m.getExceptions().size() + ",");
			out.println(".exceptions=\t\t&" + cfile.prefix
			    + "$exceptions$" + C.name(m) + ",");
		}
		if (doTrapTable || doLineNumTable) {
			out.println(".u= { .exec= {");
			if (doTrapTable) {
				out.println("  .trap_table_len=\t"
				    + traps.size() + ",");
				out.println("  .trap_table=\t\t"
				    + cfile.prefix + "$trap_table$" + C.name(m)
				    + ",");
			}
			if (doLineNumTable) {
				out.println("  .u= {");
				out.println("    .linenum= {");
				out.println("      .len=\t"
				    + lineNumberTableLength + ",");
				out.println("      .table=\t"
				    + cfile.prefix + "$linenum_table$"
				    + C.name(m));
				out.println("    },");
				out.println("  },");
			}
			out.println("} },");
		}

		// Done with method info
		out.undent();
		out.println("};");
		out.println();

		// Output line number table
		if (doLineNumTable) {
			out.println("_JC_LINENUM_TABLE("
			    + C.name(cfile.c) + ", " + C.name(m) + ");");
			out.println();
		}
	}

	public void outputMethodFunction() {

		// No body for interface virtual methods
		if (cfile.c.isInterface() && !Util.isClassInit(m))
			return;

		// Analyze method
		if (infoMap == null)
			analyzeBody();

		// Set current method
		CURRENT_METHOD.set(this);

		// Output initial stuff for body
		out.print(C.type(m));
		if (Util.isReference(m))
			out.print(" *");
		out.println(" _JC_JCNI_ATTR");
		out.println(cfile.prefix + "$method$"
		    + C.name(m) + C.paramsDecl(m, true));

		// Output normal, abstract, or native method body
		out.println('{');
		out.indent();
		if (m.isNative())
			outputNativeMethodBody();
		else if (m.isAbstract())
			outputAbstractMethodBody();
		else
			outputNormalMethodBody();
		out.undent();
		out.println('}');
		out.println();

		// Unset current method
		CURRENT_METHOD.set(null);
	}

	/**
	 * Reset state. This frees a bunch of memory.
	 */
	public void reset() {
		m.releaseActiveBody();
		body = null;
		lineNumberTableLength = 0;
		graph = null;
		bodyChain = null;
		infoMap = null;
		traps = null;
	}

	private void outputNormalMethodBody() {

		// Get sorted locals array
		Local[] locary = (Local[])body.getLocals().toArray(
		    new Local[body.getLocals().size()]);
		Arrays.sort(locary, Util.localComparator);

		// Determine which locals need to be volatile
		Set volatileLocals = determineVolatileLocals();

		// Output stack allocated object memory
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			if (!(stmt instanceof AssignStmt))
				continue;
			AssignStmt assign = (AssignStmt)stmt;
			StackAllocTag tag = (StackAllocTag)assign
			    .getRightOpBox().getTag("StackAllocTag");
			if (tag == null)
				continue;
			assign.getRightOp().apply(stackMemorySwitch);
			out.println(" mem" + tag.getId() + ";");
		}

		// Do locals
		for (int i = 0; i < locary.length; i++) {
			Local local = locary[i];
			out.print(C.type(local, true) + " ");
			if (Util.isReference(local.getType()))
				out.print("*");
			if (volatileLocals.contains(local))
				out.print("volatile ");
			out.println(local.getName() + ";");
		}

		// Do exception catcher, if any
		if (!traps.isEmpty())
			out.println("_jc_catch_frame\tcatch;");

		// Blank line
		if (locary.length > 0 || !traps.isEmpty())
			out.println();

		// Determine if we need a stack overflow check
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			if (!stmt.containsInvokeExpr())
				continue;
			if (NullCheckStmt.isNullCheck(stmt))
				continue;
			if (stmt.getInvokeExpr().getMethod().isNative())
				continue;
			out.println("// Check for stack overflow");
			out.println("_JC_STACK_OVERFLOW_CHECK(env);");
			out.println();
			break;
		}

		// Output trap targets, if any
		if (!traps.isEmpty()) {
			out.println("// Define exception targets");
			out.print("_JC_DEFINE_TRAPS(env, catch, &"
			    + cfile.prefix + "$method_info$" + C.name(m));
			for (int i = 0; i < traps.size(); i++) {
				Trap trap = (Trap)traps.get(i);
				out.print(", &&label"
				    + getInfo(trap.getHandlerUnit())
				      .labelIndex);
			}
			out.println(");");
			out.println();
		}

		// Validate body just for good measure
		if (DEBUG)
			body.validate();

		// Iterate through body units and convert to C
		StmtInfo previous = new StmtInfo();
		CStmtSwitch ss = new CStmtSwitch(this);
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();

			// Get info about this statement
			StmtInfo info = (StmtInfo)infoMap.get(stmt);

			// Output label if this statement has one
			if (info.labelIndex != -1) {
				int indent = out.getIndent();
				out.setIndent(0);
				out.println();
				out.println("label" + info.labelIndex + ":");
				out.setIndent(indent);
			}

			// Output region update if needed
			if (info.needsRegionUpdate) {
				out.println("_JC_TRAP_REGION(catch, "
				    + info.region + ");");
			}

			// Output line number table entry if required
			if (info.lineNumberIndex != -1) {
				out.println("_JC_LINE_NUMBER("
				    + (int)info.javaLineNumber + ");\t\t\t\t// "
				    + info.lineNumberIndex);
			}

			// Output check for backward branch trap handlers
			if (info.backwardTrapTarget)
				out.println("_JC_PERIODIC_CHECK(env);");

			// Output statement
			stmt.apply(ss);

			// Update for next go round
			if (info != null)
				previous = info;
		}
	}

	private final AbstractJimpleValueSwitch stackMemorySwitch
	    = new AbstractJimpleValueSwitch() {
		public void caseNewExpr(NewExpr v) {
			RefType t = (RefType)v.getBaseType();
			SootClass sc = t.getSootClass();
			out.println("struct {");
			out.indent();
			out.println("struct _jc_"
			    + C.name(sc) + "$refs\trefs;");
			out.println("struct _jc_"
			    + C.name(sc) + "$object\tobj;");
			out.undent();
			out.print("}");
		}
		public void caseNewArrayExpr(NewArrayExpr v) {
			doArray(v.getSize(), (ArrayType)v.getType());
		}
		public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
			doArray(v.getSize(0), (ArrayType)v.getType());
		}
		public void doArray(Value length, ArrayType atype) {
			int len = ((IntConstant)length).value;
			Type etype = atype.getElementType();
			out.println("struct {");
			out.indent();
			if (Util.isReference(etype)) {
				out.println("_jc_word\t\telems[" + len + "];");
				out.println("_jc_object_array\tarray;");
			} else {
				String tw = Util.typeWord(etype);
				out.println("_jc_" + tw + "_array\tarray;");
				out.println(C.type(etype)
				    + "\telems[" + len + "];");
			}
			out.undent();
			out.print("}");
		}
		public void defaultCase(Object obj) {
			throw new RuntimeException("non-new value");
		}
	};

	private Set determineVolatileLocals() {

		// No traps?
		if (traps.isEmpty())
			return Collections.EMPTY_SET;

		// Get units at the head of each trap
		Set trapHeads = new HashSet();
		for (Iterator i = traps.iterator(); i.hasNext(); )
			trapHeads.add(((Trap)i.next()).getHandlerUnit());

		// Compute follows function
		FollowsAnalysis follows = new FollowsAnalysis(graph);

		// Find locals that could be used after catching an exception
		Set volatileLocals = new HashSet();
		for (Iterator i = bodyChain.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			boolean afterException = false;
			if (trapHeads.contains(stmt))
				afterException = true;
			else {
				for (Iterator j = trapHeads.iterator();
				    j.hasNext(); ) {
					Stmt head = (Stmt)j.next();
					if (follows.canFollow(head, stmt)) {
						afterException = true;
						break;
					}
				}
			}
			if (!afterException)
				continue;
			List uses = stmt.getUseBoxes();
			for (Iterator j = uses.iterator(); j.hasNext(); ) {
				ValueBox useBox = (ValueBox)j.next();
				Value use = useBox.getValue();
				if (use instanceof Local)
					volatileLocals.add(use);
			}
		}

		// Done
		return volatileLocals;
	}

	private void outputNativeMethodBody() {
		out.println("_jc_value retval;");
		out.println();
		//out.println("_JC_STACK_OVERFLOW_CHECK(env);");
		out.print("_JC_INVOKE_NATIVE_METHOD(env, &" + cfile.prefix
		    + "$method_info$" + C.name(m) + ", &retval");
		if (!m.isStatic())
			out.print(", this");
		for (int i = 0; i < m.getParameterCount(); i++)
			out.print(", param" + i);
		out.println(");");
		if (!(m.getReturnType() instanceof VoidType)) {
			out.println("return retval."
			    + Util.typeLetter(m.getReturnType()) + ";");
		}
	}

	private void outputAbstractMethodBody() {
		out.println("_JC_ABSTRACT_METHOD(env, &"
		    + cfile.prefix + "$method_info$" + C.name(m) + ");");
	}

	// Debugging stuff..

	static void dumpBody(SootMethod m, String label) {
		System.out.println("------ " + label + ": "
		    + m.getSubSignature() + " -------");
		JimpleBody body = (JimpleBody)m.retrieveActiveBody();
		Map stmtMap = computeStmtMap(body.getUnits());
		for (Iterator i = body.getUnits().iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			Integer num = (Integer)stmtMap.get(stmt);
			Integer tnum;
			try {
				tnum = (Integer)stmtMap.get(
				    ((UnitBox)stmt.getUnitBoxes().get(0))
				    .getUnit());
			} catch (IndexOutOfBoundsException e) {
				tnum = null;
			}
			System.out.println(num + "\t" + stmt
			    + (tnum != null ? " [" + tnum + "]" : ""));
		}
		if (!body.getTraps().isEmpty()) {
			System.out.println("------ TRAPS -------");
			for (Iterator i = body.getTraps().iterator();
			    i.hasNext(); )
				dumpTrap((Trap)i.next(), stmtMap);
		}
		System.out.println("------ END -------");
	}

	private static void dumpTrap(Trap trap, Map map) {
		System.out.println("\tTrap: ["
		    + map.get(trap.getBeginUnit()) + "] - ["
		    + map.get(trap.getEndUnit()) + "] -> ["
		    + map.get(trap.getHandlerUnit()) + "] "
		    + trap.getException());
	}

	private static Map computeStmtMap(Collection units) {
		HashMap map = new HashMap();
		int count = 0;
		for (Iterator i = units.iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			map.put(stmt, new Integer(++count));
		}
		return map;
	}
}
