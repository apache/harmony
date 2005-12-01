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
 * AbstractSequentialList is an abstract implementation of the List interface.
 * This implementation does not support adding. A subclass must implement the
 * abstract method listIterator().
 */
public abstract class AbstractSequentialList extends AbstractList {

	/**
	 * Constructs a new instance of this AbstractSequentialList.
	 */
	protected AbstractSequentialList() {
		/*empty*/
	}

	/**
	 * Inserts the specified object into this List at the specified location.
	 * The object is inserted before any previous element at the specified
	 * location. If the location is equal to the size of this List, the object
	 * is added at the end.
	 * 
	 * @param location
	 *            the index at which to insert
	 * @param object
	 *            the object to add
	 * 
	 * @exception UnsupportedOperationException
	 *                when adding to this List is not supported
	 * @exception ClassCastException
	 *                when the class of the object is inappropriate for this
	 *                List
	 * @exception IllegalArgumentException
	 *                when the object cannot be added to this List
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 * @exception NullPointerException
	 *                when the object is null and this List does not support
	 *                null elements
	 */
	public void add(int location, Object object) {
		listIterator(location).add(object);
	}

	/**
	 * Inserts the objects in the specified Collection at the specified location
	 * in this List. The objects are added in the order they are returned from
	 * the Collection iterator.
	 * 
	 * @param location
	 *            the index at which to insert
	 * @param collection
	 *            the Collection of objects
	 * @return true if this List is modified, false otherwise
	 * 
	 * @exception UnsupportedOperationException
	 *                when adding to this List is not supported
	 * @exception ClassCastException
	 *                when the class of an object is inappropriate for this List
	 * @exception IllegalArgumentException
	 *                when an object cannot be added to this List
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public boolean addAll(int location, Collection collection) {
		ListIterator it = listIterator(location);
		Iterator colIt = collection.iterator();
		int next = it.nextIndex();
		while (colIt.hasNext()) {
			it.add(colIt.next());
			it.previous();
		}
		return next != it.nextIndex();
	}

	/**
	 * Answers the element at the specified location in this List.
	 * 
	 * @param location
	 *            the index of the element to return
	 * @return the element at the specified location
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object get(int location) {
		try {
			return listIterator(location).next();
		} catch (NoSuchElementException e) {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Answers an Iterator on the elements of this List. The elements are
	 * iterated in the same order that they occur in the List.
	 * 
	 * @return an Iterator on the elements of this List
	 * 
	 * @see Iterator
	 */
	public Iterator iterator() {
		return listIterator(0);
	}

	/**
	 * Answers a ListIterator on the elements of this List. The elements are
	 * iterated in the same order that they occur in the List. The iteration
	 * starts at the specified location.
	 * 
	 * @param location
	 *            the index at which to start the iteration
	 * @return a ListIterator on the elements of this List
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 * 
	 * @see ListIterator
	 */
	public abstract ListIterator listIterator(int location);

	/**
	 * Removes the object at the specified location from this List.
	 * 
	 * @param location
	 *            the index of the object to remove
	 * @return the removed object
	 * 
	 * @exception UnsupportedOperationException
	 *                when removing from this List is not supported
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object remove(int location) {
		try {
			ListIterator it = listIterator(location);
			Object result = it.next();
			it.remove();
			return result;
		} catch (NoSuchElementException e) {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Replaces the element at the specified location in this List with the
	 * specified object.
	 * 
	 * @param location
	 *            the index at which to put the specified object
	 * @param object
	 *            the object to add
	 * @return the previous element at the index
	 * 
	 * @exception UnsupportedOperationException
	 *                when replacing elements in this List is not supported
	 * @exception ClassCastException
	 *                when the class of an object is inappropriate for this List
	 * @exception IllegalArgumentException
	 *                when an object cannot be added to this List
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object set(int location, Object object) {
		ListIterator it = listIterator(location);
		Object result = it.next();
		it.set(object);
		return result;
	}
}
