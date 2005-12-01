/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util.regex;

import java.io.Serializable;


/**
 * Pattern implements a compiler for regular expressions as defined by the J2SE
 * specification. The regular expression syntax is largely similar to the syntax
 * defined by Perl 5 but has both omissions and extensions. A formal and
 * complete definition of the regular expression syntax is not provided by the
 * J2SE speTBD (TODO)
 * 
 */
public final class Pattern implements Serializable {
	static final long serialVersionUID = 5073258162644648461L;

	public static final int UNIX_LINES = 1;

	public static final int CASE_INSENSITIVE = 2;

	public static final int COMMENTS = 4;

	public static final int MULTILINE = 8;

	public static final int DOTALL = 32;

	public static final int UNICODE_CASE = 64;

	public static final int CANON_EQ = 128;

	/**
	 * Create a compiled pattern corresponding to the input regular expression
	 * string. Default options are used. For a description of option settings,
	 * including their defaults, see compile(String,int).
	 * 
	 * @param regex
	 *            A regular expression string.
	 * @return A compiled pattern
	 * @throws PatternSyntaxException
	 *             If the input regular expression does not match the required
	 *             grammar.
	 */
	public static Pattern compile(String regex) throws PatternSyntaxException {
		return null;
	}

	/**
	 * Return a compiled pattern corresponding to the input regular expression
	 * string.
	 * 
	 * The input <code>flags</code> is a mask of the following flags:
	 * <dl>
	 * <dt><code>UNIX_LINES</code> (0x0001)
	 * <dd>Enables UNIX lines mode where only \n is recognized as a line
	 * terminator. The default setting of this flag is <em>off</em> indicating
	 * that all of the following character sequences are recognized as line
	 * terminators: \n, \r, \r\n, NEL (\u0085), \u2028 and \u2029.
	 * <dt><code>CASE_INSENSITIVE</code> (0x0002)
	 * <dd>Directs matching to be done in a way that ignores differences in
	 * case. If input character sequences are encoded in character sets other
	 * than ASCII, then the UNICODE_CASE must also be set to enable Unicode case
	 * detection.
	 * <dt><code>UNICODE_CASE</code> (0x0040)
	 * <dd>Enables Unicode case folding if used in conjuntion with the
	 * <code>CASE_INSENSITIVE</code> flag. If <code>CASE_INSENSITIVE</code>
	 * is not set, then this flag has no effect.
	 * <dt><code>COMMENTS</code> (0x0004)
	 * <dd>Directs the pattern compiler to ignore whitespace and comments in
	 * the pattern. Whitespace consists of sequences including only these
	 * characters: SP (\u0020), HT (\t or \u0009), LF (\n or ), VT (\u000b), FF
	 * (\f or \u000c), and CR (\r or ). A comment is any sequence of characters
	 * beginning with the "#" (\u0023) character and ending in a LF character.
	 * <dt><code>MULTILINE</code> (0x0008)
	 * <dd>Turns on multiple line mode for matching of character sequences. By
	 * default, this mode is off so that the character "^" (\u005e) matches the
	 * beginning of the entire input sequence and the character "$" (\u0024)
	 * matches the end of the input character sequence. In multiple line mode,
	 * the character "^" matches any character in the input sequence which
	 * immediately follows a line terminator and the character "$" matches any
	 * character in the input sequence which immediately precedes a line
	 * terminator.
	 * <dt><code>DOTALL</code> (0x0020)
	 * <dd>Enables the DOT (".") character in regular expressions to match line
	 * terminators. By default, line terminators are not matched by DOT.
	 * <dt><code>CANON_EQ</code> (0x0080)
	 * <dd>Enables matching of character sequences which are cacnonically
	 * equivalent according to the Unicode standard. Canonical equivalence is
	 * described here: http://www.unicode.org/reports/tr15/. By default,
	 * canonical equivalence is not detected while matching.
	 * </dl>
	 * 
	 * @param regex
	 *            A regular expression string.
	 * @param flags
	 *            A set of flags to control the compilation of the pattern.
	 * @return A compiled pattern
	 * @throws PatternSyntaxException
	 *             If the input regular expression does not match the required
	 *             grammar.
	 */
	public static Pattern compile(String regex, int flags)
			throws PatternSyntaxException {
		return null;
	}

	/**
	 * Return the mask of flags used to compile the pattern
	 * 
	 * @return A mask of flags used to compile the pattern.
	 */
	public int flags() {
		return 0;
	}

	/**
	 * Returns the pattern string passed to the compile method
	 * 
	 * @return A string representation of the pattern
	 */
	public String pattern() {
		return null;
	}

	/**
	 * Create a matcher for this pattern and a given input character sequence
	 * 
	 * @param input
	 *            The input character sequence
	 * @return A new matcher
	 */
	public Matcher matcher(CharSequence input) {
		return null;
	}

	/**
	 * A convenience method to test for a match of an input character sequence
	 * against a regular expression string. The return value is true only if
	 * there is a <em>complete</em> match between the regular expression
	 * string and the input sequence. Partial matches that leave characters in
	 * the input sequence unmatched will cause a return value of false.
	 * 
	 * @param regex
	 *            A regular expression string
	 * @param input
	 *            A character sequence to match against
	 * 
	 * @return true iff the character sequence completely matches the given
	 *         regular expression
	 * 
	 * @throws PatternSyntaxException
	 * 
	 */
	public static boolean matches(String regex, CharSequence input)
			throws PatternSyntaxException {
		return false;
	}

	/**
	 * Tokenize the input character sequence using the pattern to recognize
	 * token separators
	 * 
	 * @param input
	 *            The input character sequence
	 * @return An array of string tokens
	 */
	public final String[] split(CharSequence input) {
		return null;
	}

	/**
	 * Split an input string using the pattern as a token separator.
	 * 
	 * @param input
	 *            Input sequence to tokenize
	 * @param limit
	 *            If positive, the maximum number of tokens to return. If
	 *            negative, an indefinite number of tokens are returned. If
	 *            zero, an indefinite number of tokens are returned but trailing
	 *            empty tokens are excluded.
	 * @return A sequence of tokens split out of the input string.
	 */
	public String[] split(CharSequence input, int limit) {
		return null;
	}

	/**
	 * Return a textual representation of the pattern.
	 * 
	 * @return The regular expression string
	 */
	public String toString() {
		return null;
	}
}
