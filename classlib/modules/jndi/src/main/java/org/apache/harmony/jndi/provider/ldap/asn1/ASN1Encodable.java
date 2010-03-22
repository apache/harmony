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

/**
 * This interface is responsible for encoding data according to ASN.1 type schema.
 * Below is type mapping between ASN.1 and Java.
 * <p>
 * Note: the mapping of SEQUENCE and CHOICE is supported only when using
 * corresponding wrapped class ASN1SequenceWrap and ASN1ChoiceWrap
 * <p>
 * <code>
 * ASN.1                Java
 * BOOLEAN              Boolean
 * INTEGER              byte[] (encode: ASN1Integer.fromIntValue(int value)  decode: ASN1Integer.toIntValue(Object decoded))
 * OCTET STRING         byte[] (encode: Utils.getBytes(String s)    decode: getString(byte[] bytes))
 * ENUMERATED           byte[] (encode: ASN1Integer.fromIntValue(int value)  decode: ASN1Integer.toIntValue(Object decoded))
 * SEQUENCE             Object[] or ASN1Encodable
 * SEQUENCE OF          java.util.List
 * SET OF               java.util.List
 * CHOICE               Object[] or ChosenValue
 * </code>
 * 
 * @see org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap
 * @see org.apache.harmony.jndi.provider.ldap.asn1.ASN1SequenceWrap
 */
public interface ASN1Encodable {

    /**
     * Encodes data into <code>values</code>. It is caller's responsibility
     * to create <code>values</code> array, and the size of
     * <code>values</code> is defined by the ASN.1 type schema which is to be
     * encoded. Classes realize this interface need to fill encoded value into
     * the <code>values</code> array.
     * 
     * @param values
     */
    public void encodeValues(Object[] values);
}
