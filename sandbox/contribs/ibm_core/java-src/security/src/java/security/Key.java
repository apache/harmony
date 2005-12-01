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
 * Defines the basic properties of all key objects.
 * 
 * @see PublicKey
 */

public interface Key extends java.io.Serializable {

	// Set the version id so we are field-compatible with JDK.
	public static final long serialVersionUID = 6603384152749567654L;

	/**
	 * Answers the name of the algorithm that this key will work with. If the
	 * algorithm is unknown, it answers null.
	 * 
	 * 
	 * @return String the receiver's algorithm
	 */
	public abstract String getAlgorithm();

	/**
	 * Answers the encoded form of the receiver.
	 * 
	 * 
	 * @return byte[] the encoded form of the receiver
	 */
	public abstract byte[] getEncoded();

	/**
	 * Answers the name of the format used to encode the key, or null if it can
	 * not be encoded.
	 * 
	 * 
	 * @return String the receiver's encoding format
	 */
	public abstract String getFormat();

}
