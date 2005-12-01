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

package java.lang.ref;

/**
 * PhantomReference objects are used to detect referents which are no longer
 * visible and are eligible to have their storage reclaimed.
 * 
 * @since JDK1.2
 */
public class PhantomReference extends java.lang.ref.Reference {

	/**
	 * Return the referent of the reference object. Phantom reference objects
	 * referents are inaccessible, and so null is returned.
	 * 
	 * 
	 * @return Object Returns null.
	 */
	public Object get() {
		return null;
	}

	/**
	 * Constructs a new instance of this class.
	 * 
	 * 
	 * @param r
	 *            referent to track.
	 * @param q
	 *            queue to register to the reference object with.
	 */
	public PhantomReference(Object r, ReferenceQueue q) {
		super();
	}
}
