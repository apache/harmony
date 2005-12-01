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


/**
 * LinkedHashMap
 * 
 */
public class LinkedHashMap extends HashMap {
	
	static final long serialVersionUID = 3801124242820219131L;

	private boolean accessOrder;

	transient private LinkedHashMapEntry head, tail;

	/**
	 * Contructs a new empty instance of LinkedHashMap.
	 */
	public LinkedHashMap() {
		super();
		accessOrder = false;
		head = null;
	}

	/**
	 * Constructor with specified size.
	 * 
	 * @param s
	 *            Size of LinkedHashMap required
	 */
	public LinkedHashMap(int s) {
		super(s);
		accessOrder = false;
		head = null;
	}

	/**
	 * Constructor with specified size and load factor.
	 * 
	 * @param s
	 *            Size of LinkedHashMap required
	 * @param lf
	 *            Load factor
	 */
	public LinkedHashMap(int s, float lf) {
		super(s, lf);
		accessOrder = false;
		head = null;
		tail = null;
	}

	/**
	 * Constructor with specified size, load factor and access order
	 * 
	 * @param s
	 *            Size of LinkedHashmap required
	 * @param lf
	 *            Load factor
	 * @param order
	 *            If true indicates that traversal order should begin with most
	 *            recently accessed
	 */
	public LinkedHashMap(int s, float lf, boolean order) {
		super(s, lf);
		accessOrder = order;
		head = null;
		tail = null;
	}

	/**
	 * Constructor with input map
	 * 
	 * @param m
	 *            Input map
	 */
	public LinkedHashMap(Map m) {
		accessOrder = false;
		head = null;
		tail = null;
		putAll(m);
	}

	static final class LinkedHashIterator extends HashMapIterator {
		LinkedHashIterator(MapEntry.Type value, LinkedHashMap hm) {
			super(value, hm);
			entry = hm.head;
		}

		public boolean hasNext() {
			return (entry != null);
		}

		public Object next() {
			checkConcurrentMod();
			if (!hasNext())
				throw new NoSuchElementException();
			Object result = type.get(entry);
			lastEntry = entry;
			entry = ((LinkedHashMapEntry) entry).chainForward;
			canRemove = true;
			return result;
		}

		public void remove() {
			checkConcurrentMod();
			if (!canRemove)
				throw new IllegalStateException();

			canRemove = false;
			associatedMap.modCount++;

			int index = associatedMap.getModuloHash(lastEntry.key);
			LinkedHashMapEntry m = (LinkedHashMapEntry) associatedMap.elementData[index];
			if (m == lastEntry) {
				associatedMap.elementData[index] = lastEntry.next;
			} else {
				while (m.next != null) {
					if (m.next == lastEntry)
						break;
					m = (LinkedHashMapEntry) m.next;
				}
				// assert m.next == entry
				m.next = lastEntry.next;
			}
			LinkedHashMapEntry lhme = (LinkedHashMapEntry) lastEntry;
			LinkedHashMapEntry p = lhme.chainBackward;
			LinkedHashMapEntry n = lhme.chainForward;
			LinkedHashMap lhm = (LinkedHashMap) associatedMap;
			if (p != null) {
				p.chainForward = n;
				if (n != null)
					n.chainBackward = p;
				else
					lhm.tail = p;
			} else {
				lhm.head = n;
				if (n != null)
					n.chainBackward = null;
				else
					lhm.tail = null;
			}
			associatedMap.elementCount--;
			expectedModCount++;
		}
	}

	static final class LinkedHashMapEntrySet extends HashMapEntrySet {
		public LinkedHashMapEntrySet(LinkedHashMap lhm) {
			super(lhm);
		}

		public Iterator iterator() {
			return new LinkedHashIterator(new MapEntry.Type() {
				public Object get(MapEntry entry) {
					return entry;
				}
			}, (LinkedHashMap) hashMap());
		}
	}

	static final class LinkedHashMapEntry extends HashMapEntry {
		LinkedHashMapEntry chainForward, chainBackward;

		LinkedHashMapEntry(Object theKey, Object theValue) {
			super(theKey, theValue);
			chainForward = null;
			chainBackward = null;
		}

		public Object clone() {
			LinkedHashMapEntry entry = (LinkedHashMapEntry) super.clone();
			entry.chainBackward = chainBackward;
			entry.chainForward = chainForward;
			LinkedHashMapEntry lnext = (LinkedHashMapEntry) entry.next;
			if (lnext != null)
				entry.next = (LinkedHashMapEntry) lnext.clone();
			return entry;
		}
	}

	/**
	 * Create a new element array
	 * 
	 * @param s
	 * @return Reference to the element array
	 */
	HashMapEntry[] newElementArray(int s) {
		return new LinkedHashMapEntry[s];
	}

	/**
	 * Retrieve the map value corresponding to the given key.
	 * 
	 * @param key
	 *            Key value
	 * @return mapped value or null if the key is not in the map
	 */
	public Object get(Object key) {
		LinkedHashMapEntry m = (LinkedHashMapEntry) getEntry(key);
		if (m == null)
			return null;
		if (accessOrder && tail != m) {
			LinkedHashMapEntry p = m.chainBackward;
			LinkedHashMapEntry n = m.chainForward;
			n.chainBackward = p;
			if (p != null)
				p.chainForward = n;
			else
				head = n;
			m.chainForward = null;
			m.chainBackward = tail;
			tail.chainForward = m;
			tail = m;
		}
		return m.value;
	}

	/*
	 * @param key
	 * @param index
	 * @return
	 */
	HashMapEntry createEntry(Object key, int index, Object value) {
		LinkedHashMapEntry m = new LinkedHashMapEntry(key, value);
		m.next = elementData[index];
		elementData[index] = m;
		linkEntry(m);
		return m;
	}

	/**
	 * Set the mapped value for the given key to the given value.
	 * 
	 * @param key
	 *            Key value
	 * @param value
	 *            New mapped value
	 * @return The old value if the key was already in the map or null
	 *         otherwise.
	 */
	public Object put(Object key, Object value) {
		int index = getModuloHash(key);
		LinkedHashMapEntry m = (LinkedHashMapEntry) findEntry(key, index);

		if (m == null) {
			modCount++;
			// Check if we need to remove the oldest entry
			// The check includes accessOrder since an accessOrder LinkedHashMap
			// does not record
			// the oldest member in 'head'.
			if (++elementCount > threshold) {
				rehash();
				index = key == null ? 0 : (key.hashCode() & 0x7FFFFFFF)
						% elementData.length;
			}
			m = (LinkedHashMapEntry) createEntry(key, index, null);
		} else {
			linkEntry(m);
		}

		Object result = m.value;
		m.value = value;

		if (removeEldestEntry(head))
			remove(head.key);

		return result;
	}

	/*
	 * @param m
	 */
	void linkEntry(LinkedHashMapEntry m) {
		if (tail == m) {
			return;
		}

		if (head == null) {
			// Check if the map is empty
			head = tail = m;
			return;
		}

		// we need to link the new entry into either the head or tail
		// of the chain depending on if the LinkedHashMap is accessOrder or not
		LinkedHashMapEntry p = m.chainBackward;
		LinkedHashMapEntry n = m.chainForward;
		if (p == null) {
			if (n != null) {
				// The entry must be the head but not the tail
				if (accessOrder) {
					head = n;
					n.chainBackward = null;
					m.chainBackward = tail;
					m.chainForward = null;
					tail.chainForward = m;
					tail = m;
				}
			} else {
				// This is a new entry
				m.chainBackward = tail;
				m.chainForward = null;
				tail.chainForward = m;
				tail = m;
			}
			return;
		}

		if (n == null) {
			// The entry must be the tail so we can't get here
			return;
		}

		// The entry is neither the head nor tail
		if (accessOrder) {
			p.chainForward = n;
			n.chainBackward = p;
			m.chainForward = null;
			m.chainBackward = tail;
			tail.chainForward = m;
			tail = m;
		}

	}

	/**
	 * Put all entries from the given map into the LinkedHashMap
	 * 
	 * @param m
	 *            Input map
	 */
	public void putAll(Map m) {
		Iterator i = m.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = (MapEntry) i.next();
			put(e.getKey(), e.getValue());
		}
	}

	/**
	 * Answers a Set of the mappings contained in this HashMap. Each element in
	 * the set is a Map.Entry. The set is backed by this HashMap so changes to
	 * one are relected by the other. The set does not support adding.
	 * 
	 * @return a Set of the mappings
	 */
	public Set entrySet() {
		return new LinkedHashMapEntrySet(this);
	}

	/**
	 * Answers a Set of the keys contained in this HashMap. The set is backed by
	 * this HashMap so changes to one are relected by the other. The set does
	 * not support adding.
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
					return LinkedHashMap.this.size();
				}

				public void clear() {
					LinkedHashMap.this.clear();
				}

				public boolean remove(Object key) {
					if (containsKey(key)) {
						LinkedHashMap.this.remove(key);
						return true;
					}
					return false;
				}

				public Iterator iterator() {
					return new LinkedHashIterator(new MapEntry.Type() {
						public Object get(MapEntry entry) {
							return entry.key;
						}
					}, LinkedHashMap.this);
				}
			};
		}
		return keySet;
	}

	/**
	 * Answers a Collection of the values contained in this HashMap. The
	 * collection is backed by this HashMap so changes to one are relected by
	 * the other. The collection does not support adding.
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
					return LinkedHashMap.this.size();
				}

				public void clear() {
					LinkedHashMap.this.clear();
				}

				public Iterator iterator() {
					return new LinkedHashIterator(new MapEntry.Type() {
						public Object get(MapEntry entry) {
							return entry.value;
						}
					}, LinkedHashMap.this);
				}
			};
		}
		return valuesCollection;
	}

	/**
	 * Remove the entry corresponding to the given key.
	 * 
	 * @param key
	 *            the key
	 * @return the value associated with the key or null if the key was no in
	 *         the map
	 */
	public Object remove(Object key) {
		LinkedHashMapEntry m = (LinkedHashMapEntry) removeEntry(key);
		if (m == null)
			return null;
		LinkedHashMapEntry p = m.chainBackward;
		LinkedHashMapEntry n = m.chainForward;
		if (p != null)
			p.chainForward = n;
		else
			head = n;
		if (n != null)
			n.chainBackward = p;
		else
			tail = p;
		return m.value;
	}

	/**
	 * This method is queried from the put and putAll methods to check if the
	 * eldest member of the map should be deleted before adding the new member.
	 * If this map was created with accessOrder = true, then the result of
	 * removeEldesrEntry is assumed to be false.
	 * 
	 * @param eldest
	 * @return true if the eldest member should be removed
	 */
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return false;
	}

	/**
	 * Removes all mappings from this HashMap, leaving it empty.
	 * 
	 * @see #isEmpty()
	 * @see #size()
	 */
	public void clear() {
		super.clear();
		head = tail = null;
	}

	/**
	 * Answers a new HashMap with the same mappings and size as this HashMap.
	 * 
	 * @return a shallow copy of this HashMap
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		LinkedHashMap map = (LinkedHashMap) super.clone();
		map.clear();
		Iterator entries = entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}
}
