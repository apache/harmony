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

package org.apache.harmony.jndi.provider.ldap.mock;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

public class BindResponse implements ASN1Encodable {

    private EncodableLdapResult result;

    private String saslCreds;

    public BindResponse() {
        this(new EncodableLdapResult(), null);
    }

    public BindResponse(EncodableLdapResult result, String saslCreds) {
        this.result = result;
        this.saslCreds = saslCreds;
    }

    public void encodeValues(Object[] values) {
        result.encodeValues(values);
        if (saslCreds != null) {
            values[4] = Utils.getBytes(saslCreds);
        }
    }

}
