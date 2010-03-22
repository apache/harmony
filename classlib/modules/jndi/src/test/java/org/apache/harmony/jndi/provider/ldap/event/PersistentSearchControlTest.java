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

package org.apache.harmony.jndi.provider.ldap.event;

import org.apache.harmony.jndi.provider.ldap.LdapControl;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;

import junit.framework.TestCase;

public class PersistentSearchControlTest extends TestCase {

    public void test_encodeValues_$LObject() {
        PersistentSearchControl controls = new PersistentSearchControl();

        ASN1TestUtils.checkEncode(controls,
                LdapASN1Constant.PersistentSearchControl);

        ASN1TestUtils.checkEncode(new LdapControl(controls),
                LdapASN1Constant.Control);
    }

    public void test_constructor() {
        PersistentSearchControl controls = new PersistentSearchControl();
        assertTrue(controls.isChangesOnly());
        assertEquals(1 | 2 | 4 | 8, controls.getChangeTypes());
        assertEquals(PersistentSearchControl.OID, controls.getID());
        assertTrue(controls.isCritical());
        assertTrue(controls.isReturnECs());
    }
}
