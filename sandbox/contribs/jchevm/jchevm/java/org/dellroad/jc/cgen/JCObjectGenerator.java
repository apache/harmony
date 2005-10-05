
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
// $Id: JCObjectGenerator.java,v 1.8 2005/03/13 02:29:51 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.dellroad.jc.ClassfileFinder;
import org.dellroad.jc.ObjectGenerator;
import org.dellroad.jc.Generate;
import org.dellroad.jc.SearchpathFinder;

/**
 * The default JC object file generator class. This class uses a
 * {@link CodeGenerator} to generate C source and header files,
 * then compiles them into ELF object files using GCC.
 *
 * <p>
 * The generated source files are expected to contain initial
 * double-slash comment lines with <code>@dep_header</code> tags
 * for each required header file and <code>@dep_class</code> tags
 * for each class file on which the generated source depends.
 */
public class JCObjectGenerator implements ObjectGenerator {

	private final CodeGenerator codeGenerator;
	private final String sourcePath[];
	private final boolean verbose;
	private final boolean debugSymbols;
	private final boolean sourcesOnly;
	private final boolean force;

	/**
	 * Instantiate an object with an instance of
	 * {@link SootCodeGenerator} using a custom {@link SourceLocator}
	 * as the underlying source file generator, and use system properties
	 * to determine where source and object files go, whether to be
	 * verbose, and what {@link MethodOptimizer} to use. This constructor
	 * should only be used when JC is the JVM.
	 */
	public JCObjectGenerator() {
		this(new SootCodeGenerator(
		    new SourceLocator(null), null,
		      Boolean.getBoolean("jc.include.line.numbers")),
		    System.getProperty("jc.source.path", "."),
		    Boolean.getBoolean("jc.verbose.gen"),
		    Boolean.getBoolean("jc.include.line.numbers"),
		    false, Boolean.getBoolean("jc.gen.force"));
	}

	/**
	 * Instantiate an object using the supplied configuration.
	 *
	 * @param codeGenerator Object that can generate JC C source code.
	 * @param sourcePath Search path for C source and header files.
	 *	First component of path is where newly generated files go.
	 * @param verbose Whether to print verbose output to standard error
	 * @param debugSymbols Whether to compile with the <code>-g</code> flag.
	 * @param sourcesOnly If <code>true</code> generate sources only,
	 *	don't compile them into ELF objects.
	 */
	public JCObjectGenerator(CodeGenerator codeGenerator,
	    String sourcePath, boolean verbose, boolean debugSymbols,
	    boolean sourcesOnly, boolean force) {
		this.codeGenerator = codeGenerator;
		this.sourcePath = SearchpathFinder.splitPath(sourcePath);
		this.verbose = verbose;
		this.debugSymbols = debugSymbols;
		this.sourcesOnly = sourcesOnly;
		this.force = force;
	}

	/**
	 * Generate object file using analysis via Soot.
	 * 
	 * <p>
	 * This method is synchronized because Soot is not reentrant.
	 */
	public synchronized void generateObject(String className,
	    ClassfileFinder finder, File objectFile) throws Exception {
		int ci;

		// Reset generator
		codeGenerator.reset();

		// Verify existing C file or generate a new one
		if (force
		    || (ci = verifySource(className, true, finder)) == -1) {
			genCFile(className, finder);
			if ((ci = verifySource(className,
			    true, finder)) == -1) {
				throw new Exception("generated file `"
				    + cFile(className, 0) + "' is not valid");
			}
		}

		// Get list of header files required by the C file
		Set hdrs = readDeps(cFile(className, ci), "@dep_header", false);

		// Recursively ensure all required header files exist
		Set doneHdrs = new HashSet();
		while (hdrs.size() > 0) {
			int hi;

			// Get the next unverified header file dependency
			Iterator i = hdrs.iterator();
			Dep dep = (Dep)i.next();
			i.remove();

			// Verify existing header file or generate a new one
			if (force
			    || (hi = verifySource(dep.className,
			      false, finder)) == -1) {
				genHFile(dep.className, finder);
				if ((hi = verifySource(dep.className,
				    false, finder)) == -1) {
					throw new Exception("generated file"
					    + "`" + hFile(className, 0)
					    + "' is not valid");
				}
			}
			doneHdrs.add(dep);

			// Add in dependencies from header file itself
			for (Iterator j = readDeps(hFile(dep.className, hi),
			    "@dep_header", false).iterator(); j.hasNext(); ) {
				Dep dep2 = (Dep)j.next();
				if (!doneHdrs.contains(dep2))
					hdrs.add(dep2);
			}
		}

		// Create directories for object file
		if (!sourcesOnly)
			mkdirs(objectFile);

		// Now compile the C file
		if (!sourcesOnly)
			compile(cFile(className, ci), objectFile);

		// Reset generator
		codeGenerator.reset();
	}

	public boolean objectIsValid(String className,
	    ClassfileFinder finder, File objectFile) throws Exception {
		int ci;

		// Verify object file exists and is non-empty
		if (!objectFile.exists() || objectFile.length() == 0)
			return false;

		// Verify existing C file and check timestamp
		if ((ci = verifySource(className, true, finder)) == -1)
			return false;
		if (cFile(className, ci).lastModified()
		   > objectFile.lastModified())
			return false;

		// Get list of header files required by the C file
		Set hdrs = readDeps(cFile(className, ci), "@dep_header", false);

		// Recursively ensure all required header files exist
		Set doneHdrs = new HashSet();
		while (hdrs.size() > 0) {
			int hi;

			// Get the next unverified header file dependency
			Iterator i = hdrs.iterator();
			Dep dep = (Dep)i.next();
			i.remove();

			// Verify existing header file and check timestamp
			if ((hi = verifySource(dep.className,
			    false, finder)) == -1)
				return false;
			if (hFile(className, ci).lastModified()
			   > objectFile.lastModified())
				return false;
			doneHdrs.add(dep);

			// Add in dependencies from header file itself
			for (Iterator j = readDeps(hFile(dep.className, hi),
			    "@dep_header", false).iterator(); j.hasNext(); ) {
				Dep dep2 = (Dep)j.next();
				if (!doneHdrs.contains(dep2))
					hdrs.add(dep2);
			}
		}

		// Looks OK
		return true;
	}

	// Generate a C source file using our CodeGenerator
	private void genCFile(String className, ClassfileFinder finder)
	    throws Exception {
		File file = cFile(className, 0);
		file.delete();
		mkdirs(file);
		OutputStream output = new FileOutputStream(file);
		if (verbose)
			System.out.println("Generating `" + file + "'");
		boolean ok = false;
		try {
			codeGenerator.generateC(className, finder, output);
			ok = true;
		} finally {
			output.close();
			if (!ok)
				file.delete();
		}
	}

	// Generate a C header file using our CodeGenerator
	private void genHFile(String className, ClassfileFinder finder)
	    throws Exception {
		File file = hFile(className, 0);
		file.delete();
		mkdirs(file);
		OutputStream output = new FileOutputStream(file);
		if (verbose)
			System.out.println("Generating `" + file + "'");
		boolean ok = false;
		try {
			codeGenerator.generateH(className, finder, output);
			ok = true;
		} finally {
			output.close();
			if (!ok)
				file.delete();
		}
	}

	private void mkdirs(File file) {
		if ((file = file.getParentFile()) == null)
			return;
		if (file.exists())
			return;
		file.mkdirs();
	}

	// Verify there is a valid source file. Delete any invalid ones.
	private int verifySource(String className,
	    boolean c, ClassfileFinder finder) throws Exception {

search:
		// Check all directories in source path
		for (int i = 0; i < sourcePath.length; i++) {

			// Get file name
			File file = new File(sourcePath[i],
			    C.encode(className, true).replace('/',
			    Generate.FILE_SEPARATOR) + (c ? ".c" : ".h"));

			// See if file exists, is normal, and is non-empty
			if (!file.exists())
				continue;
			else if (!file.isFile() || file.length() == 0) {
				file.delete();
				continue;
			}

			// Read and verify @dep_class tags
			Set deps = readDeps(file, "@dep_class", true);
			for (Iterator j = deps.iterator(); j.hasNext(); ) {
				Dep dep = (Dep)j.next();

				// If tag is correct, check the next one
				try {
					if (finder.getClassfileHash(
					    dep.className) == dep.hash)
						continue;
				} catch (ClassNotFoundException e) {
				}

				// If file can be "hidden" don't delete it
				if (i > 0) {
					if (verbose) {
						System.out.println("Ignoring"
						    + " obsolete file `"
						    + file + "'");
					}
					return -1;
				}

				// Delete the file and try next directory
				if (file.delete() && verbose) {
					System.out.println("Removing"
					    + " obsolete file `"
					    + file + "'");
				}
				continue search;
			}

			// Looks OK
			return i;
		}

		// Not found
		return -1;
	}

	/**
	 * Compile a C file into an ELF object.
	 *
	 * This assumes the class name contains slashes, not dots.
	 */
	private void compile(File sourceFile, File objectFile)
	    throws Exception {

		// Assemble gcc command
		ArrayList args = new ArrayList(40);
		args.add(System.getProperty("jc.gnu.compiler"));
		args.add("-c");
		args.add("-O2");
		args.add("-pipe");
		if (debugSymbols)
			args.add("-g");
		args.add("-fno-common");
		args.add("-w");		// don't warn for implicit casts
		args.add("-Wall");
		args.add("-Waggregate-return");
		args.add("-Wcast-align");
		args.add("-Wcast-qual");
		args.add("-Wchar-subscripts");
		args.add("-Wcomment");
		args.add("-Wformat");
		args.add("-Wimplicit");
		args.add("-Wmissing-declarations");
		args.add("-Wmissing-prototypes");
		args.add("-Wnested-externs");
		args.add("-Wno-long-long");
		args.add("-Wparentheses");
		args.add("-Wpointer-arith");
		args.add("-Wreturn-type");
		args.add("-Wshadow");
		args.add("-Wstrict-prototypes");
		args.add("-Wswitch");
		args.add("-Wtrigraphs");
		args.add("-Wuninitialized");
		args.add("-Wunused");
		args.add("-Wwrite-strings");
	//	args.add("-Werror");
		args.add("-Wa,-W");	// avoid ".rodata' attribute warnings
		args.add("-I" + System.getProperty("jc.include.dir"));
		for (int i = 0; i < sourcePath.length; i++)
			args.add("-I" + sourcePath[i]);
		args.add("-o");
		args.add(objectFile.toString());
		args.add(sourceFile.toString());
		String[] cmd = (String[])args.toArray(new String[args.size()]);

		// Execute gcc command
		boolean ok = false;
		try {
			exec(cmd);
			ok = true;
		} finally {
			if (!ok)
				objectFile.delete();
		}
	}

	/**
	 * Execute a command.
	 */
	private void exec(String[] cmd) throws Exception {

		// Get command as a String
		StringBuffer buf = new StringBuffer(cmd.length * 20);
		for (int i = 0; i < cmd.length; i++) {
			if (i > 0)
				buf.append(' ');
			buf.append(cmd[i]);
		}
		String cmdString = buf.toString();

		// Print command to console in verbose mode
		if (verbose)
			System.out.println(buf.toString());

		// Execute command
		Process p = Runtime.getRuntime().exec(cmd, null, null);

		// Copy command output to console in verbose mode
		if (verbose) {
			CopyOut stdout = new CopyOut(
			    new BufferedInputStream(p.getInputStream()));
			CopyOut stderr = new CopyOut(
			    new BufferedInputStream(p.getErrorStream()));
			stdout.start();
			stderr.start();
			stdout.join();
			stderr.join();
		}

		// Wait for command to finish
		int r = p.waitFor();
		if (r != 0) {
			throw new RuntimeException("command exited with"
			    + " non-zero return status " + r
			    + ": " + cmdString);
		}

		// Close streams to immediately free up file descriptors
		p.getOutputStream().close();
		p.getInputStream().close();
		p.getErrorStream().close();
	}

	/**
	 * Thread to copy output from external process to the console.
	 */
	private static class CopyOut extends Thread {
		private final InputStream input;
		CopyOut(InputStream input) {
			this.input = input;
		}
		public void run() {
			byte[] buf = new byte[1024];
			while (true) {
				int r;
				try {
					r = input.read(buf);
				} catch (IOException e) {
					return;
				}
				if (r == -1)
					return;
				System.out.write(buf, 0, r);
			}
		}
	}

	/**
	 * Return the file that contains the C source code for the class.
	 */
	public File cFile(String className, int i) {
		return new File(sourcePath[i],
		    C.encode(className, true).replace('/',
		    Generate.FILE_SEPARATOR) + ".c");
	}

	/**
	 * Return the file that contains the C header source for the class.
	 */
	public File hFile(String className, int i) {
		return new File(sourcePath[i],
		    C.encode(className, true).replace('/',
		    Generate.FILE_SEPARATOR) + ".h");
	}

	// Class dependency object
	private static class Dep {
		final String className;
		final long hash;
		Dep(String className, long hash) {
			this.className = className;
			this.hash = hash;
		}
		Dep(String className) {
			this(className, 0);
		}
		public boolean equals(Object obj) {
			if (!(obj instanceof Dep))
				return false;
			Dep that = (Dep)obj;
			return this.className.equals(that.className)
			    && this.hash == that.hash;
		}
		public int hashCode() {
			return className.hashCode() ^ (int)hash;
		}
	}

	/**
	 * Read dependency lines from the top of a C source file.
	 */
	private Set readDeps(File file, String tag, boolean withHash)
	    throws Exception {
		HashSet deps = new HashSet();
		BufferedReader r = new BufferedReader(
		    new InputStreamReader(
		    new FileInputStream(file), "8859_1"));
		try {
			String line;
			while ((line = r.readLine()) != null) {

				// Parse line
				String[] toks = split(line, 4);

				// Ignore blank lines
				if (toks.length == 1 && toks[0].length() == 0)
					continue;

				// Stop at the first non //-comment
				if (!toks[0].equals("//"))
					break;

				// Is this a tag line?
				if (toks.length < 3 + (withHash ? 1 : 0))
					continue;
				if (!toks[1].equals(tag))
					continue;

				// Parse class name and optional hash
				long hash;
				String cname;
				if (withHash) {
					hash = parseHash(toks[2]);
					cname = Generate.decode(toks[3]);
				} else {
					hash = 0;
					cname = Generate.decode(toks[2]);
				}

				// Add dependency
				deps.add(new Dep(cname, hash));
			}
			return deps;
		} finally {
			r.close();
		}
	}

	/**
	 * Parse an unsigned 8 byte hex value. This method is required
	 * because Long.parseLong() only parses signed values.
	 */
	public long parseHash(String s) throws NumberFormatException {
		if (s.length() > 16)
			throw new NumberFormatException(s);
		long val = 0;
		for (int i = 0; i < s.length(); i++) {
			int digit;
			if ((digit = Character.digit(s.charAt(i), 16)) == -1)
				throw new NumberFormatException(s);
			val = (val << 4) | digit;
		}
		return val;
	}

	// Classpath doesn't support String.split() yet so we have to do this
	private String[] split(String s, int max) {
		ArrayList toks = new ArrayList();
		int pos = 0;
		for (int i = 0; i < max; i++) {
			while (pos < s.length()
			    && Character.isSpace(s.charAt(pos)))
				pos++;
			if (pos == s.length())
				break;
			int start = pos;
			while (pos < s.length()
			    && !Character.isSpace(s.charAt(pos)))
				pos++;
			toks.add(s.substring(start, pos));
		}
		toks.add("");		// stupid compat with String.split()
		return (String[])toks.toArray(new String[toks.size()]);
	}
}


