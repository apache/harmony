
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
// $Id: CExpr.java,v 1.4 2005/01/25 23:12:09 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import soot.jimple.*;
import java.util.*;

/**
 * Class for assembling C language expressions and outputting them
 * as C code.
 *
 * <p>
 * The purpose of this class is to allow us to output arbitrarily
 * complex expressions in C that contain sufficient, but not
 * stupidly unnecessary, parentheses. Note that we do output
 * unnecessary parentheses in those cases where it's deemed
 * to make things clearer.
 * </p>
 */
public class CExpr {

	/* Variables and constants */
	public static final int ATOMIC = 0;

	/* Unary expressions */
	public static final int DEREFERENCE = 1;
	public static final int ADDRESS_OF = 2;
	public static final int LOGICAL_NOT = 3;
	public static final int COMPLEMENT = 4;
	public static final int NEGATE = 5;

	/* Binary expressions */
	public static final int ADD = 6;
	public static final int SUBTRACT = 7;
	public static final int LOGICAL_AND = 8;
	public static final int LOGICAL_OR = 9;
	public static final int AND = 10;
	public static final int OR = 11;
	public static final int XOR = 12;
	public static final int DIVIDE = 13;
	public static final int MULTIPLY = 14;
	public static final int MODULO = 15;
	public static final int SHIFT_LEFT = 16;
	public static final int SHIFT_RIGHT = 17;
	public static final int ARROW = 18;
	public static final int ARRAYINDEX = 19;
	public static final int LT = 20;
	public static final int LE = 21;
	public static final int GT = 22;
	public static final int GE = 23;
	public static final int EQUAL = 24;
	public static final int NOT_EQUAL = 25;
	public static final int CAST = 26;

	/* >= 2 operand expressions */
	public static final int FUNCTION = 27;
	public static final int THREEWAY = 28;

	public static final int NUM_TYPES = 29;

	private final int type;
	private final CExpr ops[];
	private String symbol;
	private String s;

	private static int[] prec;
	private static int[] order;

	static {

		// Partially order operators as to how we want or need to
		// require parentheses around operators. Note that we
		// require them in cases not strictly required for C,
		// e.g., by grouping together LOGICAL_AND and LOGICAL_OR.

		ArrayList a = new ArrayList();
		a.add(new int[] { ATOMIC });
		a.add(new int[] { CAST, ARROW, ARRAYINDEX, FUNCTION });
		a.add(new int[] { DEREFERENCE,
		    NEGATE, ADDRESS_OF, LOGICAL_NOT, COMPLEMENT });
		a.add(new int[] { MULTIPLY, DIVIDE, MODULO,
		    ADD, SUBTRACT, SHIFT_LEFT, SHIFT_RIGHT });
		a.add(new int[] { LT, LE, GT, GE, EQUAL, NOT_EQUAL });
		a.add(new int[] { AND, OR, XOR });
		a.add(new int[] { LOGICAL_AND, LOGICAL_OR });
		a.add(new int[] { THREEWAY });
		int[][] revp = (int [][])a.toArray(new int[a.size()][]);

		// Compute precedence mapping based on partial order
		prec = new int[NUM_TYPES];
		order = new int[NUM_TYPES];
		for (int i = 0; i < revp.length; i++) {
			for (int j = 0; j < revp[i].length; j++) {
				int op = revp[i][j];
				order[op] = revp[i][0];
				prec[op] = i;
			}
		}
	}

	/**
	 * Create an "atomic" C expression containing the given string.
	 */
	public CExpr(String s) {
		this.type = ATOMIC;
		this.ops = null;
		this.s = s;
	}

	/**
	 * Create a function invocation or unary C expression.
	 */
	public CExpr(int type, CExpr op1) {
		this(type, new CExpr[] { op1 });
	}

	/**
	 * Create a function invocation or binary C expression.
	 */
	public CExpr(int type, CExpr op1, CExpr op2) {
		this(type, new CExpr[] { op1, op2 });
	}

	/**
	 * Create a function invocation or ternary C expression.
	 */
	public CExpr(int type, CExpr op1, CExpr op2, CExpr op3) {
		this(type, new CExpr[] { op1, op2, op3 });
	}

	/**
	 * Create a function invocation or unary C expression.
	 */
	public CExpr(int type, Object op1) {
		this(type, new Object[] { op1 });
	}

	/**
	 * Create a function invocation or binary C expression.
	 */
	public CExpr(int type, Object op1, Object op2) {
		this(type, new Object[] { op1, op2 });
	}

	/**
	 * Create a function invocation or ternary C expression.
	 */
	public CExpr(int type, Object op1, Object op2, Object op3) {
		this(type, new Object[] { op1, op2, op3 });
	}

	/**
	 * Create a function invocation.
	 */
	public CExpr(int type, Object op1, Object op2, Object op3, Object op4) {
		this(type, new Object[] { op1, op2, op3, op4 });
	}

	public CExpr(int type, Collection ops) {
		this(type, ops.toArray());
	}

	public CExpr(int type, Object[] ops) {

		// Check arguments
		if (type < 0 || type >= NUM_TYPES)
			throw new IllegalArgumentException("bogus type");
		if (isUnary(type)) {
			if (ops.length != 1)
				throw new IllegalArgumentException("unary");
		} else if (isBinary(type)) {
			if (ops.length != 2)
				throw new IllegalArgumentException("binary");
		} else if (isTernary(type)) {
			if (ops.length != 3)
				throw new IllegalArgumentException("ternary");
		} else {
			if (ops.length < 1)
				throw new IllegalArgumentException("function");
		}
		this.type = type;

		// Convert any non-CExpr arguments to ATOMIC's
		if (ops instanceof CExpr[])
			this.ops = (CExpr[])ops;
		else {
			this.ops = new CExpr[ops.length];
			for (int i = 0; i < ops.length; i++) {
				if (ops[i] instanceof CExpr) {
					this.ops[i] = (CExpr)ops[i];
					continue;
				}
				if (ops[i] instanceof ValueBox) {
					this.ops[i] = C.value((ValueBox)ops[i]);
					continue;
				}
				if (ops[i] instanceof Type) {
					this.ops[i] = new CExpr(ADDRESS_OF,
					    C.jc_type((Type)ops[i]));
					continue;
				}
				if (ops[i] instanceof SootClass) {
					this.ops[i] = new CExpr(
					    C.name((SootClass)ops[i]));
					continue;
				}
				if (ops[i] instanceof SootField) {
					this.ops[i] = new CExpr(
					    C.name((SootField)ops[i]));
					continue;
				}
				if (ops[i] instanceof SootMethod) {
					this.ops[i] = new CExpr(
					    C.name((SootMethod)ops[i]));
					continue;
				}
				if (ops[i] instanceof Value) {
					throw new RuntimeException("Value"
					    + " should be ValueBox: " + ops[i]);
				}
				this.ops[i] = new CExpr(ops[i].toString());
			}
		}

		// Get symbol
		switch (type) {
		case DEREFERENCE:	symbol = "*";	break;
		case ADDRESS_OF:	symbol = "&";	break;
		case LOGICAL_NOT:	symbol = "!";	break;
		case COMPLEMENT:	symbol = "~";	break;
		case NEGATE:		symbol = "-";	break;
		case ADD:		symbol = "+";	break;
		case SUBTRACT:		symbol = "-";	break;
		case LOGICAL_AND:	symbol = "&&";	break;
		case LOGICAL_OR:	symbol = "||";	break;
		case AND:		symbol = "&";	break;
		case OR:		symbol = "|";	break;
		case XOR:		symbol = "^";	break;
		case DIVIDE:		symbol = "/";	break;
		case MULTIPLY:		symbol = "*";	break;
		case MODULO:		symbol = "%";	break;
		case SHIFT_LEFT:	symbol = "<<";	break;
		case SHIFT_RIGHT:	symbol = ">>";	break;
		case ARROW:		symbol = "->";	break;
		case ARRAYINDEX:	symbol = "[]";	break;	// never used
		case LT:		symbol = "<";	break;
		case LE:		symbol = "<=";	break;
		case GT:		symbol = ">";	break;
		case GE:		symbol = ">=";	break;
		case EQUAL:		symbol = "==";	break;
		case NOT_EQUAL:		symbol = "!=";	break;
		case THREEWAY:		symbol = "?:";	break;	// never used
		case CAST:		symbol = "()";	break;	// never used
		default:				break;
		}
	}

	public static boolean isUnary(int type) {
		return (type >= DEREFERENCE && type <= NEGATE);
	}

	public static boolean isBinary(int type) {
		return (type >= ADD && type <= CAST);
	}

	public static boolean isTernary(int type) {
		return (type == THREEWAY);
	}

	private void gen() {
		StringBuffer b = new StringBuffer();

		// Handle special cases
		switch (type) {
		case FUNCTION:
			addOp(b, 0);
			b.append('(');
			for (int i = 1; i < ops.length; i++) {
				if (i > 1)
					b.append(", ");
				b.append(ops[i]);
			}
			b.append(')');
			break;
		case ARRAYINDEX:
			addOp(b, 0);
			b.append('[');
			b.append(ops[1]);
			b.append(']');
			break;
		case THREEWAY:
			addOp(b, 0);
			b.append(" ? ");
			addOp(b, 1);
			b.append(" : ");
			addOp(b, 2);
			break;
		case CAST:
			b.append('(');
			addOp(b, 0);
			b.append(')');
			addOp(b, 1);
			break;
		default:
			if (isUnary(type)) {
				b.append(symbol);
				addOp(b, 0);
			} else {
				addOp(b, 0);
				if (type != ARROW)
					b.append(' ');
				b.append(symbol);
				if (type != ARROW)
					b.append(' ');
				addOp(b, 1);
			}
			break;
		}

		// Done
		s = b.toString();
	}

	private void addOp(StringBuffer b, int op) {

		// Determine whether to parenthesize
		boolean parens = false;
		switch (ops[op].type) {
		case ATOMIC:
			parens = false;
			break;
		case AND:
		case OR:
		case XOR:
			parens = true;
			break;
		default:
			if (prec[ops[op].type] >= prec[type])
				parens = true;
			if (prec[ops[op].type] == prec[type]) {
				if (isUnary(type))
					parens = false;
				if ((type == FUNCTION || type == CAST
				      || type == ARRAYINDEX || type == ARROW)
				    && op == 0)
					parens = false;
			}
			if ((type == AND || type == OR || type == XOR)
			    && !isUnary(ops[op].type))
				parens = true;
			if (type == CAST && op == 0)
				parens = false;
			break;
		}
		if (type == THREEWAY && op > 0 && ops[op].type != ATOMIC)
			parens = true;
		if (parens)
			b.append('(');
		b.append(ops[op]);
		if (parens)
			b.append(')');
	}

	/**
	 * Return valid C code that expresses this C expression.
	 */
	public String toString() {
		if (s == null)
			gen();
		return s;
	}

	private static int depth;

	/**
	 * Generate a random C expression. Used for testing.
	 */
	public static CExpr random(Random r) {
		int type;
		CExpr e;

		depth++;
		if (depth >= 5)
			type = ATOMIC;
		else
			type = r.nextInt(NUM_TYPES);
		switch (type) {
		case FUNCTION:
			CExpr[] params = new CExpr[r.nextInt(4) + 1];
			for (int i = 0; i < params.length; i++)
				params[i] = random(r);
			e = new CExpr(type, params);
			break;
		case ATOMIC:
			e = new CExpr("x");
			break;
		default:
			if (isUnary(type))
				e = new CExpr(type, new CExpr[] { random(r) });
			else if (isBinary(type)) {
				e = new CExpr(type,
				    new CExpr[] { random(r), random(r) });
			} else if (isTernary(type)) {
				e = new CExpr(type, new CExpr[] { random(r),
				    random(r), random(r) });
			} else {
				Util.panic("impossible flow");
				e = null;
			}
			break;
		}
		depth--;
		return e;
	}

	public void dumpOps(int depth) {
		for (int i = 0; i < ops.length; i++) {
			for (int j = 0; j < depth; j++)
				System.out.print("  ");
			System.out.println(i + ": " + ops[i]);
			if (ops[i].type != ATOMIC)
				ops[i].dumpOps(depth + 1);
		}
	}

	/** 
	 * Test method.
	 */
	public static void main(String[] args) {
		CExpr e = random(new Random());
		e.dumpOps(0);
		System.out.println(e);
	}
}

