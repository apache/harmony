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

import javax.naming.ldap.BasicControl;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.security.asn1.ASN1Integer;

/**
 * This class implements PersistentSearch control defined in
 * {@link http://www3.ietf.org/proceedings/01mar/I-D/ldapext-psearch-03.txt}.
 * 
 * This control extend LDAPv3 search operation, so client can receive
 * notifications of changes from server.
 */
public class PersistentSearchControl extends BasicControl implements
        ASN1Encodable {

    private static final long serialVersionUID = -524784347976291935L;

    public static final String OID = "2.16.840.1.113730.3.4.3"; //$NON-NLS-1$

    private int changeTypes;

    private boolean changesOnly;

    private boolean returnECs;

    public PersistentSearchControl() {
        // register all types of changes
        this(1 | 2 | 4 | 8, true, true);
    }

    public PersistentSearchControl(int types, boolean changesOnly,
            boolean returnECs) {
        super(OID, true, null);
        this.changesOnly = changesOnly;
        this.returnECs = returnECs;
        this.changeTypes = types;
        value = getValue();
    }

    /**
     * Get encoded content of persistent search control, which not include
     * <code>OID</code>
     * 
     * @return encoded content
     */
    private byte[] getValue() {
        return LdapASN1Constant.PersistentSearchControl.encode(this);
    }

    public void encodeValues(Object[] values) {
        values[0] = ASN1Integer.fromIntValue(changeTypes);
        values[1] = Boolean.valueOf(changesOnly);
        values[2] = Boolean.valueOf(returnECs);

    }

    public boolean isChangesOnly() {
        return changesOnly;
    }

    public int getChangeTypes() {
        return changeTypes;
    }

    public boolean isReturnECs() {
        return returnECs;
    }

}
