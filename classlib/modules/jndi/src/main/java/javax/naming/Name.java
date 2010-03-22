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

/**
 * A <code>Name</code> interface represents a name in a naming service.
 * <p>
 * A name which implements this interface has a sequence of zero or more
 * elements delimited by separators. Each element can be accessed using its
 * position. The first element is at position 0.
 * </p>
 * <p>
 * This interface is implemented by 2 classes - <code>CompoundName</code> and
 * <code>CompositeName</code>.
 * </p>
 * <p>
 * Examples of names are:
 * 
 * <pre>
 * File system name - for example /home/jenningm/.profile
 * DNS hostname     - for example www.apache.org
 * Internet URL     - for example http://www.eclipse.org/org/index.html
 * </pre>
 * 
 * </p>
 * 
 * @see CompositeName
 * @see CompoundName
 */
public interface Name extends Cloneable, Serializable, Comparable<Object> {

    public static final long serialVersionUID = -3617482732056931635L;

    /**
     * Get all the elements of this <code>Name</code>. If the
     * <code>Name</code> is empty then return an empty
     * <code>Enumeration</code>.
     * 
     * @return an enumeration of <code>Name</code> elements - cannot be null
     */
    public Enumeration<String> getAll();

    /**
     * Get an element of this <code>Name</code>.
     * 
     * @param i
     *            the index of the required element - must be greater than or
     *            equal to 0 and less than size().
     * @return the element at the specified position
     * @throws ArrayIndexOutOfBoundsException
     *             when the position is invalid. If the <code>Name</code> is
     *             empty this always returns
     *             <code>ArrayIndexOutOfBoundsException</code>
     */
    public String get(int i);

    /**
     * Create a new <code>Name</code> which comprises the first several
     * elements of this <code>Name</code>.
     * 
     * @param i
     *            the index of the first element not to be included - must be
     *            greater than or equal to 0 and less than or equal to size. If
     *            0 then an empty name is returned.
     * @return a new <code>Name</code> which comprises the first several
     *         elements of this <code>Name</code>
     * @throws ArrayIndexOutOfBoundsException
     *             when the position is invalid.
     */
    public Name getPrefix(int i);

    /**
     * Create a new <code>Name</code> which comprises the last (<code>size() - i</code>)
     * elements of this <code>Name</code>.
     * 
     * @param i
     *            the index of the first element to be included - must be
     *            greater than or equal to 0 and less than size.
     * @return a new <code>Name</code> which comprises the last (<code>size() - i</code>)
     *         elements of this <code>Name</code>
     * @throws ArrayIndexOutOfBoundsException
     *             when the position is invalid.
     */
    public Name getSuffix(int i);

    /**
     * Append a name to this <code>Name</code>. The name itself may have a
     * number of elements.
     * 
     * @param name
     *            the name to append onto this <code>Name</code>.
     * @return this <code>Name</code>
     * @throws InvalidNameException
     *             if name is invalid or the addition of the name results in
     *             this <code>Name</code> becoming invalid.
     */
    public Name addAll(Name name) throws InvalidNameException;

    /**
     * Insert a name within this <code>Name</code> at the specified position.
     * The name itself may have a number of elements.
     * 
     * @param i
     *            the index of the element where to start inserting the name -
     *            must be greater than or equal to 0 and less than or equal to
     *            size.
     * @param name
     *            the name to insert into this <code>Name</code>.
     * @return this <code>Name</code>
     * @throws InvalidNameException
     *             if name is invalid or the addition of the name results in
     *             this <code>Name</code> becoming invalid.
     */
    public Name addAll(int i, Name name) throws InvalidNameException;

    /**
     * Append an element to this <code>Name</code>.
     * 
     * @param s
     *            the string to append
     * @return this <code>Name</code>
     * @throws InvalidNameException
     *             if the addition of the element results in this
     *             <code>Name</code> becoming invalid.
     */
    public Name add(String s) throws InvalidNameException;

    /**
     * Insert an element within this <code>Name</code> at the specified
     * position.
     * 
     * @param i
     *            the index of the element where to insert the element - must be
     *            greater than or equal to 0 and less than or equal to size.
     * @param s
     *            the String to insert
     * @return this <code>Name</code>.
     * @throws InvalidNameException
     *             if the insertion of the element results in this Name becoming
     *             invalid.
     */
    public Name add(int i, String s) throws InvalidNameException;

    /**
     * Delete an element from this <code>Name</code>.
     * 
     * @param i
     *            the index of the element to delete - must be greater than or
     *            equal to 0 and less than size.
     * @return the deleted element
     * @throws InvalidNameException
     *             if the deletion of the element results in this
     *             <code>Name</code> becoming invalid.
     */
    public Object remove(int i) throws InvalidNameException;

    /**
     * Create a copy of this <code>Name</code>.
     * 
     * @return a complete (deep) copy of the object.
     */
    public Object clone();

    /**
     * Compare this <code>Name</code> with the one supplied as a parameter.
     * Each class which implements this interface will have a specification of
     * how to do the comparison.
     * 
     * @param o
     *            the object to compare - cannot be null.
     * @return a negative number means this is less than the supplied object. a
     *         positive number means this is greater than the supplied object.
     *         Zero means the two objects are equal.
     */
    public int compareTo(Object o);

    /**
     * Get the size of this <code>Name</code>. The size of a
     * <code>Name</code> is its number of elements.
     * 
     * @return the size of this name - cannot be null - can be zero
     */
    public int size();

    /**
     * Check if this <code>Name</code> is empty. A <code>Name</code> is
     * empty when it has no elements.
     * 
     * @return true if empty, else returns false
     */
    public boolean isEmpty();

    /**
     * Check if this <code>Name</code> starts with the elements in the
     * supplied name. The supplied name itself may have a number of elements.
     * 
     * @param name
     *            the name to check against this name
     * @return true when the supplied name matches else returns false
     */
    public boolean startsWith(Name name);

    /**
     * Check if this <code>Name</code> ends with the elements in the supplied
     * name. The supplied name itself may have a number of elements.
     * 
     * @param name
     *            the name to check against this name.
     * @return true when the supplied name matches else returns false.
     */
    public boolean endsWith(Name name);

}
