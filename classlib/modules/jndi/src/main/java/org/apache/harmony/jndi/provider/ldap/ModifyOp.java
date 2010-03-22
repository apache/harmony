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

import java.util.ArrayList;
import java.util.List;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

public class ModifyOp implements LdapOperation, ASN1Encodable {

    private String dn;

    private List<Modification> modifications = new ArrayList<Modification>();

    private LdapResult result;

    public ModifyOp(String dn) {
        this.dn = dn;
    }
    
    public ASN1Encodable getRequest() {
        return this;
    }

    public int getRequestId() {
        return LdapASN1Constant.OP_MODIFY_REQUEST;
    }

    public ASN1Decodable getResponse() {
        return result = (result == null) ? new LdapResult() : result;
    }

    public int getResponseId() {
        return LdapASN1Constant.OP_MODIFY_RESPONSE;
    }

    public void encodeValues(Object[] values) {
        values[0] = Utils.getBytes(dn);
        values[1] = modifications;
    }

    /**
     * 
     * @param type
     *            ldap modify type, must be one of ModifyOp.ADD, ModifyOp.DELETE
     *            or ModifyOp.REPLACE
     * @param attr
     */
    public void addModification(int type, LdapAttribute attr) {
        if (type >= 0 && type <= 2 && attr != null) {
            modifications.add(new Modification(type, attr));
        }

        // FIXME: what exception be thrown?
    }

    private static class Modification implements ASN1Encodable {
        int type;

        LdapAttribute attr;

        public Modification(int type, LdapAttribute attr) {
            this.type = type;
            this.attr = attr;
        }

        public void encodeValues(Object[] values) {
            values[0] = ASN1Integer.fromIntValue(type);
            values[1] = attr;
        }
    }

    public LdapResult getResult() {
        return result;
    }
    
}
