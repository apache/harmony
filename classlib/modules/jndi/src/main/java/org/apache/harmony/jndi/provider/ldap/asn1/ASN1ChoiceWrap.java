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

import org.apache.harmony.security.asn1.ASN1Choice;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * When encoding, the value bind to ASN1ChoiceWrap must be Object array length
 * of two or ChosenValue instance. For array, Object[0] is Integer, which is the
 * index of the chosen type (NOTE: not the tag number), Object[1] is the bind
 * value of the chosen type. For ChosenValue instance, methods
 * <code>getIndex()</code> and <code>getValue</code> should return chosen
 * index and value respectively.
 * <p>
 * 
 * When decoding, we always return ChosenValue instance
 */
public class ASN1ChoiceWrap extends ASN1Choice {

    public ASN1ChoiceWrap(ASN1Type[] types) {
        super(types);
    }

    @Override
    public int getIndex(Object object) {
        if (object instanceof ChosenValue) {
            ChosenValue chosen = (ChosenValue) object;
            return chosen.getIndex();
        }
        
        if (object instanceof ASN1Encodable) {
            Object[] value = new Object[1];
            ((ASN1Encodable) object).encodeValues(value);
            return getIndex(value[0]);
        }
        
        Object[] values = (Object[]) object;
        return ((Integer) values[0]).intValue();
    }

    @Override
    public Object getObjectToEncode(Object object) {
        if (object instanceof ChosenValue) {
            ChosenValue chosen = (ChosenValue) object;
            return chosen.getValue();
        }
        
        if (object instanceof ASN1Encodable) {
            Object[] value = new Object[1];
            ((ASN1Encodable) object).encodeValues(value);
            return getObjectToEncode(value[0]);
        }
        
        return ((Object[]) object)[1];
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        super.decode(in);
        return new ChosenValue(in.choiceIndex, getDecodedObject(in));
    }

    public static class ChosenValue {
        private int index;

        private Object value;

        public ChosenValue(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public Object getValue() {
            return value;
        }
    }
}
