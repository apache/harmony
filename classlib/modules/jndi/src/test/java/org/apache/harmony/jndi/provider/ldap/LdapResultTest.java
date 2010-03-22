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

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

public class LdapResultTest extends TestCase {
    public void test_decodeValues_$LObject() {
        int resultCode = 10;
        String matchedDN = "matchedDN";
        String errorMessage = "errorMessage";
        String[] referrals = new String[] { "referrals0", "referrals1",
                "referrals2" };
        List<byte[]> list = new ArrayList<byte[]>();
        list.add(Utils.getBytes(referrals[0]));
        list.add(Utils.getBytes(referrals[1]));
        list.add(Utils.getBytes(referrals[2]));

        Object[] values = new Object[] { ASN1Integer.fromIntValue(resultCode),
                Utils.getBytes(matchedDN), Utils.getBytes(errorMessage), list };

        LdapResult result = new LdapResult();
        result.decodeValues(values);

        assertEquals(resultCode, result.getResultCode());
        assertEquals(matchedDN, result.getMachedDN());
        assertEquals(errorMessage, result.getErrorMessage());
        assertEquals(referrals.length, result.getReferrals().length);

        for (int i = 0; i < result.getReferrals().length; ++i) {
            String referral = result.getReferrals()[i];
            assertEquals(referrals[i], referral);
        }
        
        values[3] = null;
        result = new LdapResult();
        result.decodeValues(values);
        
        assertEquals(resultCode, result.getResultCode());
        assertEquals(matchedDN, result.getMachedDN());
        assertEquals(errorMessage, result.getErrorMessage());
        assertNull(result.getReferrals());

        values[3] = new ArrayList<byte[]>();
        result = new LdapResult();
        result.decodeValues(values);
        
        assertEquals(resultCode, result.getResultCode());
        assertEquals(matchedDN, result.getMachedDN());
        assertEquals(errorMessage, result.getErrorMessage());
        assertNull(result.getReferrals());
    }

}
