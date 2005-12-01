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


/**
 * This interface is implemented by objects which wish to control access to
 * other objects.
 * 
 */
public interface Guard {
	/**
	 * Checks whether access should be granted to the argument. If access is
	 * granted, this method simply returns. If it is not granted, then a
	 * <code>SecurityException</code> should be thrown.
	 * 
	 * 
	 * @param guardedObject
	 *            java.lang.Object an object to check for accessibility
	 * @exception java.lang.SecurityException
	 *                If access is not granted to the object
	 */
	void checkGuard(Object guardedObject) throws SecurityException;
}
