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
 * SecurityPermission objects guard access to the mechanisms which implement
 * security. Security permissions have names, but not actions.
 * 
 */
public final class SecurityPermission extends BasicPermission {
	static final long serialVersionUID = 5236109936224050470L;

	/**
	 * Constants for security permissions used in this package.
	 */
	static final SecurityPermission permissionToGetPolicy = new SecurityPermission(
			"getPolicy");

	static final SecurityPermission permissionToSetPolicy = new SecurityPermission(
			"setPolicy");

	/**
	 * Creates an instance of this class with the given name.
	 * 
	 * @param name
	 *            String the name of the new permission.
	 */
	public SecurityPermission(String name) {
		super(name);
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
	public SecurityPermission(String name, String actions) {
		super(name, actions);
	}
}
