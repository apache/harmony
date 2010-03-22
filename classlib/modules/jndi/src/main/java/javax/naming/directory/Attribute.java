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
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * This interface defines the valid operations on a particular attribute of a
 * directory entry.
 * <p>
 * An attribute can have zero or more values. The value may be null.
 * </p>
 * <p>
 * When there are multiple values for a particular attribute, the collection of
 * values may be specifically ordered or unordered. This interface provides a
 * method for determining whether the order is maintained.
 * </p>
 * <p>
 * If the values of an attribute are ordered, then duplicates are allowed. If
 * the values are unordered then duplicates are not allowed.
 * </p>
 * <p>
 * If the values are unordered then the indexed operations work as if the values
 * added previously to the attribute had been done using ordered semantics. For
 * example, if the values "a", "b" and "c" were previously added to an unordered
 * attribute using "<code>add("a"); add("b"); add("c");</code>", it is
 * equivalent to adding the same objects to an ordered attribute using "<code>add(0,"a"); add(1,"b"); add(2,"c");</code>".
 * In this case, if we do "<code>remove(1)</code>" on the unordered list,
 * the value "b" is removed, changing the index of "c" to 1.
 * </p>
 * <p>
 * Multiple null values can be added to an attribute. It is not the same as
 * having no values on an attribute. If a null value is added to an unordered
 * attribute which already has a null value, the <code>add</code> method has
 * no effect.
 * </p>
 * <p>
 * A directory may optionally provide information about the syntax of an
 * attribute's value via a schema. The methods
 * <code>getAttributeDefinition</code> and
 * <code>getAttributeSyntaxDefinition</code> return the schema definitions if
 * they exist.
 * </p>
 * <p>
 * Note that updates to the attribute via this interface do not affect the
 * directory directly. The only mechanism for modifying the directory is through
 * the {@link DirContext}.
 * </p>
 * <p>
 * Concrete implementations of this <code>Attribute</code> interface may be
 * either static or dynamic, and this interface does not make any distinction
 * between the two types. A static attribute implementation retrieves its value
 * from the directory once and stores it locally, a dynamic attribute
 * implementation will go back to the directory for each request.
 * </p>
 */
public interface Attribute extends Cloneable, Serializable {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    public static final long serialVersionUID = 0x78d7ee3675a55244L;

    /**
     * Adds a value at the specified index. The index is only meaningful if the
     * values are ordered. If there are already values at this index and above,
     * they are moved up one position.
     * <p>
     * It is permissible to use this method when the values are not ordered but
     * in this case, if a value equals to <code>val</code> already exists then
     * this method throws an <code>IllegalStateException</code> because
     * duplicates are not allowed.
     * </p>
     * <p>
     * The permitted range for index is 0 &lt;= index &lt;= <code>size()</code>.
     * The range allows the list to grow by one. If the index is outside this
     * range this method throws an <code>IndexOutOfBoundsException</code>.
     * </p>
     * 
     * @param index
     *            the position index
     * @param val
     *            a new value to be added which may be null
     * @throws IllegalStateException
     *             If the new value equals to an existing value in an unordered
     *             <code>Attribute</code>.
     * @throws IndexOutOfBoundsException
     *             If the index is invalid.
     */
    void add(int index, Object val);

    /**
     * Adds a value to this attribute. For unordered attribute values this
     * method adds the new value unless the value is already present. If the new
     * value is already present in unordered attribute values, the method has no
     * effect.
     * <p>
     * For ordered attribute values, the new value is added at the end of list
     * of values.
     * </p>
     * <p>
     * This method returns true or false to indicate whether a value was added.
     * </p>
     * 
     * @param val
     *            a new value to be added which may be null
     * @return true if a value was added, otherwise false
     */
    boolean add(Object val);

    /**
     * Clears all values of this attribute.
     */
    void clear();

    /**
     * Returns a deep copy of the attribute containing all the same values. The
     * values are not cloned.
     * 
     * @return a deep clone of this attribute
     */
    Object clone();

    /**
     * Indicates whether the specified value is one of the attribute's values.
     * 
     * @param val
     *            the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains(Object val);

    /**
     * Gets a value of this attribute. For unordered values, returns any of the
     * values. For ordered values, returns the first. <code>null</code> is a
     * valid value.
     * <p>
     * If the attribute has no values this method throws
     * <code>NoSuchElementException</code>.
     * </p>
     * 
     * @return a value of this attribute
     * @throws NamingException
     *             If the attribute has no value.
     */
    Object get() throws NamingException;

    /**
     * Returns the value at the specified index, even for unordered values. This
     * method throws <code>IndexOutOfBoundsException</code> if the index is
     * outside the valid range 0 &lt;= index &lt; <code>size()</code>.
     * 
     * <p>
     * If the attribute has no values this method throws
     * <code>NoSuchElementException</code>.
     * </p>
     * 
     * @param index
     *            the position index
     * @return the value at the specified index
     * @throws IndexOutOfBoundsException
     *             If the index is invalid.
     * @throws NamingException
     *             If the attribute has no value.
     */
    Object get(int index) throws NamingException;

    /**
     * Returns an enumeration of all the attribute's values. The enumeration is
     * ordered if the values are.
     * <p>
     * The effect on the returned enumeration of adding or removing values of
     * the attribute is not specified.
     * </p>
     * <p>
     * This method will throw any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @return an enumeration of all values of the attribute
     * @throws NamingException
     *             If any <code>NamingException</code> occurs.
     */
    NamingEnumeration<?> getAll() throws NamingException;

    /**
     * Returns the attribute's schema definition. If this operation is not
     * supported, an <code>
     * OperationNotSupportedException</code> is thrown. If
     * the implementation supports schemas but no schema is set, it is valid to
     * return null.
     * <p>
     * This method will throw any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @return the schema definitions if they exist
     * @throws NamingException
     *             If any <code>NamingException</code> occurs.
     */
    DirContext getAttributeDefinition() throws NamingException;

    /**
     * Returns the attribute's syntax definition. If this operation is not
     * supported, an <code>
     * OperationNotSupportedException</code> is thrown. If
     * the implementation supports syntax definitions but no syntax definition
     * is set, it is valid to return null.
     * <p>
     * This method will throw any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @return the syntax definitions if they exist
     * @throws NamingException
     *             If any <code>NamingException</code> occurs.
     */
    DirContext getAttributeSyntaxDefinition() throws NamingException;

    /**
     * Returns the identity of this attribute. This method is not expected to
     * return null.
     * 
     * @return the ID of this attribute
     */
    String getID();

    /**
     * Indicates whether the values of this attribute are ordered or not.
     * 
     * @return true if the values of this attribute are ordered, otherwise false
     */
    boolean isOrdered();

    /**
     * Removes the values at the specified index, even for unordered values.
     * Values at higher indexes move one position lower.
     * <p>
     * If the index is outside the valid range 0 &lt;= index &lt;
     * <code>size()</code> this method throws an
     * <code>IndexOutOfBoundsException</code>.
     * </p>
     * 
     * @param index
     *            the position index
     * @return the removed value
     * @throws IndexOutOfBoundsException
     *             If the index is invalid.
     */
    Object remove(int index);

    /**
     * Removes a value that is equal to the given value. There may be more than
     * one match in ordered value, in which case the equal value with the lowest
     * index is removed. After an ordered value is removed, values at higher
     * indexes move one position lower.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code>
     * this method simply returns false.
     * </p>
     * 
     * @param val
     *            the value to be removed
     * @return true if the value is removed, otherwise false
     */
    boolean remove(Object val);

    /**
     * Replaces the value at the specified index with the given value. The old
     * value (which may be null) is returned.
     * <p>
     * If the values are unordered and the given value is already present this
     * method throws an <code>IllegalStateException</code>.
     * </p>
     * <p>
     * The valid range for the index is 0 &lt;= index &lt; <code>size()</code>.
     * This method throws an <code>IndexOutOfBoundsException</code> if the
     * index is outside this range.
     * </p>
     * 
     * @param index
     *            the position index
     * @param val
     *            the new value
     * @return the original value at the specified index
     * @throws IndexOutOfBoundsException
     *             If the index is invalid.
     */
    Object set(int index, Object val);

    /**
     * Gets the count of the values in this attribute.
     * 
     * @return the count of the values in this attribute
     */
    int size();

}
