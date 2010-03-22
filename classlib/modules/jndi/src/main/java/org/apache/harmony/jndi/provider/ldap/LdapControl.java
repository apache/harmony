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
import javax.naming.ldap.Control;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

public class LdapControl implements ASN1Encodable, ASN1Decodable {
    private Control control;

    public LdapControl(Control control) {
        this.control = control;
    }

    public LdapControl() {
        // do nothing
    }

    public void encodeValues(Object[] values) {
        values[0] = Utils.getBytes(control.getID());
        values[1] = Boolean.valueOf(control.isCritical());
        values[2] = control.getEncodedValue();
    }

    public void decodeValues(Object[] values) {
        String id = Utils.getString((byte[]) values[0]);
        boolean isCritical = ((Boolean) values[1]).booleanValue();
        byte[] encoded = (byte[]) values[2];
        control = new BasicControl(id, isCritical, encoded);
    }

    public Control getControl() {
        return control;
    }

}
