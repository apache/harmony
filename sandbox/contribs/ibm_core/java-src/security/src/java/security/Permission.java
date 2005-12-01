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

package java.security;


import java.io.Serializable;


/**
 * Abstract superclass of all classes which represent permission to access
 * system resources.
 * 
 */
public abstract class Permission implements Guard, Serializable {
	static final long serialVersionUID = -5636570222231596674L;

	/**
	 * The name of the permission.
	 */
	private String name;

	/**
	 * Constructs a new instance of this class with its name set to the
	 * argument.
	 * 
	 * 
	 * @param permissionName
	 *            String the name of the permission.
	 */
	public Permission(String permissionName) {
		name = permissionName;
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. The
	 * implementation in Object answers true only if the argument is the exact
	 * same object as the receiver (==).
	 * 
	 * 
	 * @param o
	 *            Object the object to compare with this object.
	 * @return boolean <code>true</code> if the object is the same as this
	 *         object <code>false</code> if it is different from this object.
	 * @see #hashCode
	 */
	public abstract boolean equals(Object o);

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>.equals</code> must
	 * answer the same value for this method.
	 * 
	 * 
	 * @return int the receiver's hash.
	 * 
	 * @see #equals
	 */
	public abstract int hashCode();

	/**
	 * Checks that the receiver is granted in the current access control context
	 * (Guard interface). Note that the argument is not currently used.
	 * 
	 * 
	 * @param object
	 *            Object ignored.
	 * 
	 * @exception java.lang.SecurityException
	 *                If access is not granted
	 */
	public void checkGuard(Object object) throws SecurityException {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(this);
	}

	/**
	 * Answers the actions associated with the receiver. Subclasses should
	 * return their actions in canonical form. If no actions are associated with
	 * the receiver, the empty string should be returned.
	 * 
	 * 
	 * @return String the receiver's actions.
	 */
	public abstract String getActions();

	/**
	 * Answers the name of the receiver.
	 * 
	 * 
	 * @return String the receiver's name.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Indicates whether the argument permission is implied by the receiver.
	 * 
	 * 
	 * @return boolean <code>true</code> if the argument permission is implied
	 *         by the receiver, and <code>false</code> if it is not.
	 * @param permission
	 *            Permission the permission to check.
	 */
	public abstract boolean implies(Permission permission);

	/**
	 * Answers a new PermissionCollection for holding permissions of this class.
	 * Answer null if any permission collection can be used.
	 * 
	 * 
	 * @return PermissionCollection or null a suitable permission collection for
	 *         instances of the class of the receiver.
	 */
	public PermissionCollection newPermissionCollection() {
		return null;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("Permission : "); //$NON-NLS-1$
		result.append(getName());
		result.append(" : with actions " + getActions()); //$NON-NLS-1$
		return result.toString();
	}

}
