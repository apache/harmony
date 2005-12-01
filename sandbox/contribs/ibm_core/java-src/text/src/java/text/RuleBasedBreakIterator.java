/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
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

package java.text;


/*
 * Default implmentation of BreakIterator, wrap
 * com.ibm.icu.text.RuleBasedBreakIterator
 * 
 */
class RuleBasedBreakIterator extends BreakIterator {

	/*
	 * Wraping construction
	 */
	RuleBasedBreakIterator(com.ibm.icu.text.BreakIterator iterator) {
		super(iterator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#current()
	 */
	public int current() {
		return wrapped.current();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#first()
	 */
	public int first() {
		return wrapped.first();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#following(int)
	 */
	public int following(int offset) {
		validateOffset(offset);
		return wrapped.following(offset);
	}

	/*
	 * check the offset, throw exception if it is invalid
	 */
	private void validateOffset(int offset) {
		CharacterIterator it = wrapped.getText();
		if (offset < it.getBeginIndex() || offset >= it.getEndIndex()) {
			throw new IllegalArgumentException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#getText()
	 */
	public CharacterIterator getText() {
		return wrapped.getText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#last()
	 */
	public int last() {
		return wrapped.last();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#next()
	 */
	public int next() {
		return wrapped.next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#next(int)
	 */
	public int next(int n) {
		return wrapped.next(n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#previous()
	 */
	public int previous() {
		return wrapped.previous();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#setText(java.text.CharacterIterator)
	 */
	public void setText(CharacterIterator newText) {
		// call a method to check if null pointer
		newText.current();
		wrapped.setText(newText);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#isBoundary(int)
	 */
	public boolean isBoundary(int offset) {
		validateOffset(offset);
		return wrapped.isBoundary(offset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.text.BreakIterator#preceding(int)
	 */
	public int preceding(int offset) {
		validateOffset(offset);
		return wrapped.preceding(offset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (!(o instanceof RuleBasedBreakIterator)) {
			return false;
		}
		return wrapped.equals(((RuleBasedBreakIterator) o).wrapped);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return wrapped.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return wrapped.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		RuleBasedBreakIterator cloned = (RuleBasedBreakIterator) super.clone();
		cloned.wrapped = (com.ibm.icu.text.RuleBasedBreakIterator) wrapped
				.clone();
		return cloned;
	}

}
