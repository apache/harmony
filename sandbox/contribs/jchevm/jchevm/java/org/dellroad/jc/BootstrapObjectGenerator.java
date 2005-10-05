
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
// $Id: BootstrapObjectGenerator.java,v 1.8 2004/12/27 16:04:33 archiecobbs Exp $
//

package org.dellroad.jc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.dellroad.jc.cgen.JCObjectGenerator;
import org.dellroad.jc.cgen.SootCodeGenerator;

/**
 * Used to pre-generate ELF object files for classes that JC itself uses
 * to generate ELF object files. This class is meant to be executed
 * as a separate process, typically using a JVM other than JC.
 */
public class BootstrapObjectGenerator {

	private final ObjectGenerator generator;
	private final SearchpathMatcher[] matchers;
	private final SearchpathFinder finder;
	private final HashSet generated = new HashSet();
	private final boolean verbose;
	private final boolean force;
	private File objectDir;

	static abstract class SearchpathMatcher {
		private final String name;
		public SearchpathMatcher(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
		public String[] getMatches(String pattern) {

			// Parse pattern
			pattern = pattern.replace('.', '/');
			final int plen = pattern.length();
			if (plen == 0)
				return new String[0];
			String prefix;
			int looseness;
			if (pattern.charAt(plen - 1) != '%') {
				looseness = 0;
				prefix = pattern;
			} else if (plen < 2
			    || pattern.charAt(plen - 2) != '%') {
				looseness = 1;
				prefix = pattern.substring(0, plen - 1);
			} else {
				looseness = 2;
				prefix = pattern.substring(0, plen - 2);
			}

			// Return matches
			while (prefix.endsWith("/")) {
				prefix = prefix.substring(0,
				    prefix.length() - 1);
			}
			Set matches = getMatches(prefix, looseness);
			String[] array = (String[])matches.toArray(
			    new String[matches.size()]);
			Arrays.sort(array);
			return array;
		}
		public boolean isMatch(String name,
		    String prefix, int looseness) {
			int plen = prefix.length();
			switch (looseness) {
			case 0:
				if (!name.equals(prefix))
					return false;
				break;
			case 1:
				if (name.lastIndexOf('/') > plen)
					return false;
				// FALLTHROUGH
			case 2:
				if (name.length() < plen + 2
				    || !name.startsWith(prefix))
					return false;
				if (name.charAt(plen) != '/')
					return false;
				break;
			}
			return true;
		}

		public abstract Set getMatches(String prefix, int looseness);
	}

	static class DirMatcher extends SearchpathMatcher {
		private final File topDir;
		public DirMatcher(File topDir) throws IOException {
			super(topDir.toString());
			if (!topDir.isDirectory()) {
				throw new IOException("`" + topDir
				    + "' is not a directory");
			}
			this.topDir = topDir;
		}
		public Set getMatches(String prefix, int looseness) {
			if (looseness == 0) {
				if (prefix.length() == 0)
					return Collections.EMPTY_SET;
				File file = new File(topDir, prefix + ".class");
				if (file.isFile())
					return Collections.singleton(prefix);
				return Collections.EMPTY_SET;
			}
			File dir = prefix.length() == 0 ?
			    topDir : new File(topDir, prefix);
			if (!dir.isDirectory())
				return Collections.EMPTY_SET;
			HashSet set = new HashSet();
			addFiles(dir, prefix, looseness == 2, set);
			return set;
		}
		private void addFiles(File dir, String prefix,
		    boolean subpackages, Set set) {
			String[] files = dir.list();
			if (files == null)
				return;
			for (int i = 0; i < files.length; i++) {
				String name = files[i];
				File file = new File(dir, name);
				String prefixName = (prefix.length() == 0) ?
				    name : prefix + "/" + name;
				if (file.isFile()) {
					if (!name.endsWith(".class"))
						continue;
					prefixName = prefixName.substring(
					    0, prefixName.length() - 6);
					set.add(prefixName);
					continue;
				}
				if (file.isDirectory() && subpackages)
					addFiles(file, prefixName, true, set);
			}
		}
	}

	static class ZipMatcher extends SearchpathMatcher {
		private final ZipFile zfile;
		public ZipMatcher(File zip) throws ZipException, IOException {
			super(zip.toString());
			this.zfile = new ZipFile(zip);
		}
		public Set getMatches(String prefix, int looseness) {
			HashSet set = new HashSet();
			final Enumeration e = zfile.entries();
			if (e == null)		// Classpath bug workaround
				return Collections.EMPTY_SET;
			while (e.hasMoreElements()) {
			    	ZipEntry entry = (ZipEntry)e.nextElement();
				String name = entry.getName();
				if (!name.endsWith(".class"))
					continue;
				name = name.substring(0, name.length() - 6);
				if (isMatch(name, prefix, looseness))
					set.add(name);
			}
			return set;
		}
		protected void finalize() throws Throwable {
			try {
				zfile.close();
			} catch (IOException e) {
			}
			super.finalize();
		}
	}

	private BootstrapObjectGenerator(String classpath, String srcpath,
	    String objdir, boolean force, boolean verbose,
	    boolean includeLineNumbers, boolean sourcesOnly) {

		// Initialize instance fields
		this.generator = new JCObjectGenerator(
		    new SootCodeGenerator(null, null, includeLineNumbers),
		    srcpath, verbose, includeLineNumbers, sourcesOnly, force);
		this.objectDir = new File(
		    SearchpathFinder.splitPath(objdir)[0]);
		this.finder = new SearchpathFinder(classpath);
		this.verbose = verbose;
		this.force = force;

		// Create matcher for each classpath component
		String[] cpath = SearchpathFinder.splitPath(classpath);
		ArrayList list = new ArrayList();
		for (int i = 0; i < cpath.length; i++) {
			String entry = cpath[i];
			try {
				File file = new File(entry);
				list.add(file.isDirectory() ?
				    (SearchpathMatcher)new DirMatcher(file) :
				    (SearchpathMatcher)new ZipMatcher(file));
			} catch (ZipException e) {
			} catch (IOException e) {
			}
		}
		matchers = (SearchpathMatcher[])list
		    .toArray(new SearchpathMatcher[list.size()]);
	}

	private void generateMatches(String pattern) throws Exception {
		boolean single = !pattern.endsWith("%");
		boolean found = false;
		for (int i = 0; i < matchers.length; i++) {
			SearchpathMatcher matcher = matchers[i];
			String[] matches = matcher.getMatches(pattern);
			for (int j = 0; j < matches.length; j++) {
				generate(matches[j]);
				if (single) {
					found = true;
					break;
				}
			}
		}
		if (single && !found)
			System.err.println("class `" + pattern + "' not found");
	}

	private void generate(String className) throws Exception {

		// Get slashed name
		className = className.replace('.', '/');
		File ofile = Generate.objectFile(className, objectDir);

		// Don't generate twice (e.g., if a class file appears
		// twice in the class path)
		if (generated.contains(className))
			return;
		generated.add(className);

		// Check if object file is already there
		if (!force
		    && generator.objectIsValid(className, finder, ofile)) {
			if (verbose) {
				System.out.println("`" + className
				    + "' is up to date");
			}
			return;
		}

		// Generate object
		generator.generateObject(className, finder, ofile);
	}

	/**
	 * Generate JC ELF object files for one or more classes. This class
	 * is intended for use in two cases:
	 *
	 * <ul>
	 * <li>When JC must generate ELF object files that are themselves
	 *	used to generate ELF object files, it must bootstrap generate
	 *	them by invoking this class in another Java virtual machine
	 *	as a separate process.
	 * <li>When it is desirable to pre-generate ELF objects (instead of
	 *	having them created on demand during the execution of the JC
	 *	virtual machine) to avoid runtime delays or to obviate then
	 *	need for the GCC compiler.
	 * </ul>
	 *
	 * <p>
	 * Usage:
	 * <blockquote>
	 * <code>java org.dellroad.jc.BootstrapObjectGenerator [-classpath dir1:dir2:...]
	 * [-srcpath dir1:dir2:...] [-objdir objdir] [-incdir dir] [-f] [-g] [-N] pattern ...</code>
	 * </blockquote>
	 * </p>
	 *
	 * <p>
	 * ELF objects are (re)generated for all classes found in the
	 * classpath specified by <code>-classpath</code> (or the
	 * <code>java.class.path</code> system property if no
	 * <code>-classpath</code> flag is given) that match any of the
	 * given <code>pattern</code>s. A pattern is either a class name,
	 * or a package name followed by a period and then one or two
	 * asterisks.  One asterisk matches all classes in the package,
	 * two matches all classes in the package and all subpackages. For
	 * example, <code>java.util.List</code>, <code>java.util.*</code>,
	 * and <code>java.util.**</code>. Slashes may be used instead of
	 * periods and percent signs may be used instead of asterisks.
	 * </p>
	 * <p>
	 * Objects stored in the directory hierarchy whose root is specified
	 * by <code>-objdir</code> flag (default current working directory).
	 * As a convenience feature, if <code>-objdir</code> specifies a
	 * search path, the first directory in the path is used.
	 * Intermediate C source and header files are searched for
	 * in the search path specified by <code>-srcpath</code> and
	 * those newly created (as necessary) are stored under the
	 * first component directory of <code>-srcpath</code>. If no
	 * <code>-srcpath</code> is given, &quot;.&quot; is assumed.
	 * <code>-incdir</code> tells where the JC include files live.
	 * </p>
	 * <p>
	 * If the <code>-f</code> flag is given, then all ELF object files
	 * are regenerated even if a seemingly valid ELF file already exists.
	 * </p>
	 * <p>
	 * If the <code>-g</code> flag is given, then support for Java
	 * source code line numbers in stack traces is included.
	 * </p>
	 * <p>
	 * If the <code>-N</code> flag is given, only the source files are
	 * generated; C compilation is not done and no ELF objects are created.
	 * </p>
	 * <p>
	 * The <code>jc.verbose.gen</code> system property, if equal to
	 * <code>true</code>, enables progress reports to standard output.
	 * Set via the <code>-Djc.verbose.gen=true</code> VM command line flag.
	 * </p>
	 * <p>
	 * This class uses the {@link JCObjectGenerator JCObjectGenerator}
	 * class to generate ELF objects.
	 * </p>
	 *
	 * @see JCObjectGenerator JCObjectGenerator
	 */
	public static void main(String args[]) {
		try {
			doMain(args);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	private static void doMain(String args[]) throws Exception {

		String classpath = null;
		String objdir = ".";
		String srcpath = ".";
		String incdir = "/usr/local/jc/include";
		boolean badUsage = false;
		boolean force = false;
		boolean includeLineNumbers = Boolean.getBoolean(
		    "jc.include.line.numbers");
		boolean sourcesOnly = false;
		int argi;

		// Parse command line
		for (argi = 0; argi < args.length; argi++) {
			String arg = args[argi];
			if (arg.length() < 1 || arg.charAt(0) != '-')
				break;
			if (arg.equals("-classpath")) {
				if (++argi >= args.length) {
					badUsage = true;
					break;
				}
				classpath = args[argi];
			} else if (arg.equals("-srcpath")) {
				if (++argi >= args.length) {
					badUsage = true;
					break;
				}
				srcpath = args[argi];
			} else if (arg.equals("-objdir")) {
				if (++argi >= args.length) {
					badUsage = true;
					break;
				}
				objdir = args[argi];
			} else if (arg.equals("-incdir")) {
				if (++argi >= args.length) {
					badUsage = true;
					break;
				}
				incdir = args[argi];
			} else if (arg.equals("-f")) {
				force = true;
			} else if (arg.equals("-g")) {
				includeLineNumbers = true;
			} else if (arg.equals("-N")) {
				sourcesOnly = true;
			} else {
				System.err.println("unknown flag ``"
				    + arg + "''");
				badUsage = true;
				break;
			}
		}
		if (argi == args.length)
			badUsage = true;
		if (badUsage) {
			String cname = BootstrapObjectGenerator.class.getName();
			System.err.println("usage:\tjava " + cname
			    + " [-classpat dir1:dir2:...]");
			System.err.println("\t[-srcpath dir1:dir2:...]"
			    + " [-objdir objdir] [-incdir dir] [-f] [-g] [-N]"
			    + " pattern ...");
			System.exit(1);
		}

		// Fall back to system class path
		if (classpath == null)
			classpath = System.getProperty("java.class.path");

		// Set required JC properties if not already set
		String[][] defaults = {
		    { "jc.gnu.compiler",	"gcc" },
		    { "jc.include.dir",		incdir },
		};
		for (int i = 0; i < defaults.length; i++) {
			String name = defaults[i][0];
			String value = defaults[i][1];
			if (System.getProperty(name) == null)
				System.setProperty(name, value);
		}

		// Create bootstrap generator
		BootstrapObjectGenerator bgen = new BootstrapObjectGenerator(
		    classpath, srcpath, objdir, force,
		    Boolean.getBoolean("jc.verbose.gen"), includeLineNumbers,
		    sourcesOnly);

		// Generate objects
		while (argi < args.length)
			bgen.generateMatches(args[argi++].replace('*', '%'));
	}
}

