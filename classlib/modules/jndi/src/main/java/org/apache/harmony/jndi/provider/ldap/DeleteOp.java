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
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

/**
 * Ldap delete operation. Refer to
 * {@link http://www.rfc-editor.org/rfc/rfc2251.txt} for detailed information
 * 
 */
public class DeleteOp implements LdapOperation, ASN1Encodable {
    /**
     * The distinguished name of the target entry
     */
    private String dn;

    private LdapResult result;

    public DeleteOp(String dn) {
        this.dn = dn;
    }

    public ASN1Encodable getRequest() {
        return this;
    }

    public int getRequestId() {
        return LdapASN1Constant.OP_DEL_REQUEST;
    }

    public ASN1Decodable getResponse() {
        return result = (result == null) ? new LdapResult() : result;
    }

    public int getResponseId() {
        return LdapASN1Constant.OP_DEL_RESPONSE;
    }

    public void encodeValues(Object[] values) {
        values[0] = Utils.getBytes(dn);
    }

    public LdapResult getResult() {
        return result;
    }

    public String getDn() {
        return dn;
    }

}
