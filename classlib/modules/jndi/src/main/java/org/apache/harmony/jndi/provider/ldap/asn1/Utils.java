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

package org.apache.harmony.jndi.provider.ldap.asn1;

import java.io.UnsupportedEncodingException;

import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;

public class Utils {

    public static final String CODING_CHARSET = "UTF-8"; //$NON-NLS-1$

    /**
     * conjoin two ASN1Sequence type as new one
     * 
     * @param first
     *            first ASN1Sequence type
     * @param second
     *            secod ASN1Sequence type
     * @return a new joined ASN1Sequence type
     */
    public static ASN1Sequence conjoinSequence(ASN1Sequence first,
            ASN1Sequence second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        ASN1Type[] result = new ASN1Type[first.type.length + second.type.length];
        System.arraycopy(first.type, 0, result, 0, first.type.length);
        System.arraycopy(second.type, 0, result, first.type.length,
                second.type.length);

        ASN1Sequence sequence = new ASN1SequenceWrap(result);

        System.arraycopy(first.OPTIONAL, 0, sequence.OPTIONAL, 0,
                first.OPTIONAL.length);
        System.arraycopy(second.OPTIONAL, 0, sequence.OPTIONAL,
                first.OPTIONAL.length, second.OPTIONAL.length);

        System.arraycopy(first.DEFAULT, 0, sequence.DEFAULT, 0,
                first.DEFAULT.length);
        System.arraycopy(second.DEFAULT, 0, sequence.DEFAULT,
                first.DEFAULT.length, second.DEFAULT.length);
        return sequence;
    }

    /**
     * Convert <code>obj</code> to <code>String</code>. If obj is byte[],
     * then using UTF-8 charset. when <code>obj</code> is <code>null</code>,
     * empty String would be returned.
     * 
     * @param obj
     *            object to be covert
     * @return UTF-8 String
     */
    public static String getString(Object obj) {
        if (obj == null) {
            return ""; //$NON-NLS-1$
        }

        if (obj instanceof byte[]) {
            try {
                return new String((byte[]) obj, CODING_CHARSET);
            } catch (UnsupportedEncodingException e) {
                // never reached, because UTF-8 is supported by all java
                // platform
                return ""; //$NON-NLS-1$
            }
        } else if (obj instanceof char[]) {
            return new String((char[]) obj);
        } else  {
            return (String) obj;
        }
        
    }

    /**
     * Encodes <code>obj</code> into a sequence of bytes using UTF-8 charset.
     * 
     * @param obj
     *            object to be encoded
     * @return UTF-8 byte[]
     */
    public static byte[] getBytes(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        if (obj instanceof String) {
            try {
                return ((String) obj).getBytes(CODING_CHARSET);
            } catch (UnsupportedEncodingException e) {
                // never reached, because UTF-8 is supported by all java platform
                return new byte[0];
            }
        } else if (obj instanceof char[]) {
            try {
                return new String((char[]) obj).getBytes(CODING_CHARSET);
            } catch (UnsupportedEncodingException e) {
                // never reached, because UTF-8 is supported by all java platform
                return new byte[0];
            }
        } else {
            // no other types, ignore class cast expection
            return (byte[]) obj;
        }
    }
    

    /**
     * Convert <code>obj</code> to <code>char[]</code>. If obj is byte[],
     * then using UTF-8 charset. when <code>obj</code> is <code>null</code>,
     * zero length char array would be returned.
     * 
     * @param obj
     *            object to be covert
     * @return UTF-8 char[]
     */
    public static char[] getCharArray(Object obj) {
        if (obj == null) {
            return new char[0];
        }

        if (obj instanceof String) {
            return ((String) obj).toCharArray();
        } else if (obj instanceof byte[]) {
            try {
                return new String((byte[]) obj, CODING_CHARSET).toCharArray();
            } catch (UnsupportedEncodingException e) {
                // never reached, because UTF-8 is supported by all java
                // platform
                return new char[0];
            }
        } else {
            // no other types, ignore class cast expection
            return (char[]) obj;
        }
    }
}
