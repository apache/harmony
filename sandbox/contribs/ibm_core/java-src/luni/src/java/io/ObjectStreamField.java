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

package java.io;


import java.lang.ref.WeakReference;

import com.ibm.oti.util.Sorter;

/**
 * This class represents object fields that are saved to the stream, by
 * serialization. Classes can define the collection of fields to be dumped,
 * which can differ from the actual object's declared fields.
 * 
 * @see ObjectOutputStream#writeFields()
 * @see ObjectInputStream#readFields()
 * 
 */
public class ObjectStreamField extends Object implements Comparable {
	private String name; // Declared name of the field

	private Object type; // Declared type of the field

	int offset; // offset of this field in the object

	private String typeString; // Cached version of intern'ed type String.

	private boolean unshared = false;

	/**
	 * Constructs an ObjectStreamField with the given name and the given type
	 * 
	 * @param name
	 *            a String, the name of the field
	 * @param cl
	 *            A Class object representing the type of the field
	 */
	public ObjectStreamField(String name, Class cl) {
		if (name != null && cl != null) {
			this.name = name;
			this.type = new WeakReference(cl);
		} else
			throw new NullPointerException();
	}

	/**
	 * Constructs an ObjectStreamField with the given name and the given type
	 * 
	 * @param name
	 *            a String, the name of the field
	 * @param cl
	 *            A Class object representing the type of the field
	 * @param unshared
	 *            write and read the field unshared
	 */
	public ObjectStreamField(String name, Class cl, boolean unshared) {
		if (name != null && cl != null) {
			this.name = name;
			if (cl.getClassLoader() == null) {
				this.type = cl;
			} else {
				this.type = new WeakReference(cl);
			}
			this.unshared = unshared;
		} else
			throw new NullPointerException();
	}

	/**
	 * Constructs an ObjectStreamField with the given name and the given type.
	 * The type may be null.
	 * 
	 * @param signature
	 *            A String representing the type of the field
	 * @param name
	 *            a String, the name of the field, or null
	 */
	ObjectStreamField(String signature, String name) {
		if (name != null) {
			this.name = name;
			this.typeString = signature.replace('.', '/');
		} else
			throw new NullPointerException();
	}

	/**
	 * Comparing the receiver to the parameter, according to the Comparable
	 * interface.
	 * 
	 * @param o
	 *            The object to compare against
	 * 
	 * @return -1 if the receiver is "smaller" than the parameter. 0 if the
	 *         receiver is "equal" to the parameter. 1 if the receiver is
	 *         "greater" than the parameter.
	 * 
	 */
	public int compareTo(Object o) {
		ObjectStreamField f = (ObjectStreamField) o;
		boolean thisPrimitive = this.isPrimitive();
		boolean fPrimitive = f.isPrimitive();

		// If one is primitive and the other isn't, we have enough info to
		// compare
		if (thisPrimitive != fPrimitive)
			return thisPrimitive ? -1 : 1;

		// Either both primitives or both not primitives. Compare based on name.
		return this.getName().compareTo(f.getName());
	}

	/**
	 * Return the name of the field the receiver represents
	 * 
	 * @return a String, the name of the field
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the offset of this field in the object
	 * 
	 * @return an int, the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Return the type of the field the receiver represents
	 * 
	 * @return A Class object representing the type of the field
	 */
	public Class getType() {
		if (type instanceof WeakReference) {
			return (Class) ((WeakReference) type).get();
		}
		return (Class) type;
	}

	/**
	 * Return the type code that corresponds to the class the receiver
	 * represents
	 * 
	 * @return A char, the typecode of the class
	 */
	public char getTypeCode() {
		Class t = getType();
		if (t == Integer.TYPE)
			return 'I';
		if (t == Byte.TYPE)
			return 'B';
		if (t == Character.TYPE)
			return 'C';
		if (t == Short.TYPE)
			return 'S';
		if (t == Boolean.TYPE)
			return 'Z';
		if (t == Long.TYPE)
			return 'J';
		if (t == Float.TYPE)
			return 'F';
		if (t == Double.TYPE)
			return 'D';
		if (t.isArray())
			return '[';
		return 'L';
	}

	/**
	 * Return the type signature used by the VM to represent the type for this
	 * field.
	 * 
	 * @return A String, the signature for the class of this field.
	 */
	public String getTypeString() {
		if (typeString == null)
			typeString = computeTypeString().intern();
		return typeString;
	}

	/**
	 * Return the type signature used by the VM to represent the type for this
	 * field.
	 * 
	 * @return A String, the signature for the class of this field.
	 */
	private String computeTypeString() {
		// NOTE: this is very similar to Class.getSignature(). Unfortunately
		// we can't call that due to visibility restrictions.

		Class t = getType();
		if (t.isArray())
			return t.getName().replace('.', '/');

		if (isPrimitive()) {
			// Special cases for each base type.
			// NOTE: In how many places do we find this same pattern in java ?
			// getTypeCode() return character, here we return String.
			if (t == Integer.TYPE)
				return "I"; //$NON-NLS-1$
			if (t == Byte.TYPE)
				return "B"; //$NON-NLS-1$
			if (t == Character.TYPE)
				return "C"; //$NON-NLS-1$
			if (t == Short.TYPE)
				return "S"; //$NON-NLS-1$
			if (t == Boolean.TYPE)
				return "Z"; //$NON-NLS-1$
			if (t == Long.TYPE)
				return "J"; //$NON-NLS-1$
			if (t == Float.TYPE)
				return "F"; //$NON-NLS-1$
			if (t == Double.TYPE)
				return "D"; //$NON-NLS-1$
			throw new RuntimeException();
		}

		// General case.
		return ("L" + t.getName() + ';').replace('.', '/'); //$NON-NLS-1$
	}

	/**
	 * Return a boolean indicating whether the class of this field is a
	 * primitive type or not
	 * 
	 * @return true if the type of this field is a primitive type false if the
	 *         type of this field is a regular class.
	 */
	public boolean isPrimitive() {
		Class t = getType();
		return t != null && t.isPrimitive();
	}

	/**
	 * Set the offset this field represents in the object
	 * 
	 * @param newValue
	 *            an int, the offset
	 */
	protected void setOffset(int newValue) {
		this.offset = newValue;
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return this.getClass().getName() + '(' + getName() + ':' + getType()
				+ ')';
	}

	/**
	 * Sorts the fields for dumping. Primitive types come first, then regular
	 * types.
	 * 
	 * @param fields
	 *            ObjectStreamField[] fields to be sorted
	 */
	static void sortFields(ObjectStreamField[] fields) {
		// Sort if necessary
		if (fields.length > 1) {
			Sorter.Comparator fieldDescComparator = new Sorter.Comparator() {
				public int compare(Object o1, Object o2) {
					ObjectStreamField f1 = (ObjectStreamField) o1;
					ObjectStreamField f2 = (ObjectStreamField) o2;
					return f1.compareTo(f2);
				}
			};
			Sorter.sort(fields, fieldDescComparator);
		}
	}

	void resolve(ClassLoader loader) {
		if (typeString.length() == 1) {
			switch (typeString.charAt(0)) {
			case 'I':
				type = Integer.TYPE;
				return;
			case 'B':
				type = Byte.TYPE;
				return;
			case 'C':
				type = Character.TYPE;
				return;
			case 'S':
				type = Short.TYPE;
				return;
			case 'Z':
				type = Boolean.TYPE;
				return;
			case 'J':
				type = Long.TYPE;
				return;
			case 'F':
				type = Float.TYPE;
				return;
			case 'D':
				type = Double.TYPE;
				return;
			}
		}
		String className = typeString.replace('/', '.');
		if (className.charAt(0) == 'L') {
			// remove L and ;
			className = className.substring(1, className.length() - 1);
		}
		try {
			Class cl = Class.forName(className, false, loader);
			if (cl.getClassLoader() == null) {
				type = cl;
			} else {
				type = new WeakReference(cl);
			}
		} catch (ClassNotFoundException e) {
		}
	}

	boolean getUnshared() {
		return unshared;
	}
}
