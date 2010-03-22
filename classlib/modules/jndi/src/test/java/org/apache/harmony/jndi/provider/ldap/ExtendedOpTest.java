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

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

public class ExtendedOpTest extends TestCase {
    public void test_encodeValues_$LObject() {
        ExtendedOp op = new ExtendedOp(new MockExtendedRequest());

        ASN1TestUtils.checkEncode(op, LdapASN1Constant.ExtendedRequest);

        assertEquals(LdapASN1Constant.OP_EXTENDED_REQUEST, op.getRequestId());
        assertEquals(LdapASN1Constant.OP_EXTENDED_RESPONSE, op.getResponseId());
        assertSame(op, op.getRequest());
        assertSame(op, op.getResponse());
    }

    public void test_getExtendedResponse() throws Exception {
        MockExtendedRequest mockRequest = new MockExtendedRequest();
        ExtendedOp op = new ExtendedOp(mockRequest);

        assertSame(mockRequest, op.getExtendedRequest());
        assertNull(op.getExtendedResponse());

        Object[] values = new Object[] { ASN1Integer.fromIntValue(0),
                Utils.getBytes(""), Utils.getBytes(""), null,
                Utils.getBytes("hello"), Utils.getBytes("world") };

        op.decodeValues(values);

        LdapResult result = op.getResult();
        assertNotNull(result);
        assertEquals(0, result.getResultCode());
        assertEquals("", result.getMachedDN());
        assertEquals("", result.getErrorMessage());
        assertNull(result.getReferrals());

        ExtendedResponse response = op.getExtendedResponse();
        assertTrue(response instanceof MockExtendedResponse);
    }

    public class MockExtendedRequest implements ExtendedRequest {

        public ExtendedResponse createExtendedResponse(String s, byte[] value,
                int offset, int length) throws NamingException {
            return new MockExtendedResponse();
        }

        public byte[] getEncodedValue() {
            return new byte[0];
        }

        public String getID() {
            return getClass().getName();
        }

    }

    public class MockExtendedResponse implements ExtendedResponse {

        public byte[] getEncodedValue() {
            return new byte[0];
        }

        public String getID() {
            return getClass().getName();
        }

    }
}
