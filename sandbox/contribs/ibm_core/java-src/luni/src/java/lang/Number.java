/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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
 * Number is the abstract superclass of the classes which represent numeric base
 * types (i.e. all but Character, Boolean, and Void).
 */
public abstract class Number implements java.io.Serializable {

	static final long serialVersionUID = -8742448824652078965L;

	/**
	 * Number constructor. Included for spec compatability.
	 */
	public Number() {
	}

	/**
	 * Answers the byte value which the receiver represents
	 * 
	 * @return byte the value of the receiver.
	 */
	public byte byteValue() {
		return (byte) intValue();
	}

	/**
	 * Answers the double value which the receiver represents
	 * 
	 * @return double the value of the receiver.
	 */
	public abstract double doubleValue();

	/**
	 * Answers the float value which the receiver represents
	 * 
	 * @return float the value of the receiver.
	 */
	public abstract float floatValue();

	/**
	 * Answers the int value which the receiver represents
	 * 
	 * @return int the value of the receiver.
	 */
	public abstract int intValue();

	/**
	 * Answers the long value which the receiver represents
	 * 
	 * @return long the value of the receiver.
	 */
	public abstract long longValue();

	/**
	 * Answers the short value which the receiver represents
	 * 
	 * @return short the value of the receiver.
	 */
	public short shortValue() {
		return (short) intValue();
	}
}
