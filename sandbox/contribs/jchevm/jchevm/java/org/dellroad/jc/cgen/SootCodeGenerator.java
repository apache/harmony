
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
// $Id: SootCodeGenerator.java,v 1.7 2005/02/22 15:14:02 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.dellroad.jc.ClassfileFinder;
import org.dellroad.jc.Generate;
import org.dellroad.jc.SearchpathFinder;
import soot.G;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.jimple.toolkits.pointer.CastCheckEliminatorDumper;
import soot.options.Options;

/**
 * JC's default source file generator. This class generates C code by
 * first analyzing class files using the
 * <a href="http://www.sable.mcgill.ca/soot/">Soot</a> framework, and
 * then converting byte code into C statements using the various other
 * helper classes in this package.
 */
public class SootCodeGenerator implements CodeGenerator {

	private static final String DEFAULT_METHOD_OPTIMIZER
	    = "org.dellroad.jc.cgen.DefaultMethodOptimizer";

	/**
	 * Current class file finder. This field is made package-visible
	 * because other classes in this package need access to it.
	 */
	static ClassfileFinder finder;

	private final SourceLocator sourceLocator;
	private final MethodOptimizer optimizer;
	private final boolean includeLineNumbers;

	/**
	 * Instantiate. Only one instance of this class should
	 * be used at a time.
	 *
	 * @param sourceLocator How Soot should retrieve class files,
	 *	or <code>null</code> in which case the <code>finder</code>
	 *	parameter to {@link #generateC generateC()} and
	 *	{@link #generateH generateH()} must be an instance of
	 *	{@link SearchpathFinder SearchpathFinder}.
	 * @param optimizer Object for optimizing method bodies, or
	 *	<code>null</code> to use the default.
	 */
	public SootCodeGenerator(SourceLocator sourceLocator,
	    MethodOptimizer optimizer, boolean includeLineNumbers) {

		// Get default optimizer
		if (optimizer == null) {
			try {
				optimizer = (MethodOptimizer)Class.forName(
				    System.getProperty("jc.method.optimizer",
				      DEFAULT_METHOD_OPTIMIZER)).newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// Save parameters
		this.sourceLocator = sourceLocator;
		this.optimizer = optimizer;
		this.includeLineNumbers = includeLineNumbers;
	}

	/**
	 * Reset Soot.
	 */
	public void reset() {

		// Reset Soot
		G.reset();
		System.gc();

		// Set global Soot options
		Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(true);

		// Set Jimple optimization options
		PhaseOptions.v().setPhaseOption("jop", "enabled:true");
		PhaseOptions.v().setPhaseOption("tag.ln", "on");
		PhaseOptions.v().setPhaseOption("jb.lp", "on");
		PhaseOptions.v().setPhaseOption("jap", "enabled:true");
		PhaseOptions.v().setPhaseOption("jap.npc", "on");
		//PhaseOptions.v().setPhaseOption("jap.abc", "with-cse:true");
		PhaseOptions.v().setPhaseOption("jap.abc", "on");
		PhaseOptions.v().setPhaseOption("jap.abc",
		    "with-classfield:true");

		// Include cast check elimination tagging
		PackManager.v().getPack("jap").add(
		    new Transform("jap.cce", CastCheckEliminatorDumper.v()));
		PhaseOptions.v().setPhaseOption("jap.cce", "on");

		// We have to force Soot to use our custom SourceLocator
		// using a native method because Soot doesn't let you
		if (sourceLocator != null) {
			setField(G.v(), "instanceSourceLocator",
			    "Lsoot/util/SourceLocator;", sourceLocator);
		}
	}

	/**
	 * Wrapper for {@link #genH genH()} that handles properly
	 * configuring Soot to find class files.
	 */
	public final void generateH(String className,
	    ClassfileFinder finder, OutputStream output) throws Exception {
		if (sourceLocator != null)
			sourceLocator.setFinder(finder);
		else if (finder instanceof SearchpathFinder) {
			Options.v().set_soot_classpath(
			    ((SearchpathFinder)finder).getPath());
		} else
			throw new Exception("can't set Soot class finder");
		SootCodeGenerator.finder = finder;
		genH(className.replace('/', '.'), output);
	}

	/**
	 * Wrapper for {@link #genC genC()} that handles properly
	 * configuring Soot to find class files.
	 */
	public final void generateC(String className,
	    ClassfileFinder finder, OutputStream output) throws Exception {
		if (sourceLocator != null)
			sourceLocator.setFinder(finder);
		else if (finder instanceof SearchpathFinder) {
			Options.v().set_soot_classpath(
			    ((SearchpathFinder)finder).getPath());
		} else
			throw new Exception("can't set Soot class finder");
		SootCodeGenerator.finder = finder;
		genC(className.replace('/', '.'), output);
	}

	/**
	 * Generate the C header file for the class using the Soot framework.
	 * Subclasses should override this method instead of
	 # {@link #generateH generateH()}.
	 *
	 * @param className Class name with dots instead of slashes
	 */
	public void genH(String className, OutputStream output)
	    throws Exception {
		SootClass c = Scene.v().loadClassAndSupport(className);
		c.setApplicationClass();
		new HFile(c,
		    new BufferedWriter(
		    new OutputStreamWriter(output, "8859_1"))).output();
	}

	/**
	 * Generate the C source file for the class using the Soot framework.
	 * Subclasses should override this method instead of
	 * {@link #generateC generateC()}.
	 *
	 * @param className Class name with dots instead of slashes
	 */
	public void genC(String className, OutputStream output)
	    throws Exception {
		SootClass c = Scene.v().loadClassAndSupport(className);
		c.setApplicationClass();
		new CFile(c,
		    new BufferedWriter(
		    new OutputStreamWriter(output, "8859_1")),
		    optimizer, includeLineNumbers).output();
	}

	// This method is required to set a custom SourceLocator in Soot
	private static native void setField(Object o,
	    String name, String signature, Object val);
}


