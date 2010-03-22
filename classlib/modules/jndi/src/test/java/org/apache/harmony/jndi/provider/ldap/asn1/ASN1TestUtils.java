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

import java.lang.reflect.Field;
import java.util.Collection;

import junit.framework.Assert;

import org.apache.harmony.jndi.provider.ldap.DeleteOp;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;
import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Enumerated;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.ASN1ValueCollection;

public class ASN1TestUtils {
    private static final String ASN1ENCODABLE_NAME = ASN1Encodable.class.getCanonicalName();
    private static final String CHOSENVALUE_NAME = ChosenValue.class.getCanonicalName();

    public static void checkEncode(Object value, ASN1Type type) {
        if (type instanceof ASN1Implicit) {
            type = getWrappedType((ASN1Implicit) type);
        }

        if (type instanceof ASN1Integer || type instanceof ASN1Enumerated) {
            Assert.assertTrue(value instanceof byte[]);

            Assert.assertTrue("value should not be zero-length byte array",
                    ((byte[]) value).length != 0);
        } else if (type instanceof ASN1Boolean) {
            Assert.assertTrue(value instanceof Boolean);
        } else if (type instanceof ASN1OctetString) {
            if(value instanceof DeleteOp) {
                Object[] objs = new Object[1];
                ((DeleteOp)value).encodeValues(objs);
                value = objs[0];
            }
            Assert.assertTrue(value instanceof byte[]);
        } else if (type instanceof ASN1SequenceWrap) {
            checkEncodeSequence(value, (ASN1SequenceWrap) type);
        } else if (type instanceof ASN1SequenceOf || type instanceof ASN1SetOf) {
            Assert.assertTrue(value instanceof Collection);
            Collection collection = (Collection) value;
            for (Object object : collection) {
                checkEncode(object, ((ASN1ValueCollection) type).type);
            }
        } else if (type instanceof ASN1ChoiceWrap) {
            checkEncodeChoice(value, (ASN1ChoiceWrap) type);
        }  else if (type instanceof ASN1LdapFilter) {
            checkEncodeFilter(value, (ASN1LdapFilter) type);
        } else {
            Assert.fail("Not supported ASN.1 type");
        }
    }

    private static void checkEncodeFilter(Object value, ASN1LdapFilter filter) {
        checkEncode(value, LdapASN1Constant.Filter);
    }

    private static void checkEncodeChoice(Object value, ASN1ChoiceWrap type) {
        if (value instanceof Object[]) {
            Object[] objs = (Object[]) value;
            Assert.assertEquals(2, objs.length);
            Assert.assertTrue(objs[0] instanceof Integer);

            int index = ((Integer) objs[0]).intValue();
            checkEncode(objs[1], type.type[index]);
        } else if (value instanceof ChosenValue) {
            ChosenValue chosen = (ChosenValue) value;
            checkEncode(chosen.getValue(), type.type[chosen.getIndex()]);
        } else if (value instanceof ASN1Encodable) {
            Object[] objs = new Object[1];
            ((ASN1Encodable) value).encodeValues(objs);
            checkEncode(objs[0], type);
        } else {

            Assert.fail("Value for ASN1ChoiceWrap should be Object[2], "
                    + CHOSENVALUE_NAME + " or "
                    + ASN1ENCODABLE_NAME);
        }
    }

    private static void checkEncodeSequence(Object value, ASN1SequenceWrap type) {
        if (value instanceof Object[]) {
            Object[] objs = (Object[]) value;
            Assert.assertEquals(type.type.length, objs.length);
            for (int i = 0; i < objs.length; i++) {
                if (objs[i] == null && type.OPTIONAL[i]) {
                    continue;
                }
                checkEncode(objs[i], type.type[i]);
            }
        } else if (value instanceof ASN1Encodable) {
            Object[] objs = new Object[type.type.length];
            ((ASN1Encodable) value).encodeValues(objs);
            checkEncodeSequence(objs, type);
        } else {
            Assert
                    .fail("Value for ASN1SequenceWrap should be Object[], or "
                            + ASN1ENCODABLE_NAME);
        }
    }

    private static ASN1Type getWrappedType(ASN1Implicit type) {
        try {
            Field field = ASN1Implicit.class.getDeclaredField("type");
            field.setAccessible(true);
            return (ASN1Type) field.get(type);
        } catch (Exception e) {
            // can't reach, unless implement changed
            throw new RuntimeException("Can't get wrapped type.", e);
        }
    }
}
