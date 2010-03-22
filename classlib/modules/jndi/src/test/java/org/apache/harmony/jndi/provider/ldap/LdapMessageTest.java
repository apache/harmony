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

import javax.naming.ldap.Control;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

public class LdapMessageTest extends TestCase {

    static class MockAbandonRequest implements ASN1Encodable {
        int messageId;

        public MockAbandonRequest(int id) {
            messageId = id;
        }

        public void encodeValues(Object[] values) {
            values[0] = ASN1Integer.fromIntValue(messageId);
        }

    }

    static class MockDeleteRequest implements ASN1Encodable {
        String dn;

        public MockDeleteRequest(String dn) {
            this.dn = dn;
        }

        public void encodeValues(Object[] values) {
            values[0] = Utils.getBytes(dn);
        }

    }

    static class MockExtendedRequest implements ASN1Encodable {
        String name;

        String value;

        public MockExtendedRequest(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public void encodeValues(Object[] values) {
            values[0] = Utils.getBytes(name);
            values[1] = Utils.getBytes(value);
        }

    }

    public void test_constructor() {
        LdapMessage message = new LdapMessage(null);
        assertNull(message.getControls());
        assertNull(message.getResponseOp());
        assertEquals(-1, message.getMessageId());
        assertEquals(-1, message.getOperationIndex());
    }
    
    public void test_encodeValues_$LObject() {
        LdapMessage message = new LdapMessage(
                LdapASN1Constant.OP_ABANDON_REQUEST, new MockAbandonRequest(1),
                null);
        ASN1TestUtils.checkEncode(message, LdapASN1Constant.LDAPMessage);

        message = new LdapMessage(LdapASN1Constant.OP_DEL_REQUEST,
                new MockDeleteRequest("dn"), null);
        ASN1TestUtils.checkEncode(message, LdapASN1Constant.LDAPMessage);

        message = new LdapMessage(LdapASN1Constant.OP_DEL_REQUEST,
                new MockDeleteRequest("dn"), new Control[0]);
        ASN1TestUtils.checkEncode(message, LdapASN1Constant.LDAPMessage);

        message = new LdapMessage(LdapASN1Constant.OP_EXTENDED_REQUEST,
                new MockExtendedRequest("extended", "test"), new Control[0]);
        ASN1TestUtils.checkEncode(message, LdapASN1Constant.LDAPMessage);
    }
}
