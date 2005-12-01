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
 * This runtime exception is thrown when an access control check indicates that
 * access should not be granted.
 * 
 */
public class AccessControlException extends SecurityException {

	static final long serialVersionUID = 5138225684096988535L;

	/**
	 * The permission object associated with the receiver.
	 */
	Permission perm;

	/**
	 * Constructs a new instance of this class with its walkback and message
	 * filled in.
	 * 
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 */
	public AccessControlException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructs a new instance of this class with its walkback, message and
	 * associated permission all filled in.
	 * 
	 * 
	 * @param detailMessage
	 *            String The detail message for the exception.
	 * @param perm
	 *            Permission The failed permission.
	 */
	public AccessControlException(String detailMessage, Permission perm) {
		super(detailMessage);
		this.perm = perm;
	}

	/**
	 * Answers the receiver's permission.
	 * 
	 * 
	 * @return Permission the receiver's permission
	 */
	public Permission getPermission() {
		return perm;
	}
}
