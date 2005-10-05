
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
// $Id: CFile.java,v 1.19 2005/03/13 00:18:50 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import soot.*;
import soot.tagkit.*;
import soot.jimple.*;
import java.util.*;
import java.io.*;

/**
 * Represents a C source file being constructed from a Java class file.
 */
public class CFile extends SourceFile {
	public static final String includeStringName = "_jc_include";
	HashSet[] iMethodBuckets;
	int numNonemptyIMethodBuckets;
	int numSingletonIMethodBuckets;
	HashSet fullDepends;
	HashSet baseDepends;
	HashMap methodMap;			// SootMethod -> CMethod
	MethodOptimizer optimizer;
	boolean includeLineNumbers;
	JimpleBody body;

	public CFile(SootClass c, Writer out,
	    MethodOptimizer optimizer, boolean includeLineNumbers) {
		super(c, out);
		this.optimizer = optimizer;
		this.includeLineNumbers = includeLineNumbers;
		prepare();
	}

	/**
	 * Output the C source file.
	 */
	public void output() {
		outputInitialStuff();
		if (!c.getFields().isEmpty()) {
			outputBanner("Fields");
			outputFields();
		}
		ArrayList list = new ArrayList();
		list.addAll(Arrays.asList(staticFields));
		list.addAll(Arrays.asList(virtualFields));
		SootField[] fields = (SootField[])list.toArray(
		    new SootField[list.size()]);
		Arrays.sort(fields, Util.fieldComparator);
		outputFieldList(fields, "Fields", "fields");
		if (staticMethods.length > 0) {
			outputBanner("Static methods");
			outputMethods(staticMethods);
		}
		if (constructors.length > 0) {
			outputBanner("Constructors");
			outputMethods(constructors);
		}
		if (virtualMethods.length > 0) {
			outputBanner("Instance methods");
			outputMethods(virtualMethods);
		}
		outputBanner("Method list");
		list.clear();
		list.addAll(Arrays.asList(staticMethods));
		list.addAll(Arrays.asList(constructors));
		list.addAll(Arrays.asList(virtualMethods));
		SootMethod[] methods = (SootMethod[])list.toArray(
		    new SootMethod[list.size()]);
		Arrays.sort(methods, Util.methodComparator);
		outputMethodList(methods, "Methods", "methods");
		if (staticFields.length > 0) {
			outputBanner("Class fields structure");
			outputStaticFieldStructure();
		}
		if (c.getInterfaceCount() > 0) {
			outputBanner("Interface list");
			outputInterfaceList();
		}
		if (!c.isInterface() && numNonemptyIMethodBuckets > 0) {
			outputBanner("Interface hash table");
			outputInterfaceHashTable();
			if (numSingletonIMethodBuckets > 0)
				outputInterfaceQuickTable();
		}
		outputBanner("Instanceof hash table");
		outputInstanceOfHashTable();
		outputBanner("Dependency list");
		outputDepedencyList();
		outputBanner("Class info");
		outputInnerClases();
		outputClassInfo();
		outputFinalStuff();
		out.close();
	}

	private void prepare() {

		// Do interface hash table
		prepareInterfaceMethodHashTable();

		// Create dependency set
		fullDepends = new HashSet();

		// Analyze and optimze all methods
		methodMap = new HashMap(c.getMethods().size(), 2.0f);
		for (Iterator i = c.getMethods().iterator(); i.hasNext(); ) {
			SootMethod m = (SootMethod)i.next();
			CMethod cm = new CMethod(this, m,
			    optimizer, includeLineNumbers, fullDepends);
			methodMap.put(m, cm);
		}

		// Prepare dependencies
		prepareDependencies();
	}

	/**
	 * Generate interface method hash table.
	 */
	private void prepareInterfaceMethodHashTable() {

		// Skip if class is not instantiable
		if (c.isInterface() || c.isAbstract())
			return;

		// Generate set of all implemented interfaces
		Set allInterfaces = Util.getAllInterfaces(c);

		// Create interface method hash table
		iMethodBuckets = new HashSet[IMETHOD_HASHSIZE];

		// Generate set of signatures of methods specified by any
		// (possibly inherited) interface implemented by this class
		HashSet isigs = new HashSet();
		for (Iterator i = allInterfaces.iterator(); i.hasNext(); ) {
			SootClass sc = (SootClass)i.next();
			for (Iterator j = sc.getMethods().iterator();
			    j.hasNext(); ) {
				SootMethod m = (SootMethod)j.next();
				if (m.isStatic())
					continue;
				isigs.add(m.getSubSignature());
			}
		}

		// Add corresponding methods to the hash table buckets
		for (Iterator i = isigs.iterator(); i.hasNext(); ) {
			SootMethod m = Util.findMethod(c, (String)i.next());
			Util.require(!m.isAbstract());
			int bucket = Util.imethodHash(m);
			if (iMethodBuckets[bucket] == null) {
				iMethodBuckets[bucket] = new HashSet();
				numNonemptyIMethodBuckets++;
				numSingletonIMethodBuckets++;
			} else if (iMethodBuckets[bucket].size() == 1
			    && !iMethodBuckets[bucket].contains(m))
				numSingletonIMethodBuckets--;
			iMethodBuckets[bucket].add(m);
		}
	}

	/**
	 * Add dependencies on all classes that could, if changed,
	 * require this class's JC source file to be regenerated
	 * and/or recompiled. This includes headers where any structures
	 * we use are defined.
	 */
	protected void prepareDependencies() {

		// Add this class and java.lang.Class, both of which
		// are implicit method parameter types
		fullDepends.add(c);
		fullDepends.add(
		    Scene.v().loadClassAndSupport("java.lang.Class"));

		// Add java.lang.String, because we can have String constants
		fullDepends.add(
		    Scene.v().loadClassAndSupport("java.lang.String"));

		// Add all superclasses and superinterfaces
		Util.addSupertypes(c, fullDepends);

		// Add dependencies for all fields
		for (Iterator i = c.getFields().iterator(); i.hasNext(); ) {
			addDependency(fullDepends,
			    ((SootField)i.next()).getType());
		}

		// Add dependencies for all methods
		for (Iterator i = c.getMethods().iterator(); i.hasNext(); )
			addDependencies(fullDepends, (SootMethod)i.next());

		// Add dependencies for all inner classes and outer class
		for (int i = 0; i < innerClasses.length; i++)
			fullDepends.add(innerClasses[i].inner);
		if (outerClass != null)
			fullDepends.add(outerClass);

		// Remove all superclasses to get base dependency set
		baseDepends = (HashSet)fullDepends.clone();
		for (Iterator i = fullDepends.iterator(); i.hasNext(); ) {
			for (SootClass sc = (SootClass)i.next();
			    sc.hasSuperclass(); ) {
				sc = sc.getSuperclass();
				baseDepends.remove(sc);
			}
		}
	}

	/**
	 * Add to <code>set</code> the {@link SootClass} associated with
	 * {@link Type} <code>t</code>, if any.
	 */
	protected void addDependency(Set set, Type t) {
		if (t instanceof ArrayType)
			t = ((ArrayType)t).baseType;
		if (t instanceof RefType)
			set.add(((RefType)t).getSootClass());
	}

	/**
	 * Add all SootClasses to <code>set</code> that are referenced by
	 * the method signature, throws clause, locals, traps, or body
	 * instructions.
	 */
	protected void addDependencies(final Set set, SootMethod m) {

		// Add return type
		addDependency(set, m.getReturnType());

		// Add parameter types
		for (int i = 0; i < m.getParameterCount(); i++)
			addDependency(set, m.getParameterType(i));

		// Add thrown exception types
		for (Iterator i = m.getExceptions().iterator(); i.hasNext(); )
			set.add((SootClass)i.next());

		// Stop here if there's no body
		if (m.isAbstract() || m.isNative())
			return;

		// Synchronized methods will get a new java.lang.Throwable trap
		if (m.isSynchronized())
			set.add(Scene.v().getSootClass("java.lang.Throwable"));

		// Add locals' types
		Body body = m.retrieveActiveBody();
		for (Iterator i = body.getLocals().iterator(); i.hasNext(); )
			addDependency(set, ((Local)i.next()).getType());

		// Add traps
		for (Iterator i = body.getTraps().iterator(); i.hasNext(); )
			set.add(((Trap)i.next()).getException());

		// Add classes any of whose static fields or methods we use
		// or to whom we refer directly (e.g., via instanceof).
		for (Iterator i = body.getUseAndDefBoxes().iterator();
		    i.hasNext(); ) {
			ValueBox vb = (ValueBox)i.next();
			vb.getValue().apply(new AbstractJimpleValueSwitch() {
			    public void caseSpecialInvokeExpr(
			      SpecialInvokeExpr v) {
				caseInvokeExpr(v);
			    }
			    public void caseStaticInvokeExpr(
			      StaticInvokeExpr v) {
				caseInvokeExpr(v);
			    }
			    public void caseVirtualInvokeExpr(
			      VirtualInvokeExpr v) {
				caseInvokeExpr(v);
			    }
			    private void caseInvokeExpr(InvokeExpr v) {
				set.add(
				  v.getMethod().getDeclaringClass());
			    }
			    public void caseStaticFieldRef(StaticFieldRef v) {
				set.add(
				  v.getField().getDeclaringClass());
			    }
			    public void caseCastExpr(CastExpr v) {
				addDependency(set, v.getCastType());
			    }
			    public void caseInstanceOfExpr(InstanceOfExpr v) {
				addDependency(set, v.getCheckType());
			    }
			    public void caseNewArrayExpr(NewArrayExpr v) {
				addDependency(set, v.getType());
			    }
			    public void caseNewMultiArrayExpr(
					NewMultiArrayExpr v) {
				addDependency(set, v.getType());
			    }
			    public void caseNewExpr(NewExpr v) { // needed?
				addDependency(set, v.getBaseType());
			    }
			    public void caseCaughtExceptionRef(
					CaughtExceptionRef v) {
				addDependency(set, v.getType());
			    }
			});
		}
	}

	public void outputInitialStuff() {

		// Output initial comments and #include lines
		// We #include headers for all classes we depend upon,
		// but not including any superclasses, because those
		// header files are included by base class header files.
		// Our dependency list is the full list (probably overkill)
		super.outputInitialStuff(
		    (SootClass[])baseDepends.toArray(
		      new SootClass[baseDepends.size()]),
		    (SootClass[])fullDepends.toArray(
		       new SootClass[fullDepends.size()]),
		    true);

/***
		// Do any direct included string
		Type stringType = RefType.v("java.lang.String");
		if (c.declaresField(includeStringName, stringType)) {
			SootField f = c.getField(includeStringName, stringType);
			if (f.isStatic()
			    && Util.isFinal(f) 
			    && f.hasTag("ConstantValue")) {
				byte[] value = f.getTag(
				    "ConstantValue").getValue();
				for (int i = 0; i < value.length; i++) {
					char ch = (char)(value[i] & 0xff);
					if (ch == '\n')
						out.println();
					else
						out.print(ch);
				}
			}
		}
***/
	}

	public void outputFields() {
		for (Iterator i = c.getFields().iterator(); i.hasNext(); )
			outputField((SootField)i.next());
	}

	public void outputField(SootField f) {
		out.println("// " + C.string(f.getDeclaration(), false));
		ConstantValueTag initialValue = getInitialValue(f);
		boolean isString = (f.getType() instanceof RefType)
		    && ((RefType)f.getType()).getSootClass().equals(
		      Scene.v().getSootClass("java.lang.String"));
		if (initialValue != null && !isString)
			outputInitialValue(f, initialValue);
		out.println("static _jc_field "
		    + prefix + "$field_info$" + C.name(f) + " = {");
		out.indent();
		out.println(".name=\t\t" + C.string(f.getName()) + ",");
		out.println(".signature=\t"
		    + C.string(Util.fieldDescriptor(f.getType())) + ",");
		out.println(".class=\t\t&" + prefix + "$type,");
		out.println(".type=\t\t&" + C.jc_type(f.getType()) + ",");
		out.println(".access_flags=\t" + C.accessDefs(f) + ",");
		out.print(".offset=\t");
		if (f.isStatic()) {
			out.println("_JC_OFFSETOF("
			    + prefix + "$fields_struct, " + C.name(f) + "),");
		} else if (Util.isReference(f)) {	/* { */
			out.println("_JC_OFFSETOF("
			    + prefix + "$object, refs[-1]."
			    + cname + "." + C.name(f) + "),");
		} else {				/* { */
			out.println("_JC_OFFSETOF("
			    + prefix + "$object, nonrefs."
			    + cname + "." + C.name(f) + "),");
		}
		if (initialValue != null) {
			out.print(".initial_value=\t");
			if (isString) {
				out.print(C.string(
				    ((StringConstantValueTag)initialValue)
				      .getStringValue()));
			} else {
				out.print("&" + prefix
				    + "$initial_value$" + C.name(f));
			}
			out.println(",");
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public ConstantValueTag getInitialValue(SootField f) {

		// Only static fields can have initial values
		if (!f.isStatic())
			return null;

		// Look for ConstantValue tag
		for (Iterator i = f.getTags().iterator(); i.hasNext(); ) {
			Tag tag = (Tag)i.next();
			if (tag instanceof ConstantValueTag)
				return (ConstantValueTag)tag;
		}
		return null;
	}

	public void outputInitialValue(final SootField f,
	    final ConstantValueTag cvTag) {
		TypeSwitch ts = new TypeSwitch() {
		    public void caseBooleanType(BooleanType t) {
			IntegerConstantValueTag tag
			    = (IntegerConstantValueTag)cvTag;
			out.println("static const jboolean " + prefix
			    + "$initial_value$" + C.name(f) + " = "
			    + tag.getIntValue() + ";");
		    }
		    public void caseByteType(ByteType t) {
			IntegerConstantValueTag tag
			    = (IntegerConstantValueTag)cvTag;
			out.println("static const jbyte " + prefix
			    + "$initial_value$" + C.name(f) + " = "
			    + tag.getIntValue() + ";");
		    }
		    public void caseCharType(CharType t) {
			IntegerConstantValueTag tag
			    = (IntegerConstantValueTag)cvTag;
			out.println("static const jchar " + prefix
			    + "$initial_value$" + C.name(f) + " = "
			    + tag.getIntValue() + ";");
		    }
		    public void caseShortType(ShortType t) {
			IntegerConstantValueTag tag
			    = (IntegerConstantValueTag)cvTag;
			out.println("static const jshort " + prefix
			    + "$initial_value$" + C.name(f) + " = "
			    + tag.getIntValue() + ";");
		    }
		    public void caseIntType(IntType t) {
			IntegerConstantValueTag tag
			    = (IntegerConstantValueTag)cvTag;
			out.println("static const jint " + prefix
			    + "$initial_value$" + C.name(f) + " = "
			    + tag.getIntValue() + ";");
		    }
		    public void caseLongType(LongType t) {
			LongConstantValueTag tag = (LongConstantValueTag)cvTag;
			out.println("static const jlong " + prefix
			    + "$initial_value$" + C.name(f) + " = _JC_JLONG("
			    + tag.getLongValue() + ");");
		    }
		    public void caseFloatType(FloatType t) {
			FloatConstantValueTag tag
			    = (FloatConstantValueTag)cvTag;
			out.print("static const unsigned char " + prefix
			    + "$initial_value$" + C.name(f) + "[] = {");
			byte[] bytes = tag.getValue();
			for (int i = 0; i < 4; i++) {
			    if (i > 0)
				out.print(",");
			    out.print(" 0x"
				+ Integer.toHexString(bytes[i] & 0xff));
			}
			out.println(" };");
		    }
		    public void caseDoubleType(DoubleType t) {
			DoubleConstantValueTag tag
			    = (DoubleConstantValueTag)cvTag;
			out.print("static const unsigned char " + prefix
			    + "$initial_value$" + C.name(f) + "[] = {");
			byte[] bytes = tag.getValue();
			for (int i = 0; i < 8; i++) {
			    if (i > 0)
				out.print(",");
			    out.print(" 0x"
				+ Integer.toHexString(bytes[i] & 0xff));
			}
			out.println(" };");
		    }
		    public void caseRefType(RefType t) {	// String
			StringConstantValueTag tag
			    = (StringConstantValueTag)cvTag;
			out.println("static const char *" + prefix
			    + "$initial_value$" + C.name(f) + " = "
			    + C.string(tag.getStringValue()) + ";");
		    }
		    public void defaultCase(Type t) {
			Util.panic("bogus type " + t);
		    }
		};
		f.getType().apply(ts);
	}

	public void outputStaticFieldStructure() {
		out.println(prefix + "$fields_struct "
		    + prefix + "$class_fields = { };");
		out.println();
	}

	public void outputMethods(SootMethod[] list) {
		for (int i = 0; i < list.length; i++)
			outputMethod(list[i]);
	}

	public void outputMethod(SootMethod m) {
		out.println("//");
		out.println("// " + C.string(m.getDeclaration(), false));
		out.println("//");
		out.println();
		CMethod cm = (CMethod)methodMap.get(m);
		cm.outputMethodInfo();
		cm.outputMethodFunction();
	}

	public void outputFieldList(SootField[] fields,
			String comment, String label) {
		if (fields.length == 0)
			return;
		outputCommentLine(comment);
		out.println("static _jc_field *const "
		    + prefix + "$" + label + "[] = {");
		out.indent();
		for (int i = 0; i < fields.length; i++) {
			out.print("&" + prefix + "$field_info$"
			    + C.name(fields[i]));
			if (i < fields.length - 1)
				out.print(',');
			out.println();
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputMethodList(SootMethod[] methods,
			String comment, String label) {
		if (methods.length == 0)
			return;
		outputCommentLine(comment);
		out.println("static _jc_method *const "
		    + prefix + "$" + label + "[] = {");
		out.indent();
		for (int i = 0; i < methods.length; i++) {
			out.print("&" + prefix + "$method_info$"
			    + C.name(methods[i]));
			if (i < methods.length - 1)
				out.print(',');
			out.println();
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputInterfaceList() {

		// List directly implemented interfaces
		outputCommentLine("Directly implemented interfaces");
		out.println("static _jc_type *const "
		    + prefix + "$interfaces[] = {");
		out.indent();
		Collection ifaces = c.getInterfaces();
		SootClass[] list = (SootClass[])ifaces.toArray(
		    new SootClass[ifaces.size()]);
		for (int i = 0; i < list.length; i++) {
			out.print("&_jc_" + C.name(list[i]) + "$type");
			if (i < list.length - 1)
				out.print(',');
			out.println();
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputInterfaceHashTable() {

		// Sanity check
		out.println("#if _JC_IMETHOD_HASHSIZE != " + IMETHOD_HASHSIZE);
		out.println("#error \"_JC_IMETHOD_HASHSIZE has the wrong"
		    + " value (should be " + IMETHOD_HASHSIZE + ")\"");
		out.println("#endif");
		out.println();

		outputCommentLine("Interface lookup hash table buckets");
		for (int i = 0; i < IMETHOD_HASHSIZE; i++) {
			if (iMethodBuckets[i] == null)
				continue;
			out.println("static _jc_method *"
			    + prefix + "$imethod_hash_bucket_" + i + "[] = {");
			out.indent();
			SootMethod[] list = (SootMethod[])iMethodBuckets[i]
			    .toArray(new SootMethod[iMethodBuckets[i].size()]);
			Arrays.sort(list, Util.methodComparator);
			for (int j = 0; j < list.length; j++) {
				SootClass sc = list[j].getDeclaringClass();
				out.println("&_jc_" + C.name(sc)
				    + "$method_info$" + C.name(list[j]) + ",");
			}
			out.println("_JC_NULL");
			out.undent();
			out.println("};");
		}
		out.println();

		// Output the interface hash table itself
		outputCommentLine("Interface lookup hash table");
		out.println("static _jc_method **"
		    + prefix + "$imethod_hash_table[_JC_IMETHOD_HASHSIZE] = {");
		out.indent();
		int count = 0;
		for (int i = 0; i < IMETHOD_HASHSIZE; i++) {
			if (iMethodBuckets[i] == null)
				continue;
			out.print("[" + i + "]=\t"
			    + prefix + "$imethod_hash_bucket_" + i);
			if (++count < numNonemptyIMethodBuckets)
				out.print(',');
			out.println();
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputInterfaceQuickTable() {

		outputCommentLine("Interface \"quick\" lookup hash table");
		out.println("static const void *" + prefix
		    + "$imethod_quick_table[_JC_IMETHOD_HASHSIZE] = {");
		out.indent();
		int count = 0;
		for (int i = 0; i < IMETHOD_HASHSIZE; i++) {
			if (iMethodBuckets[i] == null
			    || iMethodBuckets[i].size() != 1)
				continue;
			SootMethod m = (SootMethod)
			    iMethodBuckets[i].iterator().next();
			SootClass sc = m.getDeclaringClass();
			out.print("[" + i + "]=\t&_jc_"
			    + C.name(sc) + "$method$" + C.name(m));
			if (++count < numSingletonIMethodBuckets)
				out.print(',');
			out.println();
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputVtable() {

		// Sanity check
		if (c.isInterface())
			throw new RuntimeException("assertion failure");

		// Compute virtual method table (signature -> class mapping)
		HashMap virt = new HashMap();
		for (int i = superclasses.size() - 1; i >= 0; i--) {
			SootClass sc = (SootClass)superclasses.get(i);
			for (Iterator j = sc.getMethods().iterator();
			    j.hasNext(); ) {
				SootMethod m = (SootMethod)j.next();
				if (Util.isVirtual(m))
					virt.put(m.getSubSignature(), sc);
			}
		}

		// Output virtual method dispatch table
		for (int i = superclasses.size() - 1; i >= 0; i--) {
			SootClass sc = (SootClass)superclasses.get(i);
			out.println("." + C.name(sc) + "= {");
			SootMethod[] list = (SootMethod[])sc.getMethods()
			    .toArray(new SootMethod[sc.getMethods().size()]);
			Arrays.sort(list, Util.methodComparator);
			for (int j = 0; j < list.length; j++) {
				SootMethod m = list[j];
				if (!Util.isVirtual(m))
					continue;
				out.print("    ." + C.name(m) + "=");
				SootClass mc = (SootClass)virt.get(
				    m.getSubSignature());
				if (!sc.equals(mc)) {
					spaceFillTo(16
					    + C.name(m).length() + 2, 64);
					out.print("// overridden");
				}
				out.println();
				out.indent();
				out.println("&_jc_" + C.name(mc)
				    + "$method$" + C.name(m) + ",");
				out.undent();
			}
			out.println("},");
		}
	}

	public void outputInstanceOfHashTable() {

		// Sanity check
		out.println("#if _JC_INSTANCEOF_HASHSIZE != "
		    + INSTANCEOF_HASHSIZE);
		out.println("#error \"_JC_INSTANCEOF_HASHSIZE has the wrong"
		    + " value (should be " + INSTANCEOF_HASHSIZE + ")\"");
		out.println("#endif");
		out.println();

		// Create set of classes of which we are an instance
		Set set = Util.getAllSupertypes(c);

		// Generate hash table
		HashSet[] buckets = new HashSet[INSTANCEOF_HASHSIZE];
		for (Iterator i = set.iterator(); i.hasNext(); ) {
			SootClass sc = (SootClass)i.next();
			int bucket = Util.instanceofHash(sc);
			if (buckets[bucket] == null)
				buckets[bucket] = new HashSet();
			buckets[bucket].add(sc);
		}

		// Output hash table buckets
		outputCommentLine("Instanceof hash table buckets");
		int numNonemptyBuckets = 0;
		for (int i = 0; i < INSTANCEOF_HASHSIZE; i++) {
			if (buckets[i] == null)
				continue;
			numNonemptyBuckets++;
			out.println("static _jc_type *const "
			    + prefix + "$instanceof_hash_bucket_"
			    + i + "[] = {");
			out.indent();
			SootClass[] list = (SootClass[])buckets[i]
			    .toArray(new SootClass[buckets[i].size()]);
			Arrays.sort(list, Util.classComparator);
			for (int j = 0; j < list.length; j++) {
				out.println("&_jc_"
				    + C.name(list[j]) + "$type" + ",");
			}
			out.println("_JC_NULL");
			out.undent();
			out.println("};");
		}
		out.println();

		// Output the hash table itself
		outputCommentLine("Instanceof hash table");
		out.println("static _jc_type *const *const " + prefix
		    + "$instanceof_hash_table[_JC_INSTANCEOF_HASHSIZE] = {");
		out.indent();
		int count = 0;
		for (int i = 0; i < INSTANCEOF_HASHSIZE; i++) {
			if (buckets[i] == null)
				continue;
			out.print("[" + i + "]=\t"
			    + prefix + "$instanceof_hash_bucket_" + i);
			if (++count < numNonemptyBuckets)
				out.print(',');
			out.println();
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputInnerClases() {
		if (innerClasses.length == 0)
			return;
		outputCommentLine("Inner class list");
		out.println("static _jc_inner_class "
		    + prefix + "$inner_classes[] = {");
		out.indent();
		for (int i = 0; i < innerClasses.length; i++) {
			InnerClass inner = innerClasses[i];
			out.println("{ &_jc_" + C.name(inner.inner) + "$type, "
			    +  C.accessDefs(inner.flags) + " },");
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputDepedencyList() {
		outputCommentLine("Class dependency info");
		out.println("static _jc_class_depend "
		    + prefix + "$class_depends[] = {");
		out.indent();
		SootClass[] list = (SootClass[])fullDepends.toArray(
		    new SootClass[fullDepends.size()]);
		Arrays.sort(list, Util.classComparator);
		for (int i = 0; i < list.length; i++) {
			SootClass sc = list[i];
			if (sc.equals(c))	// don't depend on self
				continue;
			out.println("{ "
			    + C.string(sc.getName().replace('.', '/'))
			    + ",\t_JC_JLONG(0x"
			    + Long.toHexString(Util.classHash(sc)) + ") },");
		}
		out.undent();
		out.println("};");
		out.println();
	}

	public void outputClassInfo() {
		outputCommentLine("Class info");

		// Output a vtype structure
		out.println(prefix + "$vtype " + prefix + "$type = {");

		// Output type sub-structure
		out.println("    .type= {");
		out.indent();

		// Output initial info
		out.println(".name=\t\t\t"
		    + C.string(c.getName().replace('.', '/')) + ",");
		out.println(".superclass=\t\t"
		    + (c.hasSuperclass() ?
		      ("&_jc_" + C.name(c.getSuperclass()) + "$type") :
		      "_JC_NULL")
		    + ",");
		out.println(".access_flags=\t\t" + C.accessDefs(c) + ",");
		out.println(".flags=\t\t\t_JC_TYPE_REFERENCE,");

		// Output interface info
		if (c.getInterfaceCount() > 0) {
			out.println(".num_interfaces=sizeof("
			    + prefix + "$interfaces) / sizeof(*"
			    + prefix + "$interfaces),");
			out.println(".interfaces=\t\t"
			    + prefix + "$interfaces,");
		}
		out.print(".imethod_hash_table=\t");
		if (numNonemptyIMethodBuckets > 0) {
			out.println(prefix + "$imethod_hash_table,");
		} else
			out.println("_jc_empty_imethod_table,");
		out.print(".imethod_quick_table=\t");
		if (numSingletonIMethodBuckets > 0)
			out.println(prefix + "$imethod_quick_table,");
		else
			out.println("_jc_empty_quick_table,");

		// Count total number of declared virtual methods
		int totalVirtualMethods = 0;
		for (Iterator i = superclasses.iterator(); i.hasNext(); ) {
			SootClass sc = (SootClass)i.next();
			for (Iterator j = sc.getMethods().iterator();
			    j.hasNext(); ) {
				SootMethod m = (SootMethod)j.next();
				if (Util.isVirtual(m))
				    	totalVirtualMethods++;
			}
		}

		// Begin non-array info
		out.println(".u= {");
		out.println("    .nonarray= {");
		out.indent();
		out.println(".block_size_index=\t_JC_LIBJC_VERSION,");
		out.println(".num_vmethods=\t\t" + totalVirtualMethods + ",");
		out.println(".hash=\t\t\t" + "_JC_JLONG(0x"
		    + Long.toHexString(Util.classHash(c)) + "),");
		out.println(".source_file=\t\t" + C.string(sourceFile) + ",");

		// Output instance size and offset
		if (!c.isInterface()) {
			out.println(".instance_size=\t\tsizeof("
			    + prefix + "$refs) + sizeof("
			    + prefix + "$object),");
		}

		out.println(".num_virtual_refs=\t" + numVirtualRefFields + ",");

		// Output fields and methods
		outputMemberList(staticFields.length + virtualFields.length,
		    "fields");
		outputMemberList(staticMethods.length + constructors.length
		    + virtualMethods.length, "methods");

		// Output instanceof hash table
		out.println(".instanceof_hash_table=\t"
		    + prefix + "$instanceof_hash_table,");

		// Output class fields
		if (staticFields.length > 0) {
			out.println(".class_fields=\t\t"
			    + "&" + prefix + "$class_fields,");
		}

		// Output inner class info
		if (innerClasses.length > 0) {
			out.println(".num_inner_classes=\t"
			    + innerClasses.length + ",");
			out.println(".inner_classes=\t\t"
			    + prefix + "$inner_classes,");
		}
		if (outerClass != null) {
			out.println(".outer_class=\t\t"
			    + "&_jc_" + C.name(outerClass) + "$type,");
		}

		// Output class dependency info
		out.println(".num_class_depends=\t\tsizeof("
		  + prefix + "$class_depends) / sizeof(*"
		  + prefix + "$class_depends),");
		out.println(".class_depends=\t\t" + prefix + "$class_depends,");

		// End non-array info
		out.undent();
		out.println("    }");
		out.println("}");

		// End _jc_type sub-structure
		out.undent();
		out.println("    }" + (!c.isInterface() ? "," : ""));

		// Output vtable for non-interface classes
		if (!c.isInterface()) {
			out.println("    .vtable= {");
			out.indent();
			outputVtable();
			out.undent();
			out.println("    }");
		}

		// End vtype
		out.println("};");
		out.println();
	}

	private void outputMemberList(int length, String label) {
		if (length == 0)
			return;
		out.println(".num_" + label + "=\t\tsizeof(" + prefix + "$"
		    + label + ") / sizeof(*" + prefix + "$" + label + "),");
		out.println("." + label + "=\t\t" + prefix + "$" + label + ",");
	}
}

