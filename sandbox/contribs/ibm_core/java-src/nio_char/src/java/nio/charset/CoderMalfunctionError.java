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
 * Errors thrown when the encoder/decoder is malfunctioning.
 * 
 */
public class CoderMalfunctionError extends Error {

	/*
	 * -------------------------------------------------------------------
	 * Constants
	 * -------------------------------------------------------------------
	 */

	/*
	 * This constant is used during deserialization to check the J2SE version
	 * which created the serialized object.
	 */
	static final long serialVersionUID = -1151412348057794301L; // J2SE 1.4.2

	/*
	 * -------------------------------------------------------------------
	 * Constructors
	 * -------------------------------------------------------------------
	 */

	/**
	 * Constructs an instance of this error.
	 * 
	 * @param ex
	 *            the original exception thrown by the encoder/decoder
	 */
	public CoderMalfunctionError(Exception ex) {
		super(ex);
	}

}
