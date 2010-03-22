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

import java.io.Serializable;
import javax.naming.NamingEnumeration;

/**
 * This is the interface to a collection of attributes associated with a
 * directory entry.
 * <p>
 * This interface defines the methods that are implemented by a collection of a
 * particular directory entry's attributes.
 * </p>
 * <p>
 * A directory entry can have zero or more attributes comprising its attributes
 * collection. The attributes are unordered within the collection. The
 * attributes can be identified by name. The names of attributes are either case
 * sensitive or case insensitive as indicated by the <code>isCaseIgnored</code>
 * method. Method names refer to attribute ID (identity) rather than name, for
 * brevity.
 * </p>
 * <p>
 * The attribute collection is created when the directory entry is created.
 * </p>
 */
public interface Attributes extends Cloneable, Serializable {

    /**
     * Returns a deep copy of this <code>Attributes</code> instance. The
     * attribute objects are not cloned.
     * 
     * @return a deep copy of this <code>Attributes</code> instance
     */
    Object clone();

    /**
     * Returns the attribute with the specified name (ID). The name is case
     * insensitive if <code>isCaseIgnored()</code> is true. The return value
     * is <code>null</code> if no match is found.
     * 
     * @param id
     *            attribute name (ID)
     * @return the attribute with the specified name (ID)
     */
    Attribute get(String id);

    /**
     * Returns an enumeration containing the zero or more attributes in the
     * collection. The behaviour of the enumeration is not specified if the
     * attribute collection is changed.
     * 
     * @return an enumeration of all contained attributes
     * 
     */
    NamingEnumeration<? extends javax.naming.directory.Attribute> getAll();

    /**
     * Returns an enumeration containing the zero or more names (IDs) of the
     * attributes in the collection. The behaviour of the enumeration is not
     * specified if the attribute collection is changed.
     * 
     * @return an enumeration of the IDs of all contained attributes
     */
    NamingEnumeration<String> getIDs();

    /**
     * Indicates whether case is ignored in the names of the attributes.
     * 
     * @return true if case is ignored, otherwise false
     */
    boolean isCaseIgnored();

    /**
     * Places a non-null attribute in the attribute collection. If there is
     * already an attribute with the same ID as the new attribute, the old one
     * is removed from the collection and is returned by this method. If there
     * was no attribute with the same ID the return value is <code>null</code>.
     * 
     * @param attribute
     *            the attribute to be put
     * @return the old attribute with the same ID, if exists; otherwise
     *         <code>null</code>
     */
    Attribute put(Attribute attribute);

    /**
     * Places a new attribute with the supplied ID and value into the attribute
     * collection. If there is already an attribute with the same ID, the old
     * one is removed from the collection and is returned by this method. If
     * there was no attribute with the same ID the return value is
     * <code>null</code>. The case of the ID is ignored if
     * <code>isCaseIgnored()</code> is true.
     * 
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value of <code>obj</code> may be
     * <code>null</code>.
     * 
     * @param id
     *            the ID of the new attribute to be put
     * @param obj
     *            the value of the new attribute to be put
     * @return the old attribute with the same ID, if exists; otherwise
     *         <code>null</code>
     */
    Attribute put(String id, Object obj);

    /**
     * Removes the attribute with the specified ID. The removed attribute is
     * returned by this method. If there is no attribute with the specified ID,
     * the return value is <code>null</code>. The case of the ID is ignored
     * if <code>isCaseIgnored()</code> is true.
     * 
     * @param id
     *            the ID of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
    Attribute remove(String id);

    /**
     * Returns the number of attributes.
     * 
     * @return the number of attributes
     */
    int size();

}
