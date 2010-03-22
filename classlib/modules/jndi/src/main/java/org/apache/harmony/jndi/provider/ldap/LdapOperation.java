/* 
 *  Licensed to the Apache Software Foundation (ASF) under one or more 
 *  contributor license agreements.  See the NOTICE file distributed with 
 *  this work for additional information regarding copyright ownership. 
 *  The ASF licenses this file to You under the Apache License, Version 2.0 
 *  (the "License"); you may not use this file except in compliance with 
 *  the License.  You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */

package org.apache.harmony.jndi.provider.ldap;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;

/**
 * The Ldap operation interface, contains methods for getting encodable request
 * and decoded response.
 */
public interface LdapOperation {

    /**
     * Get encodable request instance of this operation.
     * 
     * @return encodable request instance of the operation
     */
    public ASN1Encodable getRequest();

    /**
     * Get decodable response instance of this operation.
     * 
     * @return decodable response instance of this operation
     */
    public ASN1Decodable getResponse();

    /**
     * Get request index to determine which operation schema to be used.
     * 
     * @return index of request of this operation
     */
    public int getRequestId();

    /**
     * Get response index.
     * 
     * @return index of response of this operation
     */
    public int getResponseId();

    /**
     * Get <code>LdapResult</code> from response. Except unbind and abandon
     * operation, all ldap operation response contains LdapResult, which
     * indicate whether the operation is successful or what error occurs.
     * 
     * @return instance of <code>LdapResult</code>. <code>null</code> if
     *         operation has not completed or response donesn't contian
     *         LDAPResult.
     */
    public LdapResult getResult();
}
