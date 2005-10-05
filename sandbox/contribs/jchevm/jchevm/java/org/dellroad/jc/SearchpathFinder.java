
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
// $Id: SearchpathFinder.java,v 1.1.1.1 2004/02/20 05:15:20 archiecobbs Exp $
//

package org.dellroad.jc;

import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

/** 
 * Implementation of {@link ClassfileFinder} used by the
 * {@link BootstrapObjectGenerator} class for object file generation
 * when another virtual machine besides JC must be used. This class
 * simply searches a standard ``classpath'' directory and ZIP/JAR file
 * search path for class files.
 */
public class SearchpathFinder implements ClassfileFinder {

	static final String fs = System.getProperty("file.separator");
	static final char ps = System.getProperty("path.separator").charAt(0);

	private final String path;
	private final File[] files;
	private Object[] cache;

	/**
	 * Create a finder using the supplied classpath.
	 *
	 * @param path Class search path containing directory names
	 *		and ZIP/JAR file names, for example
	 *		<code>/some/dir1:/some/dir2:/other/file.zip<code>.
	 */
	public SearchpathFinder(String path) {
		this.path = path;
		String[] dirs = splitPath(path);
		files = new File[dirs.length];
		for (int i = 0; i < dirs.length; i++)
			files[i] = new File(dirs[i]);
	}

	/** 
	 * Returns the constructor's argument.
	 */
	public String getPath() {
		return path;
	}

	/** 
	 * Returns the path split out into individual components.
	 */
	public File[] getPathArray() {
		return (File[])files.clone();
	}

	public byte[] getClassfile(String className)
	    throws ClassNotFoundException {

		// Load cache
		if (cache == null)
			loadCache();

		// Get classfile name
		String cfile = className.replace('.', '/')
		    .replace('/', fs.charAt(0)) + ".class";

search:
		// Search each directory and ZipFile
		for (int i = 0; i < cache.length; i++) {
			ZipFile zfile = null;
			InputStream in = null;
			byte[] rtn;

			// Try to open and read the file
			try {
				if (cache[i] instanceof File) {
					File dir = (File)cache[i];
					File file = new File(dir, cfile);
					if (!file.isFile())
						continue;
					rtn = new byte[(int)file.length()];
					in = new FileInputStream(file);
				} else if (cache[i] instanceof ZipFile) {
					zfile = (ZipFile)cache[i];
					ZipEntry zent = zfile.getEntry(cfile);
					if (zent == null)
						continue;
					rtn = new byte[(int)zent.getSize()];
					in = zfile.getInputStream(zent);
				} else
					continue;

				// Read in data
				int r;
				for (int len = 0;
				    len < rtn.length; len += r) {
					if ((r = in.read(rtn, len,
					    rtn.length - len)) == -1)
						continue search;
				}

				// Done
				return rtn;
			} catch (ZipException e) {
				continue;
			} catch (IOException e) {
				continue;
			} finally {		// clean up before leaving
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
				}
			}
		}

		// Not found
		throw new ClassNotFoundException("class `"
		    + className + "' not found");
	}

	private synchronized void loadCache() {
		if (cache != null)
			return;
		cache = new Object[files.length];
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				cache[i] = files[i];
			else if (files[i].isFile()) {
				try {
					cache[i] = new ZipFile(files[i]);
				} catch (ZipException e) {
					continue;
				} catch (IOException e) {
					continue;
				}
			}
		}
	}

	public long getClassfileHash(String className)
	    throws ClassNotFoundException {
		return Generate.hash(new ByteArrayInputStream(
		    getClassfile(className)));
	}

	protected void finalize() {
		if (cache == null)
			return;
		for (int i = 0; i < cache.length; i++) {
			if (cache[i] instanceof ZipFile) {
				try {
					((ZipFile)cache[i]).close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Split a search path into components. Splits on the platform
	 * specific path separator.
	 *
	 * @param path Search path containing file/directory names, e.g.,
	 *		<code>/some/dir1:/some/file2:/other/file.zip<code>.
	 */
	public static String[] splitPath(String path) {
		ArrayList dlist = new ArrayList();
		for (int i = 0; i < path.length(); ) {
			while (i < path.length() && path.charAt(i) == ps)
				i++;
			int start = i;
			while (i < path.length() && path.charAt(i) != ps)
				i++;
			if (i > start)
				dlist.add(path.substring(start, i));
		}
		return (String[])dlist.toArray(new String[dlist.size()]);
	}
}

