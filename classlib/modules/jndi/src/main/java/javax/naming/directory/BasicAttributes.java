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
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.naming.NamingEnumeration;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * A simple implementation of the <code>Attributes</code> interface.
 * <p>
 * The <code>BasicAttributes</code> provides operations on any types of
 * attribute. When a new attribute is created the <code>BasicAttributes</code>
 * class will create a new <code>BasicAttribute</code> and add it to the
 * attribute collection.
 * </p>
 * <p>
 * A particular instance of <code>BasicAttributes</code> can be either
 * case-sensitive or case-insensitive, as defined by the <code>isCaseIgnored()
 * </code>
 * method.
 * </p>
 * <p>
 * Note that changes to the <code>BasicAttributes</code> are local -- they do
 * not modify the directory. The directory is only modified by API calls on the
 * {@link DirContext} object.
 * </p>
 * 
 * @see Attributes
 */
public class BasicAttributes implements Attributes {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = 0x451d18d6a95539d8L;

    /**
     * Flag indicating whether the case of attribute identifier is ignored.
     * 
     * @serial
     */
    private boolean ignoreCase;

    // A map, Id => Attribute
    private transient Hashtable<String, Attribute> attrMap = new Hashtable<String, Attribute>();

    /**
     * Constructs a <code>BasicAttributes</code> instance which is
     * case-sensitive.
     */
    public BasicAttributes() {
        this(false);
    }

    /**
     * Constructs a <code>BasicAttributes</code> instance which is
     * case-sensitive if <code>flag</code> is false.
     * 
     * @param flag
     *            Indicates whether this instance is case-insensitive.
     */
    public BasicAttributes(boolean flag) {
        this.ignoreCase = flag;
    }

    /**
     * Constructs a case-sensitive <code>BasicAttributes</code> instance with
     * one attribute.
     * 
     * @param attrId
     *            the ID of the first attribute
     * @param attrObj
     *            the value of the first attribute
     */
    public BasicAttributes(String attrId, Object attrObj) {
        this(attrId, attrObj, false);
    }

    /**
     * Constructs a <code>BasicAttributes</code> instance with one attribute
     * which is case-sensitive if <code>flag</code> is false.
     * 
     * @param attrId
     *            the ID of the first attribute
     * @param attrObj
     *            the value of the first attribute
     * @param flag
     *            Indicates whether this instance is case-insensitive.
     */
    public BasicAttributes(String attrId, Object attrObj, boolean flag) {
        this.ignoreCase = flag;
        this.attrMap
                .put(convertId(attrId), new BasicAttribute(attrId, attrObj));
    }

    /*
     * Convert an attribute ID to lower case if this attribute collection is
     * case-insensitive.
     */
    private String convertId(String id) {
        return ignoreCase ? id.toLowerCase() : id;
    }

    public Attribute get(String id) {
        return attrMap.get(convertId(id));
    }

    public NamingEnumeration<Attribute> getAll() {
        return new BasicNamingEnumeration<Attribute>(attrMap.elements());
    }

    public NamingEnumeration<String> getIDs() {
        if (ignoreCase) {
            Enumeration<Attribute> e = this.attrMap.elements();
            Vector<String> v = new Vector<String>(attrMap.size());

            while (e.hasMoreElements()) {
                v.add((e.nextElement()).getID());
            }

            return new BasicNamingEnumeration<String>(v.elements());
        }
        return new BasicNamingEnumeration<String>(this.attrMap.keys());
    }

    public boolean isCaseIgnored() {
        return ignoreCase;
    }

    public Attribute put(Attribute attribute) {
        String id = convertId(attribute.getID());
        return attrMap.put(id, attribute);
    }

    public Attribute put(String id, Object obj) {
        return put(new BasicAttribute(id, obj));
    }

    public Attribute remove(String id) {
        return attrMap.remove(convertId(id));
    }

    public int size() {
        return attrMap.size();
    }

    /*
     * Serialization of the <code>BasicAttributes</code> class is as follows:
     * ignore attribute case (boolean) number of attributes (int) list of
     * attribute objects
     */
    private void readObject(ObjectInputStream ois) throws IOException,
            ClassNotFoundException {
        int size;

        ois.defaultReadObject();
        size = ois.readInt();
        attrMap = new Hashtable<String, Attribute>();
        for (int i = 0; i < size; i++) {
            BasicAttribute attr = (BasicAttribute) ois.readObject();
            attrMap.put(convertId(attr.getID()), attr);
        }
    }

    /*
     * Serialization of the <code>BasicAttributes</code> class is as follows:
     * ignore attribute case (boolean) number of attributes (int) list of
     * attribute objects
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeInt(attrMap.size());
        for (Enumeration<Attribute> enumeration = attrMap.elements(); enumeration
                .hasMoreElements();) {
            oos.writeObject(enumeration.nextElement());
        }
    }

    /**
     * Returns a deep copy of this attribute collection. The returned copy
     * contains the same attribute objects. The attribute objects are not
     * cloned.
     * 
     * @return a deep copy of this attribute collection
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            BasicAttributes c = (BasicAttributes) super.clone();
            c.attrMap = (Hashtable<String, Attribute>) this.attrMap.clone();
            return c;
        } catch (CloneNotSupportedException e) {
            // jndi.15=Failed to clone object of BasicAttributes class.
            throw new AssertionError(Messages.getString("jndi.15")); //$NON-NLS-1$
        }
    }

    /**
     * Returns true if this <code>BasicAttributes</code> instance is equal to
     * the supplied object <code>obj</code>. They are considered equal if
     * they handle case the same way and have equal attributes.
     * <code>Attribute</code> equality is tested by calling <code>
     * equals</code>
     * on each attribute, which may be overridden.
     * 
     * @param obj
     *            the object to compare with
     * @return true if this object is equal to <code>obj</code>, otherwise
     *         false
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Attributes)) {
            return false;
        }

        // compare case & size
        Attributes o = (Attributes) obj;
        if (isCaseIgnored() != o.isCaseIgnored() || size() != o.size()) {
            return false;
        }

        // compare each attribute
        Iterator<Map.Entry<String, Attribute>> it = attrMap.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<String, Attribute> e = it.next();
            if (!e.getValue().equals(o.get(e.getKey()))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the hashcode for this <code>BasicAttributes</code> instance.
     * The result is calculated by summing the hashcodes of all attributes,
     * incremented by one if this instance is not case-sensitive.
     * 
     * @return the hashcode of this <code>BasicAttributes</code> instance
     */
    @Override
    public int hashCode() {
        Enumeration<Attribute> e = attrMap.elements();
        int i = (ignoreCase ? 1 : 0);

        while (e.hasMoreElements()) {
            i += e.nextElement().hashCode();
        }

        return i;
    }

    /**
     * Returns the string representation of this <code>BasicAttributes</code>
     * instance. The result contains the attribute identifiers and values'
     * string representations.
     * 
     * @return the string representation of this object
     */
    @Override
    public String toString() {
        String s = null;
        Iterator<Map.Entry<String, Attribute>> it = attrMap.entrySet()
                .iterator();
        Map.Entry<String, Attribute> e = null;

        if (it.hasNext()) {
            // If there are one or more attributes, print them all.
            e = it.next();
            s = "{\n"; //$NON-NLS-1$
            s += e.getKey();
            s += "=" + e.getValue().toString(); //$NON-NLS-1$
            while (it.hasNext()) {
                e = it.next();
                s += "; "; //$NON-NLS-1$
                s += e.getKey();
                s += "=" + e.getValue().toString(); //$NON-NLS-1$
            }
            s += "}\n"; //$NON-NLS-1$
        } else {
            // Otherwise, print an indication that no attributes are stored.
            s = "This Attributes does not have any attributes.\n"; //$NON-NLS-1$
        }
        return s;
    }
}
