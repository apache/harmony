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

public class ModifyDNOp implements LdapOperation, ASN1Encodable {
    private LdapResult result;

    private String entry;

    private String newrdn;

    private boolean deleteoldrdn;

    private String newSuperior;

    public ModifyDNOp(String entry, String newrdn, boolean deleteoldrdn,
            String newSuperior) {
        this.entry = entry;
        this.newrdn = newrdn;
        this.deleteoldrdn = deleteoldrdn;
        this.newSuperior = newSuperior;
    }

    public ASN1Encodable getRequest() {
        return this;
    }

    public int getRequestId() {
        return LdapASN1Constant.OP_MODIFY_DN_REQUEST;
    }

    public ASN1Decodable getResponse() {
        return result = (result == null) ? new LdapResult() : result;
    }

    public int getResponseId() {
        return LdapASN1Constant.OP_MODIFY_DN_RESPONSE;
    }

    public void encodeValues(Object[] values) {
        values[0] = Utils.getBytes(entry);
        values[1] = Utils.getBytes(newrdn);
        values[2] = Boolean.valueOf(deleteoldrdn);
        if (newSuperior != null) {
            values[3] = Utils.getBytes(newSuperior);
        }
    }

    public LdapResult getResult() {
        return result;
    }

    public boolean isDeleteoldrdn() {
        return deleteoldrdn;
    }

    public String getEntry() {
        return entry;
    }

    public String getNewrdn() {
        return newrdn;
    }

    public String getNewSuperior() {
        return newSuperior;
    }

}
