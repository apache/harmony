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


import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * WeakHashMap is an implementation of Map with keys which are WeakReferences.
 * The key/value mapping is removed when the key is no longer referenced. All
 * optional operations are supported, adding and removing. Keys and values can
 * be any objects.
 */
public class WeakHashMap extends AbstractMap implements Map {
	
	private ReferenceQueue referenceQueue;

	int elementCount;

	WeakHashMapEntry[] elementData;

	private int loadFactor;

	private int threshold;

	transient int modCount = 0;

	private static final int DEFAULT_SIZE = 16;

	private static final class WeakHashMapEntry extends WeakReference {
		int hash;

		boolean isNull;

		Object value;

		WeakHashMapEntry next;

		private WeakHashMapEntry(Object key, Object object, ReferenceQueue queue) {
			super(key, queue);
			isNull = key == null;
			hash = isNull ? 0 : key.hashCode();
			value = object;
		}
	}

	private static final class KeyEntry implements Map.Entry {
		private Object key;

		private WeakHashMapEntry entry;

		interface Type {
			Object get(KeyEntry entry);
		}

		KeyEntry(Object key, WeakHashMapEntry entry) {
			this.key = key;
			this.entry = entry;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return entry.value;
		}

		public Object setValue(Object object) {
			Object result = entry.value;
			entry.value = object;
			return result;
		}
	}

	class HashIterator implements Iterator {
		private int position = 0, expectedModCount;

		private boolean canRemove = false;

		private WeakHashMapEntry entry, lastEntry;

		private KeyEntry next;

		KeyEntry.Type type;

		HashIterator(KeyEntry.Type type, WeakHashMap map) {
			this.type = type;
			expectedModCount = modCount;
		}

		public boolean hasNext() {
			if (next != null)
				return true;
			while (true) {
				if (entry == null) {
					lastEntry = null;
					while (position < elementData.length)
						if ((entry = elementData[position++]) != null)
							break;
					if (entry == null)
						return false;
				}
				Object key = entry.get();
				if (key != null || entry.isNull) {
					next = new KeyEntry(key, entry);
					return true;
				}
				entry = entry.next;
			}
		}

		public Object next() {
			if (expectedModCount == modCount) {
				if (hasNext()) {
					if (lastEntry == null)
						lastEntry = entry;
					else if (lastEntry.next != entry)
						lastEntry = lastEntry.next;
					entry = entry.next;
					canRemove = true;
					KeyEntry result = next;
					next = null;
					return type.get(result);
				} else
					throw new NoSuchElementException();
			} else
				throw new ConcurrentModificationException();
		}

		public void remove() {
			if (expectedModCount == modCount) {
				if (canRemove) {
					canRemove = false;
					modCount++;
					if (lastEntry.next == entry) {
						while (elementData[--position] == null) {
							// do nothing
						}
						elementData[position] = elementData[position].next;
						entry = null;
					} else
						lastEntry.next = entry;
					elementCount--;
					expectedModCount++;
					poll();
				} else
					throw new IllegalStateException();
			} else
				throw new ConcurrentModificationException();
		}
	}

	/**
	 * Contructs a new empty instance of WeakHashMap.
	 */
	public WeakHashMap() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Constructs a new instance of WeakHashMap with the specified capacity.
	 * 
	 * @param capacity
	 *            the initial capacity of this WeakHashMap
	 * 
	 * @exception IllegalArgumentException
	 *                when the capacity is less than zero
	 */
	public WeakHashMap(int capacity) {
		if (capacity >= 0) {
			elementCount = 0;
			elementData = new WeakHashMapEntry[capacity == 0 ? 1 : capacity];
			loadFactor = 7500; // Default load factor of 0.75
			computeMaxSize();
			referenceQueue = new ReferenceQueue();
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Constructs a new instance of WeakHashMap with the specified capacity and
	 * load factor.
	 * 
	 * @param capacity
	 *            the initial capacity
	 * @param loadFactor
	 *            the initial load factor
	 * 
	 * @exception IllegalArgumentException
	 *                when the capacity is less than zero or the load factor is
	 *                less or equal to zero
	 */
	public WeakHashMap(int capacity, float loadFactor) {
		if (capacity >= 0 && loadFactor > 0) {
			elementCount = 0;
			elementData = new WeakHashMapEntry[capacity == 0 ? 1 : capacity];
			this.loadFactor = (int) (loadFactor * 10000);
			computeMaxSize();
			referenceQueue = new ReferenceQueue();
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * Constructs a new instance of HashMap containing the mappings from the
	 * specified Map.
	 * 
	 * @param map
	 *            the mappings to add
	 */
	public WeakHashMap(Map map) {
		this(map.size() < 6 ? 11 : map.size() * 2);
		putAll(map);
	}

	/**
	 * Removes all mappings from this WeakHashMap, leaving it empty.
	 * 
	 * @see #isEmpty
	 * @see #size
	 */
	public void clear() {
		if (elementCount > 0) {
			elementCount = 0;
			Arrays.fill(elementData, null);
			modCount++;
			while (referenceQueue.poll() != null) {
				// do nothing
			}
		}
	}

	private void computeMaxSize() {
		threshold = (int) ((long) elementData.length * loadFactor / 10000);
	}

	/**
	 * Searches this WeakHashMap for the specified key.
	 * 
	 * @param key
	 *            the object to search for
	 * @return true if <code>key</code> is a key of this WeakHashMap, false
	 *         otherwise
	 */
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	/**
	 * Answers a Set of the mappings contained in this WeakHashMap. Each element
	 * in the set is a Map.Entry. The set is backed by this WeakHashMap so
	 * changes to one are relected by the other. The set does not support
	 * adding.
	 * 
	 * @return a Set of the mappings
	 */
	public Set entrySet() {
		return new AbstractSet() {
			public int size() {
				return WeakHashMap.this.size();
			}

			public void clear() {
				WeakHashMap.this.clear();
			}

			public boolean remove(Object object) {
				if (contains(object)) {
					WeakHashMap.this.remove(((Map.Entry) object).getKey());
					return true;
				}
				return false;
			}

			public boolean contains(Object object) {
				if (object instanceof Map.Entry) {
					WeakHashMapEntry entry = getEntry(((Map.Entry) object)
							.getKey());
					return object.equals(entry);
				}
				return false;
			}

			public Iterator iterator() {
				return new HashIterator(new KeyEntry.Type() {
					public Object get(KeyEntry entry) {
						return entry;
					}
				}, WeakHashMap.this);
			}
		};
	}

	/**
	 * Answers a Set of the keys contained in this WeakHashMap. The set is
	 * backed by this WeakHashMap so changes to one are reflected by the other.
	 * The set does not support adding.
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
					return WeakHashMap.this.size();
				}

				public void clear() {
					WeakHashMap.this.clear();
				}

				public boolean remove(Object key) {
					if (containsKey(key)) {
						WeakHashMap.this.remove(key);
						return true;
					}
					return false;
				}

				public Iterator iterator() {
					return new HashIterator(new KeyEntry.Type() {
						public Object get(KeyEntry entry) {
							return entry.getKey();
						}
					}, WeakHashMap.this);
				}
			};
		}
		return keySet;
	}

	/**
	 * Answers a Collection of the values contained in this WeakHashMap. The
	 * collection is backed by this WeakHashMap so changes to one are reflected
	 * by the other. The collection does not support adding.
	 * 
	 * @return a Collection of the values
	 */
	public Collection values() {
		if (valuesCollection == null) {
			valuesCollection = new AbstractCollection() {
				public int size() {
					return WeakHashMap.this.size();
				}

				public void clear() {
					WeakHashMap.this.clear();
				}

				public boolean contains(Object object) {
					return containsValue(object);
				}

				public Iterator iterator() {
					return new HashIterator(new KeyEntry.Type() {
						public Object get(KeyEntry entry) {
							return entry.getValue();
						}
					}, WeakHashMap.this);
				}
			};
		}
		return valuesCollection;
	}

	/**
	 * Answers the value of the mapping with the specified key.
	 * 
	 * @param key
	 *            the key
	 * @return the value of the mapping with the specified key
	 */
	public Object get(Object key) {
		if (key != null) {
			int index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			WeakHashMapEntry entry = elementData[index];
			while (entry != null) {
				if (key.equals(entry.get()))
					return entry.value;
				entry = entry.next;
			}
			return null;
		}
		WeakHashMapEntry entry = elementData[0];
		while (entry != null) {
			if (entry.isNull)
				return entry.value;
			entry = entry.next;
		}
		return null;
	}

	WeakHashMapEntry getEntry(Object key) {
		if (key != null) {
			int index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			WeakHashMapEntry entry = elementData[index];
			while (entry != null) {
				if (key.equals(entry.get()))
					return entry;
				entry = entry.next;
			}
			return null;
		}
		WeakHashMapEntry entry = elementData[0];
		while (entry != null) {
			if (entry.isNull)
				return entry;
			entry = entry.next;
		}
		return null;
	}

	/**
	 * Searches this WeakHashMap for the specified value, and returns true, if
	 * at least one entry has this object as its value.
	 * 
	 * @param value
	 *            the object to search for
	 * @return true if <code>value</code> is a value in this WeakHashMap,
	 *         false otherwise
	 */
	public boolean containsValue(Object value) {
		if (value != null) {
			for (int i = elementData.length; --i >= 0;) {
				WeakHashMapEntry entry = elementData[i];
				while (entry != null) {
					if (value.equals(entry.value))
						return true;
					entry = entry.next;
				}
			}
		} else {
			for (int i = elementData.length; --i >= 0;) {
				WeakHashMapEntry entry = elementData[i];
				while (entry != null) {
					if (entry.value == null)
						return true;
					entry = entry.next;
				}
			}
		}
		return false;
	}

	/**
	 * Answers if this WeakHashMap has no elements, a size of zero.
	 * 
	 * @return true if this HashMap has no elements, false otherwise
	 * 
	 * @see #size
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	void poll() {
		WeakHashMapEntry toRemove;
		while ((toRemove = (WeakHashMapEntry) referenceQueue.poll()) != null) {
			WeakHashMapEntry entry, last = null;
			int index = (toRemove.hash & 0x7FFFFFFF) % elementData.length;
			entry = elementData[index];
			// Ignore queued entries which cannot be found, the user could
			// have removed them before they were queued, i.e. using clear()
			while (entry != null) {
				if (toRemove == entry) {
					modCount++;
					if (last == null)
						elementData[index] = entry.next;
					else
						last.next = entry.next;
					elementCount--;
					break;
				}
				last = entry;
				entry = entry.next;
			}
		}
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
		poll();
		int index = 0;
		WeakHashMapEntry entry;
		if (key != null) {
			index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			entry = elementData[index];
			while (entry != null && !key.equals(entry.get()))
				entry = entry.next;
		} else {
			entry = elementData[0];
			while (entry != null && !entry.isNull)
				entry = entry.next;
		}
		if (entry == null) {
			modCount++;
			if (++elementCount > threshold) {
				rehash();
				index = key == null ? 0 : (key.hashCode() & 0x7FFFFFFF)
						% elementData.length;
			}
			entry = new WeakHashMapEntry(key, value, referenceQueue);
			entry.next = elementData[index];
			elementData[index] = entry;
			return null;
		}
		Object result = entry.value;
		entry.value = value;
		return result;
	}

	private void rehash() {
		int length = elementData.length << 1;
		if (length == 0)
			length = 1;
		WeakHashMapEntry[] newData = new WeakHashMapEntry[length];
		for (int i = 0; i < elementData.length; i++) {
			WeakHashMapEntry entry = elementData[i];
			while (entry != null) {
				int index = entry.isNull ? 0 : (entry.hash & 0x7FFFFFFF)
						% length;
				WeakHashMapEntry next = entry.next;
				entry.next = newData[index];
				newData[index] = entry;
				entry = next;
			}
		}
		elementData = newData;
		computeMaxSize();
	}

	/**
	 * Removes a mapping with the specified key from this WeakHashMap.
	 * 
	 * @param key
	 *            the key of the mapping to remove
	 * @return the value of the removed mapping or null if key is not a key in
	 *         this WeakHashMap
	 */
	public Object remove(Object key) {
		poll();
		int index = 0;
		WeakHashMapEntry entry, last = null;
		if (key != null) {
			index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
			entry = elementData[index];
			while (entry != null && !key.equals(entry.get())) {
				last = entry;
				entry = entry.next;
			}
		} else {
			entry = elementData[0];
			while (entry != null && !entry.isNull) {
				last = entry;
				entry = entry.next;
			}
		}
		if (entry != null) {
			modCount++;
			if (last == null)
				elementData[index] = entry.next;
			else
				last.next = entry.next;
			elementCount--;
			return entry.value;
		}
		return null;
	}

	/**
	 * Answers the number of mappings in this WeakHashMap.
	 * 
	 * @return the number of mappings in this WeakHashMap
	 */
	public int size() {
		int size = 0;
		for (int i = elementData.length; --i >= 0;) {
			WeakHashMapEntry entry = elementData[i];
			while (entry != null) {
				if (entry.get() != null || entry.isNull)
					size++;
				entry = entry.next;
			}
		}
		return size;
	}
}
