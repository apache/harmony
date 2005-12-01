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


import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * Arrays contains static methods which operate on arrays.
 */
public class Arrays {

	private static class ArrayList extends AbstractList implements List,
			Serializable, RandomAccess {

		static final long serialVersionUID = -2764017481108945198L;

		private Object[] a;

		ArrayList(Object[] storage) {
			if (storage == null) {
				throw new NullPointerException();
			}
			a = storage;
		}

		public boolean contains(Object object) {
			if (object != null) {
				for (int i = 0; i < a.length; i++)
					if (object.equals(a[i]))
						return true;
			} else {
				for (int i = 0; i < a.length; i++)
					if (a[i] == null)
						return true;
			}
			return false;
		}

		public Object get(int location) {
			try {
				return a[location];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IndexOutOfBoundsException();
			}
		}

		public int indexOf(Object object) {
			if (object != null) {
				for (int i = 0; i < a.length; i++)
					if (object.equals(a[i]))
						return i;
			} else {
				for (int i = 0; i < a.length; i++)
					if (a[i] == null)
						return i;
			}
			return -1;
		}

		public int lastIndexOf(Object object) {
			if (object != null) {
				for (int i = a.length - 1; i >= 0; i--)
					if (object.equals(a[i]))
						return i;
			} else {
				for (int i = a.length - 1; i >= 0; i--)
					if (a[i] == null)
						return i;
			}
			return -1;
		}

		public Object set(int location, Object object) {
			try {
				Object result = a[location];
				a[location] = object;
				return result;
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IndexOutOfBoundsException();
			} catch (ArrayStoreException e) {
				throw new ClassCastException();
			}
		}

		public int size() {
			return a.length;
		}

		public Object[] toArray() {
			return (Object[]) a.clone();
		}

		public Object[] toArray(Object[] contents) {
			int size = size();
			if (size > contents.length)
				contents = (Object[]) Array.newInstance(contents.getClass()
						.getComponentType(), size);
			System.arraycopy(a, 0, contents, 0, size);
			if (size < contents.length)
				contents[size] = null;
			return contents;
		}
	}

	private Arrays() {
		/*empty*/
	}

	/**
	 * Answers a List on the objects in the specified array. The size of the
	 * List cannot be modified, i.e. adding and removing are unsupported, but
	 * the elements can be set. Setting an element modifies the underlying
	 * array.
	 * 
	 * @param array
	 *            the array
	 * @return a List on the specified array
	 */
	public static List asList(Object[] array) {
		return new ArrayList(array);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted byte array to search
	 * @param value
	 *            the byte element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(byte[] array, byte value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;

		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted char array to search
	 * @param value
	 *            the char element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(char[] array, char value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;
		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted double array to search
	 * @param value
	 *            the double element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(double[] array, double value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;
		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted float array to search
	 * @param value
	 *            the float element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(float[] array, float value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;
		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted int array to search
	 * @param value
	 *            the int element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(int[] array, int value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;
		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted long array to search
	 * @param value
	 *            the long element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(long[] array, long value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;
		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted Object array to search
	 * @param object
	 *            the Object element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 * 
	 * @exception ClassCastException
	 *                when an element in the array or the seach element does not
	 *                implement Comparable, or cannot be compared to each other
	 */
	public static int binarySearch(Object[] array, Object object) {
		Comparable key = (Comparable) object;
		int low = 0, mid = 0, high = array.length - 1, result = 0;
		while (low <= high) {
			mid = (low + high) >> 1;
			if ((result = key.compareTo(array[mid])) > 0)
				low = mid + 1;
			else if (result == 0)
				return mid;
			else
				high = mid - 1;
		}
		return -mid - (result <= 0 ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array using the Comparator to compare elements.
	 * 
	 * @param array
	 *            the sorted char array to search
	 * @param object
	 *            the char element to find
	 * @param comparator
	 *            the Comparator
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 * 
	 * @exception ClassCastException
	 *                when an element in the array and the seach element cannot
	 *                be compared to each other using the Comparator
	 */
	public static int binarySearch(Object[] array, Object object,
			Comparator comparator) {
		int low = 0, mid = 0, high = array.length - 1, result = 0;
		while (low <= high) {
			mid = (low + high) >> 1;
			if ((result = comparator.compare(array[mid], object)) < 0)
				low = mid + 1;
			else if (result == 0)
				return mid;
			else
				high = mid - 1;
		}
		return -mid - (result >= 0 ? 1 : 2);
	}

	/**
	 * Performs a binary search for the specified element in the specified
	 * sorted array.
	 * 
	 * @param array
	 *            the sorted short array to search
	 * @param value
	 *            the short element to find
	 * @return the non-negative index of the element, or a negative index which
	 *         is the -index - 1 where the element would be inserted
	 */
	public static int binarySearch(short[] array, short value) {
		int low = 0, mid = -1, high = array.length - 1;
		while (low <= high) {
			mid = (low + high) >> 1;
			if (value > array[mid])
				low = mid + 1;
			else if (value == array[mid])
				return mid;
			else
				high = mid - 1;
		}
		if (mid < 0)
			return -1;
		return -mid - (value < array[mid] ? 1 : 2);
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the byte array to fill
	 * @param value
	 *            the byte element
	 */
	public static void fill(byte[] array, byte value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the byte array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the byte element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(byte[] array, int start, int end, byte value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the short array to fill
	 * @param value
	 *            the short element
	 */
	public static void fill(short[] array, short value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the short array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the short element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(short[] array, int start, int end, short value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the char array to fill
	 * @param value
	 *            the char element
	 */
	public static void fill(char[] array, char value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the char array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the char element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(char[] array, int start, int end, char value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the int array to fill
	 * @param value
	 *            the int element
	 */
	public static void fill(int[] array, int value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the int array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the int element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(int[] array, int start, int end, int value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the long array to fill
	 * @param value
	 *            the long element
	 */
	public static void fill(long[] array, long value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the long array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the long element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(long[] array, int start, int end, long value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the float array to fill
	 * @param value
	 *            the float element
	 */
	public static void fill(float[] array, float value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the float array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the float element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(float[] array, int start, int end, float value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the float array to fill
	 * @param value
	 *            the float element
	 */
	public static void fill(double[] array, double value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the double array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the double element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(double[] array, int start, int end, double value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the boolean array to fill
	 * @param value
	 *            the boolean element
	 */
	public static void fill(boolean[] array, boolean value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the boolean array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the boolean element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(boolean[] array, int start, int end, boolean value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified array with the specified element.
	 * 
	 * @param array
	 *            the Object array to fill
	 * @param value
	 *            the Object element
	 */
	public static void fill(Object[] array, Object value) {
		for (int i = 0; i < array.length; i++)
			array[i] = value;
	}

	/**
	 * Fills the specified range in the array with the specified element.
	 * 
	 * @param array
	 *            the Object array to fill
	 * @param start
	 *            the first index to fill
	 * @param end
	 *            the last + 1 index to fill
	 * @param value
	 *            the Object element
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void fill(Object[] array, int start, int end, Object value) {
		// Check for null first
		int length = array.length;
		if (start > end)
			throw new IllegalArgumentException();
		if (start < 0 || end > length)
			throw new ArrayIndexOutOfBoundsException();
		for (int i = start; i < end; i++)
			array[i] = value;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first byte array
	 * @param array2
	 *            the second byte array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(byte[] array1, byte[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first short array
	 * @param array2
	 *            the second short array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(short[] array1, short[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first char array
	 * @param array2
	 *            the second char array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(char[] array1, char[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first int array
	 * @param array2
	 *            the second int array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(int[] array1, int[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first long array
	 * @param array2
	 *            the second long array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(long[] array1, long[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first float array
	 * @param array2
	 *            the second float array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(float[] array1, float[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first double array
	 * @param array2
	 *            the second double array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(double[] array1, double[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first boolean array
	 * @param array2
	 *            the second boolean array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(boolean[] array1, boolean[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Compares the two arrays.
	 * 
	 * @param array1
	 *            the first Object array
	 * @param array2
	 *            the second Object array
	 * @return true when the arrays have the same length and the elements at
	 *         each index in the two arrays are equal, false otherwise
	 */
	public static boolean equals(Object[] array1, Object[] array2) {
		if (array1 == array2)
			return true;
		if (array1 == null || array2 == null || array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			Object e1 = array1[i], e2 = array2[i];
			if (!(e1 == null ? e2 == null : e1.equals(e2)))
				return false;
		}
		return true;
	}

	private static int med3(byte[] array, int a, int b, int c) {
		byte x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	private static int med3(char[] array, int a, int b, int c) {
		char x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	private static int med3(double[] array, int a, int b, int c) {
		double x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	private static int med3(float[] array, int a, int b, int c) {
		float x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	private static int med3(int[] array, int a, int b, int c) {
		int x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	private static int med3(long[] array, int a, int b, int c) {
		long x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	private static int med3(short[] array, int a, int b, int c) {
		short x = array[a], y = array[b], z = array[c];
		return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
				: a));
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the byte array to be sorted
	 */
	public static void sort(byte[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the byte array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(byte[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, byte[] array) {
		byte temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		byte partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the char array to be sorted
	 */
	public static void sort(char[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the char array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(char[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, char[] array) {
		char temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		char partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the double array to be sorted
	 */
	public static void sort(double[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the double array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(double[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, double[] array) {
		double temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		double partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the float array to be sorted
	 */
	public static void sort(float[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the float array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(float[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, float[] array) {
		float temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		float partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the int array to be sorted
	 */
	public static void sort(int[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the int array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(int[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, int[] array) {
		int temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		int partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the long array to be sorted
	 */
	public static void sort(long[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the long array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(long[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, long[] array) {
		long temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		long partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the Object array to be sorted
	 * 
	 * @exception ClassCastException
	 *                when an element in the array does not implement Comparable
	 *                or elements cannot be compared to each other
	 */
	public static void sort(Object[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the Object array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception ClassCastException
	 *                when an element in the array does not implement Comparable
	 *                or elements cannot be compared to each other
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(Object[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, Object[] array) {
		int middle = (start + end) / 2;
		if (start + 1 < middle)
			sort(start, middle, array);
		if (middle + 1 < end)
			sort(middle, end, array);
		if (start + 1 >= end)
			return; // this case can only happen when this method is called by
					// the user
		if (((Comparable) array[middle - 1]).compareTo(array[middle]) <= 0)
			return;
		if (start + 2 == end) {
			Object temp = array[start];
			array[start] = array[middle];
			array[middle] = temp;
			return;
		}
		int i1 = start, i2 = middle, i3 = 0;
		Object[] merge = new Object[end - start];
		while (i1 < middle && i2 < end) {
			merge[i3++] = ((Comparable) array[i1]).compareTo(array[i2]) <= 0 ? array[i1++]
					: array[i2++];
		}
		if (i1 < middle)
			System.arraycopy(array, i1, merge, i3, middle - i1);
		System.arraycopy(merge, 0, array, start, i2 - start);
	}

	/**
	 * Sorts the specified range in the array using the specified Comparator.
	 * 
	 * @param array
	 *            the Object array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * @param comparator
	 *            the Comparator
	 * 
	 * @exception ClassCastException
	 *                when elements in the array cannot be compared to each
	 *                other using the Comparator
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(Object[] array, int start, int end,
			Comparator comparator) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array, comparator);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, Object[] array,
			Comparator comparator) {
		int middle = (start + end) / 2;
		if (start + 1 < middle)
			sort(start, middle, array, comparator);
		if (middle + 1 < end)
			sort(middle, end, array, comparator);
		if (start + 1 >= end)
			return; // this case can only happen when this method is called by
					// the user
		if (comparator.compare(array[middle - 1], array[middle]) <= 0)
			return;
		if (start + 2 == end) {
			Object temp = array[start];
			array[start] = array[middle];
			array[middle] = temp;
			return;
		}
		int i1 = start, i2 = middle, i3 = 0;
		Object[] merge = new Object[end - start];
		while (i1 < middle && i2 < end) {
			merge[i3++] = comparator.compare(array[i1], array[i2]) <= 0 ? array[i1++]
					: array[i2++];
		}
		if (i1 < middle)
			System.arraycopy(array, i1, merge, i3, middle - i1);
		System.arraycopy(merge, 0, array, start, i2 - start);
	}

	/**
	 * Sorts the specified array using the specified Comparator.
	 * 
	 * @param array
	 *            the Object array to be sorted
	 * @param comparator
	 *            the Comparator
	 * 
	 * @exception ClassCastException
	 *                when elements in the array cannot be compared to each
	 *                other using the Comparator
	 */
	public static void sort(Object[] array, Comparator comparator) {
		sort(0, array.length, array, comparator);
	}

	/**
	 * Sorts the specified array in ascending order.
	 * 
	 * @param array
	 *            the short array to be sorted
	 */
	public static void sort(short[] array) {
		sort(0, array.length, array);
	}

	/**
	 * Sorts the specified range in the array in ascending order.
	 * 
	 * @param array
	 *            the short array to be sorted
	 * @param start
	 *            the start index to sort
	 * @param end
	 *            the last + 1 index to sort
	 * 
	 * @exception IllegalArgumentException
	 *                when <code>start > end</code>
	 * @exception ArrayIndexOutOfBoundsException
	 *                when <code>start < 0</code> or
	 *                <code>end > array.size()</code>
	 */
	public static void sort(short[] array, int start, int end) {
		if (start >= 0 && end <= array.length) {
			if (start <= end)
				sort(start, end, array);
			else
				throw new IllegalArgumentException();
		} else
			throw new ArrayIndexOutOfBoundsException();
	}

	private static void sort(int start, int end, short[] array) {
		short temp;
		int length = end - start;
		if (length < 7) {
			for (int i = start + 1; i < end; i++)
				for (int j = i; j > start && array[j - 1] > array[j]; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
			return;
		}
		int middle = (start + end) / 2;
		if (length > 7) {
			int bottom = start;
			int top = end - 1;
			if (length > 40) {
				length /= 8;
				bottom = med3(array, bottom, bottom + length, bottom
						+ (2 * length));
				middle = med3(array, middle - length, middle, middle + length);
				top = med3(array, top - (2 * length), top - length, top);
			}
			middle = med3(array, bottom, middle, top);
		}
		short partionValue = array[middle];
		int a, b, c, d;
		a = b = start;
		c = d = end - 1;
		while (true) {
			while (b <= c && array[b] <= partionValue) {
				if (array[b] == partionValue) {
					temp = array[a];
					array[a++] = array[b];
					array[b] = temp;
				}
				b++;
			}
			while (c >= b && array[c] >= partionValue) {
				if (array[c] == partionValue) {
					temp = array[c];
					array[c] = array[d];
					array[d--] = temp;
				}
				c--;
			}
			if (b > c)
				break;
			temp = array[b];
			array[b++] = array[c];
			array[c--] = temp;
		}
		length = a - start < b - a ? a - start : b - a;
		int l = start;
		int h = b - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		length = d - c < end - 1 - d ? d - c : end - 1 - d;
		l = b;
		h = end - length;
		while (length-- > 0) {
			temp = array[l];
			array[l++] = array[h];
			array[h++] = temp;
		}
		if ((length = b - a) > 0)
			sort(start, start + length, array);
		if ((length = d - c) > 0)
			sort(end - length, end, array);
	}
}
