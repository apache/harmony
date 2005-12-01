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

package java.util;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * ArrayList is an implemenation of List, backed by an array. All optional
 * operations are supported, adding, removing, and replacing. The elements can
 * be any objects.
 */
public class ArrayList extends AbstractList implements List, Cloneable,
		Serializable, RandomAccess {

	static final long serialVersionUID = 8683452581122892189L;

	transient private int firstIndex, lastIndex;

	transient private Object[] array;

	/**
	 * Contructs a new instance of ArrayList with zero capacity.
	 */
	public ArrayList() {
		this(0);
	}

	/**
	 * Constructs a new instance of ArrayList with the specified capacity.
	 * 
	 * @param capacity
	 *            the initial capacity of this ArrayList
	 */
	public ArrayList(int capacity) {
		firstIndex = lastIndex = 0;
		try {
			array = new Object[capacity];
		} catch (NegativeArraySizeException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Constructs a new instance of ArrayList containing the elements in the
	 * specified collection. The ArrayList will have an initial cacacity which
	 * is 110% of the size of the collection. The order of the elements in this
	 * ArrayList is the order they are returned by the collection iterator.
	 * 
	 * @param collection
	 *            the collection of elements to add
	 */
	public ArrayList(Collection collection) {
		int size = collection.size();
		firstIndex = lastIndex = 0;
		array = new Object[size + (size / 10)];
		addAll(collection);
	}

	/**
	 * Inserts the specified object into this ArrayList at the specified
	 * location. The object is inserted before any previous element at the
	 * specified location. If the location is equal to the size of this
	 * ArrayList, the object is added at the end.
	 * 
	 * @param location
	 *            the index at which to insert
	 * @param object
	 *            the object to add
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public void add(int location, Object object) {
		int size = size();
		if (0 < location && location < size) {
			if (firstIndex == 0 && lastIndex == array.length) {
				growForInsert(location, 1);
			} else if ((location < size / 2 && firstIndex > 0)
					|| lastIndex == array.length) {
				System.arraycopy(array, firstIndex, array, --firstIndex,
						location);
			} else {
				int index = location + firstIndex;
				System.arraycopy(array, index, array, index + 1, size
						- location);
				lastIndex++;
			}
			array[location + firstIndex] = object;
		} else if (location == 0) {
			if (firstIndex == 0)
				growAtFront(1);
			array[--firstIndex] = object;
		} else if (location == size) {
			if (lastIndex == array.length)
				growAtEnd(1);
			array[lastIndex++] = object;
		} else
			throw new IndexOutOfBoundsException();

		modCount++;
	}

	/**
	 * Adds the specified object at the end of this ArrayList.
	 * 
	 * @param object
	 *            the object to add
	 * @return true
	 */
	public boolean add(Object object) {
		if (lastIndex == array.length)
			growAtEnd(1);
		array[lastIndex++] = object;
		modCount++;
		return true;
	}

	/**
	 * Inserts the objects in the specified Collection at the specified location
	 * in this ArrayList. The objects are added in the order they are returned
	 * from the Collection iterator.
	 * 
	 * @param location
	 *            the index at which to insert
	 * @param collection
	 *            the Collection of objects
	 * @return true if this ArrayList is modified, false otherwise
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public boolean addAll(int location, Collection collection) {
		int size = size();
		int growSize = collection.size();
		if (0 < location && location < size) {
			if (array.length - size < growSize) {
				growForInsert(location, growSize);
			} else if ((location < size / 2 && firstIndex > 0)
					|| lastIndex > array.length - growSize) {
				int newFirst = firstIndex - growSize;
				if (newFirst < 0) {
					int index = location + firstIndex;
					System.arraycopy(array, index, array, index - newFirst,
							size - location);
					lastIndex -= newFirst;
					newFirst = 0;
				}
				System.arraycopy(array, firstIndex, array, newFirst, location);
				firstIndex = newFirst;
			} else {
				int index = location + firstIndex;
				System.arraycopy(array, index, array, index + growSize, size
						- location);
				lastIndex += growSize;
			}
		} else if (location == 0) {
			if (firstIndex == 0)
				growAtFront(growSize);
			firstIndex -= growSize;
		} else if (location == size) {
			if (lastIndex > array.length - growSize)
				growAtEnd(growSize);
			lastIndex += growSize;
		} else
			throw new IndexOutOfBoundsException();

		if (growSize > 0) {
			Iterator it = collection.iterator();
			int index = location + firstIndex;
			int end = index + growSize;
			while (index < end)
				array[index++] = it.next();
			modCount++;
			return true;
		}
		return false;
	}

	/**
	 * Adds the objects in the specified Collection to this ArrayList.
	 * 
	 * @param collection
	 *            the Collection of objects
	 * @return true if this ArrayList is modified, false otherwise
	 */
	public boolean addAll(Collection collection) {
		int growSize = collection.size();
		if (growSize > 0) {
			if (lastIndex > array.length - growSize)
				growAtEnd(growSize);
			Iterator it = collection.iterator();
			int end = lastIndex + growSize;
			while (lastIndex < end)
				array[lastIndex++] = it.next();
			modCount++;
			return true;
		}
		return false;
	}

	/**
	 * Removes all elements from this ArrayList, leaving it empty.
	 * 
	 * @see #isEmpty
	 * @see #size
	 */
	public void clear() {
		if (firstIndex != lastIndex) {
			Arrays.fill(array, firstIndex, lastIndex, null);
			firstIndex = lastIndex = 0;
			modCount++;
		}
	}

	/**
	 * Answers a new ArrayList with the same elements, size and capacity as this
	 * ArrayList.
	 * 
	 * @return a shallow copy of this ArrayList
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			ArrayList newList = (ArrayList) super.clone();
			newList.array = (Object[]) array.clone();
			return newList;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Searches this ArrayList for the specified object.
	 * 
	 * @param object
	 *            the object to search for
	 * @return true if <code>object</code> is an element of this ArrayList,
	 *         false otherwise
	 */
	public boolean contains(Object object) {
		if (object != null) {
			for (int i = firstIndex; i < lastIndex; i++)
				if (object.equals(array[i]))
					return true;
		} else {
			for (int i = firstIndex; i < lastIndex; i++)
				if (array[i] == null)
					return true;
		}
		return false;
	}

	/**
	 * Ensures that this ArrayList can hold the specified number of elements
	 * without growing.
	 * 
	 * @param minimumCapacity
	 *            the minimum number of elements that this ArrayList will hold
	 *            before growing
	 */
	public void ensureCapacity(int minimumCapacity) {
		if (array.length < minimumCapacity) {
			if (firstIndex > 0)
				growAtFront(minimumCapacity - array.length);
			else
				growAtEnd(minimumCapacity - array.length);
		}
	}

	/**
	 * Answers the element at the specified location in this ArrayList.
	 * 
	 * @param location
	 *            the index of the element to return
	 * @return the element at the specified index
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object get(int location) {
		if (0 <= location && location < size())
			return array[firstIndex + location];
		throw new IndexOutOfBoundsException();
	}

	private void growAtEnd(int required) {
		int size = size();
		if (firstIndex >= required - (array.length - lastIndex)) {
			int newLast = lastIndex - firstIndex;
			if (size > 0) {
				System.arraycopy(array, firstIndex, array, 0, size);
				int start = newLast < firstIndex ? firstIndex : newLast;
				Arrays.fill(array, start, array.length, null);
			}
			firstIndex = 0;
			lastIndex = newLast;
		} else {
			int increment = size / 2;
			if (required > increment)
				increment = required;
			if (increment < 12)
				increment = 12;
			Object[] newArray = new Object[size + increment];
			if (size > 0)
				System.arraycopy(array, firstIndex, newArray, firstIndex, size);
			array = newArray;
		}
	}

	private void growAtFront(int required) {
		int size = size();
		if (array.length - lastIndex >= required) {
			int newFirst = array.length - lastIndex;
			if (size > 0) {
				System.arraycopy(array, firstIndex, array, newFirst, size);
				int length = firstIndex + size > newFirst ? newFirst
						: firstIndex + size;
				Arrays.fill(array, firstIndex, length, null);
			}
			firstIndex = newFirst;
			lastIndex = array.length;
		} else {
			int increment = size / 2;
			if (required > increment)
				increment = required;
			if (increment < 12)
				increment = 12;
			Object[] newArray = new Object[size + increment];
			if (size > 0)
				System.arraycopy(array, firstIndex, newArray, newArray.length
						- lastIndex, size);
			firstIndex = newArray.length - lastIndex;
			lastIndex = newArray.length;
			array = newArray;
		}
	}

	private void growForInsert(int location, int required) {
		int size = size(), increment = size / 2;
		if (required > increment)
			increment = required;
		if (increment < 12)
			increment = 12;
		Object[] newArray = new Object[size + increment];
		if (location < size / 2) {
			int newFirst = newArray.length - (size + required);
			System.arraycopy(array, location, newArray, location + increment,
					size - location);
			System.arraycopy(array, firstIndex, newArray, newFirst, location);
			firstIndex = newFirst;
			lastIndex = newArray.length;
		} else {
			System.arraycopy(array, firstIndex, newArray, 0, location);
			System.arraycopy(array, location, newArray, location + required,
					size - location);
			firstIndex = 0;
			lastIndex += required;
		}
		array = newArray;
	}

	/**
	 * Searches this ArrayList for the specified object and returns the index of
	 * the first occurrence.
	 * 
	 * @param object
	 *            the object to search for
	 * @return the index of the first occurrence of the object
	 */
	public int indexOf(Object object) {
		if (object != null) {
			for (int i = firstIndex; i < lastIndex; i++)
				if (object.equals(array[i]))
					return i - firstIndex;
		} else {
			for (int i = firstIndex; i < lastIndex; i++)
				if (array[i] == null)
					return i - firstIndex;
		}
		return -1;
	}

	/**
	 * Answers if this ArrayList has no elements, a size of zero.
	 * 
	 * @return true if this ArrayList has no elements, false otherwise
	 * 
	 * @see #size
	 */
	public boolean isEmpty() {
		return lastIndex == firstIndex;
	}

	/**
	 * Searches this ArrayList for the specified object and returns the index of
	 * the last occurrence.
	 * 
	 * @param object
	 *            the object to search for
	 * @return the index of the last occurrence of the object
	 */
	public int lastIndexOf(Object object) {
		if (object != null) {
			for (int i = lastIndex - 1; i >= firstIndex; i--)
				if (object.equals(array[i]))
					return i - firstIndex;
		} else {
			for (int i = lastIndex - 1; i >= firstIndex; i--)
				if (array[i] == null)
					return i - firstIndex;
		}
		return -1;
	}

	/**
	 * Removes the object at the specified location from this ArrayList.
	 * 
	 * @param location
	 *            the index of the object to remove
	 * @return the removed object
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object remove(int location) {
		Object result;
		int size = size();
		if (0 <= location && location < size) {
			if (location == size - 1) {
				result = array[--lastIndex];
				array[lastIndex] = null;
			} else if (location == 0) {
				result = array[firstIndex];
				array[firstIndex++] = null;
			} else {
				int elementIndex = firstIndex + location;
				result = array[elementIndex];
				if (location < size / 2) {
					System.arraycopy(array, firstIndex, array, firstIndex + 1,
							location);
					array[firstIndex++] = null;
				} else {
					System.arraycopy(array, elementIndex + 1, array,
							elementIndex, size - location - 1);
					array[--lastIndex] = null;
				}
			}
		} else
			throw new IndexOutOfBoundsException();

		modCount++;
		return result;
	}

	/**
	 * Removes the objects in the specified range from the start to the end, but
	 * not including the end index.
	 * 
	 * @param start
	 *            the index at which to start removing
	 * @param end
	 *            the index one past the end of the range to remove
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>start < 0, start > end</code> or
	 *                <code>end > size()</code>
	 */
	protected void removeRange(int start, int end) {
		if (start >= 0 && start <= end && end <= size()) {
			if (start == end)
				return;
			int size = size();
			if (end == size) {
				Arrays.fill(array, firstIndex + start, lastIndex, null);
				lastIndex = firstIndex + start;
			} else if (start == 0) {
				Arrays.fill(array, firstIndex, firstIndex + end, null);
				firstIndex += end;
			} else {
				System.arraycopy(array, firstIndex + end, array, firstIndex
						+ start, size - end);
				int newLast = lastIndex + start - end;
				Arrays.fill(array, newLast, lastIndex, null);
				lastIndex = newLast;
			}
			modCount++;
		} else
			throw new IndexOutOfBoundsException();
	}

	/**
	 * Replaces the element at the specified location in this ArrayList with the
	 * specified object.
	 * 
	 * @param location
	 *            the index at which to put the specified object
	 * @param object
	 *            the object to add
	 * @return the previous element at the index
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object set(int location, Object object) {
		if (0 <= location && location < size()) {
			Object result = array[firstIndex + location];
			array[firstIndex + location] = object;
			return result;
		}
		throw new IndexOutOfBoundsException();
	}

	/**
	 * Answers the number of elements in this ArrayList.
	 * 
	 * @return the number of elements in this ArrayList
	 */
	public int size() {
		return lastIndex - firstIndex;
	}

	/**
	 * Answers a new array containing all elements contained in this ArrayList.
	 * 
	 * @return an array of the elements from this ArrayList
	 */
	public Object[] toArray() {
		int size = size();
		Object[] result = new Object[size];
		System.arraycopy(array, firstIndex, result, 0, size);
		return result;
	}

	/**
	 * Answers an array containing all elements contained in this ArrayList. If
	 * the specified array is large enough to hold the elements, the specified
	 * array is used, otherwise an array of the same type is created. If the
	 * specified array is used and is larger than this ArrayList, the array
	 * element following the collection elements is set to null.
	 * 
	 * @param contents
	 *            the array
	 * @return an array of the elements from this ArrayList
	 * 
	 * @exception ArrayStoreException
	 *                when the type of an element in this ArrayList cannot be
	 *                stored in the type of the specified array
	 */
	public Object[] toArray(Object[] contents) {
		int size = size();
		if (size > contents.length)
			contents = (Object[]) Array.newInstance(contents.getClass()
					.getComponentType(), size);
		System.arraycopy(array, firstIndex, contents, 0, size);
		if (size < contents.length)
			contents[size] = null;
		return contents;
	}

	/**
	 * Sets the capacity of this ArrayList to be the same as the size.
	 * 
	 * @see #size
	 */
	public void trimToSize() {
		int size = size();
		Object[] newArray = new Object[size];
		System.arraycopy(array, firstIndex, newArray, 0, size);
		array = newArray;
		firstIndex = 0;
		lastIndex = array.length;
	}

	private static final ObjectStreamField[] serialPersistentFields = { new ObjectStreamField(
			"size", Integer.TYPE) }; //$NON-NLS-1$

	private void writeObject(ObjectOutputStream stream) throws IOException {
		ObjectOutputStream.PutField fields = stream.putFields();
		fields.put("size", size()); //$NON-NLS-1$
		stream.writeFields();
		stream.writeInt(array.length);
		Iterator it = iterator();
		while (it.hasNext())
			stream.writeObject(it.next());
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField fields = stream.readFields();
		lastIndex = fields.get("size", 0); //$NON-NLS-1$
		array = new Object[stream.readInt()];
		for (int i = 0; i < lastIndex; i++)
			array[i] = stream.readObject();
	}
}
