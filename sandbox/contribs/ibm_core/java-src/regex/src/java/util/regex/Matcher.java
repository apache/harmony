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

/**
 * Matcher is a class used to create matching engines based on a specific
 * regular expression pattern and a specific input character sequence. Matcher
 * objects are stateful and therefore not thread safe. A Matcher object is
 * created by the Pattern.matcher() method. A Matcher object can be reused to
 * search the same or different character sequence through the use of the
 * reset() method.
 * 
 */
public final class Matcher {
	/**
	 * The start() method returns the index of the first character in the
	 * currently matched subsequence.
	 * 
	 * @return Returns the index of the first character in the currently matched
	 *         subsequence.
	 * @throws IllegalStateException
	 *             If the previous call to find, matches or lookingAt failed to
	 *             find a match.
	 */
	public int start() throws IllegalStateException {
		return 0;
	}

	/**
	 * The end() method returns the index of the character immediately after the
	 * last in the currently matched subsequence.
	 * 
	 * @return Returns the index of the character immediately after the last in
	 *         the currently matched subsequence.
	 * @throws IllegalStateException
	 *             If the previous call to find, matches or lookingAt failed to
	 *             find a match.
	 */
	public int end() throws IllegalStateException {
		return 0;
	}

	/**
	 * The start(int) method returns the index of the first character in the
	 * captured group identified by the groupId argument. Group 0 always refers
	 * to the entire matched subsequence so calling start(0) is equivalent to
	 * calling start() with no arguments. It is possible to match a pattern
	 * without finding a match for one or more capture groups. In the case where
	 * a capture group k has not been matched, a value of -1 will be returned
	 * from start(k). start(0) will never return -1.
	 * 
	 * @param groupId
	 *            The index of the capturing group.
	 * @return The index of the first character in the captured group or -1 if
	 *         the group was not captured.
	 * @throws IllegalStateException
	 *             If the previous call to find, matches or lookingAt failed to
	 *             find a match.
	 * @throws IndexOutOfBoundsException
	 *             If the groupId is less than zero or greater than
	 *             groupCount().
	 */
	public int start(int groupId) throws IllegalStateException,
			IndexOutOfBoundsException {
		return 0;
	}

	/**
	 * The end(int) method returns the index of the character immediately
	 * following the last in the captured group identified by the groupId
	 * argument. Group 0 always refers to the entire matched subsequence so
	 * calling end(0) is equivalent to calling end() with no arguments. It is
	 * possible to match a pattern without finding a match for one or more
	 * capture groups. In the case where a capture group k has not been matched,
	 * a value of -1 will be returned from end(k). end(0) will never return -1.
	 * 
	 * @param groupId
	 *            The index of the capturing group.
	 * @return The index of the character immediately following the last in the
	 *         captured group or -1 if the group was not captured.
	 * @throws IllegalStateException
	 *             If the previous call to find, matches or lookingAt failed to
	 *             find a match.
	 * @throws IndexOutOfBoundsException
	 *             If the groupId is less than zero or greater than
	 *             groupCount().
	 */
	public int end(int groupId) throws IllegalStateException,
			IndexOutOfBoundsException {
		return 0;
	}

	/**
	 * The group() method returns a String containing a copy of the entire
	 * matched subsequence (group 0).
	 * 
	 * @return A string containing a copy of the entire matched subsequence.
	 * @throws IllegalStateException
	 *             If the previous call to find, matches or lookingAt failed to
	 *             find a match.
	 */
	public String group() throws IllegalStateException {
		return null;
	}

	/**
	 * Return a String containing a copy of the matched capturing group
	 * identified by groupId. If the identified group was not captured, this
	 * method returns null.
	 * 
	 * @param groupId
	 *            The index of the capturing group.
	 * @return A String containing a copy of the matched capturing group
	 *         identified by groupId or null if the group was not captured.
	 * @throws IllegalStateException
	 *             If the previous call to find, matches or lookingAt failed to
	 *             find a match.
	 * @throws IndexOutOfBoundsException
	 *             If the groupId is less than zero or greater than
	 *             groupCount().
	 */
	public String group(int groupId) throws IllegalStateException,
			IndexOutOfBoundsException {
		return null;
	}

	/**
	 * Return the number of capturing groups in the pattern.
	 * 
	 * @return The number of capturing groups in the pattern.
	 */
	public int groupCount() {
		return 0;
	}

	/*
	 * The reset() method resets the internal state of the Matcher object to
	 * begin scanning at the beginning of the character sequence.
	 * 
	 * @return A reference to this Matcher.
	 */
	public Matcher reset() {
		return null;
	}

	/*
	 * This reset() method is similar to the reset method without arguments
	 * except that it also replaces the input character sequence which should be
	 * scanned by the matcher.
	 * 
	 * @param input A new character sequence to scan @return A reference to this
	 * Matcher.
	 */
	public Matcher reset(CharSequence input) {
		return null;
	}

	/**
	 * The find() method matches the pattern against the character sequence
	 * beginning at the character after the last match or at the beginning of
	 * the sequence if called immediately after reset(). The method returns true
	 * if and only if a match is found.
	 * 
	 * @return A boolean indicating if the pattern was matched.
	 */
	public boolean find() {
		return false;
	}

	/**
	 * This find() method is similar to the version without arguments except
	 * that an implicit reset() is done and the matching is started at the given
	 * character sequence index.
	 * 
	 * @param start
	 *            The starting index
	 * @return A boolean indicating if the pattern was matched.
	 * @throws IndexOutOfBoundsException
	 *             If the start index is negative or greater than the length of
	 *             the string.
	 */
	public boolean find(int start) throws IndexOutOfBoundsException {
		return false;
	}

	/**
	 * This method attempts to match the pattern against the character sequence
	 * starting at the beginning. If the pattern matches even a prefix of the
	 * input character sequence, lookingAt() will return true. Otherwise it will
	 * return false.
	 * 
	 * @return A boolean indicating if the pattern matches a prefix of the input
	 *         character sequence.
	 */
	public boolean lookingAt() {
		return false;
	}

	/**
	 * This method is identical in function to the Pattern.matches() method. It
	 * returns true if and only if the regular expression pattern matches the
	 * entire input character sequence.
	 * 
	 * @return A boolean indicating if the pattern matches the entire input
	 *         character sequence.
	 */
	public boolean matches() {
		return false;
	}

	/**
	 * Return a reference to the pattern used by this Matcher.
	 * 
	 * @return A reference to the pattern used by this Matcher.
	 */
	public Pattern pattern() {
		return null;
	}

	/**
	 * Replace all occurrences of character sequences which match the pattern
	 * with the given replacement string. The replacement string may refer to
	 * capturing groups using the syntax "$<group number>".
	 * 
	 * @param replacement
	 *            A string to replace occurrences of character sequences
	 *            matching the pattern.
	 * @return A new string with replacements inserted
	 */
	public String replaceAll(String replacement) {
		return null;
	}

	/**
	 * This is very similar to replaceAll except only the first occurrence of a
	 * sequence matching the pattern is replaced.
	 * 
	 * @param replacement
	 *            A string to replace occurrences of character sequences
	 *            matching the pattern.
	 * @return A new string with replacements inserted
	 */
	public String replaceFirst(String replacement) {
		return null;
	}

	/**
	 * TODO: appendReplacement javadoc
	 * 
	 * @param sb
	 * @param replacement
	 * @return
	 * @throws IllegalStateException
	 * @throws IndexOutOfBoundsException
	 */
	public Matcher appendReplacement(StringBuffer sb, String replacement)
			throws IllegalStateException, IndexOutOfBoundsException {
		return null;
	}

	/**
	 * TODO: appendTail(StringBuffer) javadoc
	 * 
	 * @param sb
	 * @return
	 */
	public StringBuffer appendTail(StringBuffer sb) {
		return null;
	}
}
