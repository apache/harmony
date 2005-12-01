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

package java.util;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * A PermissionCollection for holding PropertyPermissions.
 */
class PropertyPermissionCollection extends PermissionCollection {
	
	static final long serialVersionUID = 7015263904581634791L;

	Hashtable permissions = new Hashtable(30);

	public void add(Permission perm) {
		if (!isReadOnly()) {
			Permission previous = (Permission) permissions.put(perm.getName(),
					perm);
			// if the permission already existed but with only "read" or "write"
			// set, then replace with both set
			if (previous != null
					&& !previous.getActions().equals(perm.getActions()))
				permissions.put(perm.getName(), new PropertyPermission(perm
						.getName(), "read,write")); //$NON-NLS-1$
		} else
			throw new IllegalStateException();
	}

	public Enumeration elements() {
		return permissions.elements();
	}

	public boolean implies(Permission perm) {
		Enumeration elemEnum = elements();
		while (elemEnum.hasMoreElements())
			if (((Permission) elemEnum.nextElement()).implies(perm))
				return true;
		// At this point, the only way it can succeed is if both read and write
		// are set,
		// and these are separately granted by two different permissions with
		// one
		// representing a parent directory.
		return perm.getActions().equals("read,write") //$NON-NLS-1$
				&& implies(new PropertyPermission(perm.getName(), "read")) //$NON-NLS-1$
				&& implies(new PropertyPermission(perm.getName(), "write")); //$NON-NLS-1$
	}

	private static final ObjectStreamField[] serialPersistentFields = {
			new ObjectStreamField("permissions", Hashtable.class), //$NON-NLS-1$
			new ObjectStreamField("all_allowed", Boolean.TYPE) }; //$NON-NLS-1$

	private void writeObject(ObjectOutputStream stream) throws IOException {
		ObjectOutputStream.PutField fields = stream.putFields();
		fields.put("permissions", permissions); //$NON-NLS-1$
		fields.put("all_allowed", false); //$NON-NLS-1$
		stream.writeFields();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField fields = stream.readFields();
		permissions = (Hashtable) fields.get("permissions", null); //$NON-NLS-1$
	}
}
