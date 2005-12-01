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
 * A Hashtable based collection of BasicPermission objects. It make a number of
 * assumptions about what is stored in it, allowing it to be quite performant.
 * <p>
 * Limitation 1: It does <em>not</em> actually check that the contained
 * permission objects <em>grant</em> the permission being checked, only that a
 * permission with a matching name is present. Thus, this collection can not be
 * used where the Permission objects implement interesting semantics in their
 * implies methods.
 * <p>
 * Limitation 2: It assumes (and does not check) that all permissions which are
 * stored in the collection are instances of the same class.
 * <p>
 * Limitation 3: Because it uses a hashtable, it will not record the fact that
 * multiple occurances of .equal permissions have been added.
 * 
 */

class BasicPermissionCollection extends PermissionCollection {
	static final long serialVersionUID = 739301742472979399L;

	/**
	 * A flag to indicate whether the "grant all wildcard" (i.e. "*") has been
	 * added.
	 */
	boolean all_allowed = false;

	/**
	 * A hashtable which maps from a permission name to the matching permission.
	 * Multiple occurances of the same permission are ignored.
	 */
	Hashtable permissions = new Hashtable(8);

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public BasicPermissionCollection() {
		super();
	}

	/**
	 * Adds the argument to the collection.
	 * 
	 * 
	 * @param perm
	 *            java.security.Permission the permission to add to the
	 *            collection
	 */
	public void add(Permission perm) {
		if (isReadOnly()) {
			throw new IllegalStateException();
		}
		String name = perm.getName();
		all_allowed = all_allowed || name.equals("*");
		permissions.put(name, perm);
	}

	/**
	 * Answers an enumeration of the permissions in the receiver.
	 * 
	 * 
	 * @return Enumeration the permissions in the receiver.
	 */
	public Enumeration elements() {
		return permissions.elements();
	}

	/**
	 * Indicates whether the argument permission is implied by the permissions
	 * contained in the receiver. Note that, the permissions are not consulted
	 * during the operation of this method.
	 * 
	 * 
	 * @return boolean <code>true</code> if the argument permission is implied
	 *         by the permissions in the receiver, and <code>false</code> if
	 *         it is not.
	 * @param perm
	 *            java.security.Permission the permission to check
	 */
	public boolean implies(Permission perm) {
		if (all_allowed)
			return true;
		String name = perm.getName();
		if (permissions.get(name) != null)
			return true;
		int i = name.lastIndexOf('.');
		while (i >= 0) {
			// Fail for strings of the form "foo..bar" or "foo.".
			if (i + 1 == name.length())
				return false;
			name = name.substring(0, i);
			if (permissions.get(name + ".*") != null)
				return true;
			i = name.lastIndexOf('.');
		}
		return false;
	}
}
