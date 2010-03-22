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

import java.util.ArrayList;
import java.util.List;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

public class EncodableLdapResult implements ASN1Encodable {

    private int resultCode;

    private String machedDN;

    private String errorMessage;

    private String[] referrals;

    public EncodableLdapResult() {
        this(0, "", "", null);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMachedDN() {
        return machedDN;
    }

    public String[] getReferrals() {
        return referrals;
    }

    public int getResultCode() {
        return resultCode;
    }

    public EncodableLdapResult(int resultCode, String machedDN,
            String errorMessage, String[] referrals) {
        this.resultCode = resultCode;
        if (machedDN == null) {
            this.machedDN = "";
        } else {
            this.machedDN = machedDN;
        }
        if (errorMessage == null) {
            this.errorMessage = "";
        } else {
            this.errorMessage = errorMessage;
        }
        this.referrals = referrals;
    }

    public void encodeValues(Object[] values) {
        values[0] = ASN1Integer.fromIntValue(resultCode);
        values[1] = Utils.getBytes(machedDN);
        values[2] = Utils.getBytes(errorMessage);
        if (referrals != null) {
            List<byte[]> refs = new ArrayList<byte[]>();
            for (int i = 0; i < referrals.length; i++) {
                refs.add(Utils.getBytes(referrals[i]));
            }
            values[3] = refs;
        }
    }

}
