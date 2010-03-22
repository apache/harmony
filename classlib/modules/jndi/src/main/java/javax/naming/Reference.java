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

package javax.naming;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * A <code>Reference</code> contains the class of the object which is
 * referenced together with a list of all the addresses at which this object may
 * be found. Additionally the <code>Reference</code> has the location of a
 * factory which can create this object together with the classname used to
 * create the object.
 * <p>
 * The <code>Reference</code> class relates to objects which are not bound in
 * a naming service but which need to be accessed via javax.naming.
 * <code>Reference</code>, <code>RefAddr</code> and their subclasses
 * provide a way to access objects other than those in naming and directory
 * systems.
 * </p>
 */
public class Reference implements Cloneable, Serializable {

    private static final long serialVersionUID = -1673475790065791735L;

    /**
     * The class of the object which is referenced.
     * 
     * @serial
     */
    protected String className;

    /**
     * A list of the addresses provided for this object. The default is null.
     * 
     * @serial
     */
    protected Vector<RefAddr> addrs;

    /**
     * The class in a factory which is used to create an object of the type
     * which is referenced. The default is null.
     * 
     * @serial
     */
    protected String classFactory;

    /**
     * The location of class <code>classFactory</code>. To find class files
     * the URL of the classfile is given. If there is more than one URL for the
     * class then a list of URLs appears in the string using a space as a
     * separator. The default is null.
     * 
     * @serial
     */
    protected String classFactoryLocation;

    /**
     * Constructs a <code>Reference</code> instance with an empty list of
     * addresses using the supplied class name.
     * 
     * @param className
     *            the class of the object which is referenced. It cannot be
     *            null.
     */
    public Reference(String className) {
        this(className, null, null);
    }

    /**
     * Constructs a <code>Reference</code> instance with one entry of address.
     * 
     * @param className
     *            the class of the object which is referenced. It cannot be
     *            null.
     * @param addr
     *            the object's address. It cannot be null.
     */
    public Reference(String className, RefAddr addr) {
        this(className, addr, null, null);
    }

    /**
     * Constructs a <code>Reference</code> instance with an empty list of
     * addresses using the supplied class name, factory class and factory
     * location.
     * 
     * @param className
     *            the class of the object which is referenced. It cannot be
     *            null.
     * @param classFactory
     *            the class in a factory which is used to create an object of
     *            the type which is referenced. It may be null.
     * @param classFactoryLocation
     *            the location of the class file. It may be null.
     */
    public Reference(String className, String classFactory,
            String classFactoryLocation) {
        super();
        this.className = className;
        this.classFactory = classFactory;
        this.classFactoryLocation = classFactoryLocation;
        this.addrs = new Vector<RefAddr>();
    }

    /**
     * Constructs a <code>Reference</code> instance with one entry of address
     * using the supplied class name, factory class and factory location.
     * 
     * @param className
     *            the class of the object which is referenced. It cannot be
     *            null.
     * @param addr
     *            the object's address. It cannot be null.
     * @param classFactory
     *            the class in a factory which is used to create an object of
     *            the type which is referenced. It may be null.
     * @param classFactoryLocation
     *            the location of the class file. It may be null.
     */
    public Reference(String className, RefAddr addr, String classFactory,
            String classFactoryLocation) {
        this(className, classFactory, classFactoryLocation);
        this.addrs.add(addr);
    }

    /**
     * Gets the class of the object which is referenced. The result cannot be
     * null.
     * 
     * @return the class of the object which is referenced
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Gets the class in a factory which is used to create an object of the type
     * which is referenced. The result may be null.
     * 
     * @return the class in a factory which is used to create the referenced
     *         object
     */
    public String getFactoryClassName() {
        return this.classFactory;
    }

    /**
     * Gets the location of the factory class. The result may be null.
     * 
     * @return the location of the factory class
     */
    public String getFactoryClassLocation() {
        return this.classFactoryLocation;
    }

    /**
     * Gets all the addresses.
     * 
     * @return an enumeration of all the addresses
     */
    public Enumeration<RefAddr> getAll() {
        return addrs.elements();
    }

    /**
     * Gets an address where the address type matches the specified string.
     * There may be more than one entry in the list with the same address type
     * but this method returns the first one found in the list.
     * 
     * @param type
     *            the address type to look for
     * @return the first address whose type matches the string
     */
    public RefAddr get(String type) {
        Enumeration<RefAddr> elements = addrs.elements();
        RefAddr refAddr = null;

        while (elements.hasMoreElements()) {
            refAddr = elements.nextElement();
            if (type.equals(refAddr.getType())) {
                return refAddr;
            }
        }
        return null;
    }

    /**
     * Gets the address held at the specified index in the address list.
     * 
     * @param index
     *            the index of the required address. It must be greater than or
     *            equal to 0 and less than the number of entries in the list.
     * @return the address held at the specified index
     * @throws ArrayIndexOutOfBoundsException
     *             If the index is invalid.
     */
    public RefAddr get(int index) {
        return addrs.get(index);
    }

    /**
     * Appends an address to the list.
     * 
     * @param addr
     *            the address to append. It cannot be null.
     */
    public void add(RefAddr addr) {
        addrs.add(addr);
    }

    /**
     * Inserts an address within the list at the specified index.
     * 
     * @param addr
     *            the address to insert into the list. It cannot be null.
     * @param index
     *            the index where to insert the address. It must be greater than
     *            or equal to 0 and less than or equal to the number of entries
     *            in the list(size()).
     * @throws ArrayIndexOutOfBoundsException
     *             If the index is invalid.
     */
    public void add(int index, RefAddr addr) {
        addrs.add(index, addr);
    }

    /**
     * Removes an address from the address list.
     * 
     * @param index
     *            the index of the address to remove. It must be greater than or
     *            equal to 0 and less than size().
     * @return the removed address
     * @throws ArrayIndexOutOfBoundsException
     *             If the index is invalid.
     */
    public Object remove(int index) {
        return addrs.remove(index);
    }

    /**
     * Gets the number of addresses in the address list.
     * 
     * @return the size of the list
     */
    public int size() {
        return addrs.size();
    }

    /**
     * Deletes all the addresses from the address list.
     */
    public void clear() {
        addrs.clear();
    }

    /**
     * Returns a deep clone of this <code>Reference</code> instance.
     * 
     * @return a deep clone of this object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            Reference r = (Reference) super.clone();
            r.addrs = (Vector<RefAddr>) this.addrs.clone();
            return r;
        } catch (CloneNotSupportedException e) {
            // jndi.03=Failed to clone object of Reference class.
            throw new AssertionError(Messages.getString("jndi.03")); //$NON-NLS-1$
        }
    }

    /**
     * Returns true if this <code>Reference</code> instance is equal to the
     * supplied object <code>o</code>. They are considered equal if their
     * class name and list of addresses are equivalent (including the order of
     * the addresses in the list). The factory class and location are ignored
     * for the purposes of this comparison.
     * 
     * @param o
     *            the object to compare with
     * @return true if this object is equal to <code>o</code>, otherwise
     *         false
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Reference) {
            Reference r = (Reference) o;
            return r.className.equals(this.className)
                    && r.addrs.equals(this.addrs);
        }
        return false;
    }

    /**
     * Returns the hashcode for this <code>Reference</code> instance. The
     * result is calculated by summing the hashcode of the class name and the
     * hash codes of each of the addresses in the address list.
     * 
     * @return the hashcode of this <code>Reference</code> instance
     */
    @Override
    public int hashCode() {
        int i = this.className.hashCode();
        Enumeration<RefAddr> e = this.addrs.elements();

        while (e.hasMoreElements()) {
            i += e.nextElement().hashCode();
        }
        return i;
    }

    /**
     * Returns the string representation of the class name together with the
     * list of addresses.
     * 
     * @return the string representation of this object
     */
    @SuppressWarnings("nls")
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Reference Class Name: ");
        s.append(className);
        s.append("\n");

        Enumeration<RefAddr> e = this.addrs.elements();
        while (e.hasMoreElements()) {
            s.append(e.nextElement());
        }
        return s.toString();
    }

}
