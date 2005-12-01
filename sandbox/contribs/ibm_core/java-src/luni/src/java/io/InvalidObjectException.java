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
 * The object graph loaded (deserialized) can be validated by a collection of
 * validator objects. If these decide the validation fails, then will throw
 * InvalidObjectException.
 * 
 * @see ObjectInputStream#registerValidation(ObjectInputValidation, int)
 * @see ObjectInputValidation#validateObject()
 */
public class InvalidObjectException extends ObjectStreamException {

	static final long serialVersionUID = 3233174318281839583L;

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * @param detailMessage
	 *            The detail message for the exception.
	 */
	public InvalidObjectException(String detailMessage) {
		super(detailMessage);
	}

}
