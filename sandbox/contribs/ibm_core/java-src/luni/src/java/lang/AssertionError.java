/* Copyright 2002, 2004 The Apache Software Foundation or its licensors, as applicable
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
 * Assert statement support
 */
public class AssertionError extends Error {
	/**
	 * Constructs a new instance of this class with its walkback filled in.
	 */
	public AssertionError() {
		super();
	}

	/**
	 * Constructs a new instance of this class with its message filled in.
	 * 
	 * @param detailMessage
	 *            Object The detail message for the exception.
	 */
	public AssertionError(Object detailMessage) {
		super(String.valueOf(detailMessage));
	}

	/**
	 * Constructs a new instance of this class with its message filled in from
	 * the value of argument.
	 * 
	 * @param detailMessage
	 *            boolean Constructs the detail message from the boolean value
	 *            of argument.
	 */
	public AssertionError(boolean detailMessage) {
		this(String.valueOf(detailMessage));
	}

	/**
	 * Constructs a new instance of this class with its message filled in from
	 * the value of argument.
	 * 
	 * @param detailMessage
	 *            char Constructs the detail message from the char argument.
	 */
	public AssertionError(char detailMessage) {
		this(String.valueOf(detailMessage));
	}

	/**
	 * Constructs a new instance of this class with its message filled in from
	 * the value of argument.
	 * 
	 * @param detailMessage
	 *            int Constructs the detail message from the value of int
	 *            argument.
	 */
	public AssertionError(int detailMessage) {
		this(Integer.toString(detailMessage));
	}

	/**
	 * Constructs a new instance of this class with its message filled in from
	 * the value of argument.
	 * 
	 * @param detailMessage
	 *            long Constructs the detail message from the value of long
	 *            argument.
	 */
	public AssertionError(long detailMessage) {
		this(Long.toString(detailMessage));
	}

	/**
	 * Constructs a new instance of this class with its message filled in from
	 * the value of argument.
	 * 
	 * @param detailMessage
	 *            float Constructs the detail message from the value of float
	 *            argument.
	 */
	public AssertionError(float detailMessage) {
		this(Float.toString(detailMessage));
	}

	/**
	 * Constructs a new instance of this class with its message filled in from
	 * the value of argument.
	 * 
	 * @param detailMessage
	 *            double Constructs the detail message from the value of double
	 *            argument.
	 */

	public AssertionError(double detailMessage) {
		this(Double.toString(detailMessage));
	}
}
