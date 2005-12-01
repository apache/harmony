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


/**
 * Abstract superclass of classes which represent the system security policy.
 * 
 */
public abstract class Policy {

	/**
	 * The currently in place security policy.
	 */
	static Policy policy;

	/**
	 * Constructs a new instance of this class.
	 * 
	 */
	public Policy() {
		super();
	}

	/**
	 * Answers the current system security policy. If no policy has been
	 * instantiated then this is done using the security property <EM>policy.provider</EM>
	 * 
	 * 
	 * @return Policy the current system security policy.
	 */
	public static Policy getPolicy() {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(SecurityPermission.permissionToGetPolicy);
		return getPolicyImpl();
	}

	static Policy getPolicyImpl() {
		if (policy == null) {
			// Get the policy provider before the security manager is set
			String policyName = Security.getProperty("policy.provider");

			if (System.getSecurityManager() == null) {
				String clName = System.getProperty("java.security.manager");
				if (clName != null) {
					SecurityManager sm = null;
					if (clName.length() == 0) {
						sm = new SecurityManager();
					} else {
						try {
							Class cl = Class.forName(clName);
							sm = (SecurityManager) cl.newInstance();
						} catch (Exception e) {
							throw new InternalError(com.ibm.oti.util.Msg
									.getString("K00e3", clName));
						}
					}
					System.setSecurityManager(sm);
				}
			}
			try {
				if (policyName != null)
					policy = (Policy) Class.forName(policyName).newInstance();
			} catch (Exception e) {
				// Intentionally empty
			}
			if (policy == null) {
				// if instantiating the policy.provider failed for any reason,
				// assume use of the default policy provider
				policy = new com.ibm.oti.util.DefaultPolicy();
			}
		}
		return policy;
	}

	/**
	 * Sets the system-wide policy object if it is permitted by the security
	 * manager.
	 * 
	 * @param p
	 *            Policy the policy object that needs to be set.
	 */
	public static void setPolicy(Policy p) {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(SecurityPermission.permissionToSetPolicy);
		policy = p;
	}

	/**
	 * Answers a PermissionCollection describing what permissions are available
	 * to the given CodeSource based on the current security policy.
	 * <p>
	 * Note that this method is <em>not</em> called for classes which are in
	 * the system domain (i.e. system classes). System classes are
	 * <em>always</em> given full permissions (i.e. AllPermission). This can
	 * not be changed by installing a new Policy.
	 * 
	 * 
	 * @param cs
	 *            CodeSource the code source to compute the permissions for.
	 * @return PermissionCollection the permissions the code source should have.
	 */
	public abstract PermissionCollection getPermissions(CodeSource cs);

	/**
	 * Reloads the policy configuration, depending on how the type of source
	 * location for the policy information.
	 * 
	 * 
	 */
	public abstract void refresh();

	/**
	 * Answers a PermissionCollection describing what permissions are available
	 * to the given ProtectionDomain (more specifically, its CodeSource) based
	 * on the current security policy.
	 * 
	 * @param domain
	 *            ProtectionDomain the protection domain to compute the
	 *            permissions for.
	 * @return PermissionCollection the permissions the code source should have.
	 */
	public PermissionCollection getPermissions(ProtectionDomain domain) {
		return getPermissions(domain.getCodeSource());
	}

	/**
	 * Answers whether the Permission is implied by the PermissionCollection of
	 * the Protection Domain
	 * 
	 * @param domain
	 *            ProtectionDomain for which Permission to be checked
	 * @param permission
	 *            Permission for which authorization is to be verified
	 * @return boolean Permission implied by ProtectionDomain
	 */
	public boolean implies(ProtectionDomain domain, Permission permission) {
		return getPermissions(domain).implies(permission);
	}
}
