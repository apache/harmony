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

package com.ibm.oti.util;


/**
 * InvalidJarIndexException is thrown when entries in META-INF/INDEX.LIST are
 * inaccurate.
 */
public class InvalidJarIndexException extends RuntimeException {

	/**
	 * Constructs a new instance of this class.
	 */
	public InvalidJarIndexException() {
		super();
	}

	/**
	 * Constructs a new instance of this class with its message filled in.
	 * 
	 * @param message
	 *            String The message for the exception.
	 */
	public InvalidJarIndexException(String message) {
		super(message);
	}

}
