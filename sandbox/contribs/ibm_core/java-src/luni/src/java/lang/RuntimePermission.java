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

package java.lang;


import java.security.BasicPermission;

/**
 * RuntimePermission objects represent access to runtime support.
 */
public final class RuntimePermission extends BasicPermission {

	static final long serialVersionUID = 7399184964622342223L;

	/**
	 * Constants for runtime permissions used in this package.
	 */
	static final RuntimePermission permissionToSetSecurityManager = new RuntimePermission(
			"setSecurityManager");

	static final RuntimePermission permissionToCreateSecurityManager = new RuntimePermission(
			"createSecurityManager");

	static final RuntimePermission permissionToGetProtectionDomain = new RuntimePermission(
			"getProtectionDomain");

	static final RuntimePermission permissionToGetClassLoader = new RuntimePermission(
			"getClassLoader");

	static final RuntimePermission permissionToCreateClassLoader = new RuntimePermission(
			"createClassLoader");

	static final RuntimePermission permissionToModifyThread = new RuntimePermission(
			"modifyThread");

	static final RuntimePermission permissionToModifyThreadGroup = new RuntimePermission(
			"modifyThreadGroup");

	static final RuntimePermission permissionToExitVM = new RuntimePermission(
			"exitVM");

	static final RuntimePermission permissionToReadFileDescriptor = new RuntimePermission(
			"readFileDescriptor");

	static final RuntimePermission permissionToWriteFileDescriptor = new RuntimePermission(
			"writeFileDescriptor");

	static final RuntimePermission permissionToQueuePrintJob = new RuntimePermission(
			"queuePrintJob");

	static final RuntimePermission permissionToSetFactory = new RuntimePermission(
			"setFactory");

	static final RuntimePermission permissionToSetIO = new RuntimePermission(
			"setIO");

	static final RuntimePermission permissionToStopThread = new RuntimePermission(
			"stopThread");

	/**
	 * Creates an instance of this class with the given name.
	 * 
	 * 
	 * @param permissionName
	 *            String the name of the new permission.
	 */
	public RuntimePermission(String permissionName) {
		super(permissionName);
	}

	/**
	 * Creates an instance of this class with the given name and action list.
	 * The action list is ignored.
	 * 
	 * 
	 * @param name
	 *            String the name of the new permission.
	 * @param actions
	 *            String ignored.
	 */
	public RuntimePermission(String name, String actions) {
		super(name, actions);
	}
}
