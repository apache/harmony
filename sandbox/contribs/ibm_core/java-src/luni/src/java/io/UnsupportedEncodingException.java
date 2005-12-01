/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
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

package java.io;

/**
 * This IO exception is thrown when a program asks for a particular character
 * converter and it is not available.
 * 
 */
public class UnsupportedEncodingException extends IOException {

	/**
	 * Constructs a new instance of this class with its walkback filled in.
	 */
	public UnsupportedEncodingException() {
		super();
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * @param detailMessage
	 *            the detail message for the exception.
	 */
	public UnsupportedEncodingException(String detailMessage) {
		super(detailMessage);
	}

}
