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

package java.lang;


/**
 * An InheritableThreadLocal is very similar to a ThreadLocal, with the added
 * functionality that a child Thread inherits all InheritableThreadLocal from
 * its parent Thread upon Thread creation time.
 * 
 * @see java.lang.Thread
 * @see java.lang.ThreadLocal
 */

public class InheritableThreadLocal extends java.lang.ThreadLocal {
	/**
	 * Constructs a new InheritableThreadLocal object
	 */
	public InheritableThreadLocal() {
		super();
	}

	/**
	 * Computes the created Thread's initial value for this
	 * InheritableThreadLocal based on the current value of the same local on
	 * the creator Thread.
	 */
	protected Object childValue(Object parentValue) {
		return parentValue;
	}
}
