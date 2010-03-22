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

import java.util.Collection;

import javax.naming.directory.SearchControls;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

public class SearchOpTest extends TestCase {
    public void test_encode_decode() throws Exception {
        SearchControls controls = new SearchControls();
        Filter filter = new Filter(Filter.PRESENT_FILTER);
        filter.setValue(Utils.getBytes("objectClass"));
        SearchOp op = new SearchOp("test", controls, filter);

        ASN1TestUtils.checkEncode(op.getRequest(),
                LdapASN1Constant.SearchRequest);

        Object[] encoded = new Object[8];
        op.encodeValues(encoded);
        // return attributes is null, encode to empty collection
        assertNull(controls.getReturningAttributes());
        assertEquals(((Collection) encoded[7]).size(), 0);
    }
}
