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

package org.apache.harmony.jndi.provider.ldap.asn1;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.Filter;
import org.apache.harmony.jndi.provider.ldap.parser.FilterParser;

public class ASN1LdapFilterTest extends TestCase {
    public void test_encode() throws Exception {
        FilterParser parser = new FilterParser("(&(cn=1)(o=2))");
        Filter filter = parser.parse();
        LdapASN1Constant.Filter.encode(filter);
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);

        parser = new FilterParser("(object=1)");
        filter = parser.parse();
        LdapASN1Constant.Filter.encode(filter);
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
        
        parser = new FilterParser("(cn=*hello*)");
        filter = parser.parse();
        LdapASN1Constant.Filter.encode(filter);
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
        
        parser = new FilterParser("(sn=Lu\\c4\\8di\\c4\\87)");
        filter = parser.parse();
        LdapASN1Constant.Filter.encode(filter);
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
        
        parser = new FilterParser("(&(objectClass=Person)(|(sn=Jensen)(cn=Babs J*)))");
        filter = parser.parse();
        LdapASN1Constant.Filter.encode(filter);
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
    }
}
