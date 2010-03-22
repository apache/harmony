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

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.security.asn1.ASN1Integer;

/**
 * This class represents LDAPResult defined in RFC 2251 page 16.
 */
public class LdapResult implements ASN1Decodable {
    public static final int SUCCESS = 0;

    public static final int OPERATIONS_ERROR = 1;

    public static final int PROTOCOL_ERROR = 2;

    public static final int TIME_LIMIT_EXCEEDED = 3;

    public static final int SIZE_LIMIT_EXCEEDED = 4;

    public static final int COMPARE_FALSE = 5;

    public static final int COMPARE_TRUE = 6;

    public static final int AUTH_METHOD_NOT_SUPPORTED = 7;

    public static final int STRONGER_AUTH_REQUIRED = 8;

    public static final int REFERRAL = 10;

    public static final int ADMIN_LIMIT_EXCEEDED = 11;

    public static final int UNAVAILABLE_CRITICAL_EXTENSION = 12;

    public static final int CONFIDENTIALITY_REQUIRED = 13;

    public static final int SASL_BIND_IN_PROGRESS = 14;

    public static final int NO_SUCH_ATTRIBUTE = 16;

    public static final int UNDEFINED_ATTRIBUTE_TYPE = 17;

    public static final int INAPPROPRIATE_MATCHING = 18;

    public static final int CONSTRAINT_VIOLATION = 19;

    public static final int ATTRIBUTE_OR_VALUE_EXISTS = 20;

    public static final int INVALID_ATTRIBUTE_SYNTAX = 21;

    public static final int NO_SUCH_OBJECT = 32;

    public static final int ALIAS_PROBLEM = 33;

    public static final int INVALID_DN_SYNTAX = 34;

    public static final int ALIAS_DEREFERENCING_PROBLEM = 36;

    public static final int INAPPROPRIATE_AUTHENTICATION = 48;

    public static final int INVALID_CREDENTIALS = 49;

    public static final int INSUFFICIENT_ACCESS_RIGHTS = 50;

    public static final int BUSY = 51;

    public static final int UNAVAILABLE = 52;

    public static final int UNWILLING_TO_PERFORM = 53;

    public static final int LOOP_DETECT = 54;

    public static final int NAMING_VIOLATION = 64;

    public static final int OBJECT_CLASS_VIOLATION = 65;

    public static final int NOT_ALLOWED_ON_NON_LEAF = 66;

    public static final int NOT_ALLOWED_ON_RDN = 67;

    public static final int ENTRY_ALREADY_EXISTS = 68;

    public static final int OBJECT_CLASS_MODS_PROHIBITED = 69;

    public static final int AFFECTS_MULTIPLE_DSAS = 71;

    public static final int OTHER = 80;

    private int resultCode;

    private String machedDN;

    private String errorMessage;

    private String[] referrals;

    @SuppressWarnings("unchecked")
    public void decodeValues(Object[] values) {
        resultCode = ASN1Integer.toIntValue(values[0]);
        machedDN = Utils.getString((byte[]) values[1]);
        errorMessage = Utils.getString((byte[]) values[2]);

        if (values[3] != null) {
            Collection<byte[]> list = (Collection<byte[]>) values[3];
            if (list.size() != 0) {
                referrals = new String[list.size()];
                int index = 0;
                for (byte[] bytes : list) {
                    referrals[index++] = Utils.getString(bytes);
                }
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMachedDN() {
        return machedDN;
    }

    /**
     * Retrieves the referrals.
     * 
     * @return A prossibly null array. <code>null</code> means no referrals
     *         retrieved from server
     */
    public String[] getReferrals() {
        return referrals;
    }

    public int getResultCode() {
        return resultCode;
    }

}
