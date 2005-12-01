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


import com.ibm.oti.util.PriviAction;

/**
 * This class represents a domain in which classes from the same source (URL)
 * and signed by the same keys are stored. All the classes inside are given the
 * same permissions.
 * <p>
 * Note: a class can only belong to one and only one protection domain.
 */
public class ProtectionDomain {

	/**
	 * The code source of the classes in this domain.
	 */
	private CodeSource cs;

	/**
	 * The permissions which should be provided to the classes in this domain.
	 */
	private PermissionCollection pc;

	/**
	 * ClassLoader associated with the ProtectionDomain
	 */
	private ClassLoader classLoader;

	/**
	 * Principals associated with the ProtectionDomain
	 */
	private Principal[] principals;

	/**
	 * Allow dynamic permission checking (by delegating to Policy)
	 */
	private boolean dynamicPermissionChecking = false;

	/**
	 * Contructs a protection domain from the given code source and the
	 * permissions that that should be granted to the classes which are
	 * encapsulated in it.
	 * @param codesource 
	 * @param permissions 
	 */
	public ProtectionDomain(CodeSource codesource,
			PermissionCollection permissions) {
		super();
		cs = codesource;
		pc = permissions;
		if (pc != null)
			pc.setReadOnly();
	}

	/**
	 * Contructs a protection domain from the given code source and the
	 * permissions that that should be granted to the classes which are
	 * encapsulated in it. 
	 * 
	 * This constructor also allows the association of a ClassLoader and group
	 * of Principals.
	 * 
	 * @param codesource
	 *            the CodeSource associated with this domain
	 * @param permissions
	 *            the Permissions associated with this domain
	 * @param classloader
	 *            the ClassLoader associated with this domain
	 * @param principals
	 *            the Principals associated with this domain
	 */
	public ProtectionDomain(CodeSource codesource,
			PermissionCollection permissions, ClassLoader classloader,
			Principal[] principals) {
		super();
		cs = codesource;

		pc = permissions;
		if (pc != null) {
			pc.setReadOnly();
		}

		dynamicPermissionChecking = true;

		this.classLoader = classloader;

		this.principals = principals;

	}

	/**
	 * Answers the code source of this domain.
	 * 
	 * @return java.security.CodeSource the code source of this domain
	 */
	public final CodeSource getCodeSource() {
		return cs;
	}

	/**
	 * Answers the permissions that should be granted to the classes which are
	 * encapsulated in this domain.
	 * 
	 * @return java.security.PermissionCollection collection of permissions
	 *         associated with this domain.
	 */
	public final PermissionCollection getPermissions() {
		return pc;
	}

	/**
	 * Returns the Principals associated with this ProtectionDomain. A change to
	 * the returned array will not impact the ProtectionDomain.
	 * 
	 * @return Principals[] Principals associated with the ProtectionDomain.
	 */
	public final Principal[] getPrincipals() {

		// No principals? Return an empty array.
		if (this.principals == null) {
			return new Principal[0];
		}

		Principal[] result = new Principal[this.principals.length];
		System.arraycopy(this.principals, 0, result, 0, this.principals.length);
		return result;

	}

	/**
	 * Returns the ClassLoader associated with the ProtectionDomain
	 * 
	 * @return ClassLoader associated ClassLoader
	 */
	public final ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Determines whether the permission collection of this domain implies the
	 * argument permission.
	 * 
	 * 
	 * @return boolean true if this permission collection implies the argument
	 *         and false otherwise.
	 * @param perm
	 *            java.security.Permission the permission to check.
	 */
	public boolean implies(Permission perm) {

		PermissionCollection perms = null;
		final ProtectionDomain domain = this;
		if (dynamicPermissionChecking) {
			Policy policy = (Policy) AccessController
					.doPrivileged(new PriviAction());
			perms = policy.getPermissions(domain);
		} else {
			perms = this.pc;
		}

		return perms != null && perms.implies(perm);
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return String a printable representation for the receiver.
	 */
	public String toString() {
		StringBuffer answer = new StringBuffer("ProtectionDomain ");
		if (cs == null) {
			answer.append("null");
		} else {
			answer.append(cs.toString());
		}
		// The default protection domain grants access to this
		String crlf = (String) AccessController.doPrivileged(new PriviAction(
				"line.separator"));
		answer.append(crlf);
		if (pc == null) {
			answer.append("null");
		} else {
			answer.append(pc.toString());
		}
		return answer.toString();
	}
}
