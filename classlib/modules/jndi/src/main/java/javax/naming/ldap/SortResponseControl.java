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

package javax.naming.ldap;

import java.io.IOException;
import java.math.BigInteger;

import javax.naming.NamingException;

import org.apache.harmony.jndi.internal.SortResult;
import org.apache.harmony.jndi.internal.Util;
import org.apache.harmony.security.asn1.ASN1Enumerated;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * TODO: JavaDoc
 */
public final class SortResponseControl extends BasicControl {

    /**
     * Represents the following ASN1 Syntax to use with the Harmony ASN1 Parser:<br>
     * <br>
     * 
     * SortResult ::= SEQUENCE { sortResult ENUMERATED { success (0), -- results
     * are sorted operationsError (1), -- server internal failure
     * timeLimitExceeded (3), -- timelimit reached before -- sorting was
     * completed strongAuthRequired (8), -- refused to return sorted -- results
     * via insecure -- protocol adminLimitExceeded (11), -- too many matching
     * entries -- for the server to sort noSuchAttribute (16), -- unrecognized
     * attribute -- type in sort key inappropriateMatching (18), -- unrecognized
     * or -- inappropriate matching -- rule in sort key insufficientAccessRights
     * (50), -- refused to return sorted -- results to this client busy (51), --
     * too busy to process unwillingToPerform (53), -- unable to sort other (80) },
     * attributeType [0] AttributeDescription OPTIONAL }
     * 
     */
    static ASN1Type ASN1_SORTRESPONSE = new ASN1Sequence(new ASN1Type[] {
            ASN1Enumerated.getInstance(), // sortResult
            ASN1OctetString.getInstance(), // attributeType
    }) {

        {
            setOptional(1);
        }

        public Object getDecodedObject(BerInputStream in) {
            Object values[] = (Object[]) in.content;
            int sortresult = new BigInteger((byte[]) values[0]).intValue();
            String attrtype = null;
            try {
                attrtype = new String((byte[]) values[1]);
            } catch (NullPointerException e) {
            }
            return new SortResult(sortresult, attrtype);
        }

    };

    private static final long serialVersionUID = 5142939176006310877L;

    private int resultCode;

    private String badAttrId;

    private transient boolean sorted = false;

    public static final String OID = "1.2.840.113556.1.4.474";

    public SortResponseControl(String id, boolean criticality, byte[] value)
            throws IOException {
        super(OID, criticality, value);
        SortResult sr;
        sr = (SortResult) ASN1_SORTRESPONSE.decode(value);
        resultCode = sr.getSortresult();
        badAttrId = sr.getAttributeType();
        if (getResultCode() == 0) {
            sorted = true;
        } else {
            sorted = false;
        }
    }

    public String getAttributeID() {
        return badAttrId;
    }

    public int getResultCode() {
        return resultCode;
    }

    public boolean isSorted() {
        return sorted;
    }

    public NamingException getException() {
        return Util.getExceptionFromErrorCode(getResultCode());
    }

}
