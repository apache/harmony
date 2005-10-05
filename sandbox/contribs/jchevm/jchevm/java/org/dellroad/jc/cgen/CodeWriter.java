
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
// $Id: CodeWriter.java,v 1.1.1.1 2004/02/20 05:15:25 archiecobbs Exp $
//

package org.dellroad.jc.cgen;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

/**
 * Extension of {@link PrintWriter PrintWriter} that contains methods
 * useful for generating and formatting C source files. Basically you
 * can set an indentation level (see {@link #indent indent()}) that
 * persists for subsequent lines until you unset it
 * (see {@link #undent undent()}).
 */
public class CodeWriter extends PrintWriter {
	private static final char[] EOL_chars
	    = System.getProperty("line.separator").toCharArray();

	protected final Writer w;
	protected final int indent;
	protected int indentLevel;
	protected boolean needIndent;

	/**
	 * Constructor. Uses the default indentation of 8 spaces.
	 *
	 * @param w underlying output
	 */
	public CodeWriter(Writer w) {
		this(w, 8);
	}

	/**
	 * Constructor.
	 *
	 * @param w underlying output
	 * @param indent number of spaces for each indentation level
	 */
	public CodeWriter(Writer w, int indent) {
		super(w);
		if (w == null || indent <= 0)
			throw new IllegalArgumentException();
		this.w = w;
		this.indent = indent;
		this.indentLevel = 0;
	}

	/**
	 * Set indentation level to an absolute number of indents.
	 *
	 * @param level  Non-negative indentation level
	 */
	public void setIndent(int level) {
		if (level < 0)
			throw new IllegalArgumentException();
		indentLevel = level;
	}

	/**
	 * Get the current indentation level.
	 */
	public int getIndent() {
		return (indentLevel);
	}

	/** 
	 * Increase the current indentation level by one.
	 */
	public void indent() {
		indentLevel++;
	}

	/** 
	 * Decrease the current indentation level by one.
	 */
	public void undent() {
		if (indentLevel > 0)
			indentLevel--;
	}

	public void println() {
		super.println();
		needIndent = true;
	}

	/** 
	 * Output enough tabs and spaces to achieve current indentation.
	 */
	protected void doIndent() {
		int spaces = indentLevel * indent;
		while (spaces >= 8) {
			super.write('\t');
			spaces -= 8;
		}
		super.write("       ", 0, spaces);
		needIndent = false;
	}

	public void write(int ch) {
		if (needIndent) {
			char[] buf = new char[] { (char)ch };
			doIndentIfNeeded(buf, 0, 1);
		}
		super.write(ch);
	}

	public void write(char[] buf) {
		if (buf.length == 0)
			return;
		doIndentIfNeeded(buf, 0, buf.length);
		super.write(buf);
	}

	public void write(char[] buf, int off, int len) {
		if (len == 0)
			return;
		doIndentIfNeeded(buf, off, len);
		super.write(buf, off, len);
	}

	public void write(String s) {
		if (s.length() == 0)
			return;
		doIndentIfNeeded(s, 0, s.length());
		super.write(s);
	}

	public void write(String s, int off, int len) {
		if (len == 0)
			return;
		doIndentIfNeeded(s, off, len);
		super.write(s, off, len);
	}

	private void doIndentIfNeeded(String s, int off, int len) {
		if (!needIndent)
			return;
		if (len != EOL_chars.length)
			doIndent();
		doIndentIfNeeded(s.toCharArray(), off, len);
	}

	private void doIndentIfNeeded(char[] buf, int off, int len) {
		if (!needIndent)
			return;
		if (len != EOL_chars.length)
			doIndent();
		for (int i = 0; i < len; i++) {
			if (buf[off + i] != EOL_chars[i]) {
				doIndent();
				return;
			}
		}
	}
}

