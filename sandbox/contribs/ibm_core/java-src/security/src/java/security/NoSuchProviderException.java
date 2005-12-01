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

package java.security;


/**
 * Instances of this class are thrown when an attempt is made to access a
 * provider by name which is not currently available.
 * 
 * 
 * @see Throwable
 */
public class NoSuchProviderException extends GeneralSecurityException {

	/**
	 * Constructs a new instance of this class with its walkback filled in.
	 * 
	 */
	public NoSuchProviderException() {
		super();
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 */
	public NoSuchProviderException(String detailMessage) {
		super(detailMessage);
	}
}
