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

package java.net;

import com.ibm.oti.util.Msg;


/**
 * Represents an exception that occurred during parsing of a URI.
 */
public class URISyntaxException extends Exception {

	private String input;

	private int index;

	/**
	 * Constructs a URISyntaxException, containing the input that caused the
	 * exception, a description of the problem, and the index at which the error
	 * occurred.
	 * 
	 * @param input
	 * @param reason
	 * @param index
	 * @exception NullPointerException
	 *                if input or reason is null
	 * @exception IllegalArgumentException
	 *                if index < -1
	 */
	public URISyntaxException(String input, String reason, int index) {
		super(reason);

		if (input == null || reason == null)
			throw new NullPointerException();

		if (index < -1)
			throw new IllegalArgumentException();

		this.input = input;
		this.index = index;
	}

	/**
	 * Constructs a URISyntaxException containing the string that caused the
	 * exception and a description of the error.
	 * 
	 * @param input
	 * @param reason
	 * 
	 * @exception NullPointerException
	 *                if input or reason is null
	 */
	public URISyntaxException(String input, String reason) {
		super(reason);

		if (input == null || reason == null)
			throw new NullPointerException();

		this.input = input;
		index = -1;
	}

	/**
	 * Answers the index at which the syntax error was found, or -1 if the index
	 * is unknown/unavailable.
	 * 
	 * @return the index of the syntax error
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Answers a String describing the syntax error in the URI string
	 * 
	 * @return a String describing the syntax error
	 */
	public String getReason() {
		return super.getMessage();
	}

	/**
	 * Answers the String that contained the syntax error
	 * 
	 * @return the String that caused the exception
	 */
	public String getInput() {
		return input;
	}

	/**
	 * Returns a description of the exception, including the reason, the string
	 * that had the syntax error, and the index of the syntax error if
	 * available.
	 * 
	 * @return a String containing information about the exception.
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage() {
		String reason = super.getMessage();

		if (index != -1) {
			return Msg.getString("K0326", new String[] { reason,
					Integer.toString(index), input });
		}
		return Msg.getString("K0327", new String[] { reason, input });
	}
}
