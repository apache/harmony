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
 * Subclass of Permission whose instances imply all other permissions. Granting
 * this permission is equivalent to disabling security.
 * 
 */
public final class AllPermission extends Permission {
	static final long serialVersionUID = -2916474571451318075L;

	/**
	 * Constructs a new instance of this class.
	 */
	public AllPermission() {
		super("all_permissions");
	}

	/**
	 * Constructs a new instance of this class. The two argument version is
	 * provided for class <code>Policy</code> so that it has a consistant call
	 * pattern across all Permissions. The name and action list are both
	 * ignored.
	 * 
	 * @param permissionName
	 *            java.lang.String ignored.
	 * @param actions
	 *            java.lang.String ignored.
	 */
	public AllPermission(String permissionName, String actions) {
		super("all_permissions");
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. All
	 * AllPermissions are equal to eachother.
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		return o instanceof AllPermission;
	}

	/**
	 * Answers the actions associated with the receiver. Since AllPermission
	 * objects allow all actions, answer with the string "<all actions>".
	 * 
	 * @return String the actions associated with the receiver.
	 */
	public String getActions() {
		return "<all actions>";
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/**
	 * Indicates whether the argument permission is implied by the receiver.
	 * AllPermission objects imply all other permissions.
	 * 
	 * @return boolean <code>true</code> if the argument permission is implied
	 *         by the receiver, and <code>false</code> if it is not.
	 * @param p
	 *            java.security.Permission the permission to check
	 */
	public boolean implies(Permission p) {
		return true;
	}

	/**
	 * Answers a new PermissionCollection for holding permissions of this class.
	 * Answer null if any permission collection can be used.
	 * 
	 * @return a new PermissionCollection or null
	 * 
	 * @see java.security.BasicPermissionCollection
	 */
	public PermissionCollection newPermissionCollection() {
		return new AllPermissionCollection();
	}

}
