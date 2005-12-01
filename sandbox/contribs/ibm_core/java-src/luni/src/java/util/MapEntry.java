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


/**
 * MapEntry is an internal class which provides an implementation of Map.Entry.
 */
class MapEntry implements Map.Entry, Cloneable {
	
	Object key, value;

	interface Type {
		Object get(MapEntry entry);
	}

	MapEntry(Object theKey) {
		key = theKey;
	}

	MapEntry(Object theKey, Object theValue) {
		key = theKey;
		value = theValue;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object instanceof Map.Entry) {
			Map.Entry entry = (Map.Entry) object;
			return (key == null ? entry.getKey() == null : key.equals(entry
					.getKey()))
					&& (value == null ? entry.getValue() == null : value
							.equals(entry.getValue()));
		} else
			return false;
	}

	public Object getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public int hashCode() {
		return (key == null ? 0 : key.hashCode())
				^ (value == null ? 0 : value.hashCode());
	}

	public Object setValue(Object object) {
		Object result = value;
		value = object;
		return result;
	}
}
