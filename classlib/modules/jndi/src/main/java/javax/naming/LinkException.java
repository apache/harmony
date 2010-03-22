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

/**
 * Naming operations may throw a <code>LinkException</code> when attempting to
 * resolve links. Methods are provided to save diagnostic information about how
 * far link resolution has progressed.
 * <p>
 * Multithreaded access to a single <code>LinkException</code> instance is
 * only safe when client code uses appropriate synchronization and locking.
 * </p>
 */
public class LinkException extends NamingException {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private final static long serialVersionUID = -7967662604076777712L;

    /**
     * Description of why the link could not be resolved.
     */
    protected String linkExplanation;

    /**
     * Composite name containing the name which could not be resolved.
     */
    protected Name linkRemainingName;

    /**
     * Composite name containing the name which was resolved.
     */
    protected Name linkResolvedName;

    /**
     * Contains the object that linkResolvedName relates to.
     */
    protected Object linkResolvedObj;

    /**
     * Constructs a <code>LinkException</code> instance with all data
     * initialized to null.
     */
    public LinkException() {
        super();
    }

    /**
     * Constructs a <code>LinkException</code> instance with the specified
     * message.
     * 
     * @param s
     *            The detail message for the exception. It may be null.
     */
    public LinkException(String s) {
        super(s);
    }

    /**
     * Outputs the string representation of this <code>NamingException</code>
     * together with the details of the remaining name.
     * 
     * @return the string representation of this <code>NamingException</code>
     *         together with the details of the remaining name.
     */
    @Override
    public String toString() {
        return toStringImpl(false);
    }

    private String toStringImpl(boolean b) {
        StringBuilder sb = new StringBuilder(super.toString());
        sb
                .append("; the link remaining name is - '").append(linkRemainingName).append( //$NON-NLS-1$
                        "'"); //$NON-NLS-1$
        if (b && null != linkResolvedObj) {
            sb.append("; the link resolved object is - '").append( //$NON-NLS-1$
                    linkResolvedObj).append("'"); //$NON-NLS-1$
        }
        return sb.toString();
    }

    /**
     * Outputs the string representation of this <code>NamingException</code>
     * together with the details of the remaining name.
     * <p>
     * If boolean b is set to true then also outputs the resolved object.<br/>
     * If boolean b is set to false then the behavior is the same as
     * <code>toString()</code>.
     * 
     * @param b
     *            Indicates if the resolved object need to be outputted.
     * @return the string representation of this <code>NamingException</code>
     *         together with the details of the remaining name.
     */
    @Override
    public String toString(boolean b) {
        return toStringImpl(b);
    }

    /**
     * Retrieves the value of the <code>linkExplanation</code> field.
     * 
     * @return the value of the <code>linkExplanation</code> field.
     */
    public String getLinkExplanation() {
        return linkExplanation;
    }

    /**
     * Retrieves the value of the <code>linkRemainingName</code> field.
     * 
     * @return the value of the <code>linkRemainingName</code> field.
     */
    public Name getLinkRemainingName() {
        return linkRemainingName;
    }

    /**
     * Retrieves the value of the <code>linkResolvedName</code> field.
     * 
     * @return the value of the <code>linkResolvedName</code> field.
     */
    public Name getLinkResolvedName() {
        return linkResolvedName;
    }

    /**
     * Retrieves the value of the <code>linkResolvedObj</code> field.
     * 
     * @return the value of the <code>linkResolvedObj</code> field.
     */
    public Object getLinkResolvedObj() {
        return linkResolvedObj;
    }

    /**
     * Sets the <code>linkExplanation</code> field to the specified value.
     * 
     * @param string
     *            the new <code>linkExplanation</code> value to be set.
     */
    public void setLinkExplanation(String string) {
        linkExplanation = string;
    }

    /**
     * Sets the <code>linkRemainingName</code> to the specified name. It may
     * be null. The remaining name details must not change even if the original
     * <code>Name</code> itself changes.
     * 
     * @param name
     *            the new <code>linkRemainingName</code> value to be set. It
     *            may be null.
     */
    public void setLinkRemainingName(Name name) {
        linkRemainingName = null == name ? null : (Name) name.clone();
    }

    /**
     * Sets the <code>linkResolvedName</code> to the specified name. This may
     * be null. The resolved name details must not change even if the original
     * <code>Name</code> itself changes.
     * 
     * @param name
     *            the new <code>linkResolvedName</code> value to be set.
     */
    public void setLinkResolvedName(Name name) {
        linkResolvedName = null == name ? null : (Name) name.clone();
    }

    /**
     * Sets the <code>linkResolvedObj</code> field to object. This may be
     * null.
     * 
     * @param object
     *            the new <code>linkResolvedObj</code> value to be set.
     */
    public void setLinkResolvedObj(Object object) {
        linkResolvedObj = object;
    }

}
