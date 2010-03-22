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

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;
import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Type;

public class ASN1ChoiceWrapTest extends TestCase {
    private static ASN1ChoiceWrap choice = new ASN1ChoiceWrap(new ASN1Type[] {
            ASN1OctetString.getInstance(), ASN1Integer.getInstance(),
            ASN1Boolean.getInstance() });

    public void test_getIndex_LObject() {
        Object[] values = new Object[] { Integer.valueOf(1),
                Integer.valueOf(100) };
        assertEquals(1, choice.getIndex(values));

        ChosenValue chosen = new ChosenValue(2, Boolean.valueOf(true));
        assertEquals(2, choice.getIndex(chosen));
    }

    public void test_getObjectToEncode_LObject() {
        Object[] values = new Object[] { Integer.valueOf(1),
                Integer.valueOf(100) };
        assertEquals(values[1], choice.getObjectToEncode(values));

        ChosenValue chosen = new ChosenValue(0, "hello");
        assertEquals(chosen.getValue(), choice.getObjectToEncode(chosen));
    }

    public void test_decode_LBerInputStream() throws IOException {
        int index = 2;
        Boolean value = Boolean.FALSE;
        Object[] values = new Object[] { Integer.valueOf(index), value };
        byte[] encoded = choice.encode(values);
        Object decoded = choice.decode(encoded);

        assertTrue(decoded instanceof ChosenValue);
        ChosenValue chosen = (ChosenValue) decoded;

        assertEquals(index, chosen.getIndex());
        assertEquals(value, chosen.getValue());
    }
}
