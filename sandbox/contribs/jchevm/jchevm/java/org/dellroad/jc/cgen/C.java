
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
// $Id: C.java,v 1.4 2005/01/25 23:12:00 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import java.util.*;
import java.io.*;
import org.dellroad.jc.Generate;

/**
 * Utility routines for generating C code.
 */
public class C {

	private static Set reservedWords;

	private C() {
	}

	/**
	 * Returns <code>string(s, true)</code>.
	 */
	public static String string(String s) {
		return string(s, true);
	}

	/**
	 * Returns a doubly-quoted C string containing the UTF-8 encoded
	 * value of <code>s</code> with special characters suitably escaped.
	 *
	 * If <code>enquote</code> is true, then enclose the string
	 * in double quotes.
	 */
	public static String string(String s, boolean enquote) {
		StringBuffer b = new StringBuffer(s.length() + 8);
		boolean justDidHexEscape = false;
		int qmarkCount = 0;
		if (enquote)
			b.append('"');
		byte[] bytes = Util.utf8Encode(s);
		for (int i = 0; i < bytes.length; i++) {
			char ch = (char)(bytes[i] & 0xff);

			// Handle NUL byte
			if (ch == 0x00) {
				b.append("\\xc0\\x80");
				justDidHexEscape = true;
				qmarkCount = 0;
				continue;
			}

			// Handle normal ASCII bytes
			if (ch <= 0x7e) {
				switch (ch) {
				case '"':
				case '\\':
					b.append("\\");
					b.append(ch);
					justDidHexEscape = false;
					qmarkCount = 0;
					continue;
				case '\t':
					b.append("\\t");
					justDidHexEscape = false;
					qmarkCount = 0;
					continue;
				case '\r':
					b.append("\\r");
					justDidHexEscape = false;
					qmarkCount = 0;
					continue;
				case '\n':
					b.append("\\n");
					justDidHexEscape = false;
					qmarkCount = 0;
					continue;
				default:

					// Don't follow hex escape with hex char
					if ((justDidHexEscape
					     && ((ch >= '0' && ch <= '9')
					      || (ch >= 'A' && ch <= 'F')
					      || (ch >= 'a' && ch <= 'f')))
					    || ch < 0x20)
						break;

					// Avoid trigraphs
					if (qmarkCount >= 2
					    && "()<>=/'!-".indexOf(ch) != -1)
					    	break;

					// Append unescaped character
					b.append(ch);
					justDidHexEscape = false;
					if (ch == '?')
						qmarkCount++;
					continue;
				}
			}

			// Encode byte with hex escape
			b.append('\\');
			b.append('x');
			b.append(Integer.toHexString((bytes[i] >> 4) & 0x0f));
			b.append(Integer.toHexString(bytes[i] & 0x0f));
			justDidHexEscape = true;
			qmarkCount = 0;
		}
		if (enquote)
			b.append('"');
		return b.toString();
	}

	public static String name(SootClass c) {
		return encode(c.getName());
	}

	public static String name(SootField f) {
		String name = f.getName();
		if (reserved(name))
			name = "_" + name;
		return encode(name);
	}

	public static String name(SootMethod m) {
		String name = m.getName();
		StringBuffer b = new StringBuffer();
		if (name.equals("<init>"))
			b.append("_init");
		else if (name.equals("<clinit>"))
			b.append("_clinit");
		else
			b.append(encode(name));
		b.append('$');
		b.append(Util.sigHashString(m));
		return b.toString();
	}

	/**
	 * Tells us whether the token is a reserved word that cannot
	 * be used in C source. For example, we can't allow "jfloat"
	 * as a static variable name because it conflicts with the
	 * C typedef of the same name.
	 */
	public static boolean reserved(String s) {
		return reservedWords.contains(s);
	}

	// Build reserved word set. We don't need to worry about any
	// token that begins with a single underscore, as that is not
	// a possible output of the encode() function given a Java
	// identifier.
	static {
		reservedWords = new HashSet();

		// JNI typedefs
		reservedWords.add("jboolean");
		reservedWords.add("jbyte");
		reservedWords.add("jchar");
		reservedWords.add("jshort");
		reservedWords.add("jint");
		reservedWords.add("jlong");
		reservedWords.add("jfloat");
		reservedWords.add("jdouble");

		// Functions declared via #include <setjmp.h>
		reservedWords.add("setjmp");
		reservedWords.add("longjmp");
		reservedWords.add("sigsetjmp");
		reservedWords.add("siglongjmp");
		reservedWords.add("longjmperror");

		// C reserved tokens
		reservedWords.add("auto");
		reservedWords.add("break");
		reservedWords.add("case");
		reservedWords.add("char");
		reservedWords.add("const");
		reservedWords.add("continue");
		reservedWords.add("default");
		reservedWords.add("do");
		reservedWords.add("double");
		reservedWords.add("else");
		reservedWords.add("enum");
		reservedWords.add("extern");
		reservedWords.add("float");
		reservedWords.add("for");
		reservedWords.add("if");
		reservedWords.add("int");
		reservedWords.add("long");
		reservedWords.add("register");
		reservedWords.add("restrict");
		reservedWords.add("return");
		reservedWords.add("short");
		reservedWords.add("static");
		reservedWords.add("struct");
		reservedWords.add("switch");
		reservedWords.add("typedef");
		reservedWords.add("union");
		reservedWords.add("volatile");
		reservedWords.add("while");
	}

	public static String encode(String name) {
		return Generate.encode(name, false);
	}

	public static String encode(String name, boolean ignoreSlashes) {
		return Generate.encode(name, ignoreSlashes);
	}

	public static String type(SootClass sc) {
		return type(sc.getType(), false);
	}

	public static String type(SootMethod m) {
		return type(m.getReturnType(), false);
	}

	public static String type(SootField f) {
		return type(f.getType(), false);
	}

	public static String type(Value v) {
		return type(v.getType(), false);
	}

	public static String type(SootClass sc, boolean withStruct) {
		return type(sc.getType(), withStruct);
	}

	public static String type(SootMethod m, boolean withStruct) {
		return type(m.getReturnType(), withStruct);
	}

	public static String type(SootField f, boolean withStruct) {
		return type(f.getType(), withStruct);
	}

	public static String type(Value v, boolean withStruct) {
		return type(v.getType(), withStruct);
	}

	public static CExpr value(ValueBox vb) {
		CValueSwitch cvs = new CValueSwitch(CMethod.current(), vb);
		vb.getValue().apply(cvs);
		return cvs.result;
	}

	/**
	 * Equivalent to <code>type(t, false)</code>.
	 */
	public static String type(Type t) {
		return type(t, false);
	}

	/**
	 * Return the C type for an object of type <code>t</code>,
	 * which must be a normal Java type, i.e., a type that can
	 * be the type of a variable.
	 *
	 * Reference types have the <code>struct</code> code prefixed
	 * if <code>withStruct</code> is true, but never have '*' appended.
	 */
	public static String type(Type t, boolean withStruct) {
		final StringBuffer b = new StringBuffer();
		if (Util.isPrimitive(t)) {
			if (t instanceof VoidType)
				b.append("void");
			else {
				t.apply(new Util.PrimTypeSwitch() {
				    public void prim(String ptype) {
					b.append('j');
					b.append(ptype);
				    }
				});
			}
		} else if (t instanceof ArrayType) {
			ArrayType at = (ArrayType)t;
			if (Util.isPrimitive(at.getElementType())) {
				at.getElementType().apply(
				  new Util.PrimTypeSwitch() {
				    public void prim(String ptype) {
					b.append("_jc_");
					b.append(ptype);
					b.append("_array");
				    }
				});
			} else
				b.append("_jc_object_array");
		} else if (t instanceof RefType) {
			SootClass rc = ((RefType)t).getSootClass();

			if (withStruct)
				b.append("struct ");
			b.append("_jc_");
			if (!rc.isInterface()) {
				b.append(encode(rc.getName()));
				b.append('$');
			}
			b.append("object");
		} else if (t instanceof VoidType)
			b.append("void");
		else if (t instanceof NullType)
			b.append("_jc_object");
		else
			Util.panic("weird type " + t);
		return b.toString();
	}

	public static String valueType(Type t) {
		if (t instanceof VoidType)
			return "void";
		if (!Util.isPrimitive(t))
			return "_jc_object *";
		final StringBuffer b = new StringBuffer();
		t.apply(new Util.PrimTypeSwitch() {
		    public void prim(String ptype) {
			b.append('j');
			b.append(ptype);
		    }
		});
		return b.toString();
	}

	public static String jc_type(Value v) {
		return jc_type(v.getType());
	}

	public static String jc_type(Type t) {
		StringBuffer b = new StringBuffer("_jc_");
		if (Util.isPrimitive(t))
			b.append(primName(t));
		else if (t instanceof RefType)
			b.append(name(((RefType)t).getSootClass()));
		else {
			ArrayType atype = (ArrayType)t;
			t = atype.baseType;
			if (Util.isPrimitive(t))
				b.append(primName(t));
			else
				b.append(name(((RefType)t).getSootClass()));
			b.append("$array");
			if (atype.numDimensions > 1)
				b.append(atype.numDimensions);
		}
		if (Util.isPrimitive(t))
			b.append("$prim");
		b.append("$type");
		return b.toString();
	}

	public static String primName(Type t) {
		final StringBuffer b = new StringBuffer(7);
		t.apply(new Util.PrimTypeSwitch() {
		    public void prim(String name) {
			b.append(name);
		    }
		});
		return b.toString();
	}

	public static String accessDefs(SootClass c) {
		return accessDefs(c.getModifiers());
	}

	public static String accessDefs(SootField f) {
		return accessDefs(f.getModifiers());
	}

	public static String accessDefs(SootMethod m) {
		return accessDefs(m.getModifiers());
	}

	public static String accessDefs(int flags) {
		StringBuffer b = new StringBuffer(120);
		if ((flags & Modifier.PRIVATE) != 0)
			b.append("|_JC_ACC_PRIVATE");
		if ((flags & Modifier.PROTECTED) != 0)
			b.append("|_JC_ACC_PROTECTED");
		if ((flags & Modifier.PUBLIC) != 0)
			b.append("|_JC_ACC_PUBLIC");
		if ((flags & Modifier.STATIC) != 0)
			b.append("|_JC_ACC_STATIC");
		if ((flags & Modifier.FINAL) != 0)
			b.append("|_JC_ACC_FINAL");
		if ((flags & Modifier.NATIVE) != 0)
			b.append("|_JC_ACC_NATIVE");
		if ((flags & Modifier.SYNCHRONIZED) != 0)
			b.append("|_JC_ACC_SYNCHRONIZED");
		if ((flags & Modifier.TRANSIENT) != 0)
			b.append("|_JC_ACC_TRANSIENT");
		if ((flags & Modifier.VOLATILE) != 0)
			b.append("|_JC_ACC_VOLATILE");
		if ((flags & Modifier.ABSTRACT) != 0)
			b.append("|_JC_ACC_ABSTRACT");
		if ((flags & Modifier.INTERFACE) != 0)
			b.append("|_JC_ACC_INTERFACE");
		if (b.length() == 0)
			return "0";
		b.deleteCharAt(0);
		return b.toString();
	}

	public static String paramsDecl(SootMethod m, boolean withNames) {
		StringBuffer b = new StringBuffer(128);
		b.append("(_jc_env *");
		if (withNames)
			b.append("const env");
		if (!m.isStatic() || m.getName().equals("<init>")) {
			b.append(", ");
			SootClass dc = m.getDeclaringClass();
			if (!withNames)
				b.append("struct ");
			b.append(C.type(dc));
			b.append(" *");
			if (withNames)
				b.append("this");
		}
		for (int i = 0; i < m.getParameterCount(); i++) {
			Type t = m.getParameterType(i);
			b.append(", ");
			if (Util.isReference(t) && !withNames)
				b.append("struct ");
			b.append(C.type(t));
			if (Util.isReference(t) || withNames)
				b.append(' ');
			if (Util.isReference(t))
				b.append('*');
			if (withNames)
				b.append("param" + i);
		}
		b.append(")");
		return b.toString();
	}

	public static void include(String s) {
		throw new RuntimeException("C.asm() invoked with a"
		    + " non-constant string: " + s);
	}

	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++)
			System.out.println(C.string(args[i]));
	}
}

