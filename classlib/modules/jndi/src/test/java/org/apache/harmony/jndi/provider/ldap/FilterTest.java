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

package org.apache.harmony.jndi.provider.ldap;

import org.apache.harmony.jndi.internal.parser.AttributeTypeAndValuePair;
import org.apache.harmony.jndi.provider.ldap.Filter.MatchingRuleAssertion;
import org.apache.harmony.jndi.provider.ldap.Filter.SubstringFilter;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1TestUtils;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;

import junit.framework.TestCase;

public class FilterTest extends TestCase {
    private Filter filter;

    public void test_constructor_I() {
        filter = new Filter(Filter.APPROX_MATCH_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.EQUALITY_MATCH_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.EXTENSIBLE_MATCH_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.GREATER_OR_EQUAL_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.LESS_OR_EQUAL_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.NOT_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.PRESENT_FILTER);
        assertTrue(filter.isLeaf());
        filter = new Filter(Filter.SUBSTRINGS_FILTER);
        assertTrue(filter.isLeaf());

        filter = new Filter(Filter.AND_FILTER);
        assertFalse(filter.isLeaf());

        filter = new Filter(Filter.OR_FILTER);
        assertFalse(filter.isLeaf());

        try {
            filter = new Filter(-1);
            fail("Should throws IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            filter = new Filter(10);
            fail("Should throws IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void test_encodeValues() {
        // simple filter
        filter = new Filter(Filter.APPROX_MATCH_FILTER);
        filter.setValue(new AttributeTypeAndValuePair("cn", "test"));
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
        
        filter = new Filter(Filter.EXTENSIBLE_MATCH_FILTER);
        MatchingRuleAssertion value = new MatchingRuleAssertion();
        value.setDnAttributes(true);
        assertTrue(value.isDnAttributes());
        value.setMatchingRule("equal");
        assertEquals("equal", value.getMatchingRule());
        value.setMatchValue("cn");
        assertEquals("cn", value.getMatchValue());
        value.setType("type");
        assertEquals("type", value.getType());
        filter.setValue(value);

        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
        
        // composite filter
        filter = new Filter(Filter.AND_FILTER);
        Filter equal = new Filter(Filter.EQUALITY_MATCH_FILTER);
        equal.setValue(new AttributeTypeAndValuePair("sn", "tom"));
        filter.addChild(equal);
        
        Filter substring = new Filter(Filter.SUBSTRINGS_FILTER);
        SubstringFilter sub = new SubstringFilter("o");
        sub.addAny("harmony");
        sub.addFinal("good");
        substring.setValue(sub);
        filter.addChild(substring);
        
        Filter present = new Filter(Filter.PRESENT_FILTER);
        present.setValue("objectClass");
        filter.addChild(present);
        
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
        
        // more complex filter
        Filter or = new Filter(Filter.OR_FILTER);
        Filter not = new Filter(Filter.NOT_FILTER);
        Filter greater = new Filter(Filter.GREATER_OR_EQUAL_FILTER);
        greater.setValue(new AttributeTypeAndValuePair("cn", "hello"));
        not.setValue(greater);
        or.addChild(not);
        
        Filter less = new Filter(Filter.LESS_OR_EQUAL_FILTER);
        less.setValue(new AttributeTypeAndValuePair("o", "apache"));
        or.addChild(less);
        filter.addChild(or);
        
        ASN1TestUtils.checkEncode(filter, LdapASN1Constant.Filter);
    }
}
