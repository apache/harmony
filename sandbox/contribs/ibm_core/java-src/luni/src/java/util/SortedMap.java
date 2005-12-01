/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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
 * SortedMap is a Map where the iterators sequence in order of the sorted keys.
 */
public interface SortedMap extends Map {
	
	/**
	 * Answers the Comparator used to compare elements in this SortedMap.
	 * 
	 * @return a Comparator or null if the natural order is used
	 */
	public Comparator comparator();

	/**
	 * Answer the first sorted key in this SortedMap.
	 * 
	 * @return the first sorted key
	 * 
	 * @exception NoSuchElementException
	 *                when this SortedMap is empty
	 */
	public Object firstKey();

	/**
	 * Answers a <code>SortedMap</code> of the specified portion of this
	 * <code>SortedMap</code> which contains keys less than the end key. Users
	 * should be aware that the return value is actually backed by this
	 * <code>SortedMap</code>. Hence any modifications made to one will be
	 * immediately visible to the other.
	 * 
	 * @param endKey
	 *            the end key
	 * @return a submap where the keys are less than <code>endKey</code>
	 * 
	 * @exception ClassCastException
	 *                when the class of the end key is inappropriate for this
	 *                SubMap
	 * @exception NullPointerException
	 *                when the end key is null and this SortedMap does not
	 *                support null keys
	 */
	public SortedMap headMap(Object endKey);

	/**
	 * Answers the last sorted key in this SortedMap.
	 * 
	 * @return the last sorted key
	 * 
	 * @exception NoSuchElementException
	 *                when this SortedMap is empty
	 */
	public Object lastKey();

	/**
	 * Answers a SortedMap of the specified portion of this SortedMap which
	 * contains keys greater or equal to the start key but less than the end
	 * key. Users should be aware that the return value is actually backed by
	 * this <code>SortedMap</code>. Hence any modifications made to one will
	 * be immediately visible to the other.
	 * 
	 * @param startKey
	 *            the start key
	 * @param endKey
	 *            the end key
	 * @return a submap where the keys are greater or equal to
	 *         <code>startKey</code> and less than <code>endKey</code>
	 * 
	 * @exception ClassCastException
	 *                when the class of the start or end key is inappropriate
	 *                for this SubMap
	 * @exception NullPointerException
	 *                when the start or end key is null and this SortedMap does
	 *                not support null keys
	 * @exception IllegalArgumentException
	 *                when the start key is greater than the end key
	 */
	public SortedMap subMap(Object startKey, Object endKey);

	/**
	 * Answers a SortedMap of the specified portion of this SortedMap which
	 * contains keys greater or equal to the start key. The returned SortedMap
	 * is backed by this SortedMap so changes to one are reflected by the other.
	 * 
	 * @param startKey
	 *            the start key
	 * @return a submap where the keys are greater or equal to
	 *         <code>startKey</code>
	 * 
	 * @exception ClassCastException
	 *                when the class of the start key is inappropriate for this
	 *                SubMap
	 * @exception NullPointerException
	 *                when the start key is null and this SortedMap does not
	 *                support null keys
	 */
	public SortedMap tailMap(Object startKey);
}
