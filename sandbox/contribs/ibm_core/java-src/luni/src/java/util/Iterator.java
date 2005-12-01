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

package java.util;


/**
 * An Iterator is used to sequence over a collection of objects.
 */
public interface Iterator {
	/**
	 * Answers if there are more elements to iterate.
	 * 
	 * @return true if there are more elements, false otherwise
	 * 
	 * @see #next
	 */
	public boolean hasNext();

	/**
	 * Answers the next object in the iteration.
	 * 
	 * @return the next object
	 * 
	 * @exception NoSuchElementException
	 *                when there are no more elements
	 * 
	 * @see #hasNext
	 */
	public Object next();

	/**
	 * Removes the last object returned by <code>next</code> from the
	 * collection.
	 * 
	 * @exception UnsupportedOperationException
	 *                when removing is not supported by the collection being
	 *                iterated
	 * @exception IllegalStateException
	 *                when <code>next</code> has not been called, or
	 *                <code>remove</code> has already been called after the
	 *                last call to <code>next</code>
	 */
	public void remove();
}
