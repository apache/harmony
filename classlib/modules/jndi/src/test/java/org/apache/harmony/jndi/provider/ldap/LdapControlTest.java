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

import javax.naming.ldap.BasicControl;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;

public class LdapControlTest extends TestCase {
    public void test_encodeValues_$LObject() {
        LdapControl control = new LdapControl(new BasicControl("id", true,
                new byte[10]));
        ASN1TestUtils.checkEncode(control, LdapASN1Constant.Control);
        
        //controlValue is optional, so it could be null
        control = new LdapControl(new BasicControl("id2", false, null));
        ASN1TestUtils.checkEncode(control, LdapASN1Constant.Control);
    }
}
