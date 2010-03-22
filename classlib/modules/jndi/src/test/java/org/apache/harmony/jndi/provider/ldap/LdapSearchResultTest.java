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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import junit.framework.TestCase;

public class LdapSearchResultTest extends TestCase {
    public void test_getter_setter() {
        LdapSearchResult result = new LdapSearchResult();

        assertNull(result.getAddress());
        assertNull(result.getBinaryAttributes());
        assertNotNull(result.getEntries());
        assertEquals(0, result.getEntries().size());
        assertNull(result.getException());
        assertNull(result.getRefURLs());
        assertNull(result.getResult());
        assertTrue(result.isEmpty());

        result.setAddress("localhost");
        assertEquals("localhost", result.getAddress());

        result.setBinaryAttributes(new String[] { "cn", "objectClass" });
        assertEquals("cn", result.getBinaryAttributes()[0]);
        assertEquals("objectClass", result.getBinaryAttributes()[1]);

        NamingException exception = new NamingException();
        result.setException(exception);
        assertSame(exception, result.getException());

        List<String> refs = new ArrayList<String>();
        refs.add("ldap://localhost:2008");
        refs.add("ldap://localhost:1997/cn=test");
        assertNull(result.getRefURLs());
        result.setRefURLs(refs);

        assertEquals(2, result.getRefURLs().size());
        assertTrue(result.getRefURLs().contains("ldap://localhost:2008"));
        assertTrue(result.getRefURLs()
                .contains("ldap://localhost:1997/cn=test"));
    }

    public void test_toNameClassPairEnumeration() throws Exception {
        HashMap<String, Attributes> entries = new HashMap<String, Attributes>();
        entries.put("ou=harmony,o=apache,cn=test", new BasicAttributes("ou",
                "harmony", true));
        entries.put("module=jndi,cn=test", new BasicAttributes(
                "javaClassName", String.class.getName()));

        MockLdapSearchResult result = new MockLdapSearchResult();

        result.decodeEntry(entries);

        NamingEnumeration<NameClassPair> enu = result
                .toNameClassPairEnumeration("cn=test");

        HashMap<String, NameClassPair> values = new HashMap<String, NameClassPair>();
        while (enu.hasMore()) {
            NameClassPair pair = enu.next();
            values.put(pair.getName(), pair);
        }

        assertEquals(entries.size(), values.size());

        assertTrue(values.containsKey("ou=harmony,o=apache"));
        NameClassPair pair = values.get("ou=harmony,o=apache");
        assertEquals("ou=harmony,o=apache", pair.getName());
        assertEquals(DirContext.class.getName(), pair.getClassName());
        assertEquals("ou=harmony,o=apache,cn=test", pair.getNameInNamespace());

        assertTrue(values.containsKey("module=jndi"));
        pair = values.get("module=jndi");
        assertEquals("module=jndi", pair.getName());
        assertEquals(String.class.getName(), pair.getClassName());
        assertEquals("module=jndi,cn=test", pair.getNameInNamespace());
    }

    public void test_toBindingEnumeration() throws Exception {
        HashMap<String, Attributes> entries = new HashMap<String, Attributes>();
        entries.put("ou=harmony,o=apache,cn=test", new BasicAttributes("ou",
                "harmony", true));
        entries.put("module=jndi,o=apache,cn=test", new BasicAttributes(
                "javaClassName", String.class.getName()));

        MockLdapSearchResult result = new MockLdapSearchResult();

        result.decodeEntry(entries);

        NamingEnumeration<Binding> enu = result.toBindingEnumeration(
                new LdapContextImpl(new MockLdapClient(),
                        new Hashtable<Object, Object>(), "cn=test"),
                new LdapName("o=apache"));

        HashMap<String, Binding> values = new HashMap<String, Binding>();
        while (enu.hasMore()) {
            Binding binding = enu.next();
            values.put(binding.getName(), binding);
        }

        assertEquals(entries.size(), values.size());

        assertTrue(values.containsKey("ou=harmony"));
        Binding binding = values.get("ou=harmony");
        assertEquals("ou=harmony", binding.getName());
        assertEquals(DirContext.class.getName(), binding.getClassName());
        assertEquals("ou=harmony,o=apache,cn=test", binding
                .getNameInNamespace());
        assertEquals(LdapContextImpl.class, binding.getObject().getClass());

        assertTrue(values.containsKey("module=jndi"));
        binding = values.get("module=jndi");
        assertEquals("module=jndi", binding.getName());
        assertEquals(String.class.getName(), binding.getClassName());
        assertEquals("module=jndi,o=apache,cn=test", binding
                .getNameInNamespace());
    }

    public void test_toSearchResultEnumeration() throws Exception {
        HashMap<String, Attributes> entries = new HashMap<String, Attributes>();
        entries.put("ou=harmony,o=apache,cn=test", new BasicAttributes("ou",
                "harmony", true));
        entries.put("module=jndi,o=apache,cn=test", new BasicAttributes(
                "javaClassName", String.class.getName()));

        MockLdapSearchResult result = new MockLdapSearchResult();

        result.decodeEntry(entries);

        NamingEnumeration<SearchResult> enu = result
                .toSearchResultEnumeration("cn=test");

        HashMap<String, SearchResult> values = new HashMap<String, SearchResult>();
        while (enu.hasMore()) {
            SearchResult searchResult = enu.next();
            values.put(searchResult.getName(), searchResult);
        }

        assertEquals(entries.size(), values.size());

        assertTrue(values.containsKey("ou=harmony,o=apache"));
        SearchResult searchResult = values.get("ou=harmony,o=apache");
        assertEquals("ou=harmony,o=apache", searchResult.getName());
        assertNull(searchResult.getClassName());
        assertEquals("ou=harmony,o=apache,cn=test", searchResult
                .getNameInNamespace());
        assertNull(searchResult.getObject());
        
        Attributes attrs = searchResult.getAttributes();
        assertEquals(1, attrs.size());
        assertEquals(1, attrs.get("ou").size());
        assertEquals("harmony", attrs.get("ou").get());

        assertTrue(values.containsKey("module=jndi,o=apache"));
        searchResult = values.get("module=jndi,o=apache");
        assertEquals("module=jndi,o=apache", searchResult.getName());
        assertNull(searchResult.getClassName());
        assertEquals("module=jndi,o=apache,cn=test", searchResult
                .getNameInNamespace());
        assertNull(searchResult.getObject());
        
        attrs = searchResult.getAttributes();
        assertEquals(1, attrs.size());
        assertEquals(1, attrs.get("javaClassName").size());
        assertEquals(String.class.getName(), attrs.get("javaClassName").get());
    }
    
    public static class MockLdapSearchResult extends LdapSearchResult {
        public void decodeEntry(Object value) {
            entries = (HashMap<String, Attributes>) ((HashMap<String, Attributes>) value)
                    .clone();
        }
    }
}
