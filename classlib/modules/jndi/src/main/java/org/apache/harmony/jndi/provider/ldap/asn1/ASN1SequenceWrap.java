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

import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;

/**
 * When encoding, the value bind to ASN1SequenceWrap should be either array of
 * object or ASN1Encodable instance. If it is object array, the length of the
 * arry should be equals to the length of ASN1SequenceWrap. If it is
 * ASN1Encodable instance, <code>ASN1Encodable.encodeValues()</code> will be
 * invoked.
 * 
 */
public class ASN1SequenceWrap extends ASN1Sequence {

    public ASN1SequenceWrap(ASN1Type[] type) {
        super(type);
    }

    @Override
    protected void getValues(Object object, Object values[]) {
        if (object instanceof ASN1Encodable) {
            ((ASN1Encodable) object).encodeValues(values);
        } else {
            Object[] objs = (Object[]) object;
            System.arraycopy(objs, 0, values, 0, objs.length);
        }
    }
}
