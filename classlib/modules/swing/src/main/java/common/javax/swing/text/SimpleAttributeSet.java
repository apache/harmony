/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

public class SimpleAttributeSet
    implements Cloneable, MutableAttributeSet, Serializable {

    public static final AttributeSet EMPTY = EmptyAttributeSet.EMPTY;

    /**
     * This is an instance of hashtable. It contains all attributes.
     * It is declared transient for correct serialization of AttributeSet.
     */
    private transient Hashtable hashtable = new Hashtable();

    public SimpleAttributeSet() {
    }

    public SimpleAttributeSet(final AttributeSet source) {
        this();
        addAttributes(source);
    }

    public void addAttribute(final Object name, final Object value) {
        hashtable.put(name, value);
    }

    public void addAttributes(final AttributeSet attributes) {
        for (Enumeration e = attributes.getAttributeNames();
            e.hasMoreElements();) {

            Object name = e.nextElement();
            addAttribute(name, attributes.getAttribute(name));
        }
    }

    public Object clone() {
        return new SimpleAttributeSet(this);
    }

    public boolean containsAttribute(final Object name, final Object value) {
        Object obtValue = getAttribute(name);
        return (obtValue == null) ? false : obtValue.equals(value);
    }

    public boolean containsAttributes(final AttributeSet attributes) {
        for (Enumeration e = attributes.getAttributeNames();
             e.hasMoreElements();) {

            Object name = e.nextElement();
            if (!containsAttribute(name, attributes.getAttribute(name))) {
                return false;
            }
        }
        return true;
    }

    public AttributeSet copyAttributes() {
        return new SimpleAttributeSet(this);
    }

    public boolean equals(final Object obj) {
        if (obj instanceof AttributeSet) {
            return isEqual((AttributeSet)obj);
        }

        return false;
    }

    public Object getAttribute(final Object name) {
        Object value = hashtable.get(name);
        if (value == null) {
            value = hashtable.get(AttributeSet.ResolveAttribute);
            if (value instanceof AttributeSet) {
                value = ((AttributeSet)value).getAttribute(name);
            }
        }
        return value;
    }

    public int getAttributeCount() {
        return hashtable.size();
    }

    public Enumeration<?> getAttributeNames() {
        return hashtable.keys();
    }

    public AttributeSet getResolveParent() {
        return (AttributeSet)hashtable.get(AttributeSet.ResolveAttribute);
    }

    public int hashCode() {
        //Hash code of an empty instance must be equal to EMPTY.hashCode()
        if (hashtable.size() == 0) {
            return EMPTY.hashCode();
        }
        return hashtable.hashCode();
    }

    public boolean isDefined(final Object attrName) {
        return hashtable.containsKey(attrName);
    }

    public boolean isEmpty() {
        return hashtable.isEmpty();
    }

    public boolean isEqual(final AttributeSet attr) {
        if (getAttributeCount() != attr.getAttributeCount()) {
            return false;
        }

        for (Enumeration e = hashtable.keys(); e.hasMoreElements();) {
            Object name = e.nextElement();
            if (!attr.containsAttribute(name, hashtable.get(name))) {
                return false;
            }
        }

        return true;
    }

    public void removeAttribute(final Object name) {
        hashtable.remove(name);
    }

    public void removeAttributes(final AttributeSet attributes) {
        Enumeration e = attributes.getAttributeNames();
        while (e.hasMoreElements()) {
            Object name = e.nextElement();
            Object thisValue = hashtable.get(name);
            if (thisValue != null) {
                if (thisValue.equals(attributes.getAttribute(name))) {
                    removeAttribute(name);
                }
            }
        }
    }

    public void removeAttributes(final Enumeration<?> names) {
        while (names.hasMoreElements()) {
            removeAttribute(names.nextElement());
        }
    }

    public void setResolveParent(final AttributeSet parent) {
        addAttribute(AttributeSet.ResolveAttribute, parent);
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *  SimpleAttributeSet obj = new SimpleAttributeSet();
     *  obj.addAttribute("attribute", "value");
     *  System.out.println(obj.toString());
     */
    public String toString() {
        String str = new String();
        for (Enumeration e = hashtable.keys(); e.hasMoreElements();) {
            Object name = e.nextElement();
            str += name + "=";
            Object value = hashtable.get(name);
            if (value instanceof AttributeSet) {
                str += "**Attribute Set**" + " ";
            } else {
                str += value + " ";
            }
        }
        return str;
    }

    /**
     * Removes all the attributes from the set. Internally clears the attribute
     * storage <code>hashtable</code>.
     */
    final void removeAll() {
        hashtable.clear();
    }

    private void readObject(final ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        hashtable = new Hashtable();
        StyleContext.readAttributeSet(in, this);
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        StyleContext.writeAttributeSet(out, this);
    }
}
