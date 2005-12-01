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


import java.io.Serializable;
import java.util.Enumeration;

import com.ibm.oti.util.PriviAction;

/**
 * Abstract superclass of classes which are collections of Permission objects.
 * 
 */
public abstract class PermissionCollection implements Serializable {

	/**
	 * Set to true if the collection is read only.
	 */
	private boolean readOnly = false;

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public PermissionCollection() {
		// Intentionally empty
	}

	/**
	 * Adds the argument to the collection.
	 * 
	 * 
	 * @param permission
	 *            java.security.Permission the permission to add to the
	 *            collection.
	 * @exception IllegalStateException
	 *                if the collection is read only.
	 */
	public abstract void add(Permission permission);

	/**
	 * Answers an enumeration of the permissions in the receiver.
	 * 
	 * 
	 * @return Enumeration the permissions in the receiver.
	 */
	public abstract Enumeration elements();

	/**
	 * Indicates whether the argument permission is implied by the permissions
	 * contained in the receiver.
	 * 
	 * 
	 * @return boolean <code>true</code> if the argument permission is implied
	 *         by the permissions in the receiver, and <code>false</code> if
	 *         it is not.
	 * @param permission
	 *            java.security.Permission the permission to check
	 */
	public abstract boolean implies(Permission permission);

	/**
	 * Indicates whether new permissions can be added to the receiver.
	 * 
	 * 
	 * @return boolean <code>true</code> if the receiver is read only
	 *         <code>false</code> if new elements can still be added to the
	 *         receiver.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Marks the receiver as read only, so that no new permissions can be added
	 * to it.
	 * 
	 */
	public void setReadOnly() {
		readOnly = true;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		String newline = (String) AccessController
				.doPrivileged(new PriviAction("line.separator", "\n"));
		StringBuffer answer = new StringBuffer();
		answer.append(super.toString());
		answer.append(" (");
		Enumeration perms = elements();
		if (perms.hasMoreElements()) {
			answer.append(newline);
		}
		while (perms.hasMoreElements()) {
			Permission p = (Permission) perms.nextElement();
			answer.append("  ");
			answer.append(p);
			answer.append(newline);
		}
		answer.append(")");
		return answer.toString();
	}
}
