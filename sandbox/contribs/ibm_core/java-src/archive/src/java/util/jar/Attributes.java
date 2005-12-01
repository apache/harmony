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

package java.util.jar;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Attributes class is used to store values for Manifest entries. Attributes
 * keys are genrerally instances of Attributes.Name. Values associated with
 * Attributes keys are of type String.
 */
public class Attributes implements Cloneable, Map {

	protected Map map;

	public static class Name {
		private String name;

		private int hashCode;

		public static final Name CLASS_PATH = new Name("Class-Path", false);

		public static final Name MANIFEST_VERSION = new Name(
				"Manifest-Version", false);

		public static final Name MAIN_CLASS = new Name("Main-Class", false);

		public static final Name SIGNATURE_VERSION = new Name(
				"Signature-Version", false);

		public static final Name CONTENT_TYPE = new Name("Content-Type", false);

		public static final Name SEALED = new Name("Sealed", false);

		public static final Name IMPLEMENTATION_TITLE = new Name(
				"Implementation-Title", false);

		public static final Name IMPLEMENTATION_VERSION = new Name(
				"Implementation-Version", false);

		public static final Name IMPLEMENTATION_VENDOR = new Name(
				"Implementation-Vendor", false);

		public static final Name SPECIFICATION_TITLE = new Name(
				"Specification-Title", false);

		public static final Name SPECIFICATION_VERSION = new Name(
				"Specification-Version", false);

		public static final Name SPECIFICATION_VENDOR = new Name(
				"Specification-Vendor", false);

		public static final Name EXTENSION_LIST = new Name("Extension-List",
				false);

		public static final Name EXTENSION_NAME = new Name("Extension-Name",
				false);

		public static final Name EXTENSION_INSTALLATION = new Name(
				"Extension-Installation", false);

		public static final Name IMPLEMENTATION_VENDOR_ID = new Name(
				"Implementation-Vendor-Id", false);

		public static final Name IMPLEMENTATION_URL = new Name(
				"Implementation-URL", false);

		public Name(String s) {
			int i = s.length();
			if (i == 0)
				throw new IllegalArgumentException();
			for (; --i >= 0;) {
				char ch = s.charAt(i);
				if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
						|| ch == '_' || ch == '-' || (ch >= '0' && ch <= '9')))
					throw new IllegalArgumentException(s);
			}
			name = s;
		}

		Name(String s, boolean ignored) {
			name = s;
		}

		public String toString() {
			return name;
		}

		public boolean equals(Object an) {
			if (an == null)
				return false;
			return an.getClass() == this.getClass()
					&& name.equalsIgnoreCase(((Name) an).name);
		}

		public int hashCode() {
			if (hashCode == 0)
				hashCode = name.toLowerCase().hashCode();
			return hashCode;
		}
	}

	/**
	 * Constructs an Attributes instance
	 */
	public Attributes() {
		map = new HashMap();
	}

	/**
	 * Constructs an Attributes instance obtaining keys and values from the
	 * parameter Attributes, attrib
	 * 
	 * @param attrib
	 *            The Attributes to obtain entries from.
	 */
	public Attributes(Attributes attrib) {
		map = (Map) ((HashMap) attrib.map).clone();
	}

	/**
	 * Constructs an Attributes instance with initial capacity of size size
	 * 
	 * @param size
	 *            Initial size of this Attributes instance.
	 */
	public Attributes(int size) {
		map = new HashMap(size);
	}

	/**
	 * Removes all key/value pairs from this Attributes.
	 * 
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Determines whether this Attributes contains the specified key
	 * 
	 * 
	 * @param key
	 *            The key to search for.
	 * @return true if the key is found, false otherwise
	 */
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/**
	 * Determines whether this Attributes contains the specified value
	 * 
	 * @param value
	 *            The value to search for.
	 * @return true if the value is found, false otherwise
	 */
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/**
	 * Returns a set containing MapEntry's for each of the key/value pairs
	 * contained in this Attributes.
	 * 
	 * @return a set of MapEntry's
	 */
	public Set entrySet() {
		return map.entrySet();
	}

	/**
	 * Returns the value associated with the parameter key
	 * 
	 * @param key
	 *            The key to search for.
	 * @return Object associated with key, or null if key does not exist.
	 */
	public Object get(Object key) {
		return map.get(key);
	}

	/**
	 * Determines whether this Attributes contains any keys
	 * 
	 * @return true if one or more keys exist, false otherwise
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns a Set containing all the keys found in this Attributes.
	 * 
	 * @return a Set of all keys
	 */
	public Set keySet() {
		return map.keySet();
	}

	/**
	 * Store value value in this Attributes and associate it with key key.
	 * 
	 * @param key
	 *            The key to associate with value.
	 * @param value
	 *            The value to sotre in this Attributes
	 * @return The value being stored
	 * 
	 * @exception ClassCastException
	 *                when key is not an Attributes.Name or value is not a
	 *                String
	 */
	public Object put(Object key, Object value) {
		return map.put(key, value);
	}

	/**
	 * putAll method comment.
	 */
	public void putAll(Map attrib) {
		this.map.putAll(attrib);
	}

	/**
	 * Deletes the key/value pair with key key from this Attributes.
	 * 
	 * @param key
	 *            The key to remove
	 * @return the valuse associated with the removed key, null if not present.
	 */
	public Object remove(Object key) {
		return map.remove(key);
	}

	/**
	 * Returns the number of key.value pairs associated with this Attributes.
	 * 
	 * @return the size of this Attributes
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Returns a Colletion of all the values present in this Attributes.
	 * 
	 * @return a Collection of all values present
	 */
	public Collection values() {
		return map.values();
	}

	public Object clone() {
		Attributes clone;
		try {
			clone = (Attributes) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
		clone.map = (Map) ((HashMap) this.map).clone();
		return clone;
	}

	/**
	 * Returns the hashCode of this Attributes
	 * 
	 * @return the hashCode of this Object.
	 */
	public int hashCode() {
		return map.hashCode();
	}

	/**
	 * Determines if this Attributes and the parameter Attributes are equal. Two
	 * Attributes instances are equal if they contain the same keys and values.
	 * 
	 * @return true if the Attributes are equals, false otherwise
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Attributes)
			return map.equals(((Attributes) obj).map);
		return false;
	}

	/**
	 * Returns the value associated with the parameter Attributes.Name key.
	 * 
	 * @param name
	 *            The key to obtain the value for.
	 * @return the String associated with name, or null if name is not a valid
	 *         key
	 */
	public String getValue(Attributes.Name name) {
		return (String) map.get(name);
	}

	/**
	 * Returns the String associated with the parameter name.
	 * 
	 * @param name
	 *            The key to obtain the value for.
	 * @return the String associated with name, or null if name is not a valid
	 *         key
	 */
	public String getValue(String name) {
		return (String) map.get(new Attributes.Name(name));
	}

	/**
	 * Stores value val against key name in this Attributes
	 * 
	 * @param name
	 *            The key to store against.
	 * @param val
	 *            The value to store in this Attributes
	 * @return the Value being stored
	 */
	public String putValue(String name, String val) {
		return (String) map.put(new Attributes.Name(name), val);
	}
}
