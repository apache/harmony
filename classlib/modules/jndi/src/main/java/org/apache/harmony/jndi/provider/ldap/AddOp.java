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

/**
 * This class represents add operation in ldap protocol, inclue one request and
 * one response. It encode request to ASN.1 and decode response from ASN.1
 * formate bytes to java objects.
 */
final public class AddOp implements LdapOperation, ASN1Encodable {
    // Attribute list
    private List<LdapAttribute> attrList;

    // LDAP distinguished name
    private String entry;

    // LDAP operation result
    private LdapResult result;

    public AddOp(String entry, List<LdapAttribute> attrList) {
        this.entry = entry;
        this.attrList = (attrList == null) ? new ArrayList<LdapAttribute>()
                : attrList;
    }

    public AddOp(String entry) {
        this(entry, null);
    }

    public void addAttribute(LdapAttribute attr) {
        attrList.add(attr);
    }

    public ASN1Encodable getRequest() {
        return this;
    }

    public int getRequestId() {
        return LdapASN1Constant.OP_ADD_REQUEST;
    }

    public ASN1Decodable getResponse() {
        return result = (result == null) ? new LdapResult() : result;
    }

    public int getResponseId() {
        return LdapASN1Constant.OP_ADD_RESPONSE;
    }

    public LdapResult getResult() {
        return result;
    }

    public void encodeValues(Object[] values) {
        values[0] = Utils.getBytes(entry);
        values[1] = attrList;
    }

    public List<LdapAttribute> getAttributeList() {
        return attrList;
    }

    public String getEntry() {
        return entry;
    }

}
