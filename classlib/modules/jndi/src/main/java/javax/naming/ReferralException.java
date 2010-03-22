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

import java.util.Hashtable;

/**
 * A <code>ReferralException</code> is an abstract class used by service
 * providers when dealing with referral exceptions.
 */
public abstract class ReferralException extends NamingException {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = -2881363844695698876L;

    /**
     * Constructs a <code>ReferralException</code> instance with all data
     * initialized to null.
     */
    protected ReferralException() {
        super();
    }

    /**
     * Constructs a <code>ReferralException</code> instance with the specified
     * message. All other fields are initialized to null.
     * 
     * @param s
     *            The detail message for this exception. It may be null.
     */
    protected ReferralException(String s) {
        super(s);
    }

    /**
     * Returns the <code>Context</code> where the method should be resumed
     * following a referral exception. This should not return null.
     * 
     * @return the <code>Context</code> where the method should be resumed
     *         following a referral exception. This should not return null.
     * @throws NamingException
     */
    public abstract Context getReferralContext() throws NamingException;

    /**
     * The same as <code>getReferralContext()</code> except that a
     * <code>Hashtable</code> containing environment properties can be taken
     * to override the environment properties associated with the context that
     * threw this referral exception. This should not return null.
     * 
     * @param h
     *            the environment properties. It may be null and then behaves
     *            the same as <code>getReferralContext()</code>.
     * @return the <code>Context</code> where the method should be resumed
     *         following a referral exception. This should not return null.
     * @throws NamingException
     */
    public abstract Context getReferralContext(Hashtable<?, ?> h)
            throws NamingException;

    /**
     * Returns the information relating to the exception. This should not return
     * null.
     * 
     * @return the information relating to the exception. This should not return
     *         null.
     */
    public abstract Object getReferralInfo();

    /**
     * Returns true when further referral processing is outstanding.
     * 
     * @return true when further referral processing is outstanding.
     */
    public abstract boolean skipReferral();

    /**
     * Retry this referral.
     */
    public abstract void retryReferral();

}
