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

package java.lang;


/**
 * This exception is thrown when a classloader is unable to find a class.
 */
public class ClassNotFoundException extends Exception {

	static final long serialVersionUID = 9176873029745254542L;

	private Throwable ex;

	/**
	 * Constructs a new instance of this class with its walkback filled in.
	 */
	public ClassNotFoundException() {
		super((Throwable) null);
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 */
	public ClassNotFoundException(String detailMessage) {
		super(detailMessage, null);
	}

	/**
	 * Constructs a new instance of this class with its walkback, message and
	 * exception filled in.
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 * @param exception
	 *            Throwable The exception which occurred while loading the
	 *            class.
	 */
	public ClassNotFoundException(String detailMessage, Throwable exception) {
		super(detailMessage);
		ex = exception;
	}

	/**
	 * Answers the exception which occured when loading the class.
	 * 
	 * @return Throwable The exception which occurred while loading the class.
	 */
	public Throwable getException() {
		return ex;
	}

	/**
	 * Answers the cause of this Throwable, or null if there is no cause.
	 * 
	 * @return Throwable The receiver's cause.
	 */
	public Throwable getCause() {
		return ex;
	}
}
