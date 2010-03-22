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

package org.apache.harmony.jndi.provider.ldap.parser;

import java.util.List;

import org.apache.harmony.jndi.internal.parser.AttributeTypeAndValuePair;
import org.apache.harmony.jndi.provider.ldap.Filter;
import org.apache.harmony.jndi.provider.ldap.Filter.SubstringFilter;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;
import org.apache.harmony.jndi.provider.ldap.parser.FilterParser;

import junit.framework.TestCase;

public class FilterParserTest extends TestCase {

    private static final String TEST_FILE = "/parser/parser.filter.test";

    private FilterParser parser;

    private Filter filter;

    /**
     * test whether correct filters can be accepted by the parser
     * 
     * @throws Exception
     */
    public void test_grammar() throws Exception {
        parser = new FilterParser(getClass().getClassLoader()
                .getResourceAsStream(TEST_FILE));
        parser.test();
    }

    public void test_parse() throws Exception {
        parser = new FilterParser("(cn=Babs Jensen)");
        filter = parser.parse();
        assertEquals(Filter.EQUALITY_MATCH_FILTER, filter.getType());
        assertAttributeValueAssertion("cn", "Babs Jensen", filter.getValue());

        parser = new FilterParser("(!(cn=Tim Howes))");
        filter = parser.parse();
        assertEquals(Filter.NOT_FILTER, filter.getType());
        assertTrue(filter.getValue() instanceof Filter);
        filter = (Filter) filter.getValue();
        assertEquals(Filter.EQUALITY_MATCH_FILTER, filter.getType());
        assertAttributeValueAssertion("cn", "Tim Howes", filter.getValue());

        parser = new FilterParser(
                "(&(objectClass=Person)(|(sn=Jensen)(cn=Babs J*)))");
        filter = parser.parse();
        assertEquals(Filter.AND_FILTER, filter.getType());
        assertFalse(filter.isLeaf());
        List<Filter> children = filter.getChildren();
        assertEquals(2, children.size());
        assertEquals(Filter.EQUALITY_MATCH_FILTER, children.get(0).getType());
        assertAttributeValueAssertion("objectClass", "Person", children.get(0)
                .getValue());
        filter = children.get(1);
        assertEquals(Filter.OR_FILTER, filter.getType());
        assertFalse(filter.isLeaf());
        children = filter.getChildren();
        assertEquals(2, children.size());
        assertAttributeValueAssertion("sn", "Jensen", children.get(0)
                .getValue());
        filter = children.get(1);
        assertEquals(Filter.SUBSTRINGS_FILTER, filter.getType());
        assertEquals(SubstringFilter.class, filter.getValue().getClass());
        SubstringFilter sub = (SubstringFilter) filter.getValue();
        assertEquals("cn", sub.getType());
        // TODO: not sure whether '*' should be part of 'any'
        // assertEquals(1, sub.getSubstrings().size());
        ChosenValue chosen = sub.getSubstrings().get(0);
        assertEquals("Babs J", chosen.getValue());
        assertEquals(0, chosen.getIndex());
    }

    public void test_parse_special_char() throws Exception {
        parser = new FilterParser("(cn=\\2Atest)");
        filter = parser.parse();
        assertEquals(Filter.EQUALITY_MATCH_FILTER, filter.getType());
        assertAttributeValueAssertion("cn", "*test", filter.getValue());
    }

    public void test_parse_argument() throws Exception {
        parser = new FilterParser("(cn={0})");
        parser.setArgs(new Object[] { "value" });
        filter = parser.parse();
        assertEquals(Filter.EQUALITY_MATCH_FILTER, filter.getType());
        assertAttributeValueAssertion("cn", "value", filter.getValue());

        parser = new FilterParser("(cn=start{0}end)");
        parser.setArgs(new Object[] { "value" });
        filter = parser.parse();
        assertEquals(Filter.EQUALITY_MATCH_FILTER, filter.getType());
        assertAttributeValueAssertion("cn", "startvalueend", filter.getValue());

        parser = new FilterParser("({2}cn{1}=test{0})");
        parser.setArgs(new Object[] { "value0", "value1", "value2" });
        filter = parser.parse();
        assertEquals(Filter.EQUALITY_MATCH_FILTER, filter.getType());
        assertAttributeValueAssertion("value2cnvalue1", "testvalue0", filter
                .getValue());
    }

    private void assertAttributeValueAssertion(String attrType,
            String attrValue, Object filterValue) {
        assertTrue(filterValue instanceof AttributeTypeAndValuePair);
        AttributeTypeAndValuePair pair = (AttributeTypeAndValuePair) filterValue;
        assertEquals(attrType, pair.getType());
        assertEquals(attrValue, pair.getValue());
    }
}
