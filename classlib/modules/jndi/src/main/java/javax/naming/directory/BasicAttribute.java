/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.naming.directory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * A simple attribute of a directory entry.
 * <p>
 * A basic attribute does not have any schema associated with it, and attempts
 * to get the schema result in an <code>OperationNotSupportedException</code>
 * being thrown.
 * </p>
 * <p>
 * The definition of <code>equals</code> for an attribute is simply <code>
 * Object.equals</code>
 * on the value, except for values that are collections where the definition of
 * <code>equals</code> is an equivalence test (i.e. the collection contains
 * the same number of elements, and each has an equal element in the other
 * collection). For an array, <code>Object.equals</code> is used on each array
 * element.
 * </p>
 * <p>
 * Note that updates to a basic attribute do not update the directory itself --
 * updates to a directory are only possible through the {@link DirContext}
 * interface. <code>BasicAttribute</code> does not get its values dynamically
 * from the directory. It uses the values passed to the constructor or add and
 * remove methods.
 * </p>
 * 
 * @see Attribute
 */
public class BasicAttribute implements Attribute {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = 0x5d95d32a668565beL;

    /**
     * The attribute identifier. It is initialized by the public constructors
     * and is required to be not null.
     * 
     * @serial
     */
    protected String attrID;

    /**
     * Flag showing whether the values of the attribute are ordered.
     * 
     * @serial
     */
    protected boolean ordered;

    /**
     * <code>Vector</code> containing the attribute's values. This is
     * initialized by the public constructor and is required to be not null.
     */
    protected transient Vector<Object> values = new Vector<Object>();

    /**
     * Constructs an unordered <code>BasicAttribute</code> instance with the
     * supplied identifier and no values.
     * 
     * @param id
     *            the attribute ID
     */
    public BasicAttribute(String id) {
        this(id, false);
    }

    /**
     * Constructs a <code>BasicAttribute</code> instance with the supplied
     * identifier and no values. The supplied flag controls whether the values
     * will be ordered or not.
     * 
     * @param id
     *            the attribute ID
     * @param flag
     *            Indicates whether the values are ordered or not.
     */
    public BasicAttribute(String id, boolean flag) {
        attrID = id;
        ordered = flag;
    }

    /**
     * Constructs an unordered <code>BasicAttribute</code> instance with the
     * supplied identifier and one value.
     * 
     * @param id
     *            the attribute ID
     * @param val
     *            the first attribute value
     */
    public BasicAttribute(String id, Object val) {
        this(id, val, false);
    }

    /**
     * Constructs a <code>BasicAttribute</code> instance with the supplied
     * identifier and one value. The supplied flag controls whether the values
     * will be ordered or not.
     * 
     * @param id
     *            the attribute ID
     * @param val
     *            the first attribute value
     * @param flag
     *            Indicates whether the values are ordered or not.
     */
    public BasicAttribute(String id, Object val, boolean flag) {
        this(id, flag);
        values.add(val);
    }

    /*
     * Determine whether two values belonging to the two array classes
     * respectively are possible to be equal.
     */
    private boolean compareValueClasses(Class<? extends Object> c1,
            Class<? extends Object> c2) {
        if ((c1.getName().startsWith("[L") || c1.getName().startsWith("[[")) && //$NON-NLS-1$ //$NON-NLS-2$
                (c2.getName().startsWith("[L") || c2.getName().startsWith("[["))) { //$NON-NLS-1$ //$NON-NLS-2$
            /*
             * If both Class are array of Object or array of array, the compare
             * result is true, even if their class name may not be the same.
             */
            return true;
        } else if (c1.getName().equals(c2.getName())) {
            /*
             * Otherwise, at least one of them must be array of basic types. If
             * both Class have the same Class name, the compare result is true.
             */
            return true;
        } else {
            /*
             * Otherwise, the compare result is false
             */
            return false;
        }
    }

    /*
     * Determine whether the two values are equal with each other, considering
     * the possibility that they might be both arrays so that each element of
     * them has to be compared.
     */
    private boolean compareValues(Object obj1, Object obj2) {
        if (null == obj1 && null == obj2) {
            // If both are null, they are considered equal.
            return true;
        } else if (null != obj1 && null != obj2) {
            if (obj1.getClass().isArray() && obj2.getClass().isArray()) {
                /*
                 * If both are array, compare each element if it is possible
                 * that they might be equal.
                 */
                if (compareValueClasses(obj1.getClass(), obj2.getClass())) {
                    int i = Array.getLength(obj1);
                    Object val1;
                    Object val2;

                    // Compare each element of the two arrays
                    if (Array.getLength(obj2) == i) {
                        // Do the compare only if their lengths are equal
                        for (i--; i >= 0; i--) {
                            val1 = Array.get(obj1, i);
                            val2 = Array.get(obj2, i);
                            if (null == val1 ? null != val2 : !val1
                                    .equals(val2)) {
                                /*
                                 * If any of their elements at the same position
                                 * are not equal,they are not equal.
                                 */
                                return false;
                            }
                        }
                        // If all elements are equal, they are equal
                        return true;
                    }
                    // Not equal if different length
                    return false;
                }
                // Not equal if this can be inferred from their class names
                return false;
            }
            // If not both of them are array, do a normal "equals"
            return obj1.equals(obj2);
        } else {
            // Not equal if only one of them is null
            return false;
        }
    }

    /*
     * Get the hash code of an attribute value, which might be an array whose
     * hash code is the sum of all its element. Base types are converted into
     * corresponding wrapper class objects.
     */
    private int hashCodeOfValue(Object obj) {
        int hashcode = 0;

        if (null != obj) {
            // If the object is an array, sum up the hashcode of all elements.
            if (obj.getClass().isArray()) {
                Object element = null;
                // Sum up the hashcode of all elements
                for (int i = Array.getLength(obj) - 1; i >= 0; i--) {
                    element = Array.get(obj, i);
                    if (null != element) {
                        hashcode += element.hashCode();
                    }
                }
            } else {
                // Otherwise, simply get the hashcode of the given object.
                hashcode = obj.hashCode();
            }
        }

        return hashcode;
    }

    public void add(int index, Object val) {
        if (ordered) {
            values.add(index, val);
        } else {
            if (contains(val)) {
                // jndi.16=Value already exists.
                throw new IllegalStateException(Messages.getString("jndi.16")); //$NON-NLS-1$
            }
            values.add(index, val);
        }
    }

    public boolean add(Object val) {
        if (ordered) {
            return values.add(val); // always true
        }
        if (contains(val)) {
            return false;
        }
        return values.add(val); // always true
    }

    public void clear() {
        values.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            BasicAttribute attr = (BasicAttribute) super.clone();
            attr.values = (Vector<Object>) this.values.clone();
            return attr;
        } catch (CloneNotSupportedException e) {
            // jndi.17=Failed to clone object of BasicAttribute class.
            throw new AssertionError(Messages.getString("jndi.17")); //$NON-NLS-1$
        }
    }

    public boolean contains(Object val) {
        Enumeration<Object> e = this.values.elements();

        while (e.hasMoreElements()) {
            if (compareValues(e.nextElement(), val)) {
                return true;
            }
        }
        return false;
    }

    public Object get() throws NamingException {
        if (0 == values.size()) {
            // jndi.18=No values available.
            throw new NoSuchElementException(Messages.getString("jndi.18")); //$NON-NLS-1$
        }
        return values.get(0);
    }

    public Object get(int index) throws NamingException {
        return values.get(index);
    }

    public NamingEnumeration<?> getAll() throws NamingException {
        return new BasicNamingEnumeration<Object>(values.elements());
    }

    public DirContext getAttributeDefinition() throws NamingException {
        // jndi.19=BasicAttribute does not support this operation.
        throw new OperationNotSupportedException(Messages.getString("jndi.19")); //$NON-NLS-1$
    }

    public DirContext getAttributeSyntaxDefinition() throws NamingException {
        // jndi.19=BasicAttribute does not support this operation.
        throw new OperationNotSupportedException(Messages.getString("jndi.19")); //$NON-NLS-1$
    }

    public String getID() {
        return attrID;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public Object remove(int index) {
        return values.remove(index);
    }

    public boolean remove(Object val) {
        int total = this.values.size();

        for (int i = 0; i < total; i++) {
            if (compareValues(this.values.get(i), val)) {
                this.values.remove(i);
                return true;
            }
        }
        return false;
    }

    public Object set(int index, Object val) {
        if (!ordered && contains(val)) {
            // jndi.16=Value already exists.
            throw new IllegalStateException(Messages.getString("jndi.16")); //$NON-NLS-1$
        }
        return values.set(index, val);
    }

    public int size() {
        return values.size();
    }

    /*
     * Serialization of the BasicAttribute class is as follows: attribute
     * identifier (String) ordered flag (boolean) number of values (int) list of
     * value objects
     */
    private void readObject(ObjectInputStream ois) throws IOException,
            ClassNotFoundException {
        int size;

        ois.defaultReadObject();
        size = ois.readInt();
        this.values = new Vector<Object>();
        for (int i = 0; i < size; i++) {
            this.values.add(ois.readObject());
        }
    }

    /*
     * Serialization of the BasicAttribute class is as follows: attribute
     * identifier (String) ordered flag (boolean) number of values (int) list of
     * value objects
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeInt(this.values.size());
        for (Object object : this.values) {
            oos.writeObject(object);
        }
    }

    /**
     * Returns true if this <code>BasicAttribute</code> instance is equal to
     * the supplied object <code>obj</code>. Two attributes are considered
     * equal if they have equal identifiers, schemas and values. BasicAttribute
     * uses no schema.
     * <p>
     * <code>Object.equals</code> is used to test equality of identifiers and
     * values. For array values <code>Object.equals</code> is called on every
     * array element.
     * </p>
     * 
     * @param obj
     *            the object to be compared with
     * @return true if this object is equal to <code>obj</code>, otherwise
     *         false
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicAttribute) {
            BasicAttribute a = (BasicAttribute) obj;

            if (!this.attrID.equals(a.attrID)) {
                // Not equal if different ID
                return false;
            } else if (this.ordered != a.ordered) {
                // Not equal if different order definition
                return false;
            } else if (this.values.size() != a.values.size()) {
                // Not equal if different numbers of values
                return false;
            } else if (this.ordered) {
                // Otherwise, if both ordered, compare each value
                Enumeration<?> e1 = this.values.elements();
                Enumeration<?> e2 = a.values.elements();

                while (e1.hasMoreElements()) {
                    if (!compareValues(e1.nextElement(), e2.nextElement())) {
                        // Not equal if one of the values are not equal
                        return false;
                    }
                }
                // Equal only if all the values are equal
                return true;
            } else {
                /*
                 * Otherwise (i.e., both unordered), see whether containing the
                 * equal set of values.
                 */
                Enumeration<Object> e = this.values.elements();

                while (e.hasMoreElements()) {
                    if (!a.contains(e.nextElement())) {
                        return false;
                    }
                }
                return true;
            }
        }
        // Not equal if not instance of BasicAttribute
        return false;
    }

    /**
     * Returns the hashcode for this <code>BasicAttribute</code> instance. The
     * result is calculated by summing the hashcodes for the identifier and each
     * of the values, except for array values, where the hashcodes for each
     * array element are summed.
     * 
     * @return the hashcode of this <code>BasicAttribute</code> instance
     */
    @Override
    public int hashCode() {
        Object o;
        int i = attrID.hashCode();
        Enumeration<Object> e = this.values.elements();

        while (e.hasMoreElements()) {
            o = e.nextElement();
            if (null != o) {
                i += hashCodeOfValue(o);
            }
        }

        return i;
    }

    /**
     * Returns the string representation of this <code>BasicAttribute</code>
     * instance. The result contains the ID and the string representation of
     * each value.
     * 
     * @return the string representation of this object
     */
    @Override
    public String toString() {
        Enumeration<Object> e = this.values.elements();
        String s = "Attribute ID: " + this.attrID; //$NON-NLS-1$
        s += "\nAttribute values: "; //$NON-NLS-1$

        if (!e.hasMoreElements()) {
            s += "This Attribute does not have any values."; //$NON-NLS-1$
        } else {
            s += e.nextElement();
            while (e.hasMoreElements()) {
                s += "," + e.nextElement(); //$NON-NLS-1$
            }
        }
        return s + "\n"; //$NON-NLS-1$
    }

}
