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


import java.security.Permission;
import java.util.Vector;

/**
 * FilePermissionCollection is a class which holds a collection of
 * FilePermission objects and can answer a boolean indicating whether or not a
 * specific permissions is implied by a FilePermissionCollection.
 * 
 */
final class FilePermissionCollection extends java.security.PermissionCollection
		implements Serializable {
	static final long serialVersionUID = 2202956749081564585L;

	Vector permissions = new Vector();

	/**
	 * Construct a new FilePermissionCollection.
	 */
	public FilePermissionCollection() {
		super();
	}

	/**
	 * Add a permission Object to the permission collection.
	 * 
	 * @see java.security.PermissionCollection#add(java.security.Permission)
	 */
	public void add(Permission permission) {
		if (!isReadOnly()) {
			if (permission instanceof FilePermission)
				permissions.addElement(permission);
			else
				throw new java.lang.IllegalArgumentException(permission
						.toString());
		} else
			throw new IllegalStateException();
	}

	/**
	 * Answers an enumeration for the collection of permissions.
	 * 
	 * @see java.security.PermissionCollection#elements()
	 */
	public java.util.Enumeration elements() {
		return permissions.elements();
	}

	/**
	 * Answers a boolean indicating whether or not this permissions collection
	 * implies a specific <code>permission</code>.
	 * 
	 * @see java.security.PermissionCollection#implies(java.security.Permission)
	 */
	public boolean implies(Permission permission) {
		if (permission instanceof FilePermission) {
			FilePermission fp = (FilePermission) permission;
			int matchedMask = 0;
			int i = 0;
			while (i < permissions.size()
					&& ((matchedMask & fp.mask) != fp.mask)) {
				// Cast will not fail since we added it
				matchedMask |= ((FilePermission) permissions.elementAt(i))
						.impliesMask(permission);
				i++;
			}
			return ((matchedMask & fp.mask) == fp.mask);
		}
		return false;
	}
}
