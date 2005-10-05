
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
// $Id: DefaultMethodOptimizer.java,v 1.24 2005/03/18 14:16:13 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.util.*;
import org.dellroad.jc.cgen.escape.EscapeAnalysis;
import org.dellroad.jc.cgen.analysis.TypeAnalysis;
import soot.*;
import soot.grimp.*;
import soot.jimple.*;
import soot.tagkit.*;
import soot.toolkits.scalar.*;
import soot.jimple.toolkits.invoke.*;
import soot.toolkits.graph.CompleteUnitGraph;

/**
 * Method optimizer that does a few things beyond the normal Soot stuff:
 *
 * <ul>
 * <li>Convert virtual method invocations to nonvirtual invocations
 *	when the target method is final.</li>
 * <li>Inline methods suitable for inlining (i.e., short enough,
 *	nonvirtual invocation, etc.).</li>
 * </ul>
 *
 * These are local optimizations only (not using a global call graph).
 */
public class DefaultMethodOptimizer implements MethodOptimizer {

	// Default inlining parameters copied from Soot
	public static final double MAX_EXPANSION
	    = Double.parseDouble(System.getProperty(
	      "jc.gen.inline.max.expansion", "2.5"));

	// Maximum size we allow calling method to grow to
	public static final int MAX_CALLER_SIZE
	    = Integer.parseInt(System.getProperty(
	      "jc.gen.inline.max.caller", "5000"));

	// Maximum size of called method we'll inline
	public static final int MAX_CALLEE_SIZE
	    = Integer.parseInt(System.getProperty(
	      "jc.gen.inline.max.callee", "12"));

	// If below these sizes, don't worry about overflowing MAX_EXPANSION
	public static final int MIN_CALLER_SIZE
	    = Integer.parseInt(System.getProperty(
	      "jc.gen.inline.min.caller", "20"));
	public static final int MIN_CALLEE_SIZE
	    = Integer.parseInt(System.getProperty(
	      "jc.gen.inline.min.callee", "3"));

	// Maximum size of stack-allocated objects
	public static final int MAX_STACK_ALLOC
	    = Integer.parseInt(System.getProperty(
	      "jc.gen.max.stack.alloc", "1024"));

	// Whether to print inlined methods
	public static final boolean INLINE_VERBOSE
	    = Boolean.getBoolean("jc.gen.inline.verbose");

	// Represents one potential inline site
	private static class Inline {

		final SootMethod caller;
		final SootMethod callee;
		final InvokeExpr expr;
		final Stmt stmt;
		final int calleeSize;

		public Inline(SootMethod caller, Stmt stmt) {
			this.stmt = stmt;
			this.callee = stmt.getInvokeExpr().getMethod();
			this.caller = caller;
			this.expr = stmt.getInvokeExpr();

			// Compute callee size, not counting IdentityStmt's
			int size = 0;
			for (Iterator i = ((JimpleBody)callee
			      .retrieveActiveBody()).getUnits().iterator();
			    i.hasNext(); ) {
				Stmt calleeStmt = (Stmt)i.next();
				if (!(calleeStmt instanceof IdentityStmt))
					size++;
			}
			this.calleeSize = size;
		}

		// Check size parameters
		public boolean tooBig(int originalSize) {

			// Get current size of caller method
			int callerSize = ((JimpleBody)caller
			    .retrieveActiveBody()).getUnits().size();

			// Impose absolute maximum caller can grow to
			if (callerSize + calleeSize > MAX_CALLER_SIZE)
				return true;

			// Below certain thresholds, we don't care
			if (calleeSize <= MIN_CALLEE_SIZE)
				return false;

			// Don't inline large methods
			if (calleeSize > MAX_CALLEE_SIZE)
				return true;

			// Below certain thresholds, we don't care
			if (callerSize <= MIN_CALLER_SIZE)
				return false;

			// Limit the blowup of the calling method
			if (callerSize + calleeSize
			    > originalSize * MAX_EXPANSION)
				return true;

			// It's OK
			return false;
		}

		public SootClass getStaticInvokeClass() {
			if (!(expr instanceof StaticInvokeExpr))
				return null;
			return expr.getMethod().getDeclaringClass();
		}
	}

	public CompleteUnitGraph optimize(SootMethod method, Set deps) {

		// Get body and units
		JimpleBody body = (JimpleBody)method.retrieveActiveBody();
		PatchingChain units = body.getUnits();

		//
		// Look for this pattern:
		//
		// 	if (foo == null)
		//		throw new NullPointerException();
		//
		// and replace with NullCheckStmt(foo);
		//
		optimizeExplicitNullChecks(body);

		// Record original length of this method
		int originalSize = units.size();

		// Current flow graph
		CompleteUnitGraph graph;

		// Keep inlining until we can't anymore
		while (true) {

			// Apply the standard Soot jimple optimizations
			PackManager.v().getPack("jop").apply(body);

			// Work around Soot bug
			Stmt first = (Stmt)units.getFirst();
			Stmt nop = Jimple.v().newNopStmt();
			units.insertBefore(nop, first);
			nop.redirectJumpsToThisTo(first);

			// (Re)compute flow graph
			graph = new CompleteUnitGraph(body);

			// (Re)compute type analysis
			TypeAnalysis typeAnalysis = new TypeAnalysis(
			    new SimpleLocalDefs(graph));

			// Debug
			//CMethod.dumpBody(method, "BEFORE NEXT INLINING");

			// Convert virtual -> nonvirtual
			nonvirtualize(body, typeAnalysis);

			// Find the best inlinable invocation site
			Inline inline = null;
			for (Iterator i = units.iterator(); i.hasNext(); ) {

				// Must be INVOKESTATIC or INVOKESPECIAL
				Stmt stmt = (Stmt)i.next();
				if (!stmt.containsInvokeExpr())
					continue;

				// Check invocation for basic suitability
				if (!safeToInline(method, stmt))
					continue;
				Inline candidate = new Inline(method, stmt);
				if (candidate.tooBig(originalSize))
					continue;

				// Save the shortest inline
				if (inline == null
				    || candidate.calleeSize < inline.calleeSize)
					inline = candidate;
			}

			// Anything to do?
			if (inline == null)
				break;

			// Grab line number tag, if any
			LineNumberTag lineNumberTag = (LineNumberTag)
			    inline.stmt.getTag("LineNumberTag");

			// Add a null pointer check if invocation is non-static
			if (!inline.callee.getName().equals("<init>")
			    && inline.expr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iexpr
				    = (InstanceInvokeExpr)inline.expr;
				NullCheckStmt.insertNullCheck(units,
				    inline.stmt, iexpr.getBase());
			}

			// Verbosity
			if (INLINE_VERBOSE) {
				System.out.println("Inlining "
				    + inline.caller.getDeclaringClass()
				      .getName()
				    + "." + Util.fullSignature(inline.caller)
				    + " <-- "
				    + inline.callee.getDeclaringClass()
				      .getName()
				    + "." + Util.fullSignature(inline.callee));
			}

			// Remember statement just before the inline site
			Stmt pred = (Stmt)units.getPredOf(inline.stmt);

			// Inline it
			SiteInliner.inlineSite(inline.callee,
			    inline.stmt, inline.caller, Collections.EMPTY_MAP);

			// Get first statement of inlined method
			Stmt inlineStart = (Stmt)(pred != null ?
			    units.getSuccOf(pred) : units.getFirst());

			// Copy line number tag, if any
			if (lineNumberTag != null)
				inlineStart.addTag(lineNumberTag);

			// Add an active use check if method is static
			SootClass cl = inline.getStaticInvokeClass();
			if (cl != null) {
				ActiveUseCheckStmt.insertActiveUseCheck(units,
				    inlineStart, cl);
			}

			// Record a dependency on the inlined class
			deps.add(inline.callee.getDeclaringClass());
		}

		// Debug
		//CMethod.dumpBody(method, "BEFORE TAG ANNOTATION");

		// Do tag annotations
		PackManager.v().getPack("jap").apply(body);

		// Do escape analysis
		EscapeAnalysis.analyze(graph, MAX_STACK_ALLOC);

	    	// Return optimized body
		return graph;
	}

	// Basic check for inline saftey without regards to size blowup
	private boolean safeToInline(SootMethod callingMethod, Stmt stmt) {

		// Get info
		InvokeExpr invoke = stmt.getInvokeExpr();
		SootMethod calledMethod = invoke.getMethod();
		SootClass callingClass = callingMethod.getDeclaringClass();
		SootClass calledClass = calledMethod.getDeclaringClass();

		// Avoid inlining our various hacks
		if (NullCheckStmt.isNullCheck(stmt)
		    || ActiveUseCheckStmt.isActiveUseCheck(stmt))
			return false;

		// Avoid inlining recursive invocations
		if (callingMethod.equals(calledMethod))
			return false;

		// Avoid inlining when base is "null" to workaround problem
		// where gcc -O2 optimizes away code like this:
		//	((Object)null).hashCode();
		// Besides, what's the point, we know it will segfault
		if (invoke instanceof InstanceInvokeExpr
		    && ((InstanceInvokeExpr)invoke).getBase()
		      instanceof NullConstant)
			return false;

		//
		// Avoid inlining synchronized methods, due to the following
		// issues:
		//
		// - When Soot inlines static ones, it adds code to invoke
		//   Class.forName() which uses a static field, and we don't
		//   handle that (a previously generated header file would
		//   not declare the new field). 
		//
		// - We add explicit synchronization wrapping statements to
		//   the Jimple for synchronized methods, and we don't want
		//   to track whether this modification has been done yet.
		//
		// - When both methods are in the same class, both static
		//   or non-static, and both synchronized, we want to remove
		//   the "inner" synchronization as an optimization.
		//
		// - In any case, it's not clear that inlining is even much
		//   of a win in light of all the synchronization overhead.
		//
		if (calledMethod.isSynchronized())
			return false;

		// The method invocation must be to a known, fixed method
		if (!(invoke instanceof StaticInvokeExpr
		    || (invoke instanceof SpecialInvokeExpr
		      && Util.isNonvirtual((SpecialInvokeExpr)invoke,
		       callingMethod))))
			return false;

		// The target method must not be native, etc.
		if (!calledMethod.isConcrete())
			return false;

		//
		// Any INVOKESPECIAL's in the called method must be nonvirtual
		// in both classes, or else the two classes must be the same.
		// Also, don't inline anything calling VMStackWalker methods.
		//
		for (Iterator i = ((JimpleBody)calledMethod
		    .retrieveActiveBody()).getUnits().iterator();
		    i.hasNext(); ) {

			// Check invoke Stmt's
			Stmt calleeStmt = (Stmt)i.next();
			if (!calleeStmt.containsInvokeExpr())
				continue;
			InvokeExpr expr = calleeStmt.getInvokeExpr();

			// Avoid VMStackWalker
			if (expr.getMethod().getDeclaringClass().getName()
			    .equals("gnu.classpath.VMStackWalker"))
				return false;

			// Check INVOKESPECIALs
			if (!(expr instanceof SpecialInvokeExpr))
				continue;
			SpecialInvokeExpr innerInvoke = (SpecialInvokeExpr)expr;
			if ((!Util.isNonvirtual(innerInvoke, callingMethod)
			      || !Util.isNonvirtual(innerInvoke, callingMethod))
			    && !callingClass.equals(calledClass))
				return false;
		}
		return true;
	}

	// Look for virtual and interface invocations that we can
	// convert into nonvirtual invocations.
	private void nonvirtualize(JimpleBody body, TypeAnalysis typeAnalysis) {
		for (Iterator i = body.getUnits().iterator(); i.hasNext(); ) {

			// Look for method invocations
			Stmt stmt = (Stmt)i.next();
			if (!stmt.containsInvokeExpr())
				continue;
			InvokeExpr expr = stmt.getInvokeExpr();

			// Try to nonvirtualize
			SootMethod actualMethod = null;
			if (expr instanceof VirtualInvokeExpr) {
				actualMethod = nonvirtualizeVirtual(stmt,
				    (VirtualInvokeExpr)expr, typeAnalysis);
			} else if (expr instanceof InterfaceInvokeExpr) {
				actualMethod = nonvirtualizeInterface(stmt,
				    (InterfaceInvokeExpr)expr, typeAnalysis);
			}

			// Sanity check
			if (actualMethod != null
			    && actualMethod.getDeclaringClass().isInterface())
				throw new RuntimeException("oops");

			// Replace invocation with INVOKESPECIAL
			if (actualMethod != null) {
				InstanceInvokeExpr invoke
				    = (InstanceInvokeExpr)expr;
				Local base = (Local)invoke.getBase();
				stmt.getInvokeExprBox().setValue(
				    Jimple.v().newSpecialInvokeExpr(
				      base, actualMethod, invoke.getArgs()));
			}
		}
	}

	// Try to nonvirtualize an INVOKEVIRTUAL
	private SootMethod nonvirtualizeVirtual(Stmt stmt,
	    VirtualInvokeExpr invoke, TypeAnalysis typeAnalysis) {

		// Check for final methods
		SootMethod method = invoke.getMethod();
		if (Util.isFinal(method)
		    || Util.isFinal(method.getDeclaringClass()))
			return method;

		// Check if exact type is known
		SootClass c = null;
		RefLikeType type =  typeAnalysis.getExactType(
		    (Local)invoke.getBase(), stmt);
		if (type instanceof RefType)
			c = ((RefType)type).getSootClass();
//		else if (type instanceof ArrayType)
//			c = Scene.v().getSootClass("java.lang.Object");
		if (c != null) {
			return c.getMethod(method.getName(),
			    method.getParameterTypes(),
			    method.getReturnType());
		}
		return null;
	}

	// Try to nonvirtualize an INVOKEINTERFACE
	private SootMethod nonvirtualizeInterface(Stmt stmt,
	    InterfaceInvokeExpr invoke, TypeAnalysis typeAnalysis) {

		// Check if exact type is known
		SootClass c = null;
		RefLikeType type = typeAnalysis.getExactType(
		    (Local)invoke.getBase(), stmt);
		if (type instanceof RefType)
			c = ((RefType)type).getSootClass();
//		else if (type instanceof ArrayType)
//			c = Scene.v().getSootClass("java.lang.Object");
		if (c != null) {
			SootMethod method = invoke.getMethod();
			return c.getMethod(method.getName(),
			    method.getParameterTypes(),
			    method.getReturnType());
		}
		return null;
	}

	//
	// We look for this pattern:
	//
	//	if ($expr != null)
	//		goto label1;
	//	$local = new NullPointerException();
	//	invokespecial NullPointerException.<init>
	//	throw $local
	//  label1:
	//
	// and replace with a more efficient explicit null pointer check
	//
	private void optimizeExplicitNullChecks(JimpleBody body) {
		PatchingChain units = body.getUnits();
		for (Iterator i = units.snapshotIterator(); i.hasNext(); ) {

			// Look for the pattern
			Value value;
			IfStmt ifStmt;
			AssignStmt newStmt;
			InvokeStmt invokeStmt;
			ThrowStmt throwStmt;
			try {
				// Decode if (v != null) statement
				ifStmt = (IfStmt)i.next();
				Stmt ifTarget = ifStmt.getTarget();
				NeExpr ifNeExpr = (NeExpr)ifStmt.getCondition();
				Value op1 = ifNeExpr.getOp1();
				Value op2 = ifNeExpr.getOp2();
				if (op1 instanceof NullConstant)
					value = op2;
				else if (op2 instanceof NullConstant)
					value = op1;
				else
					continue;

				// Decode x = new NullPointerException
				newStmt = (AssignStmt)i.next();
				Local exception = (Local)newStmt.getLeftOp();
				NewExpr newExpr = (NewExpr)newStmt.getRightOp();
				SootClass npeClass
				    = newExpr.getBaseType().getSootClass();
				if (!npeClass.getName().equals(
				    "java.lang.NullPointerException"))
				    	continue;

				// Decode x.<init> invocation
				invokeStmt = (InvokeStmt)i.next();
				SpecialInvokeExpr invokeExpr
				    = (SpecialInvokeExpr)invokeStmt
				      .getInvokeExpr();
				if (invokeExpr.getBase() != exception)
					continue;
				SootMethod init = invokeExpr.getMethod();
				if (init.getDeclaringClass() != npeClass)
					continue;
				if (!init.getName().equals("<init>")
				    || invokeExpr.getArgCount() != 0)
				    	continue;

				// Decode throw x statement
				throwStmt = (ThrowStmt)i.next();
				if (throwStmt.getOp() != exception)
					continue;

				// The next statement must be the != target
				if (i.next() != ifTarget)
					continue;
			} catch (ClassCastException e) {
				continue;
			}

			// There must be no branches into the statements
			if (!newStmt.getBoxesPointingToThis().isEmpty())
				continue;
			if (!invokeStmt.getBoxesPointingToThis().isEmpty())
				continue;
			if (!throwStmt.getBoxesPointingToThis().isEmpty())
				continue;

			// Replace statements with explicit null check
			NullCheckStmt.insertNullCheck(units, ifStmt, value);
			units.remove(ifStmt);
			units.remove(newStmt);
			units.remove(invokeStmt);
			units.remove(throwStmt);
		}
	}
}


