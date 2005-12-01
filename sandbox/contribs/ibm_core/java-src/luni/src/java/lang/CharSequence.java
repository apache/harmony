/* Copyright 2003, 2005 The Apache Software Foundation or its licensors, as applicable
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

package java.lang;


/**
 * The CharSequence interface represets an ordered set of characters and the
 * functions to probe them.
 */
public interface CharSequence {

	/**
	 * Answers the number of characters in the sequence.
	 * 
	 * @return the number of charcters in the sequence
	 */
	public int length();

	/**
	 * Answers the character at the specified index (0-based indexing).
	 * 
	 * @param index -
	 *            of the character to return
	 * @return character indicated by index
	 * @throws IndexOutOfBoundsException
	 *             when <code>index < 0</code> or
	 *             <code>index<\code> >= the length of the <code>CharSequence</code>
	 */
	public char charAt(int index);

	/**
	 * Answers a CharSequence from the <code>start<\code> index to the
	 * <code>end<\code> index of this sequence.
	 *
	 * @param		start -- index of the start of the sub-sequence to return
	 * @param		end -- index of the end of the sub-sequence to return
	 * @return		the sub sequence from start to end
	 * @throws		IndexOutOfBoundsException when 1. either index is below 0
	 * 				2. either index >= <code>this.length()<\code>
	 * 				3. <code>start > end <\code>
	 */
	public CharSequence subSequence(int start, int end);

	/**
	 * Answers a String with the same characters and ordering of this
	 * CharSequence
	 * 
	 * @return a String based on the CharSequence
	 */
	public String toString();
}
