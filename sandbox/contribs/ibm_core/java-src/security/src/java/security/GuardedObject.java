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


import java.io.Serializable;

/**
 * GuardedObject controls access to an object, by checking all requests for the
 * object with a Guard.
 * 
 */
public class GuardedObject implements Serializable {
	static final long serialVersionUID = -5240450096227834308L;

	Object object;

	Guard guard;

	/**
	 * Constructs a GuardedObject to protect access to the specified Object
	 * using the specified Guard.
	 * 
	 * @param guardedObject
	 *            the Object to guard
	 * @param theGuard
	 *            the Guard
	 */
	public GuardedObject(Object guardedObject, Guard theGuard) {
		object = guardedObject;
		guard = theGuard;
	}

	/**
	 * Checks whether access should be granted to the object. If access is
	 * granted, this method returns the object. If it is not granted, then a
	 * <code>SecurityException</code> is thrown.
	 * 
	 * 
	 * @return the guarded object
	 * 
	 * @exception java.lang.SecurityException
	 *                If access is not granted to the object
	 */
	public Object getObject() throws SecurityException {
		if (guard != null)
			guard.checkGuard(object);
		return object;
	}
}
