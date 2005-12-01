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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * Collections contains static methods which operate on Collection classes.
 */
public class Collections {

	private static final class CopiesList extends AbstractList implements
			Serializable {
		static final long serialVersionUID = 2739099268398711800L;

		private int n;

		private Object element;

		CopiesList(int length, Object object) {
			if (length < 0) {
				throw new IllegalArgumentException();
			}
			n = length;
			element = object;
		}

		public boolean contains(Object object) {
			return element == null ? object == null : element.equals(object);
		}

		public int size() {
			return n;
		}

		public Object get(int location) {
			if (0 <= location && location < n)
				return element;
			throw new IndexOutOfBoundsException();
		}
	}

	private static final class EmptyList extends AbstractList implements
			Serializable {
		static final long serialVersionUID = 8842843931221139166L;

		public boolean contains(Object object) {
			return false;
		}

		public int size() {
			return 0;
		}

		public Object get(int location) {
			throw new IndexOutOfBoundsException();
		}
	}

	private static final class EmptySet extends AbstractSet implements
			Serializable {
		static final long serialVersionUID = 1582296315990362920L;

		public boolean contains(Object object) {
			return false;
		}

		public int size() {
			return 0;
		}

		public Iterator iterator() {
			return new Iterator() {
				public boolean hasNext() {
					return false;
				}

				public Object next() {
					throw new NoSuchElementException();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	private static final class EmptyMap extends AbstractMap implements
			Serializable {
		static final long serialVersionUID = 6428348081105594320L;

		public boolean containsKey(Object key) {
			return false;
		}

		public boolean containsValue(Object value) {
			return false;
		}

		public Set entrySet() {
			return EMPTY_SET;
		}

		public Object get(Object key) {
			return null;
		}

		public Set keySet() {
			return EMPTY_SET;
		}

		public Collection values() {
			return EMPTY_LIST;
		}
	}

	public static final List EMPTY_LIST = new EmptyList();

	public static final Set EMPTY_SET = new EmptySet();

	public static final Map EMPTY_MAP = new EmptyMap();

	private static class ReverseComparator implements Comparator, Serializable {
		static final long serialVersionUID = 7207038068494060240L;

		public int compare(Object o1, Object o2) {
			return -((Comparable) o1).compareTo(o2);
		}
	}

	private static final class SingletonSet extends AbstractSet implements
			Serializable {
		static final long serialVersionUID = 3193687207550431679L;

		Object element;

		SingletonSet(Object object) {
			element = object;
		}

		public boolean contains(Object object) {
			return element == null ? object == null : element.equals(object);
		}

		public int size() {
			return 1;
		}

		public Iterator iterator() {
			return new Iterator() {
				boolean hasNext = true;

				public boolean hasNext() {
					return hasNext;
				}

				public Object next() {
					if (hasNext) {
						hasNext = false;
						return element;
					} else
						throw new NoSuchElementException();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	private static final class SingletonList extends AbstractList implements
			Serializable {
		static final long serialVersionUID = 3093736618740652951L;

		Object element;

		SingletonList(Object object) {
			element = object;
		}

		public boolean contains(Object object) {
			return element == null ? object == null : element.equals(object);
		}

		public Object get(int location) {
			if (location == 0)
				return element;
			throw new IndexOutOfBoundsException();
		}

		public int size() {
			return 1;
		}
	}

	private static final class SingletonMap extends AbstractMap implements
			Serializable {
		static final long serialVersionUID = -6979724477215052911L;

		Object k, v;

		SingletonMap(Object key, Object value) {
			k = key;
			v = value;
		}

		public boolean containsKey(Object key) {
			return k == null ? key == null : k.equals(key);
		}

		public boolean containsValue(Object value) {
			return v == null ? value == null : v.equals(value);
		}

		public Object get(Object key) {
			if (containsKey(key))
				return v;
			return null;
		}

		public int size() {
			return 1;
		}

		public Set entrySet() {
			return new AbstractSet() {
				public boolean contains(Object object) {
					if (object instanceof Map.Entry) {
						Map.Entry entry = (Map.Entry) object;
						return containsKey(entry.getKey())
								&& containsValue(entry.getValue());
					}
					return false;
				}

				public int size() {
					return 1;
				}

				public Iterator iterator() {
					return new Iterator() {
						boolean hasNext = true;

						public boolean hasNext() {
							return hasNext;
						}

						public Object next() {
							if (hasNext) {
								hasNext = false;
								return new Map.Entry() {
									public boolean equals(Object object) {
										return contains(object);
									}

									public Object getKey() {
										return k;
									}

									public Object getValue() {
										return v;
									}

									public int hashCode() {
										return (k == null ? 0 : k.hashCode())
												^ (v == null ? 0 : v.hashCode());
									}

									public Object setValue(Object value) {
										throw new UnsupportedOperationException();
									}
								};
							} else
								throw new NoSuchElementException();
						}

						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		}
	}

	static class SynchronizedCollection implements Collection, Serializable {
		static final long serialVersionUID = 3053995032091335093L;

		Collection c;

		Object mutex;

		SynchronizedCollection(Collection collection) {
			c = collection;
			mutex = this;
		}

		SynchronizedCollection(Collection collection, Object mutex) {
			c = collection;
			this.mutex = mutex;
		}

		public boolean add(Object object) {
			synchronized (mutex) {
				return c.add(object);
			}
		}

		public boolean addAll(Collection collection) {
			synchronized (mutex) {
				return c.addAll(collection);
			}
		}

		public void clear() {
			synchronized (mutex) {
				c.clear();
			}
		}

		public boolean contains(Object object) {
			synchronized (mutex) {
				return c.contains(object);
			}
		}

		public boolean containsAll(Collection collection) {
			synchronized (mutex) {
				return c.containsAll(collection);
			}
		}

		public boolean isEmpty() {
			synchronized (mutex) {
				return c.isEmpty();
			}
		}

		public Iterator iterator() {
			synchronized (mutex) {
				return c.iterator();
			}
		}

		public boolean remove(Object object) {
			synchronized (mutex) {
				return c.remove(object);
			}
		}

		public boolean removeAll(Collection collection) {
			synchronized (mutex) {
				return c.removeAll(collection);
			}
		}

		public boolean retainAll(Collection collection) {
			synchronized (mutex) {
				return c.retainAll(collection);
			}
		}

		public int size() {
			synchronized (mutex) {
				return c.size();
			}
		}

		public java.lang.Object[] toArray() {
			synchronized (mutex) {
				return c.toArray();
			}
		}

		public Object[] toArray(Object[] array) {
			synchronized (mutex) {
				return c.toArray(array);
			}
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			synchronized (mutex) {
				stream.defaultWriteObject();
			}
		}
	}

	static class SynchronizedRandomAccessList extends SynchronizedList
			implements RandomAccess {
		static final long serialVersionUID = 1530674583602358482L;

		SynchronizedRandomAccessList(List l) {
			super(l);
		}

		SynchronizedRandomAccessList(List l, Object mutex) {
			super(l, mutex);
			list = l;
		}

		public List subList(int start, int end) {
			synchronized (mutex) {
				return new SynchronizedRandomAccessList(list
						.subList(start, end), mutex);
			}
		}

		/**
		 * Replaces this SynchronizedRandomAccessList with a SynchronizedList so
		 * that JREs before 1.4 can deserialize this object without any
		 * problems. This is necessary since RandomAccess API was introduced
		 * only in 1.4.
		 * <p>
		 * @return SynchronizedList
		 * 
		 * @see SynchronizedList#readResolve()
		 */
		private Object writeReplace() {
			return new SynchronizedList(list);
		}
	}

	static class SynchronizedList extends SynchronizedCollection implements
			List {
		static final long serialVersionUID = -7754090372962971524L;

		List list;

		SynchronizedList(List l) {
			super(l);
			list = l;
		}

		SynchronizedList(List l, Object mutex) {
			super(l, mutex);
			list = l;
		}

		public void add(int location, Object object) {
			synchronized (mutex) {
				list.add(location, object);
			}
		}

		public boolean addAll(int location, Collection collection) {
			synchronized (mutex) {
				return list.addAll(location, collection);
			}
		}

		public boolean equals(Object object) {
			synchronized (mutex) {
				return list.equals(object);
			}
		}

		public Object get(int location) {
			synchronized (mutex) {
				return list.get(location);
			}
		}

		public int hashCode() {
			synchronized (mutex) {
				return list.hashCode();
			}
		}

		public int indexOf(Object object) {
			synchronized (mutex) {
				return list.indexOf(object);
			}
		}

		public int lastIndexOf(Object object) {
			synchronized (mutex) {
				return list.lastIndexOf(object);
			}
		}

		public ListIterator listIterator() {
			synchronized (mutex) {
				return list.listIterator();
			}
		}

		public ListIterator listIterator(int location) {
			synchronized (mutex) {
				return list.listIterator(location);
			}
		}

		public Object remove(int location) {
			synchronized (mutex) {
				return list.remove(location);
			}
		}

		public Object set(int location, Object object) {
			synchronized (mutex) {
				return list.set(location, object);
			}
		}

		public List subList(int start, int end) {
			synchronized (mutex) {
				return new SynchronizedList(list.subList(start, end), mutex);
			}
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			synchronized (mutex) {
				stream.defaultWriteObject();
			}
		}

		/**
		 * Resolves SynchronizedList instances to SynchronizedRandomAccessList
		 * instances if the underlying list is a Random Access list.
		 * <p>
		 * This is necessary since SynchronizedRandomAccessList instances are
		 * replaced with SynchronizedList instances during serialization for
		 * compliance with JREs before 1.4.
		 * <p>
		 * 
		 * @return a SynchronizedList instance if the underlying list implements
		 *         RandomAccess interface, or this same object if not.
		 * 
		 * @see SynchronizedRandomAccessList#writeReplace()
		 */
		private Object readResolve() {
			if (list instanceof RandomAccess)
				return new SynchronizedRandomAccessList(list, mutex);
			else
				return this;
		}
	}

	static class SynchronizedMap implements Map, Serializable {
		static final long serialVersionUID = 1978198479659022715L;

		private Map m;

		Object mutex;

		SynchronizedMap(Map map) {
			m = map;
			mutex = this;
		}

		SynchronizedMap(Map map, Object mutex) {
			m = map;
			this.mutex = mutex;
		}

		public void clear() {
			synchronized (mutex) {
				m.clear();
			}
		}

		public boolean containsKey(Object key) {
			synchronized (mutex) {
				return m.containsKey(key);
			}
		}

		public boolean containsValue(Object value) {
			synchronized (mutex) {
				return m.containsValue(value);
			}
		}

		public Set entrySet() {
			synchronized (mutex) {
				return new SynchronizedSet(m.entrySet(), mutex);
			}
		}

		public boolean equals(Object object) {
			synchronized (mutex) {
				return m.equals(object);
			}
		}

		public Object get(Object key) {
			synchronized (mutex) {
				return m.get(key);
			}
		}

		public int hashCode() {
			synchronized (mutex) {
				return m.hashCode();
			}
		}

		public boolean isEmpty() {
			synchronized (mutex) {
				return m.isEmpty();
			}
		}

		public Set keySet() {
			synchronized (mutex) {
				return new SynchronizedSet(m.keySet(), mutex);
			}
		}

		public Object put(Object key, Object value) {
			synchronized (mutex) {
				return m.put(key, value);
			}
		}

		public void putAll(Map map) {
			synchronized (mutex) {
				m.putAll(map);
			}
		}

		public Object remove(Object key) {
			synchronized (mutex) {
				return m.remove(key);
			}
		}

		public int size() {
			synchronized (mutex) {
				return m.size();
			}
		}

		public Collection values() {
			synchronized (mutex) {
				return new SynchronizedCollection(m.values(), mutex);
			}
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			synchronized (mutex) {
				stream.defaultWriteObject();
			}
		}
	}

	static class SynchronizedSet extends SynchronizedCollection implements Set {
		static final long serialVersionUID = 487447009682186044L;

		SynchronizedSet(Set set) {
			super(set);
		}

		SynchronizedSet(Set set, Object mutex) {
			super(set, mutex);
		}

		public boolean equals(Object object) {
			synchronized (mutex) {
				return c.equals(object);
			}
		}

		public int hashCode() {
			synchronized (mutex) {
				return c.hashCode();
			}
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			synchronized (mutex) {
				stream.defaultWriteObject();
			}
		}
	}

	static class SynchronizedSortedMap extends SynchronizedMap implements
			SortedMap {
		static final long serialVersionUID = -8798146769416483793L;

		private SortedMap sm;

		SynchronizedSortedMap(SortedMap map) {
			super(map);
			sm = map;
		}

		SynchronizedSortedMap(SortedMap map, Object mutex) {
			super(map, mutex);
			sm = map;
		}

		public Comparator comparator() {
			synchronized (mutex) {
				return sm.comparator();
			}
		}

		public Object firstKey() {
			synchronized (mutex) {
				return sm.firstKey();
			}
		}

		public SortedMap headMap(Object endKey) {
			synchronized (mutex) {
				return new SynchronizedSortedMap(sm.headMap(endKey), mutex);
			}
		}

		public Object lastKey() {
			synchronized (mutex) {
				return sm.lastKey();
			}
		}

		public SortedMap subMap(Object startKey, Object endKey) {
			synchronized (mutex) {
				return new SynchronizedSortedMap(sm.subMap(startKey, endKey),
						mutex);
			}
		}

		public SortedMap tailMap(Object startKey) {
			synchronized (mutex) {
				return new SynchronizedSortedMap(sm.tailMap(startKey), mutex);
			}
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			synchronized (mutex) {
				stream.defaultWriteObject();
			}
		}
	}

	static class SynchronizedSortedSet extends SynchronizedSet implements
			SortedSet {
		static final long serialVersionUID = 8695801310862127406L;

		private SortedSet ss;

		SynchronizedSortedSet(SortedSet set) {
			super(set);
			ss = set;
		}

		SynchronizedSortedSet(SortedSet set, Object mutex) {
			super(set, mutex);
			ss = set;
		}

		public Comparator comparator() {
			synchronized (mutex) {
				return ss.comparator();
			}
		}

		public Object first() {
			synchronized (mutex) {
				return ss.first();
			}
		}

		public SortedSet headSet(Object end) {
			synchronized (mutex) {
				return new SynchronizedSortedSet(ss.headSet(end), mutex);
			}
		}

		public Object last() {
			synchronized (mutex) {
				return ss.last();
			}
		}

		public SortedSet subSet(Object start, Object end) {
			synchronized (mutex) {
				return new SynchronizedSortedSet(ss.subSet(start, end), mutex);
			}
		}

		public SortedSet tailSet(Object start) {
			synchronized (mutex) {
				return new SynchronizedSortedSet(ss.tailSet(start), mutex);
			}
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			synchronized (mutex) {
				stream.defaultWriteObject();
			}
		}
	}

	private static class UnmodifiableCollection implements Collection,
			Serializable {
		static final long serialVersionUID = 1820017752578914078L;

		Collection c;

		UnmodifiableCollection(Collection collection) {
			c = collection;
		}

		public boolean add(Object object) {
			throw new UnsupportedOperationException();
		}

		public boolean addAll(Collection collection) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean contains(Object object) {
			return c.contains(object);
		}

		public boolean containsAll(Collection collection) {
			return c.containsAll(collection);
		}

		public boolean isEmpty() {
			return c.isEmpty();
		}

		public Iterator iterator() {
			return new Iterator() {
				Iterator iterator = c.iterator();

				public boolean hasNext() {
					return iterator.hasNext();
				}

				public Object next() {
					return iterator.next();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		public boolean remove(Object object) {
			throw new UnsupportedOperationException();
		}

		public boolean removeAll(Collection collection) {
			throw new UnsupportedOperationException();
		}

		public boolean retainAll(Collection collection) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return c.size();
		}

		public java.lang.Object[] toArray() {
			return c.toArray();
		}

		public Object[] toArray(Object[] array) {
			return c.toArray(array);
		}
	}

	private static class UnmodifiableRandomAccessList extends UnmodifiableList
			implements RandomAccess {
		static final long serialVersionUID = -2542308836966382001L;

		UnmodifiableRandomAccessList(List l) {
			super(l);
		}

		public List subList(int start, int end) {
			return new UnmodifiableRandomAccessList(list.subList(start, end));
		}

		/**
		 * Replaces this UnmodifiableRandomAccessList with an UnmodifiableList
		 * so that JREs before 1.4 can deserialize this object without any
		 * problems. This is necessary since RandomAccess API was introduced
		 * only in 1.4.
		 * <p>
		 * @return UnmodifiableList
		 * 
		 * @see UnmodifiableList#readResolve()
		 */
		private Object writeReplace() {
			return new UnmodifiableList(list);
		}
	}

	private static class UnmodifiableList extends UnmodifiableCollection
			implements List {
		static final long serialVersionUID = -283967356065247728L;

		List list;

		UnmodifiableList(List l) {
			super(l);
			list = l;
		}

		public void add(int location, Object object) {
			throw new UnsupportedOperationException();
		}

		public boolean addAll(int location, Collection collection) {
			throw new UnsupportedOperationException();
		}

		public boolean equals(Object object) {
			return list.equals(object);
		}

		public Object get(int location) {
			return list.get(location);
		}

		public int hashCode() {
			return list.hashCode();
		}

		public int indexOf(Object object) {
			return list.indexOf(object);
		}

		public int lastIndexOf(Object object) {
			return list.lastIndexOf(object);
		}

		public ListIterator listIterator() {
			return listIterator(0);
		}

		public ListIterator listIterator(final int location) {
			return new ListIterator() {
				ListIterator iterator = list.listIterator(location);

				public void add(Object object) {
					throw new UnsupportedOperationException();
				}

				public boolean hasNext() {
					return iterator.hasNext();
				}

				public boolean hasPrevious() {
					return iterator.hasPrevious();
				}

				public Object next() {
					return iterator.next();
				}

				public int nextIndex() {
					return iterator.nextIndex();
				}

				public Object previous() {
					return iterator.previous();
				}

				public int previousIndex() {
					return iterator.previousIndex();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}

				public void set(Object object) {
					throw new UnsupportedOperationException();
				}
			};
		}

		public Object remove(int location) {
			throw new UnsupportedOperationException();
		}

		public Object set(int location, Object object) {
			throw new UnsupportedOperationException();
		}

		public List subList(int start, int end) {
			return new UnmodifiableList(list.subList(start, end));
		}

		/**
		 * Resolves UnmodifiableList instances to UnmodifiableRandomAccessList
		 * instances if the underlying list is a Random Access list.
		 * <p>
		 * This is necessary since UnmodifiableRandomAccessList instances are
		 * replaced with UnmodifiableList instances during serialization for
		 * compliance with JREs before 1.4.
		 * <p>
		 * 
		 * @return an UnmodifiableList instance if the underlying list
		 *         implements RandomAccess interface, or this same object if
		 *         not.
		 * 
		 * @see UnmodifiableRandomAccessList#writeReplace()
		 */
		private Object readResolve() {
			if (list instanceof RandomAccess)
				return new UnmodifiableRandomAccessList(list);
			else
				return this;
		}
	}

	private static class UnmodifiableMap implements Map, Serializable {
		static final long serialVersionUID = -1034234728574286014L;

		private Map m;

		private static class UnmodifiableEntrySet extends UnmodifiableSet {
			static final long serialVersionUID = 7854390611657943733L;

			private static class UnmodifiableMapEntry implements Map.Entry {
				Map.Entry mapEntry;

				UnmodifiableMapEntry(Map.Entry entry) {
					mapEntry = entry;
				}

				public boolean equals(Object object) {
					return mapEntry.equals(object);
				}

				public Object getKey() {
					return mapEntry.getKey();
				}

				public Object getValue() {
					return mapEntry.getValue();
				}

				public int hashCode() {
					return mapEntry.hashCode();
				}

				public Object setValue(Object object) {
					throw new UnsupportedOperationException();
				}

				public String toString() {
					return mapEntry.toString();
				}
			}

			UnmodifiableEntrySet(Set set) {
				super(set);
			}

			public Iterator iterator() {
				return new Iterator() {
					Iterator iterator = c.iterator();

					public boolean hasNext() {
						return iterator.hasNext();
					}

					public Object next() {
						return new UnmodifiableMapEntry((Map.Entry) iterator
								.next());
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			public Object[] toArray() {
				int length = c.size();
				Object[] result = new Object[length];
				Iterator it = iterator();
				for (int i = length; --i >= 0;)
					result[i] = it.next();
				return result;
			}

			public Object[] toArray(Object[] contents) {
				int size = c.size(), index = 0;
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
		}

		UnmodifiableMap(Map map) {
			m = map;
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean containsKey(Object key) {
			return m.containsKey(key);
		}

		public boolean containsValue(Object value) {
			return m.containsValue(value);
		}

		public Set entrySet() {
			return new UnmodifiableEntrySet(m.entrySet());
		}

		public boolean equals(Object object) {
			return m.equals(object);
		}

		public Object get(Object key) {
			return m.get(key);
		}

		public int hashCode() {
			return m.hashCode();
		}

		public boolean isEmpty() {
			return m.isEmpty();
		}

		public Set keySet() {
			return new UnmodifiableSet(m.keySet());
		}

		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		public void putAll(Map map) {
			throw new UnsupportedOperationException();
		}

		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return m.size();
		}

		public Collection values() {
			return new UnmodifiableCollection(m.values());
		}
	}

	private static class UnmodifiableSet extends UnmodifiableCollection
			implements Set {
		static final long serialVersionUID = -9215047833775013803L;

		UnmodifiableSet(Set set) {
			super(set);
		}

		public boolean equals(Object object) {
			return c.equals(object);
		}

		public int hashCode() {
			return c.hashCode();
		}
	}

	private static class UnmodifiableSortedMap extends UnmodifiableMap
			implements SortedMap {
		static final long serialVersionUID = -8806743815996713206L;

		private SortedMap sm;

		UnmodifiableSortedMap(SortedMap map) {
			super(map);
			sm = map;
		}

		public Comparator comparator() {
			return sm.comparator();
		}

		public Object firstKey() {
			return sm.firstKey();
		}

		public SortedMap headMap(Object before) {
			return new UnmodifiableSortedMap(sm.headMap(before));
		}

		public Object lastKey() {
			return sm.lastKey();
		}

		public SortedMap subMap(Object start, Object end) {
			return new UnmodifiableSortedMap(sm.subMap(start, end));
		}

		public SortedMap tailMap(Object after) {
			return new UnmodifiableSortedMap(sm.tailMap(after));
		}
	}

	private static class UnmodifiableSortedSet extends UnmodifiableSet
			implements SortedSet {
		static final long serialVersionUID = -4929149591599911165L;

		private SortedSet ss;

		UnmodifiableSortedSet(SortedSet set) {
			super(set);
			ss = set;
		}

		public Comparator comparator() {
			return ss.comparator();
		}

		public Object first() {
			return ss.first();
		}

		public SortedSet headSet(Object before) {
			return new UnmodifiableSortedSet(ss.headSet(before));
		}

		public Object last() {
			return ss.last();
		}

		public SortedSet subSet(Object start, Object end) {
			return new UnmodifiableSortedSet(ss.subSet(start, end));
		}

		public SortedSet tailSet(Object after) {
			return new UnmodifiableSortedSet(ss.tailSet(after));
		}
	}

	private Collections() {
		/*empty*/
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted List.
	 * 
	 * @param list
	 *            the sorted List to search
	 * @param object
	 *            the element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 * 
	 * @exception ClassCastException
	 *                when an element in the List or the seach element does not
	 *                implement Comparable, or cannot be compared to each other
	 */
	public static int binarySearch(List list, Object object) {
		if (list == null)
			throw new NullPointerException();
		Comparable key = (Comparable) object;
		if (!(list instanceof RandomAccess)) {
			ListIterator it = list.listIterator();
			while (it.hasNext()) {
				int result;
				if ((result = key.compareTo(it.next())) <= 0)
					if (result == 0)
						return it.previousIndex();
					else
						return -it.previousIndex() - 1;
			}
			return -list.size() - 1;
		}

		int low = 0, mid = list.size(), high = mid - 1, result = -1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if ((result = key.compareTo(list.get(mid))) > 0)
				low = mid + 1;
			else if (result == 0)
				return mid;
			else
				high = mid - 1;
		}
		return -mid - (result < 0 ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted List using the specified Comparator.
	 * 
	 * @param list
	 *            the sorted List to search
	 * @param object
	 *            the element to find
	 * @param comparator
	 *            the Comparator
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 * 
	 * @exception ClassCastException
	 *                when an element in the list and the searched element
	 *                cannot be compared to each other using the Comparator
	 */
	public static int binarySearch(List list, Object object,
			Comparator comparator) {
		if (!(list instanceof RandomAccess)) {
			ListIterator it = list.listIterator();
			while (it.hasNext()) {
				int result;
				if ((result = comparator.compare(object, it.next())) <= 0)
					if (result == 0)
						return it.previousIndex();
					else
						return -it.previousIndex() - 1;
			}
			return -list.size() - 1;
		}

		int low = 0, mid = list.size(), high = mid - 1, result = -1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if ((result = comparator.compare(object, list.get(mid))) > 0)
				low = mid + 1;
			else if (result == 0)
				return mid;
			else
				high = mid - 1;
		}
		return -mid - (result < 0 ? 1 : 2);
	}

	/**
	 * Copies the elements from the source list to the destination list.
	 * @param destination 
	 * @param source 
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when the destination List is smaller than the source List
	 * @exception UnsupportedOperationException
	 *                when replacing an element in the destination list is not
	 *                supported
	 */
	public static void copy(List destination, List source) {
		if (destination.size() < source.size()) {
			throw new ArrayIndexOutOfBoundsException();
		}
		Iterator srcIt = source.iterator();
		ListIterator destIt = destination.listIterator();
		while (srcIt.hasNext()) {
			try {
				destIt.next();
			} catch (NoSuchElementException e) {
				throw new ArrayIndexOutOfBoundsException();
			}
			destIt.set(srcIt.next());
		}
	}

	/**
	 * Answers an Enumeration on the specified Collection.
	 * 
	 * @param collection
	 *            the Collection to enumerate
	 * @return an Enumeration
	 */
	public static Enumeration enumeration(final Collection collection) {
		return new Enumeration() {
			Iterator it = collection.iterator();

			public boolean hasMoreElements() {
				return it.hasNext();
			}

			public Object nextElement() {
				return it.next();
			}
		};
	}

	/**
	 * Fills the specified List with the specified element.
	 * 
	 * @param list
	 *            the List to fill
	 * @param object
	 *            the fill element
	 * 
	 * @exception UnsupportedOperationException
	 *                when replacing an element in the List is not supported
	 */
	public static void fill(List list, Object object) {
		ListIterator it = list.listIterator();
		while (it.hasNext()) {
			it.next();
			it.set(object);
		}
	}

	/**
	 * Searches the specified Collection for the maximum element.
	 * 
	 * @param collection
	 *            the Collection to search
	 * @return the maximum element in the Collection
	 * 
	 * @exception ClassCastException
	 *                when an element in the Collection does not implement
	 *                Comparable or elements cannot be compared to each other
	 */
	public static Object max(Collection collection) {
		Iterator it = collection.iterator();
		Comparable max = (Comparable) it.next();
		while (it.hasNext()) {
			Object next = it.next();
			if (max.compareTo(next) < 0)
				max = (Comparable) next;
		}
		return max;
	}

	/**
	 * Searches the specified Collection for the maximum element using the
	 * specified Comparator.
	 * 
	 * @param collection
	 *            the Collection to search
	 * @param comparator
	 *            the Comparator
	 * @return the maximum element in the Collection
	 * 
	 * @exception ClassCastException
	 *                when elements in the Collection cannot be compared to each
	 *                other using the Comparator
	 */
	public static Object max(Collection collection, Comparator comparator) {
		Iterator it = collection.iterator();
		Object max = it.next();
		while (it.hasNext()) {
			Object next = it.next();
			if (comparator.compare(max, next) < 0)
				max = next;
		}
		return max;
	}

	/**
	 * Searches the specified Collection for the minimum element.
	 * 
	 * @param collection
	 *            the Collection to search
	 * @return the minimum element in the Collection
	 * 
	 * @exception ClassCastException
	 *                when an element in the Collection does not implement
	 *                Comparable or elements cannot be compared to each other
	 */
	public static Object min(Collection collection) {
		Iterator it = collection.iterator();
		Comparable min = (Comparable) it.next();
		while (it.hasNext()) {
			Object next = it.next();
			if (min.compareTo(next) > 0)
				min = (Comparable) next;
		}
		return min;
	}

	/**
	 * Searches the specified Collection for the minimum element using the
	 * specified Comparator.
	 * 
	 * @param collection
	 *            the Collection to search
	 * @param comparator
	 *            the Comparator
	 * @return the minimum element in the Collection
	 * 
	 * @exception ClassCastException
	 *                when elements in the Collection cannot be compared to each
	 *                other using the Comparator
	 */
	public static Object min(Collection collection, Comparator comparator) {
		Iterator it = collection.iterator();
		Object min = it.next();
		while (it.hasNext()) {
			Object next = it.next();
			if (comparator.compare(min, next) > 0)
				min = next;
		}
		return min;
	}

	/**
	 * Answers a List containing the specified number of the specified element.
	 * The list cannot be modified.
	 * 
	 * @param length
	 *            the size of the returned List
	 * @param object
	 *            the element
	 * @return a List containing <code>length</code> copies of the element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>length < 0</code>
	 */
	public static List nCopies(final int length, Object object) {
		return new CopiesList(length, object);
	}

	/**
	 * Returns the supplied <code>List</code> with the order of its contained
	 * elements reversed. 
	 * 
	 * @param list
	 *            the List to reverse
	 * 
	 * @exception UnsupportedOperationException
	 *                when replacing an element in the List is not supported
	 */
	public static void reverse(List list) {
		int size = list.size();
		ListIterator front = list.listIterator();
		ListIterator back = list.listIterator(size);
		for (int i = 0; i < size / 2; i++) {
			Object temp = front.next();
			front.set(back.previous());
			back.set(temp);
		}
	}

	/**
	 * A Comparator which reverses the natural order of the elements.
	 * 
	 * @return a Comparator
	 */
	public static Comparator reverseOrder() {
		return new ReverseComparator();
	}

	/**
	 * Moves every element of the List to a random new position in the list.
	 * 
	 * @param list
	 *            the List to shuffle
	 * 
	 * @exception UnsupportedOperationException
	 *                when replacing an element in the List is not supported
	 */
	public static void shuffle(List list) {
		shuffle(list, new Random());
	}

	/**
	 * Moves every element of the List to a random new position in the list
	 * using the specified random number generator.
	 * 
	 * @param list
	 *            the List to shuffle
	 * @param random
	 *            the random number generator
	 * 
	 * @exception UnsupportedOperationException
	 *                when replacing an element in the List is not supported
	 */
	public static void shuffle(List list, Random random) {
		if (!(list instanceof RandomAccess)) {
			Object[] array = list.toArray();
			for (int i = array.length - 1; i > 0; i--) {
				int index = random.nextInt() % (i + 1);
				if (index < 0)
					index = -index;
				Object temp = array[i];
				array[i] = array[index];
				array[index] = temp;
			}

			int i = 0;
			ListIterator it = list.listIterator();
			while (it.hasNext()) {
				it.next();
				it.set(array[i++]);
			}
		} else {
			for (int i = list.size() - 1; i > 0; i--) {
				int index = random.nextInt() % (i + 1);
				if (index < 0)
					index = -index;
				list.set(index, list.set(i, list.get(index)));
			}
		}
	}

	/**
	 * Answers a Set containing the specified element. The set cannot be
	 * modified.
	 * 
	 * @param object
	 *            the element
	 * @return a Set containing the element
	 */
	public static Set singleton(Object object) {
		return new SingletonSet(object);
	}

	/**
	 * Answers a List containing the specified element. The list cannot be
	 * modified.
	 * 
	 * @param object
	 *            the element
	 * @return a List containing the element
	 */
	public static List singletonList(Object object) {
		return new SingletonList(object);
	}

	/**
	 * Answers a Map containing the specified key and value. The map cannot be
	 * modified.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @return a Map containing the key and value
	 */
	public static Map singletonMap(Object key, Object value) {
		return new SingletonMap(key, value);
	}

	/**
	 * Sorts the specified List in ascending order.
	 * 
	 * @param list
	 *            the List to be sorted
	 * 
	 * @exception ClassCastException
	 *                when an element in the List does not implement Comparable
	 *                or elements cannot be compared to each other
	 */
	public static void sort(List list) {
		Object[] array = list.toArray();
		Arrays.sort(array);
		int i = 0;
		ListIterator it = list.listIterator();
		while (it.hasNext()) {
			it.next();
			it.set(array[i++]);
		}
	}

	/**
	 * Sorts the specified List using the specified Comparator.
	 * 
	 * @param list
	 *            the List to be sorted
	 * @param comparator
	 *            the Comparator
	 * 
	 * @exception ClassCastException
	 *                when elements in the List cannot be compared to each other
	 *                using the Comparator
	 */
	public static void sort(List list, Comparator comparator) {
		Object[] array = list.toArray();
		Arrays.sort(array, comparator);
		int i = 0;
		ListIterator it = list.listIterator();
		while (it.hasNext()) {
			it.next();
			it.set(array[i++]);
		}
	}

	/**
	 * Swaps the elements of List <code>list</code> at indices
	 * <code>index1</code> and <code>index2</code>
	 * 
	 * @param list
	 *            the List to manipulate on
	 * @param index1
	 *            int position of the first element to swap with the element in
	 *            index2
	 * @param index2
	 *            int position of the other element
	 * 
	 * @exception IndexOutOfBoundsException
	 *                if index1 or index2 is out of range of this list
	 */
	public static void swap(List list, int index1, int index2) {
		if (list == null)
			throw new NullPointerException();
		if (index1 == index2)
			return;
		list.set(index2, list.set(index1, list.get(index2)));
	}

	/**
	 * Replaces all occurances of Object <code>obj</code> in <code>list</code>
	 * with <code>newObj</code>. If the <code>obj</code> is
	 * <code>null</code>, then all occurances of <code>null</code> is
	 * replaced with <code>newObj</code>.
	 * 
	 * @param list
	 *            the List to modify
	 * @param obj
	 *            the Object to find and replace occurances of.
	 * @param obj2
	 *            the Object to replace all occurances of <code>obj</code> in
	 *            <code>list</code>
	 * @return true, if at least one occurance of <code>obj</code> has been
	 *         found in <code>list</code>
	 * 
	 * @exception UnsupportedOperationException
	 *                if the list does not support setting elements
	 */
	public static boolean replaceAll(List list, Object obj, Object obj2) {
		int index;
		boolean found = false;

		while ((index = list.indexOf(obj)) > -1) {
			found = true;
			list.set(index, obj2);
		}
		return found;
	}

	/**
	 * Rotates the elements in List <code>list</code> by the distance<code>dist</code>
	 * <p>
	 * e.g. for a given list with elements [1, 2, 3, 4, 5, 6, 7, 8, 9, 0],
	 * calling rotate(list, 3) or rotate(list, -7) would modify the list to look
	 * like this: [8, 9, 0, 1, 2, 3, 4, 5, 6, 7]
	 * 
	 * @param list
	 * @param dist
	 *            It can be any integer: 0, positive, negative, larger than the
	 *            list size
	 */
	public static void rotate(List list, int dist) {
		int size = list.size();

		// normalize the distance
		if (dist > 0)
			dist = dist % size;
		else
			dist = size - (dist * (-1) % size);

		if (dist == 0 || dist == size)
			return;

		if (list instanceof RandomAccess) {
			// make sure each element gets juggled
			// with the element in the position it is supposed to go to
			Object temp = list.get(0);
			int index = 0, beginIndex = 0;
			for (int i = 0; i < size; i++) {
				index = (index + dist) % size;
				temp = list.set(index, temp);
				if (index == beginIndex) {
					index = ++beginIndex;
					temp = list.get(beginIndex);
				}
			}
		} else {
			int divideIndex = (size - dist) % size;
			List sublist1 = list.subList(0, divideIndex);
			List sublist2 = list.subList(divideIndex, size);
			reverse(sublist1);
			reverse(sublist2);
			reverse(list);
		}
	}

	/**
	 * Searches the <code>list</code> for <code>sublist</code> and answers
	 * the beginning index of the first occurance.
	 * <p>
	 * -1 is returned if the <code>sublist</code> does not exist in
	 * <code>list</code>
	 * 
	 * @param list
	 *            the List to search <code>sublist</code> in
	 * @param sublist
	 *            the List to search in <code>list</code>
	 * @return the beginning index of the first occurance of
	 *         <code>sublist</code> in <code>list</code>, or -1
	 */
	public static int indexOfSubList(List list, List sublist) {
		int size = list.size();
		int sublistSize = sublist.size();

		if (sublistSize > size)
			return -1;

		if (sublistSize == 0)
			return 0;

		// find the first element of sublist in the list to get a head start
		Object firstObj = sublist.get(0);
		int index = list.indexOf(firstObj);
		if (index == -1)
			return -1;

		while (index < size && (size - index >= sublistSize)) {
			ListIterator listIt = list.listIterator(index);

			if ((firstObj == null) ? listIt.next() == null : firstObj
					.equals(listIt.next())) {

				// iterate through the elements in sublist to see
				// if they are included in the same order in the list
				ListIterator sublistIt = sublist.listIterator(1);
				boolean difFound = false;
				while (sublistIt.hasNext()) {
					Object element = sublistIt.next();
					if (!listIt.hasNext())
						return -1;
					else {
						if ((element == null) ? listIt.next() != null
								: !element.equals(listIt.next())) {
							difFound = true;
							break;
						}
					}
				}
				// All elements of sublist are found in main list
				// starting from index.
				if (!difFound)
					return index;
			}
			// This was not the sequence we were looking for,
			// continue search for the firstObj in main list
			// at the position after index.
			index++;
		}
		return -1;
	}

	/**
	 * Searches the <code>list</code> for <code>sublist</code> and answers
	 * the beginning index of the last occurance.
	 * <p>
	 * -1 is returned if the <code>sublist</code> does not exist in
	 * <code>list</code>
	 * 
	 * @param list
	 *            the List to search <code>sublist</code> in
	 * @param sublist
	 *            the List to search in <code>list</code>
	 * @return the beginning index of the last occurance of <code>sublist</code>
	 *         in <code>list</code>, or -1
	 */
	public static int lastIndexOfSubList(List list, List sublist) {
		int sublistSize = sublist.size();
		int size = list.size();

		if (sublistSize > size)
			return -1;

		if (sublistSize == 0)
			return size;

		// find the last element of sublist in the list to get a head start
		Object lastObj = sublist.get(sublistSize - 1);
		int index = list.lastIndexOf(lastObj);

		while ((index > -1) && (index + 1 >= sublistSize)) {
			ListIterator listIt = list.listIterator(index + 1);

			if ((lastObj == null) ? listIt.previous() == null : lastObj
					.equals(listIt.previous())) {
				// iterate through the elements in sublist to see
				// if they are included in the same order in the list
				ListIterator sublistIt = sublist.listIterator(sublistSize - 1);
				boolean difFound = false;
				while (sublistIt.hasPrevious()) {
					Object element = sublistIt.previous();
					if (!listIt.hasPrevious())
						return -1;
					else {
						if ((element == null) ? listIt.previous() != null
								: !element.equals(listIt.previous())) {
							difFound = true;
							break;
						}
					}
				}
				// All elements of sublist are found in main list
				// starting from listIt.nextIndex().
				if (!difFound)
					return listIt.nextIndex();
			}
			// This was not the sequence we were looking for,
			// continue search for the lastObj in main list
			// at the position before index.
			index--;
		}
		return -1;
	}

	/**
	 * Answers an ArrayList with all the elements in the
	 * <code>enumeration</code>. The elements in the returned ArrayList are
	 * in the same order as in the <code>enumeration</code>.
	 * 
	 * @param enumeration
	 *            Enumeration
	 * @return and ArrayList
	 */
	public static ArrayList list(Enumeration enumeration) {
		ArrayList list = new ArrayList();
		while (enumeration.hasMoreElements()) {
			list.add(enumeration.nextElement());
		}
		return list;
	}

	/**
	 * Answers a wrapper on the specified Collection which synchronizes all
	 * access to the Collection.
	 * 
	 * @param collection
	 *            the Collection
	 * @return a synchronized Collection
	 */
	public static Collection synchronizedCollection(Collection collection) {
		return new SynchronizedCollection(collection);
	}

	/**
	 * Answers a wrapper on the specified List which synchronizes all access to
	 * the List.
	 * 
	 * @param list
	 *            the List
	 * @return a synchronized List
	 */
	public static List synchronizedList(List list) {
		if (list == null)
			throw new NullPointerException();
		if (list instanceof RandomAccess)
			return new SynchronizedRandomAccessList(list);
		else
			return new SynchronizedList(list);
	}

	/**
	 * Answers a wrapper on the specified Map which synchronizes all access to
	 * the Map.
	 * 
	 * @param map
	 *            the Map
	 * @return a synchronized Map
	 */
	public static Map synchronizedMap(Map map) {
		return new SynchronizedMap(map);
	}

	/**
	 * Answers a wrapper on the specified Set which synchronizes all access to
	 * the Set.
	 * 
	 * @param set
	 *            the Set
	 * @return a synchronized Set
	 */
	public static Set synchronizedSet(Set set) {
		return new SynchronizedSet(set);
	}

	/**
	 * Answers a wrapper on the specified SortedMap which synchronizes all
	 * access to the SortedMap.
	 * 
	 * @param map
	 *            the SortedMap
	 * @return a synchronized SortedMap
	 */
	public static SortedMap synchronizedSortedMap(SortedMap map) {
		return new SynchronizedSortedMap(map);
	}

	/**
	 * Answers a wrapper on the specified SortedSet which synchronizes all
	 * access to the SortedSet.
	 * 
	 * @param set
	 *            the SortedSet
	 * @return a synchronized SortedSet
	 */
	public static SortedSet synchronizedSortedSet(SortedSet set) {
		return new SynchronizedSortedSet(set);
	}

	/**
	 * Answers a wrapper on the specified Collection which throws an
	 * <code>UnsupportedOperationException</code> whenever an attempt is made
	 * to modify the Collection.
	 * 
	 * @param collection
	 *            the Collection
	 * @return an unmodifiable Collection
	 */
	public static Collection unmodifiableCollection(Collection collection) {
		return new UnmodifiableCollection(collection);
	}

	/**
	 * Answers a wrapper on the specified List which throws an
	 * <code>UnsupportedOperationException</code> whenever an attempt is made
	 * to modify the List.
	 * 
	 * @param list
	 *            the List
	 * @return an unmodifiable List
	 */
	public static List unmodifiableList(List list) {
		if (list == null)
			throw new NullPointerException();
		if (list instanceof RandomAccess)
			return new UnmodifiableRandomAccessList(list);
		else
			return new UnmodifiableList(list);
	}

	/**
	 * Answers a wrapper on the specified Map which throws an
	 * <code>UnsupportedOperationException</code> whenever an attempt is made
	 * to modify the Map.
	 * 
	 * @param map
	 *            the Map
	 * @return a unmodifiable Map
	 */
	public static Map unmodifiableMap(Map map) {
		return new UnmodifiableMap(map);
	}

	/**
	 * Answers a wrapper on the specified Set which throws an
	 * <code>UnsupportedOperationException</code> whenever an attempt is made
	 * to modify the Set.
	 * 
	 * @param set
	 *            the Set
	 * @return a unmodifiable Set
	 */
	public static Set unmodifiableSet(Set set) {
		return new UnmodifiableSet(set);
	}

	/**
	 * Answers a wrapper on the specified SortedMap which throws an
	 * <code>UnsupportedOperationException</code> whenever an attempt is made
	 * to modify the SortedMap.
	 * 
	 * @param map
	 *            the SortedMap
	 * @return a unmodifiable SortedMap
	 */
	public static SortedMap unmodifiableSortedMap(SortedMap map) {
		return new UnmodifiableSortedMap(map);
	}

	/**
	 * Answers a wrapper on the specified SortedSet which throws an
	 * <code>UnsupportedOperationException</code> whenever an attempt is made
	 * to modify the SortedSet.
	 * 
	 * @param set
	 *            the SortedSet
	 * @return a unmodifiable SortedSet
	 */
	public static SortedSet unmodifiableSortedSet(SortedSet set) {
		return new UnmodifiableSortedSet(set);
	}
}
