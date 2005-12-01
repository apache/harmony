/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
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
 * IdentityHashMap
 * 
 * This is a variant on HashMap which tests equality by reference instead of by
 * value. Basically, keys and values are compared for equality by checking if
 * their references are equal rather than by calling the "equals" function.
 * 
 * IdentityHashMap uses open addressing (linear probing in particular) for
 * collision resolution. This is different from HashMap which uses Chaining.
 * 
 * Like HashMap, IdentityHashMap is not thread safe, so access by multiple
 * threads must be synchronized by an external mechanism such as
 * Collections.synchronizedMap.
 */
public class IdentityHashMap extends AbstractMap implements Map, Serializable,
		Cloneable {

	static final long serialVersionUID = 8188218128353913216L;

	/*
	 * The internal data structure to hold key value pairs This array holds keys
	 * and values alternatingly.
	 */
	transient Object[] elementData;

	/* Actual number of key-value pairs. */
	int size;

	/*
	 * maximum number of elements that can be put in this map before having to
	 * rehash.
	 */
	transient int threshold;

	/*
	 * default treshold value that an IdentityHashMap created using the default
	 * constructor would have.
	 */
	private static final int DEFAULT_MAX_SIZE = 21;

	/* Default load factor of 0.75; */
	private static final int loadFactor = 7500;

	/*
	 * modification count, to keep track of structural modificiations between
	 * the Identityhashmap and the iterator
	 */
	transient int modCount = 0;

	static class IdentityHashMapEntry extends MapEntry {
		IdentityHashMapEntry(Object theKey, Object theValue) {
			super(theKey, theValue);
		}

		public Object clone() {
			return (IdentityHashMapEntry) super.clone();
		}

		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (object instanceof Map.Entry) {
				Map.Entry entry = (Map.Entry) object;
				return (key == entry.getKey()) && (value == entry.getValue());
			}
			return false;
		}

		public int hashCode() {
			return System.identityHashCode(key)
					^ System.identityHashCode(value);
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	static class IdentityHashMapIterator implements Iterator {
		private int position = 0; // the current position

		// the position of the entry that was last returned from next()
		private int lastPosition = 0;

		IdentityHashMap associatedMap;

		int expectedModCount;

		MapEntry.Type type;

		boolean canRemove = false;

		IdentityHashMapIterator(MapEntry.Type value, IdentityHashMap hm) {
			associatedMap = hm;
			type = value;
			expectedModCount = hm.modCount;
		}

		public boolean hasNext() {
			while (position < associatedMap.elementData.length) {
				// check if position is an empty spot, not just an entry with
				// null key
				if (associatedMap.elementData[position] == null
						&& associatedMap.elementData[position + 1] == null)
					position = position + 2;
				else
					return true;
			}
			return false;
		}

		void checkConcurrentMod() throws ConcurrentModificationException {
			if (expectedModCount != associatedMap.modCount)
				throw new ConcurrentModificationException();
		}

		public Object next() {
			checkConcurrentMod();
			if (!hasNext())
				throw new NoSuchElementException();

			IdentityHashMapEntry result = new IdentityHashMapEntry(
					associatedMap.elementData[position],
					associatedMap.elementData[position + 1]);
			lastPosition = position;
			position += 2;

			canRemove = true;
			return type.get(result);
		}

		public void remove() {
			checkConcurrentMod();
			if (!canRemove)
				throw new IllegalStateException();

			canRemove = false;
			associatedMap.remove(associatedMap.elementData[lastPosition]);
			expectedModCount++;
		}
	}

	static class IdentityHashMapEntrySet extends AbstractSet {
		private IdentityHashMap associatedMap;

		public IdentityHashMapEntrySet(IdentityHashMap hm) {
			associatedMap = hm;
		}

		IdentityHashMap hashMap() {
			return associatedMap;
		}

		public int size() {
			return associatedMap.size;
		}

		public void clear() {
			associatedMap.clear();
		}

		public boolean remove(Object object) {
			if (contains(object)) {
				associatedMap.remove(((Map.Entry) object).getKey());
				return true;
			}
			return false;
		}

		public boolean contains(Object object) {
			if (object instanceof Map.Entry) {
				IdentityHashMapEntry entry = associatedMap
						.getEntry(((Map.Entry) object).getKey());
				// we must call equals on the entry obtained from "this"
				return entry != null && entry.equals(object);
			}
			return false;
		}

		public Iterator iterator() {
			return new IdentityHashMapIterator(new MapEntry.Type() {
				public Object get(MapEntry entry) {
					return entry;
				}
			}, associatedMap);
		}
	}

	/**
	 * Create an IdentityHashMap with default maximum size
	 */
	public IdentityHashMap() {
		this(DEFAULT_MAX_SIZE);
	}

	/**
	 * Create an IdentityHashMap with the given maximum size parameter
	 * 
	 * @param maxSize
	 *            The estimated maximum number of entries that will be put in
	 *            this map.
	 */
	public IdentityHashMap(int maxSize) {
		if (maxSize >= 0) {
			this.size = 0;
			threshold = getThreshold(maxSize);
			elementData = newElementArray(computeElementArraySize());
		} else
			throw new IllegalArgumentException();
	}

	private int getThreshold(int maxSize) {
		// assign the treshold to maxsize initially, this will change to a
		// higher value if rehashing occurs.
		return maxSize > 3 ? maxSize : 3;
	}

	private int computeElementArraySize() {
		return (int) (((long) threshold * 10000) / loadFactor) * 2;
	}

	/**
	 * Create a new element array
	 * 
	 * @param s
	 *            the number of elements
	 * @return Reference to the element array
	 */
	private Object[] newElementArray(int s) {
		return new Object[s];
	}

	/**
	 * Create an IdentityHashMap using the given Map as initial values.
	 * 
	 * @param map
	 *            A map of (key,value) pairs to copy into the IdentityHashMap
	 */
	public IdentityHashMap(Map map) {
		this(map.size() < 6 ? 11 : map.size() * 2);
		putAll(map);
	}

	/**
	 * Removes all elements from this Map, leaving it empty.
	 * 
	 * @exception UnsupportedOperationException
	 *                when removing from this Map is not supported
	 * 
	 * @see #isEmpty
	 * @see #size
	 */
	public void clear() {
		size = 0;
		for (int i = 0; i < elementData.length; i++) {
			elementData[i] = null;
		}
	}

	/**
	 * Searches this Map for the specified key.
	 * 
	 * @param key
	 *            the object to search for
	 * @return true if <code>key</code> is a key of this Map, false otherwise
	 */
	public boolean containsKey(Object key) {
		int index = findIndex(key, elementData);
		if (key != null)
			return elementData[index] == key;
		// if key is null, we have to make sure
		// what we found is not one of the empty spots
		return (elementData[index] == null && elementData[index + 1] != null);

	}

	/**
	 * Searches this Map for the specified value.
	 * 
	 * 
	 * @param value
	 *            the object to search for
	 * @return true if <code>value</code> is a value of this Map, false
	 *         otherwise
	 */
	public boolean containsValue(Object value) {
		for (int i = 1; i < elementData.length; i = i + 2) {
			if (elementData[i] == value) {
				if (value != null)
					return true;
				// if value is null, we have to make sure what we found is
				// not one of the empty spots
				if (elementData[i - 1] != null)
					return true;
			}
		}
		return false;
	}

	/**
	 * Answers the value of the mapping with the specified key.
	 * 
	 * @param key
	 *            the key
	 * @return the value of the mapping with the specified key
	 */
	public Object get(Object key) {
		int index = findIndex(key, elementData);

		if (elementData[index] == key)
			return elementData[index + 1];
		
		return null;
	}

	private IdentityHashMapEntry getEntry(Object key) {
		int index = findIndex(key, elementData);
		if (elementData[index] == key)
			return new IdentityHashMapEntry(key, elementData[index + 1]);
		
		return null;
	}

	/**
	 * Returns the index where the key is found at, or the index of the next
	 * empty spot if the key is not found in this table.
	 */
	private int findIndex(Object key, Object[] array) {
		int length = array.length;
		int index = getModuloHash(key, length);
		int last = (index + length - 2) % length;
		while (index != last) {
			if (array[index] == key
					|| (array[index] == null && array[index + 1] == null))
				break;
			index = (index + 2) % length;
		}
		return index;
	}

	private int getModuloHash(Object key, int length) {
		return ((System.identityHashCode(key) & 0x7FFFFFFF) % (length / 2)) * 2;
	}

	/**
	 * Maps the specified key to the specified value.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @return the value of any previous mapping with the specified key or null
	 *         if there was no mapping
	 */
	public Object put(Object key, Object value) {
		int index = findIndex(key, elementData);

		// if the key doesn't exist in the table
		if (elementData[index] != key
				|| (key == null && elementData[index + 1] == null)) {
			// if key is null, and value is null
			// this is one of the empty spots, there is not entry for key "null"
			// in the table
			modCount++;
			if (++size > threshold) {
				rehash();
				index = findIndex(key, elementData);
			}

			// insert the key and assign the value to null initially
			elementData[index] = key;
			elementData[index + 1] = null;
		}

		// insert value to where it needs to go, return the old value
		Object result = elementData[index + 1];
		elementData[index + 1] = value;
		return result;
	}

	private void rehash() {
		int newlength = elementData.length << 1;
		if (newlength == 0)
			newlength = 1;
		Object[] newData = newElementArray(newlength);
		for (int i = 0; i < elementData.length; i = i + 2) {
			Object key = elementData[i];
			if (key != null || (key == null && elementData[i + 1] != null)) { // if
				// not
				// empty
				int index = findIndex(key, newData);
				newData[index] = key;
				newData[index + 1] = elementData[i + 1];
			}
		}
		elementData = newData;
		computeMaxSize();
	}

	private void computeMaxSize() {
		threshold = (int) ((long) (elementData.length / 2) * loadFactor / 10000);
	}

	/**
	 * Removes a mapping with the specified key from this IdentityHashMap.
	 * 
	 * @param key
	 *            the key of the mapping to remove
	 * @return the value of the removed mapping, or null if key is not a key in
	 *         this Map
	 */
	public Object remove(Object key) {
		boolean hashedOk;

		int index, next, hash;
		Object result, object;
		index = next = findIndex(key, elementData);

		// store the value for this key
		result = elementData[index + 1];
		if (result == null && key == null)
			return null;

		// shift the following elements up if needed
		// until we reach an empty spot
		int length = elementData.length;
		while (true) {
			next = (next + 2) % length;
			object = elementData[next];
			if (object == null && elementData[next + 1] == null)
				break;

			hash = getModuloHash(object, length);
			hashedOk = hash > index;
			if (next < index) {
				hashedOk = hashedOk || (hash <= next);
			} else {
				hashedOk = hashedOk && (hash <= next);
			}
			if (!hashedOk) {
				elementData[index] = object;
				elementData[index + 1] = elementData[next];
				index = next;
			}
		}

		size--;
		modCount++;

		// clear both the key and the value
		elementData[index] = null;
		elementData[index + 1] = null;

		return result;
	}

	/**
	 * Answers a Set of the mappings contained in this IdentityHashMap. Each
	 * element in the set is a Map.Entry. The set is backed by this Map so
	 * changes to one are relected by the other. The set does not support
	 * adding.
	 * 
	 * @return a Set of the mappings
	 */
	public Set entrySet() {
		return new IdentityHashMapEntrySet(this);
	}

	/**
	 * Answers a Set of the keys contained in this IdentityHashMap. The set is
	 * backed by this IdentityHashMap so changes to one are relected by the
	 * other. The set does not support adding.
	 * 
	 * @return a Set of the keys
	 */
	public Set keySet() {
		if (keySet == null) {
			keySet = new AbstractSet() {
				public boolean contains(Object object) {
					return containsKey(object);
				}

				public int size() {
					return IdentityHashMap.this.size();
				}

				public void clear() {
					IdentityHashMap.this.clear();
				}

				public boolean remove(Object key) {
					if (containsKey(key)) {
						IdentityHashMap.this.remove(key);
						return true;
					}
					return false;
				}

				public Iterator iterator() {
					return new IdentityHashMapIterator(new MapEntry.Type() {
						public Object get(MapEntry entry) {
							return entry.key;
						}
					}, IdentityHashMap.this);
				}
			};
		}
		return keySet;
	}

	/**
	 * Answers a Collection of the values contained in this IdentityHashMap. The
	 * collection is backed by this IdentityHashMap so changes to one are
	 * relected by the other. The collection does not support adding.
	 * 
	 * @return a Collection of the values
	 */
	public Collection values() {
		if (valuesCollection == null) {
			valuesCollection = new AbstractCollection() {
				public boolean contains(Object object) {
					return containsValue(object);
				}

				public int size() {
					return IdentityHashMap.this.size();
				}

				public void clear() {
					IdentityHashMap.this.clear();
				}

				public Iterator iterator() {
					return new IdentityHashMapIterator(new MapEntry.Type() {
						public Object get(MapEntry entry) {
							return entry.value;
						}
					}, IdentityHashMap.this);
				}
			};
		}
		return valuesCollection;
	}

	/**
	 * Compares this map with other objects. This map is equal to another map is
	 * it represents the same set of mappings. With this map, two mappings are
	 * the same if both the key and the value are equal by reference.
	 * 
	 * When compared with a map that is not an IdentityHashMap, the equals
	 * method is not necessarily symmetric (a.equals(b) implies b.equals(a)) nor
	 * transitive (a.equals(b) and b.equals(c) implies a.equals(c)).
	 * 
	 * @return whether the argument object is equal to this object
	 */
	public boolean equals(Object object) {
		// we need to override the equals method in AbstractMap because
		// AbstractMap.equals will call ((Map) object).entrySet().contains() to
		// determine equality of the entries, so it will defer to the argument
		// for comparison, meaning that reference-based comparison will not take
		// place. We must ensure that all comparison is implemented by methods
		// in this class (or in one of our inner classes) for reference-based
		// comparison to take place.
		if (this == object)
			return true;
		if (object instanceof Map) {
			Map map = (Map) object;
			if (size() != map.size())
				return false;

			Set set = entrySet();
			// ensure we use the equals method of the set created by "this"
			return set.equals(map.entrySet());
		}
		return false;
	}

	/**
	 * Answers a new IdentityHashMap with the same mappings and size as this
	 * one.
	 * 
	 * @return a shallow copy of this IdentityHashMap
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		try {
			return (IdentityHashMap) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Answers if this IdentityHashMap has no elements, a size of zero.
	 * 
	 * @return true if this IdentityHashMap has no elements, false otherwise
	 * 
	 * @see #size
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Answers the number of mappings in this IdentityHashMap.
	 * 
	 * @return the number of mappings in this IdentityHashMap
	 */
	public int size() {
		return size;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeInt(size);
		Iterator iterator = entrySet().iterator();
		while (iterator.hasNext()) {
			MapEntry entry = (MapEntry) iterator.next();
			stream.writeObject(entry.key);
			stream.writeObject(entry.value);
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		int savedSize = stream.readInt();
		threshold = getThreshold(DEFAULT_MAX_SIZE);
		elementData = newElementArray(computeElementArraySize());
		for (int i = savedSize; --i >= 0;) {
			Object key = stream.readObject();
			put(key, stream.readObject());
		}
	}
}
