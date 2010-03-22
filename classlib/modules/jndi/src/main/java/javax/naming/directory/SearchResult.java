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

import javax.naming.Binding;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * <code>SearchResult</code> returned from a search on a directory context and
 * is provided by a <code>NamingEnumeration</code>.
 * <p>
 * This class is not thread-safe.
 * </p>
 */
public class SearchResult extends Binding {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = 0x80e805ecc9ed1c5cL;

    /**
     * The attributes that were matched for this object.
     * 
     * @serial
     */
    private Attributes attrs;

    /**
     * Creates a new instance of <code>SearchResult</code> with name
     * <code>s</code>, bound object <code>o</code> and an
     * <code>attributes</code>
     * 
     * @param s
     *            the name of this result. <code>s</code> should be relative
     *            to the target context for the search that produces this
     *            result.
     * @param o
     *            bound object of this result. The class of <code>o</code> is
     *            the value that will be returned by
     *            <code>Binding.getClassName()</code> for this search result,
     *            except where <code>setClassName()</code> is used to set a
     *            different value. If <code>o</code> is null,
     *            <code>getClassName()</code> will return null.
     * @param attributes
     *            The <code>attributes</code> should not be null. If there are
     *            no attributes for this search result, this parameter should be
     *            an empty collection.
     */
    public SearchResult(String s, Object o, Attributes attributes) {
        this(s, null, o, attributes, true);
    }

    /**
     * Creates a new instance of <code>SearchResult</code> with name
     * <code>s</code>, bound object <code>o</code>, an
     * <code>attributes</code> and a boolean <code>flag</code>
     * 
     * @param s
     *            the name of this result. A true value of <code>flag</code>
     *            means <code>s</code> is relative to the target context of
     *            the search that produces this result. A false value of
     *            <code>flag</code> means that <code>s</code> is a URL
     *            string.
     * @param o
     *            bound object of this result. The class of <code>o</code> is
     *            the value that will be returned by
     *            <code>Binding.getClassName()</code> for this search result,
     *            except where <code>setClassName()</code> is used to set a
     *            different value. If <code>o</code> is null,
     *            <code>getClassName()</code> will return null.
     * @param attributes
     *            The <code>attributes</code> should not be null. If there are
     *            no attributes for this search result, this parameter should be
     *            an empty collection.
     * @param flag
     *            A true value of <code>flag</code> means <code>s</code> is
     *            relative to the target context of the search that produces
     *            this result. A false value of <code>flag</code> means that
     *            <code>s</code> is a URL string.
     * 
     */
    public SearchResult(String s, Object o, Attributes attributes, boolean flag) {
        this(s, null, o, attributes, flag);
    }

    /**
     * Creates a new instance of <code>SearchResult</code> with name
     * <code>s</code>, class name <code>s1</code> bound object
     * <code>o</code> and an <code>attributes</code>
     * 
     * @param s
     *            the name of this result. <code>s</code> should be relative
     *            to the target context for the search that produces this
     *            result.
     * @param s1
     *            If <code>s1</code> is not null, it specifies the name of the
     *            class of the bound object <code>o</code>. Passing a null
     *            value for <code>s1</code> will not stop
     *            <code>Binding.getClassName()</code> returning the name of
     *            the class of <code>o</code>.
     * @param o
     *            bound object of this result. The class of <code>o</code> is
     *            the value that will be returned by
     *            <code>Binding.getClassName()</code> for this search result,
     *            except where <code>setClassName()</code> is used to set a
     *            different value. If <code>o</code> is null,
     *            <code>getClassName()</code> will return null.
     * @param attributes
     *            The <code>attributes</code> should not be null. If there are
     *            no attributes for this search result, this parameter should be
     *            an empty collection.
     */
    public SearchResult(String s, String s1, Object o, Attributes attributes) {
        this(s, s1, o, attributes, true);
    }

    /**
     * Creates a new instance of <code>SearchResult</code> with name
     * <code>s</code>, class name <code>s1</code> bound object
     * <code>o</code> , an <code>attributes</code> and a boolean
     * <code>flag</code>
     * 
     * @param s
     *            the name of this result. A true value of <code>flag</code>
     *            means <code>s</code> is relative to the target context of
     *            the search that produces this result. A false value of
     *            <code>flag</code> means that <code>s</code> is a URL
     *            string.
     * @param s1
     *            If <code>s1</code> is not null, it specifies the name of the
     *            class of the bound object <code>o</code>. Passing a null
     *            value for <code>s1</code> will not stop
     *            <code>Binding.getClassName()</code> returning the name of
     *            the class of <code>o</code>.
     * @param o
     *            bound object of this result. The class of <code>o</code> is
     *            the value that will be returned by
     *            <code>Binding.getClassName()</code> for this search result,
     *            except where <code>setClassName()</code> is used to set a
     *            different value. If <code>o</code> is null,
     *            <code>getClassName()</code> will return null.
     * @param attributes
     *            The <code>attributes</code> should not be null. If there are
     *            no attributes for this search result, this parameter should be
     *            an empty collection.
     * @param flag
     *            A true value of <code>flag</code> means <code>s</code> is
     *            relative to the target context of the search that produces
     *            this result. A false value of <code>flag</code> means that
     *            <code>s</code> is a URL string.
     */
    public SearchResult(String s, String s1, Object o, Attributes attributes,
            boolean flag) {
        super(s, s1, o, flag);

        if (attributes == null) {
            // jndi.8B=attrs must not be null
            throw new IllegalArgumentException(Messages.getString("jndi.8B")); //$NON-NLS-1$
        }
        this.attrs = attributes;
    }

    /**
     * Gets attributes of this search result
     * 
     * @return an attributes. It should not be null.
     */
    public Attributes getAttributes() {
        return attrs;
    }

    /**
     * Sets attributes of this search result
     * 
     * @param attributes
     *            an attributes. It should not be null.
     */
    public void setAttributes(Attributes attributes) {
        if (attributes == null) {
            // jndi.8B=attrs must not be null
            throw new IllegalArgumentException(Messages.getString("jndi.8B")); //$NON-NLS-1$
        }
        this.attrs = attributes;
    }

    /**
     * Return a concatenation of the <code>toString()</code> value for the
     * binding, and the <code>toString()</code> values for the attributes,
     * joined by colons.
     * 
     * @return string representation of this search result
     */
    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(":") //$NON-NLS-1$
                .append(attrs.toString()).toString();
    }

}
