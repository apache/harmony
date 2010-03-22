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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;

/**
 * TODO: JavaDoc
 */
public final class SortControl extends BasicControl {

    /**
     * Represents the following ASN1 Syntax:
     * 
     * SortKey ::= SEQUENCE { attributeType AttributeDescription, orderingRule
     * [0] MatchingRuleId OPTIONAL, reverseOrder [1] BOOLEAN DEFAULT FALSE }
     * 
     */
    static ASN1Sequence ASN1_SORTKEY = new ASN1Sequence(new ASN1Type[] {
            ASN1OctetString.getInstance(),
            new ASN1Implicit(0, ASN1StringType.UTF8STRING),
            new ASN1Implicit(1, ASN1Boolean.getInstance()),

    }) {

        {
            setOptional(1);
            setDefault(Boolean.FALSE, 2);
        }

        /**
         * This method encode a <code>SortKey<code> Object
         * 
         * @param object - a <code>SortKey<code> Object to encode
         * @param values - a Object array to return the encoded values
         */
        public void getValues(Object object, Object values[]) {
            SortKey sk = (SortKey) object;

            try {
                values[0] = sk.getAttributeID().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                //FIXME: is this right thing to do?
                values[0] = sk.getAttributeID().getBytes();
            }
            values[1] = sk.getMatchingRuleID();
            values[2] = Boolean.valueOf(!sk.isAscending());
        }
    };

    /**
     * Represents the following ASN1 Syntax:
     * 
     * SortKeyList ::= SEQUENCE OF SortKey;
     */
    static ASN1SequenceOf ASN1_SORTKEYLIST = new ASN1SequenceOf(ASN1_SORTKEY);

    private static final long serialVersionUID = -1965961680233330744L;

    public static final String OID = "1.2.840.113556.1.4.473"; //$NON-NLS-1$

    public SortControl(String sortBy, boolean criticality) throws IOException {
        super(OID, criticality, null);
        ArrayList<SortKey> list = new ArrayList<SortKey>();
        if (sortBy != null) {
            list.add(new SortKey(sortBy, true, null));
        } else {
            list.add(new SortKey("", true, null)); //$NON-NLS-1$
        }
        value = ASN1_SORTKEYLIST.encode(list);
    }

    public SortControl(String[] sortBy, boolean criticality) throws IOException {
        super(OID, criticality, null);
        ArrayList<SortKey> list = new ArrayList<SortKey>();
        for (int i = 0; i < sortBy.length; i++) {
            if(sortBy[i] != null){
                list.add(new SortKey(sortBy[i], true, null));
            }else{
                list.add(new SortKey("", true, null));
            }
        }
        value = ASN1_SORTKEYLIST.encode(list);
    }

    public SortControl(SortKey[] sortBy, boolean criticality)
            throws IOException {
        super(OID, criticality, null);

        ArrayList<SortKey> list = new ArrayList<SortKey>();
        for (int i = 0; i < sortBy.length; i++) {
            list.add(sortBy[i]);
        }
        this.value = ASN1_SORTKEYLIST.encode(list);
    }

}
