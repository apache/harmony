
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
// $Id: Util.java,v 1.6 2005/03/13 00:18:50 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import soot.tagkit.*;
import soot.jimple.*;
import java.util.*;
import java.io.*;
import org.dellroad.jc.Generate;

/**
 * Various utility stuff used when analyzing class files and generating code.
 */
public class Util {

	public static final int ACC_SUPER = 0x0020;

	private Util() {
	}

	/**
	 * Utility class that passes the appropriate string ("byte",
	 * "char", etc.) to the {@link #prim prim()} method depending
	 * on the primitive type.
	 */
	public static abstract class PrimTypeSwitch extends TypeSwitch {
		public void caseBooleanType(BooleanType t) {
			prim("boolean");
		}
		public void caseByteType(ByteType t) {
			prim("byte");
		}
		public void caseCharType(CharType t) {
			prim("char");
		}
		public void caseShortType(ShortType t) {
			prim("short");
		}
		public void caseIntType(IntType t) {
			prim("int");
		}
		public void caseLongType(LongType t) {
			prim("long");
		}
		public void caseFloatType(FloatType t) {
			prim("float");
		}
		public void caseDoubleType(DoubleType t) {
			prim("double");
		}
		public void caseVoidType(VoidType t) {
			prim("void");
		}
		public abstract void prim(String ptype);
		public void defaultCase(Type t) {
			Util.panic("non-primitive type " + t);
		}
	}

	/**
	 * Utility class that computes ordering preference in a C structure
	 * of the various types. We want longer types first, then shorter
	 * types, to avoid "holes". We always sort reference types first.
	 */
	public static abstract class OrderTypeSwitch extends TypeSwitch {
		public void caseBooleanType(BooleanType t) {
			order(4);
		}
		public void caseByteType(ByteType t) {
			order(4);
		}
		public void caseCharType(CharType t) {
			order(3);
		}
		public void caseShortType(ShortType t) {
			order(3);
		}
		public void caseIntType(IntType t) {
			order(2);
		}
		public void caseFloatType(FloatType t) {
			order(2);
		}
		public void caseLongType(LongType t) {
			order(1);
		}
		public void caseDoubleType(DoubleType t) {
			order(1);
		}
		public void defaultCase(Type t) {
			order(0);
		}
		public abstract void order(int order);
	}

	/**
	 * Return the _JC_TYPE_* macro appropriate for the given type.
	 */
	public static String _JC_TYPE(Type t) {
		if (isReference(t))
			return "_JC_TYPE_REFERENCE";
		TypeSwitch ts = new Util.PrimTypeSwitch() {
		    public void prim(String name) {
			setResult("_JC_TYPE_" + name.toUpperCase());
		    }
		};
		t.apply(ts);
		return (String)ts.getResult();
	}

	/**
	 * Sorts Strings in the same way that strcmp() does on their
	 * UTF-8 encodings.
	 */
	public static final Comparator utf8Comparator = new Comparator() {
		public int compare(Object x, Object y) {
			byte[] b1 = utf8Encode((String)x);
			byte[] b2 = utf8Encode((String)y);
			for (int i = 0; i < b1.length && i < b2.length; i++) {
				int diff = (b1[i] & 0xff) - (b2[i] & 0xff);
				if (diff != 0)
					return diff;
			}
			return b1.length - b2.length;
		}
	};

	/**
	 * Sorts methods by name, then by signature.
	 */
	public static final Comparator methodComparator = new Comparator() {
		public int compare(Object x, Object y) {
			SootMethod mx = (SootMethod)x;
			SootMethod my = (SootMethod)y;
			int diff;
			if ((diff = utf8Comparator.compare(mx.getName(),
			    my.getName())) != 0)
				return diff;
			return utf8Comparator.compare(
			    signature(mx), signature(my));
		}
	};

	/**
	 * Sorts local variables by name.
	 */
	public static final Comparator localComparator = new Comparator() {
		public int compare(Object x, Object y) {
			Local lx = (Local)x;
			Local ly = (Local)y;
			return lx.getName().compareTo(ly.getName());
		}
	};

	/**
	 * Sorts fields by staticness, basic type, name, then signature.
	 */
	public static final Comparator fieldComparator = new Comparator() {
		private int ots_order;
		private final OrderTypeSwitch ots = new OrderTypeSwitch() {
			public void order(int order) {
				ots_order = order;
			}
		};
		private int getOrder(Type t) {
			t.apply(ots);
			return ots_order;
		}
		public int compare(Object x, Object y) {
			SootField fx = (SootField)x;
			SootField fy = (SootField)y;
			int diff;

			if (fx.isStatic() != fy.isStatic())
				return fx.isStatic() ? -1 : 1;
			if ((diff = getOrder(fx.getType())
			    - getOrder(fy.getType())) != 0)
				return diff;
			if ((diff = utf8Comparator.compare(fx.getName(),
			    fy.getName())) != 0)
				return diff;
			return utf8Comparator.compare(signature(fx),
			    signature(fy));
		}
	};

	/**
	 * Sorts classes by their names' UTF-8 encoding.
	 */
	public static final Comparator classComparator = new Comparator() {
		public int compare(Object x, Object y) {
			SootClass cx = (SootClass)x;
			SootClass cy = (SootClass)y;
			return utf8Comparator.compare(
			    cx.getName(), cy.getName());
		}
	};

	public static boolean isReference(SootClass c) {
		return (isReference(c.getType()));
	}

	public static boolean isReference(SootField f) {
		return (isReference(f.getType()));
	}

	public static boolean isReference(SootMethod m) {
		return (isReference(m.getReturnType()));
	}

	public static boolean isPrimitive(Type type) {
		return type instanceof PrimType || type instanceof VoidType;
	}

	public static boolean isPrimitiveArray(Type type) {
		return type instanceof ArrayType
		    && isPrimitive(((ArrayType)type).getElementType());
	}

	public static boolean isFinal(SootClass c) {
		return Modifier.isFinal(c.getModifiers());
	}

	public static boolean isFinal(SootField f) {
		return Modifier.isFinal(f.getModifiers());
	}

	public static boolean isFinal(SootMethod m) {
		return Modifier.isFinal(m.getModifiers());
	}

	public static boolean isVirtual(SootMethod m) {
		return !m.isStatic() && m.getName().charAt(0) != '<';
	}

	public static boolean isConstructor(SootMethod m) {
		return !m.isStatic() && m.getName().equals("<init>");
	}

	public static boolean isClassInit(SootMethod m) {
		return m.isStatic() && m.getName().equals("<clinit>");
	}

	public static boolean hasSubtypes(SootClass c) {
		return !isFinal(c);
	}

	public static boolean hasSubtypes(SootField f) {
		return !isFinal(f);
	}

	public static boolean hasSubtypes(SootMethod m) {
		return !isFinal(m);
	}

	public static boolean hasSubtypes(Type t) {
		if (isPrimitive(t))
			return false;
		if (t instanceof RefType) {
			SootClass c = ((RefType)t).getSootClass();
			if (c.isInterface())
				return true;
			return !isFinal(c);
		}
		return hasSubtypes(((ArrayType)t).baseType);
	}

	/**
	 * Return whether type <code>t</code> is a reference type or not.
	 * <code>t</code> must be a normal Java type, i.e., a type that can
	 * be the type of a variable.
	 */
	public static boolean isReference(Type t) {
		return (t instanceof RefLikeType);
	}

	public static char typeLetter(Type t) {
		TypeSwitch ts = new TypeSwitch() {
		    public void caseBooleanType(BooleanType t) {
			    setResult("z");
		    }
		    public void caseByteType(ByteType t) {
			    setResult("b");
		    }
		    public void caseCharType(CharType t) {
			    setResult("c");
		    }
		    public void caseShortType(ShortType t) {
			    setResult("s");
		    }
		    public void caseIntType(IntType t) {
			    setResult("i");
		    }
		    public void caseLongType(LongType t) {
			    setResult("j");
		    }
		    public void caseFloatType(FloatType t) {
			    setResult("f");
		    }
		    public void caseDoubleType(DoubleType t) {
			    setResult("d");
		    }
		    public void caseArrayType(ArrayType t) {
			    setResult("l");
		    }
		    public void caseRefType(RefType t) {
			    setResult("l");
		    }
		    public void defaultCase(Type t) {
			    Util.panic("bogus type " + t);
		    }
		};
		t.apply(ts);
		return ((String)ts.getResult()).charAt(0);
	}

	public static String fieldDescriptor(Type t) {
		TypeSwitch ts = new TypeSwitch() {
		    public void caseBooleanType(BooleanType t) {
			    setResult("Z");
		    }
		    public void caseByteType(ByteType t) {
			    setResult("B");
		    }
		    public void caseCharType(CharType t) {
			    setResult("C");
		    }
		    public void caseShortType(ShortType t) {
			    setResult("S");
		    }
		    public void caseIntType(IntType t) {
			    setResult("I");
		    }
		    public void caseLongType(LongType t) {
			    setResult("J");
		    }
		    public void caseFloatType(FloatType t) {
			    setResult("F");
		    }
		    public void caseDoubleType(DoubleType t) {
			    setResult("D");
		    }
		    public void caseArrayType(ArrayType t) {
			    setResult("["
				+ fieldDescriptor(t.getElementType()));
		    }
		    public void caseRefType(RefType t) {
			    setResult("L"
				+ t.getSootClass().getName().replace('.', '/')
				+ ";");
		    }
		    public void caseVoidType(VoidType t) {
			    setResult("V");
		    }
		    public void defaultCase(Type t) {
			    Util.panic("bogus type " + t);
		    }
		};
		t.apply(ts);
		return (String)ts.getResult();
	}

	public static String typeWord(Type t) {
		if (Util.isReference(t))
			return "object";
		TypeSwitch ts = new Util.PrimTypeSwitch() {
		    public void prim(String name) {
			setResult(name);
		    }
		};
		t.apply(ts);
		return (String)ts.getResult();
	}

	public static String signature(SootField f) {
		return fieldDescriptor(f.getType());
	}

	public static String signature(SootMethod m) {
		StringBuffer b = new StringBuffer(24);
		b.append('(');
		for (int i = 0; i < m.getParameterCount(); i++)
			b.append(fieldDescriptor(m.getParameterType(i)));
		b.append(')');
		b.append(fieldDescriptor(m.getReturnType()));
		return b.toString();
	}

	public static String fullSignature(SootMethod m) {
		return m.getName() + signature(m);
	}

	public static String paramTypes(SootMethod m) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < m.getParameterCount(); i++) {
			b.append(fieldDescriptor(
			    m.getParameterType(i)).charAt(0));
		}
		b.append(fieldDescriptor(m.getReturnType()));
		return b.toString();
	}

	public static long hash(String s) {
		return Generate.hash(new ByteArrayInputStream(utf8Encode(s)));
	}

	public static long sigHash(SootMethod m) {
		return hash(fullSignature(m));
	}

	public static long classHash(SootClass sc) {
		try {
			return SootCodeGenerator.finder.getClassfileHash(
			    sc.getName().replace('.', '/'));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);	// should never happen
		}
	}

	/**
	 * Determine if an INVOKESPECIAL can be implemented
	 * like the old INVOKENONVIRTUAL.
	 */
	public static boolean isNonvirtual(SpecialInvokeExpr v,
	    SootMethod callingMethod) {

		// Get info
		SootMethod targetMethod = v.getMethod();
		SootClass callingClass = callingMethod.getDeclaringClass();

		// Check the easy stuff first
		if (targetMethod.getName().equals("<init>")
		    || targetMethod.isPrivate()
		    || (callingClass.getModifiers() & Util.ACC_SUPER) == 0)
			return true;

		// Determine if called method's class is a superclass
		boolean isSuperclass = false;
		SootClass calledClass = targetMethod.getDeclaringClass();
		for (SootClass cls = callingClass; cls.hasSuperclass(); ) {
			cls = cls.getSuperclass();
			if (cls.equals(calledClass))
				return false;
		}

		// OK
		return true;
	}

	/**
	 * Get the instanceof hash table hash.
	 */
	public static int instanceofHash(SootClass sc) {
		return ((int)classHash(sc) & 0x7fffffff)
		    % Constants.INSTANCEOF_HASHSIZE;
	}

	/**
	 * Get the interface method hash table hash.
	 */
	public static int imethodHash(SootMethod m) {
		return ((int)sigHash(m) & 0x7fffffff)
		    % Constants.IMETHOD_HASHSIZE;
	}

	public static String sigHashString(SootMethod m) {
		long hash = sigHash(m);
		String s = Long.toHexString(hash);
		if ((hash & 0xf000000000000000L) != 0)
			return s;
		StringBuffer b = new StringBuffer(s);
		while (b.length() < 16)
			b.insert(0, '0');
		return b.toString();
	}

	/**
	 * Find a method declared by a SootClass or any of its superclasses.
	 */
	public static SootMethod findMethod(SootClass c, String subSig) {
		for (SootClass oc = c; true; c = c.getSuperclass()) {
			try {
				return c.getMethod(subSig);
			} catch (RuntimeException e) {
			}
			if (!c.hasSuperclass()) {
				throw new RuntimeException("no such method in "
				    + "class " + oc.getName() + ": " + subSig);
			}
		}
	}

	public static Set getAllInterfaces(SootClass c) {
		HashSet set = new HashSet();
		addInterfaces(c, set);
		return set;
	}

	private static void addInterfaces(SootClass c, Set set) {
		if (c.isInterface())
			set.add(c);
		/* else */ if (c.hasSuperclass())
			addInterfaces(c.getSuperclass(), set);
		for (Iterator i = c.getInterfaces().iterator(); i.hasNext(); )
			addInterfaces((SootClass)i.next(), set);
	}

	public static Set getAllSupertypes(SootClass c) {
		HashSet set = new HashSet();
		addSupertypes(c, set);
		return set;
	}

	public static void addSupertypes(SootClass cls, Set set) {

		// Add this class
		set.add(cls);

		// Recurse on superclass
		if (cls.hasSuperclass()) {
			SootClass superclass = cls.getSuperclass();
			addSupertypes(superclass, set);
		}

		// Recurse on superinterfaces
		for (Iterator i = cls.getInterfaces().iterator();
		    i.hasNext(); ) {
			SootClass superinterface = (SootClass)i.next();
			addSupertypes(superinterface, set);
		}
	}

	public static void require(boolean value) {
		require(value, "assertion failure");
	}

	/**
	 * Poor man's assert().
	 */
	public static void require(boolean value, String msg) {
		if (!value)
			throw new RuntimeException(msg);
	}

	/**
	 * This method is required to work around a stupid bug
	 * in Sun's JDK String.getBytes("UTF-8") (bug #4628881).
	 */
	public static byte[] utf8Encode(String s) {
		final char[] chars = s.toCharArray();
		int elen;

		// Compute encoded length
		elen = 0;
		for (int i = 0; i < s.length(); i++) {
			int ch = chars[i];
			if (ch >= 0x0001 && ch <= 0x007f)
				elen++;
			else if (ch == 0x0000 || (ch >= 0x0080 && ch <= 0x07ff))
				elen += 2;
			else
				elen += 3;
		}

		// Do the actual encoding
		byte[] data = new byte[elen];
		elen = 0;
		for (int i = 0; i < s.length(); i++) {
			int ch = chars[i];
			if (ch >= 0x0001 && ch <= 0x007f)
				data[elen++] = (byte)ch;
			else if (ch == 0x0000
			    || (ch >= 0x0080 && ch <= 0x07ff)) {
				data[elen++]
				    = (byte)(0xc0 | ((ch >> 6) & 0x1f));
				data[elen++] = (byte)(0x80 | (ch & 0x3f));
			} else {
				data[elen++]
				    = (byte)(0xe0 | ((ch >> 12) & 0x0f));
				data[elen++]
				    = (byte)(0x80 | ((ch >> 6) & 0x3f));
				data[elen++] = (byte)(0x80 | (ch & 0x3f));
			}
		}
		return data;
	}

	public static void panic(String msg) {
		throw new RuntimeException("Panic: " + msg);
	}
}

