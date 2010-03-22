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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * A <code>CompositeName</code> represents a name in a naming service which
 * spans multiple namespaces. For example the name "www.eclipse.org/index.html"
 * spans the DNS and file system namespaces.
 * <p>
 * A <code>CompositeName</code> is a series of string elements. A composite
 * name has a sequence of zero or more elements delimited by the '/' char. Each
 * element can be accessed using its position. The first element is at position
 * 0.
 * </p>
 * <p>
 * A <code>CompositeName</code> may be empty. An empty composite name has no
 * elements. Elements may also be empty.
 * </p>
 * <p>
 * <code>CompositeName</code>s are read from left to right unlike
 * <code>CompoundName</code>s which may have their direction of ordering
 * specified by properties.
 * </p>
 * <p>
 * Special characters are as follows:
 * </p>
 * <ul>
 * <li>The separator is /</li>
 * <li>The escape character is \</li>
 * <li>Quotes can be used - both single quotes and double quotes are allowed.
 * This allows you to quote strings which contain chars such as / which are part
 * of a <code>CompositeName</code> element to avoid them being read as a
 * separator.</li>
 * </ul>
 * <p>
 * See the examples for further clarification.
 * </p>
 * <p>
 * Some Examples:<br />
 * ==============
 * </p>
 * <p>
 * The composite name "www.eclipse.org/index.html" has 2 elements.
 * "www.eclipse.org" is a name from the DNS namespace. "index.html" is a name
 * from the file system namespace.
 * </p>
 * <p>
 * Another example of a composite name is: "www.eclipse.org/org/index.html".
 * This name has 3 elements "www.eclipse.org", "org" and "index.html".
 * www.eclipse.org is a name from the DNS namespace. The last 2 elements are
 * each from the file system namespace.
 * </p>
 * <p>
 * Some more examples to clarify empty names and elements:
 * </p>
 * <p>
 * An empty CompositeName is the name "" and has no elements.
 * </p>
 * <p>
 * A CompositeName with just one empty element is the name "/".
 * </p>
 * <p>
 * The name "/org/" has 3 elements. The first and last are empty.
 * </p>
 * <p>
 * The name "/a" has 2 elements. The first element is empty and the second
 * element is "a".
 * </p>
 * <p>
 * The name "a//a" has 3 elements. The middle element is empty and the first &
 * third elements are both "a".
 * </p>
 * <p>
 * The name "a/'b/a" is invalid as there is no closing quote for the '
 * character.
 * </p>
 * <p>
 * The name "a/'a/b/b" is invalid as there is no closing quote for the '
 * character.
 * </p>
 * <p>
 * The name "a/\"b/a" is interpreted as a/"b/a and is invalid as there is no
 * closing quote for the embedded escaped " character.
 * </p>
 * <p>
 * The name "a/'b/c'/a" has 3 elements. The middle element is b/c.
 * <p>
 * The name "a/a'a/b'/b" has 4 elements: Element 0 is "a". Element 1 is "a'a".
 * Element 2 is "b'". Element 3 is "b".
 * </p>
 * <p>
 * Interestingly the name "a/a'a/b/b" is valid and has 4 elements. This is
 * because the single quote char ' is not a leading quote and is embedded in an
 * element so is treated as a character. Element 0 is "a". Element 1 is "a'a".
 * Element 2 is "b". Element 3 is "b".
 * </p>
 * <p>
 * The name "\"abcd" gives an <code>InvalidNameException</code> as there is no
 * closing quote.
 * </p>
 * <p>
 * The name "'\"abcd'" gives one element of value "abcd.
 * </p>
 * <p>
 * The name "\\abcd" gives one element of value \abcd.
 * </p>
 * <p> "" is empty. It has no elements. "/" has one empty element. "//" has 2
 * empty elements. "/a/" has 3 elements the middle one is set to a. "///" has 3
 * empty elements. "//a/" has 4 elements, the last but one is set to a.
 * </p>
 */
public class CompositeName implements Name {

    private static final long serialVersionUID = 1667768148915813118L;

    // status used by parse()
    private static final int OUT_OF_QUOTE = 0;

    private static final int IN_SINGLE_QUOTE = 1;

    private static final int IN_DOUBLE_QUOTE = 2;

    private static final int QUOTE_ENDED = 3;

    /* a list holding elements */
    private transient Vector<String> elems;

    /**
     * Private copy constructor.
     * 
     * @param elements
     *            a list of name elements
     */
    private CompositeName(List<String> elements) {
        super();
        elems = new Vector<String>(elements);
    }

    /**
     * Construct a composite name with given elements.
     * 
     * @param elements
     *            an enumeration of name elements
     */
    protected CompositeName(Enumeration<String> elements) {
        super();
        elems = new Vector<String>();
        while (elements.hasMoreElements()) {
            elems.add(elements.nextElement());
        }
    }

    /**
     * Default constructor, creates an empty name with zero elements.
     */
    public CompositeName() {
        super();
        elems = new Vector<String>();
    }

    /**
     * This constructor takes the supplied name and breaks it down into its
     * elements.
     * 
     * @param name
     *            a string containing the full composite name
     * @throws InvalidNameException
     *             if the supplied name is invalid
     */
    public CompositeName(String name) throws InvalidNameException {
        super();
        elems = parseName(name);
    }

    /**
     * Parse string name elements. Delimiter is "/". Escape is "\" and both
     * single quote and double quote are supported.
     */
    private static Vector<String> parseName(String name)
            throws InvalidNameException {

        Vector<String> l = new Vector<String>();

        // special case: all '/', means same number of empty elements
        if (isAllSlash(name)) {
            for (int i = 0; i < name.length(); i++) {
                l.add(""); //$NON-NLS-1$
            }
            return l;
        }

        // general simple case, without escape and quote
        if (name.indexOf('"') < 0 && name.indexOf('\'') < 0
                && name.indexOf('\\') < 0) {
            int i = 0, j = 0;
            while ((j = name.indexOf('/', i)) >= 0) {
                l.add(name.substring(i, j));
                i = j + 1;
            }
            l.add(name.substring(i));
            return l;
        }

        // general complicated case, consider escape and quote
        char c;
        char chars[] = name.toCharArray();
        StringBuilder buf = new StringBuilder();
        int status = OUT_OF_QUOTE;
        for (int i = 0; i < chars.length; i++) {
            c = chars[i];

            // check end quote violation
            if (status == QUOTE_ENDED) {
                if (c == '/') {
                    l.add(buf.toString());
                    buf.setLength(0);
                    status = OUT_OF_QUOTE;
                    continue;
                }
                // jndi.0C=End quote is not at the end of element
                throw new InvalidNameException(Messages.getString("jndi.0C")); //$NON-NLS-1$
            }

            if (c == '\\') {
                // escape char
                try {
                    char nc = chars[++i];
                    if (nc == '\\' || nc == '\'' || nc == '"' || nc == '/') {
                        buf.append(nc);
                    } else {
                        buf.append(c);
                        buf.append(nc);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // jndi.0D=Escape cannot be at the end of element
                    throw new InvalidNameException(Messages
                            .getString("jndi.0D")); //$NON-NLS-1$
                }
                continue;
            }
            if (c != '/' && c != '"' && c != '\'') {
                // normal char
                buf.append(c);
                continue;
            }

            // special char
            if (status == OUT_OF_QUOTE && c == '/') {
                l.add(buf.toString());
                buf.setLength(0);
            } else if (status == OUT_OF_QUOTE && c == '\'' && buf.length() == 0) {
                status = IN_SINGLE_QUOTE;
            } else if (status == OUT_OF_QUOTE && c == '"' && buf.length() == 0) {
                status = IN_DOUBLE_QUOTE;
            } else if (status == IN_SINGLE_QUOTE && c == '\'') {
                status = QUOTE_ENDED;
            } else if (status == IN_DOUBLE_QUOTE && c == '"') {
                status = QUOTE_ENDED;
            } else {
                buf.append(c);
            }
        }
        l.add(buf.toString());

        // check end status
        if (status != OUT_OF_QUOTE && status != QUOTE_ENDED) {
            // jndi.0E=Wrong quote usage.
            throw new InvalidNameException(Messages.getString("jndi.0E")); //$NON-NLS-1$
        }
        return l;
    }

    private static boolean isAllSlash(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) != '/') {
                return false;
            }
        }
        return true;
    }

    /*
     * Format name elements to its string representation.
     */
    private static String formatName(Vector<String> elems) {
        // special case: all empty elements
        if (isAllEmptyElements(elems)) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < elems.size(); i++) {
                buf.append("/"); //$NON-NLS-1$
            }
            return buf.toString();
        }

        // general case
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < elems.size(); i++) {
            String elem = elems.get(i);
            if (i > 0) {
                buf.append("/"); //$NON-NLS-1$
            }
            
            // Add quotation while elem contains separater char
            if (elem.indexOf('/') != -1){
                buf.append("\"");
                buf.append(elem);
                buf.append("\"");
            }else{
                buf.append(elem);
            }
        }
        return buf.toString();
    }

    private static boolean isAllEmptyElements(Vector<String> elems) {
        for (int i = 0; i < elems.size(); i++) {
            String elem = elems.get(i);
            if (elem.length() > 0) {
                return false;
            }
        }
        return true;
    }

    public Enumeration<String> getAll() {
        return elems.elements();
    }

    public String get(int index) {
        return elems.get(index);
    }

    public Name getPrefix(int index) {
        if (index < 0 || index > elems.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new CompositeName(elems.subList(0, index));
    }

    public Name getSuffix(int index) {
        if (index < 0 || index > elems.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new CompositeName(elems.subList(index, elems.size()));
    }

    public Name addAll(Name name) throws InvalidNameException {
        if (null == name) {
            throw new NullPointerException();
        }
        if (!(name instanceof CompositeName)) {
            // jndi.0F=Must be a CompositeName
            throw new InvalidNameException(Messages.getString("jndi.0F")); //$NON-NLS-1$
        }

        Enumeration<String> enumeration = name.getAll();
        while (enumeration.hasMoreElements()) {
            elems.add(enumeration.nextElement());
        }
        return this;
    }

    public Name addAll(int index, Name name) throws InvalidNameException {
        if (null == name) {
            throw new NullPointerException();
        }
        if (!(name instanceof CompositeName)) {
            // jndi.0F=Must be a CompositeName
            throw new InvalidNameException(Messages.getString("jndi.0F")); //$NON-NLS-1$
        }

        if (index < 0 || index > elems.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Enumeration<String> enumeration = name.getAll();
        while (enumeration.hasMoreElements()) {
            elems.add(index++, enumeration.nextElement());
        }
        return this;
    }

    public Name add(String element) throws InvalidNameException {
        elems.add(element);
        return this;
    }

    public Name add(int index, String element) throws InvalidNameException {
        if (index < 0 || index > elems.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        elems.add(index, element);
        return this;
    }

    public Object remove(int index) throws InvalidNameException {
        if (index < 0 || index >= elems.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return elems.remove(index);
    }

    public int size() {
        return elems.size();
    }

    public boolean isEmpty() {
        return elems.isEmpty();
    }

    public boolean startsWith(Name name) {
        if (!(name instanceof CompositeName)) {
            return false;
        }

        // check size
        if (name.size() > elems.size()) {
            return false;
        }

        // compare 1 by 1
        Enumeration<String> enumeration = name.getAll();
        String me, he;
        for (int i = 0; enumeration.hasMoreElements(); i++) {
            me = elems.get(i);
            he = enumeration.nextElement();
            if (!(null == me ? null == he : me.equals(he))) {
                return false;
            }
        }
        return true;
    }

    public boolean endsWith(Name name) {
        if (!(name instanceof CompositeName)) {
            return false;
        }

        // check size
        if (name.size() > elems.size()) {
            return false;
        }

        // compare 1 by 1
        Enumeration<String> enumeration = name.getAll();
        String me, he;
        for (int i = elems.size() - name.size(); enumeration.hasMoreElements(); i++) {
            me = elems.get(i);
            he = enumeration.nextElement();
            if (!(null == me ? null == he : me.equals(he))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare this <code>Name</code> with the one supplied as a parameter.
     * The elements of the names are compared in the same way as strings are
     * compared to determine whether this <code>CompositeName</code> is less
     * than, greater than or equal to the supplied object <code>o</code>.
     * 
     * @param o
     *            the object to compare, cannot be null
     * @return a negative number means this is less than the supplied object; a
     *         positive number means this is greater than the supplied object;
     *         zero means this CompositeName equals the object as specified in
     *         the description for the equals method of
     *         <code>CompositeName</code>.
     * @throws ClassCastException
     *             when <code>o</code> is not a <code>CompositeName</code>.
     */
    public int compareTo(Object o) {
        if (o instanceof CompositeName) {
            CompositeName he = (CompositeName) o;
            int r;
            for (int i = 0; i < elems.size() && i < he.elems.size(); i++) {
                r = (elems.get(i)).compareTo(he.elems.get(i));
                if (r != 0) {
                    return r;
                }
            }
            if (elems.size() == he.elems.size()) {
                return 0;
            } else if (elems.size() < he.elems.size()) {
                return -1;
            } else {
                return 1;
            }
        }
        throw new ClassCastException();
    }

    /**
     * Create a copy of this composite name, a complete (deep) copy of the
     * object.
     * 
     * @return a complete (deep) copy of the object.
     */
    @Override
    public Object clone() {
        return new CompositeName(elems);
    }

    /**
     * Returns the string representation of this <code>CompositeName</code>.
     * This is generated by concatenating the elements together with the '/'
     * char added as the separator between each of them. It may be necessary to
     * add quotes and escape chars to preserve the meaning. The resulting string
     * should produce an equivalent <code>CompositeName</code> when used to
     * create a new instance.
     * 
     * @return the string representation of this composite name.
     */
    @Override
    public String toString() {
        return formatName(elems);
    }

    /**
     * Check if this <code>CompositeName</code> is equal to the supplied
     * object.
     * 
     * @param o
     *            the <code>CompositeName</code> to compare - can be null but
     *            then returns false.
     * @return true if they have the same number of elements all of which are
     *         equal. false if they are not equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        // check type
        if (!(o instanceof CompositeName)) {
            return false;
        }

        return this.elems.equals(((CompositeName) o).elems);
    }

    /**
     * Calculate the hashcode of this <code>CompositeName</code> by summing
     * the hash codes of all of its elements.
     * 
     * @return the hashcode of this object.
     */
    @Override
    public int hashCode() {
        int sum = 0;
        for (int i = 0; i < elems.size(); i++) {
            sum += elems.get(i).hashCode();
        }
        return sum;
    }

    /**
     * Writes a serialized representation of the CompositeName. It starts with
     * an int which is the number of elements in the name, and is followed by a
     * String for each element.
     * 
     * @param oos
     * @throws IOException
     *             if an error is encountered writing to the stream.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();

        oos.writeInt(elems.size());
        for (Object element : elems) {
            oos.writeObject(element);
        }
    }

    /**
     * Recreate a CompositeName from the data in the supplied stream.
     * 
     * @param ois
     * @throws IOException
     *             if an error is encountered reading from the stream.
     * @throws ClassNotFoundException.
     */
    private void readObject(ObjectInputStream ois)
            throws OptionalDataException, ClassNotFoundException, IOException {
        ois.defaultReadObject();

        int size = ois.readInt();
        elems = new Vector<String>();
        for (int i = 0; i < size; i++) {
            elems.add((String) ois.readObject());
        }
    }

}
