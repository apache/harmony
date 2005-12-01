/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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


/**
 * ParsePosition is used to track the current position in a String being parsed.
 */
public class ParsePosition {

	private int currentPosition, errorIndex = -1;

	/**
	 * Constructs a new ParsePosition at the specified index.
	 * 
	 * @param index
	 *            the index to begin parsing
	 */
	public ParsePosition(int index) {
		currentPosition = index;
	}

	/**
	 * Compares the specified object to this ParsePosition and answer if they
	 * are equal. The object must be an instance of ParsePosition and have the
	 * same index and error index.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return true if the specified object is equal to this ParsePosition,
	 *         false otherwise
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		if (!(object instanceof ParsePosition))
			return false;
		ParsePosition pos = (ParsePosition) object;
		return currentPosition == pos.currentPosition
				&& errorIndex == pos.errorIndex;
	}

	/**
	 * Answers the index at which the parse could not continue.
	 * 
	 * @return the index of the parse error, or -1 if there is no error
	 */
	public int getErrorIndex() {
		return errorIndex;
	}

	/**
	 * Answers the current parse position.
	 * 
	 * @return the current position
	 */
	public int getIndex() {
		return currentPosition;
	}

	/**
	 * Answers an integer hash code for the receiver. Objects which are equal
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return currentPosition + errorIndex;
	}

	/**
	 * Sets the index at which the parse could not continue.
	 * 
	 * @param index
	 *            the index of the parse error
	 */
	public void setErrorIndex(int index) {
		errorIndex = index;
	}

	/**
	 * Sets the current parse position.
	 * 
	 * @param index
	 *            the current parse position
	 */
	public void setIndex(int index) {
		currentPosition = index;
	}

	/**
	 * Answers the string representation of this FieldPosition.
	 * 
	 * @return the string representation of this FieldPosition
	 */
	public String toString() {
		return getClass().getName() + "[index=" + currentPosition
				+ ", errorIndex=" + errorIndex + "]";
	}
}
