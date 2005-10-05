
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
// $Id: SourceFile.java,v 1.7 2005/03/13 00:18:50 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import soot.tagkit.*;
import java.util.*;
import java.io.*;

/**
 * Represents a Java class file being converted into C source
 * and/or header files. Contains analysis and formatting code
 * common to both C source and C header file generation.
 */
public abstract class SourceFile implements Constants {
	protected SootClass c;
	protected String cname;
	protected String prefix;
	protected ArrayList superclasses;
	protected CodeWriter out;
	protected int numVirtualRefFields;
	protected String sourceFile;
	protected boolean hasStaticInitializer;
	protected SootField[] virtualFields;
	protected SootField[] staticFields;
	protected SootMethod[] virtualMethods;
	protected SootMethod[] staticMethods;
	protected SootMethod[] constructors;
	protected InnerClass innerClasses[];
	protected SootClass outerClass;

	static class InnerClass {
		final SootClass inner;
		final int flags;
		InnerClass(String inner, int flags) {
			this.inner = Scene.v().loadClassAndSupport(
			    inner.replace('/', '.'));
			this.flags = flags;
		}
	}

	public SourceFile(SootClass c, Writer out) {
		if (c == null)
			throw new IllegalArgumentException();
		this.c = c;
		this.out = new CodeWriter(out);
		this.cname = C.name(c);
		this.prefix = "_jc_" + cname;
		prepare();
	}

	private void prepare() {

		// Generate list containing class and superclasses
		superclasses = new ArrayList();
		for (SootClass s = c; true; s = s.getSuperclass()) {
			superclasses.add(s);
			if (!s.hasSuperclass())
				break;
		}

		// Get source file name
		try {
			sourceFile = new String(c.getTag(
			    "SourceFileTag").getValue(), "UTF8");
		} catch (Exception e) {
			sourceFile = "unknown";
		}

		// Locate inner and outer classes
		ArrayList ilist = new ArrayList();
		for (Iterator i = c.getTags().iterator(); i.hasNext(); ) {
			Tag nextTag = (Tag)i.next();
			if (!(nextTag instanceof InnerClassTag))
				continue;
			InnerClassTag tag = (InnerClassTag)nextTag;
			String inner = tag.getInnerClass();
			String outer = tag.getOuterClass();
			String myName = c.getName().replace('.', '/');
			if (myName.equals(inner) && outer != null) {
				outerClass = Scene.v().loadClassAndSupport(
				    outer.replace('/', '.'));
			} else if (myName.equals(outer) && inner != null) {
				ilist.add(new InnerClass(inner,
				    tag.getAccessFlags()));
			}
		}
		innerClasses = (InnerClass[])ilist.toArray(
		    new InnerClass[ilist.size()]);

		// Prepare fields and methods
		prepareFieldsAndMethods();
	}

	// Sort fields and methods into static and non-static,
	// then sort each list alphabetically by name
	private void prepareFieldsAndMethods() {

		// Compute virtualFields
		ArrayList temp = new ArrayList();
		for (Iterator i = c.getFields().iterator(); i.hasNext(); ) {
			SootField f = (SootField)i.next();
			if (!f.isStatic())
				temp.add(f);
		}
		virtualFields = (SootField[])temp.toArray(
		    new SootField[temp.size()]);
		Arrays.sort(virtualFields, Util.fieldComparator);

		// Count number of reference virtual fields in an instance.
		// We must include fields declared in all superclasses.
		numVirtualRefFields = 0;
		for (int i = 0; i < superclasses.size(); i++) {
			SootClass sc = (SootClass)superclasses.get(i);
			for (Iterator j = sc.getFields().iterator();
			    j.hasNext(); ) {
				SootField f = (SootField)j.next();
				if (!f.isStatic() && Util.isReference(f))
					numVirtualRefFields++;
			}
		}

		// Compute staticFields
		temp = new ArrayList();
		for (Iterator i = c.getFields().iterator(); i.hasNext(); ) {
			SootField f = (SootField)i.next();
			if (f.isStatic())
				temp.add(f);
		}
		staticFields = (SootField[])temp.toArray(
		    new SootField[temp.size()]);
		Arrays.sort(staticFields, Util.fieldComparator);

		// Compute virtualMethods and look for <clinit>
		temp = new ArrayList();
		for (Iterator i = c.getMethods().iterator(); i.hasNext(); ) {
			SootMethod m = (SootMethod)i.next();
			if (Util.isVirtual(m))
				temp.add(m);
			if (Util.isClassInit(m))
				hasStaticInitializer = true;
		}
		virtualMethods = (SootMethod[])temp.toArray(
		    new SootMethod[temp.size()]);
		Arrays.sort(virtualMethods, Util.methodComparator);

		// Compute staticMethods
		temp = new ArrayList();
		for (Iterator i = c.getMethods().iterator(); i.hasNext(); ) {
			SootMethod m = (SootMethod)i.next();
			if (m.isStatic())
				temp.add(m);
		}
		staticMethods = (SootMethod[])temp.toArray(
		    new SootMethod[temp.size()]);
		Arrays.sort(staticMethods, Util.methodComparator);

		// Compute constructors
		temp = new ArrayList();
		for (Iterator i = c.getMethods().iterator(); i.hasNext(); ) {
			SootMethod m = (SootMethod)i.next();
			if (Util.isConstructor(m))
				temp.add(m);
		}
		constructors = (SootMethod[])temp.toArray(
		    new SootMethod[temp.size()]);
		Arrays.sort(constructors, Util.methodComparator);
	}

	public void include(String filename) {
		out.println("#include " + C.string(filename));
	}

	public void include(SootClass sc) {
		StringBuffer b = new StringBuffer();
		String name = sc.getName();
		int next;
		for (int i = 0; i < name.length(); i = next + 1) {
			if ((next = name.indexOf('.', i)) == -1)
				next = name.length();
			if (i > 0)
				b.append('/');
			b.append(C.encode(name.substring(i, next)));
		}
		b.append(".h");
		out.println("#include " + C.string(b.toString()));
	}

	public abstract void output();

	public void outputInitialStuff(SootClass[] hlist,
	    SootClass[] dlist, boolean defs) {

		// Output initial comment
//		outputCommentLine("generated by "
//		    + this.getClass().getName() + " " + new Date()
//		    + " (" + System.getProperty("java.vm.name") + ")");
		String modString = Modifier.toString(c.getModifiers());
		outputCommentLine(modString
		    + (modString.equals("") ? "" : " ")
		    + (c.isInterface() ? "" : "class ")
		    + C.string(c.getName(), false));
		out.println();

		// Output classfile @dep_class tags
		if (dlist.length > 0) {
			Arrays.sort(dlist, Util.classComparator);
			outputCommentLine("class file dependencies");
			for (int i = 0; i < dlist.length; i++) {
				outputCommentLine("@dep_class "
				    + Long.toHexString(Util.classHash(dlist[i]))
				    + " " + C.encode(dlist[i].getName(), true));
			}
			out.println();
		}

		// Output header file @dep_header tags
		Arrays.sort(hlist, Util.classComparator);
		if (hlist.length > 0) {
			outputCommentLine("header file dependencies");
			for (int i = 0; i < hlist.length; i++) {
				outputCommentLine("@dep_header "
				    + C.encode(hlist[i].getName(), true));
			}
			out.println();
		}

		// #include files: JC definitions
		if (defs) {
			include("jc_defs.h");
			out.println();
		}

		// #include files: other classes
		if (hlist.length > 0) {
			for (int i = 0; i < hlist.length; i++)
				include(hlist[i]);
			out.println();
		}
	}

	public void outputFinalStuff() {
		out.flush();
	}

	public void outputCommentLine(String s) {
		out.print("// ");
		out.println(s);
	}

	public void outputBanner(String s) {
		out.print('/');
		for (int i = 0; i < 8; i++)
			out.print("*********");
		out.println();
		out.print(" *");
		int start = 2 + (70 - s.length()) / 2;
		spaceFillTo(2, start);
		out.print(s);
		spaceFillTo(start + s.length(), 72);
		out.println('*');
		out.print(' ');
		for (int i = 0; i < 8; i++)
			out.print("*********");
		out.println('/');
		out.println();
	}

	public void spaceFillTo(int posn, int target) {
		while (posn < target) {
			int remain = target - posn;
			int modpos = posn % 8;

			if (remain >= 8 - modpos) {
				out.print('\t');
				posn += 8 - modpos;
			} else {
				out.print(' ');
				posn++;
			}
		}
	}
}

