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

import javax.naming.directory.SearchControls;

import junit.framework.TestCase;

public class LdapUrlParserTest extends TestCase {
    private static final String TEST_FILE = "/parser/parser.url.test";

    private LdapUrlParser parser;

    private SearchControls controls;

    private org.apache.harmony.jndi.provider.ldap.Filter filter;

    public void test_grammar() throws Exception {
        parser = new LdapUrlParser(getClass().getClassLoader()
                .getResourceAsStream(TEST_FILE));
        parser.test();
    }

    public void test_parseURL() throws Exception {
        parser = new LdapUrlParser("ldap://192.168.1.3:666");
        parser.parseURL();
        // default dn is empty
        assertEquals("", parser.getBaseObject());
        assertFalse(parser.hasFilter());
        assertNull(parser.getFilter());
        assertFalse(parser.hasAttributes());
        assertFalse(parser.hasScope());
        assertFalse(parser.hasExtensions());
        assertNull(parser.getControls());
        assertEquals("192.168.1.3", parser.getHost());
        assertEquals(666, parser.getPort());

        parser = new LdapUrlParser("ldap://192.168.1.3/o=University,c=US");
        parser.parseURL();
        assertEquals("o=University,c=US", parser.getBaseObject());
        assertFalse(parser.hasFilter());
        assertNull(parser.getFilter());
        assertFalse(parser.hasAttributes());
        assertFalse(parser.hasScope());
        assertFalse(parser.hasExtensions());
        assertNull(parser.getControls());
        assertEquals("192.168.1.3", parser.getHost());
        // default port is 389
        assertEquals(389, parser.getPort());

        parser = new LdapUrlParser(
                "ldaps://192.168.1.3/o=University,c=US?postalAddress");
        parser.parseURL();
        assertEquals("o=University,c=US", parser.getBaseObject());
        assertEquals("192.168.1.3", parser.getHost());
        assertNotNull(parser.getControls());
        assertTrue(parser.hasAttributes());
        assertFalse(parser.hasScope());
        assertFalse(parser.hasExtensions());
        controls = parser.getControls();
        assertEquals(SearchControls.ONELEVEL_SCOPE, controls.getSearchScope());
        assertEquals(1, controls.getReturningAttributes().length);
        assertEquals("postalAddress", controls.getReturningAttributes()[0]);
        assertNull(parser.getFilter());
        // default ssl port is 636
        assertEquals(636, parser.getPort());
        
        
        parser = new LdapUrlParser(
                "ldap://192.168.1.3/o=University,c=US?postalAddress,o");
        parser.parseURL();
        assertEquals("o=University,c=US", parser.getBaseObject());
        assertEquals("192.168.1.3", parser.getHost());
        assertTrue(parser.hasAttributes());
        assertNotNull(parser.getControls());
        assertFalse(parser.hasScope());
        assertFalse(parser.hasExtensions());
        controls = parser.getControls();
        assertEquals(SearchControls.ONELEVEL_SCOPE, controls.getSearchScope());
        assertEquals(2, controls.getReturningAttributes().length);
        assertEquals("postalAddress", controls.getReturningAttributes()[0]);
        assertEquals("o", controls.getReturningAttributes()[1]);
        assertNull(parser.getFilter());
        // default port is 389
        assertEquals(389, parser.getPort());
        
        parser = new LdapUrlParser("ldap:///o=University,c=US");
        parser.parseURL();
        assertEquals("o=University,c=US", parser.getBaseObject());
        assertNull(parser.getFilter());
        assertNull(parser.getControls());
        // default host is 'localhost'
        assertEquals("localhost", parser.getHost());
        // default port is 389
        assertEquals(389, parser.getPort());

        parser = new LdapUrlParser("ldap://192.168.1.3/o=University,c=US??sub");
        parser.parseURL();
        assertEquals("o=University,c=US", parser.getBaseObject());
        assertEquals("192.168.1.3", parser.getHost());
        assertFalse(parser.hasAttributes());
        assertTrue(parser.hasScope());
        assertFalse(parser.hasExtensions());
        assertNotNull(parser.getControls());
        controls = parser.getControls();
        assertEquals(SearchControls.SUBTREE_SCOPE, controls.getSearchScope());

        parser = new LdapUrlParser(
                "ldap://192.168.1.3/o=University,c=US??wrong");
        try {
            parser.parseURL();
            fail("Should raise ParseException");
        } catch (ParseException e) {
            // expected
        }

        parser = new LdapUrlParser(
                "ldap://192.168.1.3/c=US?o=University?sub?(objectClass=*)");
        parser.parseURL();
        assertEquals("c=US", parser.getBaseObject());
        assertEquals("192.168.1.3", parser.getHost());
        assertNotNull(parser.getControls());
        assertTrue(parser.hasAttributes());
        assertTrue(parser.hasScope());
        assertFalse(parser.hasExtensions());
        controls = parser.getControls();
        assertEquals(SearchControls.SUBTREE_SCOPE, controls.getSearchScope());
        assertTrue(parser.hasFilter());
        assertNotNull(parser.getFilter());

        parser = new LdapUrlParser(
                "ldap://192.168.1.3/c=US?o=University?sub?(objectClass=*)");
        parser.parseURL();
        assertEquals("c=US", parser.getBaseObject());
        assertEquals("192.168.1.3", parser.getHost());
        assertNotNull(parser.getControls());
        controls = parser.getControls();
        assertEquals(SearchControls.SUBTREE_SCOPE, controls.getSearchScope());
        assertNotNull(parser.getFilter());

    }
    
    public void test_parseURL_special_char() throws Exception {
        parser = new LdapUrlParser("ldap:///o=University%20of%20Michigan,c=US");
        parser.parseURL();
        assertEquals("o=University of Michigan,c=US", parser.getBaseObject());

    }
}
