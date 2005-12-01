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

package java.security;


/**
 * Instances of this class are used to wrap exceptions which occur within
 * privileged operations.
 * 
 */
public class PrivilegedActionException extends Exception {
	static final long serialVersionUID = 4724086851538908602L;

	/**
	 * The exception which occurred.
	 */
	private Exception exception;

	/**
	 * Constructs a new instance of this class with its exception filled in.
	 * @param ex 
	 */
	public PrivilegedActionException(Exception ex) {
		super(null, ex);
		exception = ex;
	}

	/**
	 * Answers the exception which caused the receiver to be thrown.
	 * @return exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString() {
		return super.toString() + ": " + exception;
	}

	/**
	 * Answers the cause of this Throwable, or null if there is no cause.
	 * 
	 * 
	 * @return Throwable The receiver's cause.
	 */
	public Throwable getCause() {
		return exception;
	}
}
