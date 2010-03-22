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

import org.apache.harmony.jndi.internal.PagedResultSearchControlValue;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * TODO: JavaDoc
 */
public final class PagedResultsControl extends BasicControl {

    private static final long serialVersionUID = 6684806685736844298L;

    public static final String OID = "1.2.840.113556.1.4.319"; //$NON-NLS-1$

    /**
     * Static ASN1 Encoder for Paged Result Control using
     * PagedResultSearchControlValue
     */
    static ASN1Type ASN1_ENCODER = new ASN1Sequence(new ASN1Type[] {
            ASN1Integer.getInstance(), // size
            ASN1OctetString.getInstance(), // cookie
    }) {

        public Object getDecodedObject(BerInputStream in) {
            Object values[] = (Object[]) in.content;
            int size = new BigInteger((byte[]) values[0]).intValue();
            byte[] cookie = (byte[]) values[1];
            return new PagedResultSearchControlValue(size, cookie);
        }

        public void getValues(Object object, Object values[]) {
            PagedResultSearchControlValue pg = (PagedResultSearchControlValue) object;
            values[0] = BigInteger.valueOf(pg.getSize()).toByteArray();
            if (pg.getCookie() == null) {
                values[1] = "".getBytes(); //$NON-NLS-1$
            } else {
                values[1] = pg.getCookie();
            }

        }
    };

    public PagedResultsControl(int pageSize, boolean criticality)
            throws IOException {
        super(OID, criticality, null);
        this.value = ASN1_ENCODER.encode(new PagedResultSearchControlValue(
                pageSize, "".getBytes())); //$NON-NLS-1$
    }

    public PagedResultsControl(int pageSize, byte[] cookie, boolean criticality)
            throws IOException {
        super(OID, criticality, null);
        this.value = ASN1_ENCODER.encode(new PagedResultSearchControlValue(
                pageSize, cookie));
    }
}
