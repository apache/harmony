/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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
 * FieldPosition is used to identify fields in formatted Strings.
 */
public class FieldPosition {

	private int myField, beginIndex, endIndex;

	private Format.Field myAttribute;

	/**
	 * Constructs a new FieldPosition on the specified field.
	 * 
	 * @param field
	 *            the field to identify
	 */
	public FieldPosition(int field) {
		myField = field;
	}

	/**
	 * Constructs a new FieldPosition on the specified Field attribute.
	 * 
	 * @param attribute
	 *            the field attribute to identify
	 */
	public FieldPosition(Format.Field attribute) {
		myAttribute = attribute;
		myField = -1;
	}

	/**
	 * Constructs a new FieldPosition on the specified Field attribute and field
	 * id.
	 * 
	 * @param attribute
	 *            the field attribute to identify
	 * @param field
	 *            the field to identify
	 */
	public FieldPosition(Format.Field attribute, int field) {
		myAttribute = attribute;
		myField = field;
	}

	void clear() {
		beginIndex = endIndex = 0;
	}

	/**
	 * Compares the specified object to this FieldPosition and answer if they
	 * are equal. The object must be an instance of FieldPosition with the same
	 * field, begin index and end index.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return true if the specified object is equal to this fieldPosition,
	 *         false otherwise
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object object) {
		if (!(object instanceof FieldPosition))
			return false;
		FieldPosition pos = (FieldPosition) object;
		return myField == pos.myField && myAttribute == pos.myAttribute
				&& beginIndex == pos.beginIndex && endIndex == pos.endIndex;
	}

	/**
	 * Answers the index of the beginning of the field.
	 * 
	 * @return the first index of the field
	 */
	public int getBeginIndex() {
		return beginIndex;
	}

	/**
	 * Answers the index one past the end of the field.
	 * 
	 * @return one past the index of the last character in the field
	 */
	public int getEndIndex() {
		return endIndex;
	}

	/**
	 * Answers the field which is being identified.
	 * 
	 * @return the field
	 */
	public int getField() {
		return myField;
	}

	/**
	 * Answers the attribute which is being identified.
	 * 
	 * @return the field
	 */
	public Format.Field getFieldAttribute() {
		return myAttribute;
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
		int attributeHash = (myAttribute == null) ? 0 : myAttribute.hashCode();
		return attributeHash + myField * 10 + beginIndex * 100 + endIndex;
	}

	/**
	 * Sets the index of the beginning of the field.
	 * 
	 * @param index
	 *            the index of the first character in the field
	 */
	public void setBeginIndex(int index) {
		beginIndex = index;
	}

	/**
	 * Sets the index of the end of the field.
	 * 
	 * @param index
	 *            one past the index of the last character in the field
	 */
	public void setEndIndex(int index) {
		endIndex = index;
	}

	/**
	 * Answers the string representation of this FieldPosition.
	 * 
	 * @return the string representation of this FieldPosition
	 */
	public String toString() {
		return getClass().getName() + "[attribute=" + myAttribute + ", field="
				+ myField + ", beginIndex=" + beginIndex + ", endIndex="
				+ endIndex + "]";
	}
}
