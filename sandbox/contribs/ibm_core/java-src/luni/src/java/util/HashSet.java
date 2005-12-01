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


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * HashSet is an implementation of Set. All optional operations are supported,
 * adding and removing. The elements can be any objects.
 */
public class HashSet extends AbstractSet implements Set, Cloneable,
		Serializable {

	static final long serialVersionUID = -5024744406713321676L;

	transient HashMap backingMap;

	/**
	 * Contructs a new empty instance of HashSet.
	 */
	public HashSet() {
		this(new HashMap());
	}

	/**
	 * Constructs a new instance of HashSet with the specified capacity.
	 * 
	 * @param capacity
	 *            the initial capacity of this HashSet
	 */
	public HashSet(int capacity) {
		this(new HashMap(capacity));
	}

	/**
	 * Constructs a new instance of HashSet with the specified capacity and load
	 * factor.
	 * 
	 * @param capacity
	 *            the initial capacity
	 * @param loadFactor
	 *            the initial load factor
	 */
	public HashSet(int capacity, float loadFactor) {
		this(new HashMap(capacity, loadFactor));
	}

	/**
	 * Constructs a new instance of HashSet containing the unique elements in
	 * the specified collection.
	 * 
	 * @param collection
	 *            the collection of elements to add
	 */
	public HashSet(Collection collection) {
		this(new HashMap(collection.size() < 6 ? 11 : collection.size() * 2));
		Iterator it = collection.iterator();
		while (it.hasNext())
			add(it.next());
	}

	HashSet(HashMap backingMap) {
		this.backingMap = backingMap;
	}

	/**
	 * Adds the specified object to this HashSet.
	 * 
	 * @param object
	 *            the object to add
	 * @return true when this HashSet did not already contain the object, false
	 *         otherwise
	 */
	public boolean add(Object object) {
		return backingMap.put(object, this) == null;
	}

	/**
	 * Removes all elements from this HashSet, leaving it empty.
	 * 
	 * @see #isEmpty
	 * @see #size
	 */
	public void clear() {
		backingMap.clear();
	}

	/**
	 * Answers a new HashSet with the same elements and size as this HashSet.
	 * 
	 * @return a shallow copy of this HashSet
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			HashSet clone = (HashSet) super.clone();
			clone.backingMap = (HashMap) backingMap.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Searches this HashSet for the specified object.
	 * 
	 * @param object
	 *            the object to search for
	 * @return true if <code>object</code> is an element of this HashSet,
	 *         false otherwise
	 */
	public boolean contains(Object object) {
		return backingMap.containsKey(object);
	}

	/**
	 * Answers if this HashSet has no elements, a size of zero.
	 * 
	 * @return true if this HashSet has no elements, false otherwise
	 * 
	 * @see #size
	 */
	public boolean isEmpty() {
		return backingMap.isEmpty();
	}

	/**
	 * Answers an Iterator on the elements of this HashSet.
	 * 
	 * @return an Iterator on the elements of this HashSet
	 * 
	 * @see Iterator
	 */
	public Iterator iterator() {
		return backingMap.keySet().iterator();
	}

	/**
	 * Removes an occurrence of the specified object from this HashSet.
	 * 
	 * @param object
	 *            the object to remove
	 * @return true if this HashSet is modified, false otherwise
	 */
	public boolean remove(Object object) {
		return backingMap.remove(object) != null;
	}

	/**
	 * Answers the number of elements in this HashSet.
	 * 
	 * @return the number of elements in this HashSet
	 */
	public int size() {
		return backingMap.size();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeInt(backingMap.elementData.length);
		stream.writeFloat((float) backingMap.loadFactor / 10000);
		stream.writeInt(backingMap.elementCount);
		for (int i = backingMap.elementData.length; --i >= 0;) {
			HashMap.HashMapEntry entry = backingMap.elementData[i];
			while (entry != null) {
				stream.writeObject(entry.key);
				entry = entry.next;
			}
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		int length = stream.readInt();
		float loadFactor = stream.readFloat();
		backingMap = createBackingMap(length, loadFactor);
		int elementCount = stream.readInt();
		for (int i = elementCount; --i >= 0;) {
			Object key = stream.readObject();
			backingMap.put(key, this);
		}
	}

	HashMap createBackingMap(int capacity, float loadFactor) {
		return new HashMap(capacity, loadFactor);
	}
}
