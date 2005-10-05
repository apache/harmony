
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
// $Id: CValueSwitch.java,v 1.17 2005/05/02 03:47:48 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.tags.*;
import soot.jimple.toolkits.pointer.CastCheckTag;
import soot.tagkit.*;
import java.util.*;
import org.dellroad.jc.cgen.analysis.ActiveUseTag;
import org.dellroad.jc.cgen.escape.StackAllocTag;

/**
 * Converts Jimple expressions into C expressions.
 */
public class CValueSwitch extends AbstractJimpleValueSwitch {

	/**
	 * The method in which this expression exists.
	 */
	final CMethod method;

	/**
	 * Any tags that were associated with the ValueBox.
	 */
	private final ValueBox valueBox;

	/**
	 * The result of the conversion.
	 */
	CExpr result;

	public CValueSwitch(CMethod method, ValueBox valueBox) {
		this.method = method;
		this.valueBox = valueBox;
	}

	private boolean hasTag(String name) {
		return valueBox != null && valueBox.hasTag(name);
	}

	private Tag getTag(String name) {
		if (valueBox == null)
			return null;
		return valueBox.getTag(name);
	}

	public void caseArrayRef(ArrayRef v) {
		ArrayCheckTag tag = (ArrayCheckTag)getTag("ArrayCheckTag");
		int chklo = (tag == null || tag.isCheckLower()) ? 1 : 0;
		int chkhi = (tag == null || tag.isCheckUpper()) ? 1 : 0;
		Object[] params;
		int pnum = 0;
		if (Util.isReference(v.getType())) {
			params = new Object[6];
			params[pnum++] = "_JC_REF_ELEMENT";
			params[pnum++] = "env";
		} else {
			params = new Object[7];
			params[pnum++] = "_JC_PRIM_ELEMENT";
			params[pnum++] = "env";
			params[pnum++] = Util.typeWord(v.getType());
		}
		params[pnum++] = v.getBaseBox();
		params[pnum++] = v.getIndexBox();
		params[pnum++] = "" + chklo;
		params[pnum++] = "" + chkhi;
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseDoubleConstant(DoubleConstant v) {
		long bits = Double.doubleToRawLongBits(v.value);
		if (bits == 0) {
			result = new CExpr("(jdouble)0.0");
			return;
		}
		Object[] ops = new Object[9];
		ops[0] = "_JC_DCONST";
		for (int i = 0; i < 8; i++) {
			ops[1 + i] = "0x" + Integer.toHexString(
			    (int)(bits >> (8 * (7 - i))) & 0xff);
		}
		result = new CExpr(CExpr.FUNCTION, ops);
	}

	public void caseFloatConstant(FloatConstant v) {
		int bits = Float.floatToRawIntBits(v.value);
		if (bits == 0) {
			result = new CExpr("(jfloat)0.0");
			return;
		}
		Object[] ops = new Object[5];
		ops[0] = "_JC_FCONST";
		for (int i = 0; i < 4; i++) {
			ops[1 + i] = "0x" + Integer.toHexString(
			    (bits >> (8 * (3 - i))) & 0xff);
		}
		result = new CExpr(CExpr.FUNCTION, ops);
	}

	public void caseIntConstant(IntConstant v) {
		result = new CExpr(Integer.toString(v.value));
	}

	public void caseLongConstant(LongConstant v) {
		result = new CExpr(CExpr.FUNCTION,
		    "_JC_JLONG", Long.toString(v.value));
	}

	public void caseNullConstant(NullConstant v) {
		result = new CExpr("_JC_NULL");
	}

	public void caseStringConstant(StringConstant v) {
		if (ClassConstant.isClassConstant(v)) {
			result = new CExpr("&_jc_"
			    + C.encode(ClassConstant.getClassName(v))
			    + "$class_object");
		} else {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_STRING", "env", C.string(v.value));
		}
	}

	// Common code for simple binary ops
	private void caseBinOpExpr(BinopExpr e, int type) {
		result = new CExpr(type, e.getOp1Box(), e.getOp2Box());
	}

	public void caseAddExpr(AddExpr v) {
		caseBinOpExpr(v, CExpr.ADD);
	}

	public void caseAndExpr(AndExpr v) {
		caseBinOpExpr(v, CExpr.AND);
	}

	public void caseCmpExpr(CmpExpr v) {
		result = new CExpr(CExpr.FUNCTION,
		    "_JC_LCMP", v.getOp1Box(), v.getOp2Box());
	}

	public void caseCmpgExpr(CmpgExpr v) {
		if (v.getOp1().getType() instanceof FloatType
		    && v.getOp2().getType() instanceof FloatType) {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_FCMPG", v.getOp1Box(), v.getOp2Box());
		} else {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_DCMPG", v.getOp1Box(), v.getOp2Box());
		}
	}

	public void caseCmplExpr(CmplExpr v) {
		if (v.getOp1().getType() instanceof FloatType
		    && v.getOp2().getType() instanceof FloatType) {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_FCMPL", v.getOp1Box(), v.getOp2Box());
		} else {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_DCMPL", v.getOp1Box(), v.getOp2Box());
		}
	}

	public void caseDivExpr(DivExpr v) {
		caseBinOpExpr(v, CExpr.DIVIDE);
	}

	public void caseEqExpr(EqExpr v) {
		caseBinOpExpr(v, CExpr.EQUAL);
	}

	public void caseGeExpr(GeExpr v) {
		caseBinOpExpr(v, CExpr.GE);
	}

	public void caseGtExpr(GtExpr v) {
		caseBinOpExpr(v, CExpr.GT);
	}

	public void caseLeExpr(LeExpr v) {
		caseBinOpExpr(v, CExpr.LE);
	}

	public void caseLtExpr(LtExpr v) {
		caseBinOpExpr(v, CExpr.LT);
	}

	public void caseMulExpr(MulExpr v) {
		caseBinOpExpr(v, CExpr.MULTIPLY);
	}

	public void caseNeExpr(NeExpr v) {
		caseBinOpExpr(v, CExpr.NOT_EQUAL);
	}

	public void caseOrExpr(OrExpr v) {
		caseBinOpExpr(v, CExpr.OR);
	}

	public void caseRemExpr(RemExpr v) {
		if (v.getType() instanceof DoubleType
		    || v.getType() instanceof FloatType) {
			result = new CExpr(CExpr.FUNCTION,
			    "_jc_cs_fmod", v.getOp1Box(), v.getOp2Box());
			return;
		}
		caseBinOpExpr(v, CExpr.MODULO);
	}

	public void caseShlExpr(ShlExpr v) {
		boolean isLong = (v.getOp1().getType() instanceof LongType);
		result = new CExpr(CExpr.SHIFT_LEFT, v.getOp1Box(),
		   new CExpr(CExpr.AND, v.getOp2Box(),
		     isLong ? "0x3f" : "0x1f"));
	}

	public void caseShrExpr(ShrExpr v) {
		boolean isLong = (v.getOp1().getType() instanceof LongType);
		result = new CExpr(CExpr.FUNCTION,
		    isLong ? "_JC_LSHR" : "_JC_ISHR",
		    v.getOp1Box(), v.getOp2Box());
	}

	public void caseSubExpr(SubExpr v) {
		caseBinOpExpr(v, CExpr.SUBTRACT);
	}

	public void caseUshrExpr(UshrExpr v) {
		boolean isLong = (v.getOp1().getType() instanceof LongType);
		result = new CExpr(CExpr.FUNCTION,
		    isLong ? "_JC_LUSHR" : "_JC_IUSHR",
		    v.getOp1Box(), v.getOp2Box());
	}

	public void caseXorExpr(XorExpr v) {
		caseBinOpExpr(v, CExpr.XOR);
	}

	public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
		final Object[] params = new Object[6 + v.getArgCount()];
		int pnum = 0;
		params[pnum++] = "_JC_INVOKE_INTERFACE";
		params[pnum++] = "env";
		params[pnum++] = v.getBaseBox();
		params[pnum++] = new CExpr(CExpr.FUNCTION, "_JC_JLONG", "0x"
		    + Long.toHexString(Util.sigHash(v.getMethod())));
		params[pnum++] = C.valueType(v.getType());
		params[pnum++] = C.paramsDecl(v.getMethod(), false);
		for (int i = 0; i < v.getArgCount(); i++)
			params[pnum++] = v.getArgBox(i);
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseSpecialInvokeExpr(SpecialInvokeExpr v) {
		SootMethod meth = v.getMethod();
		SootClass specialClass = meth.getDeclaringClass();
		ValueBox instance = v.getBaseBox();

		// Is this a nonvirtual invocation?
		boolean nonvirtual = Util.isNonvirtual(v, method.m);

		// Use the appropriate C macro and parameters
		Object[] params = new Object[5
		    + (nonvirtual ? 1 : 0) + v.getArgCount()];
		int pnum = 0;
		params[pnum++] = nonvirtual ?
		    "_JC_INVOKE_NONVIRTUAL" : "_JC_INVOKE_VIRTUAL";
		params[pnum++] = "env";
		params[pnum++] = specialClass;
		params[pnum++] = meth;
		if (nonvirtual) {
			NullCheckTag tag = (NullCheckTag)getTag("NullCheckTag");
			int nullchk = (tag == null || tag.needCheck()) ? 1 : 0;
			params[pnum++] = "" + nullchk;
		}
		params[pnum++] = instance;
		for (int i = 0; i < v.getArgCount(); i++)
			params[pnum++] = v.getArgBox(i);

		// Get result
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseStaticInvokeExpr(StaticInvokeExpr v) {
		SootMethod meth = v.getMethod();
		SootClass currentClass = method.m.getDeclaringClass();
		SootClass methodClass = meth.getDeclaringClass();
		Object[] params = new Object[5 + v.getArgCount()];
		ActiveUseTag tag = (ActiveUseTag)getTag("ActiveUseTag");
		boolean omitCheck = tag != null && !tag.isCheckNeeded();
		int pnum = 0;
		params[pnum++] = "_JC_INVOKE_STATIC";
		params[pnum++] = "env";
		params[pnum++] = meth.getDeclaringClass();
		params[pnum++] = meth;
		params[pnum++] = omitCheck ? "1" : "0";
		for (int i = 0; i < v.getArgCount(); i++)
			params[pnum++] = v.getArgBox(i);
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
		SootMethod meth = v.getMethod();
		ValueBox instance = v.getBaseBox();

		// Handle case where an interface method is specified
		boolean invokei = meth.getDeclaringClass().isInterface();

		// Construct expression
		Object[] params;
		int pnum = 0;
		if (invokei) {
			params = new Object[6 + v.getArgCount()];
			params[pnum++] = "_JC_INVOKE_INTERFACE";
			params[pnum++] = "env";
			params[pnum++] = instance;
			params[pnum++] = new CExpr(CExpr.FUNCTION, "_JC_JLONG",
			    "0x" + Long.toHexString(Util.sigHash(meth)));
			params[pnum++] = C.valueType(v.getType());
			params[pnum++] = C.paramsDecl(v.getMethod(), false);
		} else {
			params = new Object[5 + v.getArgCount()];
			params[pnum++] = "_JC_INVOKE_VIRTUAL";
			params[pnum++] = "env";
			params[pnum++] = meth.getDeclaringClass();
			params[pnum++] = meth;
			params[pnum++] = instance;
		}
		for (int i = 0; i < v.getArgCount(); i++)
			params[pnum++] = v.getArgBox(i);
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseCastExpr(CastExpr v) {
		final Type ctype = v.getCastType();
		final ValueBox operand = v.getOpBox();
		final Type optype = v.getOp().getType();

		// Can this cast be eliminated?
		CastCheckTag tag = (CastCheckTag)getTag("CastCheckTag");
		if (tag != null && tag.canEliminateCheck()) {
			CValueSwitch sub = new CValueSwitch(method, operand);
			operand.getValue().apply(sub);
			result = (CExpr)sub.result;
			return;
		}

		// Handle primitive casts using equivalent C casts
		if (Util.isPrimitive(ctype)) {

			// Handle floating -> integral casts
			if (!(ctype instanceof FloatType
			      || ctype instanceof DoubleType)
			    && (optype instanceof FloatType
			      || optype instanceof DoubleType)) {
				result = new CExpr(CExpr.FUNCTION,
				    new Object[] { "_JC_CAST_FLT2INT", "env", 
				    C.type(optype), C.type(ctype), operand });
				return;
			}

			// Handle "simple" casts using equivalent C casts
			result = new CExpr(CExpr.CAST, C.type(ctype), operand);
			return;
		}

		// Handle final types using an optimized cast
		if (!Util.hasSubtypes(ctype)) {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_CAST_FINAL", "env", ctype, operand);
			return;
		}

		// Normal cast
		result = new CExpr(CExpr.FUNCTION,
		    "_JC_CAST", "env", ctype, operand);
	}

	public void caseInstanceOfExpr(InstanceOfExpr v) {
		final ValueBox operand = v.getOpBox();
		final Type type = v.getCheckType();

		// Handle any final type using a direct equality test
		if (!Util.hasSubtypes(type)) {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_INSTANCEOF_FINAL", "env", operand, type);
		} else {
			result = new CExpr(CExpr.FUNCTION,
			    "_JC_INSTANCEOF", "env", operand, type);
		}
	}

	public void caseNewArrayExpr(NewArrayExpr v) {
		ArrayType arrayType = (ArrayType)v.getType();
		Type baseType = arrayType.baseType;
		StackAllocTag tag = (StackAllocTag)getTag("StackAllocTag");
		Object[] params = new Object[5 + (tag != null ? 1 : 0)];
		int pnum = 0;
		if (Util.isPrimitive(baseType)) {
			params[pnum++] = tag != null ?
			    "_JC_STACK_NEW_PRIM_ARRAY" : "_JC_NEW_PRIM_ARRAY";
			params[pnum++] = "env";
			if (tag != null)
				params[pnum++] = "&mem" + tag.getId();
			params[pnum++] = Util.typeWord(baseType);
		} else {
			RefType refType = (RefType)baseType;
			params[pnum++] = tag != null ?
			    "_JC_STACK_NEW_REF_ARRAY" : "_JC_NEW_REF_ARRAY";
			params[pnum++] = "env";
			if (tag != null)
				params[pnum++] = "&mem" + tag.getId();
			params[pnum++] = C.name(refType.getSootClass());
		}
		params[pnum++] = String.valueOf(arrayType.numDimensions);
		params[pnum++] = v.getSizeBox();
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
		ArrayType arrayType = (ArrayType)v.getType();
		Type baseType = arrayType.baseType;
		StackAllocTag tag = (StackAllocTag)getTag("StackAllocTag");
		StringBuffer sizes = new StringBuffer();
		boolean zero = false;
		int i;
		for (i = 0; i < v.getSizeCount(); i++) {
			if (i > 0)
				sizes.append(", ");
			sizes.append(C.value(v.getSizeBox(i)));
		}
		Object[] params = new Object[6 + (tag != null ? 1 : 0)];
		int pnum = 0;
		if (Util.isReference(baseType)) {
			params[pnum++] = tag != null ?
			   "_JC_STACK_NEW_REF_MULTIARRAY" :
			   "_JC_NEW_REF_MULTIARRAY"; 
			params[pnum++] = "env";
			if (tag != null)
				params[pnum++] = "&mem" + tag.getId();
			params[pnum++] = ((RefType)baseType).getSootClass();
		} else {
			params[pnum++] = tag != null ?
			    "_JC_STACK_NEW_PRIM_MULTIARRAY" :
			    "_JC_NEW_PRIM_MULTIARRAY";
			params[pnum++] = "env";
			if (tag != null)
				params[pnum++] = "&mem" + tag.getId();
			params[pnum++] = Util.typeWord(baseType);
		}
		params[pnum++] = new Integer(arrayType.numDimensions);
		params[pnum++] = new Integer(v.getSizeCount());
		params[pnum++] = sizes;
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseNewExpr(NewExpr v) {
		StackAllocTag tag = (StackAllocTag)getTag("StackAllocTag");
		result = tag != null ?
		    new CExpr(CExpr.FUNCTION, "_JC_STACK_NEW", "env",
		      "&mem" + tag.getId(), v.getBaseType().getSootClass()) :
		    new CExpr(CExpr.FUNCTION, "_JC_NEW",
		      "env", v.getBaseType().getSootClass());
	}

	public void caseLengthExpr(LengthExpr v) {
		result = new CExpr(CExpr.FUNCTION,
		    "_JC_ARRAY_LENGTH", "env", v.getOpBox());
	}

	public void caseNegExpr(NegExpr v) {
		result = new CExpr(CExpr.NEGATE, v.getOpBox());
	}

	public void caseInstanceFieldRef(InstanceFieldRef v) {
		SootField field = v.getField();
		Object[] params = new Object[5];
		params[0] = Util.isReference(field) ?
		    "_JC_REF_FIELD" : "_JC_PRIM_FIELD";
		params[1] = "env";
		params[2] = v.getBaseBox();
		params[3] = field.getDeclaringClass();
		params[4] = field;
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void caseLocal(Local local) {
		result = new CExpr(local.getName());
	}

	public void caseParameterRef(ParameterRef v) {
		result = new CExpr("param" + v.getIndex());
	}

	public void caseCaughtExceptionRef(CaughtExceptionRef v) {
		result = new CExpr(CExpr.FUNCTION,
		    "_JC_CAUGHT_EXCEPTION", "env");
	}

	public void caseThisRef(ThisRef v) {
		result = new CExpr("this");
	}

	public void caseStaticFieldRef(StaticFieldRef v) {
		SootField field = v.getField();
		SootClass currentClass = method.m.getDeclaringClass();
		SootClass fieldClass = field.getDeclaringClass();
		ActiveUseTag tag = (ActiveUseTag)getTag("ActiveUseTag");
		boolean omitCheck = tag != null && !tag.isCheckNeeded();
		Object[] params = new Object[5];
		int pnum = 0;
		params[pnum++] = "_JC_STATIC_FIELD";
		params[pnum++] = "env";
		params[pnum++] = fieldClass;
		params[pnum++] = field;
		params[pnum++] = omitCheck ? "1" : "0";
		result = new CExpr(CExpr.FUNCTION, params);
	}

	public void defaultCase(Object v) {
		Util.panic("unknown Value " + v);
	}
}

