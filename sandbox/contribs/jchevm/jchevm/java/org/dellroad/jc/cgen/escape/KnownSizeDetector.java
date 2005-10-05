
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
// $Id: KnownSizeDetector.java,v 1.1 2004/11/27 23:11:04 archiecobbs Exp $
//

package org.dellroad.jc.cgen.escape;

import java.util.*;
import org.dellroad.jc.cgen.*;
import soot.*;
import soot.jimple.*;

/**
 * Instances of this class detect 'new' expressions with a known
 * instance size. The result is an <code>Integer</code> with the
 * estimated size, or <code>null</code> if the type of the 'new'
 * expression does not have a fixed size (or the value is not
 * some kind of 'new' expression).
 */
public class KnownSizeDetector extends AbstractJimpleValueSwitch {

	public static final int REFERENCE_SIZE = 4;
	public static final int OBJECT_HEADER_SIZE = 8;

	public void caseNewExpr(NewExpr v) {
		setResult(new Integer(size(v.getBaseType())));
	}

	public void caseNewArrayExpr(NewArrayExpr v) {
		doArray(v.getSize(), (ArrayType)v.getType());
	}

	public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
		doArray(v.getSize(0), (ArrayType)v.getType());
	}

	private void doArray(Value length, ArrayType atype) {
		if (!(length instanceof IntConstant)) {
			setResult(null);
			return;
		}
		Type etype = atype.getElementType();
		int size = etype instanceof RefLikeType ?
		    REFERENCE_SIZE : size(etype);
		setResult(new Integer(((IntConstant)length).value * size));
	}

	public void defaultCase(Object obj) {
		setResult(null);
	}

	/**
	 * Computes the size in bytes of an instance of a type.
	 *
	 * @see KnownSizeDetector#size KnownSizeDetector.size()
	 */
	public static class SizeTypeSwitch extends TypeSwitch {
	    public void caseBooleanType(BooleanType t) {
		setResult(new Integer(1));
	    }
	    public void caseByteType(ByteType t) {
		setResult(new Integer(1));
	    }
	    public void caseCharType(CharType t) {
		setResult(new Integer(2));
	    }
	    public void caseShortType(ShortType t) {
		setResult(new Integer(2));
	    }
	    public void caseIntType(IntType t) {
		setResult(new Integer(4));
	    }
	    public void caseFloatType(FloatType t) {
		setResult(new Integer(4));
	    }
	    public void caseLongType(LongType t) {
		setResult(new Integer(8));
	    }
	    public void caseDoubleType(DoubleType t) {
		setResult(new Integer(8));
	    }
	    public void defaultCase(Type t) {
		if (!(t instanceof RefType)) {
			throw new IllegalArgumentException(
			    "non-reference type: " + t);
		}
		SootClass c = ((RefType)t).getSootClass();
		if (c.isInterface()) {
			throw new IllegalArgumentException(
			    "interface type: " + t);
		}
		int sz = OBJECT_HEADER_SIZE;
		while (true) {
			for (Iterator i = c.getFields().iterator();
			    i.hasNext(); ) {
				SootField f = (SootField)i.next();
				if (f.isStatic())
					continue;
				Type ftype = f.getType();
				if (ftype instanceof RefLikeType) {
					sz += REFERENCE_SIZE;
					continue;
				}
				ftype.apply(this);
				sz += ((Integer)getResult()).intValue();
			}
			if (!c.hasSuperclass())
				break;
			c = c.getSuperclass();
		}
		setResult(new Integer(sz));
	    }
	}


	/**
	 * Return our best guess of the size of an instance of the given type,
	 * in bytes. We include {@link #OBJECT_HEADER_SIZE OBJECT_HEADER_SIZE}
	 * for object overhead, but don't include any gaps for alignment.
	 *
	 * @param type a primitive type, or a non-interface, non-array
	 *	reference type.
	 * @throws IllegalArgumentException if <code>type</code>
	 */
	public static int size(Type type) {
		SizeTypeSwitch sw = new SizeTypeSwitch();
		type.apply(sw);
		return ((Integer)sw.getResult()).intValue();
	}
}

