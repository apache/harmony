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

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

public class LdapAttributeTest extends TestCase {
    public void test_encode_decode() throws NamingException {
        String id = "test";
        byte[] v1 = Utils.getBytes("value1");
        byte[] v2 = Utils.getBytes("value2");
        byte[] v3 = Utils.getBytes("value3");
        LdapAttribute attr = new LdapAttribute(id, null);
        attr.add(v1);
        attr.add(v2);
        attr.add(v3);
        ASN1TestUtils.checkEncode(attr, LdapASN1Constant.Attribute);

        Object[] encoded = new Object[2];
        attr.encodeValues(encoded);
        LdapAttribute decoded = new LdapAttribute("", null);
        decoded.decodeValues(encoded);

        assertEquals(attr.getID(), decoded.getID());

    }

    public void test_constructor_LAttribute() throws Exception {
        BasicAttribute attr = new BasicAttribute("cn");
        attr.add("test");
        attr.add("harmony");
        LdapAttribute la = new LdapAttribute(attr, null);

        ASN1TestUtils.checkEncode(la, LdapASN1Constant.Attribute);

    }

    public void test_binary_attribute() throws Exception {
        String id = "photo";
        List<byte[]> valueList = new ArrayList<byte[]>();
        byte[] bs = new byte[] { 0, 1, 2, 3, 4, 5 };
        valueList.add(bs);
        bs = Utils.getBytes("value");
        valueList.add(bs);

        Object[] values = new Object[] { Utils.getBytes(id), valueList };

        LdapAttribute la = new LdapAttribute();
        // 'photo' is binary attribute
        la.decodeValues(values);

        for (int i = 0; i < la.size(); ++i) {
            assertTrue(la.get(i) instanceof byte[]);
        }

        id = "cn";
        values = new Object[] { Utils.getBytes(id), valueList };
        la = new LdapAttribute();
        /*
         * 'cn' is not binary attribute, but LdapAttribute.decodeValues()
         * doesn't convert values to string, must call convertValueToString() to
         * do it.
         */
        la.decodeValues(values);

        for (int i = 0; i < la.size(); ++i) {
            assertTrue(la.get() instanceof byte[]);
        }

        la.convertValueToString();
        for (int i = 0; i < la.size(); ++i) {
            assertTrue(la.get() instanceof String);
        }

        id = "cn;binary";
        values = new Object[] { Utils.getBytes(id), valueList };
        la = new LdapAttribute();
        // 'cn;binary' is binary attribute
        la.decodeValues(values);

        for (int i = 0; i < la.size(); ++i) {
            assertTrue(la.get() instanceof byte[]);
        }
    }

    public void test_isBinary() {
        assertTrue(LdapAttribute.isBinary("photo", null));
        assertTrue(LdapAttribute.isBinary("Photo", null));
        assertTrue(LdapAttribute.isBinary("photo", new String[0]));
        assertTrue(LdapAttribute.isBinary("cn;binary", null));
        assertTrue(LdapAttribute.isBinary("cn;binary", new String[0]));

        assertFalse(LdapAttribute.isBinary("cn", null));
        assertFalse(LdapAttribute.isBinary("cn",
                new String[] { "ou", "person" }));
        assertTrue(LdapAttribute.isBinary("cn", new String[] { "ou", "person",
                "cn" }));
        assertTrue(LdapAttribute.isBinary("cn", new String[] { "ou", "person",
                "Cn" }));
    }

    public void test_convertValueToString() throws Exception {
        LdapAttribute attr = new LdapAttribute();

        // do nothing
        attr.convertValueToString();

        BasicAttribute basicAttribute = new BasicAttribute("cn");
        attr = new LdapAttribute(basicAttribute, null);

        // do nothing
        attr.convertValueToString();
        attr.add(Utils.getBytes("test"));
        attr.add(Utils.getBytes("binary"));

        attr.convertValueToString();

        assertEquals(2, attr.size());
        assertEquals("test", attr.get(0));
        assertEquals("binary", attr.get(1));
    }
}
