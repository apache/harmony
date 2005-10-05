
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
// $Id: HFile.java,v 1.3 2005/05/08 21:12:07 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import java.util.*;
import java.io.*;

/**
 * Represents a C header file being constructed from a Java class file.
 */
public class HFile extends SourceFile {
	protected String includeProtector;

	public HFile(SootClass c, Writer out) {
		super(c, out);
		includeProtector = "_JC_" + cname + "_H_";
	}

	/**
	 * Output the C header file.
	 */
	public void output() {
		outputInitialStuff();
		outputForwardDecls();
		outputTypedefs();
		outputStaticFieldStructure();
		outputMethodDeclarations();
		outputVirtualMethodStructure();
		outputVtable();
		outputVtype();
		outputObject();
		outputClassInfoDecl();
		outputFinalStuff();
		out.close();
	}

	public void outputInitialStuff() {

		// Include direct superclass's header file only.
		// Depend on this class and all superclasses.
		super.outputInitialStuff(superclasses.size() > 1 ?
		   new SootClass[] { (SootClass)superclasses.get(1) } :
		   new SootClass[] { /* must be java.lang.Object */ },
		   (SootClass[])superclasses.toArray(
		     new SootClass[superclasses.size()]), false);
		out.println("#ifndef " + includeProtector);
		out.println("#define " + includeProtector);
		out.println();
	}

	public void outputFinalStuff() {
		out.println("#endif\t/* " + includeProtector + " */");
		super.outputFinalStuff();
	}

	public void outputForwardDecls() {

		// Skip for interfaces
		if (c.isInterface())
			return;

		outputCommentLine("Forward structure declarations");
		HashSet decls = new HashSet();

		// Declare types for all reference fields
		// XXX not really necessary
		for (Iterator i = c.getFields().iterator(); i.hasNext(); ) {
			SootField f = (SootField)i.next();
			if (f.getType() instanceof RefType) {
				RefType t = (RefType)f.getType();
				decls.add(t.getSootClass().getName());
			}
		}

		// Declare types for all method parameters and return values
		for (Iterator i = c.getMethods().iterator(); i.hasNext(); ) {
			SootMethod m = (SootMethod)i.next();
			for (int j = 0; j < m.getParameterCount(); j++) {
				Type t = m.getParameterType(j);
				if (t instanceof RefType) {
					decls.add(((RefType)t)
					    .getSootClass().getName());
				}
			}
			if (m.getReturnType() instanceof RefType) {
				decls.add(((RefType)m.getReturnType())
				    .getSootClass().getName());
			}
		}

		// Declare type for java.lang.Class, which is
		// an implicit parameter to all static methods
		decls.add("java.lang.Class");

		// Output forward decl's
		String[] list = (String[])decls.toArray(
		    new String[decls.size()]);
		Arrays.sort(list);
		for (int i = 0; i < list.length; i++) {
			out.println("struct _jc_"
			    + C.encode(list[i]) + "$object;");
		}
		out.println();
	}

	public void outputTypedefs() {

		outputCommentLine("Typedefs");
		if (!c.isInterface()) {
			out.println("typedef struct "
			    + prefix + "$sub_refs\t" + prefix + "$sub_refs;");
			out.println("typedef struct "
			    + prefix + "$sub_nonrefs\t"
			    + prefix + "$sub_nonrefs;");
			out.println("typedef struct "
			    + prefix + "$refs\t" + prefix + "$refs;");
			out.println("typedef struct "
			    + prefix + "$nonrefs\t" + prefix + "$nonrefs;");
			out.println("typedef struct "
			    + prefix + "$object\t" + prefix + "$object;");
			out.println();
		}
		if (staticFields.length > 0) {
			out.println("typedef struct " + prefix
			    + "$fields_struct\t" + prefix + "$fields_struct;");
		}
		if (!c.isInterface()) {
			out.println("typedef const struct "
			    + prefix + "$vmethods\t" + prefix + "$vmethods;");
			out.println("typedef const struct "
			    + prefix + "$vtable\t" + prefix + "$vtable;");
		}
		out.println("typedef const struct "
		    + prefix + "$vtype\t" + prefix + "$vtype;");
		out.println();
	}

	// Output static fields structure, reference fields first
	public void outputStaticFieldStructure() {
		if (staticFields.length == 0)
			return;
		outputCommentLine("Class fields structure");
		out.println("struct " + prefix + "$fields_struct {");
		out.indent();
		for (int i = 0; i < staticFields.length; i++) {
			SootField field = staticFields[i];
			out.println(C.type(field, true) + "\t"
			    + (Util.isReference(field) ? "*" : "")
			    + (Modifier.isVolatile(field.getModifiers()) ?
			      "volatile " : "")
			    + C.name(field) + ";");
		}
		out.undent();
		out.println("};");
		out.print("extern ");
//		if (c.isInterface())
//			out.print("const ");
		out.println(prefix + "$fields_struct "
		    + prefix + "$class_fields;");
		out.println();
	}

	public void outputMethodDeclarations() {

		// Declare constructors
		if (constructors.length > 0) {
			outputCommentLine("Constructors");
			for (int i = 0; i < constructors.length; i++) {
				SootMethod method = constructors[i];
				out.print("extern ");
				out.print(C.type(method, true) + " ");
				if (Util.isReference(method))
					out.print('*');
				out.println(prefix + "$method$" + C.name(method)
				    + C.paramsDecl(method, false)
				    + " _JC_JCNI_ATTR;");
			}
			out.println();
		}

		// Declare virtual methods
		if (!c.isInterface() && virtualMethods.length > 0) {
			outputCommentLine("Virtual methods");
			for (int i = 0; i < virtualMethods.length; i++) {
				SootMethod method = virtualMethods[i];
				out.print("extern ");
				out.print(C.type(method, true) + " ");
				if (Util.isReference(method))
					out.print('*');
				out.println(prefix + "$method$" + C.name(method)
				    + C.paramsDecl(method, false)
				    + " _JC_JCNI_ATTR;");
			}
			out.println();
		}

		// Declare virtual method descriptors for use in
		// subclass interface method hash tables
		if (!c.isInterface() && virtualMethods.length > 0) {
			outputCommentLine("Virtual method descriptors");
			for (int i = 0; i < virtualMethods.length; i++) {
				SootMethod method = virtualMethods[i];
				out.println("extern _jc_method " + prefix
				    + "$method_info$" + C.name(method) + ";");
			}
			out.println();
		}

		// Declare static methods
		if (staticMethods.length > 0) {
			outputCommentLine("Static methods");
			for (int i = 0; i < staticMethods.length; i++) {
				SootMethod method = staticMethods[i];
				out.print("extern ");
				out.print(C.type(method, true) + " ");
				if (Util.isReference(method))
					out.print('*');
				out.println(prefix + "$method$" + C.name(method)
				    + C.paramsDecl(method, false)
				    + " _JC_JCNI_ATTR;");
			}
			out.println();
		}
	}

	public void outputVirtualMethodStructure() {
		if (c.isInterface())
			return;
		outputCommentLine("Virtual methods structure");
		out.println("struct " + prefix + "$vmethods {");
		out.indent();
		for (int i = 0; i < virtualMethods.length; i++) {
			SootMethod m = virtualMethods[i];
			out.print(C.type(m, true) + "\t");
			if (Util.isReference(m))
				out.print('*');
			out.println("(*" + C.name(m) + ")"
			    + C.paramsDecl(m, false) + " _JC_JCNI_ATTR;");
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputVtable() {
		if (c.isInterface())
			return;
		outputCommentLine("Vtable");
		out.println("struct " + prefix + "$vtable {");
		out.indent();
		for (int i = superclasses.size() - 1; i >= 0; i--) {
			String sc = C.name((SootClass)superclasses.get(i));
			out.println("_jc_" + sc + "$vmethods\t" + sc + ";");
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputVtype() {
		outputCommentLine("Vtype");
		out.println("struct " + prefix + "$vtype {");
		out.indent();
		out.println("_jc_type\t\t\ttype;");
		if (!c.isInterface())
			out.println(prefix + "$vtable\tvtable;");
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputObject() {

		// Skip for interfaces
		if (c.isInterface())
			return;

		// Do structure containing subclass reference fields
		outputCommentLine("Reference instance fields (subclass)");
		out.println("struct " + prefix + "$sub_refs {");
		out.indent();
		for (Iterator i = c.getFields().iterator(); i.hasNext(); ) {
			SootField f = (SootField)i.next();
			if (!Util.isReference(f) || f.isStatic())
				continue;
			out.println(C.type(f, true) + "\t*"
			    + (Modifier.isVolatile(f.getModifiers()) ?
			      "volatile " : "")
			    + C.name(f) + ";");
		}
		out.undent();
		out.println("};");
		out.println();

		// Do structure containing reference fields
		outputCommentLine("Reference instance fields (object)");
		out.println("struct " + prefix + "$refs {");
		for (int i = 0; i < superclasses.size(); i++) {
			SootClass sc = (SootClass)superclasses.get(i);
			String scname = C.name(sc);
			out.println("    _jc_"
			    + scname + "$sub_refs\t" + scname + ";");
		}
		out.println("};");
		out.println();

		// Do structure containing subclass non-reference fields
		outputCommentLine("Non-reference instance fields (subclass)");
		out.println("struct " + prefix + "$sub_nonrefs {");
		out.indent();
		for (Iterator i = c.getFields().iterator(); i.hasNext(); ) {
			SootField f = (SootField)i.next();
			if (Util.isReference(f) || f.isStatic())
				continue;
			out.println(C.type(f) + "\t"
			    + (Modifier.isVolatile(f.getModifiers()) ?
			      "volatile " : "")
			    + C.name(f) + ";");
		}
		out.undent();
		out.println("};");
		out.println();

		// Do structure containing non-reference fields
		outputCommentLine("Non-reference instance fields (object)");
		out.println("struct " + prefix + "$nonrefs {");
		for (int i = superclasses.size() - 1; i >= 0; i--) {
			SootClass sc = (SootClass)superclasses.get(i);
			String scname = C.name(sc);
			out.println("    _jc_"
			    + scname + "$sub_nonrefs\t" + scname + ";");
		}
		out.println("};");
		out.println();

		// Do object structure
		outputCommentLine("Object instance structure");
		out.println("struct " + prefix + "$object {");
		out.indent();

		// Reference fields (this class first)
		out.println(prefix + "$refs\trefs[0];");

		// Head structure
		out.println("_jc_word\t\tlockword;");
		out.println(prefix + "$vtype\t*vtype;");

		// Non-reference fields (this class last)
		out.println(prefix + "$nonrefs\tnonrefs;");

		// End object structure
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputClassInfoDecl() {
		outputCommentLine("Type structure");
		out.println("extern " + prefix + "$vtype " + prefix + "$type;");
		out.println();
		outputCommentLine("Array types class info structures");
		out.println("_JC_DECL_ARRAYS(" + cname + ", type);");
		out.println();
		outputCommentLine("java.lang.Class instance");
		out.println("extern struct _jc_java_lang_Class$object "
		    + prefix + "$class_object;");
		out.println();
	}
}

