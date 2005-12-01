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
import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * LinkedList is an implementation of List, backed by a linked list. All
 * optional operations are supported, adding, removing and replacing. The
 * elements can be any objects.
 */
public class LinkedList extends AbstractSequentialList implements List,
		Cloneable, Serializable {
	
	static final long serialVersionUID = 876323262645176354L;

	transient int size = 0;

	transient Link voidLink;

	private static final class Link {
		Object data;

		Link previous, next;

		Link(Object o, Link p, Link n) {
			data = o;
			previous = p;
			next = n;
		}
	}

	private static final class LinkIterator implements ListIterator {
		int pos, expectedModCount;

		LinkedList list;

		Link link, lastLink;

		LinkIterator(LinkedList object, int location) {
			list = object;
			expectedModCount = list.modCount;
			if (0 <= location && location <= list.size) {
				// pos ends up as -1 if list is empty, it ranges from -1 to
				// list.size - 1
				// if link == voidLink then pos must == -1
				link = list.voidLink;
				if (location < list.size / 2) {
					for (pos = -1; pos + 1 < location; pos++)
						link = link.next;
				} else {
					for (pos = list.size; pos >= location; pos--)
						link = link.previous;
				}
			} else
				throw new IndexOutOfBoundsException();
		}

		public void add(Object object) {
			if (expectedModCount == list.modCount) {
				Link next = link.next;
				Link newLink = new Link(object, link, next);
				link.next = newLink;
				next.previous = newLink;
				link = newLink;
				lastLink = null;
				pos++;
				expectedModCount++;
				list.size++;
				list.modCount++;
			} else
				throw new ConcurrentModificationException();
		}

		public boolean hasNext() {
			return link.next != list.voidLink;
		}

		public boolean hasPrevious() {
			return link != list.voidLink;
		}

		public Object next() {
			if (expectedModCount == list.modCount) {
				LinkedList.Link next = link.next;
				if (next != list.voidLink) {
					lastLink = link = next;
					pos++;
					return link.data;
				} else
					throw new NoSuchElementException();
			} else
				throw new ConcurrentModificationException();
		}

		public int nextIndex() {
			return pos + 1;
		}

		public Object previous() {
			if (expectedModCount == list.modCount) {
				if (link != list.voidLink) {
					lastLink = link;
					link = link.previous;
					pos--;
					return lastLink.data;
				} else
					throw new NoSuchElementException();
			} else
				throw new ConcurrentModificationException();
		}

		public int previousIndex() {
			return pos;
		}

		public void remove() {
			if (expectedModCount == list.modCount) {
				if (lastLink != null) {
					Link next = lastLink.next;
					Link previous = lastLink.previous;
					next.previous = previous;
					previous.next = next;
					if (lastLink == link)
						pos--;
					link = previous;
					lastLink = null;
					expectedModCount++;
					list.size--;
					list.modCount++;
				} else
					throw new IllegalStateException();
			} else
				throw new ConcurrentModificationException();
		}

		public void set(Object object) {
			if (expectedModCount == list.modCount) {
				if (lastLink != null)
					lastLink.data = object;
				else
					throw new IllegalStateException();
			} else
				throw new ConcurrentModificationException();
		}
	}

	/**
	 * Contructs a new empty instance of LinkedList.
	 * 
	 */
	public LinkedList() {
		voidLink = new Link(null, null, null);
		voidLink.previous = voidLink;
		voidLink.next = voidLink;
	}

	/**
	 * Constructs a new instance of <code>LinkedList</code> that holds 
	 * all of the elements contained in the supplied <code>collection</code>
	 * argument. The order of the elements in this new <code>LinkedList</code> 
	 * will be determined by the iteration order of <code>collection</code>. 
	 * 
	 * @param collection
	 *            the collection of elements to add
	 */
	public LinkedList(Collection collection) {
		this();
		addAll(collection);
	}

	/**
	 * Inserts the specified object into this LinkedList at the specified
	 * location. The object is inserted before any previous element at the
	 * specified location. If the location is equal to the size of this
	 * LinkedList, the object is added at the end.
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
		if (0 <= location && location <= size) {
			Link link = voidLink;
			if (location < (size / 2)) {
				for (int i = 0; i <= location; i++)
					link = link.next;
			} else {
				for (int i = size; i > location; i--)
					link = link.previous;
			}
			Link previous = link.previous;
			Link newLink = new Link(object, previous, link);
			previous.next = newLink;
			link.previous = newLink;
			size++;
			modCount++;
		} else
			throw new IndexOutOfBoundsException();
	}

	/**
	 * Adds the specified object at the end of this LinkedList.
	 * 
	 * @param object
	 *            the object to add
	 * @return true
	 */
	public boolean add(Object object) {
		// Cannot call addLast() as sublasses can override
		Link oldLast = voidLink.previous;
		Link newLink = new Link(object, oldLast, voidLink);
		voidLink.previous = newLink;
		oldLast.next = newLink;
		size++;
		modCount++;
		return true;
	}

	/**
	 * Inserts the objects in the specified Collection at the specified location
	 * in this LinkedList. The objects are added in the order they are returned
	 * from the <code>Collection</code> iterator.
	 * 
	 * @param location
	 *            the index at which to insert
	 * @param collection
	 *            the Collection of objects
	 * @return true if this LinkedList is modified, false otherwise
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public boolean addAll(int location, Collection collection) {
		int adding = collection.size();
		if (adding == 0)
			return false;
		if (0 <= location && location <= size) {
			Link previous = voidLink;
			if (location < (size / 2)) {
				for (int i = 0; i < location; i++)
					previous = previous.next;
			} else {
				for (int i = size; i >= location; i--)
					previous = previous.previous;
			}
			Link next = previous.next;

			Iterator it = collection.iterator();
			while (it.hasNext()) {
				Link newLink = new Link(it.next(), previous, null);
				previous.next = newLink;
				previous = newLink;
			}
			previous.next = next;
			next.previous = previous;
			size += adding;
			modCount++;
			return true;
		} else
			throw new IndexOutOfBoundsException();
	}

	/**
	 * Adds the objects in the specified Collection to this LinkedList.
	 * 
	 * @param collection
	 *            the Collection of objects
	 * @return true if this LinkedList is modified, false otherwise
	 */
	public boolean addAll(Collection collection) {
		int adding = collection.size();
		if (adding == 0)
			return false;
		Link previous = voidLink.previous;
		Iterator it = collection.iterator();
		while (it.hasNext()) {
			Link newLink = new Link(it.next(), previous, null);
			previous.next = newLink;
			previous = newLink;
		}
		previous.next = voidLink;
		voidLink.previous = previous;
		size += adding;
		modCount++;
		return true;
	}

	/**
	 * Adds the specified object at the begining of this LinkedList.
	 * 
	 * @param object
	 *            the object to add
	 */
	public void addFirst(Object object) {
		Link oldFirst = voidLink.next;
		Link newLink = new Link(object, voidLink, oldFirst);
		voidLink.next = newLink;
		oldFirst.previous = newLink;
		size++;
		modCount++;
	}

	/**
	 * Adds the specified object at the end of this LinkedList.
	 * 
	 * @param object
	 *            the object to add
	 */
	public void addLast(Object object) {
		Link oldLast = voidLink.previous;
		Link newLink = new Link(object, oldLast, voidLink);
		voidLink.previous = newLink;
		oldLast.next = newLink;
		size++;
		modCount++;
	}

	/**
	 * Removes all elements from this LinkedList, leaving it empty.
	 * 
	 * @see List#isEmpty
	 * @see #size
	 */
	public void clear() {
		if (size > 0) {
			size = 0;
			voidLink.next = voidLink;
			voidLink.previous = voidLink;
			modCount++;
		}
	}

	/**
	 * Answers a new LinkedList with the same elements and size as this
	 * LinkedList.
	 * 
	 * @return a shallow copy of this LinkedList
	 * 
	 * @see java.lang.Cloneable
	 */
	public Object clone() {
		return new LinkedList(this);
	}

	/**
	 * Searches this LinkedList for the specified object.
	 * 
	 * @param object
	 *            the object to search for
	 * @return true if <code>object</code> is an element of this LinkedList,
	 *         false otherwise
	 */
	public boolean contains(Object object) {
		Link link = voidLink.next;
		if (object != null) {
			while (link != voidLink) {
				if (object.equals(link.data))
					return true;
				link = link.next;
			}
		} else {
			while (link != voidLink) {
				if (link.data == null)
					return true;
				link = link.next;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.List#get(int)
	 */
	public Object get(int location) {
		if (0 <= location && location < size) {
			Link link = voidLink;
			if (location < (size / 2)) {
				for (int i = 0; i <= location; i++)
					link = link.next;
			} else {
				for (int i = size; i > location; i--)
					link = link.previous;
			}
			return link.data;
		} else
			throw new IndexOutOfBoundsException();
	}

	/**
	 * Answers the first element in this LinkedList.
	 * 
	 * @return the first element
	 * 
	 * @exception NoSuchElementException
	 *                when this LinkedList is empty
	 */
	public Object getFirst() {
		Link first = voidLink.next;
		if (first != voidLink)
			return first.data;
		throw new NoSuchElementException();
	}

	/**
	 * Answers the last element in this LinkedList.
	 * 
	 * @return the last element
	 * 
	 * @exception NoSuchElementException
	 *                when this LinkedList is empty
	 */
	public Object getLast() {
		Link last = voidLink.previous;
		if (last != voidLink)
			return last.data;
		throw new NoSuchElementException();
	}

	/**
	 * Searches this LinkedList for the specified object and returns the index
	 * of the first occurrence.
	 * 
	 * @param object
	 *            the object to search for
	 * @return the index of the first occurrence of the object
	 */
	public int indexOf(Object object) {
		int pos = 0;
		Link link = voidLink.next;
		if (object != null) {
			while (link != voidLink) {
				if (object.equals(link.data))
					return pos;
				link = link.next;
				pos++;
			}
		} else {
			while (link != voidLink) {
				if (link.data == null)
					return pos;
				link = link.next;
				pos++;
			}
		}
		return -1;
	}

	/**
	 * Searches this LinkedList for the specified object and returns the index
	 * of the last occurrence.
	 * 
	 * @param object
	 *            the object to search for
	 * @return the index of the last occurrence of the object
	 */
	public int lastIndexOf(Object object) {
		int pos = size;
		Link link = voidLink.previous;
		if (object != null) {
			while (link != voidLink) {
				pos--;
				if (object.equals(link.data))
					return pos;
				link = link.previous;
			}
		} else {
			while (link != voidLink) {
				pos--;
				if (link.data == null)
					return pos;
				link = link.previous;
			}
		}
		return -1;
	}

	/**
	 * Answers a ListIterator on the elements of this LinkedList. The elements
	 * are iterated in the same order that they occur in the LinkedList. The
	 * iteration starts at the specified location.
	 * 
	 * @param location
	 *            the index at which to start the iteration
	 * @return a ListIterator on the elements of this LinkedList
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 * 
	 * @see ListIterator
	 */
	public ListIterator listIterator(int location) {
		return new LinkIterator(this, location);
	}

	/**
	 * Removes the object at the specified location from this LinkedList.
	 * 
	 * @param location
	 *            the index of the object to remove
	 * @return the removed object
	 * 
	 * @exception IndexOutOfBoundsException
	 *                when <code>location < 0 || >= size()</code>
	 */
	public Object remove(int location) {
		if (0 <= location && location < size) {
			Link link = voidLink;
			if (location < (size / 2)) {
				for (int i = 0; i <= location; i++)
					link = link.next;
			} else {
				for (int i = size; i > location; i--)
					link = link.previous;
			}
			Link previous = link.previous;
			Link next = link.next;
			previous.next = next;
			next.previous = previous;
			size--;
			modCount++;
			return link.data;
		} else
			throw new IndexOutOfBoundsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object object) {
		Link link = voidLink.next;
		if (object != null) {
			while (link != voidLink && !object.equals(link.data))
				link = link.next;
		} else {
			while (link != voidLink && link.data != null)
				link = link.next;
		}
		if (link == voidLink)
			return false;
		Link next = link.next;
		Link previous = link.previous;
		previous.next = next;
		next.previous = previous;
		size--;
		modCount++;
		return true;
	}

	/**
	 * Removes the first object from this LinkedList.
	 * 
	 * @return the removed object
	 * 
	 * @exception NoSuchElementException
	 *                when this LinkedList is empty
	 */
	public Object removeFirst() {
		Link first = voidLink.next;
		if (first != voidLink) {
			Link next = first.next;
			voidLink.next = next;
			next.previous = voidLink;
			size--;
			modCount++;
			return first.data;
		} else
			throw new NoSuchElementException();
	}

	/**
	 * Removes the last object from this LinkedList.
	 * 
	 * @return the removed object
	 * 
	 * @exception NoSuchElementException
	 *                when this LinkedList is empty
	 */
	public Object removeLast() {
		Link last = voidLink.previous;
		if (last != voidLink) {
			Link previous = last.previous;
			voidLink.previous = previous;
			previous.next = voidLink;
			size--;
			modCount++;
			return last.data;
		} else
			throw new NoSuchElementException();
	}

	/**
	 * Replaces the element at the specified location in this LinkedList with
	 * the specified object.
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
		if (0 <= location && location < size) {
			Link link = voidLink;
			if (location < (size / 2)) {
				for (int i = 0; i <= location; i++)
					link = link.next;
			} else {
				for (int i = size; i > location; i--)
					link = link.previous;
			}
			Object result = link.data;
			link.data = object;
			return result;
		} else
			throw new IndexOutOfBoundsException();
	}

	/**
	 * Answers the number of elements in this LinkedList.
	 * 
	 * @return the number of elements in this LinkedList
	 */
	public int size() {
		return size;
	}

	/**
	 * Answers a new array containing all elements contained in this LinkedList.
	 * 
	 * @return an array of the elements from this LinkedList
	 */
	public Object[] toArray() {
		int index = 0;
		Object[] contents = new Object[size];
		Link link = voidLink.next;
		while (link != voidLink) {
			contents[index++] = link.data;
			link = link.next;
		}
		return contents;
	}

	/**
	 * Answers an array containing all elements contained in this LinkedList. If
	 * the specified array is large enough to hold the elements, the specified
	 * array is used, otherwise an array of the same type is created. If the
	 * specified array is used and is larger than this LinkedList, the array
	 * element following the collection elements is set to null.
	 * 
	 * @param contents
	 *            the array
	 * @return an array of the elements from this LinkedList
	 * 
	 * @exception ArrayStoreException
	 *                when the type of an element in this LinkedList cannot be
	 *                stored in the type of the specified array
	 */
	public Object[] toArray(Object[] contents) {
		int index = 0;
		if (size > contents.length)
			contents = (Object[]) Array.newInstance(contents.getClass()
					.getComponentType(), size);
		Link link = voidLink.next;
		while (link != voidLink) {
			contents[index++] = link.data;
			link = link.next;
		}
		if (index < contents.length)
			contents[index] = null;
		return contents;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeInt(size);
		Iterator it = iterator();
		while (it.hasNext())
			stream.writeObject(it.next());
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		size = stream.readInt();
		voidLink = new Link(null, null, null);
		Link link = voidLink;
		for (int i = size; --i >= 0;) {
			Link nextLink = new Link(stream.readObject(), link, null);
			link.next = nextLink;
			link = nextLink;
		}
		link.next = voidLink;
		voidLink.previous = link;
	}
}
