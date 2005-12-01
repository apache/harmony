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
import java.util.Vector;

class AllPermissionCollection extends PermissionCollection {
	static final long serialVersionUID = -4023755556366636806L;

	boolean all_allowed = false;

	public void add(Permission permission) {
		if (!(permission instanceof AllPermission)) {
			throw new IllegalArgumentException(permission.toString());
		}
		if (isReadOnly()) {
			throw new IllegalStateException();
		}
		all_allowed = true;
	}

	public Enumeration elements() {
		Vector temp = new Vector();
		if (all_allowed)
			temp.addElement(new AllPermission());
		return temp.elements();
	}

	public boolean implies(Permission permission) {
		return all_allowed;
	}
}
