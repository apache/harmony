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
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * A heterogeneous collection of permissions.
 * 
 */
public final class Permissions extends PermissionCollection implements
		Serializable {
	static final long serialVersionUID = 4858622370623524688L;

	/**
	 * Maps a Permission's class to an appropriate PermissionCollection.
	 */
	Hashtable perms = new Hashtable(8);

	/**
	 * Set to an AllPermissionCollection if this Permissions contains
	 * AllPermission.
	 */
	PermissionCollection allPermission;

	// An Enumeration over the elements in the receiver.
	// Note that, this is useful for printing them and not much
	// else, since it ignores the PermissionCollections that
	// they are stored in. This breaks any added semantics
	// provided by a particular PermissionCollection class.
	// In other words, the Permissions object may report that
	// a permission is implied in cases where that permission
	// is not implied by any *one* of the entries returned by
	// this enumeration.
	private class PermissionsEnumeration implements Enumeration {
		Enumeration enumMap = perms.elements();

		PermissionCollection c;

		Enumeration enumC;

		Permission next = findNextPermission();

		public boolean hasMoreElements() {
			return next != null;
		}

		public Object nextElement() {
			if (next == null) {
				throw new NoSuchElementException();
			}
			Object answer = next;
			next = findNextPermission();
			return answer;
		}

		// This method is the important one. It looks for and
		// answers the next available permission. If there are
		// no permissions left to return, it answers null.
		private Permission findNextPermission() {
			// Loop until we get a collection with at least one element.
			while (c == null && enumMap.hasMoreElements()) {
				c = (PermissionCollection) enumMap.nextElement();
				enumC = c.elements();
				if (!enumC.hasMoreElements())
					c = null;
			}
			// At this point, c == null if there are no more elements,
			// and otherwise is the first collection with a free element
			// (with enumC set up to return that element).
			if (c == null) {
				// no more elements, so return null;
				return null;
			}
			Permission answer = (Permission) enumC.nextElement();
			if (!enumC.hasMoreElements()) {
				c = null;
			}
			return answer;
		}
	}

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public Permissions() {
		super();
	}

	/**
	 * Adds the argument to the collection.
	 * 
	 * 
	 * @param permission
	 *            java.security.Permission the permission to add to the
	 *            collection
	 */
	public void add(Permission permission) {
		if (isReadOnly())
			throw new SecurityException();
		if (permission.getClass() == AllPermission.class) {
			(allPermission = findCollection(permission)).add(permission);
		} else
			findCollection(permission).add(permission);
	}

	/**
	 * Answers an enumeration of the permissions in the receiver.
	 * 
	 * 
	 * @return Enumeration the permissions in the receiver.
	 */
	public Enumeration elements() {
		PermissionsEnumeration answer = new PermissionsEnumeration();
		return answer;
	}

	/**
	 * Find the appropriate permission collection to use for the given
	 * permission.
	 * 
	 * 
	 * @param permission
	 *            Permission the permission to find a collection for
	 * @return PermissionCollection the collection to use with the permission.
	 */
	private PermissionCollection findCollection(Permission permission) {
		Class cl = permission.getClass();
		PermissionCollection answer = (PermissionCollection) perms.get(cl);
		if (answer == null) {
			answer = permission.newPermissionCollection();
			if (answer == null)
				answer = new PermissionsHash();
			perms.put(cl, answer);
		}
		return answer;
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
		// Optimization 1: If AllPermission is in there, then assume
		// we know what it does and return true immediately.
		if (allPermission != null)
			return true;

		// Optimization 2: check if permissions of this type have
		// been added in a collection of their own. If so, look
		// in that collection first.
		PermissionCollection bin = (PermissionCollection) perms.get(perm
				.getClass());
		if (bin != null)
			return bin.implies(perm);

		// Resolve any required unresolved permissions
		UnresolvedPermissionCollection unresolvedCollection = (UnresolvedPermissionCollection) perms
				.get(UnresolvedPermission.class);
		if (unresolvedCollection != null) {
			Vector permissions = unresolvedCollection.getPermissions(perm
					.getClass().getName());
			if (permissions != null) {
				Enumeration permsEnum = permissions.elements();
				while (permsEnum.hasMoreElements()) {
					Permission resolved = ((UnresolvedPermission) permsEnum
							.nextElement()).resolve(perm.getClass()
							.getClassLoader());
					if (resolved != null) {
						bin = findCollection(resolved);
						bin.add(resolved);
					}
				}
				if (bin != null)
					return bin.implies(perm);
			}
		}
		return false;
	}
}
