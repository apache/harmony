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


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Superclass of permissions which have names but no action lists.
 * 
 */

public abstract class BasicPermission extends Permission implements
		Serializable {
	static final long serialVersionUID = 6279438298436773498L;

	/**
	 * If the receiver was a correctly formatted wildcarded pattern, then this
	 * is the name with the '*' character removed. If it's not wildcarded, then
	 * it is null.
	 */
	private transient String wildcard;

	/**
	 * Creates an instance of this class with the given name and action list.
	 * 
	 * @param name
	 *            String the name of the new permission.
	 */
	public BasicPermission(String name) {
		super(name);
		// Verified programatically that JDK only treats the permission
		// as wildcarded if it has the shape described by this code.
		// Names with * characters in other positions are just treated
		// as non-wildcarded patterns, rather than exceptional conditions.
		int length = name.length();
		if (length > 1) {
			if (name.charAt(length - 1) == '*'
					&& name.charAt(length - 2) == '.')
				wildcard = name.substring(0, length - 1);
		} else if (length == 1 && name.charAt(0) == '*') {
			wildcard = "";
		} else if (length == 0)
			throw new IllegalArgumentException();
	}

	/**
	 * Creates an instance of this class with the given name and action list.
	 * The action list is ignored.
	 * 
	 * @param name
	 *            String the name of the new permission.
	 * @param actions
	 *            String ignored.
	 */
	public BasicPermission(String name, String actions) {
		this(name);
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the <em>same</em> object using a class specific comparison. In this
	 * case, the receiver and the object must have the same class and name.
	 * 
	 * @param o
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object
	 *         <code>false</code> if it is different from this object
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o != null && getClass() == o.getClass())
			return getName().equals(((BasicPermission) o).getName());
		return false;
	}

	/**
	 * Answers the actions associated with the receiver. BasicPermission objects
	 * have no actions, so answer the empty string.
	 * 
	 * @return String the actions associated with the receiver.
	 */
	public String getActions() {
		return "";
	}

	/**
	 * Answers an integer hash code for the receiver. Any two objects which
	 * answer <code>true</code> when passed to <code>equals</code> must
	 * answer the same value for this method.
	 * 
	 * @return int the receiver's hash
	 * 
	 * @see #equals
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/**
	 * Indicates whether the argument permission is implied by the receiver.
	 * 
	 * @return boolean <code>true</code> if the argument permission is implied
	 *         by the receiver, and <code>false</code> if it is not.
	 * @param p
	 *            java.security.Permission the permission to check
	 */
	public boolean implies(Permission p) {
		if (this == p)
			return true;
		if (p != null && getClass() == p.getClass()) {
			if (wildcard != null)
				return p.getName().startsWith(wildcard);
			return p.getName().equals(getName());
		}
		return false;
	}

	/**
	 * Answers a new PermissionCollection for holding permissions of this class.
	 * Answer null if any permission collection can be used.
	 * <p>
	 * Note: For BasicPermission (and subclasses which do not override this
	 * method), the collection which is returned does <em>not</em> invoke the
	 * .implies method of the permissions which are stored in it when checking
	 * if the collection implies a permission. Instead, it assumes that if the
	 * type of the permission is correct, and the name of the permission is
	 * correct, there is a match.
	 * 
	 * @return a new PermissionCollection or null
	 * 
	 * @see java.security.BasicPermissionCollection
	 */
	public PermissionCollection newPermissionCollection() {
		return new BasicPermissionCollection();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		// Verified programatically that JDK only treats the permission
		// as wildcarded if it has the shape described by this code.
		// Names with * characters in other positions are just treated
		// as non-wildcarded patterns, rather than exceptional conditions.
		String name = getName();
		int length = name.length();
		if (length > 1) {
			if (name.charAt(length - 1) == '*'
					&& name.charAt(length - 2) == '.')
				wildcard = name.substring(0, length - 1);
		} else if (length == 1 && name.charAt(0) == '*') {
			wildcard = "";
		}
	}
}
