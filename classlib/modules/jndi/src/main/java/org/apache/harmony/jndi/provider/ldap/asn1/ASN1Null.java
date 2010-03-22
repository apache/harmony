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

import org.apache.harmony.security.asn1.ASN1Constants;
import org.apache.harmony.security.asn1.ASN1Primitive;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BerOutputStream;

/**
 * This class represents ASN.1 Null type.
 */
public class ASN1Null extends ASN1Primitive {
    private static final ASN1Null asn1Null = new ASN1Null();

    public static ASN1Null getInstance() {
        return asn1Null;
    }

    private ASN1Null() {
        super(ASN1Constants.TAG_NULL);
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        if (in.tag != ASN1Constants.TAG_NULL || in.getLength() != 0) {
            throw new IOException();
        }
        return new byte[0];
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        // do nothing
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        out.length = 0;
    }

}
