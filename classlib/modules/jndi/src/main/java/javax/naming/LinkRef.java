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

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This is a type of <code>Reference</code> used to point to an address of
 * type "LinkAddress" where the address given is actually the string
 * representation of a valid <code>Name</code>.
 * 
 * @see Reference
 */
public class LinkRef extends Reference {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = -5386290613498931298L;

    /*
     * The type name of the address this LinkRef points to.
     */
    private static final String ADDR_TYPE = "LinkAddress"; //$NON-NLS-1$

    /**
     * Constructs a <code>LinkRef</code> instance using the supplied
     * <code>name
     * </code> of <code>Name</code> representation. The class name
     * is set to the name of this <code>LinkRef</code> class. The factory
     * class and location default to null. There is one address entry which has
     * "LinkAddress" as the address type and the string representation of the
     * supplied name as the address.
     * 
     * @param name
     *            the <code>Name</code> to be used as a link which cannot be
     *            null
     */
    public LinkRef(Name name) {
        this(name.toString());
    }

    /**
     * Constructs a <code>LinkRef</code> instance using the supplied
     * <code>name
     * </code> of <code>String</code> representation. The class
     * name is set to the name of this <code>LinkRef</code> class. The factory
     * class and location default to null. There is one address entry which has
     * "LinkAddress" as the address type and the string representation of the
     * supplied name as the address.
     * 
     * @param s
     *            the name to be used as a link which cannot be null
     */
    public LinkRef(String s) {
        super(LinkRef.class.getName(), new StringRefAddr(ADDR_TYPE, s));
    }

    /**
     * Gets the string representation of the name used as a link which cannot be
     * null.
     * 
     * @return the string representation of the name used as a link
     * @throws MalformedLinkException
     *             If this is not a <code>Reference</code> with a class name
     *             which matches the name of this LinkRef class.
     * @throws NamingException
     *             If other <code>NamingException</code> is encountered.
     */
    public String getLinkName() throws NamingException {
        if (!LinkRef.class.getName().equals(this.getClassName())) {
            // jndi.11=This is an invalid LinkRef object\!
            throw new MalformedLinkException(Messages.getString("jndi.11")); //$NON-NLS-1$
        }
        try {
            RefAddr addr = get(ADDR_TYPE);
            if (null == addr) {
                // jndi.12=There is no address with type: {0}
                throw new MalformedLinkException(Messages.getString(
                        "jndi.12", ADDR_TYPE)); //$NON-NLS-1$
            }
            return (String) addr.getContent();
        } catch (NullPointerException e) {
            throw new MalformedLinkException(e.getMessage());
        }
    }

}
