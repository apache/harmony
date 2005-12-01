/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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
 * A ThreadLocal is a variable that has a per-thread value. Different Threads
 * may reference the same ThreadLocal object, but the values they observe will
 * not be <code>==</code>. This provides Thread local storage.
 * 
 * @see java.lang.Thread
 */
public class ThreadLocal {
	/**
	 * Constructs a new ThreadLocal object
	 */
	public ThreadLocal() {
		super();
	}

	/**
	 * Return the value of this variable under
	 * <code>Thread.currentThread()</code>
	 */
	public Object get() {
		return Thread.currentThread().getThreadLocal(this);
	}

	/**
	 * Return the initial value of this variable under
	 * <code>Thread.currentThread()</code>
	 */
	protected Object initialValue() {
		return null;
	}

	/**
	 * Set the value of this variable under <code>Thread.currentThread()</code>
	 */
	public void set(Object value) {
		Thread.currentThread().setThreadLocal(this, value);
	}
}
