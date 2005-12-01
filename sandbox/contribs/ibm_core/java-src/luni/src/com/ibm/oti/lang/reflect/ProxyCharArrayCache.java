/* Copyright 2001, 2005 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.oti.lang.reflect;


class ProxyCharArrayCache {
	// to avoid using Enumerations, walk the individual tables skipping nulls
	char[] keyTable[];

	int valueTable[];

	int elementSize; // number of elements in the table

	int threshold;

	static boolean equals(char[] first, char[] second) {
		if (first == second) {
			return true;
		}
		if (first == null || second == null) {
			return false;
		}
		if (first.length != second.length) {
			return false;
		}

		for (int i = first.length; --i >= 0;)
			if (first[i] != second[i]) {
				return false;
			}
		return true;
	}

	ProxyCharArrayCache(int initialCapacity) {
		if (initialCapacity < 13) {
			initialCapacity = 13;
		}
		this.elementSize = 0;
		this.threshold = (int) (initialCapacity * 0.66f);
		this.keyTable = new char[initialCapacity][];
		this.valueTable = new int[initialCapacity];
	}

	int get(char[] key) {
		int index = hashCodeChar(key);
		while (keyTable[index] != null) {
			if (equals(keyTable[index], key))
				return valueTable[index];
			index = (index + 1) % keyTable.length;
		}
		return -1;
	}

	private int hashCodeChar(char[] val) {
		int length = val.length;
		int hash = 0;
		int n = 2; // number of characters skipped
		for (int i = 0; i < length; i += n)
			hash += val[i];
		return (hash & 0x7FFFFFFF) % keyTable.length;
	}

	int put(char[] key, int value) {
		int index = hashCodeChar(key);
		while (keyTable[index] != null) {
			if (equals(keyTable[index], key))
				return valueTable[index] = value;
			index = (index + 1) % keyTable.length;
		}
		keyTable[index] = key;
		valueTable[index] = value;

		// assumes the threshold is never equal to the size of the table
		if (++elementSize > threshold)
			rehash();
		return value;
	}

	private void rehash() {
		ProxyCharArrayCache newHashtable = new ProxyCharArrayCache(
				keyTable.length * 2);
		for (int i = keyTable.length; --i >= 0;)
			if (keyTable[i] != null)
				newHashtable.put(keyTable[i], valueTable[i]);

		this.keyTable = newHashtable.keyTable;
		this.valueTable = newHashtable.valueTable;
		this.threshold = newHashtable.threshold;
	}

	int size() {
		return elementSize;
	}

	public String toString() {
		int max = size();
		StringBuffer buf = new StringBuffer();
		buf.append("{");
		for (int i = 0; i < max; ++i) {
			if (keyTable[i] != null)
				buf.append(keyTable[i]).append("->").append(valueTable[i]);
			if (i < max)
				buf.append(", ");
		}
		buf.append("}");
		return buf.toString();
	}
}
