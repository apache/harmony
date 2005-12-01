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
 * An Enumeration is used to sequence over a collection of objects.
 * 
 * @see Hashtable
 * @see Properties
 * @see Vector
 */
public interface Enumeration {
	/**
	 * Answers if this Enumeration has more elements.
	 * 
	 * @return true if there are more elements, false otherwise
	 * 
	 * @see #nextElement
	 */
	public boolean hasMoreElements();

	/**
	 * Answers the next element in this Enumeration.
	 * 
	 * @return the next element in this Enumeration
	 * 
	 * @exception NoSuchElementException
	 *                when there are no more elements
	 * 
	 * @see #hasMoreElements
	 */
	public Object nextElement();
}
