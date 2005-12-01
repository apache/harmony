/* Copyright 2000, 2002 The Apache Software Foundation or its licensors, as applicable
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
import java.util.Vector;

class UnresolvedPermissionCollection extends PermissionCollection {
	static final long serialVersionUID = -7176153071733132400L;

	Hashtable permissions = new Hashtable(8);

	public void add(Permission permission) {
		if (isReadOnly())
			throw new IllegalStateException();
		Vector elements = (Vector) permissions.get(permission.getName());
		if (elements == null) {
			elements = new Vector();
			permissions.put(permission.getName(), elements);
		}
		elements.addElement(permission);
	}

	public Enumeration elements() {
		return new Enumeration() {
			Enumeration vEnum, pEnum = permissions.elements();

			Object next = findNext();

			private Object findNext() {
				if (vEnum != null) {
					if (vEnum.hasMoreElements())
						return vEnum.nextElement();
				}
				if (!pEnum.hasMoreElements())
					return null;
				vEnum = ((Vector) pEnum.nextElement()).elements();
				return vEnum.nextElement();
			}

			public boolean hasMoreElements() {
				return next != null;
			}

			public Object nextElement() {
				Object result = next;
				next = findNext();
				return result;
			}
		};
	}

	public boolean implies(Permission permission) {
		return false;
	}

	Vector getPermissions(String name) {
		return (Vector) permissions.get(name);
	}
}
