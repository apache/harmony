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
 * Hashtable associates keys with values. Keys and values cannot be null. The
 * size of the Hashtable is the number of key/value pairs it contains. The
 * capacity is the number of key/value pairs the Hashtable can hold. The load
 * factor is a float value which determines how full the Hashtable gets before
 * expanding the capacity. If the load factor of the Hashtable is exceeded, the
 * capacity is doubled.
 * 
 * @see Enumeration
 * @see java.io.Serializable
 * @see java.lang.Object#equals
 * @see java.lang.Object#hashCode
 */

public class Hashtable extends Dictionary implements Map, Cloneable,
		Serializable {

	static final long serialVersionUID = 1421746759512286392L;

	transient int elementCount;

	transient HashtableEntry[] elementData;

	private float loadFactor;

	private int threshold;

	transient int firstSlot = 0;

	transient int lastSlot = -1;

	transient int modCount = 0;

	private static final Enumeration emptyEnumerator = new Hashtable(0)
			.getEmptyEnumerator();

	private static HashtableEntry newEntry(Object key, Object value, int hash) {
		return new HashtableEntry(key, value);
	}

	private static class HashtableEntry extends MapEntry {
		HashtableEntry next;

		HashtableEntry(Object theKey, Object theValue) {
			super(theKey, theValue);
		}

		public Object clone() {
			HashtableEntry entry = (HashtableEntry) super.clone();
			if (next != null)
				entry.next = (HashtableEntry) next.clone();
			return entry;
		}

		public Object setValue(Object object) {
			if (object == null)
				throw new NullPointerException();
			Object result = value;
			value = object;
			return result;
		}

		public int getKeyHash() {
			return key.hashCode();
		}

		public boolean equalsKey(Object aKey, int hash) {
			return key.equals(aKey);
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	private final class HashIterator implements Iterator {
		private int position, expectedModCount;

		private MapEntry.Type type;

		private HashtableEntry lastEntry;

		private int lastPosition;

		private boolean canRemove = false;

		HashIterator(MapEntry.Type value) {
			type = value;
			position = lastSlot;
			expectedModCount = modCount;
		}

		public boolean hasNext() {
			if (lastEntry != null && lastEntry.next != null) {
				return true;
			}
			while (position >= firstSlot) {
				if (elementData[position] == null)
					position--;
				else
					return true;
			}
			return false;
		}

		public Object next() {
			if (expectedModCount == modCount) {
				if (lastEntry != null) {
					lastEntry = lastEntry.next;
				}
				if (lastEntry == null) {
					while (position >= firstSlot
							&& (lastEntry = elementData[position]) == null) {
						position--;
					}
					if (lastEntry != null) {
						lastPosition = position;
						// decrement the position so we don't find the same slot
						// next time
						position--;
					}
				}
				if (lastEntry != null) {
					canRemove = true;
					return type.get(lastEntry);
				}
				throw new NoSuchElementException();
			} else
				throw new ConcurrentModificationException();
		}

		public void remove() {
			if (expectedModCount == modCount) {
				if (canRemove) {
					canRemove = false;
					synchronized (Hashtable.this) {
						boolean removed = false;
						HashtableEntry entry = elementData[lastPosition];
						if (entry == lastEntry) {
							elementData[lastPosition] = entry.next;
							removed = true;
						} else {
							while (entry != null && entry.next != lastEntry) {
								entry = entry.next;
							}
							if (entry != null) {
								entry = lastEntry.next;
								removed = true;
							}
						}
						if (removed) {
							modCount++;
							elementCount--;
							expectedModCount++;
							return;
						}
						// the entry must have been (re)moved outside of the
						// iterator
						// but this condition wasn't caught by the modCount
						// check
						// throw ConcurrentModificationException() outside of
						// synchronized block
					}
				} else
					throw new IllegalStateException();
			}
			throw new ConcurrentModificationException();
		}
	}

	private final class HashEnumerator implements Enumeration {
		boolean key;

		int start;

		HashtableEntry entry;

		HashEnumerator(boolean isKey) {
			key = isKey;
			start = lastSlot + 1;
		}

		public boolean hasMoreElements() {
			if (entry != null)
				return true;
			while (--start >= firstSlot)
				if (elementData[start] != null) {
					entry = elementData[start];
					return true;
				}
			return false;
		}

		public Object nextElement() {
			if (hasMoreElements()) {
				Object result = key ? entry.key : entry.value;
				entry = entry.next;
				return result;
			} else
				throw new NoSuchElementException();
		}
	}

	/**
	 * Constructs a new Hashtable using the default capacity and load factor.
	 */
	public Hashtable() {
		this(11);
	}

	/**
	 * Constructs a new Hashtable using the specified capacity and the default
	 * load factor.
	 * 
	 * @param capacity
	 *            the initial capacity
	 */
	public Hashtable(int capacity) {
		if (capacity >= 0) {
			elementCount = 0;
			elementData = new HashtableEntry[capacity == 0 ? 1 : capacity];
			firstSlot = elementData.length;
			loadFactor = 0.75f;
			computeMaxSize();
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Constructs a new Hashtable using the specified capacity and load factor.
	 * 
	 * @param capacity
	 *            the initial capacity
	 * @param loadFactor
	 *            the initial load factor
	 */
	public Hashtable(int capacity, float loadFactor) {
		if (capacity >= 0 && loadFactor > 0) {
			elementCount = 0;
			firstSlot = capacity;
			elementData = new HashtableEntry[capacity == 0 ? 1 : capacity];
			this.loadFactor = loadFactor;
			computeMaxSize();
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Constructs a new instance of Hashtable containing the mappings from the
	 * specified Map.
	 * 
	 * @param map
	 *            the mappings to add
	 */
	public Hashtable(Map map) {
		this(map.size() < 6 ? 11 : (map.size() * 4 / 3) + 11);
		putAll(map);
	}

	private HashEnumerator getEmptyEnumerator() {
		return new HashEnumerator(false);
	}

	/**
	 * Removes all key/value pairs from this Hashtable, leaving the size zero
	 * and the capacity unchanged.
	 * 
	 * @see #isEmpty
	 * @see #size
	 */
	public synchronized void clear() {
		elementCount = 0;
		Arrays.fill(elementData, null);
		modCount++;
	}

	/**
	 * Answers a new Hashtable with the same key/value pairs, capacity and load
	 * factor.
	 * 
	 * @return a shallow copy of this Hashtable
	 * 
	 * @see java.lang.Cloneable
	 */
	public synchronized Object clone() {
		try {
			Hashtable hashtable = (Hashtable) super.clone();
			hashtable.elementData = (HashtableEntry[]) elementData.clone();
			HashtableEntry entry;
			for (int i = elementData.length; --i >= 0;)
				if ((entry = elementData[i]) != null)
					hashtable.elementData[i] = (HashtableEntry) entry.clone();
			return hashtable;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	private void computeMaxSize() {
		threshold = (int) (elementData.length * loadFactor);
	}

	/**
	 * Answers if this Hashtable contains the specified object as the value of
	 * at least one of the key/value pairs.
	 * 
	 * @param value
	 *            the object to look for as a value in this Hashtable
	 * @return true if object is a value in this Hashtable, false otherwise
	 * 
	 * @see #containsKey
	 * @see java.lang.Object#equals
	 */
	public synchronized boolean contains(Object value) {
		if (value == null) 
			throw new NullPointerException();

			for (int i = elementData.length; --i >= 0;) {
				HashtableEntry entry = elementData[i];
				while (entry != null) {
					if (value.equals(entry.value))
						return true;
					entry = entry.next;
				}
			}
			return false;
	}

	/**
	 * Answers if this Hashtable contains the specified object as a key of one
	 * of the key/value pairs.
	 * 
	 * @param key
	 *            the object to look for as a key in this Hashtable
	 * @return true if object is a key in this Hashtable, false otherwise
	 * 
	 * @see #contains
	 * @see java.lang.Object#equals
	 */
	public synchronized boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	/**
	 * Searches this Hashtable for the specified value.
	 * 
	 * @param value
	 *            the object to search for
	 * @return true if <code>value</code> is a value of this Hashtable, false
	 *         otherwise
	 */
	public boolean containsValue(Object value) {
		return contains(value);
	}

	/**
	 * Answers an Enumeration on the values of this Hashtable. The results of
	 * the Enumeration may be affected if the contents of this Hashtable are
	 * modified.
	 * 
	 * @return an Enumeration of the values of this Hashtable
	 * 
	 * @see #keys
	 * @see #size
	 * @see Enumeration
	 */
	public synchronized Enumeration elements() {
		if (elementCount == 0)
			return emptyEnumerator;
		return new HashEnumerator(false);
	}

	/**
	 * Answers a Set of the mappings contained in this Hashtable. Each element
	 * in the set is a Map.Entry. The set is backed by this Hashtable so changes
	 * to one are relected by the other. The set does not support adding.
	 * 
	 * @return a Set of the mappings
	 */
	public Set entrySet() {
		return new Collections.SynchronizedSet(new AbstractSet() {
			public int size() {
				return elementCount;
			}

			public void clear() {
				Hashtable.this.clear();
			}

			public boolean remove(Object object) {
				if (contains(object)) {
					Hashtable.this.remove(((Map.Entry) object).getKey());
					return true;
				}
				return false;
			}

			public boolean contains(Object object) {
				if (object instanceof Map.Entry) {
					HashtableEntry entry = getEntry(((Map.Entry) object)
							.getKey());
					return object.equals(entry);
				}
				return false;
			}

			public Iterator iterator() {
				return new HashIterator(new MapEntry.Type() {
					public Object get(MapEntry entry) {
						return entry;
					}
				});
			}
		}, this);
	}

	/**
	 * Compares the specified object to this Hashtable and answer if they are
	 * equal. The object must be an instance of Map and contain the same
	 * key/value pairs.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return true if the specified object is equal to this Map, false
	 *         otherwise
	 * 
	 * @see #hashCode
	 */
	public synchronized boolean equals(Object object) {
		if (this == object)
			return true;
		if (object instanceof Map) {
			Map map = (Map) object;
			if (size() != map.size())
				return false;

			Set objectSet = map.entrySet();
			Iterator it = entrySet().iterator();
			while (it.hasNext())
				if (!objectSet.contains(it.next()))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Answers the value associated with the specified key in this Hashtable.
	 * 
	 * @param key
	 *            the key of the value returned
	 * @return the value associated with the specified key, null if the
	 *         specified key does not exist
	 * 
	 * @see #put
	 */
	public synchronized Object get(Object key) {
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % elementData.length;
		HashtableEntry entry = elementData[index];
		while (entry != null) {
			if (entry.equalsKey(key, hash))
				return entry.value;
			entry = entry.next;
		}
		return null;
	}

	HashtableEntry getEntry(Object key) {
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % elementData.length;
		HashtableEntry entry = elementData[index];
		while (entry != null) {
			if (entry.equalsKey(key, hash))
				return entry;
			entry = entry.next;
		}
		return null;
	}

	/**
	 * Answers an integer hash code for the receiver. Objects which are equal
	 * answer the same value for this method.
	 * 
	 * @return the receiver's hash
	 * 
	 * @see #equals
	 */
	public synchronized int hashCode() {
		int result = 0;
		Iterator it = entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			int hash = (key != this ? key.hashCode() : 0)
					^ (value != this ? (value != null ? value.hashCode() : 0)
							: 0);
			result += hash;
		}
		return result;
	}

	/**
	 * Answers if this Hashtable has no key/value pairs, a size of zero.
	 * 
	 * @return true if this Hashtable has no key/value pairs, false otherwise
	 * 
	 * @see #size
	 */
	public synchronized boolean isEmpty() {
		return elementCount == 0;
	}

	/**
	 * Answers an Enumeration on the keys of this Hashtable. The results of the
	 * Enumeration may be affected if the contents of this Hashtable are
	 * modified.
	 * 
	 * @return an Enumeration of the keys of this Hashtable
	 * 
	 * @see #elements
	 * @see #size
	 * @see Enumeration
	 */
	public synchronized Enumeration keys() {
		if (elementCount == 0)
			return emptyEnumerator;
		return new HashEnumerator(true);
	}

	/**
	 * Answers a Set of the keys contained in this Hashtable. The set is backed
	 * by this Hashtable so changes to one are relected by the other. The set
	 * does not support adding.
	 * 
	 * @return a Set of the keys
	 */
	public Set keySet() {
		return new Collections.SynchronizedSet(new AbstractSet() {
			public boolean contains(Object object) {
				return containsKey(object);
			}

			public int size() {
				return elementCount;
			}

			public void clear() {
				Hashtable.this.clear();
			}

			public boolean remove(Object key) {
				if (containsKey(key)) {
					Hashtable.this.remove(key);
					return true;
				}
				return false;
			}

			public Iterator iterator() {
				return new HashIterator(new MapEntry.Type() {
					public Object get(MapEntry entry) {
						return entry.key;
					}
				});
			}
		}, this);
	}

	/**
	 * Associate the specified value with the specified key in this Hashtable.
	 * If the key already exists, the old value is replaced. The key and value
	 * cannot be null.
	 * 
	 * @param key
	 *            the key to add
	 * @param value
	 *            the value to add
	 * @return the old value associated with the specified key, null if the key
	 *         did not exist
	 * 
	 * @see #elements
	 * @see #get
	 * @see #keys
	 * @see java.lang.Object#equals
	 */
	public synchronized Object put(Object key, Object value) {
		if (key != null && value != null) {
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % elementData.length;
			HashtableEntry entry = elementData[index];
			while (entry != null && !entry.equalsKey(key, hash))
				entry = entry.next;
			if (entry == null) {
				modCount++;
				if (++elementCount > threshold) {
					rehash();
					index = (hash & 0x7FFFFFFF) % elementData.length;
				}
				if (index < firstSlot)
					firstSlot = index;
				if (index > lastSlot)
					lastSlot = index;
				entry = newEntry(key, value, hash);
				entry.next = elementData[index];
				elementData[index] = entry;
				return null;
			}
			Object result = entry.value;
			entry.value = value;
			return result;
		} else
			throw new NullPointerException();
	}

	/**
	 * Copies every mapping in the specified Map to this Hashtable.
	 * 
	 * @param map
	 *            the Map to copy mappings from
	 */
	public synchronized void putAll(Map map) {
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Increases the capacity of this Hashtable. This method is sent when the
	 * size of this Hashtable exceeds the load factor.
	 */
	protected void rehash() {
		int length = (elementData.length << 1) + 1;
		if (length == 0)
			length = 1;
		int newFirst = length;
		int newLast = -1;
		HashtableEntry[] newData = new HashtableEntry[length];
		for (int i = lastSlot + 1; --i >= firstSlot;) {
			HashtableEntry entry = elementData[i];
			while (entry != null) {
				int index = (entry.getKeyHash() & 0x7FFFFFFF) % length;
				if (index < newFirst)
					newFirst = index;
				if (index > newLast)
					newLast = index;
				HashtableEntry next = entry.next;
				entry.next = newData[index];
				newData[index] = entry;
				entry = next;
			}
		}
		firstSlot = newFirst;
		lastSlot = newLast;
		elementData = newData;
		computeMaxSize();
	}

	/**
	 * Remove the key/value pair with the specified key from this Hashtable.
	 * 
	 * @param key
	 *            the key to remove
	 * @return the value associated with the specified key, null if the
	 *         specified key did not exist
	 * 
	 * @see #get
	 * @see #put
	 */
	public synchronized Object remove(Object key) {
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % elementData.length;
		HashtableEntry last = null;
		HashtableEntry entry = elementData[index];
		while (entry != null && !entry.equalsKey(key, hash)) {
			last = entry;
			entry = entry.next;
		}
		if (entry != null) {
			modCount++;
			if (last == null)
				elementData[index] = entry.next;
			else
				last.next = entry.next;
			elementCount--;
			Object result = entry.value;
			entry.value = null;
			return result;
		}
		return null;
	}

	/**
	 * Answers the number of key/value pairs in this Hashtable.
	 * 
	 * @return the number of key/value pairs in this Hashtable
	 * 
	 * @see #elements
	 * @see #keys
	 */
	public synchronized int size() {
		return elementCount;
	}

	/**
	 * Answers the string representation of this Hashtable.
	 * 
	 * @return the string representation of this Hashtable
	 */
	public synchronized String toString() {
		if (isEmpty())
			return "{}";

		StringBuffer buffer = new StringBuffer(size() * 28);
		buffer.append('{');
		for (int i = lastSlot; i >= firstSlot; i--) {
			HashtableEntry entry = elementData[i];
			while (entry != null) {
				if (entry.key != this) {
					buffer.append(entry.key);
				} else {
					buffer.append("(this Map)");
				}
				buffer.append('=');
				if (entry.value != this) {
					buffer.append(entry.value);
				} else {
					buffer.append("(this Map)");
				}
				buffer.append(", ");
				entry = entry.next;
			}
		}
		// Remove the last ", "
		if (elementCount > 0)
			buffer.setLength(buffer.length() - 2);
		buffer.append('}');
		return buffer.toString();
	}

	/**
	 * Answers a Collection of the values contained in this Hashtable. The
	 * collection is backed by this Hashtable so changes to one are reflected by
	 * the other. The collection does not support adding.
	 * 
	 * @return a Collection of the values
	 */
	public Collection values() {
		return new Collections.SynchronizedCollection(new AbstractCollection() {
			public boolean contains(Object object) {
				return Hashtable.this.contains(object);
			}

			public int size() {
				return elementCount;
			}

			public void clear() {
				Hashtable.this.clear();
			}

			public Iterator iterator() {
				return new HashIterator(new MapEntry.Type() {
					public Object get(MapEntry entry) {
						return entry.value;
					}
				});
			}
		}, this);
	}

	private synchronized void writeObject(ObjectOutputStream stream)
			throws IOException {
		stream.defaultWriteObject();
		stream.writeInt(elementData.length);
		stream.writeInt(elementCount);
		for (int i = elementData.length; --i >= 0;) {
			HashtableEntry entry = elementData[i];
			while (entry != null) {
				stream.writeObject(entry.key);
				stream.writeObject(entry.value);
				entry = entry.next;
			}
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		int length = stream.readInt();
		elementData = new HashtableEntry[length];
		elementCount = stream.readInt();
		for (int i = elementCount; --i >= 0;) {
			Object key = stream.readObject();
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % length;
			if (index < firstSlot)
				firstSlot = index;
			if (index > lastSlot)
				lastSlot = index;
			HashtableEntry entry = newEntry(key, stream.readObject(), hash);
			entry.next = elementData[index];
			elementData[index] = entry;
		}
	}
}
