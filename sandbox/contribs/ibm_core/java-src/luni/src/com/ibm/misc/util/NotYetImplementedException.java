/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.misc.util;


/**
 * This exception is thrown by methods that are not currently implemented, so
 * that programs that call the stubs fail early and predictably.
 * 
 */
public class NotYetImplementedException extends RuntimeException {

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public NotYetImplementedException() {
		super();
		System.err.println("*** NOT YET IMPLEMENTED EXCEPTION ***"); //$NON-NLS-1$
		StackTraceElement thrower = getStackTrace()[0];
		System.err
				.println("*** thrown from class  -> " + thrower.getClassName()); //$NON-NLS-1$
		System.err
				.println("***             method -> " + thrower.getMethodName()); //$NON-NLS-1$

		System.err.print("*** defined in         -> "); //$NON-NLS-1$
		if (thrower.isNativeMethod()) {
			System.err.println("a native method"); //$NON-NLS-1$
		} else {
			String fileName = thrower.getFileName();
			if (fileName == null) {
				System.err.println("an unknown source"); //$NON-NLS-1$
			} else {
				int lineNumber = thrower.getLineNumber();
				System.err.print("the file \"" + fileName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				if (lineNumber >= 0) {
					System.err.print(" on line #" + lineNumber); //$NON-NLS-1$
				}
				System.err.println();
			}
		}
	}

	/**
	 * Constructor that takes a reason message.
	 * 
	 * @param detailMessage
	 */
	public NotYetImplementedException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructor that takes a reason and a wrapped exception.
	 * 
	 * @param detailMessage
	 * @param throwable
	 */
	public NotYetImplementedException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	/**
	 * Constructor that takes a wrapped exception.
	 * 
	 * @param throwable
	 */
	public NotYetImplementedException(Throwable throwable) {
		super(throwable);
	}

}
