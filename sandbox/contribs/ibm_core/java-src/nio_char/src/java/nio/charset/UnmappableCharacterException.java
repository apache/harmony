/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
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

package java.nio.charset;


/**
 * Thrown when an unmappable character for the given charset is encountered.
 * 
 * 
 */
public class UnmappableCharacterException extends CharacterCodingException {

	/*
	 * -------------------------------------------------------------------
	 * Constants
	 * -------------------------------------------------------------------
	 */

	/*
	 * This constant is used during deserialization to check the J2SE version
	 * which created the serialized object.
	 */
	static final long serialVersionUID = -7026962371537706123L; // J2SE 1.4.2

	/*
	 * -------------------------------------------------------------------
	 * Instance variables
	 * -------------------------------------------------------------------
	 */

	// the length of the unmappable character
	private int inputLength;

	/*
	 * -------------------------------------------------------------------
	 * Constructors
	 * -------------------------------------------------------------------
	 */

	/**
	 * Constructs an instance of this exception.
	 * 
	 * @param length
	 *            the length of the unmappable character
	 */
	public UnmappableCharacterException(int length) {
		this.inputLength = length;
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	/**
	 * Gets the length of the unmappable character.
	 * 
	 * @return the length of the unmappable character
	 */
	public int getInputLength() {
		return this.inputLength;
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods overriding parent class Throwable
	 * -------------------------------------------------------------------
	 */

	/**
	 * Gets a message describing this exception.
	 * 
	 * @return a message describing this exception
	 */
	public String getMessage() {
		return "The unmappable character length is " + this.inputLength + "."; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
