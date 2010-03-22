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

import org.apache.harmony.jndi.provider.ldap.Filter;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;
import org.apache.harmony.security.asn1.ASN1Choice;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BerOutputStream;

/**
 * This class deal with recursive definition of ASN.1 choice type, it's used to
 * define Ldap Filter type schema.
 */
public class ASN1LdapFilter extends ASN1Type {

    private static ASN1Choice type = null;

    public ASN1LdapFilter() {
        super(TAG_CHOICE); // has not tag number
    }

    @Override
    public final boolean checkTag(int identifier) {
        return true;
    }

    @Override
    public Object decode(BerInputStream in) throws IOException {
        return null;
    }

    @Override
    public void encodeASN(BerOutputStream out) {
        if (type == null) {
            type = (ASN1Choice) LdapASN1Constant.Filter;
        }
        // has not been decoded
        if (!(out.content instanceof byte[])) {
            ChosenValue chosen = (ChosenValue) out.content;
            int index = chosen.getIndex();
            byte[] bytes = type.type[index].encode(chosen.getValue());
            out.content = bytes;
            out.length = bytes.length;
        }

        out.encodeANY();
    }

    @Override
    public void encodeContent(BerOutputStream out) {
        // FIXME: do nothing if never be called
        throw new RuntimeException();
    }

    @Override
    public void setEncodingContent(BerOutputStream out) {
        if (type == null) {
            type = (ASN1Choice) LdapASN1Constant.Filter;
        }

        ChosenValue chosen = null;
        if (out.content instanceof Filter) {
            Object[] values = new Object[1];
            ((Filter) out.content).encodeValues(values);
            chosen = (ChosenValue) values[0];
        } else {
            chosen = (ChosenValue) out.content;
        }

        int index = chosen.getIndex();
        byte[] bytes = type.type[index].encode(chosen.getValue());

        out.content = bytes;
        out.length = bytes.length;
    }

    @Override
    public int getEncodedLength(BerOutputStream out) {
        return out.length;
    }
}
