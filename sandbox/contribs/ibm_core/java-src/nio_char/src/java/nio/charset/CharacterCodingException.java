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


import java.io.IOException;

/**
 * 
 * Type of exception thrown when an encoding or decoding error occurs.
 * 
 */
public class CharacterCodingException extends IOException {

	/*
	 * -------------------------------------------------------------------
	 * Constants
	 * -------------------------------------------------------------------
	 */

	/*
	 * This constant is used during deserialization to check the J2SE version
	 * which created the serialized object.
	 */
	static final long serialVersionUID = 8421532232154627783L; // J2SE 1.4.2

	/**
	 * Default constructor.
	 */
	public CharacterCodingException() {
		super();
	}
}
