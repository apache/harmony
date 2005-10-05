
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
// $Id: EscapeAnalysis.java,v 1.6 2005/02/20 21:14:31 archiecobbs Exp $
//

package org.dellroad.jc.cgen.escape;

import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.scalar.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
import org.dellroad.jc.cgen.analysis.ReferenceDetector;
import org.dellroad.jc.cgen.analysis.FinalizeDetector;
import org.dellroad.jc.cgen.analysis.FollowsAnalysis;

/**
 * Class that performs escape analysis to determine objects that
 * may be allocated on the stack (instead of the heap).
 */
public class EscapeAnalysis {

	private static final boolean DEBUG = false;

	private EscapeAnalysis() {
	}

	/**
	 *
	 * Find allocations of non-escaping objects. Our requirements:
	 *
	 * <ol>
	 * <li>Object must not be thrown, returned, assigned to any field,
	 *    assigned to any array element, or passed to any method.
	 *    MONITORENTER is allowed, hence these must be balanced.</li>
	 * <li>Allocation size must be known a priori.</li>
	 * <li>Object must not override <code>finalize()</code>.</li>
	 * <li>Object must not subclass
	 *    <code>java.lang.ref.Reference</code>.</li>
	 * <li>The allocation must not be within a loop</li>
	 * </ol>
	 *
	 * <p>
	 * We impose a maximum total size for all stack allocations in the
	 * method. We prioritize allocations by size (smallest first).
	 * </p>
	 *
	 * <p>
	 * All allocations that may be stack allocated are tagged with
	 * a {@link StackAllocTag StackAllocTag}.
	 * </p>
	 *
	 * @param graph graph computed from method
	 * @param maxAlloc max number of bytes of stack allocated objects
	 */
	public static void analyze(CompleteUnitGraph graph, int maxAlloc) {

		// Avoid useless work if stack allocation is disabled
		if (maxAlloc == 0)
			return;

		// Get method body
		Body body = graph.getBody();

		// Debug
		HashMap indexMap = null;
		if (DEBUG) {
			System.out.println("NEW ESCAPE ANALYSIS");
			indexMap = new HashMap();
			int index = 0;
			for (Iterator i = body.getUnits().iterator();
			    i.hasNext(); ) {
				Stmt stmt = (Stmt)i.next();
			    	indexMap.put(stmt, new Integer(++index));
				System.out.println("  [" + index + "] " + stmt);
			}
		}

		// Our stack allocatable 'new' statements
		HashSet stackAllocs = new HashSet();

		// Find all new statements with a known size
		KnownSizeDetector knownSizeDetector = new KnownSizeDetector();
		for (Iterator i = body.getUnits().iterator(); i.hasNext(); ) {
			Stmt stmt = (Stmt)i.next();
			if (!(stmt instanceof AssignStmt))
				continue;
			AssignStmt astmt = (AssignStmt)stmt;
			if (!(astmt.getLeftOp() instanceof Local))
				continue;
			astmt.getRightOp().apply(knownSizeDetector);
			Integer size = (Integer)knownSizeDetector.getResult();
			if (size != null)
				stackAllocs.add(new StackAlloc(stmt));
		}

		// Debug
		if (DEBUG) {
			System.out.println(stackAllocs.size()
			    + " FIXED SIZE ALLOCATIONS");
			for (Iterator i = stackAllocs.iterator();
			    i.hasNext(); ) {
				StackAlloc alloc = (StackAlloc)i.next();
				System.out.println("  ["
				    + indexMap.get(alloc.stmt) + "] "
				    + alloc.stmt);
			}
		}

		// Eliminate new expressions of objects that override finalize()		// or that are instances of java.lang.ref.Reference.
		FinalizeDetector finalizeDetector = new FinalizeDetector();
		ReferenceDetector referenceDetector = new ReferenceDetector();
		for (Iterator i = stackAllocs.iterator(); i.hasNext(); ) {
			StackAlloc alloc = (StackAlloc)i.next();
			alloc.value.apply(finalizeDetector);
			if (((Boolean)finalizeDetector.getResult())
			    .booleanValue()) {
			    	if (DEBUG) {
					System.out.println("FINALIZE: ["
					    + indexMap.get(alloc.stmt) + "] "
					    + alloc.stmt);
				}
				i.remove();
			}
			alloc.value.apply(referenceDetector);
			if (((Boolean)referenceDetector.getResult())
			    .booleanValue()) {
			    	if (DEBUG) {
					System.out.println("REFERENCE: ["
					    + indexMap.get(alloc.stmt) + "] "
					    + alloc.stmt);
				}
				i.remove();
			}
		}

		// Do follows flow analysis to eliminate looped allocations
		FollowsAnalysis follows = new FollowsAnalysis(body);
		for (Iterator i = stackAllocs.iterator(); i.hasNext(); ) {
			StackAlloc alloc = (StackAlloc)i.next();
			if (follows.canFollow(alloc.stmt, alloc.stmt)) {
			    	if (DEBUG) {
					System.out.println("LOOPED: ["
					    + indexMap.get(alloc.stmt) + "] "
					    + alloc.stmt);
				}
				i.remove();
			}
		}

		// If there are no more candidates, bail out early
		if (stackAllocs.isEmpty())
			return;

		// Compute uses of each local
		SimpleLocalUses localUses = new SimpleLocalUses(graph,
		    new SimpleLocalDefs(graph));

		// Initialize set of defs with known escape status
		HashMap knowns = new HashMap();

		// Initialize set of defs with unknown escape status
		HashSet unknowns = new HashSet();
		for (Iterator i = stackAllocs.iterator(); i.hasNext(); ) {
			StackAlloc alloc = (StackAlloc)i.next();
			unknowns.add(alloc.stmt);
		}

		// Keep eliminating unknowns until we're done
		EscapingStmtSwitch ess = new EscapingStmtSwitch();
		while (!unknowns.isEmpty()) {

			// Debug
			if (DEBUG) {
				System.out.println(unknowns.size()
				    + " remaining unknowns...");
				for (Iterator i = unknowns.iterator();
				    i.hasNext(); ) {
					Stmt stmt = (Stmt)i.next();
					System.out.println("  ["
					    + indexMap.get(stmt) + "] " + stmt);
				}
				System.out.println("------------------------");
			}

			// Detect forward progress
			boolean doMore = false;

			// For each unknown def, check for escape
		    unknown_loop:
			for (Iterator i = ((HashSet)unknowns.clone())
			    .iterator(); i.hasNext(); ) {

				// Get next unknown def statment
				AssignStmt stmt = (AssignStmt)i.next();
				Local defLocal = (Local)stmt.getLeftOp();

				// Look for uses that themselves escape
				boolean anyUknowns = false;
				for (Iterator j = localUses.getUsesOf(stmt)
				    .iterator(); j.hasNext(); ) {

					// Get next use of the local
					UnitValueBoxPair up
					    = (UnitValueBoxPair)j.next();
					Stmt use = (Stmt)up.getUnit();

					// Test if local escapes via this use
					ess.setLocal(defLocal);
					use.apply(ess);
					Object result = ess.getResult();

					// Only escapes maybe via this use?
					// If so, and the escape of the use
					// is unknown, then add the use to
					// the set of unknowns we need to check
					if (result == null) {
						result = knowns.get(use);
						if (result == null) {
							if (DEBUG) {
							    System.out.println(
							      "  UNKNOWN USE ["
							      + indexMap.get(
							        use) + "]");
							}
							unknowns.add(use);
							anyUknowns = true;
							doMore = true;
							continue;
						}
					}

					// Does local escape via this use?
					if (result == Boolean.TRUE) {
						if (DEBUG) {
						    System.out.println("["
						        + indexMap.get(stmt)
							+ "] ESCAPES via ["
						        + indexMap.get(use)
							+ "]");
						}
						unknowns.remove(stmt);
						knowns.put(stmt, result);
						doMore = true;
						continue unknown_loop;
					}

					// Defintely does not escape this use
					if (result != Boolean.FALSE)
						throw new RuntimeException();
				}

				// Are all uses non-escaping?
				if (!anyUknowns) {
					if (DEBUG) {
					    System.out.println("["
					        + indexMap.get(stmt)
						+ "] NO ESCAPE");
					}
					unknowns.remove(stmt);
					knowns.put(stmt, Boolean.FALSE);
					doMore = true;
					continue;
				}

				// This def is still unknown
				if (DEBUG) {
				    System.out.println("["
					+ indexMap.get(stmt)
					+ "] STILL UNKNOWN");
				}
			}

			// See if we're no longer making progress
			if (!doMore) {
				if (DEBUG)
				    System.out.println("NO FURTHER PROGRESS");
				for (Iterator i = unknowns.iterator();
				    i.hasNext(); ) {
					AssignStmt stmt = (AssignStmt)i.next();
					knowns.put(stmt, Boolean.FALSE);
				}
				unknowns.clear();
			}
		}

		// Now eliminate allocations whose locals can escape
		for (Iterator i = stackAllocs.iterator(); i.hasNext(); ) {
			AssignStmt stmt = ((StackAlloc)i.next()).stmt;
			if (((Boolean)knowns.get(stmt)).booleanValue())
				i.remove();
		}

		// Debug
		if (DEBUG) {
			System.out.println(stackAllocs.size()
			    + " STACK ALLOCATABLES");
			for (Iterator i = stackAllocs.iterator();
			    i.hasNext(); ) {
				StackAlloc alloc = (StackAlloc)i.next();
				System.out.println("  ["
				    + indexMap.get(alloc.stmt) + "] "
				    + alloc.stmt);
			}
		}

		// Sort the remaining allocations by size
		StackAlloc[] allocs = (StackAlloc[])stackAllocs.toArray(
		    new StackAlloc[stackAllocs.size()]);
		Arrays.sort(allocs, new Comparator() {
			public int compare(Object o1, Object o2) {
				StackAlloc a1 = (StackAlloc)o1;
				StackAlloc a2 = (StackAlloc)o2;
				return a1.size < a2.size ? -1
				    : a1.size > a2.size ? 1 : 0;
			}
		});

		// Include as many as we can, up to maximum total size
		int total = 0;
		HashSet stmts = new HashSet();
		for (int i = 0; i < allocs.length; i++) {
			StackAlloc alloc = allocs[i];
			if (total + alloc.cost <= maxAlloc) {
				alloc.stmt.getRightOpBox().addTag(
				    new StackAllocTag(i));
				total += alloc.cost;
			} else
				break;
		}
	}
}


