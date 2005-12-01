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

package java.util;


import java.lang.reflect.Array;

/**
 * AbstractCollection is an abstract implementation of the Collection interface.
 * This implemetation does not support adding. A subclass must implement the
 * abstract methods iterator() and size().
 */

public abstract class AbstractCollection implements Collection {

	/**
	 * Constructs a new instance of this AbstractCollection.
	 */
	protected AbstractCollection() {
		// Empty
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Object object) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Adds the objects in the specified Collection to this Collection.
	 * 
	 * @param collection
	 *            the Collection of objects
	 * @return true if this Collection is modified, false otherwise
	 * 
	 * @exception UnsupportedOperationException
	 *                when adding to this Collection is not supported
	 * @exception ClassCastException
	 *                when the class of an object is inappropriate for this
	 *                Collection
	 * @exception IllegalArgumentException
	 *                when an object cannot be added to this Collection
	 */
	public boolean addAll(Collection collection) {
		boolean result = false;
		Iterator it = collection.iterator();
		while (it.hasNext())
			if (add(it.next()))
				result = true;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		Iterator it = iterator();
		while (it.hasNext()) {
			it.next();
			it.remove();
		}
	}

	/**
	 * Searches this Collection for the specified object.
	 * 
	 * @param object
	 *            the object to search for
	 * @return true if <code>object</code> is an element of this Collection,
	 *         false otherwise
	 */
	public boolean contains(Object object) {
		Iterator it = iterator();
		if (object != null) {
			while (it.hasNext())
				if (object.equals(it.next()))
					return true;
		} else {
			while (it.hasNext())
				if (it.next() == null)
					return true;
		}
		return false;
	}

	/**
	 * Searches this Collection for all objects in the specified Collection.
	 * 
	 * @param collection
	 *            the Collection of objects
	 * @return true if all objects in the specified Collection are elements of
	 *         this Collection, false otherwise
	 */
	public boolean containsAll(Collection collection) {
		Iterator it = collection.iterator();
		while (it.hasNext())
			if (!contains(it.next()))
				return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Answers an Iterator on the elements of this Collection. A subclass must
	 * implement the abstract methods iterator() and size().
	 * 
	 * @return an Iterator on the elements of this Collection
	 * 
	 * @see Iterator
	 */
	public abstract Iterator iterator();

	/**
	 * Removes the first occurrence of the specified object from this
	 * Collection.
	 * 
	 * @param object
	 *            the object to remove
	 * @return true if this Collection is modified, false otherwise
	 * 
	 * @exception UnsupportedOperationException
	 *                when removing from this Collection is not supported
	 */
	public boolean remove(Object object) {
		Iterator it = iterator();
		if (object != null) {
			while (it.hasNext()) {
				if (object.equals(it.next())) {
					it.remove();
					return true;
				}
			}
		} else {
			while (it.hasNext()) {
				if (it.next() == null) {
					it.remove();
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Removes all occurrences in this Collection of each object in the
	 * specified Collection.
	 * 
	 * @param collection
	 *            the Collection of objects to remove
	 * @return true if this Collection is modified, false otherwise
	 * 
	 * @exception UnsupportedOperationException
	 *                when removing from this Collection is not supported
	 */
	public boolean removeAll(Collection collection) {
		boolean result = false;
		Iterator it = iterator();
		while (it.hasNext()) {
			if (collection.contains(it.next())) {
				it.remove();
				result = true;
			}
		}
		return result;
	}

	/**
	 * Removes all objects from this Collection that are not contained in the
	 * specified Collection.
	 * 
	 * @param collection
	 *            the Collection of objects to retain
	 * @return true if this Collection is modified, false otherwise
	 * 
	 * @exception UnsupportedOperationException
	 *                when removing from this Collection is not supported
	 */
	public boolean retainAll(Collection collection) {
		boolean result = false;
		Iterator it = iterator();
		while (it.hasNext()) {
			if (!collection.contains(it.next())) {
				it.remove();
				result = true;
			}
		}
		return result;
	}

	/**
	 * Answers the number of elements in this Collection.
	 * 
	 * @return the number of elements in this Collection
	 */
	public abstract int size();

	/**
	 * Answers a new array containing all elements contained in this Collection.
	 * 
	 * @return an array of the elements from this Collection
	 */
	public Object[] toArray() {
		int size = size(), index = 0;
		Iterator it = iterator();
		Object[] array = new Object[size];
		while (index < size)
			array[index++] = it.next();
		return array;
	}

	/**
	 * Answers an array containing all elements contained in this Collection. If
	 * the specified array is large enough to hold the elements, the specified
	 * array is used, otherwise an array of the same type is created. If the
	 * specified array is used and is larger than this Collection, the array
	 * element following the collection elements is set to null.
	 * 
	 * @param contents
	 *            the array
	 * @return an array of the elements from this Collection
	 * 
	 * @exception ArrayStoreException
	 *                when the type of an element in this Collection cannot be
	 *                stored in the type of the specified array
	 */
	public Object[] toArray(Object[] contents) {
		int size = size(), index = 0;
		Iterator it = iterator();
		if (size > contents.length)
			contents = (Object[]) Array.newInstance(contents.getClass()
					.getComponentType(), size);
		while (index < size)
			contents[index++] = it.next();
		if (index < contents.length)
			contents[index] = null;
		return contents;
	}

	/**
	 * Answers the string representation of this Collection.
	 * 
	 * @return the string representation of this Collection
	 */
	public String toString() {
		if (isEmpty())
			return "[]"; //$NON-NLS-1$

		StringBuffer buffer = new StringBuffer(size() * 16);
		buffer.append('[');
		Iterator it = iterator();
		while (it.hasNext()) {
			buffer.append(it.next());
			buffer.append(", "); //$NON-NLS-1$
		}
		// Remove the trailing ", "
		if (buffer.length() > 1)
			buffer.setLength(buffer.length() - 2);
		buffer.append(']');
		return buffer.toString();
	}
}
