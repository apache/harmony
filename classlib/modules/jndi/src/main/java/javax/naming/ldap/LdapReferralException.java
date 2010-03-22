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

package javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ReferralException;

/**
 * <code>LdapReferralException</code> is an abstract exception which extends
 * <code>ReferralException</code> to handle LDAPv3 request controls.
 */
public abstract class LdapReferralException extends ReferralException {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = -1668992791764950804L;

    /**
     * Default constructor.
     */
    protected LdapReferralException() {
        super();
    }

    /**
     * Constructs an LdapReferralException instance using the supplied text of
     * the message
     * 
     * @param s
     *            the supplied text of the message, which may be null
     */
    protected LdapReferralException(String s) {
        super(s);
    }

    /**
     * Gets referral context without environment properties.
     * 
     * @return referral context
     * @throws NamingException
     *             If cannot get referral context correctly.
     */
    @Override
    public abstract Context getReferralContext() throws NamingException;

    /**
     * Gets referral context with environment properties.
     * 
     * @param h
     *            environment properties
     * @return referral context
     * @throws NamingException
     *             If cannot get referral context correctly.
     */
    @Override
    public abstract Context getReferralContext(Hashtable<?, ?> h)
            throws NamingException;

    /**
     * Gets referral context with environment properties and an array of LDAPv3
     * controls.
     * 
     * @param h
     *            environment properties
     * @param cs
     *            array of LDAPv3 controls
     * @return referral context
     * @throws NamingException
     *             If cannot get referral context correctly.
     */
    public abstract Context getReferralContext(Hashtable<?, ?> h, Control[] cs)
            throws NamingException;

}
