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


import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A simple Hashtable based collection of Permission objects.
 * <p>
 * The class' .implies method simply scans each permission individually and asks
 * if the permission should be granted. No addition semantics is provided by the
 * collection, so it is not possible to grant permissions whose "grantedness" is
 * split across multiple stored Permissions.
 * <p>
 * Instances of this class can be used to store heterogeneous collections of
 * permissions, as long as it is not necessary to remember when multiple
 * occurances of .equal permissions are added.
 * 
 */

class PermissionsHash extends PermissionCollection {
	static final long serialVersionUID = -8491988220802933440L;

	/**
	 * A hashtable to store the elements of the collection.
	 */
	Hashtable perms = new Hashtable(8);

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public PermissionsHash() {
		super();
	}

	/**
	 * Adds the argument to the collection.
	 * 
	 * 
	 * @param perm
	 *            java.security.Permission the permission to add to the
	 *            collection.
	 * @exception IllegalStateException
	 *                if the collection is read only.
	 */
	public void add(Permission perm) {
		if (isReadOnly()) {
			throw new IllegalStateException();
		}
		perms.put(perm, perm);
	}

	/**
	 * Answers an enumeration of the permissions in the receiver.
	 * 
	 * 
	 * @return Enumeration the permissions in the receiver.
	 */
	public Enumeration elements() {
		return perms.keys();
	}

	/**
	 * Indicates whether the argument permission is implied by the permissions
	 * contained in the receiver.
	 * 
	 * 
	 * @return boolean <code>true</code> if the argument permission is implied
	 *         by the permissions in the receiver, and <code>false</code> if
	 *         it is not.
	 * @param perm
	 *            java.security.Permission the permission to check
	 */
	public boolean implies(Permission perm) {
		Enumeration elemEnum = elements();
		while (elemEnum.hasMoreElements())
			if (((Permission) elemEnum.nextElement()).implies(perm))
				return true;
		return false;
	}
}
