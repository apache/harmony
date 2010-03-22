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

import java.io.IOException;

import javax.naming.event.NamingEvent;
import javax.naming.ldap.BasicControl;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

/**
 * This class implements EntryChangeNotification control which defined in
 * {@link http://www3.ietf.org/proceedings/01mar/I-D/ldapext-psearch-03.txt}.
 * 
 * 
 */
public class ECNotificationControl extends BasicControl implements
        ASN1Decodable {

    private int changeType;

    private String previousDN;

    private int changeNumber;

    private static final long serialVersionUID = -1540666440189313315L;

    public static final String OID = "2.16.840.1.113730.3.4.7"; //$NON-NLS-1$

    public static final int ADD = 1;

    public static final int DELETE = 2;

    public static final int MODIFY = 4;

    public static final int MODIFY_DN = 8;

    public ECNotificationControl(byte[] encoded) {
        super(OID, true, encoded);

        decodeContend();
    }

    private void decodeContend() {
        try {
            Object[] values = (Object[]) LdapASN1Constant.EntryChangeNotificationControl
                    .decode(value);
            decodeValues(values);
        } catch (IOException e) {
            // FIXME how to deal with the exception
        }
    }

    public int getChangeNumber() {
        return changeNumber;
    }

    /**
     * get enumerated change type value defined in
     * {@link http://www3.ietf.org/proceedings/01mar/I-D/ldapext-psearch-03.txt}
     * 
     * @return change type value
     */
    public int getChangeType() {
        return changeType;
    }

    /**
     * get JNDI defined change type value which is different with
     * <code>getChangeType()</code>
     * 
     * @return JNDI defined change type value
     */
    public int getJNDIChangeType() {
        switch (changeType) {
        case ADD:
            return NamingEvent.OBJECT_ADDED;
        case DELETE:
            return NamingEvent.OBJECT_REMOVED;
        case MODIFY:
            return NamingEvent.OBJECT_CHANGED;
        case MODIFY_DN:
            return NamingEvent.OBJECT_RENAMED;
        default:
            // never reach
            return -1;
        }

    }

    public String getPreviousDN() {
        return previousDN;
    }

    public void decodeValues(Object[] values) {
        changeType = ASN1Integer.toIntValue(values[0]);
        if (values[1] != null) {
            previousDN = Utils.getString(values[1]);
        }

        if (values[2] != null) {
            changeNumber = ASN1Integer.toIntValue(values[2]);
        }
    }

}
