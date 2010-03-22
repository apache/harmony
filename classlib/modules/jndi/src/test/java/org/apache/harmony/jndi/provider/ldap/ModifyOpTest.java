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

import javax.naming.directory.DirContext;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;

import junit.framework.TestCase;

public class ModifyOpTest extends TestCase {
    public void test_encodeValues_$LObject() {
        String dn = "test dn";
        ModifyOp op = new ModifyOp(dn);
        
        ASN1TestUtils.checkEncode(op, LdapASN1Constant.ModifyRequest);
        
        LdapAttribute attr = new LdapAttribute("attr1", null);
        attr.add("value1");
        attr.add("value2");
        
        op.addModification(DirContext.ADD_ATTRIBUTE, attr);
        
        attr = new LdapAttribute("attr2", null);
        op.addModification(DirContext.REMOVE_ATTRIBUTE, attr);
        
        ASN1TestUtils.checkEncode(op, LdapASN1Constant.ModifyRequest);
    }
}
