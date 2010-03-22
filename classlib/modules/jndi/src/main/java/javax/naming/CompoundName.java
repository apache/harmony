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
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * A <code>CompoundName</code> is a series of string elements, and it
 * represents a name in a naming service within a single namespace. Typically
 * these names have a structure which is hierarchical.
 * <p>
 * A <code>CompoundName</code> has a sequence of zero or more elements
 * delimited by the char specified in the property "jndi.syntax.separator". This
 * property is required except when the direction of the name is "flat" (see
 * jndi.syntax.direction). The property "jndi.syntax.separator2" allows for the
 * specification of an additional separator. A separator string will be treated
 * as normal characters if it is preceded by the escape string or is within
 * quotes.
 * </p>
 * <p>
 * The property "jndi.syntax.direction" specifies the direction in which the
 * name is read. Permitted values are "right_to_left", "left_to_right" and
 * "flat". A flat name does not have a hierarchical structure. If this property
 * is not specified then the default is "flat". If this property is specified
 * with an invalid value then an <code>IllegalArgumentException</code> should
 * be raised.
 * </p>
 * <p>
 * Each element can be accessed using its position. The first element is at
 * position 0. The direction of the name is important. When direction is
 * "left_to_right" then the leftmost element is at position 0. Conversely when
 * the direction is "right_to_left" then the rightmost element is at position 0.
 * </p>
 * <p>
 * There are other properties which affect the syntax of a
 * <code>CompoundName</code>. The following properties are all optional:
 * <ul>
 * <li> jndi.syntax.escape - Escape sequence,The escape sequence is used to
 * escape a quote, separator or escape. When preceded itself by the escape
 * sequence it is treated as ordinary characters. When it is followed by chars
 * which are not quote or separator strings then it is treated as ordinary
 * characters</li>
 * <li> jndi.syntax.beginquote - Used as start of quoted string (Defaults to
 * endquote)</li>
 * <li> jndi.syntax.endquote - Used as end of quoted string (Defaults to
 * beginquote)</li>
 * <li> jndi.syntax.beginquote2 - Additionally used as start of quoted string
 * (Defaults to endquote2)</li>
 * <li> jndi.syntax.endquote2 - Additionally used as end of quoted string
 * (Defaults to beginquote2)</li>
 * </ul>
 * <p>
 * When a non-escaped quote appears at the start of an element it must be
 * matched at the end. That element can then be said to be quoted. When an
 * escape sequence appears within a quoted element then it is treated as normal
 * characters unless it precedes an occurrence of the quote in which case it is
 * assumed that the quoted element contains a quote which is escaped.
 * </p>
 * <p>
 * If the element does not start with a quote, then any quote strings within
 * that element are just normal characters.
 * </p>
 * <p>
 * <ul>
 * <li> jndi.syntax.ignorecase - If 'true' then ignore case when name elements
 * are compared. If false or not set then case is important.</li>
 * <li> jndi.syntax.trimblanks - If 'true' then ignore leading & trailing blanks
 * when name elements are compared. If false or not set then blanks are
 * important.</li>
 * </ul>
 * </p>
 * <p>
 * These 2 properties relate to names where the syntax includes
 * attribute/content pairs.
 * <ul>
 * <li>jndi.syntax.separator.ava</li>
 * <li>jndi.syntax.separator.typeval</li>
 * </ul>
 * For example the LDAP name, "CN=Mandy Jennings, O=Apache, C=UK". In this
 * example the pair separator jndi.syntax.separator.ava is ',', and the
 * character that separates pairs jndi.syntax.separator.typeval is '='. See
 * RFC1779 for LDAP naming conventions.
 * </p>
 * <p>
 * The jndi.syntax.separator.ava is not used when manipulating
 * <code>CompoundName</code>. The jndi.syntax.separator is still used to
 * separate elements.
 * </p>
 * <p>
 * The <code>CompoundName</code> needs to be aware of the
 * jndi.syntax.separator.typeval in case of the instance where a quoted string
 * is used to provide the content of a pair.
 * </p>
 * <p>
 * Consider the string "CN=$Mandy Jennings, O=Apache, C=UK" with
 * <ul>
 * <li> jndi.syntax.direction set to "right_to_left"</li>
 * <li> jndi.syntax.separator set to ","</li>
 * <li> jndi.syntax.separator.typeval set to "="</li>
 * </ul>
 * When no jndi.syntax.beginquote is set then this creates a valid
 * <code>CompoundName</code> with 3 elements.
 * </p>
 * <p>
 * If jndi.syntax.beginquote is then set to "$" the name becomes invalid as the
 * content part of the pair CN=$Mandy Jennings has a mismatched quote.
 * </p>
 * <p>
 * The string "CN=$Mandy Jennings$, O=Apache, C=UK" would be fine as the $
 * quotes round Mandy Jennings now balance.
 * </p>
 * <p>
 * A <code>CompoundName</code> may be empty. An empty
 * <code>CompoundName</code> has no elements. Elements may also be empty.
 * </p>
 * 
 * <pre>
 * Some Examples:
 * ==============
 * 
 * Consider the following compound name from the file system namespace:
 *     &quot;home/jenningm-abc/.profile&quot; 
 * 
 * jndi.syntax.separator is set to '/' as in the UNIX filesystem.
 * This name has 3 elements:
 *     home jenningm-abc and .profile
 * The direction should be left_to_right as in the UNIX filesystem
 * The element at position 0 would be home.
 * 
 * Consider if jndi.syntax.separator had been set to '-' then this name 
 * would have 2 elements:
 *     home/jenningm and abc/.profile
 * If the direction was right_to_left then the element at position 0
 * would be abc/.profile.
 * 
 * Consider the name &quot;&lt;ab&lt;cd&gt;ef&gt;&quot; where jndi.syntax.beginquote is &lt;
 * and jndi.syntax.endquote is &gt;. This will give rise to an 
 * InvalidNameException because a close quote was encountered before
 * the end of an element. The same is also true for &quot;&lt;abcd&gt;ef&gt;&quot;.
 * If the name was &quot;ab&lt;cd&gt;ef&quot; then this would be valid and there would
 * be one element ab&lt;cd&gt;ef.
 * However if the name was &quot;&lt;abcdef&gt;&quot; there would be one element abcdef.
 * 
 * An empty
 * <code>
 * CompoundName
 * </code>
 *  is the name &quot;&quot; and has no elements.
 * 
 * When jndi.syntax.beginquote is set to &quot; and beginquote2 is set to '
 * the behaviour is similar to CompositeName - 
 * The name &quot;\&quot;abcd&quot; gives an InvalidNameException as there is no closing quote.
 * The name &quot;'\&quot;abcd'&quot; gives one element of value &quot;abcd.
 * The name &quot;\\abcd&quot; gives one element of value \abcd. 
 *  
 * Assuming:
 *     jndi.syntax.separator is &quot;/&quot;
 *     jndi.syntax.direction is &quot;left_to_right&quot;
 * then
 *     &quot;&quot; is empty. It has no elements.
 *     &quot;/&quot; has one empty element.
 *     &quot;//&quot; has 2 empty elements.
 *     &quot;/a/&quot; has 3 elements the middle one is set to a.
 *     &quot;///&quot; has 3 empty elements.
 *     &quot;//a/&quot; has 4 elements, the last but one is set to a.
 * 
 * 
 * Assuming the only properties set are: 
 *     jndi.syntax.separator is &quot;/&quot;
 *     jndi.syntax.direction is &quot;left_to_right&quot;
 * then the String
 *     &quot;\&quot;&quot; has one element with the value &quot;
 *     &quot;\\\&quot;&quot; has one element with the value \&quot;
 *     &quot;\\\&quot;'&quot; has one element with the value \&quot;'
 * 
 * Assuming the only properties set are:
 *     jndi.syntax.separator is &quot;/&quot;
 *     jndi.syntax.direction is &quot;left_to_right&quot;
 *     jndi.syntax.beginquote is &quot;\&quot;&quot;
 * 
 * then the String
 *     &quot;\&quot;&quot; is invalid because of no closing quote
 *     &quot;\\\&quot;&quot; has one element with the value \&quot;
 *     &quot;\\\&quot;'&quot; has one element with the value \&quot;'
 * 
 * Assuming the only properties set are:
 *     jndi.syntax.separator is &quot;/&quot;
 *     jndi.syntax.direction is &quot;left_to_right&quot;
 *     jndi.syntax.beginquote is &quot;\&quot;&quot;
 *     jndi.syntax.beginquote2 is &quot;\'&quot;
 * then the String
 *     &quot;\&quot;&quot; is invalid because of no closing quote
 *     &quot;\\\&quot;&quot; has one element with the value \&quot;
 *     &quot;\\\&quot;'&quot; has one element with the value \&quot;'
 *     &quot;'\\&quot; is invalid because of no closing quote
 * </pre>
 */
public class CompoundName implements Name {

    /*
     * Note: For serialization purposes, the specified serialVersionUID must be
     * used. This class does not have serializable fields specified. Instead the
     * readObject and writeObject methods are overridden.
     */
    private static final long serialVersionUID = 3513100557083972036L;

    // const for property key
    private static final String SEPARATOR = "jndi.syntax.separator"; //$NON-NLS-1$

    private static final String SEPARATOR_AVA = "jndi.syntax.separator.ava"; //$NON-NLS-1$

    private static final String SEPARATOR_TYPEVAL = "jndi.syntax.separator.typeval"; //$NON-NLS-1$

    private static final String ESCAPE = "jndi.syntax.escape"; //$NON-NLS-1$

    private static final String BEGIN_QUOTE = "jndi.syntax.beginquote"; //$NON-NLS-1$

    private static final String END_QUOTE = "jndi.syntax.endquote"; //$NON-NLS-1$

    private static final String BEGIN_QUOTE2 = "jndi.syntax.beginquote2"; //$NON-NLS-1$

    private static final String END_QUOTE2 = "jndi.syntax.endquote2"; //$NON-NLS-1$

    private static final String IGNORE_CASE = "jndi.syntax.ignorecase"; //$NON-NLS-1$

    private static final String TRIM_BLANKS = "jndi.syntax.trimblanks"; //$NON-NLS-1$

    private static final String DIRECTION = "jndi.syntax.direction"; //$NON-NLS-1$

    private static final String SEPARATOR2 = "jndi.syntax.separator2"; //$NON-NLS-1$

    // const for direction
    private static final String LEFT_TO_RIGHT = "left_to_right"; //$NON-NLS-1$

    private static final String RIGHT_TO_LEFT = "right_to_left"; //$NON-NLS-1$

    private static final String FLAT = "flat"; //$NON-NLS-1$

    // alphabets consts
    private static final String NULL_STRING = ""; //$NON-NLS-1$

    // states consts
    private static final int NORMAL_STATUS = 0;

    private static final int QUOTE1_STATUS = 1;

    private static final int QUOTE2_STATUS = 2;

    private static final int INIT_STATUS = 3;

    private static final int QUOTEEND_STATUS = 4;

    // properties variables
    private transient String separatorString;

    private transient String separatorString2;

    private transient String escapeString;

    private transient String endQuoteString;

    private transient String endQuoteString2;

    private transient String beginQuoteString;

    private transient String beginQuoteString2;

    private transient String sepAvaString;

    private transient String sepTypeValString;

    private transient String direction;

    private transient boolean trimBlanks;

    private transient boolean ignoreCase;

    private transient boolean flat;

    // elements of compound name
    private transient Vector<String> elem;

    // property setting
    protected transient Properties mySyntax;

    /*
     * The specification calls for a protected variable called 'impl' which is
     * of a non-API type. I believe this is an error in the spec, but to be
     * complaint we have implemented this as a useless class (below).
     */
    protected transient javax.naming.NameImpl impl = new NameImpl();

    /**
     * Constructs a <code>CompoundName</code> with supplied
     * <code>Enumeration</code> and <code>Properties</code>
     * 
     * @param elements
     *            an enumeration of name elements, cannot be null
     * @param props
     *            the properties, cannot be null but may be empty. If empty, the
     *            direction defaults to flat and no other properties are
     *            required.
     */
    protected CompoundName(Enumeration<String> elements, Properties props) {
        if (null == props || null == elements) {
            throw new NullPointerException();
        }
        init(props);
        this.elem = new Vector<String>();
        while (elements.hasMoreElements()) {
            this.elem.add(elements.nextElement());
        }
    }

    /**
     * Constructs a <code>CompoundName</code> with supplied
     * <code>String</code> and <code>Properties</code>, taking the supplied
     * <code>s</code> and breaking it down into its elements.
     * 
     * @param s
     *            a string containing the full compound name
     * @param props
     *            the properties, cannot be null but may be empty for a flat
     *            name
     * @throws InvalidNameException
     *             thrown if the supplied <code>String s</code> is invalid
     * @throws NullPointerException
     *             thrown if the supplied <code>String s</code> is null
     */
    public CompoundName(String s, Properties props) throws InvalidNameException {
        if (null == s || null == props) {
            throw new NullPointerException();
        }
        init(props);
        parseName(s);
    }

    /**
     * init instance variables
     */
    private void init(Properties props) {
        trimBlanks = false;
        ignoreCase = false;
        this.mySyntax = props;
        String property;

        // read property settings
        // direction's default value is FLAT
        direction = null == (property = props.getProperty(DIRECTION)) ? FLAT
                : property;
        // if direction value must equals to one of FLAT, LEFT_TO_RIGHT and
        // RIGHT_TO_LEFT, exception threw
        if (!LEFT_TO_RIGHT.equals(direction)
                && !RIGHT_TO_LEFT.equals(direction) && !FLAT.equals(direction)) {
            // jndi.04=Illegal direction property value, which must be one of
            // right_to_left, left_to_right or flat
            throw new IllegalArgumentException(Messages.getString("jndi.04")); //$NON-NLS-1$
        }
        flat = FLAT.equals(direction);

        separatorString = flat ? NULL_STRING : props.getProperty(SEPARATOR);
        // if direction is not FLAT, separator must be set
        if (null == separatorString && !flat) {
            // jndi.05=jndi.syntax.separator property must be set when
            // jndi.syntax.direction is not flat
            throw new IllegalArgumentException(Messages.getString("jndi.05")); //$NON-NLS-1$
        }
        separatorString2 = (flat || null == (property = props
                .getProperty(SEPARATOR2))) ? NULL_STRING : property;

        // ignorecase default value is false
        ignoreCase = null == (property = props.getProperty(IGNORE_CASE)) ? false
                : Boolean.valueOf(property).booleanValue();
        // trimblanks default value is false
        trimBlanks = null == (property = props.getProperty(TRIM_BLANKS)) ? false
                : Boolean.valueOf(property).booleanValue();
        escapeString = null == (property = props.getProperty(ESCAPE)) ? NULL_STRING
                : property;
        beginQuoteString = null == (property = props.getProperty(BEGIN_QUOTE)) ? NULL_STRING
                : property;
        beginQuoteString2 = null == (property = props.getProperty(BEGIN_QUOTE2)) ? NULL_STRING
                : property;
        // end quote string default value is begin quote string
        endQuoteString = null == (property = props.getProperty(END_QUOTE)) ? beginQuoteString
                : property;
        // begin quote string default value is end quote string
        if (NULL_STRING.equals(beginQuoteString)) {
            beginQuoteString = endQuoteString;
        }
        // end quote string2 default value is begin quote string2
        endQuoteString2 = null == (property = props.getProperty(END_QUOTE2)) ? beginQuoteString2
                : property;
        // begin quote string2 default value is end quote string2
        if (NULL_STRING.equals(beginQuoteString2)) {
            beginQuoteString2 = endQuoteString2;
        }

        sepTypeValString = null == (property = props
                .getProperty(SEPARATOR_TYPEVAL)) ? NULL_STRING : property;
        sepAvaString = null == (property = props.getProperty(SEPARATOR_AVA)) ? NULL_STRING
                : property;
    }

    /*
     * parse name from string to elements
     */
    private void parseName(String s) throws InvalidNameException {
        this.elem = new Vector<String>();
        if ("".equals(s)) { //$NON-NLS-1$
            // if empty string, return empty vector
            return;
        }

        // init variables
        int status = INIT_STATUS;
        StringBuilder element = new StringBuilder();
        int pos = 0;
        int length = s.length();
        boolean hasNotNullElement = false;
        boolean includeQuote = false;

        // scan name
        while (pos < length) {
            if (startsWithFromPos(s, pos, endQuoteString)
                    && status == QUOTE1_STATUS) {
                status = QUOTEEND_STATUS;
                pos += addBuffer(element, endQuoteString, includeQuote);
            } else if (startsWithFromPos(s, pos, endQuoteString2)
                    && status == QUOTE2_STATUS) {
                status = QUOTEEND_STATUS;
                pos += addBuffer(element, endQuoteString2, includeQuote);
            } else if (startsWithFromPos(s, pos, beginQuoteString)
                    && status == INIT_STATUS) {
                hasNotNullElement = true;
                status = QUOTE1_STATUS;
                pos += addBuffer(element, beginQuoteString, includeQuote);
            } else if (startsWithFromPos(s, pos, beginQuoteString2)
                    && status == INIT_STATUS) {
                hasNotNullElement = true;
                status = QUOTE2_STATUS;
                pos += addBuffer(element, beginQuoteString2, includeQuote);
            } else if (startsWithFromPos(s, pos, separatorString)
                    && (!flat)
                    && (status == INIT_STATUS || status == QUOTEEND_STATUS || status == NORMAL_STATUS)) {
                hasNotNullElement = hasNotNullElement || element.length() > 0;
                addElement(element);
                status = INIT_STATUS;
                pos += separatorString.length();
                includeQuote = false;
            } else if (startsWithFromPos(s, pos, separatorString2)
                    && (!flat)
                    && (status == INIT_STATUS || status == QUOTEEND_STATUS || status == NORMAL_STATUS)) {
                hasNotNullElement = hasNotNullElement || element.length() > 0;
                addElement(element);
                status = INIT_STATUS;
                pos += separatorString2.length();
                includeQuote = false;
            } else if (startsWithFromPos(s, pos, escapeString)) {
                pos += escapeString.length();
                if (pos == s.length()) {
                    // if this escape char is last character, throw exception
                    // jndi.06=The {0} cannot be at end of the component
                    throw new InvalidNameException(Messages.getString(
                            "jndi.06", escapeString)); //$NON-NLS-1$
                }
                // if one escape char followed by a special char, append the
                // special char to current element
                String str = extractEscapedString(s, pos, status);
                if (null == str) {
                    pos -= escapeString.length();
                    element.append(s.charAt(pos++));
                } else {
                    pos += str.length();
                    element.append(str);
                }

            } else if (startsWithFromPos(s, pos, sepTypeValString)
                    && (status == INIT_STATUS || status == NORMAL_STATUS)) {
                includeQuote = true;
                pos += addBuffer(element, sepTypeValString, true);
                status = INIT_STATUS;
            } else if (startsWithFromPos(s, pos, sepAvaString)
                    && (status == INIT_STATUS || status == NORMAL_STATUS)) {
                includeQuote = true;
                pos += addBuffer(element, sepAvaString, true);
                status = INIT_STATUS;
            } else if (status == QUOTEEND_STATUS) {
                // jndi.07={0}: close quote must appears at end of component in
                // quoted string
                throw new InvalidNameException(Messages.getString("jndi.07", s)); //$NON-NLS-1$
            } else {
                status = status == INIT_STATUS ? NORMAL_STATUS : status;
                element.append(s.charAt(pos++));
            }
        }
        if (QUOTE1_STATUS != status && QUOTE2_STATUS != status) {
            hasNotNullElement = hasNotNullElement || element.length() > 0;
            addElement(element);
        } else {
            // jndi.08={0}: close quote is required for quoted string
            throw new InvalidNameException(Messages.getString("jndi.08", s)); //$NON-NLS-1$
        }
        if (!hasNotNullElement) {
            elem.remove(elem.size() - 1);
        }
    }

    /*
     * add des parameter to StringBuilder if include is true
     */
    private int addBuffer(StringBuilder buffer, String des, boolean include) {
        if (include) {
            buffer.append(des);
        }
        return des.length();
    }

    /*
     * add current content of supplied string buffer as one element of this
     * CompoundName and reset the string buffer to empty
     */
    private void addElement(StringBuilder element) {
        if (LEFT_TO_RIGHT == direction) {
            elem.add(element.toString());
        } else {
            elem.add(0, element.toString());
        }
        element.setLength(0);
    }

    /*
     * find string to be escaped, if cannot find special string(which means,
     * quote, separator and escape), return null
     */
    private String extractEscapedString(String s, int pos, int status) {
        String result = null;
        if (status == QUOTE1_STATUS
                && startsWithFromPos(s, pos, endQuoteString)) {
            result = endQuoteString;
        } else if (status == QUOTE2_STATUS
                && startsWithFromPos(s, pos, endQuoteString2)) {
            result = endQuoteString2;
        } else if (status != QUOTE1_STATUS && status != QUOTE2_STATUS) {
            if (startsWithFromPos(s, pos, beginQuoteString)) {
                result = beginQuoteString;
            } else if (startsWithFromPos(s, pos, beginQuoteString2)) {
                result = beginQuoteString2;
            } else if (startsWithFromPos(s, pos, endQuoteString)) {
                result = endQuoteString;
            } else if (startsWithFromPos(s, pos, endQuoteString2)) {
                result = endQuoteString2;
            } else if (startsWithFromPos(s, pos, separatorString)) {
                result = separatorString;
            } else if (startsWithFromPos(s, pos, separatorString2)) {
                result = separatorString2;
            } else if (startsWithFromPos(s, pos, escapeString)) {
                result = escapeString;
            }
        }
        return result;
    }

    /*
     * justify if string src start with des from position pos
     */
    private boolean startsWithFromPos(String src, int pos, String des) {
        if (null == src || null == des || NULL_STRING.equals(des)
                || src.length() - pos < des.length()) {
            return false;
        }
        int length = des.length();
        int i = -1;
        while (++i < length && src.charAt(pos + i) == des.charAt(i)) {
            // empty body
        }
        return i == length;
    }

    public Enumeration<String> getAll() {
        return this.elem.elements();
    }

    public String get(int index) {
        validateIndex(index, false);
        return elem.elementAt(index);
    }

    /*
     * validate the index, if isInclude is true, index which equals to
     * this.size() is considered as valid, otherwise invalid
     */
    private void validateIndex(int index, boolean isInclude) {
        if (0 > index || index > elem.size()
                || (!isInclude && index == elem.size())) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public Name getPrefix(int index) {
        validateIndex(index, true);
        return new CompoundName(new Vector<String>(elem.subList(0, index))
                .elements(), mySyntax);
    }

    public Name getSuffix(int index) {
        if (index == elem.size()) {
            return new CompoundName(new Vector<String>().elements(), mySyntax);
        }
        validateIndex(index, false);
        return new CompoundName(new Vector<String>(elem.subList(index, elem
                .size())).elements(), mySyntax);
    }

    public Name addAll(Name name) throws InvalidNameException {
        return addAll(elem.size(), name);
    }

    public Name addAll(int index, Name name) throws InvalidNameException {
        if (name == null) {
            // jndi.00=name must not be null
            throw new NullPointerException(Messages.getString("jndi.00")); //$NON-NLS-1$
        }
        if (!(name instanceof CompoundName)) {
            // jndi.09={0} is not a compound name.
            throw new InvalidNameException(Messages.getString(
                    "jndi.09", name.toString())); //$NON-NLS-1$
        }
        if (FLAT.equals(direction) && (this.size() + name.size() > 1)) {
            // jndi.0A=A flat name can only have a single component
            throw new InvalidNameException(Messages.getString("jndi.0A")); //$NON-NLS-1$
        }
        validateIndex(index, true);
        Enumeration<String> enumeration = name.getAll();
        while (enumeration.hasMoreElements()) {
            elem.add(index++, enumeration.nextElement());
        }
        return this;
    }

    public Name add(String element) throws InvalidNameException {
        if (element == null) {
            // jndi.8C=component must not be null
            throw new IllegalArgumentException(Messages.getString("jndi.8C")); //$NON-NLS-1$
        }
        if (FLAT.equals(direction) && (size() > 0)) {
            // jndi.0A=A flat name can only have a single component
            throw new InvalidNameException(Messages.getString("jndi.0A")); //$NON-NLS-1$
        }
        elem.add(element);
        return this;
    }

    /**
     * Insert an element within this CompoundName at the specified index.
     * 
     * @return this <code>CompoundName</code>.
     * @param element
     *            the String to insert
     * @param index
     *            the index of the element to insert - must be greater than or
     *            equal to 0 and less than size().
     * @throws ArrayIndexOutOfBoundsException
     *             thrown when the index is invalid.
     * @throws InvalidNameException
     *             thrown if the insertion of the element results in this
     *             <code>CompoundName</code> becoming invalid.
     */
    public Name add(int index, String element) throws InvalidNameException {
        if (element == null) {
            // jndi.8C=component must not be null
            throw new IllegalArgumentException(Messages.getString("jndi.8C")); //$NON-NLS-1$
        }
        if (FLAT.equals(direction) && (size() > 0)) {
            // jndi.0A=A flat name can only have a single component
            throw new InvalidNameException(Messages.getString("jndi.0A")); //$NON-NLS-1$
        }
        validateIndex(index, true);
        elem.add(index, element);
        return this;
    }

    /**
     * Delete an element from this <code>CompoundName</code>.
     * 
     * @return the deleted element
     * @param index
     *            the index of the element to delete - must be greater than or
     *            equal to 0 and less than size().
     * @throws ArrayIndexOutOfBoundsException
     *             thrown when the index is invalid.
     * @throws InvalidNameException
     *             thrown if the deletion of the element results in this
     *             <code>CompoundName</code> becoming invalid.
     */
    public Object remove(int index) throws InvalidNameException {
        validateIndex(index, false);
        return elem.remove(index);
    }

    @Override
    public Object clone() {
        return new CompoundName(getAll(), mySyntax);
    }

    public int size() {
        return elem.size();
    }

    public boolean isEmpty() {
        return elem.isEmpty();
    }

    public boolean startsWith(Name name) {
        if (!(name instanceof CompoundName)) {
            return false;
        }
        return equals(name, 0, name.size());
    }

    public boolean endsWith(Name name) {
        if (!(name instanceof CompoundName)) {
            return false;
        }
        return equals(name, this.size() - name.size(), name.size());
    }

    /**
     * preprocess string according to trimblank and ignorecase properties
     */
    private String preProcess(String string, boolean caseInsensitive,
            boolean removeBlanks) {
        String result = string;
        if (null != string && !"".equals(string)) { //$NON-NLS-1$
            result = caseInsensitive ? result.toLowerCase() : result;
            result = removeBlanks ? result.trim() : result;
        }
        return result;
    }

    /**
     * Writes a serialized representation of the CompoundName. It starts with
     * the properties, followed by an int which is the number of elements in the
     * name, and is followed by a String for each element.
     * 
     * @throws java.io.IOException
     *             if an error is encountered writing to the stream.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(mySyntax);
        oos.writeInt(elem.size());
        for (int i = 0; i < elem.size(); i++) {
            String element = elem.elementAt(i);
            oos.writeObject(element);
        }
    }

    /**
     * Recreate a CompoundName from the data in the supplied stream.
     * Additionally there are 2 protected fields which are not serializable. One
     * of them is of a type which is a private class and cannot therefore be
     * specified or implemented and so will be excluded from our deliverable.
     * The one protected field which we can spec and implement is as follows:
     * protected Properties mySyntax - The properties associated with a
     * CompoundName.
     * 
     * @throws java.io.IOException
     *             if an error is encountered reading from the stream.
     * @throws ClassNotFoundException.
     */
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        init(((Properties) ois.readObject()));
        int size = ois.readInt();
        elem = new Vector<String>();
        for (int i = 0; i < size; i++) {
            elem.add((String) ois.readObject());
        }
    }

    /**
     * Compare this <code>CompoundName</code> with the one supplied as a
     * param.
     * <p>
     * See the definition of the <code>equals()</code> method to see how the
     * direction, ignorecase and trimblanks properties affect the comparison of
     * a <code>CompoundName</code>. Other than that the comparison is the
     * same as that for a <code>CompositeName</code>.
     * </p>
     * 
     * @return a negative number means this is less than the supplied Object
     *         <code>o</code>. a positive number means this is greater than
     *         the supplied Object <code>o</code>. zero means the two objects
     *         are equal.
     * @param o
     *            the object to compare - cannot be null.
     * @throws ClassCastException
     *             when <code>o</code> is not a compatible class that can be
     *             compared or if the object to compare <code>o</code> is
     *             null.
     */
    public int compareTo(Object o) {
        if (!(o instanceof CompoundName)) {
            throw new ClassCastException();
        }
        int result = -1;
        CompoundName otherName = (CompoundName) o;
        Enumeration<String> otherEnum = otherName.getAll();
        String thisElement;
        String otherElement;
        int i;
        for (i = 0; i < size() && otherEnum.hasMoreElements(); i++) {
            thisElement = preProcess(elem.get(i), ignoreCase, trimBlanks);
            otherElement = preProcess(otherEnum.nextElement(), ignoreCase,
                    trimBlanks);
            result = (null == thisElement ? (null == otherElement ? 0 : -1)
                    : thisElement.compareTo(otherElement));
            if (0 != result) {
                return result;
            }
        }
        if (i < size()) {
            result = 1;
        } else if (otherEnum.hasMoreElements()) {
            result = -1;
        }
        return result;
    }

    /**
     * Calculate the hashcode of this <code>CompoundName</code> by summing the
     * hashcodes of all of its elements.
     * <p>
     * If jndi.syntax.trimblanks is set to true then remove any leading and
     * trailing blanks from the elements before calculating the hashcode.
     * </p>
     * <p>
     * If jndi.syntax.ignorecase is set to true then use the lowercase version
     * of the element to calculate its hashcode.
     * </p>
     * 
     * @return the hashcode of this object.
     */
    @Override
    public int hashCode() {
        int result = 0;
        Enumeration<String> enumeration = elem.elements();
        while (enumeration.hasMoreElements()) {
            result += preProcess(enumeration.nextElement(), ignoreCase,
                    trimBlanks).hashCode();
        }
        return result;
    }

    /**
     * Gets the string representation of this <code>CompoundName</code>.
     * <p>
     * This is generated by concatenating the elements together with the
     * separator string added as the separator between each of them. It may be
     * necessary to add quotes and escape string to preserve the meaning. The
     * resulting string should produce an equivalent <code>CompoundName</code>
     * when used to create a new instance.
     * </p>
     * 
     * @return the string representation of this <code>CompoundName</code>.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String begin = NULL_STRING.equals(beginQuoteString) ? beginQuoteString2
                : beginQuoteString;
        String end = NULL_STRING.equals(endQuoteString) ? endQuoteString2
                : endQuoteString;
        String separator = NULL_STRING.equals(separatorString) ? separatorString2
                : separatorString;
        if (RIGHT_TO_LEFT.equals(direction)) {
            for (int i = elem.size() - 1; i >= 0; i--) {
                addElement(sb, i, separator, begin, end);
            }
        } else {
            for (int i = 0; i < elem.size(); i++) {
                addElement(sb, i, separator, begin, end);
            }
        }
        if (size() * separator.length() < sb.length()) {
            // if the name contains non-empty element, delete the last separator
            // char, which is abundant
            sb.setLength(sb.length() - separator.length());
        }
        return sb.toString();
    }

    private void addElement(StringBuilder sb, int index, String separator,
            String begin, String end) {
        String elemString = elem.get(index);
        if (0 == elemString.length()) {
            // if empty element, append a separator and continue
            sb.append(separator);
            return;
        }
        int pos = sb.length();
        sb.append(elemString);
        if (!NULL_STRING.equals(begin) && !NULL_STRING.equals(end)
                && !NULL_STRING.equals(separator)
                && (0 <= elemString.indexOf(separator))) {
            // if contains separator string, quoted it
            sb.insert(pos, begin);
            pos += begin.length();
            // if quoted, then every endquote char must be escaped
            for (int i = 0, j = 0; 0 <= (j = elemString.indexOf(end, i)); i = j
                    + end.length()) {
                sb.insert(pos + j, escapeString);
                pos += escapeString.length();
            }
            sb.append(end);
        } else {
            if (startsWithFromPos(elemString, 0, beginQuoteString)
                    || startsWithFromPos(elemString, 0, beginQuoteString2)) {
                // if not quoted and start with begin quote string, escape it
                sb.insert(pos, escapeString);
                pos += escapeString.length();
            }
            // if not quoted, escape all separator string and all escape string
            for (int i = 0; i < elemString.length();) {
                if (startsWithFromPos(elemString, i, separatorString)) {
                    sb.insert(pos + i, escapeString);
                    pos += escapeString.length();
                    i += separatorString.length();
                } else if (startsWithFromPos(elemString, i, separatorString2)) {
                    sb.insert(pos + i, escapeString);
                    pos += escapeString.length();
                    i += separatorString2.length();
                } else if (startsWithFromPos(elemString, i, escapeString)) {
                    sb.insert(pos + i, escapeString);
                    pos += escapeString.length();
                    i += escapeString.length();
                } else {
                    i++;
                }
            }
        }
        sb.append(separator);
    }

    /**
     * Check if the supplied object <code>o</code> is equal to this
     * <code>CompoundName</code>.
     * <p>
     * The supplied <code>Object o</code> may be null but that will cause
     * false to be returned.
     * </p>
     * <p>
     * The supplied <code>Object o</code> may be something other than a
     * <code>CompoundName</code> but that will cause false to be returned.
     * </p>
     * <p>
     * To be equal the supplied <code>CompoundName</code> must have the same
     * number of elements and each element must match the corresponding element
     * of this <code>CompoundName</code>. The properties
     * jndi.syntax.ignorecase and jndi.syntax.trimblanks need to be considered
     * if they have been set.
     * </p>
     * <p>
     * The properties associated with the <code>CompoundName</code> must be
     * taken into account but do not have to match. For example
     * "home/jenningm-abc/.profile" with a direction of left to right is equal
     * to ".profile/jenningm-abc/home" with a direction of right to left.
     * </p>
     * 
     * @param o
     *            the object to be compared
     * @return true if supplied object <code>o</code> is equals to this
     *         <code>CompoundName</code>, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompoundName)) {
            return false;
        }

        // compare size
        CompoundName otherName = (CompoundName) o;
        final int size = otherName.size();
        if (size != this.size()) {
            return false;
        }

        // compare every element
        return equals(otherName, 0, size);
    }

    /**
     * compare this name to the supplied <code>name</code> from position
     * <code>start</code> to position <code>start</code>+
     * <code>length</code>-1
     */
    private boolean equals(Name name, int start, int length) {
        if (length > this.size()) {
            return false;
        }
        CompoundName otherName = (CompoundName) name;
        Enumeration<String> otherEnum = otherName.getAll();
        String thisElement;
        String otherElement;
        for (int i = 0; i < length; i++) {
            thisElement = preProcess(elem.get(i + start), ignoreCase,
                    trimBlanks);
            otherElement = preProcess(otherEnum.nextElement(), ignoreCase,
                    trimBlanks);
            if (!(null == thisElement ? null == otherElement : thisElement
                    .equals(otherElement))) {
                return false;
            }
        }
        return true;
    }

}
