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
 * This runtime exception is thrown when a program attempts to cast a an object
 * to a type which it is not compatable with.
 * 
 */
public class ClassCastException extends RuntimeException {
	static final long serialVersionUID = -9223365651070458532L;

	/**
	 * Constructs a new instance of this class with its walkback filled in.
	 * 
	 */
	public ClassCastException() {
		super();
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 */
	public ClassCastException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * @param instanceClass
	 *            Class The class being cast from.
	 * 
	 * @param castClass
	 *            Class The class being cast to.
	 */
	ClassCastException(Class instanceClass, Class castClass) {
		super(com.ibm.oti.util.Msg.getString("K0340", instanceClass.getName(),
				castClass.getName()));
	}
}
